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

import optimizer.param.Param;
import optimizer.trial.IterationResult;
import optimizer.utils.Utils;

import java.io.FileNotFoundException;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by david on 2017. 08. 12..
 */
public class ParticleSwarmOptimizationParallel extends ParticleSwarmOptimization {

    {
        this.parallelizable = ParallelExecution.GENERATION;
    }

    @Override
    public void updateParameters(List<Param> parameterMap, List<IterationResult> landscape) throws CloneNotSupportedException {
        //here parameters are kinda dummy

       if(is.firstStep){
           initSearchSpace(parameterMap);
           initParticles(((Number) optimizerParams.get(0).getValue()).intValue());
           is.firstStep = false;
       }
       else
           for (Particle particle : is.swarm)
                updateParticleVelocity(parameterMap.size(), particle);




    }

    /**
     * Returns the list of experiments to run(parameterlists are cloned)
     * @param pattern
     * @return
     * @throws CloneNotSupportedException
     */
    @Override
    public List<List<Param>> getParameterMapBatch(List<Param> pattern) throws CloneNotSupportedException {
        List<List<Param>> result = new LinkedList<>();
        for(Particle p : is.swarm){
            List<Param> setup =Param.cloneParamList(pattern);
            setParticlePositionForParameterSetup(setup,p);
            result.add(setup);
        }
        return result;


    }

    /**
     * Update the state of particles based on the results of run with the last parametrization
     * @param results
     * @throws CloneNotSupportedException
     */
    @Override
    public void setResults(List<IterationResult> results) throws CloneNotSupportedException {
        int i = 0;
        for(IterationResult res : results){
            updateParticleAndGlobalState(is.swarm.get(i++),res);
        }
    }

    @Override
    public void updateGlobals() throws CloneNotSupportedException {
        updateGlobalBestPositionAndFitness();
    }

    /*
    public List<Param> cloneParameterSetup(List<Param> source) throws CloneNotSupportedException {
        List<Param> res = new LinkedList<>();
        for(Param p : source)
            res.add(p.clone());
        return res;

    }*/


}
