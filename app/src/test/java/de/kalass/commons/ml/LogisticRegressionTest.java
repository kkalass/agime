package de.kalass.commons.ml;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import org.ejml.simple.SimpleMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

import static de.kalass.commons.ml.GradientDescend.minimize;
import static de.kalass.commons.ml.OctaveTestUtil.assertEqualsRoughly;
import static de.kalass.commons.ml.OctaveTestUtil.colVector;
import static de.kalass.commons.ml.OctaveTestUtil.m;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Created by klas on 03.04.14.
 */
public class LogisticRegressionTest {
    private static final Logger LOG = LoggerFactory.getLogger(LogisticRegressionTest.class);

    public final static String LOG_TAG = "TestCase";
    public static final long TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(60 * 2);

    private ThetaAndCosts learn(final SimpleMatrix xValues, final SimpleMatrix y,
                                final float stepSize, final int numIterations
    )
            throws GradientDescend.NotTerminatedException, GradientDescend.CostIncreasedException
    {
        return learn(xValues, y, stepSize, numIterations, NumberUtil.DEFAULT_EQUALS_ROUGHLY_THRESHOLD);
    }

    private ThetaAndCosts learn(final SimpleMatrix xValues, final SimpleMatrix y,
                                final float stepSize, final int numIterations,
                                final double terminationThreshold
    )
            throws GradientDescend.NotTerminatedException, GradientDescend.CostIncreasedException
    {
        if (LOG.isDebugEnabled()) {
            LOG.debug("learn | stepSize: " + stepSize + ", numIterations: " + numIterations);
        }
        Function<SimpleMatrix, CostAndGradient> costAndGradientFunction = LogisticRegression.costAndGradientFunction(xValues, y);
        SimpleMatrix start = Octave.ones(xValues.numCols(), 1); // column vector with as many rows as the input has dimensions

        return minimize(costAndGradientFunction, start, stepSize, numIterations, terminationThreshold);
    }

    private static double[] createSimplestY(int numRows) {
        double[] y = new double[numRows];
        for (int i = 0; i < y.length; i++) {
            y[i] = i%2;
        }
        return y;
    }

    private static double[][] createSimplestX(double[] y) {
        double[][] xs = new double[y.length][2];
        for (int i = 0; i < y.length; i++) {
            xs[i][0] = 1;
            xs[i][1] = y[i];
        }
        return xs;
    }

    @Test(expectedExceptions = GradientDescend.CostIncreasedException.class)
    public void testSimplestAlphaTooLarge() throws Exception {
        double[] y = createSimplestY(6);
        double[][] xs = createSimplestX(y);
        learn(m(xs), colVector(y), 1000, 400);
    }

    @Test(expectedExceptions = GradientDescend.NotTerminatedException.class)
    public void testSimplestAlphaNotTerminated() throws Exception{
        double[] y = createSimplestY(6);
        double[][] xs = createSimplestX(y);
        learn(m(xs), colVector(y), 1, 400);
    }

    @Test()
    public void testSimplestAutoOptimizeParameters() throws Exception{

        double[] y = createSimplestY(50);
        double[][] xs = createSimplestX(y);

        long learningStartMillis = System.currentTimeMillis();
        final ThetaAndCosts learnedParameters = GradientDescend.getInstance().minimize(
                LogisticRegression.costAndGradientFunction(m(xs), colVector(y)),
                TIMEOUT_MILLIS
        );
        logLearnedValues("testSimplestAutoOptimizeParameters", System.currentTimeMillis() -learningStartMillis, learnedParameters);
        testSimplestLearnedParameters(learnedParameters);
    }

    private static final double[] xRow(double x1) {
        return new double[] {1, x1};
    }

    @Test
    public void testSimplestWithFixedParams() throws GradientDescend.CostIncreasedException, GradientDescend.NotTerminatedException {
        // Generate a really simple dataset that clearly predicts the output value with one input value

        double[] y = createSimplestY(50);
        double[][] xs = createSimplestX(y);
        long learningStartMillis = System.currentTimeMillis();
        final ThetaAndCosts learnedParameters = learn(m(xs), colVector(y), 10, 10000);
        logLearnedValues("testSimplestWithFixedParams(alpha:10, iterations: 10000)", System.currentTimeMillis() - learningStartMillis, learnedParameters);
        testSimplestLearnedParameters(learnedParameters);
    }


    private void testSimplestLearnedParameters(final ThetaAndCosts learnedParameters) {

        Function<double[], Double> prediction = new LogisticRegression.CalculateProbability(learnedParameters.theta, "y=1");
        Predicate<double[]> classification = new LogisticRegression.Classify(learnedParameters, "y=1");

        assertEqualsRoughly(1d, prediction.apply(xRow(1)), 0.001d);
        assertEqualsRoughly(0d, prediction.apply(xRow(0)), 0.001d);

        assertTrue(classification.apply(xRow(1.0d)));
        assertTrue(classification.apply(xRow(0.9d)));
        assertTrue(classification.apply(xRow(0.8d)));
        assertTrue(classification.apply(xRow(0.7d)));
        assertTrue(classification.apply(xRow(0.6d)));
        assertTrue(classification.apply(xRow(0.5d)));
        assertFalse(classification.apply(xRow(0.45d)));
        assertFalse(classification.apply(xRow(0.4d)));
        assertFalse(classification.apply(xRow(0.3d)));
        assertFalse(classification.apply(xRow(0.2d)));
        assertFalse(classification.apply(xRow(0.1d)));
        assertFalse(classification.apply(xRow(0.0d)));
    }

    private static void logLearnedValues(String preMessage, final long duration, final ThetaAndCosts learnedParameters) {
        if (LOG.isInfoEnabled()) {
            LOG.info(preMessage + " | Learned Values in " + duration + "ms  | " + learnedParameters.theta + " | index: " + learnedParameters.costIndex + ", previous cost: " + learnedParameters.previousCost + ", current cost: " + learnedParameters.currentCost + "");
        }
    }


}
