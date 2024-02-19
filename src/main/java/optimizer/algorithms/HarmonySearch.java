package optimizer.algorithms;

import optimizer.common.InternalStateBase;
import optimizer.common.Solution;
import optimizer.param.Param;
import optimizer.trial.IterationResult;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;


public class HarmonySearch extends AbstractAlgorithm {
    InternalState state = new InternalState();
    Random rand = new Random();

    {
        this.parallelizable = ParallelExecution.GENERATION;
        this.optimizerParams = new LinkedList<>();
        this.optimizerParams.add(new Param(20,Integer.MAX_VALUE,5,"harmony_size"));
        // pitch adjusting rate
        this.optimizerParams.add(new Param(0.5,1.0,0,"par"));
        // harmony consideration rate
        this.optimizerParams.add(new Param(0.5,1.0,0,"harmony_rate"));
        // bandwidth
        this.optimizerParams.add(new Param(0.5,1.0,0,"bandwidth"));
    }

    private void initSearchSpace(List<Param> parameterMap) {
        state.initSearchSpace(parameterMap);
    }

    private void initHarmonies(int swarm_size) {
        for (int i = 0; i < swarm_size; ++i) {
            state.swarm.add(new Solution(
                    state.dimension,
                    state.lowerBounds,
                    state.upperBounds,
                    rand));
        }
    }

    @Override
    public void updateParameters(List<Param> parameterMap, List<IterationResult> landscape) {
        int harmony_size = ((Number)optimizerParams.get(0).getValue()).intValue();
        switch (state.phase) {
            case init:
                initSearchSpace(parameterMap);
                initHarmonies(harmony_size);
                break;
            case iteration:
                float harmony_rate = ((Number)optimizerParams.get(2).getValue()).floatValue();
                float par = ((Number)optimizerParams.get(1).getValue()).floatValue();
                state.newHarmony = new Solution(state.dimension);

                for (int dim = 0; dim < state.dimension; ++dim) {
                    if (rand.nextFloat() < harmony_rate) {
                        state.newHarmony.newPosition[dim] = getHarmony(rand.nextInt(harmony_size)).position[dim];

                        if (rand.nextFloat() < par) {
                            float bandwidth = ((Number)optimizerParams.get(3).getValue()).floatValue();
                            // [-1 , 1] = (max - min) * nextFloat() + min
                            state.newHarmony.newPosition[dim] += bandwidth * ( 2 * rand.nextFloat() - 1);
                        }
                    } else {
                        // generate a position between the bounds
                        state.newHarmony.newPosition[dim] =
                                (state.upperBounds[dim] - state.lowerBounds[dim]) * rand.nextFloat() + state.lowerBounds[dim];
                    }
                }
                break;
        }
    }

    public List<List<Param>> getParameterMapBatch(List<Param> pattern)throws CloneNotSupportedException {
        List<List<Param>> result = new LinkedList<>();
        switch (state.phase) {
            case init:
                for (int j = 0; j < state.swarm.size(); ++j) {
                    List<Param> setup = Param.cloneParamList(pattern);
                    // setup each dimension of the position
                    for (int i = 0; i < setup.size(); ++i) {
                        setup.get(i).setInitValue(getHarmony(j).newPosition[i]);
                        setup.get(i).setId(j);
                    }
                    result.add(setup);
                }
                break;
            case iteration:
                List<Param> setup = Param.cloneParamList(pattern);
                // setup each dimension of the position
                for (int i = 0; i < setup.size(); ++i) {
                    setup.get(i).setInitValue(state.newHarmony.newPosition[i]);
                }
                result.add(setup);
                break;
        }
        return result;
    }

    public void setResults(List<IterationResult> results) throws CloneNotSupportedException {
        switch (state.phase) {
            case init:
                for (IterationResult res : results) {
                    Solution harmony = getHarmony(res.getConfiguration().get(0).getId());
                    harmony.saveResultAndPosition(res);
                    state.setBest(harmony);
                }
                state.findWorst();
                break;
            case iteration:
                state.newHarmony.actualFitness = results.get(0);
                state.newHarmony.position = state.newHarmony.newPosition.clone();
                // overwrite the worst position with the new one if it is better
                if (state.newHarmony.actualFitness.betterThan(state.swarmWorstFitness)) {
                    state.swarm.set(state.swarmWorstId, state.newHarmony);
                    state.setBest(state.newHarmony);
                    state.findWorst();
                }
                break;
        }
    }

    public void updateGlobals() {
        switch (state.phase) {
            case init:
                state.phase = AlgorithmPhase.iteration;
                break;
            case iteration:
                break;
        }
    }

    Solution getHarmony(int id) {
        return state.swarm.get(id);
    }
    
    public enum AlgorithmPhase {
        init,
        iteration
    }

    public class InternalState extends InternalStateBase<Solution> {
        public AlgorithmPhase phase;

        float[] swarmWorstKnownPosition;
        IterationResult swarmWorstFitness;
        int swarmWorstId;

        Solution newHarmony;

        public InternalState() {
            super();
            phase = AlgorithmPhase.init;
            newHarmony = null;
        }

        void findWorst() throws CloneNotSupportedException {
            swarmWorstFitness = getHarmony(0).actualFitness;
            swarmWorstKnownPosition = getHarmony(0).position.clone();
            swarmWorstId = 0;

            for (int i = 0; i < state.swarm.size(); ++i) {
                if(swarmWorstFitness.betterThan(getHarmony(i).getActualFitness())) {
                    swarmWorstFitness = getHarmony(i).actualFitness;
                    swarmWorstKnownPosition = getHarmony(i).position.clone();
                    swarmWorstId = i;
                }
            }
        }

        void setWorst(Solution solution, int id) throws CloneNotSupportedException {
            if(swarmWorstFitness.betterThan(solution.getActualFitness())) {
                swarmWorstFitness = solution.actualFitness;
                swarmWorstKnownPosition = solution.position.clone();
                swarmWorstId = id;
            }
        }
    }
}
