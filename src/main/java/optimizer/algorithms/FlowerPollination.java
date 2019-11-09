package optimizer.algorithms;

import optimizer.common.InternalStateBase;
import optimizer.common.Solution;
import optimizer.param.Param;
import optimizer.trial.IterationResult;

import javax.swing.text.html.HTMLDocument;
import java.util.*;

public class FlowerPollination extends AbstractAlgorithm {
    InternalState state = new InternalState();
    Random rand = new Random();

    {
        this.parallelizable = ParallelExecution.GENERATION;
        this.optimizerParams = new LinkedList<>();
        this.optimizerParams.add(new Param(20, Integer.MAX_VALUE, 1, "swarm_size"));
        // probability_switch
        this.optimizerParams.add(new Param(0.8, 1, 0, "probability_switch"));
        // beta
        this.optimizerParams.add(new Param(1.5, 2,1, "beta"));
        // In most cases, we can use α = O(L/10), where L is the
        //characteristic scale of the problem of interest, whereas in some cases α = O(L/100)
        this.optimizerParams.add(new Param(0.1, 100,0, "alpha"));
    }

    private void initSearchSpace(List<Param> parameterMap) {
        state.initSearchSpace(parameterMap);

        int swarm_size = ((Number)optimizerParams.get(0).getValue()).intValue();
        for (int i = 0; i < swarm_size; ++i) {
            state.JK.add(i);
        }
    }

    private void initFlowers(int swarmSize) {
        //array to store all parameters
        state.swarm = new ArrayList<>();
        //initialize the nests, and add to nest array
        for (int i = 0; i < swarmSize; ++i) {
            state.swarm.add(new Flower(
                    state.dimension,
                    state.lowerBounds,
                    state.upperBounds,
                    rand));
        }
    }

    @Override
    public void updateParameters(List<Param> parameterMap, List<IterationResult> landscape) throws CloneNotSupportedException {
        int swarm_size = ((Number)optimizerParams.get(0).getValue()).intValue();
        double probability_switch = ((Number)optimizerParams.get(1).getValue()).floatValue();
        switch (state.phase) {
            case Init:
                initSearchSpace(parameterMap);
                initFlowers(swarm_size);
                break;
            case Main:
                double beta = ((Number)optimizerParams.get(2).getValue()).floatValue();
                double alpha = ((Number)optimizerParams.get(3).getValue()).floatValue();
                    if (rand.nextFloat() > probability_switch) {
                        // Pollens are carried by insects and thus can move in
                        // large scale, large distance.
                        state.swarm.get(state.currentFlower).levy_flights(
                                beta, alpha, rand, state.swarmBestKnownPosition);

                        state.swarm.get(state.currentFlower).checkBoundsForNewPosition(
                                state.dimension, state.lowerBounds, state.upperBounds);
                    } else {
                        // if not, then local pollination of neighbor flowers
                        double epsilon = rand.nextDouble();
                        Collections.shuffle(state.JK);

                        state.swarm.get(state.currentFlower).newPosition =
                                state.swarm.get(state.currentFlower).position.clone();
                        for (int i = 0; i < state.dimension; ++i) {
                            state.swarm.get(state.currentFlower).newPosition[i] +=
                                    epsilon * (state.swarm.get(state.JK.get(0)).position[i]
                                            - state.swarm.get(state.JK.get(1)).position[i]);
                        }
                    }

                break;
        }
    }

    public List<List<Param>> getParameterMapBatch(List<Param> pattern)throws CloneNotSupportedException {
        List<List<Param>> result = new LinkedList<>();
        switch (state.phase) {
            case Init:
                for(int j = 0; j < state.swarm.size(); ++j) {
                    List<Param> setup = Param.cloneParamList(pattern);
                    // setup each dimension of the position
                    for(int i = 0; i < setup.size(); ++i) {
                        setup.get(i).setInitValue(state.swarm.get(j).newPosition[i]);
                        setup.get(i).setId(j);
                    }
                    result.add(setup);
                }
                break;
            case Main:
                List<Param> setup = Param.cloneParamList(pattern);
                // setup each dimension of the position
                for(int i = 0; i < setup.size(); ++i) {
                    setup.get(i).setInitValue(state.swarm.get(state.currentFlower).newPosition[i]);
                    setup.get(i).setId(state.currentFlower);
                }
                result.add(setup);
        }
        return result;
    }

    public void setSwarmBest(int id) throws CloneNotSupportedException {
        if (state.swarm.get(id).actualFitness.betterThan(state.swarmBestFitness)) {
            state.swarmBestFitness = state.swarm.get(id).actualFitness;
            state.swarmBestKnownPosition = state.swarm.get(id).position.clone();
        }
    }

    public void setResults(List<IterationResult> results) throws CloneNotSupportedException {
        switch (state.phase) {
            case Init:
                for (IterationResult res : results) {
                    int id = res.getConfiguration().get(0).getId();
                    state.swarm.get(id).actualFitness = res;
                    state.swarm.get(id).position = state.swarm.get(id).newPosition.clone();
                    setSwarmBest(id);
                }
                break;
            case Main:
                IterationResult res = results.get(0);
                int id = res.getConfiguration().get(0).getId();
                if (res.betterThan(state.swarm.get(id).actualFitness)) {
                    state.swarm.get(id).actualFitness = res;
                    state.swarm.get(id).position = state.swarm.get(id).newPosition.clone();
                    setSwarmBest(id);
                }
                break;
        }
    }

    public void updateGlobals() throws CloneNotSupportedException {
        switch (state.phase) {
            case Init:
                state.phase = AlgorithmPhase.Main;
                break;
            case Main:
                if(state.currentFlower < state.swarm.size() - 1) {
                    state.currentFlower +=1;
                } else {
                    state.currentFlower = 0;
                }
                break;
        }
    }

    class Flower extends Solution {
        Flower(int dim,  float[] lowerBounds, float[] upperBounds, Random rand) {
            super(dim, lowerBounds, upperBounds, rand);
        }
    }


    enum AlgorithmPhase {
        Init,
        Main
    }

    class InternalState extends InternalStateBase<Flower> {
        AlgorithmPhase phase;
        int currentFlower;
        ArrayList<Integer> JK;


        public InternalState() {
            super();
            phase = AlgorithmPhase.Init;
            currentFlower = 0;
            JK = new ArrayList<>();
        }
    }
}
