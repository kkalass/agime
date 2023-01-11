package de.kalass.agime.trackactivity;

import android.app.Activity;
import android.content.Context;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.linearlistview.LinearListView;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.util.List;

import de.kalass.agime.ColorSuggestion;
import de.kalass.agime.R;
import de.kalass.agime.customfield.ActivityCustomFieldEditorModel;
import de.kalass.agime.customfield.ActivityCustomFieldListAdapter;
import de.kalass.agime.timesuggestions.TimeSuggestions;
import de.kalass.android.common.activity.BaseCRUDActivity;
import de.kalass.android.common.activity.BaseViewWrapper;
import de.kalass.android.common.util.TimeFormatUtil;
import de.kalass.android.common.widget.AutoCompleteSpinner;
import de.kalass.android.common.widget.KKSpinner;

/**
* Created by klas on 21.01.14.
*/
final class TrackedActivityFragmentView extends BaseViewWrapper {
    static final int LAYOUT = R.layout.activity_track;

    static final int ID_CUSTOM_FIELDS_LIST = R.id.custom_fields_list;
    static final int ID_ACTIVITY_TRACK_DETAILS = R.id.activity_track_details;
    static final int ID_ACTIVITY_SUGGESTION_SPINNER = R.id.activity_type;
    static final int ID_PROJECT_SPINNER = R.id.activity_project;
    static final int ID_START_TIME = R.id.start_time;
    static final int ID_DAY_INPUT_START = R.id.day_input_start;
    static final int ID_DAY_INPUT_DIVIDER = R.id.day_input_divider;
    static final int ID_DAY_INPUT = R.id.day_input;
    static final int ID_END_TIME = R.id.end_time;
    static final int ID_HEAD = R.id.head;
    static final int ID_PROJECT_COLOR_BAR_1 = R.id.project_color;


    private final Context context;
    final LinearListView customFieldsList;
    final EditText detailsEditText;
    final AutoCompleteSpinner activityTypeTextField;
    final AutoCompleteSpinner projectTextField;
    final KKSpinner startTimeSpinner;
    final Button dayButton;
    final Button dayButtonStart;
    final View dayButtonDivider;
    final KKSpinner endTimeSpinner;
    final LinearLayout head;
    final View projectColorBar1;


    TrackedActivityFragmentView(Activity context, View view) {
        super(view);
        this.context = context;
        head = getLinearLayout(ID_HEAD);
        customFieldsList = get(LinearListView.class, ID_CUSTOM_FIELDS_LIST);
        detailsEditText = getEditText(ID_ACTIVITY_TRACK_DETAILS);
        activityTypeTextField = getAutoCompleteSpinner(ID_ACTIVITY_SUGGESTION_SPINNER);
        projectTextField = getAutoCompleteSpinner(ID_PROJECT_SPINNER);
        startTimeSpinner = get(KKSpinner.class, ID_START_TIME);
        dayButton = getButton(ID_DAY_INPUT);
        dayButtonStart = getButton(ID_DAY_INPUT_START);
        endTimeSpinner = get(KKSpinner.class, ID_END_TIME);
        projectColorBar1 = getView(ID_PROJECT_COLOR_BAR_1);
        dayButtonDivider = getView(ID_DAY_INPUT_DIVIDER);

    }

    protected <T extends ListAdapter> void setAdapter(LinearListView view, T adapter) {
        if (view.getAdapter() == null) {
            view.setAdapter(adapter);
        }
    }

    public void setSelectedProject(Long id, String name, Integer colorCode, List<ActivityCustomFieldEditorModel> customFields) {
        int iid = id == null ? -1 : id.intValue();
        int color = (colorCode == null && iid == -1)
                ? context.getResources().getColor(R.color.project_background_default)
                : ColorSuggestion.suggestProjectColor(context.getResources(), iid, colorCode);
        projectColorBar1.setBackgroundColor(color);
        Toolbar toolbar = ((BaseCRUDActivity)context).getToolbar();
        if (toolbar != null) {
            toolbar.setBackgroundColor(color);
        }
        projectTextField.setBackgroundColor(color);
        getCustomFieldListAdapter().setProjectId(id);

        long lid = id == null ? -1 : id.longValue();
        if (projectTextField.getCurrentItemId() != lid) {
            projectTextField.setCurrentItem(lid, name);
        }

        getCustomFieldListAdapter().initialize(id, customFields);
    }

    private int getColor(Long id, Integer color) {
        if (id == null && color == null) {
            return ColorSuggestion.getCategoryColor(context.getResources(), null);
        }
        return color;
    }

    public void setCategory(Long id, String name, Integer color) {
        if (id == null) {
            // new name for category
            // FIXME what is this code good for???
            getActivityTypeSuggestionsAdapter().setNewCategorySuggestionName(name);
        }

        final int bgColor = getColor(id, color);
        //head.setBackgroundColor(bgColor);
        activityTypeTextField.setBackgroundColor(bgColor);
        //projectTextField.setBackgroundColor(bgColor);
    }


    // date and times are set together, becuase setting the date will change the suggestions for the times
    public void setDateTimes(
            TimeSuggestions timeSuggestions,
            long trackedActivityId,
            LocalDate date, LocalTime startTime, LocalTime endTime
    ) {
        getProjectAutocompleteAdapter().setDate(date);
        // FIXME: implement day Button start!
        dayButtonDivider.setVisibility(View.GONE);
        dayButtonStart.setVisibility(View.GONE);
        //dayButtonStart.setText(TimeFormatUtil.formatDate(context, startDate));

        dayButton.setText(TimeFormatUtil.formatDate(context, date));

        setTimeSuggestions(
                startTimeSpinner, getStartTimeSuggestionAdapter(),
                TimeSuggestions.Mode.START_TIME, timeSuggestions,
                trackedActivityId, startTime, endTime
        );
        setTimeSuggestions(
                endTimeSpinner, getEndTimeSuggestionAdapter(),
                TimeSuggestions.Mode.END_TIME, timeSuggestions,
                trackedActivityId, startTime, endTime
        );

        ActivityTypeSuggestionFilterableListAdapter suggestionsAdapter = getActivityTypeSuggestionsAdapter();
        suggestionsAdapter.setTimes(date, startTime, endTime);
    }

    public void setActivitySuggestion(Long activityTypeId, String activityTypeName) {
        //
        // The activity type field is two things in one: First of all
        // it contains the text of the activity type, and if a  user changes
        // the text it allows for creation of a new activity type.
        //
        // but second, it is backed by an adapter that contains activity suggestions
        // - and the "item" in this field is thus an activity suggestion, which may
        // or may not correspond to an activity type. The "item id" is thus *not* the id of the
        // activity type
        //
        // We adjust the text of the field here only if it differs from the original value.
        //
        final Editable text = activityTypeTextField.getText();
        final String textAsString = text == null ? null : text.toString().trim();
        if (!Objects.equal(Strings.nullToEmpty(activityTypeName).trim(), Strings.nullToEmpty(textAsString))) {
            activityTypeTextField.setText(activityTypeName);
        }
    }

    private void setTimeSuggestions(
            KKSpinner spinner,
            TimeSuggestionListAdapter adapter,
            TimeSuggestions.Mode mode,
            TimeSuggestions timeSuggestions,
            long trackedActivityId,
            LocalTime startTime,
            LocalTime endTime
    ) {
        Preconditions.checkNotNull(timeSuggestions);
        Preconditions.checkNotNull(startTime);
        Preconditions.checkNotNull(endTime);

        LocalTime time = mode == TimeSuggestions.Mode.START_TIME ? startTime : endTime;
        adapter.setItems(timeSuggestions.getSuggestions(
                mode, trackedActivityId, startTime, endTime

        ));
        int idx = TimeSuggestions.getSuggestionsIndex(time, adapter);
        spinner.setSelection(idx, false);
    }

    public void setStartTimeSuggestionAdapter(TimeSuggestionListAdapter adapter) {
        setAdapter(startTimeSpinner, adapter);
    }

    private TimeSuggestionListAdapter getStartTimeSuggestionAdapter() {
        return (TimeSuggestionListAdapter)startTimeSpinner.getAdapter();
    }

    public void setEndTimeSuggestionAdapter(TimeSuggestionListAdapter adapter) {
        setAdapter(endTimeSpinner, adapter);
    }

    private TimeSuggestionListAdapter getEndTimeSuggestionAdapter() {
        return (TimeSuggestionListAdapter)endTimeSpinner.getAdapter();
    }

    public void setActivityTypeSuggestionsAdapter(ActivityTypeSuggestionFilterableListAdapter adapter) {
        setAdapter(activityTypeTextField, adapter);
    }

    public ActivityTypeSuggestionFilterableListAdapter getActivityTypeSuggestionsAdapter() {
        return (ActivityTypeSuggestionFilterableListAdapter)activityTypeTextField.getAdapter();
    }

    public void setProjectAutocompleteAdapter(ProjectAutocompleteAdapter adapter) {
        setAdapter(projectTextField, adapter);
    }

    public ProjectAutocompleteAdapter getProjectAutocompleteAdapter() {
        return (ProjectAutocompleteAdapter)projectTextField.getAdapter();
    }

    public void setCustomFieldListAdapter(ActivityCustomFieldListAdapter customFieldListAdapter) {
        setAdapter(customFieldsList, customFieldListAdapter);
    }

    public ActivityCustomFieldListAdapter getCustomFieldListAdapter() {
        return (ActivityCustomFieldListAdapter)customFieldsList.getAdapter();
    }

}
