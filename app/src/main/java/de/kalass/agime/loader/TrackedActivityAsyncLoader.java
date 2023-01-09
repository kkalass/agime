package de.kalass.agime.loader;

import android.content.Context;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.kalass.agime.acquisitiontime.AcquisitionTimeInstance;
import de.kalass.agime.acquisitiontime.AcquisitionTimes;
import de.kalass.agime.acquisitiontime.RecurringDAO;
import de.kalass.agime.backup.BackupData;
import de.kalass.agime.model.TrackedActivityModel;
import de.kalass.agime.settings.Preferences;
import de.kalass.android.common.simpleloader.CompoundAsyncLoader;
import de.kalass.android.common.simpleloader.DelegatingAsyncLoader;
import de.kalass.android.common.simpleloader.HourMinute;
import de.kalass.android.common.simpleloader.ObserveDataSourceMode;
import de.kalass.android.common.util.DateUtil;
import de.kalass.android.common.util.TimeFormatUtil;

/**
 * A Loader that loads TrackedActivityModel instances asynchronously by combining a query to the trackpoints
 * table with a query to the symptoms table.
 * Created by klas on 22.10.13.
 *
 */
public class TrackedActivityAsyncLoader extends CompoundAsyncLoader< List<TrackedActivityModel>> {
    private final long _rangeStarttimeMillisInclusive;
    private final long _rangeEndtimeMillisExclusive;
    private final TrackedActivitySyncLoader _loader;

    public TrackedActivityAsyncLoader(
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
    public  List<TrackedActivityModel> doLoadInBackground() {
        return _loader.query(_rangeStarttimeMillisInclusive, _rangeEndtimeMillisExclusive, false /*no fakes*/);
    }

}
