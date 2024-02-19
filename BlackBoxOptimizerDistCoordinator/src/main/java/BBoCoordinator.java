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

import lib.Coordinator;
import optimizer.main.Main;
import optimizer.utils.Utils;

import java.io.IOException;

public class BBoCoordinator extends Coordinator{


    public BBoCoordinator(String name) {
        super(name);
    }

    @Override
    public String getResourceRoot() {
       return Main.getPublicFolderLocation();
    }

    @Override
    public String getSourceHome() {
        return Utils.getSourceHome();
    }
    @Override
    public boolean start(String[] args) {
        try {
            Main.setComObject(this.com);
            Main.setDistributedApplicationId(this.getName());
            Main.main(args);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (CloneNotSupportedException e) {

            e.printStackTrace();
            return false;
        }

    }



    @Override
    public boolean stopNode() {
        return false;
    }

    public static void main(String[] s){

    }


    @Override
    public void run() {
        start(this.getArguments());
    }
}
