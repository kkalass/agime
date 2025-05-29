package de.kalass.agime.backup;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.joda.time.DateTime;

import de.kalass.agime.AgimeIntents;

public class AutoBackupManagingBroadcastReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = "AutoBackupManagingBroadcastReceiver";
    public AutoBackupManagingBroadcastReceiver() {
    }

    private void initialize(Context context) {
        AlarmManager am = getAlarmManager(context);
        PendingIntent pendingIntent = createPendingIntent(context);
        am.cancel(pendingIntent);
        am.setInexactRepeating(AlarmManager.RTC,
                new DateTime().withTimeAtStartOfDay().getMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent);
    }

    private PendingIntent createPendingIntent(Context context) {
        Intent intent = new Intent(context, AutoBackupManagingBroadcastReceiver.class);
        intent.setAction(AgimeIntents.ACTION_AUTOMATIC_BACKUP);

        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // always schedule backup, but only execute it if it is enabled at time of backup
        Log.i(LOG_TAG, "onReceive " + intent);
        if (AgimeIntents.isInitializingIntent(intent)) {
            Log.i(LOG_TAG, "initialize ");
            initialize(context);
        } else if (AgimeIntents.ACTION_AUTOMATIC_BACKUP.equals(intent.getAction())) {
            if (BackupPreferences.isDailyBackup(context)) {
                context.startService(new Intent(context, BackupService.class));
            }
        }
        Log.i(LOG_TAG, "is backup: " + AgimeIntents.ACTION_AUTOMATIC_BACKUP.equals(intent.getAction()) + " - isDailyBackup: " + BackupPreferences.isDailyBackup(context) + " => " +intent.getAction() );
    }

    private static AlarmManager getAlarmManager(Context context) {
        return (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }
}
