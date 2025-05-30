package de.kalass.agime;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import de.kalass.agime.analytics.AnalyticsActionBarActivity;


/**
 * Created by klas on 22.11.13.
 */
public abstract class MainEntryPointActivity extends AnalyticsActionBarActivity {

	private static final String LOG_TAG = "MainEntryPointActivity";

	private static final String KEY_LAST_BUILD_VERSION = "keyLastBuildVersion";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		notifyAfterInstallOrUpgrade();
	}


	private void notifyAfterInstallOrUpgrade() {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		int lastBuildVersion = sharedPref.getInt(KEY_LAST_BUILD_VERSION, 0);
		//Log.i(LOG_TAG, "lastBuildVersion: " + lastBuildVersion);
		try {
			PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			//Log.i(LOG_TAG, "packageInfo.versionCode: " + packageInfo.versionCode);
			if (lastBuildVersion != packageInfo.versionCode) {
				sharedPref.edit().putInt(KEY_LAST_BUILD_VERSION, packageInfo.versionCode).commit();
				Intent ongoingNotificationManagerIntent = new Intent(AgimeIntents.ACTION_FIRST_START_AFTER_INSTALL_OR_UPGRADE);
				Log.i(LOG_TAG, "will broadcast: " + ongoingNotificationManagerIntent);
				sendBroadcast(ongoingNotificationManagerIntent);

				onAfterInstallOrUpgrade(lastBuildVersion);
			}
		}
		catch (PackageManager.NameNotFoundException e) {
			Log.e(LOG_TAG, "Install/Upgrade check failed!", e);
		}
	}


	protected void onAfterInstallOrUpgrade(int previouslyInstalledBuildVersion) {
	}
}
