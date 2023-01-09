package de.kalass.commons.ml;

import org.ejml.simple.SimpleMatrix;
import org.testng.Assert;

import java.util.Arrays;

/**
 * Created by klas on 03.04.14.
 */

public class OctaveTestUtil {

    public static void assertArrayEquals(final double[] expectedSums, final double[] sum) {
        Assert.assertEquals(sum, expectedSums, "Not equal: " + Arrays.toString(sum) + " != " + Arrays.toString(expectedSums));
    }

    public static void assertEqualDimensions(final SimpleMatrix inputMatrix, final SimpleMatrix sigmoidMatrix) {
        Assert.assertEquals(sigmoidMatrix.numCols(), inputMatrix.numCols());
        Assert.assertEquals(sigmoidMatrix.numRows(), inputMatrix.numRows());
    }

    public static void assertEqualsRoughly(final SimpleMatrix matrix, final double[][] expectedResults) {
        for (int ri = 0; ri < expectedResults.length; ri++) {
            double[] row = expectedResults[ri];
            for (int ci = 0; ci < row.length; ci++) {
                double expectedResult = expectedResults[ri][ci];
                double value = matrix.get(ri, ci);
                assertEqualsRoughly(expectedResult, value);
            }
        }
    }

    public static void assertEqualsRoughly(final double expectedResult, final double value) {
        assertEqualsRoughly(expectedResult, value, NumberUtil.DEFAULT_EQUALS_ROUGHLY_THRESHOLD);
    }

    public static void assertEqualsRoughly(final double expectedResult, final double value, final double threshold) {
        Assert.assertTrue(NumberUtil.equalsRoughly(expectedResult, value, threshold), "not roughly equal: " + expectedResult + " != " + value /*Threshold*/);
    }

    public static double[][] to2Dim(final double in) {
        return new double[][]{{in}};
    }


    public static SimpleMatrix m(double[][] m) {
        return new SimpleMatrix(m);
    }

    public static SimpleMatrix colVector(double[] data) {
        return Octave.createColMatrix(data);
    }

}
