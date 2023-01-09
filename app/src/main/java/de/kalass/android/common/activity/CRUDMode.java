package de.kalass.android.common.activity;

import android.content.Intent;

import com.google.common.base.Preconditions;

import java.util.Set;

/**
 * Created by klas on 21.12.13.
 */
public enum CRUDMode {
    VIEW,
    INSERT,
    EDIT,
    DELETE;

    /**
     * Convert an intent to a mode.
     *
     * Note that, if you accept intents with Intent.ACTION_INSERT, you must support INSERT mode.
     * If you accept intents with Intent.ACTION_EDIT you must support EDIT mode.
     * For Intent.ACTION_VIEW and Intent.ACTION_DELETE you may either support the corresponding
     * mode, or just EDIT - which is the fallback if you do not support the corresponding mode.
     */
    public static CRUDMode fromIntentAction(Intent intent, Set<CRUDMode> supportedModes) {
        String action = intent.getAction();
        if (action == null) {
            return assertContains(supportedModes, INSERT);
        }
        if (Intent.ACTION_VIEW.equals(action)) {
            if (supportedModes.contains(VIEW)) {
                return VIEW;
            }
            return assertContains(supportedModes, EDIT);
        }
        if (Intent.ACTION_INSERT.equals(action)) {
            return assertContains(supportedModes, INSERT);
        }
        if (Intent.ACTION_EDIT.equals(action)) {
            return assertContains(supportedModes, EDIT);
        }
        if (Intent.ACTION_DELETE.equals(action)) {
            if (supportedModes.contains(DELETE)) {
                return DELETE;
            }
            return assertContains(supportedModes, EDIT);
        }
        throw new IllegalStateException("Not a CRUD action: " + action);
    }

    private static CRUDMode assertContains(Set<CRUDMode> supportedModes, CRUDMode mode) {
        if (!supportedModes.contains(mode)) {
            throw new IllegalArgumentException("Expected " + mode + " to be supported, but only " + supportedModes + " are supported. ");
        }
        return mode;
    }

}
