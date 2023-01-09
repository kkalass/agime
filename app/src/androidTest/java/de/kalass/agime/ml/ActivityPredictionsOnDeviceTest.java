package de.kalass.agime.ml;

import android.content.Context;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

import junit.framework.Assert;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nullable;

import de.kalass.agime.backup.BackupData;
import de.kalass.agime.prediction.PredictionData;
import de.kalass.commons.ml.MultiClassPredictionResult;

import static de.kalass.agime.ml.SimplePredictionsTest.loadPersonalBackup;
import static de.kalass.agime.ml.SimplePredictionsTest.monday;
import static de.kalass.agime.ml.SimplePredictionsTest.tuesday;

/**
 * Created by klas on 07.04.14.
 */
public class ActivityPredictionsOnDeviceTest extends BaseAndroidTestCase {
    private static final Logger LOG = LoggerFactory.getLogger("TestCase");
    public static final long ACTIVITY_ID_PROGRAMMING = 1;
    public static final long ACTIVITY_ID_LUNCH = 10;
    public static final long ACTIVITY_ID_MONDAY_MORNING_ROUTINE = 13;
    private static final long ACTIVITY_ID_COFFEE = 3;

    private BackupData.PersonalBackup _personalBackup;

    private Map<Long, BackupData.ActivityType> _trackedActivites;
    private ActivityTypeByStartTimeModel _prediction;


    protected void setUp() throws Exception {
        super.setUp();
        _personalBackup = loadPersonalBackup(getTestContextExposed(), "agime-backup-2014-04-07.agime");
        long startTime = System.currentTimeMillis();

        //LOG.info("start training Tracked Activities ");
        //_prediction = AgimePredictions.ActivityTypeByStartTimeModel.train(_personalBackup.getTrackedActivitiesList(), TimeUnit.SECONDS.toMillis(40 * 60));
        //LOG.info("finished training on Tracked Activities in " + (System.currentTimeMillis() - startTime) + " ms");

        LOG.info("start loading Tracked Activities training result");
        _prediction = restoreFromFileInContext(getTestContextExposed(), "agime-backup-2014-04-07.predict");
        LOG.info("finished loading Tracked Activities training result in " + (System.currentTimeMillis() - startTime) + " ms");

        _trackedActivites = Maps.uniqueIndex(_personalBackup.getActivityTypesList(), new Function<BackupData.ActivityType, Long>() {
            @Nullable
            @Override
            public Long apply(@Nullable final BackupData.ActivityType input) {
                return input == null ? null : input.getIdentifier();
            }
        });
    }

    private ActivityTypeByStartTimeModel restoreFromFileInContext(final Context context, String resourceName) throws IOException {
        InputStream inputStream = context.getAssets().open(resourceName);
        Assert.assertNotNull("Konnte Backup nicht laden " + resourceName, inputStream);
        try {
            return PredictionSerializer.fromProtobuf(PredictionData.ActivityPredictionData.parseFrom(inputStream));
        } finally {
            inputStream.close();
        }
    }



    public Object[][] data() {
        return new Object[][] {
                {monday(), new LocalTime(9, 0), new LocalTime(10, 0), ACTIVITY_ID_MONDAY_MORNING_ROUTINE},
                {monday(), new LocalTime(9, 0), new LocalTime(9, 10), ACTIVITY_ID_MONDAY_MORNING_ROUTINE},
                {tuesday(), new LocalTime(9, 0), new LocalTime(9, 10), ACTIVITY_ID_COFFEE},
                {tuesday(), new LocalTime(12, 0), new LocalTime(13, 0), ACTIVITY_ID_LUNCH},
                {tuesday(), new LocalTime(14, 0), new LocalTime(15, 0), ACTIVITY_ID_PROGRAMMING},
        };
    }

    public void testActivityType0() throws IOException, TimeoutException {
        testActivityTypeByStartTimePrediction(data()[0]);
    }

    public void testActivityType1() throws IOException, TimeoutException {
        testActivityTypeByStartTimePrediction(data()[1]);
    }

    public void testActivityType2() throws IOException, TimeoutException {
        testActivityTypeByStartTimePrediction(data()[2]);
    }

    public void testActivityType3() throws IOException, TimeoutException {
        testActivityTypeByStartTimePrediction(data()[3]);
    }

    public void testActivityType4() throws IOException, TimeoutException {
        testActivityTypeByStartTimePrediction(data()[4]);
    }

    private void testActivityTypeByStartTimePrediction(final Object [] row) throws IOException, TimeoutException {
        testActivityTypeByStartTimePrediction((LocalDate)row[0], (LocalTime)row[1], (LocalTime)row[2], (Long)row[3]);
    }

    private void testActivityTypeByStartTimePrediction(final LocalDate date, final LocalTime startTime, LocalTime endTime, long expectedActivityTypeId) throws IOException, TimeoutException {

        final List<MultiClassPredictionResult<Long>> predict = _prediction.predict(
                buildPredictionInput(_prediction.getRowBuilder().getNumPrevious(), date, startTime, endTime));

        for (MultiClassPredictionResult<Long> r: predict.subList(predict.size() - 3, predict.size())) {
            BackupData.ActivityType activityType = _trackedActivites.get(r.getPredictedValue());
            LOG.debug("p(y=" +r.getPredictedValue() + ") => " + r.getProbability() + " | " + getName(activityType));
        }

        MultiClassPredictionResult<Long> last = predict.get(predict.size() - 1);

        BackupData.ActivityType mostLikely = _trackedActivites.get(last.getPredictedValue());
        LOG.info("Predicting Activity Type for " + startTime+ " - " + endTime+ ", expected  " + expectedActivityTypeId + ", got " + getName(mostLikely) + " " + last);
        LOG.info("Weekday is " + new LocalDate().getDayOfWeek() + ", expected " + DateTimeConstants.TUESDAY);
        Assert.assertNotNull("No Type for Id " + last.getPredictedValue(), mostLikely);
        Assert.assertEquals("Wrong Activity Type " + getName(mostLikely), expectedActivityTypeId, mostLikely.getIdentifier());
    }

    private ActivityTypeByStartTimeModel.PredictionInput buildPredictionInput(
            int numPrev,
            final LocalDate date, final LocalTime startTime, final LocalTime endTime) {
        return new ActivityTypeByStartTimeModel.PredictionInput()
            .setPreviousAsc(new ActivityTypeByStartTimeModel.PredictionInput.Previous[numPrev])
            .setDate(date)
            .setStartTime(startTime)
            .setEndTime(endTime);
    }

    private String getName(final BackupData.ActivityType activityType) {
        return (activityType == null ? null : (activityType.getIdentifier() + "/" + activityType.getName()));
    }
}
