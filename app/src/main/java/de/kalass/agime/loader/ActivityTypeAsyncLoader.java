package de.kalass.agime.loader;

import android.content.Context;

import java.util.List;

import de.kalass.agime.model.ActivityTypeModel;
import de.kalass.agime.provider.MCContract.ActivityType;
import de.kalass.android.common.simpleloader.DelegatingAsyncLoader;
import de.kalass.android.common.simpleloader.ObserveDataSourceMode;

/**
 * A Loader that loads TrackedActivityModel instances by combining a query to the trackpoints
 * table with a query to the symptoms table.
 * Created by klas on 22.10.13.
 */
public class ActivityTypeAsyncLoader extends DelegatingAsyncLoader<ActivityTypeSyncLoader, List<ActivityTypeModel>> {

    public ActivityTypeAsyncLoader(Context context) {
        super(context, ObserveDataSourceMode.RELOAD_ON_CHANGES, new ActivityTypeSyncLoader(context, new CategorySyncLoader(context)));
    }

    @Override
    public List<ActivityTypeModel> doLoadInBackground() {
        return getLoader().load(
                null, null,
                ActivityType.COLUMN_NAME_NAME + " asc");
    }


}
