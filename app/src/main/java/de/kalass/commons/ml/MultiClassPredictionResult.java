package de.kalass.commons.ml;

import com.google.common.base.Objects;

/**
 * Created by klas on 04.04.14.
 */
public class MultiClassPredictionResult<T> {
    private final T _predictedValue;
    private final double _probability;


    public MultiClassPredictionResult(final T predictedValue, final double probability) {
        _predictedValue = predictedValue;
        _probability = probability;
    }

    public double getProbability() {
        return _probability;
    }

    public T getPredictedValue() {
        return _predictedValue;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("value", _predictedValue)
                .add("probability", _probability)
                .toString();
    }
}
