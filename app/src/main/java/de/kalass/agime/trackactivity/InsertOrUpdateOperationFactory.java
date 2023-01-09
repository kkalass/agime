package de.kalass.agime.trackactivity;

import android.content.ContentProviderOperation;
import android.content.Context;

import java.util.ArrayList;

import de.kalass.android.common.simpleloader.ValueOrReference;

/**
* Created by klas on 21.01.14.
*/
public interface InsertOrUpdateOperationFactory {
    /**
     * Append operations to the list.
     *
     * Will be called from a background thread, so it is OK to do lengthy
     * requests here, but it is not OK to access GUI elements.
     */
    ArrayList<ContentProviderOperation> createOperationsInBackground(
            Context context,
            int numPreviousOperations,
            ValueOrReference trackedActivityId
    );
}
