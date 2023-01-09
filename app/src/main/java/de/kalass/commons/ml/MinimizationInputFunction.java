package de.kalass.commons.ml;

import com.google.common.base.Function;
import org.ejml.simple.SimpleMatrix;

/**
 * Created by klas on 04.04.14.
 */
public interface MinimizationInputFunction extends Function<SimpleMatrix/*theta*/, CostAndGradient> {
    public int getNumDimensions();
}
