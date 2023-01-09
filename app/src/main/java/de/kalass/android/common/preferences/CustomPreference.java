package de.kalass.android.common.preferences;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

/**
 * Created by klas on 08.01.14.
 */
public class CustomPreference extends Preference {
    public CustomPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public CustomPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomPreference(Context context) {
        super(context);
    }

    public void onActivityStart() {

    }

    public void onActivityStop() {

    }
}
