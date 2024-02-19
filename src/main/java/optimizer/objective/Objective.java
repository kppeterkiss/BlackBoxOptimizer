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

import optimizer.utils.Utils;

/**
 * Describes an objective of the optimization task.
 * The objectives can have many different types, based on which we can decide whether we are getting closer to an optima.
 * These types are described in {@link Relation}.
 * @param <T> The T Type parameter of on aobjective decribes the Base Java type of an objective. At current State we believe that {@link java.lang.Integer} and {@link java.lang.Float} should be enough.
 */
public class Objective<T> implements Cloneable{
    Relation relation;

    /**
     * Objectives are regarded as equal is they have the same name.
     * @param other
     * @return
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Objective)) return false;

        Objective<?> objective = (Objective<?>) other;

        return name.equals(objective.name);

    }

    /**
     * After definition of {@link #equals(Object)} follows that we only use the name
     * @return
     */
    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override

    public String toString() {
        return "Objective{" +
                "relation=" + relation +
                ", terminator=" + terminator +
                ", weight=" + weight +
                ", name='" + name + '\'' +
                ", dummy=" + dummy +
                ", value=" + value +
                ", target=" + target +
                '}';
    }

    /**
     * Specifies whether reaching the objective's termination condition terminates the entire optimization.
     */
    boolean terminator;


    @Deprecated
    public String getRelationStr() {
        return relation.toString();
    }

    public Relation getRelation() {return relation;}

    public boolean isTerminator() {
        return terminator;
    }

    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }

    public T getTarget() {
        return target;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }


    public T getLastvalue() {        return lastvalue;    }

    /**
     * In case of linear combination of multiple {@link Objective} this specifies the importance of each of them.
     */
    private float weight;
    /**
     * The name of an objective, this is the primary identifier of an objectives.
     * BBFs are expected to return an {@link Objective} value as <name><space><current value> on standard output, for example:
     * `accuracy 0.8`
     */
    private String name;
    /**
     * Dummy field to specify {@code T} - may be removed later
     */
    private T dummy;
    /**
     * The recent value of the objective
     */
    private T value;
    /**
     * The target value the {@code value} should approximate
     */
    private T target;

    /**
     * {@code value} of the previous
     */
    private T lastvalue;
    /**
     * String representation of Type parameter T ({@link Class#getCanonicalName()}) for correct deserialization.
     */
    private String typeName;
    public void setValue(T value){
        if(value==null)
            System.out.println();
        this.lastvalue = this.value;
        this.value = value;
    }

    /**
     * Method to check whether az objective that is the goal of the optimization has been met.
     * @return
     */
    public boolean met(){
        return  Utils.comply(relation,value,target,lastvalue);
    }

    /**
     * Method to qury the generic type of the Objective.
     * @return {@link Class} object of  type T
     */
    public Class<?> getType(){
        return dummy.getClass(); //before first load value
    }
    public String getTypeName(){ return this.typeName;}

    /**
     * Constructor for an objective, setting up all the instance variable of it. {@code typeName} will be inferred from the type of the object paseed as {@code dummynull}
     * @param relation
     * @param terminator
     * @param name
     * @param value
     * @param target
     * @param dummynull
     * @param weight
     */
    public Objective(Relation relation, boolean terminator, String name, T value, T target, T dummynull, float weight) {
        this.relation = relation;
        this.terminator = terminator;
        this.dummy = dummynull;
        this.name = name;
        this.value = value;
        this.target = target;
        this.weight = weight;
        this.typeName = dummy.getClass().getCanonicalName();

    }

    /**
     * Default constructor for deserialization.
     */
    public Objective() {
    }

    /**
     * Clone method to avoid leaking of inner state.
     * @return A new Objective created through field by field copy.
     * @throws CloneNotSupportedException
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return (Objective)super.clone();
    }
}
