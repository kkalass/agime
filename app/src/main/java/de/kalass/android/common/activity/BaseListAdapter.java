package de.kalass.android.common.activity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.kalass.android.common.adapter.SimpleListAdapter;
import de.kalass.android.common.model.IViewModel;

/**
 * Created by klas on 08.01.14.
 * @param <T> The java type of the data item of the list
 * @param <W> The java type of a container that wraps the relevant parts of the view instance for easy access
 */
public abstract class BaseListAdapter<W, T extends IViewModel> extends SimpleListAdapter<T> {

    private final LayoutInflater _inflater;
    private final int _layout;
    private final int _dropDownLayout;

    public BaseListAdapter(Context context, int layout) {
        this(context, layout, layout);
    }

    public BaseListAdapter(Context context, int layout, int dropDownLayout) {
        super(context);
        _inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        _layout = layout;
        _dropDownLayout = dropDownLayout;
    }

    protected abstract W onWrapView(View view);

    protected W onWrapDropDownView(View view) {
        return onWrapView(view);
    }

    @Override
    public View newView(Context context, T item, int position, ViewGroup parent) {
        View view =  _inflater.inflate(_layout, parent, false);
        view.setTag(onWrapView(view));
        return view;
    }

    public abstract void bindWrappedView(W view, T item, int position);

    @Override
    public void bindView(View view, T item, int position) {
        bindWrappedView(getWrappedView(view), item, position);
    }

    private W getWrappedView(View view) {
        return (W)view.getTag();
    }

    @Override
    public View newDropDownView(Context context, T item, int position, ViewGroup parent) {
        View view =  _inflater.inflate(_dropDownLayout, parent, false);
        view.setTag(onWrapDropDownView(view));
        return view;
    }


}
