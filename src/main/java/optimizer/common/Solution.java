package optimizer.common;

import optimizer.trial.IterationResult;

import java.util.Random;

public class Solution {
    public float[] position;
    public float[] newPosition;
    public IterationResult actualFitness;

    public Solution(int dim, float[] lowerBounds, float[] upperBounds, Random rand) {
        this.position = new float[dim];
        this.newPosition = new float[dim];
        this.actualFitness = null;

        for(int i = 0; i < dim; ++i) {
            float r = rand.nextFloat();
            this.position[i] = lowerBounds[i] + r * (upperBounds[i] - lowerBounds[i]);
        }
    }

    public void checkBoundsForNewPosition(int dimension, float[] lowerBounds, float[] upperBounds) {
        for (int i = 0; i < dimension; ++i) {
            if (newPosition[i] > upperBounds[i]) {
                newPosition[i] = upperBounds[i];
            } else if (newPosition[i] < lowerBounds[i]) {
                newPosition[i] = lowerBounds[i];
            }
        }
    }
}
