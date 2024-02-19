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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import optimizer.exception.AlgorithmException;
import optimizer.exception.ImplementationException;
import optimizer.objective.Relation;
import optimizer.param.*;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import spark.ModelAndView;
import spark.Request;
import spark.Service;
import spark.template.velocity.VelocityTemplateEngine;
import optimizer.utils.*;
import optimizer.config.TestConfig;
import optimizer.objective.Objective;
import optimizer.objective.ObjectiveContainer;
import optimizer.trial.IterationResult;

import javax.script.ScriptException;
import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;


/**
 * This class serves as the backend of the web browser GUI.
 * Created by peterkiss on 2018. 01. 22..
 */
public class BrowserInterface {


    private  Thread optimizationRunnerThread;



    private final static String[] resourceSeparator = new String[]{Utils.runningInJar()?"/":File.separator};

    private final static String layout = "templates"+resourceSeparator[0]+"layout.vtl";
    private final static String resultTemplate = "templates"+resourceSeparator[0]+"resultnew.vtl";
    private final String[] algoritmhs;
    private final Boolean[] safeMode= {false};
    private final boolean[] recoveryMode = {false};
    private final boolean[] distributedMode = {false};
    private final Com[] comObj= new Com[1];
    private final String[] initialConfigFileName = new String[1];
    private final String[] configFileName= new String[1];
    private final String[] saveFileName= new String[1];
    private final String[] publicFolderLocation = new String[1];
    private static String outputDir;
    private static String experimentDir ;
    private static String uploadDir ;
    private static String backupDir ;
    private static String outputDirName;
    private static String experimentDirName ;
    private static String uploadDirName ;
    private static String backupDirName ;
    private static List<String> classList;
    private static List<String> objTypes;
    private final String[] objectiveTypes = Arrays.stream(Relation.values()).map(v->v.toString()).toArray(String[]::new);
    private  Boolean[] executionError= new Boolean[]{false};
    private  String[] executionErrorMsg = new String[1];
    private Service sparkserverservice;
    private final String[] bestSetUp = new String[]{"Working..."} ;


    //available optimizer.algorithms
    private Map<Class<? extends AbstractAlgorithm>,String> optimizerClasses;// = new HashMap<Class<? extends AbstractAlgorithm>,String>();

    private  TestConfig[] config = new TestConfig[1];


    public static String getOutputDir() {
        return outputDir;
    }

    public static String getExperimentDir() {
        return experimentDir;
    }

    public static String getUploadDir() {
        return uploadDir;
    }

    public static String getBackupDir() {
        return backupDir;
    }

    public BrowserInterface(String initialConfigFileName, Map<Class<? extends AbstractAlgorithm>,String> optimizerClasses, String projectDir, String staticDir, String experimentDir, String outputDir, String backupDir, String uploadDir, String saveFileName, boolean distributedMode, Com comobj, String experimentDirName, String outputDirName, String backupDirName, String uploadDirName, String publicFolderLocation) throws CloneNotSupportedException, FileNotFoundException {


        //this.velocityEngine.init();
        this.initialConfigFileName[0] = initialConfigFileName;
        this.optimizerClasses = optimizerClasses;
        this.algoritmhs =  optimizerClasses.keySet().stream().map(a->a.getName()).toArray(String[]::new);
        this.saveFileName[0] =saveFileName;
        this.distributedMode[0] = distributedMode;
        this.comObj[0] = comobj;
        this.publicFolderLocation[0] = publicFolderLocation;

        BrowserInterface.uploadDir = uploadDir;
        BrowserInterface.outputDir = outputDir;
        BrowserInterface.experimentDir = experimentDir;
        BrowserInterface.backupDir = backupDir;

        BrowserInterface.uploadDirName = uploadDirName;
        BrowserInterface.outputDirName = outputDirName;
        BrowserInterface.experimentDirName = experimentDirName;
        BrowserInterface.backupDirName = backupDirName;

        BrowserInterface.classList=  Arrays.asList(Integer.class.getName(),Float.class.getName(),Boolean.class.getName(),"Enum","Function");
        BrowserInterface.objTypes=  Arrays.asList(Integer.class.getName(),Float.class.getName()/*,Boolean.class.getName(),"Enum","Function"*/);



    }

    @SuppressWarnings("unchecked")
    public void run() {

        Properties properties = new Properties();
        if(Utils.runningInJar()){
            java.net.URL u1= this.getClass().getProtectionDomain().getCodeSource().getLocation();
            String jarUrlString = u1.toString();
            if(jarUrlString.contains("!"))
                jarUrlString = jarUrlString.substring(0,jarUrlString.lastIndexOf("!"));
            if(!jarUrlString.startsWith("jar:"))
                jarUrlString = "jar:"+jarUrlString;

            properties.setProperty("resource.loader", "jar");

            properties.setProperty(
                    "jar.resource.loader.class",
                    "org.apache.velocity.runtime.resource.loader.JarResourceLoader");
            properties.setProperty(
                    "jar.resource.loader.path",
                    /*"jar:"+*/jarUrlString);
        }
        else {
            properties.setProperty("resource.loader", "class");
            properties.setProperty(
                    "class.resource.loader.class",
                    "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        }

        VelocityEngine velocityEngine = new VelocityEngine(properties);
        velocityEngine.init();
        sparkserverservice =Service.ignite().port(4567).threadPool(8);
        sparkserverservice.staticFiles.externalLocation(Utils.publicIFPath());
        sparkserverservice.staticFiles.location("public");


        sparkserverservice.exception(Exception.class, (e, request, response) -> {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw, true);
            e.printStackTrace(pw);
            System.err.println(sw.getBuffer().toString());
        });
//todo Nuulpointer
        sparkserverservice.get("/progress", (req, res) ->{
            if(executionError[0])
                return "error";

                if (optimizationRunnerThread!=null && optimizationRunnerThread.getState() == Thread.State.TERMINATED) {
                    return "done - " + bestSetUp[0];


            }return (float)config[0].getIterationCounter() / (float)config[0].getIterationCount().get()*100;

        });

        sparkserverservice.get("/error", (req, res) -> new VelocityTemplateEngine( velocityEngine).render(
                new ModelAndView(getErrorMode(executionErrorMsg[0]), layout)
        ));


        sparkserverservice.get("/getresults", (req, res) ->{
            System.out.println("RESULT");
            Map<String, Object> model1 = getResultModel(saveFileName[0]==null?"":saveFileName[0]);
            return new VelocityTemplateEngine(velocityEngine).render(
                    new ModelAndView(model1, layout)
            );
        });

        sparkserverservice.get("/stop", (req, res) ->{
            Map<String, Object> model = getGoodBye();
            sparkserverservice.stop();
            return new VelocityTemplateEngine(velocityEngine).render(
                    new ModelAndView(model, layout)
            );
        });

        sparkserverservice.get("/hello", (req, res) ->{
            this.config[0] = TestConfig.readConfigJSON(Utils.normalizePath(this.initialConfigFileName[0]));

            Map<String, Object> model = getBBSetupModel(saveFileName[0],config, classList, objectiveTypes, algoritmhs,objTypes);
            return new VelocityTemplateEngine(velocityEngine).render(
                    new ModelAndView(model, layout)
            );
        });


        sparkserverservice.post("/loadsetup","multipart/form-data",(req,res)->{
            // TODO: 28/08/17 check if exists
            config[0] = new TestConfig();
            File uploadDir = new File(BrowserInterface.uploadDir);
            Path tempFile = Files.createTempFile(uploadDir.toPath(), "", "");
            req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));
            Part filePart = req.raw().getPart("chosenfile");
            this.configFileName[0] = filePart.getSubmittedFileName();
            //prepare base of the savefilename
            String [] fnparts =  Utils.getFilePathParts(this.configFileName[0] );
            saveFileName[0] =fnparts[fnparts.length-1];
            try (InputStream input = filePart.getInputStream()) { // getPart needs to use same "name" as input field in form
                Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                if (tempFile.toFile().length() != 0)
                    config[0] = TestConfig.readConfigJSON(tempFile.toFile());
                Files.delete(tempFile);
                Map<String, Object> model = getBBSetupModel(saveFileName[0], config, classList, objectiveTypes, algoritmhs, objTypes);
                return new VelocityTemplateEngine(velocityEngine).render(
                        new ModelAndView(model, layout)
                );
            }catch (Exception e){
                return new VelocityTemplateEngine(velocityEngine).render( new ModelAndView(getErrorMode("Error in setup file: "+configFileName[0]),layout));
            }


        });



        sparkserverservice.post("/updateconfig", (request, response) -> {
            try{
                // against crashes after restart the server
                if(config[0]==null)
                    config[0]=new TestConfig();
                String safeModeString = request.queryParams("safe_mode");
                if(safeModeString != null) {
                    config[0].setSavingFrequence(Integer.parseInt(request.queryParams("frequency")));
                    safeMode[0] = true;
                }

                updateSaveFileName(request.queryParams("savefilename"));
                String useIterationString = request.queryParams("use_iterations");
                String iterationCountString = request.queryParams("iterationCount");
                String command_input = request.queryParams("commandinput");

                int counter = 0;
                List<IterationResult> landscape = new LinkedList<>();
                //followings needed for recovery(??)
                //setup previous or predefined optimizer optimizer.algorithms it there is any -
                List<Param> predefinedOptimizerParams = config[0].getOptimizerParameters();


                if(config[0].getLandscapeReference()!= null && config[0].getLandscapeReference().size()>0) {
                    recoveryMode[0] = true;
                    landscape = config[0].getLandscapeReference();
                    counter = config[0].getIterationCounter();

                }
                else
                    recoveryMode[0] = false;
                TestConfig c = new TestConfig();
                c.setAlgorithmName(config[0].getAlgorithmName());
                // in case we have them in the same file
                c.setOptimizerParameters(predefinedOptimizerParams);
                c.setBaseCommand(command_input);
                c.setIterationCounter(config[0].getIterationCounter()); //this is for recovery mode (??)
                if(useIterationString != null)
                    c.setIterationCount(Optional.of(Integer.parseInt(iterationCountString)));


                String objFileName1 = request.queryParams("objFileName");
                ObjectiveContainer objectiveContainer = readObjectives(request);
                c.setObjectiveContainer(objectiveContainer);
                String usefileStr = request.queryParams("use_file_output");
                if(usefileStr!=null)
                    c.setObjectiveFileName(objFileName1);
                c.setLandscape(landscape);
                c.setIterationCounter(counter);

                List<Param> paramList  = readParams(request,null);




                c.setScriptParameters(paramList);

                config[0]= c;
                String[] errormsg = new String[1];
                boolean[] faliure = new boolean[]{false};
                Map<String,List<Param>> algParamMap = new HashMap<>();
                optimizerClasses.forEach( (Class<? extends AbstractAlgorithm> optimizerClass, String configfile) ->{
                    if(!faliure[0]){

                        try {
                            Object algorithmObj = optimizerClass.newInstance();

                            Method setConfig= optimizerClass.getMethod("setConfiguration",TestConfig.class);
                            setConfig.invoke(algorithmObj,config[0]);

                    /*Method setConfigFromFile= optimizerClass.getMethod("loadConfigFromJsonFile",String.class);
                    setConfig.invoke(algorithmObj,"test.json");*/

                            Method setParams= optimizerClass.getMethod("updateConfigFromAlgorithmParams",List.class);
                            setParams.invoke(algorithmObj,config[0].getScriptParametersReference());

                            Method getConfig= optimizerClass.getMethod("getConfig");
                            Object o = getConfig.invoke(algorithmObj);


                            Method isApplyableMethod= optimizerClass.getMethod("isApplyableForParams");
                            if((Boolean)isApplyableMethod.invoke(algorithmObj))

                                        algParamMap.put(optimizerClass.getSimpleName(),(List<Param>)o);


                        } catch (InstantiationException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                            faliure[0]= true;
                            errormsg[0] = e.getMessage() + Arrays.toString(e.getStackTrace());
                        }

                    }

                });
                if(faliure[0])
                    return  new ModelAndView(getErrorMode(errormsg[0]), layout);
                //recovery mode if there is any predefined algorithm, otherwise the first in the list
                if(!algParamMap.keySet().contains(config[0].getAlgorithmName()))
                    config[0].setAlgorithmName(algParamMap.keySet().iterator().next());
                //we set up values for optimizerparam from configfile
                List<Param> pl = algParamMap.get(config[0].getAlgorithmName());
                if(pl!=null && config[0].getOptimizerParameters()!=null) // for algorithm without parameters
                    for(Param paramInMap : pl){ //for reloading values at recovery??
                        for(Param loadedParam : config[0].getOptimizerParameters())
                            if(paramInMap.equals(loadedParam))
                                paramInMap = loadedParam; //is this ok? it might be lost after loop

                    }


                Map<String, Object> model1 = new HashMap<>();


                model1.put("algorithmname",config[0].getAlgorithmName());
                model1.put("filename",saveFileName[0]);
                model1.put("template","templates"+resourceSeparator[0]+"algorithm.vtl");
                model1.put("algParamMap",algParamMap);
                model1.put("parametertypes",classList);

                return new ModelAndView(model1, layout);
            }
            catch (Exception e){
                String msg = e.getMessage() + Arrays.toString(e.getStackTrace());
                return new ModelAndView(getErrorMode(msg), layout);
            }


        }, new VelocityTemplateEngine(velocityEngine));

        sparkserverservice.post("/updatealgorithmconfig", (request, response) -> {
            try{

                request.queryParams().forEach(System.out::println);

                String algorithmname = request.queryParams("algorithm_names");
                config[0].setAlgorithmName(algorithmname);
                String useIterationString = request.queryParams("use_iterations");

                String iterationCountString = request.queryParams("iterationCount");
                String command_input = request.queryParams("commandinput");

                TestConfig c = new TestConfig();
                c.setAlgorithmName(algorithmname);
                c.setBaseCommand(command_input);
                c.setIterationCounter(config[0].getIterationCounter());

                if(useIterationString != null)
                    c.setIterationCount(Optional.of(Integer.parseInt(iterationCountString)));


                String objFileName1 = request.queryParams("objFileName");

                ObjectiveContainer objectiveContainer = readObjectives(request);

                c.setObjectiveContainer(objectiveContainer);
                c.setObjectiveFileName(objFileName1);
                if(recoveryMode[0]) {
                    c.setLandscape(config[0].getLandscapeReference());
                    c.setIterationCounter(config[0].getIterationCounter());
                }
                List<Param> paramList =  readParams(request,algorithmname);



                c.setScriptParameters(paramList);
                try (Writer writer = new FileWriter("test_"+algorithmname+"_conf.json")) {
                    Gson gson1 = new GsonBuilder().setPrettyPrinting().create();
                    gson1.toJson(c, writer);
                }
                config[0]= c;
                String[] errormsg = new String[1];
                boolean[] faliure = new boolean[]{false};
                Map<String,List<Param>> algParamMap = new HashMap<>();
                optimizerClasses.forEach( (optimizerClass, configfile ) ->{
                    if(!faliure[0]) {
                        try {
                            Object algorithmObj = optimizerClass.newInstance();

                            Method setConfig = optimizerClass.getMethod("setConfiguration", TestConfig.class);
                            setConfig.invoke(algorithmObj, config[0]);

                    /*Method setConfig= optimizerClass.getMethod("loadConfigFromJsonFile",String.class);
                    setConfig.invoke(algorithmObj,"test.json");*/


                            Method setParams = optimizerClass.getMethod("setOptimizerParams", List.class);
                            setParams.invoke(algorithmObj, config[0].getScriptParametersReference());

                            Method getConfig = optimizerClass.getMethod("getConfig");
                            Object o = getConfig.invoke(algorithmObj);
                            algParamMap.put(optimizerClass.getSimpleName(), (List<Param>) o);


                        } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                            faliure[0]= true;
                            errormsg[0] = e.getMessage() + Arrays.toString(e.getStackTrace());
                        }
                    }

                });
                if(faliure[0])
                    return  new ModelAndView(getErrorMode(errormsg[0]), layout);

                Map<String, Object> model1 = new HashMap<>();


                model1.put("filename",saveFileName[0]);
                model1.put("template","templates"+resourceSeparator[0]+"algorithm.vtl");
                model1.put("algParamMap",algParamMap);
                model1.put("parametertypes",classList);

                return new ModelAndView(model1, layout);
            }catch(Exception e){

                return  new ModelAndView(getErrorMode(e.getMessage()+" "+ Arrays.toString(e.getStackTrace())), layout);
            }
        }, new VelocityTemplateEngine(velocityEngine));

        sparkserverservice.post("/run", (request, response) ->{
            String algorithmname = request.queryParams("algorithm_names");
            List<Param> lp = readParams(request, algorithmname);
            config[0].setAlgorithmName(algorithmname);
            config[0].setOptimizerParameters(lp);
            config[0].setOptimizerClasses(optimizerClasses);
            if (!recoveryMode[0]) {
                config[0].setIterationCounter(0);
                config[0].clearLandscape();


            }


            String expFileName = Utils.getExperimentUniqueName(saveFileName[0], experimentDir);
            String resFileName = Utils.getExpCSVFileName(Utils.getExperimentName(expFileName), outputDir);
            String[] errormsg = new String[1];
            boolean[] faliure = new boolean[]{false};
            config[0].setDistributedMode(this.distributedMode[0]);
            config[0].setCommunicationObject(this.comObj[0]);
            config[0].setPublicFolderLocation(publicFolderLocation[0]);
            Thread.UncaughtExceptionHandler h = (th, ex) -> {
                errormsg[0] = ex.getMessage();
                faliure[0] = true;
            };
//            synchronized (optimizationRunnerThread) {
                this.optimizationRunnerThread = new Thread(() -> {

                    try {

                        bestSetUp[0] = config[0].runAndGetResultfiles(expFileName, resFileName, experimentDir, backupDir);
                    } catch (Exception e) {
                        executionError[0] = true;
                        executionErrorMsg[0] = "Error in optimization process: (are the command to execute correct?)<br>" + e.getMessage() + Arrays.toString(e.getStackTrace());
                        throw new AlgorithmException("Error in optimization process: " + executionErrorMsg[0]);
                    }

                });
        //    }

            this.optimizationRunnerThread.setUncaughtExceptionHandler(h);

            this.optimizationRunnerThread.start();

            if(faliure[0])
                return new VelocityTemplateEngine(velocityEngine).render(  new ModelAndView(getErrorMode(errormsg[0]), layout));



            Map<String, Object> model1 = getProgressModel(saveFileName[0]);//getResultModel(this.config[0].getObjectiveContainerReference().getObjectiveContainerClone(), resFileName,saveFileName[0]);

            return new VelocityTemplateEngine(velocityEngine).render(
                    new ModelAndView(model1, layout)
            );


        });


    }

    private void updateSaveFileName(String savefilename) {
        String filename = new File(savefilename.replace("\\","\\\\")).getName();
        saveFileName[0] = getExperimentDir()+File.separator+File.separator+filename;

    }

    private Map<String,Object> getErrorMode(String e) {
        Map<String,Object> res = new HashMap<>();
        res.put("template","templates/error.vtl");
        res.put("errormessage",e);
        return res;
    }

    private static Map<String,Object> getGoodBye() {
        return new HashMap<>();
    }

    private static Map<String, Object> getBBSetupModel(String file,TestConfig[] config, List<String> classList, String[] objectiveTypes, String[] algoritmhs,List<String> objtypes) throws CloneNotSupportedException {
        Map<String, Object> model = new HashMap<>();
        model.put("filename",file.replace("experiments/",""));
        model.put("parametertypes",classList);
        model.put("template","templates"+resourceSeparator[0]+"param.vtl");
        model.put("paramlist",config[0].getScriptParametersReference());
        model.put("command",config[0].getBaseCommand());
        model.put("objlist",config[0].getObjectiveContainerReference().getObjectiveListClone());
        model.put("objectivetypes", objectiveTypes);
        model.put("parameternames",config[0].getScriptParametersReference().stream().map(Param::getName).toArray(String[]::new));
        model.put("optimizer"+resourceSeparator[0]+"algorithms",algoritmhs);
        model.put("objtypes",objtypes);
        model.put("obj_filename", config[0].getObjectiveFileName());
        model.put("iteration_count", config[0].getIterationCount().isPresent()?config[0].getIterationCount().get():0);
        return model;
    }


    private  static Map<String, Object> getProgressModel(String saveFileName) {

        Map<String, Object> model1 = new HashMap<>();
        model1.put("template", "templates/progress.vtl");
        model1.put("filename",saveFileName);

        return model1;
    }


    private static Map<String, Object> getResultModel(/*List<Objective> objectives, String resFileName, String configFileName*/String saveFileName) {
     /*   final List<String> objectiveList = new LinkedList<String>();
        objectives.forEach(o->objectiveList.add(o.getName()));

        final List<String> objectiveRelationList = new LinkedList<String>();
        objectives.forEach(o->objectiveRelationList.add(o.getRelation().equals(Relation.GREATER_THAN)||o.getRelation().equals(Relation.MAXIMIZE)?"increase":"decrease"));
*/

        System.out.println("GETRESULTMODEL");
        Map<String, Object> model1 = new HashMap<>();
        model1.put("template", resultTemplate);

        Path userDir= Paths.get(System.getProperty("user.dir"));
        String outputdir1 = userDir.resolve(outputDir).toString();
        String experimentDir1 = userDir.resolve(experimentDir).toString();

        List<String>[] resFileList = new List[1];
        List<String>[] setupFileList = new List[1];
        try {
            resFileList[0] =Files.list(Paths.get(new File(outputdir1).getAbsolutePath().replace("//","/")))
                    .filter(f -> Files.isRegularFile(f) && f.toString().endsWith(".csv") ).map(f->outputDirName+"/"+f.getFileName().toString()).collect(Collectors.toList());
            setupFileList[0] = Files.list(Paths.get(new File(experimentDir1).getAbsolutePath().replace("//","/")))
                    .filter(f -> Files.isRegularFile(f) && f.toString().endsWith(".json")).map(f->experimentDirName+"/"+f.getFileName().toString()).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<String> failedExperiments = new LinkedList<>();
        setupFileList[0].stream().filter(sfn->!resFileList[0].contains(sfn.replace(".json",".csv").replace(experimentDirName+"/",outputDirName+"/"))).forEach(failedExperiments::add);
        failedExperiments.stream().forEach(sfn->setupFileList[0].remove(sfn));
        Collections.sort(resFileList[0]);
        Collections.sort(setupFileList[0]);
        model1.put("failed",failedExperiments);
        model1.put("filename",saveFileName);
        model1.put("resfilelist",resFileList[0]);
        model1.put("setupfilelist",setupFileList[0]);

        return model1;
    }

    /**
     * param_names -> all the dependencies
     ;;range_secondParam_div_paramdiv_dep_secondParam3

     dependency_ids -> div containing a param
     ;firstParam_div;secondParam_div;param0_div

     param_range_div_ids -> all the ranges (for any parameters)
     ;firstParam_div_paramdiv_dep_firstParam3;secondParam_div_paramdiv_dep_secondParam3;;param0_div_paramdiv_dep_param05

     * @param request
     * @param alg_name
     * @return
     */

    public static List<Param> readParams(Request request,String alg_name) throws ScriptException {

        String param_div_range_idsStr = request.queryParams("param_range_div_ids");
        String[] param_div_range_ids = param_div_range_idsStr.split(";");
        String param_div_idsString = request.queryParams("dependency_ids");
        String param_div_ids[] = param_div_idsString.split(";");
        String dep_div_idsString = request.queryParams("param_names");
        String dep_div_ids[] = dep_div_idsString.split(";");

        List<Param> paramList = new LinkedList<>();

        if(alg_name!=null) {
            param_div_ids = Arrays.stream(param_div_ids)
                    .filter(pn -> pn.contains(alg_name)).toArray(String[]::new);
            param_div_range_ids = Arrays.stream(param_div_range_ids)
                    .filter(pn -> pn.contains(alg_name)).toArray(String[]::new);
        }
        String name_param_id_map_string = request.queryParams("name_param_id_map");
        Map<String,String> param_id_name_map = new HashMap<>();
        if(name_param_id_map_string!=null&&!name_param_id_map_string.isEmpty()) {
            String entries[] = name_param_id_map_string.split(",");
            for (String s : entries)
                if (!s.isEmpty()) {
                    String[] s1s = s.split(":");
                    param_id_name_map.put(s1s[0], s1s[1]);
                }
        }
        //iterate over all params - paramdiv one created to describe a param
        for(String pdn : param_div_ids){
            if(!pdn.equals("")){ // skip the removed ones
                // we expect something like "param1_div", where param1 is the original name, but at optimizer.algorithms <algname>_<paramname>_div-> need de middle one
                String paramId =  pdn.replace("_div","");
                paramId = paramId.replace(alg_name+"_","");

                //String[] pdn_parts = pdn.split("_");
                //String paramId = pdn.split("_")[pdn_parts.length-2];
                String paramname = request.queryParams(pdn + "paramname");
                String typeStr = request.queryParams(pdn + "_type");
                String typePostFix = getTypePostFix(typeStr);

                boolean func = typePostFix.equals("_func");
                //iterate over all range given for any param
                for(String prdn : param_div_range_ids) {
                    //filter what belongs to the param we are examining
                    // this disaster for finding proprly one letter parameters, and the same for alg params where we start with alg name
                    String[] prdl_part_list = prdn.split("_");
                    String[] x = new String[prdl_part_list.length-1];
                    for( int i =1; i <prdl_part_list.length;++i)
                        x[i-1] = prdl_part_list[i];

                    String prdn_without_alg = String.join("_",x);
                    // should be something like n
             //       if (!prdn.equals("") && (prdl_part_list[0].equals(paramId)||prdl_part_list[0].equals(paramId)) ) {
                    if (!prdn.equals("") && (prdn.startsWith(paramId+'_')||prdn_without_alg.startsWith(paramId)) ) {
                        System.out.println("PRDN: "+prdn);
                        System.out.println("PDN:"+pdn);
                        System.out.println("PNAME:"+paramname);
                        String s = paramname;
                        System.out.println(s);
                        String valStr = "";
                        String funcRunningCountStr = "";
                        if(func){
                            valStr = request.queryParams(prdn + "_value_string"+typePostFix);
                            funcRunningCountStr = request.queryParams(prdn + "_value_int"+typePostFix);
                        }
                        else
                            valStr = request.queryParams(prdn + "_value"+typePostFix);
                        String funcRunningCount = request.queryParams(prdn + "_value"+typePostFix);

                        String lowerStr = request.queryParams(prdn + "_lower"+typePostFix);
                        System.out.println(lowerStr);
                        String upperStr = request.queryParams(prdn + "_upper"+typePostFix);
                        String enumStr = request.queryParams(pdn + "_enum_descriptor");
                        System.out.println(upperStr);
                        Param<?> param = null;


                        //looks for the param in the list, whether we have added before
                        for(Param par : paramList)
                            if(par.getName().equals(paramname))
                                param = par;


                        // checking whether the given range of the param is bounded or not = is there any dependency whose id contains the id of the range
                        boolean foundDep = false;
                        List<String> dep_ids = Arrays.stream(dep_div_ids).filter(str->str.contains(prdn)).collect(Collectors.toList());
                        for(String s1 : dep_ids) {
                            if(s1.equals(""))
                                continue;
                            foundDep = true;
                            //here we found a dependency
                            String dependencyParamId =  request.queryParams(s1 +"_select_other_name");
                            String dependencyParamName =  param_id_name_map.get(dependencyParamId);


                            Param dependencyParam = null;
                            //checking out whether the founded dependeny have been added to the paramlist
                            for(Param p1 : paramList)
                                if(p1.getName().equals(dependencyParamName))
                                    dependencyParam = p1;
                            //there is the problem, that we dont know its type here..
                            if(dependencyParam==null)
                                dependencyParam = new DummyParam(dependencyParamName);

                            //here we could find if we iterate over all the possibilities
                            String deplowerStr = "";
                            String depupperStr = "";
                            for(Class<?> t : Param.allowedClasses) {
                                String typePostFix1 = getTypePostFix(t.getCanonicalName());
                                deplowerStr = request.queryParams(s1 + "_dep_lower" + typePostFix1);
                                depupperStr = request.queryParams(s1 + "_dep_upper" + typePostFix1);
                                if(deplowerStr!=null)
                                    break;
                            }
                            if(deplowerStr==null)
                                throw new RuntimeException("not implemented!!");

                            // the examined param is not in the list-> we create it with its default range
                            if (param == null) {
                                param = createNewParamWithoutDepencency(paramname, valStr, typeStr, enumStr, lowerStr, upperStr, funcRunningCountStr);
                            }
                            else{
                                createNewParamRangeWithoutDepencency(param,paramname, valStr, typeStr, enumStr, lowerStr, upperStr, funcRunningCountStr);
                            }

                            //by now we added the param, now we can add dependency - a dummy now
                            addDependencyToNotBoundedParam(param,lowerStr,upperStr, dependencyParam,  depupperStr,deplowerStr);
                            if(!paramList.contains(param))
                                paramList.add(param);

                        }
                        //there is no dependency for the param so we add param to the list
                        if(!foundDep) {
                            if (param == null || param instanceof DummyParam) {
                                //this branch is for handling dummy params of optimizers e.g. in GridSearch parameters ar created for each variables, but some of those are unreachable..
                                if(valStr == null && enumStr==null && lowerStr==null && upperStr==null)
                                    param = new DummyParam(paramname);
                                else
                                    param = createNewParamWithoutDepencency(paramname, valStr, typeStr, enumStr, lowerStr, upperStr,funcRunningCountStr);
                                updateDependencies(paramList,param);

                                if(!paramList.contains(param))
                                    paramList.add(param);
                            }
                            else{
                                //we come here when we add a default range to a bounded parameter = when bounding param is not in its range specified to bound this

                                param.addDependency1(lowerStr,upperStr,valStr,funcRunningCountStr);

                            }

                        }

                    }
                }
            }
        }
        return paramList;
    }

    public static ObjectiveContainer readObjectives(Request request) {
        List<Objective> olist = new LinkedList<Objective>();
        String object_names = request.queryParams("object_names");
        String[] obj_div_names = object_names.split(";");
        for(String obj_div_name : obj_div_names){
            if(obj_div_name.equals(""))
                continue;
            String obj_name = request.queryParams(obj_div_name + "_name");
            String obj_relation = request.queryParams(obj_div_name + "_relation");
            String obj_weight = request.queryParams(obj_div_name + "_weight");
            String obj_value = request.queryParams(obj_div_name + "_relation_value");
            String obj_type = request.queryParams(obj_div_name + "_type");

            Objective objective = null;
            Relation rel = Relation.valueOf(obj_relation);
            boolean targetMakesSense = rel != Relation.MAXIMIZE && rel != Relation.MINIMIZE;
            if (obj_type.equals("java.lang.Integer"))
                objective = new Objective(Relation.valueOf(obj_relation),false,obj_name,null,obj_value==null||!targetMakesSense?0:new Float(Float.parseFloat(obj_value)).intValue(),0,Float.parseFloat(obj_weight));
            else if (obj_type.equals("java.lang.Float"))
                objective = new Objective(Relation.valueOf(obj_relation),false,obj_name,null,obj_value==null||!targetMakesSense?0.0:Float.parseFloat(obj_value ),0.0f,Float.parseFloat(obj_weight));

            if(!olist.contains(objective))
                olist.add(objective);


        }

        return new ObjectiveContainer(olist);
    }

    private static String getTypePostFix(String typeName ) {
        if(typeName.equals(Integer.class.getName()))
            return "_int";
        if(typeName.equals(Float.class.getName()))
            return "_float";
        if(typeName.equals(Boolean.class.getName()))
            return "_bool";
        if(typeName.equals("Function"))
            return "_func";
        return "_string";
    }
    // we defined the param properly, so if there is any depending on this, we bound it to this
    private static void updateDependencies(List<Param> paramList, Param<?> param) {
        for(Param<?> p : paramList)
            p.updateDependencies(param);


    }

    //why in the hell we need this????
    private static void addDependencyToNotBoundedParam(Param<?> param, String loweStr,String upperStr,Param dependencyParam, String depupperStr, String deplowerStr) {
        Range r =null;
        if(param.isEnumeration() && param.getParamGenericTypeName().equals("java.lang.Float"))
            r = new Range(param.getAllValueArray());
        else{
            switch (param.getParamGenericTypeName()){
                case "java.lang.String" : r = new Range(param.getAllValueArray(),upperStr,loweStr);break;
                case "java.lang.Float" : r = new Range(Float.parseFloat(upperStr), Float.parseFloat(loweStr));break;
                case "java.lang.Integer" : r = new Range(Integer.parseInt(upperStr), Integer.parseInt(loweStr));break;
                case "java.lang.Boolean" : r = new Range(Boolean.parseBoolean(loweStr), Boolean.parseBoolean(loweStr));break;
            }
        }
        String paramTypeString = dependencyParam.getParamGenericType().getName();
        if(dependencyParam.isEnumeration() && paramTypeString== "java.lang.Float")
            throw new ImplementationException("Branch does not support function param - addDependencyToNotBoundedParam");
        switch (paramTypeString) {
            case "java.lang.String":
                //String[] values = enumStr.split(",;");
                //this is dummy most probably!!!
                param.addDependencyToNotBoundedRange(r,dependencyParam,deplowerStr,depupperStr);
                break;
            case "java.lang.Boolean":
                // TODO: 2018. 02. 25. now we put the same value to both places
                param.addDependencyToNotBoundedRange(r,dependencyParam,Boolean.parseBoolean(deplowerStr),Boolean.parseBoolean(deplowerStr) );
                break;
            case "java.lang.Float":
                param.addDependencyToNotBoundedRange(r,dependencyParam,Float.parseFloat(deplowerStr),Float.parseFloat(depupperStr) );
                break;
            case "java.lang.Integer":
                param.addDependencyToNotBoundedRange(r,dependencyParam,Integer.parseInt(deplowerStr),Integer.parseInt(depupperStr) );
                break;

        }
    }

    @Deprecated
    private static void addDefaultRange(String typeStr, String lowerStr, String upperStr, String enumStr, Param<?> param) throws ScriptException {
        switch (typeStr) {
            case "Enum": {
                String[] values = enumStr.split(",;");
                Param<String> param1 = (Param<String>) param;
                param1.addDependency(values);
            }
            break;
            case "Boolean": {
                // dummy falses
                Param<Boolean> param1 = (Param<Boolean>) param;

                param1.addDependency(Boolean.FALSE, Boolean.FALSE);
            }break;
            case "Float": {
                Param<Float> param1 = (Param<Float>) param;

                param1.addDependency(Float.parseFloat(lowerStr), Float.parseFloat(upperStr));
            }break;
            case "Integer":{
                Param<Integer> param1 = (Param<Integer>) param;

                param1.addDependency(Integer.parseInt(lowerStr), Integer.parseInt(upperStr));
            }break;

        }
    }

    private static Param<?> createNewParamWithoutDepencency(String paramname, String valStr, String typeStr, String enumStr, String lowerStr,String upperStr,String funcRunningCountStr) {
        Param<?> param = createParamFromStringAttributes(paramname, valStr, typeStr, enumStr, lowerStr, upperStr, funcRunningCountStr);
        return param;
    }

    private static void  createNewParamRangeWithoutDepencency(Param p,String paramname, String valStr, String typeStr, String enumStr, String lowerStr,String upperStr,String funcRunningCountStr) {
        Param<?> param = createParamFromStringAttributes(paramname, valStr, typeStr, enumStr, lowerStr, upperStr, funcRunningCountStr);
        p.getDependencies().add((ParameterDependency)param.getDependencies().get(0));
    }

    private static Param<?> createParamFromStringAttributes(String paramname, String valStr, String typeStr, String enumStr, String lowerStr, String upperStr, String funcRunningCountStr) {
        Param<?> param = null;
        switch (typeStr) {
            case "Enum":
                String[] values = enumStr.split("[,;]");
                param = new Param<String>(valStr, values, lowerStr,upperStr,paramname);
                break;
            case "java.lang.Boolean":
                param = new Param<Boolean>(Boolean.parseBoolean(valStr), Boolean.TRUE, Boolean.FALSE, paramname);
                break;
            case "java.lang.Float":
                param = new Param<Float>(Float.parseFloat(valStr), Float.parseFloat(upperStr), Float.parseFloat(lowerStr), paramname);
                break;
            case "java.lang.Integer":
                param = new Param<Integer>(Integer.parseInt(valStr),Integer.parseInt(upperStr),Integer.parseInt(lowerStr), paramname);
                break;

            case "Function":
                try {
                    param = new FunctionParam(paramname,valStr,Integer.parseInt(funcRunningCountStr));
                } catch (ScriptException e) {
                    e.printStackTrace();
                }

                break;

        }
        return param;
    }
}
