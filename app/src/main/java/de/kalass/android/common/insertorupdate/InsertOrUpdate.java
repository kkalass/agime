package de.kalass.android.common.insertorupdate;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import com.google.common.base.Preconditions;

import de.kalass.android.common.provider.CRUDContentItem;
import de.kalass.android.common.simpleloader.ValueOrReference;

/**
 * Performs an insert or update of a tracked activity, automatically creating project, category
 * and activity type entries (as well as custom field type entries) if needed.
 * The new entry will always have precendence over existing entries and will adjust them if applicable.
 *
 * Created by klas on 21.01.14.
 */
public abstract class InsertOrUpdate<I, R> {

    public static final String LOG_TAG = "InsertOrUpdate";
    private final Context _context;
    private final String _contentAuthority;

    public InsertOrUpdate(Context context, String contentAuthority) {
        _context = context.getApplicationContext(); // will be passed to different thread, use application context
        _contentAuthority = contentAuthority;
    }

    protected abstract Operations<R> createOperations(
            boolean isInsert, Long id, I input, long now
    );

    public Context getContext() {
        return _context;
    }

    protected abstract Long getId(I input);

    public final InsertOrUpdateResult<R> execute(I input) {
        Long id = getId(input);
        // Finally: apply all those operations in a single batch
        try {
            final boolean isInsert = id == null;
            final Operations<R> operations = createOperations(isInsert, id, input, System.currentTimeMillis());

            final ValueOrReference valueOrReference = isInsert ? operations.getMainOperationReference() : ValueOrReference.ofValueNonnull(id);

            final ContentProviderResult[] contentProviderResults =
                    _context.getContentResolver().applyBatch(_contentAuthority, operations.getOps());

            Long resultId = getResultId(contentProviderResults, valueOrReference);

            return InsertOrUpdateResult.forSuccess(resultId, isInsert, operations.createResult(resultId, contentProviderResults));

        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Failed to save or update", e);
            return InsertOrUpdateResult.forError(e);
        } catch (RuntimeException e) {
            Log.e(LOG_TAG, "Failed to save or update", e);
            return InsertOrUpdateResult.forError(e);
        } catch (OperationApplicationException e) {
            Log.e(LOG_TAG, "Failed to save or update", e);
            return InsertOrUpdateResult.forError(e);
        }
    }

    private Long getResultId(ContentProviderResult[] results, ValueOrReference trackedActivityId) {
        if (!trackedActivityId.isReference()) {
            return Preconditions.checkNotNull(trackedActivityId.getValue(), "TrackedActivityId was neither set nor a reference");
        }
        final ContentProviderResult mainResult = results[trackedActivityId.getReference()];
        Uri uri = Preconditions.checkNotNull(mainResult.uri, "Main insert operation expected to return URI");
        return ContentUris.parseId(uri);
    }


    protected ContentProviderOperation.Builder createInsertOrUpdateBuilder(
            Uri dirUri,
            Long id,
            long now
    ) {
        boolean isInsert = id == null;
        ContentProviderOperation.Builder builder =
                isInsert
                        ? ContentProviderOperation.newInsert(dirUri)
                        : ContentProviderOperation.newUpdate(ContentUris.withAppendedId(dirUri, id));

        if (isInsert) {
            builder.withValue(CRUDContentItem.COLUMN_NAME_CREATED_AT, now);
        } else {
            // ensure that the correct number of rows is updated
            builder.withExpectedCount(1);
        }
        builder.withValue(CRUDContentItem.COLUMN_NAME_MODIFIED_AT, now);

        return builder;
    }


}
