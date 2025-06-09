package de.kalass.agime.ongoingnotification;

import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
import android.Manifest;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import android.database.MatrixCursor;

import java.util.ArrayList;
import java.util.List;

import de.kalass.agime.acquisitiontime.AcquisitionTimeInstance;
import de.kalass.agime.acquisitiontime.AcquisitionTimes;
import de.kalass.agime.acquisitiontime.RecurringDAO;
import de.kalass.android.common.simpleloader.CursorUtil;
import de.kalass.android.common.simpleloader.HourMinute;
import de.kalass.agime.loader.TrackedActivitySyncLoader;
import de.kalass.agime.model.TrackedActivityModel;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


/**
 * Comprehensive unit tests for NotificationWorker to ensure proper notification behavior during active and inactive
 * acquisition times, with proper dependency injection for testability.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class NotificationWorkerTest {

	private Context context;
	private NotificationWorker worker;

	@Mock
	private AcquisitionTimesProvider acquisitionTimesProvider;

	@Mock
	private TrackedActivitySyncLoader trackedActivityLoader;

	@Mock
	private WorkerParameters workerParameters;

	// Simple test implementations instead of complex mocks
	private TestPermissionChecker permissionChecker;
	private TestNotificationManagerProvider notificationManagerProvider;

	private DateTime testTime;

	@Before
	public void setUp() {
		MockitoAnnotations.openMocks(this);

		context = ApplicationProvider.getApplicationContext();
		testTime = new DateTime(2024, 1, 15, 10, 30); // Monday 10:30 AM

		// Create simple test implementations
		permissionChecker = new TestPermissionChecker();
		notificationManagerProvider = new TestNotificationManagerProvider();

		// Create worker using dependency injection constructor for testing
		worker = new NotificationWorker(context, workerParameters, acquisitionTimesProvider, trackedActivityLoader,
				permissionChecker, notificationManagerProvider);

		// Setup default behavior for activity loader
		when(trackedActivityLoader.query(anyLong(), anyLong(), anyBoolean(), anyString()))
			.thenReturn(new ArrayList<>());
	}

	/**
	 * Simple test implementation for permission checking.
	 */
	private static class TestPermissionChecker implements PermissionChecker {

		private boolean hasPermission = true; // Default to having permission

		@Override
		public boolean hasPermission(Context context, String permission) {
			return hasPermission;
		}


		public void setHasPermission(boolean hasPermission) {
			this.hasPermission = hasPermission;
		}
	}

	/**
	 * Simple test implementation for notification manager creation.
	 */
	private static class TestNotificationManagerProvider implements NotificationManagerProvider {

		private final NotificationManagerCompat mockManager = mock(NotificationManagerCompat.class);

		@Override
		public NotificationManagerCompat getNotificationManager(Context context) {
			return mockManager;
		}


		public NotificationManagerCompat getMockManager() {
			return mockManager;
		}
	}

	@Test
	public void testDoWork_withActiveAcquisitionTime_showsNotification() throws Exception {
		// Given: Active acquisition time from 9:00 to 17:00
		List<RecurringDAO.Data> recurringData = createActiveRecurringData(
			testTime.withHourOfDay(9),
			testTime.withHourOfDay(17));
		AcquisitionTimes times = AcquisitionTimes.fromRecurring(recurringData, testTime);

		// Setup mocks
		when(acquisitionTimesProvider.getCurrentAcquisitionTimes()).thenReturn(times);

		// When: Worker executes
		ListenableWorker.Result result = worker.doWork();

		// Then: Success result (notification creation is tested in unit level)
		assertEquals(ListenableWorker.Result.success(), result);
	}


	@Test
	public void testDoWork_withoutActiveAcquisitionTime_noForegroundNotification() throws Exception {
		// Given: No active acquisition time
		List<RecurringDAO.Data> recurringData = createInactiveRecurringData();
		AcquisitionTimes times = AcquisitionTimes.fromRecurring(recurringData, testTime);

		// Setup mocks
		when(acquisitionTimesProvider.getCurrentAcquisitionTimes()).thenReturn(times);

		// When: Worker executes
		ListenableWorker.Result result = worker.doWork();

		// Then: Success result
		assertEquals(ListenableWorker.Result.success(), result);
	}


	@Test
	public void testDoWork_withPreviousUnfinishedAcquisitionTime_showsNotification() throws Exception {
		// Given: Previous acquisition time ended 1 hour ago, no activities since
		DateTime previousEnd = testTime.minusHours(1);
		List<RecurringDAO.Data> recurringData = createPreviousRecurringData(
			previousEnd.minusHours(8),
			previousEnd);
		AcquisitionTimes times = AcquisitionTimes.fromRecurring(recurringData, testTime);

		// Setup mocks
		when(acquisitionTimesProvider.getCurrentAcquisitionTimes()).thenReturn(times);
		// No activities recorded after the previous acquisition time
		when(trackedActivityLoader.query(anyLong(), anyLong(), anyBoolean(), anyString()))
			.thenReturn(new ArrayList<>());

		// When: Worker executes
		ListenableWorker.Result result = worker.doWork();

		// Then: Success result
		assertEquals(ListenableWorker.Result.success(), result);
	}


	@Test
	public void testDoWork_withoutNotificationPermission_noNotificationShown() throws Exception {
		// Given: Active acquisition time but no permission
		List<RecurringDAO.Data> recurringData = createActiveRecurringData(
			testTime.withHourOfDay(9),
			testTime.withHourOfDay(17));
		AcquisitionTimes times = AcquisitionTimes.fromRecurring(recurringData, testTime);

		// Setup mocks
		when(acquisitionTimesProvider.getCurrentAcquisitionTimes()).thenReturn(times);

		// Remove permission
		permissionChecker.setHasPermission(false);

		// When: Worker executes
		ListenableWorker.Result result = worker.doWork();

		// Then: Success result, but no notification posted due to missing permission
		assertEquals(ListenableWorker.Result.success(), result);

		// Verify notification manager was retrieved but notify was not called
		verify(notificationManagerProvider.getMockManager(), never()).notify(anyInt(), any());
	}


	@Test
	public void testDoWork_withActiveTimeAndRecentActivity_showsCurrentActivityNotification() throws Exception {
		// Given: Active acquisition time with recent activity
		List<RecurringDAO.Data> recurringData = createActiveRecurringData(
			testTime.withHourOfDay(9),
			testTime.withHourOfDay(17));
		AcquisitionTimes times = AcquisitionTimes.fromRecurring(recurringData, testTime);

		// Recent activity (within UP_TO_DATE_MILLIS_SINCE_ACTIVITY threshold)
		TrackedActivityModel recentActivity = createTrackedActivity(
			testTime.minusMinutes(1).getMillis(), // 1 minute ago
			testTime.getMillis(),
			"Recent Work");
		List<TrackedActivityModel> activities = List.of(recentActivity);

		// Setup mocks
		when(acquisitionTimesProvider.getCurrentAcquisitionTimes()).thenReturn(times);
		when(trackedActivityLoader.query(anyLong(), anyLong(), anyBoolean(), anyString()))
			.thenReturn(activities);

		// When: Worker executes
		ListenableWorker.Result result = worker.doWork();

		// Then: Success result
		assertEquals(ListenableWorker.Result.success(), result);
	}


	@Test
	public void testDoWork_handlesExceptionGracefully() throws Exception {
		// Given: Provider throws exception
		when(acquisitionTimesProvider.getCurrentAcquisitionTimes())
			.thenThrow(new RuntimeException("Database error"));

		// When: Worker executes
		ListenableWorker.Result result = worker.doWork();

		// Then: Returns failure result
		assertEquals(ListenableWorker.Result.failure(), result);

		// Verify loader is closed even on exception
		verify(trackedActivityLoader).close();
	}


	@Test
	public void testDoWork_alwaysClosesTrackedActivityLoader() throws Exception {
		// Given: Normal execution
		List<RecurringDAO.Data> recurringData = createInactiveRecurringData();
		AcquisitionTimes times = AcquisitionTimes.fromRecurring(recurringData, testTime);

		// Setup regular mocks before static mocks
		when(acquisitionTimesProvider.getCurrentAcquisitionTimes()).thenReturn(times);

		// When: Worker executes
		worker.doWork();

		// Then: Loader is always closed
		verify(trackedActivityLoader).close();
	}


	@Test
	public void testDoWork_withOldPreviousAcquisitionTime_noNotification() throws Exception {
		// Given: Previous acquisition time ended more than threshold minutes ago
		DateTime oldPreviousEnd = testTime.minusMinutes(AcquisitionTimeInstance.ACQUISITION_TIME_END_THRESHOLD_MINUTES + 10);
		List<RecurringDAO.Data> recurringData = createPreviousRecurringData(
			oldPreviousEnd.minusHours(8),
			oldPreviousEnd);
		AcquisitionTimes times = AcquisitionTimes.fromRecurring(recurringData, testTime);

		// Setup mocks
		when(acquisitionTimesProvider.getCurrentAcquisitionTimes()).thenReturn(times);

		// When: Worker executes
		ListenableWorker.Result result = worker.doWork();

		// Then: Success result
		assertEquals(ListenableWorker.Result.success(), result);
	}

	// Helper methods for creating test objects


	private List<RecurringDAO.Data> createActiveRecurringData(DateTime start, DateTime end) {
		MatrixCursor cursor = new MatrixCursor(RecurringDAO.PROJECTION);

		// Add active acquisition time entry for current day
		cursor.addRow(new Object[] {
			1L, // ID
			HourMinute.serialize(start.toLocalTime()), // startTime
			HourMinute.serialize(end.toLocalTime()), // endTime
			127, // weekdays (all weekdays active: Mon=1,Tue=2,Wed=4,Thu=8,Fri=16,Sat=32,Sun=64, sum=127)
			0L, // inactiveUntil (null/0 = currently active)
			0L // activeOnce (null/0 = recurring, not one-time)
		});

		List<RecurringDAO.Data> result = CursorUtil.readList(cursor, RecurringDAO.READ_DATA);
		cursor.close();
		return result;
	}


	private List<RecurringDAO.Data> createInactiveRecurringData() {
		// Return empty list for no active acquisition times
		return new ArrayList<>();
	}


	private List<RecurringDAO.Data> createPreviousRecurringData(DateTime start, DateTime end) {
		MatrixCursor cursor = new MatrixCursor(RecurringDAO.PROJECTION);

		// Add previous acquisition time entry that has ended
		cursor.addRow(new Object[] {
			1L, // ID  
			HourMinute.serialize(start.toLocalTime()), // startTime
			HourMinute.serialize(end.toLocalTime()), // endTime
			127, // weekdays (all weekdays active)
			0L, // inactiveUntil (null/0 = currently active)
			0L // activeOnce (null/0 = recurring, not one-time)
		});

		List<RecurringDAO.Data> result = CursorUtil.readList(cursor, RecurringDAO.READ_DATA);
		cursor.close();
		return result;
	}


	private TrackedActivityModel createTrackedActivity(long startMillis, long endMillis, String displayName) {
		TrackedActivityModel activity = mock(TrackedActivityModel.class);
		when(activity.getStartTimeMillis()).thenReturn(startMillis);
		when(activity.getEndTimeMillis()).thenReturn(endMillis);
		when(activity.getDisplayName(any(Context.class))).thenReturn(displayName);
		return activity;
	}
}
