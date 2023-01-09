package de.kalass.android.common.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListAdapter;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import java.util.Arrays;
import java.util.List;

import de.kalass.android.common.model.IViewModel;
import de.kalass.android.common.util.StringUtil;

/**
 * Common Baseclass for ListAdapters that adapt a list of view models to be shown in the UI.
 *
* Created by klas on 22.10.13.
*/
public abstract class AbstractListAdapter<T> extends BaseAdapter {

    private final Context _context;
    private final int _layoutResourceId;
    private final int _dropDownLayoutResourceId;
    private List<T> _items = ImmutableList.of();
    private final LayoutInflater _inflater;
    private List<T> _allItems;

    public AbstractListAdapter(Context context, int layoutResourceId) {
        this(context, layoutResourceId, layoutResourceId);
    }
    public AbstractListAdapter(Context context, int layoutResourceId, int dropDownLayoutResourceId, T... items) {
        this(context, layoutResourceId, dropDownLayoutResourceId, Arrays.asList(items));
    }

    public AbstractListAdapter(Context context, int layoutResourceId, List<T> items) {
        this(context, layoutResourceId, layoutResourceId, items);
    }

    public LayoutInflater getLayoutInflater() {
        return _inflater;
    }

    public AbstractListAdapter(Context context, int layoutResourceId, int dropDownLayoutResourceId, List<T> items) {
        _context = context;
        _layoutResourceId = layoutResourceId;
        _dropDownLayoutResourceId = dropDownLayoutResourceId;
        _inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        setItems(items);
    }

    public String getString(int resId) {
        return getContext().getString(resId);
    }

    public String getString(int resId, Object... params) {
        return getContext().getString(resId, params);
    }

    public Context getContext() {
        return _context;
    }

    public static <T> void setItems(ListAdapter adapter, List<T> items) {
        //noinspection unchecked
        AbstractListAdapter<T> adapter1 = (AbstractListAdapter<T>)adapter;
        adapter1.setItems(items);
    }

    private void setFilteredItems(List<T> items) {
        _items = items == null ? ImmutableList.<T>of() : items;
        notifyDataSetChanged();
    }

    public void setItems(List<T> items) {
        _items = items == null ? ImmutableList.<T>of() : items;
        _allItems = _items;
        notifyDataSetChanged();
    }

    public int getPosition(T item) {
        for (int i = 0; i < _items.size(); i++) {
            T it = _items.get(i);
            if (item.equals(it)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getCount() {
        return _items.size();
    }

    @Override
    public T getItem(int position) {
        return _items.get(position);
    }

    protected List<T> getItems() {
        return _items;
    }

    @Override
    public long getItemId(int position) {
        T item = getItem(position);
        if (item instanceof IViewModel) {
            return ((IViewModel)item).getId();
        }
        return position;
    }

    protected View getOrCreateView(View convertView, ViewGroup parent, T item) {
        if (convertView == null) {
            return onCreateView(parent);
        }
        return convertView;
    }

    protected View onCreateView(ViewGroup parent) {
        return _inflater.inflate(_layoutResourceId, parent, false);
    }

    protected abstract View fillView(View view, T model, int position);

    @Override
    public final View getView(int position, View convertView, ViewGroup parent) {
        T item = getItem(position);
        View view = getOrCreateView(convertView, parent, item);
        fillView(view, item, position);

        return view;
    }


    private View getOrCreateDropDownView(View convertView, ViewGroup parent) {
        if (convertView == null) {
            return _inflater.inflate(_dropDownLayoutResourceId, parent, false);
        }
        return convertView;
    }

    protected View fillDropDownView(View view, T model, int position) {
        return fillView(view, model, position);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        T item = getItem(position);
        View view = getOrCreateDropDownView(convertView, parent);
        fillDropDownView(view, item, position);

        return view;
    }

    public static <T> T getItem(final ListAdapter listAdapter, final Class<T> ingredientClass, final int position) {
        //noinspection unchecked
        return ((AbstractListAdapter<T>)listAdapter).getItem(position);

    }

    protected abstract class ListAdapterFilter extends Filter {
        @Override
        protected FilterResults performFiltering(final CharSequence constraint) {
            ImmutableList.Builder<T> builder = ImmutableList.builder();
            for (T item: _allItems) {
                if (matches(item, constraint)) {
                    builder.add(item);
                }
            }
            ImmutableList<T> list = builder.build();
            FilterResults results = new FilterResults();
            results.count = list.size();
            results.values = list;
            return results;
        }

        public boolean matches(final CharSequence s, final CharSequence constraint) {
            String string = Strings.nullToEmpty(StringUtil.toString(s));
            String test = constraint.toString();
            boolean r = string.startsWith(test);
            Log.i("ListAdapter", "matches('" + string + "', '" + test + "') => " + r);
            return r;
            //return string.contains(constraint);
        }

        @Override
        public CharSequence convertResultToString(final Object resultValue) {
            return convertToString((T)resultValue);
        }

        public abstract CharSequence convertToString(T value);

        public boolean matches(T value, CharSequence constraint) {
            return matches(convertToString(value), constraint);
        }

        @Override
        protected void publishResults(final CharSequence constraint, final FilterResults results) {
            setFilteredItems((List<T>) results.values);
        }
    }
}
