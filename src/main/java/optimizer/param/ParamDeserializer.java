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
import java.util.LinkedList;
import java.util.List;

/**
 * Created by peterkiss on 16/08/17.
 */
public class ParamDeserializer implements JsonDeserializer<Param> {
    //no handler for array based ranges, they are  string based now
    @SuppressWarnings("unchecked")
    @Override
    public Param deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        if(jsonElement.toString().contains("functionString")) {
            FunctionParam fp = new Gson().fromJson(jsonElement, FunctionParam.class);
            // TODO: 2018. 02. 12.  this is a quick fix, should implement decent deserialization for Ranges
            List<ParameterDependency> fixedDeps = new LinkedList<ParameterDependency>();
            for(ParameterDependency pd :fp.getDependencies()){
                Float[] floatArray = new Float[pd.getRangeOfThis().getValueArray().length];
                for (int i = 0 ; i < pd.getRangeOfThis().getValueArray().length; i++)
                {
                    floatArray[i] = ((Number) pd.getRangeOfThis().getValueArray()[i]).floatValue();
                }
                ParameterDependency newpd = new ParameterDependency(floatArray);
                newpd.setP(pd.getP());
                newpd.setRangeOfOther(pd.getRangeOfOther());
                fixedDeps.add(newpd);
            }
            fp.setDependencies(fixedDeps);
            return fp;
        }

        Param p = new Gson().fromJson(jsonElement,Param.class);
        System.out.println("Call Param des");

        Param modified = p;
             if(p.getValueTypeName()!=null&&!p.getValueTypeName().equals(p.getParamGenericTypeName())) {
                if(p.getValueTypeName().equals(Integer.class.getCanonicalName()))
                    modified = new Param(((Number) p.getValue()).intValue(), ((Number) p.getUpperBound()).intValue(), ((Number) p.getLowerBound()).intValue(), p.getName());
                else
                    modified = new Param(((Number) p.getValue()).floatValue(), ((Number) p.getUpperBound()).floatValue(), ((Number) p.getLowerBound()).floatValue(), p.getName());
                modified.dependencies = new LinkedList();
                for(Object pdo : p.getDependencies()){
                    ParameterDependency pd = (ParameterDependency)pdo;
                    ParameterDependency newPd = pd;
                    if(p.getValueTypeName().equals(Integer.class.getCanonicalName()))
                        newPd = new ParameterDependency(((Number)pd.getRangeOfThis().getLowerBound()).intValue(),((Number)pd.getRangeOfThis().getUpperBound()).intValue(),pd.getP(),pd.getP()==null?null:pd.getRangeOfOther().getLowerBound(),pd.getP()==null?null:pd.getRangeOfOther().getUpperBound());
                    else
                        newPd = new ParameterDependency(((Number)pd.getRangeOfThis().getLowerBound()).floatValue(),((Number)pd.getRangeOfThis().getUpperBound()).floatValue(),pd.getP(),pd.getP()==null?null:pd.getRangeOfOther().getLowerBound(),pd.getP()==null?null:pd.getRangeOfOther().getUpperBound());
                    modified.getDependencies().add(newPd);
                }

            }

        return modified;
    }
    public boolean paramValuesFitInFloat(Param p){
        for(Object pd: p.getDependencies()){
            double d = (Double)(((ParameterDependency)pd).getRangeOfThis().getUpperBound());
            if((double)(float)d != d)
                return false;
            d = (Double)(((ParameterDependency)pd).getRangeOfThis().getUpperBound());
            if((double)(float)d != d)
                return false;
        }
        return true;
    }
}
