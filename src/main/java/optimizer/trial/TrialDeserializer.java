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

package optimizer.trial;

import com.google.gson.*;
import optimizer.config.TestConfig;
import optimizer.objective.Objective;
import optimizer.objective.ObjectiveDeserializer;
import optimizer.param.Param;
import optimizer.param.ParamDeserializer;
import optimizer.param.Range;
import optimizer.param.RangeDeserializer;

import java.lang.reflect.Type;

public class TrialDeserializer implements JsonDeserializer<Trial>{

    @SuppressWarnings("unchecked")
    @Override
    public Trial deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        GsonBuilder gsonBuilder = new GsonBuilder();

        gsonBuilder.registerTypeAdapter(Range.class, new RangeDeserializer());
        gsonBuilder.registerTypeAdapter(Param.class, new ParamDeserializer());
        gsonBuilder.registerTypeAdapter(Objective.class, new ObjectiveDeserializer());
        Gson gson = gsonBuilder.create();
        Trial t = gson.fromJson(jsonElement, Trial.class);

        return t;
    }
}
