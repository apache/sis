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

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class AbstractVectorTest extends AbstractTupleTest {

    @Override
    protected void testTuple(Tuple<?> tuple) {
        super.testTuple(tuple);

        final Vector vector = (Vector) tuple;
        testLength(vector);
        testLengthSquare(vector);
        testNormalize(vector);
        testAdd(vector);
        testSubtract(vector);
        testScale(vector);
        testCross(vector);
        testDot(vector);
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
