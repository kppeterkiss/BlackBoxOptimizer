package optimizer.algorithms;

import optimizer.common.InternalStateBase;
import optimizer.common.Solution;
import optimizer.param.Param;
import optimizer.trial.IterationResult;


import java.util.*;

public class GreyWolfOptimizer extends AbstractAlgorithm {
    InternalState state = new InternalState();
    Random rand = new Random();

    {
        this.parallelizable = ParallelExecution.GENERATION;
        this.iterationCounter = 0;
        this.optimizerParams = new LinkedList<>();
        this.optimizerParams.add(new Param(10,Integer.MAX_VALUE,5,"swarm_size"));
    }

    private void initSearchSpace(List<Param> parameterMap) {
        state.initSearchSpace(parameterMap);
    }

    private void initWolves(int swarm_size) {
        for (int i = 0; i < swarm_size; ++i) {
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
        ++iterationCounter;
        switch (state.phase) {
            case init:
                initSearchSpace(parameterMap);
                initWolves(swarm_size);
                break;
            case iteration:
                Collections.sort(state.swarm);
                double a = 2 - 2 * iterationCounter / this.config.getIterationCount().get();

                for (int i = 0; i < state.swarm.size(); ++i) {
                    double[] A1 = createA(a);
                    double[] C1 = createC(a);
                    double[] A2 = createA(a);
                    double[] C2 = createC(a);
                    double[] A3 = createA(a);
                    double[] C3 = createC(a);

                    double[] Dalpha = createD(C1, getWolf(0), getWolf(i));
                    double[] Dbeta = createD(C2, getWolf(1), getWolf(i));
                    double[] Ddelta = createD(C3, getWolf(2), getWolf(i));

                    double[] X1 = createX(getWolf(0), A1, Dalpha);
                    double[] X2 = createX(getWolf(1), A2, Dbeta);
                    double[] X3 = createX(getWolf(2), A3, Ddelta);
                    getWolf(i).newPosition = calculateNewPosition(X1, X2, X3);
                    getWolf(i).checkBoundsForNewPosition(state.dimension, state.lowerBounds, state.upperBounds);
                }
                break;
        }
    }


    public double[] createRandomArray(int dimension, Random rand) {
        double[] ret = new double[dimension];
        for (int i = 0; i < dimension; ++i) {
            ret[i] = rand.nextDouble();
        }
        return ret;
    }

    public double[] createA(double a) {
        double[] r1 = createRandomArray(state.dimension, rand);
        for (int i = 0; i < r1.length; ++i) {
            r1[i] = 2 * r1[i] * a - a;
        }
        return r1;
    }

    public double[] createC( double a) {
        double[] r2 =createRandomArray(state.dimension, rand);
        for (int i = 0; i < r2.length; ++i) {
            r2[i] = 2 * r2[i];
        }
        return r2;
    }

    public double[] createD(double[] c, Solution leader, Solution wolf) {
        double[] d = new double[c.length];
        for (int i = 0; i < d.length; ++i) {
            d[i] = Math.abs(c[i] * leader.position[i] - wolf.position[i]);
        }
        return d;
    }

    public double[] createX(Solution leader, double[] a, double[] d) {
        double[] x = new double[a.length];
        for (int i = 0; i < d.length; ++i) {
            x[i] = leader.position[i] - a[i] * d[i];
        }
        return d;
    }

    public float[] calculateNewPosition(double[] x1, double[] x2, double[] x3) {
        float[] newPos = new float[x1.length];
        for (int i = 0; i < newPos.length; ++i) {
            newPos[i] = (float)(x1[i] + x2[i] + x3[i]) / 3;
        }
        return newPos;
    }


    @Override
    public List<List<Param>> getParameterMapBatch(List<Param> pattern)throws CloneNotSupportedException {
        List<List<Param>> result = new LinkedList<>();
        for (int j = 0; j < state.swarm.size(); ++j) {
            List<Param> setup = Param.cloneParamList(pattern);
            // setup each dimension of the position
            for (int i = 0; i < setup.size(); ++i) {
                setup.get(i).setInitValue(getWolf(j).newPosition[i]);
                setup.get(i).setId(j);
            }
            result.add(setup);
        }
        return result;
    }

    public void setResults(List<IterationResult> results) throws CloneNotSupportedException {
        for (IterationResult res : results) {
            Solution wolf = getWolf(res.getConfiguration().get(0).getId());
            wolf.saveResultAndPosition(res);
            state.setBest(wolf);
        }
    }

    public void updateGlobals() throws CloneNotSupportedException {
        iterationCounter++;

        switch (state.phase) {
            case init:
                state.phase = AlgorithmPhase.iteration;
                break;
            case iteration:
                break;
        }
    }

    Solution getWolf(int id) {
        return state.swarm.get(id);
    }

    public enum AlgorithmPhase {
        init,
        iteration
    }

    public InternalState getState() {
        return state;
    }

    public class InternalState extends InternalStateBase<Solution> {
        public AlgorithmPhase phase;

        public InternalState() {
            super();
            phase = AlgorithmPhase.init;
        }
    }
}
