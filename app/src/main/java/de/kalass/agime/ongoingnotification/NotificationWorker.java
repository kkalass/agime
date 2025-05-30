package de.kalass.agime.ongoingnotification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import org.joda.time.DateTime;

import java.util.List;
import java.util.concurrent.TimeUnit;

import de.kalass.agime.AgimeIntents;
import de.kalass.agime.AgimeMainActivity;
import de.kalass.agime.R;
import de.kalass.agime.acquisitiontime.AcquisitionTimeInstance;
import de.kalass.agime.acquisitiontime.AcquisitionTimeManagementActivity;
import de.kalass.agime.acquisitiontime.AcquisitionTimes;
import de.kalass.agime.acquisitiontime.RecurringDAO;
import de.kalass.agime.loader.TrackedActivitySyncLoader;
import de.kalass.agime.model.CategoryModel;
import de.kalass.agime.model.ProjectModel;
import de.kalass.agime.model.TrackedActivityModel;
import de.kalass.agime.provider.MCContract;
import de.kalass.agime.settings.Preferences;
import de.kalass.agime.trackactivity.TrackActivity;
import de.kalass.android.common.simpleloader.CursorUtil;
import de.kalass.android.common.util.TimeFormatUtil;


/**
 * Worker für die Handhabung von Benachrichtigungen zur Zeiterfassung. Teil der Hybrid-Lösung für das
 * Benachrichtigungssystem von Agime. Der Worker ist für regelmäßige Hintergrund-Checks verantwortlich, während der
 * ShortLivedNotificationService für präzise Echtzeit-Benachrichtigungen zuständig ist.
 */
public class NotificationWorker extends Worker {

	private static final String LOG_TAG = "NotificationWorker";
	public static final int NOTIFICATION_ID = 1000;
	public static final int UP_TO_DATE_MINUTES_SINCE_ACTIVITY = 2;
	public static final long UP_TO_DATE_MILLIS_SINCE_ACTIVITY = TimeUnit.SECONDS.toMillis(UP_TO_DATE_MINUTES_SINCE_ACTIVITY * 60);

	// Konstanten für die ShortLivedNotificationService-Integration
	public static final String BACKGROUND_CHANNEL_ID = "background_monitoring_channel";

	// Legacy-Preferences für die Noise-Funktion
	public static final String PREF_NOISE_TIME_MILLIS = "notifManagingServiceCache_last_noisetime_millis";
	public static final String PREF_LAST_STARTTIME_MILLIS = "notifManagingServiceCache_last_starttime_millis";
	public static final long VALUE_MILLIS_NOT_SET = 0;

	private TrackedActivitySyncLoader trackedActivityLoader;

	public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
		super(context, params);
		trackedActivityLoader = new TrackedActivitySyncLoader(context);
	}


	@NonNull
	@Override
	public Result doWork() {
		try {
			Log.i(LOG_TAG, "NotificationWorker ausgeführt");

			// Aktuellen Status prüfen
			AcquisitionTimes times = getCurrentAcquisitionTimes();
			DateTime now = new DateTime();

			// Für aktive Erfassungszeiten prüfen, ob der ShortLivedNotificationService laufen sollte
			if (times.getCurrent() != null) {
				// Wir befinden uns in einer aktiven Erfassungsperiode
				// Prüfe, ob der kurzlebige Service aktiv ist (durch AlarmManager gesteuert)
				// Falls nicht, aktiviere ihn explizit
				if (shouldActivateShortLivedService(times.getCurrent())) {
					Log.i(LOG_TAG, "Aktive Erfassungszeit erkannt, starte ShortLivedNotificationService");
					ShortLivedNotificationService.startService(getApplicationContext());

					// Der ShortLivedNotificationService übernimmt die wichtigen Benachrichtigungen,
					// wir brauchen keine eigene Benachrichtigung anzuzeigen
					// Trotzdem planen wir die nächste Ausführung, um die Zuverlässigkeit zu erhöhen
					planNextExecution();
					return Result.success();
				}
			}

			// Für inaktive Zeiten oder wenn der ShortLivedNotificationService nicht aktiv sein sollte:
			// Standard-Benachrichtigung mit niedriger Priorität erstellen
			Notification notification = createBackgroundNotificationIfNeeded();

			if (notification != null) {
				// Als Foreground-Worker ausführen mit einer Benachrichtigung niedriger Priorität
				setForegroundAsync(new ForegroundInfo(NOTIFICATION_ID, notification));

				// Plane die nächste Ausführung
				planNextExecution();

				return Result.success();
			}
			else {
				// Keine Benachrichtigung erforderlich
				// Trotzdem die nächste Ausführung für den nächsten AcquisitionTime-Start planen
				planNextExecution();
				return Result.success();
			}
		}
		catch (Exception e) {
			Log.e(LOG_TAG, "Fehler im NotificationWorker", e);
			return Result.failure();
		}
		finally {
			if (trackedActivityLoader != null) {
				trackedActivityLoader.close();
			}
		}
	}


	/**
	 * Plant die nächste Ausführung basierend auf AcquisitionTimes. Im hybriden Ansatz koordiniert der
	 * WorkManagerController sowohl die WorkManager-Ausführungen als auch die AlarmManager-Alarme.
	 */
	private void planNextExecution() {
		// Im hybriden Ansatz delegieren wir die Planung an den WorkManagerController
		// Er wird sowohl die WorkManager-Jobs als auch die präzisen Alarme planen
		WorkManagerController.scheduleNextExecution(getApplicationContext());
	}


	/**
	 * Erstellt eine Foreground-Benachrichtigung, falls notwendig
	 */
	@Nullable
	private Notification createForegroundNotificationIfNeeded() {
		Context context = getApplicationContext();
		DateTime now = new DateTime();

		Cursor query = context.getContentResolver().query(
			RecurringDAO.CONTENT_URI, RecurringDAO.PROJECTION, null, null, null);

		List<RecurringDAO.Data> recurringItems;
		try {
			recurringItems = CursorUtil.readList(query, RecurringDAO.READ_DATA);
		}
		finally {
			if (query != null) {
				query.close();
			}
		}

		AcquisitionTimes times = AcquisitionTimes.fromRecurring(recurringItems, now);
		TrackedActivityModel lastActivity = null;
		final AcquisitionTimeInstance previous = times.getPrevious();
		final AcquisitionTimeInstance current = times.getCurrent();

		if (current != null || previous != null) {
			// Zeitbereich für die Abfrage der Aktivitäten heute
			long startTimeMillis = (current != null ? current.getStartDateTime() : previous.getStartDateTime())
				.withTimeAtStartOfDay().getMillis();

			// Hole die letzte Aktivität von heute
			List<TrackedActivityModel> activitiesToday = trackedActivityLoader.query(
				startTimeMillis,
				now.getMillis(),
				false /*keine künstlichen Einträge*/,
				MCContract.Activity.COLUMN_NAME_START_TIME + " desc");

			lastActivity = activitiesToday.size() == 0 ? null : activitiesToday.get(0);
		}

		// Prüfe, ob eine Benachrichtigung angezeigt werden soll
		if (needsForegroundNotification(now, times, lastActivity)) {
			// Erstelle die Benachrichtigung
			return createForegroundNotification(times, now, lastActivity);
		}

		return null;
	}


	/**
	 * Prüft, ob eine Foreground-Benachrichtigung angezeigt werden soll
	 */
	private boolean needsForegroundNotification(DateTime now, AcquisitionTimes times, TrackedActivityModel lastActivity) {
		return isUnfinishedAcquisitionTime(now, times, lastActivity) && !isPermanentlyHidden();
	}


	/**
	 * Prüft, ob eine unabgeschlossene Zeiterfassungsperiode existiert
	 */
	private boolean isUnfinishedAcquisitionTime(DateTime now, AcquisitionTimes times, TrackedActivityModel lastActivity) {
		if (times.getCurrent() != null) {
			return true;
		}

		final AcquisitionTimeInstance previous = times.getPrevious();
		if (previous == null) {
			return false;
		}

		// Stelle sicher, dass wir den Benutzer nicht unbegrenzt lange belästigen
		if (previous.getEndDateTime().plusMinutes(AcquisitionTimeInstance.ACQUISITION_TIME_END_THRESHOLD_MINUTES).isBeforeNow()) {
			return false;
		}

		if (lastActivity == null) {
			return true;
		}

		// Die letzte erfasste Aktivität endete vor dem Ende der vorherigen Erfassungszeit
		return lastActivity.getEndTimeMillis() < previous.getEndDateTime().getMillis();
	}


	/**
	 * Erstellt eine Foreground-Benachrichtigung
	 */
	private Notification createForegroundNotification(AcquisitionTimes times, DateTime now, TrackedActivityModel lastActivity) {
		Context context = getApplicationContext();

		final String channel;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			channel = createChannel();
		}
		else {
			channel = "";
		}

		NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channel);
		PendingIntent mainActivityIntent = createMainActivityIntent(context);
		builder.setContentIntent(mainActivityIntent);

		builder.setSmallIcon(R.drawable.ic_notif);
		Bitmap bm = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher);
		builder.setLargeIcon(bm);
		builder.setOngoing(true);

		// Inhalt der Benachrichtigung basierend auf dem aktuellen Zustand
		if (times.getCurrent() == null) {
			if (!isUnfinishedAcquisitionTime(now, times, lastActivity)) {
				throw new IllegalStateException("Keine Foreground-Benachrichtigung benötigt");
			}

			final AcquisitionTimeInstance previous = Preconditions.checkNotNull(times.getPrevious());
			long startTimeMillis = lastActivity == null ? previous.getStartDateTime().getMillis() : lastActivity.getEndTimeMillis();
			builder.setWhen(startTimeMillis);
			builder.setUsesChronometer(true);

			builder.setContentTitle(context.getString(R.string.action_last_item_of_day_heading_unfinished));

			if (lastActivity != null) {
				CharSequence durationLast = TimeFormatUtil.formatDuration(context, lastActivity.getEndTimeMillis() - lastActivity.getStartTimeMillis());
				builder.setContentText(context.getString(R.string.ongoing_notif_after_acquisition_with_previous_activity_content_text,
					lastActivity.getDisplayName(context)));
				builder.setContentInfo(context.getString(R.string.ongoing_notif_after_acquisition_with_previous_activity_content_info,
					durationLast));
			}
			else {
				builder.setContentText(context.getString(R.string.ongoing_notif_after_acquisition_without_previous_activity_content_text,
					TimeFormatUtil.formatTime(context, startTimeMillis)));
			}

			builder.addAction(R.drawable.ic_action_add,
				context.getResources().getString(R.string.action_tracktime_new_notification),
				createTrackActivityIntent(context));
			builder.addAction(android.R.drawable.ic_menu_preferences,
				context.getResources().getString(R.string.action_tracktime_notification_settings),
				createAcquisitionTimesIntent(context));

			handleNoise(builder, startTimeMillis);
		}
		else if (lastActivity == null) {
			// Erste Aktivität des Tages
			long startTimeMillis = times.getCurrent().getStartDateTime().getMillis();
			builder.setWhen(startTimeMillis);
			builder.setUsesChronometer(true);

			builder.setContentText(context.getString(R.string.ongoing_notif_in_acquisition_first_activity_content_text,
				TimeFormatUtil.formatTime(context, startTimeMillis)));
			builder.setContentTitle(context.getString(R.string.ongoing_notif_in_acquisition_first_activity_content_title));

			builder.addAction(R.drawable.ic_action_add,
				context.getResources().getString(R.string.action_tracktime_new_notification),
				createTrackActivityIntent(context));
			builder.addAction(android.R.drawable.ic_menu_preferences,
				context.getResources().getString(R.string.action_tracktime_notification_settings),
				createAcquisitionTimesIntent(context));

			handleNoise(builder, startTimeMillis);
		}
		else {
			// Mit vorheriger Aktivität
			CharSequence durationLast = TimeFormatUtil.formatDuration(context,
				lastActivity.getEndTimeMillis() - lastActivity.getStartTimeMillis());
			long millisSinceLast = now.getMillis() - lastActivity.getEndTimeMillis();

			if (millisSinceLast < UP_TO_DATE_MILLIS_SINCE_ACTIVITY) {
				// Zeige Benachrichtigung für aktuelle Aktivität
				CategoryModel category = lastActivity.getCategory();
				String categoryName = category == null ? null : category.getName();
				ProjectModel project = lastActivity.getProject();
				String projectName = project == null ? null : project.getName();

				String categoryProjectLine = !Strings.isNullOrEmpty(categoryName) ? categoryName : "";
				if (!Strings.isNullOrEmpty(projectName)) {
					if (!Strings.isNullOrEmpty(categoryProjectLine)) {
						categoryProjectLine += " - ";
					}
					categoryProjectLine += projectName;
				}

				builder.setWhen(lastActivity.getEndTimeMillis());
				builder.setUsesChronometer(false);

				builder.setContentTitle(context.getString(R.string.ongoing_notif_in_acquisition_next_activity_within_threshold_content_title,
					lastActivity.getDisplayName(context)));
				builder.setContentText(categoryProjectLine);
			}
			else {
				// Frage den Benutzer, was er getan hat
				builder.setWhen(lastActivity.getEndTimeMillis());
				builder.setUsesChronometer(true);

				builder.setContentTitle(context.getString(R.string.ongoing_notif_in_acquisition_next_activity_content_title));
				builder.setContentText(context.getString(R.string.ongoing_notif_in_acquisition_next_activity_content_text,
					lastActivity.getDisplayName(context)));
				builder.setContentInfo(context.getString(R.string.ongoing_notif_in_acquisition_next_activity_content_info, durationLast));
			}

			builder.addAction(R.drawable.ic_action_add,
				context.getResources().getString(R.string.action_tracktime_new_notification),
				createTrackActivityIntent(context));
			builder.addAction(android.R.drawable.ic_menu_preferences,
				context.getResources().getString(R.string.action_tracktime_notification_settings),
				createAcquisitionTimesIntent(context));

			handleNoise(builder, lastActivity.getEndTimeMillis());
		}

		return builder.build();
	}


	/**
	 * Erstellt einen Notification Channel (ab Android O erforderlich)
	 */
	@NonNull
	private String createChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationManager mNotificationManager = (NotificationManager)getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

			String name = "Agime";
			String description = "Agime Benachrichtigungen";
			int importance = NotificationManager.IMPORTANCE_LOW;

			NotificationChannel mChannel = new NotificationChannel("de.kalass.agime", name, importance);
			mChannel.setDescription(description);
			mChannel.enableLights(true);
			mChannel.setLightColor(Color.BLUE);

			if (mNotificationManager != null) {
				mNotificationManager.createNotificationChannel(mChannel);
			}

			return mChannel.getId();
		}
		return "";
	}


	/**
	 * Fügt Benachrichtigungssignale (Noise) hinzu, wenn erforderlich
	 */
	private void handleNoise(NotificationCompat.Builder builder, long startTimeMillis) {
		long nowMillis = System.currentTimeMillis();

		// Noise-Signale hinzufügen, falls erforderlich
		addNoiseToNotificationIfNeeded(builder, startTimeMillis, nowMillis);

		final int minutes = Preferences.getAcquisitionTimeNotificationNoiseThresholdMinutes(getApplicationContext());
		if (minutes <= 0) {
			// Keine extra Noise-Benachrichtigungen angefordert
			return;
		}

		long makeNoiseAtMillis = minutes <= 0 ? -1 : minutesToMillis(minutes) + startTimeMillis;
		while(makeNoiseAtMillis < nowMillis) {
			makeNoiseAtMillis += minutesToMillis(minutes);
		}

		PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
			.edit()
			.putLong(PREF_LAST_STARTTIME_MILLIS, startTimeMillis)
			.putLong(PREF_NOISE_TIME_MILLIS, makeNoiseAtMillis)
			.apply();
	}


	/**
	 * Fügt Benachrichtigungssignale hinzu, wenn bestimmte Bedingungen erfüllt sind
	 */
	private void addNoiseToNotificationIfNeeded(NotificationCompat.Builder builder, long startTimeMillis, long nowMillis) {
		final long lastStartTimeMillis = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
			.getLong(PREF_LAST_STARTTIME_MILLIS, VALUE_MILLIS_NOT_SET);
		final long lastNoiseTimeMillis = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
			.getLong(PREF_NOISE_TIME_MILLIS, VALUE_MILLIS_NOT_SET);

		final boolean isNotAfterLastStartTime = lastStartTimeMillis != VALUE_MILLIS_NOT_SET &&
				startTimeMillis <= lastStartTimeMillis;
		final boolean isAfterNoiseTime = lastNoiseTimeMillis != VALUE_MILLIS_NOT_SET && nowMillis >= lastNoiseTimeMillis;

		if (isNotAfterLastStartTime && (lastNoiseTimeMillis == VALUE_MILLIS_NOT_SET || isAfterNoiseTime)) {
			builder.setContentTitle(getApplicationContext().getString(R.string.ongoing_notif_in_long_after_content_title));
			if (isAfterNoiseTime) {
				builder.setDefaults(Notification.DEFAULT_ALL);
				PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
					.edit()
					.putLong(PREF_NOISE_TIME_MILLIS, VALUE_MILLIS_NOT_SET)
					.apply();
			}
		}
	}


	/**
	 * Erzeugt einen PendingIntent für die Hauptaktivität
	 */
	private PendingIntent createMainActivityIntent(Context context) {
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
		stackBuilder.addParentStack(AgimeMainActivity.class);
		stackBuilder.addNextIntent(new Intent(context, AgimeMainActivity.class));

		return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
	}


	/**
	 * Erzeugt einen PendingIntent für die TrackActivity
	 */
	private PendingIntent createTrackActivityIntent(Context context) {
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
		stackBuilder.addParentStack(TrackActivity.class);
		final Intent intent = new Intent(Intent.ACTION_INSERT, MCContract.Activity.CONTENT_URI);
		intent.setClass(context, TrackActivity.class);
		stackBuilder.addNextIntent(intent);

		return stackBuilder.getPendingIntent(1, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
	}


	/**
	 * Erzeugt einen PendingIntent für die AcquisitionTimeManagementActivity
	 */
	private PendingIntent createAcquisitionTimesIntent(Context context) {
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
		stackBuilder.addParentStack(AcquisitionTimeManagementActivity.class);
		stackBuilder.addNextIntent(new Intent(context, AcquisitionTimeManagementActivity.class));

		return stackBuilder.getPendingIntent(2, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
	}


	/**
	 * Prüft, ob die Benachrichtigungen dauerhaft ausgeblendet sind
	 */
	private boolean isPermanentlyHidden() {
		return !Preferences.isAcquisitionTimeNotificationEnabled(getApplicationContext());
	}


	/**
	 * Wandelt Millisekunden in Minuten um
	 */
	private long millisToMinutes(long millis) {
		return millis / 60000;
	}


	/**
	 * Wandelt Minuten in Millisekunden um
	 */
	private long minutesToMillis(long minutes) {
		return minutes * 60000;
	}


	/**
	 * Prüft, ob der ShortLivedNotificationService für diese Erfassungszeit aktiviert werden sollte. Wir möchten den
	 * Service nur dann aktivieren, wenn wir uns in einer aktiven Erfassungszeit befinden.
	 */
	private boolean shouldActivateShortLivedService(AcquisitionTimeInstance current) {
		if (current == null) {
			return false;
		}

		// Wir befinden uns in einer aktiven Erfassungszeit - starten
		DateTime now = new DateTime();
		return now.isAfter(current.getStartDateTime()) && now.isBefore(current.getEndDateTime());
	}


	/**
	 * Erstellt eine Hintergrund-Benachrichtigung mit niedriger Priorität, die vom WorkManager verwendet wird, wenn keine
	 * aktive Zeiterfassung stattfindet.
	 */
	@Nullable
	private Notification createBackgroundNotificationIfNeeded() {
		// Diese Methode ähnelt createForegroundNotificationIfNeeded(), aber erstellt
		// eine Benachrichtigung mit geringerer Priorität und ohne Chronometer
		AcquisitionTimes times = getCurrentAcquisitionTimes();
		DateTime now = new DateTime();

		// Wenn keine aktive oder vorherige Erfassungszeit existiert, keine Benachrichtigung anzeigen
		if (times.getCurrent() == null && times.getPrevious() == null) {
			return null;
		}

		// Letzte Aktivität abrufen
		TrackedActivityModel lastActivity = getLastActivity(times);

		// Keine Benachrichtigung notwendig, wenn keine unvollständige Zeiterfassung vorliegt
		if (!needsForegroundNotification(now, times, lastActivity)) {
			return null;
		}

		// Erstelle einen einfachen Notification-Channel für Hintergrundbenachrichtigungen
		final String channelId = createBackgroundNotificationChannel();

		NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelId)
			.setSmallIcon(R.drawable.ic_notif)
			.setContentTitle(getApplicationContext().getString(R.string.app_name))
			.setContentText(getApplicationContext().getString(R.string.ongoing_notif_background_monitoring))
			.setPriority(NotificationCompat.PRIORITY_LOW)
			.setOngoing(true);

		// ContentIntent für die Hauptaktivität
		PendingIntent mainActivityIntent = createMainActivityIntent(getApplicationContext());
		builder.setContentIntent(mainActivityIntent);

		// Aktionen hinzufügen
		builder.addAction(R.drawable.ic_action_add,
			getApplicationContext().getResources().getString(R.string.action_tracktime_new_notification),
			createTrackActivityIntent(getApplicationContext()));

		return builder.build();
	}


	/**
	 * Erstellt einen Benachrichtigungskanal mit niedriger Priorität für Hintergrundbenachrichtigungen
	 */
	private String createBackgroundNotificationChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationManager notificationManager = (NotificationManager)getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

			NotificationChannel existingChannel = notificationManager.getNotificationChannel(BACKGROUND_CHANNEL_ID);
			if (existingChannel != null) {
				return BACKGROUND_CHANNEL_ID;
			}

			NotificationChannel channel = new NotificationChannel(
					BACKGROUND_CHANNEL_ID,
					"Agime Hintergrundüberwachung",
					NotificationManager.IMPORTANCE_LOW);

			channel.setDescription("Niedrigprioritäre Benachrichtigungen außerhalb aktiver Zeiterfassung");
			channel.enableLights(false);
			channel.setSound(null, null);
			channel.enableVibration(false);

			notificationManager.createNotificationChannel(channel);
			return BACKGROUND_CHANNEL_ID;
		}
		return "";
	}


	/**
	 * Ruft die letzte Aktivität ab, basierend auf den aktuellen AcquisitionTimes
	 */
	private TrackedActivityModel getLastActivity(AcquisitionTimes times) {
		final AcquisitionTimeInstance previous = times.getPrevious();
		final AcquisitionTimeInstance current = times.getCurrent();

		if (current == null && previous == null) {
			return null;
		}

		// Zeitbereich für die Abfrage der Aktivitäten heute
		long startTimeMillis = (current != null ? current.getStartDateTime() : previous.getStartDateTime())
			.withTimeAtStartOfDay().getMillis();

		// Hole die letzte Aktivität von heute
		List<TrackedActivityModel> activitiesToday = trackedActivityLoader.query(
			startTimeMillis,
			new DateTime().getMillis(),
			false,
			MCContract.Activity.COLUMN_NAME_START_TIME + " desc");

		return activitiesToday.size() == 0 ? null : activitiesToday.get(0);
	}


	/**
	 * Lädt die aktuellen AcquisitionTimes
	 */
	private AcquisitionTimes getCurrentAcquisitionTimes() {
		Cursor query = getApplicationContext().getContentResolver().query(
			RecurringDAO.CONTENT_URI, RecurringDAO.PROJECTION, null, null, null);

		List<RecurringDAO.Data> recurringItems;
		try {
			recurringItems = CursorUtil.readList(query, RecurringDAO.READ_DATA);
			return AcquisitionTimes.fromRecurring(recurringItems, new DateTime());
		}
		finally {
			if (query != null) {
				query.close();
			}
		}
	}
}
