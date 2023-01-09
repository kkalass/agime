package de.kalass.agime;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;

import de.kalass.agime.backup.BackupPreferences;
import de.kalass.agime.backup.BackupService;
import de.kalass.android.common.DialogUtils;

/**
 * Created by klas on 21.11.13.
 */
public class SettingsActivityDriveSupport  {
    static final int RESOLVE_CONNECTION_REQUEST_CODE = 42;
    private static final String LOG_TAG = "LOG";
    private GoogleApiClient _googleApiClient;

    private boolean suppressDisconnectOnStop = false;

    private final Activity activity;
    private final DriveConnectionListener listener;

    interface DriveConnectionListener {
        void googleDriveConnectionFailed();
        void googleDriveConnectionSuspended(int cause);
        void googleDriveConnectionConnected();
    }

    public <A extends Activity & DriveConnectionListener> SettingsActivityDriveSupport(A activity) {
        this.activity = activity;
        this.listener = activity;
    }


    protected void doOnStart() {
        if (_googleApiClient != null && !suppressDisconnectOnStop) {
            //_googleApiClient.connect();
        }
    }

    protected void doOnStop() {
        if (_googleApiClient != null && !suppressDisconnectOnStop) {
            //_googleApiClient.disconnect();
        }
    }

    final class DriveApiCallback implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {



        @Override
        public void onConnected(Bundle bundle) {
            BackupPreferences.setBackupToDriveLinked(activity, true);
            Log.i(LOG_TAG, "onConnected");
            listener.googleDriveConnectionConnected();
        }

        @Override
        public void onConnectionSuspended(final int i) {

            Log.i(LOG_TAG, "onConnectionSuspended");
            listener.googleDriveConnectionSuspended(i);
        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            Log.i(LOG_TAG, "could not connect " + connectionResult);
            BackupPreferences.setBackupToDriveLinked(activity, false);
            if (connectionResult.hasResolution()) {
                try {
                    // avoid endless disconnect / reconnect loop while the user is trying to connect
                    suppressDisconnectOnStop = true;
                    connectionResult.startResolutionForResult(activity, RESOLVE_CONNECTION_REQUEST_CODE);
                    // still trying to connect  - do not yet notify the listener
                } catch (IntentSender.SendIntentException e) {
                    Log.e(LOG_TAG, "Failed to resolve connection request", e);
                    // Unable to resolve, message user appopriately
                    DialogUtils.showError(activity, R.string.error_drive_failed);
                    listener.googleDriveConnectionFailed();
                }
            }
            else {
                GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), activity, 0).show();
                listener.googleDriveConnectionFailed();
            }
        }
    }

    void ensureDriveAccess(boolean enabled) {

        if (_googleApiClient == null) {
            DriveApiCallback cb = new DriveApiCallback();

            _googleApiClient = new GoogleApiClient.Builder(activity)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(cb)
                .addOnConnectionFailedListener(cb)
                .build();
        }
        if (enabled) {
            Log.i(LOG_TAG, "enabling drive access");
            _googleApiClient.connect();
        } else {
            Log.i(LOG_TAG, "disabling drive access");
            _googleApiClient.disconnect();
            _googleApiClient = null;
        }
    }


    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case RESOLVE_CONNECTION_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    Log.i(LOG_TAG, "User connected - will try to create a new connection");
                    suppressDisconnectOnStop = false;
                    _googleApiClient.connect();

                    // Drive is properly connected
                    BackupPreferences.setBackupToDriveLinked(activity, true);
                    // trigger a backup
                    activity.startService(new Intent(activity, BackupService.class));

                } else {
                    listener.googleDriveConnectionFailed();
                }
                break;
        }
    }
}