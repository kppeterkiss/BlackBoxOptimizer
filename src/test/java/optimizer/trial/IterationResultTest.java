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

package optimizer.trial;

import optimizer.config.TestConfig;
import optimizer.objective.Objective;
import optimizer.objective.Relation;
import optimizer.param.Param;
import optimizer.trial.IterationResult;
import optimizer.utils.Utils;
import org.junit.Test;

import java.io.FileNotFoundException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IterationResultTest {

    @Test
    public void cloningTest() throws FileNotFoundException, CloneNotSupportedException {
        TestConfig tc = TestConfig.readConfigJSON("src/test/resources/Test/setup/complicated_test.json");
        IterationResult ir = new IterationResult(tc.getScriptParametersReference(),tc.getObjectiveContainerReference(),0,0);
        for(int i=0;i<tc.getScriptParametersReference().size();++i ){
            Param p = tc.getScriptParametersReference().get(i);
            if(p.isNumeric())
                p.add(1);
            else if(p.isEnumeration()){
                if(ir.getConfigurationClone().get(i).getValue().equals(p.getAllValueArray()[0]))
                    p.setInitValue(p.getAllValueArray()[1]);
                else
                    p.setInitValue(p.getAllValueArray()[0]);
            }
            else {
                if( ir.getConfigurationClone().get(i).getValue().equals(false))
                    p.setInitValue(true);
                else
                    p.setInitValue(false);
            }
            assertFalse(p.getValue().equals(ir.getConfigurationClone().get(i).getValue()));

        }


    }
    @Test
    public void getFitnessTest() throws FileNotFoundException, CloneNotSupportedException {
        TestConfig tc = TestConfig.readConfigJSON("src/test/resources/Test/setup/complicated_test.json");


        for(Objective o : tc.getObjectiveContainerReference().getObjectiveListReference()){
            for(Objective o1 : tc.getObjectiveContainerReference().getObjectiveListReference()) {
                if(o1.getType().equals(Integer.class)) {

                    if(o1.getRelation().equals(Relation.LESS_THAN))
                        o1.setValue((int)o1.getTarget()+10);
                    else if(o1.getRelation().equals(Relation.GREATER_THAN))
                        o1.setValue((int)o1.getTarget()-10);
                    else{
                        o1.setValue(0);
                        o1.setValue(2);
                    }
                }

                else {

                    if(o1.getRelation().equals(Relation.LESS_THAN))
                        o1.setValue((float)o1.getTarget()+10f);
                    else if(o1.getRelation().equals(Relation.GREATER_THAN))
                        o1.setValue((float)o1.getTarget()-10f);
                    else{
                        o1.setValue(0f);
                        o1.setValue(2f);
                    }
                }
            }
            IterationResult ir = new IterationResult(tc.getScriptParametersReference(),tc.getObjectiveContainerReference(),0,0);

            if(o.getType().equals(Integer.class)) {

                if (o.getRelation().equals(Relation.MINIMIZE_TO_CONVERGENCE) || o.getRelation().equals(Relation.MAXIMIZE_TO_CONVERGENCE)) {
                    o.setValue(1);
                } else if (o.getRelation().equals(Relation.GREATER_THAN) || o.getRelation().equals(Relation.MAXIMIZE))
                    o.setValue((int) o.getValue() + 1);
                else
                    o.setValue((int) o.getValue() - 1);
            }
            else
            {
                if (o.getRelation().equals(Relation.MINIMIZE_TO_CONVERGENCE) || o.getRelation().equals(Relation.MAXIMIZE_TO_CONVERGENCE)) {
                    o.setValue(1f);
                } else if (o.getRelation().equals(Relation.GREATER_THAN)|| o.getRelation().equals(Relation.MAXIMIZE))
                    o.setValue((float) o.getValue() + 1);
                else
                    o.setValue((float) o.getValue() - 1);
            }
            IterationResult ir1 = new IterationResult(tc.getScriptParametersReference(),tc.getObjectiveContainerReference(),0,0);
            assertTrue(ir1.betterThan(ir));

        }





    }
}
