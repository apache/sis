/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.util;

import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link ArraysExt} utility methods.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
public final strictfp class ArraysExtTest extends TestCase {
    /**
     * Tests {@link ArraysExt#removeDuplicated(Object[])}.
     */
    @Test
    public void testRemoveDuplicated() {
        final Integer[] array = new Integer[] {2, 8, 4, 8, 1, 2, 8};
        assertArrayEquals(new Integer[] {2, 8, 4, 1},
                ArraysExt.resize(array, ArraysExt.removeDuplicated(array)));
    }

    /**
     * Tests {@link ArraysExt#reverse(int[])}.
     * The test uses an array of even length, then an array of odd length.
     */
    @Test
    public void testReverse() {
        int[] array = new int[] {2, 4, 8, 10};
        ArraysExt.reverse(array);
        assertArrayEquals(new int[] {10, 8, 4, 2}, array);

        array = new int[] {2, 4, 8, 10, 11};
        ArraysExt.reverse(array);
        assertArrayEquals(new int[] {11, 10, 8, 4, 2}, array);
    }

    /**
     * Tests {@link ArraysExt#unionOfSorted(int[], int[])}.
     */
    @Test
    public void testUnionOfSorted() {
        final int[] array1 = new int[] {2, 4, 6, 9, 12};
        final int[] array2 = new int[] {1, 2, 3, 12, 13, 18, 22};
        final int[] union = ArraysExt.unionOfSorted(array1, array2);
        assertArrayEquals(new int[] {1, 2, 3, 4, 6, 9, 12, 13, 18, 22}, union);
    }

    /**
     * Tests {@link ArraysExt#isSorted(char[], boolean)}.
     */
    @Test
    public void testIsSortedCharacters() {
        final char[] array = new char[] {1, 4, 7, 9};
        assertTrue (ArraysExt.isSorted(array, false));
        assertTrue (ArraysExt.isSorted(array, true));  array[2] = 4;
        assertTrue (ArraysExt.isSorted(array, false));
        assertFalse(ArraysExt.isSorted(array, true));  array[2] = 3;
        assertFalse(ArraysExt.isSorted(array, false));
        assertFalse(ArraysExt.isSorted(array, true));
    }

    /**
     * Tests {@link ArraysExt#isSorted(byte[], boolean)}.
     */
    @Test
    public void testIsSortedBytes() {
        final byte[] array = new byte[] {1, 4, 7, 9};
        assertTrue (ArraysExt.isSorted(array, false));
        assertTrue (ArraysExt.isSorted(array, true));  array[2] = 4;
        assertTrue (ArraysExt.isSorted(array, false));
        assertFalse(ArraysExt.isSorted(array, true));  array[2] = 3;
        assertFalse(ArraysExt.isSorted(array, false));
        assertFalse(ArraysExt.isSorted(array, true));
    }

    /**
     * Tests {@link ArraysExt#isSorted(short[], boolean)}.
     */
    @Test
    public void testIsSortedShorts() {
        final short[] array = new short[] {1, 4, 7, 9};
        assertTrue (ArraysExt.isSorted(array, false));
        assertTrue (ArraysExt.isSorted(array, true));  array[2] = 4;
        assertTrue (ArraysExt.isSorted(array, false));
        assertFalse(ArraysExt.isSorted(array, true));  array[2] = 3;
        assertFalse(ArraysExt.isSorted(array, false));
        assertFalse(ArraysExt.isSorted(array, true));
    }

    /**
     * Tests {@link ArraysExt#isSorted(int[], boolean)}.
     */
    @Test
    public void testIsSortedIntegers() {
        final int[] array = new int[] {1, 4, 7, 9};
        assertTrue (ArraysExt.isSorted(array, false));
        assertTrue (ArraysExt.isSorted(array, true));  array[2] = 4;
        assertTrue (ArraysExt.isSorted(array, false));
        assertFalse(ArraysExt.isSorted(array, true));  array[2] = 3;
        assertFalse(ArraysExt.isSorted(array, false));
        assertFalse(ArraysExt.isSorted(array, true));
    }

    /**
     * Tests {@link ArraysExt#isSorted(long[], boolean)}.
     */
    @Test
    public void testIsSortedLongs() {
        final long[] array = new long[] {1, 4, 7, 9};
        assertTrue (ArraysExt.isSorted(array, false));
        assertTrue (ArraysExt.isSorted(array, true));  array[2] = 4;
        assertTrue (ArraysExt.isSorted(array, false));
        assertFalse(ArraysExt.isSorted(array, true));  array[2] = 3;
        assertFalse(ArraysExt.isSorted(array, false));
        assertFalse(ArraysExt.isSorted(array, true));
    }

    /**
     * Tests {@link ArraysExt#isSorted(float[], boolean)}.
     */
    @Test
    public void testIsSortedFloats() {
        final float[] array = new float[] {1, Float.NaN, 4, Float.NaN, 7, 9};
        assertTrue (ArraysExt.isSorted(array, false));
        assertTrue (ArraysExt.isSorted(array, true));  array[3] = 4;
        assertTrue (ArraysExt.isSorted(array, false));
        assertFalse(ArraysExt.isSorted(array, true));  array[3] = 3;
        assertFalse(ArraysExt.isSorted(array, false));
        assertFalse(ArraysExt.isSorted(array, true));
    }

    /**
     * Tests {@link ArraysExt#isSorted(double[], boolean)}.
     */
    @Test
    public void testIsSortedDoubles() {
        final double[] array = new double[] {1, Double.NaN, 4, Double.NaN, 7, 9};
        assertTrue (ArraysExt.isSorted(array, false));
        assertTrue (ArraysExt.isSorted(array, true));  array[3] = 4;
        assertTrue (ArraysExt.isSorted(array, false));
        assertFalse(ArraysExt.isSorted(array, true));  array[3] = 3;
        assertFalse(ArraysExt.isSorted(array, false));
        assertFalse(ArraysExt.isSorted(array, true));
    }

    /**
     * Tests {@link ArraysExt#isSorted(Comparable[], boolean)}.
     */
    @Test
    public void testIsSortedComparables() {
        final Integer[] array = new Integer[] {1, null, 4, null, 7, 9};
        assertTrue (ArraysExt.isSorted(array, false));
        assertTrue (ArraysExt.isSorted(array, true));  array[3] = 4;
        assertTrue (ArraysExt.isSorted(array, false));
        assertFalse(ArraysExt.isSorted(array, true));  array[3] = 3;
        assertFalse(ArraysExt.isSorted(array, false));
        assertFalse(ArraysExt.isSorted(array, true));
    }

    /**
     * Tests the {@link ArraysExt#swap(Object[], int, int)} method.
     */
    @Test
    public void testSwapObject() {
        final Integer[] array = new Integer[] {4, 8, 12, 15, 18};
        ArraysExt.swap(array, 1, 3);
        assertArrayEquals(new Integer[] {4, 15, 12, 8, 18}, array);
    }

    /**
     * Tests the {@link ArraysExt#swap(double[], int, int)} method.
     */
    @Test
    public void testSwapDouble() {
        final double[] array = new double[] {4, 8, 12, 15, 18};
        ArraysExt.swap(array, 1, 3);
        assertArrayEquals(new double[] {4, 15, 12, 8, 18}, array, 0.0);
    }

    /**
     * Tests the {@link ArraysExt#swap(double[], int, int)} method.
     */
    @Test
    public void testSwapFloat() {
        final float[] array = new float[] {4, 8, 12, 15, 18};
        ArraysExt.swap(array, 1, 3);
        assertArrayEquals(new float[] {4, 15, 12, 8, 18}, array, 0f);
    }

    /**
     * Tests the {@link ArraysExt#swap(double[], int, int)} method.
     */
    @Test
    public void testSwapLong() {
        final long[] array = new long[] {4, 8, 12, 15, 18};
        ArraysExt.swap(array, 1, 3);
        assertArrayEquals(new long[] {4, 15, 12, 8, 18}, array);
    }

    /**
     * Tests the {@link ArraysExt#swap(double[], int, int)} method.
     */
    @Test
    public void testSwapInteger() {
        final int[] array = new int[] {4, 8, 12, 15, 18};
        ArraysExt.swap(array, 1, 3);
        assertArrayEquals(new int[] {4, 15, 12, 8, 18}, array);
    }

    /**
     * Tests the {@link ArraysExt#swap(double[], int, int)} method.
     */
    @Test
    public void testSwapShort() {
        final short[] array = new short[] {4, 8, 12, 15, 18};
        ArraysExt.swap(array, 1, 3);
        assertArrayEquals(new short[] {4, 15, 12, 8, 18}, array);
    }

    /**
     * Tests the {@link ArraysExt#swap(double[], int, int)} method.
     */
    @Test
    public void testSwapByte() {
        final byte[] array = new byte[] {4, 8, 12, 15, 18};
        ArraysExt.swap(array, 1, 3);
        assertArrayEquals(new byte[] {4, 15, 12, 8, 18}, array);
    }

    /**
     * Tests the {@link ArraysExt#swap(double[], int, int)} method.
     */
    @Test
    public void testSwapChar() {
        final char[] array = new char[] {4, 8, 12, 15, 18};
        ArraysExt.swap(array, 1, 3);
        assertArrayEquals(new char[] {4, 15, 12, 8, 18}, array);
    }
}
