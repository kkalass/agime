package de.kalass.agime.customfield;

import com.google.common.base.Preconditions;

import java.util.Collection;

import de.kalass.android.common.model.IViewModel;

/**
* Created by klas on 10.01.14.
*/
public final class ActivityCustomFieldEditorModel implements IViewModel {

    final long typeId;
    final Long associationId;
    final String typeName;
    final Long originalSelectedValueId;
    final String originalSelectedValue;
    final boolean anyProject;
    final Collection<Long> enabledProjectIds;
    Long selectedValueId;
    String selectedValue;

    public ActivityCustomFieldEditorModel(
            CustomFieldTypeModel customFieldType,
            Long associationId,
            boolean anyProject,
            Collection<Long> enabledProjectIds,
            CustomFieldValueModel selected
    ) {
        this.anyProject = anyProject;
        this.enabledProjectIds = enabledProjectIds;
        Preconditions.checkArgument((associationId == null) == (selected == null));
        typeId = customFieldType.getId();
        typeName = customFieldType.getName();
        this.associationId = associationId;
        originalSelectedValueId = selected == null ? null : selected.getId();
        originalSelectedValue = selected == null ? null : selected.getValue();
        selectedValueId = originalSelectedValueId;
        selectedValue = originalSelectedValue;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getValue() {
        return originalSelectedValue;
    }

    @Override
    public long getId() {
        // each field type has one line, so this is OK as an Identifier in this context
        return getTypeId();
    }

    public long getTypeId() {
        // each field type has one line, so this is OK as an Identifier in this context
        return typeId;
    }
}
