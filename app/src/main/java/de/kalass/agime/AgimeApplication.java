package de.kalass.agime;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Configuration;

import java.util.concurrent.Executors;

import de.kalass.agime.ongoingnotification.WorkManagerController;


/**
 * Created by klas on 27.12.14.
 */
public class AgimeApplication extends Application implements Configuration.Provider {

	private static final String LOG_TAG = "AgimeApplication";

	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(LOG_TAG, "AgimeApplication onCreate");

		// WorkManager für Benachrichtigungen initialisieren
		WorkManagerController.initialize(getApplicationContext());
	}

	@NonNull
	@Override
	public Configuration getWorkManagerConfiguration() {
		return new Configuration.Builder()
				.setExecutor(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()))
				.setMinimumLoggingLevel(android.util.Log.INFO) // Optional: for debugging
				.build();
	}
}
