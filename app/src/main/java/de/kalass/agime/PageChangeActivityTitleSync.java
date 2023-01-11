package de.kalass.agime;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
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
