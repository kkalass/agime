package de.kalass.commons.ml;

import com.google.common.base.Function;
import com.google.common.base.Objects;


import org.ejml.simple.SimpleMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeoutException;

/**
 * Created by klas on 02.04.14.
 */
public class GradientDescend implements GradientMinimizer {
    private static final Logger LOG = LoggerFactory.getLogger("GradientDescend");
    public static final int ALPHA_DETECTION_ITERATIONS = 400;
    public static final double COST_TERMINATION_THRESHOLD = 0.000001; //NumberUtil.DEFAULT_EQUALS_ROUGHLY_THRESHOLD

    private static final boolean terminates(double previousCost, double currentCost, int currentIdx, double threshold) {
        if (currentIdx <= 1) {
            return false;
        }
        return NumberUtil.equalsRoughly(previousCost, currentCost, threshold) && currentCost != Double.POSITIVE_INFINITY && currentCost != Double.NaN;
    }

   public static final class CostIncreasedException extends Exception {

       public CostIncreasedException(final double previousCost, final double currentCost, final int i) {
           super("On Iteration " + i + ", cost increased from " + previousCost + " to " + currentCost);
       }
   }

   public static final class NotTerminatedException extends Exception {
       public NotTerminatedException(final double previousCost, final double currentCost, final int i) {
           super("Not terminated after " + i + " iterations, previousCost: " + previousCost + ", currentCost: " + currentCost);
       }
   }

    public static final class MatrixNaNOrInfinityException extends RuntimeException {

        MatrixNaNOrInfinityException(final SimpleMatrix vector, final double stepSize, final double previousCost, final double currentCost, SimpleMatrix matrix) {
            this(matrix, " VECTOR: " +vector + "\nAlpha: " + stepSize + ", previousCost: " + previousCost + ", currentCost: " + currentCost);
        }

        MatrixNaNOrInfinityException(final SimpleMatrix matrix, String msg) {
            super("Matrix with NaN or Infinity values: " + matrix +"\n" + msg);
        }
    }

    private GradientDescend() {

    }

    public static GradientDescend getInstance() {
        return new GradientDescend();
    }

    protected static final class ThetaAndCostsGD extends ThetaAndCosts {

        private final double _alpha;
        private final int _iterations;


        public ThetaAndCostsGD(
                double alpha, int iterations,
                final SimpleMatrix theta, final double previousCost, final double currentCost, final int costIndex) {
            super(theta, previousCost, currentCost, costIndex);
            _alpha = alpha;
            _iterations = iterations;
        }

        @Override
        public Objects.ToStringHelper toStringHelper() {
            return super.toStringHelper().add("alpha", _alpha).add("iterations", _iterations);
        }

        public double getAlpha() {
            return _alpha;
        }
    }

    /**
     * Minimizes the function, choosing parameters automatically
     * @param costFunction the function to optimize
     * @param timeoutMillis the maximum time this function is allowed to run
     * @return the optimal theta for the function
     * @throws java.util.concurrent.TimeoutException if minimization incl. paramter detection takes too long
     */
    public ThetaAndCostsGD minimize(
            MinimizationInputFunction costFunction,
            long timeoutMillis
    ) throws TimeoutException {
        SimpleMatrix initialTheta = new SimpleMatrix(costFunction.getNumDimensions(), 1); // column vector with as many rows as the input has dimensions, all zeros
        return minimize(costFunction, timeoutMillis, initialTheta, 1.0);
    }

    public ThetaAndCostsGD minimize(
            MinimizationInputFunction costFunction,
            long timeoutMillis,
            SimpleMatrix initialTheta,
            double initialAlpha
    ) throws TimeoutException {

        double terminationThreshold = COST_TERMINATION_THRESHOLD;
        long maxEndTime = System.currentTimeMillis() + timeoutMillis;
        abortOnTimeout(maxEndTime);
        // first: detect the largest value for alpha that does not quickly overshoot
        AlphaDetectionResult alphaDetectionResult = detectBestAlpha(
                initialTheta, costFunction, initialAlpha,
                ALPHA_DETECTION_ITERATIONS /*overshooting probably occurs quickly*/,
                maxEndTime, terminationThreshold);
        if (alphaDetectionResult.isTerminated()) {
            // wow - that was quick :-)
            return alphaDetectionResult.getTerminationResult();
        }
        // FIXME - maybe we should try harder for a good alpha?
        double alpha = alphaDetectionResult.getAlpha();
        int maxIterations = 10000;
        while (true) {
            try {
                return minimize(costFunction, initialTheta, alpha, maxIterations, terminationThreshold, maxEndTime - System.currentTimeMillis());
            } catch (CostIncreasedException e) {
                // overshooting occurred later in the dataset than in the initial alpha detection test
                // decrease alpha and retry as long as we got time
                alpha /=3d;
                if (LOG.isDebugEnabled()) {LOG.debug("Overshooting occurred, decreasing alpha to " + alpha + ": " + e);}
            } catch (NotTerminatedException e) {
                alpha *=1.3;
                if (LOG.isDebugEnabled()) {LOG.debug("Not terminated, slightly increasing alpha to " + alpha + ": " + e);}
            }

        }
    }
    static class AlphaDetectionResult {
        private final ThetaAndCostsGD _terminationResult;
        private final Exception _exception;
        private final double _alpha;

        AlphaDetectionResult(ThetaAndCostsGD terminationResult, Exception exception, double alpha) {
            _terminationResult = terminationResult;
            _exception = exception;
            _alpha = alpha;
        }

        public boolean isTooLarge() {
            return _exception instanceof  CostIncreasedException;
        }

        public boolean isTooSmall() {
            return _exception instanceof  NotTerminatedException;
        }

        public boolean isTerminated() {
            return _exception == null && _terminationResult != null;
        }

        public double getAlpha() {
            return _alpha;
        }

        public ThetaAndCostsGD getTerminationResult() {
            return _terminationResult;
        }

        @Override
        public String toString() {
            Objects.ToStringHelper helper = Objects.toStringHelper(this)
                    .addValue(_alpha)
                    .addValue(isTooLarge() ? "TOO_LARGE" : (isTooSmall() ? "TOO_SMALL" : (isTerminated() ? "TERMINATED" : "UNKNOWN")));
            if (_terminationResult != null) {
                helper.addValue(_terminationResult);
            }
            if (_exception != null) {
                helper.addValue(_exception);
            }
            return helper.toString();
        }
    }

    private static AlphaDetectionResult testAlpha(
            SimpleMatrix theta, MinimizationInputFunction costFunction, double alphaCandidate, int iterations, long maxEndTime,
            double terminationThreshold
    ) throws TimeoutException {
        try {
            ThetaAndCostsGD r = minimize(costFunction, theta, alphaCandidate, iterations, terminationThreshold, maxEndTime - System.currentTimeMillis());
            return new AlphaDetectionResult(r, null, alphaCandidate);
        } catch (CostIncreasedException e) {
            return new AlphaDetectionResult(null, e, alphaCandidate);
        } catch (NotTerminatedException e) {
            return new AlphaDetectionResult(null, e, alphaCandidate);
        }
    }

    private static AlphaDetectionResult detectBestAlpha(
            SimpleMatrix theta, MinimizationInputFunction costFunction, double startValue, int numIterations, long maxEndTime, double terminationThreshold
    ) throws TimeoutException {
        // FIXME should return two last results and choose the second last, instead of going downwards again - currently we are doing way too much work
        AlphaDetectionResult[] detectionResults = increaseUntilOvershooting(theta, costFunction, startValue, numIterations, maxEndTime, terminationThreshold );
        AlphaDetectionResult detectionResult = detectionResults[0];
        if (detectionResult.isTerminated()) {
            return detectionResult;
        }
        AlphaDetectionResult prevResult = detectionResults[1];
        if (prevResult != null && !prevResult.isTooLarge()) {
            return prevResult;
        }
        AlphaDetectionResult alphaDetectionResult = decreaseUntilNotOvershooting(theta, costFunction, detectionResult.getAlpha(), numIterations, maxEndTime, terminationThreshold);
        if (LOG.isDebugEnabled()) {LOG.debug("detectBestAlpha(theta: " + Octave.toString(theta) + ", costFunction, startValue: " + startValue + ", numIterations:" + numIterations + ", maxEndTime: " + maxEndTime + ", terminationThreshold: " + terminationThreshold + ") => " + alphaDetectionResult);}
        return alphaDetectionResult;
    }

    /**
     *
     * @return [0] -> the current result; [1] -> the previous result
     * @throws TimeoutException
     */
    private static AlphaDetectionResult[] increaseUntilOvershooting(
            SimpleMatrix theta, MinimizationInputFunction costFunction,
            double startValue, int numIterations, long maxEndTime,
            double terminationThreshold
    ) throws TimeoutException {
        AlphaDetectionResult[] results = new AlphaDetectionResult[] {null, null};
        double alpha = startValue;
        while(true) {
            AlphaDetectionResult result = testAlpha(theta, costFunction, alpha, numIterations, maxEndTime, terminationThreshold);
            // rotate result and write current one.
            results[1] = results[0];
            results[0] = result;
            if (result.isTooLarge() || result.isTerminated()) {
                return results;
            }
            abortOnTimeout(maxEndTime);
            alpha *= 3d; // Use the rule-of-thumb by Andrew Ng
            if (alpha == Double.POSITIVE_INFINITY) {
                // certainly not a good step size
                return results;
            }
        }
    }

    private static AlphaDetectionResult decreaseUntilNotOvershooting(
            SimpleMatrix theta, MinimizationInputFunction costFunction,
            double startValue, int numIterations, long maxEndTime,
            double terminationThreshold
    ) throws TimeoutException {
        double alpha = startValue;
        while(true) {
            AlphaDetectionResult result = testAlpha(theta, costFunction, alpha, numIterations, maxEndTime, terminationThreshold);
            if (result.isTooSmall() || result.isTerminated()) {
                return result;
            }
            abortOnTimeout(maxEndTime);
            alpha /= 3d; // Use the rule-of-thumb by Andrew Ng
        }
    }

    private static void abortOnTimeout(final long maxEndTime) throws TimeoutException {
        if (maxEndTime > 0 && System.currentTimeMillis() > maxEndTime) {
            throw new TimeoutException();
        }

    }

    public static ThetaAndCosts minimize(
            Function<SimpleMatrix/*theta*/, CostAndGradient> costFunction,
            SimpleMatrix startingGuess,
            double stepSize,
            int numIterations,
            double terminationThreshold
    ) throws CostIncreasedException, NotTerminatedException{
        try {
            return minimize(costFunction, startingGuess, stepSize, numIterations, terminationThreshold, Long.MAX_VALUE);
        } catch (TimeoutException e) {
            throw new IllegalStateException("negative end time - should not have caused a timeout");
        }
    }

    /**
     * Minimize the starting guess vector using gradient descend algorithm.
     *
     * @param terminationThreshold If the difference in costs between two values goes below this value, terminate iteration.
     */
    public static ThetaAndCostsGD minimize(
            Function<SimpleMatrix/*theta*/, CostAndGradient> costFunction,
            SimpleMatrix startingGuess,
            double stepSize,
            int numIterations,
            double terminationThreshold,
            long maxDuration
    ) throws CostIncreasedException, NotTerminatedException, TimeoutException {
        long maxEndTime = maxDuration == Long.MAX_VALUE ? -1 : System.currentTimeMillis() + maxDuration;
        terminationThreshold = Math.abs(terminationThreshold);
        SimpleMatrix vector = Octave.checkColumnVector(startingGuess);
        if (Octave.hasNaNOrInfinity(startingGuess)) {
            throw new MatrixNaNOrInfinityException(vector, "Starting Guess is invalid");
        }

        if (stepSize == Double.NEGATIVE_INFINITY || stepSize == Double.POSITIVE_INFINITY || stepSize == Double.NaN) {
            throw new IllegalArgumentException("Illegal step size: " + stepSize);
        }

        //double[] costs = new double[numIterations];
        double previousCost = 0;
        double currentCost = 0;
        for (int i = 0; i < numIterations; i++) {
            abortOnTimeout(maxEndTime);
            CostAndGradient step = costFunction.apply(vector);
            if (LOG.isTraceEnabled()) {
                LOG.trace("minimize | Alpha: " + stepSize + " Step.Vector: " + Octave.toString(vector) + " Step.Gradient: " + Octave.toString(step.gradient) + " Step.Cost: " + step.cost + " i: " + i);
            }
            previousCost = currentCost;
            currentCost = step.cost;
            if (terminates(previousCost, currentCost, i, terminationThreshold)) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("minimize terminated at | Alpha: " + stepSize + " Step.Vector: " + Octave.toString(vector) + " Step.Gradient: " + Octave.toString(step.gradient) + " Step.Cost: " + step.cost + " i: " + i);
                }
                return new ThetaAndCostsGD(stepSize, i, vector, previousCost, currentCost, i);
            }
            if (i > 0 && previousCost < currentCost) {
                throw new CostIncreasedException(previousCost, currentCost, i);
            }

            if (Octave.hasNaNOrInfinity(step.gradient)) {
                throw new MatrixNaNOrInfinityException(vector, stepSize, previousCost, currentCost, step.gradient);
            }

            SimpleMatrix previous = vector;
            SimpleMatrix scaledGradient = step.gradient.scale(stepSize);
            if (Octave.hasNaNOrInfinity(scaledGradient)) {
                throw new MatrixNaNOrInfinityException(scaledGradient, "Gradient invalid after scaling! alpha: " + stepSize + ", initial gradient " + step.gradient);
            }
            vector = previous.minus(scaledGradient);
            if (Octave.hasNaNOrInfinity(vector)) {
                throw new MatrixNaNOrInfinityException(vector, "Vector invalid after subtracting scaled vector! previous: " + previous + ", scaled gradient " + scaledGradient);
            }
        }
        throw new NotTerminatedException(previousCost, currentCost, numIterations-1);
    }


}
