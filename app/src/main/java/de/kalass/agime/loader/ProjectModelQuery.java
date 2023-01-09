package de.kalass.agime.loader;

import android.database.Cursor;
import android.net.Uri;

import com.google.common.base.Function;

import de.kalass.agime.model.ProjectModel;
import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.simpleloader.CursorFkt;

import static de.kalass.android.common.simpleloader.CursorUtil.getIndex;

/**
* Created by klas on 20.01.14.
*/
public final class ProjectModelQuery {
    public static final String CONTENT_TYPE_DIR = MCContract.Project.CONTENT_TYPE_DIR;
    public static final Uri URI = MCContract.Project.CONTENT_URI;

    public static final String COLUMN_NAME_ID = MCContract.Project.COLUMN_NAME_ID;
    public static final String COLUMN_NAME_NAME = MCContract.Project.COLUMN_NAME_NAME;
    public static final String COLUMN_NAME_COLOR_CODE = MCContract.Project.COLUMN_NAME_COLOR_CODE;
    public static final String COLUMN_NAME_ACTIVE_UNTIL_MILLIS = MCContract.Project.COLUMN_NAME_ACTIVE_UNTIL_MILLIS;

    public static final String[] PROJECTION = new String[]{
            COLUMN_NAME_ID, COLUMN_NAME_NAME, COLUMN_NAME_COLOR_CODE, COLUMN_NAME_ACTIVE_UNTIL_MILLIS
    };

    public static final int COLUMN_IDX_ID = getIndex(PROJECTION, COLUMN_NAME_ID);
    public static final int COLUMN_IDX_NAME = getIndex(PROJECTION, COLUMN_NAME_NAME);
    public static final int COLUMN_IDX_COLOR = getIndex(PROJECTION, COLUMN_NAME_COLOR_CODE);
    public static final int COLUMN_IDX_ACTIVE_UNTIL_MILLIS = getIndex(PROJECTION, COLUMN_NAME_ACTIVE_UNTIL_MILLIS);

    public static final Function<Cursor, Long> READ_ID = CursorFkt.newLongGetter(COLUMN_IDX_ID);

    public static final Function<Cursor, ProjectModel> READER = new Function<Cursor, ProjectModel> () {
        @Override
        public ProjectModel apply(Cursor cursor) {
            long id = cursor.getLong(COLUMN_IDX_ID);
            String name = cursor.getString(COLUMN_IDX_NAME);
            Integer colorCode = cursor.isNull(COLUMN_IDX_COLOR) ? null : cursor.getInt(COLUMN_IDX_COLOR);
            Long activeUntilMillis = cursor.isNull(COLUMN_IDX_ACTIVE_UNTIL_MILLIS) ? null : cursor.getLong(COLUMN_IDX_ACTIVE_UNTIL_MILLIS);
            return new ProjectModel(id, name, colorCode, activeUntilMillis);
        }
    };
}
