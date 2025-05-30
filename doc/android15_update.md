# Aktualisierung des Benachrichtigungssystems für Android 15+

## Änderungen vom 30. Mai 2025

Die folgenden Verbesserungen wurden am hybriden Benachrichtigungssystem vorgenommen, um die volle Kompatibilität mit Android 15+ zu gewährleisten und gleichzeitig die Unterstützung für ältere Android-Versionen beizubehalten.

### API-Level-spezifischer Foreground-Service-Typ

1. **Für Android 15+ (API Level 35+)**
   - Nutzung des neuen `shortService`-Typs, der speziell für kurzlebige Foreground-Services entwickelt wurde
   - Hinzufügung der erforderlichen `FOREGROUND_SERVICE_SHORT_SERVICE`-Berechtigung

2. **Für ältere Android-Versionen**
   - Beibehaltung des `dataSync`-Typs als beste verfügbare Alternative
   - Keine zusätzlichen Berechtigungen erforderlich

### Technische Implementierung

Die Lösung verwendet den Android-Ressourcenmechanismus, um die Konfiguration abhängig vom API-Level anzupassen:

1. **Ressourcen-Definition**
   - Standardwert in `values/foreground_service_types.xml`: `dataSync`
   - Spezifischer Wert in `values-v35/foreground_service_types.xml`: `shortService`

2. **Manifest-Konfiguration**
   - Referenzierung der Ressource mit `@string/foreground_service_type`
   - Deklaration beider erforderlicher Berechtigungen

```xml
<!-- Auszug aus dem Manifest -->
<service
    android:name="de.kalass.agime.ongoingnotification.ShortLivedNotificationService"
    android:enabled="true"
    android:exported="false"
    android:foregroundServiceType="@string/foreground_service_type">
</service>
```

### Vorteile des Ansatzes

1. **Zukunftssicherheit**
   - Automatische Verwendung des am besten geeigneten Service-Typs je nach Android-Version
   - Vollständige Kompatibilität mit Android 15+ und dessen Einschränkungen für Foreground-Services

2. **Rückwärtskompatibilität**
   - Funktioniert nahtlos auf älteren Android-Versionen
   - Keine API-Fehler oder Abstürze durch Verwendung nicht verfügbarer Konstanten

3. **Richtlinientreue**
   - Einhaltung der neuesten Android-Richtlinien für Foreground-Services
   - Deklaration aller erforderlichen Berechtigungen

### Zusätzliche Hinweise

1. **Wartbarkeit**
   - Änderung des Service-Typs in einer einzigen Ressourcendatei ohne Änderung des Manifests
   - Leichte Anpassbarkeit für zukünftige Android-Versionen

2. **Tests**
   - Tests sollten sowohl auf älteren Android-Versionen als auch auf Android 15+ durchgeführt werden
   - Überprüfung der korrekten Auswahl des Service-Typs je nach API-Level

Diese Änderungen ergänzen die bestehenden Vorteile des hybriden Benachrichtigungssystems, wie zeitlich begrenzte Service-Laufzeit, präzise Zeitsteuerung durch AlarmManager und langfristige Zuverlässigkeit durch WorkManager.
