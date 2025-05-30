# Test-Plan: Hybrides Benachrichtigungssystem

## Übersicht

Dieses Dokument beschreibt die Teststrategie und die Testfälle für das hybride Benachrichtigungssystem der Agime-App. Das System kombiniert WorkManager, AlarmManager und einen kurzlebigen Foreground-Service, um zuverlässige und präzise Benachrichtigungen zu liefern.

## Testumgebungen

- **Unit-Tests**: Ausführbar mit JUnit und Robolectric
- **Instrumentation-Tests**: Ausführbar auf einem Android-Gerät oder Emulator
- **Manuelle Tests**: Durchzuführen auf verschiedenen Android-Versionen (11, 12, 13, 14, 15)

## Unit-Tests

### WorkManagerController Tests

#### `WorkManagerControllerTest`

| Test-ID | Testfall | Beschreibung | Erwartetes Ergebnis |
|---------|----------|-------------|-------------------|
| UT-WMC-01 | `testInitialize` | Initialisierung des WorkManagerController | WorkManager-Jobs und Alarme werden korrekt geplant |
| UT-WMC-02 | `testScheduleImmediateCheck` | Sofortige Überprüfung planen | OneTimeWorkRequest wird korrekt erstellt und eingeplant |
| UT-WMC-03 | `testHandleIntent` | Intent-Verarbeitung | Intent wird korrekt verarbeitet und entsprechende Aktion ausgelöst |
| UT-WMC-04 | `testCancelAllWork` | Abbrechen aller geplanten Arbeiten | WorkManager-Jobs werden abgebrochen |
| UT-WMC-05 | `testScheduleNextAcquisitionTimeAlarms` | Planung von Alarmen für nächste Erfassungszeit | Alarme werden korrekt für Start und Ende einer Erfassungszeit geplant |
| UT-WMC-06 | `testGetCurrentAcquisitionTimes` | Abrufen aktueller Erfassungszeiten | Korrekte AcquisitionTimes werden zurückgegeben |

### NotificationWorker Tests

#### `NotificationWorkerTest`

| Test-ID | Testfall | Beschreibung | Erwartetes Ergebnis |
|---------|----------|-------------|-------------------|
| UT-NW-01 | `testWorkerExecution` | Ausführung des NotificationWorker | Worker wird erfolgreich ausgeführt und gibt Result.success() zurück |
| UT-NW-02 | `testCreateForegroundNotificationIfNeeded` | Erzeugung einer Foreground-Benachrichtigung | Benachrichtigung wird korrekt erstellt oder null, wenn nicht erforderlich |
| UT-NW-03 | `testCreateBackgroundNotificationIfNeeded` | Erzeugung einer Hintergrundbenachrichtigung | Benachrichtigung wird korrekt erstellt oder null, wenn nicht erforderlich |
| UT-NW-04 | `testNeedsForegroundNotification` | Überprüfung der Benachrichtigungsvoraussetzungen | Korrekte Bestimmung, ob eine Benachrichtigung angezeigt werden soll |
| UT-NW-05 | `testIsUnfinishedAcquisitionTime` | Überprüfung unvollständiger Erfassungszeit | Korrekte Bestimmung, ob eine unvollständige Erfassungszeit vorliegt |

### NotificationAlarmReceiver Tests

#### `NotificationAlarmReceiverTest`

| Test-ID | Testfall | Beschreibung | Erwartetes Ergebnis |
|---------|----------|-------------|-------------------|
| UT-NAR-01 | `testOnReceiveStartAcquisitionTime` | Start einer Erfassungszeit | ShortLivedNotificationService wird gestartet |
| UT-NAR-02 | `testOnReceiveEndAcquisitionTime` | Ende einer Erfassungszeit | WorkManagerController wird informiert |
| UT-NAR-03 | `testOnReceiveNoiseReminder` | Erinnerungsbenachrichtigung | ShortLivedNotificationService wird mit kurzer Laufzeit gestartet |
| UT-NAR-04 | `testOnReceiveUnknownAction` | Unbekannte Aktion | Standardverhalten wird ausgeführt |

### ShortLivedNotificationService Tests

#### `ShortLivedNotificationServiceTest`

| Test-ID | Testfall | Beschreibung | Erwartetes Ergebnis |
|---------|----------|-------------|-------------------|
| UT-SLNS-01 | `testServiceStartup` | Starten des Services | Service startet und erstellt Benachrichtigung |
| UT-SLNS-02 | `testServiceScheduleStop` | Geplantes Stoppen des Services | Service stoppt sich selbst nach der konfigurierten Zeit |
| UT-SLNS-03 | `testLoadCurrentState` | Laden des aktuellen Zustands | Erfassungszeiten und letzte Aktivität werden korrekt geladen |
| UT-SLNS-04 | `testUpdateNotification` | Aktualisieren der Benachrichtigung | Benachrichtigung wird periodisch aktualisiert |
| UT-SLNS-05 | `testCreateActiveTimeNotification` | Erstellen aktiver Zeitbenachrichtigung | Benachrichtigung mit korrekten Inhalten wird erstellt |

### NotificationBroadcastReceiver Tests

#### `NotificationBroadcastReceiverTest`

| Test-ID | Testfall | Beschreibung | Erwartetes Ergebnis |
|---------|----------|-------------|-------------------|
| UT-NBR-01 | `testOnReceiveBootCompleted` | Gerätstart | WorkManagerController.initialize() wird aufgerufen |
| UT-NBR-02 | `testOnReceiveTimeChanged` | Zeitänderung | WorkManagerController.initialize() wird aufgerufen |
| UT-NBR-03 | `testOnReceiveAcquisitionTimeConfigure` | Konfigurationsänderung | WorkManagerController-Methoden werden aufgerufen |
| UT-NBR-04 | `testCheckAndStartShortLivedServiceIfNeeded` | Service-Start-Check | Service wird gestartet, wenn eine aktive Erfassungszeit läuft |

## Instrumentation-Tests

### Systemintegration

#### `SystemNotificationTest`

| Test-ID | Testfall | Beschreibung | Erwartetes Ergebnis |
|---------|----------|-------------|-------------------|
| IT-SN-01 | `testNotificationDisplayed` | Anzeige von Benachrichtigungen | Benachrichtigung wird auf dem Gerät angezeigt |
| IT-SN-02 | `testNotificationUpdateDuringActiveTime` | Aktualisierung während aktiver Zeit | Chronometer und Inhalt werden korrekt aktualisiert |
| IT-SN-03 | `testServiceAutoStop` | Automatisches Stoppen des Services | Service stoppt nach der konfigurierten Zeit |
| IT-SN-04 | `testAlarmTriggering` | Auslösung von Alarmen | Alarme werden zur korrekten Zeit ausgelöst |

### Leistung und Ressourcennutzung

#### `PerformanceNotificationTest`

| Test-ID | Testfall | Beschreibung | Erwartetes Ergebnis |
|---------|----------|-------------|-------------------|
| IT-PN-01 | `testBatteryUsageDuringActiveTime` | Batterieverbrauch während aktiver Zeit | Batterieverbrauch innerhalb akzeptabler Grenzen |
| IT-PN-02 | `testMemoryUsage` | Speichernutzung | Speichernutzung bleibt innerhalb akzeptabler Grenzen |
| IT-PN-03 | `testCPUUsage` | CPU-Nutzung | CPU-Auslastung bleibt innerhalb akzeptabler Grenzen |

## Manuelle Tests

### Funktionalitätstests

| Test-ID | Testfall | Schritte | Erwartetes Ergebnis |
|---------|----------|---------|-------------------|
| MT-01 | Erfassungszeit beginnen | 1. Erfassungszeit konfigurieren<br>2. Warten bis zum Beginn | Benachrichtigung erscheint pünktlich |
| MT-02 | Erfassungszeit beenden | 1. Warten bis zum Ende einer aktiven Erfassungszeit | Benachrichtigung ändert sich pünktlich |
| MT-03 | Erinnerungsbenachrichtigung | 1. "Noise Reminder" in Einstellungen aktivieren<br>2. Warten auf Erinnerung | Erinnerungsbenachrichtigung wird angezeigt |
| MT-04 | Geräteneustart während aktiver Zeit | 1. Aktive Erfassungszeit<br>2. Gerät neustarten | Benachrichtigung wird nach Neustart wiederhergestellt |
| MT-05 | Zeitänderung | 1. Systemzeit oder -zeitzone ändern | Benachrichtigungen werden korrekt angepasst |

### Berechtigungstests

| Test-ID | Testfall | Schritte | Erwartetes Ergebnis |
|---------|----------|---------|-------------------|
| MT-06 | Benachrichtigungsberechtigung entziehen | 1. In Systemeinstellungen Benachrichtigungsberechtigung entziehen | App reagiert angemessen |
| MT-07 | Exakte Alarmberechtigung entziehen (Android 12+) | 1. In Systemeinstellungen exakte Alarmberechtigung entziehen | Fallback auf ungenaue Alarme funktioniert |
| MT-08 | Batterieoptimierung aktivieren | 1. App zur Batterieoptimierungsliste hinzufügen | Benachrichtigungssystem funktioniert trotzdem zuverlässig |

### Spezifische Android-Version-Tests

| Test-ID | Testfall | Android-Version | Erwartetes Ergebnis |
|---------|----------|----------------|-------------------|
| MT-09 | Systemfunktionalität | Android 11 | System funktioniert ohne Einschränkungen |
| MT-10 | Exakte Alarme | Android 12 | System fragt nach Berechtigung für exakte Alarme |
| MT-11 | Benachrichtigungsberechtigungen | Android 13 | System fragt nach Benachrichtigungsberechtigungen |
| MT-12 | Foreground Service | Android 14 | Foreground Service funktioniert korrekt |
| MT-13 | Foreground Service Beschränkungen | Android 15 | System bleibt innerhalb der neuen Beschränkungen funktionsfähig |

## Regressionstests

| Test-ID | Testfall | Beschreibung | Erwartetes Ergebnis |
|---------|----------|-------------|-------------------|
| RT-01 | WorkManager Integration | Überprüfen, ob das hybride System mit bestehenden WorkManager-Funktionen kompatibel ist | Keine negativen Auswirkungen auf andere WorkManager-Aufgaben |
| RT-02 | Benachrichtigungsinteraktion | Überprüfen, ob das Tippen auf Benachrichtigungen korrekt funktioniert | App öffnet sich mit korrekter Ansicht |
| RT-03 | Benachrichtigungsaktionen | Überprüfen, ob Benachrichtigungsaktionen korrekt funktionieren | Aktionen werden korrekt ausgeführt |

## Fehler- und Edge-Case-Tests

| Test-ID | Testfall | Beschreibung | Erwartetes Ergebnis |
|---------|----------|-------------|-------------------|
| ECT-01 | Gleichzeitiger Start mehrerer Services | Mehrere Service-Starts in kurzer Folge auslösen | Nur ein Service läuft, keine Duplikate |
| ECT-02 | Fehlende Erfassungszeiten | App mit leerer Datenbank für Erfassungszeiten starten | Keine Fehler, keine unnötigen Benachrichtigungen |
| ECT-03 | System tötet Service | Service während aktiver Zeit absichtlich beenden | WorkManager übernimmt zuverlässig |
| ECT-04 | Extrem langfristige Planung | Erfassungszeit weit in der Zukunft planen | AlarmManager plant korrekt neu nach Geräteneustart |
| ECT-05 | Schneller Wechsel von Erfassungszeiten | Schnell hintereinander verschiedene Erfassungszeiten konfigurieren | System reagiert korrekt ohne Fehler oder Abstürze |

## Automatisierungsansätze

### Unit-Test-Automatisierung

- Verwendung von JUnit und Robolectric für WorkManagerController und alle Komponenten
- Mocking von SystemServices und ContentResolver
- Einrichten eines CI/CD-Laufs für automatische Testausführung

### Beispiel für einen Unit-Test mit Robolectric:

```java
@Test
public void testWorkerExecution() {
    // Leeren Cursor für RecurringDAO vorbereiten
    MatrixCursor emptyCursor = new MatrixCursor(RecurringDAO.PROJECTION);
    when(mockContentResolver.query(
        eq(RecurringDAO.CONTENT_URI),
        eq(RecurringDAO.PROJECTION),
        any(),
        any(),
        any())).thenReturn(emptyCursor);

    // Worker erstellen und ausführen
    NotificationWorker worker = TestListenableWorkerBuilder.from(context, NotificationWorker.class)
        .build();

    ListenableWorker.Result result = worker.doWork();

    // Die Ausführung sollte erfolgreich sein
    assertEquals(ListenableWorker.Result.success(), result);

    // Überprüfen, ob ContentResolver abgefragt wurde
    verify(mockContentResolver, times(1)).query(
        eq(RecurringDAO.CONTENT_URI),
        eq(RecurringDAO.PROJECTION),
        any(),
        any(),
        any());
}
```

### Instrumentation-Test-Automatisierung

- Verwendung von Espresso und Android Testing Framework
- Einrichtung eines Testlaufs auf Firebase Test Lab für verschiedene Geräte

### Beispiel für einen Benachrichtigungstest mit Unibot

```java
@Test
public void testNotificationDisplayed() {
    // Voraussetzungen schaffen (Erfassungszeit konfigurieren)
    configureAcquisitionTimeForNow();

    // Warten, bis Benachrichtigung erscheint
    Unibot.assertThat(notifications())
          .withTimeout(5000)
          .anyMatch(notification -> 
              notification.getTitle().contains("Agime") && 
              notification.getContent().contains("aktiv"))
          .eventually();

    // Überprüfen der Benachrichtigungsdetails
    Notification notification = notifications().stream()
        .filter(n -> n.getTitle().contains("Agime"))
        .findFirst()
        .orElseThrow();

    assertThat(notification.getOngoing()).isTrue();
    assertThat(notification.getPriority()).isEqualTo(NotificationManager.IMPORTANCE_HIGH);
}
```

## Test-Matrix

| Komponente | Unit | Instrumentation | Manuell |
|------------|------|----------------|---------|
| WorkManagerController | 6 | 0 | 3 |
| NotificationWorker | 5 | 2 | 4 |
| NotificationAlarmReceiver | 4 | 1 | 3 |
| ShortLivedNotificationService | 5 | 3 | 5 |
| NotificationBroadcastReceiver | 4 | 1 | 2 |
| System Integration | 0 | 4 | 8 |
| **Total** | **24** | **11** | **25** |

## Berichterstattung

### Metriken zur Testabdeckung
- Line Coverage: Angestrebt >85% für alle Komponenten
- Branch Coverage: Angestrebt >80% für alle Komponenten
- Komplexität: Überwachung der zyklomatischen Komplexität der Komponenten

### Bug-Tracking
- Jeder gefundene Fehler wird mit Priorität, Reproduktionsschritten und erwarteten/tatsächlichen Ergebnissen dokumentiert
- Bugs werden kategorisiert nach:
  - Kritisch: Systemfehler, keine Benachrichtigungen
  - Hoch: Falsche Zeitpunkte, verpasste Benachrichtigungen
  - Mittel: UI-Probleme, falsche Texte
  - Niedrig: Kosmetische Probleme

## Testplan-Aktualisierung

Dieser Testplan wird aktualisiert:
- Bei Änderungen an der Architektur des Benachrichtigungssystems
- Nach Identifizierung neuer Testfälle durch gefundene Bugs
- Bei Änderungen der Android-Plattform, die das Benachrichtigungssystem betreffen

## Mitwirkende
- Agime Test-Team
- Agime Entwicklungsteam

## Änderungshistorie
- **Mai 2025:** Erste Version des Testplans
- **Juni 2025:** (Geplant) Aktualisierung mit Ergebnissen der ersten Testläufe
