package de.kalass.agime.trackactivity;

import android.content.ContentProviderOperation;
import android.content.Context;

import de.kalass.agime.provider.MCContract;

/**
* Created by klas on 21.01.14.
*/
public final class GetOrInsertCategoryByName extends GetOrInsertEntityByName {

    private final Integer _categoryColor;

    public GetOrInsertCategoryByName(Context context, Integer categoryColor) {
        super(context, MCContract.Category.CONTENT_URI, MCContract.Category.COLUMN_NAME_NAME);
        _categoryColor = categoryColor;
    }


    @Override
    protected ContentProviderOperation.Builder appendValues(ContentProviderOperation.Builder builder) {
        return builder.withValue(MCContract.Category.COLUMN_NAME_COLOR_CODE, _categoryColor);
    }
}
