package de.kalass.android.common.insertorupdate;

import android.app.AlertDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import de.kalass.android.common.DialogUtils;
import de.kalass.agime.R;

/**
* Created by klas on 21.02.14.
*/
public abstract class InsertOrUpdateEntityTask<I, P, Result> extends AsyncTask<I, P, InsertOrUpdateResult<Result>> {
    private static final String LOG_TAG = "IOUEntityTask";

    private final Context _context;
    private final String _authority;

    public InsertOrUpdateEntityTask(Context context, String authority) {
        _context = context;
        _authority = authority;
    }

    public Context getContext() {
        return _context;
    }

    protected abstract Long getId(I input);
    protected abstract Operations<Result> createOperations(
            boolean isInsert, Long id, I input, long now
    );

    @Override
    protected final void onPostExecute(InsertOrUpdateResult<Result> result) {
        if (result.isError()) {
            onError(result);
            return;
        }
        onSuccess(result);
    }


    @Override
    protected InsertOrUpdateResult<Result> doInBackground(I... params) {
        I model = params[0];
        return new InsertOrUpdate<I, Result>(getContext(), _authority) {

            @Override
            protected Long getId(I input) {
                return InsertOrUpdateEntityTask.this.getId(input);
            }

            @Override
            protected Operations<Result> createOperations(boolean isInsert, Long id,
                                                        I input,
                                                        long now) {
                return InsertOrUpdateEntityTask.this.createOperations(isInsert, id, input, now);
            }
        }.execute(model);
    }

    /**
     * Subclasses often will implement this to give some feedback, e. g. close an activity
     * or show a toast or similar.
     * @param result
     */
    protected void onSuccess(InsertOrUpdateResult<Result> result) {

    }

    protected void onError(InsertOrUpdateResult<Result> result) {
        Log.e(LOG_TAG, "Task failed " + result);
        DialogUtils.showError(getContext(), R.string.error_dialog_save_failed_message);
    }
}
