package de.kalass.android.common.simpleloader;

import android.content.Context;
import android.database.ContentObserver;
import android.widget.Filter;

/**
 * Created by klas on 20.01.14.
 */
public abstract class CompoundSyncLoaderFilter extends Filter {
    private final SyncLoader[] childLoaders;

    public CompoundSyncLoaderFilter(SyncLoader... childLoaders) {
        super();
        this.childLoaders = childLoaders;
    }

    public void releaseResources() {
        if (this.childLoaders != null) {
            for (SyncLoader child : childLoaders) {
                if (child != null) {
                    child.close();
                }
            }
        }
    }
}
