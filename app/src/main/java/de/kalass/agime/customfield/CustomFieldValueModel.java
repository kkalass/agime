package de.kalass.agime.customfield;

import com.google.common.base.Function;

import de.kalass.android.common.model.IViewModel;

/**
* Created by klas on 14.01.14.
*/
public final class CustomFieldValueModel implements IViewModel {
    static final Function<CustomFieldValueModel, Long> GET_ID = new Function<CustomFieldValueModel, Long> () {
        @Override
        public Long apply(CustomFieldValueModel data) {
            return data.id;
        }
    };
    static final Function<CustomFieldValueModel, Long> GET_TYPE_ID = new Function<CustomFieldValueModel, Long> () {
        @Override
        public Long apply(CustomFieldValueModel data) {
            return data.typeId;
        }
    };
    private final long id;
    private final long typeId;
    private final String value;

    CustomFieldValueModel(long id, long typeId, String value) {
        this.id = id;
        this.typeId = typeId;
        this.value = value;
    }

    public long getId() {
        return id;
    }

    public long getTypeId() {
        return typeId;
    }

    public String getValue() {
        return value;
    }
}
