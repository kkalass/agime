package de.kalass.agime.project;

import de.kalass.agime.analytics.AnalyticsBaseCRUDActivity;
import de.kalass.android.common.activity.BaseCRUDFragment;
import de.kalass.android.common.activity.CRUDMode;


public class ProjectEditorActivity extends AnalyticsBaseCRUDActivity {

    @Override
    protected BaseCRUDFragment newCRUDFragment(CRUDMode mode) {
        return new ProjectEditorFragment();
    }

}
