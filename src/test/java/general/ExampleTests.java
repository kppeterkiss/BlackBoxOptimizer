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
import optimizer.main.Main;
import org.junit.Test;
import optimizer.exception.OptimizerException;
import optimizer.param.Param;
import optimizer.config.TestConfig;
import optimizer.utils.Utils;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Created by peterkiss on 2018. 01. 22..
 *
 */
/**
 * This class belongs to  stresstests, that tries to run all the example projects. Those projects first requires to have a range of different execution environments,
 * some of them will mopst probably fail at building the application, and second running them takes a lot of time. Therefore by default these test are unabled. If you want to run them,
 * switch  "StressTestBase.runStressTests" true.
 */
public class ExampleTests extends StressTestBase {

    //testing all the example setups with all  applicable optimizer.algorithms
    @Test
    public void runAll1() throws IOException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, OptimizerException, CloneNotSupportedException {
        if(!runStressTests)
            return;
        optimizerClasses = Utils.findAllMatchingTypes(AbstractAlgorithm.class,Files.exists(Paths.get(defaultJarOptimizerClassLocation))?defaultJarOptimizerClassLocation:defaultOptimizerClassLocation);
        File[] files = new File("Examples/").listFiles();
        testFiles(files);
    }

    // helper method:
    public  void testFiles(File[] files) throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, OptimizerException, CloneNotSupportedException {
        int counter = 0;
        for (File file : files) {
            if (file.isDirectory()) {
                System.out.println("Directory: " + file.getName());
                testFiles(file.listFiles()); // Calls same method again.
            } else {
                if(file.getName().endsWith(".json")) {
                    String tasknameparts[] = file.getName().replace(".json","").split(optimizer.utils.Utils.platform_path_separator);
                    String tn = tasknameparts[tasknameparts.length-1];
                    TestConfig config = TestConfig.readConfigJSON(file);
                    config.setOptimizerClasses(optimizerClasses);
                    Map<String, List<Param>> algParamMap = config.filterAlgorithms();
                    for(String name: algParamMap.keySet() ) {
                        Class<? extends AbstractAlgorithm> c = config.getOptimizerClassBySimpleName(name);

                        System.out.println("============================");
                        System.out.println(file.getName()+" ==> "+c.getSimpleName());
                        System.out.println("============================");

                        config.setAlgorithmName(c.getSimpleName());
                        config.setOptimizerParameters(algParamMap.get(name));
                        config.setIterationCount( Optional.of(10) );
                        config.setIterationCounter( 0);
                        config.clearLandscape();

                        File f = new File(testResultsPath+"/"+tn + "_"+c.getSimpleName()+".csv");
                        BufferedWriter writer = new BufferedWriter(new FileWriter(f));

                        writer.write(config.runOptimizer( testExpPath,testBackupPath,testExpPath+"/"+tn + "_"+c.getSimpleName()+".json"));
                        writer.close();
                        String fn = testExpPath+"/"+tn + "_"+c.getSimpleName()+".json";
                        config.wirteExperimentDescriptionFile(fn);


                        counter++;
                    }
                }



            }
        }
        System.out.println("Tests runned:  "+counter);
    }

    @Test
    public void complicatedTest() throws IOException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, OptimizerException, CloneNotSupportedException {
        if(!runStressTests)
            return;

        optimizerClasses = Utils.findAllMatchingTypes(AbstractAlgorithm.class,Files.exists(Paths.get(defaultJarOptimizerClassLocation))?defaultJarOptimizerClassLocation:defaultOptimizerClassLocation);
        File[] files = new File(testResourcesPath+"/Test/").listFiles();
        testFiles(files);
    }

    //test command_line usage for all setup generated in runAll1
    @Test
    public void runAll2(){
        if(!runStressTests)
            return;
        File[] files = new File(testExpPath+"/").listFiles();
        testClFiles(files);

    }

    @Test
    public void runAll3(){
        if(!runStressTests)
            return;
        File[] files = new File(testBackupPath+"/").listFiles();
        testRecovery(files);

    }

    private void testRecovery(File[] files) {

           for(File file : files) {
               System.out.println("============================");
               System.out.println(file.getName() + " ==> recovery");
               System.out.println("============================");


               String[] args = new String[]{"-r", "-sp", "3", file.getAbsolutePath()};
               executeCL(args);
               try {
                   // not working because bad configurations are not written in csv
                   // assertTrue(TestConfig.readConfigJSON(file.getAbsolutePath().replace(testBackupPath, testExpPath)).getIterationCount().get()<=Files.lines(Paths.get(file.getAbsolutePath().replace(testBackupPath, testResultsPath).replace(".json", ".csv"))).count()-1);
                   assertTrue(TestConfig.readConfigJSON(file.getAbsolutePath().replace(testBackupPath, testExpPath)).getIterationCounter() <= TestConfig.readConfigJSON(file.getAbsolutePath().replace(testBackupPath, testExpPath)).getIterationCount().get());
               } catch (IOException e) {
                   fail(e.getMessage());
               }
           }

    }

    //helper method for runall2
    private void testClFiles(File[] files) {
        Arrays.stream(files).filter(file -> file.getName().contains(".json")&&!file.getName().contains("_cl.json")).forEach(file -> {
            File directory = new File(testResultsPath+"/"+ file.getName().replace(".json",""));
            if (! directory.exists()){
                directory.mkdir();

            }
            System.out.println("============================");
            System.out.println(file.getName());
            System.out.println("============================");
            String clFileName = file.getAbsolutePath().replace(".json","_cl.json");
            try {
                TestConfig tc = TestConfig.readConfigJSON(file.getAbsolutePath());
                tc.setIterationCounter(0);
                tc.wirteExperimentDescriptionFile(clFileName);
            } catch (FileNotFoundException e) {
                fail(e.getMessage());
            }


            String[] args = new String[]{"-r","-sp","3",testExpPath+"/"+ new File(clFileName).getName()};
            executeCL(args);

        });


    }

    private void executeCL(String[] args) {
        try {
            cllogFileW.write(String.join(" ",args));
            Main.main(args);
        } catch (IOException e) {
            fail(e.getMessage());
        } catch (CloneNotSupportedException e) {
            fail(e.getMessage());
        }
    }


}
