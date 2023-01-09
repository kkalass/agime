package de.kalass.agime.trackactivity;

import android.content.Context;
import android.content.res.Resources;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Strings;

import de.kalass.agime.ColorSuggestion;
import de.kalass.agime.R;
import de.kalass.agime.model.ActivityTypeModel;
import de.kalass.agime.model.ProjectModel;
import de.kalass.agime.model.TrackedActivityModel;
import de.kalass.android.common.adapter.AbstractViewModelListAdapter;

/**
* Created by klas on 30.01.14.
*/
public class TrackedActivitiesListAdapter extends AbstractViewModelListAdapter<TrackedActivityModel> {
    private static final String LOG_TAG = "TrackedActivitiesListAdapter";

    public static final int FAKE_ENTRY_SUGGESTION_MIN_HEIGHT = 64;
    public static final int FAKE_ENTRY_SUGGESTION_MAX_HEIGHT = 100;
    public static final int REAL_ENTRY_MIN_HEIGHT = 42;

    public TrackedActivitiesListAdapter(Context context) {
        super(context, R.layout.tracked_activites_item);
    }

    private int getColor(TrackedActivityModel item) {
        if (item.getFakeness() == TrackedActivityModel.Fakeness.IN_BETWEEN) {
            return getUntrackedColor();
        }
        if (item.getFakeness() == TrackedActivityModel.Fakeness.SUGGESTION) {
            return getSuggestionBackgroundColor();
        }
        return ColorSuggestion.getCategoryColor(getContext().getResources(), item.getCategory());
    }

    private int getSuggestionBackgroundColor() {
        return getSuggestionBackgroundColor(getContext());
    }

    private static int getSuggestionBackgroundColor(Context context) {
        return context.getResources().getColor(R.color.background);
    }

    private int getUntrackedColor() {
        return getUntrackedColor(getContext());
    }

    private static int getUntrackedColor(Context context) {
        return context.getResources().getColor(android.R.color.background_light);
    }


    public interface FakenessAdapter {
        String getTitle(Context context, TrackedActivityModel item);

        String getStartTimeString(Context context, TrackedActivityModel item);

        String getEndTimeString(Context context, TrackedActivityModel item, TrackedActivityModel next);

        int getScaledHeightDp(int scaledHeightDP);

        int getMinHeightDp();

        int calculateBackgroundColor(Context context, TrackedActivityModel item, LinearLayout projectItemView, ProjectModel project);

        boolean isJoinPrevious(boolean projectUnchanged, boolean activityTypeUnchanged, boolean categoryUnchanged);

        boolean isForceShowEndTime(TrackedActivityModel item, TrackedActivityModel next);

    }

    public static class RealItemAdapter implements FakenessAdapter {
        public String getTitle(Context context, TrackedActivityModel item) {
            return item.getDisplayName(context);
        }

        @Override
        public String getStartTimeString(Context context, TrackedActivityModel item) {
            return formatTime(context, item.getStartTimeMillis());
        }

        @Override
        public int getScaledHeightDp(int scaledHeightDP) {
            return scaledHeightDP;
        }

        @Override
        public int getMinHeightDp() {
            return REAL_ENTRY_MIN_HEIGHT;
        }

        @Override
        public int calculateBackgroundColor(Context context, TrackedActivityModel item, LinearLayout projectItemView, ProjectModel project) {
            return ColorSuggestion.getProjectColor(context.getResources(), project);
        }

        @Override
        public String getEndTimeString(Context context, TrackedActivityModel item, TrackedActivityModel next) {
            String formattedEndTime = formatTime(context, item.getEndTimeMillis());
            boolean showEndTime = next == null && (!item.getStarttimeDateTimeMinutes().equals(item.getEndtimeDateTimeMinutes()));
            return showEndTime ? formattedEndTime : "";
        }

        @Override
        public boolean isJoinPrevious(boolean projectUnchanged, boolean activityTypeUnchanged, boolean categoryUnchanged) {
            return projectUnchanged && activityTypeUnchanged && categoryUnchanged;
        }

        @Override
        public boolean isForceShowEndTime(TrackedActivityModel item, TrackedActivityModel next) {
            return next == null;
        }
    }

    public static class StartOfDayItemAdapter extends RealItemAdapter {
        public String getTitle(Context context, TrackedActivityModel item) {
            return DateUtils.formatDateTime(context, item.getStartTimeMillis(), DateUtils.FORMAT_SHOW_DATE);
        }

        @Override
        public String getStartTimeString(Context context, TrackedActivityModel item) {
            return  "";
        }


        @Override
        public String getEndTimeString(Context context, TrackedActivityModel item, TrackedActivityModel next) {
            return "";
        }
    }

    public static class InBetweenItemAdapter extends RealItemAdapter {
        @Override
        public String getStartTimeString(Context context, TrackedActivityModel item) {
            return "";
        }

        @Override
        public String getEndTimeString(Context context, TrackedActivityModel item, TrackedActivityModel next) {
            return "";
        }

        @Override
        public int calculateBackgroundColor(Context context, TrackedActivityModel item, LinearLayout projectItemView, ProjectModel project) {
            return getUntrackedColor(context);
        }
    }

    public static class SuggestionItemAdapter extends RealItemAdapter {

        @Override
        public int getScaledHeightDp(int scaledHeightDP) {
            return Math.min(scaledHeightDP, FAKE_ENTRY_SUGGESTION_MAX_HEIGHT);
        }

        @Override
        public int getMinHeightDp() {
            return FAKE_ENTRY_SUGGESTION_MIN_HEIGHT;
        }

        @Override
        public int calculateBackgroundColor(Context context, TrackedActivityModel item, LinearLayout projectItemView, ProjectModel project) {
            return getSuggestionBackgroundColor(context);
        }
    }

    protected static final FakenessAdapter REAL_ITEM_ADAPTER = new RealItemAdapter();

    protected static final FakenessAdapter START_OF_DAY_ITEM_ADAPTER = new StartOfDayItemAdapter();

    protected static final FakenessAdapter SUGGESTION_ITEM_ADAPTER = new SuggestionItemAdapter();

    protected static final FakenessAdapter IN_BETWEEN_ITEM_ADAPTER = new InBetweenItemAdapter();

    protected FakenessAdapter getAdapter(TrackedActivityModel.Fakeness fakeness) {
        switch (fakeness) {
            case REAL:
                return REAL_ITEM_ADAPTER;
            case IN_BETWEEN:
                return IN_BETWEEN_ITEM_ADAPTER;
            case SUGGESTION:
                return SUGGESTION_ITEM_ADAPTER;
            case START_OF_DAY:
                return START_OF_DAY_ITEM_ADAPTER;
        }
        return null;
    }


    @Override
    protected View fillView(View view, TrackedActivityModel item, int position) {
        FakenessAdapter adapter = getAdapter(item.getFakeness());
        final TrackedActivityModel previous = position == 0 ? null : getItem(position - 1);
        final TrackedActivityModel next = (position + 1) >= getCount() ? null : getItem(position + 1);
        final boolean forceShowEndTime = adapter.isForceShowEndTime(item, next);
        final TextView activityNameView = (TextView)view.findViewById(R.id.activity_name);
        final TextView projectNameView = (TextView)view.findViewById(R.id.project_name);
        final LinearLayout projectItemView = (LinearLayout)view.findViewById(R.id.project_item);
        final TextView startTimeView = (TextView)view.findViewById(R.id.start_time);
        final TextView endTimeView = (TextView)view.findViewById(R.id.end_time);
        final TextView detailsView = (TextView)view.findViewById(R.id.activity_details);
        final View descriptionContainer = view.findViewById(R.id.description_container);
        final View timeContainer = view.findViewById(R.id.time_container);

        ActivityTypeModel activityType = item.getActivityType();
        ProjectModel project = item.getProject();
        Long activityId = activityType == null ? null : activityType.getId();
        Long categoryId = item.getCategory() == null ? null : item.getCategory().getId();
        Long projectId = project == null ? null : project.getId();
        Long prevActivityId = previous == null ? null : (previous.getActivityType() == null ? null : previous.getActivityType().getId());
        Long prevCategoryId = previous == null ? null : (previous.getCategory() == null ? null : previous.getCategory().getId());
        Long prevProjectId = previous == null ? null : (previous.getProject() == null ? null : previous.getProject().getId());
        boolean activityTypeUnchanged = Objects.equal(activityId, prevActivityId);
        boolean categoryUnchanged = Objects.equal(categoryId, prevCategoryId);
        boolean projectUnchanged = Objects.equal(projectId, prevProjectId);
        boolean joinPrevious = adapter.isJoinPrevious(projectUnchanged, activityTypeUnchanged, categoryUnchanged);
        String displayActivityName = adapter.getTitle(getContext(), item);

        String details =
                Joiner.on('\n').join(item.getDetailsWithCustomFields());
        if (forceShowEndTime && details.isEmpty()) {
            // the last item always needs to have at least one line in the comment field,
            // so that the end date is rendered properly
            details = " ";
        }
        detailsView.setText(details);
        int color = getColor(item);
        descriptionContainer.setBackgroundColor(color);
        //timeContainer.setBackgroundColor(color);

        activityNameView.setText(displayActivityName);


        startTimeView.setText(adapter.getStartTimeString(getContext(), item));
        startTimeView.setTextColor( getContext().getResources().getColor(joinPrevious ? R.color.activity_time_join_previous : android.R.color.primary_text_light));


        long elapsedMinutesTotal = item.getDurationMinutes();


        // make the height of the view appear proportional to the duration
        // An hour should approximately be represented by 60 dip - but also take into
        // account that the Android design guidelines ask for a minimum height of 48dip.
        final int baseHeight = 60;
        final int baseDurationMinutes = 30;
        int minHeightDP = adapter.getMinHeightDp();
        final float heightScale = (float)baseHeight / (float)baseDurationMinutes;
        int scaledHeightDP = adapter.getScaledHeightDp((int)Math.floor(elapsedMinutesTotal * heightScale));
        Resources r = getContext().getResources();
        final int scaledTimeRelativeHeightPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                scaledHeightDP,
                r.getDisplayMetrics()
        );

        final int scaledMinHeightPx = (int)TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                Math.max(scaledHeightDP, minHeightDP),
                r.getDisplayMetrics()
        );

        projectItemView.setBackgroundColor(adapter.calculateBackgroundColor(getContext(), item, projectItemView, project));


        projectNameView.setHeight(scaledTimeRelativeHeightPx);
        descriptionContainer.setMinimumHeight(scaledMinHeightPx);
        if (Strings.isNullOrEmpty(details)) {
            detailsView.setVisibility(View.GONE);
        } else {
            detailsView.setVisibility(View.VISIBLE);
        }
        endTimeView.setText(adapter.getEndTimeString(getContext(), item, next));
        return view;
    }



    private String formatTime(long timeMillis) {
        return formatTime(getContext(), timeMillis);
    }

    public static String formatTime(Context context, long timeMillis) {
        return DateUtils.formatDateTime(context, timeMillis, DateUtils.FORMAT_SHOW_TIME);
    }

}
