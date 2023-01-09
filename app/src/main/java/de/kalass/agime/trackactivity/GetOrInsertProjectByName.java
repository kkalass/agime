package de.kalass.agime.trackactivity;

import android.content.ContentProviderOperation;
import android.content.Context;

import de.kalass.agime.provider.MCContract;

/**
* Created by klas on 21.01.14.
*/
final class GetOrInsertProjectByName extends GetOrInsertEntityByName {

    private final Integer _projectColor;

    GetOrInsertProjectByName(Context context, Integer projectColor) {
        super(context, MCContract.Project.CONTENT_URI, MCContract.Project.COLUMN_NAME_NAME);
        _projectColor = projectColor;
    }


    @Override
    protected ContentProviderOperation.Builder appendValues(ContentProviderOperation.Builder builder) {
        return builder.withValue(MCContract.Project.COLUMN_NAME_COLOR_CODE, _projectColor);
    }
}
