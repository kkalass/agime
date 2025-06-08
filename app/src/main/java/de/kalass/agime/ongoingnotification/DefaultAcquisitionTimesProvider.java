package de.kalass.agime.ongoingnotification;

import android.content.Context;
import android.database.Cursor;

import org.joda.time.DateTime;

import java.util.List;

import de.kalass.agime.acquisitiontime.AcquisitionTimes;
import de.kalass.agime.acquisitiontime.RecurringDAO;
import de.kalass.android.common.simpleloader.CursorUtil;

/**
 * Default implementation of AcquisitionTimesProvider that retrieves data from the database
 * via ContentProvider. This is the production implementation used in the real app.
 */
public class DefaultAcquisitionTimesProvider implements AcquisitionTimesProvider {
    
    private final Context context;
    
    public DefaultAcquisitionTimesProvider(Context context) {
        this.context = context;
    }
    
    @Override
    public AcquisitionTimes getCurrentAcquisitionTimes() {
        Cursor query = context.getContentResolver().query(
            RecurringDAO.CONTENT_URI, RecurringDAO.PROJECTION, null, null, null);

        List<RecurringDAO.Data> recurringItems;
        try {
            recurringItems = CursorUtil.readList(query, RecurringDAO.READ_DATA);
            return AcquisitionTimes.fromRecurring(recurringItems, new DateTime());
        }
        finally {
            if (query != null) {
                query.close();
            }
        }
    }
}
