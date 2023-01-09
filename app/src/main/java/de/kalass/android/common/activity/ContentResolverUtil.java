package de.kalass.android.common.activity;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

import java.util.List;
import java.util.Map;

import de.kalass.android.common.provider.CRUDContentItem;
import de.kalass.android.common.simpleloader.CursorUtil;
import de.kalass.android.common.util.StringUtil;

/**
 * Created by klas on 14.01.14.
 */
public class ContentResolverUtil {
    public static void assertSameContentType(Context context, Uri uri, String mimetype) {
        final String actualType = context.getContentResolver().getType(uri);
        if (!mimetype.equals(actualType)) {
            throw new IllegalArgumentException("Got Uri of type " + actualType +
                    ", but expected " + mimetype + ". URI was " + uri);
        }
    }
    public static <T> T loadFirstFromContentResolver(
            Context context,
            Function<Cursor, T> reader,
            Uri uri, String[] projection, String selection, String[] selectionArgs, String order
    ) {
        return Iterables.getFirst(loadFromContentResolver(context, reader, uri, projection, selection, selectionArgs, order), null);
    }

    public static <T> List<T> loadFromContentResolver(
            Context context,
            Function<Cursor, T> reader,
            Uri uri, String[] projection, String selection, String[] selectionArgs, String order
    ) {
        return loadFromContentResolver(context.getContentResolver(), reader, uri, projection, selection, selectionArgs, order);
    }

    public static <T> List<T> loadFromContentResolver(
            ContentResolver contentResolver,
            Function<Cursor, T> reader,
            Uri uri, String[] projection, String selection, String[] selectionArgs, String order
    ) {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(
                    uri, projection, selection, selectionArgs, order);
            List<T> result = CursorUtil.readList(cursor, reader);
            if (cursor != null) {
                cursor.close();
            }
            return result;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    public static <K, T> Multimap<K, T> loadIndexedFromContentResolver(
            Context context,
            Function<Cursor, K> keyReader,
            Function<Cursor, T> valueReader,
            Uri uri, String[] projection, String selection, String[] selectionArgs, String order
    ) {
        return loadIndexedFromContentResolver(
                context.getContentResolver(), keyReader, valueReader,
                uri, projection, selection, selectionArgs, order
        );
    }

    public static <K, T> Multimap<K, T> loadIndexedFromContentResolver(
            ContentResolver contentResolver,
            Function<Cursor, K> keyReader,
            Function<Cursor, T> valueReader,
            Uri uri, String[] projection, String selection, String[] selectionArgs, String order
    ) {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(
                    uri, projection, selection, selectionArgs, order);
            final Multimap<K, T> result = CursorUtil.index(cursor, keyReader, valueReader);
            if (cursor != null) {
                cursor.close();
            }
            return result;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    public static <K, T> Map<K, T> loadMap(
            ContentResolver contentResolver,
            Function<Cursor, K> keyReader,
            Function<Cursor, T> valueReader,
            Uri uri, String[] projection, String selection, String[] selectionArgs, String order
    ) {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(
                    uri, projection, selection, selectionArgs, order);
            final Map<K, T> result = CursorUtil.uniqueIndex(cursor, keyReader, valueReader);
            if (cursor != null) {
                cursor.close();
            }
            return result;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    public static int count(ContentResolver contentResolver, Uri uri) {
        Cursor cursor = contentResolver.query(
                uri,
                new String[]{BaseColumns._ID},
                null, null, null);
        if (cursor != null) {
            try {
                if (cursor.isBeforeFirst() && cursor.moveToNext()) {
                    return cursor.getCount();
                }
            } finally {
                cursor.close();
            }
        }
        return 0;
    }


    public static Cursor queryByName(ContentResolver resolver,
                                     CharSequence nameConstraint,
                                     Uri uri,
                                     String columnNameName,
                                     int columnIndexName,

                                     Function<Cursor, Long> idReader,
                                     String[] projection
    ) {
        return queryByName(resolver,
                nameConstraint,
                uri,
                columnNameName + " like ?",
                new String[] {"%" + nameConstraint + "%"},
                null, null,
                columnNameName + " asc",
                columnIndexName,
                CRUDContentItem.COLUMN_NAME_ID,
                idReader,
                projection);
    }

    // FIXME: I suspect this to be the source of the unreleased cursors
    public static Cursor queryByName(ContentResolver resolver,
                                     CharSequence nameConstraint,
                                     Uri uri,
                                     String selection,
                                     String[] selectionArgs,
                                     String selectionAll,
                                     String[] selectionArgsAll,
                                     String defaultOrdering,
                                     int columnIndexName,
                                     String columnNameId,
                                     Function<Cursor, Long> idReader,
                                     String[] projection
    ) {
        Cursor c = resolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                defaultOrdering
        );

        // If it is a precise match, load all and sort the matching item to the top:
        // When editing people normally expect all options to be shown
        if (!StringUtil.isTrimmedNullOrEmpty(nameConstraint) && c != null && c.getCount() > 0) {
            c.moveToFirst();
            final String name = c.getString(columnIndexName);
            if (nameConstraint.toString().equals(name)) {
                final List<Long> originallyMatchedIds = CursorUtil.readList(c, idReader);
                c.close();
                return resolver.query(
                        uri,
                        projection,
                        selectionAll,
                        selectionArgsAll,
                        columnNameId + " in (" + Joiner.on(',').join(originallyMatchedIds) + ") desc, " +
                                defaultOrdering
                );
            }
        }
        return c;
    }
}
