package de.kalass.agime.backup;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.joda.time.DateTime;

import de.kalass.agime.AgimeIntents;

public class AutoBackupManagingBroadcastReceiver extends BroadcastReceiver {
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

        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // always schedule backup, but only execute it if it is enabled at time of backup
        if (AgimeIntents.isInitializingIntent(intent)) {
            initialize(context);
        } else if (AgimeIntents.ACTION_AUTOMATIC_BACKUP.equals(intent.getAction())) {
            if (BackupPreferences.isDailyBackup(context)) {
                context.startService(new Intent(context, BackupService.class));
            }
        }
    }

    private static AlarmManager getAlarmManager(Context context) {
        return (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }
}
