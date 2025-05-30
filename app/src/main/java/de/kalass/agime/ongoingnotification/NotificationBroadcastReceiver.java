package de.kalass.agime.ongoingnotification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import de.kalass.agime.AgimeIntents;


/**
 * Ein BroadcastReceiver, der den WorkManagerController startet. Ersetzt den alten OngoingNotificationManagingReceiver.
 */
public class NotificationBroadcastReceiver extends BroadcastReceiver {

	public static final String TAG = "NotificationReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(TAG, "Broadcast-Event empfangen: " + intent);

		if (AgimeIntents.isInitializingIntent(intent)) {
			// Erster Start oder Neustart des Geräts
			WorkManagerController.initialize(context);
		}
		else if (Intent.ACTION_TIME_CHANGED.equals(intent.getAction())) {
			// Zeitänderung auf dem Gerät
			WorkManagerController.scheduleImmediateCheck(context);
		}
		else if (AgimeIntents.ACTION_ACQUISITION_TIME_CONFIGURE.equals(intent.getAction())) {
			// Konfigurationsänderung für Zeiterfassung
			WorkManagerController.scheduleImmediateCheck(context);
		}
		else if (AgimeIntents.ACTION_REFRESH_ACQUISITION_TIME_NOTIFICATION.equals(intent.getAction())) {
			// Aktualisierung der Benachrichtigung angefordert
			WorkManagerController.scheduleImmediateCheck(context);
		}
		else {
			Log.w(TAG, "Unbekannter Intent: " + intent);
		}
	}
}
