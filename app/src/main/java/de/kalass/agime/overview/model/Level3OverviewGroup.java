package de.kalass.agime.overview.model;

import java.util.List;

import de.kalass.agime.model.TrackedActivityModel;

/**
 * Created by klas on 28.11.13.
 */
public final class Level3OverviewGroup extends CompoundTimeSpanningViewModel<TrackedActivityModel> implements TrackedActivityModelContainer {
    private final Iterable<TrackedActivityModel> _activities;
    private final List<Level4Item> _level4Items;

    public Level3OverviewGroup(
            GroupHeader groupHeader,
            Iterable<TrackedActivityModel> activities,
            List<Level4Item> level4Items,
            long percentageBaseMillis
    ) {
        super(groupHeader, activities, percentageBaseMillis);
        _activities = activities;
        _level4Items = level4Items;
    }

    public Iterable<TrackedActivityModel> getActivities() {
        return _activities;
    }

    public List<Level4Item> getLevel4Items() {
        return _level4Items;
    }
}
