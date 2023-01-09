package de.kalass.agime;

import android.os.Build;

/**
 * Created by klas on 20.12.13.
 */
public final class Workarounds {

    /**
     * Seems to be a bug with 4.0 and 4.1 in the support library: nested fragments
     * will not add their menu items correctly to the action bar, but if the parent adds
     * it, the nested fragment will be called....
     */
    public static boolean nestedFragmentMenuInitializedByParent() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1;
    }
}
