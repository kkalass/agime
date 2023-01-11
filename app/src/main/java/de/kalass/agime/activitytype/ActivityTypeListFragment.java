package de.kalass.agime.activitytype;

import android.os.Bundle;
import androidx.loader.content.Loader;
import android.view.View;
import android.widget.TextView;

import java.util.List;

import de.kalass.agime.ColorSuggestion;
import de.kalass.agime.R;
import de.kalass.agime.loader.ActivityTypeAsyncLoader;
import de.kalass.agime.model.ActivityTypeModel;
import de.kalass.agime.model.CategoryModel;
import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.activity.BaseCRUDListFragment;
import de.kalass.android.common.activity.BaseLoadingViewModelListAdapter;
import de.kalass.android.common.activity.BaseViewWrapper;

/**
 * Shows the list of all Activity Types that were tracked.
 * Created by klas on 06.10.13.
 */
public class ActivityTypeListFragment extends BaseCRUDListFragment {

    public static final int LOADER_ID = 1;

    public ActivityTypeListFragment() {
        super(MCContract.ActivityType.CONTENT_TYPE_DIR);
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        setEmptyText(getText(R.string.fragment_activity_types_empty_text));

        setLoadingListAdapter(LOADER_ID, new ActivityTypeLoadingListAdapter());
    }

    @Override
    protected void deleteItems(List<Long> rowItemIds, List<Integer> selectedPositions) {
        ActivityTypeEditorDBUtil.delete(getActivity(), null, rowItemIds);
    }

    private class ActivityTypeLoadingListAdapter extends BaseLoadingViewModelListAdapter<WrappedView, ActivityTypeModel> {

        public ActivityTypeLoadingListAdapter(
        ) {
            super(ActivityTypeListFragment.this.getActivity(), WrappedView.LAYOUT);
        }

        @Override
        protected WrappedView onWrapView(View view) {
            return new WrappedView(view);
        }

        @Override
        public Loader<List<ActivityTypeModel>> onCreateLoader(int i, Bundle bundle) {
            return new ActivityTypeAsyncLoader(getContext());
        }

        @Override
        public void bindWrappedView(WrappedView view, ActivityTypeModel model, int position) {
            final CategoryModel category = model.getCategoryModel();

            view.activityName.setText(model.getName());

            view.categoryName.setTextColor(getResources().getColor(R.color.category_text));
            view.categoryName.setBackgroundColor(ColorSuggestion.getCategoryColor(getResources(), category));
            view.categoryName.setText(category == null ? "" : category.getName());

        }
    }

    static final class WrappedView extends BaseViewWrapper {
        static final int LAYOUT = R.layout.activity_list_item;

        static final int ID_ACTIVITY_NAME = R.id.activity_name;
        static final int ID_CATEGORY_NAME = R.id.category_name;

        final TextView activityName;
        final TextView categoryName;

        public WrappedView(View view) {
            super(view);
            activityName = getTextView(ID_ACTIVITY_NAME);
            categoryName = getTextView(ID_CATEGORY_NAME);
        }
    }
}
