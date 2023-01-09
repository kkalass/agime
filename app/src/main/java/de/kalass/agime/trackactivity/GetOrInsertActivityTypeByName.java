package de.kalass.agime.trackactivity;

import android.content.ContentProviderOperation;
import android.content.Context;

import com.google.common.base.Preconditions;

import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.simpleloader.ValueOrReference;

/**
* Created by klas on 21.01.14.
*/
final class GetOrInsertActivityTypeByName extends GetOrInsertEntityByName {

    private final ValueOrReference _categoryId;

    GetOrInsertActivityTypeByName(Context context, ValueOrReference categoryId) {
        super(context, MCContract.ActivityType.CONTENT_URI, MCContract.ActivityType.COLUMN_NAME_NAME);
        _categoryId = Preconditions.checkNotNull(categoryId);
    }

    @Override
    protected ContentProviderOperation.Builder appendValues(ContentProviderOperation.Builder builder) {
        return _categoryId.appendSelf(builder, MCContract.ActivityType.COLUMN_NAME_ACTIVITY_CATEGORY_ID);
    }
}
