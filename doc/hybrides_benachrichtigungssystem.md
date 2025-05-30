# Hybrides Benachrichtigungssystem für Agime

## Übersicht

Das hybride Benachrichtigungssystem der Agime-App kombiniert mehrere Android-Technologien, um eine zuverlässige Benachrichtigungserfahrung zu bieten, die gleichzeitig den neuesten Android-Beschränkungen für Hintergrunddienste entspricht. Diese Dokumentation beschreibt die Architektur und die Funktionsweise des Systems.

## Ziele und Anforderungen

### Hauptziele
- Zuverlässige Benachrichtigungen auch nach längeren Zeiträumen
- Präzise Zeitsteuerung für Start und Ende von Erfassungsperioden
- Einhaltung der Android 15-Beschränkungen für Vordergrund-Services
- Batterieoptimierte Implementierung
- Robustes Verhalten bei Systemneustarts und Zeitzonenänderungen

### Besondere Herausforderungen
- Android beschränkt zunehmend Hintergrundaktivitäten und Benachrichtigungen
- Vordergrund-Services werden in Android 15 stark reglementiert
- Exakte Timer erfordern seit Android 12 besondere Berechtigungen
- Der Nutzer muss immer informiert bleiben, ohne zu stark gestört zu werden

## Systemarchitektur

Das hybride System besteht aus drei Hauptkomponenten:

1. **WorkManager**: Dient als zuverlässiger langfristiger Scheduler für regelmäßige Überprüfungen
2. **AlarmManager**: Ermöglicht präzise Zeitsteuerung für wichtige Übergangspunkte
3. **ShortLivedNotificationService**: Ein kurzlebiger Vordergrund-Service für hochwertige Benachrichtigungen

### Komponenten im Detail

#### 1. WorkManagerController

Die zentrale Steuereinheit des Systems, koordiniert WorkManager und AlarmManager.

**Hauptfunktionen:**
- Initialisierung des Gesamtsystems beim App-Start oder Geräteneustart
- Planung regelmäßiger Überprüfungen im 15-Minuten-Intervall (Minimum von WorkManager)
- Verwaltung der AlarmManager-Alarme für Zeiterfassungsperioden
- Bereitstellung öffentlicher API für andere Komponenten

**Beispielcode:**
```java
// Initialisierung des Controllers
WorkManagerController.initialize(context);

// Planung einer sofortigen Überprüfung
WorkManagerController.scheduleImmediateCheck(context);

// Planung der nächsten Benachrichtigungen
WorkManagerController.scheduleNextExecution(context);
```

#### 2. NotificationWorker

Ein WorkManager-Worker, der regelmäßig ausgeführt wird und Benachrichtigungen mit niedriger Priorität anzeigt.

**Hauptfunktionen:**
- Regelmäßige Überprüfung der Zeiterfassung im Hintergrund
- Erstellung von Benachrichtigungen mit niedriger Priorität außerhalb aktiver Erfassungszeiten
- Delegation der Benachrichtigungserstellung für aktive Phasen an den ShortLivedNotificationService

**Besondere Merkmale:**
- Läuft automatisch mit WorkManager-Garantien für periodische Ausführung
- Koordiniert mit anderen Komponenten, um Doppelbenachrichtigungen zu vermeiden
- Verwendet einen eigenen Benachrichtigungskanal mit niedriger Priorität

#### 3. NotificationAlarmReceiver

Ein BroadcastReceiver für exakte Alarme, getriggert durch AlarmManager.

**Hauptfunktionen:**
- Reagiert auf präzise Zeitalarme für Start und Ende von Erfassungszeiten
- Startet den ShortLivedNotificationService zu Beginn einer Erfassungsphase
- Benachrichtigt den WorkManager über das Ende einer Erfassungsphase
- Handhabt auch Erinnerungsbenachrichtigungen ("Noise Reminders")

**Beispielcode für Alarmplanung:**
```java
// Planen eines Alarms für den Start einer Erfassungszeit
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

AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            instance.getStartDateTime().getMillis(), 
            pendingIntent);
}
```

#### 4. ShortLivedNotificationService

Ein kurzlebiger Vordergrund-Service, der hochqualitative Benachrichtigungen während aktiver Zeiterfassung bereitstellt.

**Hauptfunktionen:**
- Zeigt Benachrichtigungen mit hoher Priorität während aktiver Erfassungsphasen an
- Regelmäßige Aktualisierung der Benachrichtigungen (z.B. Chronometer)
- Automatische Selbstbeendigung nach einer konfigurierbaren Zeit

**Besondere Merkmale:**
- Selbstbeschränkte Laufzeit zur Einhaltung der Android-Einschränkungen
- Dynamische Anpassung der Laufzeit basierend auf der Dauer der Erfassungsphase
- Verwendet einen separaten Benachrichtigungskanal mit höherer Priorität

**Beispielcode für die Verwendung:**
```java
// Starten des Services mit Standardlaufzeit
ShortLivedNotificationService.startService(context);

// Starten des Services mit angepasster Laufzeit (z.B. für kurze Erinnerungen)
ShortLivedNotificationService.startService(context, 1); // 1 Minute Laufzeit
```

#### 5. NotificationBroadcastReceiver

Ein BroadcastReceiver für Systemereignisse, der das Benachrichtigungssystem initialisiert.

**Hauptfunktionen:**
- Initialisierung bei Gerätstart oder App-Installation
- Reaktion auf Zeitzonenänderungen
- Aktualisierung bei Konfigurationsänderungen

**Behandelte Events:**
- `BOOT_COMPLETED`
- `TIME_CHANGED` und `TIMEZONE_CHANGED`
- `ACTION_ACQUISITION_TIME_CONFIGURE` und andere App-spezifische Intents

## Benachrichtigungskanäle

Das System verwendet separate Benachrichtigungskanäle für unterschiedliche Szenarien:

1. **Aktive Zeiterfassung** (`ACTIVE_CHANNEL_ID`)
   - Hohe Priorität
   - Verwendet während aktiver Erfassungszeiten
   - Enthält Chronometer und detaillierte Informationen

2. **Hintergrundüberwachung** (`BACKGROUND_CHANNEL_ID`)
   - Niedrige Priorität
   - Verwendet außerhalb aktiver Erfassungszeiten
   - Minimale Störung des Nutzers

## Workflow und Kommunikation

### Start einer Erfassungszeit

1. **Vorbereitung:**
   - `WorkManagerController` plant einen `AlarmManager`-Alarm für den Start einer Erfassungszeit
   
2. **Auslösung:**
   - AlarmManager triggert den `NotificationAlarmReceiver` zur exakten Startzeit
   - Der Receiver startet den `ShortLivedNotificationService` für hochwertige Benachrichtigungen
   - Der Service begrenzt seine Laufzeit automatisch (Standard: 10 Minuten)
   
3. **Nach Service-Ende:**
   - `NotificationWorker` übernimmt mit Benachrichtigungen niedriger Priorität
   - Regelmäßige Überprüfungen alle 15 Minuten

### Ende einer Erfassungszeit

1. **Vorbereitung:**
   - `WorkManagerController` plant einen `AlarmManager`-Alarm für das Ende der Erfassungszeit
   
2. **Auslösung:**
   - AlarmManager triggert den `NotificationAlarmReceiver` zur exakten Endzeit
   - Der Receiver informiert `WorkManagerController`, der eine sofortige Überprüfung plant
   
3. **Aktualisierung:**
   - `NotificationWorker` aktualisiert die Benachrichtigung basierend auf dem neuen Status

### Zeitzonenänderungen

1. `NotificationBroadcastReceiver` erkennt die Änderung
2. `WorkManagerController` wird neu initialisiert
3. Alle Alarme werden neu berechnet und geplant

## Robustheit und Fehlerbehandlung

### Wiederherstellung nach Geräteneustart
- `NotificationBroadcastReceiver` initialisiert das System nach dem Boot
- `WorkManagerController` plant sowohl WorkManager-Jobs als auch Alarme neu

### Fehlerszenarien
- Falls ein Service vorzeitig beendet wird, übernimmt der periodische `NotificationWorker`
- Falls exakte Alarme fehlschlagen, sorgt die regelmäßige Überprüfung für ein Fallback
- Automatische Neuplanung bei verpassten Zeitpunkten

### Android 12+ Berechtigungen für exakte Alarme
- Fallback auf ungenaue Alarme, wenn keine Berechtigung für exakte Alarme vorliegt
- WorkManager als Backup-Mechanismus

## Batterie-Optimierungen

Das System minimiert den Batterieverbrauch durch:

1. Verwendung von `WorkManager` für die Hauptkoordination (batterieschonend)
2. Begrenzung der Laufzeit des Vordergrund-Services auf das Notwendige
3. Intelligente Planung von Alarmen nur für kritische Zeitpunkte
4. Anpassung der Service-Laufzeit an die Dauer der Erfassungsphase
5. Vermeidung von unnötigen Wakes oder kontinuierlichen Überprüfungen

## Tests

Die folgenden Testfälle sollten implementiert/durchgeführt werden:

1. **Komponententests:**
   - Funktionalität des `NotificationWorker` 
   - Korrekte Verarbeitung von Intents im `NotificationAlarmReceiver`
   - Selbstbeendigung des `ShortLivedNotificationService`

2. **Integrationsszenarien:**
   - Korrekte Erstellung und Aktualisierung von Benachrichtigungen über Komponenten hinweg
   - Fehlerfreier Übergang zwischen Service und Worker
   - Korrekte Planung und Auslösung von Alarmen

3. **Edge Cases:**
   - Verhalten bei Zeitzonenänderungen
   - Wiederherstellung nach Geräteneustart
   - Verhalten bei fehlenderen Berechtigung für exakte Alarme

## FAQ und Troubleshooting

### Warum eine hybride Lösung?
Die Kombination aus `WorkManager`, `AlarmManager` und kurzlebigen Services bietet die beste Balance zwischen Zuverlässigkeit, Präzision und Batterieeffizienz, während sie gleichzeitig die neuesten Android-Beschränkungen einhält.

### Fehlerbehebung bei nicht erscheinenden Benachrichtigungen
1. Prüfen, ob die Benachrichtigungskanäle aktiviert sind
2. Überprüfen, ob der ShortLivedNotificationService auf der Batterie-Optimierungsliste steht
3. Für Android 12+: Prüfen, ob die Berechtigung für exakte Alarme erteilt wurde

### Bekannte Einschränkungen
- Auf stark optimierten Systemen (z.B. Xiaomi, Huawei) können zusätzliche Berechtigungen erforderlich sein
- Die präzise Alarmpunktgenauigkeit kann auf einigen Geräten variieren
- Bei längeren Erfassungszeiten übernimmt der WorkManager die Benachrichtigung mit niedrigerer Priorität

## Zukünftige Verbesserungen

1. **Benachrichtigungsberechtigungen:**
   - Implementierung eines Flows zur Beantragung von Benachrichtigungsberechtigungen
   - Benutzerfreundliche Anleitung für exakte Alarmberechtigungen auf Android 12+

2. **Batterie-Optimierung:**
   - Weitere Optimierung der Intervalle basierend auf Benutzerverhalten
   - Anpassung der Aktualisierungsfrequenz an den Akku-Status

3. **Fehlerbehandlung:**
   - Erweitertes Logging für Diagnose von Problemen
   - Automatische Wiederherstellungsmechanismen

4. **Zusätzliche Features:**
   - Benutzerdefinierte Benachrichtigungsgeräusche für verschiedene Ereignisse
   - Anpassbarkeit der Benachrichtigungsdetails

## Mitwirkende

Das hybride Benachrichtigungssystem wurde entwickelt und implementiert von:
- Agime-Entwicklungsteam

## Änderungshistorie

- **Mai 2025:** Erste Implementierung des hybriden Systems
- **Juni 2025:** (Geplant) Erweiterung der Tests und Fehlerbehebungen
- **Juli 2025:** (Geplant) Optimierungen für Android 15
