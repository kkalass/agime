package de.kalass.android.common;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import com.google.common.base.Preconditions;

import java.lang.reflect.Array;
import java.util.List;

import de.kalass.android.common.util.AProfiler;
import de.kalass.agime.R;

/**
* Created by klas on 14.01.14.
 * @param <INPUT> Input type
 * @param <SUCCESS_RESULT> Type of successfull results
*/
public abstract class AbstractAsyncTask<INPUT, SUCCESS_RESULT> extends AsyncTask<INPUT, Void, AbstractAsyncTask.Result<SUCCESS_RESULT>> {
    private static final String LOG_TAG = "ApplyBatchTask";


    private int _errorTitleResource;
    private int _errorMessageResource;

    private InProgressFragment _uiInteractionFragment;
    private boolean _useProgressDialog;

    private boolean isTaskRunning = false;

    private FragmentActivity _currentActivity;

    public static final class Result<S> {
        private final Exception _error;
        private final S _successData;

        private Result(Exception error, S successData) {
            _error = error;
            _successData = successData;
        }

        public static <T> Result<T> forError(Exception error) {
            return new Result<T>(error, null);
        }

        public static <T> Result<T> forSuccess(T successData) {
            return new Result<T>(null, successData);
        }
        public static <T> Result<T> forSuccess() {
            return new Result<T>(null, null);
        }

        public boolean isError() {
            return _error != null;
        }

        public Exception getException() {
            return _error;
        }

        public S getResults() {
            Preconditions.checkState(!isError(), "Cannot call getResults for error result");
            return _successData;
        }
    }

    public AbstractAsyncTask(FragmentActivity activity) {
        this(activity, R.string.error_dialog_title, R.string.error_dialog_save_failed_message);
    }

    public AbstractAsyncTask(FragmentActivity activity, int errorTitleResource, int errorMessageResource) {
        _currentActivity = Preconditions.checkNotNull(activity);

        _errorTitleResource = errorTitleResource;
        _errorMessageResource = errorMessageResource;
    }

    public AbstractAsyncTask<INPUT, SUCCESS_RESULT> setUseProgressDialog(boolean b) {
        _useProgressDialog = b;
        return this;
    }



    public AbstractAsyncTask<INPUT, SUCCESS_RESULT> setErrorMessageResource(final int errorMessageResource) {
        _errorMessageResource = errorMessageResource;
        return this;
    }

    public AbstractAsyncTask<INPUT, SUCCESS_RESULT> setErrorTitleResource(final int errorTitleResource) {
        _errorTitleResource = errorTitleResource;
        return this;
    }

    protected Context getContext() {
        return getCurrentActivity();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        isTaskRunning = true;
        FragmentActivity currentActivity = getCurrentActivity();
        if (currentActivity != null) {
            FragmentManager fm = currentActivity.getSupportFragmentManager();
            _uiInteractionFragment = new InProgressFragment();
            _uiInteractionFragment.setArguments(newProgressDialogArguments());
            fm.beginTransaction().add(_uiInteractionFragment, InProgressFragment.PROGRESS_DLG_TAG).commitAllowingStateLoss();
        }
    }

    protected Bundle newProgressDialogArguments() {
        return InProgressFragment.createArguments(_useProgressDialog);
    }

    public void execute(Class<? super INPUT> itemCls, List<? extends INPUT> list) {
        //noinspection unchecked
        INPUT[] itemsHolder = (INPUT[])Array.newInstance(itemCls, list.size());
        execute(list.toArray(itemsHolder));
    }

    @Override
    protected final void onPostExecute(AbstractAsyncTask.Result<SUCCESS_RESULT> result) {
        isTaskRunning = false;
        cleanupProgressDialog();
        if (result != null &&result.isError()) {
            onError(result);
            return;
        }
        onSuccess(result);
    }

    protected void onSuccess(AbstractAsyncTask.Result<SUCCESS_RESULT> result) {
    }

    protected void onError(AbstractAsyncTask.Result<SUCCESS_RESULT> result) {
        Log.e(LOG_TAG, "ApplyBatchTask failed " , result._error);
        DialogUtils.showError(getContext(), _errorTitleResource, _errorMessageResource);
    }

    protected void cleanupProgressDialog() {
        Log.i(LOG_TAG, "cleanupProgressDialog " + InProgressFragment.PROGRESS_DLG_TAG + " " + _uiInteractionFragment);
        if (_uiInteractionFragment !=null && !_uiInteractionFragment.isDetached()) {
            _currentActivity = _uiInteractionFragment.getActivity();
            FragmentManager fragmentManager = _currentActivity.getSupportFragmentManager();
            if (fragmentManager != null) {
                fragmentManager.beginTransaction().remove(_uiInteractionFragment).commitAllowingStateLoss();
            }
            _uiInteractionFragment = null;
        }
    }

    public FragmentActivity getCurrentActivity() {
        FragmentActivity current = _uiInteractionFragment != null && !_uiInteractionFragment.isDetached() ? _uiInteractionFragment.getActivity() : null;
        if (current != null) {
            return current;
        }
        return _currentActivity;
    }

    protected abstract AbstractAsyncTask.Result<SUCCESS_RESULT> performInBackground(INPUT... params) throws Exception;

    @Override
    protected final AbstractAsyncTask.Result<SUCCESS_RESULT> doInBackground(INPUT... params) {
        String simpleName = getClass().getSimpleName();
        Object profilerToken = AProfiler.start(simpleName);
        try {
            return performInBackground(params);
        } catch (Exception e) {
            return AbstractAsyncTask.Result.forError(e);
        } finally {
            AProfiler.end(profilerToken);
        }
    }
}
