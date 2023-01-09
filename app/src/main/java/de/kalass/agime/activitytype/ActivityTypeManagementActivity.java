package de.kalass.agime.activitytype;

import android.net.Uri;

import de.kalass.agime.analytics.AnalyticsBaseCRUDManagementActivity;
import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.activity.BaseCRUDListFragment;


/**
 *
 */
public class ActivityTypeManagementActivity extends AnalyticsBaseCRUDManagementActivity {

    public ActivityTypeManagementActivity() {
        super(MCContract.ActivityType.CONTENT_URI);
    }

    @Override
    protected BaseCRUDListFragment newCRUDFragment() {
        return new ActivityTypeListFragment();
    }
}
