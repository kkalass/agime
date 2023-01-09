package de.kalass.android.common.provider;

import android.database.Cursor;

import edu.mit.mobile.android.content.column.DBColumnType;

/**
 * Created by klas on 04.03.14.
 */
public class LongColumn extends DBColumnType<Long> {
    @Override
    public String toCreateColumn(final String colName) {
        return toColumnDef(colName, "INTEGER");
    }

    @Override
    public Long get(final Cursor c, final int colNumber) {
        return c.getLong(colNumber);
    }
}
