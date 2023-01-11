package de.kalass.android.common.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.google.common.base.Preconditions;

import de.kalass.agime.R;

public abstract class BaseCRUDManagementActivity extends AppCompatActivity {

    private final Uri _uri;

    public BaseCRUDManagementActivity(Uri uri) {
        _uri = Preconditions.checkNotNull(uri);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.base_crud_management_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.action_bar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            final Intent intent = getIntent();
            Uri uri = intent.getData();
            /*
             * Creates an Intent to use when the Activity object's result is sent back to the
             * caller.
             */
            if (uri == null) {
                uri = _uri;
            } else {
                ContentResolverUtil.assertSameContentType(this, uri, getContentResolver().getType(_uri));
            }
            BaseCRUDListFragment fragment = newCRUDFragment();
            fragment.setArguments(BaseCRUDListFragment.setCRUDArguments(fragment.getArguments(), uri));

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, fragment)
                    .commit();
        }
    }


    protected abstract BaseCRUDListFragment newCRUDFragment();

    protected BaseCRUDListFragment getCRUDFragment() {
        return (BaseCRUDListFragment)getSupportFragmentManager().findFragmentById(R.id.container);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(de.kalass.agime.R.menu.crud_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_add) {
            insertNew();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void insertNew() {
        final Uri uri = getCRUDFragment().getContentURI();
        startActivity(newInsertItemIntent(uri));
    }

    protected Intent newInsertItemIntent(Uri uri) {
        return new Intent(Intent.ACTION_INSERT, uri);
    }

}
