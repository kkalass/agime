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
public class TrackedActivityListDataAsyncLoader extends CompoundAsyncLoader<TrackedActivityListData> {
    private final long _rangeStarttimeMillisInclusive;
    private final long _rangeEndtimeMillisExclusive;
    private final TrackedActivitySyncLoader _loader;

    public TrackedActivityListDataAsyncLoader(
            Context context,
            long rangeStarttimeMillisInclusive,
            long rangeEndtimeMillisExclusive
    ) {
        super(context, ObserveDataSourceMode.RELOAD_ON_CHANGES);
        _loader = add(new TrackedActivitySyncLoader(context));
        _rangeStarttimeMillisInclusive = rangeStarttimeMillisInclusive;
        _rangeEndtimeMillisExclusive = rangeEndtimeMillisExclusive;
    }

    @Override
    public TrackedActivityListData doLoadInBackground() {
        List<TrackedActivityModel> result = _loader.query(_rangeStarttimeMillisInclusive, _rangeEndtimeMillisExclusive, true /*insert fakes*/);

        LocalDate date = new LocalDate(_rangeStarttimeMillisInclusive);
        LocalDate today = new LocalDate();
        if (!date.equals(today)) {
            return new TrackedActivityListData(result, date);
        }

        List<RecurringDAO.Data> recurringItems = loadList(
                RecurringDAO.READ_DATA, RecurringDAO.CONTENT_URI, RecurringDAO.PROJECTION
        );

        return new TrackedActivityListData(result, recurringItems, date);
    }

}
