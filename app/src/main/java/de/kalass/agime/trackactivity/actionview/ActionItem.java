package de.kalass.agime.trackactivity.actionview;

import com.google.common.base.Preconditions;

import de.kalass.agime.acquisitiontime.AcquisitionTimeInstance;
import de.kalass.agime.model.TrackedActivityModel;

/**
* Created by klas on 14.02.14.
*/
public final class ActionItem {

    public Mode getMode() {
        return _mode;
    }

    public AcquisitionTimeInstance getAcquisitionTime() {
        return _acquisitionTime;
    }


    public enum Mode {
        /*Action for the first activity of the day - during acquisition time */
        START_OF_DAY,

        /*Action for the first activity of the day - but the acquisition time has passed already*/
        START_OF_DAY_ACQUISITION_TIME_PASSED,

        /*Action for the first activity of the day - before acquisition time starts (also if there will not be acquisition time for the day) */
        START_OF_DAY_NO_ACQUISITION_TIME,

        /* Ask the User for another Entry - only during acquisition time */
        NEW_ENTRY,

        /* Ask the User for another Entry - only during acquisition time */
        NEW_ENTRY_BEFORE_ACQUISITION_TIME,

        /*Action if there are entries today, and there is no acquisition time following*/
        END_OF_DAY,

        /*Like END_OF_DAY, but for a day in the past - not today*/
        END_OF_DAY_PAST,

        /**
         * Action, if the current day is not today and there are no items
         */
        NOTHING
    }

    final Mode _mode;
    final long _startTimeMillis;
    final long _endTimeMillis;
    final TrackedActivityModel _previousEntry;
    final Long _totalDayEntryDurationMillis;
    final AcquisitionTimeInstance _acquisitionTime;
    public ActionItem(Mode mode, long startTimeMillis, long endTimeMillis,
                      AcquisitionTimeInstance currentAcquisitionTime,
                      TrackedActivityModel previous,
                      Long totalDayEntryDurationMillis) {

        if (mode == Mode.NEW_ENTRY ||mode == Mode.NEW_ENTRY_BEFORE_ACQUISITION_TIME) {
            Preconditions.checkNotNull(previous);
        }
        _totalDayEntryDurationMillis = totalDayEntryDurationMillis;
        _acquisitionTime = currentAcquisitionTime;
        _previousEntry = previous;
        _mode = mode;
        _startTimeMillis = startTimeMillis;
        _endTimeMillis = endTimeMillis;
    }

    public Long getTotalDayEntryDurationMillis() {
        return _totalDayEntryDurationMillis;
    }

    public Long getPreviousEntryId() {
        return _previousEntry == null ? null : _previousEntry.getId();
    }

    public long getDurationMillis() {
        return _endTimeMillis - _startTimeMillis;
    }

    public long getDurationMinutes() {
        return getDurationMillis() / (60*1000);
    }

    public boolean isPreviousAcquisitionTimeUnfulfilled() {
        return _mode == ActionItem.Mode.END_OF_DAY &&
                _acquisitionTime != null &&
                _acquisitionTime.getEndDateTime().plusMinutes(AcquisitionTimeInstance.ACQUISITION_TIME_END_THRESHOLD_MINUTES).isAfterNow()
                &&
                (_previousEntry == null || (_previousEntry.getEndTimeMillis() < _acquisitionTime.getEndDateTime().getMillis()));

    }
}
