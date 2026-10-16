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
    protected void testTuple(Vector<?> tuple) {
        testAllValue(tuple, 0.0);
        assertTrue(tuple.isAll(0.0));
        testCellGetSet(tuple);
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

        testLength(tuple);
        testLengthSquare(tuple);
        testNormalize(tuple);
        testAdd(tuple);
        testSubtract(tuple);
        testScale(tuple);
        testPerpendicular(tuple);
        testCross(tuple);
        testDot(tuple);
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
     * Test tuple value getters and setters.
     */
    private void testCellGetSet(Vector<?> tuple) {
        final int dim = tuple.getDimension();

        for (int i = 0; i < dim; i++) {
            tuple.set(i, i+1);
            assertEquals(i+1, tuple.get(i), TOLERANCE);
        }

        //test out of range
        try {
            tuple.set(-1, 10);
            fail(UNVALID_INDEX_EXPECTED);
        } catch (IndexOutOfBoundsException ex) {
            //ok
        }
        try {
            tuple.set(dim, 10);
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

        for (int i = 0; i < dim; i++) {
            tuple.set(i, i+1);
        }

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

    private void testEquality(Vector<?> tuple) {
        for (int i = 0; i < tuple.getDimension(); i++) {
            tuple.set(i, i+1);
        }

        //test equals itself
        assertEquals(tuple, tuple);

        //test equals a copy
        final Vector<?> copy = tuple.copy();
        assertEquals(tuple, copy);

        //test a equals a newly created tuple
        final Vector<?> newtuple = create(tuple.getDimension());
        assertFalse(newtuple.equals(tuple));
        newtuple.set(tuple);
        assertEquals(tuple, newtuple);

        if (tuple.getDataType() == DataType.FLOAT
         || tuple.getDataType() == DataType.DOUBLE) {
            //test NaN equality
            copy.set(0, Double.NaN);
            assertFalse(copy.equals(tuple));
            tuple.set(0, Double.NaN);
            assertEquals(copy, tuple);
        }

    }

    /**
     * Test copy in another data type.
     */
    private void testCopyDataType(Vector<?> tuple) {
        for (int i = 0; i < tuple.getDimension(); i++) {
            tuple.set(i, i+1);
        }
        for (final DataType dataType : new DataType[] {DataType.DOUBLE, DataType.FLOAT, DataType.SHORT}) {
            final Vector<?> copy = tuple.copy(dataType);
            assertEquals(dataType, copy.getDataType());
            assertEquals(tuple.getSampleSystem(), copy.getSampleSystem());
            assertArrayEquals(tuple.toArrayDouble(), copy.toArrayDouble(), TOLERANCE);
            //the copy is independent of the original tuple
            copy.set(0, 100);
            assertEquals(1, tuple.get(0), TOLERANCE);
        }
    }

    /**
     * Test the creation of a tuple from some axes of another one.
     */
    private void testSwizzle(Vector<?> tuple) {
        final int dim = tuple.getDimension();
        for (int i = 0; i < dim; i++) {
            tuple.set(i, i+1);
        }
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

        //the result is independent of the original tuple
        swizzled.set(1, 100);
        assertEquals(1, tuple.get(0), TOLERANCE);

        //test out of range
        try {
            tuple.swizzle(dim);
            fail(UNVALID_INDEX_EXPECTED);
        } catch (IndexOutOfBoundsException ex) {
            //ok
        }
    }

    private void testExtremum(Vector<?> tuple, double value) {

        for (int i=0;i<tuple.getDimension();i++) {
            //test get set
            tuple.set(i, value);
            assertEquals(value, tuple.get(i), 0.0);
            tuple.set(i, 0);

            //test copy
            tuple.set(i, value);
            Vector copy = tuple.copy();
            assertEquals(value, copy.get(i), 0.0);
            tuple.set(i, 0);

            //test array
            tuple.set(i, value);
            double[] array = tuple.toArrayDouble();
            assertEquals(value, array[i], 0.0);
            tuple.set(0, 0);
        }
    }

    /**
     * Test vector length.
     */
    private void testLength(Vector<?> vector) {
        final int dim = vector.getDimension();

        for (int i = 0; i < dim; i++) {
            vector.set(i, i+1);
        }

        assertEquals(Vectors.length(vector.toArrayDouble()), vector.length(), TOLERANCE);
    }

    /**
     * Test vector length squared.
     */
    private void testLengthSquare(Vector<?> vector) {
        final int dim = vector.getDimension();

        for (int i = 0; i < dim; i++) {
            vector.set(i, i+1);
        }

        assertEquals(Vectors.lengthSquare(vector.toArrayDouble()), vector.lengthSquare(), TOLERANCE);
    }

    /**
     * Test vector normalize.
     *
     * Only tested if vector type is Float or Double.
     * This operation requiere floating point precision.
     */
    private void testNormalize(Vector<?> vector) {
        final int dim = vector.getDimension();
        if (!(vector.getDataType() == DataType.FLOAT || vector.getDataType() == DataType.DOUBLE)) {
            return;
        }

        for (int i = 0; i < dim; i++) {
            vector.set(i, i+1);
        }

        final double[] expected = Vectors.normalize(vector.toArrayDouble());
        vector.normalize();
        assertArrayEquals(expected, vector.toArrayDouble(), TOLERANCE);
    }

    /**
     * Test vector add operation.
     */
    private void testAdd(Vector<?> vector) {
        final int dim = vector.getDimension();

        final Vector<?> second = Vectors.createDouble(dim);
        for (int i = 0; i < dim; i++) {
            vector.set(i, i+1);
            second.set(i, 5);
        }

        final double[] expected = Vectors.add(vector.toArrayDouble(), second.toArrayDouble());
        vector.add(second);
        assertArrayEquals(expected, vector.toArrayDouble(), TOLERANCE);

    }

    /**
     * Test vector subtract.
     */
    private void testSubtract(Vector<?> vector) {
        final int dim = vector.getDimension();

        final Vector second = Vectors.createDouble(dim);
        for (int i = 0; i < dim; i++) {
            vector.set(i, i+5);
            second.set(i, 5);
        }

        final double[] expected = Vectors.subtract(vector.toArrayDouble(), second.toArrayDouble());
        vector.subtract(second);
        assertArrayEquals(expected, vector.toArrayDouble(), TOLERANCE);
    }

    /**
     * Test vector scale.
     */
    private void testScale(Vector<?> vector) {
        final int dim = vector.getDimension();

        final double scale = 3;
        for (int i = 0; i < dim; i++) {
            vector.set(i, i+1);
        }

        final double[] expected = Vectors.scale(vector.toArrayDouble(), scale);
        vector.scale(scale);
        assertArrayEquals(expected, vector.toArrayDouble(), TOLERANCE);
    }

    /**
     * Test removal of the component along a unit vector.
     * The unit vector is along the first axis, so the result is exact for all data types.
     */
    private void testPerpendicular(Vector<?> vector) {
        final int dim = vector.getDimension();

        final Vector<?> unit = Vectors.createDouble(dim);
        unit.set(0, 1);
        for (int i = 0; i < dim; i++) {
            vector.set(i, i+1);
        }

        final double[] expected = vector.toArrayDouble();
        expected[0] = 0;
        vector.perpendicular(unit);
        assertArrayEquals(expected, vector.toArrayDouble(), TOLERANCE);
        assertEquals(0, vector.dot(unit), TOLERANCE);
    }

    /**
     * Test vector cross product.
     * Only tested if vector size is 3.
     */
    private void testCross(Vector<?> vector) {
        final int dim = vector.getDimension();
        if (dim != 3) return;


        final Vector<?> second = Vectors.createDouble(dim);
        for (int i = 0; i < dim; i++) {
            vector.set(i, i+1);
            second.set(i, 5);
        }

        final double[] expected = Vectors.cross(vector.toArrayDouble(), second.toArrayDouble());
        final Vector<?> cross = vector.cross(second);
        assertArrayEquals(expected, cross.toArrayDouble(), TOLERANCE);
    }

    /**
     * Test vector dot product.
     */
    private void testDot(Vector<?> vector) {
        final int dim = vector.getDimension();

        final Vector<?> second = Vectors.createDouble(dim);
        for (int i = 0; i < dim; i++) {
            vector.set(i, i+1);
            second.set(i, 5);
        }

        final double expected = Vectors.dot(vector.toArrayDouble(), second.toArrayDouble());
        assertEquals(expected, vector.dot(second), TOLERANCE);
    }
}
