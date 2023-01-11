package de.kalass.android.common.adapter;

import android.content.Context;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import java.util.List;

import de.kalass.android.common.model.IViewModel;

/**
 * UI for a List of all "Trackpoints" where the user tracked a meal, drink, symptom etc
 */
public abstract class AbstractLoadingViewModelListAdapter<T extends IViewModel>
        extends AbstractViewModelListAdapter<T>
        implements LoaderManager.LoaderCallbacks<List<T>> {


    public AbstractLoadingViewModelListAdapter(Context context, int layoutResourceId) {
        super(context, layoutResourceId);
    }

    @Override
    public void onLoadFinished(Loader<List<T>> loader, List<T> data) {
        setItems(data);
    }

    @Override
    public void onLoaderReset(Loader<List<T>> loader) {
        setItems(null);
    }

}