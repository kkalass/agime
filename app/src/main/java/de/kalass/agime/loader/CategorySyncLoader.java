package de.kalass.agime.loader;

import android.content.Context;
import android.database.Cursor;

import com.google.common.base.Function;

import java.util.List;

import de.kalass.agime.model.CategoryModel;

/**
 * A Loader that loads TrackedActivityModel instances by combining a query to the trackpoints
 * table with a query to the symptoms table.
 * Created by klas on 22.10.13.
 */
public class CategorySyncLoader extends SimpleSyncLoader<CategoryModel> {

    public CategorySyncLoader(Context context) {
        super(context, CategoryModelQuery.URI, CategoryModelQuery.PROJECTION);
    }

    @Override
    public Function<Cursor, CategoryModel> getReaderFunction(Cursor originalCursor) {
        return CategoryModelQuery.READER;
    }

    protected List<CategoryModel> loadByName(CharSequence name) {
        return loadByName(name,
                CategoryModelQuery.COLUMN_NAME_NAME,
                CategoryModelQuery.COLUMN_IDX_NAME,
                CategoryModelQuery.ID_READER);
    }
}
