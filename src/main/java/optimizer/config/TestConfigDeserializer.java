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

package optimizer.config;

import com.google.gson.*;
import optimizer.objective.Objective;
import optimizer.objective.ObjectiveDeserializer;
import optimizer.param.*;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Created by peterkiss on 2018. 01. 24..
 */
public class TestConfigDeserializer  implements JsonDeserializer<TestConfig>

{
    // TODO: 16/08/17 overflow????
    //convert to float if target and value fits in its range - maybe should not use easy to go out...
    @SuppressWarnings("unchecked")
    @Override
    public TestConfig deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        System.out.println("Call deserialize");
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Range.class,new RangeDeserializer());
        gsonBuilder.registerTypeAdapter(Param.class, new ParamDeserializer());
        gsonBuilder.registerTypeAdapter(Objective.class, new ObjectiveDeserializer());
        Gson gson = gsonBuilder.create();
        TestConfig t = gson.fromJson(jsonElement, TestConfig.class);

        //TestConfig modified = null;
        List<Param> pl = t.getScriptParametersReference();
        if(t.getOptimizerParameters() == null )
            return t;
        List<Param> op = t.getOptimizerParameters();
       for(Param p : op) {
           for (Object pdo : p.getDependencies()) {
               ParameterDependency pd = (ParameterDependency)pdo;
               if(pd.getP()!=null)
                   for(Param p1: t.getOptimizerParameters())
                       if(p1.equals(pd.getP()))
                           pd.setP(p1);
           }
       }
        return t;

    }

}