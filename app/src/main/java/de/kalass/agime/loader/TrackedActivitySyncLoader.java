package de.kalass.agime.loader;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import de.kalass.agime.customfield.ActivityCustomFieldDataSyncLoader;
import de.kalass.agime.customfield.ActivityCustomFieldModel;
import de.kalass.agime.model.ActivityTypeModel;
import de.kalass.agime.model.ProjectModel;
import de.kalass.agime.model.TrackedActivityModel;
import de.kalass.agime.provider.MCContract.Activity;
import de.kalass.android.common.simpleloader.CompoundSyncLoader;
import de.kalass.android.common.simpleloader.CursorFkt;
import de.kalass.android.common.simpleloader.CursorUtil;

import static de.kalass.android.common.simpleloader.CursorUtil.getIndex;
import static de.kalass.android.common.simpleloader.CursorUtil.getPrefetched;

/**
 * A Loader that loads TrackedActivityModel instances by combining a query to the trackpoints
 * table with a query to the symptoms table.
 * Created by klas on 22.10.13.
 */
public class TrackedActivitySyncLoader extends CompoundSyncLoader {

    public static final long EARLIEST_START_TIME = Activity.EARLIEST_START_TIME;
    private final ActivityTypeSyncLoader _loader;
    private final ProjectSyncLoader _projectLoader;
    @CheckForNull private final ActivityCustomFieldDataSyncLoader _customFieldDataLoader;

    static final class TrackedActivitiesQuery {
        static final Uri URI = Activity.CONTENT_URI;
        static final String COLUMN_NAME_ID = Activity.COLUMN_NAME_ID;
        static final String COLUMN_NAME_ACTIVITY_TYPE_ID = Activity.COLUMN_NAME_ACTIVITY_TYPE_ID;
        static final String COLUMN_NAME_PROJECT_ID = Activity.COLUMN_NAME_PROJECT_ID;
        static final String COLUMN_NAME_DETAILS = Activity.COLUMN_NAME_DETAILS;
        static final String COLUMN_NAME_START_TIME = Activity.COLUMN_NAME_START_TIME;
        static final String COLUMN_NAME_END_TIME = Activity.COLUMN_NAME_END_TIME;
        static final String COLUMN_NAME_CREATED_AT = Activity.COLUMN_NAME_CREATED_AT;
        static final String COLUMN_NAME_INSERT_DURATION_MILLIS = Activity.COLUMN_NAME_INSERT_DURATION_MILLIS;
        static final String COLUMN_NAME_UPDATE_DURATION_MILLIS = Activity.COLUMN_NAME_UPDATE_DURATION_MILLIS;
        static final String COLUMN_NAME_UPDATE_COUNT = Activity.COLUMN_NAME_UPDATE_COUNT;


        static final String[] PROJECTION = new String[]{
                COLUMN_NAME_ID,
                COLUMN_NAME_ACTIVITY_TYPE_ID,
                COLUMN_NAME_PROJECT_ID,
                COLUMN_NAME_DETAILS,
                COLUMN_NAME_START_TIME,
                COLUMN_NAME_END_TIME,
                COLUMN_NAME_CREATED_AT,
                COLUMN_NAME_INSERT_DURATION_MILLIS,
                COLUMN_NAME_UPDATE_DURATION_MILLIS,
                COLUMN_NAME_UPDATE_COUNT
        };

        static final String ORDER = Activity.COLUMN_NAME_START_TIME + " asc";
        // Column indexes. The index of a column in the Cursor is the same as its relative position in
        // the projection.
        static final int IDX_ACTIVITY_TYPE_ID = getIndex(PROJECTION, COLUMN_NAME_ACTIVITY_TYPE_ID);
        static final int IDX_PROJECT_ID = getIndex(PROJECTION, COLUMN_NAME_PROJECT_ID);
        static final int IDX_DETAILS = getIndex(PROJECTION, COLUMN_NAME_DETAILS);
        static final int IDX_START_TIME = getIndex(PROJECTION, COLUMN_NAME_START_TIME);
        static final int IDX_END_TIME = getIndex(PROJECTION, COLUMN_NAME_END_TIME);
        static final int IDX_CREATED_AT = getIndex(PROJECTION, COLUMN_NAME_CREATED_AT);
        static final int IDX_ID = getIndex(PROJECTION, COLUMN_NAME_ID);
        static final int IDX_INSERT_DURATION_MILLIS = getIndex(PROJECTION, COLUMN_NAME_INSERT_DURATION_MILLIS);
        static final int IDX_UPDATE_DURATION_MILLIS = getIndex(PROJECTION, COLUMN_NAME_UPDATE_DURATION_MILLIS);
        static final int IDX_UPDATE_COUNT = getIndex(PROJECTION, COLUMN_NAME_UPDATE_COUNT);


        static final Function<Cursor, Long> GET_ACTIVITY_TYPE_ID = CursorFkt.newLongGetter(
                IDX_ACTIVITY_TYPE_ID
        );
        static final Function<Cursor, Long> GET_PROJECT_ID = CursorFkt.newLongGetter(
                IDX_PROJECT_ID
        );
    }

    public TrackedActivitySyncLoader(
            Context context
    ) {
        this(context,
                new ActivityTypeSyncLoader(context, new CategorySyncLoader(context)),
                new ProjectSyncLoader(context),
                new ActivityCustomFieldDataSyncLoader(context)
        );
    }

    public TrackedActivitySyncLoader(
            Context context,
            ActivityTypeSyncLoader activityTypeLoader,
            ProjectSyncLoader projectLoader,
            @Nullable ActivityCustomFieldDataSyncLoader customFieldDataLoader
    ) {
        super(context, activityTypeLoader, projectLoader);
        _loader = activityTypeLoader;
        _projectLoader = projectLoader;
        _customFieldDataLoader = customFieldDataLoader == null ? null : add(customFieldDataLoader);
    }

    private static <V, T> Set<T> asSet(Iterable<V> iterable, Function<V, T> fkt) {
        return ImmutableSet.copyOf(Iterables.transform(iterable, fkt));
    }

    /**
     * queries the items that ended before or at the latestEndTimeMillis.
     * @return the previous items in ascending order, i.e. result[0] is the oldest,
     * and result[numItems-1] is the latest one (i.e. the one directly before latestEndTimeMillis)
     */
    public List<TrackedActivityModel> queryPreviousDesc(long latestEndTimeMillis, int numItems) {
        return doQuery(
                Activity.COLUMN_NAME_END_TIME + " <= ? ",
                new String[]{Long.toString(latestEndTimeMillis)},
                Activity.COLUMN_NAME_START_TIME + " desc ", // to make the limit work as intended, do the querying in descending order
                numItems // maximum number of results
        );
    }

    public List<TrackedActivityModel> query( long rangeStarttimeMillisInclusive,
                                             long rangeEndtimeMillisExclusive,
                                             boolean insertFakeEntries) {
        return query(rangeStarttimeMillisInclusive, rangeEndtimeMillisExclusive, insertFakeEntries, TrackedActivitiesQuery.ORDER);
    }

    public List<TrackedActivityModel> getByIds( long[] ids) {
        List<Long> idList = new ArrayList<Long>();
        for (long id : ids) {
            idList.add(id);
        }
        return getByIds(idList);
    }

    public List<TrackedActivityModel> getByIds( Iterable<Long> ids) {
        return getByIds(ids, TrackedActivitiesQuery.ORDER);
    }

    public List<TrackedActivityModel> getByIds( Iterable<Long> ids,
                                                String order) {

        if (!ids.iterator().hasNext()) {
            return ImmutableList.of();
        }
        return doQuery(
                Activity.COLUMN_NAME_ID + " IN (" + Joiner.on(',').join(ids) + ")",
                null,
                order,
                CursorUtil.MAX_RESULTS_UNLIMITED // unlimited
        );
    }

    public List<TrackedActivityModel> query( long rangeStarttimeMillisInclusive,
                                             long rangeEndtimeMillisExclusive,
                                             boolean insertFakeEntries,
                                             String order) {
        List<TrackedActivityModel> dbResult = doQuery(rangeStarttimeMillisInclusive, rangeEndtimeMillisExclusive, order);
        if (!insertFakeEntries) {
            return dbResult;
        }
        ArrayList<TrackedActivityModel> result = new ArrayList<TrackedActivityModel>(dbResult.size());
        // fill in empty models for missing entries
        int numModels = dbResult.size();
        for (int i = 0; i < numModels; i++) {
            TrackedActivityModel model = dbResult.get(i);
            TrackedActivityModel next = (numModels-1) > i ?  dbResult.get(i + 1) : null;
            result.add(model);
            DateTime modelEndtimeMinutes = model.getEndtimeDateTimeMinutes();
            DateTime nextStarttimeMinutes = next == null ? null : next.getStarttimeDateTimeMinutes();
            if (nextStarttimeMinutes != null && !modelEndtimeMinutes.equals(nextStarttimeMinutes) && modelEndtimeMinutes.isBefore(nextStarttimeMinutes)) {
                // add a pseudo-entry
                result.add(TrackedActivityModel.fakeInBetween(
                        - model.getId(),
                        modelEndtimeMinutes.getMillis(),
                        nextStarttimeMinutes.getMillis()
                ));
            }
        }
        return result;
    }
    private List<TrackedActivityModel> doQuery( long rangeStarttimeMillisInclusive,
                                                long rangeEndtimeMillisExclusive,
                                                String order) {
        return doQuery(
            Activity.COLUMN_NAME_START_TIME + " < ? AND " + Activity.COLUMN_NAME_END_TIME + " >= ?",
            new String[]{Long.toString(rangeEndtimeMillisExclusive), Long.toString(rangeStarttimeMillisInclusive)},
            order,
            CursorUtil.MAX_RESULTS_UNLIMITED // unlimited
        );
    }
    private List<TrackedActivityModel> doQuery(
            String selection, String[] selectionArgs, String order, int maxResults
    ) {
        final Cursor trackedActivityCursor = queryUnmanaged(
                TrackedActivitiesQuery.URI,
                TrackedActivitiesQuery.PROJECTION,
                //(starttime_at < day_endtime AND endtime_at >= day_starttime)
                selection,
                selectionArgs,
                order
        );
        if (trackedActivityCursor == null) {
            return ImmutableList.of();
        }
        try {
            if (trackedActivityCursor.getCount() == 0) {
                return ImmutableList.of();
            }

            List<Long> trackedActivities = CursorUtil.readList(trackedActivityCursor, CursorFkt.newLongGetter(TrackedActivitiesQuery.IDX_ID));

            final Map<Long, List<ActivityCustomFieldModel>> customFieldData = _customFieldDataLoader == null
                    ? ImmutableMap.<Long, List<ActivityCustomFieldModel>>of()
                    : _customFieldDataLoader.loadExisting(trackedActivities);

            final Map<Long, ActivityTypeModel> activityTypes = _loader.loadAllFromCursor(
                    trackedActivityCursor, TrackedActivitiesQuery.IDX_ACTIVITY_TYPE_ID
            );
            final Map<Long, ProjectModel> projects = _projectLoader.loadAllFromCursor(
                    trackedActivityCursor, TrackedActivitiesQuery.IDX_PROJECT_ID
            );

            return CursorUtil.readList(trackedActivityCursor, maxResults, new Function<Cursor, TrackedActivityModel> () {
                @Override
                public TrackedActivityModel apply(Cursor cursor) {
                    final ActivityTypeModel activityType = getPrefetched(
                            cursor, TrackedActivitiesQuery.IDX_ACTIVITY_TYPE_ID, activityTypes
                    );
                    final ProjectModel project = getPrefetched(
                            cursor, TrackedActivitiesQuery.IDX_PROJECT_ID, projects
                    );
                    long id = cursor.getLong(TrackedActivitiesQuery.IDX_ID);
                    long starttimeMillis = cursor.getLong(TrackedActivitiesQuery.IDX_START_TIME);
                    long endtimeMillis = cursor.getLong(TrackedActivitiesQuery.IDX_END_TIME);
                    long createdAt = cursor.getLong(TrackedActivitiesQuery.IDX_CREATED_AT);
                    Long insertDurationMillis = CursorUtil.getLong(cursor, TrackedActivitiesQuery.IDX_INSERT_DURATION_MILLIS);
                    Long updateDurationMillis = CursorUtil.getLong(cursor, TrackedActivitiesQuery.IDX_UPDATE_DURATION_MILLIS);
                    Long updateCount = CursorUtil.getLong(cursor, TrackedActivitiesQuery.IDX_UPDATE_COUNT);
                    String details = cursor.getString(TrackedActivitiesQuery.IDX_DETAILS);
                    final List<ActivityCustomFieldModel> list = customFieldData.get(id);
                    return TrackedActivityModel.real(
                            id,
                            details,
                            activityType,
                            project,
                            createdAt,
                            starttimeMillis,
                            endtimeMillis,
                            insertDurationMillis, updateDurationMillis, updateCount,
                            list == null ? ImmutableList.<ActivityCustomFieldModel>of() : list
                    );
                }
            });
        } finally {
            trackedActivityCursor.close();
        }
    }
}
