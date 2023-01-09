package de.kalass.agime.customfield;

import android.net.Uri;

import de.kalass.agime.R;
import de.kalass.agime.analytics.AnalyticsBaseCRUDManagementActivity;
import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.activity.BaseCRUDListFragment;


public class CustomFieldValueManagementActivity
        extends AnalyticsBaseCRUDManagementActivity
        implements CustomFieldValueListFragment.CustomFieldValueListActivity {

    public CustomFieldValueManagementActivity() {
        super(MCContract.CustomFieldValue.CONTENT_URI);
    }

    @Override
    protected BaseCRUDListFragment newCRUDFragment() {
        return new CustomFieldValueListFragment();
    }

    @Override
    public void setTypeName(String name) {
        setTitle(getString(R.string.custom_field_value_management_activity_title, name));
    }
}
