package de.kalass.agime.trackactivity;

import com.google.common.base.Preconditions;

import java.util.List;

/**
* Created by klas on 21.01.14.
*/
public class InsertOrUpdateTrackedActivityResult {
    private final List<SplitResult> _splitResults;
    private final Long _startTimeNewMillis;
    private boolean _startedAcquisitionTime;

    public InsertOrUpdateTrackedActivityResult(List<SplitResult> splitResults, long startTimeNewMillis, boolean startedAcquisitionTime) {
        _splitResults = Preconditions.checkNotNull(splitResults);
        _startTimeNewMillis = startTimeNewMillis;
        _startedAcquisitionTime = startedAcquisitionTime;
    }

    public boolean isStartedAcquisitionTime() {
        return _startedAcquisitionTime;
    }

    public long getStartTimeNewMillis() {
        return _startTimeNewMillis;
    }


    public List<SplitResult> getSplitResults() {
        return _splitResults;
    }
}
