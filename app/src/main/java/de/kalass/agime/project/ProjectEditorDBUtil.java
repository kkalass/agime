package de.kalass.agime.project;

import android.content.ContentProviderOperation;
import android.content.Context;

import com.google.common.base.Joiner;

import java.util.List;

import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.activity.BaseCRUDDBUtil;

/**
 * Created by klas on 18.02.14.
 */
public class ProjectEditorDBUtil {

    public static void delete(
            Context context, BaseCRUDDBUtil.DeletionCallback callback,  List<Long> entityIds
    ) {

        final ContentProviderOperation removeCustomFieldTypeReferences =
                BaseCRUDDBUtil.newDeleteAllOperation(
                        MCContract.ProjectCustomFieldType.CONTENT_URI,
                        MCContract.ProjectCustomFieldType.COLUMN_NAME_PROJECT_ID,
                        entityIds);

        final ContentProviderOperation removeReferenceOperation =
                BaseCRUDDBUtil.newUpdateToNullOperation(
                        MCContract.Activity.CONTENT_URI,
                        MCContract.Activity.COLUMN_NAME_PROJECT_ID,
                        entityIds
                );

        final ContentProviderOperation deleteAllOperation =
                BaseCRUDDBUtil.newDeleteAllOperation(MCContract.Project.CONTENT_URI, entityIds);

        BaseCRUDDBUtil.performDeletionAsync(
                context, callback,
                MCContract.CONTENT_AUTHORITY,
                de.kalass.agime.R.plurals.action_delete_title,
                de.kalass.agime.R.string.action_delete_message,
                entityIds.size(),
                removeReferenceOperation, removeCustomFieldTypeReferences, deleteAllOperation
        );
    }
}
