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

package optimizer.algorithms;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import optimizer.config.TestConfig;
import optimizer.exception.AlgorithmException;
import optimizer.exception.ImplementationException;
import optimizer.main.Main;
import optimizer.objective.ObjectiveContainer;
import optimizer.param.Param;
import optimizer.trial.IterationResult;
import optimizer.trial.SortbyConfigID;
import optimizer.trial.Trial;
import optimizer.utils.Utils;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static optimizer.param.Param.cloneParamList;


/**
 * Created by peterkiss on 17/10/16.
 * This is the base class of all the optimizer optimizer.algorithms. If you want to write your own optimizer,
 * extend this. If you want to write your own optimization method, you have to extend this.
 *
 */


public abstract class AbstractAlgorithm {
    /**
     * Setter method for {@link #parallelizable}
     * @param parallelizable
     */
    public void setParallelizable(ParallelExecution parallelizable) {
        this.parallelizable = parallelizable;
    }

    /**
     * False by default, since a lot of parameter optimization algortihms build on the result of previous trials.  execution of parallelizable can be switched on by setting this member to true in the subclasses.
     */
    protected ParallelExecution parallelizable = ParallelExecution.SERIAL;



    /**
     * Getter for parameters of the optimizer.
     * @return {@link #optimizerParams}
     */
    public List<Param> getOptimizerParams() {
        return optimizerParams;
    }

    /**
     * contains {@link Param}parameters of the tuning algorithm
     */
    List<Param> optimizerParams;

    /**
     * Here you can specify what parameter types can your algorithm handle.
     */
    protected List<Class> allowedTypes = new LinkedList<>();
    {
        allowedTypes.add(Float.class);
    }

    /**
     * The time difference between the start of the optimization and its potential restart to get a correct estimation of runtime.
     */
    protected long timeDelta = 0;
    /**
     *
     */
    protected TestConfig config;
    /**
     * Counter for the executed trials.
     */
    protected Integer iterationCounter=0;
    /**
     * Some algorithms are broken into multiple phases, which have their own parameter update and fitness calculation.
     * In the papers, these should belong to one iteration.
     */
    protected Integer iterationCounterCorrection=1;

    /**
     * Setter method for {@link #timeDelta}.
     * @param d
     */
    public void setTimeDelta(long d){
        this.timeDelta = d;
    }



    /**
     *Getter for the {@link #allowedTypes}.
     * */
    private List<Class> getAllowedTypes(){
        return this.allowedTypes;
    }

    /**
     * Checks here whether our {@link AbstractAlgorithm} instance is declared to be able to handle a given configuration.
     * @return
     */
    public boolean isApplyableForParams(){
        List<Param> pl = config.getScriptParametersReference();
        for(Param p : pl)
            if(!getAllowedTypes().contains(p.getParamGenericType()))
                return false;
        return true;
    }



    /**
     * This method contains the optimizer.main loop of the optimization task.
     * @param experimetDir Folder to save the experiment describing JSON.
     * @param backupDir Folder tp store potential backup files
     * @param saveFileName Unique filename with relative path generated before for experiment describing JSON.
     * @throws Exception
     */
    public void run(String experimetDir, String backupDir,String saveFileName) throws AlgorithmException, IOException, ImplementationException {

        boolean terminated = false;
        long startTime = System.currentTimeMillis();
        //todo maybe remove the parallel ones
        while (!terminated ) {

            try{
                if(this.parallelizable == ParallelExecution.PARALLEL ){

                    int threads = Runtime.getRuntime().availableProcessors();
                    ExecutorService pool = Executors.newFixedThreadPool(threads);
                    //CompletionService<IterationResult> completionService =
                    //        new ExecutorCompletionService<IterationResult>(pool);
                    Set<Future<IterationResult>> set = new HashSet<Future<IterationResult>>();
                    List<Trial> toSend = new LinkedList<>();
                    try {
                        for (int i = 0; i < this.config.getIterationCount().get(); ++i) {
                            synchronized (this) {
                                //check and penalize not allowed parameter setups
                                if (!configAllowed(config.getScriptParametersReference())) {
                                    config.setObjectiveContainer(ObjectiveContainer.setBadObjectiveValue(config.getObjectiveContainerReference()));
                                    config.getLandscapeReference().add(new IterationResult(config.getScriptParametersReference(), config.getObjectiveContainerReference(), startTime, timeDelta));
                                }



                                // create the trial object to execute
                                Trial t = new Trial(config.getBaseCommand(), false, "", config.getObjectiveContainerReference(), cloneParamList(this.config.getScriptParametersReference()), startTime, timeDelta,this.config.getPublicFolderLocation());

                                //addToTaskList(pool, set, toSend, t);
                                addToTaskList(pool, set, toSend, t);
                                updateParameters(config.getScriptParametersReference(), config.getLandscapeReference()/*, config.getOptimizerParameters()*/);
                                Main.getLogger().info( "PARAMETERS Parallel EXEC" + config.getScriptParametersReference().toString());
                            }
                        }
                        //distributed mode branch
                        if(config.getDistributedMode()){
                            executeInDistributedSystem(toSend);

                        }

                        readAndSaveResults(experimetDir, backupDir, saveFileName, set);

                    }catch (ExecutionException e){
                        throw new ImplementationException("Parallelization failed : "+e.getStackTrace() );
                    }finally {
                        pool.shutdown();
                    }
                    config.setIterationCounter(config.getIterationCount().get());
                    terminated = true;

                }
                else if (this.parallelizable == ParallelExecution.GENERATION){
                    int threads = Runtime.getRuntime().availableProcessors();
                    ExecutorService pool = Executors.newFixedThreadPool(threads);
                    CompletionService<IterationResult> completionService =
                            new ExecutorCompletionService<IterationResult>(pool);

                    this.config.setIterationCount(Optional.of(this.config.getIterationCount().get() * this.iterationCounterCorrection));


                    try {
                        for (int i = 0; i < this.config.getIterationCount().get(); ++i) {
                            synchronized (this) {
                                List<Trial> toSend = new LinkedList<>();
                                Set<Future<IterationResult>> set = new HashSet<Future<IterationResult>>();

                                //check and penalize not allowed parameter setups -todo wrong place but not supposed to happen here anyway
                                if (!configAllowed(config.getScriptParametersReference())) {
                                    config.setObjectiveContainer(ObjectiveContainer.setBadObjectiveValue(config.getObjectiveContainerReference()));
                                    config.getLandscapeReference().add(new IterationResult(config.getScriptParametersReference(), config.getObjectiveContainerReference(), startTime, timeDelta));
                                }
                                //No id-s, the setups andy the results have to be in the same order
                                //in first round initialization
                                updateParameters(config.getScriptParametersReference(), config.getLandscapeReference()/*, config.getOptimizerParameters()*/);
                                List<List<Param>> individuals =getParameterMapBatch(config.getScriptParametersReference());
                                for (List<Param> p : individuals) {
                                    Trial t = new Trial(config.getBaseCommand(), false, "", config.getObjectiveContainerReference(), p/*Param.cloneParamList(this.config.getScriptParametersReference())*/, startTime, timeDelta, this.config.getPublicFolderLocation());
                                    addToTaskListAndWait(completionService, set, toSend, t);
                                    //addToTaskList(pool, set, toSend, t);
                                }
                                //distributed mode branch
                                if(config.getDistributedMode())
                                    //todo this is not working yet
                                    executeInDistributedSystem(toSend);

                                //here we add all the results to the landscape
                                List<IterationResult> results = readAndSaveResults(experimetDir, backupDir, saveFileName, set);

                                // order the iteration result as they came in the getParameterMapBatch
                                //Collections.sort(results, new SortbyConfigID());
                                setResults(results);
                                updateGlobals();

                                //Main.getLogger().info( "PARAMETERS Parallel EXEC" + config.getScriptParametersReference().toString());
                            }
                        }


                    }catch (ExecutionException e){
                        throw new ImplementationException("Parallelization failed : "+e.getStackTrace() );
                    }finally {
                        pool.shutdown();
                    }
                    config.setIterationCounter(config.getIterationCount().get());
                    terminated = true;

                }
                else {
                    if (configAllowed(config.getScriptParametersReference())) {
                        Trial t = new Trial(config.getBaseCommand(), false, "", config.getObjectiveContainerReference(), cloneParamList(this.config.getScriptParametersReference()), startTime, timeDelta,this.config.getPublicFolderLocation());
                        IterationResult ir = t.executeSequentially();
                        Main.getLogger().info(  "GETTING RESULT FROM " + ir.getCSVString());
                        this.config.setIterationCounter(this.config.getIterationCounter() + 1);
                        this.config.getLandscapeReference().add(ir);
                        if (this.config.getSavingFrequence() != -1 && this.config.getIterationCounter() % this.config.getSavingFrequence() == 0) {
                            String saveFileName1 = saveFileName.replace(experimetDir, backupDir).replace(".json", "_" + this.config.getIterationCounter() + ".json");
                            writeResultFile(saveFileName1);
                        }
                        /*BufferedReader r = null;
                        r = executeAndGetResultBufferedReader(config);
                        ObjectiveContainer oc = ObjectiveContainer.readObjectives(r,null, config.getObjectiveContainerReference());
                        config.setObjectiveContainer(oc);*/
                    } else {
                        config.setObjectiveContainer(ObjectiveContainer.setBadObjectiveValue(config.getObjectiveContainerReference()));

                    }
                    terminated =this.terminated();
                    config.getLandscapeReference().add(new IterationResult(config.getScriptParametersReference(), config.getObjectiveContainerReference(),startTime,timeDelta));
                }



                if(!terminated) {
                    Main.getLogger().info( "OLD PARAMETERS" + config.getScriptParametersReference().toString());
                    try {
                        updateParameters(config.getScriptParametersReference(), config.getLandscapeReference()/*, config.getOptimizerParameters()*/);
                    }  catch (Exception e){
                        Main.getLogger().info(e);
                        throw new AlgorithmException("Algorithm error");
                    }
                    Main.getLogger().info( "NEW PARAMETERS" + config.getScriptParametersReference().toString());
                }

                if(this.config.getSavingFrequence()!=-1 && this.config.getIterationCounter() % this.config.getSavingFrequence() == 0|| terminated) {
                    if(terminated && config.getLandscapeReference().size()<config.getIterationCount().get())
                        System.out.println("para");
                    this.saveState(this.config.getOptimizerStateBackupFilename()==null?"optBackUp.json":this.config.getOptimizerStateBackupFilename());
                    if(!terminated) {
                        String saveFileName1 = saveFileName.replace(experimetDir,backupDir).replace(".json","_"+this.config.getIterationCounter()+".json");
                        writeResultFile(saveFileName1);
                    }
                }
                // TODO: 2018. 02. 25. InterruptedException
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
            this.config.setIterationCounter(this.config.getIterationCounter()+1);
            Main.getLogger().info(this.config.toString());
        }

    }

    private List<IterationResult> readAndSaveResults(String experimetDir, String backupDir, String saveFileName, Set<Future<IterationResult>> set) throws InterruptedException, ExecutionException, CloneNotSupportedException, IOException {
        Main.getLogger().info( "COLLECTING RESULTS" + config.getScriptParametersReference().toString());
        List<IterationResult> res = new LinkedList<>();
        for (Future<IterationResult> future : set) {
            //todo ugly as hell do the decent way: https://stackoverflow.com/questions/19348248/waiting-on-a-list-of-future
            while(!future.isDone())Thread.sleep(10);
            IterationResult ir = future.get();
            Main.getLogger().info( "GETTING RESULT  " + ir.getCSVString());
            res.add(ir);
            this.config.getLandscapeReference().add(ir);
            if (this.config.getSavingFrequence() != -1 && this.config.getIterationCounter() % this.config.getSavingFrequence() == 0) {
                String saveFileName1 = saveFileName.replace(experimetDir, backupDir).replace(".json", "_" + this.config.getIterationCounter() + ".json");
                writeResultFile(saveFileName1);
            }

        }
        Main.getLogger().info( "ALL RESULTS ARRIVED in round " + this.config.getIterationCounter());

        this.config.setIterationCounter(this.config.getIterationCounter() + 1);
        return res;

    }

    private void addToTaskList(ExecutorService pool, Set<Future<IterationResult>> set, List<Trial> toSend, Trial t) {
        if(config.getDistributedMode())
            toSend.add(t);
        else
            set.add(pool.submit(t));
    }

    private void addToTaskListAndWait(CompletionService cs, Set<Future<IterationResult>> set, List<Trial> toSend, Trial t) {
        if(config.getDistributedMode())
            toSend.add(t);
        else
            set.add(cs.submit(t));
    }

    private void executeInDistributedSystem(List<Trial> toSend) throws InterruptedException {
        for(Trial t :toSend) {
            Gson gson1 = new GsonBuilder().setPrettyPrinting().create();
            this.config.getCommunicationObject().publish(gson1.toJson(t, Trial.class), Main.getDistributedApplicationId());
        }
        final Gson gson = new Gson();
        this.config.getCommunicationObject().distribute(toSend.stream().map(t-> gson.toJson(t,Trial.class)).collect(Collectors.toList()),Main.getDistributedApplicationId());

        while(config.getLandscapeReference().size()<toSend.size()) {
            // TODO: 2018. 10. 05. hardcoded id
            config.getLandscapeReference().addAll(config.getCommunicationObject().receive(Main.getDistributedApplicationId()).stream().map(t -> IterationResult.deserializeIterationResults(t)).collect(Collectors.toList()));
            System.out.println("COORD RECIEVING "+toSend.size() +" / "+config.getLandscapeReference().size());
            Thread.sleep(500);
            this.config.setIterationCounter(config.getLandscapeReference().size());
        }
        // TODO: 2018. 12. 14. should not shot down here
        this.config.getCommunicationObject().publish("STOP",Main.getDistributedApplicationId());
    }

    /**
     * Method to execute a process based on configuration specified at the moment in config, and returns a {@link BufferedReader} object to read its output.
     * @param config {@link TestConfig} object containg all parameter of the next run.
     * @return BufferedReader that reads either from a result file, or from standard output, based on specification in the config.
     * @throws IOException
     * @throws InterruptedException
     */
    @Deprecated
    private BufferedReader executeAndGetResultBufferedReader(TestConfig config) throws IOException, InterruptedException {
        BufferedReader r;
        String command = config.getCommand();
        Runtime rt = Runtime.getRuntime();
        Main.getLogger().info( "Executing : " + command);

        Process pr = rt.exec(command);
        pr.waitFor();
        if(config.getObjectiveFileName()!=null) {
            FileReader fr = new FileReader(config.getObjectiveFileName());
            r = new BufferedReader(fr);
        }
        else
            r= new BufferedReader(new InputStreamReader(pr.getInputStream()));
        return r;

    }



    //if one of the param is not active at a given place we say no..

    /**
     * Decides whether a given optimization problem can be runed using the given {@link AbstractAlgorithm}vThat is whether it handles the Parameter types that are in the configuration of the problem.
     * @param scriptParameters The parameters of the algorihm to be optimized.
     * @return
     * @throws Exception
     */
    private synchronized boolean configAllowed(List<Param> scriptParameters) throws ImplementationException {
        for(Param p : scriptParameters){
            if(!p.isActive())
                continue;
            if(p.isValid()&&!p.isInRange())
                return false;

        }
        return true;
    }

    //later

    /**
     * Beta method for executing Trials onn demand if the logic optimization requires it . If the Trial has been already run should return with its result without redundant execution.
     * @param configuration The configuration to try.
     * @param startTime
     * @param delay
     * @return
     * @throws InterruptedException
     * @throws IOException
     * @throws CloneNotSupportedException
     */
    public ObjectiveContainer lookup(List<Param> configuration,long startTime,long delay) throws InterruptedException, IOException, CloneNotSupportedException {
        for(IterationResult ir : config.getLandscapeReference())
            if(Utils.paramConfigsAreEqual(ir.getConfigurationClone(),configuration))
                return ir.getObjectiveContainerClone();
        String command = config.getCommand(configuration,config.getBaseCommand());
        ObjectiveContainer oc = runAlgorithm(command);

        config.getLandscapeReference().add(new IterationResult(configuration,oc,startTime,delay));
        return oc;

    }

    /**
     * The method runs the algorithm then retrieves a reads the objective values and passes them back in an {@link ObjectiveContainer}
     * @param command
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    private ObjectiveContainer runAlgorithm(String command) throws IOException, InterruptedException {
        BufferedReader r = executeAndGetResultBufferedReader(config);
        return ObjectiveContainer.readObjectives(r,null, config.getObjectiveContainerReference());
    }

    @Deprecated
    public void writeResultFile(String configFileName) throws IOException {
        /*SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String configFileName  = getAlgorithmSimpleName()+"_"+dateFormat.format(new Date());*/
        //config.getSaveFileName();
        //writeOptimizerParamsToJsonFile(configFileName);
        //this.config.setOptimizerConfigFilename(configFileName);
        try (Writer writer = new FileWriter(configFileName)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(this.config, writer);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Method for querying whether  the tuning has been terminated.
     * If termination is due to iteration number limit, it is possible that we have more trials, since some tuning optimizer.algorithms execute them in a grouped way
     * @return
     * @throws FileNotFoundException
     */
    public  boolean terminated() throws FileNotFoundException {
        if(this.config.getIterationCount().isPresent()
                && this.config.getIterationCounter()>= this.config.getIterationCount().get())
            return  true;
        return this.config.getObjectiveContainerReference() == null ? false : this.config.getObjectiveContainerReference().terminated();

    }


    /**
     * Method to get the parameters of the optimizer algorithm.
     * @return a cloned list of optimizer parameters.
     * @throws CloneNotSupportedException
     */
    public List<Param> getConfig() throws CloneNotSupportedException {
        List<Param> ret = new LinkedList<>();
        for(Param p: this.optimizerParams)
            ret.add((Param) p.clone());
        return ret;
    }

    /**
     * Methof for setting the parameters of the optimizer algorithm ({@code #optimizerParams}). This is called to set up the parameters wen we use the html interface
     * @param paramList List of parameters
     */
    public void setOptimizerParams(List<Param> paramList){
        this.optimizerParams = paramList;
    }

    /**
     * Writes the list of {@link Param} of the optimizer algorithm into a separated JSON
     * @param jsonName
     * @throws FileNotFoundException
     */
    @Deprecated
    public void writeOptimizerParamsToJsonFile(String jsonName) throws FileNotFoundException {
        if(false) {
            Type listType = new TypeToken<LinkedList<Param>>() {
            }.getType();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String s = gson.toJson(this.optimizerParams, listType);

            try {
                //PrintWriter writer = new PrintWriter(getAlgorithmSimpleName()+"_params.json", "UTF-8");
                PrintWriter writer = new PrintWriter(jsonName + ".config", "UTF-8");
                writer.println(s);
                //writer.println("The second line");
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * Set the experiment setup({@link TestConfig}) to be runned using this algorithm.
     */
    public void setConfiguration(TestConfig tc){
        this.config = tc;
    }

    /**
     * loads the entire test-configuration from a json file
     * @param configFileName
     */
    @Deprecated
    public void loadConfigFromJsonFile(String configFileName){
        if(configFileName == null)
            return;
        Main.getLogger().info( System.getProperty("user.dir"));
        Main.getLogger().info( "ConfigFile = " + configFileName);
        try {
            if (configFileName.contains(".json")) {
                Gson gson = new Gson();
                JsonReader reader = new JsonReader(new FileReader(configFileName));
                this.config =  gson.fromJson(reader, TestConfig.class);
                loadOptimizerParamsFromJsonFile(config.getOptimizerConfigFilename());
            } else {
                //this.config = new TestConfig(configFileName);
            }
        }catch (IOException e){
            Main.getLogger().info( "File not found:  "+ configFileName);
            return;
        }

    }

    /**
     * loads the parameter configuration of the optimizer from an external json
     * @param configFileName
     * @throws FileNotFoundException
     */
    @Deprecated
    public void loadOptimizerParamsFromJsonFile(String configFileName) throws FileNotFoundException {
        if(configFileName == null)
            return;
        Type listType = new TypeToken<LinkedList<Param>>(){}.getType();
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new FileReader(configFileName));
        this.optimizerParams =  gson.fromJson(reader, listType);
    }

    public String getAlgorithmQualifiedName(){
        return this.getClass().getName();
    }
    public String getAlgorithmSimpleName(){
        return this.getClass().getSimpleName();
    }

    /**
     * update the configuration of the optimizer algorithm if it is necessary.
     * @param algParams this contains the parametewrs to be optimized
     */
    public void updateConfigFromAlgorithmParams(List<Param> algParams){}

    /**
     * override to save he algorithm internal state for recovery
     */
    public void saveState(String internalStateBackupFileName){}
    /**
     * override to save he algorithm internal state for recovery
     */
    public void loadState(String internalStateBackupFileName) throws FileNotFoundException {}


    /**
     * Here the optimizer algorithm receives references to the lists containing state of the optimization
     * @param parameterMap contains the description of the {@link Param Param} values their ranges {@link Param#getDependencies()} and their dependencies. The system expects to change the parametervalues here using set
     * @param landscape readonly, contains the history of executions of the algorithm to be optimized
     * .@param optimizerParams parameters of the optimizer
     */
    public abstract void updateParameters(List< Param> parameterMap, List<IterationResult> landscape/*, List<Param> optimizerParams*/) throws CloneNotSupportedException;
    //public abstract void updateParameters(List<List< Param>> parameterMaps, List<List<IterationResult>> landscapes/*, List<Param> optimizerParams*/) throws CloneNotSupportedException;
    //

    // override these for parallelized population-based algorithms!!
    public List<List<Param>> getParameterMapBatch(List<Param> pattern)throws CloneNotSupportedException {return null;}
    public void setResults(List<IterationResult> results) throws CloneNotSupportedException {}
    public void updateGlobals() throws CloneNotSupportedException {}


    public void randomFloatInit(List<Param> setup){
        Random rand = new Random();

        for(int i = 0; i < setup.size(); ++i) {
            float r = rand.nextFloat();
            Param p = setup.get(i);
            //random initialization of position
            p.setInitValue((float)p.getLowerBound() + r * ((float)p.getUpperBound() - (float)p.getLowerBound()));
        }
    }


}
