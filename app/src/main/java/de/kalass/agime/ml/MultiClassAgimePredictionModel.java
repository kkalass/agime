package de.kalass.agime.ml;

import java.util.List;

import de.kalass.commons.ml.MultiClassPredictionResult;

/**
 * Created by klas on 08.10.14.
 */
public interface MultiClassAgimePredictionModel<I> {
    List<MultiClassPredictionResult<Long>> predict(I inputData);
}
