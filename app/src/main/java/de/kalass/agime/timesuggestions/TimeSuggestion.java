package de.kalass.agime.timesuggestions;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;

import org.joda.time.LocalTime;

import de.kalass.android.common.util.DateUtil;

/**
 * Created by klas on 19.11.13.
 */
public class TimeSuggestion {
    private final String _label;
    private final LocalTime _time;
    private final Type _type;
    private final Long _id;


    public enum Type {
        ACTIVITY_BEGIN,
        ACTIVITY_END,
        WORKINGDAY_START,
        DAY_START,
        DAY_END,
        NOW,
        ANY_TIME
    }

    public static final Function<TimeSuggestion, LocalTime> GET_TIME_MINUTE_PRECISION = new Function<TimeSuggestion, LocalTime>() {
        @Override
        public LocalTime apply(TimeSuggestion input) {
            return input == null ? null : (input.getType() == Type.ANY_TIME ? null : input._time);
        }
    };

    public static final Function<TimeSuggestion, Long> GET_ACTIVITY_ITEM_ID = new Function<TimeSuggestion, Long>() {
        @Override
        public Long apply(TimeSuggestion input) {
            return input == null ? null : input._id;
        }
    };

    private TimeSuggestion(Type type, Long id, String label, LocalTime time) {
        if (time != null && (time.getMillisOfSecond() != 0 || time.getSecondOfMinute() != 0)) {
            _time = time.withMillisOfSecond(0).withSecondOfMinute(0);
        } else {
            _time = time;
        }
        Preconditions.checkArgument(time == null || time.getSecondOfMinute() == 0);
        Preconditions.checkArgument(time == null || time.getMillisOfSecond() == 0);
        _type = type;
        _id = id;
        _label = label;
    }

    public static TimeSuggestion now() {
        return new TimeSuggestion(Type.NOW, null, null, DateUtil.getNowMinutePrecision());
    }

    public static TimeSuggestion now(LocalTime value) {
        return new TimeSuggestion(Type.NOW, null, null, value);
    }

    public static TimeSuggestion anyTime() {
        return new TimeSuggestion(Type.ANY_TIME, null, null, null);
    }

    public static TimeSuggestion anyTime(LocalTime defaultValue) {
        return new TimeSuggestion(Type.ANY_TIME, null, null, DateUtil.trimMinutePrecision(defaultValue));
    }

    public static TimeSuggestion activityEnd(long id, String label, LocalTime time) {
        return new TimeSuggestion(Type.ACTIVITY_END, id, label, time);
    }

    public static TimeSuggestion activityBegin(long id, String label, LocalTime time) {
        return new TimeSuggestion(Type.ACTIVITY_BEGIN, id, label, time);
    }

    public static TimeSuggestion workingdayStart(LocalTime time) {
        return new TimeSuggestion(Type.WORKINGDAY_START, null, null, time);
    }

    public static TimeSuggestion dayStart() {
        return new TimeSuggestion(Type.DAY_START, null, null, new LocalTime(0,0));
    }

    public static TimeSuggestion dayEnd() {
        return new TimeSuggestion(Type.DAY_END, null, null, new LocalTime(23,59));
    }

    public Long getActivityItemId() {
        return _id;
    }

    public Type getType() {
        return _type;
    }


    public String getLabel() {
        return _label;
    }

    public LocalTime getTimeMinutePrecision() {
        return _time;
    }
}
