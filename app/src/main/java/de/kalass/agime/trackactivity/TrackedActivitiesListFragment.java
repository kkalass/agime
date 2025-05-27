package de.kalass.agime.trackactivity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.nineoldandroids.view.ViewHelper;

import org.joda.time.LocalDate;

import java.util.List;
import java.util.concurrent.TimeUnit;

import de.kalass.agime.AgimeChronicleFragment;
import de.kalass.agime.R;
import de.kalass.agime.ResizableToolbarHelper;
import de.kalass.agime.Workarounds;
import de.kalass.agime.loader.TrackedActivityListData;
import de.kalass.agime.loader.TrackedActivityListDataAsyncLoader;
import de.kalass.agime.model.TrackedActivityModel;
import de.kalass.agime.provider.MCContract;
import de.kalass.agime.settings.Preferences;
import de.kalass.agime.util.KListView;
import de.kalass.agime.util.ListViewUtil;
import de.kalass.android.common.activity.CABMultiChoiceCallback;
import de.kalass.android.common.activity.UnifiedContextBarSupport;
import de.kalass.android.common.support.fragments.BaseListFragment;
import de.kalass.android.common.util.DateUtil;
import de.kalass.android.common.util.TimeFormatUtil;


/**
 * Created by klas on 06.10.13.
 *
 */
public class TrackedActivitiesListFragment extends BaseListFragment implements LoaderManager.LoaderCallbacks<TrackedActivityListData>, CABMultiChoiceCallback, ObservableScrollViewCallbacks, ResizableToolbarHelper.ToolbarResizeCallback {

	public static final int LOADER_ID_TRACKED_ACTIVITIES_LIST = 0;
	public static final String ARG_ITEM_ID = "itemId";
	public static final String ARG_DAYS_BEFORE_TODAY = "daysBeforeToday";

	private static final String LOG_TAG = "TrackedActivitiesLst";
	private static final int ACTIVITY_CODE_TRACK_TIME = 42;
	private static final int ACTIVITY_CODE_EDIT = 2;

	private LocalDate _day;
	private Runnable _updateEachMinuteRunnable;
	private TrackedActivityListData _data;

	private KListView _listView;
	private boolean updateBeforeShow = false;
	private BroadcastReceiver _powerConnectionReceiver;
	private PowerManager.WakeLock _screenOnWakelock;
	private TrackedActivitiesListeningFragment _listener;
	private View _headerView1;
	private View _footerView1;
	private int _extraHeaderPadding;
	private View _emptyPanel;

	private View _loadingPanel;

	private int _initialScrollTo;
	private int maxToolbarHeight;
	private int minToolbarHeight;
	private int currentToolbarHeight;
	private boolean _initialScrollSkipped;
	private boolean _suppressScrollNotification;
	private boolean _forceFirstScroll;
	private boolean _suppressUntilFirstScroll;
	private Runnable _resetScrollSuppression = new Runnable() {

		@Override
		public void run() {
			_suppressScrollNotification = false;
			_suppressUntilFirstScroll = true;
			//_forceFirstScroll = true;
		}
	};

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("initialScrollTo", _initialScrollTo);
		outState.putInt("maxToolbarHeight", maxToolbarHeight);
		outState.putInt("currentToolbarHeight", currentToolbarHeight);
		outState.putInt("minToolbarHeight", minToolbarHeight);
		outState.putLong("dayMillis", _day.toDateTimeAtStartOfDay().getMillis());
	}


	@Override
	public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
		super.onViewStateRestored(savedInstanceState);
		if (savedInstanceState != null) {
			_initialScrollTo = savedInstanceState.getInt("initialScrollTo");
			maxToolbarHeight = savedInstanceState.getInt("maxToolbarHeight");
			minToolbarHeight = savedInstanceState.getInt("minToolbarHeight");
			currentToolbarHeight = savedInstanceState.getInt("currentToolbarHeight");
			_day = new LocalDate(savedInstanceState.getLong("dayMillis"));
		}
	}


	public void initialScrollTo(final int screenHeight, final int maxToolbarHeight, int currentToolbarHeight, int minToolbarHeight) {

		this.maxToolbarHeight = maxToolbarHeight;
		this.minToolbarHeight = minToolbarHeight;
		this.currentToolbarHeight = currentToolbarHeight;

		//_listView.setSelection(1);
		//_listView.scrollBy(0, scrollY);
		//  _listView.smoothScrollToPosition(1);
		// restore index and position
		//int currentScrollY = _listView.getCurrentScrollY();
		_initialScrollTo = -(maxToolbarHeight - currentToolbarHeight);
		Log.d("Frg", "[" + getDaysBeforeToday() + "] ListFragment.initialScrollTo " + _initialScrollTo);
		//_listener.resizeCustomToolbarSmoothly(getDaysBeforeToday(), 0, this.maxToolbarHeight);

		doInitialScroll(maxToolbarHeight, minToolbarHeight);

	}


	protected void doInitialScroll(final int maxToolbarHeight, final int minToolbarHeight) {
		if (_listView == null) {
			Log.d("Frg", "skipping initial scroll due to  missing list view");
			_initialScrollSkipped = true;
			return;
		}
		Log.d("Frg", "preparing initial scroll");
		_listView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

			@Override
			public void onGlobalLayout() {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					jellyBeanRemoveGlobalLayoutListener();
				}
				else {
					_listView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
				}
				if (isDetached() || _footerView1 == null || _footerView1.getLayoutParams() == null) {
					Log.d("Frg", "skipping initial Scroll due to: detached: " + isDetached() + ", lastVisibleItem == null:" + (_footerView1 == null));
					_initialScrollSkipped = true;
					return;
				}
				_initialScrollSkipped = false;
				int cc = _listView.getChildCount();
				int firstPos = _listView.getFirstVisiblePosition();
				int lastPos = _listView.getLastVisiblePosition();
				if (firstPos > 0) {
					// no adjustments needed if we are scrolled past the header
					Log.d("Frg", "No Adjustments needed, we are scrolled past the list header");
					return;
				}
				// there must be two items at least: header and lastVisibleItem
				if (cc < 2) {
					Log.d("Frg", "not enough items: " + cc);
					return;
				}
				View lastVisibleItem = _listView.getChildAt(cc - 1); // cc-1 is the lastVisibleItem
				int screenHeight = _listView.getHeight();
				int footerPos = _listView.getAdapter().getCount() - 1;
				boolean footerIsVisible = lastPos == footerPos/* the adapter includes the header and lastVisibleItem in its count, testing if we see the lastVisibleItem*/;
				int footerTop = footerIsVisible ? lastVisibleItem.getTop() : lastVisibleItem.getBottom();
				Log.d("Frg", "count: " + _listView.getAdapter().getCount() + ", lastPos: " + lastPos + ", lastItemVisible: " + footerIsVisible);
				Log.d("Frg", "cc: " + cc + ", footerTop -> " + footerTop + ", footerIsVisible: " + footerIsVisible + ", screenHeight: " + screenHeight + ", firstPos: "
						+ firstPos + ", lastPos: " + lastPos + " adapter size " + _listView.getAdapter().getCount());
				View header = _listView.getChildAt(0);

				// FIXME: find a way to detect and calculate cases where the real content size is below the
				//        minimum needed size, but the footer top cannot be determined ()
				boolean isContentSizeCorrect = (lastPos == footerPos || lastPos == footerPos - 1);
				int contentSize = header.getTop() > 0 ? 0 : -header.getTop() + footerTop;
				int minNeededSize = screenHeight + maxToolbarHeight;
				if (isContentSizeCorrect && contentSize < minNeededSize) {
					// ensure that the lastVisibleItem is high enough so that the user can scroll up the toolbar
					int missingArea = screenHeight - minToolbarHeight - (footerTop - header.getBottom()) - _extraHeaderPadding /*once for header*/;
					Log.d("Frg", "screenHeight: " + screenHeight + ", minToolbarHeight: " + minToolbarHeight + ", header bottom " + header.getBottom() + ", footerTop "
							+ footerTop + " => missing: " + missingArea);
					_footerView1.getLayoutParams().height = Math.max(missingArea, _extraHeaderPadding);
				}
				else {
					// apparently, the lastVisibleItem is not needed any more
					_footerView1.getLayoutParams().height = _extraHeaderPadding;
				}
				Log.d("Frg", "Will scroll list header to custom toolbar height: " + _initialScrollTo);
				// set the visible portion  of the header to the custom toolbar height
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					_listView.setSelectionFromTop(0, _initialScrollTo);
				}
				else {
					if (_initialScrollTo < -(maxToolbarHeight - minToolbarHeight) / 2) {
						_listView.setSelection(1);
					}
					else {
						_listView.setSelection(0);
					}

				}

			}


			@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
			protected void jellyBeanRemoveGlobalLayoutListener() {
				_listView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
			}
		});
	}

	public interface TrackedActivitiesListeningFragment {

		void onRefresh(LocalDate date, TrackedActivityListData data);


		void onScrollChanged(int daysBeforeToday, int scrollY, boolean first, boolean dragging);


		void resizeCustomToolbarSmoothly(int daysBeforeToday, int scrollY, int expectedSize, Runnable runnable);
	}

	public TrackedActivitiesListFragment() {
		super();
	}


	public long getStarttimeInclusiveMillis() {
		return DateUtil.getMillisAtStartOfDay(_day);
	}


	public long getEndtimeExclusiveMillis() {
		return DateUtil.getMillisAtEndOfDay(_day);
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		_headerView1 = inflater.inflate(R.layout.empty_list_header, null, false);
		_footerView1 = inflater.inflate(R.layout.empty_list_footer, null, false);
		View view = inflater.inflate(R.layout.tracked_activities_list_fragment, container, false);
		ListView lv = (ListView)view.findViewById(android.R.id.list);
		lv.addHeaderView(_headerView1);
		lv.addFooterView(_footerView1);
		return view;
	}


	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		_listener = (TrackedActivitiesListeningFragment)getParentFragment();
	}


	@Override
	public void onDetach() {
		super.onDetach();
		if (_listener != null) {
			_listener.onRefresh(_day, null);
		}
		_listener = null;
	}


	@Override
	public void onViewCreated(View view, Bundle bundle) {
		super.onViewCreated(view, bundle);

		_loadingPanel = getView().findViewById(R.id.loadingPanel);
		_emptyPanel = _footerView1.findViewById(R.id.emptyPanel);

		_extraHeaderPadding = dpToPx(getPrivateContext(), 8);

		_day = calculateDay();

		// needs to be set to true, even if Workarounds.nestedFragmentMenuInitializedByParent()  is used
		setHasOptionsMenu(true);

		_listView = (KListView)getListView();
		// KK: strange - I did specify 0dp in the xml, but still need to set it to 0 here to make the divider go away?
		_listView.setDividerHeight(0);

		UnifiedContextBarSupport.setup(this, _listView);

		//setHeaderHeights();

		setListAdapter(new TrackedActivitiesListAdapter(getPrivateContext()));

		_listView.setScrollViewCallbacks(this);
		_listView.setOnScrollListener(new AbsListView.OnScrollListener() {

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				if (scrollState == SCROLL_STATE_IDLE) {
					if (_listView.getFirstVisiblePosition() == 0 && !_suppressScrollNotification && !_suppressUntilFirstScroll) {
						View header = _listView.getChildAt(0);
						int headerBottom = header.getBottom() - _extraHeaderPadding;
						// if the user scrolls fast, we might get stuck - adjust the toolbar height to the
						// header height
						if ((direction == Direction.DOWN && headerBottom > currentToolbarHeight) ||
								(direction == Direction.UP && headerBottom < currentToolbarHeight)) {
							_suppressScrollNotification = true;
							if (_listener != null) {
								_listener.resizeCustomToolbarSmoothly(getDaysBeforeToday(), 0, headerBottom, _resetScrollSuppression);
							}
						}
					}
				}
			}


			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

			}
		});

		// start with a progress indicator, until the loader has done its work

		//((TextView)_emptyView.findViewById(android.R.id.text1)).setText("ASFDASDFAs");
		//_listView.setEmptyView(view.findViewById(R.id.empty_text));

		getLoaderManager().initLoader(LOADER_ID_TRACKED_ACTIVITIES_LIST, null, this);
		getView().post(new Runnable() {

			@Override
			public void run() {

				setHeaderHeights();
				if (_initialScrollSkipped) {
					doInitialScroll(maxToolbarHeight, minToolbarHeight);
				}
			}
		});
	}


	protected void setHeaderHeights() {

		if (isDetached() || getActivity() == null) {
			return;
		}

		int initialHeight = getInitialCustomToolbarHeight();

		if (_headerView1 != null) {
			ViewGroup.LayoutParams lp = _headerView1.getLayoutParams();
			if (lp != null) {
				lp.height = initialHeight + _extraHeaderPadding;
			}
		}

	}


	private int getInitialCustomToolbarHeight() {
		return getResources().getDimensionPixelSize(R.dimen.custom_toolbar_height_chronicle);
	}


	@Override
	public void adjustCustomToolbarHeight(int maxToolbarHeight, int currentHeight, int targetHeight) {
		this.maxToolbarHeight = maxToolbarHeight;
		this.minToolbarHeight = targetHeight;
		this.currentToolbarHeight = currentHeight;
		setEmptyPanelVisibility(maxToolbarHeight, currentHeight, targetHeight);
	}


	protected void setEmptyPanelVisibility(int maxToolbarHeight, int currentHeight, int targetHeight) {
		float hiddenPercentage = (float)(currentHeight - targetHeight) / (float)(maxToolbarHeight - targetHeight);
		boolean empty = _data == null || _data.getTrackedActivityModels() == null || _data.getTrackedActivityModels().isEmpty();
		if (_emptyPanel != null) {
			if (!empty || hiddenPercentage >= 2f / 3f) {
				_emptyPanel.setVisibility(View.GONE);
			}
			else {
				_emptyPanel.setVisibility(View.VISIBLE);
				float alpha = 1.0f - Math.max(0.0f, Math.min(1.0f, ((hiddenPercentage - 1f / 3f) * 3f)));
				ViewHelper.setAlpha(_emptyPanel, alpha);
			}
		}
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		if (_listener != null) {
			_listener.onRefresh(_day, null);
			_listener = null;
		}
	}

	private int lastScrollPos = 0;
	private Direction direction;

	enum Direction {
		UP,
		DOWN,
		UNKNOWN;
	}

	@Override
	public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {
		if (firstScroll) {
			lastScrollPos = scrollY;
			direction = Direction.UNKNOWN;
		}
		else {
			int diff = scrollY - lastScrollPos;
			direction = diff < 0 ? Direction.DOWN : (diff > 0 ? Direction.UP : Direction.UNKNOWN);
			lastScrollPos = scrollY;
		}
		if (_suppressUntilFirstScroll) {
			if (firstScroll) {
				_suppressUntilFirstScroll = false;
			}
			else {
				return;
			}
		}
		if (_listener != null && !_suppressScrollNotification) {
			//Log.i("Frg", "onScrollChanged " + _day);
			_listener.onScrollChanged(getDaysBeforeToday(), scrollY, firstScroll || _forceFirstScroll, dragging);
			_forceFirstScroll = false;
		}
	}


	@Override
	public void onDownMotionEvent() {
	}


	@Override
	public void onUpOrCancelMotionEvent(ScrollState scrollState) {
		// FIXME: ensure that the custom toolbar height does not get out of sync and 'gets stuck'
		/*
		if (_listView.getFirstVisiblePosition() == 0 ) {
		    View header = _listView.getChildAt(0);
		    // sync header and toolbar - this means setting the toolbar size to ensure
		    // that speed or dragging offsets do not lead to unclean toolbar size
		    _listener.resizeCustomToolbarSmoothly(getDaysBeforeToday(), _listView.getCurrentScrollY(), Math.max(minToolbarHeight, header.getBottom() - _extraHeaderPadding ));
		}
		*/
	}


	public static CharSequence getPageTitle(Context context, int daysBeforeToday) {
		LocalDate day = new LocalDate().minusDays(daysBeforeToday);
		return TimeFormatUtil.formatTimeForHeader(context, day);
	}


	static int dpToPx(Context context, int dp) {
		return (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, context.getResources().getDisplayMetrics());
	}


	private void releaseScreenLock() {
		if (_powerConnectionReceiver != null) {
			getActivity().unregisterReceiver(_powerConnectionReceiver);
			_powerConnectionReceiver = null;
		}
		if (_screenOnWakelock != null) {
			_screenOnWakelock.release();
			_screenOnWakelock = null;
		}
	}


	private void initScreenLock() {
		if (!Preferences.isKeepScreenOn(getActivity())) {
			return;
		}
		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

		_powerConnectionReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(final Context context, final Intent batteryStatus) {
				// Are we charging / charged?
				int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
				boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
						status == BatteryManager.BATTERY_STATUS_FULL;

				Log.i(LOG_TAG, "isCharging: " + isCharging);
				if (_screenOnWakelock != null && !isCharging) {
					_screenOnWakelock.release();
					_screenOnWakelock = null;
				}
				else if (_screenOnWakelock == null && isCharging) {
					PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
					/**
					 * We use the deprecated SCREEN_DIM_WAKE_LOCK, because the alternative FLAG_KEEP_SCREEN_ON will keep the
					 * screen bright, which is not the desired behaviour
					 */
					_screenOnWakelock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "TrackedActivitiesListFragment");
					_screenOnWakelock.acquire();
				}
			}
		};
		getActivity().registerReceiver(_powerConnectionReceiver, ifilter);

	}


	@Override
	public final void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		UnifiedContextBarSupport.onCreateContextMenu(this, menu, v, menuInfo);
	}


	private boolean isOnlyLastItemSelected(List<Long> entityIds) {
		if (entityIds.size() != 1) {
			return false;
		}
		long itemId = entityIds.get(0);
		final TrackedActivitiesListAdapter adapter = getListAdapter();
		int adapterCount = adapter.getCount();

		if (adapterCount == 0) {
			// should be never reached
			Log.w(LOG_TAG, "Test for last selected item did not find any item in adapter");
			return false;
		}

		long lastItemId = adapter.getItemId(adapterCount - 1);
		return itemId == lastItemId;
	}


	private boolean isFakeEntry(long id) {
		return id < 0;
	}


	private boolean hasFakeEntries(List<Long> newEntityIds) {
		for (Long id : newEntityIds) {
			if (isFakeEntry(id)) {
				return true;
			}
		}
		return false;
	}


	private List<Long> withoutFakeIds(List<Long> newEntityIds) {
		if (!hasFakeEntries(newEntityIds)) {
			return newEntityIds;
		}
		ImmutableList.Builder<Long> b = ImmutableList.builder();
		for (Long id : newEntityIds) {
			if (!isFakeEntry(id)) {
				b.add(id);
			}
		}
		return b.build();
	}


	@Override
	public void onUnifiedContextBarSelectionChanged(Menu menu, List<Long> newEntityIds, List<Integer> selectedPositions) {
		List<Long> realEntityIds = withoutFakeIds(newEntityIds);

		final MenuItem nowItem = menu.findItem(R.id.action_now);
		if (nowItem != null) {
			nowItem.setVisible(getParentFragment() instanceof AgimeChronicleFragment &&
					_data.isToday() && newEntityIds.equals(realEntityIds) &&
					isOnlyLastItemSelected(realEntityIds));
		}

		final MenuItem editItem = menu.findItem(R.id.action_edit);
		if (editItem != null) {
			// "edit" of a fake entry can be mapped to an insert
			editItem.setVisible(newEntityIds.size() == 1);
		}
		final MenuItem deleteItem = menu.findItem(R.id.action_delete);
		if (deleteItem != null) {
			deleteItem.setVisible(!realEntityIds.isEmpty() && realEntityIds.size() == newEntityIds.size());
		}
	}


	@Override
	public boolean onContextItemSelected(MenuItem item) {
		return UnifiedContextBarSupport.onContextItemSelected(this, item)
				|| super.onContextItemSelected(item);
	}


	/**
	 * Override this to control the options for a selected item or selected items
	 */
	public void onCreateUnifiedContextBar(MenuInflater inflater, Menu menu) {
		inflater.inflate(R.menu.agime_context_menu_day_fragment, menu);
	}


	private TrackedActivityModel findModelById(long id) {
		final TrackedActivitiesListAdapter adapter = getListAdapter();
		for (int i = 0; i < adapter.getCount(); i++) {
			final TrackedActivityModel item = adapter.getItem(i);
			if (item != null && item.getId() == id) {
				return item;
			}
		}
		return null;
	}


	@Override
	public boolean onUnifiedContextBarItemSelected(int menuItemId, List<Long> rowItemIds, List<Integer> selectedPositions) {
		if (menuItemId == R.id.action_delete) {
			deleteItems(withoutFakeIds(rowItemIds));
			return true;
		}
		else if (menuItemId == R.id.action_edit) {
			// works for fake entries as well as normal entries
			if (rowItemIds.size() == 1) {
				long id = rowItemIds.get(0);
				if (isFakeEntry(id)) {
					final TrackedActivityModel model = findModelById(id);
					if (model != null) {
						trackTimeFromFakeEntry(model);
					}
					else {
						// fallback
						Log.w(LOG_TAG, "Edit of a fake entry requested, but no entry found for id " + id);
						trackTime();
					}
				}
				else {
					Intent intent = new Intent(Intent.ACTION_EDIT, ContentUris.withAppendedId(MCContract.Activity.CONTENT_URI, id));
					startActivity(intent);
				}
			}
			else {
				Log.w(LOG_TAG, "context menu edit called for " + rowItemIds.size() + " items, will ignore");
			}
			return true;
		}
		else if (menuItemId == R.id.action_now) {
			if (_data.isToday() && isOnlyLastItemSelected(withoutFakeIds(rowItemIds))) {
				final TrackedActivitiesListAdapter adapter = getListAdapter();
				final TrackedActivityModel item = adapter.getItem(adapter.getCount() - 1);
				Fragment parent = getParentFragment();
				if (parent instanceof AgimeChronicleFragment) {
					((AgimeChronicleFragment)parent).onExtendPreviousActivity(item.getId(), item);
				}
			}
			return true;
		}
		return false;
	}


	protected void deleteItems(List<Long> rowItemIds) {
		TrackedActivityEditorDBUtil.delete(getPrivateContext(), null, rowItemIds);
	}


	private LocalDate calculateDay() {
		int daysBeforeToday = getDaysBeforeToday();
		return new LocalDate().minusDays(daysBeforeToday);
	}


	public int getDaysBeforeToday() {
		Bundle arguments = getArguments();
		return arguments == null ? 0 : arguments.getInt(ARG_DAYS_BEFORE_TODAY, 0);
	}


	@Override
	public void onStart() {
		super.onStart();
		LocalDate calculated = calculateDay();
		if (!Objects.equal(_day, calculated)) {
			_day = calculated;
			// starttime and endtime have changed - let the loader manager restart the loader,
			// this forces the loader manager to throw away the old loader instance and data
			// and ask the fragment for a fresh new loader.
			getLoaderManager().restartLoader(LOADER_ID_TRACKED_ACTIVITIES_LIST, null, this);
		}

		// Starting of the minutely update code in onStart avoids flickering in the UI
		// that happens when the UI is refreshed onResume only
		if (getDaysBeforeToday() == 0) {
			if (_updateEachMinuteRunnable == null) {
				_updateEachMinuteRunnable = new Runnable() {

					@Override
					public void run() {
						View view = getView();
						if (getActivity() == null || isDetached()) {
							if (view != null) {
								view.removeCallbacks(this);
							}
							return;
						}
						minutelyUpdate();
						if (view != null) {
							view.postDelayed(this, TimeUnit.SECONDS.toMillis(60));
						}
					}
				};
			}
			// ensure that the extra-entry is current and that the delayed timer is started
			_updateEachMinuteRunnable.run();
			updateBeforeShow = false;
		}

	}


	@Override
	public void onResume() {
		super.onResume();

		// sometimes, the activity is not stopped but merely paused. in those cases we need
		// to restart the minutely update here
		if (_updateEachMinuteRunnable != null && updateBeforeShow) {
			_updateEachMinuteRunnable.run();
			updateBeforeShow = false;
		}
		initScreenLock();
	}


	@Override
	public void onPause() {
		super.onPause();
		if (_updateEachMinuteRunnable != null) {
			getView().removeCallbacks(_updateEachMinuteRunnable);
			updateBeforeShow = true;
		}
		releaseScreenLock();
	}


	public void minutelyUpdate() {
		Log.i(LOG_TAG, "Minutely Update");
		refreshActionItems();
	}


	private Context getPrivateContext() {
		return getActivity();
	}


	@Override
	public TrackedActivitiesListAdapter getListAdapter() {
		return (TrackedActivitiesListAdapter)super.getListAdapter();
	}


	@Override
	public Loader<TrackedActivityListData> onCreateLoader(int id, Bundle args) {
		return new TrackedActivityListDataAsyncLoader(
				getActivity(),
				getStarttimeInclusiveMillis(),
				getEndtimeExclusiveMillis());
	}


	@Override
	public void onLoadFinished(Loader<TrackedActivityListData> loader, TrackedActivityListData data) {

		_data = data;
		Log.d(LOG_TAG, "onLoadFinished " + loader);

		refreshActionItems();
		getListAdapter().setItems(data.getTrackedActivityModels());
		if (_listView != null) {
			doInitialScroll(this.maxToolbarHeight, this.minToolbarHeight);
		}
		// The list should now be shown.
		if (_loadingPanel != null) {
			_loadingPanel.setVisibility(View.GONE);
		}

		if (_emptyPanel != null && !data.getTrackedActivityModels().isEmpty()) {
			_emptyPanel.setVisibility(View.GONE);
		}
	}


	private void refreshActionItems() {
		if (_listener != null) {
			_listener.onRefresh(_day, _data);
		}

	}


	@Override
	public void onLoaderReset(Loader<TrackedActivityListData> loader) {
		_data = null;
		Log.d(LOG_TAG, "onLoaderReset " + loader);
		getListAdapter().setItems(null);
	}


	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		ListAdapter adapter = l.getAdapter();
		TrackedActivityModel item = (TrackedActivityModel)adapter.getItem(position);

		if (item == null) {
			return;
		}
		if (item.isFakeEntry()) {
			// let the user create a new entry
			trackTimeFromFakeEntry(item);
			return;
		}

		Intent intent = new Intent(Intent.ACTION_EDIT, ContentUris.withAppendedId(MCContract.Activity.CONTENT_URI, item.getId()));
		intent.setClass(getActivity(), TrackActivity.class);

		startActivityForResult(intent, ACTIVITY_CODE_EDIT);
	}


	private void trackTimeFromFakeEntry(TrackedActivityModel item) {
		Intent intent = new Intent(Intent.ACTION_INSERT, MCContract.Activity.CONTENT_URI);
		intent.setClass(getActivity(), TrackActivity.class);
		intent.putExtra(TrackActivity.EXTRA_DAY_MILLIS, item.getStartTimeMillis());
		intent.putExtra(TrackActivity.EXTRA_STARTTIME_MILLIS, item.getStartTimeMillis());
		intent.putExtra(TrackActivity.EXTRA_ENDTIME_MILLIS, item.getEndTimeMillis());

		startActivityForResult(intent, ACTIVITY_CODE_EDIT);
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == ACTIVITY_CODE_EDIT || requestCode == ACTIVITY_CODE_TRACK_TIME) {
				final Long id = data.hasExtra(TrackActivity.EXTRA_ID) ? data.getLongExtra(TrackActivity.EXTRA_ID, -1) : null;
				if (id != null) {
					scrollToItemAndExpandHeader(id);
				}
				return;
			}
		}
	}


	public void scrollToItemAndExpandHeader(Long id) {
		Log.i("Frg", "scroll to item and expand header " + id);
		_suppressScrollNotification = true;
		ListViewUtil.scrollToItemAfterDataChange(_listView, id, new Runnable() {

			@Override
			public void run() {
				if (_listener != null) {
					Log.i("Frg", "***************** RESIZE TOOLBAR TO MAX ***************+");
					_listener.resizeCustomToolbarSmoothly(getDaysBeforeToday(), 0, maxToolbarHeight, _resetScrollSuppression);
				}
			}
		});
	}


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		if (!Workarounds.nestedFragmentMenuInitializedByParent()) {
			inflateMenu(menu, inflater);
		}
	}


	public static void inflateMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.agime_main_menu_day_fragment, menu);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		return super.onOptionsItemSelected(item);
	}


	private void trackTime() {
		trackTime(getStarttimeInclusiveMillis(), null);
	}


	private void trackTime(Long startTimeMillis, Long endTimeMillis) {
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
	public String toString() {
		return MoreObjects.toStringHelper(this).add("day", _day).toString();
	}

}
