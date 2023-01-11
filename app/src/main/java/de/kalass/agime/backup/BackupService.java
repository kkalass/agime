package de.kalass.agime.backup;

import android.app.IntentService;
import android.content.Intent;

import com.google.common.collect.ImmutableList;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Creates backups that will be stored in a public external storage directory and
 * may be restored by the user via the restore dialog
 */
public class BackupService extends IntentService {

    public static final String AGIME_BACKUP_PREFIX = "agime-backup-";
    public static final String AGIME_BACKUP_SUFFIX = ".agime";


    private static final String LOG_TAG = "Agime Backup";

    public BackupService() {
        super("BackupService");
    }



    private List<BackupHelper> createHandlers(String suggestedFileName) {
        // note that async services should be first here, so that they can
        // do their work while the other services run
        return ImmutableList.of(
                // sync
                new LocalFSBackupHelper(this, suggestedFileName),
                new DropboxBackupHelper(this, suggestedFileName)
        );
    }

    /**
     * @return the List of all Handlers that were successfully prepared and want to take part
     */
    private List<BackupHelper> prepare(List<BackupHelper> handlers) {
        ImmutableList.Builder<BackupHelper> b = ImmutableList.builder();
        for (BackupHelper handler : handlers) {
            if (handler.prepare()) {
                b.add(handler);
            }
        }
        return b.build();
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        FullExportLoader trackedActivityLoader = new FullExportLoader(getApplicationContext());


        // write backup to file.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String timeStamp = sdf.format(System.currentTimeMillis());
        final String suggestedFileName = AGIME_BACKUP_PREFIX + timeStamp + AGIME_BACKUP_SUFFIX;

        List<BackupHelper> handlers = createHandlers(suggestedFileName);

        List<BackupHelper> preparedHandlers = prepare(handlers);
        if (preparedHandlers.isEmpty()) {
            // no handler is active, no need to query backup data
            return;
        }

        // Load data for Backup
        final BackupData.PersonalBackup personalBackup = trackedActivityLoader.loadBackupData();

        // Start backup, some are async and start work immediately
        for (BackupHelper handler:  preparedHandlers) {
            handler.start(personalBackup);
        }

        // Wait for all to finish
        for (BackupHelper handler:  preparedHandlers) {
            handler.finish();
        }

    }

}
