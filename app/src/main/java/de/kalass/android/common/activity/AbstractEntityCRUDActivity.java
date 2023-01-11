package de.kalass.android.common.activity;

import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.BaseColumns;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuItem;

import com.google.common.base.Strings;

import java.util.ArrayList;

import de.kalass.android.common.widget.AutoCompleteSpinner;
import de.kalass.agime.R;

/**
 * @deprecated use {@link de.kalass.android.common.activity.BaseCRUDActivity} instead.
 */
@Deprecated()
public abstract class AbstractEntityCRUDActivity extends AppCompatActivity {
    public static final String EXTRA_ID = "activityId";


    private final Uri _contentUri;

    public AbstractEntityCRUDActivity(Uri contentUri) {
        _contentUri = contentUri;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    private void setupActionBar() {
        ActionBar bar = getSupportActionBar();
        bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_HOME);

        if (isEditMode()) {
            bar.setTitle(R.string.action_edit);
        } else  {
            bar.setTitle(R.string.action_add);
        }

        bar.setHomeAsUpIndicator(R.drawable.ic_action_dismiss);


    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_delete) {
            return delete();
        }
        if (itemId == R.id.action_save) {
            save();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected boolean isEditMode() {
        return getIntent().hasExtra(EXTRA_ID);
    }

    protected ArrayList<ContentProviderOperation> createDeletionOperation(Uri entityUri) {
        ArrayList<ContentProviderOperation> list = new ArrayList<ContentProviderOperation>(1);
        list.add(ContentProviderOperation.newDelete(entityUri).withExpectedCount(1).build());
        return list;
    }

    protected boolean delete() {
        long id = getId();
        if (id < 0) {
            throw new IllegalStateException("Cannot delete item without id - is this an update?");
        }
        try {
            getContentResolver().applyBatch(
                    _contentUri.getAuthority(),
                    createDeletionOperation(ContentUris.withAppendedId(_contentUri, id))
            );
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (OperationApplicationException e) {
            throw new RuntimeException(e);
        }
        onEntityDeleted(id);
        finish();
        return true;
    }

    protected long getId() {
        return getIntent().getLongExtra(EXTRA_ID, -1);
    }

    protected Long getIdOrNull() {
        long id = getId();
        return id == -1 ? null : id;
    }

    protected final void save() {
        // Defines an object to contain the new values to insert
        ContentValues mNewValues = fillEntityData(new ContentValues());

        long id = getId();
        if (id < 0) {
            Uri uri = getContentResolver().insert(_contentUri, mNewValues);
            id = ContentUris.parseId(uri);
            onEntityInserted(id, mNewValues);
        } else {
            getContentResolver().update(
                    _contentUri,
                    mNewValues,
                    BaseColumns._ID + "=?",
                    new String[]{Long.toString(id)});
            onEntityUpdated(id, mNewValues);
        }

        // done
        finish();
    }

    protected abstract ContentValues fillEntityData(ContentValues mNewValues);

    protected void onEntityDeleted(Long entityId) {
    }

    protected void onEntityInserted(Long entityId, ContentValues values) {
        setResult(RESULT_OK, null);
    }

    protected void onEntityUpdated(Long entityId, ContentValues values) {
        setResult(RESULT_OK, null);
    }

    protected Long getNamedItemId(
            AutoCompleteSpinner field,
            Uri uri, String columnNameName
    ) {
        long activityTypeId = field.getCurrentItemId();
        if (activityTypeId >= 0) {
            return activityTypeId;
        }
        String value = field.getText().toString();
        if (Strings.isNullOrEmpty(value)) {
            return null;
        }
        Cursor cursor = getContentResolver().query(
                uri,
                new String[] {BaseColumns._ID},
                columnNameName + " = ?",
                new String[] {value},
                null);
        if (cursor != null) {
            try {
                if (cursor.isBeforeFirst() && cursor.moveToNext()) {
                    return cursor.getLong(0 /*only column*/);
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }
    protected Long getOrCreateNamedItemId(
            AutoCompleteSpinner field,
            Uri uri, String columnNameName,
            String columnModifiedAtName, String columnCreatedAtName
    ) {
        return getOrCreateNamedItemId(field, uri, columnNameName,columnModifiedAtName, columnCreatedAtName, new ContentValues());
    }

    protected Long getOrCreateNamedItemId(
            AutoCompleteSpinner field,
            Uri uri, String columnNameName,
            String columnModifiedAtName, String createdAtName,
            ContentValues data
    ) {
        Long id = getNamedItemId(field, uri, columnNameName);
        if (id != null) {
            return id;
        }
        String value = field.getText().toString();
        if (Strings.isNullOrEmpty(value)) {
            return null;
        }

        if (columnModifiedAtName != null) {
            data.put(columnModifiedAtName, System.currentTimeMillis());
        }
        if (createdAtName != null) {
            data.put(createdAtName, System.currentTimeMillis());
        }
        data.put(columnNameName, value);
        Uri newItemUri = getContentResolver().insert(uri, data);
        return ContentUris.parseId(newItemUri);
    }
}
