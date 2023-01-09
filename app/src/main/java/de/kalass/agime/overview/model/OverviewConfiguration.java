package de.kalass.agime.overview.model;

import com.google.common.collect.Ordering;

import java.util.List;

/**
* Created by klas on 15.01.14.
*/
public final class OverviewConfiguration {
    private final GroupHeaderType _level1Fkt;
    private final GroupHeaderType _level2Fkt;
    private final GroupHeaderType _level3Fkt;
    private final Ordering<Level1OverviewGroup> _level1OverviewGroupOrdering;
    private final Ordering<Level2OverviewGroup> _level2OverviewGroupOrdering;
    private final Ordering<Level3OverviewGroup> _level3OverviewGroupOrdering;

    private final List<? extends GroupHeaderType> _level4Fkts;
    private final boolean _level4IncludesDescription;


    public OverviewConfiguration(
            GroupHeaderType level1Fkt,
            GroupHeaderType level2Fkt,
            GroupHeaderType level3Fkt,
            List<? extends GroupHeaderType> level4Fkts,
            boolean level4IncludesDescription
    ) {
        _level1Fkt = level1Fkt;
        _level1OverviewGroupOrdering = getOrdering(_level1Fkt.getSortOrder());
        _level2Fkt = level2Fkt;
        _level2OverviewGroupOrdering = getOrdering(_level2Fkt.getSortOrder());
        _level3Fkt = level3Fkt;
        _level3OverviewGroupOrdering = getOrdering(_level3Fkt.getSortOrder());
        _level4Fkts = level4Fkts;
        _level4IncludesDescription = level4IncludesDescription;
    }

    private static <T extends CompoundTimeSpanningViewModel> Ordering<T> getOrdering(GroupHeaderType.SortOrder sortOrder) {
        switch (sortOrder) {
            case DURATION:
                return Ordering.natural().onResultOf(CompoundTimeSpanningViewModel.GET_DURATION_MILLIS).nullsFirst().reverse();
            case NATURAL:
                return Ordering.natural().onResultOf(CompoundTimeSpanningViewModel.GET_MODEL).nullsFirst();
            default:
                return Ordering.natural().onResultOf(CompoundTimeSpanningViewModel.GET_MODEL).nullsFirst();
        }

    }

    public GroupHeaderType getLevel1() {
        return _level1Fkt;
    }

    public Ordering<Level1OverviewGroup> getLevel1OverviewGroupOrdering() {
        return _level1OverviewGroupOrdering;
    }

    public GroupHeaderType getLevel2() {
        return _level2Fkt;
    }

    public Ordering<Level2OverviewGroup> getLevel2OverviewGroupOrdering() {
        return _level2OverviewGroupOrdering;
    }

    public GroupHeaderType getLevel3() {
        return _level3Fkt;
    }

    public Ordering<Level3OverviewGroup> getLevel3OverviewGroupOrdering() {
        return _level3OverviewGroupOrdering;
    }

    public List<? extends GroupHeaderType> getLevel4() {
        return _level4Fkts;
    }

    public boolean isLevel4IncludesDescription() {
        return _level4IncludesDescription;
    }
}
