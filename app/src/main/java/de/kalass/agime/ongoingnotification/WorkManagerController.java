package de.kalass.agime.ongoingnotification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.widget.Toast;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.common.util.concurrent.ListenableFuture;

import org.joda.time.DateTime;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import de.kalass.agime.AgimeIntents;
import de.kalass.agime.R;
import de.kalass.agime.acquisitiontime.AcquisitionTimeInstance;
import de.kalass.agime.acquisitiontime.AcquisitionTimes;
import de.kalass.agime.acquisitiontime.RecurringDAO;
import de.kalass.android.common.simpleloader.CursorUtil;


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
	private static final boolean DEBUG = true; // Enable debug logging

	/**
	 * Initialisiert den WorkManager bei App-Start oder nach Geräteneustart
	 */
	public static void initialize(Context context) {
		AcquisitionTimesProvider acquisitionTimesProvider = new DefaultAcquisitionTimesProvider(context);
		initialize(context, acquisitionTimesProvider);
	}


	/**
	 * Initialisiert den WorkManager mit dependency injection für bessere Testbarkeit
	 */
	public static void initialize(Context context, AcquisitionTimesProvider acquisitionTimesProvider) {
		Log.i(LOG_TAG, "WorkManagerController wird initialisiert");

		// Sofortige Ausführung planen
		scheduleImmediateCheck(context);

		// Regelmäßige Überprüfungen planen
		schedulePeriodicChecks(context);

		// Alarme für die nächste Acquisition Time-Phase planen
		scheduleNextAcquisitionTimeAlarms(context, acquisitionTimesProvider);
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
		AcquisitionTimesProvider acquisitionTimesProvider = new DefaultAcquisitionTimesProvider(context);
		scheduleNextExecution(context, acquisitionTimesProvider);
	}


	/**
	 * Berechnet die Zeit bis zur nächsten Ausführung und plant entsprechend mit dependency injection
	 */
	public static void scheduleNextExecution(Context context, AcquisitionTimesProvider acquisitionTimesProvider) {
		// Im aktuellen Ansatz sind regelmäßige Checks bereits geplant,
		// aber hier sollten wir auch die AlarmManager-Alarme aktualisieren
		scheduleNextAcquisitionTimeAlarms(context, acquisitionTimesProvider);

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
	 * Plant Alarme für den Start und das Ende der nächsten AcquisitionTime-Phase
	 */
	public static void scheduleNextAcquisitionTimeAlarms(Context context) {
		AcquisitionTimesProvider acquisitionTimesProvider = new DefaultAcquisitionTimesProvider(context);
		scheduleNextAcquisitionTimeAlarms(context, acquisitionTimesProvider);
	}


	/**
	 * Plant Alarme für den Start und das Ende der nächsten AcquisitionTime-Phase mit dependency injection
	 */
	public static void scheduleNextAcquisitionTimeAlarms(Context context, AcquisitionTimesProvider acquisitionTimesProvider) {
		Log.i(LOG_TAG, "Plane Alarme für die nächste AcquisitionTime");
		if (DEBUG)
			Log.d(LOG_TAG, "scheduleNextAcquisitionTimeAlarms called with context: " + context);

		// Aktuelle AcquisitionTimes abrufen
		AcquisitionTimes times = getCurrentAcquisitionTimes(context, acquisitionTimesProvider);
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
				// FIXME Foreground service removed for now - maybe we need to rework this
				// ShortLivedNotificationService.startService(context);

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
			// On Android 12+, request permission if not already granted
			// The scheduleNoiseReminderIfNeeded method will handle fallback to inexact alarms if needed
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				checkAndRequestExactAlarmPermission(context);
			}

			// Try to schedule the reminder - it will use exact alarms if possible, or fall back to inexact
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
			alarmManager.setExactAndAllowWhileIdle(
				AlarmManager.RTC_WAKEUP,
				instance.getStartDateTime().getMillis(),
				pendingIntent);
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
			alarmManager.setExactAndAllowWhileIdle(
				AlarmManager.RTC_WAKEUP,
				instance.getEndDateTime().getMillis(),
				pendingIntent);
		}
	}


	/**
	 * Plant Erinnerungsalarme während einer Erfassungszeit, falls in den Einstellungen aktiviert
	 */
	private static void scheduleNoiseReminderIfNeeded(Context context, AcquisitionTimeInstance instance) {
		if (instance == null || !instance.isActiveAt(DateTime.now())) {
			return;
		}

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

			// Check if we can schedule exact alarms (needed for Android 12+)
			boolean canScheduleExactAlarms = de.kalass.agime.util.AlarmPermissionHelper.hasExactAlarmPermission(context);

			if (canScheduleExactAlarms) {
				try {
					alarmManager.setExactAndAllowWhileIdle(
						AlarmManager.RTC_WAKEUP,
						nextNoiseTimeMillis,
						pendingIntent);
					if (DEBUG) {
						Log.d(LOG_TAG, "Exakte Noise-Erinnerung für " + new DateTime(nextNoiseTimeMillis) + " geplant");
					}
					return;
				}
				catch (SecurityException e) {
					Log.e(LOG_TAG, "Keine Berechtigung zum Planen von genauen Alarmen", e);
					// Continue to fallback
				}
			}
			else {
				if (DEBUG) {
					Log.w(LOG_TAG, "Kann keine exakte Alarm-Erinnerung planen - SCHEDULE_EXACT_ALARM-Berechtigung fehlt");
				}
			}

			// Fallback to inexact alarm if exact scheduling is not available or failed
			try {
				alarmManager.set(AlarmManager.RTC_WAKEUP, nextNoiseTimeMillis, pendingIntent);
				if (DEBUG) {
					Log.d(LOG_TAG, "Inexakte Noise-Erinnerung für " + new DateTime(nextNoiseTimeMillis) + " geplant");
				}
			}
			catch (Exception e) {
				Log.e(LOG_TAG, "Fehler beim Planen der Noise-Erinnerung", e);
			}
		}
	}

	private static final String PREF_ALARM_PERMISSION_ASKED = "alarm_permission_asked";
	private static final String PREF_NEVER_ASK_AGAIN = "never_ask_again_alarm_permission";
	private static final String PREF_SHOULD_SHOW_PERMISSION_DIALOG = "should_show_permission_dialog";

	// Call this from your Activity's onResume() to handle pending permission dialogs
	public static void checkPendingPermissionDialog(android.app.Activity activity) {
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
			return;
		}

		android.content.SharedPreferences prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(activity);
		boolean shouldShowDialog = prefs.getBoolean(PREF_SHOULD_SHOW_PERMISSION_DIALOG, false);

		if (shouldShowDialog) {
			// Clear the flag first to prevent showing it multiple times
			prefs.edit().putBoolean(PREF_SHOULD_SHOW_PERMISSION_DIALOG, false).apply();
			// Now show the dialog
			showPermissionRequestDialog(activity);
		}
	}


	/**
	 * Checks if the app has the required permissions to schedule exact alarms and shows a dialog if needed
	 *
	 * @param context The application context
	 */
	public static void checkAndRequestExactAlarmPermission(Context context) {
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
			if (DEBUG)
				Log.d(LOG_TAG, "SDK version < S, skipping exact alarm permission check");
			return;
		}

		// Check if we already have permission
		boolean hasPermission = de.kalass.agime.util.AlarmPermissionHelper.hasExactAlarmPermission(context);
		if (DEBUG)
			Log.d(LOG_TAG, "Has exact alarm permission: " + hasPermission);
		if (hasPermission) {
			return;
		}

		// Check if user selected "Don't ask again"
		android.content.SharedPreferences prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
		boolean neverAskAgain = prefs.getBoolean(PREF_NEVER_ASK_AGAIN, false);
		if (DEBUG)
			Log.d(LOG_TAG, "Never ask again: " + neverAskAgain);
		if (neverAskAgain) {
			return;
		}

		// Set a flag to show the dialog when an Activity is available
		if (DEBUG)
			Log.d(LOG_TAG, "Scheduling permission dialog for next Activity");
		prefs.edit().putBoolean(PREF_SHOULD_SHOW_PERMISSION_DIALOG, true).apply();
	}


	private static void showPermissionRequestDialog(Context context) {
		if (!(context instanceof de.kalass.agime.analytics.AnalyticsActionBarActivity) &&
				!(context.getApplicationContext() instanceof android.app.Activity)) {
			if (DEBUG)
				Log.d(LOG_TAG, "Cannot show dialog - context is not an AnalyticsActionBarActivity");
			return;
		}

		// Get the activity (either directly or from application context)
		android.app.Activity activity = (context instanceof android.app.Activity)
				? (android.app.Activity)context
				: (android.app.Activity)context.getApplicationContext();

		if (!(activity instanceof de.kalass.agime.analytics.AnalyticsActionBarActivity)) {
			if (DEBUG)
				Log.d(LOG_TAG, "Activity is not an AnalyticsActionBarActivity");
			return;
		}

		de.kalass.agime.analytics.AnalyticsActionBarActivity analyticsActivity = (de.kalass.agime.analytics.AnalyticsActionBarActivity)activity;

		// Show the snackbar with both actions
		analyticsActivity.showSnackbar(
			context.getString(R.string.snackbar_permission_needed),
			context.getString(R.string.grant_permission), // First action (main action)
			v -> {
				// Grant permission action
				try {
					de.kalass.agime.util.AlarmPermissionHelper.requestExactAlarmPermission(context);
					// Mark that we've asked
					android.preference.PreferenceManager.getDefaultSharedPreferences(context)
						.edit()
						.putBoolean(PREF_ALARM_PERMISSION_ASKED, true)
						.apply();
				}
				catch (Exception e) {
					Log.e(LOG_TAG, "Error requesting exact alarm permission", e);
				}
			},
			context.getString(R.string.learn_more), // Second action (learn more)
			v -> showPermissionExplanationDialog(context));

		if (DEBUG)
			Log.d(LOG_TAG, "Shown permission request snackbar");
	}


	private static void showPermissionExplanationDialog(Context context) {
		if (DEBUG)
			Log.d(LOG_TAG, "User clicked Learn More");
		// Show a dialog with more information when Learn More is clicked
		new androidx.appcompat.app.AlertDialog.Builder(context)
			.setTitle(R.string.exact_alarm_permission_title)
			.setMessage(R.string.snackbar_permission_detailed)
			.setPositiveButton(R.string.grant_permission, (dialog, which) -> {
				if (DEBUG)
					Log.d(LOG_TAG, "User clicked Grant Permission");
				try {
					de.kalass.agime.util.AlarmPermissionHelper.requestExactAlarmPermission(context);
					if (DEBUG)
						Log.d(LOG_TAG, "Successfully requested exact alarm permission");
				}
				catch (Exception e) {
					Log.e(LOG_TAG, "Error requesting exact alarm permission", e);
				}
				// Mark that we've asked
				android.preference.PreferenceManager.getDefaultSharedPreferences(context)
					.edit()
					.putBoolean(PREF_ALARM_PERMISSION_ASKED, true)
					.apply();
			})
			.setNeutralButton(R.string.never_ask_again, (d, which) -> {
				if (DEBUG)
					Log.d(LOG_TAG, "User chose to never ask again");
				android.preference.PreferenceManager.getDefaultSharedPreferences(context)
					.edit()
					.putBoolean(PREF_NEVER_ASK_AGAIN, true)
					.apply();
			})
			.setNegativeButton(android.R.string.cancel, null)
			.show();
	}


	public static AcquisitionTimes getCurrentAcquisitionTimes(Context context) {
		AcquisitionTimesProvider acquisitionTimesProvider = new DefaultAcquisitionTimesProvider(context);
		return getCurrentAcquisitionTimes(context, acquisitionTimesProvider);
	}


	/**
	 * Retrieves current acquisition times using dependency injection for better testability
	 */
	public static AcquisitionTimes getCurrentAcquisitionTimes(Context context, AcquisitionTimesProvider acquisitionTimesProvider) {
		try {
			return acquisitionTimesProvider.getCurrentAcquisitionTimes();
		}
		catch (Exception e) {
			Log.e(LOG_TAG, "Fehler beim Laden der AcquisitionTimes", e);
			return null;
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
