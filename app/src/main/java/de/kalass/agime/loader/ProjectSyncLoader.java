package de.kalass.agime.loader;

import android.content.Context;
import android.database.Cursor;

import com.google.common.base.Function;

import de.kalass.agime.model.ProjectModel;

/**
 * A Loader that loads TrackedActivityModel instances by combining a query to the trackpoints
 * table with a query to the symptoms table.
 * Created by klas on 22.10.13.
 */
public class ProjectSyncLoader extends SimpleSyncLoader<ProjectModel> {

    public ProjectSyncLoader(Context context) {
        super(context, ProjectModelQuery.URI, ProjectModelQuery.PROJECTION);
    }

    @Override
    protected Function<Cursor, ProjectModel> getReaderFunction(Cursor cursor) {
        return ProjectModelQuery.READER;
    }
}
