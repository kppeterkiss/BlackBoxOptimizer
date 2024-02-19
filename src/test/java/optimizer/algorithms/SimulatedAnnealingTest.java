package optimizer.algorithms;

import optimizer.config.TestConfig;
import optimizer.objective.Objective;
import optimizer.objective.ObjectiveContainer;
import optimizer.objective.Relation;
import optimizer.param.FunctionParam;
import optimizer.param.Param;
import optimizer.trial.IterationResult;
import org.junit.Test;

import javax.script.ScriptException;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Created by david on 2018. 08. 20..
 */
public class SimulatedAnnealingTest {

    @Test
    public void startTest() throws CloneNotSupportedException {

        TestConfig c = initConfig();

        SimulatedAnnealing alg = new SimulatedAnnealing();
        alg.setConfiguration(c);

        assertTrue(alg.is.hits == 0);
    }

    @Test
    public void stepsizeTest() throws CloneNotSupportedException {

        TestConfig c = initConfig();

        SimulatedAnnealing alg = new SimulatedAnnealing();
        alg.setConfiguration(c);

        List<Param> center = c.getLandscapeReference().get(c.getLandscapeReference().size()-1-alg.is.hits).getConfigurationClone();

        alg.updateParameters(c.getScriptParametersReference(),c.getLandscapeReference());
        c.getObjectiveContainerReference().getObjectiveListReference().get(0).setValue(0);
        c.getLandscapeReference().add(new IterationResult(c.getScriptParametersReference(), c.getObjectiveContainerReference(), 0, 0));

        assertTrue(Math.abs(((Number)center.get(0).getValue()).doubleValue() - ((Number)alg.getConfig().get(0).getValue()).doubleValue()) <= ((Number)alg.getOptimizerParams().get(0).getValue()).doubleValue());
        assertTrue(Math.abs(((Number)center.get(1).getValue()).doubleValue() - ((Number)alg.getConfig().get(1).getValue()).doubleValue()) <= ((Number)alg.getOptimizerParams().get(0).getValue()).doubleValue());

    }

    @Test
    public void hitTest() throws CloneNotSupportedException {

        TestConfig c = initConfig();

        SimulatedAnnealing alg = new SimulatedAnnealing();
        alg.setConfiguration(c);

        List<Param> oldcenter = c.getLandscapeReference().get(c.getLandscapeReference().size()-1-alg.is.hits).getConfigurationClone();

        alg.updateParameters(c.getScriptParametersReference(),c.getLandscapeReference());
        c.getObjectiveContainerReference().getObjectiveListReference().get(0).setValue(2);
        c.getLandscapeReference().add(new IterationResult(c.getScriptParametersReference(), c.getObjectiveContainerReference(), 0, 0));

        alg.updateParameters(c.getScriptParametersReference(),c.getLandscapeReference());
        c.getObjectiveContainerReference().getObjectiveListReference().get(0).setValue(2);
        c.getLandscapeReference().add(new IterationResult(c.getScriptParametersReference(), c.getObjectiveContainerReference(), 0, 0));
        List<Param> center = c.getLandscapeReference().get(c.getLandscapeReference().size()-1-alg.is.hits).getConfigurationClone();
        assertTrue(!Param.compareParamLists(oldcenter,center));

    }

    @Test
    public void runTest() throws CloneNotSupportedException {

        TestConfig c = initConfig();

        SimulatedAnnealing alg = new SimulatedAnnealing();
        alg.setConfiguration(c);

        List<Param> oldcenter = c.getLandscapeReference().get(c.getLandscapeReference().size()-1-alg.is.hits).getConfigurationClone();

        alg.updateParameters(c.getScriptParametersReference(),c.getLandscapeReference());
        c.getObjectiveContainerReference().getObjectiveListReference().get(0).setValue(0);
        c.getLandscapeReference().add(new IterationResult(c.getScriptParametersReference(), c.getObjectiveContainerReference(), 0, 0));

        alg.updateParameters(c.getScriptParametersReference(),c.getLandscapeReference());
        c.getObjectiveContainerReference().getObjectiveListReference().get(0).setValue(0);
        c.getLandscapeReference().add(new IterationResult(c.getScriptParametersReference(), c.getObjectiveContainerReference(), 0, 0));
        List<Param> center = c.getLandscapeReference().get(c.getLandscapeReference().size()-1-alg.is.hits).getConfigurationClone();
        assertTrue(Param.compareParamLists(oldcenter,center));
    }


    @Test
    public void temperatureTest() throws CloneNotSupportedException {
        TestConfig c = initConfig();

        SimulatedAnnealing alg = new SimulatedAnnealing();
        alg.setConfiguration(c);

        Float[] ti = (Float[])alg.getOptimizerParams().get(2).getAllValueArray();
        assertTrue(ti[3] == 0.25f);
        assertTrue(ti[1] == 0.5f);

        try {
            alg.getOptimizerParams().set(2, new FunctionParam("temperature","$n*$n",10));
            // TODO: "$n^2" not works
        } catch (ScriptException e) {
            e.printStackTrace();
        }
        ti = (Float[])alg.getOptimizerParams().get(2).getAllValueArray();
        assertTrue(ti[4] == 16.f);
    }




    private TestConfig initConfig() throws CloneNotSupportedException {

        TestConfig c = new TestConfig();


        List<Param> pl = new LinkedList<>();
        pl.add(new Param(1.f,2.f,0.f,"param1"));
        pl.add(new Param(5.f,6.f,4.f, "param2"));
        c.setScriptParameters(pl);

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
