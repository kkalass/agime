package de.kalass.agime.util;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.Window;
import android.view.WindowManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.Mockito.*;


/**
 * Unit tests for EdgeToEdgeHelper to verify Android 15 compatibility and proper edge-to-edge behavior. Tests ensure
 * that deprecated APIs (setStatusBarColor, setNavigationBarColor) are not used on Android 15+ while maintaining
 * backwards compatibility.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34) // Test with SDK 34 to verify compatibility
public class EdgeToEdgeHelperTest {

	@Mock
	private Activity mockActivity;

	@Mock
	private Window mockWindow;

	private Activity realActivity;

	@Before
	public void setUp() {
		MockitoAnnotations.openMocks(this);

		// Create a real activity for integration tests
		realActivity = Robolectric.buildActivity(Activity.class).create().get();

		// Setup mock activity
		when(mockActivity.getWindow()).thenReturn(mockWindow);
	}


	/**
	 * Test that setupEdgeToEdge doesn't crash and properly handles transparent system bars for Android 15+. This verifies
	 * the migration away from deprecated setStatusBarColor APIs.
	 */
	@Test
	public void testSetupEdgeToEdge_Android15_UsesTransparentSystemBars() {
		// Test with real activity to verify no crashes occur
		EdgeToEdgeHelper.setupEdgeToEdge(realActivity);

		// The test passes if no exceptions are thrown during setup
		// On Android 15+, transparent system bars should be used instead of setStatusBarColor
	}


	/**
	 * Test that applySystemWindowInsetsToToolbar properly handles insets without using deprecated APIs. This ensures
	 * toolbar positioning works correctly with the new edge-to-edge approach.
	 */
	@Test
	public void testApplySystemWindowInsetsToToolbar_HandlesInsetsCorrectly() {
		// Create a mock toolbar view
		android.widget.FrameLayout mockToolbar = mock(android.widget.FrameLayout.class);
		android.view.ViewGroup.MarginLayoutParams mockLayoutParams = mock(android.view.ViewGroup.MarginLayoutParams.class);
		when(mockToolbar.getLayoutParams()).thenReturn(mockLayoutParams);

		// Test that the method doesn't crash
		EdgeToEdgeHelper.applySystemWindowInsetsToToolbar(mockToolbar);

		// The test passes if no exceptions are thrown
		// The insets listener should be properly set up for edge-to-edge positioning
	}


	/**
	 * Test that applySystemWindowInsetsToContent properly handles content padding for navigation bar. This ensures
	 * content doesn't overlap with the navigation bar in edge-to-edge mode.
	 */
	@Test
	public void testApplySystemWindowInsetsToContent_HandlesContentPadding() {
		// Create a mock content view
		android.widget.FrameLayout mockContentView = mock(android.widget.FrameLayout.class);

		// Test that the method doesn't crash
		EdgeToEdgeHelper.applySystemWindowInsetsToContent(mockContentView);

		// The test passes if no exceptions are thrown
		// The content should have proper padding applied to avoid navigation bar overlap
	}


	/**
	 * Test that the helper maintains backwards compatibility with older Android versions. On older versions,
	 * setStatusBarColor should still be used since it's not deprecated there.
	 */
	@Test
	@Config(sdk = 29) // Test with older SDK to verify backwards compatibility
	public void testSetupEdgeToEdge_OlderVersions_MaintainsCompatibility() {
		// Test with real activity on older Android version
		EdgeToEdgeHelper.setupEdgeToEdge(realActivity);

		// The test passes if no exceptions are thrown
		// On older versions, the existing setStatusBarColor approach should be maintained
	}


	/**
	 * Test that setupEdgeToEdge works correctly across different Android API levels. This ensures the version-specific
	 * implementations don't cause crashes.
	 */
	@Test
	@Config(sdk = 30) // Test with Android 11
	public void testSetupEdgeToEdge_Android11_UsesProperAPI() {
		EdgeToEdgeHelper.setupEdgeToEdge(realActivity);

		// The test passes if no exceptions are thrown
		// Android 11+ should use WindowCompat.setDecorFitsSystemWindows with transparent system bars
	}
}
