package de.kalass.agime.timesuggestions;

import android.content.Context;

import com.google.common.collect.Ordering;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.util.ArrayList;
import java.util.List;

import de.kalass.agime.acquisitiontime.AcquisitionTimeInstance;
import de.kalass.agime.acquisitiontime.AcquisitionTimes;
import de.kalass.agime.acquisitiontime.RecurringDAO;
import de.kalass.agime.loader.TrackedActivitySyncLoader;
import de.kalass.agime.model.ActivityTypeModel;
import de.kalass.agime.model.TrackedActivityModel;
import de.kalass.android.common.simpleloader.CompoundSyncLoader;
import de.kalass.android.common.util.DateUtil;

/**
 * Created by klas on 19.11.13.
 */
public class TimeSuggestionSyncLoader extends CompoundSyncLoader {

    private final TrackedActivitySyncLoader _trackedActivityLoader;

    public TimeSuggestionSyncLoader(Context context) {
        this(context, new TrackedActivitySyncLoader(context));
    }

    public TimeSuggestionSyncLoader(Context context, TrackedActivitySyncLoader loader) {
        super(context, loader);
        _trackedActivityLoader = loader;
    }

    public TimeSuggestions load(LocalDate date) {
        List<RecurringDAO.Data> recurringItems = loadList(
                RecurringDAO.READ_DATA, RecurringDAO.CONTENT_URI, RecurringDAO.PROJECTION,
                RecurringDAO.selection(), RecurringDAO.selectionArgs(), null
        );
        return load(date, recurringItems);
    }

    public TimeSuggestions load(LocalDate date, List<RecurringDAO.Data> recurringItems) {

        List<TimeSuggestion> suggestionList = new ArrayList<TimeSuggestion>();

        List<TrackedActivityModel> activities = _trackedActivityLoader.query(
                DateUtil.getMillisAtStartOfDay(date),
                DateUtil.getMillisAtEndOfDay(date),
                false /*no fake entries*/
        );


        AcquisitionTimes times = AcquisitionTimes.fromRecurring(recurringItems, date.toDateTimeAtCurrentTime());

        LocalTime workingDayStart = getWorkingDayStart(times, date);
        if (workingDayStart != null) {
            suggestionList.add(TimeSuggestion.workingdayStart(workingDayStart));
        }
        for (TrackedActivityModel model : activities) {
            if (!model.isFakeEntry()) {
                ActivityTypeModel activityType = model.getActivityType();
                String name = activityType == null ? null : activityType.getName();

                suggestionList.add(TimeSuggestion.activityBegin(
                        model.getId(),
                        name, model.getStarttimeDateTimeMinutes().toLocalTime()));
                suggestionList.add(TimeSuggestion.activityEnd(
                        model.getId(),
                        name, model.getEndtimeDateTimeMinutes().toLocalTime()));
            }
        }
        suggestionList.add(TimeSuggestion.dayStart());
        suggestionList.add(TimeSuggestion.dayEnd());
        return new TimeSuggestions(date, TimeSuggestions.sortedCopy(suggestionList));
    }

    private LocalTime getWorkingDayStart(AcquisitionTimes times, LocalDate today) {
        AcquisitionTimeInstance current = times.getCurrent();
        AcquisitionTimeInstance next = times.getNext();
        if (current != null && today.equals(current.day)) {
            return current.startTime;
        }
        if (current == null && next != null && today.equals(next.day)) {
            return next.startTime;
        }
        return null;
    }
}
