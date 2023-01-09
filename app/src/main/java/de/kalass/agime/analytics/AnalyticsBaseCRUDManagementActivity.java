package de.kalass.agime.analytics;

import android.net.Uri;

import de.kalass.android.common.activity.BaseCRUDManagementActivity;

/**
 * Created by klas on 07.01.14.
 */
public abstract class AnalyticsBaseCRUDManagementActivity extends BaseCRUDManagementActivity {


    public AnalyticsBaseCRUDManagementActivity(Uri uri) {
        super(uri);
    }

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
