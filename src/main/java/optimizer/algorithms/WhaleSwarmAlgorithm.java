package optimizer.algorithms;

import optimizer.common.InternalStateBase;
import optimizer.common.Solution;
import optimizer.math.Distance;
import optimizer.param.Param;
import optimizer.trial.IterationResult;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class WhaleSwarmAlgorithm extends AbstractAlgorithm {
    InternalState state = new InternalState();
    Random rand = new Random();

    {
        this.parallelizable = ParallelExecution.GENERATION;
        this.optimizerParams = new LinkedList<>();
        this.optimizerParams.add(new Param(10,Integer.MAX_VALUE,5,"swarm_size"));
        // intensity of ultrasound at the origin of source
        this.optimizerParams.add(new Param(2,100,0,"ro0"));
        // probability of message distortion at large distances
        this.optimizerParams.add(new Param(0.002,1,0,"eta"));
    }

    private void initSearchSpace(List<Param> parameterMap) {
        state.initSearchSpace(parameterMap);
    }

    private void initWhales(int swarm_size) {
        for (int i = 0; i < swarm_size; ++i) {
            state.swarm.add(new Solution(
                    state.dimension,
                    state.lowerBounds,
                    state.upperBounds,
                    rand));
        }
    }

    @Override
    public void updateParameters(List<Param> parameterMap, List<IterationResult> landscape) throws CloneNotSupportedException {
        int swarm_size = ((Number)optimizerParams.get(0).getValue()).intValue();
        switch (state.phase) {
            case init:
                initSearchSpace(parameterMap);
                initWhales(swarm_size);
                break;
            case iteration:
                for (int i = 0; i < swarm_size; ++i) {
                   int closestBetteWhale = state.getClosestBetterWhale(i);
                   if (closestBetteWhale != -1) {
                       float modifier = rand.nextFloat() * (float)ultrasoundIntensity(i, closestBetteWhale);
                       for (int dim = 0; dim < state.dimension; ++dim) {
                           getWhale(i).newPosition[dim] =
                                   modifier * (getWhale(closestBetteWhale).position[dim] - getWhale(i).position[dim]);
                       }
                       getWhale(i).checkBoundsForNewPosition(state.dimension, state.lowerBounds, state.upperBounds);
                   } else {
                       getWhale(i).newPosition = getWhale(i).position.clone();
                   }

                }
                break;
        }
    }

    public List<List<Param>> getParameterMapBatch(List<Param> pattern)throws CloneNotSupportedException {
        List<List<Param>> result = new LinkedList<>();
        for (int j = 0; j < state.swarm.size(); ++j) {
            List<Param> setup = Param.cloneParamList(pattern);
            // setup each dimension of the position
            for (int i = 0; i < setup.size(); ++i) {
                setup.get(i).setInitValue(getWhale(j).newPosition[i]);
                setup.get(i).setId(j);
            }
            result.add(setup);
        }
        return result;
    }

    public void setResults(List<IterationResult> results) throws CloneNotSupportedException {
        for (IterationResult res : results) {
            Solution whale = getWhale(res.getConfiguration().get(0).getId());
            whale.saveResultAndPosition(res);
            state.setBest(whale);
        }
    }

    public void updateGlobals() throws CloneNotSupportedException {
        switch (state.phase) {
            case init:
                state.phase = AlgorithmPhase.iteration;
                break;
            case iteration:
                break;
        }
    }

    /*
    // The intensity ro of the ultrasound  at  any  distance d from  the  source
    */
    public double ultrasoundIntensity(int i, int j) {
        double ro = 0;
        float distance = Distance.distance(state.dimension, getWhale(i).position, getWhale(j).position);
        float ro0 = ((Number)optimizerParams.get(1).getValue()).floatValue();
        float eta = ((Number)optimizerParams.get(2).getValue()).floatValue();
        ro = ro0 * Math.exp(-eta * distance);
        return ro;
    }

    Solution getWhale(int id) {
        return state.swarm.get(id);
    }
    
    public enum AlgorithmPhase {
        init,
        iteration
    }

    public class InternalState extends InternalStateBase<Solution> {
        public AlgorithmPhase phase;

        public InternalState() {
            super();
            phase = AlgorithmPhase.init;
        }

        public int getClosestBetterWhale(int id) throws CloneNotSupportedException {
            float tempDist = Float.MAX_VALUE;
            int ret = -1;

            for (int i = 0; i < swarm.size(); ++i ) {
                if (swarm.get(i).getActualFitness().betterThan(swarm.get(id).getActualFitness())) {
                    float distance = Distance.distance(dimension, swarm.get(i).position, swarm.get(id).position);
                    if (distance < tempDist) {
                        ret = i;
                        tempDist = distance;
                    }
                }
            }
            return ret;
        }

    }
}
