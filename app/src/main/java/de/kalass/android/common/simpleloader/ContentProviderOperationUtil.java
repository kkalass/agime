package de.kalass.android.common.simpleloader;

import android.content.ContentProviderOperation;
import android.net.Uri;
import android.provider.BaseColumns;

import java.util.ArrayList;

/**
 * Created by klas on 14.01.14.
 */
public class ContentProviderOperationUtil {

    public static ValueOrReference add(ArrayList<ContentProviderOperation> ops, int additionalReferenceOffset, ContentProviderOperation op) {
        final ValueOrReference ref = ValueOrReference.ofReference(additionalReferenceOffset + ops.size());// size is index of next operation
        ops.add(op);
        return ref;
    }

    public static ValueOrReference add(ArrayList<ContentProviderOperation> ops, ContentProviderOperation op) {
        return add(ops, 0, op);
    }

    public static ContentProviderOperation.Builder newItemUpdate(Uri dirUri, ValueOrReference identifier) {
        final ContentProviderOperation.Builder updateb = ContentProviderOperation.newUpdate(dirUri);
        String id = identifier.isReference() ? null : Long.toString(identifier.getValue());
        updateb.withSelection(BaseColumns._ID + " = ?", new String[] {id});
        if (identifier.isReference()) {
            updateb.withSelectionBackReference(0, identifier.getReference());
        }
        return updateb;
    }
}
