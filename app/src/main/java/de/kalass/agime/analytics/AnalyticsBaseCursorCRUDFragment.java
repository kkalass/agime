package de.kalass.agime.analytics;

import android.database.Cursor;

import com.google.common.base.Function;

import de.kalass.android.common.activity.BaseCursorCRUDFragment;

/**
 * Created by klas on 22.01.14.
 */
public abstract class AnalyticsBaseCursorCRUDFragment<C, D> extends BaseCursorCRUDFragment<C, D> {

    public AnalyticsBaseCursorCRUDFragment(
            int layout, String contentTypeDir, String contentTypeItem, String[] projection,
            Function<Cursor, D> reader
    ) {
        super(layout, contentTypeDir, contentTypeItem, projection, reader);
    }

    protected String getEntityTypeName() {
        return getContentTypeItem();
    }


}
