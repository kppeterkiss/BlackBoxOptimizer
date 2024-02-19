/*
 *   Copyright 2018 Peter Kiss and David Fonyo
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package optimizer.objective;

import optimizer.config.TestConfig;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ObjectiveContainerTest {



    @Test
    public void terminationTest() throws FileNotFoundException {
        TestConfig tc = TestConfig.readConfigJSON("src/test/resources/Test/setup/complicated_test.json");
        ObjectiveContainer oc = tc.getObjectiveContainerReference();
        assertTrue(oc.getObjectiveListReference().size()==6);
        assertTrue(!oc.terminated());
        List<Objective> newList = new LinkedList<>();
        List<Objective> nonterminatingList = new LinkedList<>();

        for(Objective o : oc.getObjectiveListReference()) {
            if (o.getRelation() != Relation.MINIMIZE && o.getRelation() != Relation.MAXIMIZE)
                newList.add(o);
            else
                nonterminatingList.add(o);
        }
        ObjectiveContainer oc1 = new ObjectiveContainer(newList);
        for(Objective o : oc1.getObjectiveListReference()){
            if(o.getType().equals(Integer.class)) {
                if (o.getRelation().equals(Relation.MINIMIZE_TO_CONVERGENCE) || o.getRelation().equals(Relation.MAXIMIZE_TO_CONVERGENCE)) {
                    o.setValue(10);
                    o.setValue(10);
                } else if (o.getRelation().equals(Relation.GREATER_THAN))
                    o.setValue((int) o.getTarget() + 1);
                else
                    o.setValue((int) o.getTarget() - 1);
            }
            else
            {
                if (o.getRelation().equals(Relation.MINIMIZE_TO_CONVERGENCE) || o.getRelation().equals(Relation.MAXIMIZE_TO_CONVERGENCE)) {
                    o.setValue(10f);
                    o.setValue(10f);
                } else if (o.getRelation().equals(Relation.GREATER_THAN))
                    o.setValue((float) o.getTarget() + 1);
                else
                    o.setValue((float) o.getTarget() - 1);
            }

        }
        assertTrue(oc1.terminated());
        oc1.getObjectiveListReference().addAll(nonterminatingList);
        assertTrue(!oc1.terminated());



    }

    /*@Test
    public void readObjectivesTest() throws Exception {
        TestConfig tc = TestConfig.readConfigJSON("src/test/resources/Test/setup/complicated_test.json");
        Trial t = new Trial(tc.getBaseCommand(),false,null,tc.getObjectiveContainerReference(),tc.getScriptParametersReference(),0,0);
        IterationResult ir = t.call();
    }*/
    @Test
    public void readObjectivesTest() throws Exception {
        TestConfig tc = TestConfig.readConfigJSON("src/test/resources/Test/setup/complicated_test.json");
        for(Objective o : tc.getObjectiveContainerReference().getObjectiveListReference())
            assertTrue(o.getValue()==null);

        String command = TestConfig.getCommand(tc.getScriptParametersReference(),tc.getBaseCommand());
        BufferedReader outputReader, errorReader;
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec(command);
        pr.waitFor();

        errorReader = new BufferedReader(new InputStreamReader(pr.getErrorStream()));

        outputReader= new BufferedReader(new InputStreamReader(pr.getInputStream()));

        ObjectiveContainer oc =  ObjectiveContainer.readObjectives(outputReader,errorReader,tc.getObjectiveContainerReference());
        int foundO = getFoundO(tc);
        assertTrue(foundO==2);
        //now the BBF will crash-> no objective
        tc.getScriptParametersReference().stream().filter(p->p.getParamGenericType().equals(Float.class)&&!p.isEnumeration()).forEach(p->p.setInitValue(-1f) );
        ObjectiveContainer.readObjectives(outputReader,errorReader,tc.getObjectiveContainerReference());
        foundO = getFoundO(tc);
        assertTrue(foundO==0);



    }

    private int getFoundO(TestConfig tc) {
        int foundO = 0;
        for(Objective o : tc.getObjectiveContainerReference().getObjectiveListReference()) {
            Double value = new Double(((Number) o.getValue()).doubleValue());
           if(!value.equals(0.))
               foundO++;
        }
        return foundO;
    }
}
