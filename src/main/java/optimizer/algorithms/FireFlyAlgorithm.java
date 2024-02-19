package optimizer.algorithms;

import optimizer.common.InternalStateBase;
import optimizer.common.Solution;
import optimizer.param.Param;
import optimizer.trial.IterationResult;

import java.util.*;

public class FireFlyAlgorithm extends AbstractAlgorithm {
    InternalState state = new InternalState();
    Random rand = new Random();

    {
        this.optimizerParams = new LinkedList<>();
        this.optimizerParams.add(new Param(20,Integer.MAX_VALUE,1,"swarm_size"));
        // attractiveness
        this.optimizerParams.add(new Param(30.0, 100.0,0, "beta0"));
        // randomization param [0-1]
        this.optimizerParams.add(new Param(0.2, 1.0,0, "alpha"));
        // light absorption coefficient
        this.optimizerParams.add(new Param(0.5, 100,0.001, "gamma"));

        this.parallelizable = ParallelExecution.GENERATION;
    }

    private void initSearchSpace(List<Param> parameterMap) {
        state.initSearchSpace(parameterMap);
    }

    private void initFireFlies(int swarmSize) {
        //initialize the fireflies, and add to firefly array
        for (int i = 0; i < swarmSize; ++i) {
            state.swarm.add(new Solution(
                    state.dimension,
                    state.lowerBounds,
                    state.upperBounds,
                    rand));
        }
    }
    @Override
    public void updateParameters(List<Param> parameterMap, List<IterationResult> landscape) {
        // intit
        int swarm_size = ((Number)optimizerParams.get(0).getValue()).intValue();
        if (state.init) {
            initSearchSpace(parameterMap);
            initFireFlies(swarm_size);
            state.init = false;
        } else {
            float alpha = ((Number)optimizerParams.get(2).getValue()).floatValue();
            for (int i = 0; i < swarm_size; i++) {
                getFirefly(i).newPosition = getFirefly(i).position.clone();
                for (int j = 0; j < swarm_size; j++) {
                    try {
                        if (getFirefly(j).actualFitness.betterThan(getFirefly(i).actualFitness)) {
                            float distance_i_j = distance(getFirefly(i).position, getFirefly(j).position);
                            //Vary attractiveness with distance r via exp(/gamma*r)
                            float beta = getAttractiveness(distance_i_j);
                            //move firefly i towards j;
                            for (int dim = 0; dim < state.dimension; dim++) {
                                getFirefly(i).newPosition[dim] +=
                                        beta * (getFirefly(j).position[dim] - getFirefly(i).newPosition[dim])
                                                + alpha * (rand.doubles(0,1).limit(1).findFirst().getAsDouble() - 0.5);
                            }
                        }
                    } catch (CloneNotSupportedException e) {
                        e.printStackTrace();
                    }
                }
                getFirefly(i).checkBoundsForNewPosition(state.dimension,
                        state.lowerBounds, state.upperBounds);
            }
        }
    }

    @Override
    public List<List<Param>> getParameterMapBatch(List<Param> pattern)throws CloneNotSupportedException {
        List<List<Param>> result = new LinkedList<>();
        for(int j = 0; j < state.swarm.size(); ++j) {
            List<Param> setup = Param.cloneParamList(pattern);
            // setup each dimension of the position
            for(int i = 0; i < setup.size(); ++i) {
                setup.get(i).setInitValue(getFirefly(j).newPosition[i]);
                setup.get(i).setId(j);
            }
            result.add(setup);
        }
        return result;
    }

    public void setResults(List<IterationResult> results) throws CloneNotSupportedException {
        for (IterationResult res : results) {
            Solution firefly = getFirefly(res.getConfiguration().get(0).getId());
            firefly.saveResultAndPosition(res);
            state.setBest(firefly);

        }
    }

    private float getAttractiveness(float dist) {
        float beta0 = ((Number) this.optimizerParams.get(1).getValue()).floatValue(); //beta0
        float gamma = ((Number) this.optimizerParams.get(3).getValue()).floatValue(); //gamma
        return (beta0 / (1 + gamma*dist*dist));
    }

    public static float distance(float[] a, float[] b) {
        float dist = 0;
        for(int i = 0; i < a.length; i++) {
            dist += Math.pow((a[i] - b[i]), 2);
        }
        return (float) Math.sqrt(dist);
    }

    Solution getFirefly(int id) {
        return state.swarm.get(id);
    }

    class InternalState extends InternalStateBase<Solution> {
        public InternalState() {
            super();
        }
    }
}


