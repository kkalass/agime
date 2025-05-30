package de.kalass.agime.ongoingnotification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import de.kalass.agime.acquisitiontime.AcquisitionTimeInstance;
import de.kalass.agime.acquisitiontime.AcquisitionTimes;


/**
 * Ein BroadcastReceiver, der auf präzise Alarme reagiert, die vom AlarmManager ausgelöst werden. Dies ist ein
 * wesentlicher Teil der Hybrid-Lösung für Benachrichtigungen in Agime.
 * 
 * Die Empfänger wird verwendet, um zeitkritische Vorgänge zu triggern wie: - Das Starten des
 * ShortLivedNotificationService für aktive Zeiterfassungsperioden - Das präzise Aktualisieren von Benachrichtigungen zu
 * bestimmten Zeitpunkten (z.B. Start und Ende einer Erfassungszeit) - Das Auslösen von Erinnerungsbenachrichtigungen
 */
public class NotificationAlarmReceiver extends BroadcastReceiver {

	private static final String TAG = "NotifAlarmReceiver";

	// Aktionstypen, die der Empfänger verarbeiten kann
	public static final String ACTION_START_ACQUISITION_TIME = "de.kalass.agime.action.START_ACQUISITION_TIME";
	public static final String ACTION_END_ACQUISITION_TIME = "de.kalass.agime.action.END_ACQUISITION_TIME";
	public static final String ACTION_NOISE_REMINDER = "de.kalass.agime.action.NOISE_REMINDER";

	// Extra-Parameter für Intents
	public static final String EXTRA_ACQUISITION_TIME_ID = "acquisition_time_id";
	public static final String EXTRA_START_TIME_MILLIS = "start_time_millis";
	public static final String EXTRA_END_TIME_MILLIS = "end_time_millis";
	public static final String EXTRA_MAX_RUNTIME_MINUTES = "max_runtime_minutes";

	// Standardwert für die maximale Laufzeit des Services
	private static final int DEFAULT_SERVICE_RUNTIME_MINUTES = 10;

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(TAG, "Alarm empfangen: " + intent.getAction());

		String action = intent.getAction();
		if (action == null)
			return;

		switch (action) {
			case ACTION_START_ACQUISITION_TIME:
				handleAcquisitionTimeStart(context, intent);
				break;
			case ACTION_END_ACQUISITION_TIME:
				handleAcquisitionTimeEnd(context, intent);
				break;
			case ACTION_NOISE_REMINDER:
				handleNoiseReminder(context, intent);
				break;
			default:
				Log.w(TAG, "Unbekannte Aktion: " + action);
				break;
		}

		// Immer auch den WorkManager informieren, damit dieser 
		// bei Bedarf regelmäßige Checks ausführen kann
		WorkManagerController.scheduleImmediateCheck(context);
	}


	/**
	 * Behandelt den Start einer Erfassungszeit
	 */
	private void handleAcquisitionTimeStart(Context context, Intent intent) {
		// Starten des kurzlebigen Foreground-Service für präzise Benachrichtigungen
		// während der aktiven Zeiterfassung
		int maxRuntimeMinutes = intent.getIntExtra(EXTRA_MAX_RUNTIME_MINUTES, DEFAULT_SERVICE_RUNTIME_MINUTES);

		long endTimeMillis = intent.getLongExtra(EXTRA_END_TIME_MILLIS, 0);
		if (endTimeMillis > 0) {
			// Berechne, wie lange der Service maximal laufen sollte (bis zum Ende der Erfassungszeit)
			long currentTimeMillis = System.currentTimeMillis();
			long durationMillis = endTimeMillis - currentTimeMillis;

			// Wenn die Erfassungszeit weniger als die Standard-Laufzeit dauert,
			// verkürzen wir die Laufzeit des Services entsprechend
			if (durationMillis > 0 && durationMillis < TimeUnit.MINUTES.toMillis(maxRuntimeMinutes)) {
				maxRuntimeMinutes = (int)TimeUnit.MILLISECONDS.toMinutes(durationMillis) + 1; // +1 für Sicherheit
			}
		}

		// Starten des Services mit angepasster maximaler Laufzeit
		ShortLivedNotificationService.startService(context, maxRuntimeMinutes);

		// Für längere Erfassungszeiten: AlarmManager für das Ende planen
		// (wird nicht benötigt, wenn der WorkManagerController dies bereits geplant hat)
	}


	/**
	 * Behandelt das Ende einer Erfassungszeit
	 */
	private void handleAcquisitionTimeEnd(Context context, Intent intent) {
		// Beim Ende einer Erfassungszeit müssen wir die Benachrichtigung aktualisieren
		// Das kann der WorkManager übernehmen, daher informieren wir ihn
		WorkManagerController.scheduleImmediateCheck(context);
	}


	/**
	 * Behandelt Erinnerungsbenachrichtigungen (Noise)
	 */
	private void handleNoiseReminder(Context context, Intent intent) {
		// Bei einer Erinnerung starten wir den kurzlebigen Service 
		// für eine kurze Zeit, um die Benachrichtigung mit Sound/Vibration anzuzeigen
		ShortLivedNotificationService.startService(context, 1); // Kurze Laufzeit für Erinnerung
	}
}
