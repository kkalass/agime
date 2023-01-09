package de.kalass.agime.loader;

import android.content.Context;

import java.util.List;

import de.kalass.agime.model.CategoryModel;
import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.simpleloader.DelegatingAsyncLoader;
import de.kalass.android.common.simpleloader.ObserveDataSourceMode;

/**
 * A Loader that loads CategoryModel instances.
 * Created by klas on 22.10.13.
 */
public class CategoryModelAsyncLoader extends DelegatingAsyncLoader<CategorySyncLoader, List<CategoryModel>> {

    public CategoryModelAsyncLoader(Context context) {
        super(context, ObserveDataSourceMode.RELOAD_ON_CHANGES, new CategorySyncLoader(context));
    }

    @Override
    public List<CategoryModel> doLoadInBackground() {
        return getLoader().load(null, null, MCContract.Category.COLUMN_NAME_NAME + " asc");
    }
}
