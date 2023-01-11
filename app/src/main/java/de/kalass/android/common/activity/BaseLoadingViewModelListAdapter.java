package de.kalass.android.common.activity;

import android.content.Context;

import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import de.kalass.android.common.adapter.AbstractLoadingViewModelListAdapter;
import de.kalass.android.common.model.IViewModel;

/**
 * Created by klas on 08.01.14.
 * @param <W> The java type of a container that wraps the relevant parts of the view instance for easy access
 * @param <D> The java type of the data items loaded
 */
public abstract class BaseLoadingViewModelListAdapter<W, D extends IViewModel>
        extends AbstractLoadingViewModelListAdapter<D>
        implements LoaderManager.LoaderCallbacks<List<D>> {


    public BaseLoadingViewModelListAdapter(Context context, int layoutResourceId) {
        super(context, layoutResourceId);
    }


    @Override
    public void onLoadFinished(Loader<List<D>> loader, List<D> data) {
        setItems(data);
    }

    @Override
    public void onLoaderReset(Loader<List<D>> loader) {
        setItems(null);
    }

    protected abstract W onWrapView(View view);


    @Override
    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);
        setWrappedView(view, onWrapView(view));
        return view;
    }

    protected final void setWrappedView(View view, W wrapped) {
        view.setTag(wrapped);
    }

    @Override
    protected View fillView(View view, D model, int position) {
        bindWrappedView(getWrappedView(view), model, position);
        return view;
    }

    public abstract void bindWrappedView(W view, D model, int position);

    protected final W getWrappedView(View view) {
        return (W)view.getTag();
    }
}
