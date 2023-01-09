package de.kalass.agime.model;

import com.google.common.base.Function;

import java.util.concurrent.TimeUnit;

/**
 * BaseClass for items that represent a duration.
 * Created by klas on 28.11.13.
 */
public class TimeSpanning {
    public static final Function<TimeSpanning, Long> GET_DURATION_MILLIS = new Function<TimeSpanning, Long>() {
        @Override
        public Long apply(TimeSpanning input) {
            return input == null ? null : input.getDurationMillis();
        }
    };

    private final long _millis;

    public TimeSpanning(long millis) {
        _millis = millis;
    }

    public final long getDurationMillis() {
        return _millis;
    }

    public final long getDurationMinutes() {
        long elapsedSeconds = TimeUnit.MILLISECONDS.toSeconds(_millis);
        long elapsedMinutesTotal = elapsedSeconds / 60;
        return elapsedMinutesTotal;
    }
}
