# Technische Dokumentation: Hybrides Benachrichtigungssystem

## Architektur-Diagramm

```
┌──────────────────────────────────────────────────────────┐
│                  System-Auslöser                         │
│                                                          │
│  ┌─────────┐   ┌────────────┐   ┌─────────────────────┐  │
│  │Systemboot│   │Zeitänderung│   │App-Konfiguration    │  │
│  └────┬────┘   └─────┬──────┘   └──────────┬──────────┘  │
└───────┼──────────────┼───────────────────┬┼─────────────┘
        │              │                   ││
        ▼              ▼                   ▼▼
┌─────────────────────────────────────────────┐
│        NotificationBroadcastReceiver        │
└──────────────────────┬──────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────┐
│           WorkManagerController             │
│                                             │
│  ┌─────────────────┐  ┌──────────────────┐  │
│  │ScheduleImmediate│  │SchedulePeriodic  │  │
│  │     Check       │  │    Checks        │  │
│  └────────┬────────┘  └──────┬───────────┘  │
│           │                  │              │
│  ┌────────▼──────────────────▼───────────┐  │
│  │    ScheduleNextAcquisitionAlarms      │  │
│  └──┬─────────────────────────────┬──────┘  │
└─────┼─────────────────────────────┼─────────┘
      │                             │
┌─────▼─────────┐           ┌───────▼───────┐
│  WorkManager  │           │  AlarmManager │
└───────┬───────┘           └───────┬───────┘
        │                           │
        ▼                           ▼
┌───────────────┐         ┌────────────────────┐
│NotificationWorker│       │NotificationAlarmReceiver│
└────────┬──────┘         └──────────┬─────────┘
         │                          │
         │                          │
         │         ┌───────────────┐│
         └────────►│ShortLivedNotificationService│◄┘
                   └────────────────┘
```

## Komponenten-Matrix

| Komponente                    | Zweck                         | Lebensdauer        | Priorität      |
|-------------------------------|-------------------------------|-------------------|----------------|
| WorkManagerController         | Koordination & Planung        | Statisch          | -              |
| NotificationWorker            | Regelmäßige Überprüfung       | 15 Min. Intervall | Niedrig        |
| NotificationAlarmReceiver     | Präzise Zeitsteuerung         | Event-basiert     | -              |
| ShortLivedNotificationService | Hochwertige Benachrichtigung  | Max. 10 Min.      | Hoch           |
| NotificationBroadcastReceiver | System-Event-Handling         | Event-basiert     | -              |

## Sequenzdiagramme

### Sequenz: Start einer Erfassungszeit

```
┌─────────┐         ┌───────────────┐      ┌────────────┐     ┌────────────┐    ┌─────────────┐
│AlarmMgr │         │NotifAlarmRcvr │      │WorkMgrCtrl │     │ShortService│    │NotifWorker  │
└────┬────┘         └───────┬───────┘      └─────┬──────┘     └─────┬──────┘    └─────┬───────┘
     │    Alarm auslösen    │                    │                  │                 │
     │───────────────────►  │                    │                  │                 │
     │                      │                    │                  │                 │
     │                      │ handleAcquisitionTimeStart            │                 │
     │                      │────────────┐       │                  │                 │
     │                      │            │       │                  │                 │
     │                      │◄───────────┘       │                  │                 │
     │                      │                    │                  │                 │
     │                      │   startService()   │                  │                 │
     │                      │───────────────────────────────────►   │                 │
     │                      │                    │                  │                 │
     │                      │  scheduleImmediateCheck              │                 │
     │                      │────────────────────►                  │                 │
     │                      │                    │    createNotif() │                 │
     │                      │                    │                  │─────────┐       │
     │                      │                    │                  │         │       │
     │                      │                    │                  │◄────────┘       │
     │                      │                    │                  │                 │
     │                      │                    │                  │                 │
     │                      │                    │                  │ AutoStop nach   │
     │                      │                    │                  │ max. 10 Min.    │
     │                      │                    │                  │─────────┐       │
     │                      │                    │                  │         │       │
     │                      │                    │                  │◄────────┘       │
     │                      │                    │                  │                 │
     │                      │                    │  scheduleImmediateCheck            │
     │                      │                    │◄─────────────────│                 │
     │                      │                    │                  │                 │
     │                      │                    │      enqueue     │                 │
     │                      │                    │────────────────────────────────►   │
     │                      │                    │                  │                 │
     │                      │                    │                  │     doWork()    │
     │                      │                    │                  │                 │─────────┐
     │                      │                    │                  │                 │         │
     │                      │                    │                  │                 │◄────────┘
     │                      │                    │                  │                 │
```

### Sequenz: Ende einer Erfassungszeit

```
┌─────────┐         ┌───────────────┐      ┌────────────┐     ┌────────────┐    ┌─────────────┐
│AlarmMgr │         │NotifAlarmRcvr │      │WorkMgrCtrl │     │ShortService│    │NotifWorker  │
└────┬────┘         └───────┬───────┘      └─────┬──────┘     └─────┬──────┘    └─────┬───────┘
     │    Alarm auslösen    │                    │                  │                 │
     │───────────────────►  │                    │                  │                 │
     │                      │                    │                  │                 │
     │                      │ handleAcquisitionTimeEnd              │                 │
     │                      │────────────┐       │                  │                 │
     │                      │            │       │                  │                 │
     │                      │◄───────────┘       │                  │                 │
     │                      │                    │                  │                 │
     │                      │  scheduleImmediateCheck              │                 │
     │                      │────────────────────►                  │                 │
     │                      │                    │                  │                 │
     │                      │                    │      enqueue     │                 │
     │                      │                    │────────────────────────────────►   │
     │                      │                    │                  │                 │
     │                      │                    │                  │     doWork()    │
     │                      │                    │                  │                 │─────────┐
     │                      │                    │                  │                 │         │
     │                      │                    │                  │                 │◄────────┘
     │                      │                    │                  │                 │
     │                      │                    │                  │  updateNotification
     │                      │                    │                  │                 │────────┐
     │                      │                    │                  │                 │        │
     │                      │                    │                  │                 │◄───────┘
```

## Klassenübersicht mit APIs

### WorkManagerController
```java
public class WorkManagerController {
    // Konstanten
    public static final String WORKER_TAG = "notification_worker";
    public static final String PERIODIC_WORK_NAME = "periodic_notification_check";
    public static final String IMMEDIATE_WORK_NAME = "immediate_notification_check";
    
    // Öffentliche API
    public static void initialize(Context context);
    public static void scheduleImmediateCheck(Context context);
    public static void handleIntent(Context context, Intent intent);
    public static Operation cancelAllWork(Context context);
    public static void scheduleNextAcquisitionTimeAlarms(Context context);
    public static AcquisitionTimes getCurrentAcquisitionTimes(Context context);
    
    // Private Hilfsmethoden
    private static void schedulePeriodicChecks(Context context);
    private static void scheduleAcquisitionTimeStart(Context context, AcquisitionTimeInstance instance);
    private static void scheduleAcquisitionTimeEnd(Context context, AcquisitionTimeInstance instance);
    private static void scheduleNoiseReminderIfNeeded(Context context, AcquisitionTimeInstance instance);
    private static void cancelAllAlarms(Context context);
}
```

### NotificationWorker
```java
public class NotificationWorker extends Worker {
    // Konstanten
    public static final String BACKGROUND_CHANNEL_ID = "background_notification_channel";
    public static final int NOTIFICATION_ID = 1000;
    
    // WorkManager Lifecycle
    @NonNull
    @Override
    public Result doWork();
    
    // Notification Management
    private void planNextExecution();
    private Notification createForegroundNotificationIfNeeded();
    private Notification createBackgroundNotificationIfNeeded();
    private boolean needsForegroundNotification(DateTime now, AcquisitionTimes times, 
                                             TrackedActivityModel lastActivity);
    private boolean isUnfinishedAcquisitionTime(DateTime now, AcquisitionTimes times, 
                                             TrackedActivityModel lastActivity);
    private String createChannel();
    private String createBackgroundNotificationChannel();
}
```

### NotificationAlarmReceiver
```java
public class NotificationAlarmReceiver extends BroadcastReceiver {
    // Aktionstypen
    public static final String ACTION_START_ACQUISITION_TIME = "de.kalass.agime.action.START_ACQUISITION_TIME";
    public static final String ACTION_END_ACQUISITION_TIME = "de.kalass.agime.action.END_ACQUISITION_TIME";
    public static final String ACTION_NOISE_REMINDER = "de.kalass.agime.action.NOISE_REMINDER";
    
    // Intent-Extras
    public static final String EXTRA_ACQUISITION_TIME_ID = "acquisition_time_id";
    public static final String EXTRA_START_TIME_MILLIS = "start_time_millis";
    public static final String EXTRA_END_TIME_MILLIS = "end_time_millis";
    public static final String EXTRA_MAX_RUNTIME_MINUTES = "max_runtime_minutes";
    
    // BroadcastReceiver Lifecycle
    @Override
    public void onReceive(Context context, Intent intent);
    
    // Alarm-Handling
    private void handleAcquisitionTimeStart(Context context, Intent intent);
    private void handleAcquisitionTimeEnd(Context context, Intent intent);
    private void handleNoiseReminder(Context context, Intent intent);
}
```

### ShortLivedNotificationService
```java
public class ShortLivedNotificationService extends Service {
    // Konstanten
    public static final int NOTIFICATION_ID = 1001;
    public static final String ACTIVE_CHANNEL_ID = "active_time_tracking_channel";
    public static final String EXTRA_MAX_RUNTIME_MINUTES = "extra_max_runtime_minutes";
    public static final int DEFAULT_MAX_RUNTIME_MINUTES = 10;
    public static final int UPDATE_INTERVAL_MS = 5000; // 5 Sekunden
    
    // Service Lifecycle
    @Override
    public void onCreate();
    @Override
    public int onStartCommand(Intent intent, int flags, int startId);
    @Override
    public void onDestroy();
    @Nullable
    @Override
    public IBinder onBind(Intent intent);
    
    // Öffentliche API
    public static void startService(Context context);
    public static void startService(Context context, int maxRuntimeMinutes);
    
    // Helper-Methoden
    private void loadCurrentState();
    private void scheduleNotificationUpdates();
    private void updateNotification();
    private void scheduleServiceStop(int maxRuntimeMinutes);
    private Notification createActiveTimeNotification();
    private String createNotificationChannel();
}
```

### NotificationBroadcastReceiver
```java
public class NotificationBroadcastReceiver extends BroadcastReceiver {
    // BroadcastReceiver Lifecycle
    @Override
    public void onReceive(Context context, Intent intent);
    
    // Helper-Methoden
    private void checkAndStartShortLivedServiceIfNeeded(Context context);
}
```

## Testabdeckung

### Unit-Tests

#### WorkManagerNotificationTest
- `testWorkerExecution`: Überprüft, ob der NotificationWorker korrekt ausgeführt wird
- `testWorkerWithActiveAcquisitionTime`: Testet die Benachrichtigungserstellung für aktive Erfassungszeiten
- `testHandleIntent`: Verifiziert die korrekte Intent-Verarbeitung
- `testInitialize`: Überprüft die Initialisierung des Controllers
- `testSchedulePeriodicChecks`: Testet die Planung periodischer Überprüfungen

#### Zukünftige Tests (noch zu implementieren)
- `testAlarmReceiverHandling`: Überprüfung der Alarmverarbeitung
- `testShortLivedServiceLifecycle`: Test des selbstbeendenden Services
- `testCoordinationBetweenComponents`: Integration der Komponenten

## Fehlerszenarien und Lösungen

| Szenario | Problem | Lösung |
|----------|---------|--------|
| Exakte Alarmberechtigung nicht erteilt | AlarmManager kann keine exakten Alarme setzen | Fallback auf ungenaue Alarme + WorkManager als Backup |
| Service vorzeitig beendet | Keine hochwertige Benachrichtigung mehr | WorkManager übernimmt mit niedrigprioritärer Benachrichtigung |
| Gerätstart verpasst | System wird nicht initialisiert | App-Start initialisiert das System ebenfalls |
| Zeitzonenänderung | Geplante Alarme/Jobs sind nicht mehr korrekt | Neu-Initialisierung und -Planung aller Komponenten |

## Leistungsüberlegungen

### Batterienutzung
- WorkManager: Minimaler Verbrauch durch System-Batching
- AlarmManager: Nur für kritische Zeitpunkte, minimal
- ShortLivedNotificationService: Höchster Verbrauch, daher zeitlich begrenzt

### Speichernutzung
- Permanente Prozesse: Keine
- Temporäre Prozesse: Nur während aktiver Erfassungszeit oder periodischer Checks

### CPU-Nutzung
- Höchste Last: Beim Start des Services (ca. 3-5% für wenige Sekunden)
- Durchschnittliche Last: Vernachlässigbar (<0.1%)

## Kompatibilität

| Android-Version | Besondere Behandlung |
|----------------|---------------------|
| Android 12+    | Erfordert Berechtigung für exakte Alarme |
| Android 13+    | Benachrichtigungsberechtigungen zur Laufzeit erforderlich |
| Android 15     | Strengere Beschränkungen für Foreground-Services, daher selbstbegrenzende Laufzeit |

## Beispiele und Handhabung

### Konfiguration einer zusätzlichen Benutzerbenachrichtigung

```java
// In WorkManagerController eine neue Methode hinzufügen:
public static void scheduleCustomReminderAlarm(Context context, long triggerTimeMillis, String message) {
    Intent intent = new Intent(context, NotificationAlarmReceiver.class);
    intent.setAction(NotificationAlarmReceiver.ACTION_NOISE_REMINDER);
    intent.putExtra("custom_message", message);
    
    PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context, 
            REQUEST_CODE_CUSTOM_REMINDER, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    
    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis, 
                pendingIntent);
    }
}
```

### Fehlerbehandlung für fehlende Berechtigungen

```java
// Überprüfung der Alarmberechtigung in WorkManagerController
private static void checkAndRequestAlarmPermission(Context context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        if (!alarmManager.canScheduleExactAlarms()) {
            // Benachrichtigung für den Benutzer erstellen
            NotificationHelper.showPermissionRequiredNotification(context);
            
            // In den Einstellungen zu den Alarmberechtingungen führen
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
}
```
