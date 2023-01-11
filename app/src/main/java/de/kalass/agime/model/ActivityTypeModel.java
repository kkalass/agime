package de.kalass.agime.model;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import de.kalass.android.common.model.IViewModel;

/**
 * View Model for activity data.
 * Created by klas on 21.10.13.
 */
public class ActivityTypeModel implements IViewModel {
    public static final Function<ActivityTypeModel, CategoryModel> GET_CATEGORY = new Function<ActivityTypeModel, CategoryModel>() {
        @Override
        public CategoryModel apply(ActivityTypeModel input) {
            return input == null ? null : input._categoryModel;
        }
    };

    public static final Function<ActivityTypeModel, Long> GET_CATEGORY_ID = Functions.compose(CategoryModel.GET_ID, GET_CATEGORY);


    private final long _id;
    private final String _activityName;
    private final CategoryModel _categoryModel;

    public ActivityTypeModel(
            long id,
            String activityName,
            CategoryModel categoryModel
    ) {
        _id = id;
        _activityName = activityName;
        _categoryModel = categoryModel;
    }

    public long getId() {
        return _id;
    }

    public String getName() {
        return _activityName;
    }

    public CategoryModel getCategoryModel() {
        return _categoryModel;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .addValue(_id)
                .addValue(_activityName)
                .toString();
    }
}
