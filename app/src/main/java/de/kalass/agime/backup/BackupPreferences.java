package de.kalass.agime.backup;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.google.android.gms.drive.DriveId;

import java.io.File;
import java.util.UUID;

import de.kalass.agime.Consts;
import de.kalass.android.common.util.StringUtil;

import static de.kalass.agime.settings.PreferencesBase.*;
/**
 * Created by klas on 22.11.13.
 */
public class BackupPreferences {

    public static final String KEY_PREF_BACKUP_LOCAL_FS = "pref_backup_automatically";
    public static final String KEY_PREF_BACKUP_LOCAL_FS_LINKED = KEY_PREF_BACKUP_LOCAL_FS + "linked";
    public static final String KEY_PREF_BACKUP_DROPBOX = "pref_backup_to_dropbox";
    public static final String KEY_PREF_BACKUP_DROPBOX_LINKED = KEY_PREF_BACKUP_DROPBOX + "_linked";
    public static final String KEY_PREF_BACKUP_DRIVE = "pref_backup_to_drive";
    public static final String KEY_PREF_BACKUP_DRIVE_FOLDER_ID = "pref_backup_drive_folder_id";
    public static final String KEY_PREF_BACKUP_DRIVE_LINKED = KEY_PREF_BACKUP_DRIVE + "_linked";

    public static final String KEY_PREF_BACKUP_NUM_FILES = "pref_backup_num_files";

    private static final String KEY_PREF_BACKUP_UUID = "pref_backup_uuid";
    private static final String LOG_TAG = "BackupPreferences";

    public static boolean isDailyBackup(Context context) {
        return BackupPreferences.isBackupToDrive(context)
                || BackupPreferences.isBackupToLocalFS(context)
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

    public static void setBackupToDriveLinked(Context context, boolean b) {
        setBoolean(context, KEY_PREF_BACKUP_DRIVE_LINKED, b);
    }

    public static boolean isBackupToDrive(Context context) {
        return Consts.INCLUDE_GOOGLE_DRIVE && getBoolean(context, KEY_PREF_BACKUP_DRIVE, false);
    }
    public static boolean isBackupToDriveLinked(Context context) {
        return Consts.INCLUDE_GOOGLE_DRIVE && getBoolean(context, KEY_PREF_BACKUP_DRIVE_LINKED, false);
    }


    public static DriveId getBackupToDriveFolderId(Context context) {
        final String s = getString(context, KEY_PREF_BACKUP_DRIVE_FOLDER_ID, null);
        if (StringUtil.isTrimmedNullOrEmpty(s)) {
            return null;
        }
        try {
            return DriveId.decodeFromString(s);
        } catch (java.lang.IllegalArgumentException e) {
            //setBackupToDriveLinked(context, false);
            Log.w(LOG_TAG, "Failed to decode nonnull drive id, probably because it was an old version?", e);
            return null;
        }
    }

    public static void setBackupToDriveFolderId(Context context, DriveId driveId) {
        final String driveIdString = driveId == null ? null : driveId.encodeToString();
        setString(context, KEY_PREF_BACKUP_DRIVE_FOLDER_ID, driveIdString);
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
     * on multiple devices that are connected to the same google drive storage
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
