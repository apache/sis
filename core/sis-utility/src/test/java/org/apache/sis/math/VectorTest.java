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
 * Tests the {@link Vector} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public final strictfp class VectorTest extends TestCase {
    /**
     * Tests {@link SequenceVector}.
     */
    @Test
    public void testSequence() {
        Vector vector = Vector.createSequence(100, 2, 10);
        assertEquals(Byte.class, vector.getElementType());
        assertEquals(10, vector.size());
        for (int i=0; i<vector.size(); i++) {
            assertEquals(100 + 2*i, vector.byteValue(i));
        }
        /*
         * Same tests, using double values.
         */
        vector = Vector.createSequence(100, 0.1, 10);
        assertEquals(Double.class, vector.getElementType());
        assertEquals(10, vector.size());
        for (int i=0; i<vector.size(); i++) {
            assertEquals(100 + 0.1*i, vector.doubleValue(i), 1E-10);
        }
    }

    /**
     * Tests {@link ArrayVector} backed by an array of primitive type.
     * We use the {@code short} type since it doesn't have specialized
     * vector implementation.
     */
    @Test
    public void testPrimitiveTypeArray() {
        final short[] array = new short[400];
        for (int i=0; i<array.length; i++) {
            array[i] = (short) ((i + 100) * 10);
        }
        Vector vector = Vector.create(array);
        assertTrue(vector instanceof ArrayVector);
        assertSame(vector, Vector.create(vector));
        assertEquals(array.length, vector.size());
        assertEquals(Short.class, vector.getElementType());
        /*
         * Tests element values.
         */
        for (int i=0; i<array.length; i++) {
            assertEquals(array[i], vector.shortValue (i));
            assertEquals(array[i], vector.intValue   (i));
            assertEquals(array[i], vector.floatValue (i), 0);
            assertEquals(array[i], vector.doubleValue(i), 0);
        }
        /*
         * Tests exception.
         */
        try {
            vector.floatValue(array.length);
            fail("Expected an IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // This is the expected exception.
        }
        try {
            vector.byteValue(0);
            fail("Expected a ClassCastException");
        } catch (ClassCastException e) {
            // This is the expected exception.
        }
        /*
         * Tests subvector in the range [100:2:298].
         */
        vector = vector.subList(100, 2, 100);
        assertEquals(100, vector.size());
        for (int i=0; i<100; i++) {
            assertEquals(array[i*2 + 100], vector.shortValue(i), 0);
        }
        try {
            vector.shortValue(100);
            fail("Expected an IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // This is the expected exception.
        }
        /*
         * Tests subvector at specific indexes.
         */
        vector = vector.view(10, 20, 25);
        assertEquals(3, vector.size());
        assertEquals(array[120], vector.shortValue(0), 0);
        assertEquals(array[140], vector.shortValue(1), 0);
        assertEquals(array[150], vector.shortValue(2), 0);
    }

    /**
     * Tests {@link ArrayVector.Float} backed by an array of float type.
     */
    @Test
    public void testFloatArray() {
        final float[] array = new float[400];
        for (int i=0; i<array.length; i++) {
            array[i] = (i + 100) * 10;
        }
        Vector vector = Vector.create(array);
        assertTrue(vector instanceof ArrayVector.Float);
        assertSame(vector, Vector.create(vector));
        assertEquals(array.length, vector.size());
        assertEquals(Float.class, vector.getElementType());
        /*
         * Tests element values.
         */
        for (int i=0; i<array.length; i++) {
            assertEquals(array[i], vector.floatValue (i), 0);
            assertEquals(array[i], vector.doubleValue(i), 0);
        }
    }

    /**
     * Tests {@link ArrayVector.Double} backed by an array of double type.
     */
    @Test
    public void testDoubleArray() {
        final double[] array = new double[400];
        for (int i=0; i<array.length; i++) {
            array[i] = (i + 100) * 10;
        }
        Vector vector = Vector.create(array);
        assertTrue(vector instanceof ArrayVector.Double);
        assertSame(vector, Vector.create(vector));
        assertEquals(array.length, vector.size());
        assertEquals(Double.class, vector.getElementType());
        /*
         * Tests element values.
         */
        for (int i=0; i<array.length; i++) {
            assertEquals(array[i], vector.floatValue (i), 0);
            assertEquals(array[i], vector.doubleValue(i), 0);
        }
    }

    /**
     * Tests {@link Vector#reverse()}.
     */
    @Test
    public void testReverse() {
        final double[] array    = {2, 3, 8};
        final double[] expected = {8, 3, 2};
        assertEquals(Vector.create(expected), Vector.create(array).reverse());
    }

    /**
     * Tests {@link Vector#concatenate(Vector)}.
     */
    @Test
    public void testConcatenate() {
        final float[] array = new float[40];
        for (int i=0; i<array.length; i++) {
            array[i] = i * 10;
        }
        final int[] extra = new int[20];
        for (int i=0; i<extra.length; i++) {
            extra[i] = (i + 40) * 10;
        }
        Vector v1 = Vector.create(array);
        Vector v2 = Vector.create(extra);
        Vector v3 = v1.concatenate(v2);
        assertEquals("Length of V3 should be the sum of V1 and V2 length.", 60, v3.size());
        assertEquals("Component type should be the widest of V1 and V2.", Float.class, v3.getElementType());
        assertEquals("Sample from V1.", Float  .valueOf(200), v3.get(20));
        assertEquals("Sample from V2.", Integer.valueOf(500), v3.get(50));
        for (int i=0; i<60; i++) {
            assertEquals(i*10, v3.floatValue(i), 0f);
        }
        assertSame("Should be able to restitute the original vector.", v1, v3.subList( 0, 40));
        assertSame("Should be able to restitute the original vector.", v2, v3.subList(40, 60));
        /*
         * Tests concatenation of views at fixed indices. Should be
         * implemented as the concatenation of the indices arrays when possible.
         */
        final Vector expected = v3.view(10, 25, 30, 0, 35, 39);
        v2 = v1.view( 0, 35, 39);
        v1 = v1.view(10, 25, 30);
        v3 = v1.concatenate(v2);
        assertEquals(expected, v3);
        assertFalse("Expected concatenation of the indices.", v3 instanceof ConcatenatedVector);
    }
}
