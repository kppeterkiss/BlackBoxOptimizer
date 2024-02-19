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

package optimizer.main;

import lib.Com;
import optimizer.algorithms.AbstractAlgorithm;
import optimizer.config.TestConfig;
import optimizer.exception.OptimizerException;
import optimizer.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;


/**
 * Main class of BlaBoO -  starts a standalone optimization task, or instantiate a {@link BrowserInterface} object.
 * Created by peterkiss on 14/10/16.
 *
 */


public class Main {



    static Logger logger = LogManager.getLogger(Main.class);
    private static String distributedApplicationId;


    public static Logger getLogger(){
        if(logger == null){
            new Main();
        }
        return logger;
    }

    public static void setDistributedRun(boolean d){
        Main.distributedRun[0] = d;
    }

    public static void setComObject(Com c){Main.comObj[0]=c;}

    /**
     * development path for the {@link AbstractAlgorithm} .class files
     */
    static String defaultOptimizerClassLocation =  "target"+File.separator+"classes"+File.separator;//+"optimizer"+File.separator+"algorithms"+File.separator;



    /**
     * {@link java.util.Map} storing available optimizer.algorithms, along with some predefined parametrization of them -this latter will be removed.
     */
    static Map<Class<? extends AbstractAlgorithm>,String> optimizerClasses = new HashMap<Class<? extends AbstractAlgorithm>,String>();

    // TODO: 2018. 10. 19. path hack first may be removed, test it !
    // this is for development, if we are out fromm BlaBoO project, and BlaBoO runs as jar.
    static String sourcePublicLoc = new File(".."+File.separator+"BlackBoxOptimizer"+File.separator+"src"+File.separator+"main"+File.separator+"resources"+File.separator+ "public").getAbsolutePath();

    // IDE run, resources as in maven project. This might exists if we raun the jar from another project
    static String relativeSourcePublicLoc = "src"+File.separator+"main"+File.separator+"resources"+File.separator+ "public";
    //static String relativeUserFolderLoc = "src"+File.separator+"main"+File.separator+"resources"+File.separator+ "public";

    public static String getPublicFolderLocation() {
        return publicFolderLocation;
    }

    //if  on sourcepath of BlaBoO src/main/resources/public, otherwise jar mode -> /public
    final static String publicFolderLocation = Files.exists(Paths.get(sourcePublicLoc)) && System.getProperty("user.dir").endsWith("BlackBoxOptimizer")?relativeSourcePublicLoc: "public";
    //final static String userFolderLocation = "user";//Files.exists(Paths.get(sourcePublicLoc)) && System.getProperty("user.dir").endsWith("BlackBoxOptimizer")?relativeSourcePublicLoc: "public";

    //static String s = new File("public").getAbsolutePath();
    //final static String publicFolderLocation = Files.exists(Paths.get(s))? "public":sourcePublicLoc;

    /**
     * deployment path for the {@link AbstractAlgorithm} .class files
     */
    static String defaultJarOptimizerClassLocation =  publicFolderLocation+File.separator+"lib"+File.separator;//+"optimizer"+File.separator+"algorithms"+File.separator;


    final static String outputDirName = "results";
    final static String experimentDirName = "experiments";
    final static String backupDirName = "backup";
    final static String uploadDirName = "upload";


    final static String outputDir = publicFolderLocation+File.separator+outputDirName;
    final static String experimentDir = publicFolderLocation+File.separator+experimentDirName;
    final static String backupDir = publicFolderLocation+File.separator+backupDirName;
    final static String uploadDir = publicFolderLocation+File.separator+uploadDirName;
    /*final static String outputDir = userFolderLocation+File.separator+outputDirName;
    final static String experimentDir = userFolderLocation+File.separator+experimentDirName;
    final static String backupDir = userFolderLocation+File.separator+backupDirName;
    final static String uploadDir = userFolderLocation+File.separator+uploadDirName;*/



    //commandline usage
    final static boolean  inmediateRun[] = new boolean[1];
    //distributed usage
    final static boolean  distributedRun[] = new boolean[1];
    //communication object for distributed execution
    final static Com comObj[]= new Com[1];
    //save state in each iteration
    final static boolean[] safeMode = new boolean[1];
    //use optimizer settings defined in a separated file
    final static boolean[] customParamFile= new boolean[1];
    //name of file storing optimiser params
    final static String[] customParamFileName= new String[1];
    //save in every $savingFrequence iteation
    final static Integer[] savingFrequence = new Integer[]{-1};
    //indicates whether we reloaded the config, if did so we should keep the landscape
    static boolean recoveryMode = false;
    static String[] configFileName = {publicFolderLocation+File.separator+"examples"+File.separator+"Rosenbrock"+File.separator+"Rosenbrock.json"};

    public static String getSaveFileName() {
        return saveFileName[0];
    }

    static String[] saveFileName = {null};// {"experiments/Wormhole_tuning_1.json1"};

    /**
     * Entry point of BlaBoO, either runs a a standalone optimization or starts backend of the Browser GUI. Creates the necessary folder structure.
     * @param args command line arguments for start up:
     *             "-r" indicates "run" = standalone mode
     *             "-s" switch on safe mode - takes a snapshot of the optimization by default at every 10th iteration
     *             "-sp <number of iteration>" making backups with a given frequency
     *             "<experiment JSON>" file name to load the setup of the experiment
     * @throws IOException
     * @throws CloneNotSupportedException
     */
    public static void main(String[] args) throws IOException, CloneNotSupportedException {




        getLogger().info(publicFolderLocation);
        URL u = new Main().getClass().getProtectionDomain().getCodeSource().getLocation();
        getLogger().info("Working Directory = " +
                System.getProperty("user.dir"));
        getLogger().info("CODEBASE = "+u.toString());
        if(args.length==1)
            configFileName[0] = args[0];


        boolean testmode=false;
        for(int i = 0; i< args.length;i++)
        {

            String s = args[i];
            if(s.equals("-r"))
                inmediateRun[0]=true;
            else if(s.equals("-s")) {
                safeMode[0] = true;
                savingFrequence[0]= 10;
            }
            else if(s.equals("-sp")) {
                safeMode[0] = true;
                savingFrequence[0] = Integer.parseInt(args[++i]);
            }
            else if(s.equals("-apath")) {
                // change working dir in order to enable correct execution of clack-box command
                System.setProperty("user.dir", System.getProperty("user.dir")+File.separator+"modules"+File.separator+"coordinator/");
                Main.distributedRun[0]= true;
                i++;

            }

            else {
                configFileName[0] = s;

            }

        }
        String projectDir = System.getProperty("user.dir");
        String staticDir = "";

        File directory = new File(outputDir);
        if (! directory.exists()){
            directory.mkdir();
        }
        directory = new File(experimentDir);
        if (! directory.exists()) {
            directory.mkdir();
        }
        directory = new File(backupDir);
        if (! directory.exists()){
            directory.mkdir();
        }
        directory = new File(uploadDir);
        if (! directory.exists()){
            directory.mkdir();
        }
        if(configFileName[0].contains("test"))
            testmode = true;

        final TestConfig[] config = new TestConfig[1];

        String optimizerClassPath = Utils.runningInJar()?defaultJarOptimizerClassLocation:defaultOptimizerClassLocation;


        //find all availible optimizer optimizer.algorithms
        optimizerClasses = Utils.findAllMatchingTypes(AbstractAlgorithm.class,optimizerClassPath);

        //load config from json file - this branch supposed to be the only path now
        if (configFileName[0].contains(".json")) {


            if(inmediateRun[0])
            {
                try {
                    String locationModifier = testmode?File.separator+"tests":"";

                    try {
                        config[0] = TestConfig.readConfigJSON(configFileName[0]);
                    }catch (Exception e)
                    {
                        System.out.println("Inproper json file! Use the GUI if you want to make sure make sure..");
                        return;
                    }
                    config[0].setOptimizerClasses(optimizerClasses);
                    config[0].setSavingFrequence(savingFrequence[0]);
                    config[0].setDistributedMode(Main.distributedRun[0]);
                    config[0].setCommunicationObject(Main.comObj[0]);
                    config[0].setDistributedApplicationId(Main.distributedApplicationId);

                    String experimentName = Utils.getExperimentUniqueName(configFileName[0],experimentDir+locationModifier);
                    String resFileName = Utils.getExpCSVFileName(Utils.getExperimentName(experimentName),outputDir+locationModifier);


                    config[0].runAndGetResultfiles(experimentName, resFileName,experimentDir+locationModifier, backupDir+locationModifier);

                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (OptimizerException e) {
                    System.out.println(e.getMessage());
                }
                return;
            }
            else {
                saveFileName[0] = Utils.getExperimentUniqueName(configFileName[0],experimentDir);
                BrowserInterface bi = new BrowserInterface(configFileName[0], optimizerClasses, projectDir, staticDir, experimentDir, outputDir, backupDir,uploadDir,saveFileName[0],Main.distributedRun[0],Main.comObj[0],experimentDirName,outputDirName,backupDirName,uploadDirName,publicFolderLocation);
                bi.run();
            }

        }

    }

    public static String getDistributedApplicationId() {
        return distributedApplicationId;
    }

    public static void setDistributedApplicationId(String distributedApplicationId) {
        Main.distributedApplicationId = distributedApplicationId;
    }
}
