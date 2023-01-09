package de.kalass.android.common.simpleloader;

import android.content.ContentProviderOperation;

import com.google.common.base.Preconditions;
import static com.google.common.base.Preconditions.checkNotNull;
/**
* Created by klas on 21.01.14.
*/
public final class ValueOrReference {
    private final Long _value;
    private final Integer _reference;

    private ValueOrReference(Long value, Integer reference) {
        Preconditions.checkArgument(value == null || reference == null);
        _value = value;
        _reference = reference;
    }

    public static ValueOrReference ofValue(Long value) {
        return new ValueOrReference(value /*null is allowed here*/, null);
    }

    public static ValueOrReference ofValueNonnull(Long value) {
        return new ValueOrReference(checkNotNull(value), null);
    }

    public static ValueOrReference ofReference(Integer reference) {
        return new ValueOrReference(null, checkNotNull(reference));
    }

    public boolean isReference() {
        return _reference != null;
    }

    public Integer getReference() {
        return _reference;
    }

    public Long getValue() {
        return _value;
    }

    public ContentProviderOperation.Builder appendSelf(ContentProviderOperation.Builder builder, String key) {
        if (_reference != null) {
            return builder.withValueBackReference(key, _reference);
        }
        return builder.withValue(key, _value);
    }
}
