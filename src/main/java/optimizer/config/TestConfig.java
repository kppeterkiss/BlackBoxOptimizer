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

package optimizer.config;

import lib.Com;
import optimizer.algorithms.AbstractAlgorithm;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import optimizer.exception.JSONReadException;
import optimizer.objective.ObjectiveDeserializer;
import optimizer.objective.Relation;
import optimizer.param.*;
import optimizer.trial.IterationResult;
import optimizer.exception.OptimizerException;
import optimizer.utils.Utils;
import optimizer.objective.Objective;
import optimizer.objective.ObjectiveContainer;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * This class represents the entire optimization task, that can be either build using the GUI, or manually following samples
 * Created by peterkiss on 14/10/16.
 */
public class TestConfig {
    /**variable describing wether the execution is in a distributed fashion */
    private boolean distribution;

    // TODO: 2018. 10. 09. not the most clear location for these
    transient private Com communicationObject;

    public String getPublicFolderLocation() {
        return publicFolderLocation;
    }

    public void setPublicFolderLocation(String publicFolderLocation) {
        this.publicFolderLocation = publicFolderLocation;
    }

    public String getDistributedApplicationId() {
        return distributedApplicationId;
    }

    public void setDistributedApplicationId(String distributedApplicationId) {
        this.distributedApplicationId = distributedApplicationId;
    }

    private String distributedApplicationId;

    private String  publicFolderLocation;

    /**input from html or config file, to be modified by new parameter values, should not be modified*/
    private String baseCommand;

    /**path to outputfile of the BB function this feature is intended to be removed*/

    private String objectiveFileName;

    /**wsimple name of the class implementing AbstractAlgorithm*/
    private String algorithmName;

    /**for saving the actual state of optimization task we save what iteration we are in*/
    private int iterationCounter;

    /**if we have to save the optimizer algorithm's inner state we do that in this file - will be removed*/
    private String optimizerStateBackupFilename;

    /**stores the optimizer algorithm's parameter-setup - will be removed*/
    private String optimizerConfigFilename;

    /**we store the landscape here in order to be able to reload the last state in case of interruption*/
    private  List<IterationResult> landscape = new LinkedList<>();


    /**  Stores recent state of parameterspace.*/
    private List<Param> optimizerParameters;

    /**  Stores optional limit for iterations */
    private Optional<Integer> iterationCount;

    /**we store the landscape here in order to be able to reload the last state in case of interruption*/
    private Integer savingFrequence = -1;

    /**we store the landscape here in order to be able to reload the last state in case of interruption*/
    private Map<Class<? extends AbstractAlgorithm>,String> optimizerClasses;

    /**we store the landscape here in order to be able to reload the last state in case of interruption*/
    //name of file storing optimiser params
    private String  customParamFileName;





    final static Logger logger = LogManager.getLogger(TestConfig.class);

    public void setCommunicationObject(Com  c){this.communicationObject= c;}
    public void setDistributedMode(boolean b){this.distribution = b;}
    public void setBaseCommand(String baseCommand) {
        this.baseCommand = baseCommand;
    }
    public void setObjectiveFileName(String objectiveFileName) {
        this.objectiveFileName = objectiveFileName;
    }
    public void setScriptParameters(List<Param> scriptParameters) {
        this.scriptParameters = scriptParameters;
    }
    public void setIterationCount(Optional<Integer> iterationCount) {        this.iterationCount = iterationCount;    }
    public void setAlgorithmName(String algorithmName) {        this.algorithmName = algorithmName;    }
    public void setLandscape(List<IterationResult> landscape) {        this.landscape = landscape;    }
    public synchronized void  setIterationCounter(int iterationCounter) {        this.iterationCounter = iterationCounter;    }
    public void setOptimizerStateBackupFilename(String optimizerStateBackupFilename) {        this.optimizerStateBackupFilename = optimizerStateBackupFilename;    }
    public void setOptimizerClasses(Map<Class<? extends AbstractAlgorithm>, String> optimizerClasses) {        this.optimizerClasses = optimizerClasses;    }
    public void setSavingFrequence(Integer savingFrequence) {
        this.savingFrequence = savingFrequence;
    }
    @Deprecated
    public void setOptimizerConfigFilename(String optimizerConfigFilename) {        this.optimizerConfigFilename = optimizerConfigFilename;    }
    @Deprecated
    public void setCustomParamFileName(String customParamFileName) {
        this.customParamFileName = customParamFileName;
    }

    public boolean getDistributedMode(){ return this.distribution;}
    public Com<?,?> getCommunicationObject(){return this.communicationObject;}

    public String getAlgorithmName() {        return algorithmName;    }
    public String getBaseCommand() { return baseCommand; }
    public String getObjectiveFileName() { return objectiveFileName; }
    public synchronized Optional<Integer> getIterationCount() {return iterationCount;}
    public List< Param> getScriptParametersReference() {return scriptParameters;}
    public List< Param> getOptimizerParameters() {return optimizerParameters; }

    public List<IterationResult> getLandscapeReference() { return landscape; }
    public ObjectiveContainer getObjectiveContainerReference() {        return objectiveContainer;    }
    public String getOptimizerConfigFilename() {       return optimizerConfigFilename;    }
    public synchronized int getIterationCounter() {        return iterationCounter;    }
    public String getOptimizerStateBackupFilename() {        return optimizerStateBackupFilename;    }

    public Integer getSavingFrequence() {        return savingFrequence;    }

    /**
     * Resets to {@link #landscape} to an empty list to restart optimization.
     */
    public void clearLandscape(){this.landscape= new LinkedList<>();}

    /**
     * Assembles the commans to be executed to run the black box function. That is it insterts tha actual parameter values into the terminal command that will be called.
     * @return command to be executed in the terminal.
     */
    public String getCommand()
    {
        String command = "";
        List<String> s =new LinkedList<>();
        s.add(null);
        Scanner sc = new Scanner(this.getBaseCommand());
        command = replaceVariablesWithValues(scriptParameters, command, s, sc);
        return  command;
    }

    /**
     *
     * Static version of {@link #getCommand()}, expects the base string representing the terminal command, and a setup to be applied.
     * @return command to be executed in the terminal.
     */
    public static synchronized String getCommand(List<Param> scriptParameters,String basecommand)
    {
        String command = "";
        List<String> s =new LinkedList<>();
        s.add(null);
        Scanner sc = new Scanner(basecommand);
        command = replaceVariablesWithValues(scriptParameters, command, s, sc);
        return  command;
    }

    private static String replaceVariablesWithValues(List<Param> scriptParameters, String command, List<String> s, Scanner sc) {
        while (sc.hasNext()) {
            s.set(0,sc.next());
            if(s.get(0).charAt(0) == '$') {
                command += scriptParameters.stream().filter(e -> e.getName().equals(s.get(0).substring(1))).findFirst().get().getValue().toString();
            }else
                command += s.get(0);
            command += " ";
        }
        return command;
    }


    /**
     * Returns the CSV formatted {@link java.lang.String} that containsth erunned setups and the corresponding {@link Objective#value}.
     * @return String representation of the results of the Optimization task({@link #landscape}), the last line is the best configuration what the optimizer found.
     * @throws CloneNotSupportedException
     */
    public String getLandscapeCSVString() throws CloneNotSupportedException {
        StringJoiner sj = new StringJoiner("\n");
        sj.add("iteration,"+this.getLandscapeReference().get(0).getCSVHeaderString());
        int i =0;
        Collections.sort(this.getLandscapeReference());
        IterationResult best = null;
        for(final IterationResult ir :this.getLandscapeReference()){
            try {
                if(!ir.badConfig()) {
                    sj.add(i++ + "," + ir.getCSVString());
                    if(best == null || ir.betterThan(best))
                        best = ir;
                }
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }
        return sj.toString()+ "\nBest - " + best.getConfigurationString();
    }

    @Override
    /**
     * Basic string representation of {@link TestConfig}
     */
    public String toString() {
        return "TestConfig{" +
                "baseCommand='" + baseCommand + '\'' +
                ", objectiveFileName='" + objectiveFileName + '\'' +
                ", algorithmName='" + algorithmName + '\'' +
                ", objectiveContainer=" + objectiveContainer +
                ", scriptParameters=" + scriptParameters +
                ", iterationCount=" + iterationCount +
                '}';
    }

    /**
     * Default constructor to create an empty setup.
     */
    public TestConfig() {
        objectiveContainer = new ObjectiveContainer();
        scriptParameters = new LinkedList<>();
        iterationCount = Optional.empty();
    }

    /**
     * Setter for {@link #objectiveContainer} field
     * @param objectiveContainer
     */
    public void setObjectiveContainer(ObjectiveContainer objectiveContainer) {
        this.objectiveContainer = objectiveContainer;
    }

    /**
     * Stores the latest results of black box function
     */
    private ObjectiveContainer objectiveContainer;

    /**
     * Stores the recent  configuration of parameters
     */
    private List < Param> scriptParameters;

    /**
     * Setter for {@link #optimizerParameters}
     * @param optimizerParameters
     */
    public void setOptimizerParameters(List<Param> optimizerParameters) {
        this.optimizerParameters = optimizerParameters;
    }

    /**
     * A deprecated method for loading a {@link TestConfig} from a textfile
     * @param configFileName
     * @throws FileNotFoundException
     */
    @Deprecated
    public TestConfig(String configFileName) throws FileNotFoundException {
        scriptParameters = new LinkedList<>();
        this.objectiveContainer = new ObjectiveContainer();
        Scanner s = new Scanner(new BufferedReader(new FileReader(configFileName)));

            if(s.hasNextLine()) {
                baseCommand = s.nextLine();
                logger.info("Com = "+ this.baseCommand);
            }
            if(s.hasNextLine()){
                    int paramCounter = 0;

                    String line;
                    while (s.hasNextLine()) {
                        line = s.nextLine();
                        if (line.equals("")) continue;
                        logger.info("Line = "+ line);
                        if(line.contains("OBJ") && !line.contains("OBJECTIVEFILE")){
                            String[]  words = line.split(" ");
                            ObjectiveContainer oc = new ObjectiveContainer();

                            if(words.length==4){
                                logger.info("GET an objective ");
                                String name = words[1];
                                String relString = words[2];
                                String valString = words[3];

                                if(Utils.isInteger(valString))
                                    this.objectiveContainer.getObjectiveListReference().add( new Objective(Relation.valueOf(words[2]), false,name,0,Integer.parseInt(valString),0,1));
                                if(Utils.isFloat(valString))
                                    this.objectiveContainer.getObjectiveListReference().add( new Objective(Relation.valueOf(words[2]), false,name,0,Float.parseFloat(valString),0f,1));
                                if(Utils.isBoolean(valString))
                                    this.objectiveContainer.getObjectiveListReference().add( new Objective(Relation.valueOf(words[2]), false,name,!Boolean.parseBoolean(valString),Boolean.parseBoolean(valString),false,1));

                            }
                            continue;

                        }
                        if(line.trim().toUpperCase().equals("OBJECTIVEFILE")){

                            this.objectiveFileName = s.nextLine();
                            logger.info("File = "+ this.objectiveFileName);

                            continue;
                        }

                        if(line.contains("ITERATION")){

                            System.out.println("iteration");
                            Integer i = Integer.parseInt(line.split(" ")[1]);
                            this.iterationCount = Optional.of(i);
                            logger.info("IterationCount = "+this.iterationCount.get());
                            //  this.objectiveContainer.objectives.put("iteration",Utils.Relation.EQUALS,i,)
                            continue;
                        }
                        if(line.contains("$")) {
                            logger.info("param:  " + line);
                            Scanner lsc = new Scanner(line);

                            String parameterName = lsc.next();
                            Float d1;
                            if (parameterName.charAt(0) != '$' && Utils.isFloat(parameterName)) {
                                d1 = Float.parseFloat(parameterName);
                                parameterName = Integer.toString(paramCounter);
                            } else
                                d1 = lsc.nextFloat();
                            Float d2 = lsc.nextFloat();
                            Float d3 = lsc.nextFloat();

                            scriptParameters.add( new Param(d1, d2, d3,parameterName));
                            paramCounter++;
                        }

                    }

              //  }
            }
            Collections.sort(scriptParameters);

    }

    /**
     * Finds the specified algorithm by its name, sets up its parameters, safety preferences, the location of backup files
     * @param experimentDir Folder to save setup of the experiment
     * @param backupDir Folder to store the optional backup files
     * @param saveFileName name of the setup file that will be stored in {@code experimentDir}
     * @return CSV description of the experiment calling {@link #getLandscapeCSVString()}
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws OptimizerException
     * @throws CloneNotSupportedException
     */
    public   String runOptimizer(String experimentDir,String backupDir,String saveFileName) throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException, OptimizerException, CloneNotSupportedException {
        String algName = getAlgorithmName();
        Class optimizerClass = getOptimizerClassBySimpleName(algName);
        if(algName==null ||algName.equals(""))
            throw new OptimizerException("Algorithm is not specified");
        if(optimizerClass==null)
            throw new OptimizerException("Optimizer algorithm "+algName+" cannot be found");

        // final Class optimizerClasses = optimizer.main.Main.class.getClassLoader().loadClass(algorithmName[0]);
        Object algorithmObj = optimizerClass.newInstance();


        /*Method setConfig= optimizerClass.getMethod("loadConfigFromJsonFile",String.class);
        setConfig.invoke(algorithmObj,confi);*/
            Method setConfig= optimizerClass.getMethod("setConfiguration",TestConfig.class);
            setConfig.invoke(algorithmObj,this);

            Method updateConfigFromAlgorithmParamsMethod= optimizerClass.getMethod("updateConfigFromAlgorithmParams",List.class);
            updateConfigFromAlgorithmParamsMethod.invoke( algorithmObj,this.getScriptParametersReference());

            Method getOptConfig= optimizerClass.getMethod("getOptimizerParams");
            List<Param> ol = ( List<Param>) getOptConfig.invoke(algorithmObj);
            if(!Utils.correspondingParameterLists(ol,this.getOptimizerParameters()))
                throw new OptimizerException("Incorrect optimizer parameter list");

            Method setOptimizerConfigMethod= optimizerClass.getMethod("setOptimizerParams",List.class);
            setOptimizerConfigMethod.invoke( algorithmObj,this.getOptimizerParameters());

            if(this.getIterationCounter() != 0 ) {
            Method loadInternalStateMethod = optimizerClass.getMethod("loadState", String.class);
            loadInternalStateMethod.invoke(algorithmObj, this.getOptimizerStateBackupFilename());
        }

        Method loadOptimizerparams = optimizerClass.getMethod("loadOptimizerParamsFromJsonFile",String.class);
        loadOptimizerparams.invoke(algorithmObj,this.customParamFileName);

        if(this.getLandscapeReference().size()>0) {
            Method setupTimedelta = optimizerClass.getMethod("setTimeDelta", long.class);
            setupTimedelta.invoke(algorithmObj, this.getLandscapeReference().get(this.getLandscapeReference().size() - 1).getTimeStamp());
        }

        Method runMethod= optimizerClass.getMethod("run",String.class,String.class,String.class);
        runMethod.invoke( algorithmObj,experimentDir,backupDir,saveFileName);


        return (String)this.getLandscapeCSVString();
    }
    /**
     * Calls {@link #runOptimizer(String, String, String)} and then writes the result and experiment files
     * @param expFileName name of the setup file that will be stored in {@code experimentDir}
     * @param resFileName Name of the CSV result file
     * @param experimentDir Folder to save setup of the experiment
     * @param backupDir Folder to store the optional backup files
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws IOException
     * @throws OptimizerException
     * @throws CloneNotSupportedException
     */
    public String runAndGetResultfiles(String expFileName, String resFileName, String experimentDir,String backupDir) throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException, IOException, OptimizerException, CloneNotSupportedException {
        this.wirteExperimentDescriptionFile(expFileName);

        String result = this.runOptimizer(experimentDir,backupDir,expFileName);
        Path userDir= Paths.get(System.getProperty("user.dir"));
        String s1 = userDir.resolve(resFileName).toString();
        System.out.println("RESULTFILE = "+new File(s1).getAbsolutePath().replace("//","/"));
        String resultFilePath =  new File(s1).getAbsolutePath().replace("//","/");
        try (Writer writer = new FileWriter(resultFilePath)) {
            writer.write(result);
        }

        String best = result.substring(result.lastIndexOf('\n'));
        return best;
    }

    /**
     * Find the {@link AbstractAlgorithm} instance whit the specified name from the  list of instances previously loaded into {@link #optimizerClasses}
     * @param algName
     * @return The {@link java.lang.Class} with the given name, and null if it cannot be found in the list
     */
    public  Class getOptimizerClassBySimpleName(String algName) {

        Class optimizerClass = null;
        for (Class optAlg : this.optimizerClasses.keySet())
            if(optAlg.getSimpleName().equals(algName))
                optimizerClass = optAlg;
        return optimizerClass;
    }

    /**
     * Static method to read a {@link TestConfig} from file. Calls {@link #readConfigJSON(File)} internally.
     * @param configFileName Name of the JSON contains the serialized {@link TestConfig} object
     * @return the deserialized object
     * @throws FileNotFoundException
     */
    public static TestConfig readConfigJSON(String configFileName) throws FileNotFoundException {
        File f = new File(configFileName);


        return readConfigJSON1(f);
    }

    /**
     * Static method to read a {@link TestConfig} from file
     * @param configFile File object that contains the serialized {@link TestConfig} object
     * @return the deserialized object
     * @throws FileNotFoundException
     */
    public static TestConfig readConfigJSON(File configFile) throws IOException {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(TestConfig.class, new TestConfigDeserializer());
        Gson gson = gsonBuilder.create();
        JsonReader reader = new JsonReader(new FileReader(configFile));
        try {
            return gson.fromJson(reader, TestConfig.class);
        }catch (Exception e){
            throw new JSONReadException("Error during deserialization of JSON");
        }finally {
            reader.close();
        }

    }

    public static TestConfig readConfigJSON1(File configFile) throws FileNotFoundException {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Range.class,new RangeDeserializer());
        gsonBuilder.registerTypeAdapter(Param.class, new ParamDeserializer());
        gsonBuilder.registerTypeAdapter(Objective.class, new ObjectiveDeserializer());
        Gson gson = gsonBuilder.create();
        TestConfig t = gson.fromJson(new FileReader(configFile), TestConfig.class);

        //TestConfig modified = null;
        List<Param> pl = t.getScriptParametersReference();
        if(t.getOptimizerParameters() == null )
            return t;
        List<Param> op = t.getOptimizerParameters();
        for(Param p : op) {
            for (Object pdo : p.getDependencies()) {
                ParameterDependency pd = (ParameterDependency)pdo;
                if(pd.getP()!=null)
                    for(Param p1: t.getOptimizerParameters())
                        if(p1.equals(pd.getP()))
                            pd.setP(p1);
            }
        }
        return t;

    }

    /**
     * Filters the {@link AbstractAlgorithm} classes, according to which of those can handle the {@link Param}s of the {@link TestConfig} object.
     * @return A {@link java.util.Map} with keys : name({@link Class#getSimpleName()}) of the Classes, and values : the list of parameters of the optimizer algorithm{@link java.util.List<Param>}
     */
    public   Map<String, List<Param>> filterAlgorithms() {
        Map<String, List<Param>> algParamMap = new HashMap<String, List<Param>>();
        optimizerClasses.forEach((optimizerClass, configfile) -> {
            try {
                Object algorithmObj = optimizerClass.newInstance();

                Method setConfig = optimizerClass.getMethod("setConfiguration", TestConfig.class);
                setConfig.invoke(algorithmObj, this);

                Method setParams = optimizerClass.getMethod("updateConfigFromAlgorithmParams", List.class);
                setParams.invoke(algorithmObj, this.getScriptParametersReference());

                Method getConfig = optimizerClass.getMethod("getConfig");
                Object o = getConfig.invoke(algorithmObj);
                List<Param> pl = (List<Param>) o;

                Method isApplyableMethod = optimizerClass.getMethod("isApplyableForParams");
                if ((Boolean) isApplyableMethod.invoke(algorithmObj))
                    algParamMap.put(optimizerClass.getSimpleName(), (List<Param>) o);


            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        });
        return algParamMap;
    }

    /**
     * Serializes the {@link TestConfig} object, excluding the {@link #landscape}.
     * @param expFileName the name of the setup JSON file to be created
     */
    public void wirteExperimentDescriptionFile(String expFileName) {
        // TODO: 2018. 10. 11. hack
       // while(expFileName.startsWith("/"))
       //     expFileName = expFileName.substring(1);
        Path userDir= Paths.get(System.getProperty("user.dir"));
        String s1 = userDir.resolve(expFileName).toString();
        System.out.println("EXFILENAME = "+ s1.replace("//","/"));

        try (Writer writer = new FileWriter(s1.replace("//","/"))) {
            List<IterationResult> ls = this.getLandscapeReference();
            this.setLandscape(null);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(this, writer);
            writer.close();
            this.setLandscape(ls);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

}
