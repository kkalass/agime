package de.kalass.agime.customfield;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.simpleloader.CursorUtil;
import de.kalass.android.common.simpleloader.SyncLoader;

/**
 * loads the available Custom Fields as well as the corresponding value for the given activity (if available).
 *
 * Created by klas on 09.01.14.
 */
public class ActivityCustomFieldEditorDataSyncLoader extends SyncLoader {

    public ActivityCustomFieldEditorDataSyncLoader(Context context) {
        super(context);
    }

    /**
     * @param trackedActivityId may be null if the data is needed for a freshly created activity
     * @return an instance of ActivityCustomFieldEditorModel for each custom field type
     */
    public List<ActivityCustomFieldEditorModel> load(Long trackedActivityId) {
        final List<CustomFieldTypeModel> types = loadAvailableTypes();
        if (types.isEmpty()) {
            return ImmutableList.of();
        }
        final Multimap<Long, Long> enabledProjectsByCustomType = loadEnabledProjectsByCustomType();

        final List<ActivityCustomFieldValuesQuery.Data> activityCustomFieldValues = loadActivityCustomFieldValues(trackedActivityId);
        final Multimap<Long, ActivityCustomFieldValuesQuery.Data> activityCustomFieldByValueId = Multimaps.index(activityCustomFieldValues, ActivityCustomFieldValuesQuery.Data.GET_VALUE_ID);
        final List<CustomFieldValueModel> customFieldValues = loadCustomFieldValues(activityCustomFieldByValueId.keys());

        final Multimap<Long, CustomFieldValueModel> customFieldValuesByTypeId = Multimaps.index(customFieldValues, CustomFieldValueModel.GET_TYPE_ID);

        final ArrayList<ActivityCustomFieldEditorModel> result = new ArrayList<ActivityCustomFieldEditorModel>(types.size());
        for (CustomFieldTypeModel customFieldType: types) {
            final Collection<CustomFieldValueModel> values = customFieldValuesByTypeId.get(customFieldType.getId());
            CustomFieldValueModel valueData = (values == null || values.isEmpty()) ? null : values.iterator().next();
            Collection<ActivityCustomFieldValuesQuery.Data> candidates = valueData == null ? null : activityCustomFieldByValueId.get(valueData.getId());
            ActivityCustomFieldValuesQuery.Data associationData = (candidates == null || candidates.isEmpty()) ? null : candidates.iterator().next();
            final Collection<Long> enabledProjects = enabledProjectsByCustomType.get(customFieldType.getId());
            result.add(new ActivityCustomFieldEditorModel(
                    customFieldType,
                    associationData == null ? null : associationData.id,
                    customFieldType.isAnyProject(),
                    enabledProjects == null ? ImmutableSet.<Long>of() : enabledProjects,
                    valueData));
        }
        return result;
    }

    private Multimap<Long, Long> loadEnabledProjectsByCustomType() {
        return loadMultimap(
                CustomFieldTypeProjectsQuery.READ_TYPE_ID,
                CustomFieldTypeProjectsQuery.READ_PROJECT_ID,
                CustomFieldTypeProjectsQuery.CONTENT_URI, CustomFieldTypeProjectsQuery.PROJECTION,
                null, null, null
        );
    }

    private List<CustomFieldTypeModel> loadAvailableTypes() {
        return loadList(
                CustomFieldTypeModelQuery.READ,
                CustomFieldTypeModelQuery.CONTENT_URI, CustomFieldTypeModelQuery.PROJECTION);
    }

    private List<ActivityCustomFieldValuesQuery.Data> loadActivityCustomFieldValues(Long trackedActivityId) {
        if (trackedActivityId == null) {
            return ImmutableList.of();
        }
        return loadList(
                ActivityCustomFieldValuesQuery.READ,
                ActivityCustomFieldValuesQuery.CONTENT_URI,
                ActivityCustomFieldValuesQuery.PROJECTION,
                ActivityCustomFieldValuesQuery.getTrackedActivitiesSelection(ImmutableList.of(trackedActivityId)),
                ActivityCustomFieldValuesQuery.getTrackedActivitiesSelectionArgs(),
                null);
    }

    private List<CustomFieldValueModel> loadCustomFieldValues(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ImmutableList.of();
        }
        return loadList(
                CustomFieldValuesQuery.READ,
                CustomFieldValuesQuery.CONTENT_URI,                CustomFieldValuesQuery.PROJECTION,
                CustomFieldValuesQuery.getByIdsSelection(ids),
                CustomFieldValuesQuery.getByIdsSelectionArgs(),
                null);
    }


    static final class ActivityCustomFieldValuesQuery {
        static final Uri CONTENT_URI = MCContract.ActivityCustomFieldValue.CONTENT_URI;
        static final String COLUMN_NAME_ID = MCContract.ActivityCustomFieldValue._ID;
        static final String COLUMN_NAME_CUSTOM_FIELD_VALUE_ID = MCContract.ActivityCustomFieldValue.COLUMN_NAME_CUSTOM_FIELD_VALUE_ID;
        static final String COLUMN_NAME_TRACKED_ACTIVITY_ID = MCContract.ActivityCustomFieldValue.COLUMN_NAME_TRACKED_ACTIVITY_ID;
        static final String[] PROJECTION = new String[] {
                COLUMN_NAME_ID,
                COLUMN_NAME_CUSTOM_FIELD_VALUE_ID,
                COLUMN_NAME_TRACKED_ACTIVITY_ID
        };
        static final int IDX_ID = CursorUtil.getIndex(PROJECTION, COLUMN_NAME_ID);
        static final int IDX_CUSTOM_FIELD_VALUE_ID = CursorUtil.getIndex(PROJECTION, COLUMN_NAME_CUSTOM_FIELD_VALUE_ID);
        static final int IDX_TRACKED_ACTIVITY_ID = CursorUtil.getIndex(PROJECTION, COLUMN_NAME_TRACKED_ACTIVITY_ID);

        static final String getTrackedActivitiesSelection(Iterable<Long> trackedActivityIds) {
            return COLUMN_NAME_TRACKED_ACTIVITY_ID + " in ( " + Joiner.on(',').join(trackedActivityIds) + ")";
        }
        static final String[] getTrackedActivitiesSelectionArgs() {
            return null;
        }

        static final class Data {
            final long id;
            final long customFieldValueId;
            final long trackedActivityId;

            static final Function<Data, Long> GET_VALUE_ID = new Function<Data, Long> () {
                @Override
                public Long apply(Data data) {
                    return data.customFieldValueId;
                }
            };

            Data(Cursor cursor) {
                id = cursor.getLong(IDX_ID);
                customFieldValueId = cursor.getLong(IDX_CUSTOM_FIELD_VALUE_ID);
                trackedActivityId = cursor.getLong(IDX_TRACKED_ACTIVITY_ID);
            }
        }

        static final Function<Cursor, Data> READ = new Function<Cursor, Data>() {
            @Override
            public Data apply(Cursor cursor) {
                return new Data(cursor);
            }
        };
    }

    static final class CustomFieldValuesQuery {
        static final Uri CONTENT_URI = MCContract.CustomFieldValue.CONTENT_URI;
        static final String COLUMN_NAME_ID = MCContract.CustomFieldValue._ID;
        static final String COLUMN_NAME_VALUE = MCContract.CustomFieldValue.COLUMN_NAME_VALUE;
        static final String COLUMN_NAME_TYPE_ID = MCContract.CustomFieldValue.COLUMN_NAME_CUSTOM_FIELD_TYPE_ID;
        static final String[] PROJECTION = new String[] {
                COLUMN_NAME_ID,
                COLUMN_NAME_VALUE,
                COLUMN_NAME_TYPE_ID
        };
        static final int IDX_ID = CursorUtil.getIndex(PROJECTION, COLUMN_NAME_ID);
        static final int IDX_VALUE = CursorUtil.getIndex(PROJECTION, COLUMN_NAME_VALUE);
        static final int IDX_TYPE_ID = CursorUtil.getIndex(PROJECTION, COLUMN_NAME_TYPE_ID);

        static final Function<Cursor, CustomFieldValueModel> READ = new Function<Cursor, CustomFieldValueModel>() {
            @Override
            public CustomFieldValueModel apply(Cursor cursor) {
                long id = cursor.getLong(IDX_ID);
                long typeId = cursor.getLong(IDX_TYPE_ID);
                String value = cursor.getString(IDX_VALUE);
                return new CustomFieldValueModel(id, typeId, value);
            }
        };

        static final String getByIdsSelection(Iterable<Long> ids) {
            return COLUMN_NAME_ID + " in (" + Joiner.on(',').join(ids) + ")";
        }

        static final String[] getByIdsSelectionArgs() {
            return null;
        }
    }

}
