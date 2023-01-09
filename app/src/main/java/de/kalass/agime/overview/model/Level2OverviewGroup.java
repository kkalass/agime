package de.kalass.agime.overview.model;

import com.google.common.collect.ImmutableList;

import de.kalass.agime.model.TrackedActivityModel;

/**
 * Created by klas on 28.11.13.
 */
public final class Level2OverviewGroup extends CompoundTimeSpanningViewModel<Level3OverviewGroup> implements TrackedActivityModelContainer {

    public Level2OverviewGroup(GroupHeader categoryModel, Iterable<Level3OverviewGroup> children, long percentageBaseMillis) {
        super(categoryModel, children, percentageBaseMillis);
    }


    @Override
    public Iterable<TrackedActivityModel> getActivities() {
        ImmutableList.Builder<TrackedActivityModel> b = ImmutableList.builder();
        for (TimeSpanningChild<? extends TrackedActivityModelContainer> child : getChildren()) {
            b.addAll(child.getItem().getActivities());
        }
        return b.build();
    }
}
