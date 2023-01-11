package de.kalass.agime.overview;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import com.google.common.base.Predicate;

import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.util.List;

import de.kalass.agime.AgimeChronicleFragment;
import de.kalass.agime.R;
import de.kalass.agime.Workarounds;
import de.kalass.agime.customfield.CustomFieldTypeModel;
import de.kalass.agime.overview.model.GroupHeaderType;
import de.kalass.agime.overview.model.GroupHeaderTypes;
import de.kalass.agime.provider.MCContract;
import de.kalass.agime.trackactivity.TrackActivity;
import de.kalass.android.common.support.datetime.DatePickerSupport;
import de.kalass.android.common.util.TimeFormatUtil;

/**
 * Created by klas on 06.10.13.
 */
public class AgimeDayOverviewFragment extends AbstractAgimeOverviewFragment implements DatePickerSupport.LocalDateSelectedListener {
    private static final int MAX_DAYS = AgimeChronicleFragment.MAX_DAYS;
    private static final int ACTIVITY_CODE_TRACK_TIME = 42;

    public class PagerAdapter extends FragmentStatePagerAdapter {


        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }


        @Override
        public Fragment getItem(int i) {
            //Log.i("AgimeMainActivity", "getItem " + i);
            ActivitiesOverviewListFragment currentListFragment = new ActivitiesOverviewListFragment();
            Bundle args = new Bundle();
            int daysBeforeTodayAtPosition = getDaysBeforeTodayAtPosition(i);
            args.putInt(
                    ActivitiesOverviewListFragment.ARG_DAYS_BEFORE_TODAY,
                    daysBeforeTodayAtPosition);
            currentListFragment.setArguments(args);
            prepareFragment(currentListFragment);
            return currentListFragment;
        }

        @Override
        public int getCount() {
            return MAX_DAYS;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            LocalDate day = new LocalDate().minusDays(getDaysBeforeTodayAtPosition(position));
            return TimeFormatUtil.formatTimeForHeader(getActivity(), day);
        }
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

    static int getDaysBeforeTodayAtPosition(int i) {
        int diff = MAX_DAYS - (i+1);
        return Math.max(0, diff);
    }

    protected List<GroupHeaderType> createGroupHeaderTypes(List<CustomFieldTypeModel> models) {
        return createGroupHeaderTypes(models, new Predicate<GroupHeaderType>() {
            @Override
            public boolean apply(GroupHeaderType input) {
                return !(input instanceof GroupHeaderTypes.ByWeek) && !(input instanceof GroupHeaderTypes.ByMonth) && !(input instanceof GroupHeaderTypes.ByYear);
            }
        });
    }

    @Override
    public LocalDate getStartDate() {
        return new LocalDate().minusDays(getDaysBeforeTodayAtPosition(getCurrentItem()));
    }

    @Override
    public LocalDate getEndDate() {
        return getStartDate();
    }

    @Override
    protected FragmentStatePagerAdapter newAdapter(FragmentManager manager) {
        return new PagerAdapter(manager);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);
        ImageButton fab = (ImageButton) view.findViewById(R.id.fab);
        //attach to list view will make the button disappear on scroll - but we want it to stay
        //fab.attachToListView(_listView);
        fab.setVisibility(View.VISIBLE);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackTime();
            }
        });

    }

    private void trackTime() {
        trackTime(getStartDate().toDateTimeAtStartOfDay().getMillis(), null);
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (Workarounds.nestedFragmentMenuInitializedByParent()) {
            ActivitiesOverviewListFragment.inflateMenu(menu, inflater);
        }
        inflater.inflate(R.menu.agime_main_menu_day_overview, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_go_to_today:
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
        switch(token) {
            case R.id.action_go_to_today:
                goToDate(date, MAX_DAYS, R.string.action_go_to_date_error_future, R.string.action_go_to_date_error_past);
                //getWrappedView().setActiveUntilDay(date);
                break;
        }
    }

}
