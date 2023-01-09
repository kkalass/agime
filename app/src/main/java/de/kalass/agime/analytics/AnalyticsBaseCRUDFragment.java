package de.kalass.agime.analytics;

import de.kalass.android.common.activity.BaseCRUDFragment;

/**
 * Created by klas on 22.01.14.
 */
public abstract class AnalyticsBaseCRUDFragment<C, D> extends BaseCRUDFragment<C, D> {

    private long _currentStartTime;

    public AnalyticsBaseCRUDFragment(int layout, String contentTypeDir, String contentTypeItem) {
        super(layout, contentTypeDir, contentTypeItem);
    }

    @Override
    public void onStart() {
        super.onStart();
        _currentStartTime = System.currentTimeMillis();
    }

    protected long getCurrentStartTime() {
        return _currentStartTime;
    }

    protected String getEntityTypeName() {
        return getContentTypeItem();
    }
}
