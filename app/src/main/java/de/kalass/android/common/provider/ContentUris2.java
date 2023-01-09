package de.kalass.android.common.provider;

import android.content.ContentUris;
import android.net.Uri;

import java.util.List;

import edu.mit.mobile.android.content.ContentItem;
import edu.mit.mobile.android.content.column.DBColumn;
import edu.mit.mobile.android.content.column.DatetimeColumn;

/**
 * Created by klas on 22.12.13.
 */
public final class ContentUris2  {
    private ContentUris2() {
        // unused
    }

    public static final Uri getDirUriFromParent(Uri parentDirUri, long parentItemId, String childPath) {
        final Uri uri = ContentUris.withAppendedId(parentDirUri, parentItemId);
        return Uri.withAppendedPath(uri, childPath);
    }
    public static final Uri getDirUriFromParent(Uri parent, String childPath) {
        return Uri.withAppendedPath(parent, childPath);
    }

    public static final Uri getParentUriFromDirUri(Uri uri, String path) {
        final Uri.Builder builder = uri.buildUpon();
        builder.path(null);//reset
        final List<String> segments = uri.getPathSegments();
        String last = segments.get(segments.size() - 1);
        if (!path.equals(last)) {
            throw new IllegalArgumentException("Not a valid URI for " + path);
        }
        for (int i = 0; i < segments.size() - 1 /*trim last segment*/; i++) {
            builder.appendPath(segments.get(i));
        }
        return builder.build();
    }
}
