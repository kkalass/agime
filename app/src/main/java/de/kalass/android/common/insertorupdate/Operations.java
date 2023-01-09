package de.kalass.android.common.insertorupdate;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;

import java.util.ArrayList;

import de.kalass.android.common.simpleloader.ValueOrReference;

/**
* Created by klas on 27.01.14.
*/
public class Operations<R> {
    private final ArrayList<ContentProviderOperation> ops;
    private final ValueOrReference mainOperationReference;
    private final R _value;

    private Operations(ArrayList<ContentProviderOperation> ops, ValueOrReference mainOperationReference, R value) {
        this.ops = ops;
        this.mainOperationReference = mainOperationReference;
        _value = value;
    }

    public static <T> Operations<T> getInstance(ArrayList<ContentProviderOperation> ops, ValueOrReference mainOperationReference) {
        return getInstance(ops, mainOperationReference, null);
    }

    public static <T> Operations<T> getInstance(ArrayList<ContentProviderOperation> ops, ValueOrReference mainOperationReference, T value) {
        return new Operations<T>(ops, mainOperationReference, value);
    }

    public ArrayList<ContentProviderOperation> getOps() {
        return ops;
    }

    public ValueOrReference getMainOperationReference() {
        return mainOperationReference;
    }

    public R createResult(long resultId, ContentProviderResult[] contentProviderResults) {
        return _value;
    }
}
