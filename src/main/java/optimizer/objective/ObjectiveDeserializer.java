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

package optimizer.objective;

import com.google.gson.*;

import java.lang.reflect.Type;

/**
 * Created by peterkiss on 16/08/17.
 */
public class ObjectiveDeserializer implements JsonDeserializer<Objective>

    {

        @SuppressWarnings("unchecked")
        @Override
        public Objective deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws
            JsonParseException {
            Objective o = new Gson().fromJson(jsonElement, Objective.class);
            Objective modified = null;
            Object value,target,dummyNull =null;
            if(!o.getType().getCanonicalName().equals(o.getTypeName())) {
                if(o.getTypeName().equals(Integer.class.getCanonicalName())) {
                    value = o.getValue()!=null? ((Number)o.getValue()).intValue() :null;
                    target = o.getTarget()!= null?((Number)o.getTarget()).intValue():null;
                    dummyNull = 0;

                }
                else {
                    value = o.getValue()!=null? ((Number)o.getValue()).floatValue() :null;
                    target = o.getTarget()!= null?((Number)o.getTarget()).floatValue():null;
                    dummyNull = 0f;
                }

                return new Objective(o.relation, o.isTerminator(), o.getName(), value == null ? null : value, target == null ? target : target, dummyNull, o.getWeight());
            }
            return o;
        }


    }

