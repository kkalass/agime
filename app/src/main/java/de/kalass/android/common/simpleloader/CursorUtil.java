package de.kalass.android.common.simpleloader;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by klas on 01.11.13.
 */
public final class CursorUtil {

    public static int MAX_RESULTS_UNLIMITED = -1;

    private CursorUtil() {

    }

    public static int getIndex(String[] projection, String candiate) {
        for (int i = 0; i < projection.length; i++) {
            if (candiate.trim().equalsIgnoreCase(projection[i].trim())) {
                return i;
            }
        }
        throw new IllegalArgumentException(
                "Candidate String " + candiate
                        + " was not found in array " + Arrays.toString(projection));
    }



    public static <T> Set<T> readSet(
            Cursor cursor,
            Function<Cursor, T> function,
            Predicate<T> predicate
    ) {
        return (Set<T>) read(cursor, MAX_RESULTS_UNLIMITED, function, predicate, ImmutableSet.<T>builder());
    }

    public static <T> List<T> readList(
            Cursor cursor,
            Function<Cursor, T> function,
            Predicate<T> predicate
    ) {
        return (List<T>) read(cursor, MAX_RESULTS_UNLIMITED, function, predicate, ImmutableList.<T>builder());
    }

    public static <T> Set<T> readSet(
            Cursor cursor,
            Function<Cursor, T> function
    ) {
        return (Set<T>) read(cursor, MAX_RESULTS_UNLIMITED, function, Predicates.<T>alwaysTrue(), ImmutableSet.<T>builder());
    }

    public static <T> Set<T> readSetNoNulls(
            Cursor cursor,
            int limit,
            Function<Cursor, T> function
    ) {
        return (Set<T>) read(cursor, limit, function, Predicates.<T>notNull(), ImmutableSet.<T>builder());
    }

    public static <T> List<T> readList(
            Cursor cursor,
            int maxResults,
            Function<Cursor, T> function
    ) {
        return (List<T>) read(cursor, maxResults, function, Predicates.<T>alwaysTrue(), ImmutableList.<T>builder());
    }

    public static <T> List<T> readList(
            Cursor cursor,
            Function<Cursor, T> function
    ) {
        return (List<T>) read(cursor, MAX_RESULTS_UNLIMITED, function, Predicates.<T>alwaysTrue(), ImmutableList.<T>builder());
    }

    public static <K, V> Map<K, V> uniqueIndex(
            Cursor cursor,
            Function<Cursor, K> keyFunction,
            Function<Cursor, V> valueFunction
    ) {
        ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
        if (cursor.moveToFirst()) {
            do {
                K key = keyFunction.apply(cursor);
                V value = valueFunction.apply(cursor);
                builder.put(key, value);
            } while(cursor.moveToNext());
        }
        return builder.build();
    }

    public static <K, V> Multimap<K, V> index(
            Cursor cursor,
            Function<Cursor, K> keyFunction,
            Function<Cursor, V> valueFunction
    ) {
        ImmutableMultimap.Builder<K, V> builder = ImmutableMultimap.builder();
        if (cursor.moveToFirst()) {
            do {
                K key = keyFunction.apply(cursor);
                V value = valueFunction.apply(cursor);
                builder.put(key, value);
            } while(cursor.moveToNext());
        }
        return builder.build();
    }


    private static <T> Iterable<T> read(
            Cursor cursor,
            int maxResults,
            Function<Cursor, T> function,
            Predicate<T> filter,
            ImmutableCollection.Builder<T> builder
    ) {
        if (cursor != null && cursor.moveToFirst()) {
            int c = 0;
            do {
                T value = function.apply(cursor);
                if (filter.apply(value)) {
                    c++;
                    builder.add(value);
                }
            } while (cursor.moveToNext() && (maxResults == MAX_RESULTS_UNLIMITED || c < maxResults));
        }
        return builder.build();
    }

    public static <T> T getPrefetched(Cursor cursor, int columnIdx, Map<Long, T> prefetched) {
        if (cursor.isNull(columnIdx)) {
            return null;
        }
        long projectId = cursor.getLong(columnIdx);
        return Preconditions.checkNotNull(prefetched.get(projectId), "should have been precached");
    }

    public static LocalDate getLocalDate(Cursor cursor, int columnIndex) {
        return cursor.isNull(columnIndex) ? null : LocalDateConverter.deserialize(cursor.getLong(columnIndex));
    }

    public static boolean getBoolean(Cursor cursor, int columnIndex) {
        return !cursor.isNull(columnIndex) && cursor.getInt(columnIndex) != 0;
    }

    public static void putBoolean(ContentValues values, String columnName, boolean checked) {
        values.put(columnName, boolean2int(checked));
    }

    public static int boolean2int(boolean checked) {
        return checked ? 1 : 0;
    }

    public static void putLocalDate(ContentValues result, String columnName, LocalDate localDate) {
        result.put(columnName, LocalDateConverter.serialize(localDate));
    }

    public static void putHourMinute(ContentValues result, String columnName, LocalTime localTime) {
        result.put(columnName, HourMinute.serialize(localTime));
    }

    public static void putWeekdays(ContentValues result, String columnName, Set<Weekdays.Weekday> weekdays) {
        result.put(columnName, Weekdays.serialize(weekdays == null ? ImmutableSet.<Weekdays.Weekday>of() : weekdays));
    }

    public static Long getLong(Cursor cursor, int columnIndex) {
        if (cursor.isNull(columnIndex)) {
            return null;
        }
        return cursor.getLong(columnIndex);
    }

    public static long getLongOrZero(Cursor cursor, int columnIndex) {
        if (cursor.isNull(columnIndex)) {
            return 0;
        }
        return cursor.getLong(columnIndex);
    }

}
