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
package org.apache.sis.maths;

import java.util.Arrays;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.transform.MathTransforms;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class AbstractVectorTest {

    protected static final double TOLERANCE = 0.0000001;
    protected static final String UNVALID_INDEX_EXPECTED = "Accessing value our of tuple size must cause an IndexOutOfBoundsException";

    protected abstract int[] getSupportedDimensions();

    /**
     * Created tuple must have all values at zero.
     */
    protected abstract Vector<?> create(int dim);

    /**
     * Root test method, delegates to other methods.
     */
    @Test
    public void testTuple() throws Throwable{

        final int[] supportedDimensions = getSupportedDimensions();

        for (int i = 0; i < supportedDimensions.length; i++) {
            final int dim = supportedDimensions[i];
            final Vector<?> tuple = create(dim);
            try {
                assertEquals(dim, tuple.getDimension());
                testTuple(tuple);
            } catch (Throwable ex) {
                throw new Throwable(tuple.getClass().getName() +" " + ex.getMessage(), ex);
            }
        }

    }

    /**
     * Test a single tuple.
     */
    protected void testTuple(Vector<?> tuple) throws TransformException {
        testAllValue(tuple, 0.0);
        assertTrue(tuple.isAll(0.0));
        testCreate(tuple);
        testCellGet(tuple);
        testWith(tuple);
        testToArray(tuple);
        testEquality(tuple);
        testCopyDataType(tuple);
        testSwizzle(tuple);

        switch (tuple.getDataType()) {
            case BYTE :
                testExtremum(tuple, Byte.MIN_VALUE);
                testExtremum(tuple, Byte.MAX_VALUE);
                break;
            case SHORT :
                testExtremum(tuple, Short.MIN_VALUE);
                testExtremum(tuple, Short.MAX_VALUE);
                break;
            case USHORT :
                testExtremum(tuple, 0);
                testExtremum(tuple, 65535);
                break;
            case INT :
                testExtremum(tuple, Integer.MIN_VALUE);
                testExtremum(tuple, Integer.MAX_VALUE);
                break;
            case FLOAT :
                testExtremum(tuple, Float.MIN_VALUE);
                testExtremum(tuple, Float.MAX_VALUE);
                break;
            case DOUBLE :
                testExtremum(tuple, Double.MIN_VALUE);
                testExtremum(tuple, Double.MAX_VALUE);
                break;
        }

        final Vector<?> vector = vector(tuple, sequence(tuple.getDimension(), 1));
        testLength(vector);
        testCross(vector);
        testDot(vector);
        testDistance(vector);
        testVectorTo(vector);
        testTransformFunction(vector);
        testNaNInfinite(tuple);
        testUnsignedArithmetic(tuple);
        testNewVectorOperations(tuple);
    }

    /**
     * Create a vector of the same type as the given tuple with the given values.
     */
    private static Vector<?> vector(Vector<?> tuple, double... values) {
        final Vector<?> vector = Vectors.create(values, tuple.getDataType());
        assertEquals(tuple.getClass(), vector.getClass());
        return vector;
    }

    /**
     * Values (i+1)*factor.
     */
    private static double[] sequence(int dim, double factor) {
        final double[] values = new double[dim];
        for (int i = 0; i < dim; i++) {
            values[i] = (i+1) * factor;
        }
        return values;
    }

    /**
     * Test all tuple values equal the expected value.
     */
    private void testAllValue(Vector<?> tuple, double expectedValue) {
        final int dim = tuple.getDimension();

        for (int c = 0; c < dim; c++) {
            double value = tuple.get(c);
            assertEquals(expectedValue, value, TOLERANCE, "Value different at ["+c+"]");
        }

        assertTrue(tuple.isAll(expectedValue));
        assertFalse(tuple.isAll(expectedValue+1));
    }

    /**
     * Test creation of a vector with given values, it must be of the same class as the tuple.
     */
    private void testCreate(Vector<?> tuple) {
        final int dim = tuple.getDimension();
        final Vector<?> vector = vector(tuple, sequence(dim, 1));
        assertArrayEquals(sequence(dim, 1), vector.toArrayDouble(), TOLERANCE);
        //the tuple is unchanged
        testAllValue(tuple, 0.0);
    }

    /**
     * Test tuple value getter.
     */
    private void testCellGet(Vector<?> tuple) {
        final int dim = tuple.getDimension();
        final Vector<?> vector = vector(tuple, sequence(dim, 1));

        for (int i = 0; i < dim; i++) {
            assertEquals(i+1, vector.get(i), TOLERANCE);
        }

        //test out of range
        try {
            vector.get(-1);
            fail(UNVALID_INDEX_EXPECTED);
        } catch (IndexOutOfBoundsException ex) {
            //ok
        }
        try {
            vector.get(dim);
            fail(UNVALID_INDEX_EXPECTED);
        } catch (IndexOutOfBoundsException ex) {
            //ok
        }
    }

    /**
     * Test the method creating a new vector with one value replaced.
     * It must return a new vector of the same class and leave the original vector unchanged.
     */
    private void testWith(Vector<?> tuple) {
        final int dim = tuple.getDimension();
        final double[] values = sequence(dim, 1);
        final Vector<?> vector = vector(tuple, values);

        for (int i = 0; i < dim; i++) {
            final Vector<?> result = vector.with(i, 100);
            final double[] expected = values.clone();
            expected[i] = 100;
            assertNewVector(vector, vector(tuple, values), result, expected, TOLERANCE);
        }
        try {
            vector.with(dim, 10);
            fail(UNVALID_INDEX_EXPECTED);
        } catch (IndexOutOfBoundsException ex) {
            //ok
        }
    }

    /**
     * Test tuple value to array methods.
     */
    private void testToArray(Vector<?> tuple) {
        final int dim = tuple.getDimension();
        tuple = vector(tuple, sequence(dim, 1));

        {//test toShort
            final short[] values = tuple.toArrayShort();
            assertEquals(dim, values.length);
            for (int i = 0; i < dim; i++) {
                assertEquals(i+1, values[i], TOLERANCE);
            }
        }
        {//test toShort with buffer
            final short[] values = new short[dim + 3];
            tuple.toArrayShort(values, 3);
            for (int i = 0; i < dim; i++) {
                assertEquals(i+1, values[i + 3], TOLERANCE);
            }
        }
        {//test to int
            final int[] values = tuple.toArrayInt();
            assertEquals(dim, values.length);
            for (int i = 0; i < dim; i++) {
                assertEquals(i+1, values[i], TOLERANCE);
            }
        }
        {//test to int with buffer
            final int[] values = new int[dim + 3];
            tuple.toArrayInt(values, 3);
            for (int i = 0; i < dim; i++) {
                assertEquals(i+1, values[i + 3], TOLERANCE);
            }
        }
        {//test to float
            final float[] values = tuple.toArrayFloat();
            assertEquals(dim, values.length);
            for (int i = 0; i < dim; i++) {
                assertEquals(i+1, values[i], TOLERANCE);
            }
        }
        {//test to float with buffer
            final float[] values = new float[dim + 3];
            tuple.toArrayFloat(values, 3);
            for (int i = 0; i < dim; i++) {
                assertEquals(i+1, values[i + 3], TOLERANCE);
            }
        }
        {//test to double
            final double[] values = tuple.toArrayDouble();
            assertEquals(dim, values.length);
            for (int i = 0; i < dim; i++) {
                assertEquals(i+1, values[i], TOLERANCE);
            }
        }
        {//test to double with buffer
            final double[] values = new double[dim + 3];
            tuple.toArrayDouble(values, 3);
            for (int i = 0; i < dim; i++) {
                assertEquals(i+1, values[i + 3], TOLERANCE);
            }
        }

    }

    private void testEquality(Vector<?> zero) {
        final int dim = zero.getDimension();
        final Vector<?> tuple = vector(zero, sequence(dim, 1));

        //test equals itself
        assertEquals(tuple, tuple);

        //test equals a copy
        assertEquals(tuple, tuple.copy());

        //test a equals a newly created tuple
        assertFalse(zero.equals(tuple));
        assertEquals(tuple, vector(zero, sequence(dim, 1)));

        if (tuple.getDataType() == DataType.FLOAT
         || tuple.getDataType() == DataType.DOUBLE) {
            //test NaN equality
            final double[] values = sequence(dim, 1);
            values[0] = Double.NaN;
            final Vector<?> nan = vector(zero, values);
            assertFalse(nan.equals(tuple));
            assertEquals(nan, vector(zero, values));

            //test signed zero equality, which shall give the same hash code
            values[0] = 0.0;
            final Vector<?> positive = vector(zero, values);
            values[0] = -0.0;
            final Vector<?> negative = vector(zero, values);
            assertEquals(positive, negative);
            assertEquals(positive.hashCode(), negative.hashCode());
        }

    }

    /**
     * Test copy in another data type.
     */
    private void testCopyDataType(Vector<?> tuple) {
        tuple = vector(tuple, sequence(tuple.getDimension(), 1));
        for (final DataType dataType : new DataType[] {DataType.DOUBLE, DataType.FLOAT, DataType.SHORT}) {
            final Vector<?> copy = tuple.copy(dataType);
            assertEquals(dataType, copy.getDataType());
            assertEquals(tuple.getDimension(), copy.getDimension());
            assertArrayEquals(tuple.toArrayDouble(), copy.toArrayDouble(), TOLERANCE);
        }
    }

    /**
     * Test the creation of a tuple from some axes of another one.
     */
    private void testSwizzle(Vector<?> tuple) {
        final int dim = tuple.getDimension();
        tuple = vector(tuple, sequence(dim, 1));
        //reverse order
        final int[] reverse = new int[dim];
        final double[] expected = new double[dim];
        for (int i = 0; i < dim; i++) {
            reverse[i] = dim - 1 - i;
            expected[i] = dim - i;
        }
        Vector<?> swizzled = tuple.swizzle(reverse);
        assertEquals(tuple.getDataType(), swizzled.getDataType());
        assertArrayEquals(expected, swizzled.toArrayDouble(), TOLERANCE);

        //repeated and omitted axes
        swizzled = tuple.swizzle(dim - 1, 0, dim - 1);
        assertArrayEquals(new double[] {dim, 1, dim}, swizzled.toArrayDouble(), TOLERANCE);

        //test out of range
        try {
            tuple.swizzle(dim);
            fail(UNVALID_INDEX_EXPECTED);
        } catch (IndexOutOfBoundsException ex) {
            //ok
        }
    }

    private void testExtremum(Vector<?> tuple, double value) {
        final int dim = tuple.getDimension();
        for (int i = 0; i < dim; i++) {
            final double[] values = new double[dim];
            values[i] = value;
            final Vector<?> vector = vector(tuple, values);

            //test get
            assertEquals(value, vector.get(i), 0.0);

            //test copy
            assertEquals(value, vector.copy().get(i), 0.0);

            //test array
            assertEquals(value, vector.toArrayDouble()[i], 0.0);
        }
    }

    /**
     * Test vector length and length squared.
     */
    private void testLength(Vector<?> vector) {
        assertEquals(Vectors.length(vector.toArrayDouble()), vector.length(), TOLERANCE);
        assertEquals(Vectors.lengthSquare(vector.toArrayDouble()), vector.lengthSquare(), TOLERANCE);
    }

    /**
     * Test vector cross product.
     * Only tested if vector size is 3.
     */
    private void testCross(Vector<?> vector) {
        if (vector.getDimension() != 3) return;
        final Vector<?> second = new Vector3D.Double(5, 5, 5);
        final double[] expected = Vectors.cross(vector.toArrayDouble(), second.toArrayDouble());
        assertArrayEquals(expected, vector.cross(second).toArrayDouble(), TOLERANCE);
    }

    /**
     * Test vector dot product.
     */
    private void testDot(Vector<?> vector) {
        final double[] values = new double[vector.getDimension()];
        Arrays.fill(values, 5);
        final Vector<?> second = Vectors.create(values, DataType.DOUBLE);
        final double expected = Vectors.dot(vector.toArrayDouble(), second.toArrayDouble());
        assertEquals(expected, vector.dot(second), TOLERANCE);
        assertThrows(IllegalArgumentException.class, () -> vector.dot(Vectors.createDouble(vector.getDimension() + 1)));
    }

    /**
     * Test applying a function on a vector.
     */
    private void testTransformFunction(Vector<?> vector) {
        assertSame(vector, vector.transform((v) -> v));
        assertEquals(vector.getDimension(), (int) vector.transform(Vector::getDimension));
        final Vector<?> scaled = vector.transform((v) -> v.scale(2));
        assertArrayEquals(Vectors.scale(vector.toArrayDouble(), 2), scaled.toArrayDouble(), TOLERANCE);
    }

    /**
     * Test distance between two vectors.
     */
    private void testDistance(Vector<?> vector) {
        final double[] values = new double[vector.getDimension()];
        Arrays.fill(values, 5);
        final Vector<?> second = Vectors.create(values, DataType.DOUBLE);
        final double[] diff = Vectors.subtract(vector.toArrayDouble(), values);
        assertEquals(Vectors.lengthSquare(diff), vector.distanceSquare(second), TOLERANCE);
        assertEquals(Vectors.length(diff), vector.distance(second), TOLERANCE);
        assertEquals(0, vector.distance(vector), TOLERANCE);
        assertThrows(IllegalArgumentException.class, () -> vector.distance(Vectors.createDouble(vector.getDimension() + 1)));
    }

    /**
     * Test the vector from this vector to another one.
     */
    private void testVectorTo(Vector<?> vector) {
        final double[] values = new double[vector.getDimension()];
        Arrays.fill(values, 10);
        final Vector<?> second = Vectors.create(values, DataType.DOUBLE);
        final Vector<?> result = vector.vectorTo(second);
        assertEquals(vector.getClass(), result.getClass());
        assertArrayEquals(Vectors.subtract(values, vector.toArrayDouble()), result.toArrayDouble(), TOLERANCE);
    }

    /**
     * Test NaN and infinite detection.
     * Only floating point types can hold such values.
     */
    private void testNaNInfinite(Vector<?> tuple) {
        assertFalse(tuple.isNaN());
        assertFalse(tuple.isInfinite());
        if (!(tuple.getDataType() == DataType.FLOAT || tuple.getDataType() == DataType.DOUBLE)) {
            return;
        }
        final int dim = tuple.getDimension();
        final double[] values = sequence(dim, 1);
        values[dim - 1] = Double.POSITIVE_INFINITY;
        Vector<?> vector = vector(tuple, values);
        assertFalse(vector.isNaN());
        assertTrue(vector.isInfinite());
        assertFalse(vector.isFinite());

        values[0] = Double.NaN;
        vector = vector(tuple, values);
        assertTrue(vector.isNaN());
        assertFalse(vector.isInfinite(), "a vector with NaN is not infinite");
        assertFalse(vector.isFinite());
    }

    /**
     * Test arithmetic on unsigned data types with values above the signed maximum.
     */
    private void testUnsignedArithmetic(Vector<?> tuple) {
        final double value;
        switch (tuple.getDataType()) {
            case UBYTE  : value = 200; break;
            case USHORT : value = 40000; break;
            default : return;
        }
        final int dim = tuple.getDimension();
        final double[] values = new double[dim];
        Arrays.fill(values, value);
        final Vector<?> vector = vector(tuple, values);

        final Vector<?> half = vector.scale(0.5);
        assertTrue(half.isAll(value / 2), "scale");

        Arrays.fill(values, value / 4);
        final Vector<?> second = Vectors.create(values, DataType.DOUBLE);
        final Vector<?> sum = half.add(second);
        assertTrue(sum.isAll(value * 3 / 4), "add");
        assertTrue(sum.subtract(second).isAll(value / 2), "subtract");
    }

    /**
     * Test the operations returning a new vector.
     * They must leave this vector unchanged and give the same result as the array operations.
     */
    private void testNewVectorOperations(Vector<?> tuple) throws TransformException {
        final int dim = tuple.getDimension();

        final double[] v = sequence(dim, 4);
        final double[] s = new double[dim];
        Arrays.fill(s, 2);
        final double[] u = new double[dim];
        u[0] = 1;

        final Vector<?> vector = vector(tuple, v);
        final Vector<?> second = Vectors.create(s, DataType.DOUBLE);
        final Vector<?> unit = Vectors.create(u, DataType.DOUBLE);
        final Vector<?> original = vector.copy();

        final double[] perpendicular = v.clone();
        perpendicular[0] = 0;
        final double[] factors = new double[dim];
        Arrays.fill(factors, 3);

        assertNewVector(vector, original, vector.add(second),         Vectors.add(v, s), TOLERANCE);
        assertNewVector(vector, original, vector.subtract(second),    Vectors.subtract(v, s), TOLERANCE);
        assertNewVector(vector, original, vector.multiply(second),    Vectors.multiply(v, s), TOLERANCE);
        assertNewVector(vector, original, vector.divide(second),      Vectors.divide(v, s), TOLERANCE);
        assertNewVector(vector, original, vector.scale(3),            Vectors.scale(v, 3), TOLERANCE);
        assertNewVector(vector, original, vector.perpendicular(unit), perpendicular, TOLERANCE);
        assertNewVector(vector, original, vector.lerp(second, 0.5),   Vectors.lerp(v, s, 0.5, null), TOLERANCE);
        assertNewVector(vector, original, vector.transform(MathTransforms.scale(factors)), Vectors.scale(v, 3), TOLERANCE);

        switch (vector.getDataType()) {
            case UBYTE : case USHORT : case UINT : break;
            default : assertNewVector(vector, original, vector.negate(), Vectors.negate(v), TOLERANCE);
        }
        if (vector.getDataType() == DataType.FLOAT || vector.getDataType() == DataType.DOUBLE) {
            assertNewVector(vector, original, vector.normalize(), Vectors.normalize(v), TOLERANCE);
            if (dim >= 2) {
                assertNewVector(vector, original, vector.slerp(unit, 0.5), Vectors.slerp(v, u, 0.5, null), 1e-5);
            }
        }

        /*
         * Same operations through the default implementations of Vector.
         * The result is a new vector of the same data type.
         */
        final Vector<?> unmodifiable = Vectors.unmodifiable(vector);
        Vector<?> result = unmodifiable.add(second);
        assertEquals(vector.getClass(), result.getClass());
        assertArrayEquals(Vectors.add(v, s), result.toArrayDouble(), TOLERANCE);
        result = unmodifiable.lerp(second, 0.5);
        assertArrayEquals(Vectors.lerp(v, s, 0.5, null), result.toArrayDouble(), TOLERANCE);
        result = unmodifiable.transform(MathTransforms.scale(factors));
        assertArrayEquals(Vectors.scale(v, 3), result.toArrayDouble(), TOLERANCE);
        assertEquals(original, vector, "Vector must not be modified");
    }

    private static void assertNewVector(Vector<?> vector, Vector<?> original, Vector<?> result, double[] expected, double tolerance) {
        assertNotSame(vector, result);
        assertEquals(original, vector, "Vector must not be modified");
        assertEquals(vector.getClass(), result.getClass());
        assertArrayEquals(expected, result.toArrayDouble(), tolerance);
    }
}
