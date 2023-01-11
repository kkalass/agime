package de.kalass.agime.project;

import android.content.Context;
import android.os.Bundle;
import androidx.loader.content.Loader;
import android.view.View;
import android.widget.TextView;

import java.util.List;

import de.kalass.agime.ColorSuggestion;
import de.kalass.agime.R;
import de.kalass.agime.loader.ProjectModelAsyncLoader;
import de.kalass.agime.loader.ProjectModelQuery;
import de.kalass.agime.model.ProjectModel;
import de.kalass.android.common.activity.BaseCRUDListFragment;
import de.kalass.android.common.activity.BaseLoadingViewModelListAdapter;
import de.kalass.android.common.activity.BaseViewWrapper;
import de.kalass.android.common.util.TimeFormatUtil;

/**
 * Shows the list of all Activity Types that were tracked.
 * Created by klas on 06.10.13.
 */
public class ProjectListFragment extends BaseCRUDListFragment {

    public static final int LOADER_ID = 1;

    public ProjectListFragment() {
        super(ProjectModelQuery.CONTENT_TYPE_DIR);
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        setEmptyText(getText(R.string.fragment_project_management_empty_text));

        setLoadingListAdapter(LOADER_ID, new ProjectLoadingListAdapter(getActivity()));
    }

    @Override
    protected void deleteItems(List<Long> rowItemIds, List<Integer> selectedPositions) {
        ProjectEditorDBUtil.delete(getActivity(), null, rowItemIds);
    }

    private class ProjectLoadingListAdapter extends BaseLoadingViewModelListAdapter<ProjectWrappedView, ProjectModel> {


        public ProjectLoadingListAdapter(
                Context context
        ) {
            super(context, ProjectWrappedView.LAYOUT);
        }

        @Override
        public Loader<List<ProjectModel>> onCreateLoader(int id, Bundle args) {
            return new ProjectModelAsyncLoader(getContext());
        }

        @Override
        protected ProjectWrappedView onWrapView(View view) {
            return new ProjectWrappedView(view);
        }

        @Override
        public void bindWrappedView(ProjectWrappedView view, ProjectModel model, int position) {
            view.name.setText(model.getName());
            int textColor = getTextColor(model);
            view.name.setTextColor(textColor);
            view.activeUntil.setTextColor(textColor);
            if (model.isProjectDurationLimited()) {
                view.activeUntil.setVisibility(View.VISIBLE);
                view.activeUntil.setText(TimeFormatUtil.formatDateAbbrev(getContext(), model.getActiveUntilDate()));
            } else {
                view.activeUntil.setVisibility(View.GONE);
            }
            view.color.setBackgroundColor(ColorSuggestion.getProjectColor(getResources(), model));
        }

        protected int getTextColor(ProjectModel model) {

            return getContext().getResources().getColor(model.isInactive() ? R.color.material_black_text_disabled : R.color.material_black_text);
        }
    }

    static final class ProjectWrappedView extends BaseViewWrapper {
        static final int LAYOUT = R.layout.project_list_item;
        static final int ID_NAME_FIELD = R.id.project_name;
        static final int ID_COLOR = R.id.project_color;
        static final int ID_ACTIVE_UNTIL = R.id.project_active_until;

        final TextView name;
        final View color;
        final TextView activeUntil;

        ProjectWrappedView(View view) {
            super(view);
            name = getTextView(ID_NAME_FIELD);
            color = getView(ID_COLOR);
            activeUntil = getTextView(ID_ACTIVE_UNTIL);
        }
    }

}
