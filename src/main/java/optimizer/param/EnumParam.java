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

/**
 * Created by peterkiss on 14/05/17.
 */
public class EnumParam extends Param<String> {

    public EnumParam(String value, String upper, String lower, String name) {
        super(value, upper, lower, name);
    }

    public EnumParam(String value, String[] values, String name) {
        super(value, values, name);
    }

    @Override
    public String getParamTypeName(){
        return "Enum";
    }

}
