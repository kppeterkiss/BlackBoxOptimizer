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

package optimizer.algorithms;
import optimizer.config.TestConfig;
import optimizer.param.Param;
import optimizer.trial.IterationResult;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertTrue;


public class GridSearchTest {

    private List<Param> createTestParamList(){
        List<Param> parammap = new LinkedList<>();
        parammap.add(new Param(1.f,2.f,1.f,"float_param"));
        parammap.add(new Param(1,2,1,"int_param"));
        parammap.add(new Param(false,true,false, "boolean_param"));
        String[] a1 = {"1","2","3"};
        parammap.add(new Param("1",a1,"enum_param1"));
        String[] a2 = {"4","5","6"};
        parammap.add(new Param("4",a2,"enum_param2"));
        return parammap;
    }

    @Test
    public void startTest() throws CloneNotSupportedException {
        TestConfig c = intitconfig();
        GridSearch alg = getGridSearchSetup(c);

        List<Param> orig_pl = Param.cloneParamList(c.getScriptParametersReference());


        alg.updateParameters(c.getScriptParametersReference(),c.getLandscapeReference());
        List<Integer> indices =new LinkedList<Integer>();
        indices.add(orig_pl.size()-1);
        assertTrue(sameValueExceptForIndex(indices,c.getScriptParametersReference(),orig_pl));

    }

    private GridSearch getGridSearchSetup(TestConfig c) {
        GridSearch alg = new GridSearch();
        alg.setConfiguration(c);
        alg.updateConfigFromAlgorithmParams(c.getScriptParametersReference());
        return alg;
    }

    private TestConfig intitconfig() {
        TestConfig c= new TestConfig();
        List<IterationResult> landscape = new LinkedList<>();
        List<Param> pl = createTestParamList();
        c.setScriptParameters(pl);
        return c;
    }

    @Test
    public void optimizerParamInitTest(){
        TestConfig c = intitconfig();
        GridSearch alg = getGridSearchSetup(c);
        assertTrue(alg.getOptimizerParams().size()==c.getScriptParametersReference().size());

        for(int i=0;i<alg.getOptimizerParams().size();++i) {
            assertTrue(alg.getOptimizerParams().get(i).getName().equals(c.getScriptParametersReference().get(i).getName()+"_step_size"));
        }

    }

    @Test
    public void turnoverTests() throws CloneNotSupportedException {
        TestConfig c = intitconfig();
        List<Param> orig_pl = Param.cloneParamList(c.getScriptParametersReference());

        for(int i =c.getScriptParametersReference().size()-1; i>0 ;i--) {
            c.setScriptParameters(Param.cloneParamList(orig_pl));
            GridSearch alg = getGridSearchSetup(c);
            List<Integer> changedIndices = new LinkedList<>();
            for(int j = c.getScriptParametersReference().size()-1; j>=i;--j) {
                putToTheEdge(j, c.getScriptParametersReference());
                changedIndices.add(j);
            }

            List<Param> pl_before_update = Param.cloneParamList(c.getScriptParametersReference());
            if(changedIndices.size()<c.getScriptParametersReference().size())
                changedIndices.add(changedIndices.get(changedIndices.size()-1)-1);
            alg.updateParameters(c.getScriptParametersReference(),new LinkedList<>());
            assertTrue(sameValueExceptForIndex(changedIndices,c.getScriptParametersReference(),pl_before_update ));
        }

    }

    @Test
    public void fullTurnoverTests() throws CloneNotSupportedException {
        TestConfig c = intitconfig();
        List<Param> orig_pl = Param.cloneParamList(c.getScriptParametersReference());

        c.setScriptParameters(Param.cloneParamList(orig_pl));
        GridSearch alg = getGridSearchSetup(c);
        List<Integer> changedIndices = new LinkedList<>();
        for(int j = c.getScriptParametersReference().size()-1; j>=0;--j) {
            putToTheEdge(j, c.getScriptParametersReference());
            changedIndices.add(j);
        }

        List<Param> pl_before_update = Param.cloneParamList(c.getScriptParametersReference());
        if(changedIndices.size()<c.getScriptParametersReference().size())
            changedIndices.add(changedIndices.get(changedIndices.size()-1)-1);
        alg.updateParameters(c.getScriptParametersReference(),new LinkedList<>());
        assertTrue(sameValueExceptForIndex(changedIndices,c.getScriptParametersReference(),pl_before_update ));


    }



    void putToTheEdge(int index,List<Param> l1){
        Param p =  l1.get(index);
        p.setInitValue(p.getUpperBound());
    }

    boolean sameValueExceptForIndex(List<Integer> index,List<Param> l1, List<Param> l2){
        if(l1.size()!=l2.size())
            return false;
        for(int i =0; i< l1.size();++i)
            if(!index.contains(i) && !l1.get(i).getValue().equals(l2.get(i).getValue()))
                return false;
        for(int i : index)
            if(l1.get(i).getValue().equals(l2.get(i).getValue()))
                return false;
        return true;

    }

    boolean checkDifferences(List<Integer> index,List<Param> l1, List<Param> l2){
        if(l1.size()!=l2.size())
            return false;
        for(int i =0; i< l1.size();++i)
            if(!index.contains(i) && !l1.get(i).getValue().equals(l2.get(i).getValue()))
                return false;
        for(int i : index)
            if(l1.get(i).getValue().equals(l2.get(i).getValue()))
                return false;
        return true;

    }

}
