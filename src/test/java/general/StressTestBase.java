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

package general;

import optimizer.algorithms.AbstractAlgorithm;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is the base class all the stresstests, that tries to run all the example projects. Those projects first requires to have a range of different execution environments,
 * some of them will mopst probably fail at building the application, and second running them takes a lot of time. Therefore by default these test are unabled. If you want to run them,
 * switch  "StressTestBase.runStressTests" true.
 */
public class StressTestBase {

    static boolean runStressTests = false;

    final static String[] setupsToTest={
            "Rosenbrock_DifferentialEvolution_cl.json",
            "SVM_python_GridSearch_cl.json",
            "Rosenbrock_multi.json",
            "SVM_python.json",
            "Wormhole_tuning.json"
    };
    //available optimizer.algorithms
    // development path
    final static String defaultOptimizerClassLocation =  "target/classes/optimizer/algorithms/";

    final static String outputfile =  "BlackBoxOptimizer/target/classes/optimizer/algorithms/";

    //jar path
    final static String defaultJarOptimizerClassLocation =  "lib/algorithms/";


    //path for result files
    static String resultsPath = "results";
    //path for experiment descriptors
    static String expPath = "experiments";
    //path for backup files
    static String backupPath = "backup";

    //path for test result files
    static String testResultsPath = "results/tests";
    //path for test experiment descriptors
    static String testExpPath = "experiments/tests";
    //path for test backup files
    static String testBackupPath = "backup/tests";
    //path for hardcore setup files
    static String testResourcesPath = "src/test/resources/";

    static FileWriter cllogFileW;


    static Map<Class<? extends AbstractAlgorithm>,String> optimizerClasses = new HashMap<Class<? extends AbstractAlgorithm>,String>();
    @BeforeClass
    public static void setup() throws IOException {

        cllogFileW = new FileWriter(new File("Cllog.log"));
        File directory = new File(resultsPath);
        if (! directory.exists()){
            directory.mkdir();

        }
        directory = new File(expPath);
        if (! directory.exists()){
            directory.mkdir();

        }
        directory = new File(backupPath);
        if (! directory.exists()){
            directory.mkdir();

        }

        directory = new File(testResultsPath);
        if (! directory.exists()){
            directory.mkdir();

        }
        directory = new File(testExpPath);
        if (! directory.exists()){
            directory.mkdir();

        }
        directory = new File(testBackupPath);
        if (! directory.exists()){
            directory.mkdir();

        }

    }
    @AfterClass
    public static void cleanup() throws IOException {
        cllogFileW.close();
        deleteDirectory(new File(testBackupPath));
        deleteDirectory(new File(testResultsPath));
        deleteDirectory(new File(testExpPath));
    }
    static boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }
}
