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

package optimizer.param;

import com.google.gson.*;

import java.lang.reflect.Type;

public class RangeDeserializer implements JsonDeserializer<Range> {
    //no handler for array based ranges, they are  string based now
    @SuppressWarnings("unchecked")
    @Override
    public Range deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
       /* if(jsonElement.toString().contains("functionString")) {
            return new Gson().fromJson(jsonElement, FunctionParam.class);
        }*/
        System.out.println("Call Range des");
        Range p = new Gson().fromJson(jsonElement, Range.class);
        if (p.getValueArray() != null) {
               if(p.getValueArray()[0] instanceof Double){
                   Float[] floatArray = new Float[p.getValueArray().length];
                   for (int i = 0 ; i < p.getValueArray().length; i++)
                   {
                       floatArray[i] = ((Number) p.getValueArray()[i]).floatValue();
                   }
                   return new Range(floatArray);
               }

        }
        return p;
    }
}
