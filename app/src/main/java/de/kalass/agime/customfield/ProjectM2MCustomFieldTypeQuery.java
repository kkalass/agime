package de.kalass.agime.customfield;

import android.database.Cursor;
import android.net.Uri;

import com.google.common.base.Function;

import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.simpleloader.CursorFkt;

import static de.kalass.android.common.simpleloader.CursorUtil.getIndex;

/**
 * Queries the Project Ids that are enabled for a given custom field type
 * Created by klas on 14.01.14.
 */
public final class ProjectM2MCustomFieldTypeQuery {
    public static final Uri CONTENT_URI = MCContract.ProjectCustomFieldType.CONTENT_URI;
    public static final String COLUMN_NAME_TYPE_ID = MCContract.ProjectCustomFieldType.COLUMN_NAME_CUSTOM_FIELD_TYPE_ID;
    public static final String COLUMN_NAME_PROJECT_ID = MCContract.ProjectCustomFieldType.COLUMN_NAME_PROJECT_ID;


    public static final String[] PROJECTION = new String[] {
            COLUMN_NAME_PROJECT_ID
    };
    public static final String SELECTION = COLUMN_NAME_TYPE_ID + " =? ";
    public static final String[] args(long typeId) {
        return new String[] {Long.toString(typeId)};
    }

    public static final int IDX_PROJECT_ID = getIndex(PROJECTION, COLUMN_NAME_PROJECT_ID);

    public static final Function<Cursor, Long> PROJECT_ID_READER = CursorFkt.newLongGetter(IDX_PROJECT_ID);

}
