package de.kalass.agime.acquisitiontime;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.google.common.collect.ImmutableList;
import com.linearlistview.LinearListView;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.util.List;
import java.util.Set;

import de.kalass.agime.R;
import de.kalass.agime.analytics.AnalyticsActionBarActivity;
import de.kalass.agime.analytics.AnalyticsActionToolBarActivity;
import de.kalass.android.common.simpleloader.HourMinute;
import de.kalass.android.common.simpleloader.Weekdays;
import de.kalass.android.common.activity.BaseListAdapter;
import de.kalass.android.common.simpleloader.CursorUtil;
import de.kalass.android.common.util.TimeFormatUtil;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by klas on 19.12.13.
 */
public class AcquisitionTimeManagementActivity extends AnalyticsActionToolBarActivity
        implements LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener, LinearListView.OnItemClickListener {
    private static final int RECURRING_LOADER_ID = 1;
    private WrappedView view;
    private Cursor _cursor;

    public AcquisitionTimeManagementActivity() {
        super(WrappedView.LAYOUT);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        view = new WrappedView(this);

        view.setAcquisitionTimesListAdapter(new RecurringItemListAdapter(this));

        view.acquisitionTimesList.setOnItemClickListener(this);

        view.insertAcquisitionTime.setOnClickListener(this);

        getSupportLoaderManager().initLoader(RECURRING_LOADER_ID, null, this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case WrappedView.ID_INSERT_RECURRING_ACQUISITION_TIME_BUTTON:
                startActivity(new Intent(Intent.ACTION_INSERT, RecurringDAO.CONTENT_URI));
                return;
        }
    }

    @Override
    public void onItemClick(LinearListView linearListView, View view, int position, long l) {
        switch (linearListView.getId()) {
            case WrappedView.ID_RECURRING_ACQUISITION_TIMES_LIST:
                long itemId = linearListView.getAdapter().getItemId(position);
                Intent editItemIntent = new Intent(Intent.ACTION_EDIT, ContentUris.withAppendedId(RecurringDAO.CONTENT_URI, itemId));
                startActivity(editItemIntent);
                return;
        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch(id) {
            case RECURRING_LOADER_ID:
                return new CursorLoader(
                    this, RecurringDAO.CONTENT_URI, RecurringDAO.PROJECTION,
                        RecurringDAO.selection(), RecurringDAO.selectionArgs(), null
                );
            default:
                // invalid ID was passed in
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch(loader.getId()) {
            case RECURRING_LOADER_ID:
                setCursor(cursor);

                List<RecurringDAO.Data> recurringItems = CursorUtil.readList(cursor, RecurringDAO.READ_DATA);
                onRecurringDataLoaded(recurringItems);
                break;
            default:
                // invalid ID was passed in
        }
    }

    private void setCursor(Cursor cursor) {
        Cursor oldCursor = _cursor;
        _cursor = cursor;
        if (oldCursor != null && oldCursor != cursor) {
            oldCursor.close();
        }
    }

    private void onRecurringDataLoaded(List<RecurringDAO.Data> recurringItems) {

        AcquisitionTimes times = AcquisitionTimes.fromRecurring(recurringItems, new DateTime());
        AcquisitionTimeInstance current = times.getCurrent();
        if (current == null) {
            view.currentAcquisitionActiveTime.setText(getString(R.string.acquisition_times_current_inactive));
            view.currentAcquisitionUntilTime.setVisibility(View.INVISIBLE);
        } else {
            view.currentAcquisitionActiveTime.setText(getString(R.string.acquisition_times_current_active));
            view.currentAcquisitionUntilTime.setVisibility(View.VISIBLE);
            view.currentAcquisitionUntilTime.setText(HourMinute.serialize(current.endTime));
        }
        AcquisitionTimeInstance next = times.getNext();
        if (next == null) {
            view.nextAcquisitionSecondLine.setVisibility(View.GONE);
            view.nextAcquisitionFirstLine.setText(R.string.acquisition_times_none_configured);
        } else {
            view.nextAcquisitionSecondLine.setVisibility(View.VISIBLE);

            view.nextAcquisitionFirstLine.setText(TimeFormatUtil.getInNumDays(getContext(), next.day));
            view.nextAcquisitionDate.setText(TimeFormatUtil.formatDate(getContext(), next.day));
            view.nextAcquisitionTime.setText(HourMinute.serialize(next.startTime) + " - " + HourMinute.serialize(next.endTime));
        }

        view.getAcquisitionTimesListAdapter().setItems(recurringItems);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch(loader.getId()) {
            case RECURRING_LOADER_ID:
                // ensure that the cursor is closed
                setCursor(null);
                view.getAcquisitionTimesListAdapter().setItems(null);
                break;
            default:
                // invalid ID was passed in
        }
    }


    protected final Context getContext() {
        return this;
    }

    public static final class WrappedView {
        public static final int LAYOUT = R.layout.acquisition_time_management_activity;

        public static final int ID_CURRENT_ACQUISITION_UNTIL_TEXT_VIEW = R.id.current_acquisition_until;
        public static final int ID_CURRENT_ACQUISITION_ACTIVE_TEXT_VIEW = R.id.current_acquisition_active;
        public static final int ID_NEXT_ACQUISITION_DAY_TEXT_VIEW = R.id.next_acquisition_day;
        public static final int ID_NEXT_ACQUISITION_TIME_TEXT_VIEW = R.id.next_acquisition_time;
        public static final int ID_NEXT_ACQUISITION_FIRST_LINE = R.id.next_acquisition_first_line;
        public static final int ID_NEXT_ACQUISITION_SECOND_LINE = R.id.next_acquisition_second_line;
        public static final int ID_INSERT_RECURRING_ACQUISITION_TIME_BUTTON = R.id.insert_aquisition_time;
        public static final int ID_RECURRING_ACQUISITION_TIMES_LIST = R.id.acquisition_times_list;

        private final LinearListView acquisitionTimesList;
        private final TextView currentAcquisitionUntilTime;
        private final TextView currentAcquisitionActiveTime;
        private final TextView nextAcquisitionFirstLine;
        private final View nextAcquisitionSecondLine;
        private final TextView nextAcquisitionDate;
        private final TextView nextAcquisitionTime;
        private final View insertAcquisitionTime;

        WrappedView(Activity view) {
            currentAcquisitionUntilTime = checkNotNull((TextView)view.findViewById(ID_CURRENT_ACQUISITION_UNTIL_TEXT_VIEW));
            currentAcquisitionActiveTime = checkNotNull((TextView)view.findViewById(ID_CURRENT_ACQUISITION_ACTIVE_TEXT_VIEW));
            nextAcquisitionDate = checkNotNull((TextView)view.findViewById(ID_NEXT_ACQUISITION_DAY_TEXT_VIEW));
            nextAcquisitionTime = checkNotNull((TextView)view.findViewById(ID_NEXT_ACQUISITION_TIME_TEXT_VIEW));
            nextAcquisitionFirstLine = checkNotNull((TextView)view.findViewById(ID_NEXT_ACQUISITION_FIRST_LINE));
            nextAcquisitionSecondLine = checkNotNull(view.findViewById(ID_NEXT_ACQUISITION_SECOND_LINE));
            insertAcquisitionTime = checkNotNull(view.findViewById(ID_INSERT_RECURRING_ACQUISITION_TIME_BUTTON));
            acquisitionTimesList = checkNotNull((LinearListView)view.findViewById(ID_RECURRING_ACQUISITION_TIMES_LIST));
        }

        public void setAcquisitionTimesListAdapter(RecurringItemListAdapter adapter) {
            acquisitionTimesList.setAdapter(adapter);
        }

        public RecurringItemListAdapter getAcquisitionTimesListAdapter() {
            return (RecurringItemListAdapter) acquisitionTimesList.getAdapter();
        }
    }

    private static class RecurringItemListAdapter extends BaseListAdapter<RecurringItemWrappedView, RecurringDAO.Data> {

        public RecurringItemListAdapter(Context context) {
            super(context, RecurringItemWrappedView.LAYOUT);
        }

        @Override
        protected RecurringItemWrappedView onWrapView(View view) {
            return new RecurringItemWrappedView(view);
        }

        @Override
        public void bindWrappedView(RecurringItemWrappedView view, RecurringDAO.Data item, int position) {
            Context context = getContext();
            // read data
            String startTimeString = HourMinute.serialize(item.startTime);
            String endTimeString = HourMinute.serialize(item.endTime);
            LocalDate inactiveUntil = item.inactiveUntil;
            boolean currentlyActive = item.isCurrentlyEnabled(new LocalDate(), new LocalTime());
            Set<Weekdays.Weekday> weekdays = item.weekdays;
            String weekdaysString = item.activeOnce != null
                    ? TimeFormatUtil.getInNumDays(context, item.activeOnce)
                    : Weekdays.toUserVisibleString(
                        context, weekdays
                    );

            // apply data to view
            view.checkBoxView.setChecked(currentlyActive);
            boolean showInactiveUntil = !currentlyActive && inactiveUntil != null;
            view.inactiveUntilLineView.setVisibility(showInactiveUntil ? View.VISIBLE : View.GONE);
            if (showInactiveUntil) {
                view.inactiveUntilDateView.setText(TimeFormatUtil.getInNumDays(context, inactiveUntil));
            }
            view.checkBoxView.setClickable(false);
            view.timespanView.setText(startTimeString + " - " + endTimeString);
            view.timespanView.setEnabled(currentlyActive);
            view.weekdaysView.setText(weekdaysString);
            view.weekdaysView.setEnabled(currentlyActive);
        }
    }

    private static final class RecurringItemWrappedView {
        public static final int LAYOUT = R.layout.acquisition_time_recurring_item;

        public static final int ID_CHECK_BOX = R.id.checkBox;
        public static final int ID_INACTIVE_UNTIL_ROW = R.id.inactive_until;
        public static final int ID_WEEKDAYS_TEXT_VIEW = R.id.weekdays;
        public static final int ID_TIMESPAN_TEXT_VIEW = R.id.timespan;
        public static final int ID_INACTIVE_UNTIL_DATE_TEXT_VIEW = R.id.inactive_until_date;

        private final CheckBox checkBoxView;
        private final View inactiveUntilLineView;
        private final TextView weekdaysView;
        private final TextView timespanView;
        private final TextView inactiveUntilDateView;

        RecurringItemWrappedView(View view) {
            checkBoxView = (CheckBox)view.findViewById(ID_CHECK_BOX);
            inactiveUntilLineView = view.findViewById(ID_INACTIVE_UNTIL_ROW);
            weekdaysView = (TextView)view.findViewById(ID_WEEKDAYS_TEXT_VIEW);
            timespanView = (TextView)view.findViewById(ID_TIMESPAN_TEXT_VIEW);
            inactiveUntilDateView = (TextView)view.findViewById(ID_INACTIVE_UNTIL_DATE_TEXT_VIEW);
        }
    }
}
