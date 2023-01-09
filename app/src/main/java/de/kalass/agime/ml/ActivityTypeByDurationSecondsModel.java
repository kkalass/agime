package de.kalass.agime.ml;

import com.google.common.base.Preconditions;

import java.util.concurrent.TimeUnit;

import de.kalass.agime.backup.BackupData;

/**
 * Predicts the Id of the Activity Type, based on the duration.
 */
public final class ActivityTypeByDurationSecondsModel {

    private static final int NUM_FEATURES = 2 /*exactly one feature: duration in seconds*/;

    private static  MultiClassAgimePredictionModelImpl.Adapter<Long> ADAPTER = new MultiClassAgimePredictionModelImpl.Adapter<Long>(NUM_FEATURES) {

        @Override
        protected Long toInput(BackupData.TrackedActivity data, int rowNum) {
            long millis = data.getEndtimeMillis() - data.getStarttimeMillis();
            return TimeUnit.MILLISECONDS.toSeconds(millis);
        }

        @Override
        protected Long toClassIdentifier(BackupData.TrackedActivity trackedActivity) {
            return trackedActivity.getActivityTypeReference();
        }

    };

    private static  MultiClassAgimePredictionModelImpl.DataRowConverter<Long> CONVERTER = new MultiClassAgimePredictionModelImpl.DataRowConverter<Long>() {


        @Override
        public double[] fillRow(double[] row, Long inputData) {
            Preconditions.checkArgument(row.length == NUM_FEATURES);
            row[0] = 1;
            row[1] = inputData;
            return row;
        }
    };

    public static MultiClassAgimePredictionModel<Long> train(BackupData.PersonalBackup personalBackup, final long timeoutMillis) throws java.util.concurrent.TimeoutException {

        return MultiClassAgimePredictionModelImpl.train(personalBackup, timeoutMillis, ADAPTER, CONVERTER);
    }
}
