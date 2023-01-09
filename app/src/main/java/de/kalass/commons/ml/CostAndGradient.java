package de.kalass.commons.ml;


import org.ejml.simple.SimpleMatrix;

/**
* Created by klas on 02.04.14.
*/
public class CostAndGradient {
    public final SimpleMatrix gradient;
    public final double cost;

    public CostAndGradient(final SimpleMatrix gradient, final double cost) {
        this.gradient = gradient;
        this.cost = cost;
    }
}
