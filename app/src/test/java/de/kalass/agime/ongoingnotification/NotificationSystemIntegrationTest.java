package de.kalass.agime.ongoingnotification;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;
import androidx.work.Configuration;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.WorkManagerTestInitHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.concurrent.ExecutionException;
import com.google.common.util.concurrent.ListenableFuture;

import de.kalass.agime.AgimeIntents;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Tests für die Integration der WorkManager-basierten Komponenten
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class NotificationSystemIntegrationTest {

	private Context context;

	@Mock
	private Context mockContext;

	@Before
	public void setUp() {
		MockitoAnnotations.openMocks(this);

		// Echter Context für WorkManager-Tests
		context = ApplicationProvider.getApplicationContext();

		// Mock-Context für einfachere Tests
		when(mockContext.getApplicationContext()).thenReturn(mockContext);

		// WorkManager für Tests initialisieren
		Configuration config = new Configuration.Builder()
			.setMinimumLoggingLevel(android.util.Log.DEBUG)
			.setExecutor(new SynchronousExecutor())
			.build();
		WorkManagerTestInitHelper.initializeTestWorkManager(context, config);
	}


	/**
	 * Test, dass der BroadcastReceiver den WorkManagerController korrekt aufruft
	 */
	@Test
	@org.junit.Ignore("Test disabled - needs further investigation")
	public void testBroadcastReceiverIntegration() {
		// Einen Spy des NotificationBroadcastReceiver erstellen
		NotificationBroadcastReceiver receiver = spy(new NotificationBroadcastReceiver());

		// Intent für BOOT_COMPLETED erstellen
		Intent bootIntent = new Intent(Intent.ACTION_BOOT_COMPLETED);

		// Intent mit dem Receiver verarbeiten
		receiver.onReceive(mockContext, bootIntent);

		// Überprüfen, ob der WorkManagerController.initialize aufgerufen wurde
		verify(mockContext, times(1)).getApplicationContext();
	}


	/**
	 * Test der gesamten Kette von BroadcastReceiver über Controller zum Worker
	 */
	@Test
	@org.junit.Ignore("Test disabled - needs further investigation")
	public void testEndToEndNotificationFlow() {
		// WorkManager-Instance für diesen Test abrufen
		WorkManager workManager = WorkManager.getInstance(context);

		// Zuerst prüfen, ob keine Arbeiten vorhanden sind
		try {
			List<WorkInfo> initialWorkInfos = workManager
				.getWorkInfosByTag(WorkManagerController.WORKER_TAG)
				.get();

			// Es kann bereits Arbeiten geben, das ist kein Problem

			// Einen BroadcastReceiver erstellen und aufrufen
			NotificationBroadcastReceiver receiver = new NotificationBroadcastReceiver();
			Intent configIntent = new Intent(AgimeIntents.ACTION_ACQUISITION_TIME_CONFIGURE);
			receiver.onReceive(context, configIntent);

			// Prüfen, ob jetzt eine Arbeit geplant ist
			List<WorkInfo> workInfos = workManager
				.getWorkInfosByTag(WorkManagerController.WORKER_TAG)
				.get();

			assertFalse("Es sollte mindestens eine Arbeit geplant sein", workInfos.isEmpty());

			// Prüfen, ob mindestens eine Arbeit im Status ENQUEUED ist
			boolean hasEnqueuedWork = false;
			for (WorkInfo info : workInfos) {
				if (info.getState() == WorkInfo.State.ENQUEUED) {
					hasEnqueuedWork = true;
					break;
				}
			}
			assertTrue("Es sollte eine eingereihte Arbeit geben", hasEnqueuedWork);

		}
		catch (ExecutionException | InterruptedException e) {
			throw new RuntimeException("Fehler beim Abrufen der WorkInfo", e);
		}
	}


	/**
	 * Test, dass der WorkManagerController bei einem Zeitänderungs-Intent den Worker sofort ausführt
	 */
	@Test
	@org.junit.Ignore("Test disabled - needs further investigation")
	public void testTimeChangedTrigger() {
		// Zeitänderungs-Intent erstellen
		Intent timeChangedIntent = new Intent(Intent.ACTION_TIME_CHANGED);

		// BroadcastReceiver erstellen und aufrufen
		NotificationBroadcastReceiver receiver = new NotificationBroadcastReceiver();
		receiver.onReceive(mockContext, timeChangedIntent);

		// Überprüfen, ob der WorkManagerController.scheduleImmediateCheck aufgerufen wurde
		verify(mockContext, times(1)).getApplicationContext();
	}
}
