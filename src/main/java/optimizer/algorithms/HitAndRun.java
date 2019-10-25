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
import optimizer.trial.IterationResult;
import optimizer.param.Param;
import optimizer.utils.Utils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by david on 2017. 07. 10..
 */

public class HitAndRun extends AbstractAlgorithm {

    InternalState is = new InternalState();

    {
        this.optimizerParams = new LinkedList<>();
        this.optimizerParams.add(new Param(1.f, Utils.FLOAT_REDEFINED_MAX_VALUE,Float.MIN_VALUE,"max_step_size"));
        this.optimizerParams.add(new Param(5,Integer.MAX_VALUE,Integer.MIN_VALUE,"max_number_of_runs"));
    }

    /**
     * 1. Select a starting point Xo from S and set i =O.
     * 2. Generate a random direction di+l uniformly distributed over a direction set D ~ Rn. Find the line set L = S intersetion with {x|x =Xi + lambda di +l , lambda a real scalar} and generate a random point Xi+l uniformly distributed over L.
     * 3. If i=N,stop. Otherwise,set i=i+1 and return to 2.
     * @param parameterMap contains the description of the {@link Param Param} values their ranges {@link Param#getDependencies()} and their dependencies. The system expects to change the parametervalues here using set
     * @param landscape readonly, contains the history of executions of the algorithm to be optimized
     */
    @Override
    public void updateParameters( List<Param> parameterMap, List<IterationResult> landscape) {

        try {
            //last setup is better than
            if (is.hits != 0 && landscape.get(landscape.size() - 1).betterThan(landscape.get(landscape.size() - 1 - is.hits))) {
                is.hits = 0;
                updateParameters(parameterMap, landscape);
                return;
            }
            else {
                if(is.hits == (int)optimizerParams.get(1).getValue()) {
                    //????
                    this.iterationCounter = this.config.getIterationCount().get();
                    return;
                }
                // s vector of direction where to move
                int dimension = parameterMap.size();

                double[] s = getRandomVector(dimension);
                // c will be the stepsize
                double c = ((Float) optimizerParams.get(0).getValue()).doubleValue();
                // pick the center point in searchspace
                List<Param> center = landscape.get(landscape.size()-1-is.hits).getConfigurationClone();

                c = adjustC(s, c, center);
                moveVector(s, c, center);
                Param.refillList(parameterMap,center);

                ++is.hits;
            }

        }
        catch(CloneNotSupportedException e) {
            e.printStackTrace();
        }

    }

    /**
     * shifts the vector in a given direction on a specified scale
     * @param s direction of the shift
     * @param c maximal shift
     * @param center the vector to shift
     */
    private void moveVector(double[] s, double c, List<Param> center) {
        Random gen = new Random();
        c *= gen.nextDouble();
        for (int i = 0; i < s.length; ++i) {
            center.get(i).add(new Double(c * s[i]).floatValue());
        }
    }

    /**
     * Generates a vector of the given length in unit cube.
     * @param dimension dimension of the vector to be generated.
     * @return generated vector
     */
    private double[] getRandomVector(int dimension) {
        double[] s = new double[dimension];
        Random gen = new Random();
        for (int i = 0; i < s.length; ++i) {
            s[i] = (2 * gen.nextDouble()) - 1;
        }
        return s;
    }

    //wis this ok???

    /**
     * C = stepsize. Starting from the maximal stepsize, iterating over the coordinates of the vector of move.
     * If the move would bring out from the searchspace, we scale it down to the extent it would only bring us to the edge of the space.
     * @param s direction vector
     * @param c the maximal step size
     * @param center actual position in the space
     * @return the downscaled stepsize, taht ensures that after the update we stay within the search space
     */
    static double adjustC(double[] s, double c, List<Param> center) {
        for (int i = 0; i < s.length; ++i) {
            //todo c always positive ???
            double distanceFromUpperEdge = ((Number) center.get(i).getUpperBound()).doubleValue() - ((Number) center.get(i).getValue()).doubleValue();
            if (s[i] > 0 && distanceFromUpperEdge < c * s[i])
                c = (distanceFromUpperEdge) / s[i];
            double distanceFromLowerEdge = ((Number) center.get(i).getValue()).doubleValue() - ((Number) center.get(i).getLowerBound()).doubleValue();
            //this case s[i] < 0
            if (s[i] < 0 && distanceFromLowerEdge < -(c * s[i]))
                c = (distanceFromLowerEdge) / -s[i];
        }
        return c;
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
    }
}
