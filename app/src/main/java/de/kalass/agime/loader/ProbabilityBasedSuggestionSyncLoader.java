package de.kalass.agime.loader;

import android.content.Context;
import android.util.Log;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.Minutes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import de.kalass.agime.customfield.ActivityCustomFieldDataSyncLoader;
import de.kalass.agime.ml.ActivityTypeByStartTimeModel;
import de.kalass.agime.ml.PredictionSerializer;
import de.kalass.agime.model.ActivityTypeModel;
import de.kalass.agime.model.ActivityTypeSuggestionModel;
import de.kalass.agime.model.TrackedActivityModel;
import de.kalass.agime.prediction.PredictionData;
import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.simpleloader.CompoundSyncLoader;
import de.kalass.commons.ml.MultiClassPredictionResult;

/**
 * Created by klas on 22.10.13.
 */
public class ProbabilityBasedSuggestionSyncLoader extends CompoundSyncLoader {
    private static final String LOG_TAG = "PredictingSuggestion";
    private final ActivityTypeSyncLoader _activityTypeLoader;
    private final TrackedActivitySyncLoader _trackedActivitySyncLoader;
    private final ActivityTypeSuggestionSyncLoader _activityTypeSuggestionLoader;
    private boolean initialized;
    private ActivityTypeByStartTimeModel _model;

    public ProbabilityBasedSuggestionSyncLoader(Context context,
                                                ProjectSyncLoader projectSyncLoader,
                                                @Nullable ActivityCustomFieldDataSyncLoader customFieldDataSyncLoader,
                                                ActivityTypeSyncLoader activityTypeLoader,
                                                ActivityTypeSuggestionSyncLoader activityTypeSuggestionLoader) {
        super(context);
        _trackedActivitySyncLoader = add(new TrackedActivitySyncLoader(context, activityTypeLoader, projectSyncLoader, customFieldDataSyncLoader));
        _activityTypeLoader = add(activityTypeLoader);
        _activityTypeSuggestionLoader = add(activityTypeSuggestionLoader);
    }

    private ActivityTypeByStartTimeModel readPredictionState() throws IOException {
        final Context context = getContext();
        final File dir =  context.getExternalFilesDir("Agime");
        final File f = new File(dir, "data.predict");
        if (!f.exists()) {
            Log.i(LOG_TAG, "Cannot find training data " + f + ", will continue with default ordering");
            return null;
        }
        InputStream inputStream = new FileInputStream(f);
        try {
            return PredictionSerializer.fromProtobuf(PredictionData.ActivityPredictionData.parseFrom(inputStream));
        } finally {
            inputStream.close();
        }
    }

    public List<ActivityTypeSuggestionModel> loadByLastUse(LocalDate refDay) {
        final String order = MCContract.ActivityType.COLUMN_NAME_NAME + " asc";
        final Map<Long, ActivityTypeModel> candidateActivities = _activityTypeLoader.loadAsMap(null, null, order);
        return _activityTypeSuggestionLoader.queryAll(candidateActivities,
                ImmutableList.<Long>of() /*do not take projects into consideration, just load all activities*/,
                refDay,
                // include all activities, no matter wether or not the project(s) for which the activity was entered is active or not
                ImmutableSet.<ActivityTypeSuggestionFeature>of(ActivityTypeSuggestionFeature.SUGGEST_ALL_ACTIVITIES));
    }

    public List<ActivityTypeSuggestionModel> query(
            final LocalDate localDate,
            final LocalTime startTime,
            LocalTime endTime
    ) {
        List<ActivityTypeSuggestionModel> allModels = loadByLastUse(localDate);
        ensureInitialized();
        if (_model != null) {
            long starttimeMillis = localDate.toDateTime(startTime).getMillis();

            List<TrackedActivityModel> trackedActivityModels = _trackedActivitySyncLoader.queryPreviousDesc(starttimeMillis, _model.getNumPrevious());
            ActivityTypeByStartTimeModel.PredictionInput.Previous[] previous = new ActivityTypeByStartTimeModel.PredictionInput.Previous[_model.getNumPrevious()];
            for (int i = 0; i < previous.length; i++) {
                int idx = trackedActivityModels.size() - i - 1;
                if (idx >= 0) {
                    TrackedActivityModel model = trackedActivityModels.get(idx);
                    if (model != null) {
                        DateTime startTimeDateTime = model.getStarttimeDateTimeMinutes();
                        int daysBetween = Days.daysBetween(startTimeDateTime.toLocalDate(), localDate).getDays();
                        int durationMinutes = Minutes.minutesBetween(startTimeDateTime, model.getEndtimeDateTimeMinutes()).getMinutes();
                        ActivityTypeModel activityType = model.getActivityType();
                        previous[i] = new ActivityTypeByStartTimeModel.PredictionInput.Previous()
                                .setDaysBefore(daysBetween)
                                .setDurationMinutes(durationMinutes)
                                .setActivityTypeId(activityType == null ? 0 : activityType.getId());
                    }
                }
            }

            ActivityTypeByStartTimeModel.PredictionInput predictionInput = new ActivityTypeByStartTimeModel.PredictionInput()
                    .setDate(localDate)
                    .setStartTime(startTime)
                    .setEndTime(endTime)
                    .setPreviousAsc(previous);
            List<MultiClassPredictionResult<Long>> predictionResults = _model.predict(
                    predictionInput
            );

            return applyPredictionToOrdering(allModels, predictionResults);
        }
        return allModels;
    }

    private List<ActivityTypeSuggestionModel> applyPredictionToOrdering(final List<ActivityTypeSuggestionModel> allModels, final List<MultiClassPredictionResult<Long>> predictionResults) {
        List<MultiClassPredictionResult<Long>> relevantResults = predictionResults.subList(Math.max(0, predictionResults.size() - 3), predictionResults.size());
        LinkedList<ActivityTypeSuggestionModel> result = Lists.newLinkedList(allModels);
        for (MultiClassPredictionResult<Long> predictionResult: relevantResults) {
            ActivityTypeSuggestionModel model = take(result, predictionResult);
            if (model != null) {
                result.addFirst(model);
            }
        }
        return result;
    }

    private ActivityTypeSuggestionModel take(final LinkedList<ActivityTypeSuggestionModel> result, final MultiClassPredictionResult<Long> predictionResult) {
        Iterator<ActivityTypeSuggestionModel> it = result.iterator();
        while (it.hasNext()) {
            ActivityTypeSuggestionModel model = it.next();
            if (Objects.equal(predictionResult.getPredictedValue(), model.getActivityTypeId())) {
                it.remove();
                return model;
            }
        }
        return null;
    }

    private void ensureInitialized() {
        if (!initialized) {
            try {
                _model = readPredictionState();
            } catch (IOException e) {
                Log.w(LOG_TAG, "Cannot read predictions, will continue with normal ordering", e);
            } catch (RuntimeException e) {
                Log.w(LOG_TAG, "Cannot read predictions, will continue with normal ordering", e);
            }
            initialized = true;
        }
    }


}
