package optimizer.algorithms;

import optimizer.common.InternalStateBase;
import optimizer.common.Solution;
import optimizer.param.Param;
import optimizer.trial.IterationResult;

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
        // characteristic scale of the problem of interest, whereas in some cases α = O(L/100)
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
        //initialize the nests, and add to nest array
        for (int i = 0; i < swarmSize; ++i) {
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
                        getFlower(state.currentFlower).levy_flights(
                                beta, alpha, rand, state.swarmBestKnownPosition);

                        getFlower(state.currentFlower).checkBoundsForNewPosition(
                                state.dimension, state.lowerBounds, state.upperBounds);
                    } else {
                        // if not, then local pollination of neighbor flowers
                        double epsilon = rand.nextDouble();
                        Collections.shuffle(state.JK);

                        getFlower(state.currentFlower).newPosition =
                                getFlower(state.currentFlower).position.clone();
                        for (int i = 0; i < state.dimension; ++i) {
                            getFlower(state.currentFlower).newPosition[i] +=
                                    epsilon * (getFlower(state.JK.get(0)).position[i]
                                            - getFlower(state.JK.get(1)).position[i]);
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
                        setup.get(i).setInitValue(getFlower(j).newPosition[i]);
                        setup.get(i).setId(j);
                    }
                    result.add(setup);
                }
                break;
            case Main:
                List<Param> setup = Param.cloneParamList(pattern);
                // setup each dimension of the position
                for(int i = 0; i < setup.size(); ++i) {
                    setup.get(i).setInitValue(getFlower(state.currentFlower).newPosition[i]);
                    setup.get(i).setId(state.currentFlower);
                }
                result.add(setup);
        }
        return result;
    }

    public void setResults(List<IterationResult> results) throws CloneNotSupportedException {
        for (IterationResult res : results) {
            Solution flower = getFlower(res.getConfiguration().get(0).getId());
            flower.saveResultAndPosition(res);
            if (flower.actualFitness.betterThan(state.swarmBestFitness)) {
                state.swarmBestFitness = flower.actualFitness;
                state.swarmBestKnownPosition = flower.position.clone();
            }
        }
    }

    public void updateGlobals() {
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

    private Solution getFlower(int id) {
        return state.swarm.get(id);
    }

    enum AlgorithmPhase {
        Init,
        Main
    }

    class InternalState extends InternalStateBase<Solution> {
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
