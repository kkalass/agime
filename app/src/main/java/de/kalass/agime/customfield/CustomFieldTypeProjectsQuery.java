package de.kalass.agime.customfield;

import android.database.Cursor;
import android.net.Uri;

import com.google.common.base.Function;

import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.simpleloader.CursorFkt;

import static de.kalass.android.common.simpleloader.CursorUtil.getIndex;

/**
* Created by klas on 15.01.14.
*/
public final class CustomFieldTypeProjectsQuery {
    public static final Uri CONTENT_URI = MCContract.ProjectCustomFieldType.CONTENT_URI;
    public static final String COLUMN_NAME_TYPE_ID = MCContract.ProjectCustomFieldType.COLUMN_NAME_CUSTOM_FIELD_TYPE_ID;
    public static final String COLUMN_NAME_PROJECT_ID = MCContract.ProjectCustomFieldType.COLUMN_NAME_PROJECT_ID;


    public static final String[] PROJECTION = new String[] {
            COLUMN_NAME_PROJECT_ID,
            COLUMN_NAME_TYPE_ID,
    };

    public static final int IDX_PROJECT_ID = getIndex(PROJECTION, COLUMN_NAME_PROJECT_ID);
    public static final int IDX_TYPE_ID = getIndex(PROJECTION, COLUMN_NAME_TYPE_ID);

    public static final Function<Cursor, Long> READ_TYPE_ID = CursorFkt.newLongGetter(IDX_TYPE_ID);
    public static final Function<Cursor, Long> READ_PROJECT_ID = CursorFkt.newLongGetter(IDX_PROJECT_ID);

}
