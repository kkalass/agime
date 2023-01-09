package de.kalass.agime.util;

import android.content.Context;
import android.util.Log;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.widget.ListView;

import de.kalass.android.common.util.ListAdapterUtil;

/**
 * Created by klas on 27.11.13.
 */
public class ListViewUtil {

    public static void scrollToItemAfterDataChange(final KListView listView, final long itemId, final Runnable finishedCallback) {
        if (listView == null) {
            return;
        }

        new OneTimeDataSetObserver() {
            @Override
            protected void changed() {
                Context c = listView.getContext();
                if (c == null) {
                    return;
                }
                listView.post(new Runnable() {

                    @Override
                    public void run() {
                        if (listView.getContext() == null) {
                            return;
                        }
                        ListAdapter adapter = listView.getAdapter();
                        if (adapter == null) {
                            return;
                        }
                        final int pos = ListAdapterUtil.getPosition(adapter, itemId);
                        if (pos >= 0) {

                            listView.smoothScrollToPosition(pos);
                            listView.addOnScrollListener(new AbsListView.OnScrollListener() {
                                boolean isVisible = false;
                                @Override
                                public void onScrollStateChanged(AbsListView view, int scrollState) {
                                    //Log.d("Frg", "onScrollStateChanged " + scrollState);
                                    if (isVisible && scrollState == SCROLL_STATE_IDLE) {
                                        //Log.d("Frg", "************ AFTER SCROLL **********+");
                                        listView.removeOnScrollListener(this);
                                        finishedCallback.run();
                                    }
                                }

                                @Override
                                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                                    //Log.d("Frg", "onScroll(" + firstVisibleItem + ", " + visibleItemCount + ")");
                                    isVisible = firstVisibleItem <= pos && firstVisibleItem + visibleItemCount >= pos;
                                }
                            });

                        } else {

                            finishedCallback.run();
                        }
                    }
                });
            }
        }.once(listView.getAdapter());
    }
}
