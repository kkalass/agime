package de.kalass.agime.customfield;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.linearlistview.LinearListView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.kalass.agime.ColorSuggestion;
import de.kalass.agime.R;
import de.kalass.agime.analytics.AnalyticsBaseCRUDFragment;
import de.kalass.agime.loader.ProjectSyncLoader;
import de.kalass.agime.model.ProjectModel;
import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.activity.BaseListAdapter;
import de.kalass.android.common.activity.CRUDMode;
import de.kalass.android.common.activity.ContentResolverUtil;
import de.kalass.android.common.model.IViewModel;
import de.kalass.android.common.simpleloader.AbstractLoader;
import de.kalass.android.common.simpleloader.CompoundAsyncLoader;
import de.kalass.android.common.simpleloader.CursorFkt;
import de.kalass.android.common.simpleloader.CursorUtil;
import de.kalass.android.common.simpleloader.ObserveDataSourceMode;
import de.kalass.android.common.simpleloader.ValueOrReference;
import de.kalass.android.common.util.StringUtil;
import de.kalass.android.common.util.TimeFormatUtil;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.kalass.android.common.simpleloader.CursorUtil.getIndex;

/**
 * The editor fragment of a custom field type.
 */
public class CustomFieldTypeEditorFragment
        extends AnalyticsBaseCRUDFragment<CustomFieldTypeEditorFragment.WrappedView, CustomFieldTypeEditorFragment.CustomFieldTypeEditorData> implements CompoundButton.OnCheckedChangeListener {


    public CustomFieldTypeEditorFragment() {
        super(WrappedView.LAYOUT,
                Content.CONTENT_TYPE_DIR,
                Content.CONTENT_TYPE_ITEM
        );
    }

    @Override
    protected CRUDMode getMode() {
        // we currently do not support a real view mode
        CRUDMode requestedMode = super.getMode();
        return requestedMode == CRUDMode.VIEW ? CRUDMode.EDIT : requestedMode;
    }


    @Override
    public AbstractLoader<CustomFieldTypeEditorData> createLoader(int id, Bundle args) {
        return new CustomFieldTypeEditorDataLoader(
                getContext(),
                // do not reload while the user tries to edit data
                ObserveDataSourceMode.IGNORE_CHANGES,
                getMode(), getEntityId(), getUri()
        );
    }


    @Override
    protected WrappedView onWrapView(View view) {
        return new WrappedView(view);
    }

    @Override
    protected void onBindView(WrappedView view, CustomFieldTypeEditorData data) {
        Preconditions.checkArgument(getMode() == CRUDMode.INSERT || data.data != null, "Only new items do not provide data");
        String name = data.getName();
        boolean anyProject = data.isAnyProject();

        view.name.setText(name);
        final boolean projectDependendInvisible = data.projectSelectedItems.isEmpty() && data.isAnyProject();
        view.projectDependendSwitch.setVisibility(projectDependendInvisible ? View.GONE: View.VISIBLE);
        view.projectDependendSwitch.setChecked(!anyProject);
        view.setProjectDependendContentState(!anyProject);


        final CustomFieldTypeProjectListAdapter adapter = new CustomFieldTypeProjectListAdapter(getContext(), data.projectSelectedItems);
        view.projectsList.setAdapter(adapter);


        view.projectDependendSwitch.setOnCheckedChangeListener(this);

    }

    @Override
    protected void save() {
        final WrappedView view = getWrappedView();
        CustomFieldTypeProjectListAdapter projectListAdapter = (CustomFieldTypeProjectListAdapter)view.projectsList.getAdapter();

        // Create an operation for the custom field type
        ContentValues values = new ContentValues();
        values.put(Content.COLUMN_NAME_NAME, view.name.getText().toString());
        CursorUtil.putBoolean(values, Content.COLUMN_NAME_ANY_PROJECT, !view.projectDependendSwitch.isChecked());

        // create operations for the association between the custom field type and the projects
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>(1 + projectListAdapter.getCount());
        ContentProviderOperation mainOp = createSaveOrUpdateOperation(getMode(), values, System.currentTimeMillis());
        int mainOpIdx = ops.size();
        ops.add(mainOp);

        for (int i = 0; i < projectListAdapter.getCount(); i++) {
            final ProjectSelectedItem item = projectListAdapter.getItem(i);
            // FIXME  ensure that original state still is valid
            /*
            ContentProviderOperation.Builder builder = ContentProviderOperation.newAssertQuery(ProjectM2MCustomFieldTypeQuery.CONTENT_URI);
            appendProjectTypeNaturalKey(builder, item.project.getId(), mainOpIdx);
            ops.add(builder.withExpectedCount(item.persistedSelected ? 1 : 0).build());
*/
            if (!item.persistedSelected && item.selected) {
                final ValueOrReference mainOpRef = getMode() == CRUDMode.INSERT
                        ? ValueOrReference.ofReference(mainOpIdx)
                        : ValueOrReference.ofValue(checkNotNull(getEntityId()));
                final ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(ProjectM2MCustomFieldTypeQuery.CONTENT_URI);
                builder.withValue(ProjectM2MCustomFieldTypeQuery.COLUMN_NAME_PROJECT_ID, item.project.getId());
                mainOpRef.appendSelf(builder, ProjectM2MCustomFieldTypeQuery.COLUMN_NAME_TYPE_ID);
                ops.add(builder.build());
            } else if (item.persistedSelected && !item.selected) {
                Preconditions.checkState(getMode() != CRUDMode.INSERT, "How can the item be persisted as selected, but in insert mode???");
                final ContentProviderOperation.Builder builder = ContentProviderOperation.newDelete(ProjectM2MCustomFieldTypeQuery.CONTENT_URI);
                builder.withSelection(
                        ProjectM2MCustomFieldTypeQuery.COLUMN_NAME_PROJECT_ID + " = ? AND " + ProjectM2MCustomFieldTypeQuery.COLUMN_NAME_TYPE_ID + " = ? ",
                        new String[]{Long.toString(item.project.getId()), getEntityId().toString()});
                ops.add(builder.withExpectedCount(1).build());
            }
        }
        ContentProviderOperation[] opsArray = ops.toArray(new ContentProviderOperation[ops.size()]);
        performSaveOrUpdateAsync(mainOpIdx, opsArray);
    }



    @Override
    protected void delete() {
        assertCanDelete();
        String name = getWrappedView().name.getText().toString();
        CustomFieldTypeEditorDBUtil.delete(getContext(), this, name, ImmutableList.of(getEntityId()));
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case WrappedView.ID_PROJECT_DEPENDEND_SWITCH:

                final WrappedView wrappedView = getWrappedView();
                if (isChecked) {
                    CustomFieldTypeProjectListAdapter adapter = (CustomFieldTypeProjectListAdapter)wrappedView.projectsList.getAdapter();
                    for (int i = 0; i < adapter.getCount(); i++) {
                        final ProjectSelectedItem item = adapter.getItem(i);
                        item.selected = true;
                    }
                    adapter.notifyDataSetChanged();
                }
                wrappedView.setProjectDependendContentState(isChecked);
                return;
        }
    }

    static final class CustomFieldTypeProjectListAdapter
            extends BaseListAdapter<ProjectWrappedView, ProjectSelectedItem> implements CompoundButton.OnCheckedChangeListener {
        CustomFieldTypeProjectListAdapter(Context context, List<ProjectSelectedItem> data) {
            super(context, ProjectWrappedView.LAYOUT);
            setItems(data);
        }

        @Override
        protected ProjectWrappedView onWrapView(View view) {
            return new ProjectWrappedView(view);
        }

        @Override
        public void bindWrappedView(ProjectWrappedView view, ProjectSelectedItem model, int position) {
            String name = model.project.isInactive() ?  model.project.getName() + " [" + getContext().getString(R.string.inactive) + "]":  model.project.getName();

            view.name.setText(name);
            view.name.setTextColor(getContext().getResources().getColor(model.project.isInactive() ? R.color.material_black_text_disabled : R.color.material_black_text));
            view.color.setBackgroundColor(ColorSuggestion.getProjectColor(getContext().getResources(), model.project));
            view.checkBox.setChecked(model.selected);

            view.checkBox.setTag(position);
            view.checkBox.setOnCheckedChangeListener(this);
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            int position = (Integer)buttonView.getTag();
            final ProjectSelectedItem item = getItem(position);
            item.selected = isChecked;
        }
    }

    static final class ProjectSelectedItem implements IViewModel {
        final ProjectModel project;
        final boolean persistedSelected;
        boolean selected;

        ProjectSelectedItem(ProjectModel project, boolean selected) {
            this.project = project;
            this.persistedSelected = selected;
            this.selected = selected;
        }

        @Override
        public long getId() {
            return project.getId();
        }
    }

    static final class ProjectWrappedView {
        static final int LAYOUT = R.layout.fragment_custom_field_type_editor_project_item;
        static final int ID_PROJECT_NAME_FIELD = R.id.project_name;
        static final int ID_PROJECT_COLOR_FIELD = R.id.project_color;
        static final int ID_PROJECT_CHECKBOX = R.id.checkBox;

        final TextView name;
        final View color;
        final CheckBox checkBox;

        ProjectWrappedView(View view) {
            name = (TextView)checkNotNull(view.findViewById(ID_PROJECT_NAME_FIELD));
            color = checkNotNull(view.findViewById(ID_PROJECT_COLOR_FIELD));
            checkBox = (CheckBox)checkNotNull(view.findViewById(ID_PROJECT_CHECKBOX));
        }
    }

    static final class WrappedView {
        static final int LAYOUT = R.layout.fragment_custom_field_type_editor;
        static final int ID_NAME_FIELD = R.id.name;
        static final int ID_PROJECT_DEPENDEND_SWITCH = R.id.status_switch;
        static final int ID_PROJECTS_LIST = R.id.list;
        static final int ID_PROJECT_DEPENDEND_CONTENT = R.id.project_dependend;

        final EditText name;
        final SwitchCompat projectDependendSwitch;
        final LinearListView projectsList;
        final View projectDependendContent;

        WrappedView(View view) {
            name = checkNotNull((EditText) view.findViewById(ID_NAME_FIELD));
            projectDependendSwitch = checkNotNull((SwitchCompat)view.findViewById(ID_PROJECT_DEPENDEND_SWITCH));
            projectsList = checkNotNull((LinearListView)view.findViewById(ID_PROJECTS_LIST));
            projectDependendContent = checkNotNull(view.findViewById(ID_PROJECT_DEPENDEND_CONTENT));
        }

        public void setProjectDependendContentState(boolean value) {
            projectDependendContent.setVisibility(value ? View.VISIBLE : View.GONE);
        }
    }

    public static final class Content {
        public static final String CONTENT_TYPE_DIR = MCContract.CustomFieldType.CONTENT_TYPE_DIR;
        public static final String CONTENT_TYPE_ITEM = MCContract.CustomFieldType.CONTENT_TYPE_ITEM;
        public static final String COLUMN_NAME_NAME = MCContract.CustomFieldType.COLUMN_NAME_NAME;
        public static final String COLUMN_NAME_ANY_PROJECT = MCContract.CustomFieldType.COLUMN_NAME_ANY_PROJECT;
        public static final String[] PROJECTION = new String[] {
                MCContract.CustomFieldType._ID,
                COLUMN_NAME_NAME,
                COLUMN_NAME_ANY_PROJECT
        };
        public static final int IDX_NAME = getIndex(PROJECTION, COLUMN_NAME_NAME);
        public static final int IDX_ANY_PROJECT = getIndex(PROJECTION, COLUMN_NAME_ANY_PROJECT);

        public static final Function<Cursor, Data> READER = new Function<Cursor, Data>() {
            @Override
            public Data apply(Cursor cursor) {
                boolean anyProject = CursorUtil.getBoolean(cursor, IDX_ANY_PROJECT);
                return new Data(cursor.getString(IDX_NAME), anyProject);
            }
        };

        public static final class Data {
            final String name;
            final boolean anyProject;

            public Data(String name, boolean anyProject) {
                this.name = name;
                this.anyProject = anyProject;
            }

        }

    }

    public static class CustomFieldTypeEditorData {
        private final Content.Data data;
        private final List<ProjectSelectedItem> projectSelectedItems;

        public CustomFieldTypeEditorData(Content.Data data, List<ProjectSelectedItem> projectSelectedItems) {
            this.data = data;
            this.projectSelectedItems = projectSelectedItems;
        }

        public String getName() {
            return data == null ? null : data.name;
        }

        public boolean isAnyProject() {
            return data == null || data.anyProject;
        }
    }

    private static class CustomFieldTypeEditorDataLoader extends CompoundAsyncLoader<CustomFieldTypeEditorData> {
        private final CRUDMode _mode;
        private final Long _entityId;
        private final Uri _uri;
        private ProjectSyncLoader _projectLoader;

        public CustomFieldTypeEditorDataLoader(
                Context context,
                ObserveDataSourceMode observeDataSourceMode,
                CRUDMode mode, Long entityId, Uri uri
        ) {
            this(context, observeDataSourceMode, new ProjectSyncLoader(context), mode, entityId, uri);
        }

        public CustomFieldTypeEditorDataLoader(
                Context context,
                ObserveDataSourceMode observeDataSourceMode,
                ProjectSyncLoader projectSyncLoader,
                CRUDMode mode, Long entityId, Uri uri
        ) {
            super(context, observeDataSourceMode, projectSyncLoader);
            _mode = mode;
            _entityId = entityId;
            _uri = uri;
            _projectLoader = projectSyncLoader;
        }

        public CRUDMode getMode() {
            return _mode;
        }

        private Content.Data loadFieldTypeData() {
            if (getMode() == CRUDMode.INSERT) {
                return null;
            }
            return loadFirst(Content.READER, _uri, Content.PROJECTION);
        }

        private Collection<Long> loadEnabledProjectIds() {
            if (getMode() == CRUDMode.INSERT) {
                return ImmutableSet.of();
            }
            return loadList(
                    ProjectM2MCustomFieldTypeQuery.PROJECT_ID_READER,
                    ProjectM2MCustomFieldTypeQuery.CONTENT_URI,
                    ProjectM2MCustomFieldTypeQuery.PROJECTION,
                    ProjectM2MCustomFieldTypeQuery.SELECTION,
                    ProjectM2MCustomFieldTypeQuery.args(_entityId),
                    null);
        }

        public CustomFieldTypeEditorData doLoadInBackground() {
            Content.Data data = loadFieldTypeData();
            final List<ProjectModel> projectModels = _projectLoader.load(null, null, null);
            final Collection<Long> enabledProjectIds = loadEnabledProjectIds();
            final ArrayList<ProjectSelectedItem> projectItems = new ArrayList<ProjectSelectedItem>(projectModels.size());
            for (ProjectModel m : projectModels) {
                final boolean selected = enabledProjectIds.contains(m.getId());
                if (selected || !m.isInactive()) {
                    projectItems.add(new ProjectSelectedItem(m, selected));
                }
            }
            return new CustomFieldTypeEditorData(data, projectItems);
        }
    }

}
