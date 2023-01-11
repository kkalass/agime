package de.kalass.agime.backup;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

/**
 * Created by klas on 13.02.14.
 */
public class ServiceUtil {
    private static final int SERVICE_FAILURE_NOTIFICATION_ID = 32;

    /**
     * If there is an error in the backend that needs to be shown to the user.
     * @param target The activity to open if the user clicks the error message
     */
    public static void showServiceError(Context context,
                                        Class<? extends Activity> target,
                                        int notifTitleResId, int notifTextResId) {
        showServiceError(context, target, context.getString(notifTitleResId), context.getString(notifTextResId));
    }

    /**
     * If there is an error in the backend that needs to be shown to the user.
     * @param intent The intent to open if the user clicks the error message
     */
    public static void showServiceError(Context context,
                                        PendingIntent intent,
                                        int notifTitleResId, int notifTextResId) {
        showServiceError(context, intent, context.getString(notifTitleResId), context.getString(notifTextResId));
    }

    /**
     * If there is an error in the backend that needs to be shown to the user.
     * @param target The activity to open if the user clicks the error message
     */
    public static void showServiceError(
            Context context, Class<? extends Activity> target,
            String notifTitle, String notifText
    ) {
        showServiceError(context, createContentIntent(context, target), notifTitle, notifText);
    }

    public static void showServiceError(
            Context context,
            PendingIntent intent,
            String notifTitle, String notifText
    ) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentText(notifText);
        builder.setContentTitle(notifTitle);
        builder.setSmallIcon(de.kalass.agime.R.drawable.ic_launcher);
        builder.setContentIntent(intent);
        builder.setAutoCancel(true);
        NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle();
        style.setSummaryText(notifText);
        style.setBigContentTitle(notifTitle);
        builder.setStyle(style);
        NotificationManager ns = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        ns.notify(SERVICE_FAILURE_NOTIFICATION_ID, builder.build());
    }

    private static PendingIntent createContentIntent(Context context, Class<? extends Activity> target) {
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack
        stackBuilder.addParentStack(target);
        // Adds the Intent to the top of the stack
        stackBuilder.addNextIntent(new Intent(context, target));

        return stackBuilder.getPendingIntent(SERVICE_FAILURE_NOTIFICATION_ID, PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }

}
