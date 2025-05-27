package de.kalass.agime;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import org.joda.time.LocalDate;

import de.kalass.agime.acquisitiontime.AcquisitionTimeManagementActivity;
import de.kalass.agime.ongoingnotification.NotificationManagingService;
import de.kalass.agime.overview.AgimeDayOverviewFragment;
import de.kalass.agime.overview.AgimeMonthOverviewFragment;
import de.kalass.agime.overview.AgimeTotalOverviewFragment;
import de.kalass.agime.overview.AgimeWeekOverviewFragment;
import de.kalass.agime.overview.AgimeYearOverviewFragment;
import de.kalass.android.common.DialogUtils;


public class AgimeMainActivity extends MainEntryPointActivity implements ResizableToolbarHelper.ResizableToolbarActivity {

  private static final int POSITION_CHRONICLE_BY_DAY = 0;
  private static final int POSITION_REPORT = 1;
  private static final short TYPE_REPORT_BY_DAY = 1;
  private static final short TYPE_REPORT_BY_WEEK = 2;
  private static final short TYPE_REPORT_BY_MONTH = 3;
  private static final short TYPE_REPORT_BY_YEAR = 8;
  private static final short TYPE_REPORT_TOTAL = 9;
  private static final String KEY_MODE = "keyMode";
  private static final String LOG_TAG = "AgimeMainActivity";
  public static final String KEY_NAV_ITEM_INDEX = "navItemIndex";
  public static final String KEY_NAV_ITEM_ID = "navItemId";
  private static final int POS_ACTIVITY_SETTINGS_ACQUISITION = 4;
  private static final int POS_ACTIVITY_SETTINGS = 5;
  private static final int POS_ACTIVITY_FAQ = 6;
  private static final int POS_ACTIVITY_ABOUT = 7;
  private static final String KEY_REPORT_TYPE = "reportType";
  private static final String KEY_IS_REPORT_MODE = "isReportMode";

  private int _selectedPosition;
  private int _selectedItemId;
  private ActionBarDrawerToggle _drawerToggle;
  private View.OnClickListener _clickListener = new View.OnClickListener() {

    @Override
    public void onClick(View v) {
      int id = v.getId();
      if (id == R.id.action_about) {
        selectItem(id, POS_ACTIVITY_ABOUT, null);
        return;
      }
      else if (id == R.id.action_faq) {
        selectItem(id, POS_ACTIVITY_FAQ, null);
        return;
      }
      else if (id == R.id.action_settings) {
        selectItem(id, POS_ACTIVITY_SETTINGS, null);
        return;
      }
      else if (id == R.id.action_acquisition_times) {
        selectItem(id, POS_ACTIVITY_SETTINGS_ACQUISITION, null);
        return;
      }
      else if (id == R.id.action_chronicle) {
        selectItem(id, POSITION_CHRONICLE_BY_DAY, null);
        return;
      }
      else if (id == R.id.action_report) {
        selectItem(id, POSITION_REPORT, _reportType == null ? TYPE_REPORT_BY_DAY : _reportType);
        return;
      }
    }
  };

  private ServiceConnection _notificationServiceConnection = new ServiceConnection() {

    @Override
    public void onServiceConnected(ComponentName className,
        IBinder service) {
      // service is bound to control its foreground state, so there is nothing to do here
    }


    @Override
    public void onServiceDisconnected(ComponentName arg0) {
      // service is bound to control its foreground state, so there is nothing to do here
    }
  };

  private View _drawer;
  private DrawerLayout _drawerLayout;
  private Short _reportType;
  private boolean _reportMode;

  private ResizableToolbarHelper _resizableToolbarHelper;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ActivityCompat.requestPermissions(this, new String[] {
        Manifest.permission.POST_NOTIFICATIONS
      }, 43);
    }
    setContentView(R.layout.agime_main_activity);
    Toolbar toolbar = getToolbar();
    setSupportActionBar(toolbar);

    _drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);

    _drawer = findViewById(R.id.left_drawer);

    findViewById(R.id.action_chronicle).setOnClickListener(_clickListener);
    findViewById(R.id.action_report).setOnClickListener(_clickListener);
    findViewById(R.id.action_about).setOnClickListener(_clickListener);
    findViewById(R.id.action_settings).setOnClickListener(_clickListener);
    findViewById(R.id.action_acquisition_times).setOnClickListener(_clickListener);
    findViewById(R.id.action_faq).setOnClickListener(_clickListener);

    _drawerToggle = new ActionBarDrawerToggle(this, _drawerLayout,
        R.string.drawer_open, R.string.drawer_close) {

      /** Called when a drawer has settled in a completely closed state. */
      public void onDrawerClosed(View view) {
        super.onDrawerClosed(view);
        //getActionBar().setTitle(_title);
        doinvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
      }


      /** Called when a drawer has settled in a completely open state. */
      public void onDrawerOpened(View drawerView) {
        super.onDrawerOpened(drawerView);
        //getActionBar().setTitle(_drawerTitle);
        doinvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
      }
    };

    // Set the drawer toggle as the DrawerListener
    _drawerLayout.setDrawerListener(_drawerToggle);
    _drawerLayout.setStatusBarBackgroundColor(getResources().getColor(R.color.primary_dark));

    ActionBar actionBar = getSupportActionBar();
    //actionBar.setElevation(0);

    //actionBar.setIcon(R.drawable.ic_launcher);
    actionBar.setDisplayShowTitleEnabled(false);
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setHomeButtonEnabled(true);

    if (savedInstanceState != null) {
      final int idx = savedInstanceState.getInt(KEY_NAV_ITEM_INDEX, 0);
      _selectedItemId = savedInstanceState.getInt(KEY_NAV_ITEM_ID, R.id.action_chronicle);
      _selectedPosition = idx;
      _reportType = savedInstanceState.getShort(KEY_REPORT_TYPE, TYPE_REPORT_BY_DAY);
    }
    else {
      _selectedPosition = 0;
      _selectedItemId = R.id.action_chronicle;
      _reportType = TYPE_REPORT_BY_DAY;
    }
    _reportMode = _selectedPosition == POSITION_REPORT;
    selectItem(_selectedItemId, _selectedPosition, _reportType);

    _resizableToolbarHelper = new ResizableToolbarHelper(this);

  }


  private Toolbar getToolbar() {
    return (Toolbar)findViewById(R.id.agime_action_bar);
  }


  protected Fragment getCurrentFragment() {
    return getSupportFragmentManager().findFragmentById(R.id.fragment_container);
  }


  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    // Sync the toggle state after onRestoreInstanceState has occurred.
    _drawerToggle.syncState();
  }


  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    _drawerToggle.onConfigurationChanged(newConfig);
  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.agime_main_menu_overview, menu);

    return super.onCreateOptionsMenu(menu);
  }


  /* Called whenever we call doinvalidateOptionsMenu() */
  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    // If the nav drawer is open, hide action items related to the content view
    boolean drawerOpen = _drawerLayout.isDrawerOpen(_drawer);

    MenuItem reportDayItem = menu.findItem(R.id.report_day);
    MenuItem reportWeekItem = menu.findItem(R.id.report_week);
    MenuItem reportMonthItem = menu.findItem(R.id.report_month);
    MenuItem reportYearItem = menu.findItem(R.id.report_year);
    MenuItem reportTotalItem = menu.findItem(R.id.report_total);

    boolean reportMode = _reportMode;
    if (reportDayItem != null) {
      reportDayItem.setVisible(reportMode);
      reportDayItem.setChecked(_reportType == TYPE_REPORT_BY_DAY);
    }
    if (reportWeekItem != null) {
      reportWeekItem.setVisible(reportMode);
      reportWeekItem.setChecked(_reportType == TYPE_REPORT_BY_WEEK);
    }
    if (reportMonthItem != null) {
      reportMonthItem.setVisible(reportMode);
      reportMonthItem.setChecked(_reportType == TYPE_REPORT_BY_MONTH);
    }
    if (reportYearItem != null) {
      reportYearItem.setVisible(reportMode);
      reportYearItem.setChecked(_reportType == TYPE_REPORT_BY_YEAR);
    }
    if (reportTotalItem != null) {
      reportTotalItem.setVisible(reportMode);
      reportTotalItem.setChecked(_reportType == TYPE_REPORT_TOTAL);
    }
    // FIXME handle item visibility
    //menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);
    return super.onPrepareOptionsMenu(menu);
  }


  protected int getPositionOf(Fragment fragment) {
    if (fragment instanceof AgimeChronicleFragment) {
      return POSITION_CHRONICLE_BY_DAY;
    }
    else if (fragment instanceof AgimeDayOverviewFragment) {
      return POSITION_REPORT;
    }
    else if (fragment instanceof AgimeMonthOverviewFragment) {
      return POSITION_REPORT;
    }
    else if (fragment instanceof AgimeWeekOverviewFragment) {
      return POSITION_REPORT;
    }
    else if (fragment instanceof AgimeYearOverviewFragment) {
      return POSITION_REPORT;
    }
    else if (fragment instanceof AgimeTotalOverviewFragment) {
      return POSITION_REPORT;
    }
    throw new IllegalStateException("Unknown fragemnt type " + fragment);
  }


  protected int getReportTypeOf(Fragment fragment) {
    if (fragment instanceof AgimeDayOverviewFragment) {
      return TYPE_REPORT_BY_DAY;
    }
    else if (fragment instanceof AgimeMonthOverviewFragment) {
      return TYPE_REPORT_BY_MONTH;
    }
    else if (fragment instanceof AgimeWeekOverviewFragment) {
      return TYPE_REPORT_BY_WEEK;
    }
    else if (fragment instanceof AgimeYearOverviewFragment) {
      return TYPE_REPORT_BY_YEAR;
    }
    else if (fragment instanceof AgimeTotalOverviewFragment) {
      return TYPE_REPORT_TOTAL;
    }
    throw new IllegalStateException("Unknown fragemnt type " + fragment);
  }


  private Fragment createFragment(int position, Short typeReport) {

    if (position == POSITION_CHRONICLE_BY_DAY) {
      return new AgimeChronicleFragment();
    }
    else if (position == POSITION_REPORT) {
      if (typeReport != null && typeReport == TYPE_REPORT_BY_MONTH) {
        return new AgimeMonthOverviewFragment();
      }
      else if (typeReport != null && typeReport == TYPE_REPORT_BY_YEAR) {
        return new AgimeYearOverviewFragment();
      }
      else if (typeReport != null && typeReport == TYPE_REPORT_TOTAL) {
        return new AgimeTotalOverviewFragment();
      }
      else if (typeReport != null && typeReport == TYPE_REPORT_BY_WEEK) {
        return new AgimeWeekOverviewFragment();
      }
      else {
        return new AgimeDayOverviewFragment();
      }
    }
    else {
      throw new IllegalStateException("Not a valid position: " + position);
    }
  }


  protected void doOnStart() {
    Intent intent = new Intent(this, NotificationManagingService.class);
    startService(intent);
    bindService(intent, _notificationServiceConnection, BIND_ADJUST_WITH_ACTIVITY);
  }


  protected void doOnStop() {
    unbindService(_notificationServiceConnection);
  }


  public void selectItem(int itemId, int position, Short typeReport) {

    if (position == POS_ACTIVITY_ABOUT) {
      startActivity(new Intent(this, AboutActivity.class));
      closeDrawer();
      return;
    }
    if (position == POS_ACTIVITY_SETTINGS) {
      openSettings();
      closeDrawer();
      return;
    }
    if (position == POS_ACTIVITY_FAQ) {
      showFAQDialog();
      closeDrawer();
      return;
    }
    if (position == POS_ACTIVITY_SETTINGS_ACQUISITION) {
      startActivity(new Intent(this, AcquisitionTimeManagementActivity.class));
      closeDrawer();
      return;
    }

    View listItem = findViewById(itemId);
    listItem.setSelected(true);
    if (_selectedItemId != itemId) {
      View previous = findViewById(_selectedItemId);
      if (previous != null) {
        previous.setSelected(false);
      }
    }
    _reportType = typeReport == null ? _reportType : typeReport; // make sure we remember the last type
    _reportMode = position == POSITION_REPORT;
    _selectedItemId = itemId;
    _selectedPosition = position;
    // Create new fragment from our own Fragment class
    final FragmentManager supportFragmentManager = getSupportFragmentManager();
    final Fragment oldFragment = supportFragmentManager.findFragmentById(R.id.fragment_container);

    Log.d(LOG_TAG, "selectItem(" + position + ") / oldFragment = " + oldFragment);

    if (oldFragment != null && position == getPositionOf(oldFragment)) {
      if (position != POSITION_REPORT) {
        Log.d(LOG_TAG, "selectItem(" + position + ") => skipping because position already filled with matching fragment");
        // will happen when restoring state, for example on orientation change
        return;
      }
      if (_reportType == getReportTypeOf(oldFragment)) {
        Log.d(LOG_TAG, "selectItem(" + position + ") => skipping because position already filled with matching fragment");
        // will happen when restoring state, for example on orientation change
        return;
      }
    }

    LocalDate initalDate = getInitalFragmentDate(oldFragment);
    Log.d(LOG_TAG, "selectItem(" + position + ") => initial date " + initalDate);
    Fragment newFragment = createFragment(position, typeReport);
    Log.d(LOG_TAG, "selectItem(" + position + ") => create new fragment " + newFragment);
    Bundle args = newFragment.getArguments();
    if (args == null) {
      args = new Bundle();
    }
    args.putLong(LocalDateSpanning.ARG_INITIAL_DATE_MILLIS, initalDate.toDateTimeAtStartOfDay().getMillis());
    newFragment.setArguments(args);
    FragmentTransaction ft = supportFragmentManager.beginTransaction();
    // Replace whatever is in the fragment container with this fragment
    ft.replace(R.id.fragment_container, newFragment);
    // Apply changes
    ft.commitAllowingStateLoss();

    // Highlight the selected item, update the title, and close the drawer
    //_drawerList.setItemChecked(position, true);
    //setTitle("FIXME " + position);

    /*
    _drawerLayout.post(new Runnable() {
    		@Override
    		public void run() {
    // update stuff
    				_resizableToolbarHelper.onInitialLayout();
    		}
    });
    */
    closeDrawer();
  }


  protected void closeDrawer() {
    _drawerLayout.closeDrawer(_drawer);
  }


  protected LocalDate getInitalFragmentDate(Fragment oldFragment) {
    if (!(oldFragment instanceof LocalDateSpanning)) {
      // if in doubt, use today
      return new LocalDate();
    }
    final LocalDateSpanning f = (LocalDateSpanning)oldFragment;
    final LocalDate endDate = f.getEndDate();
    final LocalDate startDate = f.getStartDate();
    final LocalDate initialDate = f.getInitialDate();
    if (!initialDate.isBefore(startDate) && !initialDate.isAfter(endDate)) {
      // initial date is in the current range - use it as a base for
      // the date-span of the new fragment
      return initialDate;
    }
    return startDate;
  }


  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Pass the event to ActionBarDrawerToggle, if it returns
    // true, then it has handled the app icon touch event
    if (_drawerToggle.onOptionsItemSelected(item)) {
      return true;
    }
    int itemId = item.getItemId();
    if (itemId == R.id.report_day) {
      selectItem(R.id.action_report, POSITION_REPORT, TYPE_REPORT_BY_DAY);
      // make sure, that the options menu is refreshed if the user switches report types
      doinvalidateOptionsMenu();
      return true;
    }
    else if (itemId == R.id.report_week) {
      selectItem(R.id.action_report, POSITION_REPORT, TYPE_REPORT_BY_WEEK);
      // make sure, that the options menu is refreshed if the user switches report types
      doinvalidateOptionsMenu();
      return true;
    }
    else if (itemId == R.id.report_month) {
      selectItem(R.id.action_report, POSITION_REPORT, TYPE_REPORT_BY_MONTH);
      // make sure, that the options menu is refreshed if the user switches report types
      doinvalidateOptionsMenu();
      return true;
    }
    else if (itemId == R.id.report_year) {
      selectItem(R.id.action_report, POSITION_REPORT, TYPE_REPORT_BY_YEAR);
      // make sure, that the options menu is refreshed if the user switches report types
      doinvalidateOptionsMenu();
      return true;
    }
    else if (itemId == R.id.report_total) {
      selectItem(R.id.action_report, POSITION_REPORT, TYPE_REPORT_TOTAL);
      // make sure, that the options menu is refreshed if the user switches report types
      doinvalidateOptionsMenu();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }


  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (outState != null) {
      outState.putInt(KEY_NAV_ITEM_ID, _selectedItemId);
      outState.putInt(KEY_NAV_ITEM_INDEX, _selectedPosition);
      outState.putShort(KEY_REPORT_TYPE, _reportType);
      outState.putBoolean(KEY_IS_REPORT_MODE, _reportMode);
    }
  }


  @Override
  protected void onAfterInstallOrUpgrade(int previouslyInstalledBuildVersion) {
    if (previouslyInstalledBuildVersion <= 9) {
      showWelcomeDialog();
    }
  }


  protected void showWelcomeDialog() {
    // show welcome Dialog
    DialogUtils.showSimpleInfoDialog(this, "dialog_welcome", R.string.welcome_title, R.string.welcome_message);
  }


  protected void showFAQDialog() {
    DialogUtils.showSimpleInfoDialog(this, "dialog_faq", R.string.faq_title, R.string.faq_message);
  }


  private void openSettings() {
    startActivity(new Intent(this, SettingsActivity.class));
  }


  public void setCustomToolbar(View customToolbar, ResizableToolbarHelper.ToolbarResizeCallback callback, int maxCustomToolbarHeight) {
    _resizableToolbarHelper.setCustomToolbar(customToolbar, callback, maxCustomToolbarHeight);
  }


  @Override
  public int getCurrentToolbarHeight() {
    return _resizableToolbarHelper.getCurrentToolbarHeight();
  }


  @Override
  public int getMaxToolbarHeight() {
    return _resizableToolbarHelper.getMaxToolbarHeight();
  }


  @Override
  public int getMinToolbarHeight() {
    return _resizableToolbarHelper.getMinToolbarHeight();
  }


  public void onScrollChanged(Object source, int scrollY, boolean first, boolean dragging) {
    _resizableToolbarHelper.onScrollChanged(source, scrollY, first, dragging);
  }


  @Override
  public void resizeCustomToolbarSmoothly(Object source, int scrollY, int expectedSize, Runnable runnable) {
    _resizableToolbarHelper.resizeCustomToolbarSmoothly(source, scrollY, expectedSize, runnable);
  }


  @Override
  public int getScreenHeight() {
    return findViewById(R.id.drawer_layout).getHeight();
  }


  @TargetApi(11)
  public void doinvalidateOptionsMenu() {
    if (BuildConfig.VERSION_CODE >= 11) {
      invalidateOptionsMenu();
    }
  }
}
