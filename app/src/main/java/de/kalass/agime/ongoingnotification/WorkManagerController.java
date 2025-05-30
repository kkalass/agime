package de.kalass.agime.ongoingnotification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Operation;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.common.util.concurrent.ListenableFuture;

import org.joda.time.DateTime;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import de.kalass.agime.AgimeIntents;
import de.kalass.agime.acquisitiontime.AcquisitionTimeInstance;
import de.kalass.agime.acquisitiontime.AcquisitionTimes;
import de.kalass.agime.acquisitiontime.RecurringDAO;
import de.kalass.android.common.simpleloader.CursorUtil;
import de.kalass.android.common.support.AlarmManagerSupport;


/**
 * Verwaltet die Ausführung des NotificationWorker über WorkManager. Ersetzt die Steuerungslogik des alten
 * NotificationManagingService.
 */
public class WorkManagerController {

	private static final String LOG_TAG = "WorkManagerController";

	// Eindeutige Namen für die Arbeit
	public static final String WORKER_TAG = "notification_worker";
	public static final String PERIODIC_WORK_NAME = "periodic_notification_check";
	public static final String IMMEDIATE_WORK_NAME = "immediate_notification_check";

	// Intervall für die regelmäßige Überprüfung (15 Minuten ist das Minimum für PeriodicWorkRequest)
	private static final long CHECK_INTERVAL_MINUTES = 15;

	// Request-Codes für die verschiedenen PendingIntents
	private static final int REQUEST_CODE_START_ACQUISITION = 100;
	private static final int REQUEST_CODE_END_ACQUISITION = 200;
	private static final int REQUEST_CODE_NOISE_REMINDER = 300;

	// Flag für Debugging
	private static final boolean DEBUG = false;

	/**
	 * Initialisiert den WorkManager bei App-Start oder nach Geräteneustart
	 */
	public static void initialize(Context context) {
		Log.i(LOG_TAG, "WorkManagerController wird initialisiert");

		// Sofortige Ausführung planen
		scheduleImmediateCheck(context);

		// Regelmäßige Überprüfungen planen
		schedulePeriodicChecks(context);

		// Alarme für die nächste Acquisition Time-Phase planen
		scheduleNextAcquisitionTimeAlarms(context);
	}


	/**
	 * Plant eine sofortige Ausführung des NotificationWorker
	 */
	public static void scheduleImmediateCheck(Context context) {
		Log.i(LOG_TAG, "Plane sofortige Benachrichtigungsprüfung");

		OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(NotificationWorker.class)
			.addTag(WORKER_TAG)
			.build();

		WorkManager.getInstance(context)
			.enqueueUniqueWork(
				IMMEDIATE_WORK_NAME,
				ExistingWorkPolicy.REPLACE,
				workRequest);
	}


	/**
	 * Plant regelmäßige Ausführungen des NotificationWorker
	 */
	private static void schedulePeriodicChecks(Context context) {
		Log.i(LOG_TAG, "Plane regelmäßige Benachrichtigungsprüfungen");

		Constraints constraints = new Constraints.Builder()
			.setRequiredNetworkType(NetworkType.NOT_REQUIRED)
			.build();

		PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(
				NotificationWorker.class,
				CHECK_INTERVAL_MINUTES,
				TimeUnit.MINUTES)
					.setConstraints(constraints)
					.addTag(WORKER_TAG)
					.build();

		WorkManager.getInstance(context)
			.enqueueUniquePeriodicWork(
				PERIODIC_WORK_NAME,
				ExistingPeriodicWorkPolicy.KEEP, // Behalte bestehende Planung falls vorhanden
				periodicWorkRequest);
	}


	/**
	 * Berechnet die Zeit bis zur nächsten Ausführung und plant entsprechend
	 */
	public static void scheduleNextExecution(Context context) {
		// Im aktuellen Ansatz sind regelmäßige Checks bereits geplant,
		// aber hier sollten wir auch die AlarmManager-Alarme aktualisieren
		scheduleNextAcquisitionTimeAlarms(context);

		try {
			ListenableFuture<List<WorkInfo>> workInfos = WorkManager.getInstance(context)
				.getWorkInfosByTag(WORKER_TAG);

			List<WorkInfo> infoList = workInfos.get();
			boolean hasScheduledWork = false;

			for (WorkInfo info : infoList) {
				if (info.getState() == WorkInfo.State.ENQUEUED ||
						info.getState() == WorkInfo.State.RUNNING) {
					hasScheduledWork = true;
					break;
				}
			}

			if (!hasScheduledWork) {
				// Falls keine geplante Arbeit vorhanden ist, plane eine neue
				schedulePeriodicChecks(context);
			}

		}
		catch (ExecutionException | InterruptedException e) {
			Log.e(LOG_TAG, "Fehler beim Prüfen geplanter Worker", e);
			// Im Fehlerfall einfach neu planen
			schedulePeriodicChecks(context);
		}
	}


	/**
	 * Verarbeitet einen Intent, der für den alten NotificationManagingService bestimmt war
	 */
	public static void handleIntent(Context context, Intent intent) {
		if (intent == null) {
			return;
		}

		String action = intent.getAction();
		if (action == null) {
			scheduleImmediateCheck(context);
			return;
		}

		switch (action) {
			case AgimeIntents.ACTION_ACQUISITION_TIME_CONFIGURE:
			case AgimeIntents.ACTION_REFRESH_ACQUISITION_TIME_NOTIFICATION:
				// Diese Aktionen erfordern eine sofortige Prüfung
				scheduleImmediateCheck(context);
				break;
			default:
				Log.w(LOG_TAG, "Unbekannte Aktion: " + action);
				break;
		}
	}


	/**
	 * Stoppt alle geplanten Arbeiten
	 */
	public static Operation cancelAllWork(Context context) {
		return WorkManager.getInstance(context).cancelAllWorkByTag(WORKER_TAG);
	}


	/**
	 * Plant Alarme für den Start und das Ende der nächsten AcquisitionTime-Phase
	 */
	public static void scheduleNextAcquisitionTimeAlarms(Context context) {
		Log.i(LOG_TAG, "Plane Alarme für die nächste AcquisitionTime");

		// Aktuelle AcquisitionTimes abrufen
		AcquisitionTimes times = getCurrentAcquisitionTimes(context);
		if (times == null) {
			Log.w(LOG_TAG, "Keine AcquisitionTimes verfügbar");
			return;
		}

		// Zuvor geplante Alarme abbrechen
		cancelAllAlarms(context);

		AcquisitionTimeInstance current = times.getCurrent();
		AcquisitionTimeInstance next = times.getNext();
		DateTime now = new DateTime();

		// 1. Wenn eine aktive Erfassungszeit läuft, plane das Ende
		if (current != null) {
			if (current.getEndDateTime().isAfter(now)) {
				scheduleAcquisitionTimeEnd(context, current);

				// Starte sofort den ShortLivedNotificationService für präzise Benachrichtigungen
				ShortLivedNotificationService.startService(context);

				if (DEBUG)
					Log.d(LOG_TAG, "Aktive Erfassungszeit gefunden, Ende geplant für: " +
							current.getEndDateTime().toString());
			}
		}

		// 2. Plane den Start der nächsten Erfassungszeit
		if (next != null && next.getStartDateTime().isAfter(now)) {
			scheduleAcquisitionTimeStart(context, next);
			if (DEBUG)
				Log.d(LOG_TAG, "Nächste Erfassungszeit gefunden, Start geplant für: " +
						next.getStartDateTime().toString());
		}

		// 3. Plane Erinnerungsalarme falls nötig
		if (current != null) {
			scheduleNoiseReminderIfNeeded(context, current);
		}
	}


	/**
	 * Plant einen Alarm für den Start einer Erfassungszeit
	 */
	private static void scheduleAcquisitionTimeStart(Context context, AcquisitionTimeInstance instance) {
		Intent intent = new Intent(context, NotificationAlarmReceiver.class);
		intent.setAction(NotificationAlarmReceiver.ACTION_START_ACQUISITION_TIME);
		intent.putExtra(NotificationAlarmReceiver.EXTRA_ACQUISITION_TIME_ID, instance.getId());
		intent.putExtra(NotificationAlarmReceiver.EXTRA_START_TIME_MILLIS, instance.getStartDateTime().getMillis());
		intent.putExtra(NotificationAlarmReceiver.EXTRA_END_TIME_MILLIS, instance.getEndDateTime().getMillis());

		PendingIntent pendingIntent = PendingIntent.getBroadcast(
			context,
			REQUEST_CODE_START_ACQUISITION,
			intent,
			PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

		AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
			// Auf Android 12+ benötigen wir spezielle Berechtigung für exakte Alarme
			Log.w(LOG_TAG, "Keine Berechtigung für exakte Alarme, verwende ungenauen Alarm");
			alarmManager.set(
				AlarmManager.RTC_WAKEUP,
				instance.getStartDateTime().getMillis(),
				pendingIntent);
		}
		else {
			// Auf älteren Versionen oder mit Berechtigung verwenden wir exakte Alarme
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				alarmManager.setExactAndAllowWhileIdle(
					AlarmManager.RTC_WAKEUP,
					instance.getStartDateTime().getMillis(),
					pendingIntent);
			}
			else {
				AlarmManagerSupport.setAlarm(
					alarmManager,
					AlarmManager.RTC_WAKEUP,
					instance.getStartDateTime().getMillis(),
					pendingIntent);
			}
		}
	}


	/**
	 * Plant einen Alarm für das Ende einer Erfassungszeit
	 */
	private static void scheduleAcquisitionTimeEnd(Context context, AcquisitionTimeInstance instance) {
		Intent intent = new Intent(context, NotificationAlarmReceiver.class);
		intent.setAction(NotificationAlarmReceiver.ACTION_END_ACQUISITION_TIME);
		intent.putExtra(NotificationAlarmReceiver.EXTRA_ACQUISITION_TIME_ID, instance.getId());
		intent.putExtra(NotificationAlarmReceiver.EXTRA_END_TIME_MILLIS, instance.getEndDateTime().getMillis());

		PendingIntent pendingIntent = PendingIntent.getBroadcast(
			context,
			REQUEST_CODE_END_ACQUISITION,
			intent,
			PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

		AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
			alarmManager.set(
				AlarmManager.RTC_WAKEUP,
				instance.getEndDateTime().getMillis(),
				pendingIntent);
		}
		else {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				alarmManager.setExactAndAllowWhileIdle(
					AlarmManager.RTC_WAKEUP,
					instance.getEndDateTime().getMillis(),
					pendingIntent);
			}
			else {
				AlarmManagerSupport.setAlarm(
					alarmManager,
					AlarmManager.RTC_WAKEUP,
					instance.getEndDateTime().getMillis(),
					pendingIntent);
			}
		}
	}


	/**
	 * Plant Erinnerungsalarme während einer Erfassungszeit, falls in den Einstellungen aktiviert
	 */
	private static void scheduleNoiseReminderIfNeeded(Context context, AcquisitionTimeInstance instance) {
		// Nur planen, wenn die Einstellung aktiviert ist
		int noiseThresholdMinutes = de.kalass.agime.settings.Preferences.getAcquisitionTimeNotificationNoiseThresholdMinutes(context);
		if (noiseThresholdMinutes <= 0) {
			return;
		}

		long noiseIntervalMillis = TimeUnit.MINUTES.toMillis(noiseThresholdMinutes);
		long startTimeMillis = instance.getStartDateTime().getMillis();
		long endTimeMillis = instance.getEndDateTime().getMillis();
		long nowMillis = System.currentTimeMillis();

		// Nächsten Erinnerungszeitpunkt berechnen
		long nextNoiseTimeMillis = startTimeMillis + noiseIntervalMillis;
		while(nextNoiseTimeMillis < nowMillis) {
			nextNoiseTimeMillis += noiseIntervalMillis;
		}

		// Nur planen, wenn die nächste Erinnerung noch in die aktuelle Erfassungszeit fällt
		if (nextNoiseTimeMillis < endTimeMillis) {
			Intent intent = new Intent(context, NotificationAlarmReceiver.class);
			intent.setAction(NotificationAlarmReceiver.ACTION_NOISE_REMINDER);

			PendingIntent pendingIntent = PendingIntent.getBroadcast(
				context,
				REQUEST_CODE_NOISE_REMINDER,
				intent,
				PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

			AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextNoiseTimeMillis,
                    pendingIntent);
            if (DEBUG)
                Log.d(LOG_TAG, "Noise-Erinnerung für " + new DateTime(nextNoiseTimeMillis).toString() + " geplant");
        }
	}


	/**
	 * Lädt die aktuellen AcquisitionTimes
	 */
	public static AcquisitionTimes getCurrentAcquisitionTimes(Context context) {
		Cursor query = context.getContentResolver().query(
			RecurringDAO.CONTENT_URI, RecurringDAO.PROJECTION, null, null, null);

		List<RecurringDAO.Data> recurringItems;
		try {
			recurringItems = CursorUtil.readList(query, RecurringDAO.READ_DATA);
			return AcquisitionTimes.fromRecurring(recurringItems, new DateTime());
		}
		catch (Exception e) {
			Log.e(LOG_TAG, "Fehler beim Laden der AcquisitionTimes", e);
			return null;
		}
		finally {
			if (query != null) {
				query.close();
			}
		}
	}


	/**
	 * Bricht alle geplanten Alarme ab
	 */
	private static void cancelAllAlarms(Context context) {
		AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

		// Alarm für Erfassungszeit-Start abbrechen
		Intent startIntent = new Intent(context, NotificationAlarmReceiver.class);
		startIntent.setAction(NotificationAlarmReceiver.ACTION_START_ACQUISITION_TIME);
		PendingIntent startPendingIntent = PendingIntent.getBroadcast(
			context, REQUEST_CODE_START_ACQUISITION, startIntent,
			PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
		if (startPendingIntent != null) {
			alarmManager.cancel(startPendingIntent);
			startPendingIntent.cancel();
		}

		// Alarm für Erfassungszeit-Ende abbrechen
		Intent endIntent = new Intent(context, NotificationAlarmReceiver.class);
		endIntent.setAction(NotificationAlarmReceiver.ACTION_END_ACQUISITION_TIME);
		PendingIntent endPendingIntent = PendingIntent.getBroadcast(
			context, REQUEST_CODE_END_ACQUISITION, endIntent,
			PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
		if (endPendingIntent != null) {
			alarmManager.cancel(endPendingIntent);
			endPendingIntent.cancel();
		}

		// Alarm für Noise-Erinnerungen abbrechen
		Intent noiseIntent = new Intent(context, NotificationAlarmReceiver.class);
		noiseIntent.setAction(NotificationAlarmReceiver.ACTION_NOISE_REMINDER);
		PendingIntent noisePendingIntent = PendingIntent.getBroadcast(
			context, REQUEST_CODE_NOISE_REMINDER, noiseIntent,
			PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
		if (noisePendingIntent != null) {
			alarmManager.cancel(noisePendingIntent);
			noisePendingIntent.cancel();
		}
	}
}
