package de.kalass.android.common.util;

import android.content.Context;
import android.text.format.DateUtils;
import android.text.format.Time;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import de.kalass.agime.R;

/**
 * Created by klas on 06.11.13.
 */
public class TimeFormatUtil {
    public static boolean isDifferentDay(Time time1, Time time2) {
        return time1.year != time2.year || time1.month != time2.month || time1.monthDay != time2.monthDay;
    }

    public static CharSequence getDateSpan(long now, long starttime) {
        return DateUtils.getRelativeTimeSpanString(starttime, now, DateUtils.DAY_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE);
    }

    public static CharSequence getTimeSpan(Context context, long now, long starttime, boolean forcePrecise) {
        boolean today = getDateSpan(now, starttime).equals(getDateSpan(now, now));
        if (forcePrecise || !today) {
            return DateUtils.formatDateTime(context, starttime, DateUtils.FORMAT_SHOW_TIME);
        }
        long millisElapsed = starttime-now;
        long minutesElapsed = millisElapsed / 60000;
        if (minutesElapsed == 0) {
            return context.getResources().getString(R.string.track_activity_time_now);
        }
        return DateUtils.getRelativeTimeSpanString(starttime, now, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE);
    }

    public static CharSequence formatTimeForHeader(Context context, LocalDate day) {
        long millis = day.toDateTimeAtCurrentTime().getMillis();
        LocalDate now = LocalDate.now();

        if (now.getYear() == day.getYear()) {
            return DateUtils.formatDateTime(
                    context,
                    millis,
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_RELATIVE );
        }
        // the normal format is too long to include year - choose a shorter display
        return DateUtils.formatDateTime(
                context,
                millis,
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_ALL );
    }

    public static CharSequence formatWeekForHeader(Context context, LocalDate day) {
        LocalDate startDay = DateUtil.getFirstDayOfWeek(day);
        LocalDate endDay = DateUtil.getLastDayOfWeek(day);
        return DateUtils.formatDateRange(
                context,
                DateUtil.getMillisAtStartOfDay(startDay),
                DateUtil.getMillisAtEndOfDay(endDay),
                DateUtils.FORMAT_SHOW_DATE /*| DateUtils.FORMAT_SHOW_WEEKDAY */ | DateUtils.FORMAT_ABBREV_ALL
        );
    }

    public static CharSequence formatMonthForHeader(Context context, LocalDate day) {
        if (LocalDate.now().getYear() == day.getYear() ) {
            return new SimpleDateFormat("MMMM").format(day.toDate());
        } else {
            return new SimpleDateFormat("MMMM yyyy").format(day.toDate());
        }
    }
    public static CharSequence formatYearForHeader(Context context, LocalDate day) {
        return new SimpleDateFormat("yyyy").format(day.toDate());
    }

    public static CharSequence formatDuration(Context context, long millis) {
        long elapsedSeconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        long elapsedMinutesTotal = elapsedSeconds / 60;
        long elapsedMinutes = elapsedMinutesTotal % 60;
        long elapsedHours = elapsedMinutesTotal / 60;

        if (elapsedHours > 0) {
            return format(context, R.string.time_format_duration_h_m, elapsedHours, elapsedMinutes);
        }
        return format(context, R.string.time_format_duration_m, elapsedMinutes);
    }

    private static String format(Context context, int resId, Object... params) {
        //return String.format(context.getString(resId), params);
        return context.getString(resId, params);
    }

    public static CharSequence formatDurationSeconds(Context context, long millis) {
        long elapsedSeconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        long elapsedMinutesTotal = elapsedSeconds / 60;
        elapsedSeconds = elapsedSeconds % 60;
        long elapsedMinutes = elapsedMinutesTotal % 60;
        long elapsedHours = elapsedMinutesTotal / 60;

        if (elapsedHours > 0) {
            return format(context, R.string.time_format_duration_h_m, elapsedHours, elapsedMinutes);
        }
        if (elapsedMinutes > 0) {
            return format(context, R.string.time_format_duration_m_s, elapsedMinutes, elapsedSeconds);
        }
        return format(context, R.string.time_format_duration_s, elapsedSeconds);
    }
    public static CharSequence formatDateTime(Context context, long millis) {
        return DateUtils.formatDateTime(context, millis, DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE);
    }
    public static CharSequence formatTime(Context context, long millis) {
        return DateUtils.formatDateTime(context, millis, DateUtils.FORMAT_SHOW_TIME);
    }

    public static CharSequence formatDate(Context context, LocalDate date) {
        return formatDate(context, date.toDateTimeAtStartOfDay().getMillis());
    }

    public static CharSequence formatDate(Context context, long millis) {
        long now = System.currentTimeMillis();
        Time timeNow = new Time();
        timeNow.set(now);
        Time time = new Time();
        time.set(millis);
        CharSequence result = DateUtils.formatDateTime(context, millis, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY);
        /*if (!isDifferentDay(time, timeNow)) {
            return getDateSpan(now, millis) + " - " + result;
        }*/
        return result;
    }

    public static CharSequence formatDateAbbrev(Context context, LocalDate date) {
        if (date.isEqual(LocalDate.now())) {
            return context.getString(R.string.today);
        }
        return formatDateAbbrev(context, date.toDateTimeAtStartOfDay().getMillis());
    }

    public static CharSequence formatDateAbbrev(Context context, long millis) {
        long now = System.currentTimeMillis();
        Time timeNow = new Time();
        timeNow.set(now);
        Time time = new Time();
        time.set(millis);


        CharSequence result = DateUtils.formatDateTime(context, millis, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_ALL);
        /*if (!isDifferentDay(time, timeNow)) {
            return getDateSpan(now, millis) + " - " + result;
        }*/
        return result;
    }

    public static String getInNumDays(Context context, LocalDate date) {
        int numDays = Days.daysBetween(new LocalDate(), date).getDays();
        return getInNumDays(context, numDays);
    }

    public static String getInNumDays(Context context, int numDays) {
        if (numDays == 0) {
            // unfortunately, "0" is not a supported quantity for english plurals
            return context.getString(R.string.time_format_in_zero_days);
        }
        return context.getResources().getQuantityString(R.plurals.time_format_in_num_days, numDays, numDays);
    }
}
