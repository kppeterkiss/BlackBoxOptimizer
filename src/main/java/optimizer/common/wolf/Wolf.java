package optimizer.common.wolf;

import optimizer.algorithms.GreyWolfOptimizer.*;
import optimizer.common.Solution;

import java.util.Random;

public class Wolf extends Solution implements Comparable<Wolf> {

    public Wolf(int dim,  float[] lowerBounds, float[] upperBounds, Random rand) {
        super(dim, lowerBounds, upperBounds, rand);
    }

    @Override
    public int compareTo(Wolf wolf) {
        try {
            if (this.actualFitness.betterThan(wolf.actualFitness))
                return -1;
            else
                return  1;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
