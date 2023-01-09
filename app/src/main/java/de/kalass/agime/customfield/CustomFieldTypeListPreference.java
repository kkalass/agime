package de.kalass.agime.customfield;

import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.linearlistview.LinearListView;

import java.util.ArrayList;
import java.util.List;

import de.kalass.agime.R;
import de.kalass.agime.provider.MCContract;
import de.kalass.agime.settings.Preferences;
import de.kalass.android.common.activity.BaseListAdapter;
import de.kalass.android.common.activity.ContentResolverUtil;
import de.kalass.android.common.preferences.CustomPreference;
import de.kalass.android.common.simpleloader.CursorUtil;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Pseudo-Preference: makes the list of custom field types embeddable in the preferences screen,
 * including an "add" button.
 *
 * Created by klas on 19.12.13.
 */
public class CustomFieldTypeListPreference extends CustomPreference
        implements View.OnClickListener, LinearListView.OnItemClickListener {
    private static final int TOKEN_QUERY_CUSTOM_FIELD_TYPES = 3;
    private static final String LOG_TAG = "CustomFieldTypeListPreference";
    private AsyncQueryHandler _async;
    private WrappedView _wrappedView;

    public CustomFieldTypeListPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setLayoutResource(WrappedView.LAYOUT);
    }

    public CustomFieldTypeListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(WrappedView.LAYOUT);
    }

    public CustomFieldTypeListPreference(Context context) {
        this(context, null);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        final Context context = getContext();
        View view = super.onCreateView(parent);
        _wrappedView = new WrappedView(view);
        view.setTag(_wrappedView);
        _wrappedView.setListAdapter(new CustomFieldTypeListAdapter(context));

        _wrappedView.listView.setOnItemClickListener(this);

        _wrappedView.insertButton.setOnClickListener(this);

        _async = new AsyncQueryHandler(context.getContentResolver()) {
            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                switch(token) {
                    case TOKEN_QUERY_CUSTOM_FIELD_TYPES:
                        WrappedView view = (WrappedView)cookie;
                        try {
                            List<CustomFieldTypeDAO.Data> customFields = CursorUtil.readList(cursor, CustomFieldTypeDAO.READ_DATA);
                            view.getListAdapter().setItems(customFields);
                        } finally {
                            cursor.close();
                        }
                        break;
                    default:
                        // invalid ID was passed in
                }
            }

        };

        queryData();
        return view;
    }

    private void queryData() {
        _async.startQuery(TOKEN_QUERY_CUSTOM_FIELD_TYPES, _wrappedView, CustomFieldTypeDAO.CONTENT_URI, CustomFieldTypeDAO.PROJECTION, null, null, null);
    }

    @Override
    public void onActivityStart() {
        super.onActivityStart();
        // apparently. onActivityStart will be propagated before onCreateView was called
        // - but if it was created once, we still need to refresh the data on next activity start
        if (_async != null && _wrappedView != null) {
            queryData();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case WrappedView.ID_INSERT_BUTTON:
                startActivity(new Intent(Intent.ACTION_INSERT, MCContract.CustomFieldType.CONTENT_URI));
                return;
        }
    }

    @Override
    public void onItemClick(LinearListView linearListView, View view, int position, long l) {
        switch (linearListView.getId()) {
            case WrappedView.ID_LIST:
                long itemId = linearListView.getAdapter().getItemId(position);
                Uri uri = MCContract.CustomFieldValue.getDirUriForType(itemId);

                String type = getContext().getContentResolver().getType(uri);
                Log.i("PREF", "detected type: " + type);
                Intent editItemIntent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(editItemIntent);
                return;
        }

    }

    private void startActivity(Intent intent) {

        getContext().startActivity(intent);
    }

    public static final class WrappedView {
        public static final int LAYOUT = R.layout.preference_custom_field_types_management;

        public static final int ID_LIST = R.id.list;
        public static final int ID_INSERT_BUTTON = R.id.insert;

        private final LinearListView listView;
        private final View insertButton;

        WrappedView(View view) {
            insertButton = checkNotNull(view.findViewById(ID_INSERT_BUTTON));
            listView = checkNotNull((LinearListView)view.findViewById(ID_LIST));
        }

        public void setListAdapter(CustomFieldTypeListAdapter adapter) {
            listView.setAdapter(adapter);
        }

        public CustomFieldTypeListAdapter getListAdapter() {
            return (CustomFieldTypeListAdapter) listView.getAdapter();
        }
    }

    private static class CustomFieldTypeListAdapter extends BaseListAdapter<CustomFieldTypeWrappedView, CustomFieldTypeDAO.Data> implements View.OnClickListener {

        public CustomFieldTypeListAdapter(Context context) {
            super(context, CustomFieldTypeWrappedView.LAYOUT);
            prefillItems(context);
        }

        /**
         * The number of custom fields is cached in the preferences so that the UI
         * contains the correct number of custom field entries right from the start.
         *
         * This avoids flickering and "jumping" in the preference activity
         * @param context
         */
        private void prefillItems(Context context) {
            final int cachedNumCustomFieldTypes = Preferences.getCachedNumCustomFieldTypes(context);
            if (cachedNumCustomFieldTypes > 0) {
                ArrayList<CustomFieldTypeDAO.Data> l = new ArrayList<CustomFieldTypeDAO.Data>(cachedNumCustomFieldTypes);
                for (int i = 0; i < cachedNumCustomFieldTypes; i++) {
                    l.add(null);
                }
                super.setItems(l);
            }
        }

        @Override
        public void setItems(List<CustomFieldTypeDAO.Data> items) {
            super.setItems(items);
            if (items != null) {
                Preferences.setCachedNumCustomFieldTypes(getContext(), items.size());
            }
        }

        @Override
        protected CustomFieldTypeWrappedView onWrapView(View view) {
            return new CustomFieldTypeWrappedView(view);
        }

        @Override
        public void bindWrappedView(CustomFieldTypeWrappedView view, CustomFieldTypeDAO.Data item, int position) {
            if (item == null) {
                view.name.setText("");
                return;
            }
            view.name.setText(item.name);
            view.edit.setTag(ContentUris.withAppendedId(CustomFieldTypeDAO.CONTENT_URI, item.getId()));
            view.edit.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case CustomFieldTypeWrappedView.ID_EDIT:
                    Uri uri = (Uri)v.getTag();
                    getContext().startActivity(new Intent(Intent.ACTION_EDIT, uri));
                    return;
            }
        }
    }

    private static final class CustomFieldTypeWrappedView {
        public static final int LAYOUT = R.layout.preference_custom_field_types_item;

        public static final int ID_NAME = android.R.id.text1;
        public static final int ID_EDIT = R.id.edit;

        private final TextView name;
        private final View edit;

        CustomFieldTypeWrappedView(View view) {
            name = (TextView)view.findViewById(ID_NAME);
            edit = view.findViewById(ID_EDIT);
        }
    }
}
