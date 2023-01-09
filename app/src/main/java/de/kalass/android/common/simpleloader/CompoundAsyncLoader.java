package de.kalass.android.common.simpleloader;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;

import java.util.List;
import java.util.Map;

/**
 * Asynchronous loader that delegates all calls to a SyncLoader - this way, we can reuse code more easily
 * Created by klas on 19.01.14.
 */
public abstract class CompoundAsyncLoader<T> extends DelegatingAsyncLoader<CompoundSyncLoader, T> {

    public CompoundAsyncLoader(Context context, ObserveDataSourceMode observeDataSourceMode, SyncLoader... delegates) {
        this(context, observeDataSourceMode, new CompoundSyncLoader(context, delegates) {});
    }

    public CompoundAsyncLoader(Context context, ObserveDataSourceMode observeDataSourceMode, CompoundSyncLoader loader) {
        super(context, observeDataSourceMode, loader);

    }

    protected <L extends SyncLoader> L add(L loader) {
        getLoader().add(loader);
        return loader;
    }

    protected void observe(Uri uri) {
        getLoader().observe(uri);
    }


    protected <T> T loadById(
            Function<Cursor, T> reader,
            Uri dirUri, String[] projection, long id
    ) {
        return Preconditions.checkNotNull(getLoader().loadFirst(reader, ContentUris.withAppendedId(dirUri, id), projection));
    }

    protected <T> T loadFirst(
            Function<Cursor, T> reader,
            Uri uri, String[] projection
    ) {
        return getLoader().loadFirst(reader, uri, projection);
    }

    protected <T> T loadFirst(
            Function<Cursor, T> reader,
            Uri uri, String[] projection, String selection, String[] selectionArgs, String order
    ) {
        return getLoader().loadFirst(reader, uri, projection, selection, selectionArgs, order);
    }

    protected <T> List<T> loadList(
            Function<Cursor, T> reader,
            Uri uri, String[] projection, String selection, String[] selectionArgs, String order
    ) {
        return getLoader().loadList(reader, uri, projection, selection, selectionArgs, order);
    }

    protected <T> List<T> loadList(
            Function<Cursor, T> reader,
            Uri uri, String[] projection
    ) {
        return getLoader().loadList(reader, uri, projection);
    }

    protected <K, T> Multimap<K, T> loadMultimap(
            Function<Cursor, K> keyReader,
            Function<Cursor, T> valueReader,
            Uri uri, String[] projection, String selection, String[] selectionArgs, String order
    ) {
        return getLoader().loadMultimap(keyReader, valueReader, uri, projection, selection, selectionArgs, order);
    }

    protected <K, T> Map<K, T> loadMap(
            Function<Cursor, K> keyReader,
            Function<Cursor, T> valueReader,
            Uri uri, String[] projection, String selection, String[] selectionArgs, String order
    ) {
        return getLoader().loadMap(keyReader, valueReader, uri, projection, selection, selectionArgs, order);
    }

    /**
     * Like ContentResolver#query, but observes the Uri.
     *
     * <b>Important: </b> you need to manage the cursor yourself - i.e. do not forget to close it!
     */
    protected final Cursor queryUnmanaged(Uri uri, String[] projection,
                                       String selection, String[] selectionArgs, String sortOrder) {
        return getLoader().queryUnmanaged(uri, projection, selection, selectionArgs, sortOrder);
    }

    protected int count(Uri uri) {
        return getLoader().count(uri);
    }

}
