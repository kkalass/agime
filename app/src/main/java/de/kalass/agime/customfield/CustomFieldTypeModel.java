package de.kalass.agime.customfield;

import com.google.common.base.Function;

import de.kalass.android.common.model.IViewModel;

/**
* Created by klas on 14.01.14.
*/
public final class CustomFieldTypeModel implements IViewModel {
    static final Function<CustomFieldTypeModel, Long> GET_ID = new Function<CustomFieldTypeModel, Long> () {
        @Override
        public Long apply(CustomFieldTypeModel data) {
            return data.id;
        }
    };

    private final long id;
    private final String name;
    private final boolean anyProject;

    public CustomFieldTypeModel(long id, String name, boolean anyProject) {
        this.id = id;
        this.name = name;
        this.anyProject = anyProject;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isAnyProject() {
        return anyProject;
    }
}
