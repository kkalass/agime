package de.kalass.agime;

import android.app.Application;
import android.util.Log;

import de.kalass.agime.ongoingnotification.WorkManagerController;


/**
 * Created by klas on 27.12.14.
 */
public class AgimeApplication extends Application {

	private static final String LOG_TAG = "AgimeApplication";

	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(LOG_TAG, "AgimeApplication onCreate");

		// WorkManager für Benachrichtigungen initialisieren
		WorkManagerController.initialize(getApplicationContext());
	}
}
