package de.kalass.android.common;

import android.app.AlertDialog;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.Context;
import android.content.OperationApplicationException;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.util.Log;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.kalass.android.common.util.Arrays2;

/**
* Created by klas on 14.01.14.
*/
public class ApplyBatchTask extends AbstractContentProviderBatchTask<ContentProviderOperation> {


    public ApplyBatchTask(Context context, int errorTitleResource, int errorMessageResource) {
        super(context, errorTitleResource, errorMessageResource);
    }

    @Override
    protected ArrayList<ContentProviderOperation> createOperationsInBackground(ContentProviderOperation... params) {
        return Arrays2.asArrayList(params);
    }

    @Override
    protected ArrayList<ContentProviderOperation> createOperationsInBackground(ContentProviderOperation input) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>(1);
        ops.add(input);
        return ops;
    }

}
