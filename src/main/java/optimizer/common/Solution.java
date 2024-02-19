package optimizer.common;

import optimizer.trial.IterationResult;
import org.apache.commons.math3.special.Gamma;

import java.util.Random;

public class Solution implements Comparable<Solution>{
    public float[] position;
    public float[] newPosition;

    public IterationResult getActualFitness() {
        return actualFitness;
    }

    public IterationResult actualFitness;

    public Solution(int dim) {
        this.position = new float[dim];
        this.newPosition = new float[dim];
        this.actualFitness = null;
    }

    public Solution(int dim, float[] lowerBounds, float[] upperBounds, Random rand) {
        this(dim);

        for(int i = 0; i < dim; ++i) {
            float r = rand.nextFloat();
            this.newPosition[i] = lowerBounds[i] + r * (upperBounds[i] - lowerBounds[i]);
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

    public void saveResultAndPosition(IterationResult result) {
        actualFitness = result;
        position = newPosition.clone();
    }

    public void levy_flights(double beta, double alpha, Random rand, float[] swarmBestKnownPosition) {
        int dimension = position.length;
        double tmpdiv = Gamma.gamma((1 + beta) / 2) * beta * Math.pow(2,((beta - 1) / 2));
        double sigma = Math.pow((Gamma.gamma(1 + beta) * Math.sin(Math.PI * beta / 2)) / tmpdiv, 1 / beta) ;

        // Levy flights by Mantegna's algorithm
        double[] step = new double[dimension];
        for (int i = 0; i < dimension; ++i) {
            // u / (v)^(1/beta)
            step[i] = (rand.nextGaussian() * sigma) / Math.pow(Math.abs(rand.nextGaussian()), 1 / beta);
        }

        //stepSize = alpha * step[i] * (s - best);
        for (int i = 0; i < dimension; ++i) {
            newPosition[i] = position[i] + (float) (alpha * step[i] * (position[i] - swarmBestKnownPosition[i]));
        }
    }

    @Override
    public int compareTo(Solution solution) {
        try {
            if (this.actualFitness.betterThan(solution.actualFitness))
                return -1;
            else
                return  1;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
