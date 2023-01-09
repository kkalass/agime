package de.kalass.agime.backup;

import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.common.collect.ImmutableSet;

import org.joda.time.LocalTime;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.kalass.agime.R;
import de.kalass.android.common.AbstractContentProviderBatchTask;
import de.kalass.android.common.simpleloader.HourMinute;
import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.simpleloader.Weekdays;
import de.kalass.android.common.provider.CRUDContentItem;
import de.kalass.android.common.simpleloader.CursorUtil;

/**
 *
 * FIXME ensure that no-one can access the data while restore is in progress!
 *
* Created by klas on 18.12.13.
*/
public class RestoreTask extends AbstractContentProviderBatchTask<Uri> {
    public static final String MIME_TYPE = "application/octet-stream";

    private ProgressDialog _pd;


    private final long _importStartTimeMillis = System.currentTimeMillis();
    public RestoreTask(Context context) {
        super(context, R.string.restore_failed_title, R.string.restore_failed_message);
    }

    @Override
    protected void onPreExecute() {
        _pd = new ProgressDialog(getContext());
        _pd.setTitle("Processing...");
        _pd.setMessage("Please wait.");
        _pd.setCancelable(false);
        _pd.setIndeterminate(true);
        _pd.show();
    }

    @Override
    protected ArrayList<ContentProviderOperation> createOperationsInBackground(Uri selectedUri) throws IOException {

        Log.i("Agime", "File choosen: " + selectedUri);
        ContentResolver contentResolver = getContext().getContentResolver();
        InputStream inputStream = contentResolver.openInputStream(selectedUri);


        try {
            final BackupData.PersonalBackup personalBackup = BackupData.PersonalBackup.parseFrom(inputStream);

            // prepare - do this after we know that we can at least read the backup.
            // This should avoid deleting all user data and then failing due to a corrupt backup file...
            RestoreUtil.deleteAllData(getContext());

            // now, convert all data in the personal backup to the appropriate ContentProvider Operations

            return createAllInserts(personalBackup);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e("Agime", "Failed to close " + selectedUri , e);
                }
            }
        }
    }

    /**
     * Note that custom field types need to be inserted first, before the projects can be inserted
     */
    private ArrayList<ContentProviderOperation> createAllInserts(BackupData.PersonalBackup backup) {
        final ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>(
                // not the precise number of needed operations (ignores all inner data structures)
                // but better than starting with default
                backup.getActivityTypeCategoriesCount()
                        + backup.getCustomFieldTypesCount()
                        + backup.getProjectsCount()
                        + backup.getActivityTypesCount()
                        + backup.getTrackedActivitiesCount()
                        + backup.getRecurringAcquisitionTimesCount()
        );

        for (BackupData.RecurringAcquisitionTime recurring: backup.getRecurringAcquisitionTimesList()) {
            operations.add(createRecurringAcquisitionTime(recurring));
        }

        for (BackupData.CustomFieldType type: backup.getCustomFieldTypesList()) {
            // First: Type
            final long typeId = type.getIdentifier();
            operations.add(createCustomFieldTypeInsert(type));

            // Second: Values - they reference the type
            for (BackupData.CustomFieldTypeValue value: type.getValuesList()) {
                operations.add(createCustomFieldTypeValueInsert(typeId, value));
            }
        }

        for (BackupData.ActivityTypeCategory category: backup.getActivityTypeCategoriesList()) {
            operations.add(createActivityTypeCategoryInsert(category));
        }

        for (BackupData.ActivityType type: backup.getActivityTypesList()) {
            operations.add(createActivityTypeInsert(type));
        }

        for (BackupData.Project project: backup.getProjectsList()) {
            // First: insert Project
            operations.add(createProjectInsert(project));

            // Second: insert children of Project
            long projectId = project.getIdentifier();
            for (BackupData.ProjectCustomFieldType type : project.getCustomFieldTypesList()) {
                operations.add(createProjectCustomFieldTypeInsert(projectId, type));
            }

        }

        for (BackupData.TrackedActivity trackedActivity: backup.getTrackedActivitiesList()) {
            final long trackedActivityId = trackedActivity.getIdentifier();
            operations.add(createTrackedActivityInsert(trackedActivity));

            for (BackupData.ActivityCustomFieldValue value: trackedActivity.getCustomFieldValuesList()) {
                operations.add(createTrackedActivityCustomFieldValueInsert(trackedActivityId, value));
            }
        }

        return operations;
    }

    private ContentProviderOperation createProjectInsert(BackupData.Project item) {
        final ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(MCContract.Project.CONTENT_URI);
        withId(builder, item.getIdentifier());
        withCreatedAt(builder, item.hasCreatedAt(), item.getCreatedAt());
        withModifiedAt(builder, item.hasModifiedAt(), item.getModifiedAt());
        withName(builder, R.string.data_type_name_project, MCContract.Project.COLUMN_NAME_NAME, item.hasName(), item.getName(), item.getIdentifier());

        if (item.hasColorCode()) {
            builder.withValue(MCContract.Project.COLUMN_NAME_COLOR_CODE, item.getColorCode());
        }
        if (item.hasActiveUntilMillis()) {
            builder.withValue(MCContract.Project.COLUMN_NAME_ACTIVE_UNTIL_MILLIS, item.getActiveUntilMillis());
        }

        return builder.build();
    }


    private ContentProviderOperation createProjectCustomFieldTypeInsert(long projectId, BackupData.ProjectCustomFieldType item) {
        final ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(MCContract.ProjectCustomFieldType.CONTENT_URI);
        withId(builder, item.getIdentifier());
        withCreatedAt(builder, item.hasCreatedAt(), item.getCreatedAt());
        withModifiedAt(builder, item.hasModifiedAt(), item.getModifiedAt());

        builder.withValue(MCContract.ProjectCustomFieldType.COLUMN_NAME_PROJECT_ID, projectId);
        builder.withValue(MCContract.ProjectCustomFieldType.COLUMN_NAME_CUSTOM_FIELD_TYPE_ID, item.getCustomFieldTypeReference());

        return builder.build();
    }

    private ContentProviderOperation createCustomFieldTypeInsert(BackupData.CustomFieldType item) {
        final ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(MCContract.CustomFieldType.CONTENT_URI);
        withId(builder, item.getIdentifier());
        withCreatedAt(builder, item.hasCreatedAt(), item.getCreatedAt());
        withModifiedAt(builder, item.hasModifiedAt(), item.getModifiedAt());

        withName(builder, R.string.data_type_name_custom_field, MCContract.CustomFieldType.COLUMN_NAME_NAME, item.hasName(), item.getName(), item.getIdentifier());

        builder.withValue(MCContract.CustomFieldType.COLUMN_NAME_ANY_PROJECT, CursorUtil.boolean2int(!item.hasAnyProject() ||item.getAnyProject()));

        return builder.build();
    }


    private ContentProviderOperation createCustomFieldTypeValueInsert(long typeId, BackupData.CustomFieldTypeValue item) {
        final ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(MCContract.CustomFieldValue.CONTENT_URI);
        withId(builder, item.getIdentifier());
        withCreatedAt(builder, item.hasCreatedAt(), item.getCreatedAt());
        withModifiedAt(builder, item.hasModifiedAt(), item.getModifiedAt());

        withName(builder, R.string.data_type_name_custom_value, MCContract.CustomFieldValue.COLUMN_NAME_VALUE,
                item.hasFieldValue(), item.getFieldValue(), item.getIdentifier());
        builder.withValue(MCContract.CustomFieldValue.COLUMN_NAME_CUSTOM_FIELD_TYPE_ID, typeId);

        return builder.build();
    }

    private ContentProviderOperation createActivityTypeInsert(BackupData.ActivityType item) {
        final ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(MCContract.ActivityType.CONTENT_URI);
        withId(builder, item.getIdentifier());
        withCreatedAt(builder, item.hasCreatedAt(), item.getCreatedAt());
        withModifiedAt(builder, item.hasModifiedAt(), item.getModifiedAt());

        withName(builder, R.string.data_type_name_activity, MCContract.ActivityType.COLUMN_NAME_NAME,
                item.hasName(), item.getName(), item.getIdentifier());

        if (item.hasActivityTypeCategoryReference()) {
            builder.withValue(MCContract.ActivityType.COLUMN_NAME_ACTIVITY_CATEGORY_ID, item.getActivityTypeCategoryReference());
        }

        return builder.build();
    }

    private ContentProviderOperation createActivityTypeCategoryInsert(BackupData.ActivityTypeCategory item) {
        final ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(MCContract.Category.CONTENT_URI);
        withId(builder, item.getIdentifier());
        withCreatedAt(builder, item.hasCreatedAt(), item.getCreatedAt());
        withModifiedAt(builder, item.hasModifiedAt(), item.getModifiedAt());

        withName(builder, R.string.data_type_name_category, MCContract.Category.COLUMN_NAME_NAME,
                item.hasName(), item.getName(), item.getIdentifier());

        if (item.hasColorCode()) {
            builder.withValue(MCContract.Category.COLUMN_NAME_COLOR_CODE, item.getColorCode());
        }

        return builder.build();
    }

    private ContentProviderOperation createTrackedActivityInsert(BackupData.TrackedActivity item) {
        final ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(MCContract.Activity.CONTENT_URI);
        withId(builder, item.getIdentifier());
        withCreatedAt(builder, item.hasCreatedAt(), item.getCreatedAt());
        withModifiedAt(builder, item.hasModifiedAt(), item.getModifiedAt());

        if (item.hasActivityTypeReference()) {
            builder.withValue(MCContract.Activity.COLUMN_NAME_ACTIVITY_TYPE_ID, item.getActivityTypeReference());
        }
        if (item.hasProjectReference()) {
            builder.withValue(MCContract.Activity.COLUMN_NAME_PROJECT_ID, item.getProjectReference());
        }
        if (item.hasDetails()) {
            builder.withValue(MCContract.Activity.COLUMN_NAME_DETAILS, item.getDetails());
        }
        if (item.hasInsertDurationMillis()) {
            builder.withValue(MCContract.Activity.COLUMN_NAME_INSERT_DURATION_MILLIS, item.getInsertDurationMillis());
        }
        if (item.hasUpdateDurationMillis()) {
            builder.withValue(MCContract.Activity.COLUMN_NAME_UPDATE_DURATION_MILLIS, item.getUpdateDurationMillis());
        }
        if (item.hasUpdateCount()) {
            builder.withValue(MCContract.Activity.COLUMN_NAME_UPDATE_COUNT, item.getUpdateCount());
        }
        builder.withValue(MCContract.Activity.COLUMN_NAME_START_TIME, item.getStarttimeMillis());
        builder.withValue(MCContract.Activity.COLUMN_NAME_END_TIME, item.getEndtimeMillis());

        return builder.build();
    }

    private ContentProviderOperation createTrackedActivityCustomFieldValueInsert(
            long trackedActivityId, BackupData.ActivityCustomFieldValue item
    ) {
        final ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(MCContract.ActivityCustomFieldValue.CONTENT_URI);
        withId(builder, item.getIdentifier());
        withCreatedAt(builder, item.hasCreatedAt(), item.getCreatedAt());
        withModifiedAt(builder, item.hasModifiedAt(), item.getModifiedAt());

        builder.withValue(MCContract.ActivityCustomFieldValue.COLUMN_NAME_CUSTOM_FIELD_VALUE_ID, item.getCustomFieldValueReference());
        builder.withValue(MCContract.ActivityCustomFieldValue.COLUMN_NAME_TRACKED_ACTIVITY_ID, trackedActivityId);
        return builder.build();
    }


    private ContentProviderOperation createRecurringAcquisitionTime(BackupData.RecurringAcquisitionTime item) {
        final ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(MCContract.RecurringAcquisitionTime.CONTENT_URI);
        withId(builder, item.getIdentifier());
        withCreatedAt(builder, item.hasCreatedAt(), item.getCreatedAt());
        withModifiedAt(builder, item.hasModifiedAt(), item.getModifiedAt());

        String starttime = HourMinute.serialize(new LocalTime(item.getStartTimeHours(), item.getStartTimeMinutes()));
        builder.withValue(MCContract.RecurringAcquisitionTime.COLUMN_NAME_START_TIME, starttime);
        String endtime = HourMinute.serialize(new LocalTime(item.getEndTimeHours(), item.getEndTimeMinutes()));
        builder.withValue(MCContract.RecurringAcquisitionTime.COLUMN_NAME_END_TIME, endtime);

        int weekdayPattern = Weekdays.serialize(convertWeekdays(item.getWeekdaysList()));
        builder.withValue(MCContract.RecurringAcquisitionTime.COLUMN_NAME_WEEKDAY_PATTERN, weekdayPattern);

        if (item.hasInactiveUntilMillis()) {
            builder.withValue(MCContract.RecurringAcquisitionTime.COLUMN_NAME_INACTIVE_UNTIL, item.getInactiveUntilMillis());
        }

        if (item.hasActiveOnceDateMillis()) {
            builder.withValue(MCContract.RecurringAcquisitionTime.COLUMN_NAME_ACTIVE_ONCE_DATE, item.getActiveOnceDateMillis());
        }

        return builder.build();
    }

    private Set<Weekdays.Weekday> convertWeekdays(List<BackupData.RecurringAcquisitionTime.Weekday> weekdaysList) {
        ImmutableSet.Builder<Weekdays.Weekday> result = ImmutableSet.builder();
        for (BackupData.RecurringAcquisitionTime.Weekday weekday : weekdaysList) {
            result.add(convertWeekday(weekday));
        }
        return result.build();
    }

    private Weekdays.Weekday convertWeekday(BackupData.RecurringAcquisitionTime.Weekday weekday) {
        switch (weekday) {
            case MO:
                return Weekdays.Weekday.MO;
            case TUE:
                return Weekdays.Weekday.TUE;
            case WED:
                return Weekdays.Weekday.WED;
            case THU:
                return Weekdays.Weekday.THU;
            case FR:
                return Weekdays.Weekday.FR;
            case SA:
                return Weekdays.Weekday.SA;
            case SU:
                return Weekdays.Weekday.SU;
            default:
                throw new IllegalArgumentException("Unexpected value for Weekday: " + weekday);
        }
    }

    private void withId(ContentProviderOperation.Builder builder, long identifier) {
        builder.withValue(CRUDContentItem.COLUMN_NAME_ID, identifier);
    }

    private void withName(
            ContentProviderOperation.Builder builder,
            int defaultNameBaseResId,
            String nameColumnName, boolean hasName, String name, long identifier) {
        String typeName = getContext().getString(defaultNameBaseResId);
        builder.withValue(nameColumnName, hasName ? name : typeName + "-" + identifier);
    }
    private void withCreatedAt(ContentProviderOperation.Builder builder, boolean hasValue, long createdAtMillis) {
        builder.withValue(CRUDContentItem.COLUMN_NAME_CREATED_AT, hasValue ? createdAtMillis : _importStartTimeMillis);
    }

    private void withModifiedAt(ContentProviderOperation.Builder builder, boolean hasValue, long modifiedAtMillis) {
        builder.withValue(CRUDContentItem.COLUMN_NAME_MODIFIED_AT, hasValue ? modifiedAtMillis : _importStartTimeMillis);
    }

    @Override
    protected void onError(Result result) {
        cleanupProgressDialog();
        super.onError(result);
    }

    @Override
    protected void onSuccess(Result result) {
        cleanupProgressDialog();
        super.onSuccess(result);
    }

    protected void cleanupProgressDialog() {
        if (_pd !=null) {
            _pd.dismiss();
        }
    }

}
