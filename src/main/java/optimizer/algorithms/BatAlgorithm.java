package optimizer.algorithms;

import optimizer.common.InternalStateBase;
import optimizer.common.Solution;
import optimizer.param.Param;
import optimizer.trial.IterationResult;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class BatAlgorithm extends AbstractAlgorithm{
    InternalState state = new InternalState();
    Random rand = new Random();

    {
        this.parallelizable = ParallelExecution.GENERATION;
        this.optimizerParams = new LinkedList<>();
        this.optimizerParams.add(new Param(10,Integer.MAX_VALUE,1,"swarm_size"));
        // loudness (constant or decreasing)
        this.optimizerParams.add(new Param(0.25, 1,0, "A"));
        // Pulse rate (constant or decreasing)
        this.optimizerParams.add(new Param(0.5, 1,0, "r"));
        // Frequency minimum
        this.optimizerParams.add(new Param(0, 100,0, "Qmin"));
        // Frequency maximum
        this.optimizerParams.add(new Param(2, 100,0, "Qmax"));
        // scaling factor
        this.optimizerParams.add(new Param(0.01, 10,0, "sigma"));
    }

    private void initSearchSpace(List<Param> parameterMap) {
        state.initSearchSpace(parameterMap);
    }

    private void initBats(int swarmSize) {
        //array to store all parameters
        state.swarm = new ArrayList<>();
        //initialize the nests, and add to nest array
        for (int i = 0; i < swarmSize; ++i) {
            state.swarm.add(new Bat(
                    state.dimension,
                    state.lowerBounds,
                    state.upperBounds,
                    rand));
        }
    }

    @Override
    public void updateParameters(List<Param> parameterMap, List<IterationResult> landscape) throws CloneNotSupportedException {
        int swarmSize = ((Number)optimizerParams.get(0).getValue()).intValue();
        if (state.firstStep) {
            initSearchSpace(parameterMap);
            initBats(swarmSize);
        } else {
            double Qmin = ((Number)optimizerParams.get(3).getValue()).floatValue();
            double Qmax = ((Number)optimizerParams.get(4).getValue()).floatValue();

            // start calculating the new position
            double Q = Qmin + (Qmin - Qmax) * rand.nextDouble();
            for (int i = 0; i < state.dimension; ++i) {
                // update velocity
                state.swarm.get(state.activeBat).velocity[i] +=
                        (state.swarm.get(state.activeBat).position[i] - state.swarmBestKnownPosition[i]) * Q;
                // update newPosition
                state.swarm.get(state.activeBat).newPosition[i] =
                        state.swarm.get(state.activeBat).position[i] + state.swarm.get(state.activeBat).velocity[i];
            }

            float r = ((Number)optimizerParams.get(2).getValue()).floatValue();
            float sigma = ((Number)optimizerParams.get(5).getValue()).floatValue();
            if (rand.nextFloat() > r) {
                for (int i = 0; i < state.dimension; ++i) {
                    state.swarm.get(state.activeBat).newPosition[i] =
                            (float) (state.swarmBestKnownPosition[i] + sigma * rand.nextGaussian());
                }
            }
            state.swarm.get(state.activeBat).checkBoundsForNewPosition(state.dimension,
                    state.lowerBounds, state.upperBounds);
        }
    }

    @Override
    public List<List<Param>> getParameterMapBatch(List<Param> pattern)throws CloneNotSupportedException {
        List<List<Param>> result = new LinkedList<>();
        if (state.firstStep) {
            for (Bat bat : state.swarm) {
                List<Param> setup = Param.cloneParamList(pattern);
                // setup each dimension of the position
                for (int i = 0; i < setup.size(); ++i) {
                    setup.get(i).setInitValue(bat.position[i]);
                }
                result.add(setup);
            }
        } else {
            List<Param> setup = Param.cloneParamList(pattern);
            // setup each dimension of the position
            for (int i = 0; i < setup.size(); ++i) {
                setup.get(i).setInitValue(state.swarm.get(state.activeBat).newPosition[i]);
            }
            result.add(setup);
        }

        return result;
    }

    public void setResults(List<IterationResult> results) throws CloneNotSupportedException {
        if (state.firstStep) {
            int i = 0;
            for (IterationResult res : results) {
                state.swarm.get(i).actualFitness = res;
                if(state.swarm.get(i).actualFitness.betterThan(state.swarmBestFitness)) {
                    state.swarmBestFitness = state.swarm.get(i).actualFitness;
                    state.swarmBestKnownPosition = state.swarm.get(i).position.clone();
                }
                ++i;
            }
        } else {
            // if the solution improves or not too loudness
            float A = ((Number)optimizerParams.get(1).getValue()).floatValue();

            if(results.get(0).betterThan(state.swarm.get(state.activeBat).actualFitness) && (rand.nextFloat() <  A)) {
                state.swarm.get(state.activeBat).position = state.swarm.get(state.activeBat).newPosition.clone();
                state.swarm.get(state.activeBat).actualFitness = results.get(0);
            }

            // set new best positin
            if(results.get(0).betterThan(state.swarmBestFitness)) {
                state.swarmBestFitness = results.get(0);
                state.swarmBestKnownPosition = state.swarm.get(state.activeBat).newPosition.clone();
            }
        }

    }

    public void updateGlobals() throws CloneNotSupportedException {
        // to the next bat
        if (!state.firstStep) {
            int swarmSize = ((Number)optimizerParams.get(0).getValue()).intValue();

            if (state.activeBat < swarmSize - 1)
                state.activeBat++;
            else
                state.activeBat = 0;
        }

        state.firstStep = false;
    }


    class Bat extends Solution {
        float[] velocity;

        Bat(int dim,  float[] lowerBounds, float[] upperBounds, Random rand) {
            super(dim, lowerBounds, upperBounds, rand);
            velocity = new float[dim];
        }
    }

    class InternalState extends InternalStateBase<Bat> {
        int activeBat;

        public InternalState() {
            super();
            activeBat = 0;
        }
    }
}
