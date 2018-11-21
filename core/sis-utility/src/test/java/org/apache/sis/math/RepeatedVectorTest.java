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

import static org.opengis.test.Assert.*;


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
        assertInstanceOf("Should have been compressed.", RepeatedVector.class, vec);
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
        assertInstanceOf("Should have been compressed.", RepeatedVector.class, vec);
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
        assertInstanceOf("Should have been compressed.", RepeatedVector.class, vec);
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
}
