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

- Grundlegende Unit-Tests für den Worker und den Controller wurden hinzugefügt
- Weitere Tests sollten vor der Veröffentlichung hinzugefügt werden

## Bekannte Probleme und ToDo

- Die neue Implementierung benötigt noch detailliertere Tests
- Die WorkManager-Implementierung plant Ausführungen aktuell in Intervallen von mindestens 15 Minuten (WorkManager-Einschränkung)

## Migrationsschritte

Die folgenden Dateien wurden geändert:
- AgimeApplication.java
- AndroidManifest.xml
- AgimeMainActivity.java
- TrackActivity.java
- SettingsActivity.java
- RecurringAcquisitionTimeEditorFragment.java

Neue Dateien:
- NotificationWorker.java
- WorkManagerController.java
- NotificationBroadcastReceiver.java
- WorkManagerNotificationTest.java
