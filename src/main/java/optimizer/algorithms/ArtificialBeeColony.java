package optimizer.algorithms;

import optimizer.common.Probability;
import optimizer.common.RouletteWheelSelection;
import optimizer.common.bee.Bee;
import optimizer.common.bee.Bee.BeeType;
import optimizer.common.bee.BeeInternalState;
import optimizer.exception.AlgorithmException;
import optimizer.main.Main;
import optimizer.objective.Objective;
import optimizer.objective.Relation;
import optimizer.param.Param;
import optimizer.trial.IterationResult;

import java.util.*;


public class ArtificialBeeColony extends AbstractAlgorithm{
    BeeInternalState<AlgorithmPhase> state = new BeeInternalState();
    RouletteWheelSelection selection = new RouletteWheelSelection();
    Random rand = new Random();

    {
        this.parallelizable = ParallelExecution.GENERATION;
        this.iterationCounterCorrection = 3;
        this.optimizerParams = new LinkedList<>();
        this.optimizerParams.add(new Param(10,Integer.MAX_VALUE,1,"number_of_employer"));
        // employed_bees_percentage
        this.optimizerParams.add(new Param(15, Integer.MAX_VALUE,1, "number_of_onlooker"));
        // If a position cannot be improved over a predefined number (called limit) of cycles, then the food source is abandoned.
        this.optimizerParams.add(new Param(5,Integer.MAX_VALUE,1,"limit"));
        state.phase = AlgorithmPhase.first;
    }

    private void checkObjectivePresence() throws AlgorithmException {
        List<Objective> objectives = this.config.getObjectiveContainerReference().getObjectiveListReference();
        if (objectives.isEmpty()) {
            String msg = "No objective is present. The algorithm can not work propery without it.";
            Main.getLogger().error(msg);
            throw new AlgorithmException(msg);
        }
    }

    private void initSearchSpace(List<Param> parameterMap) {
        checkObjectivePresence();
        state.initSearchSpace(parameterMap);
    }

    private void initBees(int numOfEmployer, int numOfOnlooker) {
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
                    int j = state.getRandomBee(i, number_of_employer, rand);
                    state.moveBee(i,j, rand);
                    ++i;
                }
                break;
            case onlooker:
                List<Probability> probs = selection.createProbabilities(number_of_employer, state.swarm, state.swarmBestFitness);
                // create intervals to choose from
                selection.createIntervals(probs);

                for (int onlooker = number_of_employer; onlooker < swarmSize; ++onlooker) {
                    double r = rand.nextDouble();
                    for (int j = 0; j < probs.size() - 1; ++j) {
                        if (r >= probs.get(j).getProbability() && r < probs.get(j + 1).getProbability()) {
                            // First move the onlooker where the employer is.
                            int employerId = probs.get(j).getId();
                            getBee(onlooker).position = getBee(employerId).position.clone();
                            getBee(onlooker).actualFitness = getBee(employerId).actualFitness;
                            getBee(onlooker).helpingTo(employerId);

                            // the movement is the same as the employers do
                            // it cannot be the selected_employer
                            int id = state.getRandomBee(employerId, number_of_employer, rand);
                            state.moveBee(onlooker, id, rand);
                            break;
                        }
                    }
                }
                break;
            case scout:
                int limit = ((Number)optimizerParams.get(2).getValue()).intValue();
                for(int j = 0; j < number_of_employer; ++j) {
                    if (getBee(j).trial >= limit) {
                        // new location/bee is generated
                        state.swarm.set(j, new Bee(
                                state.dimension,
                                state.lowerBounds,
                                state.upperBounds,
                                rand,
                                BeeType.scout));
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
            case employer:
                createParamBatch(pattern, result, 0, number_of_employer);
                break;
            case onlooker:
                createParamBatch(pattern, result, number_of_employer, number_of_employer + number_of_onlooker);
                break;
            case scout:
                for (int j = 0; j < number_of_employer; ++j) {
                    if (getBee(j).type == BeeType.scout) {
                        List<Param> setup = Param.cloneParamList(pattern);
                        // setup each dimension of the position
                        for (int i = 0; i < setup.size(); ++i) {
                            setup.get(i).setInitValue(getBee(j).newPosition[i]);
                            setup.get(i).setId(j);
                        }
                        result.add(setup);
                    }
                }
                break;
        }
        return result;
    }

    private void createParamBatch(List<Param> pattern, List<List<Param>> result, int start, int end) throws CloneNotSupportedException {
        for (int j = start; j < end; ++j) {
            List<Param> setup = Param.cloneParamList(pattern);
            // setup each dimension of the position
            for (int i = 0; i < setup.size(); ++i) {
                setup.get(i).setInitValue(getBee(j).newPosition[i]);
                setup.get(i).setId(j);
            }
            result.add(setup);
        }
    }

    public void setResults(List<IterationResult> results) throws CloneNotSupportedException {
        switch (state.phase) {
            case first:
                state.setAllResults(results);
                break;
            case employer:
            case onlooker:
            case scout:
                for (IterationResult res : results) {
                    // get the id of the solution
                    Bee bee = getBee(res.getConfiguration().get(0).getId());
                    if (res.betterThan(bee.actualFitness)) {
                        bee.saveResultAndPosition(res);
                        state.setBest(bee);
                        // fitness is improved so trial is 0
                        if (state.phase == AlgorithmPhase.onlooker) {
                            getBee(bee.helpingToEmployerId).trial = 0;
                        } else {
                            bee.trial = 0;
                        }
                    } else {

                        if (state.phase == AlgorithmPhase.onlooker) {
                            getBee(bee.helpingToEmployerId).trial += 1;
                        } else {
                            bee.trial += 1;
                        }
                    }
                    if (bee.type == BeeType.scout) bee.type = BeeType.employer;
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
                    getBee(onlooker).helpingToEmployerId = -1;
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

    Bee getBee(int id) {
        return state.swarm.get(id);
    }

    enum AlgorithmPhase {
        first,
        employer,
        onlooker,
        scout
    }
}
