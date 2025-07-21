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
package org.apache.sis.geometries.math;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class AbstractTupleTest {

    protected static final double TOLERANCE = 0.0000001;
    protected static final String UNVALID_INDEX_EXPECTED = "Accessing value our of tuple size must cause an IndexOutOfBoundsException";

    protected abstract int[] getSupportedDimensions();

    /**
     * Created tuple must have all values at zero.
     */
    protected abstract Tuple<?> create(int dim);

    /**
     * Root test method, delegates to other methods.
     */
    @Test
    public void testTuple() throws Throwable{

        final int[] supportedDimensions = getSupportedDimensions();

        for (int i = 0; i < supportedDimensions.length; i++) {
            final int dim = supportedDimensions[i];
            final Tuple<?> tuple = create(dim);
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
    protected void testTuple(Tuple<?> tuple) {
        testAllValue(tuple, 0.0);
        assertTrue(tuple.isAll(0.0));
        testCellGetSet(tuple);
        testToArray(tuple);
        testEquality(tuple);

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
    }

    /**
     * Test all tuple values equal the expected value.
     */
    private void testAllValue(Tuple<?> tuple, double expectedValue) {
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
    private void testCellGetSet(Tuple<?> tuple) {
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
    private void testToArray(Tuple<?> tuple) {
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

    private void testEquality(Tuple<?> tuple) {
        for (int i = 0; i < tuple.getDimension(); i++) {
            tuple.set(i, i+1);
        }

        //test equals itself
        assertEquals(tuple, tuple);

        //test equals a copy
        final Tuple<?> copy = tuple.copy();
        assertEquals(tuple, copy);

        //test a equals a newly created tuple
        final Tuple<?> newtuple = create(tuple.getDimension());
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

    private void testExtremum(Tuple<?> tuple, double value) {

        for (int i=0;i<tuple.getDimension();i++) {
            //test get set
            tuple.set(i, value);
            assertEquals(value, tuple.get(i), 0.0);
            tuple.set(i, 0);

            //test copy
            tuple.set(i, value);
            Tuple copy = tuple.copy();
            assertEquals(value, copy.get(i), 0.0);
            tuple.set(i, 0);

            //test array
            tuple.set(i, value);
            double[] array = tuple.toArrayDouble();
            assertEquals(value, array[i], 0.0);
            tuple.set(0, 0);
        }
    }
}
