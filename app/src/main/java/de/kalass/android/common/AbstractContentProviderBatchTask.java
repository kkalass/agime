package de.kalass.android.common;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.Context;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.util.Log;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.kalass.android.common.util.Arrays2;
import de.kalass.agime.R;

import static com.google.common.base.Preconditions.checkNotNull;

/**
* Created by klas on 14.01.14.
*/
public abstract class AbstractContentProviderBatchTask<I> extends AsyncTask<I, Void, AbstractContentProviderBatchTask.Result> {
    private static final String LOG_TAG = "ApplyBatchTask";

    private final int _errorTitleResource;
    private final int _errorMessageResource;
    private final Context _context;

    public static final class Result {
        private final Exception _error;
        private final ContentProviderResult[] _results;
        private final ArrayList<ContentProviderOperation> _ops;

        private Result(Exception error, ArrayList<ContentProviderOperation> ops, ContentProviderResult[] results) {
            _error = error;
            _ops = ops;
            _results = results;
        }

        public static Result forError(Exception error, ArrayList<ContentProviderOperation> ops) {
            return new Result(error, ops, null);
        }

        public static Result forOperations(ContentProviderResult[] results) {
            return new Result(null, null, results);
        }

        public boolean isError() {
            return _error != null;
        }

        public Exception getException() {
            return _error;
        }

        public ArrayList<ContentProviderOperation> getOperations() {
            return _ops;
        }

        public ContentProviderResult[] getResults() {
            Preconditions.checkState(!isError(), "Cannot call getResults for error result");
            return _results;
        }
    }

    public AbstractContentProviderBatchTask(Context context) {
        this(context, R.string.error_dialog_title, R.string.error_dialog_save_failed_message);
    }

    public AbstractContentProviderBatchTask(Context context, int errorTitleResource, int errorMessageResource) {
        _context = context;

        _errorTitleResource = errorTitleResource;
        _errorMessageResource = errorMessageResource;
    }

    protected Context getContext() {
        return _context;
    }

    public void execute(Class<? super I> itemCls, List<? extends I> list) {
        //noinspection unchecked
        I[] itemsHolder = (I[])Array.newInstance(itemCls, list.size());
        execute(list.toArray(itemsHolder));
    }

    @Override
    protected final void onPostExecute(AbstractContentProviderBatchTask.Result result) {
        if (result.isError()) {
            onError(result);
            return;
        }
        onSuccess(result);
    }

    protected void onSuccess(AbstractContentProviderBatchTask.Result result) {

    }

    protected void onError(AbstractContentProviderBatchTask.Result result) {
        final ArrayList<ContentProviderOperation> ops = result.getOperations();
        Log.e(LOG_TAG, "ApplyBatchTask failed " + (ops == null ? "" : ops), result._error);
        DialogUtils.showError(_context, _errorTitleResource, _errorMessageResource);
    }

    protected abstract ArrayList<ContentProviderOperation> createOperationsInBackground(I input) throws Exception;

    protected ArrayList<ContentProviderOperation> createOperationsInBackground(I... params) throws Exception {
        Preconditions.checkArgument(params.length == 1);
        I input = params[0];
        return createOperationsInBackground(input);
    }

    @Override
    protected AbstractContentProviderBatchTask.Result doInBackground(I... params) {

        ArrayList<ContentProviderOperation> ops = null;
        try {
            ops = createOperationsInBackground(params);
            if (ops.isEmpty()) {
                // nothing to do
                return AbstractContentProviderBatchTask.Result.forOperations(new ContentProviderResult[0]);
            }
            ContentProviderOperation first = checkNotNull(ops.get(0), "Null values not allowed for items in the operations list");
            Uri uri = checkNotNull(first.getUri(), "Operation has no URI to work on");
            final ContentProviderResult[] results = _context.getContentResolver().applyBatch(uri.getAuthority(), ops);
            return AbstractContentProviderBatchTask.Result.forOperations(results);
        } catch (RemoteException e) {
            return AbstractContentProviderBatchTask.Result.forError(e, ops);
        } catch (RuntimeException e) {
            return AbstractContentProviderBatchTask.Result.forError(e, ops);
        } catch (OperationApplicationException e) {
            return AbstractContentProviderBatchTask.Result.forError(e, ops);
        } catch (Exception e) {
            return AbstractContentProviderBatchTask.Result.forError(e, ops);
        }
    }
}
