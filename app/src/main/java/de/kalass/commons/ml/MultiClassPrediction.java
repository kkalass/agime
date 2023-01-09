package de.kalass.commons.ml;



import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;

import org.ejml.simple.SimpleMatrix;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Predicts a value of a non-ordinal-class with more than two values (classification).
 *
 * Created by klas on 04.04.14.
 */
public class MultiClassPrediction<T> implements Function<double[], MultiClassPredictionResult<T>> {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger("MultiClassPrediction");

    private static final Ordering<MultiClassPredictionResult<?>> ORDERING = new Ordering<MultiClassPredictionResult<?>>() {
        @Override
        public int compare(@Nullable final MultiClassPredictionResult<?> left, @Nullable final MultiClassPredictionResult<?> right) {
            return Double.compare(left.getProbability(), right.getProbability());
        }
    };
    public static final boolean TRAIN_SINGLE_THREADED = false;

    private final Collection<InputItem<T>> _probabilityFunctions;
    private final Octave.DFunction _scalingFunction;
    private final double[] _means;
    private final double[] _stdDevs;

    public static final class InputItem<T> {
        private final T _classValue;
        private final SimpleMatrix _theta;

        public InputItem(final T classValue, final SimpleMatrix theta) {
            _classValue = classValue;
            _theta = theta;
        }

        public SimpleMatrix getTheta() {
            return _theta;
        }

        public T getClassValue() {
            return _classValue;
        }

        public Double calculateProbability(double[] input) {
            return LogisticRegression.calculateProbability(_classValue == null ? null : _classValue.toString(), _theta, input);
        }
    }

    public MultiClassPrediction(final Collection<InputItem<T>> probabilityFunctions, double[] means, double[] stdDevs) {
        _means = means;
        _stdDevs = stdDevs;
        _scalingFunction = Octave.createScaleFunction(means, stdDevs);
        _probabilityFunctions = Preconditions.checkNotNull(probabilityFunctions);
    }

    public double[] getMeans() {
        return _means;
    }

    public double[] getStdDevs() {
        return _stdDevs;
    }

    public Collection<InputItem<T>> getProbabilityFunctions() {
        return _probabilityFunctions;
    }

    @Nullable
    @Override
    public MultiClassPredictionResult<T> apply(@Nullable final double[] input) {
        List<MultiClassPredictionResult<T>> predictionResults = calculatePredictions(input);
        return ORDERING.max(predictionResults);
    }

    @Nonnull
    public List<MultiClassPredictionResult<T>> predict(double[] input) {
        return ORDERING.sortedCopy(calculatePredictions(input));
    }

    private final double[] scaleDown(double[] input) {
        double[] res = new double[input.length];
        for (int i = 0; i < input.length; i++) {
            res[i] = _scalingFunction.apply(0, i, input[i]);
        }
        return res;
    }

    private List<MultiClassPredictionResult<T>> calculatePredictions(double[] unscaledInput) {
        double[] input = scaleDown(unscaledInput);
        ImmutableList.Builder<MultiClassPredictionResult<T>> b = ImmutableList.builder();
        for (InputItem<T> e: _probabilityFunctions) {

            double probability = e.calculateProbability(input);
            b.add(new MultiClassPredictionResult<T>(e._classValue, probability));
        }
        return b.build();
    }
    public static <T> MultiClassPrediction<T> train(

            SimpleMatrix trainingXsUnscaled, T[] trainingYs, long timeoutMillis
    ) throws TimeoutException {
        // FIXME: use amount of available cores as default size for executor
        return train(Executors.newFixedThreadPool(4), trainingXsUnscaled, trainingYs, timeoutMillis);
    }

    public static <T> MultiClassPrediction<T> train(
            ExecutorService executorService,
            SimpleMatrix trainingXsUnscaled, T[] trainingYs, long timeoutMillis
    ) throws TimeoutException {
        final long endTime = System.currentTimeMillis() + timeoutMillis;
        int numColumns = trainingXsUnscaled.numCols();

        final double[] means = new double[numColumns];
        final double[] stdDev = new double[numColumns];

        for (int ci = 1; ci < numColumns; ci++) {
            means[ci] = Octave.meanOfColumn(trainingXsUnscaled, ci);
            stdDev[ci] = Octave.stdevOfColumn(trainingXsUnscaled, ci, means[ci]);
        }

        Octave.DFunction scaleFunction = Octave.createScaleFunction(means, stdDev);
        SimpleMatrix trainingXs = Octave.transform(trainingXsUnscaled, scaleFunction);

        // zeroes
        final SimpleMatrix initialTheta = new SimpleMatrix(trainingXsUnscaled.numCols(), 1); // column vector with as many rows as the input has dimensions
        //final SimpleMatrix initialTheta = Octave.ones(trainingXsUnscaled.numCols(), 1); // column vector with as many rows as the input has dimensions
        final double initialAlpha = 1.0;

        ImmutableSet<T> classes = ImmutableSet.copyOf(trainingYs);
        List<InputItem<T>> inputItems =
                TRAIN_SINGLE_THREADED ? trainSingleThreaded(trainingYs, endTime, trainingXs, initialTheta, initialAlpha, classes)
                 : trainMultithreaded(executorService, trainingYs, endTime, trainingXs, initialTheta, initialAlpha, classes);
        return new MultiClassPrediction<T>(inputItems, means, stdDev);
    }

    private static <T> ImmutableList<InputItem<T>> trainSingleThreaded(final T[] trainingYs,
                                                                       final long endTime,
                                                                       final SimpleMatrix trainingXs,
                                                                       SimpleMatrix initialTheta, double initialAlpha,
                                                                       final ImmutableSet<T> classes
    ) throws TimeoutException {
        ImmutableList.Builder<InputItem<T>> builder = ImmutableList.builder();
        int c = 0;
        for (final T t: classes) {
            c++;
            SimpleMatrix oneVsAllYs = col(trainingYs, t);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Training " + t + " => " + oneVsAllYs);
            }
            long startTime = System.currentTimeMillis();
            MinimizationInputFunction costFunction = LogisticRegression.costAndGradientFunction(trainingXs, oneVsAllYs);
            final GradientDescend.ThetaAndCostsGD learnedParameters = train(endTime - System.currentTimeMillis(), initialTheta, initialAlpha, costFunction);
            //if (c==1) {
                initialAlpha = learnedParameters.getAlpha();
                initialTheta = learnedParameters.theta;
            //}
            if (LOG.isDebugEnabled()) {
                LOG.debug("Trained " + c + " of " + classes.size() + " (" + t + ") in " + (System.currentTimeMillis() - startTime) + " ms " + ": " + learnedParameters + " | " + (endTime - System.currentTimeMillis()) + " ms remaining for training");
            }


            builder.add(new InputItem<T>(t, learnedParameters.theta));
        }

        return builder.build();
    }

    private static <T> ImmutableList<InputItem<T>> trainMultithreaded(final ExecutorService executorService, final T[] trainingYs, final long endTime, final SimpleMatrix trainingXs, final SimpleMatrix initialTheta, final double initialAlpha, final ImmutableSet<T> classes) throws TimeoutException {
        ImmutableList.Builder<Future<InputItem<T>>> futureBuilder = ImmutableList.builder();
        int c = 0;
        for (final T t: classes) {
            c++;
            SimpleMatrix oneVsAllYs = col(trainingYs, t);
            if (LOG.isWarnEnabled()) {
                LOG.warn("Training " + t/* + " => " + oneVsAllYs*/);
            }
            //long startTime = System.currentTimeMillis();
            MinimizationInputFunction costFunction = LogisticRegression.costAndGradientFunction(trainingXs, oneVsAllYs);
            Callable<InputItem<T>> task = trainingCallable(endTime, initialTheta, initialAlpha, t, costFunction);
            Future<InputItem<T>> f = executorService.submit(task);
            futureBuilder.add(f);
        }

        ImmutableList<Future<InputItem<T>>> futures = futureBuilder.build();

        ImmutableList.Builder<InputItem<T>> builder = ImmutableList.builder();
        for (Future<InputItem<T>> f : futures) {
            try {
                builder.add(f.get());
            } catch (InterruptedException e) {
                throw new TimeoutException("Training failed due to interruption " + e);
            } catch (ExecutionException e) {
                throw new IllegalStateException("Training failed", e.getCause());
            }
        }
        return builder.build();
    }

    private static <T> Callable<InputItem<T>> trainingCallable(final long endTime, final SimpleMatrix initialTheta, final double initialAlpha, final T t, final MinimizationInputFunction costFunction) {
        return new Callable<InputItem<T>>() {
            @Override
            public InputItem<T> call() throws Exception {
                long starttime = System.currentTimeMillis();
                LOG.warn("START Train " + t);
                try {
                    final GradientDescend.ThetaAndCostsGD learnedParameters = train(endTime - System.currentTimeMillis(), initialTheta, initialAlpha, costFunction);
                    return new InputItem<T>(t, learnedParameters.theta);
                } finally {
                    LOG.warn("END training of " + t + " in " + (System.currentTimeMillis() - starttime) + "ms ");
                }
            }
        };
    }

    private static <T> SimpleMatrix col(final T[] ys, final T t) {
        SimpleMatrix matrix = new SimpleMatrix(ys.length, 1);
        for (int i = 0; i < ys.length; i++) {
            matrix.set(i, 0, t.equals(ys[i]) ? 1.0 : 0.0);
        }
        return matrix;
    }

    private static GradientDescend.ThetaAndCostsGD train(
            long timeoutMillis,
            SimpleMatrix initialTheta,
            double initialAlpha, final MinimizationInputFunction costFunction
    ) throws TimeoutException {
        return GradientDescend.getInstance().minimize(
                costFunction,
                timeoutMillis,
                initialTheta, initialAlpha
        );
    }

}
