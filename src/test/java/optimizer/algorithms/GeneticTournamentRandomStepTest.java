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
 * Created by david on 2018. 08. 20..
 */
public class GeneticTournamentRandomStepTest {

    @Test
    public void startTest() throws CloneNotSupportedException {

        TestConfig c = initConfig();

        GeneticTournamentRandomStep alg = new GeneticTournamentRandomStep();
        alg.setConfiguration(c);

        assertTrue(alg.is.generation == 0);
        assertTrue(alg.is.population == 1);

    }

    @Test
    public void skeletonTest() throws CloneNotSupportedException {
        TestConfig c = initConfig();

        GeneticTournamentRandomStep alg = new GeneticTournamentRandomStep();
        alg.setConfiguration(c);

        assertTrue(alg.is.generation == 0);
        assertTrue(alg.is.population == 1);

        alg.updateParameters(c.getScriptParametersReference(),c.getLandscapeReference());
        c.getObjectiveContainerReference().getObjectiveListReference().get(0).setValue(2);
        c.getLandscapeReference().add(new IterationResult(c.getScriptParametersReference(), c.getObjectiveContainerReference(), 0, 0));
        assertTrue(alg.is.population == 2);


        alg.updateParameters(c.getScriptParametersReference(),c.getLandscapeReference());
        c.getObjectiveContainerReference().getObjectiveListReference().get(0).setValue(2);
        c.getLandscapeReference().add(new IterationResult(c.getScriptParametersReference(), c.getObjectiveContainerReference(), 0, 0));
        assertTrue(alg.is.population == 3);
        assertTrue(alg.is.generation == 0);

        alg.updateParameters(c.getScriptParametersReference(),c.getLandscapeReference());
        c.getObjectiveContainerReference().getObjectiveListReference().get(0).setValue(2);
        c.getLandscapeReference().add(new IterationResult(c.getScriptParametersReference(), c.getObjectiveContainerReference(), 0, 0));
        assertTrue(alg.is.population == 1);
        assertTrue(alg.is.generation == 1);
    }

    @Test
    public void bestChromosomeTest() throws CloneNotSupportedException {
        TestConfig c = initConfig();

        GeneticTournamentRandomStep alg = new GeneticTournamentRandomStep();
        alg.setConfiguration(c);

        alg.updateParameters(c.getScriptParametersReference(),c.getLandscapeReference());
        c.getObjectiveContainerReference().getObjectiveListReference().get(0).setValue(2.f);
        c.getLandscapeReference().add(new IterationResult(c.getScriptParametersReference(), c.getObjectiveContainerReference(), 0, 0));

        alg.updateParameters(c.getScriptParametersReference(),c.getLandscapeReference());
        c.getObjectiveContainerReference().getObjectiveListReference().get(0).setValue(4.f);
        c.getLandscapeReference().add(new IterationResult(c.getScriptParametersReference(), c.getObjectiveContainerReference(), 0, 0));

        alg.updateParameters(c.getScriptParametersReference(),c.getLandscapeReference());
        c.getObjectiveContainerReference().getObjectiveListReference().get(0).setValue(2.f);
        c.getLandscapeReference().add(new IterationResult(c.getScriptParametersReference(), c.getObjectiveContainerReference(), 0, 0));


        alg.updateParameters(c.getScriptParametersReference(),c.getLandscapeReference());
        c.getObjectiveContainerReference().getObjectiveListReference().get(0).setValue(2.f);
        c.getLandscapeReference().add(new IterationResult(c.getScriptParametersReference(), c.getObjectiveContainerReference(), 0, 0));
        assertTrue(alg.is.bestChromosome == 2);

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
