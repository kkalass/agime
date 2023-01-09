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

import org.joda.time.LocalDate;
import org.joda.time.Years;

import de.kalass.agime.AgimeChronicleFragment;
import de.kalass.agime.R;
import de.kalass.android.common.support.datetime.DatePickerSupport;
import de.kalass.android.common.util.DateUtil;
import de.kalass.android.common.util.TimeFormatUtil;

/**
 * Created by klas on 29.11.13.
 * Of course this Fragment (Total) does not need to have a pager. But it seems to be easier to keep it in sync with the other views this way.
 */
public class AgimeTotalOverviewFragment extends AbstractAgimeOverviewFragment  {



    public class PagerAdapter extends FragmentStatePagerAdapter {


        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }



        @Override
        public Fragment getItem(int i) {
            //Log.i("AgimeMainActivity", "getItem " + i);
            ActivitiesOverviewListFragment currentListFragment = new ActivitiesOverviewListFragment();
            Bundle args = new Bundle();
            // no args defaults to "total"
            currentListFragment.setArguments(args);
            prepareFragment(currentListFragment);
            return currentListFragment;
        }

        @Override
        public int getCount() {
            return 1;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getActivity().getString(R.string.overview_total);
        }
    }


    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        setHasOptionsMenu(true);
    }

    @Override
    public LocalDate getStartDate() {
        return new LocalDate(1970,1,1);
    }

    public LocalDate getEndDate() {
        return LocalDate.now();
    }


    @Override
    protected FragmentStatePagerAdapter newAdapter(FragmentManager manager) {
        return new PagerAdapter(manager);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.agime_main_menu_total_overview, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }



    @Override
    public void onDateSelected(int token, final LocalDate date) {
        switch(token) {
            case R.id.action_go_to_today:
                // nothing to do
                Toast.makeText(getActivity(), R.string.action_go_to_date_error_not_supported, Toast.LENGTH_LONG).show();
                break;
        }
    }
}
