# WorkManager-Migration für Agime Benachrichtigungen

## Übersicht

Diese Änderungen ersetzen den bestehenden `NotificationManagingService` (mit `foregroundServiceType="dataSync"`) durch eine moderne WorkManager-basierte Implementierung.

## Warum die Migration?

Die aktuelle Implementierung nutzt `foregroundServiceType="dataSync"`, was für folgende Probleme sorgt:

1. In Android 15+ werden Foreground-Services vom Typ "dataSync" auf eine maximale Laufzeit von 6 Stunden beschränkt.
2. Ab Android 15 können solche Services nicht mehr durch BOOT_COMPLETED Broadcast Receiver gestartet werden.
3. Der "dataSync" Typ ist eigentlich für Datenübertragungsoperationen gedacht, während unsere App lediglich Benachrichtigungen anzeigen möchte.

## Wichtige Komponenten der neuen Implementierung

1. `NotificationWorker.java` - Ersetzt den alten Service mit einem WorkManager Worker, der Benachrichtigungen erstellt und anzeigt
2. `WorkManagerController.java` - Verwaltet die Zeitplanung und Ausführung des Workers
3. `NotificationBroadcastReceiver.java` - Ersetzt den alten OngoingNotificationManagingReceiver und interagiert mit dem WorkManagerController

## Integration in die App

Die neue Implementierung:

- Wird bei App-Start und Geräteneustart automatisch initialisiert
- Reagiert auf Änderungen der Benutzereinstellungen
- Aktualisiert Benachrichtigungen basierend auf AcquisitionTimes und Benutzeraktivitäten
- Benötigt keine bindable Service-Komponenten mehr
- Nutzt den modernen WorkManager für effizienten Batterieverbrauch

## Testabdeckung

Die Implementierung ist mit umfangreichen Tests abgedeckt:

- **WorkManagerNotificationTest**: Grundlegende Tests für die Worker- und Controller-Funktionalität
- **NotificationSystemIntegrationTest**: End-to-End-Tests für die Integration der verschiedenen Komponenten
- **NotificationEdgeCasesTest**: Tests für Randfälle und Fehlerbehandlung (SQL-Fehler, NULL-Cursors, etc.)
- **NotificationPerformanceTest**: Performance-Tests mit großen Datenmengen

Die Tests decken folgende Szenarien ab:
- Erfolgreiche Ausführung des Workers
- Korrekte Verarbeitung von Intents durch den Controller und BroadcastReceiver
- Fehlerhafte Datenbankabfragen und deren Behandlung
- Verhalten bei deaktivierten Benachrichtigungen
- Performance bei vielen wiederkehrenden Erfassungszeiten

## Bekannte Probleme und ToDo

- Die WorkManager-Implementierung plant Ausführungen aktuell in Intervallen von mindestens 15 Minuten (WorkManager-Einschränkung)
- Die Tests könnten mit einer In-Memory-Datenbank erweitert werden, um einen realistischeren Testaufbau zu ermöglichen

## Migrationsschritte

Die folgenden Dateien wurden geändert:
- AgimeApplication.java
- AndroidManifest.xml
- AgimeMainActivity.java
- TrackActivity.java
- SettingsActivity.java
- RecurringAcquisitionTimeEditorFragment.java

Neue Dateien:
- NotificationWorker.java - Worker für die Erstellung der Benachrichtigungen
- WorkManagerController.java - Controller für die Planung und Verwaltung
- NotificationBroadcastReceiver.java - BroadcastReceiver für System-Events

Test-Dateien:
- WorkManagerNotificationTest.java - Allgemeine Tests für den Worker und Controller
- NotificationSystemIntegrationTest.java - Integration der verschiedenen Komponenten
- NotificationEdgeCasesTest.java - Tests für Randfälle und Fehlerbehandlung
- NotificationPerformanceTest.java - Tests der Performance und Ressourcennutzung
