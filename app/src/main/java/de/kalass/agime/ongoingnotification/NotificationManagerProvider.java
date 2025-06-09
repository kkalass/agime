package de.kalass.agime.ongoingnotification;

import android.content.Context;
import androidx.core.app.NotificationManagerCompat;


/**
 * Interface for notification management operations, enabling dependency injection and better testability. Abstracts
 * notification manager creation and operations to allow for proper unit testing.
 */
public interface NotificationManagerProvider {

	/**
	 * Creates a NotificationManagerCompat instance for the given context.
	 * 
	 * @param context The application context
	 * @return NotificationManagerCompat instance
	 */
	NotificationManagerCompat getNotificationManager(Context context);
}
