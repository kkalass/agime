package de.kalass.agime.overview;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import org.joda.time.LocalDate;

import java.io.File;
import java.io.IOException;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import de.kalass.agime.AbstractViewPagerFragment;
import de.kalass.agime.AgimeMainActivity;
import de.kalass.agime.LocalDateSpanning;
import de.kalass.agime.PageChangeActivityTitleSync;
import de.kalass.agime.R;
import de.kalass.agime.ResizableToolbarHelper;
import de.kalass.agime.Workarounds;
import de.kalass.agime.backup.CSVFileReaderWriter;
import de.kalass.agime.customfield.CustomFieldTypeModel;
import de.kalass.agime.customfield.CustomFieldTypeModelQuery;
import de.kalass.agime.customfield.CustomFieldTypeProjectsQuery;
import de.kalass.agime.loader.TrackedActivitySyncLoader;
import de.kalass.agime.model.ProjectModel;
import de.kalass.agime.model.TrackedActivityModel;
import de.kalass.agime.overview.model.GroupHeader;
import de.kalass.agime.overview.model.GroupHeaderType;
import de.kalass.agime.overview.model.GroupHeaderTypes;
import de.kalass.agime.overview.model.OverviewConfiguration;
import de.kalass.agime.settings.Preferences;
import de.kalass.android.common.AbstractAsyncTask;
import de.kalass.android.common.activity.BaseListAdapter;
import de.kalass.android.common.activity.BaseViewWrapper;
import de.kalass.android.common.activity.ContentResolverUtil;
import de.kalass.android.common.simpleloader.CompoundAsyncLoader;
import de.kalass.android.common.simpleloader.ObserveDataSourceMode;
import de.kalass.android.common.support.datetime.DatePickerSupport;
import de.kalass.android.common.util.StringUtil;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by klas on 06.10.13.
 */
public abstract class AbstractAgimeOverviewFragment extends AbstractViewPagerFragment
        implements LocalDateSpanning,
        DatePickerSupport.LocalDateSelectedListener,
        LoaderManager.LoaderCallbacks<AbstractAgimeOverviewFragment.ActivitiesOverviewsConfigurationData>,
        AdapterView.OnItemSelectedListener, ResizableToolbarHelper.ToolbarResizeCallback, View.OnClickListener {
    public static final int LOADER_ID_CONFIGURATION_DATA = 2;
    private static final String LOG_TAG = "AbstractAgimeOverview";
    private WrappedView _views;
    private ActivitiesOverviewsConfigurationData _data;
    private OverviewConfiguration config;
    private ProjectDependendTrackedActivityModelPredicate predicate;
    private View _customToolbar;
    private CustomToolbarWrappedView _customToolbarViews;

    protected AbstractAgimeOverviewFragment() {
        super(WrappedView.LAYOUT, WrappedView.ID_PAGER);
    }

    @Override
    public LocalDate getInitialDate() {
        long millis = getArguments().getLong(ARG_INITIAL_DATE_MILLIS);
        return new LocalDate(millis);
    }

    @Override
    public void adjustCustomToolbarHeight(int initialHeight, int currentHeight, int targetHeight) {

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == CustomToolbarWrappedView.ID_HEADING_TEXT || v.getId() == CustomToolbarWrappedView.ID_HEADING_TEXT_ICON) {
            onChangeDayClicked();
        }
    }

    public void onChangeDayClicked() {
        DatePickerSupport.showDatePickerDialog(
                getActivity(),
                getFragmentManager(),
                R.id.action_go_to_today,
                this, getStartDate());
    }



    public static final class WrappedView {
        static final int LAYOUT = R.layout.agime_overview_fragment;

        static final int ID_PAGER = R.id.pager;

        final ViewPager viewPager;

        WrappedView(View view) {

            viewPager = checkNotNull((ViewPager)view.findViewById(ID_PAGER));
        }

        public void setOnPageChangeListener(ViewPager.OnPageChangeListener listener) {
            viewPager.setOnPageChangeListener(listener);
        }

    }

    public static final class CustomToolbarWrappedView extends BaseViewWrapper {
        static final int LAYOUT = R.layout.agime_overview_fragment_custom_toolbar;

        static final int ID_HEADING_TEXT = R.id.heading_text;
        static final int ID_HEADING_TEXT_ICON = R.id.heading_text_icon;
        static final int ID_HEADING = R.id.heading;

        static final int ID_LEVEL1_GROUP_HEADER_SPINNER = R.id.level1_selector;
        static final int ID_LEVEL2_GROUP_HEADER_SPINNER = R.id.level2_selector;
        static final int ID_LEVEL3_GROUP_HEADER_SPINNER = R.id.level3_selector;

        final Spinner level1Spinner;
        final Spinner level2Spinner;
        final Spinner level3Spinner;
        final TextView headingText;
        final View heading;
        final View headingTextIcon;

        CustomToolbarWrappedView(View view) {
            super(view);
            level1Spinner = checkNotNull((Spinner)view.findViewById(ID_LEVEL1_GROUP_HEADER_SPINNER));
            level2Spinner = checkNotNull((Spinner)view.findViewById(ID_LEVEL2_GROUP_HEADER_SPINNER));
            level3Spinner = checkNotNull((Spinner)view.findViewById(ID_LEVEL3_GROUP_HEADER_SPINNER));
            headingText = getTextView(ID_HEADING_TEXT);
            heading = getView(ID_HEADING);
            headingTextIcon = getView(ID_HEADING_TEXT_ICON);
        }

        public void setLevel1GroupTypesAdapter(GroupTypesAdapter groupTypesAdapter) {
            level1Spinner.setAdapter(groupTypesAdapter);
        }

        public void setLevel2GroupTypesAdapter(GroupTypesAdapter groupTypesAdapter) {
            level2Spinner.setAdapter(groupTypesAdapter);
        }

        public void setLevel3GroupTypesAdapter(GroupTypesAdapter groupTypesAdapter) {
            level3Spinner.setAdapter(groupTypesAdapter);
        }

        public GroupTypesAdapter getLevel1GroupTypesAdapter() {
            return (GroupTypesAdapter)level1Spinner.getAdapter();
        }

        public GroupTypesAdapter getLevel2GroupTypesAdapter() {
            return (GroupTypesAdapter)level2Spinner.getAdapter();
        }

        public GroupTypesAdapter getLevel3GroupTypesAdapter() {
            return (GroupTypesAdapter)level3Spinner.getAdapter();
        }

        public void setGroupTypesListener(AdapterView.OnItemSelectedListener listener) {
            level1Spinner.setOnItemSelectedListener(listener);
            level2Spinner.setOnItemSelectedListener(listener);
            level3Spinner.setOnItemSelectedListener(listener);
        }
    }

    private PageChangeActivityTitleSync _changeListener = new PageChangeActivityTitleSync(this) {
        @Override
        public void onPageSelected(int position) {
            updateHeadingText();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        _customToolbar = inflater.inflate(R.layout.agime_overview_fragment_custom_toolbar, container, false);
        ((AgimeMainActivity)getActivity()).setCustomToolbar(_customToolbar, this, getResources().getDimensionPixelSize(R.dimen.custom_toolbar_height_overview_fragment));
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);

        // configure views
        _views = new WrappedView(getView());
        _customToolbarViews = new CustomToolbarWrappedView(_customToolbar);
        _customToolbarViews.setLevel1GroupTypesAdapter(new GroupTypesAdapter(getActivity()));
        _customToolbarViews.setLevel2GroupTypesAdapter(new GroupTypesAdapter(getActivity()));
        _customToolbarViews.setLevel3GroupTypesAdapter(new GroupTypesAdapter(getActivity()));

        _views.setOnPageChangeListener(_changeListener);
        _customToolbarViews.headingText.setOnClickListener(this);
        _customToolbarViews.headingTextIcon.setOnClickListener(this);
        _changeListener.onPageSelected(getViewPager().getCurrentItem());


        // Kickoff data loading
        getLoaderManager().initLoader(LOADER_ID_CONFIGURATION_DATA, null, this);
        updateHeadingText();
    }

    protected void updateHeadingText() {
        _customToolbarViews.headingText.setText(_views.viewPager.getAdapter().getPageTitle(_views.viewPager.getCurrentItem()));
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (getView() != null) {
            _changeListener.onPageSelected(getViewPager().getCurrentItem());
        }
    }

    private ViewPager getViewPager() {
        View vp = getView();
        return (ViewPager)vp.findViewById(R.id.pager);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        final int viewId = parent.getId();
        switch (viewId) {
            case CustomToolbarWrappedView.ID_LEVEL1_GROUP_HEADER_SPINNER:
                Preferences.setLevel1GroupTypeId(getActivity(), (int)id);
                updateOverviewConfiguration();
                return;
            case CustomToolbarWrappedView.ID_LEVEL2_GROUP_HEADER_SPINNER:
                Preferences.setLevel2GroupTypeId(getActivity(), (int) id);
                updateOverviewConfiguration();
                return;
            case CustomToolbarWrappedView.ID_LEVEL3_GROUP_HEADER_SPINNER:
                Preferences.setLevel3GroupTypeId(getActivity(), (int) id);
                updateOverviewConfiguration();
                return;
        }
    }

    private GroupHeaderTypes.CustomFieldType getToplevelCustomFieldTypeHeader(GroupHeaderType level1Header, GroupHeaderType level2Header) {
        if ((level1Header instanceof  GroupHeaderTypes.Any)
                && (level2Header instanceof GroupHeaderTypes.CustomFieldType)) {
            return (GroupHeaderTypes.CustomFieldType)level2Header;
        }
        if (level1Header instanceof GroupHeaderTypes.CustomFieldType) {
            return (GroupHeaderTypes.CustomFieldType)level1Header;
        }
        return null;
    }

    protected void updateOverviewConfiguration() {
        Log.d(LOG_TAG, "update overview configuration");
        // the configuration of how the overview should be build was changed
        final GroupHeaderType level1Header = (GroupHeaderType) _customToolbarViews.level1Spinner.getSelectedItem();
        final GroupHeaderType level2Header = (GroupHeaderType) _customToolbarViews.level2Spinner.getSelectedItem();
        final GroupHeaderType level3Header = (GroupHeaderType) _customToolbarViews.level3Spinner.getSelectedItem();

        // If the user selected "any" on first level, but a custom type on second level
        // which is available on selected projects only, restrict the activities
        // to activities of the selected projects - the same if he selects a custom field for the toplevel
        GroupHeaderTypes.CustomFieldType toplevelCustomFieldTypeHeader = getToplevelCustomFieldTypeHeader(level1Header, level2Header);
        if (toplevelCustomFieldTypeHeader != null) {

            final CustomFieldTypeModel typeModel = toplevelCustomFieldTypeHeader.getTypeModel();
            if (!typeModel.isAnyProject()) {
                final OverviewConfiguration config;
                if (level1Header instanceof GroupHeaderTypes.Any) {
                    // restrict the selection,
                    final GroupHeaderTypes.Any level1HeaderReplacement = new GroupHeaderTypes.Any(
                            level1Header.getTitle(),
                            StringUtil.formatOptional(
                                    getString(R.string.overview_level1_title_any_restricted_custom_field),
                                    level2Header.getTitle())
                    );
                    config = createOverviewConfiguration(
                            /*Use an adapater that does not contain the header we are going to replace*/
                            _customToolbarViews.getLevel2GroupTypesAdapter(),
                            level1HeaderReplacement, level2Header, level3Header
                     );
                } else {
                    config = createOverviewConfiguration(
                            _customToolbarViews.getLevel1GroupTypesAdapter(),
                            level1Header, level2Header, level3Header
                    );
                }
                Long typeId = typeModel.getId();
                final Collection<Long> projectIds = _data.enabledProjectsByCustomType.get(typeId);
                setOverviewConfiguration(config, new ProjectDependendTrackedActivityModelPredicate(projectIds));
                return;
            }
        }
        final OverviewConfiguration config = createOverviewConfiguration(
                _customToolbarViews.getLevel1GroupTypesAdapter(),
                level1Header, level2Header, level3Header
        );
        setOverviewConfiguration(config, null);
    }

    private void setOverviewConfiguration(OverviewConfiguration config,
                                          ProjectDependendTrackedActivityModelPredicate predicate) {

        //ActivitiesOverviewListFragment lf;
        //lf.setOverviewConfiguration(config, predicate);
        this.config = config;
        this.predicate = predicate;
        for (Fragment f : getChildFragmentManager().getFragments()) {
            //Log.i("*********", "Fragment " + f);
            if (f instanceof ActivitiesOverviewListFragment) {
                ((ActivitiesOverviewListFragment) f).setOverviewConfiguration(config, predicate);
            }

        }
    }

    protected void prepareFragment(ActivitiesOverviewListFragment currentListFragment) {
        currentListFragment.setOverviewConfiguration(config, predicate);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // ignore
    }

    @Override
    public Loader<ActivitiesOverviewsConfigurationData> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_CONFIGURATION_DATA:
                return new ActivitiesOverviewConfigurationDataLoader(getActivity());
        }
        return null;
    }

    protected List<GroupHeaderType> createGroupHeaderTypes(List<CustomFieldTypeModel> models) {
        return createGroupHeaderTypes(models, Predicates.<GroupHeaderType>alwaysTrue());
    }

    protected List<GroupHeaderType> createGroupHeaderTypes(List<CustomFieldTypeModel> models, Predicate<GroupHeaderType> predicate) {
        List<GroupHeaderType> types = new ArrayList<GroupHeaderType>(models.size() + 3);

        add(types, predicate, new GroupHeaderTypes.Any(getString(R.string.overview_level1_title_any_dropdown_value), getString(R.string.overview_level1_title_any)));

        add(types, predicate, new GroupHeaderTypes.Project(getString(R.string.overview_group_types_project)));
        add(types, predicate, new GroupHeaderTypes.Category(getString(R.string.overview_group_types_category)));
        add(types, predicate, new GroupHeaderTypes.ActivityType(getString(R.string.overview_group_types_activity_type)));
        add(types, predicate, new GroupHeaderTypes.ByDay(getActivity(), getString(R.string.overview_group_types_by_day)));
        add(types, predicate, new GroupHeaderTypes.ByWeek(getActivity(), getString(R.string.overview_group_types_by_week)));
        add(types, predicate, new GroupHeaderTypes.ByMonth(getActivity(), getString(R.string.overview_group_types_by_month)));
        add(types, predicate, new GroupHeaderTypes.ByYear(getActivity(), getString(R.string.overview_group_types_by_year)));
        for (CustomFieldTypeModel model : models) {
            add(types, predicate, new GroupHeaderTypes.CustomFieldType(model));
        }
        return types;
    }

    private void add(List<GroupHeaderType> types, Predicate<GroupHeaderType> predicate, GroupHeaderType type) {
        if (predicate.apply(type)) {
            types.add(type);
        }
    }

    protected OverviewConfiguration createOverviewConfiguration(
            GroupTypesAdapter adapter,
            GroupHeaderType level1GroupHeader,
            GroupHeaderType level2GroupHeader,
            GroupHeaderType level3GroupHeader
    ) {

        List<GroupHeaderType> remaining = new ArrayList<GroupHeaderType>();
        for (int i = 0; i < adapter.getCount(); i++) {
            final GroupHeaderType item = adapter.getItem(i);
            if (!item.equals(level1GroupHeader) && !item.equals(level2GroupHeader) && !item.equals(level3GroupHeader)) {
                remaining.add(item);
            }
        }
        return new OverviewConfiguration(
                level1GroupHeader,
                level2GroupHeader,
                level3GroupHeader,
                remaining,
                true
        );
    }

    @Override
    public void onLoaderReset(Loader<ActivitiesOverviewsConfigurationData> loader) {
        switch (loader.getId()) {
            case LOADER_ID_CONFIGURATION_DATA:
                // free
                _data = null;
                return;
        }
    }


    private int getGroupIdPosition(List<GroupHeaderType> models, int groupTypeId, int defaultPos) {
        for (int i = 0; i < models.size(); i++) {
            GroupHeaderType m = models.get(i);
            if (m.getGroupTypeId() == groupTypeId) {
                return i;
            }
        }
        return defaultPos;
    }


    static final class GroupTypesAdapter extends BaseListAdapter<GroupHeaderTypeWrappedView, GroupHeaderType> {

        public GroupTypesAdapter(Context context) {
            super(context, GroupHeaderTypeWrappedView.LAYOUT, GroupHeaderTypeWrappedView.LAYOUT_DROP_DOWN_ITEM);
        }

        @Override
        protected GroupHeaderTypeWrappedView onWrapView(View view) {
            return new GroupHeaderTypeWrappedView(view);
        }

        @Override
        public void bindWrappedView(GroupHeaderTypeWrappedView view, GroupHeaderType item, int position) {
            view.name.setText(item.getTitle());
        }
    }

    static final class GroupHeaderTypeWrappedView {
        static final int LAYOUT = R.layout.activities_overview_group_spinner_item;
        static final int LAYOUT_DROP_DOWN_ITEM = android.R.layout.simple_list_item_1;
        static final int ID_TYPE_NAME = android.R.id.text1;

        final TextView name;
        GroupHeaderTypeWrappedView(View view) {
            name = checkNotNull((TextView)view.findViewById(ID_TYPE_NAME));
        }
    }

    private static class ActivitiesOverviewConfigurationDataLoader extends CompoundAsyncLoader<ActivitiesOverviewsConfigurationData> {
        public ActivitiesOverviewConfigurationDataLoader(Context context) {
            super(context, ObserveDataSourceMode.RELOAD_ON_CHANGES);
        }

        @Override
        public ActivitiesOverviewsConfigurationData doLoadInBackground() {
            final List<CustomFieldTypeModel> customFieldTypes = loadList(
                    CustomFieldTypeModelQuery.READ,
                    CustomFieldTypeModelQuery.CONTENT_URI,
                    CustomFieldTypeModelQuery.PROJECTION,
                    null, null, null);

            final Multimap<Long, Long> enabledProjectsByCustomType = loadMultimap(
                    CustomFieldTypeProjectsQuery.READ_TYPE_ID,
                    CustomFieldTypeProjectsQuery.READ_PROJECT_ID,
                    CustomFieldTypeProjectsQuery.CONTENT_URI, CustomFieldTypeProjectsQuery.PROJECTION,
                    null, null, null
            );
            return new ActivitiesOverviewsConfigurationData(customFieldTypes, enabledProjectsByCustomType);
        }
    }
    @Override
    public void onLoadFinished(Loader<ActivitiesOverviewsConfigurationData> loader, ActivitiesOverviewsConfigurationData data) {
        switch (loader.getId()) {
            case LOADER_ID_CONFIGURATION_DATA:
                _data = data;
                final List<GroupHeaderType> groupHeaderTypes = createGroupHeaderTypes(_data.customFieldTypes);
                _customToolbarViews.setGroupTypesListener(null); // avoid firing too early

                final List<GroupHeaderType> level2Items = groupHeaderTypes.subList(1, groupHeaderTypes.size());
                final List<GroupHeaderType> level3Items = groupHeaderTypes.subList(1, groupHeaderTypes.size());

                _customToolbarViews.getLevel1GroupTypesAdapter().setItems(groupHeaderTypes);
                _customToolbarViews.getLevel2GroupTypesAdapter().setItems(level2Items);
                _customToolbarViews.getLevel3GroupTypesAdapter().setItems(level3Items);

                _customToolbarViews.level1Spinner.setSelection(
                        getGroupIdPosition(groupHeaderTypes, Preferences.getLevel1GroupTypeId(getActivity()), 0)
                );
                _customToolbarViews.level2Spinner.setSelection(
                        Math.min(level2Items.size() - 1, getGroupIdPosition(level2Items, Preferences.getLevel2GroupTypeId(getActivity()), 1))
                );
                _customToolbarViews.level3Spinner.setSelection(
                        Math.min(level3Items.size() - 1, getGroupIdPosition(level3Items, Preferences.getLevel3GroupTypeId(getActivity()), 2))
                );
                _customToolbarViews.setGroupTypesListener(this); // let the next line trigger creation of the overview configuration
                updateOverviewConfiguration();
                return;
        }
    }


    public static final class ActivitiesOverviewsConfigurationData {
        final List<CustomFieldTypeModel> customFieldTypes;
        final Multimap<Long, Long> enabledProjectsByCustomType;

        public ActivitiesOverviewsConfigurationData(List<CustomFieldTypeModel> customFieldTypes,
                                                    Multimap<Long, Long> enabledProjectsByCustomType) {
            this.customFieldTypes = customFieldTypes;
            this.enabledProjectsByCustomType = enabledProjectsByCustomType;
        }
    }

    private static class ProjectDependendTrackedActivityModelPredicate implements Predicate<TrackedActivityModel> {
        private final Collection<Long> projectIds;

        public ProjectDependendTrackedActivityModelPredicate(Collection<Long> projectIds) {
            this.projectIds = projectIds == null ? ImmutableSet.<Long>of() : projectIds;
        }

        @Override
        public boolean apply(TrackedActivityModel trackedActivityModel) {
            final ProjectModel project = trackedActivityModel.getProject();
            return project != null && projectIds.contains(project.getId());
        }
    }

}
