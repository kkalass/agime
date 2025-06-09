package de.kalass.agime.ongoingnotification;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;


/**
 * Minimal test to verify Robolectric setup is working.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class MinimalRobolectricTest {

	@Test
	public void testRobolectricBasics() {
		// When: Get application context
		Context context = ApplicationProvider.getApplicationContext();

		// Then: Should not be null
		assertNotNull(context);

		// Should be able to get package name
		String packageName = context.getPackageName();
		assertNotNull(packageName);
	}
}
