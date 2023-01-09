package de.kalass.commons.ml;


import org.ejml.simple.SimpleMatrix;

import java.util.EnumSet;
import java.util.Set;

/**
 * Created by klas on 02.04.14.
 */
public class Octave {
    public static SimpleMatrix checkColumnVector(SimpleMatrix matrix) {
        if (matrix.numCols() != 1) {
            throw new IllegalArgumentException("input matrix is not a vector: " + matrix.numCols() +  " dimensions ");
        }
        return matrix;
    }
    interface DFunction {
        double apply(int row, int column, double value);
    }

    public static SimpleMatrix sigmoid(SimpleMatrix matrix) {
        return transform(matrix, new DFunction() {
            @Override
            public double apply(int row, int column, final double value) {
                return sigmoid(value);
            }
        });
    }

    public static double sigmoid(final double value) {
        return 1.0d / (double)(1 + Math.exp(-value));
    }

    public static SimpleMatrix log(SimpleMatrix matrix) {
        return transform(matrix, new DFunction() {
            @Override
            public double apply(int row, int column, final double value) {
                return Math.log(value);
            }
        });
    }

    public enum Flag {
        ZERO_MULTIPLICATION_FIX
    }

    public static SimpleMatrix scalarMultiply(SimpleMatrix left, final SimpleMatrix right) {
        return scalarMultiply(left, right, EnumSet.allOf(Flag.class));
    }

    public static SimpleMatrix scalarMultiply(SimpleMatrix left, final SimpleMatrix right, final Set<Flag> flags) {
        assertSameColumnDimension(left, right);
        assertSameRowDimension(left, right);
        return new SimpleMatrixChangingVisitor() {
            @Override
            public double visit(final int row, final int column, final double value) {
                double rightEntry = right.get(row, column);
                // 0*NaN is not zero for java - but we really want
                if (!flags.contains(Flag.ZERO_MULTIPLICATION_FIX)) {
                    return value * rightEntry;
                }
                return value == 0.0 || rightEntry == 0.0 ? 0.0 :  value * rightEntry;
            }

        }.performOnCopy(left);
    }

    private static final class NaNOrInfinityVisitor extends SimpleMatrixPreservingVisitor  {
        boolean retval = false;

        @Override
        public void visit(final int row, final int column, final double value) {
            retval |= (Double.isInfinite(value) || Double.isNaN(value));
        }
    }
    public static boolean hasNaNOrInfinity(final SimpleMatrix gradient) {
        NaNOrInfinityVisitor v = new NaNOrInfinityVisitor();
        v.perform(gradient);
        return v.retval;
    }


    public static SimpleMatrix ones(int rows, int cols) {
        double[][] r = new double[rows][cols];
        for (int ri = 0; ri < rows; ri++) {
            for (int ci = 0; ci < cols; ci++) {
                r[ri][ci] = 1;
            }
        }
        return new SimpleMatrix(r);
    }

    public static SimpleMatrix scalarSubtract(SimpleMatrix left, final SimpleMatrix right) {
        return left.minus(right);
    }

    private static void assertSameRowDimension(final SimpleMatrix left, final SimpleMatrix right) {
        if (left.numRows() != right.numRows()) {
            throw new IllegalArgumentException("left " + left.numRows() + " rows != right " + right.numRows() + " rows");
        }
    }

    private static void assertSameColumnDimension(final SimpleMatrix left, final SimpleMatrix right) {
        if (left.numCols() != right.numCols()) {
            throw new IllegalArgumentException("left " + left.numCols() + " columns != right " + right.numCols() + " columns");
        }
    }


    /**
     * Computes the mean or average of all the elements.
     *
     * @return mean
     */
    public static double meanOfColumn(SimpleMatrix matrix, int ci) {
        double total = 0;

        final int N = matrix.numRows();
        for( int ri = 0; ri < N; ri++ ) {
            // FIXME overflow?
            total += matrix.get(ri, ci);
        }

        return total/N;
    }

    /**
     * Computes the unbiased standard deviation of all the elements.
     *
     * @return standard deviation
     */
    public static double stdevOfColumn(SimpleMatrix matrix, int ci, double mean) {


        double total = 0;

        final int N = matrix.numRows();
        if( N <= 1 )
            throw new IllegalArgumentException("There must be more than one element to compute stdev");


        for( int ri = 0; ri < N; ri++ ) {
            double x = matrix.get(ri, ci);
            // FIXME overflow?
            total += (x - mean)*(x - mean);
        }

        total /= (N-1);

        return Math.sqrt(total);
    }
    /**
     * @return a function that works on matrixes and scales the columns
     */
    public static DFunction createScaleFunction(final double[] means, final double[] stdDev) {

        return new DFunction() {
            @Override
            public double apply(int row, int column, final double value) {
                double mean = means[column];
                double stdev = stdDev[column];
                // avoid division by zero
                if (stdev == 0.0) {
                    return value - mean;
                }
                return (value - mean)/stdev;
            }
        };
    }


    public static SimpleMatrix createRowMatrix(final double[] input) {
        SimpleMatrix imatrix = new SimpleMatrix(1, input.length);
        for (int i = 0; i < input.length; i++) {
            imatrix.set(0, i, input[i]);
        }
        return imatrix;
    }

    public static SimpleMatrix createColMatrix(final double[] input) {
        SimpleMatrix imatrix = new SimpleMatrix(input.length, 1);
        for (int i = 0; i < input.length; i++) {
            imatrix.set(i, 0, input[i]);
        }
        return imatrix;
    }
    public static double[] sum(SimpleMatrix matrix) {
        if (matrix.numRows() == 1) {
            return sumRows(matrix);
        }
        return sumCols(matrix);
    }

    /**
     For each row, calculate the sum
     */
    public static double[] sumRows(SimpleMatrix matrix) {
        final double[] result = new double[matrix.numRows()];
        new SimpleMatrixPreservingVisitor() {
            @Override
            public void visit(final int row, final int column, final double value) {
                if (column == 0) {
                    result[row] = value;
                } else {
                    result[row] += value;
                }
            }

        }.perform(matrix);
        return result;
    }

    public static double[] sumCols(SimpleMatrix matrix) {
        final double[] result = new double[matrix.numCols()];

        new SimpleMatrixPreservingVisitor() {

            @Override
            public void visit(final int row, final int column, final double value) {
                if (row == 0) {
                    result[column] = value;
                } else {
                    result[column] += value;
                }
            }

        }.perform(matrix);
        return result;
    }

    public static SimpleMatrix transform(SimpleMatrix matrix, final DFunction func){
        //1/(1+e^-z)

        return new SimpleMatrixChangingVisitor() {
            @Override
            public double visit(final int row, final int column, final double value) {
                return func.apply(row, column, value);
            }

        }.performOnCopy(matrix);

    }

    private static abstract class SimpleMatrixChangingVisitor {
        abstract double visit(final int row, final int column, final double value);

        public SimpleMatrix performOnCopy(SimpleMatrix m) {
            return perform(m.copy());
        }

        public SimpleMatrix perform(SimpleMatrix m) {
            for (int ri = 0; ri< m.numRows(); ri++) {
                for (int ci = 0; ci < m.numCols(); ci++) {
                    double value = m.get(ri, ci);
                    m.set(ri, ci, visit(ri, ci, value));
                }
            }
            return m;
        }
    }

    private static abstract class SimpleMatrixPreservingVisitor {
        abstract void visit(final int row, final int column, final double value);

        public void perform(SimpleMatrix m) {
            for (int ri = 0; ri< m.numRows(); ri++) {
                for (int ci = 0; ci < m.numCols(); ci++) {
                    double value = m.get(ri, ci);
                    visit(ri, ci, value);
                }
            }

        }
    }


    public static SimpleMatrix createSimpleMatrix(int numRows, int numCols) {
        return new SimpleMatrix(numRows, numCols);
    }

    public static SimpleMatrix createSimpleMatrix(final double[][] input) {
        return new SimpleMatrix(input);
    }

    public static String toString(SimpleMatrix vector) {
        if (vector.numCols() == 1) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i< vector.numRows(); i++) {
                sb.append(vector.get(i, 0));
                if (i +1 < vector.numRows()) {
                    sb.append(",");
                }
            }
            return sb.append("]").toString();
        }
        return vector.toString();
    }

}
