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
import com.google.gson.stream.JsonReader;
import optimizer.param.DummyParam;
import optimizer.trial.IterationResult;
import optimizer.param.Param;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;

/**
 * Grid Search
 * Supported parameter types: float, integer, boolean, enumeration
 * Parallelizable
 *
 * Search on a points of a grid. The step size in the direction of the i-th axis is stored in optimizerparams.get(i).
 * Created by david on 17/02/17.
 */
public class GridSearch extends AbstractAlgorithm {


    InternalState is = new InternalState();

    {
        this.parallelizable = ParallelExecution.PARALLEL;
        this.optimizerParams = new LinkedList<>();
        this.allowedTypes.add(Integer.class);
        this.allowedTypes.add(Boolean.class);
        this.allowedTypes.add(String.class);
    }

    @Override
    public void updateParameters(List< Param> parameterMap, List<IterationResult> landscape) {

        if(is.lastConfiguration==null) {
            is.lastConfiguration = parameterMap;
        }

            int i = is.lastConfiguration.size() - 1;
            while (i >= 0) {
                if (optimizerParams.get(i).getValue().equals("dummy")) {
                    i--;
                    continue; //we dont care about unreachable variables
                }
                if (this.config.getScriptParametersReference().get(i).isActive()) {
                    if (this.config.getScriptParametersReference().get(i).isNumeric()) {
                        //if still in range increase this and leave the others
                        if (((Number) is.lastConfiguration.get(i).getValue()).doubleValue() + ((Number) optimizerParams.get(i).getValue()).doubleValue() <=
                                ((Number) is.lastConfiguration.get(i).getUpperBound()).doubleValue()) {
                            this.config.getScriptParametersReference().get(i).add(optimizerParams.get(i).getValue());
                            break;
                        } else
                            // at the upper border of the range, we leave set it to the lower bound and let the next one to be changed ->
                            // set back to lower bound, then increase the next one.
                            // if more  is at the edge, all will be set back and increase the first that is not
                            this.config.getScriptParametersReference().get(i).setInitValue(is.lastConfiguration.get(i).getLowerBound());
                    } else if (is.lastConfiguration.get(i).isBoolean()) {
                        if ((Boolean) is.lastConfiguration.get(i).getValue()) {
                            this.config.getScriptParametersReference().get(i).setInitValue(false);
                        } else {
                            this.config.getScriptParametersReference().get(i).setInitValue(true);
                            break;
                        }
                    } else if (is.lastConfiguration.get(i).isEnumeration()) {
                        int actInd = -1;
                        // check for index of the active value
                        for (int j = 0; j < is.lastConfiguration.get(i).getActiveValueArray().length; ++j)
                            if (is.lastConfiguration.get(i).getActiveValueArray()[j].equals(is.lastConfiguration.get(i).getValue()))
                                actInd = j;
                        if (actInd + (int) optimizerParams.get(i).getValue() < is.lastConfiguration.get(i).getActiveValueArray().length) {
                            this.config.getScriptParametersReference().get(i).setInitValue(is.lastConfiguration.get(i).getActiveValueArray()[actInd + (int) optimizerParams.get(i).getValue()]);
                            break;
                        } else {
                            this.config.getScriptParametersReference().get(i).setInitValue(is.lastConfiguration.get(i).getActiveValueArray()[actInd + (int) optimizerParams.get(i).getValue() - is.lastConfiguration.get(i).getActiveValueArray().length]);
                        }
                    }
                }
                --i;
            }
        try {
            is.lastConfiguration = Param.cloneParamList(this.config.getScriptParametersReference());
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

    }



    @Override
    public void loadState(String internalStateBackupFileName) throws FileNotFoundException {
        if(this.config.getOptimizerStateBackupFilename()==null)
            return;
        else{
            Gson gson = new Gson();
            JsonReader reader = new JsonReader(new FileReader(this.config.getOptimizerStateBackupFilename()));
            this.optimizerParams =  gson.fromJson(reader, PatternSearch.InternalState.class);

        }

    }

    //nothing to save here
    @Override
    public void saveState(String internalStateBackupFileName) {}

    @Override
    public void updateConfigFromAlgorithmParams(List<Param> algParams) {
        //// TODO: 21/09/17 reconsider the solution for stepsize boundaries
        for(Param p : algParams){
            if(p.isValid()) {
                if (p.getParamTypeName().equals("Enum") || p.getParamTypeName().equals("Function"))
                    this.optimizerParams.add(new Param<Integer>(1, p.getOuterRange().getValueArray().length - 1, 1, p.getName() + "_step_size"));
                else if (p.getParamTypeName().equals("java.lang.Integer"))
                    this.optimizerParams.add(new Param<Integer>(1, (Integer) p.getOuterRange().getUpperBound() - (Integer) p.getOuterRange().getLowerBound(), 1, p.getName() + "_step_size"));
                else  if (p.getParamTypeName().equals("java.lang.Float") )
                    this.optimizerParams.add(new Param<Float>(0.0001f, ((Number) p.getOuterRange().getUpperBound()).floatValue() - ((Number) p.getOuterRange().getLowerBound()).floatValue(), 0.0001f, p.getName() + "_step_size"));
                else  if (p.getParamTypeName().equals("java.lang.Boolean") ) //only for display and placeholder
                    this.optimizerParams.add(new Param<Integer>(1, 1, 1, p.getName() + "_step_size"));
            }else
                this.optimizerParams.add(new DummyParam(p.getName()+"_step_size")); //placeholder for the alorithm, these are unreacheable params, that is that no valid range for them..

        }

    }

    class InternalState {
        List<Param> lastConfiguration;
    }
}
