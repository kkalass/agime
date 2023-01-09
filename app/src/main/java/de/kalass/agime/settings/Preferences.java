package de.kalass.agime.settings;

import android.content.Context;
import android.util.Log;

import de.kalass.agime.overview.model.GroupHeaderTypes;

import static de.kalass.agime.settings.PreferencesBase.getBoolean;
import static de.kalass.agime.settings.PreferencesBase.getInt;
import static de.kalass.agime.settings.PreferencesBase.getString;
import static de.kalass.agime.settings.PreferencesBase.setBoolean;
import static de.kalass.agime.settings.PreferencesBase.setInt;

/**
 * Created by klas on 22.11.13.
 */
public class Preferences {
    private static final String KEY_PREF_LEVEL_1_GROUP_TYPE = "pref_key_overview_level1_group_type";
    private static final String KEY_PREF_LEVEL_2_GROUP_TYPE = "pref_key_overview_level2_group_type";
    private static final String KEY_PREF_LEVEL_3_GROUP_TYPE = "pref_key_overview_level3_group_type";

    private static final String KEY_PREF_PROJECT_COLLAPSED = "pref_key_project_collapsed_";

    public static final String KEY_PREF_ACQUISITION_TIME_NOTIFICATION = "pref_acquisition_time_notification";
    public static final String KEY_PREF_ACQUISITION_TIME_NOTIFICATION_INTERVAL =
            "pref_acquisition_time_notification_noise_interval";

    public static final String KEY_PREF_KEEP_SCREEN_ON = "pref_keep_screen_on";

    private static final String KEY_CACHED_NUM_CUSTOM_FIELD_TYPES = "CachedNumCustomFieldTypes";
    private static final String LOG_TAG = "Preferences";

    public static boolean isLevel1Collapsed(Context context, int groupId, long projectId) {
        return getBoolean(context, KEY_PREF_PROJECT_COLLAPSED + "/" + groupId + "/" + projectId, false);
    }

    public static void setLevel1Collapsed(Context context, int groupId, long projectId, boolean value) {
        setBoolean(context, KEY_PREF_PROJECT_COLLAPSED + "/" + groupId + "/" +projectId, value);
    }

    public static void setLevel1GroupTypeId(Context context, int groupTypeId) {
        setInt(context, KEY_PREF_LEVEL_1_GROUP_TYPE, groupTypeId);
    }

    public static int getLevel1GroupTypeId(Context context) {
        return getInt(context, KEY_PREF_LEVEL_1_GROUP_TYPE, GroupHeaderTypes.Project.GROUP_TYPE);
    }

    public static void setLevel2GroupTypeId(Context context, int groupTypeId) {
        setInt(context, KEY_PREF_LEVEL_2_GROUP_TYPE, groupTypeId);
    }

    public static int getLevel2GroupTypeId(Context context) {
        return getInt(context, KEY_PREF_LEVEL_2_GROUP_TYPE, GroupHeaderTypes.Category.GROUP_TYPE);
    }

    public static void setLevel3GroupTypeId(Context context, int groupTypeId) {
        setInt(context, KEY_PREF_LEVEL_3_GROUP_TYPE, groupTypeId);
    }

    public static int getLevel3GroupTypeId(Context context) {
        return getInt(context, KEY_PREF_LEVEL_3_GROUP_TYPE, GroupHeaderTypes.ActivityType.GROUP_TYPE);
    }

    public static int getAcquisitionTimeNotificationNoiseThresholdMinutes(Context context) {
        int defaultNum = 120;
        final String s = getString(context, KEY_PREF_ACQUISITION_TIME_NOTIFICATION_INTERVAL, Integer.toString(defaultNum));
        try {
            return Integer.parseInt(s, 10);
        } catch (IllegalArgumentException e) {
            Log.w(LOG_TAG, "Failed to convert stored value for number of ,omites, will use default value", e);
            return defaultNum;
        }
    }


    public static boolean isAcquisitionTimeNotificationEnabled(Context context) {
        return getBoolean(context, KEY_PREF_ACQUISITION_TIME_NOTIFICATION, true);
    }


    public static boolean isKeepScreenOn(Context context) {
        return getBoolean(context, KEY_PREF_KEEP_SCREEN_ON, false);
    }

    public static void setCachedNumCustomFieldTypes(Context context, int size) {
        setInt(context, KEY_CACHED_NUM_CUSTOM_FIELD_TYPES, size);
    }

    public static int getCachedNumCustomFieldTypes(Context context) {
        return getInt(context, KEY_CACHED_NUM_CUSTOM_FIELD_TYPES, 0);
    }
}
