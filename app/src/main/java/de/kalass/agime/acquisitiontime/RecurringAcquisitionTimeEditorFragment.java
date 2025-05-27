package de.kalass.agime.acquisitiontime;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import androidx.appcompat.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.common.base.Functions;
import com.google.common.base.Preconditions;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import de.kalass.agime.AgimeIntents;
import de.kalass.agime.R;
import de.kalass.agime.analytics.AnalyticsBaseCursorCRUDFragment;
import de.kalass.agime.ongoingnotification.OngoingNotificationManagingReceiver;
import de.kalass.android.common.activity.BaseViewWrapper;
import de.kalass.android.common.simpleloader.CursorUtil;
import de.kalass.android.common.simpleloader.HourMinute;
import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.simpleloader.Weekdays;
import de.kalass.android.common.simpleloader.Weekdays.Weekday;
import de.kalass.android.common.activity.CRUDMode;
import de.kalass.android.common.support.datetime.DatePickerSupport;
import de.kalass.android.common.support.datetime.TimePickerSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.kalass.android.common.simpleloader.CursorUtil.getIndex;
import static de.kalass.android.common.simpleloader.CursorUtil.putLocalDate;
import static de.kalass.android.common.simpleloader.CursorUtil.putWeekdays;
import static de.kalass.android.common.simpleloader.CursorUtil.putHourMinute;


/**
 * A placeholder fragment containing a simple view.
 */
public class RecurringAcquisitionTimeEditorFragment extends AnalyticsBaseCursorCRUDFragment<RecurringAcquisitionTimeEditorFragment.WrappedView, Cursor> implements View.OnClickListener, AdapterView.OnItemClickListener, CompoundButton.OnCheckedChangeListener, TimePickerSupport.LocalTimeSelectedListener {

	private static final String TAG = "EditorFragment";

	private EditableData _data;

	public RecurringAcquisitionTimeEditorFragment() {
		super(WrappedView.LAYOUT, Query.CONTENT_TYPE_DIR, Query.CONTENT_TYPE_ITEM, Query.PROJECTION, Functions.<Cursor> identity());
	}


	@Override
	protected CRUDMode getMode() {
		// we currently do not support a real view mode
		CRUDMode requestedMode = super.getMode();
		return requestedMode == CRUDMode.VIEW ? CRUDMode.EDIT : requestedMode;
	}


	@Override
	public WrappedView onWrapView(View view) {
		WrappedView result = new WrappedView(getContext(), view);
		result.setWeekdayAdapter(new WeekdaysAdapter(getContext()));
		return result;
	}


	@Override
	protected void onBindViewToCursor(WrappedView view, Cursor cursor) {
		Preconditions.checkArgument(getMode() == CRUDMode.INSERT || cursor != null, "Only new items do not provide data");
		_data = new EditableData(cursor);

		view.setCRUDMode(getMode());
		view.setTime(_data.startTime, _data.endTime);
		view.setWeekdays(_data.weekdays);
		view.setActive(_data.isCurrentlyActive(), _data.inactiveUntil);
		view.setActiveOnce(_data.activeOnce);

		view.weekdaysGrid.setOnItemClickListener(this);
		view.startTimeButton.setOnClickListener(this);
		view.endTimeButton.setOnClickListener(this);
		view.switchView.setOnCheckedChangeListener(this);

		view.currentAcquisitionUntil.setOnClickListener(this);

	}


	@Override
	protected void readDataFromView(WrappedView view, ContentValues result) {
		putLocalDate(result, Query.COLUMN_NAME_ACTIVE_ONCE_DATE, _data.activeOnce);
		putLocalDate(result, Query.COLUMN_NAME_INACTIVE_UNTIL, _data.inactiveUntil);
		putHourMinute(result, Query.COLUMN_NAME_START_TIME, _data.startTime);
		putHourMinute(result, Query.COLUMN_NAME_END_TIME, _data.endTime);
		putWeekdays(result, Query.COLUMN_NAME_WEEKDAY_PATTERN, _data.weekdays);
	}


	@Override
	public void onClick(View v) {
		int viewId = v.getId();
		if (viewId == WrappedView.ID_INACTIVE_UNTIL_DATE_BUTTON) {
			editInactiveUntilDate();
			return;
		}
		else if (viewId == WrappedView.ID_START_TIME_BUTTON) {
			TimePickerSupport.showTimePickerDialog(
				getContext(),
				getFragmentManager(),
				WrappedView.ID_START_TIME_BUTTON,
				this,
				_data.startTime);
			return;
		}
		else if (viewId == WrappedView.ID_END_TIME_BUTTON) {
			TimePickerSupport.showTimePickerDialog(
				getContext(),
				getFragmentManager(),
				WrappedView.ID_END_TIME_BUTTON,
				this,
				_data.endTime);
			return;
		}
	}


	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		int buttonId = buttonView.getId();
		if (buttonId == WrappedView.ID_ACTIVATION_SWITCH) {
			editActiveState(isChecked);
			return;
		}
	}


	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		int parentId = parent.getId();
		if (parentId == WrappedView.ID_WEEKDAYS_GRID) {
			toggleWeekday(parent, view, position);
			return;
		}
	}


	private void toggleWeekday(AdapterView<?> weekdaysView, View view, int position) {
		WeekdaysAdapter adapter = (WeekdaysAdapter)weekdaysView.getAdapter();
		// toggle the current value
		ToggleButton toggleButton = adapter.getToggleButton(view);
		boolean checked = toggleButton.isChecked();
		Weekday weekday = adapter.getItem(position);

		Log.i(TAG, "onItemClick weekdays grid " + weekday);
		if (!checked) {
			_data.weekdays.add(weekday);
		}
		else {
			_data.weekdays.remove(weekday);
		}
		toggleButton.setChecked(!checked);
	}


	private void editInactiveUntilDate() {
		DatePickerSupport.showDatePickerDialog(
			getContext(),
			getFragmentManager(),
			WrappedView.ID_INACTIVE_UNTIL_DATE_BUTTON,
			getString(R.string.acquisition_times_temporarily_disabled_title),
			new DatePickerSupport.LocalDateSelectedListener() {

				@Override
				public void onDateSelected(int token, LocalDate date) {
					_data.inactiveUntil = date;
					getWrappedView().setActive(_data.isCurrentlyActive(), _data.inactiveUntil);
				}
			},
			_data.inactiveUntil);
	}


	private void editActiveState(boolean checked) {
		Log.i(TAG, "active state " + checked);
		_data.inactiveUntil = checked ? null : new LocalDate().plusDays(1);
		getWrappedView().setActive(_data.isCurrentlyActive(), _data.inactiveUntil);

		if (!checked) {
			// ask the user to enter the time
			editInactiveUntilDate();
		}

	}


	@Override
	public void onTimeSelected(int token, LocalTime time) {
		switch (token) {
			case WrappedView.ID_END_TIME_BUTTON:
				endTimeSelected(time);
				break;
			case WrappedView.ID_START_TIME_BUTTON:
				startTimeSelected(time);
				break;
		}
	}


	private void startTimeSelected(LocalTime time) {
		_data.startTime = time;
		if (_data.startTime.isAfter(_data.endTime)) {
			_data.endTime = _data.startTime;
		}
		getWrappedView().setTime(_data.startTime, _data.endTime);
	}


	private void endTimeSelected(LocalTime time) {
		_data.endTime = time;
		if (_data.endTime.isBefore(_data.startTime)) {
			_data.startTime = _data.endTime;
		}
		getWrappedView().setTime(_data.startTime, _data.endTime);
	}


	@Override
	public void onEntityInserted(long entityId, Object payload) {
		sendReconfigureBroadcast();
		super.onEntityInserted(entityId, payload);
	}


	@Override
	public void onEntityDeleted(long entityId, Object payload) {
		sendReconfigureBroadcast();
		super.onEntityDeleted(entityId, payload);
	}


	@Override
	public void onEntityUpdated(long entityId, Object payload) {
		sendReconfigureBroadcast();
		super.onEntityUpdated(entityId, payload);
	}


	private void sendReconfigureBroadcast() {
		sendReconfigureBroadcast(getActivity());
	}


	public static void sendReconfigureBroadcast(Context context) {
		// This is not implemented as an observer but as an explicit intent to a broadcast receiver,
		// to make sure that reconfiguration of the ongoing notification managing service
		// happens even if it is currently not running.
		Intent intent = new Intent(context, OngoingNotificationManagingReceiver.class);
		intent.setAction(AgimeIntents.ACTION_ACQUISITION_TIME_CONFIGURE);
		context.sendBroadcast(intent);
	}

	static final class Query {

		static final String CONTENT_TYPE_DIR = MCContract.RecurringAcquisitionTime.CONTENT_TYPE_DIR;
		static final String CONTENT_TYPE_ITEM = MCContract.RecurringAcquisitionTime.CONTENT_TYPE_ITEM;
		static final String COLUMN_NAME_START_TIME = MCContract.RecurringAcquisitionTime.COLUMN_NAME_START_TIME;
		static final String COLUMN_NAME_END_TIME = MCContract.RecurringAcquisitionTime.COLUMN_NAME_END_TIME;
		static final String COLUMN_NAME_INACTIVE_UNTIL = MCContract.RecurringAcquisitionTime.COLUMN_NAME_INACTIVE_UNTIL;
		static final String COLUMN_NAME_ACTIVE_ONCE_DATE = MCContract.RecurringAcquisitionTime.COLUMN_NAME_ACTIVE_ONCE_DATE;
		static final String COLUMN_NAME_WEEKDAY_PATTERN = MCContract.RecurringAcquisitionTime.COLUMN_NAME_WEEKDAY_PATTERN;

		static final String[] PROJECTION = new String[] {
			MCContract.RecurringAcquisitionTime._ID,
			COLUMN_NAME_START_TIME,
			COLUMN_NAME_END_TIME,
			COLUMN_NAME_INACTIVE_UNTIL,
			COLUMN_NAME_ACTIVE_ONCE_DATE,
			COLUMN_NAME_WEEKDAY_PATTERN
		};

		static final int IDX_START_TIME = getIndex(PROJECTION, COLUMN_NAME_START_TIME);
		static final int IDX_END_TIME = getIndex(PROJECTION, COLUMN_NAME_END_TIME);
		static final int IDX_INACTIVE_UNTIL = getIndex(PROJECTION, COLUMN_NAME_INACTIVE_UNTIL);
		static final int IDX_ACTIVE_ONCE_DATE = getIndex(PROJECTION, COLUMN_NAME_ACTIVE_ONCE_DATE);
		static final int IDX_WEEKDAY_PATTERN = getIndex(PROJECTION, COLUMN_NAME_WEEKDAY_PATTERN);
	}

	static final class WrappedView extends BaseViewWrapper {

		static final int LAYOUT = R.layout.fragment_recurring_acquisition_time_editor;

		static final int ID_START_TIME_BUTTON = R.id.start_time;
		static final int ID_END_TIME_BUTTON = R.id.end_time;
		static final int ID_ACTIVATION_SWITCH = R.id.status_switch;
		static final int ID_INACTIVE_UNTIL_DATE_BUTTON = R.id.current_acquisition_until;
		static final int ID_WEEKDAYS_GRID = R.id.gridview;
		static final int ID_WEEKDAYS_ROW = R.id.weekdays_row;
		static final int ID_WEEKDAYS_LABEL = R.id.weekdays_label;
		static final int ID_HEADING_DATE = R.id.heading_date;
		static final int ID_HEADING_LABEL = R.id.heading_label;

		final Button startTimeButton;
		final Button endTimeButton;
		final GridView weekdaysGrid;
		final View weekdaysRow;
		final View weekdaysLabel;
		final SwitchCompat switchView;
		final View currentAcquisitionUntilRow;
		final View editRow;
		final TextView currentAcquisitionUntil;
		final View statusRow;
		final View statusRowInsert;
		final TextView headingDate;
		final View headingLabel;
		private final Context _context;

		WrappedView(Context context, View view) {
			super(view);
			_context = context;
			startTimeButton = getButton(ID_START_TIME_BUTTON);
			endTimeButton = getButton(ID_END_TIME_BUTTON);
			weekdaysGrid = get(GridView.class, ID_WEEKDAYS_GRID);
			weekdaysRow = getView(ID_WEEKDAYS_ROW);
			weekdaysLabel = getView(ID_WEEKDAYS_LABEL);

			switchView = get(SwitchCompat.class, ID_ACTIVATION_SWITCH);
			currentAcquisitionUntil = getTextView(ID_INACTIVE_UNTIL_DATE_BUTTON);
			editRow = getView(R.id.edit);
			currentAcquisitionUntilRow = getView(R.id.current_acquisition_until_row);
			statusRow = getView(R.id.status_row);
			statusRowInsert = getView(R.id.status_row_insert);

			headingDate = getTextView(ID_HEADING_DATE);
			headingLabel = getView(ID_HEADING_LABEL);
		}


		void setWeekdayAdapter(WeekdaysAdapter adapter) {
			weekdaysGrid.setAdapter(adapter);
		}


		WeekdaysAdapter getWeekdaysAdapter() {
			return (WeekdaysAdapter)weekdaysGrid.getAdapter();
		}


		void setTime(LocalTime startTime, LocalTime endTime) {
			startTimeButton.setText(HourMinute.serialize(startTime));
			endTimeButton.setText(HourMinute.serialize(endTime));
		}


		void setCRUDMode(CRUDMode mode) {
			if (mode == CRUDMode.INSERT) {
				statusRow.setVisibility(View.GONE);
				statusRowInsert.setVisibility(View.VISIBLE);
			}
			else {
				statusRow.setVisibility(View.VISIBLE);
				statusRowInsert.setVisibility(View.GONE);
			}
		}


		public void setActive(boolean currentlyActive, LocalDate inactiveUntil) {
			switchView.setChecked(currentlyActive);
			currentAcquisitionUntilRow.setVisibility(currentlyActive ? View.GONE : View.VISIBLE);
			editRow.setVisibility(currentlyActive ? View.VISIBLE : View.GONE);
			if (!currentlyActive) {
				currentAcquisitionUntil.setText(Util.formatLocalDate(_context, inactiveUntil));
			}

		}


		public void setWeekdays(Set<Weekday> weekdays) {
			getWeekdaysAdapter().setWeekdays(weekdays);
		}


		public void setActiveOnce(LocalDate activeOnce) {
			if (activeOnce != null) {
				setActive(true, null);
			}

			final int weekdaysVisibility = activeOnce == null ? View.VISIBLE : View.GONE;
			weekdaysLabel.setVisibility(weekdaysVisibility);
			weekdaysRow.setVisibility(weekdaysVisibility);

			final int disableableHeadingVisibility = activeOnce == null ? View.VISIBLE : View.GONE;
			switchView.setVisibility(disableableHeadingVisibility);

			final int activeOnceHeadingVisibility = activeOnce == null ? View.GONE : View.VISIBLE;
			headingDate.setVisibility(activeOnceHeadingVisibility);
			headingLabel.setVisibility(activeOnceHeadingVisibility);

			headingDate.setText(Util.formatLocalDate(_context, activeOnce));
		}
	}

	private static final class EditableData {

		final Set<Weekday> weekdays;
		LocalTime startTime;
		LocalTime endTime;
		LocalDate inactiveUntil;
		LocalDate activeOnce;

		EditableData(Cursor cursor) {
			if (cursor == null) {
				//
				inactiveUntil = null;
				weekdays = new HashSet<Weekday>(EnumSet.of(Weekday.MO, Weekday.TUE, Weekday.WED, Weekday.THU, Weekday.FR));
				startTime = new LocalTime(9, 0);
				endTime = new LocalTime(17, 0);
				activeOnce = null;
			}
			else {
				activeOnce = CursorUtil.getLocalDate(cursor, Query.IDX_ACTIVE_ONCE_DATE);
				inactiveUntil = CursorUtil.getLocalDate(cursor, Query.IDX_INACTIVE_UNTIL);
				weekdays = new HashSet<Weekday>(Weekdays.deserialize(cursor.getInt(Query.IDX_WEEKDAY_PATTERN)));
				startTime = HourMinute.deserialize(cursor.getString(Query.IDX_START_TIME));
				endTime = HourMinute.deserialize(cursor.getString(Query.IDX_END_TIME));
			}
		}


		boolean isCurrentlyActive() {
			return Util.isCurrentlyActive(inactiveUntil);
		}

	}

	private static class WeekdaysAdapter extends BaseAdapter {

		private final Context context;
		private Set<Weekday> weekdays;

		public WeekdaysAdapter(Context context) {
			this.context = context;
		}


		public void setWeekdays(Set<Weekday> weekdays) {
			this.weekdays = weekdays;
			notifyDataSetChanged();
		}


		@Override
		public int getCount() {
			return Weekday.values().length;
		}


		@Override
		public Weekday getItem(int position) {
			return Weekday.values()[position];
		}


		@Override
		public boolean hasStableIds() {
			return true;
		}


		@Override
		public long getItemId(int position) {
			return position;
		}


		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ToggleButton btn = (convertView != null) ? (ToggleButton)convertView : new ToggleButton(context);
			Weekday item = getItem(position);
			btn.setChecked(weekdays != null && weekdays.contains(item));
			String text = Weekdays.toUserVisibleString(context, item);
			btn.setTag(item);
			btn.setClickable(false);
			btn.setFocusable(false);
			//btn.setOnClickListener(this);
			btn.setText(text);
			btn.setTextOff(text);
			btn.setTextOn(text);
			return btn;
		}


		public ToggleButton getToggleButton(View view) {
			return (ToggleButton)view;
		}
	}
}
