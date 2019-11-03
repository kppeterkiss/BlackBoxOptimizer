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
import com.google.gson.stream.JsonReader;
import optimizer.config.TestConfig;
import optimizer.config.TestConfigDeserializer;
import optimizer.exception.JSONReadException;
import optimizer.objective.Relation;
import optimizer.objective.Objective;
import optimizer.objective.ObjectiveContainer;
import optimizer.param.Param;
import optimizer.utils.Utils;

import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.IntStream;

/**
 * This class represents the important data regarding to a single run of the black box function:
 * the parameter setup and the corresponding objective values stored in a {@link java.util.List< Param >} and an {@link ObjectiveContainer}
 * Created by peterkiss on 20/04/17.
 */
public class IterationResult implements Comparable<IterationResult>{

    public static IterationResult deserializeIterationResults(String s){
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(IterationResult.class, new IterationResultDeserializer());
        Gson gson = gsonBuilder.create();
        try {
            return gson.fromJson(s, IterationResult.class);
        }catch (Exception e){
            throw new JSONReadException("Error during deserialization of JSON");
        }


    }

    /**
     * Recent parametrization of the BBF .
     */
    private List<Param> configuration;
    /**
     * Results of the run given the {@code configuration}.
     */
    private ObjectiveContainer objectives;
    /**
     * Start time of the optimization task.
     */
    private long timeStamp;

    /**
     * Getter for {@link #timeStamp}.
     * @return
     */
    public long getTimeStamp() {
        return timeStamp;
    }

    /**
     * Constructor:  creating a new IterationResult object by cloning Params and the ObjectiveContainer
     * @param scriptParameters
     * @param objectiveContainer
     * @param startime
     * @param delta
     * @throws CloneNotSupportedException
     */
    public IterationResult(List<Param> scriptParameters, ObjectiveContainer objectiveContainer,long startime, long delta) throws CloneNotSupportedException {
        this.setConfiguration(scriptParameters);
        this.setObjectives(objectiveContainer);
        this.timeStamp = System.currentTimeMillis()-startime+delta;
    }

    /**
     * Returns a copy of the {@link Param} setup of the given run (or {@link Trial})
     * @return Cloned {@link List} of {@link Param}s.
     * @throws CloneNotSupportedException
     */
    public List<Param> getConfigurationClone() throws CloneNotSupportedException {
        List<Param> list =  new LinkedList<Param>();
        for(Param p : configuration)
            list.add((Param) p.clone());
        return list;

    }

    /**
     * Sets the paramerter setup for the {@link IterationResult} object.
     * @param configuration the {@link List} of {@link Param}s, that will be cloned and stored in the {@link IterationResult}.
     * @throws CloneNotSupportedException
     */
    public void setConfiguration(List<Param> configuration) throws CloneNotSupportedException {
        List<Param> list =  new LinkedList<Param>();
        for(Param p : configuration)
            list.add((Param) p.clone());

        this.configuration = list;
    }

    /**
     * Helper to decide whether the given {@link Trial} runned with invalid {@link Param} setup, that is, some of the constraints were violated.
     * @return True is none
     * @throws CloneNotSupportedException
     */
    public boolean badConfig() throws CloneNotSupportedException {
        for(Objective o : this.objectives.getObjectiveListClone()) {
            try {
                if ((o.getRelation().equals(Relation.LESS_THAN) || (o.getRelation().equals(Relation.MINIMIZE))) && ((Number) o.getValue()).floatValue() == Float.MAX_VALUE)
                    return true;
                else if ((o.getRelation().equals(Relation.GREATER_THAN) || (o.getRelation().equals(Relation.MAXIMIZE))) && ((Number) o.getValue()).floatValue() == Float.MIN_VALUE)
                    return true;
            }catch (Exception e){
                // TODO: 2018. 02. 24.
                System.out.println("Bad objective value.");
            }
        }
        return false;

    }

    /**
     * CLones the stored {@link ObjectiveContainer} and returns the copy.
     * @return Clone of {@link #objectives}
     * @throws CloneNotSupportedException
     */
    public ObjectiveContainer getObjectiveContainerClone() throws CloneNotSupportedException {
        return (ObjectiveContainer)objectives.clone();
    }

    /**
     * Clones the {@link List} od {@link Objective}s received as argument and sets the internal {@link List} to it.
     * @param objectives
     * @throws CloneNotSupportedException
     */
    public void setObjectives(ObjectiveContainer objectives) throws CloneNotSupportedException {
        this.objectives = (ObjectiveContainer) objectives.clone();
    }

    /**
     * Gives a measure of how good the configuration is. That can be seen as weighted sum of distances from the goal values.
     * The bigger value of the fitness ALWAYS represents a better solution based on the objective
     * @return Sum  of {@link Objective#weight}} times a distance from the {@link Objective#target} if there is any, or in case {@link Relation#MAXIMIZE} or {@link Relation#MINIMIZE} what is bigger/smaller.
     * todo should apply some normalization,
     */
    public double getFitness() {
        double distance = 0;
        for(Objective o : objectives.getObjectiveListReference()) {
            if(o.getValue() instanceof Number) {
                double value = ((Number)o.getValue()).doubleValue();
                double target = ((Number)o.getTarget()).doubleValue();
                double weight = (double) o.getWeight();
                switch (o.getRelation()) {

                    case MAXIMIZE: //bigger value smaller distance
                        distance -= weight * value; break;
                    case MINIMIZE: //bigger value bigger distance
                        distance += weight * value;break;
                    case LESS_THAN: //if reached d = 0 otherwise the bigger the value means the bigger growth in dist
                        distance += value < target ? 0 :weight * Math.abs(value - target);break;
                    case GREATER_THAN: //if reached d = 0 otherwise the smaller the value means the bigger growth in dist
                        distance += value > target ? 0 : weight * Math.abs(target - value);break;
                    case MAXIMIZE_TO_CONVERGENCE:
                    case MINIMIZE_TO_CONVERGENCE:{
                        double lastValue = ((Number)o.getLastvalue()).doubleValue();
                        double step = Math.abs(value-lastValue);
                        distance += step < target ? 0 : weight * step;
                    } break;
                }
            }
        }
        return -distance;
    }

    /**
     * A comparision over {@link IterationResult} objects, that uses getFitness method to decide which configuration has better results.
     * @param other The {@link IterationResult} we compare the this object to.
     * @return true, if this is better than the parameter {@link IterationResult}
     * @throws CloneNotSupportedException
     */
    public boolean betterThan(IterationResult other) throws CloneNotSupportedException {
        if(other == null)
            return true;
        return getFitness() > other.getFitness();
    }

    public String getConfigurationString() throws CloneNotSupportedException {
        String[] header = getCSVHeaderString().split(",");
        String[] values = getCSVString().split(",");
        String res[] = new String[header.length];

        IntStream.range(0,header.length).forEach(i->res[i]=header[i]+" -> "+values[i]);
        return String.join("; ",res);
    }

    public String getCSVString() throws CloneNotSupportedException {
        StringJoiner sj = new StringJoiner(",");
        sj.add(String.valueOf(this.timeStamp));
        for(Param p:configuration)
            sj.add(p.getValue().toString());
        for(Objective o : objectives.getObjectiveListClone())
            sj.add(o.getValue()!=null?o.getValue().toString():"0");
        return  sj.toString();
    }


    /**
     * Generates a coma separated {@link String} representing the headers of the iteration results. This will be written in the result file as first line.
     * @return Coma separated {@link String} enumerating the id of iteration, the timestamp, than values of {@link Param}s and values of {@link Objective}s
     * @throws CloneNotSupportedException
     */
    public String getCSVHeaderString() throws CloneNotSupportedException {
        StringJoiner sj = new StringJoiner(",");
        sj.add("timestamp");
        for(Param p:configuration)
            sj.add(p.getName());
        for(Objective o : objectives.getObjectiveListClone())
            sj.add(o.getName());
        return  sj.toString();
    }


    @Override
    public String toString() {
        return "IterationResult{" +
                "configuration=" + configuration +
                ", fitness=" + objectives +
                '}';
    }

    /**
     * Defines an ordering over {@link IterationResult}s inn terms of execution time. Necessary for convergence graph, since with multithreading the order of getting the results can differ from order of submission of {@link Trial}s.
     * @param other The other {@link IterationResult} to comparison.
     * @return An integer that is 0< if this was executed later than the other and 0> if the other was the first and 0 if therewas submitted at the same moment(theoretical option).
     */
    @Override
    public int compareTo(IterationResult other) {
        return (int)(this.timeStamp-other.timeStamp);
    }

    public List<Param> getConfiguration() {
        return configuration;
    }
}
