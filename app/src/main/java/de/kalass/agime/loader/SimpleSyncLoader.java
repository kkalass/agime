package de.kalass.agime.loader;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;
import java.util.Set;

import de.kalass.android.common.activity.ContentResolverUtil;
import de.kalass.android.common.model.IViewModel;
import de.kalass.android.common.simpleloader.CompoundSyncLoader;
import de.kalass.android.common.simpleloader.CursorFkt;
import de.kalass.android.common.simpleloader.CursorUtil;
import de.kalass.android.common.simpleloader.SyncLoader;

/**
 * A Loader that loads TrackedActivityModel instances by combining a query to the trackpoints
 * table with a query to the symptoms table.
 * Created by klas on 22.10.13.
 */
public abstract class SimpleSyncLoader<T extends IViewModel> extends CompoundSyncLoader {

    private final Uri _uri;
    private final String[] _projection;

    public SimpleSyncLoader(
            Context context,
            Uri uri,
            String[] projection,
            SyncLoader... loaders
    ) {
        super(context, loaders);
        _uri = uri;
        _projection = projection;
    }

    public Map<Long, T> loadAsMap(String selection, String[] args, String order) {
        final List<T> list = load(selection, args, order);
        return Maps.uniqueIndex(list, IViewModel.GET_ID);
    }

    public T load(long id) {
        return Iterables.getOnlyElement(load(BaseColumns._ID + " = ? ", new String[]{Long.toString(id)}, null));
    }

    protected List<T> loadByName(CharSequence name, String columnNameName, int columnNameIdx, Function<Cursor, Long> idReader) {
        observe(_uri);
        Cursor r = ContentResolverUtil.queryByName(
                getContentResolver(),
                name,
                _uri,
                columnNameName,
                columnNameIdx,
                idReader,
                _projection
        );
        if (r == null) {
            return ImmutableList.of();
        }
        try {
            return CursorUtil.readList(r, getReaderFunction(r));
        } finally {
            r.close();
        }
    }

    public List<T> load(
            String selection,
            String[] selectionArgs,
            String order
    ) {
        observe(_uri);
        final Cursor cursor = getContentResolver().query(
                _uri,
                _projection,
                selection,
                selectionArgs,
                order
        );
        if (cursor == null) {
            return ImmutableList.of();
        }
        try {
            if (cursor.getCount() == 0) {
                return ImmutableList.of();
            }

            Function<Cursor, T> readerFunction = getReaderFunction(cursor);
            return CursorUtil.readList(cursor, readerFunction);
        } finally {
            cursor.close();
        }

    }

    protected abstract Function<Cursor,T> getReaderFunction(Cursor cursor);


    public Map<Long, T> loadAllFromCursor(Cursor cursor, int fkColumnIndex) {
        final Set<Long> ids = CursorUtil.readSet(
                cursor,
                CursorFkt.newLongGetter(fkColumnIndex),
                Predicates.<Long>notNull()
        );

        final List<T> loaders = load(
                BaseColumns._ID + " in (" + Joiner.on(',').join(ids) + ")",
                null,
                null
        );
        return Maps.uniqueIndex(loaders, IViewModel.GET_ID);
    }


}
