package de.kalass.agime.analytics;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import de.kalass.agime.ongoingnotification.WorkManagerController;

/**
 * Created by klas on 07.01.14.
 */
public class AnalyticsActionBarActivity extends AppCompatActivity {


    protected void doOnStart() {
    }

    protected void doOnStop() {
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check for pending permission dialogs
        WorkManagerController.checkPendingPermissionDialog(this);
    }

    @Override
    public final void onStart() {
        super.onStart();
        doOnStart();
    }

    @Override
    public final void onStop() {
        super.onStop();
        doOnStop();
    }

}
