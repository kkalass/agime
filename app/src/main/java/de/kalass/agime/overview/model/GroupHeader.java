package de.kalass.agime.overview.model;

import android.content.res.Resources;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import de.kalass.android.common.model.IViewModel;

/**
 * Created by klas on 14.01.14.
 */
public abstract class GroupHeader implements IViewModel, Comparable<GroupHeader>  {

    static final int DEFAULT_ITEM_ID = -42;
    private final long _itemId;
    private final int _groupTypeId;

    protected GroupHeader(int groupId, long itemId) {
        Preconditions.checkArgument(itemId >= 0 || itemId == DEFAULT_ITEM_ID);
        _groupTypeId = groupId;
        _itemId = itemId;
    }

    public final boolean isDefault() {
        return _itemId == DEFAULT_ITEM_ID;
    }

    public final int getGroupType() {
        return _groupTypeId;
    }

    @Override
    public final long getId() {
        return _itemId;
    }

    /**
     * The value to use, when this group header is used for level4
     */
    public abstract String getLevel4Line();

    public abstract String getDisplayName(Resources resources);

    public abstract Integer getColorCode(Resources resources);

    @Override
    public int hashCode() {
        if (isDefault()) {
            return getGroupType();
        }
        return Objects.hashCode(getId(), getGroupType());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof GroupHeader) {
            GroupHeader header = (GroupHeader) o;
            if (getGroupType() != header.getGroupType()) {
                return false;
            }
            return (isDefault() && header.isDefault()) || (getId() == header.getId());
        }
        return false;
    }

    @Override
    public int compareTo(GroupHeader another) {
       int typecomp = getGroupType() - another.getGroupType();
        if (typecomp != 0 ) {
            return typecomp;
        }
        return (int) getId() - (int) another.getId();
    }
}
