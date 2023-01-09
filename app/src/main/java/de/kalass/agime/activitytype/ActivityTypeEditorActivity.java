package de.kalass.agime.activitytype;

import de.kalass.agime.analytics.AnalyticsBaseCRUDActivity;
import de.kalass.android.common.activity.BaseCRUDFragment;
import de.kalass.android.common.activity.CRUDMode;


public class ActivityTypeEditorActivity extends AnalyticsBaseCRUDActivity {

    @Override
    protected BaseCRUDFragment newCRUDFragment(CRUDMode mode) {
        return new ActivityTypeEditorFragment();
    }

}
