package de.kalass.agime.analytics;

import de.kalass.android.common.activity.BaseCRUDActivity;

/**
 * Created by klas on 07.01.14.
 */
public abstract class AnalyticsBaseCRUDActivity extends BaseCRUDActivity {


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
