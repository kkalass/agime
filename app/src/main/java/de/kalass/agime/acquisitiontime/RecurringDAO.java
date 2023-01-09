package de.kalass.agime.acquisitiontime;

import android.database.Cursor;
import android.net.Uri;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.util.Set;

import javax.annotation.Nullable;

import de.kalass.android.common.simpleloader.CursorUtil;
import de.kalass.android.common.simpleloader.HourMinute;
import de.kalass.android.common.simpleloader.LocalDateConverter;
import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.simpleloader.Weekdays;
import de.kalass.android.common.model.IViewModel;

import static de.kalass.android.common.simpleloader.CursorUtil.getIndex;

/**
* Created by klas on 23.12.13.
*/
public final class RecurringDAO {
    public static final Uri CONTENT_URI = MCContract.RecurringAcquisitionTime.CONTENT_URI;

    public static final String COLUMN_NAME_ID = MCContract.RecurringAcquisitionTime.COLUMN_NAME_ID;
    public static final String COLUMN_NAME_START_TIME = MCContract.RecurringAcquisitionTime.COLUMN_NAME_START_TIME;
    public static final String COLUMN_NAME_END_TIME = MCContract.RecurringAcquisitionTime.COLUMN_NAME_END_TIME;
    public static final String COLUMN_NAME_INACTIVE_UNTIL = MCContract.RecurringAcquisitionTime.COLUMN_NAME_INACTIVE_UNTIL;
    public static final String COLUMN_NAME_ACTIVE_ONCE_DATE = MCContract.RecurringAcquisitionTime.COLUMN_NAME_ACTIVE_ONCE_DATE;
    public static final String COLUMN_NAME_WEEKDAY_PATTERN = MCContract.RecurringAcquisitionTime.COLUMN_NAME_WEEKDAY_PATTERN;

    public static final String[] PROJECTION = new String[] {
            COLUMN_NAME_ID,
            COLUMN_NAME_START_TIME,
            COLUMN_NAME_END_TIME,
            COLUMN_NAME_INACTIVE_UNTIL,
            COLUMN_NAME_ACTIVE_ONCE_DATE,
            COLUMN_NAME_WEEKDAY_PATTERN
    };

    public static final int IDX_ID = getIndex(PROJECTION, COLUMN_NAME_ID);
    public static final int IDX_START_TIME = getIndex(PROJECTION, COLUMN_NAME_START_TIME);
    public static final int IDX_END_TIME = getIndex(PROJECTION, COLUMN_NAME_END_TIME);
    public static final int IDX_INACTIVE_UNTIL = getIndex(PROJECTION, COLUMN_NAME_INACTIVE_UNTIL);
    public static final int IDX_ACTIVE_ONCE = getIndex(PROJECTION, COLUMN_NAME_ACTIVE_ONCE_DATE);
    public static final int IDX_WEEKDAY_PATTERN = getIndex(PROJECTION, COLUMN_NAME_WEEKDAY_PATTERN);

    public static final Function<Cursor, Data> READ_DATA = new Function<Cursor, Data>() {
        @Override
        public Data apply(Cursor cursor) {
            return cursor == null ? null : new Data(cursor);
        }
    };

    public static final String selection() {
        return COLUMN_NAME_ACTIVE_ONCE_DATE + " is NULL OR " + COLUMN_NAME_ACTIVE_ONCE_DATE + " >= ? ";
    }

    public static final String[] selectionArgs() {
        return new String[] {
                Long.toString(new LocalDate().toDateTimeAtStartOfDay().getMillis())
        };
    }

    public static final class Data implements IViewModel {
        static final Predicate<Data> IS_ACTIVE_ONCE = new Predicate<Data>() {
            @Override
            public boolean apply(@Nullable Data data) {
                return data != null && data.isActiveOnce();
            }
        };
        final long id;
        final Set<Weekdays.Weekday> weekdays;
        final LocalTime startTime;
        final LocalTime endTime;
        final LocalDate inactiveUntil;
        final LocalDate activeOnce;

        Data(Cursor cursor) {
            id = cursor.getLong(IDX_ID);
            inactiveUntil = CursorUtil.getLocalDate(cursor, IDX_INACTIVE_UNTIL);
            activeOnce = CursorUtil.getLocalDate(cursor, IDX_ACTIVE_ONCE);
            weekdays = Weekdays.deserialize(cursor.getInt(IDX_WEEKDAY_PATTERN));
            startTime = HourMinute.deserialize(cursor.getString(IDX_START_TIME));
            endTime = HourMinute.deserialize(cursor.getString(IDX_END_TIME));
        }

        public LocalTime getStartTime() {
            return startTime;
        }

        public LocalTime getEndTime() {
            return endTime;
        }

        @Override
        public long getId() {
            return id;
        }

        public boolean hasWeekday(Weekdays.Weekday day) {
            return weekdays.contains(day);
        }

        public boolean isCurrentlyEnabled(LocalDate today, LocalTime now) {
            if (activeOnce != null) {
                return today.isBefore(activeOnce) || (today.isEqual(activeOnce) && now.isBefore(endTime));
            }
            return Util.isCurrentlyActive(inactiveUntil, today);
        }

        public boolean isCurrentlyEnabled(LocalDate today) {
            return isActiveOnce() || Util.isCurrentlyActive(inactiveUntil, today);
        }

        public boolean isActiveOnce() {
            return activeOnce != null;
        }

        public boolean isWithin(LocalTime time) {
            return !startTime.isAfter(time) && !endTime.isBefore(time);
        }
    }
}
