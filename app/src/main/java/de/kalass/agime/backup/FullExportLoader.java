package de.kalass.agime.backup;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;

import org.joda.time.LocalTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import de.kalass.agime.backup.BackupData.PersonalBackup;
import de.kalass.android.common.simpleloader.HourMinute;
import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.simpleloader.Weekdays;
import de.kalass.android.common.activity.ContentResolverUtil;
import de.kalass.android.common.simpleloader.CursorUtil;

import static de.kalass.android.common.simpleloader.CursorFkt.newLongGetter;
import static de.kalass.android.common.simpleloader.CursorUtil.getIndex;

/**
 * Loads a full export of all Agime DataProvider data.
 * Created by klas on 17.01.14.
 */
public class FullExportLoader {

    private final Context _context;

    FullExportLoader(Context context) {
        _context = context;
    }

    private <D> List<D> loadAll(Function<Cursor, D> reader, Uri uri, String[] projection) {
        return ContentResolverUtil.loadFromContentResolver(_context, reader, uri, projection, null, null, null);
    }

    private <I, D> Multimap<I, D> loadAllIndexed(Function<Cursor, I> keyReader, Function<Cursor, D> valueReader, Uri uri, String[] projection) {
        return ContentResolverUtil.loadIndexedFromContentResolver(
                _context, keyReader, valueReader, uri, projection, null, null, null
        );
    }

    private static final class LatestTimestampQuery {
        static final Uri[] URIS = new Uri[] {
                MCContract.Activity.CONTENT_URI,
                MCContract.ActivityCustomFieldValue.CONTENT_URI,
                MCContract.CustomFieldValue.CONTENT_URI,
                MCContract.ActivityType.CONTENT_URI,
                MCContract.Category.CONTENT_URI,
                MCContract.CustomFieldType.CONTENT_URI,
                MCContract.Project.CONTENT_URI,
                MCContract.ProjectCustomFieldType.CONTENT_URI,
                MCContract.RecurringAcquisitionTime.CONTENT_URI,
        };

        static final Function<Cursor, Long> READER = new Function<Cursor, Long>() {

            @Override
            public Long apply(Cursor cursor) {
                return Math.max(CursorUtil.getLongOrZero(cursor, 0), CursorUtil.getLongOrZero(cursor, 1));
            }
        };
        static final String[] PROJECTION = new String[]{"max(" + MCContract.Activity.COLUMN_NAME_CREATED_AT + ")", "max(" + MCContract.Activity.COLUMN_NAME_MODIFIED_AT + ")"};

        static final long getLatestTimestamp(Context context) {
            long latestTimestamp = 0;
            for (Uri uri : URIS) {
                long ts = ContentResolverUtil.loadFirstFromContentResolver(context,
                        LatestTimestampQuery.READER,
                        uri,
                        LatestTimestampQuery.PROJECTION,
                        null, null, null
                );
                latestTimestamp = Math.max(latestTimestamp, ts);
            }
            return latestTimestamp;
        }

    }

    public PersonalBackup loadBackupData() {
        final long latestTimestamp = LatestTimestampQuery.getLatestTimestamp(_context);

        final Multimap<Long, BackupData.CustomFieldTypeValue> customFieldValuesByTypeId = loadAllIndexed(
                newLongGetter(CustomFieldTypeValueQuery.IDX_CUSTOM_FIELD_TYPE_ID),
                new CustomFieldTypeValueQuery(),
                CustomFieldTypeValueQuery.CONTENT_URI,
                CustomFieldTypeValueQuery.PROJECTION
        );
        final List<BackupData.CustomFieldType> customFieldTypes = loadAll(
                new CustomFieldTypeQuery(customFieldValuesByTypeId), CustomFieldTypeQuery.CONTENT_URI, CustomFieldTypeQuery.PROJECTION
        );
        final List<BackupData.ActivityTypeCategory> activityTypeCategories = loadAll(
                new ActivityTypeCategoryQuery(), ActivityTypeCategoryQuery.CONTENT_URI, ActivityTypeCategoryQuery.PROJECTION
        );
        final List<BackupData.ActivityType> activityTypes = loadAll(
                new ActivityTypeQuery(), ActivityTypeQuery.CONTENT_URI, ActivityTypeQuery.PROJECTION
        );
        final Multimap<Long, BackupData.ProjectCustomFieldType> customFieldTypesByProjectId = loadAllIndexed(
                newLongGetter(ProjecCustomFieldTypeQuery.IDX_PROJECT_ID),
                new ProjecCustomFieldTypeQuery(),
                ProjecCustomFieldTypeQuery.CONTENT_URI,
                ProjecCustomFieldTypeQuery.PROJECTION
        );
        final List<BackupData.Project> projects = loadAll(
                new ProjectQuery(customFieldTypesByProjectId), ProjectQuery.CONTENT_URI, ProjectQuery.PROJECTION
        );

        final List<BackupData.RecurringAcquisitionTime> recurringAcquisitionTimes = loadAll(
                new RecurringAcquisitionTimeQuery(), RecurringAcquisitionTimeQuery.CONTENT_URI, RecurringAcquisitionTimeQuery.PROJECTION
        );

        final Multimap<Long, BackupData.ActivityCustomFieldValue> activityCustomFieldValues = loadAllIndexed(
                newLongGetter(ActivityCustomFieldValueQuery.IDX_TRACKED_ACTIVITY_ID),
                new ActivityCustomFieldValueQuery(),
                ActivityCustomFieldValueQuery.CONTENT_URI,
                ActivityCustomFieldValueQuery.PROJECTION
        );
        final List<BackupData.TrackedActivity> trackedActivities = loadAll(
                new TrackedActivityQuery(activityCustomFieldValues),
                TrackedActivityQuery.CONTENT_URI, TrackedActivityQuery.PROJECTION
        );

        return PersonalBackup.newBuilder()
                .addAllActivityTypeCategories(activityTypeCategories)
                .addAllActivityTypes(activityTypes)
                .addAllProjects(projects)
                .addAllCustomFieldTypes(customFieldTypes)
                .addAllRecurringAcquisitionTimes(recurringAcquisitionTimes)
                .addAllTrackedActivities(trackedActivities)
                .setCreatedAt(System.currentTimeMillis())
                .setLatestInsertOrUpdateMillis(latestTimestamp)
                .build();
    }

    static final class ActivityTypeCategoryQuery extends AbstractCRUDContentItemQuery<BackupData.ActivityTypeCategory> {
        static final Uri CONTENT_URI = MCContract.Category.CONTENT_URI;
        static final String COLUMN_NAME_NAME = MCContract.Category.COLUMN_NAME_NAME;
        static final String COLUMN_NAME_COLOR_CODE = MCContract.Category.COLUMN_NAME_COLOR_CODE;

        static final String[] PROJECTION = buildProjection(
                COLUMN_NAME_NAME,
                COLUMN_NAME_COLOR_CODE
        );

        static final int IDX_NAME = getIndex(PROJECTION, COLUMN_NAME_NAME);
        static final int IDX_COLOR_CODE = getIndex(PROJECTION, COLUMN_NAME_COLOR_CODE);

        @Override
        public BackupData.ActivityTypeCategory build(Cursor cursor, long id, long createdAtMillis, long modifiedAtMillis) {
            final BackupData.ActivityTypeCategory.Builder builder = BackupData.ActivityTypeCategory.newBuilder()
                    .setIdentifier(id)
                    .setCreatedAt(createdAtMillis)
                    .setModifiedAt(modifiedAtMillis)
                    .setName(cursor.getString(IDX_NAME));

            if (!cursor.isNull(IDX_COLOR_CODE)) {
                builder.setColorCode(cursor.getLong(IDX_COLOR_CODE));
            }
            return builder.build();
        }
    }

    static final class ActivityTypeQuery extends AbstractCRUDContentItemQuery<BackupData.ActivityType> {
        static final Uri CONTENT_URI = MCContract.ActivityType.CONTENT_URI;
        static final String COLUMN_NAME_NAME = MCContract.ActivityType.COLUMN_NAME_NAME;
        static final String COLUMN_NAME_ACTIVITY_TYPE_CATEGORY_ID = MCContract.ActivityType.COLUMN_NAME_ACTIVITY_CATEGORY_ID;

        static final String[] PROJECTION = buildProjection(
                COLUMN_NAME_NAME,
                COLUMN_NAME_ACTIVITY_TYPE_CATEGORY_ID
        );

        static final int IDX_NAME = getIndex(PROJECTION, COLUMN_NAME_NAME);
        static final int IDX_ACTIVITIY_TYPE_CATEGORY_ID = getIndex(PROJECTION, COLUMN_NAME_ACTIVITY_TYPE_CATEGORY_ID);

        @Override
        public BackupData.ActivityType build(Cursor cursor, long id, long createdAtMillis, long modifiedAtMillis) {
            final BackupData.ActivityType.Builder builder = BackupData.ActivityType.newBuilder()
                    .setIdentifier(id)
                    .setCreatedAt(createdAtMillis)
                    .setModifiedAt(modifiedAtMillis)
                    .setName(cursor.getString(IDX_NAME));

            if (!cursor.isNull(IDX_ACTIVITIY_TYPE_CATEGORY_ID)) {
                builder.setActivityTypeCategoryReference(cursor.getLong(IDX_ACTIVITIY_TYPE_CATEGORY_ID));
            }
            return builder.build();
        }
    }


    static final class ProjectQuery extends AbstractCRUDContentItemQuery<BackupData.Project> {
        static final Uri CONTENT_URI = MCContract.Project.CONTENT_URI;
        static final String COLUMN_NAME_NAME = MCContract.Project.COLUMN_NAME_NAME;
        static final String COLUMN_NAME_COLOR_CODE = MCContract.Project.COLUMN_NAME_COLOR_CODE;
        static final String COLUMN_NAME_ACTIVE_UNTIL_MILLIS = MCContract.Project.COLUMN_NAME_ACTIVE_UNTIL_MILLIS;

        static final String[] PROJECTION = buildProjection(
                COLUMN_NAME_NAME,
                COLUMN_NAME_COLOR_CODE,
                COLUMN_NAME_ACTIVE_UNTIL_MILLIS
        );

        static final int IDX_NAME = getIndex(PROJECTION, COLUMN_NAME_NAME);
        static final int IDX_COLOR_CODE = getIndex(PROJECTION, COLUMN_NAME_COLOR_CODE);
        static final int IDX_ACTIVE_UNTIL_MILLIS = getIndex(PROJECTION, COLUMN_NAME_ACTIVE_UNTIL_MILLIS);
        private final Multimap<Long, BackupData.ProjectCustomFieldType> customFieldTypesByProjectId;

        ProjectQuery(Multimap<Long, BackupData.ProjectCustomFieldType> customFieldTypesByProjectId) {
            this.customFieldTypesByProjectId = customFieldTypesByProjectId;
        }

        @Override
        public BackupData.Project build(Cursor cursor, long projectId, long createdAtMillis, long modifiedAtMillis) {
            final BackupData.Project.Builder builder = BackupData.Project.newBuilder()
                .setIdentifier(projectId)
                .setCreatedAt(createdAtMillis)
                .setModifiedAt(modifiedAtMillis)
                .setName(cursor.getString(IDX_NAME));

            if (!cursor.isNull(IDX_ACTIVE_UNTIL_MILLIS)) {
                builder.setActiveUntilMillis(cursor.getLong(IDX_ACTIVE_UNTIL_MILLIS));
            }
            if (!cursor.isNull(IDX_COLOR_CODE)) {
                builder.setColorCode(cursor.getLong(IDX_COLOR_CODE));
            }
            Iterable<? extends BackupData.ProjectCustomFieldType> customFieldTypes = customFieldTypesByProjectId.get(projectId);
            if (customFieldTypes != null) {
                builder.addAllCustomFieldTypes(customFieldTypes);
            }
            return builder.build();
        }
    }

    static final class CustomFieldTypeQuery extends AbstractCRUDContentItemQuery<BackupData.CustomFieldType> {
        static final Uri CONTENT_URI = MCContract.CustomFieldType.CONTENT_URI;
        static final String COLUMN_NAME_NAME = MCContract.CustomFieldType.COLUMN_NAME_NAME;
        static final String COLUMN_NAME_ANY_PROJECT = MCContract.CustomFieldType.COLUMN_NAME_ANY_PROJECT;

        static final String[] PROJECTION = buildProjection(
                COLUMN_NAME_NAME,
                COLUMN_NAME_ANY_PROJECT
        );

        static final int IDX_NAME = getIndex(PROJECTION, COLUMN_NAME_NAME);
        static final int IDX_ANY_PROJECT = getIndex(PROJECTION, COLUMN_NAME_ANY_PROJECT);

        private final Multimap<Long, BackupData.CustomFieldTypeValue> valuesByTypeId;

        CustomFieldTypeQuery(Multimap<Long, BackupData.CustomFieldTypeValue> valuesByTypeId) {
            this.valuesByTypeId = valuesByTypeId;
        }

        @Override
        public BackupData.CustomFieldType build(Cursor cursor, long id, long createdAtMillis, long modifiedAtMillis) {
            final BackupData.CustomFieldType.Builder builder = BackupData.CustomFieldType.newBuilder()
                    .setIdentifier(id)
                    .setCreatedAt(createdAtMillis)
                    .setModifiedAt(modifiedAtMillis)
                    .setName(cursor.getString(IDX_NAME));

            if (!cursor.isNull(IDX_ANY_PROJECT)) {
                builder.setAnyProject(CursorUtil.getBoolean(cursor, IDX_ANY_PROJECT));
            }
            final Collection<BackupData.CustomFieldTypeValue> values = valuesByTypeId.get(id);
            if (values != null) {
                builder.addAllValues(values);
            }
            return builder.build();
        }
    }

    static final class CustomFieldTypeValueQuery extends AbstractCRUDContentItemQuery<BackupData.CustomFieldTypeValue> {
        static final Uri CONTENT_URI = MCContract.CustomFieldValue.CONTENT_URI;
        static final String COLUMN_NAME_VALUE = MCContract.CustomFieldValue.COLUMN_NAME_VALUE;
        static final String COLUMN_NAME_CUSTOM_FIELD_TYPE_ID = MCContract.CustomFieldValue.COLUMN_NAME_CUSTOM_FIELD_TYPE_ID;

        static final String[] PROJECTION = buildProjection(
                COLUMN_NAME_VALUE,
                COLUMN_NAME_CUSTOM_FIELD_TYPE_ID
        );

        static final int IDX_VALUE = getIndex(PROJECTION, COLUMN_NAME_VALUE);
        static final int IDX_CUSTOM_FIELD_TYPE_ID = getIndex(PROJECTION, COLUMN_NAME_CUSTOM_FIELD_TYPE_ID);


        @Override
        public BackupData.CustomFieldTypeValue build(Cursor cursor, long id, long createdAtMillis, long modifiedAtMillis) {
            return BackupData.CustomFieldTypeValue.newBuilder()
                    .setIdentifier(id)
                    .setCreatedAt(createdAtMillis)
                    .setModifiedAt(modifiedAtMillis)
                    .setFieldValue(cursor.getString(IDX_VALUE))
                    .build();
        }
    }

    static final class ProjecCustomFieldTypeQuery extends AbstractCRUDContentItemQuery<BackupData.ProjectCustomFieldType> {
        static final Uri CONTENT_URI = MCContract.ProjectCustomFieldType.CONTENT_URI;
        static final String COLUMN_NAME_COLUMN_NAME_PROJECT_ID = MCContract.ProjectCustomFieldType.COLUMN_NAME_PROJECT_ID;
        static final String COLUMN_NAME_CUSTOM_FIELD_TYPE_ID = MCContract.ProjectCustomFieldType.COLUMN_NAME_CUSTOM_FIELD_TYPE_ID;

        static final String[] PROJECTION = buildProjection(
                COLUMN_NAME_COLUMN_NAME_PROJECT_ID,
                COLUMN_NAME_CUSTOM_FIELD_TYPE_ID
        );

        static final int IDX_PROJECT_ID = getIndex(PROJECTION, COLUMN_NAME_COLUMN_NAME_PROJECT_ID);
        static final int IDX_CUSTOM_FIELD_TYPE_ID = getIndex(PROJECTION, COLUMN_NAME_CUSTOM_FIELD_TYPE_ID);


        @Override
        public BackupData.ProjectCustomFieldType build(Cursor cursor, long id, long createdAtMillis, long modifiedAtMillis) {
            return BackupData.ProjectCustomFieldType.newBuilder()
                    .setIdentifier(id)
                    .setCreatedAt(createdAtMillis)
                    .setModifiedAt(modifiedAtMillis)
                    .setCustomFieldTypeReference(cursor.getLong(IDX_CUSTOM_FIELD_TYPE_ID))
                    .build();
        }
    }

    static final class RecurringAcquisitionTimeQuery extends AbstractCRUDContentItemQuery<BackupData.RecurringAcquisitionTime> {
        static final Uri CONTENT_URI = MCContract.RecurringAcquisitionTime.CONTENT_URI;
        static final String COLUMN_NAME_START_TIME = MCContract.RecurringAcquisitionTime.COLUMN_NAME_START_TIME;
        static final String COLUMN_NAME_END_TIME = MCContract.RecurringAcquisitionTime.COLUMN_NAME_END_TIME;
        static final String COLUMN_NAME_ACTIVE_ONCE = MCContract.RecurringAcquisitionTime.COLUMN_NAME_ACTIVE_ONCE_DATE;
        static final String COLUMN_NAME_INACTIVE_UNTIL = MCContract.RecurringAcquisitionTime.COLUMN_NAME_INACTIVE_UNTIL;
        static final String COLUMN_NAME_WEEKDAY_PATTERN = MCContract.RecurringAcquisitionTime.COLUMN_NAME_WEEKDAY_PATTERN;

        static final String[] PROJECTION = buildProjection(
                COLUMN_NAME_START_TIME,
                COLUMN_NAME_END_TIME,
                COLUMN_NAME_ACTIVE_ONCE,
                COLUMN_NAME_INACTIVE_UNTIL,
                COLUMN_NAME_WEEKDAY_PATTERN
        );

        static final int IDX_START_TIME = getIndex(PROJECTION, COLUMN_NAME_START_TIME);
        static final int IDX_END_TIME = getIndex(PROJECTION, COLUMN_NAME_END_TIME);
        static final int IDX_INACTIVE_UNTIL = getIndex(PROJECTION, COLUMN_NAME_INACTIVE_UNTIL);
        static final int IDX_ACTIVE_ONCE = getIndex(PROJECTION, COLUMN_NAME_ACTIVE_ONCE);
        static final int IDX_WEEKDAY_PATTERN = getIndex(PROJECTION, COLUMN_NAME_WEEKDAY_PATTERN);


        @Override
        public BackupData.RecurringAcquisitionTime build(Cursor cursor, long id, long createdAtMillis, long modifiedAtMillis) {
            final LocalTime startTime = getHourMinute(cursor.getString(IDX_START_TIME));
            final LocalTime endTime = getHourMinute(cursor.getString(IDX_END_TIME));
            final BackupData.RecurringAcquisitionTime.Builder builder = BackupData.RecurringAcquisitionTime.newBuilder()
                    .setIdentifier(id)
                    .setCreatedAt(createdAtMillis)
                    .setModifiedAt(modifiedAtMillis)
                    .setStartTimeHours(startTime.getHourOfDay())
                    .setStartTimeMinutes(startTime.getMinuteOfHour())
                    .setEndTimeHours(endTime.getHourOfDay())
                    .setEndTimeMinutes(endTime.getMinuteOfHour());

            if (!cursor.isNull(IDX_WEEKDAY_PATTERN)) {
                builder.addAllWeekdays(readWeekdays(cursor.getInt(IDX_WEEKDAY_PATTERN)));
            }
            if (!cursor.isNull(IDX_INACTIVE_UNTIL)) {
                builder.setInactiveUntilMillis(cursor.getLong(IDX_INACTIVE_UNTIL));
            }

            if (!cursor.isNull(IDX_ACTIVE_ONCE)) {
                builder.setActiveOnceDateMillis(cursor.getLong(IDX_ACTIVE_ONCE));
            }
            return builder.build();
        }

        /**
         * Ensures that the int value is a valid weekdays bitmask by deserializing and then serializing
         * it again.
         */
        private Iterable<BackupData.RecurringAcquisitionTime.Weekday> readWeekdays(int weekdaysBitmask) {
            // deserialize from DB
            final Set<Weekdays.Weekday> weekdays = Weekdays.deserialize(weekdaysBitmask);
            // ..and convert to protobuf
            final List<BackupData.RecurringAcquisitionTime.Weekday> result = new ArrayList<BackupData.RecurringAcquisitionTime.Weekday>(weekdays.size());
            for (Weekdays.Weekday weekday: weekdays) {
                result.add(toBackupData(weekday));
            }
            return result;
        }

        private BackupData.RecurringAcquisitionTime.Weekday toBackupData(Weekdays.Weekday weekday) {
            switch (weekday) {
                case MO:
                    return BackupData.RecurringAcquisitionTime.Weekday.MO;
                case TUE:
                    return BackupData.RecurringAcquisitionTime.Weekday.TUE;
                case WED:
                    return BackupData.RecurringAcquisitionTime.Weekday.WED;
                case THU:
                    return BackupData.RecurringAcquisitionTime.Weekday.THU;
                case FR:
                    return BackupData.RecurringAcquisitionTime.Weekday.FR;
                case SA:
                    return BackupData.RecurringAcquisitionTime.Weekday.SA;
                case SU:
                    return BackupData.RecurringAcquisitionTime.Weekday.SU;
                default:
                    throw new IllegalArgumentException("Unsupported value for Weekday: " + weekday);
            }
        }

        private LocalTime getHourMinute(String string) {
            return HourMinute.deserialize(string);
        }
    }

    static final class ActivityCustomFieldValueQuery extends AbstractCRUDContentItemQuery<BackupData.ActivityCustomFieldValue> {

        static final Uri CONTENT_URI = MCContract.ActivityCustomFieldValue.CONTENT_URI;
        static final String COLUMN_NAME_TRACKED_ACTIVITY_ID = MCContract.ActivityCustomFieldValue.COLUMN_NAME_TRACKED_ACTIVITY_ID;
        static final String COLUMN_NAME_CUSTOM_FIELD_VALUE_ID = MCContract.ActivityCustomFieldValue.COLUMN_NAME_CUSTOM_FIELD_VALUE_ID;


        static final String[] PROJECTION = buildProjection(
                COLUMN_NAME_TRACKED_ACTIVITY_ID,
                COLUMN_NAME_CUSTOM_FIELD_VALUE_ID
        );

        static final int IDX_TRACKED_ACTIVITY_ID = getIndex(PROJECTION, COLUMN_NAME_TRACKED_ACTIVITY_ID);
        static final int IDX_CUSTOM_FIELD_VALUE_ID = getIndex(PROJECTION, COLUMN_NAME_CUSTOM_FIELD_VALUE_ID);

        @Override
        BackupData.ActivityCustomFieldValue build(Cursor cursor, long id, long createdAtMillis, long modifiedAtMillis) {
            return BackupData.ActivityCustomFieldValue.newBuilder()
                    .setIdentifier(id)
                    .setCreatedAt(createdAtMillis)
                    .setModifiedAt(modifiedAtMillis)
                    .setCustomFieldValueReference(cursor.getLong(IDX_CUSTOM_FIELD_VALUE_ID))
                    .build();
        }
    }

    static final class TrackedActivityQuery extends AbstractCRUDContentItemQuery<BackupData.TrackedActivity> {
        static final Uri CONTENT_URI = MCContract.Activity.CONTENT_URI;
        static final String COLUMN_NAME_ACTIVITY_TYPE_ID = MCContract.Activity.COLUMN_NAME_ACTIVITY_TYPE_ID;
        static final String COLUMN_NAME_PROJECT_ID = MCContract.Activity.COLUMN_NAME_PROJECT_ID;
        static final String COLUMN_NAME_DETAILS = MCContract.Activity.COLUMN_NAME_DETAILS;
        static final String COLUMN_NAME_START_TIME = MCContract.Activity.COLUMN_NAME_START_TIME;
        static final String COLUMN_NAME_END_TIME = MCContract.Activity.COLUMN_NAME_END_TIME;
        static final String COLUMN_NAME_INSERT_DURATION_MILLIS = MCContract.Activity.COLUMN_NAME_INSERT_DURATION_MILLIS;
        static final String COLUMN_NAME_UPDATE_DURATION_MILLIS = MCContract.Activity.COLUMN_NAME_UPDATE_DURATION_MILLIS;
        static final String COLUMN_NAME_UPDATE_COUNT = MCContract.Activity.COLUMN_NAME_UPDATE_COUNT;


        static final String[] PROJECTION = buildProjection(
                COLUMN_NAME_ACTIVITY_TYPE_ID,
                COLUMN_NAME_PROJECT_ID,
                COLUMN_NAME_DETAILS,
                COLUMN_NAME_START_TIME,
                COLUMN_NAME_END_TIME,
                COLUMN_NAME_INSERT_DURATION_MILLIS,
                COLUMN_NAME_UPDATE_DURATION_MILLIS,
                COLUMN_NAME_UPDATE_COUNT
        );

        static final int IDX_ACTIVITY_TYPE_ID = getIndex(PROJECTION, COLUMN_NAME_ACTIVITY_TYPE_ID);
        static final int IDX_PROJECT_ID = getIndex(PROJECTION, COLUMN_NAME_PROJECT_ID);
        static final int IDX_DETAILS = getIndex(PROJECTION, COLUMN_NAME_DETAILS);
        static final int IDX_START_TIME = getIndex(PROJECTION, COLUMN_NAME_START_TIME);
        static final int IDX_END_TIME = getIndex(PROJECTION, COLUMN_NAME_END_TIME);
        static final int IDX_INSERT_DURATION_MILLIS = getIndex(PROJECTION, COLUMN_NAME_INSERT_DURATION_MILLIS);
        static final int IDX_UPDATE_DURATION_MILLIS = getIndex(PROJECTION, COLUMN_NAME_UPDATE_DURATION_MILLIS);
        static final int IDX_UPDATE_COUNT = getIndex(PROJECTION, COLUMN_NAME_UPDATE_COUNT);

        private final Multimap<Long, BackupData.ActivityCustomFieldValue> _customFieldValues;

        TrackedActivityQuery(Multimap<Long, BackupData.ActivityCustomFieldValue> customFieldValues) {
            _customFieldValues = Preconditions.checkNotNull(customFieldValues);
        }

        @Override
        public BackupData.TrackedActivity build(Cursor cursor, long id, long createdAtMillis, long modifiedAtMillis) {
            final BackupData.TrackedActivity.Builder builder = BackupData.TrackedActivity.newBuilder()
                    .setIdentifier(id)
                    .setCreatedAt(createdAtMillis)
                    .setModifiedAt(modifiedAtMillis)
                    .setStarttimeMillis(cursor.getLong(IDX_START_TIME))
                    .setEndtimeMillis(cursor.getLong(IDX_END_TIME))
                    ;
            if (!cursor.isNull(IDX_ACTIVITY_TYPE_ID)) {
                builder.setActivityTypeReference(cursor.getLong(IDX_ACTIVITY_TYPE_ID));
            }
            if (!cursor.isNull(IDX_PROJECT_ID)) {
                builder.setProjectReference(cursor.getLong(IDX_PROJECT_ID));
            }
            if (!cursor.isNull(IDX_DETAILS)) {
                builder.setDetails(cursor.getString(IDX_DETAILS));
            }
            if (!cursor.isNull(IDX_UPDATE_DURATION_MILLIS)) {
                builder.setUpdateDurationMillis(cursor.getInt(IDX_UPDATE_DURATION_MILLIS));
            }
            if (!cursor.isNull(IDX_UPDATE_COUNT)) {
                builder.setUpdateCount(cursor.getInt(IDX_UPDATE_COUNT));
            }
            if (!cursor.isNull(IDX_INSERT_DURATION_MILLIS)) {
                builder.setInsertDurationMillis(cursor.getInt(IDX_INSERT_DURATION_MILLIS));
            }
            final Collection<BackupData.ActivityCustomFieldValue> customFieldValues = _customFieldValues.get(id);
            if (customFieldValues != null) {
                builder.addAllCustomFieldValues(customFieldValues);
            }
            return builder.build();
        }
    }

    static abstract class AbstractCRUDContentItemQuery<D> implements Function<Cursor, D> {
        static final String COLUMN_NAME_ID = MCContract.Project.COLUMN_NAME_ID;
        static final String COLUMN_NAME_CREATED_AT = MCContract.Project.COLUMN_NAME_CREATED_AT;
        static final String COLUMN_NAME_MODIFIED_AT = MCContract.Project.COLUMN_NAME_MODIFIED_AT;

        static final String[] buildProjection(String... args) {
            final String[] r = new String[3 + args.length];
            r[IDX_ID] = COLUMN_NAME_ID;
            r[IDX_CREATED_AT] = COLUMN_NAME_CREATED_AT;
            r[IDX_MODIFIED_AT] = COLUMN_NAME_MODIFIED_AT;
            for (int i = 0; i < args.length; i++) {
                r[3 + i] = args[i];
            }
            return r;
        };

        static final int IDX_ID = 0;
        static final int IDX_CREATED_AT = 1;
        static final int IDX_MODIFIED_AT = 2;

        @Override
        public final D apply(Cursor cursor) {
            long id = cursor.getLong(IDX_ID);
            long createdAt = cursor.getLong(IDX_CREATED_AT);
            long modifiedAt = cursor.getLong(IDX_MODIFIED_AT);

            return build(cursor, id, createdAt, modifiedAt);
        }

        abstract D build(Cursor cursor, long id, long createdAtMillis, long modifiedAtMillis);
    }
}
