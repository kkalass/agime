package de.kalass.agime.project;

import android.net.Uri;

import de.kalass.agime.analytics.AnalyticsBaseCRUDManagementActivity;
import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.activity.BaseCRUDListFragment;


/**
 *
 */
public class ProjectManagementActivity extends AnalyticsBaseCRUDManagementActivity {

    public ProjectManagementActivity() {
        super(MCContract.Project.CONTENT_URI);
    }

    @Override
    protected BaseCRUDListFragment newCRUDFragment() {
        return new ProjectListFragment();
    }
}
