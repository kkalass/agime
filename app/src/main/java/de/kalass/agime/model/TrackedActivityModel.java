package de.kalass.agime.model;

import android.content.Context;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.kalass.agime.R;
import de.kalass.agime.customfield.ActivityCustomFieldModel;
import de.kalass.agime.customfield.CustomFieldTypeModel;
import de.kalass.android.common.model.IViewModel;
import de.kalass.android.common.util.StringUtil;

/**
 * View Model for activity data.
 * Created by klas on 21.10.13.
 */
public class TrackedActivityModel extends TimeSpanning implements IViewModel {

    public static final Function<TrackedActivityModel, ProjectModel> GET_PROJECT = new Function<TrackedActivityModel, ProjectModel>() {
        @Override
        public ProjectModel apply(TrackedActivityModel input) {
            return input == null ? null : input.getProject();
        }
    };

    public static final Function<TrackedActivityModel, Long> GET_PROJECT_ID = Functions.compose(ProjectModel.GET_ID, GET_PROJECT);

    public static final Function<TrackedActivityModel, ActivityTypeModel> GET_ACTIVITY_TYPE = new Function<TrackedActivityModel, ActivityTypeModel>() {
        @Override
        public ActivityTypeModel apply(TrackedActivityModel input) {
            return input == null ? null : input.getActivityType();
        }
    };
    public static final Function<TrackedActivityModel, Long> GET_CATEGORY_ID = Functions.compose(
            ActivityTypeModel.GET_CATEGORY_ID, GET_ACTIVITY_TYPE
    );

    public static final Function<TrackedActivityModel, Long> GET_ACTIVITY_TYPE_ID = Functions.compose(
            ActivityTypeModel.GET_ID, GET_ACTIVITY_TYPE
    );

    public enum Fakeness {
        REAL,
        IN_BETWEEN,
        SUGGESTION,
        START_OF_DAY
    }

    private final long _id;
    private final String _details;
    private final long _createdAtMillis;
    private final ActivityTypeModel _activity;
    private final ProjectModel _project;
    private final long _starttimeMillis;
    private final long _endtimeMillis;
    private final Long _insertDurationMillis;
    private final Long _updateDurationMillis;
    private final Long _updateCount;
    private final Fakeness _fakeness;
    private final List<ActivityCustomFieldModel> _customFieldData;
    private DateTime _endtimeDateTime;
    private DateTime _starttimeDateTime;

    private TrackedActivityModel(
            long id,
            String details,
            ActivityTypeModel activity,
            ProjectModel project,
            long createdAtMillis,
            long starttimeMillis,
            long endtimeMillis,
            Long insertDurationMillis,
            Long updateDurationMillis,
            Long updateCount,
            Fakeness fakeEntry,
            List<ActivityCustomFieldModel> customFieldData
    ) {
        super(endtimeMillis - starttimeMillis);
        _id = id;
        _details = details;
        _activity = activity;
        _project = project;
        _starttimeMillis = starttimeMillis;
        _createdAtMillis = createdAtMillis;
        _endtimeMillis = endtimeMillis;
        _insertDurationMillis = insertDurationMillis;
        _updateDurationMillis = updateDurationMillis;
        _updateCount = updateCount;
        _fakeness = fakeEntry;
        _customFieldData = Preconditions.checkNotNull(customFieldData);
    }

    public static TrackedActivityModel fakeInBetween(long fakeID, long startTimeMillis, long endTimeMillis) {
        return new TrackedActivityModel(
                fakeID,
                "",
                null,
                null,
                System.currentTimeMillis(),
                startTimeMillis,
                endTimeMillis,
                null, null, null,
                Fakeness.IN_BETWEEN,
                ImmutableList.<ActivityCustomFieldModel>of()
        );
    }

    public static TrackedActivityModel fakeStartOfDay(long fakeID, long startTimeMillis) {
        return new TrackedActivityModel(
                fakeID,
                "",
                null,
                null,
                System.currentTimeMillis(),
                startTimeMillis,
                startTimeMillis,
                null, null, null,
                Fakeness.START_OF_DAY,
                ImmutableList.<ActivityCustomFieldModel>of()
        );
    }

    public static TrackedActivityModel fakeSuggestion(long fakeID, String title, long startTimeMillis, long endTimeMillis) {
        return new TrackedActivityModel(
                fakeID,
                "",
                new ActivityTypeModel(-1, title, null),
                null,
                System.currentTimeMillis(),
                startTimeMillis,
                endTimeMillis,
                null, null, null,
                Fakeness.SUGGESTION,
                ImmutableList.<ActivityCustomFieldModel>of()
        );
    }

    public static TrackedActivityModel real(
            long id,
            String details,
            ActivityTypeModel activity,
            ProjectModel project,
            long createdAtMillis,
            long starttimeMillis,
            long endtimeMillis,
            Long insertDurationMillis,
            Long updateDurationMillis,
            Long updateCount,
            List<ActivityCustomFieldModel> customFieldData
    ) {
        return new TrackedActivityModel(
                id, details, activity, project, createdAtMillis, starttimeMillis, endtimeMillis,
                insertDurationMillis, updateDurationMillis, updateCount,
                Fakeness.REAL, customFieldData);
    }

    public boolean isFakeEntry() {
        return _fakeness != Fakeness.REAL;
    }

    public Fakeness getFakeness() {
        return _fakeness;
    }

    public long getId() {
        return _id;
    }

    public long getCreatedAtMillis() {
        return _createdAtMillis;
    }

    public ActivityTypeModel getActivityType() {
        return _activity;
    }

    public CategoryModel getCategory() {
        return _activity == null ? null : _activity.getCategoryModel();
    }

    public ProjectModel getProject() {
        return _project;
    }

    public long getStartTimeMillis() {
        return _starttimeMillis;
    }

    public long getEndTimeMillis() {
        return _endtimeMillis;
    }

    public List<ActivityCustomFieldModel> getCustomFieldData() {
        return _customFieldData;
    }

    public List<String> getDetailsWithCustomFields() {

        final List<ActivityCustomFieldModel> custom = getCustomFieldData();
        final String originalDetails = getDetails();
        if (custom.isEmpty()) {
            if (StringUtil.isTrimmedNullOrEmpty(originalDetails)) {
                return ImmutableList.of();
            }
            return ImmutableList.of(originalDetails);
        }
        ArrayList<String> details = new ArrayList<String>(custom.size());
        if (!StringUtil.isTrimmedNullOrEmpty(originalDetails)) {
            details.add(originalDetails);
            if (!custom.isEmpty()) {
                details.add("");// newline
            }
        }
        for (ActivityCustomFieldModel c: custom) {
            if (!StringUtil.isTrimmedNullOrEmpty(c.getValue())) {
                CustomFieldTypeModel typeModel = c.getTypeModel();
                if (typeModel != null) {
                    details.add(typeModel.getName() + ": " + c.getValue());
                } else {
                    details.add(c.getValue());
                }
            }
        }
        return details;
    }

    public DateTime getEndtimeDateTimeMinutes() {
        if (_endtimeDateTime != null) {
            return _endtimeDateTime;
        }
        _endtimeDateTime = getDateTimeMinutes(_endtimeMillis);
        return _endtimeDateTime;
    }

    private DateTime getDateTimeMinutes(long millis) {
        return new DateTime(millis).withMillisOfSecond(0).withSecondOfMinute(0);
    }

    public DateTime getStarttimeDateTimeMinutes() {
        if (_starttimeDateTime != null) {
            return _starttimeDateTime;
        }
        _starttimeDateTime = getDateTimeMinutes(_starttimeMillis);
        return _starttimeDateTime;
    }


    public String getDetails() {
        return _details;
    }

    public String getDisplayName(Context context) {
        String activityName = _activity == null ? null : _activity.getName();
        if (_fakeness == Fakeness.IN_BETWEEN) {
            return null;
        }
        if (_fakeness == Fakeness.SUGGESTION) {
            return activityName;
        }
        return getDisplayName(context, activityName);
    }

    public static String getDisplayName(Context context, String activityName) {
        if (Strings.isNullOrEmpty(activityName)) {
            return context.getResources().getString(R.string.activity_name_default);
        }
        return activityName;
    }

    /**
     * The total duration for creating and editing the entry.
     */
    public long getEntryDurationMillis() {
        long duration = 0;
        if (_insertDurationMillis != null) {
            duration += _insertDurationMillis;
        }
        if (_updateDurationMillis != null) {
            duration += _updateDurationMillis;
        }
        return duration;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .addValue(_id)
                .addValue(_activity)
                .toString();
    }
}
