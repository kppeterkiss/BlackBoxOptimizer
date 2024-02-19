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

package optimizer.exception;

import optimizer.param.FunctionParam;
import optimizer.param.Param;
import optimizer.param.ParameterDependency;
import org.junit.Test;

import javax.script.ScriptException;

import static org.junit.Assert.assertTrue;

public class InvalidParameterValueExceptionTest {

    @Test(expected = InvalidParameterValueException.class)
    public void addDependencyWithInvalidBoundary1(){

            Param enump = new Param("1", new String[]{"1","2","3"},"e");
            ParameterDependency pd  = new ParameterDependency(null, null,enump,"0","2");

    }

    @Test(expected = InvalidParameterValueException.class)
    public void addDependencyWithInvalidBoundary2(){

        Param enump = new Param("1", new String[]{"1","2","3"},"e");
        ParameterDependency pd  = new ParameterDependency(null, null,enump,"1","15");

    }

    @Test(expected = InvalidParameterValueException.class)
    public void addDependencyWithInvalidBoundary3() throws ScriptException {
        Param funcp = new FunctionParam("f", "2*$x", 10);
        funcp.setInitValue(2f);
        ParameterDependency pd = new ParameterDependency(null, null, funcp, 1f, 5f);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void setWrongInitValueException() throws ScriptException {
        Param funcp = new FunctionParam("f", "2*$x", 10);
        funcp.setInitValue(1f);
    }


}
