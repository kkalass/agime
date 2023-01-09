package de.kalass.agime.backup;

import android.os.Build;

/**
 * Created by klas on 14.02.14.
 */
public class BackupHelperUtil {

    public static boolean canSkip(BackupData.PersonalBackup latestStored, BackupData.PersonalBackup currentData) {
        if (latestStored == null) {
            return false;
        }
        if (latestStored.hasLatestInsertOrUpdateMillis() && currentData.hasLatestInsertOrUpdateMillis()) {
            if (latestStored.getLatestInsertOrUpdateMillis() == currentData.getLatestInsertOrUpdateMillis()) {
                return true;
            }
        }
        return false;
    }

    public static String getDeviceFolderName() {
        return Build.MODEL.replaceAll("\\W+", " ");
    }
}
