package de.kalass.agime.trackactivity.actionview;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.cocosw.undobar.UndoBarController;
import com.google.common.base.Preconditions;
import com.nineoldandroids.view.ViewHelper;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.kalass.agime.R;
import de.kalass.agime.acquisitiontime.AcquisitionTimeInstance;
import de.kalass.agime.acquisitiontime.AcquisitionTimeOps;
import de.kalass.agime.acquisitiontime.AcquisitionTimes;
import de.kalass.agime.acquisitiontime.RecurringAcquisitionTimeEditorFragment;
import de.kalass.agime.acquisitiontime.RecurringDAO;
import de.kalass.agime.model.TrackedActivityModel;
import de.kalass.agime.provider.MCContract;
import de.kalass.agime.trackactivity.InsertOrUpdateTrackedActivity;
import de.kalass.agime.trackactivity.RecurringSyncLoader;
import de.kalass.android.common.ApplyBatchTask;
import de.kalass.android.common.insertorupdate.InsertOrUpdateEntityTask;
import de.kalass.android.common.insertorupdate.InsertOrUpdateResult;
import de.kalass.android.common.insertorupdate.Operations;
import de.kalass.android.common.simpleloader.CursorUtil;
import de.kalass.android.common.simpleloader.ValueOrReference;
import de.kalass.android.common.util.DateUtil;
import de.kalass.android.common.util.TimeFormatUtil;

/**
 * Created by klas on 14.02.14.
 */
public class ActionViewController implements View.OnClickListener,
        UndoBarController.UndoListener {
    private static final int UP_TO_DATE_MINUTES_SINCE_ACTIVITY = 2;
    private static final long UP_TO_DATE_MILLIS_SINCE_ACTIVITY = TimeUnit.SECONDS.toMillis(UP_TO_DATE_MINUTES_SINCE_ACTIVITY * 60);

    private static final String LOG_TAG = "ActionViewController";
    private static final int BUTTON1_VISIBLE = View.VISIBLE;
    private final ActionViewWrapper _actionView;
    private final Activity _activity;
    private final Callback _callback;

    private LocalDate _date;
    private List<RecurringDAO.Data> _recurring;
    private boolean _isToday;

    public interface Callback {
        void onChangeDayClicked();

        void trackTime(Long startTimeMillis, Long endTimeMillis);

        void onExtendPreviousActivity(long id, TrackedActivityModel previousEntry);
    }
    public ActionViewController(Activity activity, Callback callback, View actionView) {
        _activity = activity;
        _callback = callback;
        _actionView = new ActionViewWrapper(actionView);

        _actionView.button1.setOnClickListener(this);
        _actionView.button2.setOnClickListener(this);
        _actionView.heading.setOnClickListener(this);
    }

    public View getView() {
        return _actionView.root;
    }
    private Context getContext() {
        return _activity;
    }

    private Activity getActivity() {
        return _activity;
    }

    private CharSequence getText(int resId) {
        return getContext().getText(resId);
    }

    private String getString(int resId) {
        return getContext().getString(resId);
    }

    private String getString(int resId, Object... formatArgs) {
        return getContext().getString(resId, formatArgs);
    }

    @Override
    public void onClick(View v) {
        ActionItem item = (ActionItem)v.getTag();
        switch (v.getId()) {
            case ActionViewWrapper.ID_BUTTON1:

                if (isStartAcquisitionTime(item)) {
                    onStartAcquisitionTime(item);
                } else if (item._mode == ActionItem.Mode.END_OF_DAY || item._mode == ActionItem.Mode.NOTHING) {
                    trackTime(DateUtil.getMillisAtStartOfDay(_date), null);
                } else {
                    trackTime(item._startTimeMillis, item._endTimeMillis);
                }
                return;
            case ActionViewWrapper.ID_BUTTON2:
                onExtendPreviousActivity((ActionItem)v.getTag());
                return;
                //onCloseActionItemHeading((ActionItem) v.getTag());
            case ActionViewWrapper.ID_HEADING:
                _callback.onChangeDayClicked();
                return;

        }
    }


    public void onStartAcquisitionTime(final ActionItem item) {
        final AcquisitionTimes acquisitionTimes = AcquisitionTimes.fromRecurring(_recurring, new DateTime());
        if (acquisitionTimes.hasCurrent()) {
            // - hmm, bad timing: the user waited exactly until the moment when the acquisition time
            // started - and managed to press the button right before the minutely update. Will ignore this
            Log.d(LOG_TAG, "bad timing: the user waited exactly until the moment when the acquisition time started");
            return;
        }
        
        if (!_isToday) {
            Log.w(LOG_TAG,  "start acquisition time should only be possible for 'today'");
            return;
        }
        final ContentProviderOperation operation = AcquisitionTimeOps.insert(
                _date, System.currentTimeMillis(), DateUtil.getNowMinutePrecision(), acquisitionTimes.getNext()
        );
        new ApplyBatchTask(getContext(),
                R.string.action_first_item_of_day_start_acquisition_time_error_title,
                R.string.action_first_item_of_day_start_acquisition_time_error_message) {
            @Override
            protected void onSuccess(Result result) {
                RecurringAcquisitionTimeEditorFragment.sendReconfigureBroadcast(getContext());
                Toast.makeText(getContext(), R.string.action_first_item_of_day_start_acquisition_time_success, Toast.LENGTH_SHORT).show();;
            }
        }.execute(operation);
    }

    public void onCloseActionItemHeading(ActionItem tag) {
        final AcquisitionTimeInstance instance = tag._acquisitionTime;
        if (instance == null) {
            Log.w(LOG_TAG, "onCloseActionItemHeading called, but there is no current acquisition time to cancel!");
            return;
        }
        TrackedActivityModel previousEntry = tag == null ? null : tag._previousEntry;
        final LocalTime nowTime = new LocalTime();
        final DateTime acquisitionEndDateTime = previousEntry == null ? null : previousEntry.getEndtimeDateTimeMinutes();
        final LocalTime acquisitionEndTime =
                (acquisitionEndDateTime != null && acquisitionEndDateTime.toLocalDate().equals(_date))
                        ? acquisitionEndDateTime.toLocalTime() : null;
        final LocalTime endTimeSuggestion = nowTime.minusMinutes(1);

        // The user explicitely asked for ending acquisition time.
        // we will use the end time of the last activity to document that the user
        // does not intend to enter any more activities after that time
        final LocalTime endTime = (acquisitionEndTime != null && acquisitionEndTime.isBefore(endTimeSuggestion))
                ? acquisitionEndTime
                : endTimeSuggestion;

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.action_item_of_day_end_acquisition_time_message)
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public ArrayList<ContentProviderOperation> createEndCurrentAcquisitionTimeOperations(final AcquisitionTimeInstance instance) {
                        long now = System.currentTimeMillis();
                        final LocalDate nowDate = new LocalDate();
                        final List<RecurringDAO.Data> items = instance.getItems();
                        final ArrayList<ContentProviderOperation> result = new ArrayList<ContentProviderOperation>(items.size());
                        for (RecurringDAO.Data item : items) {
                            ContentProviderOperation.Builder b = ContentProviderOperation
                                    .newUpdate(ContentUris.withAppendedId(MCContract.RecurringAcquisitionTime.CONTENT_URI, item.getId()))
                                    .withExpectedCount(1);
                            ContentValues values = new ContentValues();
                            values.put(MCContract.RecurringAcquisitionTime.COLUMN_NAME_MODIFIED_AT, now);
                            if (item.isActiveOnce()) {

                                if (!item.getStartTime().isBefore(endTime)) {
                                    CursorUtil.putHourMinute(values, MCContract.RecurringAcquisitionTime.COLUMN_NAME_START_TIME, endTime /*now*/);
                                }
                                CursorUtil.putHourMinute(values, MCContract.RecurringAcquisitionTime.COLUMN_NAME_END_TIME, endTime /*now*/);
                            } else {

                                CursorUtil.putLocalDate(values, MCContract.RecurringAcquisitionTime.COLUMN_NAME_INACTIVE_UNTIL, nowDate.plusDays(1));
                            }
                            b.withValues(values);

                            result.add(b.build());
                        }
                        return result;
                    }

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        ArrayList<ContentProviderOperation> operations = createEndCurrentAcquisitionTimeOperations(instance);

                        new ApplyBatchTask(getActivity(),
                                R.string.action_item_of_day_end_acquisition_time_error_title,
                                R.string.action_item_of_day_end_acquisition_time_error_message)
                                .execute(operations.toArray(new ContentProviderOperation[operations.size()]));

                    }
                })
                .show();
    }

    private void trackTime(Long startTimeMillis, Long endTimeMillis) {
        _callback.trackTime(startTimeMillis, endTimeMillis);
    }

    @Override
    public void onUndo(Parcelable token) {
        if (token instanceof Bundle) {
            onUndoExtendPreviousActivity((Bundle) token);
        }
    }

    private void onUndoExtendPreviousActivity(Bundle b) {
        long id = b.getLong("id");
        long endTimeMillis = b.getLong("endTimeMillis");

        final ContentProviderOperation operation = ContentProviderOperation
                .newUpdate(ContentUris.withAppendedId(MCContract.Activity.CONTENT_URI, id))
                .withValue(MCContract.Activity.COLUMN_NAME_END_TIME, endTimeMillis)
                .withValue(MCContract.Activity.COLUMN_NAME_MODIFIED_AT, System.currentTimeMillis())
                .build();

        new ApplyBatchTask(getContext(),
                R.string.extend_previous_action_undo_error_title,
                R.string.extend_previous_action_undo_error_message) {

            @Override
            protected void onSuccess(Result result) {
                Toast.makeText(getContext(), R.string.extend_previous_action_reversed, Toast.LENGTH_LONG).show();
            }
        }.execute(operation);
    }




    private void onExtendPreviousActivity(final ActionItem item) {
        long id = Preconditions.checkNotNull(item.getPreviousEntryId(), "No previous item known");
        final TrackedActivityModel previousEntry = item._previousEntry;
        _callback.onExtendPreviousActivity(id, previousEntry);
    }

    public void internalExtendPreviousActivity(final long id,  TrackedActivityModel model, final Runnable success) {
        final DateTime previousStartTime = model.getStarttimeDateTimeMinutes();
        final DateTime previousEndTime = model.getEndtimeDateTimeMinutes();
        final long newEndTime = DateUtil.getNowMinutePrecisionMillis();

        new InsertOrUpdateEntityTask<TrackedActivityModel, Void, Void>(getContext(), MCContract.CONTENT_AUTHORITY) {

            @Override
            protected Long getId(TrackedActivityModel input) {
                return input.getId();
            }

            protected void onSuccess(InsertOrUpdateResult<Void> result) {
                success.run();
                    Bundle undoArgs = new Bundle();
                    undoArgs.putLong("id", id);

                /*
                 * Undo: set back to the start of the time period which was extended
                 */
                final long previousEndTimeMillis = previousEndTime.getMillis();
                undoArgs.putLong("endTimeMillis", previousEndTimeMillis);
                new UndoBarController.UndoBar(getActivity())
                        .message(getString(R.string.undo_expand_message, TimeFormatUtil.formatDuration(getActivity(), newEndTime - previousEndTimeMillis)))
                        .listener(ActionViewController.this)

                        .token(undoArgs).show();
            }

            @Override
            protected Operations<Void> createOperations(boolean isInsert, Long id, TrackedActivityModel input, long now) {

                List<RecurringDAO.Data> acquisitionTimes = new RecurringSyncLoader(getContext()).loadRecurring();

                final ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
                ops.add(ContentProviderOperation
                        .newUpdate(ContentUris.withAppendedId(MCContract.Activity.CONTENT_URI, id))
                        .withValue(MCContract.Activity.COLUMN_NAME_END_TIME, newEndTime)
                        .withValue(MCContract.Activity.COLUMN_NAME_MODIFIED_AT, System.currentTimeMillis())
                        .build());
                InsertOrUpdateTrackedActivity.adjustAcquisitionTime(
                        acquisitionTimes,
                        previousStartTime,
                        new DateTime(newEndTime),
                        System.currentTimeMillis(),
                        ops
                );
                return Operations.getInstance(ops, ValueOrReference.ofValue(id));
            }

        }.execute(model);


    }


    public void bind(ActionItem actionItemData, LocalDate date, boolean isToday, List<RecurringDAO.Data> recurring) {

        _isToday = isToday;
        _date = date;
        _recurring = recurring;

        if (actionItemData == null) {
            _actionView.setButtonsVisibility(View.GONE);
            _actionView.details.setVisibility(View.GONE);
            _actionView.heading.setVisibility(View.GONE);
            //_actionView.view.setVisibility(View.GONE);
            return;
        }
        _actionView.setButtonsVisibility(View.VISIBLE);
        _actionView.details.setVisibility(View.VISIBLE);
        _actionView.heading.setVisibility(View.VISIBLE);
        //_actionView.view.setVisibility(View.VISIBLE);
        bindActionViewHeader(_actionView, actionItemData);
    }

    private void bindActionViewHeader(ActionViewWrapper actionView, ActionItem actionItemData) {
        actionView.button1.setTag(actionItemData);
        actionView.button2.setTag(actionItemData);
        actionView.headingTextIcon.setVisibility(View.GONE);
        switch (actionItemData._mode) {
            case START_OF_DAY:
            case START_OF_DAY_ACQUISITION_TIME_PASSED:
            case START_OF_DAY_NO_ACQUISITION_TIME:
                bindFirstActionOfDay(actionView, actionItemData);
                return;
            case NEW_ENTRY:
            case NEW_ENTRY_BEFORE_ACQUISITION_TIME:
                bindNewEntryNeeded(actionView, actionItemData);
                return;
            case END_OF_DAY_PAST:
            case END_OF_DAY:
                bindEndOfDay(actionView, actionItemData);
                return;
            case NOTHING:
                bindNothing(actionView, actionItemData);
                return;
        }
    }

    private void bindNewEntryNeeded(ActionViewWrapper actionView, ActionItem actionItemData) {
        final TrackedActivityModel previousEntry = Preconditions.checkNotNull(actionItemData._previousEntry);
        actionView.button1.setVisibility(View.GONE);
        actionView.button1.setText(R.string.action_item_of_day_action);
        actionView.button2.setVisibility(View.VISIBLE);
        final String previousTitle = previousEntry == null ? null : previousEntry.getDisplayName(getContext());
        actionView.button2.setText(Html.fromHtml(getString(R.string.action_item_of_day_extend_previous_action, previousTitle, Long.toString(actionItemData.getDurationMinutes()))));

        if (actionItemData.getDurationMinutes() < UP_TO_DATE_MINUTES_SINCE_ACTIVITY) {
            actionView.headingText.setText(getText(R.string.action_fresh_item_of_day_heading));
            if (previousEntry != null && previousEntry.getEntryDurationMillis() != 0) {
                final String text = getString(R.string.action_fresh_item_of_day_message_with_previous_details,
                        TimeFormatUtil.formatDurationSeconds(getActivity(), previousEntry.getEntryDurationMillis()),
                        previousTitle,
                        TimeFormatUtil.formatDuration(getActivity(), previousEntry.getDurationMillis())
                );
                actionView.details.setText(Html.fromHtml(text));
            } else {
                actionView.details.setVisibility(View.GONE);
            }
            actionView.setButtonsVisibility(View.VISIBLE);
            return;
        }
        actionView.headingText.setText(getText(R.string.action_next_item_of_day_heading));
        if (previousEntry == null) {
            final String text = getText(R.string.action_next_item_of_day_message_no_title).toString();
            actionView.details.setText(Html.fromHtml(String.format(text,
                    Long.toString(actionItemData.getDurationMinutes()),
                    TimeFormatUtil.formatTime(getContext(), actionItemData._startTimeMillis)
            )));
        } else {
            final String text = getText(R.string.action_next_item_of_day_message).toString();
            actionView.details.setText(Html.fromHtml(String.format(text,
                    Long.toString(actionItemData.getDurationMinutes()),
                    TimeFormatUtil.formatTime(getContext(), actionItemData._startTimeMillis),
                    previousTitle
            )));
        }

    }

    private void bindFirstActionOfDay(ActionViewWrapper actionView, ActionItem actionItemData) {
        /*
        actionView.heading.setText(getText(R.string.welcome_title));
        actionView.details.setText(getText(R.string.welcome_message));
        */

        actionView.headingText.setText(getText(R.string.action_first_item_of_day_heading));

        actionView.button1.setVisibility(View.VISIBLE);

        if (isStartAcquisitionTime(actionItemData)) {
            actionView.details.setText(Html.fromHtml(getString(R.string.action_first_item_of_day_message_no_minutes)));
            actionView.button1.setText(R.string.action_first_item_of_day_start_acquisition_time);
        } else {
            if (actionItemData.getDurationMinutes() < 5) {
                if (actionItemData._mode == ActionItem.Mode.START_OF_DAY) {
                    actionView.details.setText(Html.fromHtml(getString(R.string.action_first_item_of_day_message_acquisition_time_started, formatEndTime(actionItemData))));
                } else {
                    actionView.details.setText(Html.fromHtml(getString(R.string.action_first_item_of_day_message_no_minutes)));
                }

            } else {
                actionView.details.setText(Html.fromHtml(getString(R.string.action_first_item_of_day_message, Long.toString(actionItemData.getDurationMinutes()), TimeFormatUtil.formatTime(getContext(), actionItemData._startTimeMillis))));
            }
            actionView.button1.setText(R.string.action_item_of_day_action);
        }
        actionView.button2.setVisibility(View.GONE);
    }

    private CharSequence formatEndTime(ActionItem actionItemData) {
        final AcquisitionTimeInstance atime = actionItemData._acquisitionTime;
        if (atime == null) {
            return getString(R.string.end_time_unknown);
        }

        final DateTime endDateTime = atime.getEndDateTime();
        LocalTime lt = endDateTime.toLocalTime();
        if (lt.getMinuteOfHour() == 59 && lt.getHourOfDay() == 23) {
            return getString(R.string.end_time_end_of_day);
        }
        return TimeFormatUtil.formatTime(getContext(), endDateTime.getMillis());
    }

    private boolean isStartAcquisitionTime(ActionItem actionItemData) {
        return actionItemData._mode == ActionItem.Mode.START_OF_DAY_NO_ACQUISITION_TIME
                || actionItemData._mode == ActionItem.Mode.START_OF_DAY_ACQUISITION_TIME_PASSED;
    }


    private void bindEndOfDay(ActionViewWrapper actionView, ActionItem actionItemData) {
        // the action item will contain the previous acquisition time of today, if available
        //final AcquisitionTimeInstance previousAcquisitionTimeToday = actionItemData._acquisitionTime;
        final TrackedActivityModel previousEntry = actionItemData._previousEntry;
        boolean previousAcquisitionTimeUnfulfilled = actionItemData.isPreviousAcquisitionTimeUnfulfilled();

        if (actionItemData._mode == ActionItem.Mode.END_OF_DAY) {
            actionView.heading.setVisibility(View.VISIBLE);
        } else {
            actionView.heading.setVisibility(View.VISIBLE);
        }

        double percentage = 100.0 * ((double)actionItemData.getTotalDayEntryDurationMillis() / (double)actionItemData.getDurationMillis());
        //actionView.heading.setText(getText(R.string.action_last_item_of_day_heading));


        if (previousAcquisitionTimeUnfulfilled) {

            actionView.button1.setText(R.string.action_item_of_day_action);
            actionView.setButtonsVisibility(View.VISIBLE);
            if (previousEntry != null) {
                actionView.button1.setVisibility(View.GONE);
                final long durationMillis = DateUtil.getNowMinutePrecisionMillis() - previousEntry.getEndTimeMillis();
                long durationMinutes = durationMillis/60000;
                actionView.button2.setVisibility(View.VISIBLE);
                final String previousTitle = previousEntry.getDisplayName(getContext());
                actionView.button2.setText(Html.fromHtml(getString(R.string.action_item_of_day_extend_previous_action, previousTitle,  Long.toString(durationMinutes))));
            } else {
                actionView.button1.setVisibility(View.VISIBLE);
                actionView.button2.setVisibility(View.GONE);
            }
            actionView.headingText.setText(getText(R.string.action_last_item_of_day_heading_unfinished));

            if (previousEntry == null) {
                final String text = getText(R.string.action_next_item_of_day_message_no_title_unfinished).toString();
                actionView.details.setText(Html.fromHtml(text
                ));
            } else {
                final long durationMillis = DateUtil.getNowMinutePrecisionMillis() - previousEntry.getEndTimeMillis();
                long durationMinutes = durationMillis/60000;
                actionView.details.setText(Html.fromHtml(getString(R.string.action_next_item_of_day_message_unifished,
                        Long.toString(durationMinutes),
                        previousEntry.getDisplayName(getContext())
                )));
            }


        } else {
            final String text = getText(
                    actionItemData._mode == ActionItem.Mode.END_OF_DAY
                            ? R.string.action_last_item_of_today_message
                            : R.string.action_last_item_of_day_message).toString();
            actionView.details.setText(Html.fromHtml(String.format(text,
                    TimeFormatUtil.formatDurationSeconds(getActivity(), actionItemData.getTotalDayEntryDurationMillis()),
                    percentage
            )));
            actionView.headingText.setText(actionItemData._mode == ActionItem.Mode.END_OF_DAY ?
                    getText(R.string.action_last_item_of_day_heading)
                    : TimeFormatUtil.formatTimeForHeader(getContext(), _date));
            actionView.setButtonsVisibility(View.VISIBLE);
            if (actionItemData._mode != ActionItem.Mode.END_OF_DAY) {
                actionView.headingTextIcon.setVisibility(View.VISIBLE);
            }
            actionView.button1.setText(R.string.action_supplement);
            actionView.button1.setVisibility(View.VISIBLE);
            actionView.button2.setVisibility(View.GONE);

        }

    }

    private void bindNothing(ActionViewWrapper actionView, ActionItem actionItemData) {
        actionView.headingTextIcon.setVisibility(View.VISIBLE);
        actionView.headingText.setText(TimeFormatUtil.formatTimeForHeader(getContext(), _date));
        actionView.heading.setVisibility(View.VISIBLE);
        //actionView.details.setVisibility(View.GONE);
        actionView.details.setText(R.string.fragment_tracked_activities_empty_text);
        actionView.setButtonsVisibility(View.VISIBLE);
        actionView.button1.setText(R.string.action_supplement);
        actionView.button1.setVisibility(View.VISIBLE);
        actionView.button2.setVisibility(View.GONE);
    }

    public void reduceHeight(int initialHeight, int currentHeight, int targetHeight) {
        int currentDiffToTargetHeight = Math.max(0, currentHeight - targetHeight);
        int initialDiffToTargetHeight = initialHeight - targetHeight;

        float scale = (float)currentDiffToTargetHeight / (float)initialDiffToTargetHeight;

        reduceTitle(scale);
        reduceDetails(initialDiffToTargetHeight, scale);
        reduceButtons(initialDiffToTargetHeight, scale);
    }

    protected void reduceButtons(int initialDiffToTargetHeight, float scale) {
        // within the first two thirds of transformation, keep alpha for buttons at 1.0, then set it from 1.0 to 0.0 within the third
        // simultaneously change speed of transformation to half speed once the fadeout starts
        float buttonTyFirstThirds =  -initialDiffToTargetHeight * (1.0f - Math.max(1f/3f, scale));
        float buttonTyLastThird =  -initialDiffToTargetHeight* (1.0f - Math.min(1.0f, scale + 2f/3f))/2f;
        ViewHelper.setTranslationY(_actionView.buttons, buttonTyFirstThirds + buttonTyLastThird );
        float buttonAlphaScale = Math.min(1.0f, Math.max(0,  scale)*3f);
        ViewHelper.setAlpha(_actionView.buttons, buttonAlphaScale);
        if (buttonAlphaScale <= 0.001f) {
            _actionView.buttons.setVisibility(View.GONE);
        } else {
            _actionView.restoreButtonsVisibility();
        }
        //Log.i("Frg", "reduceButtons(" + initialDiffToTargetHeight + ", " + scale + ") -> ty: " +buttonTyFirstThirds  + ", alpha: " + buttonAlphaScale + "") ;
    }

    protected void reduceDetails(int initialDiffToTargetHeight, float scale) {
        // within the first third of transformation, set the alpha for details from 1.0 to 0.0
        // and simultaneously move the details at half speed compared to the total movement
        ViewHelper.setTranslationY(_actionView.details, -initialDiffToTargetHeight * (1.0f - scale) / 2f);

        float detailsScale = Math.max(0,  scale - 2f/3f )*3f;
        ViewHelper.setAlpha(_actionView.details, detailsScale);
    }

    protected void reduceTitle(float scale) {
        //float headerTargetFactor = (float)getContext().getResources().getDimensionPixelSize(R.dimen.text_size_title_material_toolbar)/(float)getContext().getResources().getDimensionPixelSize(R.dimen.text_size_headline_material);
        float textSizeInitial = getDimensionPixelSize(R.dimen.chronicle_toolbar_header_text_size_initial);
        float headerTargetFactor = getDimensionPixelSize(R.dimen.chronicle_toolbar_header_text_size_target)/textSizeInitial;
        float headerTargetTranslationY = getDimensionPixelSize(R.dimen.chronicle_toolbar_header_margin_top_target) - getDimensionPixelSize(R.dimen.chronicle_toolbar_header_margin_top_initial);

        downscale(_actionView.heading, headerTargetFactor, scale);
        /*
        float headingScale = headerTargetFactor + (1.0f - headerTargetFactor) * scale;
        _actionView.headingText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeInitial * headingScale);
*/
        ViewHelper.setTranslationY(_actionView.heading, headerTargetTranslationY * (1.0f - scale));
    }

    protected float getDimensionPixelSize(int resId) {
        return (float)getContext().getResources().getDimensionPixelSize(resId);
    }

    protected void downscale(View view, float targetScaleFactor, float scale) {

        float headingScale = targetScaleFactor + (1.0f-targetScaleFactor) * scale;

        ViewHelper.setScaleY(view, headingScale);
        ViewHelper.setScaleX(view, headingScale);
        ViewHelper.setPivotX(view, 0);
        ViewHelper.setPivotY(view, 0);
        //
    }
}
