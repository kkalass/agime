package de.kalass.agime;

import android.content.Intent;

/**
 * Created by klas on 19.12.13.
 */
public final class AgimeIntents {
    private AgimeIntents() {}

    /**
     * An activity was created that may have been called from outside of Agime
     */
    public static final String ACTION_FIRST_START_AFTER_INSTALL_OR_UPGRADE = "de.kalass.agime.intent.action.FIRST_START_AFTER_INSTALL_OR_UPGRADE";

    /**
     * Acquisition Time has started, stopped or some other reason to refresh the notification
     */
    public static final String ACTION_REFRESH_ACQUISITION_TIME_NOTIFICATION = "de.kalass.agime.intent.action.ACQUISITION_TIME_REFRESH_NOTIFICATION";

    public static final String ACTION_ACQUISITION_TIME_CONFIGURE = "de.kalass.agime.intent.action.ACQUISITION_TIME_CONFIGURE";

    public static final String ACTION_AUTOMATIC_BACKUP = "de.kalass.agime.intent.action.AUTOMATIC_BACKUP_START";

    /**
     * Tests if the given intent is one of the events that will be fired when e. g. AlarmManager setup
     * should be performed. Typically this will be the case after system boot, package replacement and first launch.
     *
     * @param intent the intent
     * @return if the intent represents an action that should lead to initialization of long-running services
     */
    public static boolean isInitializingIntent(Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return true;
        } else if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            return true;
        } else if (AgimeIntents.ACTION_FIRST_START_AFTER_INSTALL_OR_UPGRADE.equals(intent.getAction())) {
            return true;
        }
        return false;
    }
}
