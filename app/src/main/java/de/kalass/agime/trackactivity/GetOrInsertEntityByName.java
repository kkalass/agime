package de.kalass.agime.trackactivity;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import com.google.common.base.Function;

import java.util.ArrayList;

import de.kalass.android.common.activity.ContentResolverUtil;
import de.kalass.android.common.provider.CRUDContentItem;
import de.kalass.android.common.simpleloader.ContentProviderOperationUtil;
import de.kalass.android.common.simpleloader.CursorFkt;
import de.kalass.android.common.simpleloader.CursorUtil;
import de.kalass.android.common.simpleloader.ValueOrReference;

/**
* Created by klas on 21.01.14.
*/
public abstract class GetOrInsertEntityByName {

    private static final String[] PROJECTION = new String[]{BaseColumns._ID};
    private static final int IDX_ID = CursorUtil.getIndex(PROJECTION, BaseColumns._ID);

    private static final Function<Cursor, Long> ID_READER = CursorFkt.newLongGetter(IDX_ID);
    private final Context _context;
    private final Uri _dirUri;
    private final String _columnNameName;

    public GetOrInsertEntityByName(Context context, Uri dirUri, String columnNameName) {
        _context = context;
        _dirUri = dirUri;
        _columnNameName = columnNameName;
    }

    protected final String getColumnNameName() {
        return _columnNameName;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        final String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }

    /**
     * Find an existing entity based on its name.
     *
     * Note: If you change this method, you will very likely need to adjust findIdByNameIsEmptyAssert.
     */
    protected Long findIdByName(String name) {
        return ContentResolverUtil.loadFirstFromContentResolver(
                _context,
                ID_READER,
                _dirUri,
                PROJECTION,
                getSelectionString(),
                getSelectionArgs(name),
                null);
    }

    /**
     * The Assertion, that the result of #findIdByName is still null.
     *
     * Note: If you change this method, you will very likely need to adjust findIdByName.
     */
    protected ContentProviderOperation findIdByNameIsEmptyAssert(String name) {
        return ContentProviderOperation
                .newAssertQuery(_dirUri)
                .withSelection(getSelectionString(), getSelectionArgs(name))
                .withExpectedCount(0)
                .build();
    }

    protected ContentProviderOperation.Builder appendValues(ContentProviderOperation.Builder builder) {
        return builder;
    }

    public ValueOrReference addInsertOperation(ArrayList<ContentProviderOperation> ops,
                                               int additionalOpsOffset,
                                               long now,
                                               Long id,
                                               String pname) {
        if (id != null) {
            // category choosen
            return ValueOrReference.ofValue(id);
        }

        String name = trimToNull(pname);
        if (name == null) {
            // both id and name not set - nothing choosen
            return ValueOrReference.ofValue(null);
        }

        id = findIdByName(name);
        if (id != null) {
            // a category of the specified name exists already, no need to create a new one
            return ValueOrReference.ofValue(id);
        }

        // need to append an insert Operation - but first ensure that the item is not inserted in the mean time
        ops.add(findIdByNameIsEmptyAssert(name));
        final ContentProviderOperation.Builder builder = ContentProviderOperation
                .newInsert(_dirUri)
                .withValue(_columnNameName, name)
                .withValue(CRUDContentItem.COLUMN_NAME_MODIFIED_AT, now)
                .withValue(CRUDContentItem.COLUMN_NAME_CREATED_AT, now);

        appendValues(builder);

        return ContentProviderOperationUtil.add(ops, additionalOpsOffset, builder.build());
    }


    protected String getSelectionString() {
        return _columnNameName + " = ?";
    }

    protected String[] getSelectionArgs(String name) {
        return new String[] {name};
    }
}
