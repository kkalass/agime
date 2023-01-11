package de.kalass.agime.overview.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import de.kalass.agime.model.TimeSpanning;

/**
 * Container for a TimeSpanning instance and its percentage relative to the total of
 * TimeSpannig instances.
 *
 * Created by klas on 28.11.13.
 */
public class TimeSpanningChild<T extends TimeSpanning> {
    private final float _percentage;
    private final T _item;

    public TimeSpanningChild(
            T item, float percentage
    ) {
        _item = item;
        _percentage = percentage;
    }

    public T getItem() {
        return _item;
    }

    public float getPercentage() {
        return _percentage;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .addValue(_item)
                .addValue((_percentage * 100f) + " %")
                .toString();
    }
}
