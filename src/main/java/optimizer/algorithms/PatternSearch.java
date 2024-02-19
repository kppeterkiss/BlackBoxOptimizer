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

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Pattern Search
 *
 * Created by david on 2017. 07. 10..
 */

public class PatternSearch extends AbstractAlgorithm {
    // TODO: 31/07/17 set algorithmparams 
    InternalState is = new InternalState();

    {
        this.optimizerParams = new LinkedList<>();
        this.optimizerParams.add(new Param(1f,1000f,0,"step_size"));
        this.optimizerParams.add(new Param(10,1000,1,"max_number_of_iterations_without_stepping"));

    }

    @Override
    public void updateParameters( List<Param> parameterMap, List<IterationResult> landscape/*, List<Param> optimizerParams*/) {

        if(is.firstMove) {
            is.stepsize = (float)optimizerParams.get(0).getValue();
            is.firstMove = false;
        }
        try {
            ArrayList<Param> center = new ArrayList<>(landscape.get(landscape.size() - 1 - is.movesFromCenter).getConfigurationClone());

            if (is.movesFromCenter < 2 * parameterMap.size()) {
                center.get(is.movesFromCenter / 2).add((is.movesFromCenter % 2 == 1 ? is.stepsize : -is.stepsize));
                Param.refillList(parameterMap, center);
                ++is.movesFromCenter;
                return;
            }

            ArrayList<Param> bestConfiguration = new ArrayList<>(center);
            IterationResult bestResult = landscape.get(landscape.size() - 1 - is.movesFromCenter);
            int modifiedIndex = -1;


            for (int i = 0; i < parameterMap.size() * 2; ++i) {

                int j = landscape.size() - 1 - 2 * parameterMap.size();
                if (landscape.get(j + i).betterThan(bestResult)) {
                    bestResult = landscape.get(j + i);
                    if (modifiedIndex != -1) {
                        bestConfiguration.get(modifiedIndex).setInitValue(center.get(modifiedIndex).getValue());
                    }
                    bestConfiguration.get(i / 2).setInitValue(landscape.get(j + i).getConfigurationClone().get(i / 2).getValue());
                    modifiedIndex = i / 2;
                }
            }

            if (modifiedIndex == -1) {
                is.stepsize /= 2f;
                Param.refillList(parameterMap, center);
                is.movesFromCenter = 0;
                ++is.consecutiveSteplessIterations;
            }
            else {
                is.consecutiveSteplessIterations = 0;
                Param.refillList(parameterMap, bestConfiguration);
                is.movesFromCenter = 0;
            }

            if (is.consecutiveSteplessIterations == (int)optimizerParams.get(1).getValue())
                this.iterationCounter = this.config.getIterationCount().get();

        }
        catch(CloneNotSupportedException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void loadState(String internalStateBackupFileName) throws FileNotFoundException {
        /*if(this.config.getOptimizerStateBackupFilename()==null)
            return;
        else{
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(Param.class, new ParamDeserializer());
            gsonBuilder.registerTypeAdapter(ObjectiveContainer.Objective.class, new ObjectiveDeserializer());
            Gson gson = gsonBuilder.create();
            JsonReader reader = new JsonReader(new FileReader(this.config.getOptimizerStateBackupFilename()));
            this.optimizerParams =  gson.fromJson(reader, InternalState.class);

        }*/

    }

    @Override
    public void saveState(String internalStateBackupFileName) {
        /*if (this.config.getOptimizerStateBackupFilename() == null)
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

        }*/

    }
    @Override
    public void updateConfigFromAlgorithmParams(List<Param> algParams) {
        //nothing to do here
    }
    class InternalState {
        int consecutiveSteplessIterations = 0;
        int movesFromCenter = 0;
        float stepsize;
        boolean firstMove = true;
    }

}
