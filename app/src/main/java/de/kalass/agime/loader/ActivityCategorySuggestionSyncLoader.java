package de.kalass.agime.loader;

import android.content.Context;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import java.util.List;

import de.kalass.agime.model.ActivityCategorySuggestionModel;
import de.kalass.agime.model.CategoryModel;
import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.activity.ContentResolverUtil;
import de.kalass.android.common.simpleloader.CompoundSyncLoader;
import de.kalass.android.common.util.StringUtil;

/**
 *
 * Created by klas on 22.10.13.
 */
public class ActivityCategorySuggestionSyncLoader extends CompoundSyncLoader {

    public static final Function<CategoryModel,ActivityCategorySuggestionModel> CONVERTER = new Function<CategoryModel, ActivityCategorySuggestionModel>() {
        @Override
        public ActivityCategorySuggestionModel apply(CategoryModel categoryModel) {
            return new ActivityCategorySuggestionModel(
                    categoryModel
            );
        }
    };
    private final CategorySyncLoader _categoryLoader;


    public ActivityCategorySuggestionSyncLoader(Context context) {
        this(context, new CategorySyncLoader(context));
    }

    public ActivityCategorySuggestionSyncLoader(Context context, CategorySyncLoader categoryLoader) {
        super(context, new CategorySyncLoader(context));
        this._categoryLoader = categoryLoader;
    }


    public List<ActivityCategorySuggestionModel> loadByName(
            CharSequence name
    ) {

        final List<CategoryModel> categoryModels = _categoryLoader.loadByName(name);
        final Iterable<ActivityCategorySuggestionModel> suggestionModels = Iterables.transform(
                categoryModels, CONVERTER
        );
        return ImmutableList.copyOf(suggestionModels);
    }

}
