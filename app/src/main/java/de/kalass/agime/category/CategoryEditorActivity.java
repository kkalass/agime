package de.kalass.agime.category;

import de.kalass.agime.analytics.AnalyticsBaseCRUDActivity;
import de.kalass.android.common.activity.BaseCRUDFragment;
import de.kalass.android.common.activity.CRUDMode;


public class CategoryEditorActivity extends AnalyticsBaseCRUDActivity {
    @Override
    protected BaseCRUDFragment newCRUDFragment(CRUDMode mode) {
        return new CategoryEditorFragment();
    }
}
