package de.kalass.agime.overview;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.common.base.Predicate;

import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.Weeks;

import java.util.List;

import de.kalass.agime.AgimeChronicleFragment;
import de.kalass.agime.R;
import de.kalass.agime.customfield.CustomFieldTypeModel;
import de.kalass.agime.overview.model.GroupHeaderType;
import de.kalass.agime.overview.model.GroupHeaderTypes;
import de.kalass.android.common.support.datetime.DatePickerSupport;
import de.kalass.android.common.util.DateUtil;
import de.kalass.android.common.util.TimeFormatUtil;

/**
 * Created by klas on 29.11.13.
 */
public class AgimeWeekOverviewFragment extends AbstractAgimeOverviewFragment {


    private static final int MAX_WEEKS = AgimeChronicleFragment.MAX_YEARS*53; // more than all weeks of the last 10 years

    public class PagerAdapter extends FragmentStatePagerAdapter {


        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }



        @Override
        public Fragment getItem(int i) {
            ActivitiesOverviewListFragment currentListFragment = new ActivitiesOverviewListFragment();
            Bundle args = new Bundle();
            int weeksBeforeCurrentAtPosition = getWeeksBeforeCurrentAtPosition(i);
            args.putInt(
                    ActivitiesOverviewListFragment.ARG_WEEKS_BEFORE_CURRENT,
                    weeksBeforeCurrentAtPosition);
            currentListFragment.setArguments(args);
            prepareFragment(currentListFragment);
            return currentListFragment;
        }

        @Override
        public int getCount() {
            return MAX_WEEKS;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            LocalDate day = (new LocalDate()).minusWeeks(getWeeksBeforeCurrentAtPosition(position));
            return TimeFormatUtil.formatWeekForHeader(getActivity(), day);
        }
    }

    static int getWeeksBeforeCurrentAtPosition(int i) {
        int diff = MAX_WEEKS - (i+1);
        return Math.max(0, diff);
    }

    protected List<GroupHeaderType> createGroupHeaderTypes(List<CustomFieldTypeModel> models) {
        return createGroupHeaderTypes(models, new Predicate<GroupHeaderType>() {
            @Override
            public boolean apply(GroupHeaderType input) {
                return !(input instanceof GroupHeaderTypes.ByMonth) && !(input instanceof GroupHeaderTypes.ByYear);
            }
        });
    }

    @Override
    protected int getInitialPageNum(FragmentStatePagerAdapter adapter, Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            // initial creation, not restoring
            final LocalDate initialDate = getInitialDate();
            final LocalDate today = getToday();
            final int weeks = Weeks.weeksBetween(DateUtil.getFirstDayOfWeek(initialDate), DateUtil.getFirstDayOfWeek(today)).getWeeks();
            int result =  MAX_WEEKS - (weeks + 1);
            return result;
        }
        return super.getInitialPageNum(adapter, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        setHasOptionsMenu(true);
    }

    @Override
    public LocalDate getStartDate() {
        return DateUtil.getFirstDayOfWeek(getDateInWeek());
    }

    @Override
    public LocalDate getEndDate() {
        return DateUtil.getLastDayOfWeek(getDateInWeek());
    }

    protected LocalDate getDateInWeek() {
        return (new LocalDate()).minusWeeks(getWeeksBeforeCurrentAtPosition(getCurrentItem()));
    }

    @Override
    protected FragmentStatePagerAdapter newAdapter(FragmentManager manager) {
        return new PagerAdapter(manager);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.agime_main_menu_week_overview, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_go_to_today:
                //onChangeDayClicked();
                goToCurrent(MAX_WEEKS, R.string.action_go_to_current_week_superfluous);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onDateSelected(int token, final LocalDate date) {
        switch(token) {
            case R.id.action_go_to_today:
                LocalDate firstDayOfSelectedWeek = DateUtil.getFirstDayOfWeek(date);
                LocalDate firstDayOfCurrentWeek = DateUtil.getFirstDayOfWeek(LocalDate.now());
                if (firstDayOfSelectedWeek.isAfter(firstDayOfCurrentWeek)) {
                    Toast.makeText(getActivity(), R.string.action_go_to_date_error_future, Toast.LENGTH_LONG).show();
                    return;
                }
                int currentWeekIndex = MAX_WEEKS -1;
                int weeksPast = Weeks.weeksBetween(firstDayOfSelectedWeek, firstDayOfCurrentWeek).getWeeks();
                int index = currentWeekIndex - weeksPast;

                if (index < 0 ){
                    Toast.makeText(getActivity(), R.string.action_go_to_date_error_past, Toast.LENGTH_LONG).show();
                    return;
                }
                setCurrentItem(index);
                break;
        }
    }

}
