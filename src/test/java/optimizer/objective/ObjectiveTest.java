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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ObjectiveTest {

    String o_int_max = " {\n" +
            "        \"relation\": \"MAXIMIZE\",\n" +
            "        \"terminator\": false,\n" +
            "        \"weight\": 100.0,\n" +
            "        \"name\": \"true_positives\",\n" +
            "        \"dummy\": 0,\n" +
            "        \"target\": 0,\n" +
            "        \"typeName\": \"java.lang.Integer\"\n" +
            "      }";

    String o_float_min = " {\n" +
            "        \"relation\": \"MINIMIZE\",\n" +
            "        \"terminator\": false,\n" +
            "        \"weight\": 40.0,\n" +
            "        \"name\": \"true_positives\",\n" +
            "        \"dummy\": 0,\n" +
            "        \"target\": 0,\n" +
            "        \"typeName\": \"java.lang.Float\"\n" +
            "      }";

    String o_float_lt = " {\n" +
            "        \"relation\": \"LESS_THAN\",\n" +
            "        \"terminator\": false,\n" +
            "        \"weight\": 40.0,\n" +
            "        \"name\": \"true_positives\",\n" +
            "        \"dummy\": 0,\n" +
            "        \"target\": 50.0,\n" +
            "        \"typeName\": \"java.lang.Float\"\n" +
            "      }";

    String o_int_gt = " {\n" +
            "        \"relation\": \"GREATER_THAN\",\n" +
            "        \"terminator\": false,\n" +
            "        \"weight\": 50,\n" +
            "        \"name\": \"true_positives\",\n" +
            "        \"dummy\": 0,\n" +
            "        \"target\": 50,\n" +
            "        \"typeName\": \"java.lang.Integer\"\n" +
            "      }";

    String o_float_maxc = " {\n" +
            "        \"relation\": \"MAXIMIZE_TO_CONVERGENCE\",\n" +
            "        \"terminator\": false,\n" +
            "        \"weight\": 50,\n" +
            "        \"name\": \"true_positives\",\n" +
            "        \"dummy\": 0,\n" +
            "        \"target\": 0.01,\n" +
            "        \"typeName\": \"java.lang.Float\"\n" +
            "      }";

    String o_int_minc = " {\n" +
            "        \"relation\": \"MINIMIZE_TO_CONVERGENCE\",\n" +
            "        \"terminator\": false,\n" +
            "        \"weight\": 50,\n" +
            "        \"name\": \"true_positives\",\n" +
            "        \"dummy\": 0,\n" +
            "        \"target\": 1,\n" +
            "        \"typeName\": \"java.lang.Integer\"\n" +
            "      }";

    public Objective deserializeObjective(String in){
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Objective.class, new ObjectiveDeserializer());
        Gson gson = gsonBuilder.create();
        return gson.fromJson(in, Objective.class);
    }

    @Test
    public void deserialize(){
        Objective<?> o = deserializeObjective(o_int_max);
        assertTrue(o.getRelation().equals(Relation.MAXIMIZE));
        assertTrue(o.getName().equals("true_positives"));
        assertTrue(o.getType().equals(Integer.class));
        assertTrue(Math.abs(o.getWeight()-100f)<0.0001f);

        Objective<?> o1 = deserializeObjective(o_float_min);
        assertTrue(o1.getRelation().equals(Relation.MINIMIZE));
        assertTrue(o1.getName().equals("true_positives"));
        assertTrue(o1.getType().equals(Float.class));
        assertTrue(Math.abs(o1.getWeight()-40f)<0.0001f);

        Objective<?> o2 = deserializeObjective(o_float_lt);
        assertTrue(o2.getRelation().equals(Relation.LESS_THAN));
        assertTrue(o2.getName().equals("true_positives"));
        assertTrue(Math.abs(((Number)o2.getTarget()).floatValue()-50f)<0.0001f);
        assertTrue(o2.getType().equals(Float.class));
        assertTrue(Math.abs(o2.getWeight()-40)<0.0001f);

        Objective<?> o3 = deserializeObjective(o_int_gt);
        assertTrue(o3.getRelation().equals(Relation.GREATER_THAN));
        assertTrue(o3.getName().equals("true_positives"));
        assertTrue(((Number)o3.getTarget()).intValue()==50);
        assertTrue(o3.getType().equals(Integer.class));
        assertTrue(Math.abs(o3.getWeight()-50f)<0.0001f);

        Objective<?> o4 = deserializeObjective(o_float_maxc);
        assertTrue(o4.getRelation().equals(Relation.MAXIMIZE_TO_CONVERGENCE));
        assertTrue(o4.getName().equals("true_positives"));
        assertTrue(Math.abs(((Number)o4.getTarget()).floatValue()-0.01f)<0.0001f);
        assertTrue(o4.getType().equals(Float.class));
        assertTrue(Math.abs(o4.getWeight()-50f)<0.0001f);

        Objective<?> o5 = deserializeObjective(o_int_minc);
        assertTrue(o5.getRelation().equals(Relation.MINIMIZE_TO_CONVERGENCE));
        assertTrue(o5.getName().equals("true_positives"));
        assertTrue(((Number)o5.getTarget()).intValue()==1);
        assertTrue(o5.getType().equals(Integer.class));
        assertTrue(Math.abs(o5.getWeight()-50f)<0.0001f);


    }
    @Test
    public void cloneTest() throws CloneNotSupportedException {
        Objective<Integer> o = deserializeObjective(o_int_max);
        o.setValue(10);
        Objective o1 = (Objective) o.clone();
        o1.setValue(11);
        assertTrue(!o1.getValue().equals(o.getValue()));


    }

    @Test
    public void equalsTest() throws CloneNotSupportedException {
        Objective<Integer> o = deserializeObjective(o_int_max);
        o.setValue(10);
        Objective o1 = (Objective) o.clone();
        o1.setValue(11);
        assertTrue(o1.equals(o));
    }

    @Test
    public void metTest(){
        Objective<Float> o = deserializeObjective(o_float_maxc);
        o.setValue(10f);
        assertTrue(!o.met());
        o.setValue(11f);
        assertTrue(!o.met());
        o.setValue(11f);
        assertTrue(o.met());


        Objective<Float> o1 = deserializeObjective(o_int_minc);
        o1.setValue(10f);
        assertTrue(!o1.met());
        o1.setValue(11f);
        assertTrue(!o1.met());
        o1.setValue(11f);
        assertTrue(o1.met());


        Objective<Float> o2 = deserializeObjective(o_float_lt);
        assertTrue(!o2.met());
        o2.setValue(51f);
        assertTrue(!o2.met());
        o2.setValue(50f);
        assertTrue(!o2.met());
        o2.setValue(49f);
        assertTrue(o2.met());


        Objective<Integer> o3 = deserializeObjective(o_int_gt);
        assertTrue(!o3.met());
        o3.setValue(51);
        assertTrue(o3.met());
        o3.setValue(50);
        assertTrue(!o3.met());
        o3.setValue(49);
        assertTrue(!o3.met());

        Objective<Integer> o4 = deserializeObjective(o_int_max);
        assertTrue(!o4.met());
        o4.setValue(51);
        assertTrue(!o4.met());
        o4.setValue(50);
        assertTrue(!o4.met());
        o4.setValue(49);
        assertTrue(!o4.met());

        Objective<Float> o5 = deserializeObjective(o_float_min);
        assertTrue(!o5.met());
        o5.setValue(51f);
        assertTrue(!o5.met());
        o5.setValue(50f);
        assertTrue(!o5.met());
        o5.setValue(49f);
        assertTrue(!o5.met());
    }


}
