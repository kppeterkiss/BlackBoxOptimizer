package optimizer.algorithms;

import optimizer.common.Solution;
import optimizer.param.Param;
import optimizer.trial.IterationResult;
import optimizer.utils.Utils;
import org.junit.Test;

import java.util.*;

import static junit.framework.TestCase.assertEquals;


public class FireFlyAlgorithm extends AbstractAlgorithm {
    InternalState state = new InternalState();
    Random rand = new Random();

    {
        this.optimizerParams = new LinkedList<>();
        this.optimizerParams.add(new Param(5,Integer.MAX_VALUE,1,"max_generation"));
        this.optimizerParams.add(new Param(5,Integer.MAX_VALUE,1,"population_size"));
        // attractiveness
        this.optimizerParams.add(new Param(1.0, Utils.FLOAT_REDEFINED_MAX_VALUE,Float.MIN_VALUE, "beta0"));
        // randomization param [0-1]
        this.optimizerParams.add(new Param(0.5, Utils.FLOAT_REDEFINED_MAX_VALUE,Float.MIN_VALUE, "alpha"));
        // variation of attractiveness
        this.optimizerParams.add(new Param(0.5, Utils.FLOAT_REDEFINED_MAX_VALUE,Float.MIN_VALUE, "gamma"));

        this.parallelizable = ParallelExecution.GENERATION;
    }

    private void initSearchSpace(List<Param> parameterMap){
        state.dimension  = parameterMap.size();
        state.lowerBounds = new float[state.dimension];
        state.upperBounds = new float[state.dimension];
        for(int i = 0; i < state.dimension; ++i) {
            state.lowerBounds[i] = ((Number)parameterMap.get(i).getLowerBound()).floatValue();
            state.upperBounds[i] = ((Number)parameterMap.get(i).getUpperBound()).floatValue();
        }
    }

    private void initFireFlies(int swarmSize) {
        //array to store all parameters
        state.swarm = new ArrayList<>();
        //initialize the fireflies, and add to firefly array
        for (int i = 0; i < swarmSize; ++i) {
            state.swarm.add(new FireFly(
                    state.dimension,
                    state.lowerBounds,
                    state.upperBounds));
        }
    }
    @Override
    public void updateParameters(List<Param> parameterMap, List<IterationResult> landscape) {
        // intit
        int swarm_size = ((Number)optimizerParams.get(1).getValue()).intValue();
        if (state.firstStep) {
            initSearchSpace(parameterMap);
            initFireFlies(swarm_size);  //swarm_size
            state.firstStep = false;
        } else {
            float alpha = ((Number)optimizerParams.get(1).getValue()).floatValue();
            for (int i = 0; i < swarm_size; i++) {
                state.swarm.get(i).newPosition = state.swarm.get(i).position.clone();
                for (int j = 0; j < swarm_size; j++) {
                    try {
                        if (state.swarm.get(j).actualFitness.betterThan(state.swarm.get(i).actualFitness)) {
                            float distance_i_j = distance(state.swarm.get(i).position, state.swarm.get(j).position);
                            //Vary attractiveness with distance r via exp(/gamma*r)
                            float beta = getAttractiveness(distance_i_j);
                            //move firefly i towards j;
                            for (int dim = 0; dim < state.dimension; dim++) {
                                state.swarm.get(i).newPosition[dim] +=
                                        beta * (state.swarm.get(j).position[dim] - state.swarm.get(i).newPosition[dim])
                                                + alpha * (rand.doubles(0,1).limit(1).findFirst().getAsDouble() - 0.5);
                            }
                        }
                    } catch (CloneNotSupportedException e) {
                        e.printStackTrace();
                    }
                }
            }
            for (FireFly f: state.swarm) {
                f.checkBoundsForNewPosition(state.dimension,
                        state.lowerBounds, state.upperBounds);
                f.setPositionToNew();
            }
        }
    }

    @Override
    public List<List<Param>> getParameterMapBatch(List<Param> pattern)throws CloneNotSupportedException {
        List<List<Param>> result = new LinkedList<>();
        for(FireFly fly : state.swarm){
            List<Param> setup = Param.cloneParamList(pattern);
            // setup each dimension of the position
            for(int i = 0; i < setup.size(); ++i) {
                setup.get(i).setInitValue(fly.position[i]);
            }
            result.add(setup);
        }
        return result;
    }

    private void updateFireflyAndGlobalState(FireFly fly, IterationResult ir) throws CloneNotSupportedException {
        fly.actualFitness = ir;
        if(ir.betterThan(fly.bestFitness) ){
            fly.bestKnownPosition = fly.position.clone();
            fly.bestFitness = ir;

            if(fly.bestFitness.betterThan(state.swarmBestFitness)) {
                state.swarmBestFitness = fly.bestFitness;
                state.swarmBestKnownPosition = fly.bestKnownPosition.clone();
            }
        }
    }

    public void setResults(List<IterationResult> results) throws CloneNotSupportedException {
        int i = 0;
        for(IterationResult res : results) {
            updateFireflyAndGlobalState(state.swarm.get(i++),res);
        }
    }

    private float getAttractiveness(float dist) {
        float beta0 = ((Number) this.optimizerParams.get(1).getValue()).floatValue(); //beta0
        float gamma = ((Number) this.optimizerParams.get(4).getValue()).floatValue(); //gamma
        return (beta0 / (1 + gamma*dist*dist));
    }

    public static float distance(float[] a, float[] b) {
        float dist = 0;
        for(int i = 0; i < a.length; i++) {
            dist += Math.pow((a[i] - b[i]), 2);
        }
        return (float) Math.sqrt(dist);
    }

    class FireFly extends Solution{
        float[] bestKnownPosition;
        IterationResult bestFitness;

        FireFly(int dim, float[] lowerBounds, float[] upperBounds) {
            super(dim, lowerBounds, upperBounds, rand);
            this.bestKnownPosition = this.position.clone();
            this.bestFitness = null;
        }

        public String print() {
            StringBuilder sb = new StringBuilder();
            sb.append("position: " + Arrays.toString(this.position));
            sb.append("\n");
            sb.append("bestKnownPosition: " + Arrays.toString(this.bestKnownPosition));
            sb.append("\n");
            sb.append("bestFitness: " + this.bestFitness);
            return sb.toString();
        }

        public void setPositionToNew() {
            this.position = newPosition.clone();
        }
    }

    class InternalState {
        ArrayList<FireFly> swarm;
        int actualFireFly;
        float[] swarmBestKnownPosition;
        IterationResult swarmBestFitness;
        boolean firstStep = true;

        float[] upperBounds;
        float[] lowerBounds;
        int dimension;
    }
}


