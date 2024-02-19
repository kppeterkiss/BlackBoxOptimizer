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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import optimizer.param.FunctionParam;
import optimizer.trial.IterationResult;
import optimizer.param.Param;

import javax.script.ScriptException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Simulated annealing
 * Supported parameter types: float
 *
 * Random sampling technique, motivated by physics.
 * A new point x_i is compared to x_(i-1) and is accepted with a probability
 * P(x_i | x_(i-1)) = exp(-(f(x_i)-f(x_(i-1))) / T_i), where T_i is a decreasing "temperature" parameter,
 * which permit the algorithm to escape from local optimums of the function.
 * Created by david on 2017. 07. 10..
 */


public class SimulatedAnnealing extends AbstractAlgorithm {

    InternalState is = new InternalState();

    /**
     * @param max_step_size Maximum of ||x_i - x_(i-1)||.
     * @param max_number_of_runs Number of trials of new candidates from a given center.
     * @param temperature Temperature function. It is recommended to choose a strictly decreasing positive function which converge to 0.
     */
    {
        this.optimizerParams = new LinkedList<>();
        this.optimizerParams.add(new Param(10f,10f,0.0001f,"max_step_size"));
        this.optimizerParams.add(new Param(3,1000,1,"max_number_of_runs"));
        // TODO: 09/08/17 js compatible closed formula
        try {
            this.optimizerParams.add(new FunctionParam("temperature","1/(1+$n)",100));
        } catch (ScriptException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateParameters( List<Param> parameterMap, List<IterationResult> landscape) throws CloneNotSupportedException {


        if (is.hits != 0) {
            Random gen = new Random();
            double p = gen.nextDouble();

           double c = landscape.get(landscape.size() - 1).getFitness() - (landscape.get(landscape.size() - 1 - is.hits)).getFitness();

            Float[] ti = (Float[])optimizerParams.get(2).getAllValueArray();
            c = Math.exp(-c / ti[is.centers]);
            if(p < c) {
                ++is.centers;
                is.hits = 0;
                updateParameters(parameterMap, landscape);
                return;
            }
        }


        double[] s = new double[parameterMap.size()];
        Random gen = new Random();
        for (int i = 0; i < s.length; ++i) {
            s[i] = (2 * gen.nextDouble()) - 1;
        }
        double c = ((Number) optimizerParams.get(0).getValue()).doubleValue();
        List<Param> center = landscape.get(landscape.size()-1-is.hits).getConfigurationClone();

        c = HitAndRun.adjustC(s, c, center);

        c *= gen.nextDouble();
        for (int i = 0; i < s.length; ++i) {
            center.get(i).add(new Double(c * s[i]).floatValue());
        }
        Param.refillList(parameterMap,center);
        ++is.hits;


        if(is.hits == (int)optimizerParams.get(1).getValue()) {
            this.iterationCounter = this.config.getIterationCount().get();
            return;
        }

    }


    @Override
    public void loadState(String internalStateBackupFileName) throws FileNotFoundException {
        if(this.config.getOptimizerStateBackupFilename()==null)
            return;
        else{
            Gson gson = new Gson();
            JsonReader reader = new JsonReader(new FileReader(this.config.getOptimizerStateBackupFilename()));
            this.optimizerParams =  gson.fromJson(reader, InternalState.class);

        }

    }

    @Override
    public void saveState(String internalStateBackupFileName) {
        if (this.config.getOptimizerStateBackupFilename() == null)
            return;
        else {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String s = gson.toJson(this.optimizerParams, InternalState.class);
            try{
                //PrintWriter writer = new PrintWriter(getAlgorithmSimpleName()+"_params.json", "UTF-8");
                PrintWriter writer = new PrintWriter(this.config.getOptimizerStateBackupFilename(), "UTF-8");
                writer.println(s);
                //writer.println("The second line");
                writer.close();
            } catch (IOException e) {
                // do something
            }

        }

    }
    @Override
    public void updateConfigFromAlgorithmParams(List<Param> algParams) {
        //nothing to do here
    }

    class InternalState {
        int hits = 0;
        int centers = 0;
    }
}
