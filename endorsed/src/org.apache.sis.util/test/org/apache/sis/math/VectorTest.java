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

import org.apache.sis.measure.NumberRange;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertMessageContains;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link Vector} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class VectorTest extends TestCase {
    /**
     * The tested vector.
     */
    private Vector vector;

    /**
     * Creates a new test case.
     */
    public VectorTest() {
    }

    /**
     * Tests {@link SequenceVector} with byte values.
     */
    @Test
    public void testSequenceOfBytes() {
        vector = Vector.createSequence(100, 2, 10);
        assertEquals(Integer.class, vector.getElementType());
        assertEquals(10, vector.size());
        for (int i=0; i<vector.size(); i++) {
            assertEquals(100 + 2*i, vector.byteValue(i));
        }
    }

    /**
     * Tests {@link SequenceVector} with float values.
     */
    @Test
    public void testSequenceOfFloats() {
        vector = Vector.createSequence(100, 0.1, 10);
        assertEquals(Double.class, vector.getElementType());
        assertEquals(10, vector.size());
        for (int i=0; i<vector.size(); i++) {
            assertEquals(100 + 0.1*i, vector.doubleValue(i), 1E-10);
        }
    }

    /**
     * Tests {@link ArrayVector} backed by an array of primitive type.
     * We use the {@code short} type for this test.
     */
    @Test
    @SuppressWarnings("UnnecessaryBoxing")
    public void testShortArray() {
        final short[] array = new short[400];
        for (int i=0; i<array.length; i++) {
            array[i] = (short) ((i - 20) * 10);
        }
        vector = Vector.create(array, false);
        assertTrue(vector instanceof ArrayVector);
        assertSame(vector, Vector.create(vector, false));
        assertEquals(array.length, vector.size());
        assertEquals(Short.class, vector.getElementType());
        /*
         * Verify element values. The wrapper class shall be `Short`.
         */
        for (int i=0; i<array.length; i++) {
            final short expected = array[i];
            assertEquals(expected, vector.shortValue (i));
            assertEquals(expected, vector.intValue   (i));
            assertEquals(expected, vector.floatValue (i));
            assertEquals(expected, vector.doubleValue(i));
            assertEquals(Short.valueOf(expected), vector.get(i));
        }
        /*
         * Test exception for invalid index and for invalid narrowing cast.
         */
        RuntimeException e;
        e = assertThrows(IndexOutOfBoundsException.class, () -> vector.floatValue(array.length));
        assertNotNull(e);

        e = assertThrows(ArithmeticException.class, () -> vector.byteValue(0));
        assertMessageContains(e, "byte");
        /*
         * Test subvector in the range [100:2:298].
         */
        vector = vector.subSampling(100, 2, 100);
        assertEquals(100, vector.size());
        for (int i=0; i<100; i++) {
            assertEquals(array[i*2 + 100], vector.shortValue(i));
        }
        e = assertThrows(IndexOutOfBoundsException.class, () -> vector.shortValue(100));
        assertNotNull(e);
        /*
         * Test subvector at specific indices. The indices of picked values below and indices
         * in the sub-vector tested above. They may to the original array by `index*2 + 100`.
         */
        vector = vector.pick(10, 20, 25);
        assertEquals(3, vector.size());
        assertEquals(array[120], vector.shortValue(0));
        assertEquals(array[140], vector.shortValue(1));
        assertEquals(array[150], vector.shortValue(2));
    }

    /**
     * Tests {@link ArrayVector} backed by an array of primitive type handled as unsigned values.
     */
    @Test
    @SuppressWarnings("UnnecessaryBoxing")
    public void testUnsignedByteArray() {
        final byte[] array = new byte[100];
        for (int i=0; i<array.length; i++) {
            array[i] = (byte) ((i + 3) * 2);
        }
        vector = Vector.create(array, true);
        assertTrue(vector instanceof ArrayVector);
        assertSame(vector, Vector.create(vector, true));
        assertEquals(array.length, vector.size());
        assertEquals(Byte.class, vector.getElementType());
        /*
         * Verify element values. Bytes shall be cast to shorts in order to handle unsigned values.
         * The widening casts should be unconditional, even for positive values that could fit in a byte,
         * because some codes using `Vector` expect a stable type (for example `NetcdfStore` which copies
         * vector values into feature properties).
         */
        for (int i=0; i<array.length; i++) {
            final int expected = Byte.toUnsignedInt(array[i]);
            assertEquals(expected, vector.shortValue (i));
            assertEquals(expected, vector.intValue   (i));
            assertEquals(expected, vector.floatValue (i));
            assertEquals(expected, vector.doubleValue(i));
            assertEquals(Short.valueOf((short) expected), vector.get(i));       // See above comment.
        }
        /*
         * Test exception for invalid narrowing cast.
         */
        assertEquals(106, vector.byteValue (50));
        assertEquals(146, vector.shortValue(70));
        var e = assertThrows(ArithmeticException.class, () -> vector.byteValue(70));
        assertNotNull(e);
        /*
         * Test writing.
         */
        vector.set(70, (short) 200);
        assertEquals(200, vector.shortValue(70));
    }

    /**
     * Tests {@link ArrayVector} backed by an array of float type.
     */
    @Test
    public void testFloatArray() {
        final float[] array = new float[400];
        for (int i=0; i<array.length; i++) {
            array[i] = (i + 100) * 10;
        }
        vector = Vector.create(array, false);
        assertEquals("Floats", vector.getClass().getSimpleName());
        assertSame(vector, Vector.create(vector, false));
        assertEquals(array.length, vector.size());
        assertEquals(Float.class, vector.getElementType());
        /*
         * Tests element values.
         */
        for (int i=0; i<array.length; i++) {
            assertEquals(array[i], vector.floatValue (i));
            assertEquals(array[i], vector.doubleValue(i));
        }
    }

    /**
     * Tests {@link ArrayVector} backed by an array of double type.
     */
    @Test
    public void testDoubleArray() {
        final double[] array = new double[400];
        for (int i=0; i<array.length; i++) {
            array[i] = (i + 100) * 10;
        }
        vector = Vector.create(array, false);
        assertEquals("Doubles", vector.getClass().getSimpleName());
        assertSame(vector, Vector.create(vector, false));
        assertEquals(array.length, vector.size());
        assertEquals(Double.class, vector.getElementType());
        /*
         * Test element values.
         */
        for (int i=0; i<array.length; i++) {
            assertEquals(array[i], vector.floatValue (i));
            assertEquals(array[i], vector.doubleValue(i));
        }
    }

    /**
     * Tests {@link Vector#reverse()}.
     */
    @Test
    public void testReverse() {
        final double[] array    = {2, 3, 8};
        final double[] expected = {8, 3, 2};
        assertEquals(Vector.create(expected, false),
                     Vector.create(array, false).reverse());
    }

    /**
     * Tests {@link Vector#concatenate(Vector)}.
     */
    @Test
    @SuppressWarnings("UnnecessaryBoxing")
    public void testConcatenate() {
        final float[] array = new float[40];
        for (int i=0; i<array.length; i++) {
            array[i] = i * 10;
        }
        final int[] extra = new int[20];
        for (int i=0; i<extra.length; i++) {
            extra[i] = (i + 40) * 10;
        }
        Vector v1 = Vector.create(array, false);
        Vector v2 = Vector.create(extra, false);
        Vector v3 = v1.concatenate(v2);
        assertEquals(60, v3.size(), "Length of V3 should be the sum of V1 and V2 length.");
        assertEquals(Number.class, v3.getElementType(), "Component type should be the common parent of V1 and V2.");
        assertEquals(Float  .valueOf(200), v3.get(20));
        assertEquals(Integer.valueOf(500), v3.get(50));
        for (int i=0; i<60; i++) {
            assertEquals(i*10, v3.floatValue(i));
        }
        assertSame(v1, v3.subList( 0, 40), "Should be able to restitute the original vector.");
        assertSame(v2, v3.subList(40, 60), "Should be able to restitute the original vector.");
        /*
         * Test concatenation of views at fixed indices.
         * Should be implemented as the concatenation of the indices arrays when possible.
         */
        final Vector expected = v3.pick(10, 25, 30, 0, 35, 39);
        v2 = v1.pick( 0, 35, 39);
        v1 = v1.pick(10, 25, 30);
        v3 = v1.concatenate(v2);
        assertEquals(expected, v3);
        assertFalse(v3 instanceof ConcatenatedVector, "Expected concatenation of the indices.");
    }

    /**
     * Tests {@link Vector#fill(int, int, Number)}.
     */
    @Test
    public void testFill() {
        vector = Vector.create(new int[] {2, 5, 3}, false).concatenate(Vector.create(new int[] {7, 2, 8}, false));
        vector.fill(2, 5, -1);
        assertContentEquals(Vector.create(new int[] {2, 5, -1, -1, -1, 8}, false), vector);
    }

    /**
     * Tests a vector backed by an array of strings.
     * This is not recommended, but happen in GDAL extensions of GeoTIFF.
     * See {@code org.apache.sis.storage.geotiff.Type.ASCII}.
     */
    @Test
    public void testStringArray() {
        vector = Vector.create(new String[] {"100", "80", "-20"}, false);
        assertEquals(  3, vector.size());
        assertEquals(100, vector.intValue(0));
        assertEquals( 80, vector.shortValue(1));
        assertEquals(-20, vector.doubleValue(2));
    }

    /**
     * Tests the {@link Vector#toString()} method on a vector of signed and unsigned bytes.
     */
    @Test
    public void testToString() {
        final byte[] array = new byte[] {(byte) 10, (byte) 100, (byte) 200};

        vector = Vector.create(array, true);
        assertEquals("[10, 100, 200]", vector.toString());

        vector = Vector.create(array, false);
        assertEquals("[10, 100, -56]", vector.toString());
    }

    /**
     * Tests {@link Vector#repetitions(int...)}.
     */
    @Test
    public void testRepetitions() {
        Vector vec = Vector.create(new int[] {
                10, 10, 10, 10,
                12, 12, 13, 12,             // Different value (13) break the regularity.
                15, 15, 15, 15}, false);
        assertArrayEquals(new int[] {}, vec.repetitions());

        vec = Vector.create(new int[] {
                10, 10, 10, 10,
                12, 12, 12, 12,
                15, 15, 15, 15}, false);
        assertArrayEquals(new int[] {4}, vec.repetitions());

        vec = Vector.create(new int[] {
                10, 10, 10,  12, 12, 12,  15, 15, 15,  18, 18, 18,
                10, 10, 10,  12, 12, 12,  15, 15, 15,  18, 18, 18,
                10, 10, 10,  12, 12, 12,  15, 15, 15,  18, 18, 18}, false);
        assertArrayEquals(new int[] {3,4}, vec.repetitions());

        vec = Vector.create(new int[] {
                10, 12, 15, 18,
                10, 12, 15, 18,
                10, 12, 15, 18}, false);
        assertArrayEquals(new int[] {1,4}, vec.repetitions());
    }

    /**
     * Tests {@link Vector#increment(double)}.
     */
    @Test
    public void testIncrement() {
        for (int type = 0; type <= 9; type++) {
            final Vector vec;
            switch (type) {
                case  0: vec = Vector.create(new double[] {  5,      8,     11,         14,         17}, false); break;
                case  1: vec = Vector.create(new float[]  { -5,     -2,      1,          4,          7}, false); break;
                case  2: vec = Vector.create(new long[]   { -5,     -2,      1,          4,          7}, false); break;
                case  3: vec = Vector.create(new long[]   {120,    123,    126,        129,        132}, true ); break;
                case  4: vec = Vector.create(new int[]    { -5,     -2,      1,          4,          7}, false); break;
                case  5: vec = Vector.create(new int[]    {120,    123,    126,        129,        132}, true ); break;
                case  6: vec = Vector.create(new short[]  { -5,     -2,      1,          4,          7}, false); break;
                case  7: vec = Vector.create(new short[]  {120,    123,    126,        129,        132}, true ); break;
                case  8: vec = Vector.create(new byte[]   { -5,     -2,      1,          4,          7}, false); break;
                case  9: vec = Vector.create(new byte[]   {120,    123,    126, (byte) 129, (byte) 132}, true ); break;
                default: throw new AssertionError(type);
            }
            String message = vec.getElementType().getSimpleName();
            if (vec.isUnsigned()) {
                message = "Unsigned " + message;
            }
            final Number inc = vec.increment(0);
            assertNotNull(inc, message);
            assertEquals(3, inc.doubleValue(), message);
            assertEquals(vec.getElementType(), inc.getClass(), message);
        }
    }

    /**
     * Tests {@link Vector#range()}. This test depends on most other tests defined in this {@code VectorTest} class
     * since it needs to test various combination of vectors and sub-vectors.
     */
    @Test
    public void testRange() {
        for (int type = 0; type <= 11; type++) {
            final Vector vec;
            switch (type) {
                case  0: vec = Vector.create(new double[] { 3,   2,   9,   7,   -8 }, false); break;
                case  1: vec = Vector.create(new float[]  { 3,   2,   9,   7,   -8 }, false); break;
                case  2: vec = Vector.create(new long[]   { 3,   2,   9,   7,   -8 }, false); break;
                case  3: vec = Vector.create(new long[]   { 3,   2,   9,   7,   -8 }, true ); break;
                case  4: vec = Vector.create(new int[]    { 3,   2,   9,   7,   -8 }, false); break;
                case  5: vec = Vector.create(new int[]    { 3,   2,   9,   7,   -8 }, true ); break;
                case  6: vec = Vector.create(new short[]  { 3,   2,   9,   7,   -8 }, false); break;
                case  7: vec = Vector.create(new short[]  { 3,   2,   9,   7,   -8 }, true ); break;
                case  8: vec = Vector.create(new byte[]   { 3,   2,   9,   7,   -8 }, false); break;
                case  9: vec = Vector.create(new byte[]   { 3,   2,   9,   7,   -8 }, true ); break;
                case 10: vec = Vector.create(new Number[] { 3,   2,   9,   7,   -8 }, false); break;
                case 11: vec = Vector.create(new String[] {"3", "2", "9", "7", "-8"}, false); break;
                default: throw new AssertionError(type);
            }
            String message = vec.getElementType().getSimpleName();
            if (vec.isUnsigned()) {
                message = "Unsigned " + message;
            }
            /*
             * Verify the minimum and maximum values of the {3, 2, 9, 7, -8} vector (signed case).
             * Those minimum and maximum are -8 and 9 respectively when interpreted as signed numbers.
             * In the unsigned case, -8 become some large positive number (how large depends on the type).
             * So the new minimum value in the unsigned case is 2 and the maximum value is type-dependent.
             */
            NumberRange<?> range = vec.range();
            if (vec.isUnsigned()) {
                assertEquals(2, range.getMinDouble(), message);
                assertTrue(range.getMaxDouble() > Byte.MAX_VALUE, message);
            } else {
                assertEquals(-8, range.getMinDouble(), message);
                assertEquals( 9, range.getMaxDouble(), message);
            }
            /*
             * Verify the minimum and maximum values of the {2, 7} vector.
             */
            final Vector sub = vec.subSampling(1, 2, 2);
            range = sub.range();
            assertEquals(2, range.getMinDouble(), message);
            assertEquals(7, range.getMaxDouble(), message);
            /*
             * Verify the minimum and maximum values of the {3, 9, 7} vector.
             */
            final Vector pick = vec.pick(0, 2, 3);
            range = pick.range();
            assertEquals(3, range.getMinDouble(), message);
            assertEquals(9, range.getMaxDouble(), message);
            /*
             * Verify the minimum and maximum values of the {3, 9, 7, 2, 7} vector.
             */
            final Vector union = sub.concatenate(pick);
            range = union.range();
            assertEquals(2, range.getMinDouble(), message);
            assertEquals(9, range.getMaxDouble(), message);
        }
    }

    /**
     * Tests {@link Vector#compress(double)}.
     */
    @Test
    public void testCompress() {
        /*
         * Values that can be not be compressed further. We use a byte[] array
         * with values different enough for requiring all 8 bits of byte type.
         */
        Vector vec =  Vector.create(new byte[] {30, 120, -50, -120}, false);
        Vector compressed = vec.compress(0);
        assertSame(vec, compressed);
        /*
         * Values that can be compressed as signed bytes.
         */
        vec =  Vector.create(new double[] {30, 120, -50, -120}, false);
        assertNotSame(vec, compressed = vec.compress(0));
        assertInstanceOf(ArrayVector.class, compressed);
        assertEquals(Byte.class, compressed.getElementType());
        assertFalse(compressed.isUnsigned());
        assertContentEquals(vec, compressed);
        /*
         * Values that can be compressed as unsigned bytes.
         */
        vec =  Vector.create(new float[] {30, 120, 250, 1}, false);
        assertNotSame(vec, compressed = vec.compress(0));
        assertInstanceOf(ArrayVector.class, compressed);
        assertEquals(Byte.class, compressed.getElementType());
        assertTrue(compressed.isUnsigned());
        assertContentEquals(vec, compressed);
        /*
         * Values that can be compressed as signed shorts.
         */
        vec =  Vector.create(new long[] {32000, 120, -25000, 14}, false);
        assertNotSame(vec, compressed = vec.compress(0));
        assertInstanceOf(ArrayVector.class, compressed);
        assertEquals(Short.class, compressed.getElementType());
        assertFalse(compressed.isUnsigned());
        assertContentEquals(vec, compressed);
        /*
         * Values that can be compressed as unsigned unsigned shorts.
         */
        vec =  Vector.create(new float[] {3, 60000, 25, 4}, false);
        assertNotSame(vec, compressed = vec.compress(0));
        assertInstanceOf(ArrayVector.class, compressed);
        assertEquals(Short.class, compressed.getElementType());
        assertTrue(compressed.isUnsigned());
        assertContentEquals(vec, compressed);
        /*
         * Values that can be compressed in a PackedVector.
         * Values below require less bits than the 'byte' type.
         * Note that we need at least PackedVector.MINIMAL_SIZE data for enabling this test.
         */
        vec =  Vector.create(new double[] {30, 27, 93, 72, -8, -3, 12, 4, 29, -5}, false);
        assertTrue(vec.size() >= PackedVector.MINIMAL_SIZE);
        assertNotSame(vec, compressed = vec.compress(0));
        assertInstanceOf(PackedVector.class, compressed);
        assertContentEquals(vec, compressed);
        /*
         * Vector that could be compressed in a PackedVector, but without advantage
         * because the number of bits required for storing the values is exactly 8.
         */
        vec =  Vector.create(new double[] {200, 100, 20, 80, 180, 10, 11, 12}, false);
        assertTrue(vec.size() >= PackedVector.MINIMAL_SIZE);
        assertNotSame(vec, compressed = vec.compress(0));
        assertInstanceOf(ArrayVector.class, compressed);
        assertEquals(Byte.class, compressed.getElementType());
        assertTrue(compressed.isUnsigned());
        assertContentEquals(vec, compressed);
        /*
         * Vector that can be compressed in a PackedVector as bytes with a factor of 20.
         */
        vec =  Vector.create(new double[] {200, 100, 20, 80, 180, 2000, 500, 120}, false);
        assertTrue(vec.size() >= PackedVector.MINIMAL_SIZE);
        assertNotSame(vec, compressed = vec.compress(0));
        assertInstanceOf(PackedVector.class, compressed);
        assertContentEquals(vec, compressed);
        /*
         * Values that can be compressed as float types.
         */
        vec =  Vector.create(new double[] {3.10, 60.59, -25.32, 4.78}, false);
        assertNotSame(vec, compressed = vec.compress(0));
        assertInstanceOf(ArrayVector.class, compressed);
        assertEquals(Float.class, compressed.getElementType());
        assertFalse(compressed.isUnsigned());
        assertContentEquals(vec, compressed);
    }

    /**
     * Asserts that the content of the given vector are equal.
     * The vectors do not need to use the same element type.
     */
    private static void assertContentEquals(final Vector expected, final Vector actual) {
        final int length = expected.size();
        assertEquals(length, actual.size());
        for (int i=0; i<length; i++) {
            assertEquals(expected.doubleValue(i), actual.doubleValue(i));
        }
    }
}
