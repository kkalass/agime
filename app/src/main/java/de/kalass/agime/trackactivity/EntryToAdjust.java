package de.kalass.agime.trackactivity;

import android.database.Cursor;

import com.google.common.base.Function;
import com.google.common.base.Objects;

import de.kalass.android.common.simpleloader.CursorUtil;
import de.kalass.android.common.simpleloader.ValueOrReference;

/**
* Created by klas on 03.02.14.
*/
public final class EntryToAdjust {
    final ValueOrReference id;
    final long startTime;
    final long endTime;
    final ValueOrReference projectId;
    final ValueOrReference activityTypeId;
    final String activityDetails;

    static final Function<Cursor, EntryToAdjust> READER = new Function<Cursor, EntryToAdjust>(){
        @Override
        public EntryToAdjust apply(Cursor cursor) {
            return new EntryToAdjust(
                ValueOrReference.ofValue(cursor.getLong(InsertOrUpdateTrackedActivity.SplitQuery.IDX_ID)),
                cursor.getLong(InsertOrUpdateTrackedActivity.SplitQuery.IDX_START_TIME),
                cursor.getLong(InsertOrUpdateTrackedActivity.SplitQuery.IDX_END_TIME),
                ValueOrReference.ofValue(CursorUtil.getLong(cursor, InsertOrUpdateTrackedActivity.SplitQuery.IDX_PROJECT_ID)),
                ValueOrReference.ofValue(CursorUtil.getLong(cursor, InsertOrUpdateTrackedActivity.SplitQuery.IDX_ACTIVITY_TYPE_ID)),
                cursor.getString(InsertOrUpdateTrackedActivity.SplitQuery.IDX_DETAILS)
            );
        }
    };

    EntryToAdjust(ValueOrReference id, long startTime, long endTime, ValueOrReference projectId,
                  ValueOrReference activityTypeId, String activityDetails) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.projectId = projectId;
        this.activityTypeId = activityTypeId;
        this.activityDetails = activityDetails;
    }

    public EntryToAdjust withStartTime(long startTime) {
        return new EntryToAdjust(id, startTime, endTime, projectId, activityTypeId, activityDetails);
    }

    public EntryToAdjust withEndTime(long endTime) {
        return new EntryToAdjust(id, startTime, endTime, projectId, activityTypeId, activityDetails);
    }

    public EntryToAdjust withId(ValueOrReference id) {
        return new EntryToAdjust(id, startTime, endTime, projectId, activityTypeId, activityDetails);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .addValue(id)
                .add("startTime", startTime)
                .add("endTime", endTime)
                .toString();
    }
}
