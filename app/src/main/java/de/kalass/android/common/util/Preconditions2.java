package de.kalass.android.common.util;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Created by klas on 07.03.14.
 */
public final class Preconditions2 {

    private Preconditions2() {}

    public static <T> void assertSetEqual(Set<? extends T> expected, Set<? extends T> actual) {
        assertSetEqual(expected, actual, "Sets are not equal. ");
    }

    public static <T> void assertSetEqual(Set<? extends T> expected, Set<? extends T> actual, String msg) {
        String inequal = getInequalSetMessage(expected, actual);
        if (inequal != null) {
            throw new IllegalStateException(msg + inequal);
        }
    }

    public static <T> String getInequalSetMessage(Set<? extends T> expected, Set<? extends T> actual) {
        if (expected.equals(actual)) {
            return null;
        }
        HashSet<T> missing = new HashSet<T>(expected);
        missing.removeAll(actual);
        HashSet<T> superfluous = new HashSet<T>(actual);
        superfluous.removeAll(expected);
        return "Missing: " + missing + ", Superfluous: " + superfluous;
    }
}
