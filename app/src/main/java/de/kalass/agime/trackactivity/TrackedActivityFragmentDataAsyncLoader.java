package de.kalass.agime.trackactivity;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.util.List;

import de.kalass.agime.acquisitiontime.RecurringDAO;
import de.kalass.agime.customfield.ActivityCustomFieldEditorDataSyncLoader;
import de.kalass.agime.customfield.ActivityCustomFieldEditorModel;
import de.kalass.agime.loader.ActivityTypeSyncLoader;
import de.kalass.agime.loader.CategorySyncLoader;
import de.kalass.agime.loader.ProjectModelQuery;
import de.kalass.agime.project.ProjectEditorFragment;
import de.kalass.agime.provider.MCContract;
import de.kalass.agime.timesuggestions.TimeSuggestionSyncLoader;
import de.kalass.agime.timesuggestions.TimeSuggestions;
import de.kalass.android.common.activity.CRUDMode;
import de.kalass.android.common.simpleloader.CompoundAsyncLoader;
import de.kalass.android.common.simpleloader.CursorUtil;
import de.kalass.android.common.simpleloader.ObserveDataSourceMode;

import static com.google.common.base.Preconditions.checkArgument;
import static de.kalass.android.common.simpleloader.CursorUtil.getIndex;

/**
* Created by klas on 21.01.14.
*/
final class TrackedActivityFragmentDataAsyncLoader extends CompoundAsyncLoader<TrackedActivityFragmentData> {

    private final CRUDMode _mode;
    private final Long _entityId;
    private final Uri _uri;
    private final LocalDate _date;
    private final Bundle _args;
    private final TimeSuggestionSyncLoader _timeSuggestionLoader;
    private final ActivityCustomFieldEditorDataSyncLoader _customFieldLoader;
    private final ActivityTypeSyncLoader _activityTypeLoader;
    private final RecurringSyncLoader _recurringSyncLoader;


    static class TrackedActivityQuery {
        public static final String COLUMN_NAME_ID = MCContract.Activity.COLUMN_NAME_ID;
        public static final String COLUMN_NAME_START_TIME = MCContract.Activity.COLUMN_NAME_START_TIME;
        public static final String COLUMN_NAME_END_TIME = MCContract.Activity.COLUMN_NAME_END_TIME;
        public static final String COLUMN_NAME_PROJECT_ID = MCContract.Activity.COLUMN_NAME_PROJECT_ID;
        public static final String COLUMN_NAME_DETAILS = MCContract.Activity.COLUMN_NAME_DETAILS;
        public static final String COLUMN_NAME_MODIFIED_AT = MCContract.Activity.COLUMN_NAME_MODIFIED_AT;
        public static final String COLUMN_NAME_ACTIVITY_TYPE_ID = MCContract.Activity.COLUMN_NAME_ACTIVITY_TYPE_ID;
        public static final String COLUMN_NAME_INSERT_DURATION_MILLIS = MCContract.Activity.COLUMN_NAME_INSERT_DURATION_MILLIS;
        public static final String COLUMN_NAME_UPDATE_DURATION_MILLIS = MCContract.Activity.COLUMN_NAME_UPDATE_DURATION_MILLIS;
        public static final String COLUMN_NAME_UPDATE_COUNT = MCContract.Activity.COLUMN_NAME_UPDATE_COUNT;

        public static final String[] PROJECTION = new String[]{
                COLUMN_NAME_ID,
                COLUMN_NAME_START_TIME,
                COLUMN_NAME_END_TIME,
                COLUMN_NAME_PROJECT_ID,
                COLUMN_NAME_DETAILS,
                COLUMN_NAME_MODIFIED_AT,
                COLUMN_NAME_ACTIVITY_TYPE_ID,
                COLUMN_NAME_INSERT_DURATION_MILLIS,
                COLUMN_NAME_UPDATE_DURATION_MILLIS,
                COLUMN_NAME_UPDATE_COUNT
        };
        public static final int IDX_ID = getIndex(PROJECTION, COLUMN_NAME_ID);
        public static final int IDX_START_TIME = getIndex(PROJECTION, COLUMN_NAME_START_TIME);
        public static final int IDX_END_TIME = getIndex(PROJECTION, COLUMN_NAME_END_TIME);
        public static final int IDX_PROJECT_ID = getIndex(PROJECTION, COLUMN_NAME_PROJECT_ID);
        public static final int IDX_DETAILS = getIndex(PROJECTION, COLUMN_NAME_DETAILS);
        public static final int IDX_ACTIVITY_TYPE_ID = getIndex(PROJECTION, COLUMN_NAME_ACTIVITY_TYPE_ID);
        public static final int IDX_INSERT_DURATION_MILLIS = getIndex(PROJECTION, COLUMN_NAME_INSERT_DURATION_MILLIS);
        public static final int IDX_UPDATE_DURATION_MILLIS = getIndex(PROJECTION, COLUMN_NAME_UPDATE_DURATION_MILLIS);
        public static final int IDX_UPDATE_COUNT = getIndex(PROJECTION, COLUMN_NAME_UPDATE_COUNT);
    }
    
    public TrackedActivityFragmentDataAsyncLoader(
            Context context, CRUDMode mode, Long entityId, Bundle args, Uri uri, ObserveDataSourceMode observeDataSource
    ) {
        super(context, observeDataSource);

        _timeSuggestionLoader = add(new TimeSuggestionSyncLoader(context));
        _customFieldLoader = add(new ActivityCustomFieldEditorDataSyncLoader(context));
        _activityTypeLoader = add(new ActivityTypeSyncLoader(context, new CategorySyncLoader(context)));
        _recurringSyncLoader = add(new RecurringSyncLoader(getContext()));
        _mode = mode;
        _entityId = entityId;
        _uri = uri;
        _args = args == null ? new Bundle() : args;
        _date = new DateTime(_args.getLong(TrackedActivityFragment.EXTRA_DAY_MILLIS, System.currentTimeMillis())).toLocalDate();
    }

    @Override
    public TrackedActivityFragmentData doLoadInBackground() {
        List<RecurringDAO.Data> acquisitionTimes = _recurringSyncLoader.loadRecurring();

        final int newProjectColor = ProjectEditorFragment.loadNewProjectColor(getContext());
        if (_mode == CRUDMode.INSERT) {
            final TimeSuggestions timeSuggestions = _timeSuggestionLoader.load(_date);
            final List<ActivityCustomFieldEditorModel> customFields = _customFieldLoader.load(_entityId);
            final TrackedActivityFragmentData result = new TrackedActivityFragmentData(
                    _entityId, customFields, acquisitionTimes, newProjectColor,
                    // no insert or update happened yet
                    null, null, null);
            result.setDate(_date, timeSuggestions);

            // a new entry shall be created - take values into consideration that were passed as arguments
            if (_args.containsKey(TrackedActivityFragment.EXTRA_STARTTIME_MILLIS) && _args.containsKey(TrackedActivityFragment.EXTRA_ENDTIME_MILLIS)) {
                result.setTimes(new LocalTime(_args.getLong(TrackedActivityFragment.EXTRA_STARTTIME_MILLIS)), new LocalTime(_args.getLong(TrackedActivityFragment.EXTRA_ENDTIME_MILLIS)));
            }

            return result;
        }

        final Cursor cursor = queryUnmanaged(_uri, TrackedActivityQuery.PROJECTION, null, null, null);
        try {
            cursor.moveToFirst();
            final long startTimeMillis = cursor.getLong(TrackedActivityQuery.IDX_START_TIME);
            final LocalDate date = new LocalDate(startTimeMillis);
            final LocalTime startTime = new LocalTime(startTimeMillis);
            final LocalTime endTime = new LocalTime(cursor.getLong(TrackedActivityQuery.IDX_END_TIME));
            final String details = cursor.isNull(TrackedActivityQuery.IDX_DETAILS) ? null : cursor.getString(TrackedActivityQuery.IDX_DETAILS);

            checkArgument(_entityId.longValue() == cursor.getLong(TrackedActivityQuery.IDX_ID));

            final TimeSuggestions timeSuggestions = _timeSuggestionLoader.load(date, acquisitionTimes);
            final List<ActivityCustomFieldEditorModel> customFields = _customFieldLoader.load(_entityId);
            Long originalInsertDurationMillis = CursorUtil.getLong(cursor, TrackedActivityQuery.IDX_INSERT_DURATION_MILLIS);
            Long originalUpdateDurationMillis = CursorUtil.getLong(cursor, TrackedActivityQuery.IDX_UPDATE_DURATION_MILLIS);
            Long originalUpdateCount = CursorUtil.getLong(cursor, TrackedActivityQuery.IDX_UPDATE_COUNT);

            final TrackedActivityFragmentData result = new TrackedActivityFragmentData(
                    _entityId, customFields, acquisitionTimes, newProjectColor,
                    originalInsertDurationMillis, originalUpdateDurationMillis, originalUpdateCount
            );

            if (!cursor.isNull(TrackedActivityQuery.IDX_PROJECT_ID)) {
                final long projectId = cursor.getLong(TrackedActivityQuery.IDX_PROJECT_ID);
                result.setSelectedProject(loadById(ProjectModelQuery.READER, ProjectModelQuery.URI, ProjectModelQuery.PROJECTION, projectId));
            }

            if (!cursor.isNull(TrackedActivityQuery.IDX_ACTIVITY_TYPE_ID)) {
                result.setSelectedActivityTypeAndCategory(_activityTypeLoader.load(cursor.getLong(TrackedActivityQuery.IDX_ACTIVITY_TYPE_ID)));
            }

            result.setTimes(startTime, endTime);
            result.setDate(date, timeSuggestions);

            result.setDetails(details);
            return result;
        } finally {
            cursor.close();
        }
    }

}
