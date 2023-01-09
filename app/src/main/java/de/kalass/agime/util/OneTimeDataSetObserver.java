package de.kalass.agime.util;

import android.database.DataSetObserver;
import android.widget.Adapter;

import com.google.common.base.Preconditions;

/**
 * Created by klas on 27.11.13.
 */
public abstract class OneTimeDataSetObserver extends DataSetObserver {

    private Adapter _adapter;

    public void once(Adapter adapter) {
        _adapter = adapter;
        _adapter.registerDataSetObserver(this);
    }

    @Override
    public final void onChanged() {
        unregister();
        changed();
    }

    protected void changed() {
    }

    private void unregister() {
        Preconditions.checkArgument(_adapter != null,
                "OneTimeDataSetObserver subclasses must be registered via OneTimeDataSetObserver.once()");
        _adapter.unregisterDataSetObserver(this);
    }

    @Override
    public final void onInvalidated() {
        unregister();
        invalidated();
    }

    protected void invalidated() {
    }
}
