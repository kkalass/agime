package de.kalass.agime.ongoingnotification;

import android.content.Context;
import androidx.core.app.NotificationManagerCompat;


/**
 * Default implementation of NotificationManagerProvider that delegates to Android's NotificationManagerCompat. Used in
 * production code for actual notification management.
 */
public class DefaultNotificationManagerProvider implements NotificationManagerProvider {

	@Override
	public NotificationManagerCompat getNotificationManager(Context context) {
		return NotificationManagerCompat.from(context);
	}
}
