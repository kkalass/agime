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

# Android 15 Edge-to-Edge Migration

## Problem mit veralteten APIs (Juni 2025)
Die Google Play Console zeigte eine Warnung über veraltete APIs in Android 15:

```
Deine App verwendet nicht mehr unterstützte APIs oder Parameter für die randlose Anzeige
Mindestens eine der von dir verwendeten APIs oder Parameter, für die du die Nutzung der randlosen und der Fensteranzeige festgelegt hast, wurde in Android 15 eingestellt. Deine App verwendet die folgenden nicht mehr unterstützten APIs oder Parameter:

android.view.Window.setStatusBarColor
android.view.Window.setNavigationBarColor
```

## Lösung
Die `EdgeToEdgeHelper` Klasse wurde angepasst, um mit Android 15 kompatibel zu sein, während die Rückwärtskompatibilität zu älteren Android-Versionen erhalten bleibt.

### Änderungen

#### 1. Android 15+ (API 35+) - setupEdgeToEdgeV35()
- **Vorher**: Verwendete `setStatusBarColor()` und `setNavigationBarColor()` mit spezifischen Farben
- **Nachher**: Verwendet transparente System-Bars (`Color.TRANSPARENT`) und `WindowCompat.setDecorFitsSystemWindows()`
- **Grund**: Die direkte Farbsetzung ist in Android 15 veraltet und funktioniert nicht mehr

#### 2. Android 11-14 (API 30-34) - setupEdgeToEdgeV30()
- **Vorher**: Verwendete `setStatusBarColor()` mit spezifischer Farbe
- **Nachher**: Verwendet transparente System-Bars für Konsistenz mit Android 15+
- **Grund**: Einheitliches Verhalten über alle modernen Android-Versionen

#### 3. Android < 11 (API < 30) - setupStatusBarColorForOlderVersions()
- **Unverändert**: Verwendet weiterhin `setStatusBarColor()`
- **Grund**: Diese APIs sind auf älteren Versionen noch nicht veraltet und funktionieren korrekt

#### 4. Status Bar Hintergrund
- Für Android 11+ wird die `addStatusBarBackground()` Methode verwendet
- Erstellt eine farbige View hinter der transparenten Status Bar
- Die View passt ihre Höhe automatisch an die Status Bar-Höhe an

### Implementierung

```java
@RequiresApi(api = Build.VERSION_CODES.VANILLA_ICE_CREAM)
private static void setupEdgeToEdgeV35(Activity activity) {
    // Android 15+ - use proper edge-to-edge API instead of deprecated setStatusBarColor
    WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
    
    // Make system bars transparent - this is the recommended approach for Android 15+
    activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
    activity.getWindow().setNavigationBarColor(Color.TRANSPARENT);
    
    // The status bar background color will be handled by addStatusBarBackground() method
}
```

### Vorteile

1. **Android 15 Kompatibilität**: Keine veralteten APIs mehr
2. **Rückwärtskompatibilität**: Ältere Android-Versionen funktionieren weiterhin
3. **Konsistentes Verhalten**: Einheitliches Edge-to-Edge-Verhalten über alle Versionen
4. **Zukunftssicher**: Folgt Googles empfohlenen Praktiken für moderne Android-Apps

### Tests
- Neue umfassende Tests in `EdgeToEdgeHelperTest` 
- Tests für verschiedene Android-Versionen (29, 30, 34)
- Bestehende Tests in `ResizableToolbarHelperTest` bleiben funktionsfähig

### Ergebnis
Die Google Play Console Warnung sollte nach dem nächsten App-Update verschwinden, da keine veralteten APIs mehr verwendet werden.
