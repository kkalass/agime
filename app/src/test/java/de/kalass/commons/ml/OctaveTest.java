package de.kalass.commons.ml;

import org.ejml.simple.SimpleMatrix;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;

import static java.lang.Math.log;
import static de.kalass.commons.ml.Octave.*;
import static de.kalass.commons.ml.OctaveTestUtil.*;
import static org.testng.Assert.assertEquals;

/**
 * Created by klas on 03.04.14.
 */
public class OctaveTest {

    @DataProvider
    public Object[][] columnVectors() {
        return new Object[][] {
                {createSimpleMatrix(123, 1)},
                {createSimpleMatrix(1, 1)},
                {createSimpleMatrix(2, 1)}
        };
    }

    @DataProvider
    public Object[][] noColumnVectors() {
        return new Object[][] {
                {createSimpleMatrix(123, 12)},
                {createSimpleMatrix(1, 2)}
        };
    }

    @Test(dataProvider = "columnVectors")
    public void testCheckIsColumnVector(SimpleMatrix matrix) {
        Octave.checkColumnVector(matrix);
    }


    @Test(dataProvider = "noColumnVectors", expectedExceptions = IllegalArgumentException.class)
    public void testCheckIsNoColumnVector(SimpleMatrix matrix) {
        Octave.checkColumnVector(matrix);
    }


    @DataProvider
    public Object[][] singleSigmoidData() {
        return new Object[][] {
                {0, 0.5},
                {9999, 1},
                {-9999, 0}
        };
    }

    @DataProvider
    public Object[][] sigmoidData() {
        return new Object[][] {
                {new double[][]{{0}, {234234}, {0}, {-32422}}, new double[][]{{0.5}, {1}, {0.5}, {0}}},
                {new double[][]{{0, 234234}, {0, -32422}}, new double[][]{{0.5, 1}, {0.5, 0}}}
        };
    }

    @Test(dataProvider = "singleSigmoidData")
    public void testSingleSigmoid(double in, double expectedResult) throws Exception {
        testSigmoid(to2Dim(in), to2Dim(expectedResult));
    }

    @Test(dataProvider = "sigmoidData")
    public void testSigmoid(double[][] in, double[][] expectedResults) throws Exception {
        SimpleMatrix inputMatrix = createSimpleMatrix(in);
        SimpleMatrix sigmoidMatrix = Octave.sigmoid(inputMatrix);
        assertEqualDimensions(inputMatrix, sigmoidMatrix);

        assertEqualsRoughly(sigmoidMatrix, expectedResults);
    }


    @DataProvider
    public Object[][] logData() {
        return new Object[][] {
                {to2Dim(23), to2Dim(log(23))},
                {to2Dim(0), to2Dim(log(0))},
                {to2Dim(-23), to2Dim(log(-23))},
                {new double[][] {{-23}, {23}, {0}, {2234}},
                        new double[][] {{log(-23)}, {log(23)}, {log(0)}, {log(2234)}}}
        };
    }

    @Test(dataProvider = "logData")
    private void testLog(double[][] in, double[][] expectedResults) throws Exception {
        SimpleMatrix matrix = createSimpleMatrix(in);
        SimpleMatrix log = Octave.log(matrix);
        assertEqualDimensions(matrix, log);
        assertEqualsRoughly(log, expectedResults);
    }

    @DataProvider
    public Object[][] scalarMultiplyDimensionMismatchData() {
        return new Object[][] {
                {new double[][]{{1, 2}}, new double[][]{{1}, {2}}}
        };
    }

    @DataProvider
    public Object[][] scalarMultiplyData() {
        return new Object[][] {
                {to2Dim(1), to2Dim(1), to2Dim(1)},
                {to2Dim(1), to2Dim(1), to2Dim(1)},
                {to2Dim(0), to2Dim(1), to2Dim(0)},
                {to2Dim(1), to2Dim(0), to2Dim(0)},
                {new double[][]{{1, 2}, {3, 4}}, new double[][]{{5, 6}, {7, 8}},new double[][]{{5, 2*6}, {3*7, 4*8}}},
        };
    }

    @DataProvider
    public Object[][] scalarSubtractData() {
        return new Object[][] {
                {to2Dim(0), to2Dim(0), to2Dim(0)},
                {to2Dim(0), to2Dim(2), to2Dim(-2)},
                {to2Dim(2), to2Dim(0), to2Dim(2)},
                {new double[][]{{1}, {2}}, new double[][]{{1}, {2}},new double[][]{{0}, {0}}},
                {new double[][]{{1, 10}, {2, -20}}, new double[][]{{2, -20}, {2, -20}},new double[][]{{-1, 30}, {0, 0}}}
        };
    }

    @DataProvider
    public Object[][] scalarSubtractDimensionMismatchData() {
        return new Object[][] {
                {new double[][]{{1, 2}}, new double[][]{{1}, {2}}}
        };
    }

    @Test(dataProvider = "scalarMultiplyData")
    public void testScalarMultiply(double[][] left, double[][] right, double[][] expectedResult) throws Exception {
        SimpleMatrix result = Octave.scalarMultiply(createSimpleMatrix(left), createSimpleMatrix(right));
        assertEqualsRoughly(result, expectedResult);
    }

    @Test(dataProvider = "scalarSubtractData")
    public void testScalarSubtract(double[][] left, double[][] right, double[][] expectedResult) throws Exception {
        SimpleMatrix result = Octave.scalarSubtract(createSimpleMatrix(left), createSimpleMatrix(right));
        assertEqualsRoughly(result, expectedResult);
    }

    @Test(dataProvider = "scalarMultiplyDimensionMismatchData", expectedExceptions = IllegalArgumentException.class)
    public void testScalarMultiplyDMismatch(double[][] left, double[][] right) throws Exception {
        Octave.scalarMultiply(createSimpleMatrix(left), createSimpleMatrix(right));
    }

    @Test(dataProvider = "scalarSubtractDimensionMismatchData", expectedExceptions = IllegalArgumentException.class)
    public void testScalarSubtractDMismatch(double[][] left, double[][] right) throws Exception {
        Octave.scalarSubtract(createSimpleMatrix(left), createSimpleMatrix(right));
    }


    @DataProvider
    public Object[][] sumTestData() {
        return new Object[][] {
                {new double[][]{{0.0, 1.0, 2.0}}, new double[]{3}},
                {new double[][] {{0.1}}, new double[] {0.1}},
                {new double[][] {{0.1}, {2}, {3}}, new double[] {5.1}}
        };
    }

    @Test(dataProvider = "sumTestData")
    public void testSum(double[][] input, double[] expectedSums) {
        double[] sum = Octave.sum(createSimpleMatrix(input));
        assertArrayEquals(expectedSums, sum);
    }


    @DataProvider
    public Object[][] sumRowsTestData() throws Exception {
        return new Object[][] {
                {new double[][] {{1, 2, 3}}, new double[] {6}},
                {new double[][] {{1, 2, 3}, {3, 4, 5}}, new double[] {6, 12}},
                {new double[][] {{1}, {3}}, new double[] {1, 3}}
        };
    }

    @Test(dataProvider = "sumRowsTestData")
    public void testSumRows(double[][] input, double[] expectedSums) {
        double[] sum = Octave.sumRows(createSimpleMatrix(input));
        Assert.assertTrue(Arrays.equals(sum, expectedSums));
    }

    @DataProvider
    public Object[][]  sumColsTestData() throws Exception {
        return new Object[][]{
            {new double[][]{{1, 2, 3}}, new double[]{1, 2, 3}},
            {new double[][]{{1, 2, 3}, {0, 2, 3}}, new double[]{1, 4, 6}},
            {new double[][]{{1, 2, 3}, {0, 0.2, 0.3}, {3, 1.8, 0.7}}, new double[]{4, 4, 4}},
            {new double[][]{{1}, {0}, {3}}, new double[]{4}}
        };
    }

    @Test(dataProvider = "sumColsTestData")
    public void testSumCols(double[][] input, double[] expectedSums) {
        double[] sum = Octave.sumCols(createSimpleMatrix(input));
        assertArrayEquals(expectedSums, sum);
    }

    @DataProvider
    public Object[][] onesTestData() {
        return new Object[][] {
                {1, 1},
                {10, 13},
                {1, 13},
                {14, 1}
        };
    }

    @Test(dataProvider = "onesTestData")
    public void testOnes(int numRows, int numCols) {
        SimpleMatrix matrix = Octave.ones(numRows, numCols);
        assertEquals(numRows, matrix.numRows());
        assertEquals(numCols, matrix.numCols());
        for (int ri = 0; ri < numRows; ri++) {
            for (int ci = 0; ci < numCols; ci++) {
                assertEquals(1d, matrix.get(ri, ci));
            }
        }
    }

}
