package de.kalass.agime.category;

import de.kalass.agime.analytics.AnalyticsBaseCRUDManagementActivity;
import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.activity.BaseCRUDListFragment;


/**
 *
 */
public class CategoryManagementActivity extends AnalyticsBaseCRUDManagementActivity {

    public CategoryManagementActivity() {
        super(MCContract.Category.CONTENT_URI);
    }

    @Override
    protected BaseCRUDListFragment newCRUDFragment() {
        return new CategoryListFragment();
    }
}
