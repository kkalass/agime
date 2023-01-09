package de.kalass.commons.ml;

import com.google.common.base.Function;
import com.google.common.base.Predicate;


import org.ejml.ops.CommonOps;
import org.ejml.simple.SimpleMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import javax.annotation.Nullable;

import static de.kalass.commons.ml.Octave.checkColumnVector;
import static de.kalass.commons.ml.Octave.scalarMultiply;
import static de.kalass.commons.ml.Octave.sigmoid;

/**
 * Created by klas on 02.04.14.
 */
public class LogisticRegression {
    private static final Logger LOG = LoggerFactory.getLogger("LogisticRegression");

    public static class CalculateProbability implements Function<double[], Double> {
        private final SimpleMatrix _theta;
        private final String _name;

        public CalculateProbability(final SimpleMatrix theta, String name) {
            _theta = theta;
            _name = name;
        }

        @Nullable
        @Override
        public Double apply(@Nullable final double[] input) {
            return calculateProbability(_name, _theta, input);
        }

    }

    /**
     * Calculates the probability for the y value being yValueName, given the input values and
     * the previously learned theta.
     */
    public static Double calculateProbability(String yValueName, SimpleMatrix theta, final double[] input) {
        SimpleMatrix imatrix = Octave.createRowMatrix(input);
        SimpleMatrix matrix = imatrix.mult(theta);

        if (matrix.numCols() != 1) {
            throw new IllegalStateException("Got " + matrix.numCols() + " columns " + matrix);
        }
        if (matrix.numRows() != 1) {
            throw new IllegalStateException("Got " + matrix.numRows() + " rows " + matrix);
        }

        double z = matrix.get(0, 0);
        double g = Octave.sigmoid(z);
        if (LOG.isDebugEnabled()) {LOG.debug("p(y=" + yValueName + ") " + Arrays.toString(input) + " => z: " + z + ", g(z): " + g);}
        return g;
    }


    public static final class Classify implements Predicate<double[]> {
        private final Function<double[], Double> _prediction;

        public Classify(ThetaAndCosts thetaAndCosts, String name) {
            _prediction = new CalculateProbability(thetaAndCosts.theta, name);
        }

        @Override
        public boolean apply(@Nullable final double[] input) {
            Double prediction = _prediction.apply(input);
            return prediction.doubleValue() >= 0.5d;
        }
    }

    public static Function<SimpleMatrix/*theta*/, Double> costFunction(final SimpleMatrix trainingSetInput, final SimpleMatrix y) {
        checkTrainingSet(trainingSetInput, y);
        return new Function<SimpleMatrix, Double>() {
            @Nullable
            @Override
            public Double apply(@Nullable final SimpleMatrix input) {
                // (1/m)*sum(-y.*log(sigmoid(X*theta)) - (1-y).*log(1 - sigmoid(X*theta)));
                int m = trainingSetInput.numRows();
                SimpleMatrix y1predictions = predict(trainingSetInput, input);
                SimpleMatrix y0predictions = y1predictions.scale(-1);
                CommonOps.add(y0predictions.getMatrix(), 1);
                SimpleMatrix minusY = y.scale(-1);
                SimpleMatrix oneMinusY = minusY.copy();
                CommonOps.add(oneMinusY.getMatrix(), 1);

                SimpleMatrix y1term = checkColumnVector(scalarMultiply(minusY, Octave.log(y1predictions)));
                SimpleMatrix y0Term = checkColumnVector(scalarMultiply(oneMinusY, Octave.log(y0predictions)));
                double[] sums = Octave.sumCols(Octave.scalarSubtract(y1term, y0Term));
                if (sums.length != 1) {
                    throw new IllegalStateException("Expected exactly one entry as a result of sumCols for a column vector, but got " + sums.length);
                }
                double sum = sums[0];
                return (1d/(double)m)*sum;
            }


        };
    }

    /**
     * sigmoid(X*theta)
     * @return for each row in the xMatrix the prediction for the probability of y being 1 (instead of 0)
     */
    private static SimpleMatrix predict(final SimpleMatrix xMatrix, final SimpleMatrix theta) {
        return checkColumnVector(sigmoid(xMatrix.mult(theta)));
    }

    public static Function<SimpleMatrix/*theta*/, SimpleMatrix> gradientFunction(final SimpleMatrix xMatrix, final SimpleMatrix y) {
        checkTrainingSet(xMatrix, y);
        return new Function<SimpleMatrix, SimpleMatrix>() {
            @Nullable
            @Override
            public SimpleMatrix apply(@Nullable final SimpleMatrix theta) {
                if (LOG.isTraceEnabled()) {LOG.trace("gradientFunction(X=" + xMatrix + ", y=" + y + ", theta=" + theta + ")");}

                // ((1/m).*sum(((sigmoid(X*theta)-y)*ones(1, size(X,2))).*X, 1))'
                int m = xMatrix.numRows();
                if (LOG.isTraceEnabled()) {LOG.trace("gradientFunction | m: " + m);}

                // sigmoid(X*theta)
                SimpleMatrix y1predictions = predict(xMatrix, theta);

                if (LOG.isTraceEnabled()) {LOG.trace("gradientFunction | y1Predictions: " + y1predictions);}

                // sigmoid(X*theta)-y
                SimpleMatrix distances = Octave.scalarSubtract(y1predictions, y);
                if (LOG.isTraceEnabled()) {LOG.trace("gradientFunction | distances: " + distances);}

                // (sigmoid(X*theta)-y)*ones(1, size(X,2))
                SimpleMatrix distancesInAllColumns = distances.mult(Octave.ones(1, xMatrix.numCols()));

                if (LOG.isTraceEnabled()) {LOG.trace("gradientFunction | distancesInAllColumns: " + distancesInAllColumns);}

                // ((sigmoid(X*theta)-y)*ones(1, size(X,2))).*X
                SimpleMatrix distancesTimesInputValues = Octave.scalarMultiply(distancesInAllColumns, xMatrix);

                if (LOG.isTraceEnabled()) {LOG.trace("gradientFunction | distancesTimesInputValues: " + distancesTimesInputValues);}

                // sum(((sigmoid(X*theta)-y)*ones(1, size(X,2))).*X, 1)
                double[] resultPerColumn = Octave.sumCols(distancesTimesInputValues);

                if (LOG.isTraceEnabled()) {LOG.trace("gradientFunction | resultPerColumn: " + Arrays.toString(resultPerColumn));}

                // (1/m).*sum(((sigmoid(X*theta)-y)*ones(1, size(X,2))).*X, 1)
                // transpose not nessessary, due to createColumnSimpleMatrix

                SimpleMatrix result = Octave.createColMatrix(resultPerColumn).scale(1d / m);

                if (LOG.isTraceEnabled()) {LOG.trace("gradientFunction | result: " + result);}

                return result;
            }
        };

    }

    private static void checkTrainingSet(final SimpleMatrix trainingSetInput, final SimpleMatrix trainingSetOutput) {
        checkColumnVector(trainingSetOutput);

        if (trainingSetInput.numRows() != trainingSetOutput.numRows()) {
            throw new IllegalStateException("Input has " + trainingSetInput.numRows() + " rows, output has " + trainingSetOutput.numRows() + " rows");
        }
    }



    public static MinimizationInputFunction costAndGradientFunction(final SimpleMatrix trainingSetInput, SimpleMatrix trainingSetOutput) {

        final Function<SimpleMatrix, Double> costFunction = costFunction(trainingSetInput, trainingSetOutput);
        final Function<SimpleMatrix, SimpleMatrix> gradientFunction = gradientFunction(trainingSetInput, trainingSetOutput);
        return new MinimizationInputFunction() {
            @Override
            public int getNumDimensions() {
                return trainingSetInput.numCols();
            }

            @Nullable
            @Override
            public CostAndGradient apply(@Nullable final SimpleMatrix input) {
                return new CostAndGradient(gradientFunction.apply(input), costFunction.apply(input));
            }
        };
    }

}