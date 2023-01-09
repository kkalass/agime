package de.kalass.agime.backup;

import android.content.Context;
import android.util.Log;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import de.kalass.agime.R;
import de.kalass.agime.SettingsActivity;

/**
* Created by klas on 14.02.14.
*/
public final class LocalFSBackupHelper implements BackupHelper {
    public static final Predicate<File> BACKUP_FILE_FILTER = new BackupFileFilter<File>() {
        @Override
        public String getName(@Nonnull File file) {
            return file.getName();
        }
    };
    public static final Ordering<File> BACKUP_FILENAME_COMPARATOR_NEWEST_FIRST = new BackupFileOrdering<File>() {
        @Override
        public String getName(@Nonnull File file) {
            return file.getName();
        }
    };

    public static final String LOG_TAG = "LocalFSBackupHelper";
    private final Context _context;
    private final File _file;

    LocalFSBackupHelper(Context context, String suggestedFileName) {
        _context = context;
        _file = new File(BackupPreferences.getExternalStorageBackupDir(), suggestedFileName);
    }

    protected boolean isEnabled() {
        return BackupPreferences.isBackupToLocalFS(getContext());
    }

    public Context getContext() {
        return _context;
    }

    /**
     * @return null if local backup is not possible, else the existing files
     */
    @Override
    public boolean prepare() {
        if (!isEnabled()) {
            return false;
        }

        File dir = _file.getParentFile();
        if (!dir.exists() && !dir.mkdirs()) {
            Log.e(LOG_TAG, "Failed to write backup because backup dir could not be created: " + dir);
            BackupPreferences.setBackupToLocalFSLinked(_context, false);
            showServiceError();
            return false;
        }

        final List<File> files = getSortedBackupFiles(dir);

        // Cleanup
        for (int i = BackupPreferences.getBackupNumFiles(getContext()); i < files.size(); i++) {

            File f = files.get(i);
            if (!f.delete()) {
                Log.w(LOG_TAG, "Could not delete backup file " + f.getAbsolutePath());
            }

        }
        return true;
    }

    private List<File> getSortedBackupFiles(File dir) {
        final File[] array = dir.listFiles();
        if (array == null) {
            return ImmutableList.of();
        }
        final Iterable<File> filteredFiles = Iterables.filter(Arrays.asList(array), BACKUP_FILE_FILTER);
        return BACKUP_FILENAME_COMPARATOR_NEWEST_FIRST.sortedCopy(filteredFiles);
    }

    @Override
    public void start(BackupData.PersonalBackup data) {
        writeLocalBackup(_file, data);
    }

    @Override
    public void finish() {
        // nothing to do
    }

    private void writeLocalBackup(File file, BackupData.PersonalBackup personalBackup) {
        final List<File> files = getSortedBackupFiles(file.getParentFile());

        if (!files.isEmpty()) {
            File latestFile = files.get(0);
            final BackupData.PersonalBackup latestEntry = readLatestEntry(latestFile);
            if (BackupHelperUtil.canSkip(latestEntry, personalBackup)) {
                Log.i(LOG_TAG, "Skipping backup, because nothing changed since " + latestEntry.getLatestInsertOrUpdateMillis());
                return;
            }
        }

        if (file.exists()) {
            Log.i(LOG_TAG, "Will replace backup from earlier today");
        }

        FileOutputStream out = null;
        Log.i(LOG_TAG, "Writing Backup to file " + file);
        try {
            out = new FileOutputStream(file);
            personalBackup.writeTo(out);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Failed to write backup", e);
            showServiceError();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Failed to close backup stream!", e);
                    showServiceError();
                }
            }
        }
    }

    private BackupData.PersonalBackup readLatestEntry(File latestFile) {
        try {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(latestFile);
                return BackupData.PersonalBackup.parseFrom(fis);
            } finally {
                if (fis != null) {
                    fis.close();
                }
            }
        } catch (IOException e) {
            Log.w(LOG_TAG, "Latest backup could not be read", e);
            return null;
        }
    }

    void showServiceError() {
        ServiceUtil.showServiceError(getContext(),
                SettingsActivity.class,
                de.kalass.agime.R.string.backup_failure_notif_title,
                R.string.backup_failure_notif_details);
    }
}
