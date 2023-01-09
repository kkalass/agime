package de.kalass.agime.loader;

import android.database.Cursor;
import android.net.Uri;

import com.google.common.base.Function;

import de.kalass.agime.model.CategoryModel;
import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.simpleloader.CursorFkt;

import static de.kalass.android.common.simpleloader.CursorUtil.getIndex;

/**
* Created by klas on 27.01.14.
*/
public final class CategoryModelQuery {
    static final Uri URI = MCContract.Category.CONTENT_URI;

    static final String COLUMN_NAME_ID = MCContract.Category.COLUMN_NAME_ID;
    static final String COLUMN_NAME_NAME = MCContract.Category.COLUMN_NAME_NAME;
    static final String COLUMN_NAME_COLOR_CODE = MCContract.Category.COLUMN_NAME_COLOR_CODE;

    public static final String[] PROJECTION = new String[]{
            COLUMN_NAME_ID,
            COLUMN_NAME_NAME,
            COLUMN_NAME_COLOR_CODE
    };
    static final int COLUMN_IDX_ID = getIndex(PROJECTION, COLUMN_NAME_ID);
    static final int COLUMN_IDX_NAME = getIndex(PROJECTION, COLUMN_NAME_NAME);
    static final int COLUMN_IDX_COLOR_CODE = getIndex(PROJECTION, COLUMN_NAME_COLOR_CODE);

    public static final Function<Cursor, Long> ID_READER = CursorFkt.newLongGetter(COLUMN_IDX_ID);

    public static final Function<Cursor, CategoryModel> READER = new Function<Cursor, CategoryModel> () {
        @Override
        public CategoryModel apply(Cursor cursor) {
            long id = cursor.getLong(CategoryModelQuery.COLUMN_IDX_ID);
            String name = cursor.getString(CategoryModelQuery.COLUMN_IDX_NAME);
            int color = cursor.getInt(CategoryModelQuery.COLUMN_IDX_COLOR_CODE);

            return new CategoryModel(id, name, color);
        }
    };
}
