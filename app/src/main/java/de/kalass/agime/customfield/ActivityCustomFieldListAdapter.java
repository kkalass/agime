package de.kalass.agime.customfield;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.sqlite.SQLiteCursor;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.FilterQueryProvider;
import android.widget.TextView;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.kalass.agime.R;
import de.kalass.agime.provider.MCContract;
import de.kalass.agime.trackactivity.InsertOrUpdateOperationFactory;
import de.kalass.android.common.activity.BaseCursorAdapter;
import de.kalass.android.common.activity.BaseListAdapter;
import de.kalass.android.common.activity.ContentResolverUtil;
import de.kalass.android.common.simpleloader.CursorFkt;
import de.kalass.android.common.simpleloader.CursorUtil;
import de.kalass.android.common.util.StringUtil;
import de.kalass.android.common.widget.AutoCompleteSpinner;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A ListAdapter for Custom Fields in tracked activities.
 *
 * loads the available Custom Fields as well as the corresponding value for the given activity (if available).
 *
 * Created by klas on 09.01.14.
 */
public class ActivityCustomFieldListAdapter
        extends BaseListAdapter<
                            ActivityCustomFieldListAdapter.ActivityCustomFieldViewWrapper,
                            ActivityCustomFieldEditorModel
                        >
{

    private List<ActivityCustomFieldEditorModel> _unfilteredItems;
    private List<ActivityCustomFieldValuesSuggestionAdapter> _adapters;
    private Long _projectId;

    public ActivityCustomFieldListAdapter(Context context) {
        super(context, ActivityCustomFieldViewWrapper.LAYOUT);
    }

    public void initialize(Long projectId,
                           List<ActivityCustomFieldEditorModel> customFields) {
        _projectId = projectId;
        _unfilteredItems = customFields;
        updateFilteredItems();
    }



    protected void updateFilteredItems() {
        final List<ActivityCustomFieldEditorModel> items = filterItems(_projectId, _unfilteredItems);
        int size = items == null ? 0 : items.size();
        clearAdapters();
        _adapters = new ArrayList<ActivityCustomFieldValuesSuggestionAdapter>(size);
        super.setItems(items);
    }

    private void clearAdapters() {
        if (_adapters != null) {
            /*
             * Really important: reset the dependend adapters which contain references
             * to cursors that will be leaked if not changed to null!
             */
            for (ActivityCustomFieldValuesSuggestionAdapter adapter: _adapters) {
                Log.i("LOG", "Clearing child adapter with cursor " + adapter.getCursor());
                adapter.changeCursor(null);
            }
        }
    }

    private List<ActivityCustomFieldEditorModel> filterItems(
            Long projectId,
            List<ActivityCustomFieldEditorModel> unfilteredItems
    ) {
        if (unfilteredItems == null) {
            return null;
        }
        ArrayList<ActivityCustomFieldEditorModel> result = new ArrayList<ActivityCustomFieldEditorModel>(unfilteredItems.size());
        for (ActivityCustomFieldEditorModel model : unfilteredItems) {
            if (accept(model, projectId)) {
                result.add(model);
            }
        }
        return result;
    }

    private boolean accept(ActivityCustomFieldEditorModel model, Long projectId) {
        if (model == null) {
            return false;
        }
        if (model.anyProject) {
            return true;
        }
        return model.enabledProjectIds.contains(projectId);
    }

    public void setProjectId(Long projectId) {
        Long oldProjectId = _projectId;
        _projectId = projectId;
        if (!com.google.common.base.Objects.equal(oldProjectId, _projectId)) {
            updateFilteredItems();
        }
    }

    @Override
    protected ActivityCustomFieldViewWrapper onWrapView(View view) {
        return new ActivityCustomFieldViewWrapper(view);
    }

    public void setSelectedItems(List<ActivityCustomFieldModel> customFieldModels) {
        Map<Long, ActivityCustomFieldModel> dataByCustomFieldTypeId = Maps.uniqueIndex(customFieldModels, ActivityCustomFieldModel.GET_TYPE_ID);
        for (int i = 0; i < getCount(); i++) {
            final ActivityCustomFieldEditorModel item = getItem(i);
            final ActivityCustomFieldModel suggestion = dataByCustomFieldTypeId.get(item.getTypeId());
            final CustomFieldValueModel value = suggestion == null ? null : suggestion.getValueModel();

            // do not override selected values, if the user for example selected the custom field
            // values first and then decided to use a different activity type
            if (StringUtil.isTrimmedNullOrEmpty(item.selectedValue)) {
                item.selectedValue = value == null ? null : value.getValue();
                item.selectedValueId = value == null ? null : value.getId();
            }
        }
        notifyDataSetChanged();
    }

    /**
     * Applies changes of the custom field data to the model instance
     */
    static final class ActivityCustomFieldChangedListener implements TextWatcher, AutoCompleteSpinner.OnItemSetListener{
        private final ActivityCustomFieldEditorModel _model;

        ActivityCustomFieldChangedListener(ActivityCustomFieldEditorModel model) {
            _model = model;
        }

        @Override
        public void onItemSet(AutoCompleteSpinner spinner, boolean userSelectedExplicitely, int position, long itemId) {
            _model.selectedValueId = itemId;
        }

        @Override
        public void onItemReset(AutoCompleteSpinner spinner) {
            _model.selectedValueId = null;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            _model.selectedValue = s == null ? null : s.toString();
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }

    public void setItems(List<ActivityCustomFieldEditorModel> _unfilteredItems) {
        this._unfilteredItems = _unfilteredItems;
        updateFilteredItems();
    }

    @Override
    public void bindWrappedView(ActivityCustomFieldViewWrapper view, ActivityCustomFieldEditorModel model, int position) {
        view.label.setText(model.typeName);
        final ActivityCustomFieldValuesSuggestionAdapter adapter = new ActivityCustomFieldValuesSuggestionAdapter(getContext(), model.typeId);
        _adapters.add(adapter);
        view.spinner.setAdapter(adapter);
        if (model.selectedValueId != null) {
            view.spinner.setCurrentItem(model.selectedValueId, model.selectedValue);
        }

        ActivityCustomFieldChangedListener listener = new ActivityCustomFieldChangedListener(model);
        view.spinner.setOnItemSetListener(listener);
        view.spinner.addTextChangedListener(listener);
    }


    public InsertOrUpdateOperationFactory getSaveOrUpdateOperationFactory() {
        ImmutableList.Builder<TrackedActivitySaveOrUpdateOperationFactoryImpl.Data> builder = ImmutableList.builder();
        for (int i = 0; i < getCount(); i++) {
            final ActivityCustomFieldEditorModel item = getItem(i);
            TrackedActivitySaveOrUpdateOperationFactoryImpl.Data data = new TrackedActivitySaveOrUpdateOperationFactoryImpl.Data(
                    item.selectedValueId, item.selectedValue, item.originalSelectedValue, item.originalSelectedValueId, item.associationId, item.typeId
            );
            builder.add(data);
        }
        return new TrackedActivitySaveOrUpdateOperationFactoryImpl(builder.build());
    }


    static final class ActivityCustomFieldValuesSuggestionAdapter
            extends BaseCursorAdapter<ActivityCustomFieldValuesSuggestionViewWrapper>
            //implements FilterQueryProvider
    {

        private static final String LOG_TAG = "CustomFieldAdapter";
        private final long _typeId;

        /**
         * @param context The context
         */
        public ActivityCustomFieldValuesSuggestionAdapter(Context context, long typeId) {
            super(context, ActivityCustomFieldValuesSuggestionViewWrapper.LAYOUT, 0);
            _typeId = typeId;
            //setFilterQueryProvider(this);
        }


        @Override
        public CharSequence convertToString(Cursor cursor) {
            return cursor.getString(ActivityCustomFieldValueSuggestionQuery.IDX_VALUE);
        }

        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {

            Cursor retval = ContentResolverUtil.queryByName(
                    getContext().getContentResolver(),
                    constraint,
                    ActivityCustomFieldValueSuggestionQuery.CONTENT_URI,
                    ActivityCustomFieldValueSuggestionQuery.SELECTION,
                    ActivityCustomFieldValueSuggestionQuery.args(constraint, _typeId),
                    ActivityCustomFieldValueSuggestionQuery.SELECTION,
                    ActivityCustomFieldValueSuggestionQuery.args("", _typeId),
                    ActivityCustomFieldValueSuggestionQuery.ORDERING,
                    ActivityCustomFieldValueSuggestionQuery.IDX_VALUE,
                    ActivityCustomFieldValueSuggestionQuery.COLUMN_NAME_ID,
                    ActivityCustomFieldValueSuggestionQuery.ID_READER,
                    ActivityCustomFieldValueSuggestionQuery.PROJECTION
            );

            return retval;
        }

        @Override
        protected ActivityCustomFieldValuesSuggestionViewWrapper onWrapView(View view) {
            return new ActivityCustomFieldValuesSuggestionViewWrapper(view);
        }

        @Override
        public void bindWrappedView(ActivityCustomFieldValuesSuggestionViewWrapper view, Context context, Cursor cursor) {
            view.value.setText(cursor.getString(ActivityCustomFieldValueSuggestionQuery.IDX_VALUE));
        }
    }

    static final class ActivityCustomFieldValuesSuggestionViewWrapper {
        static final int LAYOUT = android.R.layout.simple_list_item_1;
        static final int ID_VALUE = android.R.id.text1;
        final TextView value;
        ActivityCustomFieldValuesSuggestionViewWrapper(View view) {
            value = (TextView)view.findViewById(ID_VALUE);
        }
    }

    static final class ActivityCustomFieldValueSuggestionQuery {
        static final Uri CONTENT_URI = MCContract.CustomFieldValue.CONTENT_URI_SUGGESTION;
        static final String COLUMN_NAME_ID = v(MCContract.CustomFieldValue._ID);
        static final String COLUMN_NAME_VALUE = v(MCContract.CustomFieldValue.COLUMN_NAME_VALUE);
        static final String COLUMN_NAME_TRACKED_ACTIVITY_ID = a(MCContract.ActivityCustomFieldValue.COLUMN_NAME_TRACKED_ACTIVITY_ID);
        static final String COLUMN_NAME_TYPE_ID = v(MCContract.CustomFieldValue.COLUMN_NAME_CUSTOM_FIELD_TYPE_ID);
        static final String[] PROJECTION = new String[] {
                COLUMN_NAME_ID,
                COLUMN_NAME_VALUE,
                COLUMN_NAME_TYPE_ID
        };
        static final int IDX_ID = CursorUtil.getIndex(PROJECTION, COLUMN_NAME_ID);
        static final int IDX_VALUE = CursorUtil.getIndex(PROJECTION, COLUMN_NAME_VALUE);
        static final int IDX_TYPE_ID = CursorUtil.getIndex(PROJECTION, COLUMN_NAME_TYPE_ID);

        static final String ORDERING = " max(" + COLUMN_NAME_TRACKED_ACTIVITY_ID + ") desc, " + COLUMN_NAME_ID + " asc";
        static final String SELECTION = COLUMN_NAME_VALUE + " like ? AND " + COLUMN_NAME_TYPE_ID + " = ? ";

        static final String[] args(CharSequence constraint, long typeId) {
            return new String[]{"%" + constraint + "%", Long.toString(typeId)};
        }
        /**
         * Qualifies a column name with the table name.
         */
        private static final String v(String column) {
            return  MCContract.CustomFieldValue.TABLE + "." + column;
        }
        /**
         * Qualifies a column name with the table name.
         */
        private static final String a(String column) {
            return  MCContract.ActivityCustomFieldValue.TABLE + "." + column;
        }

        static final class Data {

            final long id;
            final long typeId;
            final String value;

            Data(Cursor cursor) {
                id = cursor.getLong(IDX_ID);
                typeId = cursor.getLong(IDX_TYPE_ID);
                value = cursor.getString(IDX_VALUE);
            }
        }

        static final Function<Cursor, Long> ID_READER = CursorFkt.newLongGetter(IDX_ID);

    }

    static final class ActivityCustomFieldViewWrapper {
        static final int LAYOUT = R.layout.activity_track_custom_field_row;

        static final int ID_LABEL = R.id.custom_field_value_label;
        static final int ID_SPINNER = R.id.custom_field_value;

        final TextView label;
        final AutoCompleteSpinner spinner;

        ActivityCustomFieldViewWrapper(View view) {
            label = (TextView)checkNotNull(view.findViewById(ID_LABEL));
            spinner = (AutoCompleteSpinner)checkNotNull(view.findViewById(ID_SPINNER));
        }
    }

}
