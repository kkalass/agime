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

import org.joda.time.LocalDate;
import org.joda.time.Years;

import de.kalass.agime.AgimeChronicleFragment;
import de.kalass.agime.R;
import de.kalass.android.common.support.datetime.DatePickerSupport;
import de.kalass.android.common.util.DateUtil;
import de.kalass.android.common.util.TimeFormatUtil;


/**
 * Created by klas on 29.11.13.
 */
public class AgimeYearOverviewFragment extends AbstractAgimeOverviewFragment implements DatePickerSupport.LocalDateSelectedListener {

	private static final int MAX_YEARS = AgimeChronicleFragment.MAX_YEARS; // 10 years back

	public class PagerAdapter extends FragmentStatePagerAdapter {

		public PagerAdapter(FragmentManager fm) {
			super(fm);
		}


		@Override
		public Fragment getItem(int i) {
			//Log.i("AgimeMainActivity", "getItem " + i);
			ActivitiesOverviewListFragment currentListFragment = new ActivitiesOverviewListFragment();
			Bundle args = new Bundle();
			int yearsBeforeCurrent = getYearsBeforeCurrent(i);
			args.putInt(
				ActivitiesOverviewListFragment.ARG_YEARS_BEFORE_CURRENT,
				yearsBeforeCurrent);
			currentListFragment.setArguments(args);
			prepareFragment(currentListFragment);
			return currentListFragment;
		}


		@Override
		public int getCount() {
			return MAX_YEARS;
		}


		@Override
		public CharSequence getPageTitle(int position) {
			LocalDate day = new LocalDate().minusYears(getYearsBeforeCurrent(position));
			return TimeFormatUtil.formatYearForHeader(getActivity(), day);
		}
	}

	@Override
	protected int getInitialPageNum(FragmentStatePagerAdapter adapter, Bundle savedInstanceState) {
		if (savedInstanceState == null) {
			// initial creation, not restoring
			final int years = Years.yearsBetween(DateUtil.getFirstDayOfYear(getInitialDate()), DateUtil.getFirstDayOfYear(getToday())).getYears();
			return MAX_YEARS - (years + 1);
		}
		return super.getInitialPageNum(adapter, savedInstanceState);
	}


	static int getYearsBeforeCurrent(int i) {
		int diff = MAX_YEARS - (i + 1);
		return Math.max(0, diff);
	}


	@Override
	public void onViewCreated(View view, Bundle bundle) {
		super.onViewCreated(view, bundle);
		setHasOptionsMenu(true);
	}


	@Override
	public LocalDate getStartDate() {
		return DateUtil.getFirstDayOfYear(getDateInYear());
	}


	public LocalDate getEndDate() {
		return DateUtil.getLastDayOfYear(getDateInYear());
	}


	protected LocalDate getDateInYear() {
		return new LocalDate().minusYears(getYearsBeforeCurrent(getCurrentItem()));
	}


	@Override
	protected FragmentStatePagerAdapter newAdapter(FragmentManager manager) {
		return new PagerAdapter(manager);
	}


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.agime_main_menu_year_overview, menu);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_go_to_today) {
			goToCurrent(MAX_YEARS, R.string.action_go_to_current_year_superfluous);
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
		if (token == R.id.action_go_to_today) {
			LocalDate firstDayOfSelectedYear = DateUtil.getFirstDayOfYear(date);
			LocalDate firstDayOfCurrentYear = DateUtil.getFirstDayOfYear(LocalDate.now());
			if (firstDayOfSelectedYear.isAfter(firstDayOfCurrentYear)) {
				Toast.makeText(getActivity(), R.string.action_go_to_date_error_future, Toast.LENGTH_LONG).show();
				return;
			}
			int currentWeekIndex = MAX_YEARS - 1;
			int yearsPast = Years.yearsBetween(firstDayOfSelectedYear, firstDayOfCurrentYear).getYears();
			int index = currentWeekIndex - yearsPast;

			if (index < 0) {
				Toast.makeText(getActivity(), R.string.action_go_to_date_error_past, Toast.LENGTH_LONG).show();
				return;
			}
			setCurrentItem(index);
		}
	}
}
