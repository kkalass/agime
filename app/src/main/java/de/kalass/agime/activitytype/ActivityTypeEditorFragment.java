package de.kalass.agime.activitytype;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.EditText;

import com.google.common.collect.ImmutableList;

import de.kalass.agime.ColorSuggestion;
import de.kalass.agime.R;
import de.kalass.agime.analytics.AnalyticsBaseCRUDFragment;
import de.kalass.agime.category.CategoryEditorFragment;
import de.kalass.agime.loader.ActivityTypeSyncLoader;
import de.kalass.agime.loader.CategorySyncLoader;
import de.kalass.agime.model.ActivityCategorySuggestionModel;
import de.kalass.agime.model.ActivityTypeModel;
import de.kalass.agime.model.CategoryModel;
import de.kalass.agime.provider.MCContract;
import de.kalass.agime.provider.MCContract.Category;
import de.kalass.android.common.activity.BaseViewWrapper;
import de.kalass.android.common.activity.CRUDMode;
import de.kalass.android.common.simpleloader.CompoundAsyncLoader;
import de.kalass.android.common.simpleloader.ObserveDataSourceMode;
import de.kalass.android.common.util.StringUtil;
import de.kalass.android.common.widget.AutoCompleteSpinner;


public class ActivityTypeEditorFragment
    extends AnalyticsBaseCRUDFragment<ActivityTypeEditorFragment.ActivityTypeFragmentView, ActivityTypeEditorFragment.ActivityTypeFragmentData>
    implements AutoCompleteSpinner.OnItemSetListener
{

    private ActivityTypeFragmentData _data;

    public ActivityTypeEditorFragment() {
        super(ActivityTypeFragmentView.LAYOUT,
                MCContract.ActivityType.CONTENT_TYPE_DIR,
                MCContract.ActivityType.CONTENT_TYPE_ITEM);
    }

    @Override
    protected CRUDMode getMode() {
        // we currently do not support a real view mode
        CRUDMode requestedMode = super.getMode();
        return requestedMode == CRUDMode.VIEW ? CRUDMode.EDIT : requestedMode;
    }

    @Override
    public Loader<ActivityTypeFragmentData> createLoader(int id, Bundle arg) {
        return new ActivityTypeFragmentDataAsyncLoader(getContext(), getEntityId());
    }

    @Override
    protected ActivityTypeFragmentView onWrapView(View view) {
        ActivityTypeFragmentView v = new ActivityTypeFragmentView(view);
        v.setCategorySuggestionAdapter(new ActivityCategorySuggestionFilterableListAdapter(getContext()));
        return v;
    }


    @Override
    protected void onBindView(ActivityTypeFragmentView view, ActivityTypeFragmentData data) {
        _data = data;
        view.category.setOnItemSetListener(null);

        view.name.setText(data.getName());
        view.setCategory(getContext(), data.getCategoryName(), data.getCategoryColorCode());

        view.category.setOnItemSetListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _data = null;
        getWrappedView().getCategorySuggestionAdapter().setItems(null);
    }

    @Override
    public void onItemSet(AutoCompleteSpinner spinner, boolean userSelectedExplicitely, int position, long itemId) {
        switch (spinner.getId()) {
            case ActivityTypeFragmentView.ID_CATEGORY:
                onCategorySuggestionItemSet(position);
                return;
        }
    }

    @Override
    public void onItemReset(AutoCompleteSpinner spinner) {
        switch (spinner.getId()) {
            case ActivityTypeFragmentView.ID_CATEGORY:
                onCategorySuggestionItemReset();
                return;
        }
    }

    private void onCategorySuggestionItemSet(int position) {
        if (position < 0) {
            // the item was set programmatically, the suggestion adapter did not load the
            // corresponding data. The caller who set this item programmatically has to take
            // care to update the UI accordingly
            return;
        }
        final ActivityTypeFragmentView view = getWrappedView();
        ActivityCategorySuggestionModel item = view.getCategorySuggestionAdapter().getItem(position);

        _data.setSelectedCategory(item.getCategory());

        view.setCategory(getContext(), _data.getCategoryName(), _data.getCategoryColorCode());
    }

    private void onCategorySuggestionItemReset() {
        final ActivityTypeFragmentView view = getWrappedView();
        String value = StringUtil.toString(view.category.getText());
        _data.setSelectedCategory(null, value, null);
        view.setCategory(getContext(), _data.getCategoryName(), _data.getCategoryColorCode());
    }

    @Override
    protected void delete() {
        assertCanDelete();
        ActivityTypeEditorDBUtil.delete(getContext(), this, ImmutableList.of(getEntityId()));
    }

    @Override
    protected void save() {
        assertIsSaveOrUpdate();

        final ActivityTypeFragmentView view = getWrappedView();

        final InsertOrUpdateInputBuilder builder = new InsertOrUpdateInputBuilder()
                .setEntityId(getEntityId());

        final Long categoryId = _data.getCategoryId();
        builder.setCategoryTypeId(categoryId);
        builder.setCategoryColor(_data.getCategoryColorCode());
        if (categoryId == null) {
            // no existing project, but maybe the user wants to create a new one - use
            // the value of the textfield
            builder.setCategoryName(StringUtil.toString(view.category.getText()));
        } else {
            builder.setCategoryName(_data.getCategoryName());
        }

        // the details are not synced automatically to "data", so we read them from the view
        builder.setActivityTypeName(StringUtil.toString(view.name.getText()));

        final InsertOrUpdateInput input = builder.createInsertOrUpdateInput();

        performSaveOrUpdateAsync(input, new InsertOrUpdateActivityType(getContext()));
    }

    public static final class ActivityTypeFragmentView extends BaseViewWrapper {
        static final int LAYOUT = R.layout.activity_type_edit;
        static final int ID_CATEGORY = R.id.activity_category;
        static final int ID_NAME = R.id.activity_type;

        final EditText name;
        final AutoCompleteSpinner category;

        public ActivityTypeFragmentView(View view) {
            super(view);
            name = getEditText(ID_NAME);
            category = getAutoCompleteSpinner(ID_CATEGORY);
        }

        public void setCategorySuggestionAdapter(ActivityCategorySuggestionFilterableListAdapter adapter) {
            category.setAdapter(adapter);
        }

        public ActivityCategorySuggestionFilterableListAdapter getCategorySuggestionAdapter() {
            return (ActivityCategorySuggestionFilterableListAdapter)category.getAdapter();
        }

        public void setCategory(Context context, String categoryName, Integer categoryColorCode) {
            int color = categoryColorCode == null
                    ? context.getResources().getColor(R.color.category5_default)
                    : categoryColorCode;

            category.setBackgroundColor(color);
            if (!StringUtil.trim(categoryName).equals(StringUtil.trim(category.getText()))) {
                category.setText(categoryName);
            }
        }
    }

    public static final class ActivityTypeFragmentData {
        private Long categoryId;
        private String categoryName;
        private Integer categoryColorCode;

        private final String name;
        private final Integer newCategoryColor;

        public ActivityTypeFragmentData(String name, int newCategoryColor) {
            this.name = name;
            this.newCategoryColor = newCategoryColor;
        }

        public String getName() {
            return name;
        }

        public void setSelectedCategory(Long id, String name, Integer colorCode) {
            categoryId = id;
            categoryName = name;
            categoryColorCode = (colorCode == null && id == null && !StringUtil.isTrimmedNullOrEmpty(name))
                    ? newCategoryColor : colorCode;

        }

        public void setSelectedCategory(CategoryModel m) {
            if (m == null) {
                setSelectedCategory(null, null, null);
            } else {
                setSelectedCategory(m.getId(), m.getName(), m.getColour());
            }
        }

        public Long getCategoryId() {
            return categoryId;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public Integer getCategoryColorCode() {
            return categoryColorCode;
        }


    }

    public static final class ActivityTypeFragmentDataAsyncLoader extends CompoundAsyncLoader<ActivityTypeFragmentData> {

        private final ActivityTypeSyncLoader loader;
        private final Long entityId;

        public ActivityTypeFragmentDataAsyncLoader(Context context, Long entityId) {
            super(context, ObserveDataSourceMode.IGNORE_CHANGES /*editor should not reload on changes*/ );
            this.entityId = entityId;
            loader = add(new ActivityTypeSyncLoader(context, new CategorySyncLoader(context)));
        }

        @Override
        public ActivityTypeFragmentData doLoadInBackground() {
            final int newCategoryDefaultColor = CategoryEditorFragment.loadNewCategoryColor(getContext());

            if (entityId == null) {
                return new ActivityTypeFragmentData(null, newCategoryDefaultColor);
            }
            final ActivityTypeModel typeModel = loader.load(entityId);

            ActivityTypeFragmentData r = new ActivityTypeFragmentData(typeModel.getName(), newCategoryDefaultColor);
            r.setSelectedCategory(typeModel == null ? null : typeModel.getCategoryModel());
            return r;
        }
    }
}
