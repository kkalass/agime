package de.kalass.agime.util;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

/**
 * Helper class for managing alarm-related permissions
 */
public class AlarmPermissionHelper {
    
    /**
     * Check if the app has the SCHEDULE_EXACT_ALARM permission
     */
    public static boolean hasExactAlarmPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            return alarmManager.canScheduleExactAlarms();
        }
        // Below Android 12, no runtime permission is needed
        return true;
    }
    
    /**
     * Open the system settings where the user can grant the SCHEDULE_EXACT_ALARM permission
     */
    public static void requestExactAlarmPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
    
    /**
     * Check if the permission can be requested (i.e., if we should show UI to request it)
     */
    @RequiresApi(api = Build.VERSION_CODES.S)
    public static boolean canRequestExactAlarmPermission(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return !alarmManager.canScheduleExactAlarms() && 
               context.getPackageManager().isAutoRevokeWhitelisted();
    }
}
