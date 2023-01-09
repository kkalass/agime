package de.kalass.agime.backup;

import com.google.android.gms.common.api.Status;

/**
* Created by klas on 13.02.14.
*/
interface Continuation {
    void finishSuccess();
    void finishWithError(int msgTitleId, int msgTextId);
    boolean finishOnError(String msg, Status status);
}
