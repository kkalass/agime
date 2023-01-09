package de.kalass.agime.customfield;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;

import de.kalass.android.common.model.IViewModel;

/**
* Created by klas on 10.01.14.
*/
public final class ActivityCustomFieldModel implements IViewModel {

    public static final Function<ActivityCustomFieldModel, Long> GET_TYPE_ID = new Function<ActivityCustomFieldModel, Long>() {
        @Override
        public Long apply(ActivityCustomFieldModel input) {
            return input == null ? null : input._customFieldType.getId();
        }
    };

    private final Long _associationId;
    private final Long _selectedValueId;
    private final String _selectedValue;
    private final CustomFieldTypeModel _customFieldType;
    private final CustomFieldValueModel _customFieldValue;

    public ActivityCustomFieldModel(
            CustomFieldTypeModel customFieldType,
            Long associationId,
            CustomFieldValueModel selected
    ) {
        _customFieldType = Preconditions.checkNotNull(customFieldType);

        Preconditions.checkArgument((associationId == null) == (selected == null));
        _customFieldValue = selected;
        _associationId = associationId;

        _selectedValueId = selected == null ? null : selected.getId();
        _selectedValue = selected == null ? null : selected.getValue();
    }

    public String getValue() {
        return _selectedValue;
    }

    /**
     * @return null if no value was selected for this type
     */
    public Long getAssociationId() {
        return _associationId;
    }

    public CustomFieldTypeModel getTypeModel() {
        return _customFieldType;
    }

    /**
     *
     * @return null, if no value was selected
     */
    public CustomFieldValueModel getValueModel() {
        return _customFieldValue;
    }

    @Override
    public long getId() {
        // each field type has one line, so this is OK as an Identifier in this context
        return _customFieldType.getId();
    }
}
