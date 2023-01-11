package de.kalass.agime.overview;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.common.base.Predicate;

import org.joda.time.LocalDate;
import org.joda.time.Months;

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
public class AgimeMonthOverviewFragment extends AbstractAgimeOverviewFragment implements DatePickerSupport.LocalDateSelectedListener {

    private static final int MAX_MONTHS = AgimeChronicleFragment.MAX_YEARS*12; // All months, 10 years back

    public class PagerAdapter extends FragmentStatePagerAdapter {


        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }



        @Override
        public Fragment getItem(int i) {
            //Log.i("AgimeMainActivity", "getItem " + i);
            ActivitiesOverviewListFragment currentListFragment = new ActivitiesOverviewListFragment();
            Bundle args = new Bundle();
            int monthsBeforeCurrent = getMonthsBeforeCurrent(i);
            args.putInt(
                    ActivitiesOverviewListFragment.ARG_MONTHS_BEFORE_CURRENT,
                    monthsBeforeCurrent);
            currentListFragment.setArguments(args);
            prepareFragment(currentListFragment);
            return currentListFragment;
        }

        @Override
        public int getCount() {
            return MAX_MONTHS;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            LocalDate day = new LocalDate().minusMonths(getMonthsBeforeCurrent(position));
            return TimeFormatUtil.formatMonthForHeader(getActivity(), day);
        }
    }
    @Override
    protected int getInitialPageNum(FragmentStatePagerAdapter adapter, Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            // initial creation, not restoring
            final int months = Months.monthsBetween(DateUtil.getFirstDayOfMonth(getInitialDate()), DateUtil.getFirstDayOfMonth(getToday())).getMonths();
            return MAX_MONTHS - (months + 1);
        }
        return super.getInitialPageNum(adapter, savedInstanceState);
    }

    protected List<GroupHeaderType> createGroupHeaderTypes(List<CustomFieldTypeModel> models) {
        return createGroupHeaderTypes(models, new Predicate<GroupHeaderType>() {
            @Override
            public boolean apply(GroupHeaderType input) {
                return !(input instanceof GroupHeaderTypes.ByYear);
            }
        });
    }

    static int getMonthsBeforeCurrent(int i) {
        int diff = MAX_MONTHS - (i+1);
        return Math.max(0, diff);
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        setHasOptionsMenu(true);
    }

    @Override
    public LocalDate getStartDate() {
        return DateUtil.getFirstDayOfMonth(getDateInMonth());
    }

    public LocalDate getEndDate() {
        return DateUtil.getLastDayOfMonth(getDateInMonth());
    }

    protected LocalDate getDateInMonth() {
        return new LocalDate().minusMonths(getMonthsBeforeCurrent(getCurrentItem()));
    }

    @Override
    protected FragmentStatePagerAdapter newAdapter(FragmentManager manager) {
        return new PagerAdapter(manager);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.agime_main_menu_month_overview, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_go_to_today:

                goToCurrent(MAX_MONTHS, R.string.action_go_to_current_month_superfluous);
                //onChangeDayClicked();
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
                LocalDate firstDayOfSelectedMonth = DateUtil.getFirstDayOfMonth(date);
                LocalDate firstDayOfCurrentMonth = DateUtil.getFirstDayOfMonth(LocalDate.now());
                if (firstDayOfSelectedMonth.isAfter(firstDayOfCurrentMonth)) {
                    Toast.makeText(getActivity(), R.string.action_go_to_date_error_future, Toast.LENGTH_LONG).show();
                    return;
                }
                int currentWeekIndex = MAX_MONTHS -1;
                int monthsPast = Months.monthsBetween(firstDayOfSelectedMonth, firstDayOfCurrentMonth).getMonths();
                int index = currentWeekIndex - monthsPast;

                if (index < 0 ){
                    Toast.makeText(getActivity(), R.string.action_go_to_date_error_past, Toast.LENGTH_LONG).show();
                    return;
                }
                setCurrentItem(index);
                break;
        }
    }
}
