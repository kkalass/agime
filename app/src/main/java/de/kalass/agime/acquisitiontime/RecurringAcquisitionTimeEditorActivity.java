package de.kalass.agime.acquisitiontime;

import de.kalass.agime.analytics.AnalyticsBaseCRUDActivity;
import de.kalass.android.common.activity.BaseCRUDFragment;
import de.kalass.android.common.activity.CRUDMode;

public class RecurringAcquisitionTimeEditorActivity extends AnalyticsBaseCRUDActivity {

    @Override
    protected BaseCRUDFragment newCRUDFragment(CRUDMode mode) {

        return new RecurringAcquisitionTimeEditorFragment();
    }

}
