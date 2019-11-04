package optimizer.algorithms;

import optimizer.common.InternalStateBase;
import optimizer.common.Solution;
import optimizer.param.Param;
import optimizer.trial.IterationResult;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class ArtificialBeeColony extends AbstractAlgorithm{
    InternalState state = new InternalState();
    Random rand = new Random();

    {
        this.parallelizable = ParallelExecution.GENERATION;
        this.optimizerParams = new LinkedList<>();
        this.optimizerParams.add(new Param(10,Integer.MAX_VALUE,1,"swarm_size"));
        // employed_bees_percentage
        this.optimizerParams.add(new Param(0.9, 1,0, "employed_bees_percentage"));
        // If a position cannot be improved over a predefined number (called limit) of cycles, then the food source is abandoned.
        this.optimizerParams.add(new Param(5,Integer.MAX_VALUE,1,"limit"));
    }

    private void initSearchSpace(List<Param> parameterMap) {
        state.initSearchSpace(parameterMap);
    }

    private void initBees(int swarmSize, float employed_bees_percentage) {
        //array to store all parameters
        state.swarm = new ArrayList<>();
        state.calculateResultsForIds = new LinkedList<>();
        //initialize the nests, and add to nest array
        for (int i = 0; i < swarmSize; ++i) {
            if (((float)i / (float) swarmSize) <= employed_bees_percentage) {
                state.swarm.add(new Bee(
                        state.dimension,
                        state.lowerBounds,
                        state.upperBounds,
                        rand,
                        BeeType.employer));
            } else {
                state.swarm.add(new Bee(
                        state.dimension,
                        BeeType.onlooker));
            }
        }
    }

    @Override
    public void updateParameters(List<Param> parameterMap, List<IterationResult> landscape) throws CloneNotSupportedException {
        int swarmSize = ((Number)optimizerParams.get(0).getValue()).intValue();
        float employed_bees_percentage = ((Number)optimizerParams.get(1).getValue()).floatValue();

        switch (state.phase) {
            case first:
                initSearchSpace(parameterMap);
                initBees(swarmSize, employed_bees_percentage);
                break;
            case employer:
                break;
            case onlooker:
                break;
            case scout:
                break;
        }
    }

    @Override
    public List<List<Param>> getParameterMapBatch(List<Param> pattern)throws CloneNotSupportedException {
        List<List<Param>> result = new LinkedList<>();
        switch (state.phase) {
            case first:
                // not the whole swarm is calculated
                for (int j = 0; j < state.swarm.size(); ++j) {
                    if (state.swarm.get(j).type == BeeType.employer) {
                        List<Param> setup = Param.cloneParamList(pattern);
                        // setup each dimension of the position
                        for (int i = 0; i < setup.size(); ++i) {
                            setup.get(i).setInitValue(state.swarm.get(j).position[i]);
                            setup.get(i).setId(j);
                        }
                        state.calculateResultsForIds.add(j);
                        result.add(setup);
                    }
                }
                break;
            case employer:
            /*
            for (int j = 0; j < state.swarm.size(); ++j) {
                List<Param> setup = Param.cloneParamList(pattern);
                // setup each dimension of the position
                for (int i = 0; i < setup.size(); ++i) {
                    setup.get(i).setInitValue(state.firstStep ? state.swarm.get(j).position[i] : state.swarm.get(j).newPosition[i]);
                    setup.get(i).setId(j);
                }
                result.add(setup);
            }
            */
            case onlooker:
                break;
            case scout:
                break;
        }
        return result;
    }

    public void setResults(List<IterationResult> results) throws CloneNotSupportedException {
        switch (state.phase) {
            case first:
                int i = 0;
                for (IterationResult res : results) {
                    int id = state.calculateResultsForIds.get(i);
                    Bee bee = state.swarm.get(id);
                    bee.actualFitness = res;

                    if(bee.actualFitness.betterThan(state.swarmBestFitness)) {
                        state.swarmBestFitness = bee.actualFitness;
                        state.swarmBestKnownPosition = bee.position.clone();
                    }
                    ++i;
                }
                break;
            case employer:
                break;
            case onlooker:
                break;
            case scout:
                break;
        }
    }

    public void updateGlobals() throws CloneNotSupportedException {
        switch (state.phase){
            case first:
                state.phase = AlgorithmPhase.employer;
                break;
            case employer:
                state.phase = AlgorithmPhase.onlooker;
                break;
            case onlooker:
                state.phase = AlgorithmPhase.scout;
                break;
            case scout:
                state.phase = AlgorithmPhase.employer;
                break;
            default:
                break;
                //error
        }
        state.calculateResultsForIds.clear();
    }

    enum BeeType {
        employer,
        onlooker,
        scout
    }

    enum AlgorithmPhase {
        first,
        employer,
        onlooker,
        scout
    }

    class Bee extends Solution {
        BeeType type;

        Bee(int dim, BeeType type) {
            super(dim);
            this.type = type;
        }

        Bee(int dim,  float[] lowerBounds, float[] upperBounds, Random rand, BeeType type) {
            super(dim, lowerBounds, upperBounds, rand);
            this.type = type;
        }
    }

    class InternalState extends InternalStateBase<Bee> {
        AlgorithmPhase phase;
        List<Integer> calculateResultsForIds;
        public InternalState() {
            super();
            phase = AlgorithmPhase.first;
        }
    }
}
