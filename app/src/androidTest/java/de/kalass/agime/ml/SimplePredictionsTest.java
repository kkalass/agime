package de.kalass.agime.ml;

import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import junit.framework.Assert;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nullable;

import de.kalass.agime.backup.BackupData;
import de.kalass.commons.ml.MultiClassPredictionResult;


/**
 * Test cases for activity predictions using machine learning.
 */
@RunWith(AndroidJUnit4.class)
public class SimplePredictionsTest extends BaseAndroidTestCase {

	private BackupData.PersonalBackup _personalBackup;

	private ImmutableMap<Long, BackupData.ActivityTypeCategory> _categoryMap;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		_personalBackup = loadPersonalBackup(getTestContextExposed(), "agime-backup-2014-04-07.agime");

		_categoryMap = Maps.uniqueIndex(_personalBackup.getActivityTypeCategoriesList(), new Function<BackupData.ActivityTypeCategory, Long>() {

			@Nullable
			@Override
			public Long apply(@Nullable final BackupData.ActivityTypeCategory input) {
				return input.getIdentifier();
			}
		});
	}


	static BackupData.PersonalBackup loadPersonalBackup(Context context, final String resourceName) throws IOException {
		//InputStream inputStream1 = getContext().getAssets().open("sql/00001.sql");
		//Assert.assertNotNull("loading sql from asset", inputStream1);

		//InputStream inputStream2 = getClass().getResourceAsStream("assets/sql/00001.sql");
		//Assert.assertNotNull("loading sql from classpath ", inputStream2);

		InputStream inputStream = context.getAssets().open(resourceName);
		Assert.assertNotNull("Konnte Backup nicht laden " + resourceName, inputStream);
		try {
			return BackupData.PersonalBackup.parseFrom(inputStream);
		}
		finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				}
				catch (IOException e) {
					Log.e(LOG_TAG, "Failed to close ", e);
				}
			}
		}
	}


	// FIXME re-enable
	public void DO_NOT_testActivityCategoryByStartTimePrediction() throws IOException, TimeoutException {
		long startTime = System.currentTimeMillis();
		Log.i(LOG_TAG, "start training ActivityCategoryByStartTimePrediction on " + _personalBackup.getTrackedActivitiesCount() + " Tracked Activities");
		final ActivityCategoryByStartTimePrediction prediction = ActivityCategoryByStartTimePrediction.train(_personalBackup, TimeUnit.SECONDS.toMillis(10 * 60));
		Log.i(LOG_TAG, "finished training ActivityCategoryByStartTimePrediction");
		assertCategoryPrediction(prediction, monday(), new LocalTime(9, 0), 2);
		assertCategoryPrediction(prediction, tuesday(), new LocalTime(9, 0), 2);
		assertCategoryPrediction(prediction, monday(), new LocalTime(10, 0), 2);
		assertCategoryPrediction(prediction, tuesday(), new LocalTime(10, 0), 2);
		assertCategoryPrediction(prediction, tuesday(), new LocalTime(10, 15), 2);
		assertCategoryPrediction(prediction, tuesday(), new LocalTime(11, 0), 2);
		assertCategoryPrediction(prediction, tuesday(), new LocalTime(12, 0), 2);
		assertCategoryPrediction(prediction, tuesday(), new LocalTime(13, 0), 2);
		assertCategoryPrediction(prediction, tuesday(), new LocalTime(14, 0), 1);
		assertCategoryPrediction(prediction, tuesday(), new LocalTime(15, 0), 1);
		assertCategoryPrediction(prediction, tuesday(), new LocalTime(15, 10), 1);
		assertCategoryPrediction(prediction, tuesday(), new LocalTime(16, 0), 1);
		assertCategoryPrediction(prediction, tuesday(), new LocalTime(17, 0), 1);
		Log.i(LOG_TAG, "finished in " + (System.currentTimeMillis() - startTime) + " ms");

	}


	public static LocalDate tuesday() {
		return new LocalDate().withDayOfWeek(DateTimeConstants.TUESDAY);
	}


	public static LocalDate monday() {
		return new LocalDate().withDayOfWeek(DateTimeConstants.MONDAY);
	}


	private void assertCategoryPrediction(final ActivityCategoryByStartTimePrediction prediction, final LocalDate date, final LocalTime startTime,
			long expectedCategoryId) {
		Log.i(LOG_TAG, "Predicting Category for Starttime " + startTime + ", expecting  " + expectedCategoryId);
		ActivityCategoryByStartTimePrediction.Input input = new ActivityCategoryByStartTimePrediction.Input(date, startTime);
		final List<MultiClassPredictionResult<Long>> predict = prediction.predict(input);

		for (MultiClassPredictionResult<Long> r : predict) {
			BackupData.ActivityTypeCategory cat = _categoryMap.get(r.getPredictedValue());
			Log.i(LOG_TAG, "p(y=" + r.getPredictedValue() + ") => " + r.getProbability() + " | " + getName(cat));
		}

		MultiClassPredictionResult<Long> last = predict.get(predict.size() - 1);
		//Assert.assertEquals(pr);
		BackupData.ActivityTypeCategory mostLikely = _categoryMap.get(last.getPredictedValue());
		Log.i(LOG_TAG, "Most likely: " + last + " => " + getName(mostLikely));

		if (expectedCategoryId != mostLikely.getIdentifier()) {
			Log.i(LOG_TAG, "WARN: Wrong category. Expected " + expectedCategoryId + " but got " + mostLikely.getIdentifier());
		}
		//Assert.assertEquals("Wrong category ", expectedCategoryId, mostLikely.getIdentifier());

		//System.err.println("FIRST: " + first);
		//System.err.println("LAST: " + last);
	}


	private String getName(final BackupData.ActivityTypeCategory leastLikey) {
		return (leastLikey == null ? null : (leastLikey.getIdentifier() + "/" + leastLikey.getName()));
	}
}
