package de.kalass.android.common.provider;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import de.kalass.android.common.util.StringUtil;
import edu.mit.mobile.android.content.DBHelper;
import edu.mit.mobile.android.content.GenericDBHelper;
import edu.mit.mobile.android.content.QuerystringWrapper;
import edu.mit.mobile.android.content.SimpleContentProvider;

/**
 * Created by klas on 04.11.13.
 */
public class AbstractContentProvider extends SimpleContentProvider {
    private final String _authority;

    public AbstractContentProvider(String authority, int dbVersion) {
        this(authority, null, dbVersion);
    }

    public AbstractContentProvider(String authority, String dbName, int dbVersion) {
        super(authority, dbName, dbVersion);
        _authority = authority;
    }

    public final String getAuthority() {
        return _authority;
    }

    protected void registerContentItem(Class contractClass, String path) {
        final GenericDBHelper helper = new GenericDBHelper(contractClass);

        // By wrapping the main helper like so, this will translate the query portion of the URI
        // (that is, the part after the "?") into a select statement to limit the results.
        final QuerystringWrapper queryWrapper = new QuerystringWrapper(helper);

        // This adds a mapping between the given content:// URI path and the
        // helper.
        addDirAndItemUri(queryWrapper, path);

    }

    protected void registerContentItem(Class contractClass, String path, final String contentDirType, final String contentItemType) {
        final GenericDBHelper helper = new GenericDBHelper(contractClass);

        // By wrapping the main helper like so, this will translate the query portion of the URI
        // (that is, the part after the "?") into a select statement to limit the results.
        final QuerystringWrapper queryWrapper = new QuerystringWrapper(helper);

        // This adds a mapping between the given content:// URI path and the
        // helper.
        addDirAndItemUri(queryWrapper, path, contentDirType, contentItemType);

    }



    protected static boolean inBetween(int oldVersion, int newVersion, int version) {
        return version > oldVersion && version <= newVersion;
    }

    /**
     * Instantiate a new {@link DatabaseOpenHelper} for this provider.
     *
     * Note that we are changing the implementation from auto-generation to application of patches.
     *
     * @return
     */
    protected DatabaseOpenHelper createDatabaseOpenHelper() {
        return new PatchFileBasedDatabaseOpenHelper(getContext(), getDBName(), getDBVersion(), getDBHelpers());
    }


    public static class PatchFileBasedDatabaseOpenHelper extends DatabaseOpenHelper {
        private final int version;

        public PatchFileBasedDatabaseOpenHelper(Context context, String name, int version, List<DBHelper> dbHelpers) {
            super(context, name, version, dbHelpers);
            this.version = version;
        }


        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            applyPatchFiles(getContext(), db, oldVersion, newVersion, MissingPatchFile.FAIL);
            afterUpgrade(db, oldVersion, newVersion);
        }

        /**
         * Called after the migration script files were executed.
         *
         * Useful if you need to insert locale- or device dependend default or example data.
         *
         * @param db the database
         * @param oldVersion the old database version before the migration was executed
         * @param newVersion the new version
         */
        protected void afterUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            onUpgrade(db, 0, version);
        }
    }

    private static List<String> readPatchFile(Context context, int patchlevel,  MissingPatchFile missing) {
        AssetManager resources = context.getAssets();
        String filename = String.format(Locale.US, "sql/%05d.sql", patchlevel);

        List<String> result = new ArrayList<String>();
        try {
            if (missing == MissingPatchFile.SKIP) {
                String[] listed = resources.list(filename);
                if (listed == null || listed.length == 0) {
                    Log.i("DB", "No Patchfile found for Patchlevel " + patchlevel + ", skipping.");
                    return ImmutableList.of();
                }
            }

            InputStream stream = resources.open(filename);
            try {
                Scanner scanner = new Scanner(stream).useDelimiter(";");
                while (scanner.hasNext()) {
                    result.add(scanner.next());
                }
            } finally {
                stream.close();
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return result;
    }

    protected enum MissingPatchFile {
        SKIP,
        FAIL
    }
    protected static void applyPatchFiles(Context context, final SQLiteDatabase db, final int oldVersion, final int newVersion, MissingPatchFile missing) {
        for (int i = oldVersion; i < newVersion; i++) {
            List<String> sql = readPatchFile(context, i + 1, missing);
            for (String stmt: sql) {
                if (!StringUtil.isTrimmedNullOrEmpty(stmt)) {
                    Log.i("DB", "executing: " + stmt);
                    db.execSQL(stmt);
                }
            }
        }
    }
}
