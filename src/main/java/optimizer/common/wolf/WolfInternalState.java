package optimizer.common.wolf;

import optimizer.common.InternalStateBase;
import optimizer.common.Solution;
import optimizer.trial.IterationResult;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public class WolfInternalState<AlgoPhase> extends InternalStateBase<Solution> {
    public AlgoPhase phase;

    public WolfInternalState() {
        super();
    }
}