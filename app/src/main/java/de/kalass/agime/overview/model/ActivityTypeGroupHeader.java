package de.kalass.agime.overview.model;

import android.content.res.Resources;

import de.kalass.agime.R;
import de.kalass.agime.model.ActivityTypeModel;

/**
 * Created by klas on 14.01.14.
 */
class ActivityTypeGroupHeader extends GroupHeader {
    static final int GROUP_TYPE = 3;
    private final ActivityTypeModel _model;

    public ActivityTypeGroupHeader(ActivityTypeModel model) {
        super(GROUP_TYPE, model == null ? DEFAULT_ITEM_ID : model.getId());
        _model = model;
    }

    @Override
    public String getLevel4Line() {
        return _model == null ? null : _model.getName();
    }

    public String getDisplayName(Resources resources) {
        return isDefault() ? resources.getString(R.string.activity_name_default) : _model.getName();
    }

    public Integer getColorCode(Resources resources) {
        return null;
    }
}
