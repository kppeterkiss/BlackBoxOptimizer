package optimizer.algorithms;

import optimizer.common.Solution;
import optimizer.config.TestConfig;
import optimizer.objective.Objective;
import optimizer.objective.Relation;
import optimizer.param.Param;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

public class FireFlyAlgorithmTest {
    FireFlyAlgorithm algo;
    TestConfig config;
    int swarm_size = 20;

    @Before
    public void setup() throws CloneNotSupportedException {
        config = initConfig();
        algo = new FireFlyAlgorithm();
        algo.setConfiguration(config);
        // change the swarm_size

        algo.optimizerParams.get(1).setInitValue(swarm_size);
    }

    @Test
    public void test_initialization_phase() {
        assertTrue(algo.state.init);
        algo.updateParameters(config.getScriptParametersReference(), config.getLandscapeReference());

        // search space
        assertEquals(algo.state.dimension, 2);
        for (int i = 0; i < algo.state.dimension; ++i) {
            assertEquals(algo.state.lowerBounds[i], config.getScriptParametersReference().get(i).getLowerBound());
            assertEquals(algo.state.upperBounds[i], config.getScriptParametersReference().get(i).getUpperBound());
        }

        // swarm
        assertEquals(algo.state.swarm.size(), swarm_size);

        assertFalse(algo.state.init);
    }

    private TestConfig initConfig() throws CloneNotSupportedException {
        TestConfig config = new TestConfig();

        List<Param> pl = new LinkedList<>();
        pl.add(new Param(1f,10f,-10f,"param1"));
        pl.add(new Param(5f,10f,-10f, "param2"));
        config.setScriptParameters(pl);

        /*
        ObjectiveContainer oc = new ObjectiveContainer();
        oc.getObjectiveListReference().add(testObjective(1.f));
        config.setObjectiveContainer(oc);

        List<IterationResult> landscape = new LinkedList<>();
        landscape.add(
                new IterationResult(
                        config.getScriptParametersReference(),
                        config.getObjectiveContainerReference(),
                        0,
                        0));
        config.setLandscape(landscape);
        */
        return config;
    }

    private Objective testObjective(float value) {
        return new Objective(
                Relation.MAXIMIZE,
                false,
                "objective",
                value,
                0.f,
                0.f,
                1.f);
    }

}