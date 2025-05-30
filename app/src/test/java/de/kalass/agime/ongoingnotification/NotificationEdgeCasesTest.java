package de.kalass.agime.ongoingnotification;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Build;
import android.preference.PreferenceManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.work.Configuration;
import androidx.work.ListenableWorker;
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
// Dieser Import wurde entfernt, da ShadowPreferenceManager in neueren Robolectric-Versionen nicht verfügbar ist

import java.util.concurrent.Executor;

import de.kalass.agime.acquisitiontime.RecurringDAO;
import de.kalass.agime.provider.MCContract;
import de.kalass.agime.settings.Preferences;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;


/**
 * Tests für spezielle Edge Cases und Fehlerbehandlung
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class NotificationEdgeCasesTest {

	private Context context;

	@Mock
	private ContentResolver mockContentResolver;

	@Before
	public void setUp() {
		MockitoAnnotations.openMocks(this);

		context = spy(ApplicationProvider.getApplicationContext());
		when(context.getContentResolver()).thenReturn(mockContentResolver);

		// SharedPreferences für Tests vorbereiten
		SharedPreferences mockPreferences = mock(SharedPreferences.class);
		SharedPreferences.Editor mockEditor = mock(SharedPreferences.Editor.class);
		when(mockPreferences.edit()).thenReturn(mockEditor);
		when(mockEditor.putLong(anyString(), anyLong())).thenReturn(mockEditor);
		when(mockEditor.putBoolean(anyString(), any(Boolean.class))).thenReturn(mockEditor);
		when(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor);
		when(mockEditor.apply()).thenReturn(null);

		// Mock für statische PreferenceManager-Methode ersetzen
		try {
			doReturn(mockPreferences).when(context).getSharedPreferences(anyString(), anyInt());
		}
		catch (Exception e) {
			// Fallback, falls der direkte Mock nicht funktioniert
		}

		// Synchroner Executor für Tests
		Executor executor = command -> command.run();

		// WorkManager für Tests initialisieren
		Configuration config = new Configuration.Builder()
			.setMinimumLoggingLevel(android.util.Log.DEBUG)
			.setExecutor(executor)
			.build();
		WorkManagerTestInitHelper.initializeTestWorkManager(context, config);
	}


	/**
	 * Test, dass der Worker mit einer SQLException umgehen kann
	 */
	@Test
	public void testWorkerWithSQLException() {
		// SQLException beim Abfragen des ContentResolvers simulieren
		doThrow(new RuntimeException("Simulierter SQL-Fehler"))
			.when(mockContentResolver).query(
				eq(RecurringDAO.CONTENT_URI),
				any(), any(), any(), any());

		// Worker ausführen
		NotificationWorker worker = TestListenableWorkerBuilder.from(context, NotificationWorker.class)
			.build();

		// Der Worker sollte mit Failure enden
		ListenableWorker.Result result = worker.doWork();
		assertEquals(ListenableWorker.Result.failure(), result);
	}


	/**
	 * Test, dass der Worker mit deaktivierten Benachrichtigungen umgehen kann
	 */
	@Test
	public void testWorkerWithDisabledNotifications() {
		// Context mit deaktivierten Benachrichtigungen vorbereiten
		Context spyContext = spy(context);

		// Leeren Cursor für RecurringDAO vorbereiten
		MatrixCursor emptyCursor = new MatrixCursor(RecurringDAO.PROJECTION);
		when(mockContentResolver.query(
			eq(RecurringDAO.CONTENT_URI),
			any(), any(), any(), any())).thenReturn(emptyCursor);

		// Worker mit deaktivierten Benachrichtigungen erstellen
		NotificationWorker worker = spy(TestListenableWorkerBuilder.from(spyContext, NotificationWorker.class)
			.build());

		// isPermanentlyHidden() überschreiben, um true zurückzugeben
		doReturn(true).when(worker).isPermanentlyHidden();

		// Worker ausführen - sollte erfolgreich sein, aber keine Benachrichtigung erstellen
		ListenableWorker.Result result = worker.doWork();
		assertEquals(ListenableWorker.Result.success(), result);
	}


	/**
	 * Test, dass der Worker mit NULL-Cursor umgehen kann
	 */
	@Test
	public void testWorkerWithNullCursor() {
		// NULL-Cursor vom ContentResolver zurückgeben
		when(mockContentResolver.query(
			eq(RecurringDAO.CONTENT_URI),
			any(), any(), any(), any())).thenReturn(null);

		// Worker ausführen
		NotificationWorker worker = TestListenableWorkerBuilder.from(context, NotificationWorker.class)
			.build();

		// Der Worker sollte trotzdem erfolgreich sein
		ListenableWorker.Result result = worker.doWork();
		assertEquals(ListenableWorker.Result.success(), result);
	}


	/**
	 * Test, dass der Worker mit leeren Aktivitätsdaten umgehen kann
	 */
	@Test
	public void testWorkerWithEmptyActivities() {
		// Cursor für RecurringDAO mit aktiver Erfassungszeit erstellen
		DateTime now = new DateTime();
		MatrixCursor recurringCursor = createMockRecurringCursor(now);

		when(mockContentResolver.query(
			eq(RecurringDAO.CONTENT_URI),
			any(), any(), any(), any())).thenReturn(recurringCursor);

		// Cursor ohne Aktivitäten für TrackedActivitySyncLoader vorbereiten
		MatrixCursor emptyCursor = new MatrixCursor(new String[] {
			MCContract.Activity._ID,
			MCContract.Activity.COLUMN_NAME_START_TIME,
			MCContract.Activity.COLUMN_NAME_END_TIME,
			MCContract.Activity.COLUMN_NAME_NAME
		});
		when(mockContentResolver.query(
			eq(MCContract.Activity.CONTENT_URI),
			any(), any(), any(), any())).thenReturn(emptyCursor);

		// Worker ausführen
		NotificationWorker worker = TestListenableWorkerBuilder.from(context, NotificationWorker.class)
			.build();

		// Der Worker sollte erfolgreich sein
		ListenableWorker.Result result = worker.doWork();
		assertEquals(ListenableWorker.Result.success(), result);
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
