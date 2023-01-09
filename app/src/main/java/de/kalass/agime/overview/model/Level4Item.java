package de.kalass.agime.overview.model;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.List;

import de.kalass.agime.model.TimeSpanning;
import de.kalass.agime.model.TrackedActivityModel;
import de.kalass.android.common.model.IViewModel;

/**
 * Created by klas on 14.01.14.
 */
public class Level4Item extends TimeSpanning implements IViewModel, TrackedActivityModelContainer  {
    public static final Function<Level4Item, GroupHeaderType> GET_TYPE = new Function<Level4Item, GroupHeaderType>() {
        @Override
        public GroupHeaderType apply(Level4Item level4Item) {
            return level4Item == null ? null : level4Item.getType();
        }
    };

    public static final Function<Level4Item, String> GET_DETAILS = new Function<Level4Item, String>() {
        @Override
        public String apply(Level4Item level4Item) {
            return level4Item == null ? null : level4Item.getDetails();
        }
    };

    private final long _fakeId;
    private final String _details;
    private final GroupHeaderType _type;
    private final List<TrackedActivityModel> _trackedActivities;

    public Level4Item(String details, TrackedActivityModel model,  GroupHeaderType type) {
        this(details, ImmutableList.of(model), type);
    }

    public Level4Item(String details, List<TrackedActivityModel> model,  GroupHeaderType type) {
        super(0/*fake duration*/);
        _type = Preconditions.checkNotNull(type);
        _fakeId = -1/*fake id*/;
        _details = Preconditions.checkNotNull(details);
        _trackedActivities = model;
    }

    @Override
    public List<TrackedActivityModel> getActivities() {
        return _trackedActivities;
    }

    public String getDetails() {
        return _details;
    }

    /**
     * @return the type of level 3, if this represents the details of a tracked activity
     */
    public GroupHeaderType getType() {
        return _type;
    }

    @Override
    public int hashCode() {
        return _details.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof Level4Item) {
            Level4Item other = (Level4Item)o;
            return _details.equals(other._details) && (Objects.equal(_type, other._type));
        }
        return false;
    }

    @Override
    public long getId() {
        return _fakeId;
    }
}
