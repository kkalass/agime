package de.kalass.agime.model;

import android.content.Context;

import com.google.common.base.Function;
import com.google.common.base.Strings;

import de.kalass.agime.R;
import de.kalass.android.common.model.IViewModel;

/**
 * View Model for activity data.
 * Created by klas on 21.10.13.
 */
public class ActivityCategorySuggestionModel implements IViewModel {
    public static Function<ActivityCategorySuggestionModel, String> GET_NAME = new Function<ActivityCategorySuggestionModel, String> () {
        @Override
        public String apply(ActivityCategorySuggestionModel categoryModel) {
            return categoryModel.getName();
        }
    };

    private final CategoryModel _category;

    public ActivityCategorySuggestionModel(
            CategoryModel category
    ) {
        _category = category;
    }


    public long getId() {
        return _category.getId();
    }

    public CategoryModel getCategory() {
        return _category;
    }

    public String getName() {
        return _category.getName();
    }

    public static String getProjectName(Context context, ProjectModel projectModel) {
        String name = projectModel == null ? null : projectModel.getName();
        if (!Strings.isNullOrEmpty(name)) {
            return name;
        }
        return context.getResources().getString(R.string.activity_project_default);
    }
}
