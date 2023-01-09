package de.kalass.agime.customfield;

import android.content.Context;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.kalass.agime.customfield.ActivityCustomFieldEditorDataSyncLoader.ActivityCustomFieldValuesQuery;
import de.kalass.agime.customfield.ActivityCustomFieldEditorDataSyncLoader.CustomFieldValuesQuery;
import de.kalass.android.common.simpleloader.SyncLoader;

/**
 * loads the available Custom Fields as well as the corresponding value for the given activity (if available).
 *
 * Created by klas on 09.01.14.
 */
public class ActivityCustomFieldDataSyncLoader extends SyncLoader {


    public ActivityCustomFieldDataSyncLoader(Context context) {
        super(context);
    }


    /**
     * Load only existing associations for existing tracked activities
     */
    public Map<Long, List<ActivityCustomFieldModel>> loadExisting(Collection<Long> trackedActivityIds) {
        final List<CustomFieldTypeModel> types = loadAvailableTypes();
        if (types.isEmpty()) {
            return ImmutableMap.of();
        }
        Map<Long, CustomFieldTypeModel> typeById = Maps.uniqueIndex(types, CustomFieldTypeModel.GET_ID);

        final List<ActivityCustomFieldValuesQuery.Data> activityCustomFieldValues = loadActivityCustomFieldValues(trackedActivityIds);
        final Multimap<Long, ActivityCustomFieldValuesQuery.Data> activityCustomFieldByValueId = Multimaps.index(activityCustomFieldValues, ActivityCustomFieldValuesQuery.Data.GET_VALUE_ID);
        final List<CustomFieldValueModel> customFieldValues = loadCustomFieldValues(activityCustomFieldByValueId.keys());

        final Map<Long, CustomFieldValueModel> customFieldValuesByValueId = Maps.uniqueIndex(customFieldValues, CustomFieldValueModel.GET_ID);

        final Map<Long, List<ActivityCustomFieldModel>> result = new HashMap<Long, List<ActivityCustomFieldModel>>(trackedActivityIds.size());
        for (ActivityCustomFieldValuesQuery.Data assoc: activityCustomFieldValues) {
            CustomFieldValueModel customFieldValue = Preconditions.checkNotNull(customFieldValuesByValueId.get(assoc.customFieldValueId));
            CustomFieldTypeModel customFieldType = typeById.get(customFieldValue.getTypeId());
            List<ActivityCustomFieldModel> list = result.get(assoc.trackedActivityId);
            if (list == null) {
                list = new ArrayList<ActivityCustomFieldModel>(types.size());
                result.put(assoc.trackedActivityId, list);
            }
            list.add(new ActivityCustomFieldModel(customFieldType, assoc.id, customFieldValue));
        }
        return result;
    }

    private List<CustomFieldTypeModel> loadAvailableTypes() {
        return loadList(
                CustomFieldTypeModelQuery.READ,
                CustomFieldTypeModelQuery.CONTENT_URI, CustomFieldTypeModelQuery.PROJECTION);
    }

    private List<ActivityCustomFieldValuesQuery.Data> loadActivityCustomFieldValues(Iterable<Long> ids) {
        if (ids == null || Iterables.isEmpty(ids)) {
            return ImmutableList.of();
        }
        return loadList(
                ActivityCustomFieldValuesQuery.READ,
                ActivityCustomFieldValuesQuery.CONTENT_URI,
                ActivityCustomFieldValuesQuery.PROJECTION,
                ActivityCustomFieldValuesQuery.getTrackedActivitiesSelection(ids),
                ActivityCustomFieldValuesQuery.getTrackedActivitiesSelectionArgs(),
                null);
    }

    private List<CustomFieldValueModel> loadCustomFieldValues(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ImmutableList.of();
        }
        return loadList(
                CustomFieldValuesQuery.READ,
                CustomFieldValuesQuery.CONTENT_URI,
                CustomFieldValuesQuery.PROJECTION,
                CustomFieldValuesQuery.getByIdsSelection(ids),
                CustomFieldValuesQuery.getByIdsSelectionArgs(),
                null);
    }

}
