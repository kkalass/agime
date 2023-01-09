package de.kalass.agime.loader;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.kalass.agime.customfield.ActivityCustomFieldDataSyncLoader;
import de.kalass.agime.customfield.ActivityCustomFieldModel;
import de.kalass.agime.model.ActivityTypeModel;
import de.kalass.agime.model.ActivityTypeSuggestionModel;
import de.kalass.agime.model.ProjectModel;
import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.simpleloader.CompoundSyncLoader;
import de.kalass.android.common.simpleloader.CursorFkt;
import de.kalass.android.common.simpleloader.CursorUtil;

import static de.kalass.android.common.simpleloader.CursorUtil.getIndex;
import static de.kalass.android.common.simpleloader.CursorUtil.getPrefetched;

/**
 * Created by klas on 22.10.13.
 */
public class ActivityTypeSuggestionSyncLoader extends CompoundSyncLoader {

    private final ProjectSyncLoader _projectLoader;
    private final ActivityTypeSyncLoader _activityTypeLoader;
    private final ActivityCustomFieldDataSyncLoader _customFieldDataLoader;


    static final class TrackedActivityQuery {
        static final Uri URI_SUGGESTION = MCContract.Activity.CONTENT_URI_SUGGESTION;
        /**
         * Suggestion is a group by, and we always want to have the "latest" Activity item of the group
         */
        static final String COLUMN_NAME_ID = "max(" + MCContract.Activity.COLUMN_NAME_ID + ")";
        static final String COLUMN_NAME_ACTIVITY_TYPE_ID = MCContract.Activity.COLUMN_NAME_ACTIVITY_TYPE_ID;
        static final String COLUMN_NAME_PROJECT_ID = MCContract.Activity.COLUMN_NAME_PROJECT_ID;
        static final String[] PROJECTION = new String[] {
                COLUMN_NAME_ID,
                COLUMN_NAME_ACTIVITY_TYPE_ID,
                COLUMN_NAME_PROJECT_ID
        };

        static final int COLUMN_IDX_ID = getIndex(PROJECTION, COLUMN_NAME_ID);
        static final int COLUMN_IDX_ACTIVITY_TYPE_ID = getIndex(PROJECTION, COLUMN_NAME_ACTIVITY_TYPE_ID);
        static final int COLUMN_IDX_PROJECT_ID = getIndex(PROJECTION, COLUMN_NAME_PROJECT_ID);

        static final Function<Cursor, Long> READ_ID = CursorFkt.newLongGetter(TrackedActivityQuery.COLUMN_IDX_ID);
        public static final String ORDERING = "max(" + BaseColumns._ID + ") desc";

        public static String selection(Collection<Long> candidateActivityIds, Iterable<Long> projectIds, Iterable<Long> limitToProjectIds) {
            List<String> selection = new ArrayList<String>(2);
            if (!candidateActivityIds.isEmpty()) {
                selection.add(TrackedActivityQuery.COLUMN_NAME_ACTIVITY_TYPE_ID + " in (" + Joiner.on(",").join(candidateActivityIds) + ")");
            }
            if (!Iterables.isEmpty(projectIds)) {
                selection.add(TrackedActivityQuery.COLUMN_NAME_PROJECT_ID + " in (" + Joiner.on(",").join(projectIds) + ")");
            }

            final String limitToProjectIdsString;
            if (limitToProjectIds == null) {
                limitToProjectIdsString = "";
            } else {
                limitToProjectIdsString = " AND ( " + TrackedActivityQuery.COLUMN_NAME_PROJECT_ID + " IS NULL "
                        + (Iterables.isEmpty(limitToProjectIds) ? "" : " OR " + TrackedActivityQuery.COLUMN_NAME_PROJECT_ID + " IN (" + Joiner.on(",").join(limitToProjectIds) + ")") + ")";
            }
            String matchSelection = selection.size() > 1 ?  "("  + Joiner.on(" OR ").join(selection) + ") AND " : (selection.size() == 1 ? selection.get(0) + " AND " : "");
            return matchSelection + COLUMN_NAME_ACTIVITY_TYPE_ID + " IS NOT NULL " + limitToProjectIdsString;
        }

        public static String[] selectionArgs() {
            return null;
        }
    }

    public ActivityTypeSuggestionSyncLoader(Context context) {
        this(context, new ProjectSyncLoader(context), new ActivityTypeSyncLoader(context, new CategorySyncLoader(context)));
    }

    public ActivityTypeSuggestionSyncLoader(Context context, ProjectSyncLoader projectSyncLoader, ActivityTypeSyncLoader activityTypeSyncLoader) {
        super(context, projectSyncLoader, activityTypeSyncLoader);
        this._projectLoader = projectSyncLoader;
        this._activityTypeLoader = activityTypeSyncLoader;
        this._customFieldDataLoader = add(new ActivityCustomFieldDataSyncLoader(context));
    }

    private List<ActivityTypeSuggestionModel> candidatesAsSuggestions(Map<Long, ActivityTypeModel> remainingActivities) {
        if (remainingActivities.isEmpty()) {
            return ImmutableList.of();
        }
        final List<ActivityTypeSuggestionModel> result = new ArrayList<ActivityTypeSuggestionModel>(remainingActivities.size());
        for (ActivityTypeModel r: remainingActivities.values()) {
            result.add(ActivityTypeSuggestionModel.forExisting(r, null, ImmutableList.<ActivityCustomFieldModel>of()));
        }
        return result;
    }

    public List<ActivityTypeSuggestionModel> queryAll(
            final Map<Long, ActivityTypeModel> candidateActivities,
            final Iterable<Long> projectIds,
            final LocalDate refDate,
            Set<ActivityTypeSuggestionFeature> features
    ) {
        if (candidateActivities.isEmpty() && Iterables.isEmpty(projectIds)) {
            return ImmutableList.of();
        }
        final List<Long> limitToProjectIds;
        if (features.contains(ActivityTypeSuggestionFeature.SUGGEST_ALL_PROJECTS)) {
            limitToProjectIds = null;
        } else {
            final List<ProjectModel> limitToProjectModels = _projectLoader.load(
                    MCContract.Project.COLUMN_NAME_ACTIVE_UNTIL_MILLIS + " IS NULL OR " + MCContract.Project.COLUMN_NAME_ACTIVE_UNTIL_MILLIS + " >= ? ",
                    new String[]{refDate.toDateTimeAtStartOfDay().getMillis() + ""},
                    MCContract.Project.COLUMN_NAME_NAME + " asc"
            );
            limitToProjectIds = Lists.transform(limitToProjectModels, ProjectModel.GET_ID);
        }
        final Cursor cursor = queryUnmanaged(
                TrackedActivityQuery.URI_SUGGESTION,
                TrackedActivityQuery.PROJECTION,
                TrackedActivityQuery.selection(candidateActivities.keySet(), projectIds, limitToProjectIds),
                TrackedActivityQuery.selectionArgs(),
                TrackedActivityQuery.ORDERING //
        );
        if (cursor == null) {
            return candidatesAsSuggestions(candidateActivities);
        }
        try {
            if (cursor.getCount() == 0) {
                return candidatesAsSuggestions(candidateActivities);
            }

            final Map<Long, ProjectModel> projects =  _projectLoader.loadAllFromCursor(
                    cursor, TrackedActivityQuery.COLUMN_IDX_PROJECT_ID
            );

            final Map<Long, ActivityTypeModel> activities = Iterables.isEmpty(projectIds)
                    ? candidateActivities
                    : _activityTypeLoader.loadAllFromCursor(cursor, TrackedActivityQuery.COLUMN_IDX_ACTIVITY_TYPE_ID);

            Set<Long> trackedActivityIds = CursorUtil.readSet(cursor, TrackedActivityQuery.READ_ID);

            final Map<Long, List<ActivityCustomFieldModel>> customFields = _customFieldDataLoader.loadExisting(trackedActivityIds);

            // make sure that all matching activities are included in the suggestion result, by appending
            // them
            final Map<Long, ActivityTypeModel> remainingActivities = new HashMap<Long, ActivityTypeModel>(activities.size());
            remainingActivities.putAll(activities);

            List<ActivityTypeSuggestionModel> cResult = CursorUtil.readList(cursor, new Function<Cursor, ActivityTypeSuggestionModel>() {
                @Override
                public ActivityTypeSuggestionModel apply(Cursor cursor) {
                    final ProjectModel project = getPrefetched(
                            cursor, TrackedActivityQuery.COLUMN_IDX_PROJECT_ID, projects
                    );

                    final ActivityTypeModel activity = getPrefetched(
                            cursor, TrackedActivityQuery.COLUMN_IDX_ACTIVITY_TYPE_ID, activities
                    );

                    if (activity == null) {

                        throw new IllegalStateException("Candidates: " + activities);
                    }
                    remainingActivities.remove(activity.getId());
                    final List<ActivityCustomFieldModel> customFieldModels = customFields.get(cursor.getLong(TrackedActivityQuery.COLUMN_IDX_ID));
                    return ActivityTypeSuggestionModel.forExisting(activity, project, customFieldModels);
                }
            });

            if (!features.contains(ActivityTypeSuggestionFeature.SUGGEST_ALL_ACTIVITIES) || remainingActivities.isEmpty()) {
                return cResult;
            }
            final List<ActivityTypeSuggestionModel> result = new ArrayList<ActivityTypeSuggestionModel>(cResult.size() + remainingActivities.size());
            result.addAll(cResult);
            result.addAll(candidatesAsSuggestions(remainingActivities));
            return result;
        } finally {
            cursor.close();
        }

    }


}
