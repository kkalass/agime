package de.kalass.agime.loader;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.google.common.base.Function;

import java.util.Map;

import de.kalass.agime.model.ActivityTypeModel;
import de.kalass.agime.model.CategoryModel;
import de.kalass.agime.provider.MCContract.ActivityType;

import static de.kalass.android.common.simpleloader.CursorUtil.getIndex;
import static de.kalass.android.common.simpleloader.CursorUtil.getPrefetched;

/**
 * A Loader that loads TrackedActivityModel instances by combining a query to the trackpoints
 * table with a query to the symptoms table.
 * Created by klas on 22.10.13.
 */
public class ActivityTypeSyncLoader extends SimpleSyncLoader<ActivityTypeModel> {

    private final CategorySyncLoader _categoryLoader;

    public static final class ActivityTypeQuery {
        public static final Uri URI = ActivityType.CONTENT_URI;
        static final String[] PROJECTION = new String[]{
                ActivityType._ID,
                ActivityType.COLUMN_NAME_NAME,
                ActivityType.COLUMN_NAME_ACTIVITY_CATEGORY_ID
        };
        static final int COLUMN_IDX_ID = getIndex(PROJECTION, ActivityType._ID);
        static final int COLUMN_IDX_NAME = getIndex(PROJECTION, ActivityType.COLUMN_NAME_NAME);
        static final int COLUMN_IDX_CATEGORY_ID = getIndex(PROJECTION, ActivityType.COLUMN_NAME_ACTIVITY_CATEGORY_ID);

    }


    public ActivityTypeSyncLoader(Context context, CategorySyncLoader categoryLoader) {
        super(context,
                ActivityTypeQuery.URI,
                ActivityTypeQuery.PROJECTION,
                categoryLoader
        );
        _categoryLoader = categoryLoader;
    }

    @Override
    protected Function<Cursor, ActivityTypeModel> getReaderFunction(Cursor originalCursor) {

        final Map<Long, CategoryModel> categories =  _categoryLoader.loadAllFromCursor(
                originalCursor, ActivityTypeQuery.COLUMN_IDX_CATEGORY_ID
        );

        return new Function<Cursor, ActivityTypeModel> () {
            @Override
            public ActivityTypeModel apply(Cursor cursor) {
                final long activityTypeId = cursor.getLong(ActivityTypeQuery.COLUMN_IDX_ID);
                final String activityName = cursor.getString(ActivityTypeQuery.COLUMN_IDX_NAME);
                final CategoryModel category = getPrefetched(
                        cursor, ActivityTypeQuery.COLUMN_IDX_CATEGORY_ID, categories
                );
                return new ActivityTypeModel(activityTypeId, activityName, category);
            }
        };
    }
}
