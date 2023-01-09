package de.kalass.android.common.support;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by klas on 19.12.13.
 */
public class AlarmManagerSupport {

    private static final String LOG_TAG = "AlarmManagerSupport";

    public static void setExact(AlarmManager am, int type, long triggerAt, PendingIntent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            setExactNew(am, type, triggerAt, intent);
        } else {
            am.set(type, triggerAt, intent);
        }

    }

    private static void setExactNew(AlarmManager am, int type, long triggerAt, PendingIntent intent) {

        try {
            Method setExact = am.getClass().getMethod("setExact", int.class, long.class, PendingIntent.class);
            try {
                setExact.invoke(am, type, triggerAt, intent);
            } catch (IllegalAccessException e) {
                Log.w(LOG_TAG, "Error while calling setExact(), resorting to set", e);
                am.set(type, triggerAt, intent);
            } catch (InvocationTargetException e) {
                Log.w(LOG_TAG, "Error while calling setExact(), resorting to set", e);
                am.set(type, triggerAt, intent);
            }
        } catch (NoSuchMethodException e) {
            Log.w(LOG_TAG, "setExact() not available, resorting to set", e);
            am.set(type, triggerAt, intent);
        }
    }
}
