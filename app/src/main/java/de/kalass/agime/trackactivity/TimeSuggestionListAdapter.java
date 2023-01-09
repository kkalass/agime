package de.kalass.agime.trackactivity;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.google.common.base.Objects;

import de.kalass.agime.R;
import de.kalass.agime.model.TrackedActivityModel;
import de.kalass.agime.timesuggestions.TimeSuggestion;
import de.kalass.android.common.adapter.AbstractListAdapter;

/**
* Created by klas on 21.01.14.
*/
class TimeSuggestionListAdapter extends AbstractListAdapter<TimeSuggestion> {
    private final Long _trackedActivityId;

    public TimeSuggestionListAdapter(Context context, Long trackedActivityId) {
        super(context, R.layout.time_suggestion_selection_list_item_selected, R.layout.time_suggestion_selection_list_item);
        _trackedActivityId = trackedActivityId;
    }


    private String toLabelSelected(TimeSuggestion suggestion) {
        if (_trackedActivityId != null && Objects.equal(_trackedActivityId, suggestion.getActivityItemId())) {
            return "";
        }
        switch (suggestion.getType()) {
            case ACTIVITY_BEGIN:
                return getString(R.string.time_suggestion_label_selected_activity_begin);
            case ACTIVITY_END:
                return getString(R.string.time_suggestion_label_selected_activity_end);
            case DAY_START:
                return getString(R.string.time_suggestion_label_selected_day_start);
            case DAY_END:
                return getString(R.string.time_suggestion_label_selected_day_end);
            case NOW:
                return getString(R.string.time_suggestion_label_selected_now);
            case WORKINGDAY_START:
                return getString(R.string.time_suggestion_label_selected_workingday_start);
        }
        return "";
    }
    private String getDisplayName(TimeSuggestion suggestion) {
        return TrackedActivityModel.getDisplayName(getContext(), suggestion.getLabel());
    }
    private String toLabel(TimeSuggestion suggestion) {
        if (_trackedActivityId != null && Objects.equal(_trackedActivityId, suggestion.getActivityItemId())) {
            return "";
        }
        switch (suggestion.getType()) {
            case ACTIVITY_BEGIN:
                return getString(R.string.time_suggestion_label_activity_begin, getDisplayName(suggestion));
            case ACTIVITY_END:
                return getString(R.string.time_suggestion_label_activity_end,  getDisplayName(suggestion));
            case DAY_START:
                return getString(R.string.time_suggestion_label_day_start);
            case DAY_END:
                return getString(R.string.time_suggestion_label_day_end);
            case NOW:
                return getString(R.string.time_suggestion_label_now);
            case WORKINGDAY_START:
                return getString(R.string.time_suggestion_label_workingday_start);
            case ANY_TIME:
                return getString(R.string.time_suggestion_label_anytime);
        }
        return "";
    }

    private String toTimeString(TimeSuggestion suggestion) {
        switch (suggestion.getType()) {
            case ANY_TIME:
                return "--:--";
        }
        return suggestion.getTimeMinutePrecision().toString("HH:mm");
    }

    private String toTimeStringSelected(TimeSuggestion suggestion) {
        return suggestion.getTimeMinutePrecision().toString("HH:mm");
    }

    @Override
    protected View fillView(View view, TimeSuggestion suggestion, int position) {
        TextView nameView = (TextView)view.findViewById(R.id.suggestion_name);
        TextView timeView = (TextView)view.findViewById(R.id.suggestion_time);
        String nameString = toLabelSelected(suggestion);
        String timeString = toTimeStringSelected(suggestion);
        timeView.setBackgroundColor(getContext().getResources().getColor(R.color.background));
        timeView.setText(timeString);
        nameView.setText(nameString);
        nameView.setTextColor(getContext().getResources().getColor(android.R.color.black));
        return view;
    }

    @Override
    protected View fillDropDownView(View view, TimeSuggestion suggestion, int position) {
        TextView nameView = (TextView)view.findViewById(R.id.suggestion_name);
        TextView timeView = (TextView)view.findViewById(R.id.suggestion_time);
        timeView.setBackgroundColor(getContext().getResources().getColor(R.color.background));
        String nameString = toLabel(suggestion);
        String timeString = toTimeString(suggestion);
        timeView.setText(timeString);
        nameView.setText(nameString);
        nameView.setTextColor(getContext().getResources().getColor(android.R.color.black));
        return view;
    }
}
