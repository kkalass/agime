package de.kalass.commons.ml;

/**
 * Created by klas on 03.04.14.
 */
public class NumberUtil {
    public static final double DEFAULT_EQUALS_ROUGHLY_THRESHOLD = 0.000001;

    public static boolean equalsRoughly(double d1, double d2) {
        return equalsRoughly(d1, d2, DEFAULT_EQUALS_ROUGHLY_THRESHOLD);
    }

    public static boolean equalsRoughly(double d1, double d2, double threshold) {
        return Double.compare(d1, d2) == 0 || Math.abs(d1 - d2) < threshold;
    }
}
