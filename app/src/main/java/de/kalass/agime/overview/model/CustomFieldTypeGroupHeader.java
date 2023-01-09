package de.kalass.agime.overview.model;

import android.content.res.Resources;

import com.google.common.base.Preconditions;

import de.kalass.agime.R;
import de.kalass.agime.customfield.CustomFieldTypeModel;
import de.kalass.agime.customfield.CustomFieldValueModel;

/**
 * Created by klas on 14.01.14.
 */
public class CustomFieldTypeGroupHeader extends GroupHeader {
    private static final int GROUP_TYPE_CUSTOM_FIELD_BASE = 100;
    private final CustomFieldTypeModel _type;
    private final CustomFieldValueModel _value;

    public CustomFieldTypeGroupHeader(CustomFieldTypeModel type,
                                      CustomFieldValueModel value) {
        super(getGroupTypeId(type), value == null ? DEFAULT_ITEM_ID : value.getId());
        Preconditions.checkNotNull(type, "Custom Field Type must not be null");
        _type = type;
        _value = value;
        Preconditions.checkArgument(value == null || type.getId() == value.getTypeId());
    }

    protected static int getGroupTypeId(CustomFieldTypeModel type) {
        return GROUP_TYPE_CUSTOM_FIELD_BASE + (int)type.getId();
    }

    @Override
    public String getLevel4Line() {
        return _value == null ? null : _value.getValue();
    }

    public String getDisplayName(Resources resources) {
        return isDefault() ? resources.getString(R.string.overview_group_types_custom_field_empty_value, _type.getName()): _value.getValue();
    }

    public Integer getColorCode(Resources resources) {
        return isDefault() ? resources.getColor(R.color.overview_group_header_custom_field_not_specified_background) : null;
    }

}
