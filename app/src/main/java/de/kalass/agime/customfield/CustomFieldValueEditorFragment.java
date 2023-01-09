package de.kalass.agime.customfield;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.database.Cursor;
import android.view.View;
import android.widget.EditText;

import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import de.kalass.agime.R;
import de.kalass.agime.analytics.AnalyticsBaseCursorCRUDFragment;
import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.activity.CRUDMode;
import de.kalass.android.common.util.StringUtil;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.kalass.android.common.simpleloader.CursorUtil.getIndex;

/**
 * The editor fragment of a custom field type value.
 */
public class CustomFieldValueEditorFragment extends AnalyticsBaseCursorCRUDFragment<CustomFieldValueEditorFragment.WrappedView, Cursor>
{

    public CustomFieldValueEditorFragment() {
        super(WrappedView.LAYOUT, Content.CONTENT_TYPE_DIR, Content.CONTENT_TYPE_ITEM, Content.PROJECTION, Functions.<Cursor>identity());
    }

    @Override
    protected CRUDMode getMode() {
        // we currently do not support a real view mode
        CRUDMode requestedMode = super.getMode();
        return requestedMode == CRUDMode.VIEW ? CRUDMode.EDIT : requestedMode;
    }


    @Override
    protected WrappedView onWrapView(View view) {
        return new WrappedView(view);
    }

    @Override
    protected void onBindViewToCursor(WrappedView view, Cursor cursor) {
        Preconditions.checkArgument(getMode() == CRUDMode.INSERT || cursor != null, "Only new items do not provide data");
        String value = cursor == null ? null : cursor.getString(Content.IDX_VALUE);

        view.value.setText(value);
    }

    @Override
    protected void readDataFromView(WrappedView view, ContentValues result) {
        result.put(Content.COLUMN_NAME_VALUE, StringUtil.toString(view.value.getText()));
    }

    @Override
    protected void delete() {
        assertCanDelete();
        // Delete all references to custom field values
        CustomFieldValueEditorDBUtil.delete(getContext(), this, ImmutableList.of(getEntityId()));
    }

    static final class WrappedView {
        static final int LAYOUT = R.layout.fragment_custom_field_value_editor;
        static final int ID_VALUE_FIELD = R.id.name;

        final EditText value;

        WrappedView(View view) {
            value = checkNotNull((EditText) view.findViewById(ID_VALUE_FIELD));
        }

    }
    public static final class Content {
        public static final String CONTENT_TYPE_DIR = MCContract.CustomFieldValue.CONTENT_TYPE_DIR;
        public static final String CONTENT_TYPE_ITEM = MCContract.CustomFieldValue.CONTENT_TYPE_ITEM;
        public static final String COLUMN_NAME_VALUE = MCContract.CustomFieldValue.COLUMN_NAME_VALUE;
        public static final String[] PROJECTION = new String[] {
                MCContract.CustomFieldType._ID,
                COLUMN_NAME_VALUE
        };
        public static final int IDX_VALUE = getIndex(PROJECTION, COLUMN_NAME_VALUE);
    }
}
