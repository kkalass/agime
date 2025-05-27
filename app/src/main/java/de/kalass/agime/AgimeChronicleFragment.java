package de.kalass.agime;

import android.app.Activity;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.kalass.agime.loader.TrackedActivityListData;
import de.kalass.agime.model.TrackedActivityModel;
import de.kalass.agime.provider.MCContract;
import de.kalass.agime.trackactivity.TrackActivity;
import de.kalass.agime.trackactivity.TrackedActivitiesListFragment;
import de.kalass.agime.trackactivity.actionview.ActionItem;
import de.kalass.agime.trackactivity.actionview.ActionItemFactory;
import de.kalass.agime.trackactivity.actionview.ActionViewController;
import de.kalass.android.common.support.datetime.DatePickerSupport;


/**
 * Created by klas on 06.10.13.
 */
public class AgimeChronicleFragment extends AbstractViewPagerFragment implements LocalDateSpanning, DatePickerSupport.LocalDateSelectedListener, ResizableToolbarHelper.ToolbarResizeCallback, TrackedActivitiesListFragment.TrackedActivitiesListeningFragment, ActionViewController.Callback {

	public static final int MAX_YEARS = 10;// 10 years back in time for agime should suffice...
	public static final int MAX_DAYS = MAX_YEARS * 365;

	private static final int ACTIVITY_CODE_TRACK_TIME = 42;
	private ActionItem _actionItemData;
	private View _customToolbar;
	private int _currentDaysBeforeToday;

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("currentDaysBeforeToday", _currentDaysBeforeToday);
	}


	@Override
	public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
		super.onViewStateRestored(savedInstanceState);
		if (savedInstanceState != null) {
			_currentDaysBeforeToday = savedInstanceState.getInt("currentDaysBeforeToday");
		}
	}


	@Override
	public void adjustCustomToolbarHeight(int initialHeight, int currentHeight, int targetHeight) {
		TrackedActivitiesListPagerAdapter pagerAdapter = getPagerAdapter();
		for (Fragment cached : pagerAdapter.getCachedFragments()) {
			if (cached instanceof ResizableToolbarHelper.ToolbarResizeCallback) {
				((ResizableToolbarHelper.ToolbarResizeCallback)cached).adjustCustomToolbarHeight(initialHeight, currentHeight, targetHeight);
			}

		}
		//ViewHelper.setTranslationY(_customToolbar, -scrollY);
		_actionView.reduceHeight(initialHeight, currentHeight, targetHeight);
	}


	protected int getActionBarSize() {
		TypedValue typedValue = new TypedValue();
		int[] textSizeAttr = new int[] {androidx.appcompat.R.attr.actionBarSize};
		int indexOfAttrTextSize = 0;
		TypedArray a = getActivity().obtainStyledAttributes(typedValue.data, textSizeAttr);
		int actionBarSize = a.getDimensionPixelSize(indexOfAttrTextSize, -1);
		a.recycle();
		return actionBarSize;
	}


	private TrackedActivitiesListPagerAdapter getPagerAdapter() {
		return (TrackedActivitiesListPagerAdapter)getViewPager().getAdapter();
	}


	public void onExtendPreviousActivity(final long id, TrackedActivityModel item) {
		_actionView.internalExtendPreviousActivity(id, item, new Runnable() {

			@Override
			public void run() {
				TrackedActivitiesListFragment currentFragment = getCurrentListFragment();
				if (currentFragment != null) {
					currentFragment.scrollToItemAndExpandHeader(id);
				}

			}
		});
	}


	protected TrackedActivitiesListFragment getCurrentListFragment() {
		TrackedActivitiesListPagerAdapter pagerAdapter = getPagerAdapter();
		int currentItem = getCurrentItem();
		return (TrackedActivitiesListFragment)pagerAdapter.getCached(currentItem);
	}

	public class TrackedActivitiesListPagerAdapter extends FragmentStatePagerAdapter {

		private Map<Integer, Fragment> fragmentCache = new HashMap<Integer, Fragment>();

		public TrackedActivitiesListPagerAdapter(FragmentManager fm) {
			super(fm);
		}


		@Override
		public void setPrimaryItem(ViewGroup container, int position, Object object) {
			super.setPrimaryItem(container, position, object);
			// clear the cache of fragments: this uses knowledge about caching
			// behaviour. Maybe there is a cleaner way to clean our cache?
			Set<Integer> removeKeys = null;
			for (Integer cachedPos : fragmentCache.keySet()) {
				// convert to int primary type
				int cachedPosition = cachedPos;
				if (cachedPosition < position - 3 || cachedPosition > position + 3) {
					if (removeKeys == null) {
						removeKeys = new HashSet<Integer>();
					}
					removeKeys.add(cachedPos);
				}
			}
			if (removeKeys != null) {
				for (Integer p : removeKeys) {
					fragmentCache.remove(p);
				}
			}
		}


		public Fragment getCached(int pos) {
			return fragmentCache.get(pos);
		}


		@Override
		public Fragment getItem(int i) {
			Fragment c = getCached(i);
			if (c != null) {
				return c;
			}
			//Log.i("AgimeMainActivity", "getItem " + i);
			TrackedActivitiesListFragment currentListFragment = new TrackedActivitiesListFragment();
			Bundle args = new Bundle();
			int daysBeforeTodayAtPosition = getDaysBeforeTodayAtPosition(i);
			args.putInt(
				TrackedActivitiesListFragment.ARG_DAYS_BEFORE_TODAY,
				daysBeforeTodayAtPosition);
			args.putInt(TrackedActivitiesListFragment.ARG_ITEM_ID, i);
			currentListFragment.setArguments(args);

			fragmentCache.put(i, currentListFragment);
			return currentListFragment;
		}


		@Override
		public int getCount() {
			return MAX_DAYS;
		}


		@Override
		public CharSequence getPageTitle(int position) {
			return "";
			/*
			if (position == MAX_DAYS - 1) {
			    return "";
			}
			return TrackedActivitiesListFragment.getPageTitle(getActivity(), getDaysBeforeTodayAtPosition(position));
			*/
		}


		public Collection<Fragment> getCachedFragments() {
			return fragmentCache.values();
		}
	}

	private final Map<LocalDate, TrackedActivityListData> _cache = new HashMap<LocalDate, TrackedActivityListData>();

	private final ViewPager.OnPageChangeListener changeListener = new PageChangeActivityTitleSync(this) {

		@Override
		public void onPageSelected(final int position) {
			super.onPageSelected(position);
			_currentDaysBeforeToday = getDaysBeforeTodayAtPosition(position);

			LocalDate date = getStartDate(position);
			Log.i("Frg", "page selected " + position);
			doRefresh(date, _cache.get(date));
			TrackedActivitiesListPagerAdapter pagerAdapter = getPagerAdapter();
			TrackedActivitiesListFragment currentFragment = (TrackedActivitiesListFragment)pagerAdapter.getItem(position);
			Log.i("Frg", "after page selected " + currentFragment);
			ResizableToolbarHelper.ResizableToolbarActivity a = getResizableToolbarActivity();

			if (currentFragment != null) {

				currentFragment.initialScrollTo(a.getScreenHeight(), a.getMaxToolbarHeight(), a.getCurrentToolbarHeight(), a.getMinToolbarHeight());
			}

		}
	};

	private ActionViewController _actionView;

	public AgimeChronicleFragment() {
		super(R.layout.agime_chronicle_fragment, R.id.pager);

	}


	public void onRefresh(LocalDate day, TrackedActivityListData data) {
		if (data == null) {
			_cache.remove(day);
		}
		else {
			_cache.put(day, data);
		}
		doRefresh(day, data);
	}


	public void doRefresh(LocalDate day, TrackedActivityListData data) {
		if (_actionView == null) {

			return;
		}
		if (!getStartDate().equals(day)) {

			return;
		}
		if (data != null) {
			_actionView.getView().setVisibility(View.VISIBLE);
			final ActionItem actionItemData = ActionItemFactory.createActionItemData(data, day);
			_actionItemData = actionItemData;

			_actionView.bind(actionItemData, data.getDate(), data.isToday(), data.getRecurringAcquisitionConfigurationData());

		}
		else {
			_actionView.getView().setVisibility(View.GONE);
		}
	}


	private ViewPager getViewPager() {
		View vp = getView();
		return (ViewPager)vp.findViewById(R.id.pager);
	}


	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (getView() != null) {
			changeListener.onPageSelected(getViewPager().getCurrentItem());
		}

	}


	static int getDaysBeforeTodayAtPosition(int i) {
		int diff = MAX_DAYS - (i + 1);
		return Math.max(0, diff);
	}


	@Override
	protected int getInitialPageNum(FragmentStatePagerAdapter adapter, Bundle savedInstanceState) {
		if (savedInstanceState == null) {
			// initial creation, not restoring
			final int daysBeforeToday = Days.daysBetween(getInitialDate(), getToday()).getDays();
			return MAX_DAYS - (daysBeforeToday + 1);
		}
		return super.getInitialPageNum(adapter, savedInstanceState);
	}


	@Override
	public LocalDate getInitialDate() {
		long millis = getArguments().getLong(ARG_INITIAL_DATE_MILLIS);
		return new LocalDate(millis);
	}


	public boolean isToday() {
		return getStartDate().equals(LocalDate.now());
	}


	@Override
	public LocalDate getStartDate() {
		return getStartDate(getCurrentItem());
	}


	public LocalDate getStartDate(int item) {
		return new LocalDate().minusDays(getDaysBeforeTodayAtPosition(item));
	}


	@Override
	public LocalDate getEndDate() {
		return getStartDate();
	}


	@Override
	protected FragmentStatePagerAdapter newAdapter(FragmentManager manager) {
		return new TrackedActivitiesListPagerAdapter(manager);
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		_customToolbar = inflater.inflate(R.layout.agime_chronicle_fragment_custom_toolbar, container, false);
		getResizableToolbarActivity().setCustomToolbar(_customToolbar, this, getResources().getDimensionPixelSize(R.dimen.custom_toolbar_height_chronicle));
		return super.onCreateView(inflater, container, savedInstanceState);
	}


	protected ResizableToolbarHelper.ResizableToolbarActivity getResizableToolbarActivity() {
		return ((ResizableToolbarHelper.ResizableToolbarActivity)getActivity());
	}


	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		setHasOptionsMenu(true);
		getViewPager().setOnPageChangeListener(this.changeListener);
		changeListener.onPageSelected(getViewPager().getCurrentItem());
		View actionViewRoot = _customToolbar.findViewById(R.id.actionview);
		_actionView = new ActionViewController(getActivity(), this, actionViewRoot);

		ImageButton fab = (ImageButton)view.findViewById(R.id.fab);
		//attach to list view will make the button disappear on scroll - but we want it to stay
		//fab.attachToListView(_listView);
		fab.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				trackTime();
			}
		});

	}


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		if (Workarounds.nestedFragmentMenuInitializedByParent()) {
			TrackedActivitiesListFragment.inflateMenu(menu, inflater);
		}
		inflater.inflate(R.menu.agime_main_menu_chronicle, menu);

	}


	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		boolean endAcquisitionTime = isToday() && _actionItemData != null &&
				(_actionItemData.isPreviousAcquisitionTimeUnfulfilled() ||
						(_actionItemData.getMode() != ActionItem.Mode.END_OF_DAY && _actionItemData.getAcquisitionTime() != null));
		boolean restartAcquisitionTime = isToday() && !endAcquisitionTime;

		final MenuItem stopAcquisition = menu.findItem(R.id.action_stop_acquisition);
		if (stopAcquisition != null) {
			stopAcquisition.setVisible(endAcquisitionTime);
		}
		final MenuItem restartAcquisition = menu.findItem(R.id.action_restart_acquisition);
		if (restartAcquisition != null) {
			restartAcquisition.setVisible(restartAcquisitionTime);
		}
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.action_stop_acquisition) {
			if (_actionItemData != null) {
				_actionView.onCloseActionItemHeading(_actionItemData);
			}
			return true;
		}
		else if (itemId == R.id.action_restart_acquisition) {
			if (_actionItemData != null) {
				_actionView.onStartAcquisitionTime(_actionItemData);
			}
			return true;
		}
		else if (itemId == R.id.action_go_to_today) {
			//onChangeDayClicked();
			goToCurrent(MAX_DAYS, R.string.action_go_to_current_day_superfluous);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}


	public void onChangeDayClicked() {
		DatePickerSupport.showDatePickerDialog(
			getActivity(),
			getFragmentManager(),
			R.id.action_go_to_today,
			this, getStartDate());
	}


	@Override
	public void onDateSelected(int token, final LocalDate date) {
		if (token == R.id.action_go_to_today) {
			goToDate(date, MAX_DAYS, R.string.action_go_to_date_error_future, R.string.action_go_to_date_error_past);
			//getWrappedView().setActiveUntilDay(date);
		}
	}


	private void trackTime() {
		trackTime(getStartDate().toDateTimeAtStartOfDay().getMillis(), null);
	}


	public void trackTime(Long startTimeMillis, Long endTimeMillis) {
		Intent intent = new Intent(Intent.ACTION_INSERT, MCContract.Activity.CONTENT_URI);
		intent.setClass(getActivity(), TrackActivity.class);
		intent.putExtra(TrackActivity.EXTRA_DAY_MILLIS, startTimeMillis);
		if (endTimeMillis != null && startTimeMillis != null) {
			intent.putExtra(TrackActivity.EXTRA_STARTTIME_MILLIS, startTimeMillis);
			intent.putExtra(TrackActivity.EXTRA_ENDTIME_MILLIS, endTimeMillis);
		}
		startActivityForResult(intent, ACTIVITY_CODE_TRACK_TIME);
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == ACTIVITY_CODE_TRACK_TIME) {
			Fragment fragment = getPagerAdapter().getCached(getCurrentItem());
			if (fragment instanceof TrackedActivitiesListFragment) {
				TrackedActivitiesListFragment f = (TrackedActivitiesListFragment)fragment;
				f.onActivityResult(requestCode, resultCode, data);
			}
			return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}


	public void onScrollChanged(int daysBeforeToday, int scrollY, boolean first, boolean dragging) {
		if (daysBeforeToday != _currentDaysBeforeToday) {
			return;
		}
		getResizableToolbarActivity().onScrollChanged(daysBeforeToday, scrollY, first, dragging);
	}


	@Override
	public void resizeCustomToolbarSmoothly(int daysBeforeToday, int scrollY, int expectedSize, Runnable runnable) {
		if (daysBeforeToday != _currentDaysBeforeToday) {
			return;
		}
		getResizableToolbarActivity().resizeCustomToolbarSmoothly(daysBeforeToday, scrollY, expectedSize, runnable);
	}

}
