package de.kalass.agime.category;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.TextView;

import com.google.common.collect.ImmutableList;

import java.util.List;

import de.kalass.agime.ColorSuggestion;
import de.kalass.agime.R;
import de.kalass.agime.loader.CategoryModelAsyncLoader;
import de.kalass.agime.model.CategoryModel;
import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.activity.BaseCRUDListFragment;
import de.kalass.android.common.activity.BaseLoadingViewModelListAdapter;
import de.kalass.android.common.activity.BaseViewWrapper;

/**
 * Shows the list of all Categories that were tracked.
 * Created by klas on 06.10.13.
 */
public class CategoryListFragment extends BaseCRUDListFragment {
    public static final int LOADER_ID = 1;

    public CategoryListFragment() {
        super(MCContract.Category.CONTENT_TYPE_DIR);
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        setEmptyText(getText(R.string.fragment_category_management_empty_text));

        setLoadingListAdapter(LOADER_ID, new CategoryLoadingListAdapter(getActivity()));
    }

    @Override
    protected void deleteItems(List<Long> rowItemIds, List<Integer> selectedPositions) {
        CategoryEditorDBUtil.delete(getActivity(), null, rowItemIds);
    }

    private final class CategoryLoadingListAdapter extends BaseLoadingViewModelListAdapter<WrappedView, CategoryModel> {

        public CategoryLoadingListAdapter(
                Context context
        ) {
            super(context, WrappedView.LAYOUT);
        }

        @Override
        public Loader<List<CategoryModel>> onCreateLoader(int i, Bundle bundle) {
            return new CategoryModelAsyncLoader(getActivity());
        }

        @Override
        protected WrappedView onWrapView(View view) {
            return new WrappedView(view);
        }

        @Override
        public void bindWrappedView(WrappedView view, CategoryModel model, int position) {
            view.name.setText(model.getName());
            view.color.setBackgroundColor(ColorSuggestion.getCategoryColor(getResources(), model));
        }

    }

    static final class WrappedView extends BaseViewWrapper {
        static final int LAYOUT = R.layout.category_list_item;
        static final int ID_NAME = R.id.category_name;
        static final int ID_COLOR = R.id.category_color;

        final TextView name;
        final View color;


        public WrappedView(View view) {
            super(view);
            name = getTextView(ID_NAME);
            color = getView(ID_COLOR);
        }
    }
}
