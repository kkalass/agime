package de.kalass.agime.analytics;

import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import android.view.View;

import de.kalass.agime.R;
import de.kalass.agime.util.EdgeToEdgeHelper;


/**
 * Created by klas on 07.01.14.
 */
public class AnalyticsActionToolBarActivity extends AnalyticsActionBarActivity {

	private final int _layoutResId;

	protected AnalyticsActionToolBarActivity(int layoutRes) {
		_layoutResId = layoutRes;
	}


	protected void onCreate(Bundle savedInstanceState) {
		// Configure edge-to-edge display for Android 15+
		EdgeToEdgeHelper.setupEdgeToEdge(this);
		super.onCreate(savedInstanceState);
		setContentView(_layoutResId);

		Toolbar toolbar = (Toolbar)findViewById(R.id.action_bar);
		setSupportActionBar(toolbar);

		final ActionBar actionBar = getSupportActionBar();

		actionBar.setDisplayShowHomeEnabled(false);
		actionBar.setDisplayHomeAsUpEnabled(true);

		toolbar.setNavigationOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				AnalyticsActionToolBarActivity.this.finish();
			}
		});

		// Apply window insets to the toolbar for edge-to-edge display
		setupToolbarInsets();
	}


	private void setupToolbarInsets() {
		Toolbar toolbar = (Toolbar)findViewById(R.id.action_bar);
		EdgeToEdgeHelper.applySystemWindowInsetsToToolbar(toolbar);
	}

}
