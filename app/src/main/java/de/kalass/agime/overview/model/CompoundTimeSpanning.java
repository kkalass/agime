package de.kalass.agime.overview.model;

import com.google.common.collect.ImmutableList;

import java.util.List;

import de.kalass.agime.model.TimeSpanning;

/**
 * Subclass for TimeSpanning instances that are made up of other time spanning instances
 * Created by klas on 28.11.13.
 */
public class CompoundTimeSpanning<T extends TimeSpanning> extends TimeSpanning {

    private final List<TimeSpanningChild<T>> _children;

    public CompoundTimeSpanning(Iterable<? extends T> items, Long percentageBaseMillis) {
        super(sumMillis(items));
        _children = asChildren(items, percentageBaseMillis == null ? getDurationMillis() : percentageBaseMillis.longValue());
    }

    public final List<TimeSpanningChild<T>> getChildren() {
        return _children;
    }

    private List<TimeSpanningChild<T>> asChildren(Iterable<? extends T> items, long totalDurationMillis) {
        ImmutableList.Builder<TimeSpanningChild<T>> builder = ImmutableList.builder();
        for (T item : items) {
            builder.add(new TimeSpanningChild<T>(item, percentage(totalDurationMillis, item)));
        }
        return builder.build();
    }

    private float percentage(long totalDurationMillis, T item) {
        return totalDurationMillis == 0 ? 1f : (float)item.getDurationMillis()/(float)totalDurationMillis;
    }

    public static long sumMillis(Iterable<? extends TimeSpanning> iterable) {
        long total = 0;
        for (TimeSpanning t : iterable) {
            total += t.getDurationMillis();
        }
        return total;
    }
}
