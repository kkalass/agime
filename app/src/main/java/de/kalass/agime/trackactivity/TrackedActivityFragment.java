package de.kalass.agime.trackactivity;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Toast;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.util.List;
import java.util.Map;

import de.kalass.agime.R;
import de.kalass.agime.acquisitiontime.RecurringAcquisitionTimeEditorFragment;
import de.kalass.agime.activitytype.ActivityTypeEditorActivity;
import de.kalass.agime.analytics.AnalyticsBaseCRUDFragment;
import de.kalass.agime.customfield.ActivityCustomFieldListAdapter;
import de.kalass.agime.loader.ActivityTypeSuggestionFeature;
import de.kalass.agime.loader.ActivityTypeSuggestionSyncLoader;
import de.kalass.agime.loader.ActivityTypeSyncLoader;
import de.kalass.agime.loader.CategorySyncLoader;
import de.kalass.agime.loader.ProjectModelQuery;
import de.kalass.agime.loader.ProjectSyncLoader;
import de.kalass.agime.model.ActivityTypeModel;
import de.kalass.agime.model.ActivityTypeSuggestionModel;
import de.kalass.agime.model.CategoryModel;
import de.kalass.agime.model.ProjectModel;
import de.kalass.agime.provider.MCContract;
import de.kalass.agime.provider.MCContract.Activity;
import de.kalass.agime.timesuggestions.TimeSuggestion;
import de.kalass.agime.timesuggestions.TimeSuggestionSyncLoader;
import de.kalass.agime.timesuggestions.TimeSuggestions;
import de.kalass.android.common.activity.BaseViewWrapper;
import de.kalass.android.common.activity.CRUDMode;
import de.kalass.android.common.simpleloader.AbstractLoader;
import de.kalass.android.common.simpleloader.ObserveDataSourceMode;
import de.kalass.android.common.support.datetime.DatePickerSupport;
import de.kalass.android.common.support.datetime.TimePickerSupport;
import de.kalass.android.common.util.StringUtil;
import de.kalass.android.common.widget.AutoCompleteSpinner;

import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class TrackedActivityFragment
    extends
        AnalyticsBaseCRUDFragment<TrackedActivityFragmentView, TrackedActivityFragmentData>
    implements
        View.OnClickListener,
        AdapterView.OnItemSelectedListener,
        TimePickerSupport.LocalTimeSelectedListener,
        AutoCompleteSpinner.OnItemSetListener,
        DatePickerSupport.LocalDateSelectedListener
{
    private static final int ACTIVITY_CODE_EDIT_ACTIVITY_TYPE = 8;

    public static final String EXTRA_DAY_MILLIS = "dayMillis";
    public static final String EXTRA_STARTTIME_MILLIS = "starttimeMillis";
    public static final String EXTRA_ENDTIME_MILLIS = "endtimeMillis";
    private static final String LOG_TAG = "TrackedActivityFragment";

    private TrackedActivityFragmentData _data;
    private Bundle _previousState;

    public TrackedActivityFragment() {
        super(TrackedActivityFragmentView.LAYOUT, Activity.CONTENT_TYPE_DIR, Activity.CONTENT_TYPE_ITEM);
    }

    @Override
    protected CRUDMode getMode() {
        // we currently do not support a real view mode
        CRUDMode requestedMode = super.getMode();
        return requestedMode == CRUDMode.VIEW ? CRUDMode.EDIT : requestedMode;
    }

    @Override
    public void onAttach(android.app.Activity activity) {
        super.onAttach(activity);
        TrackActivity a = (TrackActivity)activity;
        Toolbar toolbar = a.getToolbar();
        if (toolbar != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            toolbar.setElevation(0);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //toolbar elevation needs to be set here, because the toolbar does not exist
        // in 'onAttach' if the device orientation is changed.
        Toolbar toolbar = ((TrackActivity)getActivity()).getToolbar();
        if (toolbar != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            toolbar.setElevation(0);
        }

    }

    @Override
    public AbstractLoader<TrackedActivityFragmentData> createLoader(int id, Bundle args) {
        return new TrackedActivityFragmentDataAsyncLoader(
                getContext(),
                getMode(), getEntityId(), getArguments(), getUri(), ObserveDataSourceMode.IGNORE_CHANGES
        );
    }

    @Override
    public void onLoaderReset(Loader<TrackedActivityFragmentData> loader) {
        super.onLoaderReset(loader);
        if (_data != null) {
            _data = null;
        }
    }


    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        _previousState = savedInstanceState;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.i(LOG_TAG, "TrackedActivityFragment.onDestroyView called");
        // ensure that resources are released, for example cursors created by autocomplete text view
        getWrappedView().getCustomFieldListAdapter().setItems(null);
        getWrappedView().getProjectAutocompleteAdapter().changeCursor(null);
        getWrappedView().getActivityTypeSuggestionsAdapter().setItems(null);
    }

    @Override
    protected TrackedActivityFragmentView onWrapView(View v) {
        final TrackedActivityFragmentView view = new TrackedActivityFragmentView(getActivity(), v);
        view.setActivityTypeSuggestionsAdapter(new ActivityTypeSuggestionFilterableListAdapter(getContext()));
        view.setProjectAutocompleteAdapter(new ProjectAutocompleteAdapter(getContext()));
        view.setStartTimeSuggestionAdapter(new TimeSuggestionListAdapter(getContext(), getEntityId()));
        view.setEndTimeSuggestionAdapter(new TimeSuggestionListAdapter(getContext(), getEntityId()));
        view.setCustomFieldListAdapter(new ActivityCustomFieldListAdapter(getContext()));
        return view;
    }

    @Override
    protected void onBindView(final TrackedActivityFragmentView view, final TrackedActivityFragmentData data) {

        _data = data; // ensure that the convenient shortcut is available
        // set the data to the view

        view.detailsEditText.setText(_previousState == null ? data.getDetails(): _previousState.getString("details"));
        view.setSelectedProject(data.getProjectId(), data.getProjectName(), data.getProjectColorCode(), data.customFields);
        view.setActivitySuggestion(data.getActivityTypeId(), data.getActivityTypeName());
        view.setCategory(data.getCategoryId(), data.getCategoryName(), data.getCategoryColorCode());
        view.setDateTimes(data.getTimeSuggestions(), data.id, data.getDate(), data.getStartTime(), data.getEndTime());

        // set user interaction callbacks
        view.dayButton.setOnClickListener(this);
        view.startTimeSpinner.setOnItemSelectedByClickListener(this);
        view.endTimeSpinner.setOnItemSelectedByClickListener(this);
        view.activityTypeTextField.setOnItemSetListener(this);
        view.projectTextField.setOnItemSetListener(this);

        // Show soft keyboard automatically

        if (_previousState == null && getMode() == CRUDMode.INSERT ) {
            view.activityTypeTextField.setOnFocusChangeListener(new OpenKeyboardOnFocus(this));
            view.activityTypeTextField.requestFocus();
        }
    }

    private String getText(EditText field) {
        return StringUtil.toString(field.getText());
    }

    @Override
    protected void save() {
        assertIsSaveOrUpdate();

        final TrackedActivityFragmentView view = getWrappedView();
        final InsertOrUpdateOperationFactory customFieldsOperationFactory =
                view.getCustomFieldListAdapter().getSaveOrUpdateOperationFactory();

        final InsertOrUpdateInputBuilder builder = new InsertOrUpdateInputBuilder()
                .setEntityId(getEntityId())
                .setStartTimeMillis(_data.getStartTimeMillis())
                .setEndTimeMillis(_data.getEndTimeMillis())
                .setActivityTypeId(_data.getActivityTypeId())
                .setActivityTypeName(_data.getActivityTypeName())
                .setCategoryTypeId(_data.getCategoryId())
                .setCategoryColor(_data.getCategoryColorCode())
                .setCategoryName(_data.getCategoryName())
                .setAcquisitionTimeCandidates(_data.getAcquisitionTimes())
                .setAdditional(ImmutableList.of(customFieldsOperationFactory));

        final Long projectId = _data.getProjectId();
        builder.setProjectId(projectId);
        builder.setProjectColor(_data.getProjectColorCode());
        if (projectId == null) {
            // no existing project, but maybe the user wants to create a new one - use
            // the value of the textfield
            builder.setProjectName(getText(view.projectTextField));
        } else {
            builder.setProjectName(_data.getProjectName());
        }

        final Long activityTypeId = _data.getActivityTypeId();
        builder.setActivityTypeId(activityTypeId);
        if (activityTypeId == null) {
            // no existing activity type, but maybe the user wants to create a new one - use
            // the value of the textfield
            builder.setActivityTypeName(getText(view.activityTypeTextField));
        } else {
            builder.setActivityTypeName(_data.getActivityTypeName());
        }


        // the details are not synced automatically to "data", so we read them from the view
        builder.setDetails(getText(view.detailsEditText));
        builder.setOriginalInsertDurationMillis(_data.originalInsertDurationMillis);
        builder.setOriginalUpdateDurationMillis(_data.originalUpdateDurationMillis);
        builder.setOriginalUpdateCount(_data.originalUpdateCount);

        performSaveOrUpdateAsync(builder.createInsertOrUpdateInput(), new InsertOrUpdateTrackedActivity(getContext(), getCurrentStartTime()));
    }

    @Override
    public void onEntityUpdated(long entityId, Object entity) {
        super.onEntityUpdated(entityId, entity);
        notifyUserAboutResults((InsertOrUpdateTrackedActivityResult) entity);
    }

    @Override
    public void onEntityInserted(long entityId, Object entity) {
        super.onEntityInserted(entityId, entity);
        notifyUserAboutResults((InsertOrUpdateTrackedActivityResult) entity);
    }


    private void notifyUserAboutResults(InsertOrUpdateTrackedActivityResult result) {

        if (result.isStartedAcquisitionTime()) {
            RecurringAcquisitionTimeEditorFragment.sendReconfigureBroadcast(getActivity());
        }

        List<SplitResult> splitResults = result.getSplitResults();
        Context context = getContext();
        if (!splitResults.isEmpty()) {
            for (SplitResult r : splitResults) {
                Log.i("SPLIT TO ", r.type + " " + r.entries);
            }
            boolean inverseCorrected = Iterables.any(splitResults, SplitResult.IS_INVERSE_CORRECTED);
            int textID = inverseCorrected ? R.string.track_activity_new_adjusted : R.string.track_activity_existing_adjusted;
            Toast.makeText(context, textID, Toast.LENGTH_LONG).show();

        }
    }

    @Override
    protected void delete() {
        assertCanDelete();
        TrackedActivityEditorDBUtil.delete(getContext(), this, ImmutableList.of(getEntityId()));
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        switch(parent.getId()) {
            case TrackedActivityFragmentView.ID_START_TIME:
                onTimeSuggestionSelected(
                    TrackedActivityFragmentView.ID_START_TIME,
                    (TimeSuggestion) parent.getSelectedItem(),
                    _data.getStartTime()
                );
                break;
            case TrackedActivityFragmentView.ID_END_TIME:
                onTimeSuggestionSelected(
                        TrackedActivityFragmentView.ID_END_TIME,
                        (TimeSuggestion) parent.getSelectedItem(),
                        _data.getEndTime()
                );
                break;
        }
    }


    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }


    @Override
    public void onTimeSelected(int token, LocalTime time) {
        switch (token) {
            case TrackedActivityFragmentView.ID_START_TIME:
                _data.setStartTime(time);
                getWrappedView().setDateTimes(
                        _data.getTimeSuggestions(), _data.id,
                        _data.getDate(), _data.getStartTime(), _data.getEndTime());
                break;
            case TrackedActivityFragmentView.ID_END_TIME:
                _data.setEndTime(time);
                getWrappedView().setDateTimes(
                        _data.getTimeSuggestions(), _data.id,
                        _data.getDate(), _data.getStartTime(), _data.getEndTime());
                break;
        }
    }


    @Override
    public void onItemSet(AutoCompleteSpinner spinner, boolean userSelectedExplicitely, int position, long itemId) {
        switch (spinner.getId()) {
            case TrackedActivityFragmentView.ID_PROJECT_SPINNER:
                onProjectSpinnerItemSet(position, (CursorAdapter) spinner.getAdapter());
                break;
            case TrackedActivityFragmentView.ID_ACTIVITY_SUGGESTION_SPINNER:
                onActivitySuggestionItemSet(
                        (ActivityTypeSuggestionFilterableListAdapter) spinner.getAdapter(),
                        userSelectedExplicitely, position, itemId
                );
                break;
        }
    }

    @Override
    public void onItemReset(AutoCompleteSpinner spinner) {
        switch (spinner.getId()) {
            case TrackedActivityFragmentView.ID_PROJECT_SPINNER:
                onProjectSpinnerItemReset();
                break;
            case TrackedActivityFragmentView.ID_ACTIVITY_SUGGESTION_SPINNER:
                onActivitySuggestionItemReset();
                break;
        }
    }

    private void onActivitySuggestionItemSet(
            ActivityTypeSuggestionFilterableListAdapter adapter,
            boolean selectedExplicitely, int position, long itemId) {
        if (position < 0) {
            // the item was set programmatically and might not exist in the (suggestion) adapter.
            // But since it was set programmatically, we silently assume that its effects
            // will have been handled seperately

            // Please note that this is not really clean and due to the duplicate nature of
            // the activity suggestion field: if set programmatically, it is only about the
            // activity type itself (id + name). But if set via a suggestion, the user choose
            // to use a "template" and we use the category information, as well as the project
            // from the sugestion
            return;
        }
        checkConsistentId(position, itemId, adapter);

        // apply user selection to data
        final ActivityTypeSuggestionModel model = checkNotNull(adapter.getItem(position));
        final ActivityTypeSuggestionModel.Type suggestionType = model.getType();
        final CategoryModel category = model.getCategory();
        final Long categoryId = category == null ? null : category.getId();
        final String categoryName = category == null ? null : category.getName();
        final Integer categoryColour = category == null ? null : category.getColour();
        // do not change the project, if the suggestion is for editing or creating - only if it corresponds to an existing entry
        final boolean projectSelected = suggestionType == ActivityTypeSuggestionModel.Type.EXISTING_DATA;

        _data.setSelectedCategory(categoryId, categoryName, categoryColour);
        _data.setSelectedActivityType(model.getActivityTypeId(), model.getActivityName());

        if (projectSelected) {
            _data.setSelectedProject(model.getProject());
        }

        // sync view with data
        final TrackedActivityFragmentView view = getWrappedView();
        view.setActivitySuggestion(_data.getActivityTypeId(), _data.getActivityTypeName());
        view.setCategory(_data.getCategoryId(), _data.getCategoryName(), _data.getCategoryColorCode());
        if (projectSelected) {
            view.setSelectedProject(
                    _data.getProjectId(), _data.getProjectName(), _data.getProjectColorCode(),
                    _data.customFields);
        }

        view.getCustomFieldListAdapter().setSelectedItems(model.getCustomFieldModels());

        // handle further interaction options of the suggestion
        switch (suggestionType) {
            case NEW_CREATE_CATEGORY:
                // opens a dialog. If the user submits the dialog, it will set details for a new category to data and view
                if (selectedExplicitely) {
                    onCreateCategorySelected(model);
                }
                break;
            case EDIT:
                if (selectedExplicitely) {
                    onEditActivityTypeSelected(model.getActivityTypeId());
                }
                break;
        }

    }
    /**
     * Starts an activity that allows the user to edit the choosen activity type
     */
    public void onEditActivityTypeSelected(long activityTypeId) {
        Intent intent = new Intent(Intent.ACTION_EDIT, ContentUris.withAppendedId(MCContract.ActivityType.CONTENT_URI, activityTypeId));
        startActivityForResult(intent, ACTIVITY_CODE_EDIT_ACTIVITY_TYPE);
    }


    public void onCreateCategorySelected(final ActivityTypeSuggestionModel model) {
        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        class CreateCategoryWrappedView extends BaseViewWrapper {
            public static final int LAYOUT = R.layout.new_category_name_dialog;
            public static final int ID_CATEGORY_NAME = R.id.activity_category_name;
            final EditText categoryName;

            CreateCategoryWrappedView(View view) {
                super(view);
                categoryName = getEditText(ID_CATEGORY_NAME);
            }
        }
        final View dialogView = checkNotNull(getLayoutInflater(null).inflate(CreateCategoryWrappedView.LAYOUT, null));
        final CreateCategoryWrappedView view = new CreateCategoryWrappedView(dialogView);
        view.categoryName.setText(model.getNewCategoryName());
        final int categoryColor = model.getCategoryColor(getContext());
        dialogView.setBackgroundColor(categoryColor);
// 2. Chain together various setter methods to set the dialog characteristics
        builder.setView(dialogView)
                .setTitle(R.string.new_category_title)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String text = StringUtil.trim(view.categoryName.getText());
                        _data.setSelectedCategory(null, text, categoryColor);
                        getWrappedView().setCategory(_data.getCategoryId(), _data.getCategoryName(), _data.getCategoryColorCode());
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        _data.setSelectedCategory(null, null, null);
                        getWrappedView().setCategory(_data.getCategoryId(), _data.getCategoryName(), _data.getCategoryColorCode());
                    }
                })
        ;

// 3. Get the AlertDialog from create()
        AlertDialog dialog = builder.create();
        view.categoryName.requestFocus();
        dialog.getWindow().setSoftInputMode(SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    private void onActivitySuggestionItemReset() {
        // DO NOT reset the project, just because the activity field name does not match any more
        // this is a little bit strange, and due to the fact that the ativity type selection
        // field is at the same time some sort of "tracked activity settings suggestion"
        final TrackedActivityFragmentView view = getWrappedView();
        _data.setSelectedActivityType(null, getText(view.activityTypeTextField)/*reset means: no existing data matches, create new one*/);
        _data.setSelectedCategory(null, null, null);

        view.setActivitySuggestion(_data.getActivityTypeId(), _data.getActivityTypeName());
        view.setCategory(_data.getCategoryId(), _data.getCategoryName(), _data.getCategoryColorCode());
    }

    private void onProjectSpinnerItemSet(int position, CursorAdapter adapter) {
        if (position < 0) {
            // the item was set programmatically and might not exist in the (suggestion) adapter.
            // But since it was set programmatically, we silently assume that its effects
            // will have been handled seperately
            return;
        }
        Preconditions.checkArgument(position >= 0);
        final ProjectModel projectModel = ProjectModelQuery.READER.apply(adapter.getCursor());
        _data.setSelectedProject(projectModel);
        getWrappedView().setSelectedProject(_data.getProjectId(), _data.getProjectName(), _data.getProjectColorCode(), _data.customFields);
    }

    private void onProjectSpinnerItemReset() {
        // reset means: the user choose not to use any of the suggestions, but instead
        // wants to enter a new project - and the name of the project is the new text
        // of the project text field
        _data.setSelectedProject(null, getText(getWrappedView().projectTextField), null);
        getWrappedView().setSelectedProject(_data.getProjectId(), _data.getProjectName(), _data.getProjectColorCode(), _data.customFields);
    }

    private void onTimeSuggestionSelected(int token, TimeSuggestion suggestion, LocalTime currentValue) {
        switch (suggestion.getType()) {
            case ANY_TIME:
                // let the user select or dismiss selection
                TimePickerSupport.showTimePickerDialog(
                        getContext(), getFragmentManager(), token, this, currentValue
                );
                break;
            default:
                onTimeSelected(token, suggestion.getTimeMinutePrecision());
                break;
        }
    }

    public void onChangeDayClicked() {
        DatePickerSupport.showDatePickerDialog(
                getContext(),
                getFragmentManager(),
                TrackedActivityFragmentView.ID_DAY_INPUT,
                this, _data.getDate());
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case TrackedActivityFragmentView.ID_DAY_INPUT:
                onChangeDayClicked();
                return;
        }
    }

    @Override
    public void onDateSelected(int token, final LocalDate date) {
        switch(token) {
            case TrackedActivityFragmentView.ID_DAY_INPUT:
                new ApplySelectedDayTask(date).execute(date);
                break;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (outState != null) {
            outState.putString("details", getText(getWrappedView().detailsEditText));
        }
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACTIVITY_CODE_EDIT_ACTIVITY_TYPE && resultCode == android.app.Activity.RESULT_OK) {
            final long editedId = data.getLongExtra(ActivityTypeEditorActivity.EXTRA_ID, -1);
            checkArgument(editedId > 0);
            new ApplyActivityTypeEditTask().execute(editedId);
        }
    }


    private void checkConsistentId(int position, long itemId, ActivityTypeSuggestionFilterableListAdapter adapter) {
        long curItemId = adapter.getItemId(position);
        if (curItemId != itemId) {
            throw new IllegalStateException("Expected " + itemId + " but was " + curItemId);
        }
    }


    /**
     * To apply a selected day, we need to load the time suggestions for the day first.
     * Asynchronously, of course.
     */
    private class ApplySelectedDayTask extends AsyncTask<LocalDate, Void, TimeSuggestions> {
        private final LocalDate date;

        public ApplySelectedDayTask(LocalDate date) {
            this.date = date;
        }

        @Override
        protected TimeSuggestions doInBackground(LocalDate... params) {
            Preconditions.checkArgument(params.length == 1);
            LocalDate date = params[0];
            final TimeSuggestionSyncLoader loader = new TimeSuggestionSyncLoader(getContext());
            try {
                return loader.load(date);
            } finally {
                loader.close();
            }
        }

        @Override
        protected void onPostExecute(TimeSuggestions timeSuggestions) {
            _data.setDate(date, timeSuggestions);
            getWrappedView().setDateTimes(
                    _data.getTimeSuggestions(), _data.id,
                    _data.getDate(), _data.getStartTime(), _data.getEndTime());
        }
    }

    private class ApplyActivityTypeEditTask extends AsyncTask<Long, Void, ActivityTypeSuggestionModel> {

        @Override
        protected ActivityTypeSuggestionModel doInBackground(Long... params) {
            checkArgument(params.length == 1);
            long activityTypeId = checkNotNull(params[0]);
            final ProjectSyncLoader projectSyncLoader = new ProjectSyncLoader(getContext());
            final ActivityTypeSyncLoader activityTypeSyncLoader = new ActivityTypeSyncLoader(getContext(), new CategorySyncLoader(getContext()));
            final ActivityTypeSuggestionSyncLoader suggestionSyncLoader = new ActivityTypeSuggestionSyncLoader(getContext(), projectSyncLoader, activityTypeSyncLoader);

            try {
                final Map<Long,ActivityTypeModel> modelMap = activityTypeSyncLoader.loadAsMap(MCContract.ActivityType._ID + "=?", new String[]{Long.toString(activityTypeId)}, MCContract.ActivityType._ID + " desc");
                final List<ActivityTypeSuggestionModel> r = suggestionSyncLoader.queryAll(modelMap,
                        ImmutableList.<Long>of() /*no project based suggestions*/,
                        LocalDate.now() /*no project limitations*/,
                        ImmutableSet.of(ActivityTypeSuggestionFeature.SUGGEST_ALL_ACTIVITIES, ActivityTypeSuggestionFeature.SUGGEST_ALL_PROJECTS)
                );

                // if there are multiple responses, the activity was probably used in multiple
                // projects. If there is no response, the user deleted it.
                final ActivityTypeSuggestionModel model = Iterables.getFirst(r, null);
                checkState(model == null || activityTypeId == model.getActivityTypeId());
                return model;
            } finally {
                // since this loader has the other loaders as dependencies, it will delete
                // resource releasing to them
                suggestionSyncLoader.close();
            }
        }

        @Override
        protected void onPostExecute(ActivityTypeSuggestionModel suggestion) {
            if (suggestion == null) {
                // activity type was deleted - reset data
                _data.setSelectedActivityType(null, null);
                _data.setSelectedCategory(null);
            } else {
                _data.setSelectedActivityType(suggestion.getActivityTypeId(), suggestion.getActivityName());
                _data.setSelectedCategory(suggestion.getCategory());
            }
            final TrackedActivityFragmentView view = getWrappedView();
            view.setActivitySuggestion(_data.getActivityTypeId(), _data.getActivityTypeName());
            view.setCategory(_data.getCategoryId(), _data.getCategoryName(), _data.getCategoryColorCode());
        }
    }

}
