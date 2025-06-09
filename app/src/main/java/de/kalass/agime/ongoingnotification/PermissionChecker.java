package de.kalass.agime.ongoingnotification;

import android.content.Context;


/**
 * Interface for checking permissions, enabling dependency injection and better testability. Abstracts static permission
 * checking methods to allow for proper unit testing.
 */
public interface PermissionChecker {

	/**
	 * Checks if the application has the specified permission.
	 * 
	 * @param context The application context
	 * @param permission The permission to check (e.g., Manifest.permission.POST_NOTIFICATIONS)
	 * @return true if permission is granted, false otherwise
	 */
	boolean hasPermission(Context context, String permission);
}
