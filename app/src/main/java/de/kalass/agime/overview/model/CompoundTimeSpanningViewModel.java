package de.kalass.agime.overview.model;

import android.content.res.Resources;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import de.kalass.agime.model.TimeSpanning;
import de.kalass.android.common.model.IViewModel;

/**
 * Created by klas on 28.11.13.
 */
public class CompoundTimeSpanningViewModel<T extends TimeSpanning> extends CompoundTimeSpanning<T> implements IViewModel {
    private final GroupHeader _model;
    private final long _id;

    public static final Function<CompoundTimeSpanningViewModel, GroupHeader> GET_MODEL = new Function<CompoundTimeSpanningViewModel, GroupHeader>() {
        @Override
        public GroupHeader apply(CompoundTimeSpanningViewModel input) {
            return input == null ? null : input._model;
        }
    };

    public CompoundTimeSpanningViewModel(GroupHeader groupHeader, Iterable<? extends T> items, Long percentageBaseMillis) {
        super(items, percentageBaseMillis);
        _model = Preconditions.checkNotNull(groupHeader);
        _id = groupHeader.getId();
        Preconditions.checkArgument(_model.isDefault() || _id >= 0, "Entities must not have negative ids");
    }

    public GroupHeader getGroupHeader() {
         return _model;
    }

    public int getGroupTypeId() {
        return _model.getGroupType();
    }

    public String getName(Resources resources) {
        return _model.getDisplayName(resources);
    }

    public Integer getColorCode(Resources resources) {
        return _model.getColorCode(resources);
    }

    @Override
    public long getId() {
        return _id;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .addValue(_id)
                .addValue(getChildren())
                .toString();
    }
}
