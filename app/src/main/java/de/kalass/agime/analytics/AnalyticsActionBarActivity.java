package de.kalass.agime.analytics;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by klas on 07.01.14.
 */
public class AnalyticsActionBarActivity extends AppCompatActivity {


    protected void doOnStart() {
    }

    protected void doOnStop() {
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
