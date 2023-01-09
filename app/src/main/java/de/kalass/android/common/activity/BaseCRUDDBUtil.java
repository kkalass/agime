package de.kalass.android.common.activity;

import android.app.AlertDialog;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;

import com.google.common.base.Joiner;
import de.kalass.agime.R;
import java.util.List;

import de.kalass.android.common.ApplyBatchTask;
import de.kalass.android.common.provider.CRUDContentItem;


/**
 * Created by klas on 18.02.14.
 */
public class BaseCRUDDBUtil {

    public interface DeletionCallback {
        void onDeletionStart();
        void onDeletionSuccess();
    }

    public static ContentProviderOperation newDeleteAllOperation(Uri uri, String idColumnName, List<Long> entityIds) {
        return ContentProviderOperation.newDelete(uri)
                .withSelection(idColumnName + " in (" + Joiner.on(", ").join(entityIds) + ")", null)
                // Note: we cannot make an assumption about the number of rows to delete
                .build();
    }

    public static ContentProviderOperation newDeleteAllOperation(Uri uri, List<Long> entityIds) {
        return ContentProviderOperation.newDelete(uri)
                .withSelection(CRUDContentItem.COLUMN_NAME_ID + " in (" + Joiner.on(", ").join(entityIds) + ")", null)
                .withExpectedCount(entityIds.size()).build();
    }

    public static ContentProviderOperation newUpdateToNullOperation(Uri uri, String columnName, List<Long> entityIds) {
        return ContentProviderOperation.newUpdate(uri)
                .withSelection(columnName + " in (" + Joiner.on(", ").join(entityIds) + ")", null)
                .withValue(columnName, null)
                .build();
    }

    public static void performDeletionAsync(Context context,
                                            DeletionCallback callback,
                                            String authority,
                                            int title, int message,
                                            int numItems,
                                            final ContentProviderOperation... ops) {
        performDeletionAsync(context, callback, authority,
                context.getResources().getQuantityString(title, numItems, numItems),
                context.getString(message), ops);
    }

    /**
     * Ask the user for confirmation of deletion and perform the operations in a background thread.
     *
     * needs to be called on the ui thread
     */
    public static void performDeletionAsync(
            final Context context,
            final DeletionCallback callback,
            final String authority,
            String title, String message,
            final ContentProviderOperation... ops) {

        new AlertDialog.Builder(context)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // nothing to do
                        dialog.dismiss();
                    }
                })
                .setPositiveButton(R.string.action_delete_confirm_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (callback != null) {
                            callback.onDeletionStart();
                        }
                        new ApplyBatchTask(context, R.string.delete_failed_title, R.string.delete_failed_message) {

                            @Override
                            protected void onSuccess(Result result) {
                                if (callback != null) {
                                    callback.onDeletionSuccess();
                                }
                            }

                        }.execute(ops);
                    }
                })
                .setTitle(title)
                .setMessage(message)
                .show();
    }
}
