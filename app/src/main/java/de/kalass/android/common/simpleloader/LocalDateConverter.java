package de.kalass.android.common.simpleloader;

import org.joda.time.LocalDate;

/**
 * Created by klas on 22.12.13.
 */
public class LocalDateConverter {

    public static Long serialize(LocalDate localDate) {
        return localDate == null ? null : localDate.toDateTimeAtStartOfDay().getMillis();
    }

    public static LocalDate deserialize(Long value) {
        return value == null ? null : new LocalDate(value);
    }
}
