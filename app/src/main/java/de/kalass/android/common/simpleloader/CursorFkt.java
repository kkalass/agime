package de.kalass.android.common.simpleloader;

import android.database.Cursor;

import com.google.common.base.Function;

/**
 * Created by klas on 01.11.13.
 */
public final class CursorFkt {
    private static final class GetInt implements Function<Cursor, Integer> {
        private final int _columnIndex;

        public GetInt(int columnIndex) {
            _columnIndex = columnIndex;
        }

        @Override
        public Integer apply(Cursor cursor) {
            if (cursor.isNull(_columnIndex)) {
                return null;
            }
            return cursor.getInt(_columnIndex);
        }
    }

    private static final class GetLong implements Function<Cursor, Long> {
        private final int _columnIndex;

        public GetLong(int columnIndex) {
            _columnIndex = columnIndex;
        }

        @Override
        public Long apply(Cursor cursor) {
            if (cursor.isNull(_columnIndex)) {
                return null;
            }
            return cursor.getLong(_columnIndex);
        }
    }

    private static final class GetString implements Function<Cursor, String> {
        private final int _columnIndex;

        public GetString(int columnIndex) {
            _columnIndex = columnIndex;
        }

        @Override
        public String apply(Cursor cursor) {
            return cursor.getString(_columnIndex);
        }
    }

    private CursorFkt() {

    }


    public static Function<Cursor, Long> newLongGetter(int columnIndex) {
        return new GetLong(columnIndex);
    }

    public static Function<Cursor, Integer> newIntGetter(int columnIndex) {
        return new GetInt(columnIndex);
    }

    public static Function<Cursor, String> newStringGetter(int columnIndex) {
        return new GetString(columnIndex);
    }

}
