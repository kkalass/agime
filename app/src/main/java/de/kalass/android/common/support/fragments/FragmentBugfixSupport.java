package de.kalass.android.common.support.fragments;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.util.SparseIntArray;

import java.util.List;

/**
 * Provides a workaround for bugs in the support library wrt nested fragments.
 *
 * Based on https://code.google.com/p/android/issues/detail?id=40537
 *
 * <ul>
 *     <li>onActivityResult is not forwarded to child fragment, but to parent fragment</li>
 * </ul>
 * Created by klas on 20.12.13.
 */
public class FragmentBugfixSupport {

    private final SparseIntArray mRequestCodes = new SparseIntArray();

    /**
     * Registers request code (used in
     * {@link Fragment#startActivityForResult(android.content.Intent, int)}).
     *
     * @param requestCode
     *            the request code.
     * @param id
     *            the fragment ID (can be {@link android.support.v4.app.Fragment#getId()} of
     *            {@link android.support.v4.app.Fragment#hashCode()}).
     */
    public void registerRequestCode(int requestCode, int id) {
        mRequestCodes.put(requestCode, id);
    }

    public boolean delegateStartActivityForResultToParent(Fragment fragment, Intent intent, int requestCode) {
        if (fragment.getParentFragment() instanceof FragmentBugfix) {
            ((FragmentBugfix) fragment.getParentFragment()).registerRequestCode(
                    requestCode, fragment.hashCode());
            fragment.getParentFragment().startActivityForResult(intent, requestCode);
            return true;
        }
        return false;
    }

    /**
     * Checks to see whether there is any children fragments which has been
     * registered with {@code requestCode} before. If so, let it handle the
     * {@code requestCode}.
     *
     * @param requestCode
     *            the code from {@link Fragment#onActivityResult(int, int, android.content.Intent)}.
     * @param resultCode
     *            the code from {@link Fragment#onActivityResult(int, int, android.content.Intent)}.
     * @param data
     *            the data from {@link Fragment#onActivityResult(int, int, android.content.Intent)}.
     * @return {@code true} if the results have been handed over to some child
     *         fragment. {@code false} otherwise.
     */
    protected boolean checkNestedFragmentsForResult(Fragment f, int requestCode,
                                                    int resultCode, Intent data) {
        final int id = mRequestCodes.get(requestCode);
        if (id == 0)
            return false;

        mRequestCodes.delete(requestCode);

        List<Fragment> fragments = f.getChildFragmentManager().getFragments();
        if (fragments == null)
            return false;

        for (Fragment fragment : fragments) {
            if (fragment != null && fragment.hashCode() == id) {
                fragment.onActivityResult(requestCode, resultCode, data);
                return true;
            }
        }

        return false;
    }
}
