package de.kalass.agime.acquisitiontime;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
* Created by klas on 23.12.13.
*/
public final class AcquisitionTimeInstance {
    private final List<RecurringDAO.Data> _items;
    public final LocalDate day;
    public final LocalTime startTime;
    public final LocalTime endTime;

    public static final int ACQUISITION_TIME_END_THRESHOLD_MINUTES = 120;

    AcquisitionTimeInstance(List<RecurringDAO.Data> items, LocalDate day, LocalTime startTime, LocalTime endTime) {
        _items = items;
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public DateTime getEndDateTime() {
        return day.toDateTime(endTime);
    }

    public DateTime getStartDateTime() {
        return day.toDateTime(startTime);
    }

    public List<RecurringDAO.Data> getItems() {
        return _items;
    }

    public List<RecurringDAO.Data> getActiveOnceItems() {
        return ImmutableList.copyOf(Iterables.filter(_items, RecurringDAO.Data.IS_ACTIVE_ONCE));
    }

    @CheckForNull
    public RecurringDAO.Data findRecurringItem() {
        for (RecurringDAO.Data d : _items) {
            if (!d.isActiveOnce()) {
                return d;
            }
        }
        return null;
    }
}
