package de.kalass.android.common.simpleloader;

import android.content.Context;

import com.google.common.base.Joiner;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Created by klas on 20.12.13.
 */
public class Weekdays {


    public enum Weekday {
        MO(1, Calendar.MONDAY),
        TUE(2, Calendar.TUESDAY),
        WED(4, Calendar.WEDNESDAY),
        THU(8, Calendar.THURSDAY),
        FR(16, Calendar.FRIDAY),
        SA(32, Calendar.SATURDAY),
        SU(64, Calendar.SUNDAY);

        private final int _pattern;
        private final int _dayOfWeek;
        Weekday(int pattern, int dayOfWeek) {
            _pattern = pattern;
            _dayOfWeek = dayOfWeek;
        }

        public int dayOfWeek() {
            return _dayOfWeek;
        }

        public Weekday next() {
            int nextOrdinal = ordinal() + 1;
            if (nextOrdinal == Weekday.values().length) {
                return Weekday.values()[0];
            }
            return Weekday.values()[nextOrdinal];
        }
    }

    public static Weekday getWeekday(LocalDate now) {
        return Weekday.values()[now.getDayOfWeek()-1];
    }

    private static boolean matches(int weekdays, Weekday weekday) {
        return (weekdays & weekday._pattern) > 0;
    }

    public static final Set<Weekday> deserialize(int weekdays) {
        Set<Weekday> r = new HashSet<Weekday>();
        for (Weekday weekday: Weekday.values()) {
            if (matches(weekdays, weekday)) {
                r.add(weekday);
            }
        }
        if (r.isEmpty()) {
            return EnumSet.noneOf(Weekday.class);
        }
        return EnumSet.copyOf(r);
    }

    public static int serialize(Set<Weekday> weekdays) {
        int r = 0;
        for (Weekday weekday: weekdays) {
            r |= weekday._pattern;
        }
        return r;
    }

    public static String toUserVisibleString(Context context, Weekday weekday) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, weekday.dayOfWeek());
        return String.format(Locale.getDefault(), "%1$ta", cal.getTime());
    }

    public static String toUserVisibleString(Context context, Set<Weekday> weekdays) {
        Weekday[] values = Weekday.values();
        List<String> result = new ArrayList<String>(1);
        Weekday first = null;
        Weekday last = null;
        for (int i = 0; i < values.length; i++) {
            Weekday weekday = values[i];
            boolean contained = weekdays.contains(weekday);
            if (contained) {
                if (first == null) {
                    first = weekday;
                }
                last = weekday;
            } else {
                if (last != null) {
                    append(context, result, first, last);
                    first = null;
                    last = null;
                }
            }
        }
        // ensure we are not missing the last day
        if (last != null) {
            append(context, result, first, last);
        }
        return Joiner.on(", ").join(result);
    }

    private static void append(Context context, List<String> result, Weekday first, Weekday last) {
        if (first == last) {
            result.add(toUserVisibleString(context, first));
        } else {
            result.add(toUserVisibleString(context, first) + " - " + toUserVisibleString(context, last));
        }
    }
}
