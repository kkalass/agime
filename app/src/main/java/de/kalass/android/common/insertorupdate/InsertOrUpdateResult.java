package de.kalass.android.common.insertorupdate;

import com.google.common.base.Preconditions;


/**
* Created by klas on 21.01.14.
*/
public class InsertOrUpdateResult<T> {
    private final Long _id;
    private final Boolean _isInsert;
    private final T _payload;
    private final Exception _e;

    InsertOrUpdateResult(Long id, Boolean isInsert, T payload, Exception e) {
        _id = id;
        _isInsert = isInsert;
        _payload = payload;
        _e = e;

    }
    public static <T> InsertOrUpdateResult<T> forError(Exception e) {
        return new InsertOrUpdateResult<T>(null, null, null, e);
    }

    public static <T> InsertOrUpdateResult<T> forSuccess(long id, boolean isInsert, T payload) {
        return new InsertOrUpdateResult<T>(id, isInsert, payload, null);
    }

    public boolean isError() {
        return _e != null;
    }

    public long getId() {
        Preconditions.checkState(!isError(), "Cannot call getId() for error results");
        return _id;
    }


    public boolean isInsert() {
        Preconditions.checkState(!isError(), "Cannot call isInsert() for error results");
        return _isInsert;
    }

    public T get() {
        Preconditions.checkState(!isError(), "Cannot call get() for error results");
        return _payload;
    }
}
