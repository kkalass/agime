package de.kalass.agime.customfield;

import android.database.Cursor;
import android.net.Uri;

import com.google.common.base.Function;

import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.simpleloader.CursorUtil;

/**
* Created by klas on 15.01.14.
*/
public final class CustomFieldTypeModelQuery {
    public static final Uri CONTENT_URI = MCContract.CustomFieldType.CONTENT_URI;
    public static final String COLUMN_NAME_ID = MCContract.CustomFieldType._ID;
    public static final String COLUMN_NAME_NAME = MCContract.CustomFieldType.COLUMN_NAME_NAME;
    public static final String COLUMN_NAME_ANY_PROJECT = MCContract.CustomFieldType.COLUMN_NAME_ANY_PROJECT;
    public static final String[] PROJECTION = new String[] {
            COLUMN_NAME_ID,
            COLUMN_NAME_NAME,
            COLUMN_NAME_ANY_PROJECT
    };
    public static final int IDX_ID = CursorUtil.getIndex(PROJECTION, COLUMN_NAME_ID);
    public static final int IDX_NAME = CursorUtil.getIndex(PROJECTION, COLUMN_NAME_NAME);
    public static final int IDX_ANY_PROJECT = CursorUtil.getIndex(PROJECTION, COLUMN_NAME_ANY_PROJECT);

    public static final Function<Cursor, CustomFieldTypeModel> READ = new Function<Cursor, CustomFieldTypeModel>() {
        @Override
        public CustomFieldTypeModel apply(Cursor cursor) {
            long id = cursor.getLong(CustomFieldTypeModelQuery.IDX_ID);
            String name = cursor.getString(CustomFieldTypeModelQuery.IDX_NAME);
            boolean anyProject = CursorUtil.getBoolean(cursor, CustomFieldTypeModelQuery.IDX_ANY_PROJECT);
            return new CustomFieldTypeModel(id, name, anyProject);
        }
    };
}
