package de.kalass.agime;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;

/**
* Created by klas on 18.03.15.
*/
public class PageChangeActivityTitleSync implements ViewPager.OnPageChangeListener {

    private Fragment agimeChronicleFragment;

    public PageChangeActivityTitleSync(Fragment agimeChronicleFragment) {
        this.agimeChronicleFragment = agimeChronicleFragment;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        // not needed any more?

        FragmentActivity activity = agimeChronicleFragment.getActivity();
        if (activity != null) {

            View view = agimeChronicleFragment.getView();
            ViewPager pager = (ViewPager)view.findViewById(R.id.pager);
            PagerAdapter adapter = pager.getAdapter();
            activity.setTitle(adapter.getPageTitle(pager.getCurrentItem()));
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
