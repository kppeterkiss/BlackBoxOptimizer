package optimizer.common;

import optimizer.param.Param;
import optimizer.trial.IterationResult;

import java.util.ArrayList;
import java.util.List;

public class InternalStateBase<T> {
    public ArrayList<T> swarm;
    public float[] swarmBestKnownPosition;
    public IterationResult swarmBestFitness;

    public boolean firstStep = true;

    public float[] upperBounds;
    public float[] lowerBounds;
    public int dimension;

    public InternalStateBase() {
        swarm = new ArrayList<>();
        swarmBestKnownPosition = null;
        swarmBestFitness = null;
        firstStep = true;
        upperBounds = null;
        lowerBounds = null;
        dimension = -1;
    }

    public void initSearchSpace(List<Param> parameterMap) {
        dimension  = parameterMap.size();
        lowerBounds = new float[dimension];
        upperBounds = new float[dimension];
        for(int i = 0; i < dimension; ++i) {
            lowerBounds[i] = ((Number)parameterMap.get(i).getLowerBound()).floatValue();
            upperBounds[i] = ((Number)parameterMap.get(i).getUpperBound()).floatValue();
        }
    }
}
