package de.kalass.agime;

import android.app.Activity;
import android.content.Intent;

import com.dropbox.sync.android.DbxAccountManager;

import de.kalass.agime.backup.BackupPreferences;
import de.kalass.agime.backup.BackupService;

/**
 * Created by klas on 21.11.13.
 */
public class SettingsActivityDropboxSupport {
    static final int RESOLVE_CONNECTION_REQUEST_CODE = 32;
    private static final String LOG_TAG = "SettingsActivityDropboxSupport";

    private DbxAccountManager mDbxAcctMgr;

    private final Activity activity;

    public SettingsActivityDropboxSupport(Activity activity) {
        this.activity = activity;
    }


    protected void doOnStart() {
    }

    protected void doOnStop() {
    }

    void ensureDriveAccess(boolean enabled) {

        if (enabled) {
            mDbxAcctMgr = DbxAccountManager.getInstance(activity.getApplicationContext(), Consts.DROPBOX_APP_KEY, Consts.DROPBOX_APP_SECRET);
            mDbxAcctMgr.startLink(activity, RESOLVE_CONNECTION_REQUEST_CODE);
        }

    }


    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case RESOLVE_CONNECTION_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    // Dropbox is properly connected
                    BackupPreferences.setBackupToDropboxLinked(activity, true);
                    // trigger a backup
                    activity.startService(new Intent(activity, BackupService.class));
                }
                break;
        }
    }
}