package de.kalass.android.common.simpleloader;

import android.content.Context;
import android.database.ContentObserver;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by klas on 19.01.14.
 */
public abstract class CompoundSyncLoader extends SyncLoader {

    private final List<SyncLoader> childLoaders;

    public CompoundSyncLoader(Context context, SyncLoader... childLoaders) {
        super(context);
        this.childLoaders = new ArrayList<SyncLoader>();

        for (SyncLoader loader: childLoaders) {
            add(loader);
        }

    }

    public <L extends SyncLoader> L add(L loader) {
        this.childLoaders.add(Preconditions.checkNotNull(loader));
        final ContentObserver contentObserver = getContentObserver();
        if (contentObserver != null) {
            loader.setContentObserver(contentObserver);
        }
        return loader;
    }

    @Override
    public void setContentObserver(ContentObserver observer) {
        super.setContentObserver(observer);
        for (SyncLoader child : childLoaders) {
            child.setContentObserver(observer);
        }
    }

    public void close() {
        try {
            for (SyncLoader child : childLoaders) {
                child.close();
            }
        } finally {
             super.close();
        }
    }
}
