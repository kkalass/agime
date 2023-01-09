package de.kalass.android.common.util;

import com.google.common.base.Objects;

import java.util.Locale;

/**
 * Created by klas on 02.01.14.
 */
public class StringUtil {
    public static boolean isTrimmedNullOrEmpty(CharSequence s) {
        return s == null || s.toString().trim().length() == 0;
    }

    public static String canonicalize(String value) {
        if (StringUtil.isTrimmedNullOrEmpty(value)) {
            return "";
        }
        return value.trim().toLowerCase().replaceAll("\\s+", " ");
    }
    public static String formatOptional(String string, String param1) {
        if (!string.contains("%1$s")) {
            return string;
        }
        return String.format(Locale.getDefault(), string, param1);
    }

    public static String toString(Object o) {
        return o == null ? null : o.toString();
    }

    public static String trim(Object string) {
        return string == null ? "" : string.toString().trim();
    }

    public static boolean equal(CharSequence name, CharSequence name1) {
        return Objects.equal(trim(name), trim(name1));
    }
}
