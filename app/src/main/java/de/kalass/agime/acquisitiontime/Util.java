package de.kalass.agime.acquisitiontime;

import android.content.Context;

import org.joda.time.LocalDate;

import de.kalass.android.common.util.TimeFormatUtil;

/**
 * Created by klas on 21.12.13.
 */
public class Util {

    public static boolean isCurrentlyActive(LocalDate inactiveUntil) {
        return isCurrentlyActive(inactiveUntil, new LocalDate());
    }

    public static boolean isCurrentlyActive(LocalDate inactiveUntil, LocalDate today) {
        return inactiveUntil == null || !inactiveUntil.isAfter(today);
    }

    public static CharSequence formatLocalDate(Context context, LocalDate inactiveUntil) {
        return inactiveUntil == null ? "" : TimeFormatUtil.formatDateAbbrev(context, inactiveUntil);
    }
}
