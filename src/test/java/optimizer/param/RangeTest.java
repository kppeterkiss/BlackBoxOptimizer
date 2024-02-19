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

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertTrue;

public class RangeTest {

    @Test
    public void enumRangeTest1(){
        String[] array = new String[]{"1","2","3","4","5"};
        Range r1 = new Range(array);
        assertTrue(r1.getValueArray().length==array.length);
        for (int i = 0;i< r1.getValueArray().length;i++){
            assertTrue(r1.getValueArray()[i].equals(array[i]));
        }

    }

    @Test
    public void booleanRangeTest1(){
        Range r = new Range(false,false);
        assertTrue(r.getLowerBound().equals(false));
        assertTrue(r.getUpperBound().equals(false));
    }
    @Test
    public void booleanRangeTest2(){
        Range r = new Range(true,true);
        assertTrue(r.getLowerBound().equals(true));
        assertTrue(r.getUpperBound().equals(true));
    }

    @Test // TODO: 2018. 02. 27. not the best
    public void booleanRangeTest3(){
        Range r = new Range(false,true);
        assertTrue(r.getLowerBound().equals(true));
        assertTrue(r.getUpperBound().equals(false));
    }

    @Test // TODO: 2018. 02. 27. not the best
    public void booleanRangeTest4(){
        Range r = new Range(true,false);
        assertTrue(r.getLowerBound().equals(false));
        assertTrue(r.getUpperBound().equals(true));
    }


    @Test
    public void floatEnumRangeTest(){
        Float[] array = new Float[]{1f,2f,3f,4f,5f};
        Range r1 = new Range(array);
        assertTrue(r1.getValueArray().length==array.length);
        for (int i = 0;i< r1.getValueArray().length;i++){
            assertTrue(r1.getValueArray()[i].equals(array[i]));
        }

    }

    @Test
    public void floatEnumRangeTest2(){
        Float[] array = new Float[]{1f,2f,3f,4f,5f};
        Float[] expectedarray = new Float[]{2f,3f,4f};
        Range r1 = new Range(array,4f,2f);
        assertTrue(r1.getValueArray().length==expectedarray.length);
        for (int i = 0;i< r1.getValueArray().length;i++){
            assertTrue(r1.getValueArray()[i].equals(expectedarray[i]));
        }

    }


    @Test
    public void enumRangeTest2(){
        String[] array = new String[]{"1","2","3","4","5"};
        String[] expectedarray = new String[]{"2","3","4"};
        Range r1 = new Range(array,"4","2");
        assertTrue(r1.getValueArray().length==expectedarray.length);
        for (int i = 0;i< r1.getValueArray().length;i++){
            assertTrue(r1.getValueArray()[i].equals(expectedarray[i]));
        }

    }


    @Test
    public void leakageRangeTest1(){
        String[] array = new String[]{"1","2","3","4","5"};
        Range fullRange = new Range(array);
        array[0]="100";
        assertTrue(!array.equals(fullRange.getValueArray()));

    }


    @Test
    public void equalTest(){
        Range r1 = new Range(10f,1f);
        Range r2 = new Range(10f,1f);
        assertTrue(r1.equals(r2));

        r2 =  new Range(10f,2f);
        assertTrue(!r1.equals(r2));

    }
    @Test
    public void floatIntersectionTest1(){
        Range r = new Range(10f,1f);
        Range r2 = new Range(12f,9f);
        assertTrue(Range.intersection(r,r2).equals(new Range(10f,9f)));
    }

    @Test
    public void floatIntersectionTest2(){
        Range r = new Range(10f,1f);
        Range r2 = new Range(8f,7f);
        assertTrue(Range.intersection(r,r2).equals(new Range(8f,7f)));
    }

    @Test
    public void floatIntersectionNull1(){
        Range r = new Range(10f,1f);
        Range r2 = null;
        assertTrue(Range.intersection(r,r2).equals(new Range(10f,1f)));
    }

    @Test
    public void floatIntersectionNull2(){
        Range r = null;
        Range r2 = new Range(8f,7f);
        assertTrue(Range.intersection(r,r2).equals(new Range(8f,7f)));
    }


    @Test
    public void floatEnumIntersectioTest1(){
        Float[] array1 = new Float[]{1f,2f,3f,4f,5f};
        Float[] array2 = new Float[]{2f,3f,4f};
        Range r1 = new Range(array1);
        Range r2 = new Range(array2);
        assertTrue(Range.intersection(r1,r2).equals(new Range(new Float[]{2f,3f,4f})));

    }


    @Test
    public void floatEnumIntersectioTes2(){
        Float[] array1 = new Float[]{1f,2f,3f,4f,5f};
        Float[] array2 = new Float[]{1f,3f,5f};
        Range r1 = new Range(array1);
        Range r2 = new Range(array2);
        assertTrue(Range.intersection(r1,r2).equals(new Range(new Float[]{1f,3f,5f})));

    }

    //this should not happen due to generation of this type in FunctionParameter
    @Test
    public void floatEnumIntersectioTest3(){
        Float[] array1 = new Float[]{1f,2f,3f,4f,5f};
        Float[] array2 = new Float[]{5f,3f,1f};
        Range r1 = new Range(array1);
        Range r2 = new Range(array2);
        assertTrue(Range.intersection(r1,r2).equals(new Range(new Float[]{1f,3f,5f})));

    }

    @Test
    public void booleanIntersectionTest1(){

        Range r1 = new Range(false,false);
        Range r2 = new Range(true,true);
        assertTrue(Range.intersection(r1,r2)==null);


    }

    @Test
    public void booleanIntersectionTest2(){

        Range r1 = new Range(true,false);
        Range r2 = new Range(true,true);
        assertTrue(Range.intersection(r1,r2).equals(new Range(true,true)));


    }

    @Test
    public void booleanIntersectionTest3(){

        Range r1 = new Range(false,true);
        Range r2 = new Range(true,true);
        assertTrue(Range.intersection(r1,r2).equals(new Range(true,true)));


    }

    @Test
    public void booleanComplyTest1(){

        Range r1 = new Range(false,true);
        assertTrue(r1.comply(false));
        assertTrue(r1.comply(true));

        Range r4 = new Range(true,false);
        assertTrue(r4.comply(false));
        assertTrue(r4.comply(true));

        Range r2 = new Range(true,true);
        assertTrue(!r2.comply(false));
        assertTrue(r2.comply(true));

        Range r3 = new Range(false,false);
        assertTrue(r3.comply(false));
        assertTrue(!r3.comply(true));

    }
    @Test
    public void floatComplyTest(){
        Range r1 = new Range(3f,1f);
        assertTrue(r1.comply(1f));
        assertTrue(r1.comply(2f));
        assertTrue(r1.comply(3f));
        assertTrue(!r1.comply(0f));
        assertTrue(!r1.comply(4f));
    }


    @Test
    public void intComplyTest(){
        Range r1 = new Range(3,1);
        assertTrue(r1.comply(1));
        assertTrue(r1.comply(2));
        assertTrue(r1.comply(3));
        assertTrue(!r1.comply(0));
        assertTrue(!r1.comply(4));
    }

    @Test
    public void enumComplyTest1(){

        Range r1 = new Range(new String[]{"1","2","3"});
        assertTrue(r1.comply("1"));
        assertTrue(r1.comply("2"));
        assertTrue(r1.comply("3"));
        assertTrue(!r1.comply("0"));
        assertTrue(!r1.comply("4"));

        r1 = new Range(new Float[]{1f,2f,3f});
        assertTrue(r1.comply(1f));
        assertTrue(r1.comply(2f));
        assertTrue(r1.comply(3f));
        assertTrue(!r1.comply(0f));
        assertTrue(!r1.comply(4f));



    }

}
