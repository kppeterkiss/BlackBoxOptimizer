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

import optimizer.trial.IterationResult;
import optimizer.param.Param;
import optimizer.utils.Utils;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by david on 2017. 08. 12..
 */
public class DifferentialEvolution extends AbstractAlgorithm {
    InternalState is = new InternalState();

    {
        this.optimizerParams = new LinkedList<>();
        this.optimizerParams.add(new Param(5,Integer.MAX_VALUE,Integer.MIN_VALUE,"population_size"));
        this.optimizerParams.add(new Param(0.5, Utils.FLOAT_REDEFINED_MAX_VALUE,Float.MIN_VALUE,"phi"));
        this.optimizerParams.add(new Param(0.5,Utils.FLOAT_REDEFINED_MAX_VALUE,Float.MIN_VALUE,"kappa"));
    }

    @Override
    public void updateParameters(List<Param> parameterMap, List<IterationResult> landscape) {

        try {
            Random rand = new Random();

            if(is.firstGeneration) {
                //init searchspace+
                for(Param p : parameterMap) {
                    float lb = ((Number)p.getLowerBound()).floatValue();
                    float ub = ((Number)p.getUpperBound()).floatValue();
                    float r = rand.nextFloat();
                    p.setInitValue(lb + r * (ub - lb));
                }
                ++is.actualIndividual;
                // end of the first round
                if(is.actualIndividual == ((Number)optimizerParams.get(0).getValue()).intValue()) {
                    is.firstGeneration = false;
                    is.firstMutation = true;
                    is.members = new int[(int)optimizerParams.get(0).getValue()];
                    is.newmembers = new int[(int)optimizerParams.get(0).getValue()];
                    for(int i = 0; i < is.members.length; ++i)
                        is.members[i] = i;
                    is.actualIndividual = 0;
                }
                return;
            }

            if(!is.firstMutation) {
                //
                select(landscape);
            }

            mutate(landscape);


        }
        catch(CloneNotSupportedException e) {
            e.printStackTrace();
        }
    }

    private void select(List<IterationResult> landscape) {
        is.newmembers[is.actualIndividual] = landscape.get(landscape.size()-1).getFitness() > landscape.get(is.members[is.actualIndividual]).getFitness() ?
                landscape.size()-1 : is.members[is.actualIndividual];
        ++is.actualIndividual;
        if(is.actualIndividual == is.members.length) {
            is.members = is.newmembers.clone();
            is.actualIndividual = 0;
        }
    }

    private void mutate(List<IterationResult> landscape) throws CloneNotSupportedException {
        Random rand = new Random();
        int a,b,c;
        a = rand.nextInt(is.members.length-1);
        b = rand.nextInt(is.members.length-1);
        c = rand.nextInt(is.members.length-1);

        List<Param> xm =landscape.get(is.members[a]).getConfigurationClone();
        for(int i = 0; i < xm.size(); ++i) {
            float phi = ((Number) optimizerParams.get(1).getValue()).floatValue();
            float b_i = ((Number) landscape.get(is.members[b]).getConfigurationClone().get(i).getValue()).floatValue();
            float c_i = ((Number) landscape.get(is.members[c]).getConfigurationClone().get(i).getValue()).floatValue();
            xm.get(i).add((b_i -c_i)* phi);
        }
        Param.refillList(is.xc,landscape.get(is.members[is.actualIndividual]).getConfigurationClone());
//// TODO: 23/09/17
        for(int i = 0; i < is.members.length && i<is.xc.size(); ++i) {
            float r = rand.nextFloat();
            float kappa = ((Number) optimizerParams.get(2).getValue()).floatValue();
            if(r < kappa) {
                is.xc.get(i).setInitValue(xm.get(i).getValue());
            }
        }
    }


    @Override
    public void loadState(String internalStateBackupFileName) throws FileNotFoundException {
    }

    @Override
    public void saveState(String internalStateBackupFileName) {

    }
    @Override
    public void updateConfigFromAlgorithmParams(List<Param> algParams) {
        //nothing to do here
    }


    class InternalState {
        boolean firstGeneration = true;
        int actualIndividual = 0;
        int[] members;
        int[] newmembers;
        List<Param> xc = new ArrayList<>();
        boolean firstMutation = false;

    }
}
