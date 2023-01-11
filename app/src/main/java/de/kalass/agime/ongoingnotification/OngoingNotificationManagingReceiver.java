package de.kalass.agime.ongoingnotification;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import de.kalass.agime.AgimeIntents;

/**
 * A BroadcastReceiver that calls the NotificationManagingService.
 */
public class OngoingNotificationManagingReceiver extends WakefulBroadcastReceiver {
    public static final String TAG = "Ongoing Notification";


    public OngoingNotificationManagingReceiver() {
    }


    private void callService(Context context, String action) {
        // FIXME upgrading android target from 21 to 33, our foreground
        // service crashes the application. Will disable it for now
        if (NotificationManagingService.DISABLE_FOREGROUND_SERVICE) {
            return;
        }
        Intent intent = new Intent(context, NotificationManagingService.class);
        intent.setAction(action);
        startWakefulService(context, intent);
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "received broadcast event " + intent);
        if (AgimeIntents.isInitializingIntent(intent)) {
            callService(context, AgimeIntents.ACTION_ACQUISITION_TIME_CONFIGURE);
        } else if (Intent.ACTION_TIME_CHANGED.equals(intent.getAction())) {
            callService(context, AgimeIntents.ACTION_ACQUISITION_TIME_CONFIGURE);
        } else if (AgimeIntents.ACTION_ACQUISITION_TIME_CONFIGURE.equals(intent.getAction())) {
            callService(context, AgimeIntents.ACTION_ACQUISITION_TIME_CONFIGURE);
        } else if (AgimeIntents.ACTION_REFRESH_ACQUISITION_TIME_NOTIFICATION.equals(intent.getAction())) {
            callService(context, AgimeIntents.ACTION_REFRESH_ACQUISITION_TIME_NOTIFICATION);
        } else {
            Log.w(TAG, "Unknown intent " + intent);
        }
    }

}
