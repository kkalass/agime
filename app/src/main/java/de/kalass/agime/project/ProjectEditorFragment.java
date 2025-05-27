package de.kalass.agime.project;

import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.loader.content.Loader;
import androidx.appcompat.widget.SwitchCompat;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

import org.joda.time.LocalDate;

import de.kalass.agime.ColorSuggestion;
import de.kalass.agime.R;
import de.kalass.agime.analytics.AnalyticsBaseCRUDFragment;
import de.kalass.agime.color.ColorChooserAdapter;
import de.kalass.agime.loader.ProjectModelQuery;
import de.kalass.agime.model.ProjectModel;
import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.activity.BaseViewWrapper;
import de.kalass.android.common.activity.CRUDMode;
import de.kalass.android.common.activity.ContentResolverUtil;
import de.kalass.android.common.simpleloader.CompoundAsyncLoader;
import de.kalass.android.common.simpleloader.ObserveDataSourceMode;
import de.kalass.android.common.support.datetime.DatePickerSupport;
import de.kalass.android.common.util.StringUtil;
import de.kalass.android.common.util.TimeFormatUtil;


public class ProjectEditorFragment extends AnalyticsBaseCRUDFragment<ProjectEditorFragment.WrappedView, ProjectEditorFragment.ProjectEditorModel> implements View.OnClickListener, DatePickerSupport.LocalDateSelectedListener {

	private ProjectEditorModel _data;
	private Bundle savedInstanceState;

	public ProjectEditorFragment() {
		super(WrappedView.LAYOUT,
				MCContract.Project.CONTENT_TYPE_DIR,
				MCContract.Project.CONTENT_TYPE_ITEM);
	}


	@Override
	protected CRUDMode getMode() {
		// we currently do not support a real view mode
		CRUDMode requestedMode = super.getMode();
		return requestedMode == CRUDMode.VIEW ? CRUDMode.EDIT : requestedMode;
	}


	@Override
	public Loader<ProjectEditorModel> createLoader(int id, Bundle arg) {
		return new ProjectEditorModelAsyncLoader(getContext(), getEntityId());
	}


	@Override
	protected WrappedView onWrapView(View view) {
		WrappedView v = new WrappedView(view);
		v.setColorChooserAdapter(new ColorChooserAdapter(getContext()));
		return v;
	}


	@Override
	protected void onBindView(WrappedView view, ProjectEditorModel data) {
		view.name.setText(data.getName());
		view.setColor(data.getColorCode());
		view.durationLimited.setChecked(data.isProjectDurationLimited());
		view.setActiveUntilDay(data.getActiveUntilDay());

		if (savedInstanceState != null) {
			applyInstanceState(savedInstanceState, view);
			savedInstanceState = null;
		}

		view.activeUntilButton.setOnClickListener(this);

	}


	private void applyInstanceState(Bundle savedInstanceState, WrappedView view) {
		String name = savedInstanceState.getString("name");
		int color = savedInstanceState.getInt("color");
		boolean durationLimited = savedInstanceState.getBoolean("durationLimited", false);
		long activeUntilMillis = savedInstanceState.getLong("activeUntilMillis", 0);

		view.name.setText(name);
		view.setColor(color);
		if (activeUntilMillis != 0) {
			view.setActiveUntilDay(new LocalDate(activeUntilMillis));
		}
		view.durationLimited.setChecked(durationLimited);
	}


	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (outState != null) {
			WrappedView view = getWrappedView();
			LocalDate date = view.getActiveUntilDay();
			boolean durationLimited = view.durationLimited.isChecked();
			outState.putBoolean("durationLimited", durationLimited);
			outState.putLong("activeUntilMillis", date == null ? 0 : date.toDateTimeAtStartOfDay().getMillis());
			outState.putString("name", view.name.getText().toString());
			outState.putInt("color", (Integer)view.color.getSelectedItem());
		}
	}


	@Override
	public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
		super.onViewStateRestored(savedInstanceState);
		this.savedInstanceState = savedInstanceState;
	}


	@Override
	protected void save() {

		ContentValues values = readDataFromView(getWrappedView(), new ContentValues());
		final ContentProviderOperation operation = createSaveOrUpdateOperation(getMode(), values, System.currentTimeMillis());
		performSaveOrUpdateAsync(operation);
	}


	@Override
	protected void delete() {
		assertCanDelete();
		ProjectEditorDBUtil.delete(getContext(), this, ImmutableList.of(getEntityId()));
	}


	protected ContentValues readDataFromView(WrappedView view, ContentValues values) {

		Integer selectedColor = (Integer)view.color.getSelectedItem();
		String name = StringUtil.toString(view.name.getText());
		boolean limitedDuration = view.durationLimited.isChecked();
		Long activeUntilMillis = limitedDuration ? view.getActiveUntilDay().toDateTimeAtStartOfDay().getMillis() : null;

		values.put(MCContract.Project.COLUMN_NAME_NAME, name);
		values.put(MCContract.Project.COLUMN_NAME_COLOR_CODE, selectedColor);
		values.put(MCContract.Project.COLUMN_NAME_ACTIVE_UNTIL_MILLIS, activeUntilMillis);

		return values;
	}


	public void onClick(View v) {
		int viewId = v.getId();
		if (viewId == WrappedView.ID_ACTIVE_UNTIL_BUTTON) {
			onChangeDayClicked();
			return;
		}
	}


	public void onChangeDayClicked() {
		DatePickerSupport.showDatePickerDialog(
			getContext(),
			getFragmentManager(),
			WrappedView.ID_ACTIVE_UNTIL_BUTTON,
			this, getWrappedView().getActiveUntilDay());
	}


	@Override
	public void onDateSelected(int token, final LocalDate date) {
		switch (token) {
			case WrappedView.ID_ACTIVE_UNTIL_BUTTON:
				getWrappedView().setActiveUntilDay(date);
				break;
		}
	}

	static final class WrappedView extends BaseViewWrapper {

		static final int LAYOUT = R.layout.project_edit;
		static final int ID_NAME = R.id.project_name;
		static final int ID_COLOR_SPINNER = R.id.project_color_spinner;
		static final int ID_DURATION_LIMITED_SWITCH = R.id.duration_limited;
		static final int ID_ACTIVE_UNTIL_ROW = R.id.active_until_row;
		static final int ID_ACTIVE_UNTIL_BUTTON = R.id.active_until;

		final EditText name;
		final Spinner color;
		final SwitchCompat durationLimited;
		final LinearLayout activeUntilRow;
		final Button activeUntilButton;

		public WrappedView(View view) {
			super(view);
			name = getEditText(ID_NAME);
			color = getSpinner(ID_COLOR_SPINNER);
			durationLimited = get(SwitchCompat.class, ID_DURATION_LIMITED_SWITCH);
			activeUntilRow = get(LinearLayout.class, ID_ACTIVE_UNTIL_ROW);
			activeUntilButton = get(Button.class, ID_ACTIVE_UNTIL_BUTTON);

			durationLimited.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					activeUntilRow.setVisibility(isChecked ? View.VISIBLE : View.GONE);
				}
			});
		}


		public void setColorChooserAdapter(ColorChooserAdapter adapter) {
			color.setAdapter(adapter);
		}


		public ColorChooserAdapter getColorChooserAdapter() {
			return (ColorChooserAdapter)color.getAdapter();
		}


		public void setColor(Integer colorCode) {
			final ColorChooserAdapter adapter = getColorChooserAdapter();
			adapter.setColor(colorCode);
			int colorPosition = adapter.getColorPosition(colorCode);
			color.setSelection(Math.max(0, colorPosition));
		}


		public void setActiveUntilDay(LocalDate activeUntilDay) {
			activeUntilButton.setTag(activeUntilDay);
			activeUntilButton.setText(TimeFormatUtil.formatDate(getContext(), activeUntilDay));
		}


		public LocalDate getActiveUntilDay() {
			return (LocalDate)activeUntilButton.getTag();
		}
	}

	public final static class ProjectEditorModel {

		private final String _name;
		private final Integer _colorCode;
		private final LocalDate _activeUntilDate;
		private final boolean _projectDurationLimited;

		public ProjectEditorModel(String name, Integer colorCode, boolean projectDurationLimited, LocalDate activeUntilDate) {
			_name = name;
			_colorCode = colorCode;
			_projectDurationLimited = projectDurationLimited;
			_activeUntilDate = activeUntilDate;
		}


		public boolean isProjectDurationLimited() {
			return _projectDurationLimited;
		}


		public LocalDate getActiveUntilDay() {
			return _activeUntilDate == null ? LocalDate.now().minusDays(1) : _activeUntilDate;
		}


		public String getName() {
			return _name;
		}


		public Integer getColorCode() {
			return _colorCode;
		}


		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
				.addValue(_name)
				.toString();
		}
	}

	public final static class ProjectEditorModelAsyncLoader extends CompoundAsyncLoader<ProjectEditorModel> {

		private final Long _entityId;

		public ProjectEditorModelAsyncLoader(Context context, Long entityId) {
			super(context, ObserveDataSourceMode.IGNORE_CHANGES /*no reload during edit*/);
			_entityId = entityId;
		}


		@Override
		public ProjectEditorModel doLoadInBackground() {
			if (_entityId == null) {
				final int newProjectColor = loadNewProjectColor(getContext());
				return new ProjectEditorModel(null, newProjectColor, false /*duration unlimited*/, null);
			}
			final ProjectModel projectModel = loadFirst(ProjectModelQuery.READER,
				ContentUris.withAppendedId(MCContract.Project.CONTENT_URI, _entityId),
				ProjectModelQuery.PROJECTION);
			return new ProjectEditorModel(projectModel.getName(), projectModel.getColorCode(), projectModel.isProjectDurationLimited(),
					projectModel.getActiveUntilDate());
		}
	}

	public static int loadNewProjectColor(Context context) {
		int count = ContentResolverUtil.count(context.getContentResolver(), MCContract.Project.CONTENT_URI);
		return ColorSuggestion.suggestProjectColor(context.getResources(), count + 1, null);
	}

}
