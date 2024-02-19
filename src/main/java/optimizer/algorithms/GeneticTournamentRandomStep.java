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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by david on 2017. 08. 04..
 */

//optimizerParams[2] : max step size
public class GeneticTournamentRandomStep extends Genetic {

    {
        this.optimizerParams.add(new Param(10.0f,10000f,0.0001f,"max_step_size"));
    }

    @Override
    protected void select(List<IterationResult> landscape) {

        double fitnessSum = landscape.get(is.bestChromosome).getFitness();
        for(int i = (is.generation-1)*((int)optimizerParams.get(0).getValue()-1); i < is.generation*((int)optimizerParams.get(0).getValue()-1); ++i)
            fitnessSum += landscape.get(i).getFitness();
        Random rand = new Random();
        double tresholdf = fitnessSum * rand.nextDouble();
        double tresholdm = fitnessSum * rand.nextDouble();

        if(landscape.get(is.bestChromosome).getFitness() >= tresholdf)
            is.father = is.bestChromosome;
        else {
            double s = landscape.get(is.bestChromosome).getFitness();
            for (int i = (is.generation - 1) * ((int) optimizerParams.get(0).getValue() - 1); i < is.generation * ((int) optimizerParams.get(0).getValue() - 1); ++i) {
                s += landscape.get(i).getFitness();
                if (s >= tresholdf) {
                    is.father = i;
                    break;
                }
            }
        }

        if(landscape.get(is.bestChromosome).getFitness() >= tresholdm)
            is.mother = is.bestChromosome;
        else {
            double s = landscape.get(is.bestChromosome).getFitness();
            for(int i = (is.generation-1)*((int)optimizerParams.get(0).getValue()-1); i < is.generation*((int)optimizerParams.get(0).getValue()-1); ++i) {
                s += landscape.get(i).getFitness();
                if(s >= tresholdm) {
                    is.mother = i;
                    break;
                }
            }

        }



    }


    @Override
    protected List<Param> crossover(List<IterationResult> landscape, int mother, int father) throws CloneNotSupportedException {
        List<Param> result = new ArrayList<>(landscape.get(0).getConfigurationClone());

        Random rand = new Random();
        try {
            for(int i = 0; i < landscape.get(0).getConfigurationClone().size(); ++i) {
                boolean b = rand.nextBoolean();
                result.get(i).setInitValue(b ? landscape.get(father).getConfigurationClone().get(i).getValue() :
                        landscape.get(mother).getConfigurationClone().get(i).getValue());
            }

            double[] s = new double[result.size()];
            for (int i = 0; i < s.length; ++i) {
                s[i] = (2 * rand.nextDouble()) - 1;
            }
            double c = (float)optimizerParams.get(2).getValue();

            for (int i = 0; i < s.length; ++i) {
                if (s[i] > 0 && ((Number) result.get(i).getUpperBound()).doubleValue() - ((Number) result.get(i).getValue()).doubleValue() < c * s[i])
                    c = (((Number) result.get(i).getUpperBound()).doubleValue() - ((Number) result.get(i).getValue()).doubleValue()) / s[i];
                if (s[i] < 0 && ((Number) result.get(i).getValue()).doubleValue() - ((Number) result.get(i).getLowerBound()).doubleValue() < c * s[i])
                    c = (((Number) result.get(i).getValue()).doubleValue() - ((Number) result.get(i).getLowerBound()).doubleValue()) / s[i];
            }

            c *= rand.nextDouble();
            for (int i = 0; i < s.length; ++i) {
                result.get(i).add(c * s[i]);
            }

        }
        catch(CloneNotSupportedException e) {
            e.printStackTrace();
        }

        return result;
    }
}
