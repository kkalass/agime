package de.kalass.android.common.util;

/**
 * Created by klas on 31.01.14.
 */
public final class LongUtil {
    private LongUtil() {

    }

    public static long nullToZero(Long l) {
        if (l == null) {
            return 0;
        }
        return l;
    }
}
