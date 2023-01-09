package de.kalass.agime.customfield;

import de.kalass.agime.analytics.AnalyticsBaseCRUDActivity;
import de.kalass.android.common.activity.BaseCRUDFragment;
import de.kalass.android.common.activity.CRUDMode;

public class CustomFieldValueEditorActivity extends AnalyticsBaseCRUDActivity {

    @Override
    protected BaseCRUDFragment newCRUDFragment(CRUDMode mode) {
        return new CustomFieldValueEditorFragment();
    }

}
