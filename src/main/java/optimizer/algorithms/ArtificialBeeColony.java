package optimizer.algorithms;

import optimizer.common.InternalStateBase;
import optimizer.common.Solution;
import optimizer.param.Param;
import optimizer.trial.IterationResult;

import java.util.*;

import static java.lang.Math.round;

public class ArtificialBeeColony extends AbstractAlgorithm{
    InternalState state = new InternalState();
    Random rand = new Random();

    {
        this.parallelizable = ParallelExecution.GENERATION;
        this.optimizerParams = new LinkedList<>();
        this.optimizerParams.add(new Param(10,Integer.MAX_VALUE,1,"number_of_employer"));
        // employed_bees_percentage
        this.optimizerParams.add(new Param(10, Integer.MAX_VALUE,1, "number_of_onlooker"));
        // If a position cannot be improved over a predefined number (called limit) of cycles, then the food source is abandoned.
        this.optimizerParams.add(new Param(5,Integer.MAX_VALUE,1,"limit"));
    }

    private void initSearchSpace(List<Param> parameterMap) {
        state.initSearchSpace(parameterMap);
    }

    private void initBees(int numOfEmployer, int numOfOnlooker) {
        //array to store all parameters
        state.swarm = new ArrayList<>();
        //initialize the nests, and add to nest array
        for (int i = 0; i < numOfEmployer + numOfOnlooker; ++i) {
            if (i < numOfEmployer) {
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
        int d = rand.nextInt(state.dimension);
        // create new solution
        state.swarm.get(beeId).newPosition = state.swarm.get(beeId).position.clone();
        // [-1,1] , min + Math.random() * (max - min);
        state.swarm.get(beeId).newPosition[d] +=
                (rand.nextFloat() * 2 - 1) *
                        (state.swarm.get(beeId).position[d] - state.swarm.get(otherBeeId).position[d]);

        state.swarm.get(beeId).checkBoundsForNewPosition(state.dimension, state.lowerBounds, state.upperBounds);
    }

    @Override
    public void updateParameters(List<Param> parameterMap, List<IterationResult> landscape) throws CloneNotSupportedException {
        int number_of_employer = ((Number)optimizerParams.get(0).getValue()).intValue();
        int number_of_onlooker = ((Number)optimizerParams.get(1).getValue()).intValue();
        int swarmSize = number_of_employer + number_of_onlooker;

        switch (state.phase) {
            case first:
                initSearchSpace(parameterMap);
                initBees(number_of_employer, number_of_onlooker);
                break;
            case employer:
                int i = 0;
                while (i < number_of_employer) {
                    // select random candidate (i != j)
                    int j = getRandomBee(i, number_of_employer);
                    moveBee(i,j);
                    ++i;
                }
                break;
            case onlooker:
                ArrayList<Double> probs = createProbabilities(number_of_employer);

                for (int onlooker = number_of_employer; onlooker < swarmSize; ++onlooker) {
                    double r = rand.nextDouble();
                    for (int employerId = 0; employerId < probs.size() - 1; ++employerId) {
                        if (r >= probs.get(employerId) && r < probs.get(employerId +1 )) {
                            // first move the onlooker where the employer is
                            state.swarm.get(onlooker).position = state.swarm.get(employerId).position.clone();
                            state.swarm.get(onlooker).actualFitness = state.swarm.get(employerId).actualFitness;
                            state.swarm.get(onlooker).helpingTo(employerId);

                            // the movement is the same as the employers do
                            // it cannot be the selected_employer
                            int id = getRandomBee(employerId, number_of_employer);
                            moveBee(onlooker, id);
                            break;
                        }
                    }
                }
                break;
            case scout:
                int limit = ((Number)optimizerParams.get(2).getValue()).intValue();
                for(int j = 0; j < number_of_employer; ++j) {
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
        int number_of_employer = ((Number)optimizerParams.get(0).getValue()).intValue();
        int number_of_onlooker = ((Number)optimizerParams.get(1).getValue()).intValue();
        List<List<Param>> result = new LinkedList<>();

        switch (state.phase) {
            case first:
            case scout:
                for (int j = 0; j < number_of_employer; ++j) {
                    if (state.swarm.get(j).trial == 0) {
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
                createParamBatch(pattern, result, 0, number_of_employer);
                break;
            case onlooker:
                createParamBatch(pattern, result, number_of_employer, number_of_employer + number_of_onlooker);
                break;
        }
        return result;
    }

    private void createParamBatch(List<Param> pattern, List<List<Param>> result, int start, int end) throws CloneNotSupportedException {
        for (int j = start; j < end; ++j) {
            List<Param> setup = Param.cloneParamList(pattern);
            // setup each dimension of the position
            for (int i = 0; i < setup.size(); ++i) {
                setup.get(i).setInitValue(state.swarm.get(j).newPosition[i]);
                setup.get(i).setId(j);
            }
            result.add(setup);
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
                        if (state.phase == AlgorithmPhase.onlooker) {
                            state.swarm.get(bee.helpingToEmployerId).trial += 1;
                        } else {
                            bee.trial += 1;
                        }
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
                int number_of_employer = ((Number)optimizerParams.get(0).getValue()).intValue();
                for (int onlooker = number_of_employer; onlooker < state.swarm.size(); ++onlooker) {
                    state.swarm.get(onlooker).helpingToEmployerId = -1;
                }
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
        probs.add(0.0);

        int max_id = 0;
        double max = Math.abs(state.swarm.get(0).actualFitness.getFitness());
        for (int i = 1; i < numOfEmployers; ++i)
        {
            if ((Math.abs(state.swarm.get(i).actualFitness.getFitness()) > max)) {
                max = Math.abs(state.swarm.get(i).actualFitness.getFitness());
                max_id = i;
            }

        }

        for (int i = 0; i < numOfEmployers; ++i)
        {
            probs.add(
                    state.swarm.get(i).actualFitness.getFitness() / state.swarm.get(max_id).actualFitness.getFitness());
        }

        //order the probabilities
        Collections.sort(probs);
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
        int helpingToEmployerId;

        Bee(int dim, BeeType type) {
            super(dim);
            this.type = type;
            helpingToEmployerId = -1;
        }

        Bee(int dim,  float[] lowerBounds, float[] upperBounds, Random rand, BeeType type) {
            super(dim, lowerBounds, upperBounds, rand);
            this.type = type;
            helpingToEmployerId = -1;
        }

        public void helpingTo(int beeId) {
            helpingToEmployerId = beeId;
        }
    }

    class InternalState extends InternalStateBase<Bee> {
        AlgorithmPhase phase;

        public InternalState() {
            super();
            phase = AlgorithmPhase.first;
        }
    }
}
