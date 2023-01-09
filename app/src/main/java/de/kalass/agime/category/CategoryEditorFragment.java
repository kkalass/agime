package de.kalass.agime.category;

import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

import de.kalass.agime.ColorSuggestion;
import de.kalass.agime.R;
import de.kalass.agime.analytics.AnalyticsBaseCRUDFragment;
import de.kalass.agime.color.ColorChooserAdapter;
import de.kalass.agime.loader.CategoryModelQuery;
import de.kalass.agime.model.CategoryModel;
import de.kalass.agime.provider.MCContract;
import de.kalass.agime.provider.MCContract.Category;
import de.kalass.android.common.activity.BaseViewWrapper;
import de.kalass.android.common.activity.CRUDMode;
import de.kalass.android.common.activity.ContentResolverUtil;
import de.kalass.android.common.simpleloader.CompoundAsyncLoader;
import de.kalass.android.common.simpleloader.ObserveDataSourceMode;
import de.kalass.android.common.util.StringUtil;


public class CategoryEditorFragment
        extends AnalyticsBaseCRUDFragment<CategoryEditorFragment.WrappedView, CategoryEditorFragment.CategoryEditModel> {


    public CategoryEditorFragment() {
        super(WrappedView.LAYOUT, Category.CONTENT_TYPE_DIR, Category.CONTENT_TYPE_ITEM);
    }

    @Override
    protected CRUDMode getMode() {
        // we currently do not support a real view mode
        CRUDMode requestedMode = super.getMode();
        return requestedMode == CRUDMode.VIEW ? CRUDMode.EDIT : requestedMode;
    }


    @Override
    public Loader<CategoryEditModel> createLoader(int id, Bundle arg) {
        return new CategoryEditModelAsyncLoader(getContext(), getEntityId());
    }

    @Override
    protected WrappedView onWrapView(View view) {
        WrappedView v =  new WrappedView(view);
        v.setColorChooserAdapter(new ColorChooserAdapter(getContext()));
        return v;
    }

    @Override
    protected void onBindView(WrappedView view, CategoryEditModel data) {
        view.name.setText(data.getName());
        view.setColor(data.getColour());
    }

    @Override
    protected void save() {
        ContentValues values = readDataFromView(getWrappedView(), new ContentValues());
        final ContentProviderOperation operation = createSaveOrUpdateOperation(getMode(), values, System.currentTimeMillis());
        performSaveOrUpdateAsync(operation);
    }

    @Override
    protected void delete() {
        assertCanDelete();
        CategoryEditorDBUtil.delete(getContext(), this, ImmutableList.of(getEntityId()));
    }

    protected ContentValues readDataFromView(WrappedView view, ContentValues values) {
        Integer selectedColor = (Integer)view.color.getSelectedItem();
        int colorCode = selectedColor == null ? 0 : selectedColor.intValue();

        values.put(Category.COLUMN_NAME_NAME, StringUtil.toString(view.name.getText()));
        values.put(Category.COLUMN_NAME_COLOR_CODE, colorCode);
        return values;
    }


    static final class WrappedView extends BaseViewWrapper {
        static final int LAYOUT = R.layout.category_edit;

        static final int ID_NAME = R.id.category_name;
        static final int ID_COLOR = R.id.category_color_spinner;

        final EditText name;
        final Spinner color;

        public WrappedView(View view) {
            super(view);
            name = getEditText(ID_NAME);
            color = getSpinner(ID_COLOR);
        }

        public void setColorChooserAdapter(ColorChooserAdapter adapter) {
            color.setAdapter(adapter);
        }

        public ColorChooserAdapter getColorChooserAdapter() {
            return (ColorChooserAdapter)color.getAdapter();
        }

        public void setColor(Integer colorCode) {
            final ColorChooserAdapter adapter = getColorChooserAdapter();
            adapter.setColor(colorCode);
            int colorPosition = adapter.getColorPosition(colorCode);
            color.setSelection(Math.max(0, colorPosition));
        }
    }

    public static class CategoryEditModel {

        private final int _colour;
        private final String _name;

        public CategoryEditModel(
                String name, int colour
        ) {
            _name = name;
            _colour = colour;
        }



        public int getColour() {
            return _colour;
        }

        public String getName() {
            return _name;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .addValue(_name)
                    .toString();
        }
    }

    public static class CategoryEditModelAsyncLoader extends CompoundAsyncLoader<CategoryEditModel> {

        private final Long _entityId;

        public CategoryEditModelAsyncLoader(Context context, Long entityId) {
            super(context, ObserveDataSourceMode.IGNORE_CHANGES /*no reload during edit*/);
            _entityId = entityId;
        }

        @Override
        public CategoryEditModel doLoadInBackground() {
            if (_entityId == null) {
                final int newCategoryColor = loadNewCategoryColor(getContext());
                return new CategoryEditModel(null, newCategoryColor);
            }
            CategoryModel model = loadFirst(
                    CategoryModelQuery.READER,
                    ContentUris.withAppendedId(Category.CONTENT_URI, _entityId),
                    CategoryModelQuery.PROJECTION);
            return new CategoryEditModel(model.getName(), model.getColour());
        }
    }

    public static int loadNewCategoryColor(Context context) {
        int count = ContentResolverUtil.count(context.getContentResolver(), Category.CONTENT_URI);
        int newCategoryDefaultColor = ColorSuggestion.suggestCategoryColor(context.getResources(), count + 1);
        return newCategoryDefaultColor;
    }
}
