package de.kalass.agime.loader;

import android.content.Context;

import org.joda.time.LocalDate;

import java.util.List;

import de.kalass.agime.acquisitiontime.RecurringDAO;
import de.kalass.agime.model.TrackedActivityModel;
import de.kalass.android.common.simpleloader.CompoundAsyncLoader;
import de.kalass.android.common.simpleloader.ObserveDataSourceMode;

/**
 * A Loader that loads TrackedActivityModel instances asynchronously by combining a query to the trackpoints
 * table with a query to the symptoms table.
 * Created by klas on 22.10.13.
 *
 */
public class TrackedActivityListByIdsAsyncLoader extends CompoundAsyncLoader<List<TrackedActivityModel>> {
    private final long[] _ids;
    private final TrackedActivitySyncLoader _loader;

    public TrackedActivityListByIdsAsyncLoader(
            Context context,
            long[] ids
    ) {
        super(context, ObserveDataSourceMode.RELOAD_ON_CHANGES);
        _loader = add(new TrackedActivitySyncLoader(context));
        _ids = ids;
    }

    @Override
    public List<TrackedActivityModel> doLoadInBackground() {
        return _loader.getByIds(_ids);

    }

}
