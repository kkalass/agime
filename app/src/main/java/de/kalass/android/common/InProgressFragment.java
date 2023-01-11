package de.kalass.android.common;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.Fragment;

import de.kalass.agime.R;

/**
 * We use the progress dialog fragment for two things: first (obviously) showing
 * a progress dialog. But the second purpose is, to provide a hook to the
 * current activity and allow the currently executing task to access the activity instance,
 * thus making it robust against orientation changes.
 */
public final class InProgressFragment extends Fragment {
    private static final String LOG_TAG = "ProgressDialog";
    public static final String ARG_TITLE = "title";
    public static final String ARG_MESSAGE = "message";
    public static final String PROGRESS_DLG_TAG = "AsyncTask_ProgressDlg";

    private ProgressDialog progressDialog;


    public static InProgressFragment createInstance(boolean useProgressDialog) {
        Bundle b = createArguments(useProgressDialog);
        InProgressFragment f  = new InProgressFragment();
        f.setArguments(b);
        return f;
    }

    public static Bundle createArguments(boolean useProgressDialog) {
        Bundle b = new Bundle();
        b.putBoolean("useProgressDialog", useProgressDialog);
        return b;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        //Log.i(LOG_TAG, "************* UI Interaction created ***********");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //Log.i(LOG_TAG, "************* UI Interaction; activity created ***********");
        // If we are returning here from a screen orientation
        // and the AsyncTask is still working, re-create and display the
        // progress dialog.
        Bundle arguments = getArguments();
        boolean useProgressDialog = arguments.getBoolean("useProgressDialog", false);
        progressDialog = useProgressDialog ? newProgressDialog(getActivity()) : null;
        if (progressDialog!= null) {
            progressDialog.show();
        }
    }

    @Override
    public void onDetach() {
        // All dialogs should be closed before leaving the activity in order to avoid
        // the: Activity has leaked window com.android.internal.policy... exception
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
        super.onDetach();
    }


    private String getString(Context context, String key, int fbResId) {
        Bundle args = getArguments();
        if (args != null && args.containsKey(key)) {
            return args.getString(key);
        }
        return context.getString(fbResId);
    }

    protected ProgressDialog newProgressDialog(Context context) {
        ProgressDialog pd = new ProgressDialog(context);

        pd.setTitle(getString(context, ARG_TITLE, R.string.async_task_progress_title));
        pd.setMessage(getString(context, ARG_MESSAGE, R.string.async_task_progress_message));

        pd.setCancelable(false);
        pd.setIndeterminate(true);
        return pd;
    }

}
