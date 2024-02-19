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

package optimizer.objective;


import optimizer.exception.ImplementationException;
import optimizer.main.Main;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Wrapper class for the list of {@link Objective}s of an optimization task.
 * Created by peterkiss on 01/04/17.
 */
public class ObjectiveContainer implements Cloneable {


    public ObjectiveContainer() {
        this.objectives = new LinkedList<>();
    }

    @Override
    public String toString() {
        return "ObjectiveContainer{" +
                "objectives=" + objectives +
                '}';
    }

    /**
     * Constructor for {@link ObjectiveContainer}.
     * @param objectives The {@link List} of {@link Objective}s that will be wrapped by the object.
     */
    public ObjectiveContainer(List<Objective> objectives) {
        this.objectives = objectives;
    }

    /**
     * Returnes a clone of the stored {@link List} of  {@link Objective}s
     * @return A clone of {@link #objectives}.
     * @throws CloneNotSupportedException
     */
    public synchronized List<Objective> getObjectiveListClone() throws CloneNotSupportedException {
        List<Objective> copy = new LinkedList<>();
        for(Objective o : this.objectives)
            copy.add((Objective) o.clone());
        return copy;
    }

    /**
     * A reference for the list {@link Objective}s.
     */
    public List<Objective> getObjectiveListReference() {
        return this.objectives;
    }

    /**
     * {@link List} of stored {@link Objective}s
     */
    private List<Objective> objectives;

    /**
     * Method to specify whether the optimization task is terminated that is, if one of the terminating condition, or all of the the condition  has been met.
     * @return Boolean telling if  the composed termination condition has met or not.
     */
    public boolean terminated(){
        if(objectives.size() == 0)
            return false;
        boolean result = true;
        for(Objective e : objectives) {
            result &= e.met();
            if (e.terminator)
                return true;
        }
        return result;
    }

    /**
     * Sets a value very far from the target. The goal of applying this method is to indicate to the optimizer algorithm that the proposed setup is not feasible according to the constraints of the {@link optimizer.param.Param}s.
     * @param lastObjectiveContainer An {@link ObjectiveContainer} reference in which we set the current values of the objective.
     * @return
     */
    public static ObjectiveContainer setBadObjectiveValue( ObjectiveContainer lastObjectiveContainer){
        lastObjectiveContainer.objectives.stream().forEach(
                objective -> {
                    if(objective.getTarget() instanceof Float) {
                        if(objective.getRelation().equals(Relation.GREATER_THAN)||objective.getRelation().equals(Relation.MAXIMIZE)||objective.getRelation().equals(Relation.MAXIMIZE_TO_CONVERGENCE))
                            objective.setValue(Float.MIN_VALUE);
                        else if(objective.getRelation().equals(Relation.LESS_THAN)||objective.getRelation().equals(Relation.MINIMIZE)||objective.getRelation().equals(Relation.MINIMIZE_TO_CONVERGENCE))
                            objective.setValue(Float.MAX_VALUE);
                        else
                            throw new ImplementationException("Relation "+objective.getRelation()+ "is not supported." );
                    }

                   else if(objective.getTarget() instanceof Integer) {
                        if(objective.getRelation().equals(Relation.GREATER_THAN)||objective.getRelation().equals(Relation.MAXIMIZE)||objective.getRelation().equals(Relation.MAXIMIZE_TO_CONVERGENCE))
                            objective.setValue(Integer.MIN_VALUE);
                        else if(objective.getRelation().equals(Relation.LESS_THAN)||objective.getRelation().equals(Relation.MINIMIZE)||objective.getRelation().equals(Relation.MINIMIZE_TO_CONVERGENCE))
                            objective.setValue(Integer.MAX_VALUE);
                        else
                            throw new ImplementationException("Relation "+objective.getRelation()+ "is not supported." );

                    }
                    else  throw new ImplementationException("Objective value of type "+objective.getClass().getCanonicalName()+ "is not supported." );


                }
        );
        return lastObjectiveContainer;

    }

    /**
     * Static method to read output of the algorithm to be optimized. The method reads Either a file that contains outputs of the  black box function or its standard output. Above these if #errorReader is not null, reads the standard error too. The readed obejective values then will be converted and written the {@link ObjectiveContainer} object, that will be returned.
     * @param resultBufferedReader A {@link BufferedReader} object, from which we read the output of the black box function.
     * @param errorReader A {@link BufferedReader} Used to read error messages potentially raised during the execution of the black box function.
     * @param lastObjectiveContainer An {@link ObjectiveContainer} reference in wich we set the current values of the objective.
     * @return An {@link ObjectiveContainer} containing the successfully identified {@link Objective} values.s
     * @throws IOException
     */
    public static  ObjectiveContainer readObjectives(BufferedReader resultBufferedReader, BufferedReader errorReader,ObjectiveContainer lastObjectiveContainer) throws IOException {


        List<String> objNameList = lastObjectiveContainer.objectives.stream().map(o->o.getName()).collect(Collectors.toList());
        Map<String,String> map = new HashMap<>();
        for (String sn : objNameList) map.put(sn,null);
        String objLine = "";
        while ((objLine = resultBufferedReader.readLine())!=null) {
            Main.getLogger().info("Readed from sout of runned alg: "+ objLine);
            if(objLine.trim().split(" ").length==2) {
                Scanner s1 = new Scanner(objLine);
                String objectiveName = s1.next().trim();
                String objectiveValue = s1.next().trim();
                if(map.containsKey(objectiveName)) {
                    map.put(objectiveName, objectiveValue);
                    Main.getLogger().info("Objective recognized: "+ objLine);
                }
            }
        }
        if(errorReader!=null) {
            while ((objLine = errorReader.readLine()) != null) {
                Main.getLogger().info( "Readed from stderr of runned alg: " + objLine);
            }
        }
        for(Map.Entry<String,String> element : map.entrySet()) {
            String objectiveValue = element.getValue();
            String objectiveName = element.getKey();
            //  if objective is not specified so basically we aren't interested in obj file-entry handle elsewhere
            Class<?> objType = null;
            Optional<Objective> o = lastObjectiveContainer.objectives.stream().filter(e -> objectiveName.equals(e.getName())).findFirst();
            if (o.isPresent())
                objType = o.get().getType();
            else
                continue;
            if(objectiveValue == null) {
                Main.getLogger().info("Missing objective: " + objectiveName);
                objectiveValue = "0";
            }
            Double readedval = Double.parseDouble(objectiveValue);
            if (objType.equals(Integer.class)) {

                ((Objective<Integer>) o.get()).setValue(readedval.intValue());
            }
            else if (objType.equals(Float.class) ) {
                ((Objective<Float>) o.get()).setValue(readedval.floatValue());
            }

            else
                Main.getLogger().error("Type error!!!!!!");
        }
        return lastObjectiveContainer;
    }

    /**
     * Method for cloning the {@link List} of {@link Objective}s wrapped by the {@link ObjectiveContainer} object.
     * @return Close of the {@link List} of the {@link Objective}s.
     * @throws CloneNotSupportedException
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        ObjectiveContainer cloned = (ObjectiveContainer)super.clone();
        cloned.objectives = getObjectiveListClone();
        return cloned;

    }


}
