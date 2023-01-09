package de.kalass.agime.model;

import android.content.Context;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import java.util.List;

import de.kalass.agime.ColorSuggestion;
import de.kalass.agime.R;
import de.kalass.agime.customfield.ActivityCustomFieldModel;
import de.kalass.android.common.model.IViewModel;

/**
 * View Model for activity data.
 * Created by klas on 21.10.13.
 */
public class ActivityTypeSuggestionModel implements IViewModel {
    static final Function<ActivityTypeSuggestionModel, Long> GET_ID = new Function<ActivityTypeSuggestionModel, Long>() {
        @Override
        public Long apply(ActivityTypeSuggestionModel item) {
            return item.getId();
        }
    };

    public enum Type {
        EXISTING_DATA,
        NEW_WITH_CATEGORY,
        NEW_CREATE_CATEGORY,
        EDIT
    }

    private final long _id;
    private final Long _activityTypeId;
    private final String _activityName;
    private final CategoryModel _categoryModel;

    private final String _newCategoryName;
    private final int _newCategoryColor;
    private final ProjectModel _project;
    private Type _type;
    private final List<ActivityCustomFieldModel> _customFieldModels;

    private ActivityTypeSuggestionModel(
            long id,
            Long activityTypeId,
            String name,
            CategoryModel categoryModel,
            Type type,
            String newCategoryName,
            int newCategoryColor,
            ProjectModel projectModel,
            List<ActivityCustomFieldModel> customFieldModels
    ) {
        _activityTypeId = activityTypeId;
        _newCategoryName = newCategoryName;
        _newCategoryColor = newCategoryColor;
        _project = projectModel;
        _id = id;
        _activityName = name;
        _categoryModel = categoryModel;
        _type = type;
        _customFieldModels = Preconditions.checkNotNull(customFieldModels);
    }

    public static ActivityTypeSuggestionModel forNewEntry(
            long idWithinList,
            String name,
            CategoryModel categoryModel
    ) {
        Preconditions.checkArgument(idWithinList < 0);
        return new ActivityTypeSuggestionModel(
                idWithinList,
                null,
                name, categoryModel, Type.NEW_WITH_CATEGORY,
                null, 0, null, ImmutableList.<ActivityCustomFieldModel>of());
    }

    public static ActivityTypeSuggestionModel forEditEntry(
            long idWithinList,
            ActivityTypeSuggestionModel suggestionToEdit
    ) {
        Preconditions.checkArgument(idWithinList < 0);
        return new ActivityTypeSuggestionModel(
                idWithinList,
                suggestionToEdit.getActivityTypeId(),
                suggestionToEdit.getActivityName(),
                suggestionToEdit.getCategory(), Type.EDIT,
                null, 0, null, ImmutableList.<ActivityCustomFieldModel>of());
    }

    public static ActivityTypeSuggestionModel forNewEntryRequestingNewCategory(
            long idWithinList,
            String name,
            String newCategoryName,
            int newCategoryColor
    ) {
        Preconditions.checkArgument(idWithinList < 0);
        return new ActivityTypeSuggestionModel(
                idWithinList, null, name, null,
                Type.NEW_CREATE_CATEGORY,
                newCategoryName, newCategoryColor, null,
                ImmutableList.<ActivityCustomFieldModel>of());
    }

    public static ActivityTypeSuggestionModel forExisting(
            ActivityTypeModel activityTypeModel,
            ProjectModel projectModel,
            List<ActivityCustomFieldModel> customFieldModels
    ) {
        Preconditions.checkArgument(activityTypeModel.getId() >= 0);
        return new ActivityTypeSuggestionModel(
                activityTypeModel.getId(),
                activityTypeModel.getId(),
                activityTypeModel.getName(),
                activityTypeModel.getCategoryModel(),
                Type.EXISTING_DATA,
                null,
                0,
                projectModel,
                customFieldModels == null ? ImmutableList.<ActivityCustomFieldModel>of() : customFieldModels);
    }

    public Type getType() {
        return _type;
    }

    public String getNewCategoryName() {
        return _newCategoryName;
    }

    public int getNewCategoryColor() {
        return _newCategoryColor;
    }

    public long getId() {
        return _id;
    }

    public Long getActivityTypeId() {
        return _activityTypeId;
    }

    public String getActivityName() {
        return _activityName;
    }

    public CategoryModel getCategory() {
        return _categoryModel;
    }

    public ProjectModel getProject() {
        return _project;
    }

    public String getCategoryName(Context context) {
        return getCategoryName(context, this);
    }

    public List<ActivityCustomFieldModel> getCustomFieldModels() {
        return _customFieldModels;
    }

    public static String getCategoryName(Context context, ActivityTypeSuggestionModel model) {
        CategoryModel categoryModel = model == null ? null : model.getCategory();
        String name = categoryModel == null ? null : categoryModel.getName();
        if (!Strings.isNullOrEmpty(name)) {
            return name;
        }
        return context.getResources().getString(R.string.activity_category_default);
    }

    public String getProjectName(Context context) {
        return getProjectName(context, this);
    }

    public static String getProjectName(Context context, ActivityTypeSuggestionModel model) {
        return ActivityCategorySuggestionModel.getProjectName(context, model == null ? null : model.getProject());
    }

    public int getCategoryColor(Context context) {
        if (_type == Type.NEW_CREATE_CATEGORY) {
            return _newCategoryColor;
        }
        return getBackgroundColor(context, this);
    }

    public static int getBackgroundColor(Context context, ActivityTypeSuggestionModel model) {
        CategoryModel category = model == null ? null : model.getCategory();
        return ColorSuggestion.getCategoryColor(context.getResources(), category);
    }

}
