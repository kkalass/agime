package de.kalass.agime.customfield;

import android.content.ContentProviderOperation;
import android.content.Context;

import java.util.List;

import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.activity.BaseCRUDDBUtil;

/**
 * Created by klas on 18.02.14.
 */
public class CustomFieldValueEditorDBUtil {
    public static void delete(
            Context context,
            BaseCRUDDBUtil.DeletionCallback callback,
            List<Long> entityIds
    ) {
        // Delete all references to custom field values
        ContentProviderOperation refDelete = BaseCRUDDBUtil.newDeleteAllOperation(
                MCContract.ActivityCustomFieldValue.CONTENT_URI,
                MCContract.ActivityCustomFieldValue.COLUMN_NAME_CUSTOM_FIELD_VALUE_ID,
                entityIds
        );

        ContentProviderOperation operation = BaseCRUDDBUtil.newDeleteAllOperation(
                MCContract.CustomFieldValue.CONTENT_URI,
                entityIds
        );
        BaseCRUDDBUtil.performDeletionAsync(
                context, callback,
                MCContract.CONTENT_AUTHORITY,
                de.kalass.agime.R.plurals.action_delete_title,
                de.kalass.agime.R.string.action_delete_message,
                entityIds.size(),
                refDelete, operation
        );
    }
}
