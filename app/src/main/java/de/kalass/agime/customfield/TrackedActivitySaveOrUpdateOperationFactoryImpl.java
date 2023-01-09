package de.kalass.agime.customfield;

import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

import de.kalass.agime.provider.MCContract;
import de.kalass.agime.trackactivity.GetOrInsertEntityByName;
import de.kalass.agime.trackactivity.InsertOrUpdateOperationFactory;
import de.kalass.android.common.simpleloader.ValueOrReference;
import de.kalass.android.common.util.StringUtil;

/**
* Created by klas on 21.01.14.
*/
final class TrackedActivitySaveOrUpdateOperationFactoryImpl implements InsertOrUpdateOperationFactory {

    static final class Data {

        final Long selectedValueId;
        final String selectedValue;
        final String originalSelectedValue;
        final Long originalSelectedValueId;
        final Long associationId;
        final long typeId;


        Data(Long selectedValueId,
             String selectedValue,
             String originalSelectedValue,
             Long originalSelectedValueId,
             Long associationId,
             long typeId
        ) {
            this.selectedValueId = selectedValueId;
            this.selectedValue = selectedValue;
            this.originalSelectedValue = originalSelectedValue;
            this.originalSelectedValueId = originalSelectedValueId;
            this.associationId = associationId;
            this.typeId = typeId;
        }
    }

    private final ImmutableList<Data> _data;

    TrackedActivitySaveOrUpdateOperationFactoryImpl(List<Data> data) {
        _data = ImmutableList.copyOf(data);
    }

    @Override
    public ArrayList<ContentProviderOperation> createOperationsInBackground(
            Context context,
            int numPreviousOps,
            ValueOrReference trackedActivityId
    ) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        for (Data data: _data) {
            appendCustomFieldTypeOperations(context, ops, numPreviousOps, data, trackedActivityId);
        }
        return ops;
    }

    private ArrayList<ContentProviderOperation> appendCustomFieldTypeOperations(
            Context context,
            ArrayList<ContentProviderOperation> ops,
            int numPreviousOps,
            Data item,
            ValueOrReference trackedActivityId
    ) {
        final Long newValueId = item.selectedValueId;
        final String newValue = item.selectedValue;
        final boolean hasValue = !StringUtil.isTrimmedNullOrEmpty(newValue);

        if ((Objects.equal(newValue, item.originalSelectedValue) &&
                Objects.equal(newValueId, item.originalSelectedValueId))) {
            // nothing to do
            return ops;
        }
        final boolean isInsert = item.associationId == null;
        final Uri associationUri = isInsert
                ? MCContract.ActivityCustomFieldValue.CONTENT_URI
                : ContentUris.withAppendedId(MCContract.ActivityCustomFieldValue.CONTENT_URI, item.associationId);

        if (!hasValue) {
            if (!isInsert) {
                // delete the association, but not the custom value
                ops.add(ContentProviderOperation.newDelete(associationUri).withExpectedCount(1).build());
            }
            return ops;
        }

        long now = System.currentTimeMillis();
        final ValueOrReference newValueIdOrRef = new GetOrInsertValue(context, item.typeId)
                .addInsertOperation(ops, numPreviousOps, now, newValueId, newValue);

        final ContentProviderOperation.Builder associationBuilder;
        if (isInsert) {
            // new association needed
            associationBuilder = ContentProviderOperation.newInsert(associationUri)
                    .withValue(MCContract.ActivityCustomFieldValue.COLUMN_NAME_CREATED_AT, now);

            trackedActivityId.appendSelf(
                    associationBuilder,
                    MCContract.ActivityCustomFieldValue.COLUMN_NAME_TRACKED_ACTIVITY_ID
            );
        } else {
            final Long taId = Preconditions.checkNotNull(trackedActivityId.getValue());

            // update old association
            associationBuilder = ContentProviderOperation.newUpdate(associationUri)
                    .withExpectedCount(1)
                    .withSelection(
                            MCContract.ActivityCustomFieldValue.COLUMN_NAME_TRACKED_ACTIVITY_ID + " = ?",
                            new String[]{taId.toString()}
                    );
        }

        associationBuilder.withValue(MCContract.ActivityCustomFieldValue.COLUMN_NAME_MODIFIED_AT, now);
        newValueIdOrRef.appendSelf(associationBuilder, MCContract.ActivityCustomFieldValue.COLUMN_NAME_CUSTOM_FIELD_VALUE_ID);
        ops.add(associationBuilder.build());
        return ops;
    }

    private static class GetOrInsertValue extends GetOrInsertEntityByName {
        private final long _typeId;

        public GetOrInsertValue(Context context, long typeId) {
            super(context,
                    MCContract.CustomFieldValue.getDirUriForType(typeId),
                    MCContract.CustomFieldValue.COLUMN_NAME_VALUE);
            _typeId = typeId;
        }

        @Override
        protected String getSelectionString() {
            return getColumnNameName() + " = ? AND " +
                    MCContract.CustomFieldValue.COLUMN_NAME_CUSTOM_FIELD_TYPE_ID + " = ? ";
        }

        @Override
        protected String[] getSelectionArgs(String name) {
            return new String[]{name, Long.toString(_typeId)};
        }

        @Override
        protected ContentProviderOperation.Builder appendValues(ContentProviderOperation.Builder builder) {
            return builder.withValue(MCContract.CustomFieldValue.COLUMN_NAME_CUSTOM_FIELD_TYPE_ID, _typeId);
        }
    }

}
