package de.kalass.agime.ml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import de.kalass.agime.backup.BackupData;
import de.kalass.commons.ml.MultiClassPrediction;
import de.kalass.commons.ml.Octave;

/**
 * Predicts the Id of the Activity Type, based on the starttime.
 */
public final class MultiClassAgimePredictionModelImpl<I> implements MultiClassAgimePredictionModel<I> {
    private static final Logger LOG = LoggerFactory.getLogger("TRAINING");

    private final MultiClassPrediction<Long> _model;
    private final DataRowConverter<I> rowConverter;
    private final int numFeatures;

    interface DataRowConverter<I> {
        double[] fillRow(double[] row, I inputData);
    }

    public static abstract class Adapter<I> {
        private final int numFeatures;

        protected Adapter(int numFeatures) {
            this.numFeatures = numFeatures;
        }

        protected final int getNumFeatures() {
            return numFeatures;
        }

        protected abstract I toInput(BackupData.TrackedActivity data, int rowNum);
        /**
         * Returns the Long value that identifies the class, in which the activity falls.
         *
         * Note that currently an activity always belongs to exactly one class, not to multiple ones.
         */
        protected abstract Long toClassIdentifier(BackupData.TrackedActivity trackedActivity);


    }


    public MultiClassAgimePredictionModelImpl(MultiClassPrediction<Long> model, DataRowConverter<I> rowConverter, int numFeatures) {
        _model = model;
        this.rowConverter = rowConverter;
        this.numFeatures = numFeatures;
    }

    public java.util.List<de.kalass.commons.ml.MultiClassPredictionResult<Long>> predict(I inputData) {
        return _model.predict(rowConverter.fillRow(new double[numFeatures], inputData));
    }

    public MultiClassPrediction<Long> getInternalModel() {
        return _model;
    }

    static <I> MultiClassAgimePredictionModelImpl<I> train(
            BackupData.PersonalBackup personalBackup,
            final long timeoutMillis,
            Adapter<I> adapter,
            DataRowConverter<I> dataRowConverter
    ) throws java.util.concurrent.TimeoutException {

        return new MultiClassAgimePredictionModelImpl<I>(trainMultiClassPrediction(personalBackup, timeoutMillis, adapter, dataRowConverter), dataRowConverter, adapter.getNumFeatures());
    }


    static <I> MultiClassPrediction<Long> trainMultiClassPrediction(
            BackupData.PersonalBackup personalBackup,
            final long timeoutMillis,
            Adapter<I> adapter,
            DataRowConverter<I> dataRowConverter
    ) throws java.util.concurrent.TimeoutException {
        int trackedActivitiesCount = personalBackup.getTrackedActivitiesCount();
        double[][] xMatrix = new double[trackedActivitiesCount][adapter.getNumFeatures()];
        Long[] y = new Long[trackedActivitiesCount];

        for (int i = 0; i < trackedActivitiesCount; i++) {
            BackupData.TrackedActivity trackedActivity = personalBackup.getTrackedActivities(i);
            double[] row = xMatrix[i];
            I input = adapter.toInput(trackedActivity, i);
            dataRowConverter.fillRow(row, input);
            long classIdentifier = adapter.toClassIdentifier(trackedActivity);
            y[i] = classIdentifier;
        }

        return MultiClassPrediction.train(Octave.createSimpleMatrix(xMatrix), y, timeoutMillis);
    }
}
