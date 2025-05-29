package de.kalass.android.common.support;

import android.app.AlarmManager;
import android.app.PendingIntent;

/**
 * Created by klas on 19.12.13.
 */
public class AlarmManagerSupport {

    private static final String LOG_TAG = "AlarmManagerSupport";

    public static void setAlarm(AlarmManager am, int type, long triggerAt, PendingIntent intent) {
        am.set(type, triggerAt, intent);
    }

}
