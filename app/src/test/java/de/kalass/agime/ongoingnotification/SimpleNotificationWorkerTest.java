package de.kalass.agime.ongoingnotification;

import android.content.Context;
import android.content.pm.PackageManager;
import android.Manifest;
import androidx.core.app.NotificationManagerCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

import de.kalass.agime.loader.TrackedActivitySyncLoader;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


/**
 * Simple test to verify basic NotificationWorker functionality with dependency injection.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class SimpleNotificationWorkerTest {

	private Context context;

	@Mock
	private AcquisitionTimesProvider acquisitionTimesProvider;

	@Mock
	private TrackedActivitySyncLoader trackedActivityLoader;

	@Mock
	private WorkerParameters workerParameters;

	// Simple test implementations instead of complex mocks
	private TestPermissionChecker permissionChecker;
	private TestNotificationManagerProvider notificationManagerProvider;

	@Before
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		context = ApplicationProvider.getApplicationContext();

		// Create simple test implementations
		permissionChecker = new TestPermissionChecker();
		notificationManagerProvider = new TestNotificationManagerProvider();

		// Setup default mock behavior
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
	 * Simple test implementation for notification manager provider.
	 */
	private static class TestNotificationManagerProvider implements NotificationManagerProvider {

		@Override
		public NotificationManagerCompat getNotificationManager(Context context) {
			return mock(NotificationManagerCompat.class);
		}
	}

	@Test
	public void testWorkerCanBeCreated() {
		// When: Create worker with dependency injection
		NotificationWorker worker = new NotificationWorker(
				context,
				workerParameters,
				acquisitionTimesProvider,
				trackedActivityLoader,
				permissionChecker,
				notificationManagerProvider);

		// Then: Worker should not be null
		assertNotNull(worker);
	}


	@Test
	public void testWorkerExecutesWithoutException() throws Exception {
		// Given: Mock acquisition times provider returns null (no active times)
		when(acquisitionTimesProvider.getCurrentAcquisitionTimes()).thenReturn(null);

		// When: Create and execute worker
		NotificationWorker worker = new NotificationWorker(
				context,
				workerParameters,
				acquisitionTimesProvider,
				trackedActivityLoader,
				permissionChecker,
				notificationManagerProvider);

		ListenableWorker.Result result = worker.doWork();

		// Then: Should return success (even with no acquisition times)
		assertEquals(ListenableWorker.Result.success(), result);

		// Verify loader is closed
		verify(trackedActivityLoader).close();
	}
}
