package de.kalass.android.common.activity;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import com.google.common.base.Function;

/**
 * Baseclass for simple CRUD Fragments that operate on a Cursor.
 *
 * @param <C> Type of the view wrapper - extremely useful for storing shortcuts to child views and generally organizing access to your view
 */
public abstract class BaseCursorCRUDFragment<C, D> extends BaseCRUDFragment<C, Cursor> {
    private static final String TAG = "BaseCursorCRUDFragment";

    private final String[] _projection;
    private final Function<Cursor, D> _reader;

    private Cursor _cursor;

    public BaseCursorCRUDFragment(
            int layout,
            String contentTypeDir,
            String contentTypeItem,
            String[] projection,
            Function<Cursor, D> reader
    ) {
        super(layout, contentTypeDir, contentTypeItem);
        _projection = projection;
        _reader = reader;
    }


    @Override
    public AsyncTaskLoader<Cursor> createLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_CRUD:
                if (getMode() == CRUDMode.INSERT) {
                    // ensure that there is an empty cursor for inserts - this is necessary,
                    // so that the loader callbacks are called for inserts, and the subclasses
                    // may consistently bind their data
                    return new CursorLoader(getContext(), getUri(), _projection, " _id is null ", null, null);
                }
                return new CursorLoader(getContext(), getUri(), _projection, null, null, null);
        }

        return null;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() == LOADER_ID_CRUD) {
            if (_cursor != null) {
                _cursor.close();
                _cursor = null;
            }
        }
        super.onLoaderReset(loader);
    }

    @Override
    protected final void onBindView(C view, Cursor data) {
        if (_cursor != null && _cursor != data) {
            _cursor.close();
        }
        _cursor = data;
        if (_cursor != null) {
            _cursor.moveToFirst();
        }
        final Cursor cursor = (_cursor != null && _cursor.getCount() == 0) ? null : _cursor;
        final D converted = cursor == null ? null : _reader.apply(cursor);
        onBindViewToCursor(view, converted);
    }

    protected abstract void onBindViewToCursor(C view, D data);

    protected abstract void readDataFromView(C view, ContentValues values);

    protected void save() {
        ContentValues values = new ContentValues();
        readDataFromView(getWrappedView(), values);

        performSaveOrUpdateAsync(createSaveOrUpdateOperation(
                getMode(), values, System.currentTimeMillis()
        ));
    }


}
