package de.kalass.agime.model;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import de.kalass.android.common.model.IViewModel;

/**
 * View Model for activity data.
 * Created by klas on 21.10.13.
 */
public class CategoryModel implements IViewModel {

    private final long _id;
    private final int _colour;
    private final String _name;

    public CategoryModel(
            long id, String name, int colour
    ) {
        _id = id;
        _name = name;
        _colour = colour;
    }


    public long getId() {
        return _id;
    }

    public int getColour() {
        return _colour;
    }

    public String getName() {
        return _name;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .addValue(_id)
                .addValue(_name)
                .toString();
    }
}
