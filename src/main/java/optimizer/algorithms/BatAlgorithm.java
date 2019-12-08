package optimizer.algorithms;

import optimizer.common.InternalStateBase;
import optimizer.common.Solution;
import optimizer.param.Param;
import optimizer.trial.IterationResult;

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
        this.optimizerParams.add(new Param(0.9, 1,0, "A"));
        // Pulse rate (constant or increasing)
        this.optimizerParams.add(new Param(0.25, 1,0, "r"));
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
        if (state.init) {
            initSearchSpace(parameterMap);
            initBats(swarmSize);
        } else {
            double Qmin = ((Number)optimizerParams.get(3).getValue()).floatValue();
            double Qmax = ((Number)optimizerParams.get(4).getValue()).floatValue();

            for (int j = 0; j < swarmSize; ++j) {
                // start calculating the new position
                double Q = Qmin + (Qmin - Qmax) * rand.nextDouble();
                for (int i = 0; i < state.dimension; ++i) {
                    // update velocity
                    getBat(j).velocity[i] +=
                            (getBat(j).position[i] - state.swarmBestKnownPosition[i]) * Q;
                    // update newPosition
                    getBat(j).newPosition[i] =
                            getBat(j).position[i] + getBat(j).velocity[i];
                }

                float r = ((Number) optimizerParams.get(2).getValue()).floatValue();
                float sigma = ((Number) optimizerParams.get(5).getValue()).floatValue();

                if (rand.nextFloat() > r) {
                    for (int i = 0; i < state.dimension; ++i) {
                        getBat(j).newPosition[i] =
                                (float) (state.swarmBestKnownPosition[i] + sigma * rand.nextGaussian());
                    }
                }
                getBat(j).checkBoundsForNewPosition(state.dimension,
                        state.lowerBounds, state.upperBounds);
            }
        }
    }

    @Override
    public List<List<Param>> getParameterMapBatch(List<Param> pattern)throws CloneNotSupportedException {
        List<List<Param>> result = new LinkedList<>();
        for (int j = 0; j < state.swarm.size(); ++j) {
            List<Param> setup = Param.cloneParamList(pattern);
            // setup each dimension of the position
            for (int i = 0; i < setup.size(); ++i) {
                setup.get(i).setInitValue(getBat(j).newPosition[i]);
                setup.get(i).setId(j);
            }
            result.add(setup);
        }
        return result;
    }

    public void setResults(List<IterationResult> results) throws CloneNotSupportedException {
        float A = ((Number)optimizerParams.get(1).getValue()).floatValue();

        for (IterationResult res : results) {
            Bat bat = getBat(res.getConfiguration().get(0).getId());
            if (state.init) {
                bat.saveResultAndPosition(res);
                state.setBest(bat);
            } else {
                // "if the solution improves or not too loudness"
                if (res.betterThan(bat.actualFitness) && (rand.nextFloat() < A)) {
                    bat.saveResultAndPosition(res);
                }
                // set new best position
                if (res.betterThan(state.swarmBestFitness)) {
                    state.swarmBestFitness = res;
                    state.swarmBestKnownPosition = bat.newPosition.clone();
                }
            }
        }
    }

    public void updateGlobals() throws CloneNotSupportedException {
        state.init = false;
    }

    Bat getBat(int id) {
        return state.swarm.get(id);
    }

    class Bat extends Solution {
        float[] velocity;

        Bat(int dim,  float[] lowerBounds, float[] upperBounds, Random rand) {
            super(dim, lowerBounds, upperBounds, rand);
            velocity = new float[dim];
        }


        public void checkBoundsForNewPosition(int dimension, float[] lowerBounds, float[] upperBounds) {
            // The velociy reset is not in the pseudo code,
            // but it seems a necessary addition
            for (int i = 0; i < dimension; ++i) {
                if (newPosition[i] > upperBounds[i]) {
                    velocity[i] = 0f;
                } else if (newPosition[i] < lowerBounds[i]) {
                    velocity[i] = 0f;
                }
            }
            super.checkBoundsForNewPosition(dimension,lowerBounds,upperBounds);
        }
    }

    class InternalState extends InternalStateBase<Bat> {
        public InternalState() {
            super();
        }
    }
}
