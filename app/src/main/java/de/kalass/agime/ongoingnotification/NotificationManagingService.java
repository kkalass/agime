package de.kalass.agime.ongoingnotification;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.RateLimiter;

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
import de.kalass.android.common.support.AlarmManagerSupport;
import de.kalass.android.common.support.HandlerThreadSupport;
import de.kalass.android.common.util.TimeFormatUtil;

/**
 * A Service that manages
 */
public class NotificationManagingService extends Service {
    public static final boolean DISABLE_FOREGROUND_SERVICE = false;
    public static final int NOTIFICATION_ID = 1000;
    private static final String LOG_TAG = "NotifManagingService";
    public static final int UP_TO_DATE_MINUTES_SINCE_ACTIVITY = 2;
    public static final long UP_TO_DATE_MILLIS_SINCE_ACTIVITY = TimeUnit.SECONDS.toMillis(UP_TO_DATE_MINUTES_SINCE_ACTIVITY*60);

    private static final int FLAG_UNSUPPRESS_NOTIFICATION = 1;
    private static final int FLAG_SUPPRESS_NOTIFICATION = 2;
    public static final String PREF_NOISE_TIME_MILLIS = "notifManagingServiceCache_last_noisetime_millis";
    public static final String PREF_LAST_STARTTIME_MILLIS = "notifManagingServiceCache_last_starttime_millis";
    public static final long VALUE_MILLIS_NOT_SET = 0;

    private BackgroundService _serviceHandler;
    private HandlerThread _thread;


    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    private long _instanceId = System.currentTimeMillis();
    private ContentObserver _activityObserver;
    private ContentObserver _trackedActivityLoaderObserver;

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        NotificationManagingService getService() {
            // Return this instance of LocalService so clients can call public methods
            return NotificationManagingService.this;
        }
    }

    // Handler that receives messages from the thread and executes
    // the messages in a separate thread.

    private final class BackgroundService extends Handler {

        private final TrackedActivitySyncLoader _trackedActivityLoader;
        private boolean _suppressNotification = false;


        public BackgroundService(Looper looper) {
            super(looper);
            _trackedActivityLoader = new TrackedActivitySyncLoader(getContext());
        }

        public void  cleanup() {
            _trackedActivityLoader.setContentObserver(null);
            _trackedActivityLoader.close();
        }

        @Override
        public void handleMessage(Message msg) {
            if (DISABLE_FOREGROUND_SERVICE) {
                return;
            }
            // the trigger could be wrong/untimely, so we ignore the type of trigger
            // and update the notification and scheduling according to the actual state
            int startId = msg.arg1;
            if (msg.arg2 == FLAG_SUPPRESS_NOTIFICATION) {
                _suppressNotification = true;
            } else if (msg.arg2 == FLAG_UNSUPPRESS_NOTIFICATION){
                _suppressNotification = false;
            }

            // intent will be null if an observed change triggers the refresh
            Intent intent = (Intent)msg.obj;
            if (isPermanentlyHidden()) {
                Log.v(LOG_TAG, "notification permanently hidden");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE);
                } else {
                    stopForeground(true);
                }
                stopSelf();
                if (intent != null) {
                    // release the wakelock if applicable
                    OngoingNotificationManagingReceiver.completeWakefulIntent(intent);
                }
                return;
            }

            Log.v(LOG_TAG, "updating ongoing notification");
            Notification fgNotification = createForegroundNotificationIfNeeded();
            if (fgNotification != null) {
                if (_suppressNotification) {
                    stopForeground(true);
                } else {
                    startForeground(NOTIFICATION_ID, fgNotification);
                }
            } else {
                stopForeground(true);
                // only stop this service if this is *not* a foreground service any more
                if (intent == null) {
                    // triggered through an observed data change
                    stopSelf();
                } else {
                    stopSelf(startId);
                }
            }

            if (intent != null) {
                // release the wakelock if applicable
                OngoingNotificationManagingReceiver.completeWakefulIntent(intent);
            }
        }


        /**
         *
         * @return false if the notification is not foreground
         */
        private Notification createForegroundNotificationIfNeeded() {

            Context context = getContext();
            DateTime now = new DateTime();

            Cursor query = getContentResolver().query(RecurringDAO.CONTENT_URI, RecurringDAO.PROJECTION, null, null, null);

            List<RecurringDAO.Data> recurringItems;
            try {
                recurringItems = CursorUtil.readList(query, RecurringDAO.READ_DATA);
            } finally {
                if (query != null) {
                    query.close();
                }
            }
            AcquisitionTimes times = AcquisitionTimes.fromRecurring(recurringItems, now);
            TrackedActivityModel lastActivity = null;
            final AcquisitionTimeInstance previous = times.getPrevious();
            final AcquisitionTimeInstance current = times.getCurrent();
            if (current != null || previous != null) {
                // the suggested starttime for new entries will not respect the acquisition
                // start time, but use the endtime of the previous activity on the same day
                // so, we should rather mimic this behaviour here

                long startTimeMillis = (current != null ? current.getStartDateTime() : previous.getStartDateTime())
                        .withTimeAtStartOfDay().getMillis();
                List<TrackedActivityModel> actvitiesToday = _trackedActivityLoader.query(
                        startTimeMillis,
                        now.getMillis(),
                        false /*no fake entries*/,
                        MCContract.Activity.COLUMN_NAME_START_TIME + " desc");
                lastActivity = actvitiesToday.size() == 0 ? null : actvitiesToday.get(0);
            }
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            am.cancel(createPendingIntent(context, AgimeIntents.ACTION_REFRESH_ACQUISITION_TIME_NOTIFICATION));
            if (needsForegroundNotification(now, times, lastActivity)) {
                // currently running
                return createForegroundNotificationAndInstallTimer(am, times, now, lastActivity);
            } else {
                onStopAcquisitionTime(am, times);
                return null;
            }
        }

        private boolean needsForegroundNotification(
                DateTime now, AcquisitionTimes times, TrackedActivityModel lastActivity
        ) {
            return isUnfinishedAcquisitionTime(now, times, lastActivity);
        }

        private boolean isUnfinishedAcquisitionTime(DateTime now, AcquisitionTimes times, TrackedActivityModel lastActivity) {
            if (times.getCurrent() != null) {
                return true;
            }
            final AcquisitionTimeInstance previous = times.getPrevious();
            if (previous == null) {
                return false;
            }
            // ensure that we do not bother the user for unlimited amount of time
            if (previous.getEndDateTime().plusMinutes(AcquisitionTimeInstance.ACQUISITION_TIME_END_THRESHOLD_MINUTES).isBeforeNow()) {
                return false;
            }
            if (lastActivity == null) {
                return true;
            }
            // the last tracked activity has ended before the end of the previous acquisition time
            // FIXME: Currently, the user is constantly notified/reminded that he/she did not fill
            //        the acquisition time completely with activities. BUT: This is not always needed / expected.
            //
            // How do I want it to be? What is wrong/What is right?
            // Problems:
            //   - Notification does not even go away after midnight - this seems strange to many
            //   - End? Notification should at least have 'Done' Shortcut
            //   - Would be good if End? notification was not sticky
            //   - End? Toolbar should probably have 'Done' less hidden?
            //   - Implicit Acquisition Time creation is sometimes confusing, especially if the user adds entries afterwards/in the evening
            //
            // Easy solution: remove End? Notification.
            // Drawback:
            //   - If a user uses Agime in a timely fashion, he/she will not be reminded of timely recording in the evening
            //     -> I do not want that either!
            //
            // what is the solution? A u
            //   - fix shortcut for prolonging activities until now for selected activities
            //   - In End? State: do not show 'prolong', but 'End acquisition time' instead
            //   - Notification: remove foreground flag for those notifications -> how can I make this end acquisition time?
            //     Alternative: include a threshold for 'End?' state
            //   - Notification: refresh on/after midnight
            //
            // BUT: Takes too long to implement now. Quickfix was implemented: limit the time for 'End?' state and make sure to refresh
            return lastActivity.getEndTimeMillis() < previous.getEndDateTime().getMillis();
        }

        private PendingIntent createPendingIntent(Context context, String action) {
            Intent intent = new Intent(context, OngoingNotificationManagingReceiver.class);
            intent.setAction(action);
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_ONE_SHOT|PendingIntent.FLAG_IMMUTABLE);
        }

        private void onStopAcquisitionTime(AlarmManager am, AcquisitionTimes times) {
            Context context = getContext();
            AcquisitionTimeInstance next = times.getNext();
            if (next != null) {
                PendingIntent pendingIntent = createPendingIntent(context, AgimeIntents.ACTION_REFRESH_ACQUISITION_TIME_NOTIFICATION);
                am.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        next.getStartDateTime().getMillis(), pendingIntent);
            }
        }

        private Notification createForegroundNotificationAndInstallTimer(
                AlarmManager am,
                AcquisitionTimes times,
                DateTime now,
                TrackedActivityModel lastActivity
        ) {
            Context context = getContext();
            AcquisitionTimeInstance current = times.getCurrent();
            PendingIntent pendingIntent = createPendingIntent(context, AgimeIntents.ACTION_REFRESH_ACQUISITION_TIME_NOTIFICATION);
            if (current != null) {
                AlarmManagerSupport.setAlarm(am,
                        AlarmManager.RTC_WAKEUP,
                        current.getEndDateTime().getMillis(), pendingIntent);
            } else {
                AcquisitionTimeInstance previous = times.getPrevious();

                if (previous != null) {
                    DateTime notificationRefreshTime = previous.getEndDateTime().plusMinutes(AcquisitionTimeInstance.ACQUISITION_TIME_END_THRESHOLD_MINUTES).plusMillis(500);
                    AlarmManagerSupport.setAlarm(am,
                            AlarmManager.RTC_WAKEUP,
                            notificationRefreshTime.getMillis(), pendingIntent);
                }
            }
            return createForegroundNotification(am, times, now, lastActivity, context, pendingIntent);
        }

        @NonNull
        @TargetApi(26)
        private synchronized String createChannel() {
            NotificationManager mNotificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);

            String name = "Agime";
            int importance = NotificationManager.IMPORTANCE_LOW;

            NotificationChannel mChannel = new NotificationChannel("de.kalass.agime", name, importance);

            mChannel.enableLights(true);
            mChannel.setLightColor(Color.BLUE);
            if (mNotificationManager != null) {
                mNotificationManager.createNotificationChannel(mChannel);
            } else {
                stopSelf();
            }
            return mChannel.getId();
        }

        private Notification createForegroundNotification(AlarmManager am, AcquisitionTimes times, DateTime now, TrackedActivityModel lastActivity, Context context, PendingIntent pendingIntent) {
            final String channel;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                channel = createChannel();
            else {
                channel = "";
            }
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channel);
            PendingIntent mainActivityIntent = createMainActivityIntent(context);
            builder.setContentIntent(mainActivityIntent);

            builder.setSmallIcon(R.drawable.ic_notif);
            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
            builder.setLargeIcon(bm);

            //builder.setAutoCancel(true);
            //builder.setLights(Color.WHITE, 300, 1500);
            builder.setOngoing(true);
            final long updateInMillis;
            boolean needExact = true;
            if (times.getCurrent() == null) {
                if (!isUnfinishedAcquisitionTime(now, times, lastActivity)) {
                    throw new IllegalStateException("not a foreground notification");
                }
                final AcquisitionTimeInstance previous = Preconditions.checkNotNull(times.getPrevious());
                long startTimeMillis = lastActivity == null ? previous.getStartDateTime().getMillis() : lastActivity.getEndTimeMillis();
                builder.setWhen(startTimeMillis);
                builder.setUsesChronometer(true);

                builder.setContentTitle(getString(R.string.action_last_item_of_day_heading_unfinished));

                if (lastActivity != null) {
                    CharSequence durationLast = TimeFormatUtil.formatDuration(getContext(), lastActivity.getEndTimeMillis() - lastActivity.getStartTimeMillis());
                    builder.setContentText(getString(R.string.ongoing_notif_after_acquisition_with_previous_activity_content_text, lastActivity.getDisplayName(context)));
                    builder.setContentInfo(getString(R.string.ongoing_notif_after_acquisition_with_previous_activity_content_info, durationLast));
                } else {
                    builder.setContentText(getString(R.string.ongoing_notif_after_acquisition_without_previous_activity_content_text, TimeFormatUtil.formatTime(context, startTimeMillis)));
                }

                builder.addAction(R.drawable.ic_action_add,
                        getResources().getString(R.string.action_tracktime_new_notification),
                        createTrackActivityIntent(context));
                builder.addAction(android.R.drawable.ic_menu_preferences,
                        getResources().getString(R.string.action_tracktime_notification_settings),
                        //"",
                        createAcquisitionTimesIntent(context));

                updateInMillis = handleNoise(builder, startTimeMillis);
            } else if (lastActivity == null) {
                // First activity of day
                long startTimeMillis = times.getCurrent().getStartDateTime().getMillis();
                builder.setWhen(startTimeMillis);
                builder.setUsesChronometer(true);
                builder.setContentText(getString(R.string.ongoing_notif_in_acquisition_first_activity_content_text, TimeFormatUtil.formatTime(context, startTimeMillis)));
                builder.setContentTitle(getString(R.string.ongoing_notif_in_acquisition_first_activity_content_title));
                builder.addAction(R.drawable.ic_action_add,
                        getResources().getString(R.string.action_tracktime_new_notification),
                        createTrackActivityIntent(context));
                builder.addAction(android.R.drawable.ic_menu_preferences,
                        getResources().getString(R.string.action_tracktime_notification_settings),
                        //"",
                        createAcquisitionTimesIntent(context));

                updateInMillis = handleNoise(builder, startTimeMillis);

            } else {
                CharSequence durationLast = TimeFormatUtil.formatDuration(getContext(), lastActivity.getEndTimeMillis() - lastActivity.getStartTimeMillis());
                long millisSinceLast = now.getMillis() - lastActivity.getEndTimeMillis();
                final long nextUpdateInMillis;
                if (millisSinceLast < UP_TO_DATE_MILLIS_SINCE_ACTIVITY) {
                    // show notification for current activity
                    CategoryModel category = lastActivity.getCategory();
                    String categoryName = category == null ? null : category.getName();
                    ProjectModel project = lastActivity.getProject();
                    String projectName = project == null ? null : project.getName();
                    String categoryProjectLine = !Strings.isNullOrEmpty(categoryName) ? categoryName: "";
                    if (!Strings.isNullOrEmpty(projectName)) {
                        if (!Strings.isNullOrEmpty(categoryProjectLine)) {
                            categoryProjectLine += " - ";
                        }
                        categoryProjectLine += projectName;
                    }
                    builder.setWhen(lastActivity.getEndTimeMillis());
                    builder.setUsesChronometer(false);

                    builder.setContentTitle(getString(R.string.ongoing_notif_in_acquisition_next_activity_within_threshold_content_title, lastActivity.getDisplayName(context)));
                    builder.setContentText(categoryProjectLine);
                    //builder.setContentInfo("Dauer " + durationLast);

                    // update the notification
                    nextUpdateInMillis = UP_TO_DATE_MILLIS_SINCE_ACTIVITY - millisSinceLast;
                } else {
                    // ask the user what he did

                    builder.setWhen(lastActivity.getEndTimeMillis());
                    builder.setUsesChronometer(true);

                    builder.setContentTitle(getString(R.string.ongoing_notif_in_acquisition_next_activity_content_title));
                    builder.setContentText(getString(R.string.ongoing_notif_in_acquisition_next_activity_content_text, lastActivity.getDisplayName(context)));
                    builder.setContentInfo(getString(R.string.ongoing_notif_in_acquisition_next_activity_content_info, durationLast));

                    /*
                    builder.addAction(R.drawable.ic_action_accept,
                        getResources().newStringGetter(R.string.action_tracktime_notification_continue),
                        createContinueActivityIntent(context, lastActivity.getId()));
                        */
                    builder.addAction(R.drawable.ic_action_add,
                            getResources().getString(R.string.action_tracktime_new_notification),
                            createTrackActivityIntent(context));
                    builder.addAction(android.R.drawable.ic_menu_preferences,
                            getResources().getString(R.string.action_tracktime_notification_settings),
                            //"",
                            createAcquisitionTimesIntent(context));
                    nextUpdateInMillis = VALUE_MILLIS_NOT_SET;
                }

                long nextNoiseUpdateInMillis = handleNoise(builder, lastActivity.getEndTimeMillis());
                if (nextNoiseUpdateInMillis != VALUE_MILLIS_NOT_SET && nextUpdateInMillis != VALUE_MILLIS_NOT_SET) {
                    updateInMillis = Math.min(nextNoiseUpdateInMillis, nextUpdateInMillis);
                } else if (nextNoiseUpdateInMillis != VALUE_MILLIS_NOT_SET) {
                    updateInMillis = nextNoiseUpdateInMillis;
                } else {
                    updateInMillis = nextUpdateInMillis;
                    needExact = false;
                }
            }

            if (updateInMillis != VALUE_MILLIS_NOT_SET) {
                if (needExact) {
                    AlarmManagerSupport.setAlarm(am, AlarmManager.ELAPSED_REALTIME,
                            SystemClock.elapsedRealtime() + updateInMillis, pendingIntent);
                } else {
                    am.set(AlarmManager.ELAPSED_REALTIME,
                            SystemClock.elapsedRealtime() + updateInMillis, pendingIntent);
                }
            }

            //here-------------------------------------
            return builder.build();
        }

        private long millisToMinutes(long millis) {
            return millis / 60000;
        }
        private long minutesToMillis(long minutes) {
            return minutes * 60000;
        }

        private long handleNoise(NotificationCompat.Builder builder, long startTimeMillis) {
            /**
             * User Story:
             * I as a User want Agime to notify me with an extra noise every XXX Minutes if I
             * did not track my activity, so that I learn to always track my activities at least every XXX Minutes.
             *
             *
             * Umsetzung:
             *  - Schedulen, dass dieser Service aufgerufen wird - hier gibt es ggf. Konflikte mit
             *    den anderen Alarm-Schedulings -> rückgabewert, in wie vielen Millis die
             *    nächste Benachrichtigung kommen soll.
             *  - Auslösen einer besonderen Benachrichtigung, wenn die Notification aktualisiert
             *    wird und die Bedingung eintritt.
             *
             * Ich verwende User Preferences, um den nächsten Noise-Punkt festzulegen.
             * Wenn der Noise-Punkt erreicht oder überschritten wird, dann noise hinzufügen.
             * Noise-Punkt ergibt sich aus dem Tupel "letzte startzeit" und "noise zeit",
             * damit bei zwischenzeitlicher Veränderung die
             */
            long nowMillis = System.currentTimeMillis();

            // add noise if neccessary, ressetting noise settings in user preferences to nil.
            addNoiseToNotificationIfNeeded(builder, startTimeMillis, nowMillis);


            final int minutes = Preferences.getAcquisitionTimeNotificationNoiseThresholdMinutes(getContext());
            if (minutes <= 0) {
                // no extra noisy notifications requested
                return VALUE_MILLIS_NOT_SET;
            }
            long makeNoiseAtMillis = minutes <= 0 ? -1 : minutesToMillis(minutes) + startTimeMillis;
            while (makeNoiseAtMillis < nowMillis) {
                makeNoiseAtMillis += minutesToMillis(minutes);
            }

            PreferenceManager.getDefaultSharedPreferences(getContext())
                    .edit()
                    .putLong(PREF_LAST_STARTTIME_MILLIS, startTimeMillis)
                    .putLong(PREF_NOISE_TIME_MILLIS, makeNoiseAtMillis)
                    .commit();

            return (makeNoiseAtMillis - nowMillis) + 100 /*add 100 ms to ensure, that the notification update will be a little bit later and will lead to noise*/;
        }

        private void addNoiseToNotificationIfNeeded(NotificationCompat.Builder builder, long startTimeMillis, long nowMillis) {
            final long lastStartTimeMillis = PreferenceManager.getDefaultSharedPreferences(getContext())
                    .getLong(PREF_LAST_STARTTIME_MILLIS, VALUE_MILLIS_NOT_SET);
            final long lastNoiseTimeMillis = PreferenceManager.getDefaultSharedPreferences(getContext())
                    .getLong(PREF_NOISE_TIME_MILLIS, VALUE_MILLIS_NOT_SET);

            final boolean isNotAfterLastStartTime = lastStartTimeMillis != VALUE_MILLIS_NOT_SET &&
                    startTimeMillis <= lastStartTimeMillis;
            final boolean isAfterNoiseTime = lastNoiseTimeMillis != VALUE_MILLIS_NOT_SET && nowMillis >= lastNoiseTimeMillis;
            if (isNotAfterLastStartTime && (lastNoiseTimeMillis == VALUE_MILLIS_NOT_SET || isAfterNoiseTime)) {
                builder.setContentTitle(getString(R.string.ongoing_notif_in_long_after_content_title));
                if (isAfterNoiseTime) {
                    builder.setDefaults(Notification.DEFAULT_ALL);
                    PreferenceManager.getDefaultSharedPreferences(getContext())
                            .edit()
                            //.putLong(PREF_LAST_STARTTIME_MILLIS, VALUE_MILLIS_NOT_SET)
                            .putLong(PREF_NOISE_TIME_MILLIS, VALUE_MILLIS_NOT_SET)
                            .commit();
                }
            }
        }

        protected Context getContext() {
            return NotificationManagingService.this;
        }

        private PendingIntent createMainActivityIntent(Context context) {
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            // Adds the back stack
            stackBuilder.addParentStack(AgimeMainActivity.class);
            // Adds the Intent to the top of the stack
            stackBuilder.addNextIntent(new Intent(context, AgimeMainActivity.class));

            return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        }

        private PendingIntent createTrackActivityIntent(Context context) {

            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            // Adds the back stack
            stackBuilder.addParentStack(TrackActivity.class);
            // Adds the Intent to the top of the stack
            final Intent intent = new Intent(Intent.ACTION_INSERT, MCContract.Activity.CONTENT_URI);
            intent.setClass(context, TrackActivity.class);
            stackBuilder.addNextIntent(intent);
            return stackBuilder.getPendingIntent(1, PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        }

        private PendingIntent createAcquisitionTimesIntent(Context context) {
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            // Adds the back stack
            stackBuilder.addParentStack(AcquisitionTimeManagementActivity.class);
            // Adds the Intent to the top of the stack
            stackBuilder.addNextIntent(new Intent(context, AcquisitionTimeManagementActivity.class));

            return stackBuilder.getPendingIntent(2, PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        }

        public boolean isPermanentlyHidden() {
            return !Preferences.isAcquisitionTimeNotificationEnabled(getContext());
        }
    }

    public NotificationManagingService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        _thread = new HandlerThread("NotificationManagingService",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        _thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        _serviceHandler = new BackgroundService(_thread.getLooper());


        _activityObserver = new ContentObserver(null) {
            @Override
            public void onChange(boolean selfChange) {
                Log.v(LOG_TAG, "[" + _instanceId + "] - Detected Content change for " + MCContract.Activity.CONTENT_URI + ", selfChange: " + selfChange );
                Message msg = _serviceHandler.obtainMessage();
                msg.arg1 = 0;
                msg.obj = null;
                _serviceHandler.sendMessage(msg);
            }
        };

        final RateLimiter rateLimiter = RateLimiter.create(1);
        _trackedActivityLoaderObserver = new ContentObserver(null) {
            @Override
            public void onChange(boolean selfChange) {
                // this content observer will be called extremely often - we thus throttle its effect
                // TODO: find a better/cleaner way to achieve this without throttling

                if (rateLimiter.tryAcquire()) {
                    Log.v(LOG_TAG, "[" + _instanceId + "] - Detected Content change for TrackedActivityLoader, selfChange: " + selfChange);
                    Message msg = _serviceHandler.obtainMessage();
                    msg.arg1 = 0;
                    msg.obj = null;
                    _serviceHandler.sendMessage(msg);
                } else {
                    // skipping
                }
            }
        };

        getContentResolver().registerContentObserver(MCContract.Activity.CONTENT_URI, true, _activityObserver);
        _serviceHandler._trackedActivityLoader.setContentObserver(_trackedActivityLoaderObserver);
    }


    @Override
    public void onRebind(final Intent intent) {
        //FIXME - completely remove binding for this service?
        //suppressForegroundNotification();
        // force refresh instead of hiding
        unsuppressForgroundNotification();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(LOG_TAG, "Notification: BIND called");
        //FIXME - completely remove binding for this service?
        //suppressForegroundNotification();
        // force refresh instead of hiding
        unsuppressForgroundNotification();
        return mBinder;
    }

    @Override
    public boolean onUnbind(final Intent intent) {
        Log.i(LOG_TAG, "Notification: UNBIND called");
        //FIXME - completely remove binding for this service?
        // force refresh
        unsuppressForgroundNotification();
        return true;
    }

    private void unsuppressForgroundNotification() {
        Log.i(LOG_TAG, "Notification: Unsuppress notification called");
        Message msg = _serviceHandler.obtainMessage();
        msg.arg2 = FLAG_UNSUPPRESS_NOTIFICATION;
        msg.obj = null;
        _serviceHandler.sendMessage(msg);
    }

    private void suppressForegroundNotification() {
        Log.i(LOG_TAG, "Notification: suppress notification called");
        Message msg = _serviceHandler.obtainMessage();
        msg.arg2 = FLAG_SUPPRESS_NOTIFICATION;
        msg.obj = null;
        _serviceHandler.sendMessage(msg);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(LOG_TAG, "Notification: onStartCommand");
        Message msg = _serviceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        _serviceHandler.sendMessage(msg);

        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        stopForeground(true);
        _serviceHandler.cleanup();
        getContentResolver().unregisterContentObserver(_activityObserver);
        _serviceHandler._trackedActivityLoader.setContentObserver(null);

        HandlerThreadSupport.quitSafelyIfPossible(_thread);
    }

}
