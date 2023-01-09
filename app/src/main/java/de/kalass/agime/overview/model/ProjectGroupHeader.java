package de.kalass.agime.overview.model;

import android.content.res.Resources;

import de.kalass.agime.R;
import de.kalass.agime.model.ProjectModel;

/**
 * Created by klas on 14.01.14.
 */
class ProjectGroupHeader extends GroupHeader {
    static final int GROUP_TYPE = 1;
    private final ProjectModel _project;

    public ProjectGroupHeader(ProjectModel project) {
        super(GROUP_TYPE, project == null ? DEFAULT_ITEM_ID : project.getId());
        _project = project;
    }

    @Override
    public String getLevel4Line() {
        return _project == null ? null : _project.getName();
    }

    public String getDisplayName(Resources resources) {
        return _project == null ? resources.getString(R.string.activity_project_default) : _project.getName();
    }

    public Integer getColorCode(Resources resources) {
        final Integer colorCode = _project == null ? null : _project.getColorCode();
        return colorCode == null ? resources.getColor(R.color.project_background_default_header) : colorCode.intValue();
    }

}
