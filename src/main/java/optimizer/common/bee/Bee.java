package optimizer.common.bee;

import optimizer.common.Solution;

import java.util.Random;

public class Bee extends Solution {
    public BeeType type;
    public int trial;
    public int helpingToEmployerId;

    public Bee(int dim, BeeType type) {
        super(dim);
        this.type = type;
        helpingToEmployerId = -1;
    }

    public Bee(int dim, float[] lowerBounds, float[] upperBounds, Random rand, BeeType type) {
        super(dim, lowerBounds, upperBounds, rand);
        this.type = type;
        helpingToEmployerId = -1;
    }

    public void helpingTo(int beeId) {
        helpingToEmployerId = beeId;
    }

    public enum BeeType {
        employer,
        onlooker,
        scout
    }
}
