package de.kalass.android.common.preferences;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import androidx.appcompat.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;
import android.widget.ListAdapter;

import de.kalass.agime.R;

/**
 * An activity, that will notify the preferences of activity start/stop - and thus allows
 * to attach content change listeners and such.
 *
 * Created by klas on 08.01.14.
 */
public class CustomPreferenceActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.pref_with_toolbar);

        Toolbar toolbar = (Toolbar) findViewById(R.id.action_bar);

        //addPreferencesFromResource(R.xml.preferences);

        toolbar.setClickable(true);
        toolbar.setNavigationIcon(getResIdFromAttribute(this, androidx.appcompat.R.attr.homeAsUpIndicator));
        toolbar.setTitle(R.string.menu_settings);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    private static int getResIdFromAttribute(final Activity activity, final int attr) {
        if (attr == 0) {
            return 0;
        }
        final TypedValue typedvalueattr = new TypedValue();
        activity.getTheme().resolveAttribute(attr, typedvalueattr, true);
        return typedvalueattr.resourceId;
    }

    @Override
    protected void onStart() {
        super.onStart();
        propagateOnStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        propagateOnStop();
    }

    private void propagateOnStart() {
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        final ListAdapter preferenceScreenRootAdapter = preferenceScreen.getRootAdapter();
        for (int i = 0; i < preferenceScreenRootAdapter.getCount(); i++) {
            final Object item = preferenceScreenRootAdapter.getItem(i);
            if (item instanceof CustomPreference) {
                ((CustomPreference)item).onActivityStart();
            }

        }
    }

    private void propagateOnStop() {
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        final ListAdapter preferenceScreenRootAdapter = preferenceScreen.getRootAdapter();
        for (int i = 0; i < preferenceScreenRootAdapter.getCount(); i++) {
            final Object item = preferenceScreenRootAdapter.getItem(i);
            if (item instanceof CustomPreference) {
                ((CustomPreference)item).onActivityStop();
            }

        }
    }
}
