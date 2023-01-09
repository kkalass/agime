package de.kalass.agime.overview.model;

import android.content.res.Resources;

/**
 * Created by klas on 14.01.14.
 */
class AnyGroupHeader extends GroupHeader {
    static final int GROUP_TYPE = 4;
    private final String _displayName;

    public AnyGroupHeader(String displayName) {
        super(GROUP_TYPE, DEFAULT_ITEM_ID);
        _displayName = displayName;
    }

    @Override
    public String getLevel4Line() {
        return null;
    }

    public String getDisplayName(Resources resources) {
        return _displayName;
    }

    public Integer getColorCode(Resources resources) {
        return null;
    }

}
