package de.kalass.agime.customfield;

import android.database.Cursor;
import android.net.Uri;

import com.google.common.base.Function;

import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.model.IViewModel;

import static de.kalass.android.common.simpleloader.CursorUtil.getIndex;

/**
* Created by klas on 23.12.13.
*/
public final class CustomFieldTypeDAO {
    public static final Uri CONTENT_URI = MCContract.CustomFieldType.CONTENT_URI;
    public static final String COLUMN_NAME_NAME = MCContract.CustomFieldType.COLUMN_NAME_NAME;
    public static final String[] PROJECTION = new String[] {
            MCContract.CustomFieldType._ID,
            COLUMN_NAME_NAME
    };
    public static final int IDX_ID = getIndex(PROJECTION, MCContract.CustomFieldType._ID);
    public static final int IDX_NAME = getIndex(PROJECTION, COLUMN_NAME_NAME);

    public static final Function<Cursor, Data> READ_DATA = new Function<Cursor, Data>() {
        @Override
        public Data apply(Cursor cursor) {
            return cursor == null ? null : new Data(cursor);
        }
    };

    public static final class Data implements IViewModel {
        final long id;
        final String name;

        Data(Cursor cursor) {
            id = cursor.getLong(IDX_ID);
            name = cursor.getString(IDX_NAME);
        }

        @Override
        public long getId() {
            return id;
        }

    }
}
