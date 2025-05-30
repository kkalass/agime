package de.kalass.agime.ongoingnotification;

import android.content.Context;
import android.content.Intent;
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

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import de.kalass.agime.AgimeIntents;


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

	/**
	 * Initialisiert den WorkManager bei App-Start oder nach Geräteneustart
	 */
	public static void initialize(Context context) {
		Log.i(LOG_TAG, "WorkManagerController wird initialisiert");

		// Sofortige Ausführung planen
		scheduleImmediateCheck(context);

		// Regelmäßige Überprüfungen planen
		schedulePeriodicChecks(context);
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
		// aber hier könnten spezifischere Ausführungszeiten basierend auf AcquisitionTimes geplant werden

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
}
