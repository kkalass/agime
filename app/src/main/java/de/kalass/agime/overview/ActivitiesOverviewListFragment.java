package de.kalass.agime.overview;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimaps;

import org.joda.time.LocalDate;

import java.text.NumberFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.kalass.agime.R;
import de.kalass.agime.Workarounds;
import de.kalass.agime.backup.CSVFileReaderWriter;
import de.kalass.agime.loader.TrackedActivityAsyncLoader;
import de.kalass.agime.loader.TrackedActivityListByIdsAsyncLoader;
import de.kalass.agime.model.TrackedActivityModel;
import de.kalass.agime.overview.model.CompoundTimeSpanningViewModel;
import de.kalass.agime.overview.model.GroupHeaderType;
import de.kalass.agime.overview.model.Level1OverviewGroup;
import de.kalass.agime.overview.model.Level2OverviewGroup;
import de.kalass.agime.overview.model.Level3OverviewGroup;
import de.kalass.agime.overview.model.Level4Item;
import de.kalass.agime.overview.model.OverviewBuilder;
import de.kalass.agime.overview.model.OverviewConfiguration;
import de.kalass.agime.overview.model.TimeSpanningChild;
import de.kalass.agime.overview.model.TrackedActivityModelContainer;
import de.kalass.agime.provider.MCContract;
import de.kalass.agime.settings.Preferences;
import de.kalass.agime.trackactivity.TrackActivity;
import de.kalass.agime.trackactivity.TrackedActivitiesListAdapter;
import de.kalass.android.common.activity.BaseViewWrapper;
import de.kalass.android.common.adapter.AbstractListAdapter;
import de.kalass.android.common.model.IViewModel;
import de.kalass.android.common.support.fragments.BaseFragment;
import de.kalass.android.common.util.DateUtil;
import de.kalass.android.common.util.TimeFormatUtil;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A fragment that organizes activities in 4 Levels and displays them to the user.
 * Created by klas on 06.10.13.
 */
public class ActivitiesOverviewListFragment extends BaseFragment {
    private static final String LOG_TAG = "ActivitiesOverview";

    public static final String ARG_DAYS_BEFORE_TODAY = "daysBeforeToday";
    public static final String ARG_WEEKS_BEFORE_CURRENT = "weeksBeforeCurrent";
    public static final String ARG_MONTHS_BEFORE_CURRENT = "monthsBeforeCurrent";
    public static final String ARG_YEARS_BEFORE_CURRENT = "yearsBeforeCurrent";
    private static final int ACTIVITY_CODE_TRACK_TIME = 42;

    public static final int LOADER_ID_OVERVIEW_DATA = 1;
    private static final int TAG_ITEMS = R.id.activity_track_details;
    private static final int REQUEST_CODE_CHOOSE_DIRECTORY = 29;

    private LocalDate _today;
    private long _startTimeInclusiveMillis;
    private long _endTimeExclusiveMillis;
    private boolean _showLevel4;
    private Mode _mode;
    private ActivitiesOverviewsWrappedView _views;
    private OverviewConfiguration configuration;
    private Predicate<TrackedActivityModel> filterPredicate;
    private View _headerView1;
    private int _extraHeaderPadding;

    enum Mode {
        DAY,
        WEEK,
        MONTH,
        YEAR,
        TOTAL
    }

    public ActivitiesOverviewListFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        _headerView1 = inflater.inflate(R.layout.empty_list_header, null, false);
        return inflater.inflate(ActivitiesOverviewsWrappedView.LAYOUT, null);
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        // general configuration
        setHasOptionsMenu(true);

        _extraHeaderPadding = dpToPx(getActivity(), 8);
        // initialize data from arguments
        _mode = detectMode(getArguments());
        _today = new LocalDate();
        _showLevel4 = true;//getArguments().containsKey(ARG_DAYS_BEFORE_TODAY);
        initializeTimeFields(_today);


        // configure views
        _views = new ActivitiesOverviewsWrappedView(getView());
        _views.emptyText.setText(R.string.fragment_tracked_activities_empty_text);

        _views.list.addHeaderView(_headerView1);
        _views.setListAdapter(getActivity(), new Level1OverviewLoadingListAdapter(
                getStarttimeInclusiveMillis(), getEndtimeExclusiveMillis()
        ));

        setOverviewConfiguration(this.configuration, this.filterPredicate);

        // Kickoff data loading
        getLoaderManager().initLoader(LOADER_ID_OVERVIEW_DATA, null, _views.getListAdapter());
        getView().post(new Runnable() {
            @Override
            public void run() {

                setHeaderHeights();
            }
        });
    }

    static int dpToPx(Context context, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, context.getResources().getDisplayMetrics());
    }

    protected void setHeaderHeights() {

        if (!isDetached() && !isRemoving() && getActivity() != null ) {
            int initialHeight = getResources().getDimensionPixelSize(R.dimen.custom_toolbar_height_overview_fragment);

            if (_headerView1 != null) {
                ViewGroup.LayoutParams lp = _headerView1.getLayoutParams();
                if (lp != null) {
                    lp.height = initialHeight + _extraHeaderPadding;
                }
            }
        }
    }



    public long getStarttimeInclusiveMillis() {
        return _startTimeInclusiveMillis;
    }

    public long getEndtimeExclusiveMillis() {
        return _endTimeExclusiveMillis;
    }

    private Mode detectMode(Bundle args) {
        if (args.containsKey(ARG_DAYS_BEFORE_TODAY)) {
            return Mode.DAY;
        } else if (args.containsKey(ARG_WEEKS_BEFORE_CURRENT)) {
            return Mode.WEEK;
        } else if (args.containsKey(ARG_MONTHS_BEFORE_CURRENT)) {
            return Mode.MONTH;
        } else if (args.containsKey(ARG_YEARS_BEFORE_CURRENT)) {
            return Mode.YEAR;
        } else {
            return Mode.TOTAL;
        }
    }

    private void initializeTimeFields(LocalDate today) {
        LocalDate day;
        Bundle args = getArguments();
        switch(_mode) {
            case DAY:
                day = today.minusDays(args.getInt(ARG_DAYS_BEFORE_TODAY, 0));
                _startTimeInclusiveMillis = DateUtil.getMillisAtStartOfDay(day);
                _endTimeExclusiveMillis = DateUtil.getMillisAtEndOfDay(day);
                break;
            case WEEK:
                day = today.minusWeeks(args.getInt(ARG_WEEKS_BEFORE_CURRENT, 0));
                _startTimeInclusiveMillis = DateUtil.getMillisAtStartOfDay(DateUtil.getFirstDayOfWeek(day));
                _endTimeExclusiveMillis = DateUtil.getMillisAtEndOfDay(DateUtil.getLastDayOfWeek(day));
                break;
            case MONTH:
                day = today.minusMonths(args.getInt(ARG_MONTHS_BEFORE_CURRENT, 0));
                _startTimeInclusiveMillis = DateUtil.getMillisAtStartOfDay(DateUtil.getFirstDayOfMonth(day));
                _endTimeExclusiveMillis = DateUtil.getMillisAtEndOfDay(DateUtil.getLastDayOfMonth(day));
                break;
            case YEAR:
                day = today.minusYears(args.getInt(ARG_YEARS_BEFORE_CURRENT, 0));
                _startTimeInclusiveMillis = DateUtil.getMillisAtStartOfDay(DateUtil.getFirstDayOfYear(day));
                _endTimeExclusiveMillis = DateUtil.getMillisAtEndOfDay(DateUtil.getLastDayOfYear(day));
                break;
            case TOTAL:
                _startTimeInclusiveMillis = new LocalDate(1970, 1, 1).plusDays(2).toDateTimeAtStartOfDay().getMillis();
                _endTimeExclusiveMillis = DateUtil.getMillisAtEndOfDay(LocalDate.now());
                break;
            default:
                throw new IllegalStateException("Must specify days, weeks, months or years before current");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        LocalDate current = new LocalDate();
        if (!Objects.equal(_today, current)) {
            _today = current;
            initializeTimeFields(_today);
            refreshListAdapter();
        }
    }

    public void setOverviewConfiguration(
            OverviewConfiguration configuration,
            Predicate<TrackedActivityModel> filterPredicate
    ) {
        this.configuration = configuration;
        this.filterPredicate = filterPredicate;
        Log.d(LOG_TAG, "setOverviewConfiguration");
        if (_views == null) {
            return;
        }
        final Level1OverviewLoadingListAdapter adapter = _views.getListAdapter();
        if (adapter != null) {
            adapter.setConfiguration(configuration, filterPredicate);
        }
    }

    private void refreshListAdapter() {
        _views.setListAdapter(getActivity(), new Level1OverviewLoadingListAdapter(
                getStarttimeInclusiveMillis(), getEndtimeExclusiveMillis()
        ));
        setOverviewConfiguration(this.configuration, this.filterPredicate);
    }


    static final class Level1Subviews {
        static final int LAYOUT = R.layout.activites_overview_level1;
        private final View colorBar;
        private final TextView name;
        private final TextView duration;
        private final ImageView expandIcon;

        Level1Subviews(View view) {
            expandIcon = checkNotNull((ImageView)view.findViewById(R.id.expand_icon));
            colorBar = checkNotNull(view.findViewById(R.id.overview_level1_color));
            name = checkNotNull((TextView)view.findViewById(R.id.overview_level1_name));
            duration = checkNotNull((TextView)view.findViewById(R.id.overview_level1_duration_sum));
        }
    }

    static final class Level2Subviews extends BaseViewWrapper {
        static final int LAYOUT = R.layout.activites_overview_level2;
        static final int ID_HEADER_VIEW = R.id.overview_level2_expanded_header_row;
        private final View headerView;
        private final TextView name;
        private final TextView duration;
        private final TextView durationPercentage;

        Level2Subviews(View view) {
            super(view);
            headerView = getView(ID_HEADER_VIEW);
            name = getTextView(R.id.overview_level2_name);
            duration = getTextView(R.id.overview_level2_duration);
            durationPercentage = getTextView(R.id.overview_level2_duration_percentage);
        }
    }

    static final class Level3Subviews {

        static final int LAYOUT = R.layout.activites_overview_level3;

        private final TextView durationPercentageView;
        private final TextView durationView;
        private final TextView name;
        private final View content;

        Level3Subviews(View view) {
            durationPercentageView = checkNotNull((TextView)view.findViewById(R.id.overview_level3_duration_percentage));
            durationView = checkNotNull((TextView)view.findViewById(R.id.overview_level3_duration));
            name = checkNotNull((TextView)view.findViewById(R.id.overview_level3_name));
            content = checkNotNull(view.findViewById(R.id.content));
        }
    }

    static final class Level4Subviews {
        static final int LAYOUT = R.layout.activites_overview_level4;
        static final int ID_DETAILS = R.id.overview_level4_details;
        static final int ID_CONTENT_CONTAINER = R.id.content;

        private final TextView detailsView;
        private final View content;

        Level4Subviews(View view) {
            detailsView = checkNotNull((TextView)view.findViewById(ID_DETAILS));
            content = checkNotNull(view.findViewById(ID_CONTENT_CONTAINER));
        }
    }



    private class Level1OverviewLoadingListAdapter
            extends BaseExpandableListAdapter
            implements LoaderManager.LoaderCallbacks<List<TrackedActivityModel>>,
            ExpandableListView.OnGroupClickListener, View.OnClickListener {

        private final long _rangeStarttimeMillisInclusive;
        private final long _rangeEndtimeMillisExclusive;

        private OverviewConfiguration _configuration;
        private Predicate<TrackedActivityModel> _filterPredicate;
        private List<Level1OverviewGroup> _data = ImmutableList.of();
        private List<List<TimeSpanningChild<? extends IViewModel>>> _childItems = ImmutableList.of();
        private Iterable<TrackedActivityModel> _activities;

        public Level1OverviewLoadingListAdapter(
                long rangeStarttimeMillisInclusive,
                long rangeEndtimeMillisExclusive
        ) {
            super();
            _rangeStarttimeMillisInclusive = rangeStarttimeMillisInclusive;
            _rangeEndtimeMillisExclusive = rangeEndtimeMillisExclusive;
        }

        public OverviewConfiguration getConfiguration() {
            return _configuration;
        }

        public List<TrackedActivityModel> getActivities() {
            return ImmutableList.copyOf(_activities);
        }

        @Override
        public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
            // stop the user from collapsing the only element
            return isCollapseIconHidden(parent.isGroupExpanded(groupPosition));
        }

        protected boolean isCollapseIconHidden(boolean groupExpanded) {
            return getGroupCount() == 1 && groupExpanded;
        }

        public void setConfiguration(
                OverviewConfiguration configuration,
                Predicate<TrackedActivityModel> filterPredicate
        ) {
            _configuration = configuration;
            _filterPredicate = filterPredicate;
            updateItems();
        }

        @Override
        public Loader<List<TrackedActivityModel>> onCreateLoader(int i, Bundle bundle) {
            return new TrackedActivityAsyncLoader(
                    getActivity(),
                    _rangeStarttimeMillisInclusive,
                    _rangeEndtimeMillisExclusive
            );
        }


        @Override
        public void onLoadFinished(Loader<List<TrackedActivityModel>> loader, List<TrackedActivityModel> activities) {
            setActivities(activities);

            // The list should now be shown.
            if (_views != null) {
                _views.loading.setVisibility(View.GONE);
                _views.empty.setVisibility(activities.isEmpty() ? View.VISIBLE : View.GONE);
            }

        }

        void setActivities(Iterable<TrackedActivityModel> activities) {
            _activities = activities;
            updateItems();
        }

        void updateItems() {
            if (_activities == null || _configuration == null) {
                setItems(null);
                return;
            }
            Iterable<TrackedActivityModel> activities =
                    _filterPredicate != null ? Iterables.filter(_activities, _filterPredicate): _activities;
            setItems(OverviewBuilder.createOverviewHierachy(_configuration, activities));
            expandStates(_views.list);
        }


        @Override
        public void onLoaderReset(Loader<List<TrackedActivityModel>> loader) {
            setActivities(null);
        }

        private void setItems(List<Level1OverviewGroup> items) {
            Log.d(LOG_TAG, "setItems");
            _data = items == null ? ImmutableList.<Level1OverviewGroup>of() : items;
            ImmutableList.Builder<List<TimeSpanningChild<? extends IViewModel>>> builder = ImmutableList.builder();
            if (items != null) {
                for (Level1OverviewGroup level1OverviewGroup : items) {
                    ImmutableList.Builder<TimeSpanningChild<? extends IViewModel>> childBuilder = ImmutableList.builder();
                    for (TimeSpanningChild<Level2OverviewGroup> level2Item: level1OverviewGroup.getChildren()) {
                        childBuilder.add(level2Item);
                        for (TimeSpanningChild<Level3OverviewGroup> level3Item: level2Item.getItem().getChildren()) {
                            childBuilder.add(level3Item);
                            if (_showLevel4) {
                                final List<Level4Item> level4Items = level3Item.getItem().getLevel4Items();

                                final ImmutableListMultimap<GroupHeaderType,Level4Item> index = Multimaps.index(level4Items, Level4Item.GET_TYPE);

                                // shrink the level 4 items - but only if they are not real details
                                final ImmutableMap<GroupHeaderType,Collection<Level4Item>> map = index.asMap();
                                for (Map.Entry<GroupHeaderType, Collection<Level4Item>> entry : map.entrySet()) {
                                    float percentage = 1f/map.size(); // fake
                                    final Collection<Level4Item> values = entry.getValue();
                                    final GroupHeaderType key = entry.getKey();
                                    if (values.size() == 1 || key == _configuration.getLevel3()) {
                                        for (Level4Item item: values) {
                                            childBuilder.add(new TimeSpanningChild<Level4Item>(item, percentage ));
                                        }
                                    } else {
                                        ImmutableList.Builder<TrackedActivityModel> b = ImmutableList.builder();
                                        for (Level4Item item : values) {
                                            b.addAll(item.getActivities());
                                        }
                                        Set<String> uniqueDetails = ImmutableSet.copyOf(Iterables.transform(values, Level4Item.GET_DETAILS));
                                        childBuilder.add(new TimeSpanningChild<Level4Item>(new Level4Item(Joiner.on(", ").join(uniqueDetails), b.build(), key), percentage ));
                                    }
                                }
                            }
                        }
                    }
                    builder.add(childBuilder.build());
                }
            }
            _childItems = builder.build();
            Log.d(LOG_TAG, "setItems - will notify data set changed for " + _childItems.size() + " groups");
            notifyDataSetChanged();
            if (items != null) {
                // data was loaded
                //_views.empty.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public int getGroupCount() {
            return _data.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return _childItems.get(groupPosition).size();
        }

        @Override
        public Level1OverviewGroup getGroup(int groupPosition) {
            return _data.get(groupPosition);
        }

        @Override
        public TimeSpanningChild<? extends IViewModel> getChild(int groupPosition, int childPosition) {
            List<TimeSpanningChild<? extends IViewModel>> timeSpanningChildren = _childItems.get(groupPosition);
            return timeSpanningChildren.get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return _data.get(groupPosition).getId();
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            // there are children of different types mixed within a single group - unfortunately
            // this means that we cannot use getChild(groupPosition, childPosition).getItem().getId().
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            // some smart way to use the Id if the actual item and make this stable again?
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            Log.d(LOG_TAG, "getGroupView");
            final View view;
            if (convertView != null && (convertView.getTag() instanceof Level1Subviews)) {
                view = convertView;
            } else {
                view = getLayoutInflater(null).inflate(Level1Subviews.LAYOUT, null);
                view.setTag(new Level1Subviews(view));
            }
            final Resources resources = getResources();
            final Level1OverviewGroup item = getGroup(groupPosition);
            final Level1Subviews subviews = (Level1Subviews)view.getTag();
            Drawable drawable = getResources().getDrawable(isExpanded ? R.drawable.ic_navigation_expand : R.drawable.ic_navigation_collapse);
            subviews.expandIcon.setImageDrawable(drawable);
            subviews.expandIcon.setVisibility(isCollapseIconHidden(isExpanded) ? View.INVISIBLE : View.VISIBLE);
            subviews.colorBar.setBackgroundColor(getColorCode(item, resources, R.color.overview_level1_group_header_default_background));
            subviews.name.setText(item.getName(resources));
            subviews.duration.setText(TimeFormatUtil.formatDuration(getActivity(), item.getDurationMillis()));
            return view;
        }


        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                Log.d(LOG_TAG, "getChildView(" + groupPosition + ", " + childPosition + ", " + isLastChild + ", " + convertView + ", " + parent + ")");
            }
            boolean childIsLevel2Content = isLevel2Content(getChild(groupPosition, childPosition));
            boolean nextIsLevel2Content = (childPosition + 1 < getChildrenCount(groupPosition)) && isLevel2Content(getChild(groupPosition, childPosition + 1));
            View view = doGetChildView(
                    groupPosition,
                    childPosition,
                    isLastChild || (childIsLevel2Content && !nextIsLevel2Content),
                    convertView,
                    parent
            );

            int bottomPadding = isLastChild ? getResources().getDimensionPixelSize(R.dimen.overview_level1_marginBottom) : 0;
            view.setPadding(
                    view.getPaddingLeft(),
                    view.getPaddingTop(),
                    view.getPaddingRight(),
                    bottomPadding
            );
            if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                Log.d(LOG_TAG, "getChildView( ... ) => " + view);
            }
            return view;
        }

        private void adjustLevel2ContentPadding(View view, boolean isLastContentItem) {
            // control the bottom padding
            int paddingBottom = isLastContentItem
                    ? getResources().getDimensionPixelSize(R.dimen.overview_level2_card_padding_bottom)
                    : 0;

            view.setPadding(
                    view.getPaddingLeft(),
                    view.getPaddingTop(),
                    view.getPaddingRight(),
                    paddingBottom
            );

        }

        /**
         * @return true, if next is level 3 or level 4 - i.e. a content item of Level2
         */
        private boolean isLevel2Content(TimeSpanningChild<? extends IViewModel> next) {
            if (next == null) {
                return false;
            }
            IViewModel model = next.getItem();
            return (model instanceof Level3OverviewGroup) || (model instanceof Level4Item);
        }

        private View doGetChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            final TimeSpanningChild<?> model = getChild(groupPosition, childPosition);
            if (model.getItem() instanceof Level2OverviewGroup) {
                return getLevel2View((TimeSpanningChild<Level2OverviewGroup>) model, isLastChild, convertView, parent);
            }
            if (model.getItem() instanceof Level3OverviewGroup) {
                return getLevel3View((TimeSpanningChild<Level3OverviewGroup>) model, isLastChild, convertView, parent);
            }
            if (model.getItem() instanceof  Level4Item) {
                return getLevel4View((TimeSpanningChild<Level4Item>) model, isLastChild, convertView, parent);
            }

            throw new IllegalStateException("unsupported type " + model.getItem());
        }

        public View getLevel2View(TimeSpanningChild<Level2OverviewGroup> model, boolean isLastChild, View convertView, ViewGroup parent) {
            final View view;
            if (convertView != null && (convertView.getTag() instanceof Level2Subviews)) {
                view = convertView;
            } else {
                view = getLayoutInflater(null).inflate(Level2Subviews.LAYOUT, null);
                view.setTag(new Level2Subviews(view));
            }
            final Level2Subviews subviews = (Level2Subviews)view.getTag();

            final Level2OverviewGroup item = checkNotNull(model.getItem());
            view.setTag(TAG_ITEMS, item);
            view.setOnClickListener(this);

            final Resources resources = getResources();

            boolean showDuration = (item.getChildren().size() > 1);
            subviews.duration.setVisibility(showDuration ? View.VISIBLE : View.GONE);
            subviews.headerView.setBackgroundColor(getColorCode(item, resources, R.color.overview_level2_group_header_default_background));

            subviews.name.setText(item.getName(resources));
            subviews.durationPercentage.setText(NumberFormat.getPercentInstance().format(model.getPercentage()));
            subviews.duration.setText(TimeFormatUtil.formatDuration(getActivity(), model.getItem().getDurationMillis()));

            return view;
        }

        public View getLevel3View(TimeSpanningChild<Level3OverviewGroup> model, boolean isLastChild, View convertView, ViewGroup parent) {
            final View view;
            if (convertView != null && (convertView.getTag() instanceof Level3Subviews)) {
                view = convertView;
            } else {
                view = getLayoutInflater(null).inflate(Level3Subviews.LAYOUT, null);
                view.setTag(new Level3Subviews(view));
            }
            final Level3Subviews subviews = (Level3Subviews)view.getTag();

            final Level3OverviewGroup item = model.getItem();
            view.setTag(TAG_ITEMS, item);
            view.setOnClickListener(this)
            ;
            final Resources resources = getResources();

            subviews.durationPercentageView.setVisibility(View.VISIBLE);
            subviews.durationPercentageView.setText(NumberFormat.getPercentInstance().format(model.getPercentage()));
            subviews.durationView.setText(TimeFormatUtil.formatDuration(getActivity(), model.getItem().getDurationMillis()));
            subviews.name.setText(item.getName(resources));

            adjustLevel2ContentPadding(subviews.content, isLastChild);
            return view;
        }

        public View getLevel4View(TimeSpanningChild<Level4Item> model, boolean isLastChild, View convertView, ViewGroup parent) {
            final View view;
            if (convertView != null && (convertView.getTag() instanceof Level4Subviews)) {
                view = convertView;
            } else {
                view = getLayoutInflater(null).inflate(Level4Subviews.LAYOUT, null);
                view.setTag(new Level4Subviews(view));
            }
            final Level4Subviews subviews = (Level4Subviews)view.getTag();

            final Level4Item item = model.getItem();
            view.setTag(TAG_ITEMS, item);
            view.setOnClickListener(this);

            final GroupHeaderType type = item.getType();
            if (type != _configuration.getLevel3()) {
                String title = type.getTitle();
                SpannableString text = new SpannableString(title + ": " + item.getDetails());
                text.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, title.length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                subviews.detailsView.setText(text);
            } else {
                subviews.detailsView.setText(item.getDetails());
            }
            adjustLevel2ContentPadding(subviews.content, isLastChild);

            return view;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            // no items are selecteable
            return false;
        }

        @Override
        public void onClick(View v) {
            //if (v.getId() == R.id.overview_level2_expanded_header_row) {
            // FIXME: do something..
            Object item = v.getTag(TAG_ITEMS);

            Log.i("TEST", "DO IT " + item);
            //}

            if (item instanceof TrackedActivityModelContainer) {
                Iterable<TrackedActivityModel> activities = ((TrackedActivityModelContainer) item).getActivities();

                ShowTrackedActivitiesDialogFragment f = new ShowTrackedActivitiesDialogFragment();
                // will set args,
                f.setList(ImmutableList.copyOf(activities));
                f.show(getFragmentManager(), "details");
            }
        }
    }

    public static final class OverviewTrackedActivitiesListAdapter extends TrackedActivitiesListAdapter implements View.OnClickListener {

        public OverviewTrackedActivitiesListAdapter(Context context) {
            super(context);
        }

        @Override
        protected View getOrCreateView(View convertView, ViewGroup parent, TrackedActivityModel item) {
            TrackedActivityModel oldItem = convertView == null ? null : (convertView.getTag() instanceof TrackedActivityModel ? (TrackedActivityModel)convertView.getTag(): null);
            if (oldItem != null && oldItem.getFakeness() == item.getFakeness()) {
                return convertView;
            }
            View r;
            if (item.getFakeness() == TrackedActivityModel.Fakeness.START_OF_DAY) {
                r = getLayoutInflater().inflate(ActivityChooserDialogViewWrapper.LAYOUT_DAY_START, parent, false);
            } else if (item.getFakeness() == TrackedActivityModel.Fakeness.IN_BETWEEN) {
                r = getLayoutInflater().inflate(ActivityChooserDialogViewWrapper.LAYOUT_IN_BETWEEN, parent, false);
            } else {
                r = super.getOrCreateView(null, parent, item);
            }
            r.setTag(item);
            return r;
        }

        @Override
        protected View fillView(View view, TrackedActivityModel item, int position) {
            if (item.getFakeness() == TrackedActivityModel.Fakeness.START_OF_DAY || item.getFakeness() == TrackedActivityModel.Fakeness.IN_BETWEEN) {

                TextView textView = (TextView)view.findViewById(android.R.id.text1);
                if (item.getFakeness() == TrackedActivityModel.Fakeness.IN_BETWEEN) {
                    textView.setText("");
                } else {
                    textView.setText(DateUtils.formatDateTime(getContext(), item.getStartTimeMillis(), DateUtils.FORMAT_SHOW_DATE));
                }

                view.setOnClickListener(
                        null
                );

                return view;
            }

            View v = super.fillView(view, item, position);
            v.setTag(item);
            v.setOnClickListener(this);
            return v;
        }

        @Override
        protected TrackedActivitiesListAdapter.FakenessAdapter getAdapter(TrackedActivityModel.Fakeness fakeness) {
            switch (fakeness) {
                case IN_BETWEEN:
                    return new InBetweenItemAdapter() {
                        @Override
                        public int getScaledHeightDp(int scaledHeightDP) {
                            // no need to enlarge item here
                            return 0;
                        }
                    };
                case REAL:
                    return new RealItemAdapter() {

                        @Override
                        public String getEndTimeString(Context context, TrackedActivityModel item, TrackedActivityModel next) {
                            return formatTime(context, item.getEndTimeMillis());
                        }

                        @Override
                        public boolean isJoinPrevious(boolean projectUnchanged, boolean activityTypeUnchanged, boolean categoryUnchanged) {
                            return false;
                        }

                        @Override
                        public boolean isForceShowEndTime(TrackedActivityModel item, TrackedActivityModel next) {
                            return true;
                        }
                    };
            }
            return super.getAdapter(fakeness);
        }

        @Override
        public void onClick(View v) {
            TrackedActivityModel item = (TrackedActivityModel)v.getTag();
            if (item.getFakeness() != TrackedActivityModel.Fakeness.REAL) {

                return;
            }
            Intent intent = new Intent(Intent.ACTION_EDIT, ContentUris.withAppendedId(MCContract.Activity.CONTENT_URI, item.getId()));
            intent.setClass(getContext(), TrackActivity.class);

            getContext().startActivity(intent);

        }
    }

    private static final class ActivityChooserDialogViewWrapper extends BaseViewWrapper {
        public static final int LAYOUT_DEFAULT = R.layout.tracked_activities_selection_dialog_fragment;
        public static final int LAYOUT_IN_BETWEEN = R.layout.tracked_activities_selection_in_between_item;
        public static final int LAYOUT_DAY_START = R.layout.tracked_activities_selection_start_day_item;

        public static final int ID_LIST = android.R.id.list;

        final ListView list;

        ActivityChooserDialogViewWrapper(View view) {
            super(view);
            list = get(ListView.class, ID_LIST);
        }

        public void setListAdapter(AbstractListAdapter<TrackedActivityModel> adapter) {
            list.setAdapter(adapter);
        }
    }

    public static final class ShowTrackedActivitiesDialogFragment extends DialogFragment implements LoaderManager.LoaderCallbacks<List<TrackedActivityModel>> {
        public static final int LOADER_ID_TRACKED_ACTIVITIES_LIST = 0;

        private static final String ARG_IDS = "ids";
        private AbstractListAdapter<TrackedActivityModel> activities;
        private ImmutableList<TrackedActivityModel> cachedActivities;

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

        }

        public void setList(List<TrackedActivityModel> models) {
            long[] ids = new long[models.size()];
            for (int i = 0; i < ids.length; i++) {
                ids[i] = models.get(i).getId();
            }
            cachedActivities = getTrackedActivityListItems(models);

            Bundle args = new Bundle();
            args.putLongArray(ARG_IDS, ids);
            setArguments(args);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {


            getLoaderManager().initLoader(LOADER_ID_TRACKED_ACTIVITIES_LIST, null, this);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            activities = new OverviewTrackedActivitiesListAdapter(getActivity());
            if (cachedActivities != null) {
                activities.setItems(cachedActivities);
            }

            View v = getActivity().getLayoutInflater().inflate(ActivityChooserDialogViewWrapper.LAYOUT_DEFAULT, null);

            ActivityChooserDialogViewWrapper view = new ActivityChooserDialogViewWrapper(v);
            view.setListAdapter(activities);
            view.list.setDivider(null);
            view.list.setDividerHeight(0);

            builder.setView(v);

            return builder.create();

        }

        protected ImmutableList<TrackedActivityModel> getTrackedActivityListItems(Iterable<TrackedActivityModel> activitiesList) {
            ImmutableList.Builder<TrackedActivityModel> b = ImmutableList.builder();
            TrackedActivityModel last = null;
            for (TrackedActivityModel m : activitiesList) {
                boolean differentDay = last == null || !m.getStarttimeDateTimeMinutes().toLocalDate().equals(last.getStarttimeDateTimeMinutes().toLocalDate());
                if (differentDay) {
                    b.add(TrackedActivityModel.fakeStartOfDay(-m.getId(), m.getStartTimeMillis()));
                }

                if (!differentDay && !m.getStarttimeDateTimeMinutes().toLocalTime().equals(last.getEndtimeDateTimeMinutes().toLocalTime())) {
                    b.add(TrackedActivityModel.fakeInBetween(-m.getId(), last.getEndTimeMillis(), m.getStartTimeMillis()));
                }

                b.add(m);
                last = m;
            }
            return b.build();
        }



        public Loader<List<TrackedActivityModel>> onCreateLoader(int id, Bundle args) {
            long[] ids = (args == null || !args.containsKey(ARG_IDS))? getArguments().getLongArray(ARG_IDS) : args.getLongArray(ARG_IDS);
            return new TrackedActivityListByIdsAsyncLoader(getActivity(), ids);
        }

        @Override
        public void onLoadFinished(Loader<List<TrackedActivityModel>> loader, List<TrackedActivityModel> data) {

            cachedActivities = getTrackedActivityListItems(data);

            Log.d(LOG_TAG, "onLoadFinished " + loader);

            if (activities != null) {
                activities.setItems(cachedActivities);
            }
            // The list should now be shown.
            // FIXME hide custom progress indicator
        }


        @Override
        public void onLoaderReset(Loader<List<TrackedActivityModel>> loader) {
            cachedActivities = null;
            Log.d(LOG_TAG, "onLoaderReset " + loader);
            if (activities != null) {
                activities.setItems(null);
            }
        }

    }

    static int getColorCode(CompoundTimeSpanningViewModel<?> item, Resources resources, int fallback) {
        final Integer colorCode = item.getColorCode(resources);
        return colorCode == null ? resources.getColor(fallback) : colorCode.intValue();
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.agime_main_menu_overview_fragment, menu);
        if (!Workarounds.nestedFragmentMenuInitializedByParent() && (_mode == Mode.DAY)) {
            inflateMenu(menu, inflater);
        }
    }

    public static void inflateMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.agime_main_menu_day_fragment, menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_add:
                trackTime();
                return true;
            case R.id.action_export_csv:
                final String title = "Aktivitäten " + DateUtils.formatDateTime(getActivity(), getStarttimeInclusiveMillis(), DateUtils.FORMAT_SHOW_DATE)+ " - " + DateUtils.formatDateTime(getActivity(), getEndtimeExclusiveMillis(), DateUtils.FORMAT_SHOW_DATE) +  ".csv";

                if (Build.VERSION.SDK_INT >= 19) {
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                            .addCategory(Intent.CATEGORY_OPENABLE)
                            .setType(CSVFileReaderWriter.V2FileFormat.MIME_TYPE)
                            .putExtra(Intent.EXTRA_TITLE, title);

                    startActivityForResult(intent, REQUEST_CODE_CHOOSE_DIRECTORY);
                } else {
                    final Level1OverviewLoadingListAdapter listAdapter = _views.getListAdapter();
                    final ExportCSVInput input = new ExportCSVInput(listAdapter.getConfiguration(), listAdapter.getActivities());
                    new ExportCSVTaskSupport(getActivity(), title).execute(input);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CHOOSE_DIRECTORY && resultCode == Activity.RESULT_OK) {
            final Level1OverviewLoadingListAdapter listAdapter = _views.getListAdapter();
            final ExportCSVInput input = new ExportCSVInput(data.getData(), listAdapter.getConfiguration(), listAdapter.getActivities());
            new ExportCSVTask(getActivity()).setUseProgressDialog(true).execute(input);
        }
    }

    private void trackTime() {
        Intent intent = new Intent(Intent.ACTION_INSERT, MCContract.Activity.CONTENT_URI);
        intent.setClass(getActivity(), TrackActivity.class);
        intent.putExtra(TrackActivity.EXTRA_DAY_MILLIS, getStarttimeInclusiveMillis());

        startActivityForResult(intent, ACTIVITY_CODE_TRACK_TIME);
    }

    private static class CollapseListener implements ExpandableListView.OnGroupCollapseListener, ExpandableListView.OnGroupExpandListener {
        private final Context _context;
        private final Level1OverviewLoadingListAdapter adapter;

        public CollapseListener(Context context, Level1OverviewLoadingListAdapter adapter) {
            _context = context;
            this.adapter = adapter;
        }

        @Override
        public void onGroupCollapse(int groupPosition) {
            onGroupCollapse(groupPosition, true);
        }

        public void onGroupCollapse(int groupPosition, boolean v) {
            Level1OverviewGroup group = adapter.getGroup(groupPosition);
            Preferences.setLevel1Collapsed(_context, group.getGroupTypeId(), group.getId(), v);
        }

        @Override
        public void onGroupExpand(int groupPosition) {
            onGroupCollapse(groupPosition, false);
        }
    }

    void expandStates(ExpandableListView lv) {
        final ExpandableListAdapter adapter = lv.getExpandableListAdapter();
        final int groupCount = adapter.getGroupCount();
        if (groupCount == 1) {
            // if there is only one group, it should always be expanded
            lv.expandGroup(0);
            return;
        }
        for (int i = 0; i < groupCount; i++) {
            Level1OverviewGroup group = (Level1OverviewGroup)checkNotNull(adapter.getGroup(i));
            boolean collapsed = Preferences.isLevel1Collapsed(getActivity(), group.getGroupTypeId(), group.getId());
            if (!collapsed) {
                lv.expandGroup(i);
            } else {
                lv.collapseGroup(i);
            }
        }
    }

    public static final class ActivitiesOverviewsWrappedView extends BaseViewWrapper {
        static final int LAYOUT = R.layout.activites_overview_list_fragment;
        static final int ID_LIST = android.R.id.list;
        static final int ID_EMPTY = android.R.id.empty;
        static final int ID_EMPTY_TEXT = R.id.empty_text;
        static final int ID_LOADING = R.id.loadingPanel;

        final ExpandableListView list;
        final TextView emptyText;
        final View empty;
        final View loading;

        ActivitiesOverviewsWrappedView(View view) {
            super(view);
            list = get(ExpandableListView.class, ID_LIST);
            emptyText = getTextView(ID_EMPTY_TEXT);
            empty = getView(ID_EMPTY);
            loading = getView(ID_LOADING);

            // view configuration that was not done in the layout XML
            list.setDivider(null);
            list.setChildDivider(null);
            list.setGroupIndicator(null);
            list.setChildIndicator(null);
            list.setDividerHeight(0);
        }

        Level1OverviewLoadingListAdapter getListAdapter() {
            return (Level1OverviewLoadingListAdapter)list.getExpandableListAdapter();
        }

        public void setListAdapter(Context contxt, Level1OverviewLoadingListAdapter adapter) {
            CollapseListener listener = new CollapseListener(contxt, adapter);
            list.setOnGroupCollapseListener(listener);
            list.setOnGroupExpandListener(listener);
            list.setOnGroupClickListener(adapter);
            list.setAdapter(adapter);
        }

    }

}
