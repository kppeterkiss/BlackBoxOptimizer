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

import org.junit.Test;

import javax.script.ScriptException;

import static  org.junit.Assert.assertTrue;

public class ParamDependencyTest {
    @Test
    public void complyTestEnum() {
        Param enump = new Param("1", new String[]{"1","2","3"},"e");
        ParameterDependency pd  = new ParameterDependency(null, null,enump,"1","2");
        assertTrue(pd.comply());

        enump.setInitValue("3");
        assertTrue(!pd.comply());

    }

    @Test
    public void complyTestFloat() {
        Param floatp = new Param(1f, 10f,0f,"f");
        ParameterDependency pd  = new ParameterDependency(null, null,floatp,1f,2f);
        assertTrue(pd.comply());

        floatp.setInitValue(3f);
        assertTrue(!pd.comply());

    }

    @Test
    public void complyTestInt() {
        Param intp = new Param(1, 10,0,"i");
        ParameterDependency pd  = new ParameterDependency(null, null,intp,1,2);
        assertTrue(pd.comply());

        intp.setInitValue(3);
        assertTrue(!pd.comply());

    }


    @Test
    public void complyTestBoolean() {
        Param booleanp = new Param(false, true,false,"b");
        ParameterDependency pd  = new ParameterDependency(null, null,booleanp,false,false);
        assertTrue(pd.comply());

        booleanp.setInitValue(true);
        assertTrue(!pd.comply());

    }

    @Test
    public void complyTestFunction() throws ScriptException {
        Param funcp = new FunctionParam("f", "2*$x",10);
        funcp.setInitValue(2f);
        ParameterDependency pd  = new ParameterDependency(null, null,funcp,2f,10f);
        assertTrue(pd.comply());

        funcp.setInitValue(12f);
        assertTrue(!pd.comply());

    }

}
