package de.kalass.commons.ml;

import com.google.common.collect.ImmutableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static de.kalass.commons.ml.OctaveTestUtil.m;

/**
 * Created by klas on 03.04.14.
 */
public class LogisticRegressionMultiClassTest {
    private static final Logger LOG = LoggerFactory.getLogger(LogisticRegressionMultiClassTest.class);

    public static final long TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(60 * 2);

    @DataProvider
    public Object[][] stringClassificationData() {
        return new Object[][] {
                {new double[][] {
                        {1, 0},
                        {1, 0},
                        {1, -1000},
                        {1, 0},
                        {1, 1000},
                        {1, -1000},
                        {1, 1000},
                        {1, 1000},
                        {1, -1000},

                },new String[] {
                        "Wenig",
                        "Wenig",
                        "Sehr Wenig",
                        "Wenig",
                        "Sehr Viel",
                        "Sehr Wenig",
                        "Sehr Viel",
                        "Sehr Viel",
                        "Sehr Wenig",
                }},
                {new double[][] {
                        {1, -0.0001},
                        {1, -0.0001},
                        {1, -1},
                        {1, -0.0001},
                        {1, 1},
                        {1, -1},
                        {1, 1},
                        {1, 1},
                        {1, -1}
                }, new String[] {
                        "Wenig",
                        "Wenig",
                        "Sehr Wenig",
                        "Wenig",
                        "Sehr Viel",
                        "Sehr Wenig",
                        "Sehr Viel",
                        "Sehr Viel",
                        "Sehr Wenig",
                }}
        };
    }

    @Test(dataProvider = "stringClassificationData")
    public void testSimple(final double[][] trainingXs, final String[] trainingY) throws TimeoutException {
        long startTime = System.currentTimeMillis();
        MultiClassPrediction<String> prediction = MultiClassPrediction.train(m(trainingXs), trainingY, TIMEOUT_MILLIS);
        if (LOG.isInfoEnabled()) {
            LOG.info("Trained Multiclass in " + (System.currentTimeMillis() - startTime) + " ms");
        }
        String[] predictionResults = new String[trainingY.length];
        for (int i = 0; i < trainingXs.length; i++) {
            MultiClassPredictionResult<String> result = prediction.apply(trainingXs[i]);
            predictionResults[i] = result.getPredictedValue();
        }
        Assert.assertEquals(
                ImmutableList.copyOf(trainingY), ImmutableList.<String>of().copyOf(predictionResults)
        );
    }



}
