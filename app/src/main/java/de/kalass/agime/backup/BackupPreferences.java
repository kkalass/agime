package de.kalass.agime.backup;

import static de.kalass.agime.settings.PreferencesBase.getBoolean;
import static de.kalass.agime.settings.PreferencesBase.getString;
import static de.kalass.agime.settings.PreferencesBase.setBoolean;
import static de.kalass.agime.settings.PreferencesBase.setString;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.UUID;

import de.kalass.agime.Consts;
import de.kalass.android.common.util.StringUtil;
/**
 * Created by klas on 22.11.13.
 */
public class BackupPreferences {

    public static final String KEY_PREF_BACKUP_LOCAL_FS = "pref_backup_automatically";
    public static final String KEY_PREF_BACKUP_LOCAL_FS_LINKED = KEY_PREF_BACKUP_LOCAL_FS + "linked";
    public static final String KEY_PREF_BACKUP_DROPBOX = "pref_backup_to_dropbox";
    public static final String KEY_PREF_BACKUP_DROPBOX_LINKED = KEY_PREF_BACKUP_DROPBOX + "_linked";

    public static final String KEY_PREF_BACKUP_NUM_FILES = "pref_backup_num_files";

    private static final String KEY_PREF_BACKUP_UUID = "pref_backup_uuid";
    private static final String LOG_TAG = "BackupPreferences";

    public static boolean isDailyBackup(Context context) {
        return  BackupPreferences.isBackupToLocalFS(context)
                || BackupPreferences.isBackupToDropbox(context);
    }

    public static boolean isBackupToLocalFS(Context context) {
        return getBoolean(context, KEY_PREF_BACKUP_LOCAL_FS, true);
    }

    public static boolean isBackupToLocalFSLinked(Context context) {
        return getBoolean(context, KEY_PREF_BACKUP_LOCAL_FS_LINKED, true);
    }

    public static void setBackupToLocalFSLinked(Context context, boolean value) {
        setBoolean(context, KEY_PREF_BACKUP_LOCAL_FS_LINKED, value);
    }

    public static boolean isBackupToDropbox(Context context) {
        return Consts.INCLUDE_DROPBOX && getBoolean(context, KEY_PREF_BACKUP_DROPBOX, false);
    }

    public static boolean isBackupToDropboxLinked(Context context) {
        return Consts.INCLUDE_DROPBOX && getBoolean(context, KEY_PREF_BACKUP_DROPBOX_LINKED, false);
    }

    public static void setBackupToDropboxLinked(Context context, boolean b) {
        setBoolean(context, KEY_PREF_BACKUP_DROPBOX_LINKED, b);
    }



    public static int getBackupNumFiles(Context context) {
        int defaultNum = 50;
        final String s = getString(context, KEY_PREF_BACKUP_NUM_FILES, Integer.toString(defaultNum));
        try {
            return Integer.parseInt(s, 10);
        } catch (IllegalArgumentException e) {
            Log.w(LOG_TAG, "Failed to convert stored value for number of backup files, will use default value", e);
            return defaultNum;
        }
    }

    /**
     * Unique UUID to identify this installation, used as part of the filename.
     *
     * This UUID is needed to avoid dataloss if a user  should install Agime
     * on multiple devices that are connected to the same storage
     */
    public static String getBackupUUID(Context context) {
        final String uuidString = getString(context, KEY_PREF_BACKUP_UUID, null);
        if (!StringUtil.isTrimmedNullOrEmpty(uuidString)) {
            return uuidString;
        }
        String uuid = UUID.randomUUID().toString();
        setString(context, KEY_PREF_BACKUP_UUID, uuid);
        return uuid;
    }

    public static File getExternalStorageBackupDir() {
        return Environment.getExternalStoragePublicDirectory("Agime/Backup");
    }

}
