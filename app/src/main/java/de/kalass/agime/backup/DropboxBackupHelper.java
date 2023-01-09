package de.kalass.agime.backup;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileInfo;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import de.kalass.agime.Consts;
import de.kalass.agime.R;
import de.kalass.agime.SettingsActivity;
import de.kalass.android.common.util.StringUtil;

/**
 * Implements backup to Google Drive
 * Created by klas on 13.02.14.
 */
public class DropboxBackupHelper implements BackupHelper {
    private static final String LOG_TAG = "DropboxBackupHelper";

    public static final Predicate<DbxFileInfo> BACKUP_FILE_FILTER = new BackupFileFilter<DbxFileInfo>() {

        @Override
        public String getName(@Nonnull DbxFileInfo dbxFileInfo) {
            return dbxFileInfo.path.getName();
        }
    };

    public static final Ordering<DbxFileInfo> BACKUP_FILENAME_COMPARATOR_NEWEST_FIRST = new BackupFileOrdering<DbxFileInfo>() {
        @Override
        public String getName(@Nonnull DbxFileInfo dbxFileInfo) {
            return dbxFileInfo.path.getName();
        }
    };

    private final Context _context;
    private final String backupFileName;
    private DbxFileSystem dbxFs;
    private DbxPath rootPath;

    DropboxBackupHelper(Context context, String backupFileName) {
        _context = context;
        this.backupFileName = backupFileName;
    }

    @Override
    public boolean prepare() {
        if (!BackupPreferences.isBackupToDropbox(_context)) {
            return false;
        }

        DbxAccountManager dbxAcctMgr = DbxAccountManager.getInstance(_context.getApplicationContext(), Consts.DROPBOX_APP_KEY, Consts.DROPBOX_APP_SECRET);
        if (!dbxAcctMgr.hasLinkedAccount()) {
            BackupPreferences.setBackupToDropboxLinked(_context, false);
            ServiceUtil.showServiceError(
                    _context,
                    SettingsActivity.class,
                    R.string.backup_failure_dropbox_title,
                    R.string.backup_failure_dropbox_link_message);
            return false;
        }
        try {
            dbxFs = DbxFileSystem.forAccount(dbxAcctMgr.getLinkedAccount());
            String deviceFolderName = BackupHelperUtil.getDeviceFolderName();
            deviceFolderName = StringUtil.isTrimmedNullOrEmpty(deviceFolderName) ? "" : deviceFolderName + "/";
            rootPath = DbxPath.ROOT.getChild(deviceFolderName + BackupPreferences.getBackupUUID(_context));
        } catch (DbxException.Unauthorized e) {
            Log.w(LOG_TAG, "Account Manager claims that the account is linked, but still unauthorized", e);
            BackupPreferences.setBackupToDropboxLinked(_context, false);
            ServiceUtil.showServiceError(
                    _context,
                    SettingsActivity.class,
                    R.string.backup_failure_dropbox_title,
                    R.string.backup_failure_dropbox_link_message);
            return false;
        }

        try {
            if (!dbxFs.exists(rootPath)) {
                dbxFs.createFolder(rootPath);
            }
        } catch (DbxException e) {
            Log.w(LOG_TAG, "Could not create root folder for this device", e);
            BackupPreferences.setBackupToDropboxLinked(_context, false);
            ServiceUtil.showServiceError(
                    _context,
                    SettingsActivity.class,
                    R.string.backup_failure_dropbox_title,
                    R.string.backup_failure_dropbox_message);
            return false;
        }
        return cleanupOldFiles();
    }

    private boolean cleanupOldFiles() {
        try {
            final List<DbxFileInfo> files = listSortedBackupFiles(dbxFs);


            // Cleanup
            for (int i = BackupPreferences.getBackupNumFiles(_context); i < files.size(); i++) {
                final DbxFileInfo fileInfo = files.get(i);
                try {
                    dbxFs.delete(fileInfo.path);
                } catch (DbxException.NotFound e) {
                    Log.w(LOG_TAG, "Path " + fileInfo.path + " was listed, but not found now that we delete it. Will continue.", e);
                } catch (DbxException e) {
                    Log.w(LOG_TAG, "Error while deleting Path " + fileInfo.path + ". Will continue.", e);
                }
            }
            return true;
        } catch (DbxException e) {
            Log.e(LOG_TAG, "Error accessing dropbox - will not try to sync", e);
            return false;
        }
    }

    private void writeBackup(DbxFileSystem dbxFs, BackupData.PersonalBackup personalBackup, String backupFileName) throws DbxException {
        final List<DbxFileInfo> files = listSortedBackupFiles(dbxFs);

        DbxPath path = rootPath.getChild(backupFileName);

        if (files.size() > 0) {
            DbxFileInfo latestFile = files.get(0);
            final BackupData.PersonalBackup latestEntry = readLatestEntry(dbxFs, latestFile);
            if (BackupHelperUtil.canSkip(latestEntry, personalBackup)) {
                Log.i(LOG_TAG, "Skipping backup, because nothing changed since " + latestEntry.getLatestInsertOrUpdateMillis());
                return;
            }

        }

        final boolean exists = dbxFs.exists(path);
        if (exists) {
            Log.i(LOG_TAG, "Will replace backup from earlier today");
        }

        DbxFile dbxFile = null;
        Log.i(LOG_TAG, "Writing Backup to file " + path);
        try {
            dbxFile = exists ? dbxFs.open(path) :  dbxFs.create(path);
            personalBackup.writeTo(dbxFile.getWriteStream());
        } catch (IOException e) {
            Log.e(LOG_TAG, "Failed to write backup", e);
            showServiceError();
        } finally {
            if (dbxFile != null) {
                dbxFile.close();
            }
        }
    }

    private List<DbxFileInfo> listSortedBackupFiles(DbxFileSystem dbxFs) throws DbxException {
        final List<DbxFileInfo> dbxFileInfos = dbxFs.listFolder(rootPath);
        final List<DbxFileInfo> usfiles = Lists.newArrayList(Iterables.filter(dbxFileInfos, BACKUP_FILE_FILTER));
        return BACKUP_FILENAME_COMPARATOR_NEWEST_FIRST.sortedCopy(usfiles);
    }

    private BackupData.PersonalBackup readLatestEntry(DbxFileSystem dbxFs, DbxFileInfo latestFile) {
        try {
            DbxFile file = null;
            try {
                file = dbxFs.open(latestFile.path);
                return BackupData.PersonalBackup.parseFrom(file.getReadStream());
            } finally {
                if (file != null) {
                    file.close();
                }
            }
        } catch (IOException e) {
            Log.w(LOG_TAG, "Latest backup could not be read", e);
            return null;
        }
    }


    private void showServiceError() {
        ServiceUtil.showServiceError(_context,
                SettingsActivity.class,
                de.kalass.agime.R.string.backup_failure_notif_title,
                R.string.backup_failure_notif_details);
    }

    @Override
    public void start(BackupData.PersonalBackup data) {
        Preconditions.checkNotNull(dbxFs, "prepare must be called first");
        try {
            writeBackup(dbxFs, data, backupFileName);
        } catch (DbxException e) {
            Log.e(LOG_TAG, "Failure writing Dropbox backup", e);
            ServiceUtil.showServiceError(
                    _context,
                    SettingsActivity.class,
                    R.string.backup_failure_dropbox_title,
                    R.string.backup_failure_dropbox_message);
        }
    }

    @Override
    public void finish() {
       // nothing to do
    }
}
