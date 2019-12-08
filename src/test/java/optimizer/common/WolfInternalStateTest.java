package optimizer.common;
import optimizer.algorithms.GreyWolfOptimizer;
import optimizer.algorithms.GreyWolfOptimizer.InternalState;

import optimizer.objective.Objective;
import optimizer.objective.ObjectiveContainer;
import optimizer.objective.Relation;
import optimizer.param.Param;
import optimizer.trial.IterationResult;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;


public class WolfInternalStateTest {
    GreyWolfOptimizer algo;
    private final int dimension;
    private float[] lowerBounds;
    private float[] upperBounds;

    private List<Param> paramerters = new LinkedList<>();
    private Param p1 = new Param(2f,3f,1f,"p1");
    private Param p2 = new Param(5f,10f,4f,"p2");

    private Random rand;
    private InternalState state;

    public WolfInternalStateTest() {
        algo = new GreyWolfOptimizer();
        dimension = 2;
        lowerBounds = new float[]{-10, -10};
        upperBounds = new float[]{ 10,  10};
        rand = new Random();
        state = algo.getState();
    }

    private void setup_data(int swarm_size, float[] values, Relation relation) throws CloneNotSupportedException {
        for (int i = 0; i < swarm_size; ++i) {
            ArrayList<Objective> objList = new ArrayList<>();

            //value: fitness value
            Objective obj = new Objective(relation, true, "test1", values[i], 0f, 0f, 1f);

            objList.add(obj);

            ObjectiveContainer objectiveContainer1 = new ObjectiveContainer(objList);

            List<Param> params = new LinkedList<>();
            params.add(p1);
            params.add(p2);
            IterationResult res1 = new IterationResult(params, objectiveContainer1, 0, 1);

            state.swarm.add(new Solution(dimension, lowerBounds, upperBounds, rand));
            state.swarm.get(state.swarm.size() - 1).actualFitness = res1;
        }

    }

    @Test
    public void leaders_are_sorted_in_case_of_maximize() throws CloneNotSupportedException {
        paramerters.add(p1);
        paramerters.add(p2);

        state.initSearchSpace(paramerters);
        float[] fitness_values = {2, 22, 59, 23, 47, 17, 37, 1, 27, 2};
        setup_data(10, fitness_values, Relation.MAXIMIZE);

        Collections.sort(state.swarm);
        assertEquals(59.0, state.swarm.get(0).getActualFitness().getFitness(), 0.001);
        assertEquals(47.0, state.swarm.get(1).getActualFitness().getFitness(), 0.001);
        assertEquals(37.0, state.swarm.get(2).getActualFitness().getFitness(), 0.001);
    }

    @Test
    public void leaders_are_sorted_in_case_of_minimize() throws CloneNotSupportedException {
        paramerters.add(p1);
        paramerters.add(p2);

        state.initSearchSpace(paramerters);
        float[] fitness_values = {2, 22, 59, 23, 47, 17, 37, 1, 27, 2};
        setup_data(10, fitness_values, Relation.MINIMIZE);

        Collections.sort(state.swarm);
        assertEquals(-1.0, state.swarm.get(0).getActualFitness().getFitness(), 0.001);
        assertEquals(-2.0, state.swarm.get(1).getActualFitness().getFitness(), 0.001);
        assertEquals(-2.0, state.swarm.get(2).getActualFitness().getFitness(), 0.001);
    }
}