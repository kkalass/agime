package de.kalass.agime.ml;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import de.kalass.agime.backup.BackupData;

/**
 * Predicts the Id of the Activity Type, based on the starttime.
 */
public final class ActivityCategoryByStartTimePrediction extends DelegatingMultiClassAgimePredictionModel<ActivityCategoryByStartTimePrediction.Input> {
    private static final Logger LOG = LoggerFactory.getLogger("ActivityCategoryByStartTimePrediction");

    public ActivityCategoryByStartTimePrediction(MultiClassAgimePredictionModel<Input> model) {
        super(model);
    }

    public static final class Input {
        final LocalDate date;
        final LocalTime time;

        public Input(LocalDate date, LocalTime time) {
            this.date = date;
            this.time = time;
        }
    }

    private static final class ActivityCategoryByStartTimeAdapter extends MultiClassAgimePredictionModelImpl.Adapter<Input> {

        private ImmutableMap<Long, BackupData.ActivityType> activityTypeMap;

        ActivityCategoryByStartTimeAdapter(ImmutableMap<Long, BackupData.ActivityType> activityTypeMap) {
            super(DataRowConverter.NUM_FEATURES);
            this.activityTypeMap = activityTypeMap;
        }

        @Override
        protected Input toInput(BackupData.TrackedActivity trackedActivity, int rowNum) {
            DateTime startTime = new DateTime(trackedActivity.getStarttimeMillis());
            return new Input(startTime.toLocalDate(), startTime.toLocalTime());
        }

        @Override
        protected Long toClassIdentifier(BackupData.TrackedActivity trackedActivity) {
            long activityTypeId = trackedActivity.getActivityTypeReference();
            BackupData.ActivityType activityType = activityTypeMap.get(activityTypeId);

            return activityType == null ? -42 : activityType.getActivityTypeCategoryReference();
        }

    }

    private static final class DataRowConverter implements MultiClassAgimePredictionModelImpl.DataRowConverter<Input> {
        private static final int NUM_FEATURES = 6;

        private static double[] setInputRowValues(final double[] row, LocalDate date, final LocalTime time) {
            Preconditions.checkArgument(row.length == NUM_FEATURES);
            int minutes = time.getHourOfDay() * 60 + time.getMinuteOfHour();
            row[0] = 1d;
            row[1] = minutes;
            row[2] = date.getDayOfWeek();
            row[3] = date.getDayOfWeek() * minutes;
            row[4] = Math.pow(minutes, 2);
            row[5] = Math.pow(date.getDayOfWeek(), 2);
            return row;
        }

        @Override
        public double[] fillRow(double[] row, Input inputData) {
            return setInputRowValues(row, inputData.date, inputData.time);
        }
    }

    public static ActivityCategoryByStartTimePrediction train(
            BackupData.PersonalBackup personalBackup,
            final long timeoutMillis
    ) throws java.util.concurrent.TimeoutException {
        ImmutableMap<Long, BackupData.ActivityType> activityTypeMap = Maps.uniqueIndex(personalBackup.getActivityTypesList(), new Function<BackupData.ActivityType, Long>() {
            @Nullable
            @Override
            public Long apply(@Nullable final BackupData.ActivityType input) {
                return input == null ? null : input.getIdentifier();
            }
        });
        MultiClassAgimePredictionModelImpl<Input> model = MultiClassAgimePredictionModelImpl.train(
                personalBackup, timeoutMillis,
                new ActivityCategoryByStartTimeAdapter(activityTypeMap),
                new DataRowConverter()
        );
        return new ActivityCategoryByStartTimePrediction(model);
    }

}
