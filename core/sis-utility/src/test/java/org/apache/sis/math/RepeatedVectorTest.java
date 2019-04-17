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
package org.apache.sis.math;

import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link RepeatedVector} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final strictfp class RepeatedVectorTest extends TestCase {
    /**
     * Tests the case where values in a grid are repeated horizontally.
     */
    @Test
    public void testHorizontal() {
        Vector vec = Vector.create(new int[] {
                10, 10, 10, 10,
                12, 12, 12, 12,
                15, 15, 15, 15}, false);

        vec = new RepeatedVector(vec, vec.repetitions(), 0);
        assertArrayEquals(new int[] {4}, vec.repetitions());

        assertEquals(10, vec.intValue  ( 0));
        assertEquals(10, vec.shortValue( 1));
        assertEquals(10, vec.longValue ( 2));
        assertEquals(10, vec.intValue  ( 3));
        assertEquals(12, vec.intValue  ( 4));
        assertEquals(12, vec.shortValue( 7));
        assertEquals(15, vec.longValue ( 8));
        assertEquals(15, vec.intValue  (11));

        Vector sub = vec.subSampling(0, 4, 3);
        assertFalse("Expected the backing array.", sub instanceof RepeatedVector);
        assertArrayEquals(new float[] {10, 12, 15}, sub.floatValues(), (float) STRICT);
    }

    /**
     * Tests the case where values in a grid are repeated vertically.
     */
    @Test
    public void testVertical() {
        Vector vec = Vector.create(new int[] {
                10, 12, 15, 18,
                10, 12, 15, 18,
                10, 12, 15, 18}, false);

        vec = new RepeatedVector(vec, vec.repetitions(), 0);
        assertArrayEquals(new int[] {1,4}, vec.repetitions());

        assertEquals(10, vec.intValue  ( 0));
        assertEquals(12, vec.shortValue( 1));
        assertEquals(15, vec.longValue ( 2));
        assertEquals(18, vec.intValue  ( 3));
        assertEquals(10, vec.intValue  ( 4));
        assertEquals(18, vec.shortValue( 7));
        assertEquals(10, vec.longValue ( 8));
        assertEquals(15, vec.intValue  (10));

        Vector sub = vec.subList(0, 4);
        assertFalse("Expected the backing array.", sub instanceof RepeatedVector);
        assertArrayEquals(new float[] {10, 12, 15, 18}, sub.floatValues(), (float) STRICT);
    }

    /**
     * Tests the case mixing both kind of repetitions.
     */
    @Test
    public void testMixed() {
        Vector vec = Vector.create(new int[] {
                10, 10, 10,  12, 12, 12,  15, 15, 15,  18, 18, 18,
                10, 10, 10,  12, 12, 12,  15, 15, 15,  18, 18, 18,
                10, 10, 10,  12, 12, 12,  15, 15, 15,  18, 18, 18}, false);

        vec = new RepeatedVector(vec, vec.repetitions(), 0);
        assertArrayEquals(new int[] {3,4}, vec.repetitions());

        assertEquals(10, vec.intValue  ( 0));
        assertEquals(10, vec.shortValue( 1));
        assertEquals(10, vec.longValue ( 2));
        assertEquals(12, vec.intValue  ( 3));
        assertEquals(12, vec.intValue  ( 4));
        assertEquals(15, vec.shortValue( 7));
        assertEquals(15, vec.longValue ( 8));
        assertEquals(18, vec.intValue  (11));

        assertEquals(10, vec.intValue  (13));
        assertEquals(12, vec.shortValue(17));
        assertEquals(15, vec.longValue (18));
        assertEquals(18, vec.intValue  (22));
        assertEquals(10, vec.intValue  (24));
        assertEquals(15, vec.shortValue(31));
        assertEquals(18, vec.longValue (23));
        assertEquals(12, vec.intValue  (28));

        Vector sub = vec.subSampling(0, 3, 4);
        assertFalse("Expected the backing array.", sub instanceof RepeatedVector);
        assertArrayEquals(new float[] {10, 12, 15, 18}, sub.floatValues(), (float) STRICT);
    }

    /**
     * Tests the case where values in a grid are repeated vertically with a constant prefix.
     * This case has a special code path for performance reasons.
     */
    @Test
    public void testVerticalWithConstantPrefix() {
        testVerticalWithConstantPrefix(10);
        testVerticalWithConstantPrefix(11);
    }

    /**
     * Implementation of {@link #testVerticalWithConstantPrefix()} with the possibility
     * to inject an "impurity" in the side of constant values.
     *
     * @param  ip  10 for constant values on left side, or another value for injecting an "impurity".
     */
    private static void testVerticalWithConstantPrefix(final int ip) {
        Vector vec = Vector.create(new int[] {
                10, 10, 10,  12, 15, 18,
                10, ip, 10,  12, 15, 18,
                10, 10, 10,  12, 15, 18}, false);

        vec = new RepeatedVector(vec, vec.repetitions(), 0);
        final int expectedBackingVectorLength = (ip == 10) ? 6 : 12;
        assertArrayEquals(new int[] {1, expectedBackingVectorLength}, vec.repetitions());

        assertEquals(10, vec.intValue  ( 0));
        assertEquals(10, vec.shortValue( 1));
        assertEquals(10, vec.longValue ( 2));
        assertEquals(12, vec.intValue  ( 3));
        assertEquals(15, vec.intValue  ( 4));
        assertEquals(18, vec.longValue ( 5));
        assertEquals(10, vec.intValue  ( 6));
        assertEquals(ip, vec.intValue  ( 7));
        assertEquals(10, vec.intValue  ( 8));
        assertEquals(10, vec.intValue  (13));
        assertEquals(15, vec.shortValue(10));
        assertEquals(12, vec.longValue (15));
        assertEquals(18, vec.intValue  (17));

        Vector sub = vec.subSampling(0, 1, 6);
        assertFalse("Expected the backing array.", sub instanceof RepeatedVector);
        assertArrayEquals(new float[] {10, 10, 10, 12, 15, 18}, sub.floatValues(), (float) STRICT);

        sub = vec.subSampling(0, 1, 12);
        assertArrayEquals(new float[] {10, 10, 10, 12, 15, 18,
                                       10, ip, 10, 12, 15, 18}, sub.floatValues(), (float) STRICT);
    }

    /**
     * Tests {@link Vector#repeat(boolean, int)} with {@code eachValue} set to {@code false}.
     */
    @Test
    public void testRepeatVector() {
        Vector vec = Vector.create(new int[] {2, -4, 7, 3}, false);
        assertSame(vec, vec.repeat(false, 1));

        vec = vec.repeat(false, 3);
        assertArrayEquals(new float[] {
            2, -4, 7, 3,
            2, -4, 7, 3,
            2, -4, 7, 3
        }, vec.floatValues(), (float) STRICT);
        assertSame(vec, vec.repeat(false, 1));

        vec = vec.repeat(false, 2);
        assertArrayEquals(new float[] {
            2, -4, 7, 3,
            2, -4, 7, 3,
            2, -4, 7, 3,
            2, -4, 7, 3,
            2, -4, 7, 3,
            2, -4, 7, 3
        }, vec.floatValues(), (float) STRICT);
    }

    /**
     * Tests {@link Vector#repeat(boolean, int)} with {@code eachValue} set to {@code true}.
     */
    @Test
    public void testRepeatValues() {
        Vector vec = Vector.create(new int[] {2, -4, 7, 3}, false);
        assertSame(vec, vec.repeat(true, 1));

        vec = vec.repeat(true, 3);
        assertArrayEquals(new float[] {
             2,  2,  2,
            -4, -4, -4,
             7,  7,  7,
             3,  3,  3
        }, vec.floatValues(), (float) STRICT);
        assertSame(vec, vec.repeat(false, 1));

        vec = vec.repeat(true, 2);
        assertArrayEquals(new float[] {
             2,  2,  2,  2,  2,  2,
            -4, -4, -4, -4, -4, -4,
             7,  7,  7,  7,  7,  7,
             3,  3,  3,  3,  3,  3
        }, vec.floatValues(), (float) STRICT);
    }
}
