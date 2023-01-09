package de.kalass.commons.ml;

import com.google.common.base.Objects;


import org.ejml.simple.SimpleMatrix;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.kalass.commons.ml.Octave.checkColumnVector;

/**
* Created by klas on 02.04.14.
*/
public class ThetaAndCosts {
    public final SimpleMatrix theta;
    public final double previousCost;
    public final double currentCost;
    public final int costIndex;

    public ThetaAndCosts(final SimpleMatrix theta, final double previousCost, double currentCost, int costIndex) {
        this.theta = checkColumnVector(checkNotNull(theta));
        this.previousCost = previousCost;
        this.currentCost = currentCost;
        this.costIndex = costIndex;
    }

    @Override
    public String toString() {
        return toStringHelper()
                .toString();
    }

    protected Objects.ToStringHelper toStringHelper() {
        return Objects.toStringHelper(this)
                .add("theta", Octave.toString(theta))
                .add("previousCost", previousCost)
                .add("currentCost", currentCost)
                .add("costIndex", costIndex);
    }
}
