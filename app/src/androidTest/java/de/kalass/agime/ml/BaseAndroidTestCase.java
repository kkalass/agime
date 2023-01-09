package de.kalass.agime.ml;

import android.content.Context;
import android.test.AndroidTestCase;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * Created by klas on 07.04.14.
 */
public class BaseAndroidTestCase extends AndroidTestCase {
    public final static String LOG_TAG = "TestCase";
    private Context _testContext;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        try {
            Method m = AndroidTestCase.class.getMethod("getTestContext", new Class[] {});
            _testContext = (Context) m.invoke(this, (Object[]) null);
        } catch (Exception x) {
            Log.e(LOG_TAG, "Error getting test context: ", x);
            throw x;
        }
    }

    /**
     * The Context the test is running within - needed to access resources that are packaged for the test.
     */
    public Context getTestContextExposed() {
        return _testContext;
    }
}
