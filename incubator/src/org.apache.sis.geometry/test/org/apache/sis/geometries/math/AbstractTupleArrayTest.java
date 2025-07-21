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

import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.MathTransforms;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class AbstractTupleArrayTest {

    protected static final double TOLERANCE = 0.0000001;
    protected static final String UNVALID_INDEX_EXPECTED = "Accessing value our of tuple size must cause an IndexOutOfBoundsException";

    protected abstract int[] getSupportedDimensions();

    /**
     * Created tuple array must have all values at zero.
     */
    protected abstract TupleArray create(int dim, int length);


    /**
     * Root test method, delegates to other methods.
     */
    @Test
    public void testTupleArray() throws TransformException{

        final int[] supportedDimensions = getSupportedDimensions();

        for (int i = 0; i < supportedDimensions.length; i++) {
            final int dim = supportedDimensions[i];
            final TupleArray array = create(dim, 5);
            assertEquals(dim, array.getDimension());
            assertEquals(5, array.getLength());
            testTupleArray(array);
        }

    }

    /**
     * Test a single tuple array.
     */
    private void testTupleArray(TupleArray array) throws TransformException {
        testGetSet(array);
        testToArray(array);
        testToArrayWithRange(array);
        testCopy(array);
        testResize(array);
        testCursor(array);
        testTransform(array);
    }

    /**
     * Test array resizing.
     */
    private void testResize(TupleArray array) {
        final int length = array.getLength();
        final int dimension = array.getDimension();
        int inc = 0;
        for (int i = 0, n = length; i < n; i++) {
            Vector tuple = Vectors.createDouble(dimension);
            for (int d = 0, dn = array.getDimension(); d < dn; d++) {
                tuple.set(d, inc++);
            }
            array.set(i, tuple);
        }

        final TupleArray resized = array.resize(array.getLength() + 10);
        assertEquals(length + 10, resized.getLength());

        //test values are still here
        inc = 0;
        for (int i = 0, n = length; i < n; i++) {
            Vector tuple = Vectors.createDouble(dimension);
            resized.get(i, tuple);
            for (int d = 0, dn = array.getDimension(); d < dn; d++) {
                assertEquals(inc++, tuple.get(d), TOLERANCE);
            }
        }
    }

    /**
     * Test array copy.
     */
    private void testCopy(TupleArray array) {
        final int length = array.getLength();
        final int dimension = array.getDimension();
        int inc = 0;
        for (int i = 0, n = length; i < n; i++) {
            Vector tuple = Vectors.createDouble(dimension);
            for (int d = 0, dn = array.getDimension(); d < dn; d++) {
                tuple.set(d, inc++);
            }
            array.set(i, tuple);
        }

        final TupleArray copy = array.copy();

        //test values are still here
        inc = 0;
        for (int i = 0, n = length; i < n; i++) {
            Vector tuple = Vectors.createDouble(dimension);
            copy.get(i, tuple);
            for (int d = 0, dn = array.getDimension(); d < dn; d++) {
                assertEquals(inc++, tuple.get(d), TOLERANCE);
            }
        }

        assertEquals(array, copy);
        assertFalse(array == copy);
    }

    /**
     * Test tuple array to arrays.
     */
    private void testToArray(TupleArray array) {
        final int length = array.getLength();
        final int dimension = array.getDimension();
        int inc = 0;
        for (int i = 0, n = length; i < n; i++) {
            Vector tuple = Vectors.createDouble(dimension);
            for (int d = 0; d < dimension; d++) {
                tuple.set(d, inc++);
            }
            array.set(i, tuple);
        }

        {//to short array
            final short[] table = array.toArrayShort();
            inc = 0;
            for (int i = 0, n = length; i < n; i++) {
                for (int d = 0; d < dimension; d++) {
                    assertEquals(inc++, table[i * dimension + d], TOLERANCE);
                }
            }
        }

        {//to int array
            final int[] table = array.toArrayInt();
            inc = 0;
            for (int i = 0, n = length; i < n; i++) {
                for (int d = 0; d < dimension; d++) {
                    assertEquals(inc++, table[i * dimension + d], TOLERANCE);
                }
            }
        }

        {//to float array
            final float[] table = array.toArrayFloat();
            inc = 0;
            for (int i = 0, n = length; i < n; i++) {
                for (int d = 0; d < dimension; d++) {
                    assertEquals(inc++, table[i * dimension + d], TOLERANCE);
                }
            }
        }

        {//to double array
            final double[] table = array.toArrayDouble();
            inc = 0;
            for (int i = 0, n = length; i < n; i++) {
                for (int d = 0; d < dimension; d++) {
                    assertEquals(inc++, table[i * dimension + d], TOLERANCE);
                }
            }
        }
    }

    /**
     * Test tuple array range to arrays.
     */
    private void testToArrayWithRange(TupleArray array) {
        final int length = array.getLength();
        if (length < 3) return;

        final int dimension = array.getDimension();
        int inc = 0;
        for (int i = 0, n = length; i < n; i++) {
            Vector tuple = Vectors.createDouble(dimension);
            for (int d = 0; d < dimension; d++) {
                tuple.set(d, inc++);
            }
            array.set(i, tuple);
        }

        int clip = 1;
        int rangeStart = clip;
        int rangeEnd = length - 2 * clip;
        int rangeLength = rangeEnd - rangeStart;
        {//to short array
            final short[] table = array.toArrayShort(rangeStart, rangeLength);
            inc = rangeStart*dimension;
            for (int i = 0; i < rangeLength; i++) {
                for (int d = 0; d < dimension; d++) {
                    assertEquals(inc++, table[i * dimension + d], TOLERANCE);
                }
            }
        }

        {//to int array
            final int[] table = array.toArrayInt(rangeStart, rangeLength);
            inc = rangeStart*dimension;
            for (int i = 0; i < rangeLength; i++) {
                for (int d = 0; d < dimension; d++) {
                    assertEquals(inc++, table[i * dimension + d], TOLERANCE);
                }
            }
        }

        {//to float array
            final float[] table = array.toArrayFloat(rangeStart, rangeLength);
            inc = rangeStart*dimension;
            for (int i = 0; i < rangeLength; i++) {
                for (int d = 0; d < dimension; d++) {
                    assertEquals(inc++, table[i * dimension + d], TOLERANCE);
                }
            }
        }

        {//to double array
            final double[] table = array.toArrayDouble(rangeStart, rangeLength);
            inc = rangeStart*dimension;
            for (int i = 0; i < rangeLength; i++) {
                for (int d = 0; d < dimension; d++) {
                    assertEquals(inc++, table[i * dimension + d], TOLERANCE);
                }
            }
        }
    }

    /**
     * Test array getter and setter methods.
     */
    private void testGetSet(TupleArray array) {
        final int length = array.getLength();
        final int dimension = array.getDimension();
        int inc = 0;
        for (int i = 0, n = length; i < n; i++) {
            final Vector tuple = Vectors.createDouble(dimension);
            for (int d = 0; d < dimension; d++) {
                tuple.set(d, inc++);
            }
            array.set(i, tuple);

            assertEquals(tuple, array.get(i));
            final Vector buffer = Vectors.createDouble(dimension);
            array.get(i, buffer);
            assertEquals(tuple, buffer);
        }
    }

    /**
     * Test array cursor.
     */
    private void testCursor(TupleArray array) {
        final int length = array.getLength();
        final int dimension = array.getDimension();
        int inc = 0;
        for (int i = 0, n = length; i < n; i++) {
            Vector tuple = Vectors.createDouble(dimension);
            for (int d = 0; d < dimension; d++) {
                tuple.set(d, inc++);
            }
            array.set(i, tuple);
        }

        final TupleArrayCursor cursor = array.cursor();
        inc = 0;
        for (int i = 0, n = length; i < n; i++) {
            assertTrue(cursor.next());

            final Vector tuple = Vectors.createDouble(dimension);
            for (int d = 0; d < dimension; d++) {
                tuple.set(d, inc++);
            }

            assertEquals(tuple, cursor.samples());
        }
        assertFalse(cursor.next());

        //check error on wrong index
        try {
            cursor.moveTo(-1);
            fail(UNVALID_INDEX_EXPECTED);
        } catch (IndexOutOfBoundsException ex) {
            //ok
        }
        try {
            cursor.moveTo(length);
            fail(UNVALID_INDEX_EXPECTED);
        } catch (IndexOutOfBoundsException ex) {
            //ok
        }
    }

    /**
     * Test array transform.
     */
    private void testTransform(TupleArray array) throws TransformException {
        final int length = array.getLength();
        final int dimension = array.getDimension();
        int inc = 0;
        for (int i = 0, n = length; i < n; i++) {
            Vector tuple = Vectors.createDouble(dimension);
            for (int d = 0; d < dimension; d++) {
                tuple.set(d, inc++);
            }
            array.set(i, tuple);
        }

        //add 2 to each sample
        final MatrixSIS matrix = Matrices.createIdentity(dimension +1);
        for (int d = 0; d < dimension; d++) {
            matrix.setElement(d, dimension, 2);
        }
        array.transform(MathTransforms.linear(matrix));

        inc = 0;
        for (int i = 0, n = length; i < n; i++) {
            Vector tuple = Vectors.create(dimension, array.getDataType());
            for (int d = 0; d < dimension; d++) {
                tuple.set(d, inc++ + 2);
            }
            assertEquals(tuple, array.get(i));
        }

    }
}
