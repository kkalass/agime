package de.kalass.android.common;

/**
 * Created by klas on 20.05.15.
 */

import android.app.Activity;
import android.content.Context;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

/**
 * Created by klas on 23.01.14.
 */
public class OpenKeyboardOnFocus implements View.OnFocusChangeListener {
    private Fragment fragment;

    public OpenKeyboardOnFocus(Fragment fragment) {
        this.fragment = fragment;
    }

    @Override
    public void onFocusChange(final View v, final boolean hasFocus) {
        if (hasFocus && (v != null)) {
            v.post(new Runnable() {

                @Override
                public void run() {
                    Log.i("OnFocus", "called " + hasFocus + " " + fragment);
                    if (fragment == null) {
                        return;
                    }
                    Activity activity = fragment.getActivity();
                    if (activity == null) {
                        return;
                    }
                    InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm == null) {
                        return;
                    }
                    Log.i("OnFocus", "calling show implicit ");
                    //imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
                    Window window = activity.getWindow();
                    if (window == null) {
                        return;
                    }
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                }
            });
        }

    }
}
