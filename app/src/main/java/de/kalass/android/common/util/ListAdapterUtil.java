package de.kalass.android.common.util;

import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import com.google.common.base.Objects;

import de.kalass.android.common.adapter.AbstractListAdapter;

/**
 * Created by klas on 27.11.13.
 */
public class ListAdapterUtil {

    public static int getPosition(ListAdapter adapter, long itemId) {
        for (int i = 0; i < adapter.getCount(); i++) {
            long curId = adapter.getItemId(i);
            if (curId == itemId) {
                return i;
            }
        }
        return -1;
    }

    public static <T> int getPositionOf(ArrayAdapter<T> adapter, T numServings) {
        return getPositionOf2(adapter, numServings);
    }

    public static <T> int getPositionOf(AbstractListAdapter<T> adapter, T numServings) {
        return getPositionOf2(adapter, numServings);
    }

    private static <T> int getPositionOf2(Adapter adapter, T numServings) {
        for (int i = 0; i < adapter.getCount(); i++) {
            if (Objects.equal(numServings, adapter.getItem(i))) {
                return i;
            }
        }
        return -1;
    }
}
