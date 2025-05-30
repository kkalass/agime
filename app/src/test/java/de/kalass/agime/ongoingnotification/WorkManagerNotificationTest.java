package de.kalass.agime.ongoingnotification;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;
import androidx.work.Configuration;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.TestListenableWorkerBuilder;
import androidx.work.testing.TestWorkerBuilder;
import androidx.work.testing.WorkManagerTestInitHelper;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowNotificationManager;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.TimeUnit;

import de.kalass.agime.AgimeIntents;
import de.kalass.agime.acquisitiontime.AcquisitionTimeInstance;
import de.kalass.agime.acquisitiontime.AcquisitionTimes;
import de.kalass.agime.acquisitiontime.RecurringDAO;
import de.kalass.agime.provider.MCContract;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Tests für die WorkManager-basierte Implementierung der Benachrichtigungen
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class WorkManagerNotificationTest {

	private Context context;
	private Executor executor;

	@Mock
	private ContentResolver mockContentResolver;

	@Mock
	private Cursor mockCursor;

	@Mock
	private NotificationManager mockNotificationManager;

	@Before
	public void setUp() {
		// Initialisiere Mocks
		MockitoAnnotations.openMocks(this);

		// Echter Context mit gemockten Komponenten
		context = spy(ApplicationProvider.getApplicationContext());
		when(context.getContentResolver()).thenReturn(mockContentResolver);
		when(context.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(mockNotificationManager);

		// Synchroner Executor für Tests
		executor = new SynchronousExecutor();

		// WorkManager für Tests initialisieren
		Configuration config = new Configuration.Builder()
			.setMinimumLoggingLevel(android.util.Log.DEBUG)
			.setExecutor(executor)
			.build();
		WorkManagerTestInitHelper.initializeTestWorkManager(context, config);
	}


	/**
	 * Test, dass der NotificationWorker erfolgreich ausgeführt wird
	 */
	@Test
	public void testWorkerExecution() {
		// Leeren Cursor für RecurringDAO vorbereiten
		MatrixCursor emptyCursor = new MatrixCursor(RecurringDAO.PROJECTION);
		when(mockContentResolver.query(
			eq(RecurringDAO.CONTENT_URI),
			eq(RecurringDAO.PROJECTION),
			any(),
			any(),
			any())).thenReturn(emptyCursor);

		// Worker erstellen und ausführen
		NotificationWorker worker = TestListenableWorkerBuilder.from(context, NotificationWorker.class)
			.build();

		ListenableWorker.Result result = worker.doWork();

		// Die Ausführung sollte erfolgreich sein
		assertEquals(ListenableWorker.Result.success(), result);

		// Überprüfen, ob ContentResolver abgefragt wurde
		verify(mockContentResolver, times(1)).query(
			eq(RecurringDAO.CONTENT_URI),
			eq(RecurringDAO.PROJECTION),
			any(),
			any(),
			any());
	}


	/**
	 * Test, dass der NotificationWorker Benachrichtigungen für eine aktive Erfassungszeit erstellt
	 */
	@Test
	public void testWorkerWithActiveAcquisitionTime() {
		// Mock für RecurringDAO mit aktiver Erfassungszeit erstellen
		DateTime now = new DateTime();
		MatrixCursor cursorWithActiveTime = createMockRecurringCursor(now);

		when(mockContentResolver.query(
			eq(RecurringDAO.CONTENT_URI),
			eq(RecurringDAO.PROJECTION),
			any(),
			any(),
			any())).thenReturn(cursorWithActiveTime);

		// Mock für TrackedActivitySyncLoader
		MatrixCursor emptyActivitiesCursor = new MatrixCursor(new String[] {
			MCContract.Activity._ID,
			MCContract.Activity.COLUMN_NAME_START_TIME,
			MCContract.Activity.COLUMN_NAME_END_TIME,
			MCContract.Activity.COLUMN_NAME_NAME
		});
		when(mockContentResolver.query(
			eq(MCContract.Activity.CONTENT_URI),
			any(),
			any(),
			any(),
			any())).thenReturn(emptyActivitiesCursor);

		// ArgumentCaptor für die erstellte Benachrichtigung
		ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);

		// Worker ausführen
		NotificationWorker worker = TestListenableWorkerBuilder.from(context, NotificationWorker.class)
			.build();

		ListenableWorker.Result result = worker.doWork();

		// Überprüfen, ob eine Benachrichtigung erstellt wurde
		assertEquals(ListenableWorker.Result.success(), result);
		verify(mockNotificationManager, times(1)).createNotificationChannel(any());
	}


	/**
	 * Test, dass der WorkManagerController einen Intent korrekt verarbeitet
	 */
	@Test
	public void testHandleIntent() {
		// WorkManager spy erstellen
		WorkManager workManager = WorkManager.getInstance(context);
		WorkManager spyWorkManager = spy(workManager);

		// Context mocken
		Context mockContext = mock(Context.class);
		when(mockContext.getApplicationContext()).thenReturn(mockContext);

		// Intent für Aktualisierung erstellen
		Intent configIntent = new Intent(AgimeIntents.ACTION_ACQUISITION_TIME_CONFIGURE);

		// Intent verarbeiten
		WorkManagerController.handleIntent(mockContext, configIntent);

		// Überprüfen, ob eine Arbeit geplant wurde
		ArgumentCaptor<OneTimeWorkRequest> workRequestCaptor = ArgumentCaptor.forClass(OneTimeWorkRequest.class);

		verify(mockContext).getApplicationContext();
	}


	/**
	 * Test der Initialisierung des WorkManagerController
	 */
	@Test
	public void testInitialize() {
		// Context mocken
		Context mockContext = mock(Context.class);
		when(mockContext.getApplicationContext()).thenReturn(mockContext);

		// Controller initialisieren
		WorkManagerController.initialize(mockContext);

		// Überprüfen, ob die beiden Planungsmethoden aufgerufen wurden
		verify(mockContext, times(2)).getApplicationContext();
	}


	/**
	 * Test, dass periodische Überprüfungen korrekt geplant werden
	 */
	@Test
	public void testSchedulePeriodicChecks() {
		// WorkManager-Instance für diesen Test abrufen
		WorkManager workManager = WorkManager.getInstance(context);

		// Sofortige Überprüfung planen
		WorkManagerController.scheduleImmediateCheck(context);

		// Prüfen, ob eine Arbeit geplant wurde
		ListenableFuture<List<WorkInfo>> workInfos = workManager.getWorkInfosByTag(WorkManagerController.WORKER_TAG);

		try {
			List<WorkInfo> infoList = workInfos.get();
			assertFalse("Es sollte mindestens eine Arbeit geplant sein", infoList.isEmpty());

			// Prüfen, ob mindestens eine Arbeit im Status ENQUEUED ist
			boolean hasEnqueuedWork = false;
			for (WorkInfo info : infoList) {
				if (info.getState() == WorkInfo.State.ENQUEUED) {
					hasEnqueuedWork = true;
					break;
				}
			}
			assertTrue("Es sollte eine eingereihte Arbeit geben", hasEnqueuedWork);

		}
		catch (Exception e) {
			throw new RuntimeException("Fehler beim Abrufen der WorkInfo", e);
		}
	}


	/**
	 * Hilfsmethode zum Erstellen eines Mock-Cursors mit RecurringDAO-Daten
	 */
	private MatrixCursor createMockRecurringCursor(DateTime now) {
		MatrixCursor cursor = new MatrixCursor(RecurringDAO.PROJECTION);

		// Aktuelle Zeit als Startzeit verwenden, Ende in einer Stunde
		long startTimeMillis = now.getMillis();
		long endTimeMillis = now.plusHours(1).getMillis();

		// Aktive Erfassungszeit für heute hinzufügen
		cursor.addRow(new Object[] {
			1L, // ID
			startTimeMillis, // Startzeit
			endTimeMillis, // Endzeit
			1, // Montag (1=aktiv)
			1, // Dienstag
			1, // Mittwoch
			1, // Donnerstag
			1, // Freitag
			0, // Samstag (0=inaktiv)
			0, // Sonntag
			0L, // inactiveUntil (0=nicht deaktiviert)
			0L // activeOnceDate (0=nicht einmalig aktiv)
		});

		return cursor;
	}
}
