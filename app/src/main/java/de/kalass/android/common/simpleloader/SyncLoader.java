package de.kalass.android.common.simpleloader;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;

import com.google.common.base.Function;
import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.kalass.android.common.activity.ContentResolverUtil;

/**
 * Created by klas on 19.01.14.
 */
public abstract class SyncLoader {

    private final Context _context;
    private ContentObserver _observer;

    public SyncLoader(
            Context context
    ) {
        // be consistent with async loaders: always use the application context to avoid leaking
        // an activity
        _context = context.getApplicationContext();
    }

    public Context getContext() {
        return _context;
    }

    protected ContentObserver getContentObserver() {
        return _observer;
    }

    public void setContentObserver(ContentObserver observer) {
        ContentObserver oldObserver = _observer;
        _observer = observer;
        if (oldObserver != observer && oldObserver != null) {
            getContentResolver().unregisterContentObserver(oldObserver);
        }
    }

    public void close() {
        if (_observer != null) {
            getContentResolver().unregisterContentObserver(_observer);
        }
    }

    protected void observe(Uri uri) {
        if (_observer != null) {
            getContentResolver().registerContentObserver(uri, true, _observer);
        }
    }

    protected <T> T loadFirst(
            Function<Cursor, T> reader,
            Uri uri, String[] projection
    ) {
        return loadFirst(reader, uri, projection, null, null, null);
    }

    protected <T> T loadFirst(
            Function<Cursor, T> reader,
            Uri uri, String[] projection, String selection, String[] selectionArgs, String order
    ) {
        observe(uri);
        return ContentResolverUtil.loadFirstFromContentResolver(_context, reader, uri, projection, selection, selectionArgs, order);
    }

    protected int count(
            Uri uri
    ) {
        observe(uri);
        return ContentResolverUtil.count(_context.getContentResolver(), uri);
    }

    protected <T> List<T> loadList(
            Function<Cursor, T> reader,
            Uri uri, String[] projection, String selection, String[] selectionArgs, String order
    ) {
        observe(uri);
        return ContentResolverUtil.loadFromContentResolver(_context, reader, uri, projection, selection, selectionArgs, order);
    }

    protected <T> List<T> loadList(
            Function<Cursor, T> reader,
            Uri uri, String[] projection
    ) {
        return loadList(reader, uri, projection, null, null, null);
    }

    protected <K, T> Multimap<K, T> loadMultimap(
            Function<Cursor, K> keyReader,
            Function<Cursor, T> valueReader,
            Uri uri, String[] projection, String selection, String[] selectionArgs, String order
    ) {
        observe(uri);
        return ContentResolverUtil.loadIndexedFromContentResolver(_context, keyReader, valueReader, uri, projection, selection, selectionArgs, order);
    }

    protected <K, T> Map<K, T> loadMap(
            Function<Cursor, K> keyReader,
            Function<Cursor, T> valueReader,
            Uri uri, String[] projection, String selection, String[] selectionArgs, String order
    ) {
        observe(uri);
        return ContentResolverUtil.loadMap(getContentResolver(), keyReader, valueReader, uri, projection, selection, selectionArgs, order);
    }

    /**
     * Like ContentResolver#query, but observes the Uri.
     *
     * <b>Important: </b> you need to manage the cursor yourself - i.e. do not forget to close it!
     */
    public final Cursor queryUnmanaged(Uri uri, String[] projection,
                                     String selection, String[] selectionArgs, String sortOrder) {
        observe(uri);
        return getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
    }

    protected ContentResolver getContentResolver() {
        return _context.getContentResolver();
    }

}
