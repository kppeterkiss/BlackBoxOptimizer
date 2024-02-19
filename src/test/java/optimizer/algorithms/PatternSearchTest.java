package optimizer.algorithms;

import optimizer.config.TestConfig;
import optimizer.objective.Objective;
import optimizer.objective.ObjectiveContainer;
import optimizer.objective.Relation;
import optimizer.param.Param;
import optimizer.trial.IterationResult;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Created by david on 2018. 08. 19..
 */
public class PatternSearchTest {


    @Test
    public void startTest() throws CloneNotSupportedException {

        TestConfig c = initConfig();

        PatternSearch alg = new PatternSearch();
        alg.setConfiguration(c);

        assertTrue(alg.is.firstMove);
        assertTrue(alg.is.consecutiveSteplessIterations == 0);
        assertTrue(alg.is.movesFromCenter == 0);

        alg.updateParameters(c.getScriptParametersReference(),c.getLandscapeReference());
        assertTrue(!alg.is.firstMove);
    }

    @Test
    public void centerChangeTest() throws CloneNotSupportedException {

        TestConfig c = initConfig();

        PatternSearch alg = new PatternSearch();
        alg.setConfiguration(c);

        List<Param> oldCenter = c.getLandscapeReference().get(0).getConfigurationClone();

        alg.updateParameters(c.getScriptParametersReference(),c.getLandscapeReference());
        c.getObjectiveContainerReference().getObjectiveListReference().get(0).setValue(2);
        c.getLandscapeReference().add(new IterationResult(c.getScriptParametersReference(), c.getObjectiveContainerReference(), 0, 0));

        List<Param> center = new LinkedList<>(c.getLandscapeReference().get(c.getLandscapeReference().size() - 1 - alg.is.movesFromCenter).getConfigurationClone());

        assertTrue(Param.compareParamLists(oldCenter,center));

        alg.updateParameters(c.getScriptParametersReference(),c.getLandscapeReference());
        c.getObjectiveContainerReference().getObjectiveListReference().get(0).setValue(2);
        c.getLandscapeReference().add(new IterationResult(c.getScriptParametersReference(), c.getObjectiveContainerReference(), 0, 0));

        center = new LinkedList<>(c.getLandscapeReference().get(c.getLandscapeReference().size() - 1 - alg.is.movesFromCenter).getConfigurationClone());

        assertTrue(Param.compareParamLists(oldCenter,center));

        alg.updateParameters(c.getScriptParametersReference(),c.getLandscapeReference());
        c.getObjectiveContainerReference().getObjectiveListReference().get(0).setValue(2);
        c.getLandscapeReference().add(new IterationResult(c.getScriptParametersReference(), c.getObjectiveContainerReference(), 0, 0));

        center = new LinkedList<>(c.getLandscapeReference().get(c.getLandscapeReference().size() - 1 - alg.is.movesFromCenter).getConfigurationClone());

        assertTrue(Param.compareParamLists(oldCenter,center));

        alg.updateParameters(c.getScriptParametersReference(),c.getLandscapeReference());
        c.getObjectiveContainerReference().getObjectiveListReference().get(0).setValue(2);
        c.getLandscapeReference().add(new IterationResult(c.getScriptParametersReference(), c.getObjectiveContainerReference(), 0, 0));

        center = new LinkedList<>(c.getLandscapeReference().get(c.getLandscapeReference().size() - 1 - alg.is.movesFromCenter).getConfigurationClone());

        assertTrue(Param.compareParamLists(oldCenter,center));

        alg.updateParameters(c.getScriptParametersReference(),c.getLandscapeReference());
        c.getObjectiveContainerReference().getObjectiveListReference().get(0).setValue(2);
        c.getLandscapeReference().add(new IterationResult(c.getScriptParametersReference(), c.getObjectiveContainerReference(), 0, 0));

        center = new LinkedList<>(c.getLandscapeReference().get(c.getLandscapeReference().size() - 1 - alg.is.movesFromCenter).getConfigurationClone());

        assertTrue(!Param.compareParamLists(oldCenter,center));
    }

    @Test
    public void centerStayTest() throws CloneNotSupportedException {

        TestConfig c = initConfig();

        PatternSearch alg = new PatternSearch();
        alg.setConfiguration(c);

        List<Param> oldCenter = c.getLandscapeReference().get(0).getConfigurationClone();

        alg.updateParameters(c.getScriptParametersReference(),c.getLandscapeReference());
        c.getObjectiveContainerReference().getObjectiveListReference().get(0).setValue(0);
        c.getLandscapeReference().add(new IterationResult(c.getScriptParametersReference(), c.getObjectiveContainerReference(), 0, 0));

        List<Param> center = new LinkedList<>(c.getLandscapeReference().get(c.getLandscapeReference().size() - 1 - alg.is.movesFromCenter).getConfigurationClone());

        assertTrue(Param.compareParamLists(oldCenter,center));

        alg.updateParameters(c.getScriptParametersReference(),c.getLandscapeReference());
        c.getObjectiveContainerReference().getObjectiveListReference().get(0).setValue(0);
        c.getLandscapeReference().add(new IterationResult(c.getScriptParametersReference(), c.getObjectiveContainerReference(), 0, 0));

        center = new LinkedList<>(c.getLandscapeReference().get(c.getLandscapeReference().size() - 1 - alg.is.movesFromCenter).getConfigurationClone());

        assertTrue(Param.compareParamLists(oldCenter,center));

        alg.updateParameters(c.getScriptParametersReference(),c.getLandscapeReference());
        c.getObjectiveContainerReference().getObjectiveListReference().get(0).setValue(0);
        c.getLandscapeReference().add(new IterationResult(c.getScriptParametersReference(), c.getObjectiveContainerReference(), 0, 0));

        center = new LinkedList<>(c.getLandscapeReference().get(c.getLandscapeReference().size() - 1 - alg.is.movesFromCenter).getConfigurationClone());

        assertTrue(Param.compareParamLists(oldCenter,center));

        alg.updateParameters(c.getScriptParametersReference(),c.getLandscapeReference());
        c.getObjectiveContainerReference().getObjectiveListReference().get(0).setValue(0);
        c.getLandscapeReference().add(new IterationResult(c.getScriptParametersReference(), c.getObjectiveContainerReference(), 0, 0));

        center = new LinkedList<>(c.getLandscapeReference().get(c.getLandscapeReference().size() - 1 - alg.is.movesFromCenter).getConfigurationClone());

        assertTrue(Param.compareParamLists(oldCenter,center));

        alg.updateParameters(c.getScriptParametersReference(),c.getLandscapeReference());
        c.getObjectiveContainerReference().getObjectiveListReference().get(0).setValue(0);
        c.getLandscapeReference().add(new IterationResult(c.getScriptParametersReference(), c.getObjectiveContainerReference(), 0, 0));

        center = new LinkedList<>(c.getLandscapeReference().get(c.getLandscapeReference().size() - 1 - alg.is.movesFromCenter).getConfigurationClone());

        assertTrue(Param.compareParamLists(oldCenter,center));
    }



    private TestConfig initConfig() throws CloneNotSupportedException {

        TestConfig c = new TestConfig();


        List<Param> pl = new LinkedList<>();

        c.setScriptParameters(pl);
        pl.add(new Param(1.f,2.f,0.f,"param1"));
        pl.add(new Param(5.f,0.f,10.f, "param2"));

        ObjectiveContainer oc = new ObjectiveContainer();
        oc.getObjectiveListReference().add(testObjective(1.f));
        c.setObjectiveContainer(oc);


        List<IterationResult> landscape = new LinkedList<>();
        landscape.add(new IterationResult(c.getScriptParametersReference(), c.getObjectiveContainerReference(),0,0));
        c.setLandscape(landscape);

        return c;
    }



    private Objective testObjective(float value) {
        return new Objective(Relation.MAXIMIZE, false, "objective", value, 0.f, 0.f, 1.f);
    }


}
