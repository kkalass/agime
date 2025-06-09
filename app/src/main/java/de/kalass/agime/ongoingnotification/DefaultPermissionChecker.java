package de.kalass.agime.ongoingnotification;

import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;


/**
 * Default implementation of PermissionChecker that delegates to Android's ActivityCompat. Used in production code for
 * actual permission checking.
 */
public class DefaultPermissionChecker implements PermissionChecker {

	@Override
	public boolean hasPermission(Context context, String permission) {
		return ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
	}
}
