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

package optimizer.trial;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.spotify.docker.client.exceptions.DockerException;
import optimizer.config.TestConfig;
import optimizer.exception.JSONReadException;
import optimizer.objective.ObjectiveContainer;
import optimizer.param.Param;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * This class is intended to execute one trial in a separated thread.
 * Created by peterkiss on 01/04/17.
 */
public class Trial implements Callable<IterationResult> {

    public static Trial deserializeTrial(String s){
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Trial.class, new TrialDeserializer());
        Gson gson = gsonBuilder.create();
        try {
            return gson.fromJson(s, Trial.class);
        }catch (Exception e){
            throw new JSONReadException("Error during deserialization of JSON - " +s);
        }


    }

    public DockerHandler.Container getContainer() {
        return container;
    }

    public void setContainer(DockerHandler.Container container) {
        this.container = container;
    }

    // TODO: 2019. 01. 07. probably put it elsewhere
    private DockerHandler.Container container;

    /**
     * A member specifying whether the executed process writes its results into a file or on the standard output
     * Since using the same filename in parallel executed instances of the process can be problematic now it is set up to false all the times.
     */
    private final Boolean useFile;
    /**
     * The command pattern, where recent values of the parameters will be inserted.
     */
    private String baseCommand;
    /**
     * Name of the output file of the executed process.
     */
    private String processOutputFileName;
    /**
     * This  member contains an instance of an {@link ObjectiveContainer}, storing a prototype of the objective values to be read at the end of the execution of the runned process.
     */
    private ObjectiveContainer pattern;
    /**
     * Stores the timestamp of starting moment of the process.
     */
    long startTime;
    /**
     * stores the time difference stemming from the possible interruption of optimization.
     */
    long delta;
    /**
     * The recent configuration of {@link Param}, with those the process will be run here.
     */
    List<Param> config;

    /**
     * working directory to execute the BBF.
     */
    String workingDir;
    public Trial(String baseCommand, boolean fileProcessOutPut, String processOutputFileName, ObjectiveContainer pattern, List<Param> config, long startTime, long delta, String workingDir){
        this.baseCommand = baseCommand;
        this.useFile = fileProcessOutPut;
        this.processOutputFileName = processOutputFileName;
        this.config= config;
        this.pattern = pattern;
        this.startTime = startTime;
        this.delta = delta;
        this.workingDir = workingDir;

    }

    /**
     * Here we execute the trial with parameters specified in {@link #config}.
     * @return The result of the trial.
     * @throws Exception
     */
    @Override
    public IterationResult call() throws Exception {
      return executeSequentially1();
    }

    public IterationResult executeSequentially1() throws InterruptedException, CloneNotSupportedException, IOException, DockerException {
        if(this.container == null){
            return executeSequentially();
        }
        BufferedReader outputReader, errorReader;

        String command = TestConfig.getCommand(this.config,baseCommand);
        String res = this.container.execute(command.split(" "),null);
        outputReader= new BufferedReader(new StringReader(res));
        System.out.println("RES = "+res);

        return new IterationResult(this.config, ObjectiveContainer.readObjectives(outputReader,null,this.pattern),startTime,delta);

    }
    /**
     * Plainsequential execution of the BBF with parameters specified in {@link #config}.
     * @return The result of the trial.
     */
    public IterationResult executeSequentially() throws InterruptedException, IOException, CloneNotSupportedException {
        String command = TestConfig.getCommand(this.config,baseCommand);
        BufferedReader outputReader, errorReader;
        //Runtime rt = Runtime.getRuntime();
        ProcessBuilder builder = new ProcessBuilder( command.split(" "));
        System.out.println("WORKINGDIR SQAVed --"+this.workingDir);
        Path userDir= Paths.get(System.getProperty("user.dir"));
        System.out.println("userdir -- "+userDir);
        String s1 = userDir.resolve(this.workingDir).toString();
        System.out.println("s1 -- "+s1);

        // TODO: 2018. 12. 10. hack, in real distribution paths are messed up
        if (!command.contains("docker"))
            builder.directory(new File(s1).getAbsoluteFile());
        builder.redirectErrorStream(true);
        //builder.inheritIO();
        System.out.println("Executing1: "+builder.command());
        Process pr = null;
        //builder.redirectOutput();
        try {
            pr = builder.start();//= rt.exec(command);
        }catch (Exception e){
            e.printStackTrace();
        }
        int res = pr.waitFor();

        errorReader = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
        if(this.useFile) {
            FileReader fr = new FileReader(processOutputFileName);
            outputReader = new BufferedReader(fr);
        }
        else
            outputReader= new BufferedReader(new InputStreamReader(pr.getInputStream()));
        System.out.println("RES = "+res);

        return new IterationResult(this.config, ObjectiveContainer.readObjectives(outputReader,errorReader,this.pattern),startTime,delta);
    }
    //todo docker integration

    public void setBaseCommand(String baseCommand) {
        this.baseCommand = baseCommand;
    }

    public String getBaseCommand() {
        return baseCommand;
    }

    public String getRunnableName() {
        String fileToRun = getBaseCommand().trim();
        fileToRun=       fileToRun.split(" ")[1];
        return fileToRun.substring(fileToRun.lastIndexOf("/")+1,fileToRun.length());
    }

    /**
     *
     * @return the folder name within which the code to run and used files should be located.
     */
    public String getCodeHome(){
        String fileToRun = getBaseCommand().trim();
        fileToRun=       fileToRun.split(" ")[1];
        //here we return the folder, where the runnable file resides assuming all the necessary files are within that
        // remove the runnable
        String diroffileToRun =fileToRun.substring(0,fileToRun.lastIndexOf("/"));
        // removing path to containing folder
        diroffileToRun = diroffileToRun.substring(diroffileToRun.lastIndexOf("/")+1,diroffileToRun.length());
        System.out.println("FTR: "+fileToRun);
        System.out.println("DFTR: "+diroffileToRun);

        return diroffileToRun;
    }
}
