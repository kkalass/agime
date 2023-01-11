package de.kalass.android.common.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.EnumSet;
import java.util.Set;

import de.kalass.agime.R;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class BaseCRUDActivity extends AppCompatActivity implements BaseCRUDFragment.CRUDListener {
    public static final String EXTRA_ID = "id";


    private CRUDMode _mode;

    public BaseCRUDActivity() {
    }

    /**
     * @return the set of CRUDModes you support directly. The default is EDIT and INSERT,
     * because Intents for VIEW or DELETE can be handled by an EDIT mode as well.
     *
     * If you do implement a specific VIEW or DELETE mode you may override this here.
     */
    protected Set<CRUDMode> getSupportedModes() {
        return EnumSet.of(CRUDMode.EDIT, CRUDMode.INSERT);
    }

    protected CRUDMode getMode() {
        return _mode;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.base_crud_activity);


        Toolbar toolbar = getToolbar();

        final Intent intent = getIntent();
        _mode = CRUDMode.fromIntentAction(intent, getSupportedModes());
        if (savedInstanceState == null) {

            Uri uri = intent.getData();
            BaseCRUDFragment fragment = newCRUDFragment(_mode);
            // some fragments change the mode, for example from view to edit

            initializeFromCRUDIntent(fragment, _mode, uri);

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, fragment)
                    .commit();
        }

        setupActionBar(toolbar);
        setupBottomBar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //updateBottomBarVisibility();
    }

    public Toolbar getToolbar() {
        return (Toolbar) findViewById(R.id.action_bar);
    }

    public static <T extends BaseCRUDFragment> T initializeFromCRUDIntent (T fragment, CRUDMode mode, Uri uri) {
        fragment.setArguments(BaseCRUDFragment.setCRUDArguments(fragment.getArguments(), mode, uri));
        return fragment;
    }

    protected BaseCRUDFragment getCRUDFragment() {
        return (BaseCRUDFragment)getSupportFragmentManager().findFragmentById(R.id.container);
    }

    protected abstract BaseCRUDFragment newCRUDFragment(CRUDMode mode);

    private void setupBottomBar() {
        Toolbar bottomToolbar = (Toolbar)findViewById(R.id.bottom_bar);
        Button deleteButton = (Button) bottomToolbar.findViewById(R.id.action_delete);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCRUDFragment().delete();

            }
        });
        updateBottomBarVisibility();
    }

    private void updateBottomBarVisibility() {
        View container = findViewById(R.id.container);
        Toolbar bottomToolbar = (Toolbar)findViewById(R.id.bottom_bar);

        BaseCRUDFragment crudFragment = getCRUDFragment();
        boolean notDeletable = _mode == CRUDMode.INSERT || (crudFragment != null && !crudFragment.isDeletable()) || !isDeletable();
        int visibility = notDeletable ? View.GONE : View.VISIBLE;
        bottomToolbar.setVisibility(visibility);
        View sep = findViewById(R.id.bottom_bar_separator);
        sep.setVisibility(visibility);
        if (visibility == View.GONE) {
            LinearLayout.LayoutParams margin = new LinearLayout.LayoutParams(container.getLayoutParams());
            margin.bottomMargin = 0;
            container.setLayoutParams(margin);
        }
    }

    protected boolean isDeletable() {
        return true;
    }

    private void setupActionBar(Toolbar toolbar) {
        setSupportActionBar(toolbar);
        ActionBar bar = getSupportActionBar();

        boolean editMode = _mode == CRUDMode.INSERT
                || _mode == CRUDMode.EDIT
                || (_mode == CRUDMode.DELETE && !getSupportedModes().contains(_mode))
                || (_mode == CRUDMode.VIEW && !getSupportedModes().contains(_mode));
        if (editMode) {
            setupEditModeActionBar(bar, toolbar);

        } else {
            bar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupEditModeActionBar(ActionBar actionBar, final Toolbar toolbar) {
        //bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_HOME);
        actionBar.setHomeButtonEnabled(true);

        if (_mode == CRUDMode.EDIT) {
            toolbar.setTitle(R.string.action_edit);
        } else if (_mode == CRUDMode.INSERT) {
            toolbar.setTitle(R.string.action_add);
        }

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BaseCRUDActivity.this.finish();
                //Log.i("BaseCRUD", "navigation clicked!");
            }
        });
        //bar.setDisplayShowTitleEnabled(true);
        toolbar.setNavigationIcon(R.drawable.ic_action_dismiss);
        //bar.setDisplayShowCustomEnabled(true);
        //bar.setHomeAsUpIndicator(R.drawable.ic_action_dismiss);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //if (_mode == CRUDMode.EDIT || _mode == CRUDMode.VIEW) {
        getMenuInflater().inflate(de.kalass.agime.R.menu.crud_edit, menu);
        MenuItem saveItem = menu.findItem(R.id.action_save);
        MenuItem editItem = menu.findItem(R.id.action_edit);
        if (saveItem != null) {
            saveItem.setVisible(getMode() == CRUDMode.EDIT || getMode() == CRUDMode.INSERT);
        }
        if (editItem != null) {
            editItem.setVisible(getMode() == CRUDMode.VIEW);
        }
        //}
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_save) {
            getCRUDFragment().save();
            return true;
        }
        if (itemId == R.id.action_edit) {
            Intent intent = getIntent();
            startActivity(new Intent(Intent.ACTION_EDIT, intent.getData()));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onEntityDeleted(BaseCRUDFragment<?,?> fragment, long entityId, Object payload) {
        Intent result = new Intent();
        result.putExtra(EXTRA_ID, entityId);
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    public void onEntityInserted(BaseCRUDFragment<?,?> fragment, long entityId, Object payload) {
        Intent result = new Intent();
        result.putExtra(EXTRA_ID, entityId);
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    public void onEntityUpdated(BaseCRUDFragment<?,?> fragment, long entityId, Object payload) {
        Intent result = new Intent();
        result.putExtra(EXTRA_ID, entityId);
        setResult(RESULT_OK, result);
        finish();
    }



}
