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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by david on 2017. 08. 12..
 */
public class DifferentialEvolutionParallel extends GeneticParallel {


    {
        this.is = new InternalState();
        this.optimizerParams = new LinkedList<>();
        this.optimizerParams.add(new Param(5,Integer.MAX_VALUE,Integer.MIN_VALUE,"population_size"));
        this.optimizerParams.add(new Param(0.5, Utils.FLOAT_REDEFINED_MAX_VALUE,Float.MIN_VALUE,"phi"));
        this.optimizerParams.add(new Param(0.5,Utils.FLOAT_REDEFINED_MAX_VALUE,Float.MIN_VALUE,"kappa"));
    }

    @Override
    protected List<List<Param>> mutatePopulation(List<List<Param>> chromosomes) throws CloneNotSupportedException {
        List<List<Param>> mutated = new LinkedList<>();
        for(int i=0;i<chromosomes.size();++i){
            mutated.add(mutateIndividual(i,chromosomes));

        }
        return mutated;



        //super.mutatePopulation(chromosomes);
    }
    @Override
    protected List<List<Param>>  crossoverOnPopulation(List<List<Param>> chromosomes) throws CloneNotSupportedException {
        //no need here, done at mutate stage
        return chromosomes;
    }


    @Override
    protected List<List<Param>> selectPopulation(List<IterationResult> landscape) throws CloneNotSupportedException {

        if(((InternalState)is).members== null) {
            ((InternalState)is).members = new LinkedList<>();
            for (int i = 0; i < is.populationSize; ++i) {
                ((InternalState)is).members.add(((InternalState)is).newmembers.get(i));

            }
        }
        else {

            for (int i = 0; i < is.populationSize; ++i) {
                ((InternalState)is).members.set(i,  ((InternalState)is).newmembers.get(i).betterThan(((InternalState)is).members.get(i)) ?
                        ((InternalState)is).newmembers.get(i) : ((InternalState)is).members.get(i));

            }
        }
        List<List<Param>> res = new LinkedList<>();
        for(IterationResult ir: ((InternalState)is).members)
            res.add(ir.getConfigurationClone());
        return res;
    }

    @Override
    protected void select(List<IterationResult> landscape) {

    }

    @Override
    protected List<Param> crossover(List<IterationResult> landscape, int mother, int father) throws CloneNotSupportedException {
        return null;
    }
    @Override
    public void setResults(List<IterationResult> results) throws CloneNotSupportedException {
        ((InternalState)is).newmembers=results;
    }


    private List<Param> mutateIndividual(int i, List<List<Param>> setups ) throws CloneNotSupportedException {
        Random rand = new Random();
        float kappa = ((Number) optimizerParams.get(2).getValue()).floatValue();
        float phi = ((Number) optimizerParams.get(1).getValue()).floatValue();

        int b=i,c=i;
        while (b==i)
            b = rand.nextInt(setups.size()-1);
        while(c==i)
            c = rand.nextInt(setups.size()-1);

        List<Param> xm = Param.cloneParamList(setups.get(i));
        for(int j = 0; j < xm.size(); ++j) {
            float r = rand.nextFloat();
            if(r >= kappa)
                continue;
            float b_i = ((Number) setups.get(b).get(j).getValue()).floatValue();
            float c_i = ((Number)  setups.get(c).get(j).getValue()).floatValue();
            xm.get(j).add((b_i -c_i)* phi);
        }
        for(Param p : xm){
            if((float)p.getValue()>(float)p.getUpperBound())
                p.setInitValue(p.getUpperBound());
            else if((float)p.getValue()<(float)p.getLowerBound())
                p.setInitValue(p.getLowerBound());
        }
       return xm;
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



    class InternalState extends GeneticParallel.InternalState {
        //common


        List<IterationResult> members;
        List<IterationResult> newmembers;

    }
}
