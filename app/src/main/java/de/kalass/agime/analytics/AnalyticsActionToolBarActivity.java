package de.kalass.agime.analytics;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.View;

import de.kalass.agime.R;

/**
 * Created by klas on 07.01.14.
 */
public class AnalyticsActionToolBarActivity extends AnalyticsActionBarActivity {
    private final int _layoutResId;

    protected AnalyticsActionToolBarActivity(int layoutRes) {
        _layoutResId = layoutRes;
    }

    protected void onCreate(Bundle savedInstanceState) {
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

    }

}
