package de.kalass.android.common.support.fragments;

import android.content.Intent;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.ListFragment;

/**
 * Provides a workaround for bugs in the support library wrt nested fragments. Should be used as
 * baseclass  for the toplevel fragment.
 *
 * <ul>
 *     <li>onActivityResult is not forwarded to child fragment, but to parent fragment</li>
 * </ul>
 * Created by klas on 20.12.13.
 */
public class BaseListFragment extends ListFragment implements FragmentBugfix {

    private final FragmentBugfixSupport _support = new FragmentBugfixSupport();

    /**
     * Registers request code (used in
     * {@link #startActivityForResult(android.content.Intent, int)}).
     *
     * @param requestCode
     *            the request code.
     * @param id
     *            the fragment ID (can be {@link Fragment#getId()} of
     *            {@link Fragment#hashCode()}).
     */
    public void registerRequestCode(int requestCode, int id) {
       _support.registerRequestCode(requestCode, id);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        if (!_support.delegateStartActivityForResultToParent(this, intent, requestCode)) {
            super.startActivityForResult(intent, requestCode);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!_support.checkNestedFragmentsForResult(this, requestCode, resultCode, data))
            super.onActivityResult(requestCode, resultCode, data);
    }// onActivityResult()

}
