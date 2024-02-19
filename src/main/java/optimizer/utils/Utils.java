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

package optimizer.utils;


import optimizer.algorithms.AbstractAlgorithm;
import optimizer.exception.InvalidParameterValueException;
import optimizer.objective.Relation;
import optimizer.param.Param;
import optimizer.trial.IterationResult;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;


import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * Created by peterkiss on 01/04/17.
 */
public class Utils {

    public static boolean isWindows = System.getProperty("os.name")
            .toLowerCase().startsWith("windows");
    public static String platform_path_separator = File.separator.equals("/")?File.separator:File.separator+File.separator+File.separator+File.separator;
    /**
     * checks whether the to list are exchangeable, specifically for checking user defined optimizersetups
     * @param l1
     * @param l2
     * @return true if there are the same parameters of same name and with the same type
     */
    public static boolean correspondingParameterLists(List<Param> l1, List<Param> l2){
        if(l1 == null || l1.isEmpty()) {
            if (l2 == null || l2.isEmpty())
                return true;
            else
                return false;
        }
        else if(l2 == null|| l2.isEmpty())
            return false;
        if(l1.size()!=l2.size())
            return false;
        int numberOfMatches = 0;
        for(Param<?> p: l1) {
            for (Param<?> p2 : l2) {
                if (p.getName().equals(p2.getName())) {
                    // TODO: 2018. 02. 14. Double again at smbo whaaat?? 
                    // if (p.getParamTypeName().equals(p2.getParamTypeName())) {
                    numberOfMatches++;
                    break;
                    //}
                }
            }
        }
        if( numberOfMatches!=l1.size())
            System.out.println("x");
        return numberOfMatches==l1.size();

    }


    /**
     * Due to the limits of some Browsers, we set an upper bound on float values going to the GUI.
     */
    public static Float FLOAT_REDEFINED_MAX_VALUE = 1000000f;

    /**
     * Creates the relative path for the CSV result file of the optimization.
     * @param experimentName The name of the experiment
     * @param outputDir The path for a folder to store the results
     * @return The created relative path to the file
     */
    public static String getExpCSVFileName(String experimentName,String outputDir) {
        return outputDir+File.separator+experimentName+".csv";
    }

    /**
     * Creates the relative path for the JSON experiment file of the optimization.
     * @param experimentName The name of the experiment
     * @param experimentDir The path for a folder to store the experiment setups
     * @return The created relative path to the file
     */
    public static String getExpJSONFileName(String experimentName,String experimentDir) {
        return experimentDir +File.separator+experimentName+".json";
    }

    public static String[] getFilePathParts(String s){
        String separator = File.separator;
        while(s.startsWith(File.separator))
            s = s.substring(1);
        while(s.endsWith(File.separator))
            s = s.substring(0,s.length()-2);
        if(File.separator.equals("\\")) {
            s = s.replace(File.separator, File.separator + File.separator);
            separator+=File.separator;
        }
        String[] test = s.split(separator);
        /*File f = new File(s);
        LinkedList<String> result = new LinkedList<>();
        result.push(f.getName());

        while(!s.startsWith(result.peek()))
            result.push(f.getParent());
        return result.toArray(new String[result.size()]);*/
        return test;
    }

    /**
     * Returns the user defined  name of the experiment from the given save file name.
     * @param saveFileNameWithPath User defined experiment filename
     * @return The inferred name of the experiment
     */
    public static String getExperimentName(String saveFileNameWithPath){
        String[] fnparts = getFilePathParts(saveFileNameWithPath);//saveFileNameWithPath.split(Utils.platform_path_separator);
        String[] saveFileNameParts = fnparts[fnparts.length-1].split("\\.");
        return saveFileNameParts[0];
    }

    /**
     * Checks whether there is an experiment file in the experiment directory, and if so modifies and returnes the user defined name by adding a timestamp.
     * @param saveFileName User defined experiment filename.
     * @param experimentDir The path for a folder to store the experiment setups.
     * @return Unique name for an experiment.
     */
    public static String getExperimentUniqueName(String saveFileName,String experimentDir) {
        String experimentName = getExperimentName( saveFileName);
        String experimentFileName=getExpJSONFileName(experimentName,experimentDir);
        // TODO: 2018. 11. 11. remove
        Path userDir= Paths.get(System.getProperty("user.dir"));
        String experimentFileName1 = userDir.resolve(experimentFileName).toString();
        if(new File(experimentFileName1).exists()) { //here can be a problem
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH-mm-ss");
            experimentFileName = experimentFileName.replace(".json","_" + dateFormat.format(new Date())+".json");
        }
        return getExpJSONFileName(getExperimentName(experimentFileName),experimentDir);
    }
    // TODO: 16/05/17 hacked loading paths, qualified names
    // TODO: 10/08/17 Should be run on server

    /**
     * Finds the available  subclasses of parameter of type {{@link java.lang.Class}}, then collect them in a {@link java.util.Map},
     * key:  {@link java.lang.Class } that  extends {@code toFind},
     * value: setup files for the given class with the same name - to be removed.
     * @param toFind {@link java.lang.Class }, whose subclasses we want to find .
     * @param optimizerClassLocation the location where we want to search for the subclasses.
     * @param <T> {@link java.lang.Class } the super class ehose descendant we are curious of.
     * @return
     * @throws IOException
     */
    public static <T> Map<Class<? extends T>, String> findAllMatchingTypes(Class<T> toFind,String optimizerClassLocation) throws IOException {
        Map<Class<? extends T>, String> foundClasses = new HashMap<Class<? extends T>, String>();
        System.out.println("CLASSPATH = " + System.getProperty("java.class.path"));
        System.out.println(System.getProperty("user.dir"));
        // TODO: 2018. 10. 03. hack for classloader
        Class<T>[] toFind1 = new Class[1];

        URL url = new URL("file:"+optimizerClassLocation);
            ClassLoader cl = new URLClassLoader(new URL[]{url});
            try {
                toFind1[0] = (Class<T>) cl.loadClass("optimizer.algorithms.AbstractAlgorithm");
            }catch (ClassNotFoundException e){
                System.out.println("Superclass not found");
                return foundClasses;
            }
        Path userDir= Paths.get(System.getProperty("user.dir"));
        String s =     Paths.get(System.getProperty("user.dir")+File.separator+optimizerClassLocation+File.separator+"optimizer"+File.separator+"algorithms").toAbsolutePath().toString();
        String s1 = userDir.resolve(optimizerClassLocation+File.separator+"optimizer"+File.separator+"algorithms").toString();
        try(final Stream<Path> pathsStream = Files.walk(userDir.resolve(optimizerClassLocation+File.separator+"optimizer"+File.separator+"algorithms"))) {
            pathsStream.forEach(filePath -> {
                if (Files.isRegularFile(filePath)) {
                    if (filePath.toString().contains(".class")){
                        File f = filePath.toFile();
                        try {
                            //URL url = f.toURI().toURL();
                            //URL url = new URL("file:/Users/peterkiss/IdeaProjects/BBCom/modules/coordinator/target/classes/");
                            //System.out.println("URL: "+url.toString());
                            //URL[] urls = new URL[]{url};
                            //ClassLoader cl = new URLClassLoader(urls);
                            // TODO: 2018. 10. 02. remove
                            System.out.println("to load: optimizer.algorithms." + f.getName().split("\\.")[0]);
                            //Class optimizerClass = Class.forName("optimizer.algorithms." + f.getName().split("\\.")[0], true, cl);
                            Class optimizerClass = Class.forName("optimizer.algorithms." + f.getName().split("\\.")[0]);//cl.loadClass("optimizer.algorithms." + f.getName().split("\\.")[0]);
                            if(isImplementedAlgorithm( optimizerClass,toFind1[0]))
                                foundClasses.put(optimizerClass,null);
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }

                }
            });
        }
        findConfigFiles(foundClasses,optimizerClassLocation);
        return foundClasses;
    }

    /**
     * Checks the modifiers and super classes of a given reflected class, returns true if we found an instantiable subclass of {@link AbstractAlgorithm}
     * @param optimizerClass the {@link java.lang.Class} we want to examione
     * @return
     */
    public static boolean isImplementedAlgorithm(Class optimizerClass,Class<?> superclass){
        if(Modifier.isAbstract(optimizerClass.getModifiers()))
            return false;
        Class C = optimizerClass;
        while (C != null) {
            // TODO: 2018. 10. 03. HACK !!!!!!!!!!
//            if(C.equals(superclass))
           if(C.toString().equals(superclass.toString()))
                return true;
            C = C.getSuperclass();
        }
        return false;
    }

    /**
     * We are searching for custom setup files of extension '.config' at the location of the optimiter '.class' files., then we add as velues to foundClasses {@link Map}.
     * This custom config files are not really useful, so this will be removed.
     * @param foundClasses Input {@link Map}, with nulls at the values, and {@link AbstractAlgorithm} {@link Class} objects at keys.
     * @param optimizerClassLocation The location of the class files.
     * @param <T>
     */
    @Deprecated
    public static <T> void findConfigFiles(Map<Class<? extends T>, String> foundClasses,String optimizerClassLocation) {
        Path p = Paths.get(optimizerClassLocation);
        //System.out.println(p);
        if(!Files.exists(p))
            return;
        try(final Stream<Path> pathsStream = Files.walk(p)) {
            pathsStream.forEach( filePath -> {
                        if (Files.isRegularFile(filePath) && filePath.toString().contains(".config")) {
                            Optional<Class<? extends T>> cl =foundClasses.keySet().stream().filter(c -> filePath.toString().equals(c.getName())).findFirst();
                            if(cl.isPresent()) {
                                foundClasses.put(cl.get(), "");
                            }
                        }
                    }
            );

        }
        catch (IOException e) {
            e.printStackTrace();

        }
    }

    /**
     * Compares two {@link List}s of {@link Param}s to decide whether they correspond to each other.
     * @param p1 First {@link List} of {@link Param}s.
     * @param p2 Second {@link List} of {@link Param}s.
     * @return
     */
    public static boolean paramConfigsAreEqual(List<Param> p1,List<Param> p2){
        if(p1.size()!=p2.size())
            return false;
        return !p1.stream()
                .filter(
                        param -> !p2.stream().filter(
                                param1 -> param1.equals(param)&&param.getValue().equals(param1.getValue())
                        ).findFirst().isPresent()
                ).findFirst().isPresent();

    }

    /**
     * A method to execute JavaScript formulas. This will generate an {@link Float[]} executing the script iteratively on natural numbers.
     * @param function The script to be executed. One variable van be used in a formula, denoted by `$` prefix.
     * @param arrayLength The number of element to be generated.
     * @return The array of generated {@link Float}s of size arrayLength
     * @throws ScriptException
     */
    public static Float[] evalFunction(String function, int arrayLength) throws ScriptException {
        Float[] ret = new Float[arrayLength];
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("javascript");
        ScriptContext context = engine.getContext();
        StringWriter writer = new StringWriter();
        context.setWriter(writer);
        String variablePattern = "\\$[a-zA-Z0-9_]*";

        for(Integer i = 0;i<arrayLength;++i ) {
            String toEval = function.replaceAll(variablePattern, i.toString());
            ret[i] = ((Number) engine.eval(toEval)).floatValue();
        }
        for(Integer i = 0;i<arrayLength;++i ) {
            if(ret[i].equals(Float.POSITIVE_INFINITY))
                ret[i] = FLOAT_REDEFINED_MAX_VALUE;
            else if(ret[i].equals(Float.NEGATIVE_INFINITY))
                ret[i] = Float.MIN_VALUE;
        }
        return ret;
    }
    //// TODO: 21/09/17 a huge hack..
    public static double compareNumbers(Number a, Number b){
        if(a==null || b == null)
            throw new InvalidParameterValueException("Null in number comparision ");
        double diff = a.doubleValue() - b.doubleValue();
        if(Math.abs(diff)<0.00001) return 0;
        return diff ;
       /* if(a == null && b == null)
            return 0;
        if(a == null)
            return -1;
        if(b==null)
            return 1;
        try {
            return new BigDecimal(a.toString()).compareTo(new BigDecimal(b.toString()));
        /
        catch (Exception e){
            //// TODO: 20/09/17 hack
            if(a.equals(Float.MAX_VALUE)) return 1;
            if(b.equals(Float.MAX_VALUE)) return -1;

        }
        return 0;*/

    }

    /**
     * Method for deciding, whether a numeric value is betwee two other values.
     * @param value The value to examine.
     * @param upper Upper bound.
     * @param lower Lower bound
     * @param <T> The actual type of the value and the boundaries.
     * @return
     */
    @Deprecated
    public static <T extends  Number> boolean comply (T value , T upper, T lower){
        return Utils.compareNumbers(value,upper)<0 && Utils.compareNumbers(lower,value)<0;

    }

    /**
     * Corresponds to comply on numners for {@link java.lang.Boolean}s, defined as TRUE if the value equals the upper bound.
     * @param value
     * @param upper
     * @param lower
     * @param <Boolean>
     * @return
     */
    @Deprecated
    public static <Boolean> boolean comply (Boolean value , Boolean upper, Boolean lower){
        return value.equals(upper);

    }
    @Deprecated
    public static  boolean comply (String value , String upper, String lower){
        return value.equals(upper);

    }

    /**
     * Decides whether the  termination condition has met, if there is any.
     * @param relation The type of the {@link optimizer.objective.Objective}
     * @param value The actual value of the {@link optimizer.objective.Objective}
     * @param target The target value of the {@link optimizer.objective.Objective}, it can be a limit to reach or a threshold for differences between succeeding values-
     * @param lastValue The previous value of the {@link optimizer.objective.Objective}, needed to examine convergence.
     * @param <T> The generic type of the {@link optimizer.objective.Objective}
     * @return
     */
    public static <T> boolean comply (Relation relation, T value, T target, T lastValue){

        if(value instanceof Number) {
            Float castedValue = ((Number) value).floatValue();

            Double castedThreshold = ((Number)target).doubleValue();
            double rel = Utils.compareNumbers(castedValue,castedThreshold);
            if (relation.equals(Relation.LESS_THAN) && rel < 0 )
                return true;
            if (relation.equals(Relation.GREATER_THAN) && rel > 0)
                return true;
            if (relation.equals(Relation.MINIMIZE_TO_CONVERGENCE) && lastValue!=null&& Math.abs(castedValue-((Number)lastValue).floatValue())<castedThreshold )
                return true;
            if (relation.equals(Relation.MAXIMIZE_TO_CONVERGENCE)  && lastValue!=null&& Math.abs(castedValue-((Number)lastValue).floatValue())<castedThreshold )
                return true;
        }
        return false;
    }

    /**
     * Decides,whether a String corresponds to an integer value.
     * @param  str {@link java.lang.String} to examine.
     * @return
     */
    public static boolean isInteger(String str) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        if (length == 0) {
            return false;
        }
        int i = 0;
        if (str.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < length; i++) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
    /**
     * Decides,whether a String corresponds to a float value.
     * @param  s Input {@link java.lang.String} to examine.
     * @return
     */
    public static boolean isFloat(String s)
    {
        try{
            float d = Float.parseFloat(s);
        }
        catch(Exception e){
            return  false;
        }
        return true;
    }

    /**
     * Decides,whether a String corresponds to a logical value.
     * @param  s Input {@link java.lang.String} to examine.
     * @return
     */
    public static boolean isBoolean(String s)
    {
        if(s.toUpperCase().trim().equals("FALSE")||s.toUpperCase().trim().equals("TRUE"))
            return true;
        return false;
    }
    @Deprecated
    public static void printParameters(Map<String,Param> params){
        for(Map.Entry<String,Param> entry : params.entrySet())
            System.out.println(entry.toString());

    }
    @Deprecated
    public static String printLandScape(List<IterationResult> landscape){
        String res = "";
        for(IterationResult ir :landscape){
            res += ir;
        }
        return res;
    }

    // TODO: 2018. 10. 08. ugly
    public static String publicIFPath() {
        //return "/Users/peterkiss/IdeaProjects/BBCom/modules/coordinator/public/";
        String jarPublicLocation = System.getProperty("user.dir")+File.separator+ "public";
        String resourcePublicLocation =System.getProperty("user.dir") +File.separator+"src" +File.separator+"main" +File.separator+"resources"+File.separator+ "public";

        String ret =  Files.exists(Paths.get(jarPublicLocation))?jarPublicLocation:resourcePublicLocation;
        return ret;
    }

    public static String normalizePath(String s) {
        String ret = System.getProperty("user.dir")+File.separator+s;
        String doubleSep = File.separator+File.separator;
        while (ret.contains(doubleSep))
            ret = ret.replace(doubleSep,File.separator);
        return ret;
    }

    public static boolean runningInJar(){

        String u = Utils.class.getProtectionDomain().getCodeSource().getLocation().getPath().toString();
        //System.out.println("Source url="+u);
        if(u.contains(".jar"))
            return true;
        return false;

    }

    public static String getSourceHome(){
        return Utils.class.getProtectionDomain().getCodeSource().getLocation().getPath();

    }

    /*public static String getPublicFolderLocation(){
        if(runningInJar())

            return Utils.class.getProtectionDomain().getCodeSource().getLocation().getPath();

    }*/
}
