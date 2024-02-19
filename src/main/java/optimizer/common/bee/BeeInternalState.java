package optimizer.common.bee;

import optimizer.common.InternalStateBase;
import optimizer.trial.IterationResult;

import java.util.*;

public class BeeInternalState<AlgoPhase> extends InternalStateBase<Bee> {
    public AlgoPhase phase;

    public BeeInternalState() {
        super();
    }

    public void setAllResults(List<IterationResult> results) throws CloneNotSupportedException {
        for (IterationResult res : results) {
            Bee bee = swarm.get(res.getConfiguration().get(0).getId());
            bee.saveResultAndPosition(res);
            setBest(bee);
        }
    }

    /*
     * Move bee with "beeId" towards the otherBeeId
     * in a random dimension
     */
    public void moveBee(int beeId, int otherBeeId, Random rand) {
        // select random dimension
        int d = rand.nextInt(dimension);
        // create new solution
        swarm.get(beeId).newPosition = swarm.get(beeId).position.clone();
        // [-1,1] , min + Math.random() * (max - min);
        swarm.get(beeId).newPosition[d] +=
                (rand.nextFloat() * 2 - 1) *
                        (swarm.get(beeId).position[d] - swarm.get(otherBeeId).position[d]);

        swarm.get(beeId).checkBoundsForNewPosition(dimension, lowerBounds, upperBounds);
    }

    /*
     * Generate an int between 0 and max with
     * the exclusion of "excluded"
     */
    public int getRandomBee(int excluded, int max, Random rand) {
        int num = rand.nextInt(max - 1);
        if (num >= excluded) {
            ++num;
        }
        return num;
    }
}
