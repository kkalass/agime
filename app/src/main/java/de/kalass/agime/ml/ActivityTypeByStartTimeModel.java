package de.kalass.agime.ml;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.Minutes;
import org.joda.time.Seconds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import de.kalass.agime.backup.BackupData;
import de.kalass.commons.ml.MultiClassPrediction;

/**
* Created by klas on 07.10.14.
*/
public final class ActivityTypeByStartTimeModel extends DelegatingMultiClassAgimePredictionModel<ActivityTypeByStartTimeModel.PredictionInput> {
    private static final Logger LOG = LoggerFactory.getLogger("ActivityTypeByStartTimeModel");

    private final MultiClassAgimePredictionModelImpl<PredictionInput> model;
    private final RowBuilder rowBuilder;

    public ActivityTypeByStartTimeModel(MultiClassAgimePredictionModelImpl<PredictionInput> model, RowBuilder rowBuilder) {
        super(model);
        this.model = model;
        this.rowBuilder = rowBuilder;
    }

    public RowBuilder getRowBuilder() {
        return rowBuilder;
    }

    public MultiClassPrediction<Long> getInternalModel() {
        return this.model.getInternalModel();
    }

    public int getNumPrevious() {
        return this.rowBuilder.getNumPrevious();
    }

    private static final class DataRowConverter implements MultiClassAgimePredictionModelImpl.DataRowConverter<PredictionInput> {
        private final RowBuilder rowBuilder;

        private DataRowConverter(RowBuilder rowBuilder) {
            this.rowBuilder = rowBuilder;
        }

        @Override
        public double[] fillRow(double[] row, PredictionInput inputData) {
            rowBuilder.setInputRowValues(row, inputData);
            return row;
        }
    }

    private static final class ActivityTypeByStartTimeModelAdapter extends MultiClassAgimePredictionModelImpl.Adapter<PredictionInput> {


        private final List<BackupData.TrackedActivity> trackedActivities;
        private final int numPrevious;

        protected ActivityTypeByStartTimeModelAdapter(RowBuilder rowBuilder, List<BackupData.TrackedActivity> trackedActivities) {
            super(rowBuilder.getNumDimensions());
            this.numPrevious = rowBuilder.getNumPrevious();
            this.trackedActivities = trackedActivities;
        }

        @Override
        protected PredictionInput toInput(BackupData.TrackedActivity trackedActivity, int rowNum) {
            Preconditions.checkArgument(trackedActivities.size() > rowNum);
            Preconditions.checkArgument(trackedActivities.get(rowNum).equals(trackedActivity));

            DateTime startTime = new DateTime(trackedActivity.getStarttimeMillis());
            DateTime endTime = new DateTime(trackedActivity.getEndtimeMillis());
            LocalDate date = startTime.toLocalDate();
            LocalTime time = startTime.toLocalTime();
            PredictionInput input = new PredictionInput()
                    .setDate(date)
                    .setStartTime(time)
                    .setEndTime(endTime.toLocalTime())
                    .setPreviousAsc(buildPreviousAsc(
                            trackedActivities, rowNum, date, numPrevious
                    ));
            return input;
        }

        @Override
        protected Long toClassIdentifier(BackupData.TrackedActivity trackedActivity) {

            return trackedActivity.getActivityTypeReference();
        }
    }

    public static final class PredictionInput {
        private LocalDate date;
        private LocalTime startTime;
        private LocalTime endTime;
        private PredictionInput.Previous[] previous;

        /**
         * the previous items in ascending order - in other words;
         *
         * setPreviousAsc(threeBefore, twoBefore, oneBefore);
         */
        public PredictionInput setPreviousAsc(PredictionInput.Previous... previous) {
            this.previous = previous;
            return this;
        }

        public PredictionInput setDate(LocalDate date) {
            this.date = date;
            return this;
        }

        public PredictionInput setStartTime(LocalTime time) {
            this.startTime = time;
            return this;
        }

        public PredictionInput setEndTime(LocalTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public static class Previous {
            private long activityTypeId;
            private int daysBefore;
            private int durationMinutes;

            public PredictionInput.Previous setActivityTypeId(long id) {
                this.activityTypeId = id;
                return this;
            }

            public PredictionInput.Previous setDaysBefore(int daysBefore) {
                this.daysBefore = daysBefore;
                return this;
            }

            public PredictionInput.Previous setDurationMinutes(int durationMinutes) {
                this.durationMinutes = durationMinutes;
                return this;
            }

            @Override
            public String toString() {
                return MoreObjects.toStringHelper(this)
                        .addValue(activityTypeId).add("daysBefore", daysBefore).add("durationMinutes", durationMinutes)
                        .toString();
            }
        }

    }

    /**
     * Predicts the Id of the Activity Type, based on the starttime.
     */
    static final class RowBuilder {
        static final int WEEKDAYS_FIELDS_NUM_SINGLE = 1;
        static final int WEEKDAYS_FIELDS_NUM_ONE_EACH = 7;

        static final int WEEKDAY_FIELDS_BASE = 2;
        private final int _fieldsPerPrevious;
        private final int _weekdayFieldsNum; // 7 for "field per weekday", 1 else
        private final long[] _activityTypeIdsArray;
        private final int _numDimensions;
        private final List<LocalTime> _timeSliceBorders;
        private final int _timeSliceBase;
        private final int _previousBase;
        private final int _numPrevious;

        /**
         * @param activityTypeIdsArray Ids of those activity types that should create a new column for the previous item
         */
        RowBuilder(long[] activityTypeIdsArray) {
            this(WEEKDAYS_FIELDS_NUM_ONE_EACH,
                    activityTypeIdsArray.length == 0 ? 0 : 3,
                    createTimeSliceBorders(new LocalTime(9,0), new LocalTime(18,0)),
                    activityTypeIdsArray);
        }

        RowBuilder(int weekdayFieldsNum, int numPrevious,
                   List<LocalTime> timeSliceBorders,
                   long[] activityTypeIdsArray
        ) {
            _weekdayFieldsNum = weekdayFieldsNum;
            _activityTypeIdsArray = activityTypeIdsArray;
            _timeSliceBase = WEEKDAY_FIELDS_BASE + _weekdayFieldsNum;
            _timeSliceBorders = timeSliceBorders;
            _previousBase = _timeSliceBase + 1 + _timeSliceBorders.size();
            _numPrevious = numPrevious;
            _fieldsPerPrevious = activityTypeIdsArray.length;
            _numDimensions = _previousBase + (_fieldsPerPrevious * _numPrevious);
        }


        long[] getActivityTypeIdsArray() {
            return _activityTypeIdsArray;
        }

        public int getNumPrevious() {
            return _numPrevious;
        }

        int getTimeSliceBase() {
            return _timeSliceBase;
        }

        List<LocalTime> getTimeSliceBorders() {
            return _timeSliceBorders;
        }

        int getWeekdayFieldsNum() {
            return _weekdayFieldsNum;
        }

        public int getNumDimensions() {
            return _numDimensions;
        }

        static List<LocalTime> createTimeSliceBorders(LocalTime min, LocalTime max) {
            ImmutableList.Builder<LocalTime> b = ImmutableList.builder();
            LocalTime border = min;
            while (border.isBefore(max)) {
                b.add(border);
                border = border.plusMinutes(15);
            }
            b.add(max);

            return b.build();
        }

        double[] setDateRangeFlags(final double[] row, final LocalTime start, final LocalTime endTime) {

            LocalTime previousBorder = null;
            LocalTime border = null;
            for (int i = 0; i < _timeSliceBorders.size(); i++) {
                previousBorder = border;
                border = _timeSliceBorders.get(i);
                if (start.isBefore(border) && (previousBorder == null || previousBorder.isBefore(endTime))) {
                    row[_timeSliceBase + i] = 1;
                } else {
                    row[_timeSliceBase + i] = 0;
                }
            }
            if (endTime.isAfter(border)) {
                row[_timeSliceBase + _timeSliceBorders.size()] = 1;
            } else {
                row[_timeSliceBase + _timeSliceBorders.size()] = 0;
            }

            return row;
        }

        double weekdayValue(LocalDate date, int weekday) {
            return date.getDayOfWeek() == weekday ? 1.0: 0.0;
        }

        double[] setInputRowValues(
                final double[] row,
                final PredictionInput values
        ) {
            if (values.previous == null) {
                throw new IllegalArgumentException("Previous data must not be null");
            }
            if (_numPrevious != values.previous.length) {
                throw new IllegalArgumentException("Illegal Format: expected " + _numPrevious + " previous items, but got " + values.previous.length);
            }
            //int minutes = time.getHourOfDay() * 60 + time.getMinuteOfHour();
            Preconditions.checkArgument(row.length == _numDimensions);
            row[0] = 1d;
            row[1] = Seconds.secondsBetween(values.startTime, values.endTime).getSeconds();

            for (int i = 0; i < values.previous.length; i++) {
                PredictionInput.Previous previous = values.previous[i];
                // first version of my previous-algorithm: only take items of same day into consideration
                if (previous != null && previous.daysBefore == 0) {
                    setPreviousFields(row, i, previous);
                }
            }

            setWeekdayFields(row, values.date, _weekdayFieldsNum);

            setDateRangeFlags(row, values.startTime, values.endTime);

            //row[2] = time.getMillisOfDay() * time.getMillisOfDay();
            if (LOG.isTraceEnabled()) {
                String[] headers = headers(row, values);
                for (int i = 0; i < row.length; i++) {
                    LOG.trace(headers[i] + "=" + row[i]);
                }
            }

            return row;
        }

        private String[] headers(final double[] row, final PredictionInput values) {
            String[] names = new String[row.length];
            names[0] = "x0";
            names[1] = "DurationSeconds";
            for (int i = 0; i < values.previous.length; i++) {
                PredictionInput.Previous previous = values.previous[i];
                int idx = _previousBase + (i* _fieldsPerPrevious);
                String prefix = "previous-" + i +"-";
                //names[idx] = prefix + "durationMinutes";
                //names[idx + 1] = prefix + "daysBefore";
                // extend this to one dimension per activity...
                for (int i2 = 0; i2 < _activityTypeIdsArray.length; i2++) {
                    names[idx + i2] = prefix+"activityTypeId-" + _activityTypeIdsArray[i2];
                }
            }
            if (_weekdayFieldsNum == 7) {
                names[WEEKDAY_FIELDS_BASE] = "MO";
                names[WEEKDAY_FIELDS_BASE + 1] = "TUE";
                names[WEEKDAY_FIELDS_BASE + 2] = "WED";
                names[WEEKDAY_FIELDS_BASE + 3] = "THU";
                names[WEEKDAY_FIELDS_BASE + 4] = "FRI";
                names[WEEKDAY_FIELDS_BASE + 5] = "SAT";
                names[WEEKDAY_FIELDS_BASE + 6] = "SUN";
            } else {
                names[WEEKDAY_FIELDS_BASE] = "Weekday";
            }

            for (int i = 0; i < _timeSliceBorders.size(); i++) {
                LocalTime border = _timeSliceBorders.get(i);
                names[_timeSliceBase + i] = "timeSlice-" + i + "-before-" + border;
            }
            names[_timeSliceBase + _timeSliceBorders.size()] = "timeSlice-" + _timeSliceBorders.size() + "-after-" + _timeSliceBorders.get(_timeSliceBorders.size()-1);
            return names;
        }

        private void setPreviousFields(final double[] row, final int i, final PredictionInput.Previous previous) {
            int idx = _previousBase + (i* _fieldsPerPrevious);
            //row[idx] = previous.durationMinutes;
            //row[idx + 1] = previous.daysBefore;
            // extend this to one dimension per activity...
            for (int i2 = 0; i2 < _activityTypeIdsArray.length; i2++) {
                row[idx + i2] = previous.activityTypeId == _activityTypeIdsArray[i2] ? 1.0: 0.0;
            }
        }

        private void setWeekdayFields(final double[] row, final LocalDate date, int numFields) {
            // One Field per weekday: seems to yield equivalent results
            if (numFields == 1) {
                row[WEEKDAY_FIELDS_BASE] = date.getDayOfWeek();
            } else if (numFields == 7) {
                row[WEEKDAY_FIELDS_BASE] = weekdayValue(date, DateTimeConstants.MONDAY);
                row[WEEKDAY_FIELDS_BASE + 1] = weekdayValue(date, DateTimeConstants.TUESDAY);
                row[WEEKDAY_FIELDS_BASE + 2] = weekdayValue(date, DateTimeConstants.WEDNESDAY);
                row[WEEKDAY_FIELDS_BASE + 3] = weekdayValue(date, DateTimeConstants.THURSDAY);
                row[WEEKDAY_FIELDS_BASE + 4] = weekdayValue(date, DateTimeConstants.FRIDAY);
                row[WEEKDAY_FIELDS_BASE + 5] = weekdayValue(date, DateTimeConstants.SATURDAY);
                row[WEEKDAY_FIELDS_BASE + 6] = weekdayValue(date, DateTimeConstants.SUNDAY);
            } else {
                throw new IllegalStateException("Don't know what to do with " + _weekdayFieldsNum + " weekday fields");
            }

        }
    }

    private static final class IdWithCount {
        static final Ordering<IdWithCount> ORDERING = new Ordering<IdWithCount>() {

            @Override
            public int compare(@Nullable final IdWithCount left, @Nullable final IdWithCount right) {
                return left.count - right.count;
            }
        }.reverse();
        final long id;
        final int count;

        private IdWithCount(final long id, final int count) {
            this.id = id;
            this.count = count;
        }

    }

    public static ActivityTypeByStartTimeModel restore(MultiClassPrediction<Long> prediction,
                                                       ActivityTypeByStartTimeModel.RowBuilder rowBuilder
    ) {
        final MultiClassAgimePredictionModelImpl<PredictionInput> model = new MultiClassAgimePredictionModelImpl<>(prediction, new DataRowConverter(rowBuilder), rowBuilder.getNumDimensions());
        return new ActivityTypeByStartTimeModel(model, rowBuilder);
    }

    public static ActivityTypeByStartTimeModel train(
            BackupData.PersonalBackup personalBackup,
            final long timeoutMillis
    ) throws java.util.concurrent.TimeoutException {
        // FIXME try again with taking previous tasks into consideration.
        // 09.04.2014 (KK): If I include previous tasks here, results get a lot worse than for time based predictions.
        /*
        List<IdWithCount> activityTypeIds = IdWithCount.ORDERING.immutableSortedCopy(count(trackedActivities));
        long[] activityTypeIdsArray = new long[Math.min(activityTypeIds.size(), 30)];
        for (int i = 0; i < activityTypeIdsArray.length; i++) {
            activityTypeIdsArray[i] = activityTypeIds.get(i).id;
        }
        final RowBuilder rowBuilder = new RowBuilder(activityTypeIdsArray);
*/
        final RowBuilder rowBuilder = new RowBuilder(new long[0]);

        MultiClassAgimePredictionModelImpl<PredictionInput> model = MultiClassAgimePredictionModelImpl.train(
                personalBackup,
                timeoutMillis,
                new ActivityTypeByStartTimeModelAdapter(rowBuilder, personalBackup.getTrackedActivitiesList()),
                new DataRowConverter(rowBuilder)
        );

        return new ActivityTypeByStartTimeModel(model, rowBuilder);
    }

    private static List<IdWithCount> count(final List<BackupData.TrackedActivity> trackedActivities) {
        ImmutableListMultimap<Long, BackupData.TrackedActivity> map = Multimaps.index(trackedActivities, new Function<BackupData.TrackedActivity, Long>() {
            @Nullable
            @Override
            public Long apply(@Nullable final BackupData.TrackedActivity input) {
                return input == null ? null : input.getActivityTypeReference();
            }
        });
        return ImmutableList.copyOf(Iterables.transform(map.asMap().entrySet(), new Function<Map.Entry<Long, Collection<BackupData.TrackedActivity>>, IdWithCount>() {
            @Nullable
            @Override
            public IdWithCount apply(@Nullable final Map.Entry<Long, Collection<BackupData.TrackedActivity>> input) {
                return new IdWithCount(input.getKey(), input.getValue().size());
            }
        }));
    }

    private static PredictionInput.Previous[] buildPreviousAsc(
            final List<BackupData.TrackedActivity> trackedActivities,
            final int i,
            final LocalDate date,
            final int numPrevious
    ) {
        //LOG.info("buildPreviousAsc(" + i +", " + date + ", " + numPrevious + ")");
        PredictionInput.Previous[] previous = new PredictionInput.Previous[numPrevious];

        for (int ii = 0; ii < previous.length; ii++) {
            int prevIdx = i - (previous.length - ii );
            if (prevIdx < 0 || prevIdx >= i) {

                previous[ii] = null;
            } else {
                BackupData.TrackedActivity prev = trackedActivities.get(prevIdx);

                DateTime prevStartTime = new DateTime(prev.getStarttimeMillis());
                LocalDate prevDate = prevStartTime.toLocalDate();
                DateTime prevEndTime = new DateTime(prev.getEndtimeMillis());
                int prevDurationMinutes = Minutes.minutesBetween(prevEndTime, prevStartTime).getMinutes();
                previous[ii] = new PredictionInput.Previous()
                    .setActivityTypeId(prev.getActivityTypeReference())
                    .setDaysBefore(Days.daysBetween(prevDate, date).getDays())
                    .setDurationMinutes(prevDurationMinutes);

            }
            //LOG.info("prevIdx=" + prevIdx + ", i=" + i + ", ii="+ii + " => " + previous[ii]);
        }
        return  previous;
    }
}
