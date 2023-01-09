package de.kalass.agime;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;

import de.kalass.agime.backup.BackupPreferences;
import de.kalass.agime.ongoingnotification.NotificationManagingService;
import de.kalass.agime.settings.Preferences;
import de.kalass.android.common.preferences.CustomPreferenceActivity;

/**
 * Created by klas on 21.11.13.
 */
public class SettingsActivity extends CustomPreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener, SettingsActivityDriveSupport.DriveConnectionListener {

    private static final String LOG_TAG = "SettingsActivity";
    private SettingsActivityDriveSupport _driveSupport;
    private SettingsActivityDropboxSupport _dropboxSupport;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        //getActionBar().setDisplayHomeAsUpEnabled(true);
        addPreferencesFromResource(R.xml.preferences);


        updateBackupSDCardSummary();

        if (Consts.INCLUDE_GOOGLE_DRIVE) {
            _driveSupport = new SettingsActivityDriveSupport(this);
            updateDriveState();
        } else {
            final PreferenceCategory storage = (PreferenceCategory)findPreference("pref_key_storage_settings");
            storage.removePreference(findPreference(BackupPreferences.KEY_PREF_BACKUP_DRIVE));
        }

        if (Consts.INCLUDE_DROPBOX) {
            _dropboxSupport = new SettingsActivityDropboxSupport(this);
            updateDropboxState();
        } else {
            final PreferenceCategory storage = (PreferenceCategory)findPreference("pref_key_storage_settings");
            storage.removePreference(findPreference(BackupPreferences.KEY_PREF_BACKUP_DROPBOX));
        }

        updateNumFilesPreference();

        updateNoisyNotification();
    }

    private void updateLocalFSState() {
        boolean linked = BackupPreferences.isBackupToLocalFSLinked(this);
        if (!linked) {
            Log.w(LOG_TAG, "updateLocalFSState: Disabling LocalFS ");
            final CheckBoxPreference preference = (CheckBoxPreference)findPreference(BackupPreferences.KEY_PREF_BACKUP_LOCAL_FS);
            preference.setChecked(false);
        }
    }

    private void updateDropboxState() {
        boolean linked = BackupPreferences.isBackupToDropboxLinked(this);
        if (!linked) {
            Log.w(LOG_TAG, "updateDropboxState: Disabling Dropbox ");
            final CheckBoxPreference preference = (CheckBoxPreference)findPreference(BackupPreferences.KEY_PREF_BACKUP_DROPBOX);
            preference.setChecked(false);
        }
    }

    private void updateDriveState() {
        boolean linked = BackupPreferences.isBackupToDriveLinked(this);

        if (!linked) {
            Log.w(LOG_TAG, "updateDriveState: Disabling Drive ");
            final CheckBoxPreference preference = (CheckBoxPreference)findPreference(BackupPreferences.KEY_PREF_BACKUP_DRIVE);
            preference.setChecked(false);
        }
    }

    protected void doOnStart() {
        if (_driveSupport != null) {
            _driveSupport.doOnStart();
        }
    }

    protected void doOnStop() {
        if (_driveSupport != null) {
            _driveSupport.doOnStop();
        }
    }

    @Override
    public final void onStart() {
        super.onStart();
        doOnStart();
    }

    @Override
    public final void onStop() {
        super.onStop();
        doOnStop();
    }

    private void updateBackupSDCardSummary() {


        final Preference preference = findPreference(BackupPreferences.KEY_PREF_BACKUP_LOCAL_FS);
        final File externalStorageBackupDir = BackupPreferences.getExternalStorageBackupDir();
        if (externalStorageBackupDir == null || (!externalStorageBackupDir.isDirectory() && !externalStorageBackupDir.mkdirs())) {
            BackupPreferences.setBackupToLocalFSLinked(this, false);
        } else {
            BackupPreferences.setBackupToLocalFSLinked(this, true);
            preference.setSummary(R.string.pref_backup_to_sd_summary);
        }
        preference.setEnabled(BackupPreferences.isBackupToLocalFSLinked(this));

        updateLocalFSState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }



    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Preferences.KEY_PREF_ACQUISITION_TIME_NOTIFICATION)) {
            updateNoisyNotification();
            Intent intent = new Intent(this, NotificationManagingService.class);
            startService(intent);
        } else if (BackupPreferences.KEY_PREF_BACKUP_DRIVE.equals(key)) {
            if (_driveSupport != null) {
                _driveSupport.ensureDriveAccess(sharedPreferences.getBoolean(key, false));
            }

            updateNumFilesPreference();
        } else if (BackupPreferences.KEY_PREF_BACKUP_DROPBOX.equals(key)) {
            if (_dropboxSupport != null) {
                _dropboxSupport.ensureDriveAccess(sharedPreferences.getBoolean(key, false));
            }

            updateNumFilesPreference();
        } else if (BackupPreferences.KEY_PREF_BACKUP_LOCAL_FS.equals(key)) {
            updateNumFilesPreference();
        } else if (Preferences.KEY_PREF_ACQUISITION_TIME_NOTIFICATION_INTERVAL.equals(key)) {
            updateNoisyNotification();
        }
    }


    private void updateNumFilesPreference() {
        final Preference numFilesPref = findPreference(BackupPreferences.KEY_PREF_BACKUP_NUM_FILES);
        numFilesPref.setEnabled(BackupPreferences.isDailyBackup(this));
        numFilesPref.setSummary(getString(R.string.pref_backup_num_files_summary, BackupPreferences.getBackupNumFiles(this)));
    }

    private void updateNoisyNotification() {

        final ListPreference numFilesPref = (ListPreference)findPreference(Preferences.KEY_PREF_ACQUISITION_TIME_NOTIFICATION_INTERVAL);
        if (numFilesPref == null) {
            return;
        }
        final int minutes = Preferences.getAcquisitionTimeNotificationNoiseThresholdMinutes(this);
        if (minutes == 0 || !Preferences.isAcquisitionTimeNotificationEnabled(this)) {
            numFilesPref.setSummary(getString(R.string.pref_acquisition_time_notification_noise_interval_summary_off));
        } else {
            numFilesPref.setSummary(getString(R.string.pref_acquisition_time_notification_noise_interval_summary_on, numFilesPref.getEntry()));
        }

    }


    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case SettingsActivityDriveSupport.RESOLVE_CONNECTION_REQUEST_CODE:
                if (_driveSupport != null) {
                    _driveSupport.onActivityResult(requestCode, resultCode, data);
                }
                break;
            case SettingsActivityDropboxSupport.RESOLVE_CONNECTION_REQUEST_CODE:
                if (_dropboxSupport != null) {
                    _dropboxSupport.onActivityResult(requestCode, resultCode, data);
                }
                updateDropboxState();
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void googleDriveConnectionConnected() {
        updateDriveState();
    }

    @Override
    public void googleDriveConnectionSuspended(int cause) {
        updateDriveState();
    }

    @Override
    public void googleDriveConnectionFailed() {
        updateDriveState();
    }
}