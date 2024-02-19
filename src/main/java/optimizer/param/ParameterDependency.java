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

package optimizer.param;

import optimizer.exception.InvalidParameterValueException;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


/**
 * Class representing boundaries on {@link Param}s. A {@link ParameterDependency} can be either bounded by another {@link Param}, or unbounded. A Param can have multiple ParameterDependencies, but only one can be unbounded then.
 * The boundaries of the {@link Param} are in fact represented by a {@link Range}.
 * @param <T2> The generic type of the bounding {@link Param}
 * @param <T1> The generic type of the bounded {@link Param}
 */
public class ParameterDependency<T2,T1> implements Cloneable{
    /**
     *The {@link Range} of the bounded {@link Param}
     */
    Range<T1> rangeOfThis;
    /**
     * The bounding {@link Param}
     */
    Param<T2> p;
    /**
     *The {@link Range} of the bounding {@link Param}
     */
    Range<T2> rangeOfOther;

    public Range<T1> getRangeOfThis() {
        return rangeOfThis;
    }

    public void setRangeOfThis(Range<T1> rangeOfThis) {
        this.rangeOfThis = rangeOfThis;
    }

    public Param<T2> getP() {
        return p;
    }

    public void setP(Param<T2> p) {
        this.p = p;
    }

    public Range<T2> getRangeOfOther() {
        return rangeOfOther;
    }

    public void setRangeOfOther(Range<T2> rangeOfOther) {
        this.rangeOfOther = rangeOfOther;
    }

    /**
     * Method to query whether {@link #p} is in its specified bounding range({@code rangeOfOther}).
     * That means that here we check, whether the given {@link ParameterDependency} is active, that is the {@link Range} given by it should be taken into consideration at computing {@link Param#getActiveRange()}.
     * If there are bo bounding {@link Param}, returns true.
     * @return
     */
    public boolean comply(){
        if(p==null)
            return true;
        return rangeOfOther.comply(p.getValue());
    }

    /**
     * Basic {@link String} representation of {@link ParameterDependency}
     * @return
     */
    @Override
    public String toString() {
        return "ParameterDependency{" +
                "rangeOfThis=" + rangeOfThis.toString() +
                ", p=" + (p==null? "null" : p.getName() )+
                ", rangeOfOther=" + (p == null ? "null" : rangeOfOther) +
                '}';
    }

    /**
     * Most general constructor for {@link ParameterDependency}
     * @param lower Lower bound for bounded {@link Param}.
     * @param upper Upper bound for bounded {@link Param}.
     * @param p Bounding {@link Param}.
     * @param lowerBound Lower bound for bounding {@link Param}.
     * @param upperBound Upper bound for bounding {@link Param}.
     */
    public ParameterDependency(T1 lower, T1 upper, Param<T2> p, T2 lowerBound , T2 upperBound) {
        this.rangeOfThis = new Range<T1>(upper,lower);
        this.p = p;
        if(p!=null && lowerBound!=null && upperBound!=null) {
            if (p.isEnumeration()) {
                if(!Arrays.asList(p.getAllValueArray()).contains(lowerBound))
                    throw new InvalidParameterValueException("Lower bound for dependency not in valid value array.");
                if(!Arrays.asList(p.getAllValueArray()).contains(upperBound))
                    throw new InvalidParameterValueException("Upper bound for dependency not in valid value array.");
                List<T2> res = new LinkedList<T2>();
                boolean afterFirst = false, beforeLast = true;
                for(T2 element : p.getAllValueArray()  ) {
                    if (element.equals(lowerBound))
                        afterFirst = true;
                    if(element.equals(upperBound)){
                        res.add(element);
                        beforeLast = false;
                    }
                    if(afterFirst&&beforeLast)
                        res.add(element);
                }
                this.rangeOfOther = new Range<T2>((T2[])res.toArray());
            }
            else
                this.rangeOfOther = new Range<T2>(upperBound, lowerBound);

        }
    }

    /**
     *This should be the main constructor
     * @param allValueArray All available values for the bounded {@link Param}, the bounded value array
     * @param lower Lower bound for bounded {@link Param}.
     * @param upper Upper bound for bounded {@link Param}.
     * @param p Bounding {@link Param}.
     * @param lowerBound Lower bound for bounding {@link Param}.
     * @param upperBound Upper bound for bounding {@link Param}.
     */
    public ParameterDependency(T1[] allValueArray,T1 lower, T1 upper, Param<T2> p, T2 lowerBound , T2 upperBound) {
        this.rangeOfThis = new Range<T1>(allValueArray,upper,lower);
        this.p = p;
        if(p!=null && lowerBound!=null && upperBound!=null) {
            if (p.isEnumeration()) {
                if(!Arrays.asList(p.getAllValueArray()).contains(lowerBound))
                    throw new InvalidParameterValueException("Lower bound for dependency not in valid value array.");
                if(!Arrays.asList(p.getAllValueArray()).contains(upperBound))
                    throw new InvalidParameterValueException("Upper bound for dependency not in valid value array.");
                List<T2> res = new LinkedList<T2>();
                boolean afterFirst = false, beforeLast = true;
                for(T2 element : p.getAllValueArray()  ) {
                    if (element.equals(lowerBound))
                        afterFirst = true;
                    if(element.equals(upperBound)){
                        res.add(element);
                        beforeLast = false;
                    }
                    if(afterFirst&&beforeLast)
                        res.add(element);
                }
                this.rangeOfOther = new Range<T2>((T2[])res.toArray());
            }
            else
                this.rangeOfOther = new Range<T2>(upperBound, lowerBound);

        }
    }


    /**
     * Constructor for not bounded {@link EnumParam}.
     * @param array The array of possible values.
     */
    public ParameterDependency(T1[] array) {
        this.rangeOfThis = new Range<T1>(array);
        this.p = null;
        this.rangeOfOther = null;
    }

    /**
     * Constructor for not bounded {@link Param}.
     * @param lower Lower boundary.
     * @param upper Upper boundary.
     */
    public ParameterDependency(T1 lower, T1 upper) {
        this.rangeOfThis = new Range<T1>(upper,lower);
        this.p = null;
        this.rangeOfOther = null;
    }

    /**
     * Contructor for empty dependency, used at reading from GUI.
     */
    public ParameterDependency(){
        this.rangeOfThis = null;
        this.p = null;
        this.rangeOfOther = null;
    }

    /**
     * Creates a clone of the {@link ParameterDependency}.
     * !!!It clones the {@link Param} too!!!
     * @return
     * @throws CloneNotSupportedException
     */
    public Object clone() throws CloneNotSupportedException {
        ParameterDependency pd =   (ParameterDependency)super.clone();
        pd.rangeOfThis = (Range) this.rangeOfThis.clone();
        if(this.p != null) {
            pd.rangeOfOther = (Range) this.rangeOfOther.clone();
            pd.p = (Param) this.p.clone();
        }
        return pd;

    }


}
