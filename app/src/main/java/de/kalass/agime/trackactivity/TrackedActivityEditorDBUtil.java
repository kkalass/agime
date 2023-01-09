package de.kalass.agime.trackactivity;

import android.content.ContentProviderOperation;
import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import de.kalass.agime.R;
import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.activity.BaseCRUDDBUtil;

/**
 * Created by klas on 18.02.14.
 */
public class TrackedActivityEditorDBUtil {
    public static void delete(
            Context context, BaseCRUDDBUtil.DeletionCallback callback,  List<Long> trackedActivityIds
    ) {
        final int numActivities = trackedActivityIds.size();
        BaseCRUDDBUtil.performDeletionAsync(
                context, callback, MCContract.CONTENT_AUTHORITY,
                context.getResources().getQuantityString(R.plurals.track_activity_delete_title, numActivities, numActivities),
                context.getString(R.string.track_activity_delete_message),
                BaseCRUDDBUtil.newDeleteAllOperation(
                        MCContract.ActivityCustomFieldValue.CONTENT_URI,
                        MCContract.ActivityCustomFieldValue.COLUMN_NAME_TRACKED_ACTIVITY_ID,
                        trackedActivityIds
                ),
                BaseCRUDDBUtil.newDeleteAllOperation(
                        MCContract.Activity.CONTENT_URI,
                        trackedActivityIds
                )
                );

    }
}
