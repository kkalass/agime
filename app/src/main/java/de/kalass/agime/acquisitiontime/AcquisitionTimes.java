package de.kalass.agime.acquisitiontime;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.simpleloader.CursorUtil;
import de.kalass.android.common.simpleloader.Weekdays;


/**
 * Created by klas on 23.12.13.
 */
public final class AcquisitionTimes {

	public static final Ordering<AcquisitionTimeInstance> COMPARE_START_TIME_ORDERING = new Ordering<AcquisitionTimeInstance>() {

		@Override
		public int compare(AcquisitionTimeInstance first, AcquisitionTimeInstance second) {
			int dayCompare = first.day.compareTo(second.day);
			if (dayCompare != 0) {
				return dayCompare;
			}
			return first.startTime.compareTo(second.startTime);
		}
	};
	public static final Ordering<AcquisitionTimeInstance> COMPARE_END_TIME_ORDERING = new Ordering<AcquisitionTimeInstance>() {

		@Override
		public int compare(AcquisitionTimeInstance first, AcquisitionTimeInstance second) {
			int dayCompare = first.day.compareTo(second.day);
			if (dayCompare != 0) {
				return dayCompare;
			}
			return first.endTime.compareTo(second.endTime);
		}
	};

	private final AcquisitionTimeInstance _previous;
	private final AcquisitionTimeInstance _current;
	private final AcquisitionTimeInstance _next;

	AcquisitionTimes(AcquisitionTimeInstance previous, AcquisitionTimeInstance current, AcquisitionTimeInstance next) {
		_previous = previous;
		_current = current;
		_next = next;
	}


	/**
	 * @return null if there is no current acquisition data
	 */
	public AcquisitionTimeInstance getCurrent() {
		return _current;
	}


	public boolean hasCurrent() {
		return _current != null;
	}


	public AcquisitionTimeInstance getPrevious() {
		return _previous;
	}


	/**
	 * @return null if there will not be an acquisition scheduled
	 */
	public AcquisitionTimeInstance getNext() {
		return _next;
	}


	private static List<RecurringDAO.Data> getCurrent(List<RecurringDAO.Data> recurring, LocalDate day, LocalTime time) {

		ImmutableList.Builder<RecurringDAO.Data> b = ImmutableList.builder();
		for (RecurringDAO.Data data : recurring) {
			if (data.hasWeekday(Weekdays.getWeekday(day)) && data.isCurrentlyEnabled(day, time)) {
				if (data.isWithin(time)) {
					b.add(data);
				}
			}
			else if (data.activeOnce != null && day.equals(data.activeOnce) && data.isWithin(time)) {
				b.add(data);
			}
		}
		return b.build();
	}


	private static LocalDate firstValid(LocalDate today, LocalDate invalidUntil, LocalDate activeOnce) {
		if (activeOnce != null) {
			if (activeOnce.isBefore(today)) {
				return null;
			}
			return activeOnce;
		}
		if (invalidUntil == null) {
			return today;
		}
		if (invalidUntil.isBefore(today)) {
			return today;
		}
		return invalidUntil;
	}


	private static LocalDate firstValidWeekday(LocalDate date, Set<Weekdays.Weekday> weekdays) {
		if (weekdays.isEmpty()) {
			return null;
		}
		int counter = 0;
		do {
			if (weekdays.contains(Weekdays.getWeekday(date))) {
				return date;
			}
			date = date.plusDays(1);
			counter++;
		} while(counter <= Weekdays.Weekday.values().length);
		throw new IllegalStateException("Weekday not found in " + weekdays + " , date " + date + ", counter " + counter);
	}


	private static List<AcquisitionTimeInstance> getNext(List<RecurringDAO.Data> recurring, AcquisitionTimeInstance current, LocalDate today, LocalTime time) {
		LocalTime earliestTime = current == null ? time : current.endTime.isBefore(time) ? time : current.endTime;
		return getNext(recurring, today, earliestTime);
	}


	private static List<AcquisitionTimeInstance> getNext(List<RecurringDAO.Data> recurring, LocalDate today, LocalTime earliestTime) {

		// for each recurring, calculate the next occurance that starts after day and time and does not overlap current
		ImmutableList.Builder<AcquisitionTimeInstance> b = ImmutableList.builder();
		for (RecurringDAO.Data data : recurring) {
			// first: take inactive recurring acquisition times into account
			LocalDate firstValidDate = firstValid(today, data.inactiveUntil, data.activeOnce);
			if (firstValidDate == null) {
				// ignore items that will never be valid again
				continue;
			}
			// second: if the date is today, and the recurring acquisition time has started, fast forward to the next day
			firstValidDate = (today.equals(firstValidDate) && earliestTime.isAfter(data.startTime)) ? firstValidDate.plusDays(1) : firstValidDate;
			// third: iterate through the weekdays until you find a valid one.
			// Note: can return null, if there is no weekday at all
			if (!today.equals(data.activeOnce)) {
				firstValidDate = firstValidWeekday(firstValidDate, data.weekdays);
			}
			if (firstValidDate != null) {
				Preconditions.checkState(data.isCurrentlyEnabled(firstValidDate));
				b.add(new AcquisitionTimeInstance(data.getId(), ImmutableList.of(data), firstValidDate, data.startTime, data.endTime));
			}
		}
		return COMPARE_START_TIME_ORDERING.sortedCopy(b.build());
	}


	private static List<AcquisitionTimeInstance> getPreviousToday(List<RecurringDAO.Data> recurring, LocalDate today, LocalTime latestEndTime) {

		// for each recurring, calculate the next occurance that starts after day and time and does not overlap current
		ImmutableList.Builder<AcquisitionTimeInstance> b = ImmutableList.builder();
		for (RecurringDAO.Data data : recurring) {
			// first: only take items into account that are valid today
			LocalDate firstValidDate = firstValid(today, data.inactiveUntil, data.activeOnce);
			if (firstValidDate == null || firstValidDate.isAfter(today)) {
				// ignore items that will never be valid again
				continue;
			}
			// second: if the date is today, and the recurring acquisition time has started, fast forward to the next day

			// third: iterate through the weekdays until you find a valid one.
			// Note: can return null, if there is no weekday at all
			if (!today.equals(data.activeOnce)) {
				firstValidDate = firstValidWeekday(firstValidDate, data.weekdays);
			}
			if (firstValidDate == null || !firstValidDate.isEqual(today)) {
				// again, not valid today
				continue;
			}
			if (data.endTime != null && data.endTime.isBefore(latestEndTime)) {
				b.add(new AcquisitionTimeInstance(data.getId(), ImmutableList.of(data), firstValidDate, data.startTime, data.endTime));
			}
		}
		return COMPARE_END_TIME_ORDERING.reverse().sortedCopy(b.build());
	}


	public static AcquisitionTimes fromRecurring(List<RecurringDAO.Data> recurring, DateTime now) {
		LocalDate date = now.toLocalDate();
		LocalTime time = now.toLocalTime();
		List<RecurringDAO.Data> currentCandidates = getCurrent(recurring, date, time);
		AcquisitionTimeInstance current = null;
		if (currentCandidates.size() > 0) {

			RecurringDAO.Data d = currentCandidates.get(0);
			LocalTime startTime = d.startTime;
			long startTimeId = d.getId();
			LocalTime endTime = d.endTime;
			if (currentCandidates.size() > 1) {
				final Ordering<Comparable> natural = Ordering.natural();
				for (RecurringDAO.Data candidate : currentCandidates.subList(1, currentCandidates.size())) {
					if (startTime.isAfter(candidate.startTime)) {
						startTimeId = candidate.getId();
					}
					startTime = natural.min(startTime, candidate.startTime);
					endTime = natural.max(endTime, candidate.endTime);
				}
			}
			// Use the ID of the first candidate as the ID for the combined instance
			long id = currentCandidates.isEmpty() ? -1 : startTimeId;
			current = new AcquisitionTimeInstance(id, currentCandidates, date, startTime, endTime);
		}
		AcquisitionTimeInstance next = Iterables.getFirst(getNext(recurring, current, date, time), null);
		AcquisitionTimeInstance previous = Iterables.getFirst(getPreviousToday(recurring, date, time), null);
		return new AcquisitionTimes(previous, current, next);
	}

}
