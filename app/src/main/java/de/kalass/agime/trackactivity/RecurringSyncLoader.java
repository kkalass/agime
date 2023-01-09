package de.kalass.agime.trackactivity;

import android.content.Context;

import java.util.List;

import de.kalass.agime.acquisitiontime.RecurringDAO;
import de.kalass.android.common.simpleloader.SyncLoader;

/**
* Created by klas on 20.02.14.
*/
public class RecurringSyncLoader extends SyncLoader {
    public RecurringSyncLoader(Context context) {
        super(context);
    }

    public List<RecurringDAO.Data> loadRecurring() {
        return loadList(
                RecurringDAO.READ_DATA, RecurringDAO.CONTENT_URI, RecurringDAO.PROJECTION,
                RecurringDAO.selection(), RecurringDAO.selectionArgs(), null
        );
    }
}
