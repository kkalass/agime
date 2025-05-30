package de.kalass.agime.ml;

import android.content.Context;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;


/**
 * Base test case providing common functionality for instrumented tests. Migrated from deprecated AndroidTestCase to
 * AndroidX testing framework.
 */
public class BaseAndroidTestCase {

	public final static String LOG_TAG = "TestCase";
	private Context _testContext;

	/**
	 * Sets up the test context using AndroidX InstrumentationRegistry. Call this method in your test setup (@Before
	 * annotated method).
	 */
	protected void setUp() throws Exception {
		try {
			// Use AndroidX InstrumentationRegistry to get the test context
			_testContext = InstrumentationRegistry.getInstrumentation().getContext();
		}
		catch (Exception x) {
			Log.e(LOG_TAG, "Error getting test context: ", x);
			throw x;
		}
	}


	/**
	 * Returns the target application context under test.
	 */
	protected Context getTargetContext() {
		return InstrumentationRegistry.getInstrumentation().getTargetContext();
	}


	/**
	 * The Context the test is running within - needed to access resources that are packaged for the test.
	 */
	public Context getTestContextExposed() {
		return _testContext;
	}
}
