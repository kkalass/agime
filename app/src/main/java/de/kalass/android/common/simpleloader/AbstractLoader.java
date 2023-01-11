package de.kalass.android.common.simpleloader;

import android.content.Context;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;

import com.freiheit.fuava.ctprofiler.core.CallTreeProfiler;
import com.freiheit.fuava.ctprofiler.core.TimeKeeper;

import de.kalass.android.common.util.AProfiler;

/**
 * An abstract baseclass for Loaders that do all the work in loadInBackground, reading the
 * entire requested Data.
 *
 * The main usecase is lists that combine data from multiple data sources.
 *
 * It adheres to http://www.androiddesignpatterns.com/2012/08/implementing-loaders.html
 * and tries to make implementing "correct" loaders a snap.
 *
 * Created by klas on 01.11.13.
 */
public abstract class AbstractLoader<T>  extends AsyncTaskLoader<T> {
    private static final CallTreeProfiler PROFILER = AProfiler.getProfiler();
    private static final TimeKeeper KEEPER = AProfiler.getTimeKeeper();
    private AProfiler.BackgroundTask task;


    public static final String LOG_TAG = "AbstractLoader";
    private T _data;

    private Throwable _throwable;
    private String _failMessage;

    public AbstractLoader(Context context) {
        super(context);
        task = AProfiler.beforeBackgroundTask(getClass().getSimpleName());
    }

    protected void setFailed(String message, Throwable e) {
        _throwable = e;
        _failMessage = message;
    }

    public boolean isFailed() {
        return _throwable != null;
    }

    public String getFailureMessage() {
        return _failMessage;
    }

    public Throwable getFailureThrowable() {
        return _throwable;
    }

    public static void showFailure(Context context, Loader<?> loader) {
        if (loader instanceof  AbstractLoader) {
            AbstractLoader al = (AbstractLoader) loader;
            if (al.isFailed()) {
                Log.e(LOG_TAG, al.getFailureMessage(), al.getFailureThrowable());
                new AlertDialog.Builder(context)
                        .setTitle("Failure")
                        .setMessage(al._failMessage)
                        .show();
            }
        }
    }
    private String tkName(String fkt) {
        if (KEEPER.isEnabled()) {
            return getClass().getSimpleName() + "." + fkt;
        }
        return fkt;
    }

    public T  preload() {
        final String name = tkName("preload");
        KEEPER.begin(name);
        try {
            _data = doLoadInBackground();
            return _data;
        } finally {
            KEEPER.end(name);
        }
    }


    public abstract T doLoadInBackground();

    @Override
    public final T loadInBackground() {
        final String name = tkName("loadInBackground");
        KEEPER.begin(name);
        try {
            return doLoadInBackground();
        } finally {
            KEEPER.end(name);
            if (task != null) {
                task.onFinish();
            }

        }
    }

    @Override
    public final void deliverResult(T data) {
        if (task != null) {
            task.afterBackgroundTask();
            task = null;
        }
        if (isReset()) {
            Log.v(LOG_TAG, "deliverResult: isReset => releaseResources");
            // The Loader has been reset; ignore the result and invalidate the data.
            releaseResources(data);
            return;
        }

        // Hold a reference to the old data so it doesn't get garbage collected.
        // We must protect it until the new data has been delivered.
        T oldData = _data;
        _data = data;

        if (isStarted()) {
            Log.v(LOG_TAG, "deliverResult: isStarted => super.deliverResult");
            // If the Loader is in a started state, deliver the results to the
            // client. The superclass method does this for us.
            super.deliverResult(data);
        }

        // Invalidate the old data as we don't need it any more.
        if (oldData != null && oldData != data) {
            Log.v(LOG_TAG, "deliverResult: Invalidate the old data as we don't need it any more");
            releaseResources(oldData);
        }

    }

    /**
     * For a simple List, there is nothing to do. For something like a Cursor, we
     * would close it in this method. All resources associated with the Loader
     * should be released here.
     * @param data
     */
    protected void releaseResources(T data) {}

    /**
     * Starts an asynchronous load of the  data. When the result is ready the callbacks
     * will be called on the UI thread. If a previous load has been completed and is still valid
     * the result may be passed to the callbacks immediately.
     *
     * Must be called from the UI thread
     */
    @Override
    protected final void onStartLoading() {
        Log.v(LOG_TAG, "onStartLoading");
        if (_data != null) {
            Log.v(LOG_TAG, "onStartLoading: Deliver previously loaded data immediately");
            deliverResult(_data);
        }
        beginMonitoringUnderlyingDataSource();


        if (takeContentChanged() || _data == null) {
            // When the observer detects a change, it should call onContentChanged()
            // on the Loader, which will cause the next call to takeContentChanged()
            // to return true. If this is ever the case (or if the current data is
            // null), we force a new load.
            Log.v(LOG_TAG, "onStartLoading: contentChanged, or data is null => force load");
            forceLoad();
        }
    }

    /**
     * If you plan to make use of automatic data change notifications, register your observer here
     */
    protected void beginMonitoringUnderlyingDataSource() { }

    /**
     * The Loader is being reset, so we should stop monitoring for changes.
     */
    protected void endMonitoringUnderlyingDataSource() {}

    /**
     * Must be called from the UI thread
     */
    @Override
    protected final void onStopLoading() {
        // The Loader is in a stopped state, so we should attempt to cancel the
        // current load (if there is one).
        cancelLoad();

        // Note that we leave the observer as is. Loaders in a stopped state
        // should still monitor the data source for changes so that the Loader
        // will know to force a new load if it is ever started again.
    }


    @Override
    protected final void onReset() {
        super.onReset();

        // Ensure the loader has been stopped.
        onStopLoading();

        // At this point we can release the resources associated with 'mData'.
        if (_data != null) {
            Log.v(LOG_TAG, "onReset: releasing resources");
            releaseResources(_data);
            _data = null;
        }

        endMonitoringUnderlyingDataSource();
    }

    @Override
    public final void onCanceled(T data) {
        // Attempt to cancel the current asynchronous load.
        super.onCanceled(data);

        // The load has been canceled, so we should release the resources
        // associated with 'data'.
        Log.v(LOG_TAG, "onCanceled: releasing resources");
        releaseResources(data);
    }
}
