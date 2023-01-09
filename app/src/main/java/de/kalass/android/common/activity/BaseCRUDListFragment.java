package de.kalass.android.common.activity;

import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.List;

import de.kalass.agime.R;

/**
 * Shows the list of all Activity Types that were tracked.
 * Created by klas on 06.10.13.
 */
public abstract class BaseCRUDListFragment extends ListFragment implements CABMultiChoiceCallback {
    private static final String LOG_TAG = "BaseCRUDListFragment";
    protected static String ARGS_KEY_CRUD_URI = "de.kalass.crud.uri";
    private final String _mimetype;

    private Uri _contentURI;
    private ActionMode mMode;

    public BaseCRUDListFragment(
        String mimetype
    ) {
        super();
        _mimetype = mimetype;
    }


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        _contentURI = readContentURI(args);
        ContentResolverUtil.assertSameContentType(getActivity(), _contentURI, _mimetype);
    }

    protected Uri readContentURI(Bundle args) {
        return Uri.parse(args.getString(ARGS_KEY_CRUD_URI));
    }


    protected abstract void deleteItems(List<Long> rowItemIds, List<Integer> selectedPositions);

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final ListView listView = getListView();

        UnifiedContextBarSupport.setup(this, listView);

    }



    @Override
    public final void onCreateContextMenu(ContextMenu menu, View v,
                                          ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        UnifiedContextBarSupport.onCreateContextMenu(this, menu, v, menuInfo);
    }


    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return UnifiedContextBarSupport.onContextItemSelected(this, item)
                || super.onContextItemSelected(item);
    }

    /**
     * Override this to control the options for a selected item or selected items
     */
    public void onCreateUnifiedContextBar(MenuInflater inflater, Menu menu) {
        inflater.inflate(R.menu.crud_cab, menu);
    }

    @Override
    public void onUnifiedContextBarSelectionChanged(Menu menu, List<Long> newEntityIds, List<Integer> selectedPositions) {
        // modify the menu here
        final MenuItem editItem = menu.findItem(R.id.action_edit);
        if (editItem != null) {
            editItem.setVisible(newEntityIds.size() == 1);
        }
    }

    @Override
    public boolean onUnifiedContextBarItemSelected(int menuItemId, List<Long> rowItemIds, List<Integer> selectedPositions) {
        if (menuItemId == R.id.action_delete) {
            deleteItems(rowItemIds, selectedPositions);
            return true;
        }
        if (menuItemId == R.id.action_edit) {
            if (rowItemIds.size() == 1) {
                long id = rowItemIds.get(0);
                Intent intent = newEditItemIntent(id);
                startActivity(intent);
            } else {
                Log.w(LOG_TAG, "context menu edit called for " + rowItemIds.size() + " items, will ignore");
            }
            return true;
        }
        return false;
    }

    public final Uri getContentURI() {
        return _contentURI;
    }

    protected <T, A extends ListAdapter & LoaderManager.LoaderCallbacks<T>> void setLoadingListAdapter(
            int loaderId, A adapter
    ) {
        setListAdapter(adapter);
        getLoaderManager().initLoader(loaderId, null, adapter);
    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = newEditItemIntent(id);
        startActivity(intent);
    }

    private Intent newEditItemIntent(long id) {
        return new Intent(Intent.ACTION_EDIT, ContentUris.withAppendedId(_contentURI, id));
    }

    public static <T extends BaseCRUDFragment> Bundle setCRUDArguments (Bundle args, Uri uri) {
        args = args == null ? new Bundle() : args;
        args.putString(ARGS_KEY_CRUD_URI, uri.toString());
        return args;
    }

}
