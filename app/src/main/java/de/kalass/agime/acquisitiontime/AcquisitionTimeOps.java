package de.kalass.agime.acquisitiontime;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.net.Uri;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.util.EnumSet;

import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.simpleloader.CursorUtil;
import de.kalass.android.common.simpleloader.Weekdays;

/**
* Created by klas on 23.12.13.
*/
public final class AcquisitionTimeOps {

    private AcquisitionTimeOps() {
    }

    public static ContentProviderOperation insert(
            LocalDate today, long now, LocalTime startTime, AcquisitionTimeInstance nextAcquisitionTime
    ) {


        // if there is a recurring acquisition time later in the day, make the new acquisition
        // time stop together with the next one. This way, the next acquisition time will start
        // as expected if the user cancels the new one before normal acquisition time start,
        // but if he makes an entry after the old time starts, it will count from the new time on,
        // as the user expects it.
        LocalTime endTimeLocalTime = nextAcquisitionTime == null || !today.equals(nextAcquisitionTime.day) ? new LocalTime(23, 59) : nextAcquisitionTime.endTime;

        return insert(today, now, startTime, endTimeLocalTime);
    }

    public static ContentProviderOperation assertQuery(LocalDate today, Uri itemUri) {
        final ContentProviderOperation.Builder assertQueryBuilder = ContentProviderOperation.newAssertQuery(itemUri);
        ContentValues assertValues = new ContentValues();
        CursorUtil.putLocalDate(assertValues, MCContract.RecurringAcquisitionTime.COLUMN_NAME_ACTIVE_ONCE_DATE, today);
        assertQueryBuilder.withExpectedCount(1);
        assertQueryBuilder.withValues(assertValues);
        return  assertQueryBuilder.build();
    }

    public static ContentProviderOperation insert(
            LocalDate today, long now, LocalTime startTime, LocalTime endTimeLocalTime
    ) {
        ContentProviderOperation.Builder b = ContentProviderOperation
                .newInsert(MCContract.RecurringAcquisitionTime.CONTENT_URI);

        ContentValues values = new ContentValues();
        values.put(MCContract.RecurringAcquisitionTime.COLUMN_NAME_CREATED_AT, now);
        values.put(MCContract.RecurringAcquisitionTime.COLUMN_NAME_MODIFIED_AT, now);
        CursorUtil.putLocalDate(values, MCContract.RecurringAcquisitionTime.COLUMN_NAME_ACTIVE_ONCE_DATE, today);
        CursorUtil.putHourMinute(values, MCContract.RecurringAcquisitionTime.COLUMN_NAME_START_TIME, startTime);
        CursorUtil.putHourMinute(values, MCContract.RecurringAcquisitionTime.COLUMN_NAME_END_TIME, endTimeLocalTime);
        CursorUtil.putWeekdays(values, MCContract.RecurringAcquisitionTime.COLUMN_NAME_WEEKDAY_PATTERN, EnumSet.noneOf(Weekdays.Weekday.class));

        b.withValues(values);

        return b.build();
    }

    public static ContentProviderOperation updateEndTime(
            Uri itemUri, LocalTime endTimeLocalTime, long nowMillis
    ) {
        ContentProviderOperation.Builder b = ContentProviderOperation.newUpdate(itemUri);

        ContentValues values = new ContentValues();
        values.put(MCContract.RecurringAcquisitionTime.COLUMN_NAME_MODIFIED_AT, nowMillis);
        CursorUtil.putHourMinute(values, MCContract.RecurringAcquisitionTime.COLUMN_NAME_END_TIME, endTimeLocalTime);

        b.withValues(values);

        return b.build();
    }
}
