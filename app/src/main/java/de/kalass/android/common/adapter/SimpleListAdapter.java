package de.kalass.android.common.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.google.common.collect.ImmutableList;

import java.util.Arrays;
import java.util.List;

import de.kalass.android.common.model.IViewModel;

/**
 * Created by klas on 22.12.13.
 */
public abstract class SimpleListAdapter<T extends IViewModel> extends BaseAdapter {


    private final Context _context;
    private List<T> _items = ImmutableList.of();

    public SimpleListAdapter(Context context) {
        _context = context;
    }

    public Context getContext() {
        return _context;
    }

    public void setItems(List<T> items) {
        _items = items == null ? ImmutableList.<T>of() : items;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return _items.size();
    }

    @Override
    public T getItem(int position) {
        return _items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId();
    }

    public abstract View newView(Context context, T item, int position, ViewGroup parent);

    public View newDropDownView(Context context, T item, int position, ViewGroup parent) {
        return newView(context, item, position, parent);
    }

    public abstract void bindView(View view, T item, int position);

    public void bindDropDownView(View view, T item, int position) {
        bindView(view, item, position);
    }

    @Override
    public final View getView(int position, View convertView, ViewGroup parent) {
        T item = getItem(position);
        View view = convertView == null ? newView(_context, item, position, parent) : convertView;
        bindView(view, item, position);
        return view;
    }

    @Override
    public final View getDropDownView(int position, View convertView, ViewGroup parent) {
        T item = getItem(position);
        View view = convertView == null ? newDropDownView(_context, item, position, parent) : convertView;
        bindDropDownView(view, item, position);
        return view;
    }


}
