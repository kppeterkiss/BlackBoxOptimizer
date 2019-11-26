package optimizer.common.wolf;

import optimizer.common.InternalStateBase;
import optimizer.trial.IterationResult;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public class WolfInternalState<AlgoPhase> extends InternalStateBase<Wolf> {
    public AlgoPhase phase;

    public WolfInternalState() {
        super();
    }

    public void setBest(Wolf wolf) throws CloneNotSupportedException {
        if(wolf.actualFitness.betterThan(swarmBestFitness)) {
            swarmBestFitness = wolf.actualFitness;
            swarmBestKnownPosition = wolf.position.clone();
        }
    }
}