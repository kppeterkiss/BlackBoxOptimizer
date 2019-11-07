package optimizer.algorithms;

import optimizer.common.InternalStateBase;
import optimizer.common.Solution;
import optimizer.param.Param;
import optimizer.trial.IterationResult;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static java.lang.Math.round;

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

    /*
     * Generate an int between 0 and max with
     * the exclusion of "excluded"
     */
    public int getRandomBee(int excluded, int max) {
        int num = rand.nextInt(max - 1);
        if (num >= excluded) {
            ++num;
        }
        return num;
    }

    /*
     * Move bee with "beeId" towards the otherBeeId
     * in a random dimension
     */
    public void moveBee(int beeId, int otherBeeId) {
        // select random dimension
        int d = rand.nextInt(state.dimension - 1);
        // create new solution
        state.swarm.get(beeId).newPosition = state.swarm.get(beeId).position.clone();
        // [-1,1] , min + Math.random() * (max - min);
        state.swarm.get(beeId).newPosition[d] +=
                (rand.nextFloat() * 2 - 1) *
                        (state.swarm.get(beeId).position[d] - state.swarm.get(otherBeeId).position[d]);
    }

    @Override
    public void updateParameters(List<Param> parameterMap, List<IterationResult> landscape) throws CloneNotSupportedException {
        int swarmSize = ((Number)optimizerParams.get(0).getValue()).intValue();
        float employed_bees_percentage = ((Number)optimizerParams.get(1).getValue()).floatValue();
        int numOfEmployers = round(swarmSize * employed_bees_percentage);

        switch (state.phase) {
            case first:
                initSearchSpace(parameterMap);
                initBees(swarmSize, employed_bees_percentage);
                break;
            case employer:
                int i = 0;
                while (i < numOfEmployers) {
                    // select random candidate (i != j)
                    int j = getRandomBee(i, numOfEmployers);
                    moveBee(i,j);
                    ++i;
                }
                break;
            case onlooker:
                ArrayList<Double> probs = createProbabilities(swarmSize);

                // optimize this
                int selected_employer = -1;
                for (int selected_onlooker = 0; selected_onlooker < state.swarm.size(); ++selected_onlooker) {
                    if (state.swarm.get(selected_onlooker).type == BeeType.onlooker) {
                        double r = rand.nextDouble();
                        for (int j = 0; j < probs.size() - 1; ++j) {
                            if (r >= probs.get(j) && r < probs.get(j +1 )) {
                                selected_employer = j;
                            }
                        }
                    }
                    // first move the onlooker where the employer is
                    state.swarm.get(selected_onlooker).position = state.swarm.get(selected_employer).position;
                    state.swarm.get(selected_onlooker).actualFitness = state.swarm.get(selected_employer).actualFitness;

                    // the movement is the same as the employers do
                    // it cannot be the selected_employer
                    int j = getRandomBee(selected_employer, numOfEmployers);
                    moveBee(selected_onlooker, j);
                }
                break;
            case scout:
                int limit = ((Number)optimizerParams.get(2).getValue()).intValue();
                for(int j = 0; j < state.swarm.size(); ++j) {
                    if (state.swarm.get(j).trial >= limit) {
                        // new location/bee is generated
                        state.swarm.set(j, new Bee(
                                state.dimension,
                                state.lowerBounds,
                                state.upperBounds,
                                rand,
                                BeeType.employer));
                    }
                }
                break;
        }
    }

    @Override
    public List<List<Param>> getParameterMapBatch(List<Param> pattern)throws CloneNotSupportedException {
        List<List<Param>> result = new LinkedList<>();
        switch (state.phase) {
            case first:
            case scout:
                for (int j = 0; j < state.swarm.size(); ++j) {
                    if (state.swarm.get(j).type == BeeType.employer && state.swarm.get(j).trial == 0) {
                        List<Param> setup = Param.cloneParamList(pattern);
                        // setup each dimension of the position
                        for (int i = 0; i < setup.size(); ++i) {
                            setup.get(i).setInitValue(state.swarm.get(j).position[i]);
                            setup.get(i).setId(j);
                        }
                        result.add(setup);
                    }
                }
                break;
            case employer:
                createParamBatch(pattern, result, BeeType.employer);
                break;
            case onlooker:
                createParamBatch(pattern, result, BeeType.onlooker);
                break;
        }
        return result;
    }

    private void createParamBatch(List<Param> pattern, List<List<Param>> result, BeeType type) throws CloneNotSupportedException {
        for (int j = 0; j < state.swarm.size(); ++j) {
            if (state.swarm.get(j).type == type) {
                List<Param> setup = Param.cloneParamList(pattern);
                // setup each dimension of the position
                for (int i = 0; i < setup.size(); ++i) {
                    setup.get(i).setInitValue(state.swarm.get(j).newPosition[i]);
                    setup.get(i).setId(j);
                }
                result.add(setup);
            }
        }
    }

    public void setBest(Bee bee) throws CloneNotSupportedException {
        if(bee.actualFitness.betterThan(state.swarmBestFitness)) {
            state.swarmBestFitness = bee.actualFitness;
            state.swarmBestKnownPosition = bee.position.clone();
        }
    }

    public void setResults(List<IterationResult> results) throws CloneNotSupportedException {
        switch (state.phase) {
            case first:
                for (IterationResult res : results) {
                    Bee bee = state.swarm.get(res.getConfiguration().get(0).getId());
                    bee.actualFitness = res;
                    setBest(bee);
                }
                break;
            case employer:
            case onlooker:
            case scout:
                for (IterationResult res : results) {
                    // get the id of the solution
                    Bee bee = state.swarm.get(res.getConfiguration().get(0).getId());
                    if (res.betterThan(bee.actualFitness)) {
                        bee.actualFitness = res;
                        bee.position = bee.newPosition.clone();
                        setBest(bee);
                    } else {
                        bee.trial += 1;
                    }
                }
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
    }

    /*
     * Todo: is this ok in case of minimize or maximize?????
     */
    public ArrayList<Double> createProbabilities(int numOfEmployers)
    {
        ArrayList<Double> probs = new ArrayList<>();

        double totalFittness = 0;

        for (int i = 0; i < numOfEmployers; ++i)
        {
            totalFittness += state.swarm.get(i).actualFitness.getFitness();
        }

        double sumOfProbs = 0;
        for (int i = 0; i < numOfEmployers; ++i)
        {
            double prob = state.swarm.get(i).actualFitness.getFitness() / totalFittness;
            probs.add(prob + sumOfProbs);
            sumOfProbs += prob;
        }

        return probs;
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
        int trial;

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
        int trial;

        public InternalState() {
            super();
            phase = AlgorithmPhase.first;
            trial = 0;
        }
    }
}
