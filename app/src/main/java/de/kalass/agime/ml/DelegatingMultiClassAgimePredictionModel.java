package de.kalass.agime.ml;

import java.util.List;

import de.kalass.commons.ml.MultiClassPredictionResult;

/**
 * Created by klas on 08.10.14.
 */
public class DelegatingMultiClassAgimePredictionModel<I> implements MultiClassAgimePredictionModel<I> {
    private final MultiClassAgimePredictionModel<I> _model;

    public DelegatingMultiClassAgimePredictionModel(MultiClassAgimePredictionModel<I> model) {
        _model = model;
    }

    @Override
    public List<MultiClassPredictionResult<Long>> predict(I inputData) {
        return _model.predict(inputData);
    }
}
