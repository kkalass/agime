package de.kalass.agime.timesuggestions;

import android.view.textservice.SuggestionsInfo;
import android.widget.SpinnerAdapter;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Created by klas on 19.11.13.
 */
public class TimeSuggestions {
    private final List<TimeSuggestion> _suggestions;
    private final LocalDate _date;


    public TimeSuggestions(LocalDate date, List<TimeSuggestion> suggestions) {
        _date = date;
        _suggestions = ImmutableList.copyOf(suggestions);
    }

    public static List<TimeSuggestion> sortedCopy(List<TimeSuggestion> suggestions) {
        return Ordering.natural()
                .nullsLast()
                .onResultOf(TimeSuggestion.GET_TIME_MINUTE_PRECISION)
                .sortedCopy(suggestions);
    }

    private void checkIsValidTime(LocalTime timeMinutePrecision) {
        Preconditions.checkArgument(timeMinutePrecision.getSecondOfMinute() == 0);
        Preconditions.checkArgument(timeMinutePrecision.getMillisOfSecond() == 0);
    }


    private static Predicate<TimeSuggestion> isAtTime(final LocalTime timeMinutePrecision) {
        return new Predicate<TimeSuggestion>() {
            @Override
            public boolean apply(TimeSuggestion input) {
                return Objects.equal(input.getTimeMinutePrecision(), timeMinutePrecision);
            }
        };
    }

    private static Predicate<TimeSuggestion> isAfterTime(final LocalTime timeMinutePrecision) {
        return new Predicate<TimeSuggestion>() {
            @Override
            public boolean apply(TimeSuggestion input) {
                LocalTime suggestedTime = input.getTimeMinutePrecision();
                if (suggestedTime == null) {
                    return false;
                }
                return suggestedTime.isAfter(timeMinutePrecision);
            }
        };
    }

    private static Predicate<TimeSuggestion> isBeforeTime(final LocalTime timeMinutePrecision) {
        return new Predicate<TimeSuggestion>() {
            @Override
            public boolean apply(TimeSuggestion input) {
                LocalTime suggestedTime = input.getTimeMinutePrecision();
                if (suggestedTime == null) {
                    return false;
                }
                return suggestedTime.isBefore(timeMinutePrecision);
            }
        };
    }
    public static Predicate<TimeSuggestion> suggestionOfType(final TimeSuggestion.Type type1, final TimeSuggestion.Type... types) {
        return anyOf(EnumSet.of(type1, types));
    }

    public static Predicate<TimeSuggestion> anyOf(final Set<TimeSuggestion.Type> types) {
        return new Predicate<TimeSuggestion>() {
            @Override
            public boolean apply(TimeSuggestion input) {
                return types.contains(input.getType());
            }
        };
    }

    public static Predicate<TimeSuggestion> noneOf(final TimeSuggestion.Type type1, final TimeSuggestion.Type... types) {
        return noneOf(EnumSet.of(type1, types));
    }

    public static Predicate<TimeSuggestion> noneOf(final Set<TimeSuggestion.Type> types) {
        return Predicates.not(anyOf(types));
    }

    private int getLastIdx(
            List<TimeSuggestion> suggestions,
            Predicate<TimeSuggestion> predicate
    ) {
        for (int i = (suggestions.size() - 1); i >= 0; i--) {
            TimeSuggestion suggestion = suggestions.get(i);
            if (predicate.apply(suggestion)) {
                return i;
            }
        }
        return -1;
    }

    private int getFirstIdx(
            List<TimeSuggestion> suggestions,
            Predicate<TimeSuggestion> predicate
    ) {
        for (int i = 0; i < suggestions.size(); i++) {
            TimeSuggestion suggestion = suggestions.get(i);
            if (predicate.apply(suggestion)) {
                return i;
            }
        }
        return -1;
    }

    public enum Mode {
        START_TIME,
        END_TIME
    }

    public List<TimeSuggestion> getSuggestions(Mode mode, long itemId, LocalTime startTime, LocalTime endTime) {
        if (mode == Mode.START_TIME) {
            return getStarttimeSuggestions(itemId, startTime, _suggestions);
        }
        return getEndtimeSuggestions(itemId, startTime, endTime, _suggestions);
    }

    boolean isUniqueTime(List<TimeSuggestion> suggestions, int idx) {
        // assume time-sorted list
        TimeSuggestion item = suggestions.get(idx);
        TimeSuggestion prev = idx > 0 ? suggestions.get(idx - 1) : null;
        TimeSuggestion next = (idx + 1) < suggestions.size() ? suggestions.get(idx + 1) : null;
        LocalTime prevTime = prev == null ? null : prev.getTimeMinutePrecision();
        LocalTime nextTime = next == null ? null : next.getTimeMinutePrecision();
        return !Objects.equal(item.getTimeMinutePrecision(), prevTime)
                && !Objects.equal(item.getTimeMinutePrecision(), nextTime);
    }

    private List<TimeSuggestion> getStarttimeSuggestions(
            long itemId,
            LocalTime startTime,
            List<TimeSuggestion> suggestions
    ) {
        int oldStartTimeIdx = getActivityStartTimeIdx(itemId, suggestions);

        int oldEndTimeIdx = getActivityEndTimeIdx(itemId, suggestions);

        int prevIdx;
        if (oldStartTimeIdx >= 0) {

            prevIdx = getLastIdx(suggestions.subList(0, oldStartTimeIdx + 1),
                suggestionOfType(TimeSuggestion.Type.ACTIVITY_END, TimeSuggestion.Type.WORKINGDAY_START, TimeSuggestion.Type.DAY_START));
        } else {
            prevIdx = getLastIdx(suggestions, isAtTime(startTime));
            if (prevIdx < 0) {
                // there is no old entry for the tracked activity, and the default starttime
                // does not correspond to an existing suggestion (for example 'Workingday start')
                // => start with the very first item of the day (e. g. "start of day)
                prevIdx = getFirstIdx(suggestions, Predicates.<TimeSuggestion>alwaysTrue());
                // old logic: use the first item after the suggested start time - but what was that good for?
                //prevIdx = getFirstIdx(suggestions, isAfterTime(startTime));
            }

        }
        int maxSuggestionIndex = oldEndTimeIdx > 0 ? oldEndTimeIdx :getLastEndtimeSuggestionIdx(itemId, prevIdx, suggestions);

        ImmutableList.Builder<TimeSuggestion> builder = ImmutableList.builder();
        //
        for (int i = Math.max(0, prevIdx); i <= maxSuggestionIndex; i++) {
            TimeSuggestion suggestion = suggestions.get(i);
            if (suggestion.getType() == TimeSuggestion.Type.DAY_END ||Objects.equal(itemId, suggestion.getActivityItemId()) && !isUniqueTime(suggestions, i)) {
                // skip old value if the time it represents is represented by any other event
            } else {
                builder.add(suggestion);
            }
        }
        /*
        TimeSuggestion oldValue = oldStartTimeIdx < 0 ? null : suggestions.get(oldStartTimeIdx);
        if (oldValue != null) {
            builder.add(TimeSuggestion.anyTime(oldValue.getTimeMinutePrecision()));
        } else {
            builder.add(TimeSuggestion.anyTime());
        }
        */
        int startTimeIdx = getFirstIdx(suggestions, isAfterTime(startTime));
        int lastEndtimeSuggestionIdx = getLastEndtimeSuggestionIdx(itemId, startTimeIdx, suggestions);
        if (isToday() && (lastEndtimeSuggestionIdx < 0 || !Iterables.tryFind(suggestions.subList(lastEndtimeSuggestionIdx, suggestions.size()), suggestionOfType(TimeSuggestion.Type.ACTIVITY_BEGIN)).isPresent())) {
            // "now" should only be added if the entry is going to be added
            // for the current day, and there is no other activity starting at or after the suggested endtime
            builder.add(TimeSuggestion.now());
        }

        builder.add(TimeSuggestion.anyTime(startTime));
        return sortedCopy(builder.build());
    }

    public static int getSuggestionsIndex(LocalTime endTime, List<TimeSuggestion> suggestions) {
        for (int i = 0; i < suggestions.size(); i++) {
            if (Objects.equal(endTime, suggestions.get(i).getTimeMinutePrecision())) {
                return i;
            }
        }
        return -1;
    }

    public static int getSuggestionsIndex(LocalTime endTime, SpinnerAdapter suggestions) {
        for (int i = 0; i < suggestions.getCount(); i++) {
            final TimeSuggestion item = (TimeSuggestion)suggestions.getItem(i);
            if (Objects.equal(endTime, item.getTimeMinutePrecision())) {
                return i;
            }
        }
        return -1;
    }

    private int getActivityEndTimeIdx(long itemId, List<TimeSuggestion> suggestions) {
        return getFirstIdx(suggestions, isActivityItemOfType(itemId, TimeSuggestion.Type.ACTIVITY_END));
    }

    private int getActivityStartTimeIdx(long itemId, List<TimeSuggestion> suggestions) {
        return getFirstIdx(suggestions, isActivityItemOfType(itemId, TimeSuggestion.Type.ACTIVITY_BEGIN));
    }

    private Predicate<TimeSuggestion> isActivityItemOfType(long itemId, TimeSuggestion.Type type) {
        return Predicates.and(isActivityItem(itemId), suggestionOfType(type));
    }

    private Predicate<TimeSuggestion> isActivityItem(long itemId) {
        return Predicates.compose(Predicates.equalTo(itemId), TimeSuggestion.GET_ACTIVITY_ITEM_ID);
    }


    private List<TimeSuggestion> getEndtimeSuggestions(
            long itemId, LocalTime starttime, LocalTime endTime, List<TimeSuggestion> suggestions
    ) {

        int startTimeIdx = getFirstIdx(suggestions, isAfterTime(starttime));
        int lastEndtimeSuggestionIdx = getLastEndtimeSuggestionIdx(itemId, startTimeIdx, suggestions);
        //DateTime defaultEndTime = getEndtimeDefault(itemId);
        ImmutableList.Builder<TimeSuggestion> builder = ImmutableList.builder();
        if (startTimeIdx >= 0 ) {
            // nice: there actually are entries after the starttime, lets test
            // if they can be used as suggestions.

            int lastIdx = lastEndtimeSuggestionIdx < 0 ? (suggestions.size() - 1) : lastEndtimeSuggestionIdx;
            for (int i = startTimeIdx; i <= lastIdx; i++) {
                TimeSuggestion suggestion = suggestions.get(i);
                if (Objects.equal(itemId, suggestion.getActivityItemId()) && !isUniqueTime(suggestions, i)) {
                    // skip old value if the time it represents is represented by any other event
                } else {
                    builder.add(suggestion);
                }
            }
        }

        if (isToday() && (lastEndtimeSuggestionIdx < 0 || !Iterables.tryFind(suggestions.subList(lastEndtimeSuggestionIdx, suggestions.size()), suggestionOfType(TimeSuggestion.Type.ACTIVITY_BEGIN)).isPresent())) {
            // "now" should only be added if the entry is going to be added
            // for the current day, and there is no other activity starting at or after the suggested endtime
            builder.add(TimeSuggestion.now());
        }

        /*
        if (defaultEndTime != null) {
            builder.add(TimeSuggestion.anyTime(defaultEndTime.toLocalTime()));
        } else {
            builder.add(TimeSuggestion.anyTime());
        }*/
        builder.add(TimeSuggestion.anyTime(endTime));
        return sortedCopy(builder.build());
    }

    private boolean isToday() {
        return _date.equals(new LocalDate());
    }

    private DateTime getActivityTime(List<TimeSuggestion> suggestions, int idx) {
        if (idx >= 0) {
            TimeSuggestion suggestion = Preconditions.checkNotNull(
                    suggestions.get(idx),
                    "Persisted activity not known to suggestion. Is it from a different day?");
            LocalTime time = Preconditions.checkNotNull(
                    suggestion.getTimeMinutePrecision(),
                    "No time set"
            );
            return _date.toDateTime(time);
        }
        return null;
    }

    public DateTime getStarttimeDefault(long id) {
        DateTime result = getActivityTime(_suggestions, getActivityStartTimeIdx(id, _suggestions));
        if (result != null) {
            return result;
        }
        TimeSuggestion lastItem = getLastActivityItem();
        if (lastItem != null) {
            // default: suggest to line up with previous activity.
            return _date.toDateTime(lastItem.getTimeMinutePrecision());
        }
        // new activity and first activity of the day: default to workingday start entry
        TimeSuggestion workingdayStart = Iterables.find(_suggestions, suggestionOfType(TimeSuggestion.Type.WORKINGDAY_START), null);
        if (workingdayStart != null) {
            DateTime startTime = _date.toDateTime(workingdayStart.getTimeMinutePrecision());
            if (startTime.isBeforeNow()) {
                return startTime;
            }
        }
        // either there is no workingday start configured, or it is still before the working day
        // starts... there really is no good suggestion possible yet, so just use "dayWithCurrentTime".
        return dayWithCurrentTime();
    }

    public DateTime getEndtimeDefault(long id) {
        DateTime result = getActivityTime(_suggestions, getActivityEndTimeIdx(id, _suggestions));
        if (result != null) {
            return result;
        }
        DateTime starttime = getStarttimeDefault(id);
        DateTime currentTime = dayWithCurrentTime();
        if (starttime.isAfter(currentTime)) {
            // could happen if the _day is not "today" and the current time is earlier
            // than applicable time suggestions.
            return starttime;
        }
        return currentTime;
    }

    private DateTime dayWithCurrentTime() {
        return _date.toDateTimeAtCurrentTime().withMillisOfSecond(0).withSecondOfMinute(0);
    }

    private TimeSuggestion getLastActivityItem() {
        return Iterables.getLast(Iterables.filter(_suggestions, suggestionOfType(TimeSuggestion.Type.ACTIVITY_BEGIN, TimeSuggestion.Type.ACTIVITY_END)), null);
    }


    private Predicate<TimeSuggestion> isBeforeOrAtNow() {
        return new Predicate<TimeSuggestion>() {
            @Override
            public boolean apply(TimeSuggestion input) {
                final LocalTime time = input == null ? null : input.getTimeMinutePrecision();
                if (time == null) {
                    // special handling for null
                    return true;
                }
                return !_date.toDateTime(time).isAfterNow();
            }
        };
    }

    private int getLastEndtimeSuggestionIdx(long itemId, int firstSuggestionIdx, List<TimeSuggestion> suggestions) {
        int oldEndTimeIdx = getActivityEndTimeIdx(itemId, suggestions);
        int startIdx = oldEndTimeIdx < 0 ? firstSuggestionIdx : oldEndTimeIdx;
        if (startIdx < 0) {
            return -1;
        }
        int itemsPastStartIdx = getFirstIdx(suggestions.subList(startIdx, suggestions.size()),
                suggestionOfType(TimeSuggestion.Type.ACTIVITY_BEGIN));
        final int idx = itemsPastStartIdx < 0 ? suggestions.size() - 1 : startIdx + itemsPastStartIdx;
        return Math.min(idx, getLastIdx(suggestions, isBeforeOrAtNow()));
    }
}
