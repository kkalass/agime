package de.kalass.agime.util;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.AbsListView;

import com.github.ksoichiro.android.observablescrollview.ObservableListView;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by klas on 13.04.15.
 */
public class KListView extends ObservableListView {
    private static final class OnScrollListenerDelegate implements OnScrollListener {
        private OnScrollListener _primary;
        private List<OnScrollListener> _listeners;

        public void set(OnScrollListener l) {
            _primary = l;
        }

        public boolean add(OnScrollListener l) {
            if (_listeners == null) {
                _listeners = new ArrayList<>();
            }
            return _listeners.add(l);
        }

        public boolean remove(OnScrollListener l){
            if (_listeners == null) {
                return false;
            }
            return _listeners.remove(l);
        }


        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if (_primary != null) {
                _primary.onScrollStateChanged(view, scrollState);
            }
            if (_listeners != null) {
                // copy, to make sure that there is no concurrent modification exception when
                // the listener removes itself
                for (OnScrollListener l: ImmutableList.copyOf(_listeners)) {
                    l.onScrollStateChanged(view, scrollState);
                }
            }
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            if (_primary != null) {
                _primary.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
            }
            if (_listeners != null) {
                // copy, to make sure that there is no concurrent modification exception when
                // the listener removes itself
                for (OnScrollListener l: ImmutableList.copyOf(_listeners)) {
                    l.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
                }
            }

        }
    }

    private final OnScrollListenerDelegate listener = new OnScrollListenerDelegate();

    public KListView(Context context) {
        super(context);
        super.setOnScrollListener(listener);
    }

    public KListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        super.setOnScrollListener(listener);
    }

    public KListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        super.setOnScrollListener(listener);
    }


    @Override
    public void setOnScrollListener(OnScrollListener l) {
        listener.set(l);
    }

    public boolean addOnScrollListener(OnScrollListener l) {
        return listener.add(l);
    }

    public boolean removeOnScrollListener(OnScrollListener l) {
        return listener.remove(l);
    }
}
