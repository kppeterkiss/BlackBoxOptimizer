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

package optimizer.docker;

import optimizer.utils.Utils;

import java.io.*;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * This class is intended to handle docker container
 */
public class DockerWrapper {

    Boolean done = false;
    String location;
    String name;

    public String getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }

    public DockerWrapper(String location){
        this.location = location;
        this.name = location.substring(location.lastIndexOf("/")+1,location.length()).toLowerCase();
        //for win
        this.name = this.name.substring(this.name.lastIndexOf(File.separator)+1,this.name.length()).toLowerCase();

    }
    public boolean build() throws IOException, InterruptedException {
        String homeDirectory = System.getProperty("user.home");
        Process process;
        /*if (Utils.isWindows) {
            // TODO: 2018. 11. 09.
            String command = String.format("cmd.exe build -t %s ./%s", homeDirectory)
            process = Runtime.getRuntime()
                    .exec(command);
        } else */{
            System.out.println("User.home: "+ homeDirectory);
            System.out.println("User.dir: "+ System.getProperty("user.dir"));
            String command = String.format("docker build -t %s %s", this.name,this.location);
            String[] command1 = { "/bin/bash", "-c","docker","build","-t",this.name,this.location};
            System.out.println(" to build docker : "+command);

            process = Runtime.getRuntime()
                    .exec(command1);
        }
        StreamGobbler streamGobbler =
                new StreamGobbler(process.getInputStream(), System.out::println);
        Executors.newSingleThreadExecutor().submit(streamGobbler);
        int exitCode = process.waitFor();
        done = true;
        // TODO: 2018. 11. 09. i do nothing with this
        assert exitCode == 0;
        return exitCode == 0;
        
    }

    public String getCommand(String command){
        while (!done){;}
        String[] parts = command.trim().split(" ");
        String com = parts[0];
        int execStartIdx = parts[1].lastIndexOf("/");
        if(execStartIdx == -1)
            execStartIdx = parts[1].lastIndexOf("\\");
        String executable =  parts[1].substring(parts[1].lastIndexOf("/")+1,parts[1].length());
        System.out.println("EXECUTABLE: "+executable);
        String leftover = "";
        for (int i = 2; i <parts.length ;  i++) {
            leftover+=" "+parts[i];
        }
        String command2 = "/bin/bash -c docker run "+name+" "+ com + " "+executable+" "+leftover;
        System.out.println("Running: "+command2);

        return command2;

    } public String getCommand1(String command){
       // while (!done){;}
        String[] parts = command.trim().split(" ");
        String com = parts[0];
        int execStartIdx = parts[1].lastIndexOf("/");
        if(execStartIdx == -1)
            execStartIdx = parts[1].lastIndexOf("\\");
        String executable =  parts[1].substring(parts[1].lastIndexOf("/")+1,parts[1].length());
        System.out.println("EXECUTABLE: "+executable);
        String leftover = "";
        for (int i = 2; i <parts.length ;  i++) {
            leftover+=" "+parts[i];
        }
        String command2 = com + " "+executable+" "+leftover;
        System.out.println("Running: "+command2);

        return command2;

    }
    private static class StreamGobbler implements Runnable {
        private InputStream inputStream;
        private Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines()
                    .forEach(consumer);
        }
    }
}
