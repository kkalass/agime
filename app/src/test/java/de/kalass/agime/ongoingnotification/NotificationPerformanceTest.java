package de.kalass.agime.ongoingnotification;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;
import androidx.work.Configuration;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.WorkManager;
import androidx.work.testing.TestListenableWorkerBuilder;
import androidx.work.testing.WorkManagerTestInitHelper;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import de.kalass.agime.acquisitiontime.RecurringDAO;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;


/**
 * Tests für die Performance und Ressourcennutzung der WorkManager-Implementierung
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class NotificationPerformanceTest {

	private Context context;

	@Mock
	private ContentResolver mockContentResolver;

	@Before
	public void setUp() {
		MockitoAnnotations.openMocks(this);

		context = spy(ApplicationProvider.getApplicationContext());
		when(context.getContentResolver()).thenReturn(mockContentResolver);

		// Synchroner Executor für Tests
		Executor executor = new androidx.work.testing.SynchronousExecutor();

		Configuration config = new Configuration.Builder()
			.setMinimumLoggingLevel(android.util.Log.DEBUG)
			.setExecutor(executor)
			.build();
		WorkManagerTestInitHelper.initializeTestWorkManager(context, config);
	}


	/**
	 * Test zur Messung der Ausführungszeit des Workers
	 */
	@Test
	public void testWorkerExecutionTime() {
		// Cursor für RecurringDAO vorbereiten (mit vielen Einträgen für Performance-Test)
		MatrixCursor largeCursor = createLargeRecurringCursor(100);
		when(mockContentResolver.query(
			eq(RecurringDAO.CONTENT_URI),
			any(), any(), any(), any())).thenReturn(largeCursor);

		// Worker mit Input-Daten erstellen
		Data inputData = new Data.Builder().build();
		NotificationWorker worker = TestListenableWorkerBuilder.from(context, NotificationWorker.class)
			.setInputData(inputData)
			.build();

		// Ausführungszeit messen
		long startTime = System.currentTimeMillis();
		ListenableWorker.Result result = worker.doWork();
		long endTime = System.currentTimeMillis();
		long executionTime = endTime - startTime;

		// Die Ausführung sollte erfolgreich sein
		assertEquals(ListenableWorker.Result.success(), result);

		// Die Ausführung sollte in einer angemessenen Zeit erfolgen (auf einem modernen Gerät)
		// Dieser Wert kann je nach Testumgebung angepasst werden
		assertTrue("Worker-Ausführung dauerte zu lange: " + executionTime + " ms", executionTime < 5000);
	}


	/**
	 * Test der WorkManagerController-Initialisierung mit vielen Anfragen
	 */
	@Test
	public void testControllerWithMultipleRequests() {
		// Mehrere Anfragen nacheinander senden
		long startTime = System.currentTimeMillis();

		for (int i = 0; i < 50; i++) {
			WorkManagerController.scheduleImmediateCheck(context);
		}

		long endTime = System.currentTimeMillis();
		long executionTime = endTime - startTime;

		// Die Ausführungszeit sollte in einer angemessenen Zeit erfolgen
		assertTrue("Controller-Ausführung dauerte zu lange: " + executionTime + " ms", executionTime < 3000);

		// WorkManager sollte korrekt initialisiert sein
		WorkManager workManager = WorkManager.getInstance(context);
		assertNotNull("WorkManager sollte initialisiert sein", workManager);
	}


	/**
	 * Hilfsmethode zum Erstellen eines großen Mock-Cursors mit RecurringDAO-Daten
	 */
	private MatrixCursor createLargeRecurringCursor(int count) {
		MatrixCursor cursor = new MatrixCursor(RecurringDAO.PROJECTION);

		DateTime now = new DateTime();

		// Viele Einträge für den Performance-Test hinzufügen
		for (int i = 0; i < count; i++) {
			// Verschiedene Start- und Endzeiten verwenden
			long startTimeMillis = now.minusHours(i % 12).getMillis();
			long endTimeMillis = now.plusHours((i % 8) + 1).getMillis();

			cursor.addRow(new Object[] {
				(long)i, // ID
				startTimeMillis, // Startzeit
				endTimeMillis, // Endzeit
				i % 2, // Montag (abwechselnd aktiv/inaktiv)
				(i + 1) % 2, // Dienstag
				i % 2, // Mittwoch
				(i + 1) % 2, // Donnerstag
				i % 2, // Freitag
				0, // Samstag (immer inaktiv)
				0, // Sonntag (immer inaktiv)
				i % 5 == 0 ? now.plusDays(i % 10).getMillis() : 0L, // inactiveUntil
				i % 7 == 0 ? now.plusDays(i % 14).getMillis() : 0L // activeOnceDate
			});
		}

		return cursor;
	}
}
