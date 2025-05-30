package de.kalass.agime.acquisitiontime;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.util.List;

import javax.annotation.CheckForNull;

/**
 * Created by klas on 23.12.13.
 */
public final class AcquisitionTimeInstance {
	private final long id;
	private final List<RecurringDAO.Data> items;
	public final LocalDate day;
	public final LocalTime startTime;
	public final LocalTime endTime;

	public static final int ACQUISITION_TIME_END_THRESHOLD_MINUTES = 120;

	AcquisitionTimeInstance(long id, List<RecurringDAO.Data> items, LocalDate day, LocalTime startTime, LocalTime endTime) {
		this.id = id;
		this.items = items;
		this.day = day;
		this.startTime = startTime;
		this.endTime = endTime;
	}
	
	public long getId() {
		return id;
	}

	public DateTime getEndDateTime() {
		return day.toDateTime(endTime);
	}

	public DateTime getStartDateTime() {
		return day.toDateTime(startTime);
	}

	public List<RecurringDAO.Data> getItems() {
		return items;
	}

	public List<RecurringDAO.Data> getActiveOnceItems() {
		return ImmutableList.copyOf(Iterables.filter(items, RecurringDAO.Data.IS_ACTIVE_ONCE));
	}

	@CheckForNull
	public RecurringDAO.Data findRecurringItem() {
		for (RecurringDAO.Data d : items) {
			if (!d.isActiveOnce()) {
				return d;
			}
		}
		return null;
	}
}
