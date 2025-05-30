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
import de.kalass.agime.util.EdgeToEdgeHelper;


/**
 * An activity, that will notify the preferences of activity start/stop - and thus allows to attach content change
 * listeners and such.
 *
 * Created by klas on 08.01.14.
 */
public class CustomPreferenceActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Configure edge-to-edge display for Android 15+
		EdgeToEdgeHelper.setupEdgeToEdge(this);
		super.onCreate(savedInstanceState);

		setContentView(R.layout.pref_with_toolbar);

		Toolbar toolbar = (Toolbar)findViewById(R.id.action_bar);

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

		// Apply window insets to the toolbar for edge-to-edge display
		setupToolbarInsets();
	}


	private void setupToolbarInsets() {
		Toolbar toolbar = (Toolbar)findViewById(R.id.action_bar);
		EdgeToEdgeHelper.applySystemWindowInsetsToToolbar(toolbar);
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
