package de.kalass.agime.activitytype;

import android.content.ContentProviderOperation;
import android.content.Context;

import java.util.List;

import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.activity.BaseCRUDDBUtil;

/**
 * Created by klas on 18.02.14.
 */
public class ActivityTypeEditorDBUtil {
    public static void delete(
            Context context, BaseCRUDDBUtil.DeletionCallback callback,  List<Long> activityTypeIds
    ) {

        final ContentProviderOperation removeReferenceOperation =
                BaseCRUDDBUtil.newUpdateToNullOperation(
                        MCContract.Activity.CONTENT_URI,
                        MCContract.Activity.COLUMN_NAME_ACTIVITY_TYPE_ID,
                        activityTypeIds
                );

        final ContentProviderOperation deleteAllOperation =
                BaseCRUDDBUtil.newDeleteAllOperation(MCContract.ActivityType.CONTENT_URI, activityTypeIds);

        BaseCRUDDBUtil.performDeletionAsync(
                context, callback,
                MCContract.CONTENT_AUTHORITY,
                de.kalass.agime.R.plurals.action_delete_title,
                de.kalass.agime.R.string.action_delete_message,
                activityTypeIds.size(),
                removeReferenceOperation, deleteAllOperation
        );
    }
}
