package de.kalass.agime.ongoingnotification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import de.kalass.agime.AgimeIntents;


/**
 * Ein BroadcastReceiver, der den WorkManagerController und das Benachrichtigungssystem initialisiert. Teil der
 * Hybrid-Lösung für Benachrichtigungen, kombiniert WorkManager und AlarmManager.
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
		else if (Intent.ACTION_TIME_CHANGED.equals(intent.getAction()) ||
				Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
			// Zeitänderung auf dem Gerät - komplette Neuplanung notwendig
			WorkManagerController.initialize(context); // Besser als nur scheduleImmediateCheck
		}
		else if (AgimeIntents.ACTION_ACQUISITION_TIME_CONFIGURE.equals(intent.getAction())) {
			// Konfigurationsänderung für Zeiterfassung - erfordert Neuplanung aller Alarme
			WorkManagerController.scheduleNextAcquisitionTimeAlarms(context);
			WorkManagerController.scheduleImmediateCheck(context);

			// Bei Konfigurationsänderungen prüfen, ob wir gerade in einer aktiven Erfassungszeit sind
			// und ggf. den kurzlebigen Service starten
			checkAndStartShortLivedServiceIfNeeded(context);
		}
		else if (AgimeIntents.ACTION_REFRESH_ACQUISITION_TIME_NOTIFICATION.equals(intent.getAction())) {
			// Aktualisierung der Benachrichtigung angefordert
			WorkManagerController.scheduleImmediateCheck(context);
			checkAndStartShortLivedServiceIfNeeded(context);
		}
		else {
			Log.w(TAG, "Unbekannter Intent: " + intent);
			// Trotzdem den Standardfall durchführen
			WorkManagerController.scheduleImmediateCheck(context);
		}
	}


	/**
	 * Prüft, ob wir uns gerade in einer aktiven Erfassungszeit befinden und startet ggf. den kurzlebigen Service f��r
	 * hochpräzise Benachrichtigungen
	 */
	private void checkAndStartShortLivedServiceIfNeeded(Context context) {
		// Diese Prüfung könnte auch direkt im ShortLivedNotificationService stattfinden,
		// aber da der Service einen Foreground-Service startet, ist es besser, erst zu prüfen
		// und nur bei Bedarf zu starten
		AcquisitionTimes times = WorkManagerController.getCurrentAcquisitionTimes(context);
		if (times != null && times.getCurrent() != null) {
			// Wir sind in einer aktiven Erfassungszeit - starte den Service
			ShortLivedNotificationService.startService(context);
		}
	}
}
