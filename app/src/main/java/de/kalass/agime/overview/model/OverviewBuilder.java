package de.kalass.agime.overview.model;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.kalass.agime.model.TrackedActivityModel;

/**
 * Created by klas on 28.11.13.
 */
public final class OverviewBuilder {



    private OverviewBuilder() {
    }

    public static final List<Level1OverviewGroup> createOverviewHierachy(
            OverviewConfiguration configuration,
            Iterable<TrackedActivityModel> trackedActivities
    ) {
        return createLevel1Hierachy(configuration, trackedActivities);
    }

    private static final List<Level1OverviewGroup> createLevel1Hierachy(
            OverviewConfiguration configuration,
            Iterable<TrackedActivityModel> trackedActivities
    ) {
        Map<GroupHeader, Collection<TrackedActivityModel>> groupedActivities = OverviewUtil.group(
                trackedActivities, configuration.getLevel1()
        );

        return OverviewUtil.convertAndSort(
                new Level1OverviewGroup[groupedActivities.size()],
                groupedActivities,
                new Level1OverviewFactory(configuration),
                configuration.getLevel1OverviewGroupOrdering());
    }


    private static class Level1OverviewFactory
            implements Function<Map.Entry<GroupHeader, Collection<TrackedActivityModel>>, Level1OverviewGroup> {
        private final OverviewConfiguration _configuration;

        private Level1OverviewFactory(OverviewConfiguration configuration) {
            _configuration = configuration;
        }

        @Override
        public Level1OverviewGroup apply(Map.Entry<GroupHeader, Collection<TrackedActivityModel>> entry) {
            final Collection<TrackedActivityModel> activities = entry.getValue();
            final GroupHeader level1Header = entry.getKey();
            long total = CompoundTimeSpanning.sumMillis(activities);

            Map<GroupHeader, Collection<TrackedActivityModel>> groupedActivities =
                    OverviewUtil.group(activities, _configuration.getLevel2());

            List<Level2OverviewGroup> level2OverviewGroups = OverviewUtil.convertAndSort(
                    new Level2OverviewGroup[groupedActivities.size()],
                    groupedActivities,
                    new Level2OverviewFactory(_configuration, total),
                    _configuration.getLevel2OverviewGroupOrdering());

            return new Level1OverviewGroup(level1Header, level2OverviewGroups, total);
        }
    }

    private static class Level2OverviewFactory implements Function<Map.Entry<GroupHeader, Collection<TrackedActivityModel>>, Level2OverviewGroup> {
        private final OverviewConfiguration _configuration;
        private final long _percentageBaseMillis;

        public Level2OverviewFactory(OverviewConfiguration configuration, long percentageBaseMillis) {
            _configuration = configuration;
            _percentageBaseMillis = percentageBaseMillis;
        }

        @Override
        public Level2OverviewGroup apply(Map.Entry<GroupHeader, Collection<TrackedActivityModel>> e) {
            final GroupHeader groupHeader = e.getKey();
            final Collection<TrackedActivityModel> activities = e.getValue();

            Map<GroupHeader, Collection<TrackedActivityModel>> groupedActivities =
                    OverviewUtil.group(activities, _configuration.getLevel3());

            List<Level3OverviewGroup> level3OverviewGroups =  OverviewUtil.convertAndSort(
                    new Level3OverviewGroup[groupedActivities.size()],
                    groupedActivities,
                    new Level3OverviewFactory(_configuration, _percentageBaseMillis),
                    _configuration.getLevel3OverviewGroupOrdering());

            return new Level2OverviewGroup(
                    groupHeader, level3OverviewGroups, _percentageBaseMillis
            );
        }
    }

    private static class Level3OverviewFactory implements Function<Map.Entry<GroupHeader, Collection<TrackedActivityModel>>, Level3OverviewGroup> {
        private final OverviewConfiguration _configuration;
        private final long _percentageBaseMillis;

        public Level3OverviewFactory(OverviewConfiguration configuration, long percentageBaseMillis) {
            _configuration = configuration;
            _percentageBaseMillis = percentageBaseMillis;
        }

        @Override
        public Level3OverviewGroup apply(Map.Entry<GroupHeader, Collection<TrackedActivityModel>> e) {
            final GroupHeader groupHeader = e.getKey();
            final Collection<TrackedActivityModel> activities = e.getValue();

            final List<Level4Item> level4Items = getLevel4Items(activities);
            return new Level3OverviewGroup(
                    groupHeader, activities, level4Items,  _percentageBaseMillis);
        }

        protected ImmutableList<Level4Item> getLevel4Items(Collection<TrackedActivityModel> activities) {
            List<Level4Item> allDetails = getStrings(activities);
            if (allDetails.isEmpty()) {
                return ImmutableList.of();
            }
            ImmutableList.Builder<Level4Item> childBuilder = ImmutableList.builder();
            childBuilder.addAll(allDetails);
            return childBuilder.build();
        }

        protected List<Level4Item> getStrings(Collection<TrackedActivityModel> activities) {

            if (! _configuration.isLevel4IncludesDescription() && _configuration.getLevel4().isEmpty()) {
                return ImmutableList.of();
            }
            ArrayList<Level4Item> allDetails = new ArrayList<Level4Item>();
            if (_configuration.isLevel4IncludesDescription()) {
                for (TrackedActivityModel tracked: activities) {
                    final String trimmed = Strings.nullToEmpty(tracked.getDetails()).trim();
                    if (trimmed.length() > 0) {
                        allDetails.add(new Level4Item(trimmed, tracked, _configuration.getLevel3()));
                    }
                }
            }
            for (GroupHeaderType level4Fkt : _configuration.getLevel4()) {
                for (TrackedActivityModel tracked: activities) {
                    final GroupHeader level4 = level4Fkt.apply(tracked);
                    if (level4 != null) {
                        final String trimmed = Strings.nullToEmpty(level4.getLevel4Line()).trim();
                        if (trimmed.length() > 0) {
                            allDetails.add(new Level4Item(trimmed, tracked, level4Fkt));
                        }
                    }
                }
            }

            return allDetails;
        }
    }
}
