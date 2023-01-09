package de.kalass.android.common.provider;

import android.net.Uri;

import java.util.List;

import edu.mit.mobile.android.content.ContentItem;
import edu.mit.mobile.android.content.column.DBColumn;
import edu.mit.mobile.android.content.column.DatetimeColumn;

/**
 * Created by klas on 22.12.13.
 */
public interface CRUDContentItem extends ContentItem {

    /**
     * Convenience shortcut to adhere to naming standards
     */
    String COLUMN_NAME_ID = _ID;

    /**
     * Creation Date in milliseconds
     */
    @DBColumn(type = LongColumn.class)
    String COLUMN_NAME_CREATED_AT = "created_at";

    /**
     * Modification Date in milliseconds
     */
    @DBColumn(type = LongColumn.class, defaultValue = DatetimeColumn.NOW_IN_MILLISECONDS)
    String COLUMN_NAME_MODIFIED_AT = "modified_at";

}
