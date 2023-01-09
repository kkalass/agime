package de.kalass.agime.category;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.net.Uri;

import com.google.common.base.Joiner;

import java.util.List;

import de.kalass.agime.provider.MCContract;
import de.kalass.agime.provider.MCProvider;
import de.kalass.android.common.activity.BaseCRUDDBUtil;

/**
 * Created by klas on 18.02.14.
 */
public class CategoryEditorDBUtil {
    public static void delete(
            Context context, BaseCRUDDBUtil.DeletionCallback callback,  List<Long> entityIds
    ) {
            final ContentProviderOperation removeReferenceOperation =
                    BaseCRUDDBUtil.newUpdateToNullOperation(
                            MCContract.ActivityType.CONTENT_URI,
                            MCContract.ActivityType.COLUMN_NAME_ACTIVITY_CATEGORY_ID,
                            entityIds
                    );

            ContentProviderOperation operation =
                    BaseCRUDDBUtil.newDeleteAllOperation(MCContract.Category.CONTENT_URI, entityIds);

            BaseCRUDDBUtil.performDeletionAsync(
                    context,
                    callback,
                    MCContract.CONTENT_AUTHORITY,
                    de.kalass.agime.R.plurals.action_delete_title,
                    de.kalass.agime.R.string.action_delete_message,
                    entityIds.size(),
                    removeReferenceOperation, operation
            );
    }
}
