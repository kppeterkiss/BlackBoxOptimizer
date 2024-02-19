package optimizer.common;

import optimizer.objective.Objective;
import optimizer.objective.ObjectiveContainer;
import optimizer.objective.Relation;
import optimizer.param.Param;
import optimizer.trial.IterationResult;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RoulettWheelSelectionTest {
    int dimension = 2;
    float[] lowerBounds = {0, 10};
    float[] upperBounds = {0, 10};
    Param p1 = new Param(5f,10f,0f,"p1");
    Param p2 = new Param(5f,10f,0f,"p2");

    Random randMock = mock(Random.class);

    ArrayList<Solution> swarm = new ArrayList<>();
    RouletteWheelSelection selection = new RouletteWheelSelection();
    IterationResult swarmBestFitness;

    void setup_data(float val1, float val2, float val3, Relation relation) throws CloneNotSupportedException {

        ArrayList<Objective> objList1 = new ArrayList<>();
        ArrayList<Objective> objList2 = new ArrayList<>();
        ArrayList<Objective> objList3 = new ArrayList<>();

        Objective obj1 = new Objective(relation, true, "test1",val1,0f,0f,100f);
        Objective obj2 = new Objective(relation, true, "test2",val2,0f,0f,100f);
        Objective obj3 = new Objective(relation, true, "test3",val3,0f,0f,100f);

        objList1.add(obj1);
        objList2.add(obj2);
        objList3.add(obj3);

        ObjectiveContainer objectiveContainer1 = new ObjectiveContainer(objList1);
        ObjectiveContainer objectiveContainer2 = new ObjectiveContainer(objList2);
        ObjectiveContainer objectiveContainer3 = new ObjectiveContainer(objList3);

        List<Param> params = new LinkedList<>();
        params.add(p1);
        params.add(p2);
        IterationResult res1 = new IterationResult(params,objectiveContainer1, 0, 1);
        IterationResult res2 = new IterationResult(params,objectiveContainer2, 0, 1);
        IterationResult res3 = new IterationResult(params,objectiveContainer3, 0, 1);

        swarm.add(new Solution(dimension,lowerBounds,upperBounds,randMock));
        swarm.get(swarm.size() - 1).actualFitness = res1;

        swarm.add(new Solution(dimension,lowerBounds,upperBounds,randMock));
        swarm.get(swarm.size() - 1).actualFitness = res2;

        swarm.add(new Solution(dimension,lowerBounds,upperBounds,randMock));
        swarm.get(swarm.size() - 1).actualFitness = res3;

        swarmBestFitness = res3;
    }

    @Before
    public void setup() throws CloneNotSupportedException {
        when(randMock.nextFloat()).thenReturn(0.5f);
    }

    @Test
    public void createProbabilities_1() {
        try {
            setup_data(1f,2f,3f, Relation.MAXIMIZE);
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        List<Probability> probs = selection.createProbabilities(3, swarm, swarmBestFitness);
        assertEquals(0, probs.get(0).getId());
        assertEquals(0, probs.get(0).getProbability(), 0.0001);

        assertEquals(0, probs.get(1).getId());
        assertEquals(0.16666, probs.get(1).getProbability(), 0.0001);

        assertEquals(1, probs.get(2).getId());
        assertEquals(0.33333, probs.get(2).getProbability(), 0.0001);

        assertEquals(2, probs.get(3).getId());
        assertEquals(0.5, probs.get(3).getProbability(), 0.0001);

        selection.createIntervals(probs);

        assertEquals(0, probs.get(0).getId());
        assertEquals(0, probs.get(0).getProbability(), 0.0001);

        assertEquals(0, probs.get(1).getId());
        assertEquals(0.16666, probs.get(1).getProbability(), 0.0001);

        assertEquals(1, probs.get(2).getId());
        assertEquals(0.5, probs.get(2).getProbability(), 0.0001);

        assertEquals(2, probs.get(3).getId());
        assertEquals(1, probs.get(3).getProbability(), 0.0001);
    }

    @Test
    public void createProbabilities_2() {
        try {
            setup_data(1f,2f,3f, Relation.MINIMIZE);
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        List<Probability> probs = selection.createProbabilities(3, swarm, swarmBestFitness);
        assertEquals(2, probs.get(0).getId());
        assertEquals(0, probs.get(0).getProbability(), 0.0001);

        assertEquals(2, probs.get(1).getId());
        assertEquals(0.16666, probs.get(1).getProbability(), 0.0001);

        assertEquals(1, probs.get(2).getId());
        assertEquals(0.33333, probs.get(2).getProbability(), 0.0001);

        assertEquals(0, probs.get(3).getId());
        assertEquals(0.5, probs.get(3).getProbability(), 0.0001);

        selection.createIntervals(probs);

        assertEquals(2, probs.get(0).getId());
        assertEquals(0, probs.get(0).getProbability(), 0.0001);

        assertEquals(2, probs.get(1).getId());
        assertEquals(0.16666, probs.get(1).getProbability(), 0.0001);

        assertEquals(1, probs.get(2).getId());
        assertEquals(0.49999, probs.get(2).getProbability(), 0.0001);

        assertEquals(0, probs.get(3).getId());
        assertEquals(1, probs.get(3).getProbability(), 0.0001);
    }

    @Test
    public void createProbabilities_3() {
        try {
            setup_data(7f,3f,5f, Relation.MINIMIZE);
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        List<Probability> probs = selection.createProbabilities(3, swarm, swarmBestFitness);
        assertEquals(0, probs.get(0).getId());
        assertEquals(0, probs.get(0).getProbability(), 0.0001);

        assertEquals(0, probs.get(1).getId());
        assertEquals(0.19999, probs.get(1).getProbability(), 0.0001);

        assertEquals(2, probs.get(2).getId());
        assertEquals(0.33333, probs.get(2).getProbability(), 0.0001);

        assertEquals(1, probs.get(3).getId());
        assertEquals(0.46666, probs.get(3).getProbability(), 0.0001);

        selection.createIntervals(probs);

        assertEquals(0, probs.get(0).getId());
        assertEquals(0, probs.get(0).getProbability(), 0.0001);

        assertEquals(0, probs.get(1).getId());
        assertEquals(0.19999, probs.get(1).getProbability(), 0.0001);

        assertEquals(2, probs.get(2).getId());
        assertEquals(0.53333, probs.get(2).getProbability(), 0.0001);

        assertEquals(1, probs.get(3).getId());
        assertEquals(1, probs.get(3).getProbability(), 0.0001);

    }

    @Test
    public void createProbabilities_4() {
        try {
            setup_data(101f,102f,103f, Relation.MAXIMIZE);
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        List<Probability> probs = selection.createProbabilities(3, swarm, swarmBestFitness);
        assertEquals(0, probs.get(0).getId());
        assertEquals(0, probs.get(0).getProbability(), 0.0001);

        assertEquals(0, probs.get(1).getId());
        assertEquals(0.33006, probs.get(1).getProbability(), 0.0001);

        assertEquals(1, probs.get(2).getId());
        assertEquals(0.33333, probs.get(2).getProbability(), 0.0001);

        assertEquals(2, probs.get(3).getId());
        assertEquals(0.33660, probs.get(3).getProbability(), 0.0001);

        selection.createIntervals(probs);

        assertEquals(0, probs.get(0).getId());
        assertEquals(0, probs.get(0).getProbability(), 0.0001);

        assertEquals(0, probs.get(1).getId());
        assertEquals(0.33006, probs.get(1).getProbability(), 0.0001);

        assertEquals(1, probs.get(2).getId());
        assertEquals(0.66339, probs.get(2).getProbability(), 0.0001);

        assertEquals(2, probs.get(3).getId());
        assertEquals(1, probs.get(3).getProbability(), 0.0001);

    }

    @Test
    public void createProbabilities_5() {
        try {
            setup_data(101f,102f,103f, Relation.MINIMIZE);
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        List<Probability> probs = selection.createProbabilities(3, swarm, swarmBestFitness);
        assertEquals(2, probs.get(0).getId());
        assertEquals(0, probs.get(0).getProbability(), 0.0001);

        assertEquals(2, probs.get(1).getId());
        assertEquals(0.33006, probs.get(1).getProbability(), 0.0001);

        assertEquals(1, probs.get(2).getId());
        assertEquals(0.33333, probs.get(2).getProbability(), 0.0001);

        assertEquals(0, probs.get(3).getId());
        assertEquals(0.33660, probs.get(3).getProbability(), 0.0001);

        selection.createIntervals(probs);

        assertEquals(2, probs.get(0).getId());
        assertEquals(0, probs.get(0).getProbability(), 0.0001);

        assertEquals(2, probs.get(1).getId());
        assertEquals(0.33006, probs.get(1).getProbability(), 0.0001);

        assertEquals(1, probs.get(2).getId());
        assertEquals(0.66339, probs.get(2).getProbability(), 0.0001);

        assertEquals(0, probs.get(3).getId());
        assertEquals(1, probs.get(3).getProbability(), 0.0001);
    }


    @Test
    public void createProbabilities_6() {
        try {
            setup_data(2f,4f,8f, Relation.MINIMIZE);
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        List<Probability> probs = selection.createProbabilities(3, swarm, swarmBestFitness);
        assertEquals(2, probs.get(0).getId());
        assertEquals(0, probs.get(0).getProbability(), 0.0001);

        assertEquals(2, probs.get(1).getId());
        assertEquals(0.14285, probs.get(1).getProbability(), 0.0001);

        assertEquals(1, probs.get(2).getId());
        assertEquals(0.28571, probs.get(2).getProbability(), 0.0001);

        assertEquals(0, probs.get(3).getId());
        assertEquals(0.57142, probs.get(3).getProbability(), 0.0001);

        selection.createIntervals(probs);

        assertEquals(2, probs.get(0).getId());
        assertEquals(0, probs.get(0).getProbability(), 0.0001);

        assertEquals(2, probs.get(1).getId());
        assertEquals(0.14285, probs.get(1).getProbability(), 0.0001);

        assertEquals(1, probs.get(2).getId());
        assertEquals(0.42857, probs.get(2).getProbability(), 0.0001);

        assertEquals(0, probs.get(3).getId());
        assertEquals(1, probs.get(3).getProbability(), 0.0001);
    }

    @Test
    public void createIntervals_last_probability_is_rounded_to_one() {
        List<Probability> probs = new LinkedList<>();
        probs.add(new Probability(0, 0));
        probs.add(new Probability(0, 0.15));
        probs.add(new Probability(0, 0.7));
        probs.add(new Probability(0, 0.1));

        selection.createIntervals(probs);

        assertEquals(0, probs.get(0).getProbability(), 0.0000000001);
        assertEquals(0.15, probs.get(1).getProbability(), 0.0000000001);
        assertEquals(0.85, probs.get(2).getProbability(), 0.0000000001);
        assertEquals(1, probs.get(3).getProbability(), 0.0000000001);
    }
}