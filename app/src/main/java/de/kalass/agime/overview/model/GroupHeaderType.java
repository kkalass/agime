package de.kalass.agime.overview.model;

import com.google.common.base.Function;

import de.kalass.agime.model.TrackedActivityModel;
import de.kalass.android.common.model.IViewModel;

/**
* Created by klas on 15.01.14.
*/
public abstract class GroupHeaderType implements Function<TrackedActivityModel, GroupHeader>, IViewModel {
    private final int _groupTypeId;
    private final String _title;

    enum SortOrder {
        DURATION,
        NATURAL
    }

    GroupHeaderType(int groupTypeId, String title) {
        _groupTypeId = groupTypeId;
        _title = title;
    }

    public SortOrder getSortOrder() {
        return SortOrder.DURATION;
    }

    @Override
    public long getId() {
        // IMPORTANT: return group id here - the id of the selected item will be interpreted as group type id!
        return getGroupTypeId();
    }

    public int getGroupTypeId() {
        return _groupTypeId;
    }

    public String getTitle() {
        return _title;
    }

    @Override
    public int hashCode() {
        return _groupTypeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof GroupHeaderType) {
            GroupHeaderType other = (GroupHeaderType)o;
            return other._groupTypeId == _groupTypeId;
        }
        return false;
    }
}
