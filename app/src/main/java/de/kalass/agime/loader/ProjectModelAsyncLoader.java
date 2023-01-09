package de.kalass.agime.loader;

import android.content.Context;

import java.util.List;

import de.kalass.agime.model.ProjectModel;
import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.simpleloader.DelegatingAsyncLoader;
import de.kalass.android.common.simpleloader.ObserveDataSourceMode;

/**
 * A Loader that loads ProjectModel instances.
 * Created by klas on 22.10.13.
 */
public class ProjectModelAsyncLoader extends DelegatingAsyncLoader<ProjectSyncLoader, List<ProjectModel>> {

    public ProjectModelAsyncLoader(Context context) {
        super(context, ObserveDataSourceMode.RELOAD_ON_CHANGES, new ProjectSyncLoader(context));
    }

    @Override
    public List<ProjectModel> doLoadInBackground() {
        return getLoader().load(null, null, MCContract.Project.COLUMN_NAME_NAME + " asc");
    }


}
