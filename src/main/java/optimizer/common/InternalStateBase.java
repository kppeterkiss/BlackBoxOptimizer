package optimizer.common;

import optimizer.trial.IterationResult;

import java.util.ArrayList;

public class InternalStateBase<T> {
    public ArrayList<T> swarm;
    public float[] swarmBestKnownPosition;
    public IterationResult swarmBestFitness;

    public boolean firstStep = true;

    public float[] upperBounds;
    public float[] lowerBounds;
    public int dimension;

    public InternalStateBase() {}
}
