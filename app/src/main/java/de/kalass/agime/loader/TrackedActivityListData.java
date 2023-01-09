package de.kalass.agime.loader;

import com.google.common.collect.ImmutableList;

import org.joda.time.LocalDate;

import java.util.List;

import de.kalass.agime.acquisitiontime.RecurringDAO;
import de.kalass.agime.model.TrackedActivityModel;

/**
 * Created by klas on 30.01.14.
 */
public class TrackedActivityListData {
    private final List<TrackedActivityModel> trackedActivityModels;
    private final List<RecurringDAO.Data> recurringAcquisitionConfigurationData;
    private final LocalDate date;
    private final boolean isToday;

    public TrackedActivityListData(List<TrackedActivityModel> trackedActivityModels, LocalDate date) {
        this(trackedActivityModels, ImmutableList.<RecurringDAO.Data>of(), date, false);
    }

    public TrackedActivityListData(
            List<TrackedActivityModel> trackedActivityModels,
            List<RecurringDAO.Data> recurringAcquisitionConfigurationData,
            LocalDate date
    ) {
        this(trackedActivityModels, recurringAcquisitionConfigurationData, date, true);
    }

    private TrackedActivityListData(
            List<TrackedActivityModel> trackedActivityModels,
            List<RecurringDAO.Data> recurringAcquisitionConfigurationData,
            LocalDate date,
            boolean isToday
    ) {
        this.trackedActivityModels = trackedActivityModels;
        this.recurringAcquisitionConfigurationData = recurringAcquisitionConfigurationData;
        this.date = date;
        this.isToday = isToday;
    }

    public List<TrackedActivityModel> getTrackedActivityModels() {
        return trackedActivityModels;
    }

    public List<RecurringDAO.Data> getRecurringAcquisitionConfigurationData() {
        return recurringAcquisitionConfigurationData;
    }

    public LocalDate getDate() {
        return date;
    }

    public boolean isToday() {
        return isToday;
    }
}
