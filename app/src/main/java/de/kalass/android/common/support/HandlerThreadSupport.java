package de.kalass.android.common.support;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.HandlerThread;

/**
 * Created by klas on 23.12.13.
 */
public class HandlerThreadSupport {

    public static void quitSafelyIfPossible(HandlerThread thread) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            quitSafelyNew(thread);
        } else {
            thread.quit();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private static void quitSafelyNew(HandlerThread thread) {
        thread.quitSafely();
    }
}
