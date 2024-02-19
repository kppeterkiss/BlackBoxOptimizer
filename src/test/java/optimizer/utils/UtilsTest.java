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

package optimizer.utils;

import optimizer.objective.Relation;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UtilsTest {

    @Test
    public void complyTest(){
        assertTrue(Utils.comply(Relation.GREATER_THAN,15,10,14));
        assertFalse(Utils.comply(Relation.LESS_THAN,15,10,14));
        assertTrue(Utils.comply(Relation.MAXIMIZE_TO_CONVERGENCE,15,2,14));
        assertFalse(Utils.comply(Relation.MAXIMIZE_TO_CONVERGENCE,17,2,14));

        assertTrue(Utils.comply(Relation.GREATER_THAN,15f,10f,14f));
        assertFalse(Utils.comply(Relation.LESS_THAN,15f,10f,14f));
        assertTrue(Utils.comply(Relation.MAXIMIZE_TO_CONVERGENCE,15f,2f,14f));
        assertFalse(Utils.comply(Relation.MAXIMIZE_TO_CONVERGENCE,17f,2f,14f));

        assertFalse(Utils.comply(Relation.MAXIMIZE,15f,10f,14f));
        assertFalse(Utils.comply(Relation.MAXIMIZE,15f,10f,14f));
        assertFalse(Utils.comply(Relation.MINIMIZE,15f,2f,14f));
        assertFalse(Utils.comply(Relation.MINIMIZE,17f,2f,14f));    
    }
}
