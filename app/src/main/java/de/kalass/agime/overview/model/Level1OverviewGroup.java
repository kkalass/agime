package de.kalass.agime.overview.model;

import java.util.List;

/**
 * Created by klas on 28.11.13.
 */
public final class Level1OverviewGroup extends CompoundTimeSpanningViewModel<Level2OverviewGroup> {

    public Level1OverviewGroup(GroupHeader header, List<Level2OverviewGroup> children, Long percentageBaseMillis) {
        super(header, children, percentageBaseMillis);
    }

}
