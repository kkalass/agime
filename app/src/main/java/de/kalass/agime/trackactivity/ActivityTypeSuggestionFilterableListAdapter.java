package de.kalass.agime.trackactivity;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import de.kalass.agime.ColorSuggestion;
import de.kalass.agime.R;
import de.kalass.agime.category.CategoryEditorFragment;
import de.kalass.agime.loader.ActivityTypeSuggestionFeature;
import de.kalass.agime.loader.ActivityTypeSuggestionSyncLoader;
import de.kalass.agime.loader.ActivityTypeSyncLoader;
import de.kalass.agime.loader.CategorySyncLoader;
import de.kalass.agime.loader.ProbabilityBasedSuggestionSyncLoader;
import de.kalass.agime.loader.ProjectSyncLoader;
import de.kalass.agime.model.ActivityTypeModel;
import de.kalass.agime.model.ActivityTypeSuggestionModel;
import de.kalass.agime.model.CategoryModel;
import de.kalass.agime.model.ProjectModel;
import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.adapter.AbstractViewModelListAdapter;
import de.kalass.android.common.simpleloader.CompoundSyncLoaderFilter;

/**
* Created by klas on 06.11.13.
*/
class ActivityTypeSuggestionFilterableListAdapter
    extends AbstractViewModelListAdapter<ActivityTypeSuggestionModel>
    implements Filterable
{

    public static final int ID_NEW_ENTRY_NO_CATEGORY = -1;
    public static final int ID_NEW_ENTRY_NEW_CATEGORY = -2;
    public static final int ID_NEW_ENTRY_FOR_CATEGORY_OFFSET = -10;
    public static final int ID_EDIT_ENTRY = -3;

    private String _newCategorySuggestionName;
    private final AtomicReference<SuggestionFilterConstraints> _filterConstraints = new AtomicReference<SuggestionFilterConstraints>();

    public ActivityTypeSuggestionFilterableListAdapter(Context context) {
        super(context, android.R.layout.simple_list_item_2);
    }

    public void setNewCategorySuggestionName(String name) {
        synchronized (this) {
            _newCategorySuggestionName = name;
        }
        notifyDataSetChanged();
    }

    public synchronized String getNewCategorySuggestionName() {
        return _newCategorySuggestionName;
    }

    @Override
    protected View fillView(View view, ActivityTypeSuggestionModel model, int position) {
        TextView titleView = (TextView)view.findViewById(android.R.id.text1);
        TextView projectView = (TextView)view.findViewById(android.R.id.text2);

        final Context context = getContext();
        view.setBackgroundColor(model.getCategoryColor(context));
        Resources resources = context.getResources();
        titleView.setTextColor(resources.getColor(R.color.category_text));
        projectView.setTextColor(resources.getColor(R.color.category_text));

        switch(model.getType()) {
            case NEW_CREATE_CATEGORY:
                titleView.setText(getString(R.string.activity_type_suggestion_create_activity_create_category_title, model.getActivityName()));
                String cname = model.getNewCategoryName();
                projectView.setText(Strings.isNullOrEmpty(cname)
                        ? getString(R.string.activity_type_suggestion_create_activity_create_category_subtitle_unnamed)
                        : getString(R.string.activity_type_suggestion_create_activity_create_category_subtitle, cname));
                break;
            case NEW_WITH_CATEGORY:
                titleView.setText(getString(R.string.activity_type_suggestion_create_activity_with_category_title, model.getActivityName()));
                projectView.setText(model.getCategoryName(context));
                break;
            case EDIT:
                titleView.setText(getString(R.string.activity_type_suggestion_edit_activity_title, model.getActivityName()));
                projectView.setText("");
                view.setBackgroundColor(resources.getColor(R.color.edit_category_background_default));
                titleView.setTextColor(resources.getColor(R.color.edit_category_text));
                projectView.setTextColor(resources.getColor(R.color.edit_category_text));
                break;
            default:
                titleView.setText(model.getActivityName());
                projectView.setText(model.getCategoryName(context) + " - " + model.getProjectName(context));
                break;
        }

        return view;
    }


    @Override
    public Filter getFilter() {
        return new ActivityTypeSuggestionFilter(getContext(), _filterConstraints);
    }

    public ActivityTypeSuggestionModel getItemById(long currentItemId) {
        for (int i = 0; i < getCount(); i++) {
            ActivityTypeSuggestionModel item = getItem(i);
            if (item != null && item.getId() == currentItemId) {
                return item;
            }
        }
        return null;
    }

    public void setTimes(final LocalDate date, final LocalTime startTime, final LocalTime endTime) {
        _filterConstraints.set(new SuggestionFilterConstraints(date, startTime, endTime));
    }

    private static final class SuggestionFilterConstraints {
        final LocalDate date;
        final LocalTime startTime;
        final LocalTime endTime;

        private SuggestionFilterConstraints(final LocalDate date, final LocalTime startTime, final LocalTime endTime) {
            this.date = date;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    private class ActivityTypeSuggestionFilter extends CompoundSyncLoaderFilter {
        private final ProbabilityBasedSuggestionSyncLoader _probabilityBasedSuggestionLoader;
        private final ActivityTypeSuggestionSyncLoader _activityTypeSuggestionLoader;
        private final CategorySyncLoader _categoryLoader;
        private final ProjectSyncLoader _projectLoader;
        private final ActivityTypeSyncLoader _activityTypeLoader;
        private final AtomicReference<SuggestionFilterConstraints> _constraints;

        public ActivityTypeSuggestionFilter(Context context, AtomicReference<SuggestionFilterConstraints> constraints) {
            this(context, new CategorySyncLoader(context), constraints);
        }

        private ActivityTypeSuggestionFilter(Context context, CategorySyncLoader categoryLoader, AtomicReference<SuggestionFilterConstraints> constraints) {
            this(context,
                    categoryLoader,
                    new ProjectSyncLoader(context),
                    new ActivityTypeSuggestionSyncLoader(context),
                    new ActivityTypeSyncLoader(context, categoryLoader),
                    constraints);
        }

        private ActivityTypeSuggestionFilter(Context context,
                                             CategorySyncLoader categoryLoader,
                                             ProjectSyncLoader projectSyncLoader,
                                             ActivityTypeSuggestionSyncLoader activityTypeSuggestionLoader,
                                             ActivityTypeSyncLoader activityTypeLoader,
                                             AtomicReference<SuggestionFilterConstraints> constraints
        ) {
            this(new ProbabilityBasedSuggestionSyncLoader(context, projectSyncLoader, null/*no custom field*/, activityTypeLoader, activityTypeSuggestionLoader),
                 activityTypeSuggestionLoader,
                    categoryLoader,
                    projectSyncLoader,
                    activityTypeLoader,
                    constraints);
        }

        private ActivityTypeSuggestionFilter(
                ProbabilityBasedSuggestionSyncLoader probabilityBasedSuggestionLoader,
                ActivityTypeSuggestionSyncLoader activityTypeSuggestionLoader,
                CategorySyncLoader categoryLoader,
                ProjectSyncLoader projectLoader,
                ActivityTypeSyncLoader activityTypeLoader,
                AtomicReference<SuggestionFilterConstraints> constraints) {
            super(probabilityBasedSuggestionLoader,
                  activityTypeSuggestionLoader,
                  categoryLoader,
                  projectLoader,
                  activityTypeLoader);
            _probabilityBasedSuggestionLoader = probabilityBasedSuggestionLoader;
            _activityTypeSuggestionLoader = activityTypeSuggestionLoader;
            _categoryLoader = categoryLoader;
            _projectLoader = projectLoader;
            _activityTypeLoader = activityTypeLoader;
            _constraints = constraints;
        }


        private String asName(CharSequence constraint) {
            return constraint == null ? null : constraint.toString();
        }

        protected List<ActivityTypeSuggestionModel> loadData(String name) {
            boolean all = Strings.isNullOrEmpty(name);
            final String order = MCContract.ActivityType.COLUMN_NAME_NAME + " asc";
            SuggestionFilterConstraints suggestionFilterConstraints = _constraints.get();
            if (all) {

                if (suggestionFilterConstraints == null) {
                    return _probabilityBasedSuggestionLoader.loadByLastUse(LocalDate.now());
                }
                return _probabilityBasedSuggestionLoader.query(
                        suggestionFilterConstraints.date,
                        suggestionFilterConstraints.startTime,
                        suggestionFilterConstraints.endTime
                );
            }

            // categories of matching name
            List<CategoryModel> categoryModels = _categoryLoader.load(
                    MCContract.Category.COLUMN_NAME_NAME + " like ?",
                    new String[]{"%" + name + "%"},
                    MCContract.Category.COLUMN_NAME_NAME + " asc"
            );
            List<Long> categoryIds = Lists.transform(categoryModels, CategoryModel.GET_ID);

            // activities of matching name
            String selection = MCContract.ActivityType.COLUMN_NAME_NAME + " like ? ";
            if (!categoryIds.isEmpty()) {
                selection += " OR " + MCContract.ActivityType.COLUMN_NAME_ACTIVITY_CATEGORY_ID + " in (" + Joiner.on(",").join(categoryIds) + ")";
            }

            final Map<Long, ActivityTypeModel> candidateActivities = _activityTypeLoader.loadAsMap(selection, new String[]{"%" + name + "%"}, order);

            LocalDate date = (suggestionFilterConstraints == null || suggestionFilterConstraints.date == null) ? LocalDate.now() : suggestionFilterConstraints.date;
            // projects with matching name
            List<ProjectModel> projectModels = _projectLoader.load(
                    MCContract.Project.COLUMN_NAME_NAME + " like ? AND (" + MCContract.Project.COLUMN_NAME_ACTIVE_UNTIL_MILLIS + " IS NULL OR " + MCContract.Project.COLUMN_NAME_ACTIVE_UNTIL_MILLIS + " >= ? )",
                    new String[]{"%" + name + "%", date.toDateTimeAtStartOfDay().getMillis() + ""},
                    MCContract.Project.COLUMN_NAME_NAME + " asc"
            );
            List<Long> projectIds = Lists.transform(projectModels, ProjectModel.GET_ID);

            return _activityTypeSuggestionLoader.queryAll(candidateActivities,
                    projectIds,
                    date,
                    // suggest activities, that were not tracked with the available projects before.
                    // This path will be called if the user has entered a search string, so we may
                    // safely assume that the user intends to search all activity types
                    ImmutableSet.of(ActivityTypeSuggestionFeature.SUGGEST_ALL_ACTIVITIES));
        }

        protected List<ActivityTypeSuggestionModel> load(String name) {
            final boolean loadAllOnPreciseMatch = true;
            List<ActivityTypeSuggestionModel> activityTypeModels = loadData(name);

            if (Strings.isNullOrEmpty(name)) {
                return activityTypeModels;
            }

            if (!activityTypeModels.isEmpty()) {
                ActivityTypeSuggestionModel firstItem = activityTypeModels.get(0);
                if (name.equalsIgnoreCase(firstItem.getActivityName())) {
                    // precise match, add an option to edit
                    ImmutableList.Builder<ActivityTypeSuggestionModel> builder = ImmutableList.builder();
                    builder.add(firstItem);
                    builder.add(ActivityTypeSuggestionModel.forEditEntry(ID_EDIT_ENTRY, firstItem));

                    // when an item is queried and selected because the user explicitely
                    // focussed the field, he will expect a complete dropdown of all options
                    if (loadAllOnPreciseMatch) {
                        Set<Long> ids = new HashSet<Long>(activityTypeModels.size());
                        ids.add(firstItem.getId());
                        for (int i = 1 ; i < activityTypeModels.size(); ++i) {
                            ActivityTypeSuggestionModel m = activityTypeModels.get(i);
                            builder.add(m);
                            ids.add(m.getId());
                        }
                        final List<ActivityTypeSuggestionModel> all = loadData(null);
                        for (ActivityTypeSuggestionModel m: all) {
                            if (!ids.contains(m.getId())) {
                                builder.add(m);
                            }
                        }
                    } else {
                        if (activityTypeModels.size() > 1) {
                            builder.addAll(activityTypeModels.subList(1, activityTypeModels.size()));
                        }
                    }
                    return builder.build();
                } else {
                    return activityTypeModels;
                }
            }
            // create an entry for each category
            List<CategoryModel> categoryModels = _categoryLoader.load(null, null, MCContract.Category.COLUMN_NAME_NAME + " asc");
            ImmutableList.Builder<ActivityTypeSuggestionModel> builder = ImmutableList.builder();
            // special "id" and entry for an entry without category
            // should be first, because creating a new entry without category should be default
            // if the user chooses not to explicitely select any item
            builder.add(ActivityTypeSuggestionModel.forNewEntry(
                    ID_NEW_ENTRY_NO_CATEGORY, name, null
            ));

            // special "id" and entry for creating a new Category with the new Activity
            builder.add(ActivityTypeSuggestionModel.forNewEntryRequestingNewCategory(
                    ID_NEW_ENTRY_NEW_CATEGORY,
                    name,
                    getNewCategorySuggestionName(),
                    CategoryEditorFragment.loadNewCategoryColor(getContext())
            ));

            for(CategoryModel model : categoryModels) {
                builder.add(ActivityTypeSuggestionModel.forNewEntry(
                        ID_NEW_ENTRY_FOR_CATEGORY_OFFSET - model.getId(),
                        name,
                        model
                ));
            }

            return builder.build();
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            String name = asName(constraint);
            try {
                return asFilterResults(load(name));
            } finally {
                releaseResources();
            }
        }

        private int getNewCategoryColor(Resources resources, List<CategoryModel> categoryModels) {
            if (categoryModels.isEmpty()) {
                return ColorSuggestion.suggestCategoryColor(resources, 1 /*going to be the first category*/);
            }
            Long maxCategoryId = Ordering.natural().onResultOf(CategoryModel.GET_ID).max(categoryModels).getId();
            return ColorSuggestion.suggestCategoryColor(resources, maxCategoryId.intValue() + 1);
        }

        private FilterResults asFilterResults(List<ActivityTypeSuggestionModel> activityTypeModels) {
            FilterResults results = new FilterResults();
            results.count = activityTypeModels.size();
            results.values = activityTypeModels;
            return results;
        }

        @Override
        public CharSequence convertResultToString(Object resultValue) {
            ActivityTypeSuggestionModel result = (ActivityTypeSuggestionModel)resultValue;
            return result.getActivityName();
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            setItems((List<ActivityTypeSuggestionModel>)results.values);
        }
    }
}
