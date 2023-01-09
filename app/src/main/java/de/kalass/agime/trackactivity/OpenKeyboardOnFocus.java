package de.kalass.agime.trackactivity;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

/**
* Created by klas on 23.01.14.
*/
class OpenKeyboardOnFocus implements View.OnFocusChangeListener {
    private TrackedActivityFragment trackedActivityFragment;

    public OpenKeyboardOnFocus(TrackedActivityFragment trackedActivityFragment) {
        this.trackedActivityFragment = trackedActivityFragment;
    }

    @Override
    public void onFocusChange(final View v, final boolean hasFocus) {
        if (hasFocus && (v != null)) {
            v.post(new Runnable() {

                @Override
                public void run() {
                    if (trackedActivityFragment == null) {
                        return;
                    }
                    Activity activity = trackedActivityFragment.getActivity();
                    if (activity == null) {
                        return;
                    }
                    InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm == null) {
                        return;
                    }
                    imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
                        //getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);         }
                }
            });
        }

    }
}
