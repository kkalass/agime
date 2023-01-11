package de.kalass.agime;

import android.os.Bundle;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.common.base.Objects;

import org.joda.time.Days;
import org.joda.time.LocalDate;

import de.kalass.android.common.support.fragments.BaseFragment;

/**
 * Baseclass for Layouts that contain a ViewPager.
 *
 * The trouble is, that at least on Android 4.1 there seems to be subtle bugs in the support
 * library when it comes to nesting fragments: The +-Button will not be added and - worse - the
 * action bar spinner does not lead to the appropriate callback method being called.
 *
 * This Baseclass tries to work around the problems by using post handlers
 *
 * Inspired by http://stackoverflow.com/questions/6221763/android-can-you-nest-fragments
 *
 * Created by klas on 06.10.13.
 */
public abstract class AbstractViewPagerFragment extends BaseFragment {

    public static final String ARGS_PAGE_NUM = "argsPageNum";
    public static final String LOG_TAG = "AbstractViewPagerFrgmt";

    private LocalDate _today;
    private ViewPager _viewPager;
    private final int _layout;
    private final int _pagerId;


    protected AbstractViewPagerFragment(int layout, int pagerId) {
        _layout = layout;
        _pagerId = pagerId;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        _today = new LocalDate();
        View view = inflater.inflate(_layout, container, false);

        _viewPager = (ViewPager) view.findViewById(_pagerId);

        final FragmentStatePagerAdapter adapter = newAdapter(getChildFragmentManager());

        int currentPageNum = getInitialPageNum(adapter, savedInstanceState);
        setAdapter(currentPageNum, adapter);
        return view;
    }

    protected int getInitialPageNum(FragmentStatePagerAdapter adapter,Bundle savedInstanceState) {
        Log.i(LOG_TAG, "getInitialPageNum: " + savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey(ARGS_PAGE_NUM)) {
            int savedValue =  savedInstanceState.getInt(ARGS_PAGE_NUM);
            Log.i(LOG_TAG, "getInitialPageNum 2: " + savedValue);
            return savedValue;
        }

        return adapter.getCount() - 1;
    }

    public LocalDate getToday() {
        return _today;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        final int currentItem = getCurrentItem();
        Log.i(LOG_TAG, "onSaveInstanceState currentItem: " + currentItem);
        outState.putInt(ARGS_PAGE_NUM, currentItem);
    }

    protected int getCurrentItem() {
        return _viewPager.getCurrentItem();
    }

    protected void setCurrentItem(int item) {
        _viewPager.setCurrentItem(item);
    }

    protected abstract FragmentStatePagerAdapter newAdapter(FragmentManager manager);


    /**
     * Set the ViewPager adapter from this or from a subclass.
     *
     * @author chris.jenkins
     * @param adapter
     */
    protected void setAdapter(final int currentPageNum, final FragmentStatePagerAdapter adapter)
    {
        doInsertFragments(currentPageNum, adapter);
    }

    protected void doInsertFragments(int currentPageNum, final FragmentStatePagerAdapter pagerAdapter) {
        _viewPager.setAdapter(pagerAdapter);
        _viewPager.setCurrentItem(currentPageNum);
    }

    @Override
    public void onStart() {
        super.onStart();

        LocalDate today = new LocalDate();
        if (!Objects.equal(today, _today) && _viewPager != null) {
            PagerAdapter adapter = _viewPager.getAdapter();
            if (adapter != null) {
                // enforce rerendering of titles
                adapter.notifyDataSetChanged();
            }
        }
        _today = today;

    }

    protected void goToDate(LocalDate date, int max, int futureErrorResId, int pastErrorResId) {
        LocalDate today = LocalDate.now();
        if (date.isAfter(today)) {
            Toast.makeText(getActivity(), futureErrorResId, Toast.LENGTH_LONG).show();
            return;
        }
        int todayIndex = max -1;
        int daysPast = Days.daysBetween(date, today).getDays();
        int index = todayIndex - daysPast;

        if (index < 0 ){
            Toast.makeText(getActivity(), pastErrorResId, Toast.LENGTH_LONG).show();
            return;
        }
        setCurrentItem(index);
    }

    protected void goToCurrent(int max, int superfluousResId) {
        int index = max - 1;
        if (getCurrentItem() == index) {
            Toast.makeText(getActivity(), superfluousResId, Toast.LENGTH_LONG).show();
        } else {
            setCurrentItem(index);
        }
    }

}
