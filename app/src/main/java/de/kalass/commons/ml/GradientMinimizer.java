package de.kalass.commons.ml;

import java.util.concurrent.TimeoutException;

/**
 * Created by klas on 04.04.14.
 */
public interface GradientMinimizer {
    /**
     * Minimizes the function, choosing parameters automatically
     * @param costFunction the function to optimize
     * @param timeoutMillis the maximum time this function is allowed to run
     * @return the optimal theta for the function
     * @throws java.util.concurrent.TimeoutException if minimization incl. paramter detection takes too long
     */
    ThetaAndCosts minimize(
            MinimizationInputFunction costFunction,
            long timeoutMillis
    ) throws TimeoutException;
}
