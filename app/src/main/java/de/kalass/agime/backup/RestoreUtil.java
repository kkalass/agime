package de.kalass.agime.backup;

import android.content.ContentResolver;
import android.content.Context;

import de.kalass.agime.provider.MCContract;

/**
 * Created by klas on 17.01.14.
 */
public class RestoreUtil {
    static final void deleteAllData(Context context) {
        final ContentResolver contentResolver = context.getContentResolver();
        // First: delete all existing data!
        contentResolver.delete(MCContract.ActivityCustomFieldValue.CONTENT_URI, null, null);
        contentResolver.delete(MCContract.Activity.CONTENT_URI, null, null);
        contentResolver.delete(MCContract.ActivityType.CONTENT_URI, null, null);
        contentResolver.delete(MCContract.Category.CONTENT_URI, null, null);
        contentResolver.delete(MCContract.ProjectCustomFieldType.CONTENT_URI, null, null);
        contentResolver.delete(MCContract.Project.CONTENT_URI, null, null);
        contentResolver.delete(MCContract.CustomFieldValue.CONTENT_URI, null, null);
        contentResolver.delete(MCContract.CustomFieldType.CONTENT_URI, null, null);
        contentResolver.delete(MCContract.RecurringAcquisitionTime.CONTENT_URI, null, null);
    }
}
