package de.kalass.agime.trackactivity.actionview;

import com.google.common.base.Preconditions;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

import de.kalass.agime.acquisitiontime.AcquisitionTimeInstance;
import de.kalass.agime.acquisitiontime.AcquisitionTimes;
import de.kalass.agime.acquisitiontime.RecurringDAO;
import de.kalass.agime.loader.TrackedActivityListData;
import de.kalass.agime.model.TrackedActivityModel;
import de.kalass.android.common.util.DateUtil;

/**
 * Created by klas on 14.02.14.
 */
public class ActionItemFactory {

    public static ActionItem createActionItemData(TrackedActivityListData data, LocalDate day) {
        final LocalDate date = data == null ? null : data.getDate();
        if (data == null || !data.isToday()) {
            // not today
            final List<TrackedActivityModel> result = data == null ? null : data.getTrackedActivityModels();
            if (result != null && result.size() > 0) {
                TrackedActivityModel first = result.get(0);
                TrackedActivityModel last = result.get(result.size() - 1);
                long totalEntryDurationMillis = calculateTotalEntryDurationMillis(result);
                if (totalEntryDurationMillis > 0) {
                    return createEndOfDayEntry(ActionItem.Mode.END_OF_DAY_PAST,
                            totalEntryDurationMillis,
                            first.getStartTimeMillis(), last.getEndTimeMillis());
                } else {
                    // migration: entries without duration tracked
                    return null;
                }
            } else {
                long now = System.currentTimeMillis();
                return new ActionItem(ActionItem.Mode.NOTHING, now, now, null, null, null);
            }

        }
        // "today": add a fake model as UI element for quick interaction
        final List<TrackedActivityModel> result = data.getTrackedActivityModels();
        final List<RecurringDAO.Data> recurringItems = data.getRecurringAcquisitionConfigurationData();
        final DateTime now = DateUtil.trimMinutePrecision(date.toDateTimeAtCurrentTime());
        AcquisitionTimes times = AcquisitionTimes.fromRecurring(recurringItems, now);

        // first entry of day: create a default entry starting when the current acquisition time
        // started, or "now" if there is no current acquisition time
        if (result.isEmpty()) {
            final AcquisitionTimeInstance current = times.getCurrent();
            if (current != null) {
                return createActionItem(ActionItem.Mode.START_OF_DAY, current, current);
            } else  {
                AcquisitionTimes startOfDayTimes = AcquisitionTimes.fromRecurring(recurringItems, date.toDateTimeAtStartOfDay());
                final AcquisitionTimeInstance acquisitionTimeInstance = startOfDayTimes.hasCurrent() ? startOfDayTimes.getCurrent() : startOfDayTimes.getNext();
                if (acquisitionTimeInstance != null && date.equals(acquisitionTimeInstance.day) && acquisitionTimeInstance.getStartDateTime().isBefore(now)) {

                    return createActionItem(ActionItem.Mode.START_OF_DAY_ACQUISITION_TIME_PASSED, acquisitionTimeInstance, null);
                } else {
                    // there was no entry today - there might be one later in the day, or none at all.
                    // use a default entry, but remember the first start of the day.
                    Long startTimeMillis =  DateUtil.getNowMinutePrecisionMillis();
                    Preconditions.checkNotNull(startTimeMillis);
                    return createActionItem(ActionItem.Mode.START_OF_DAY_NO_ACQUISITION_TIME,
                            startTimeMillis, DateUtil.getNowMinutePrecisionMillis(), null, null, null);
                }
            }
        }

        List<TrackedActivityModel> r = new ArrayList<TrackedActivityModel>(result.size() + 1);
        r.addAll(result);
        TrackedActivityModel last = result.get(result.size() - 1);
        final AcquisitionTimeInstance current = times.getCurrent();
        final AcquisitionTimeInstance next = times.getNext();
        final long startTimeMillis = last.getEndTimeMillis();
        final long endTimeMillis = getEndTimeMillis(current);
        if (current != null && current.getEndDateTime().getMillis() > DateUtil.getNowMinutePrecisionMillis()) {
            return createActionItem(ActionItem.Mode.NEW_ENTRY, startTimeMillis, endTimeMillis, current, last, null);
        } else if (next != null && day.equals(next.day)) {
            // there will be an entry later today
            return createActionItem(
                    ActionItem.Mode.NEW_ENTRY_BEFORE_ACQUISITION_TIME, startTimeMillis, endTimeMillis,
                    null, last, null
            );
        }
        long totalEntryDurationMillis = calculateTotalEntryDurationMillis(result);
        TrackedActivityModel first = result.get(0);
        return createActionItem(
                ActionItem.Mode.END_OF_DAY, first.getStartTimeMillis(), last.getEndTimeMillis(),
                times.getPrevious(),
                last /*the previous item*/, totalEntryDurationMillis);

    }

    private static ActionItem createEndOfDayEntry(ActionItem.Mode mode, long durationMillis, long startTimeMillis, long endTimeMillis) {
        Preconditions.checkArgument(mode == ActionItem.Mode.END_OF_DAY || mode == ActionItem.Mode.END_OF_DAY_PAST);
        // last entry of the day

        return createActionItem(mode, startTimeMillis, endTimeMillis, null /*acquisition time has passed*/,
                null /*no real item connected*/, durationMillis);
    }

    private static long calculateTotalEntryDurationMillis(List<TrackedActivityModel> result) {
        long totalEntryDurationMillis = 0;
        for (TrackedActivityModel m : result) {
            totalEntryDurationMillis += m.getEntryDurationMillis();
        }
        return totalEntryDurationMillis;
    }


    private static ActionItem createActionItem(ActionItem.Mode mode, AcquisitionTimeInstance acquisitionTimeInstance,
                                        AcquisitionTimeInstance current) {
        final long endTimeMillis = getEndTimeMillis(acquisitionTimeInstance);
        return createActionItem(mode, acquisitionTimeInstance.getStartDateTime().getMillis(), endTimeMillis,
                current, null, null);
    }

    private static long getEndTimeMillis(AcquisitionTimeInstance current) {

        final long nowMillis = DateUtil.getNowMinutePrecisionMillis();
        if (current == null) {
            return nowMillis;
        }
        final long endMillis = current.getEndDateTime().getMillis();
        return nowMillis < endMillis ? nowMillis : endMillis;
    }

    private static ActionItem createActionItem(ActionItem.Mode mode, long startTimeMillis, long endTimeMillis,
                                        AcquisitionTimeInstance currentAcquisitionTime,
                                        TrackedActivityModel previous,
                                        Long totalDayEntryDurationMillis) {
        return new ActionItem(mode, startTimeMillis, endTimeMillis, currentAcquisitionTime, previous, totalDayEntryDurationMillis);
    }

}
