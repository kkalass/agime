package de.kalass.agime.customfield;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.TextView;

import java.util.List;

import de.kalass.agime.R;
import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.activity.BaseCRUDListFragment;
import de.kalass.android.common.activity.BaseLoadingCursorAdapter;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.kalass.android.common.simpleloader.CursorUtil.getIndex;

/**
 * Shows the list of all Activity Types that were tracked.
 * Created by klas on 06.10.13.
 */
public class CustomFieldValueListFragment extends BaseCRUDListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LOADER_ID_CUSTOM_FIELD_VALUES = 1;

    private static final int LOADER_ID_CUSTOM_FIELD_TYPE = 2;

    interface CustomFieldValueListActivity {
        void setTypeName(String name);
    }

    public CustomFieldValueListFragment() {
        super(Content.CONTENT_TYPE_DIR);
    }

    @Override
    protected void deleteItems(List<Long> rowItemIds, List<Integer> selectedPositions) {
        CustomFieldValueEditorDBUtil.delete(getActivity(), null, rowItemIds);
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);

        final Uri contentURI = getContentURI();

        setLoadingListAdapter(
                LOADER_ID_CUSTOM_FIELD_VALUES,
                new CustomFieldValueListAdapter(getActivity())
                        .setQueryParams(
                                contentURI,
                                Content.PROJECTION,
                                null, null, null
                        )
        );

        getLoaderManager().initLoader(LOADER_ID_CUSTOM_FIELD_TYPE, null, this);
    }

    private Uri getTypeUri() {
        return Content.getParentUri(getContentURI());
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_CUSTOM_FIELD_TYPE:
                return new CursorLoader(getActivity(), getTypeUri(), CustomFieldTypeDAO.PROJECTION, null, null, null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case LOADER_ID_CUSTOM_FIELD_TYPE:
                data.moveToFirst();
                final String typeName = data.getString(CustomFieldTypeDAO.IDX_NAME);
                applyTypeName(typeName);
                return;
        }
    }

    protected void applyTypeName(String typeName) {
        final Activity activity = getActivity();
        if (activity instanceof  CustomFieldValueListActivity) {
            CustomFieldValueListActivity a = (CustomFieldValueListActivity)activity;
            a.setTypeName(typeName);
        }
        if (typeName == null) {
            typeName = getString(R.string.data_type_name_custom_field);
        }
        setEmptyText(getString(R.string.custom_field_value_list_empty_text, typeName));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LOADER_ID_CUSTOM_FIELD_TYPE:
                // ignore
                return;
        }
    }

    private class CustomFieldValueListAdapter extends BaseLoadingCursorAdapter<CustomFieldValueWrappedView> {

        public CustomFieldValueListAdapter(
                Context context
        ) {
            super(context, CustomFieldValueWrappedView.LAYOUT, FLAG_REGISTER_CONTENT_OBSERVER);
        }

        @Override
        protected CustomFieldValueWrappedView onWrapView(View view) {
            return new CustomFieldValueWrappedView(view);
        }

        @Override
        public void bindWrappedView(CustomFieldValueWrappedView view, Context context, Cursor cursor) {
            view.value.setText(cursor.getString(Content.IDX_VALUE));
        }
    }

    static final class CustomFieldValueWrappedView {
        static final int LAYOUT = R.layout.crud_management_list_item;
        static final int ID_VALUE_FIELD = R.id.name;

        final TextView value;

        CustomFieldValueWrappedView(View view) {
            value = checkNotNull((TextView) view.findViewById(ID_VALUE_FIELD));
        }
    }

    public static final class Content {
        public static final String CONTENT_TYPE_DIR = MCContract.CustomFieldValue.CONTENT_TYPE_DIR;
        public static final String COLUMN_NAME_VALUE = MCContract.CustomFieldValue.COLUMN_NAME_VALUE;
        public static final String[] PROJECTION = new String[] {
                MCContract.CustomFieldValue._ID,
                COLUMN_NAME_VALUE
        };
        public static final int IDX_VALUE = getIndex(PROJECTION, COLUMN_NAME_VALUE);
        public static final Uri getParentUri(Uri uri) {
            return MCContract.CustomFieldValue.getTypeUriFromDirURI(uri);
        }
    }
}
