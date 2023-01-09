package de.kalass.android.common.simpleloader;

import android.content.Context;
import android.util.Log;

/**
 * Asynchronous user that delegates all calls to a SyncLoader - this way, we can reuse code more easily
 * Created by klas on 19.01.14.
 */
public abstract class DelegatingAsyncLoader<L extends SyncLoader, T> extends AbstractLoader<T> {
    public static final String LOG_TAG = "DelegatingAsyncLoader";
    private final L _loader;
    private final boolean _observeDataSource;
    private ForceLoadContentObserver _observer;

    public DelegatingAsyncLoader(Context context, ObserveDataSourceMode observeDataSourceMode, L loader) {
        super(context);
        _observeDataSource = observeDataSourceMode == ObserveDataSourceMode.RELOAD_ON_CHANGES;
        _loader = loader;
    }

    public L getLoader() {
        return _loader;
    }

    @Override
    protected void beginMonitoringUnderlyingDataSource() {
        if (_observeDataSource) {
            Log.d(LOG_TAG, "beginMonitoringUnderlyingDataSource " + toString());
            if (_observer == null) {
                _observer = new ForceLoadContentObserver();
            }
            _loader.setContentObserver(_observer);
        }
    }

    @Override
    protected void endMonitoringUnderlyingDataSource() {
        if (_observeDataSource) {
            Log.d(LOG_TAG, "endMonitoringUnderlyingDataSource " + toString());
            _loader.setContentObserver(null);
            _observer = null;
        }
    }

    @Override
    protected void releaseResources(T data) {
        // currently, we do not support holding resources generally - all cursors must be
        // fully read and then closed when the sync loaders are used.
    }
}
