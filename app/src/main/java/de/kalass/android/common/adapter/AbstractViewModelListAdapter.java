package de.kalass.android.common.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.google.common.collect.ImmutableList;

import java.util.List;

import de.kalass.android.common.model.IViewModel;

/**
 * Common Baseclass for ListAdapters that adapt a list of view models to be shown in the UI.
 *
* Created by klas on 22.10.13.
*/
public abstract class AbstractViewModelListAdapter<T extends IViewModel> extends AbstractListAdapter<T> {


    public AbstractViewModelListAdapter(Context context, int layoutResourceId) {
        super(context, layoutResourceId);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId();
    }

}
