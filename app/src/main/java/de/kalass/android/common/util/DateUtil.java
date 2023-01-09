package de.kalass.android.common.util;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

/**
 * Created by klas on 21.11.13.
 */
public final class DateUtil {

    private DateUtil() {

    }

    public static long getMillisAtEndOfDay(LocalDate today) {
        return today.plusDays(1).toDateTimeAtStartOfDay().minusMillis(1).getMillis();
    }

    public static long getMillisAtStartOfDay(LocalDate today) {
        return today.toDateTimeAtStartOfDay().getMillis();
    }


    public static LocalDate getFirstDayOfWeek(LocalDate day) {
        return day.withDayOfWeek(1);
    }

    public static LocalDate getLastDayOfWeek(LocalDate day) {
        return day.withDayOfWeek(7);
    }

    public static LocalDate getFirstDayOfMonth(LocalDate day) {
        return day.withDayOfMonth(1);
    }

    public static LocalDate getFirstDayOfYear(LocalDate day) {
        return day.withDayOfMonth(1).withDayOfYear(1);
    }

    public static LocalDate getLastDayOfMonth(LocalDate day) {
        return getFirstDayOfMonth(day).plusMonths(1).minusDays(1);
    }
    public static LocalDate getLastDayOfYear(LocalDate day) {
        return getFirstDayOfYear(day).plusYears(1).minusDays(1);
    }

    public static LocalTime getNowMinutePrecision() {
        return new LocalTime().withMillisOfSecond(0).withSecondOfMinute(0);
    }

    public static LocalTime trimMinutePrecision(LocalTime time) {
        return time.withMillisOfSecond(0).withSecondOfMinute(0);
    }

    public static DateTime trimMinutePrecision(DateTime time) {
        return time.withMillisOfSecond(0).withSecondOfMinute(0);
    }

    public static long getNowMinutePrecisionMillis() {
        return new DateTime().withMillisOfSecond(0).withSecondOfMinute(0).getMillis();
    }
}
