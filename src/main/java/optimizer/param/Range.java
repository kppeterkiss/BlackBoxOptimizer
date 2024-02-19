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
import optimizer.utils.Utils;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Class representing the boundaries of a {@link Param}. The boundaries can be numeric  upperBound and lowerBound in case of numeric wrapper {@link Param}s,
 * or it can be an enumeration of values (valueArray) for {@link Param} with finite finite values: {@link FunctionParam}
 * and {@link EnumParam}.
 * In latter case  lowerBound and upperBound will be the first and last element of the valueArray
 * @param <T>
 */
public class Range<T> implements Cloneable{
    private  T upperBound;
    private T lowerBound;

    public T[] getValueArray() {
        return valueArray;
    }

    /**
     * Array of possible value in case of finite value set.
     */
    private T[] valueArray;

    public T getUpperBound() {
        return upperBound;
    }

    public T getLowerBound() {
        return lowerBound;
    }

    @Override
    public String toString() {
        return "Range{" +
                "upperBound=" + upperBound +
                ", lowerBound=" + lowerBound +
                '}';
    }

    /**
     * Constructor for numerical {@link Range}
     * @param upperBound
     * @param lowerBound
     */
    public Range(T upperBound, T lowerBound) {
        this.upperBound = upperBound;
        this.lowerBound = lowerBound;
    }

    /**
     * Constructor for discrete range, where the possible values are given in an array, and the values will be members of the arrays that are between from and to parameters.
     * @param array All the possible values.
     * @param from Boundaries on the possible values.
     * @param to  Boundaries on the possible values.
     */
    public Range(T[] array, T to, T from) {

        if(!Arrays.asList(array).contains(from))
            throw new InvalidParameterValueException("Lower bound for Range not in the array.");
        if(!Arrays.asList(array).contains(to))
            throw new InvalidParameterValueException("Upper bound for Range not in the array.");
        List<T> arr1 = Arrays.asList(array);
        List<T> arr2 = new LinkedList<T>();
        int fromIndex = arr1.indexOf(from);
        int toIndex = arr1.indexOf(to);
        if(toIndex<fromIndex)
            throw new InvalidParameterValueException("Lower bound for Range not in higher that upper bound.");
        for(int i = fromIndex;i<=toIndex;i++)
            arr2.add(arr1.get(i));
        T[] arr = (T[]) arr2.toArray();
        this.lowerBound = arr[0];
        this.upperBound = arr[arr.length-1];
        this.valueArray = arr;
    }

    /**
     * Constructor for discrete {@link Range} that will correspond to the array parameter. Copies the references of elements of the array in an internal one.
     * @param array Array of the possible values.
     */
    public Range(T[] array) {
        this.valueArray = Arrays.copyOf(array,array.length);
        this.lowerBound = this.valueArray[0];
        this.upperBound = this.valueArray[this.valueArray.length-1];
    }

    /**
     * Creates a clone of the {@link Range} by copying member by member.
     * @return
     * @throws CloneNotSupportedException
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return (Range)super.clone();
    }

    /**
     * Method to specify the intersection of {@link Range}s.
     * @param r1 First {@link Range}.
     * @param r2 Second {@link Range}
     * @param <T> Generic parameter of the two {@link Range}s.
     * @return The intersection of the two {@link Range}.
     */
    public static <T> Range<T> intersection(Range<T> r1, Range<T> r2){
        if(r1==null)
            return r2;
        if(r2==null)
            return r1;
        if(r1.getValueArray()!=null){
            int r1l = r1.valueArray.length;
            int r2l = r2.valueArray.length;
            int l = Math.max(r1l,r2l);
            T[] x = r1l>r2l?r1.valueArray:r2.valueArray;
            List<T> l1 = Arrays.asList(r1.valueArray);
            List<T> l2 = Arrays.asList(r2.valueArray);
            List<T> res = new LinkedList<>();
            for(int i=0;i<l;i++)
                if(l1.contains(x[i])&&l2.contains(x[i]))
                    res.add(x[i]);

            // if there is no intersection we want the Range to be 0
            if(res.size()>0)
                return new Range<T>( (T[])res.toArray());
            return null;
        }
         if(r1.lowerBound.getClass().equals(Integer.class)) {
            Range<Integer> r1n = (Range<Integer>) r1;
            Range<Integer> r2n = (Range<Integer>) r2;
            Range<Integer> min = (r1n.lowerBound < r2n.lowerBound ? r1n : r2n);
            Range<Integer> max = (min.equals(  r1n) ? r2n : r2n);

            //min ends before max starts -> no intersection
            if (min.upperBound < max.lowerBound)
                return null;//the ranges don't intersect

            return (Range<T>)new Range<Integer>( (min.upperBound < max.upperBound ? min.upperBound : max.upperBound),max.lowerBound);
        }
         if(r1.lowerBound.getClass().equals(Float.class)) {
            Range<Float> r1n = (Range<Float>) r1;
            Range<Float> r2n = (Range<Float>) r2;
            Range<Float> min = (r1n.lowerBound < r2n.lowerBound ? r1n : r2n);
            Range<Float> max = (min.equals(r1n) ? r2n : r2n);

            //min ends before max starts -> no intersection
            if (min.upperBound < max.lowerBound)
                return null;//the ranges don't intersect

            return (Range<T>)new Range<Float>( (min.upperBound < max.upperBound ? min.upperBound : max.upperBound),max.lowerBound);
        }
        if(r1.lowerBound.getClass().equals(Double.class)) {

            Range<Double> r1n = (Range<Double>) r1;
            Range<Double> r2n = (Range<Double>) r2;
            //Range<Double> min = (r1n.lowerBound < r2n.lowerBound ? r1n : r2n);
            //Range<Double> max = (min.equals(r1n) ? r2n : r2n);
            Double min = (r1n.lowerBound < r2n.lowerBound ? r2n.lowerBound : r1n.lowerBound);
            Double max = (r1n.upperBound > r2n.upperBound ? r2n.upperBound : r1n.upperBound );

            //min ends before max starts -> no intersection
            if (min > max)
                return null;//the ranges don't intersect

            return (Range<T>)new Range<Double>(max,min);
        }
        if(r1.lowerBound.getClass().equals(Boolean.class)) {


            Range<Boolean> r1n = (Range<Boolean>) r1;
            Range<Boolean> r2n = (Range<Boolean>) r2;
            if(r1n.lowerBound^r1n.upperBound) {
                if (r2n.lowerBound ^ r2n.upperBound)
                    return (Range<T>)new Range(true, false);
                else return (Range<T>)new Range(r2n.upperBound, r2n.lowerBound);
            }
            else if(r1n.lowerBound != r2n.lowerBound)
                return null;
            return (Range<T>)new Range<Boolean>(r1n.lowerBound,r2n.lowerBound);
        }
     /*   if(r1.lowerBound.getClass().equals(String.class)){


            int r1l = r1.valueArray.length;
            int r2l = r2.valueArray.length;
            int l = Math.max(r1l,r2l);
            T[] x = r1l>r2l?r1.valueArray:r2.valueArray;
            int maxlen = Math.max(r1.valueArray.length,r2.valueArray.length);
            List<T> l1 = Arrays.asList(r1.valueArray);
            List<T> l2 = Arrays.asList(r2.valueArray);
            List<T> res = new LinkedList<>();
            for(int i=0;i<l;i++)
                if(l1.contains(x[i])&&l2.contains(x[i]))
                    res.add(x[i]);

            // if there is no intersection we want the Range to be 0
            if(res.size()>0)
                return new Range<T>( (T[])res.toArray());

        }*/
        return null;
    }

    /**
     * Two {@link Range} is regarded ad equals when the boundaries agree.
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Range<?> range = (Range<?>) o;

        if (!upperBound.equals(range.upperBound)) return false;
        return lowerBound.equals(range.lowerBound);

    }

    /**
     * Generated using the boundaries.
     * @return
     */
    @Override
    public int hashCode() {
        int result = upperBound.hashCode();
        result = 31 * result + lowerBound.hashCode();
        return result;
    }

    //TODO utils compile bypassed, no template spec for methods ?
    /**
     * Methog to query whether the passed value is in the {@link Range}.
     * @param value The value to check if it is in this {@link Range}
     * @return
     */
    public   boolean comply (T value){
        if(this.getValueArray()!=null) // for enumerated values even for Float = FunctionParam
            return Arrays.asList(this.getValueArray()).contains(value);
        else if(value instanceof Number)
            return Utils.compareNumbers((Number)value,(Number) this.upperBound)<=0 && Utils.compareNumbers((Number) this.lowerBound,(Number)value)<=0;
        else //Boolean
            return this.lowerBound.equals(value) || this.upperBound.equals(value); // this for Booleans

    }

}
