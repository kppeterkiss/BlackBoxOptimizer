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

import java.util.List;
import java.util.Random;

/**
 * Random Search
 * Supported parameter types: float, integer, boolean, enumeration
 * Parallelizable
 *
 * The next parameter configuration is chosen uniform randomly from the search space.
 * Created by peterkiss on 17/10/16.
 */
public class RandomSearch extends AbstractAlgorithm {

    {
        this.parallelizable = ParallelExecution.PARALLEL;
        this.allowedTypes.add(Integer.class);
        this.allowedTypes.add(Boolean.class);
        this.allowedTypes.add(String.class);
    }

    @Override
    public void updateParameters(List< Param> parameterMap, List<IterationResult> landscape/*, List<Param > optimizerParams*/) {
        System.out.println(parameterMap);
        Random rand = new Random();
        for(Param entry : parameterMap)
        {
            if(entry.isActive()) {
                if(entry.isEnumeration()){
                    int enumPos = rand.nextInt(entry.getActiveValueArray().length-1);
                    entry.setInitValue(entry.getActiveValueArray()[enumPos]);
                }
                else if (entry.getValue() instanceof Float ) {
                    float lb = ((Number)entry.getLowerBound()).floatValue();
                    float ub = ((Number)entry.getUpperBound()).floatValue();
                    float r = rand.nextFloat();
                    float newVal = lb + r * (ub - lb);
                    entry.setInitValue(newVal);
                }
                else if (entry.getValue() instanceof Integer) {
                    Integer u = (Integer) entry.getUpperBound();
                    Integer l = (Integer) entry.getLowerBound();
                    int newVal = l + rand.nextInt(u-l) ;
                   entry.setInitValue(newVal);
                }
                else if (entry.getValue() instanceof Boolean) {
                    boolean newVal = rand.nextBoolean();
                   entry.setInitValue(newVal);
                }

            }
        }

    }
    @Override
    public List<Param> getConfig(){
        return this.optimizerParams;
    }

}
