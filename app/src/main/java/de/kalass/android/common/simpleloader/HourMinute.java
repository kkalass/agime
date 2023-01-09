package de.kalass.android.common.simpleloader;

import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Created by klas on 22.12.13.
 */
public class HourMinute {
    private static final DateTimeFormatter FORMATTER = ISODateTimeFormat.hourMinute();

    public static LocalTime deserialize(String hourMinuteString) {
        return FORMATTER.parseLocalTime(hourMinuteString);
    }

    public static String serialize(LocalTime time) {
        return FORMATTER.print(time);
    }
}
