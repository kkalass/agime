package de.kalass.agime.ongoingnotification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

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
import de.kalass.agime.model.TrackedActivityModel;
import de.kalass.agime.provider.MCContract;
import de.kalass.agime.settings.Preferences;
import de.kalass.agime.trackactivity.TrackActivity;
import de.kalass.android.common.simpleloader.CursorUtil;
import de.kalass.android.common.util.TimeFormatUtil;


/**
 * Ein kurzlebiger Vordergrund-Service, der hochpräzise Benachrichtigungen während aktiver Zeiterfassungsphasen
 * bereitstellt. Dieser Service wird nur für kurze Zeit gestartet und stoppt sich selbst, um den Android
 * 15-Beschränkungen zu entsprechen.
 */
public class ShortLivedNotificationService extends Service {

	private static final String TAG = "ShortLivedNotifService";
	public static final int NOTIFICATION_ID = 1001;
	public static final String ACTIVE_CHANNEL_ID = "active_time_tracking_channel";
	public static final String EXTRA_MAX_RUNTIME_MINUTES = "extra_max_runtime_minutes";
	public static final int DEFAULT_MAX_RUNTIME_MINUTES = 10;
	public static final int UPDATE_INTERVAL_MS = 5000; // 5 Sekunden

	private final Handler handler = new Handler(Looper.getMainLooper());
	private TrackedActivitySyncLoader trackedActivityLoader;
	private AcquisitionTimes currentTimes;
	private TrackedActivityModel lastActivity;
	private long serviceStopTime;

	@Override
	public void onCreate() {
		super.onCreate();
		trackedActivityLoader = new TrackedActivitySyncLoader(this);
		Log.i(TAG, "ShortLivedNotificationService erstellt");
	}


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		int maxRuntimeMinutes = intent != null ? intent.getIntExtra(EXTRA_MAX_RUNTIME_MINUTES, DEFAULT_MAX_RUNTIME_MINUTES) : DEFAULT_MAX_RUNTIME_MINUTES;

		Log.i(TAG, "ShortLivedNotificationService gestartet, maximale Laufzeit: " + maxRuntimeMinutes + " Minuten");

		// Aktuelle Erfassungszeiten und letzte Aktivität abrufen
		loadCurrentState();

		// Start im Vordergrund mit der aktuellen Benachrichtigung
		Notification notification = createActiveTimeNotification();
		startForeground(NOTIFICATION_ID, notification);

		// Regelmäßige Updates für die Benachrichtigung während der aktiven Phase
		scheduleNotificationUpdates();

		// Service stoppt sich selbst nach der maximalen Laufzeit
		serviceStopTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(maxRuntimeMinutes);
		scheduleServiceStop(maxRuntimeMinutes);

		return START_STICKY;
	}


	@Override
	public void onDestroy() {
		Log.i(TAG, "ShortLivedNotificationService wird beendet");
		handler.removeCallbacksAndMessages(null);
		if (trackedActivityLoader != null) {
			trackedActivityLoader.close();
			trackedActivityLoader = null;
		}
		super.onDestroy();
	}


	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}


	/**
	 * Lädt die aktuellen Erfassungszeiten und die letzte Aktivität
	 */
	private void loadCurrentState() {
		DateTime now = new DateTime();

		// Erfassungszeiten laden
		Cursor query = getContentResolver().query(
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

		currentTimes = AcquisitionTimes.fromRecurring(recurringItems, now);

		// Letzte Aktivität laden, wenn verfügbar
		AcquisitionTimeInstance current = currentTimes.getCurrent();
		AcquisitionTimeInstance previous = currentTimes.getPrevious();

		if (current != null || previous != null) {
			long startTimeMillis = (current != null ? current.getStartDateTime() : previous.getStartDateTime())
				.withTimeAtStartOfDay().getMillis();

			List<TrackedActivityModel> activitiesToday = trackedActivityLoader.query(
				startTimeMillis,
				now.getMillis(),
				false,
				MCContract.Activity.COLUMN_NAME_START_TIME + " desc");

			lastActivity = activitiesToday.size() == 0 ? null : activitiesToday.get(0);
		}
	}


	/**
	 * Plant regelmäßige Updates der Benachrichtigung
	 */
	private void scheduleNotificationUpdates() {
		handler.postDelayed(new Runnable() {

			@Override
			public void run() {
				if (System.currentTimeMillis() < serviceStopTime) {
					updateNotification();
					handler.postDelayed(this, UPDATE_INTERVAL_MS);
				}
			}
		}, UPDATE_INTERVAL_MS);
	}


	/**
	 * Aktualisiert die Benachrichtigung mit den neuesten Daten
	 */
	private void updateNotification() {
		loadCurrentState();
		Notification notification = createActiveTimeNotification();
		NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(NOTIFICATION_ID, notification);
	}


	/**
	 * Plant das Stoppen des Services nach der angegebenen Zeit
	 */
	private void scheduleServiceStop(int maxRuntimeMinutes) {
		handler.postDelayed(new Runnable() {

			@Override
			public void run() {
				Log.i(TAG, "ShortLivedNotificationService hat maximale Laufzeit erreicht, wird beendet");
				stopSelf();

				// WorkManager für weitere Updates benachrichtigen
				WorkManagerController.scheduleImmediateCheck(getApplicationContext());
			}
		}, TimeUnit.MINUTES.toMillis(maxRuntimeMinutes));
	}


	/**
	 * Erstellt eine Benachrichtigung mit hoher Priorität für aktive Zeiterfassungsphasen
	 */
	private Notification createActiveTimeNotification() {
		final String channelId = createNotificationChannel();

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
			.setSmallIcon(R.drawable.ic_notif)
			.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
			.setOngoing(true)
			.setPriority(NotificationCompat.PRIORITY_HIGH);

		// ContentIntent für die Hauptansicht
		PendingIntent mainActivityIntent = createMainActivityIntent();
		builder.setContentIntent(mainActivityIntent);

		DateTime now = new DateTime();

		if (currentTimes.getCurrent() == null) {
			// Keine aktive Erfassungszeit mehr, aber vermutlich unvollständige letzte Aktivität
			if (lastActivity != null) {
				long startTimeMillis = lastActivity.getEndTimeMillis();
				builder.setWhen(startTimeMillis);
				builder.setUsesChronometer(true);
				builder.setContentTitle(getString(R.string.action_last_item_of_day_heading_unfinished));

				CharSequence durationLast = TimeFormatUtil.formatDuration(this,
					lastActivity.getEndTimeMillis() - lastActivity.getStartTimeMillis());
				builder.setContentText(getString(R.string.ongoing_notif_after_acquisition_with_previous_activity_content_text,
					lastActivity.getDisplayName(this)));
				builder.setContentInfo(getString(R.string.ongoing_notif_after_acquisition_with_previous_activity_content_info,
					durationLast));
			}
		}
		else if (lastActivity == null) {
			// Erste Aktivität des Tages (noch keine vorhanden)
			long startTimeMillis = currentTimes.getCurrent().getStartDateTime().getMillis();
			builder.setWhen(startTimeMillis);
			builder.setUsesChronometer(true);

			builder.setContentTitle(getString(R.string.ongoing_notif_in_acquisition_first_activity_content_title));
			builder.setContentText(getString(R.string.ongoing_notif_in_acquisition_first_activity_content_text,
				TimeFormatUtil.formatTime(this, startTimeMillis)));
		}
		else {
			// Mit vorheriger Aktivität
			CharSequence durationLast = TimeFormatUtil.formatDuration(this,
				lastActivity.getEndTimeMillis() - lastActivity.getStartTimeMillis());
			long millisSinceLast = now.getMillis() - lastActivity.getEndTimeMillis();

			builder.setWhen(lastActivity.getEndTimeMillis());
			builder.setUsesChronometer(true);

			builder.setContentTitle(getString(R.string.ongoing_notif_in_acquisition_next_activity_content_title));
			builder.setContentText(getString(R.string.ongoing_notif_in_acquisition_next_activity_content_text,
				lastActivity.getDisplayName(this)));
			builder.setContentInfo(getString(R.string.ongoing_notif_in_acquisition_next_activity_content_info,
				durationLast));
		}

		// Actions hinzufügen
		builder.addAction(R.drawable.ic_action_add,
			getResources().getString(R.string.action_tracktime_new_notification),
			createTrackActivityIntent());
		builder.addAction(android.R.drawable.ic_menu_preferences,
			getResources().getString(R.string.action_tracktime_notification_settings),
			createAcquisitionTimesIntent());

		return builder.build();
	}


	/**
	 * Erstellt den hochprioritären Notification Channel für aktive Zeiterfassung
	 */
	private String createNotificationChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

			NotificationChannel channel = new NotificationChannel(
					ACTIVE_CHANNEL_ID,
					"Aktive Zeiterfassung",
					NotificationManager.IMPORTANCE_HIGH);

			channel.setDescription("Hochprioritäre Benachrichtigungen während aktiver Zeiterfassung");
			channel.enableLights(true);
			channel.setLightColor(Color.BLUE);
			channel.enableVibration(false);
			channel.setSound(null, null);

			notificationManager.createNotificationChannel(channel);
			return ACTIVE_CHANNEL_ID;
		}
		return "";
	}


	/**
	 * Erzeugt einen PendingIntent für die Hauptaktivität
	 */
	private PendingIntent createMainActivityIntent() {
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		stackBuilder.addParentStack(AgimeMainActivity.class);
		stackBuilder.addNextIntent(new Intent(this, AgimeMainActivity.class));

		return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
	}


	/**
	 * Erzeugt einen PendingIntent für die TrackActivity
	 */
	private PendingIntent createTrackActivityIntent() {
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		stackBuilder.addParentStack(TrackActivity.class);
		final Intent intent = new Intent(Intent.ACTION_INSERT, MCContract.Activity.CONTENT_URI);
		intent.setClass(this, TrackActivity.class);
		stackBuilder.addNextIntent(intent);

		return stackBuilder.getPendingIntent(1, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
	}


	/**
	 * Erzeugt einen PendingIntent für die AcquisitionTimeManagementActivity
	 */
	private PendingIntent createAcquisitionTimesIntent() {
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		stackBuilder.addParentStack(AcquisitionTimeManagementActivity.class);
		stackBuilder.addNextIntent(new Intent(this, AcquisitionTimeManagementActivity.class));

		return stackBuilder.getPendingIntent(2, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
	}


	/**
	 * Startet den Service mit Standardparametern
	 */
	public static void startService(Context context) {
		startService(context, DEFAULT_MAX_RUNTIME_MINUTES);
	}


	/**
	 * Startet den Service mit angegebener maximaler Laufzeit
	 */
	public static void startService(Context context, int maxRuntimeMinutes) {
		Intent intent = new Intent(context, ShortLivedNotificationService.class);
		intent.putExtra(EXTRA_MAX_RUNTIME_MINUTES, maxRuntimeMinutes);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			context.startForegroundService(intent);
		}
		else {
			context.startService(intent);
		}
	}
}
