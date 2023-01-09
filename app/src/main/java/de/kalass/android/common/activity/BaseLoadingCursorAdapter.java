package de.kalass.android.common.activity;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;

/**
 * Created by klas on 08.01.14.
 * @param <W> The java type of a container that wraps the relevant parts of the view instance for easy access
 */
public abstract class BaseLoadingCursorAdapter<W> extends BaseCursorAdapter<W>
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private Uri _uri;
    private String[] _projection;
    private String _selection;
    private String[] _selectionArgs;
    private String _sortOrder;

    /**
     *
     * @param context  The context
     * @param flags    Flags used to determine the behavior of the adapter; may
     *                 be any combination of {@link #FLAG_AUTO_REQUERY} and
     *                 {@link #FLAG_REGISTER_CONTENT_OBSERVER}.
     */
    public BaseLoadingCursorAdapter(Context context, int layout, int flags) {
        super(context, layout, flags);
    }

    public BaseLoadingCursorAdapter<W> setQueryParams(Uri uri, String[] projection, String selection,
                               String[] selectionArgs, String sortOrder) {
        _uri = uri;
        _projection = projection;
        _selection = selection;
        _selectionArgs = selectionArgs;
        _sortOrder = sortOrder;
        return this;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(
                getContext(),
                _uri, _projection, _selection, _selectionArgs, _sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        changeCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        changeCursor(null);
    }
}
