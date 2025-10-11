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
package org.apache.sis.referencing.operation.matrix;

import static java.lang.Double.NaN;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.cs.AxisDirection;
import static org.opengis.referencing.cs.AxisDirection.*;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.internal.shared.DoubleDouble;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.util.iso.Types;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertMultilinesEquals;

// Specific to the main branch:
import static org.apache.sis.test.GeoapiAssert.assertMatrixEquals;


/**
 * Tests the {@link Matrices} implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 */
@SuppressWarnings("exports")
public final class MatricesTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public MatricesTest() {
    }

    /**
     * Tests {@link Matrices#create(int, int, Number[])}.
     */
    @Test
    public void testCreateFromNumbers() {
        final int SIZE = Matrix3.SIZE;
        final Matrix3 expected = new Matrix3(
                  1,    2,    3,
                0.1,  0.2,  StrictMath.PI,
                 -1,   -2,   -3);
        final Number[] elements = {
                  1,    2,    3,
                0.1,  0.2,  DoubleDouble.PI,
                 -1,   -2,   -3};

        final MatrixSIS matrix = Matrices.create(SIZE, SIZE, elements);
        assertExtendedPrecision(matrix);
        assertNotEquals(expected, matrix);
        assertTrue(Matrices.equals(expected, matrix, ComparisonMode.BY_CONTRACT));
    }

    /**
     * Tests {@link Matrices#createTransform(AxisDirection[], AxisDirection[])} with the same sequence of axes.
     * The result shall be an identity matrix.
     *
     * {@code Matrices.createTransform(AxisDirection[], AxisDirection[])} needs to be tested with special care,
     * because this method will be the most frequently invoked one when building CRS.
     */
    @Test
    public void testCreateTransformWithSameAxes() {
        final MatrixSIS matrix = Matrices.createTransform(
                new AxisDirection[] {NORTH, EAST, UP},
                new AxisDirection[] {NORTH, EAST, UP});

        assertExtendedPrecision(matrix);
        assertTrue(matrix.isAffine());
        assertTrue(matrix.isIdentity());
        assertEquals(4, matrix.getNumRow());
        assertEquals(4, matrix.getNumCol());
    }

    /**
     * Tests {@link Matrices#createTransform(AxisDirection[], AxisDirection[])} with different axes.
     * Axes are moved in different positions, and some axes will have opposite directions.
     * However, the number of axes stay the same.
     *
     * {@code Matrices.createTransform(AxisDirection[], AxisDirection[])} needs to be tested with special care,
     * because this method will be the most frequently invoked one when building CRS.
     */
    @Test
    public void testCreateTransformWithDifferentAxes() {
        final MatrixSIS matrix = Matrices.createTransform(
                new AxisDirection[] {NORTH, EAST, UP},
                new AxisDirection[] {WEST, UP, SOUTH});

        assertExtendedPrecision(matrix);
        assertTrue (matrix.isAffine());
        assertFalse(matrix.isIdentity());
        assertEquals(4, matrix.getNumRow());
        assertEquals(4, matrix.getNumCol());
        Matrix expected = Matrices.create(4, 4, new double[] {
            0,-1, 0, 0,
            0, 0, 1, 0,
           -1, 0, 0, 0,
            0, 0, 0, 1
        });
        assertMatrixEquals(expected, matrix, "(N,E,U) → (W,U,S)");
    }

    /**
     * Tests {@link Matrices#createTransform(AxisDirection[], AxisDirection[])} with less axes
     * in the destination than in the source.
     *
     * {@code Matrices.createTransform(AxisDirection[], AxisDirection[])} needs to be tested with special care,
     * because this method will be the most frequently invoked one when building CRS.
     */
    @Test
    public void testCreateTransformWithLessAxes() {
        final MatrixSIS matrix = Matrices.createTransform(
                new AxisDirection[] {NORTH, EAST, UP},
                new AxisDirection[] {DOWN, NORTH});

        assertExtendedPrecision(matrix);
        assertFalse (matrix.isIdentity());
        assertEquals(3, matrix.getNumRow());
        assertEquals(4, matrix.getNumCol());
        Matrix expected = Matrices.create(3, 4, new double[] {
            0, 0,-1, 0,
            1, 0, 0, 0,
            0, 0, 0, 1
        });
        assertMatrixEquals(expected, matrix, "(N,E,U) → (D,N)");
    }

    /**
     * Tests {@link Matrices#createTransform(AxisDirection[], AxisDirection[])} with the axis repeated twice.
     * This unusual, but shall nevertheless be supported.
     *
     * {@code Matrices.createTransform(AxisDirection[], AxisDirection[])} needs to be tested with special care,
     * because this method will be the most frequently invoked one when building CRS.
     */
    @Test
    public void testCreateTransformWithRepeatedAxes() {
        final MatrixSIS matrix = Matrices.createTransform(
                new AxisDirection[] {NORTH, EAST, UP},
                new AxisDirection[] {DOWN, DOWN});

        assertExtendedPrecision(matrix);
        assertFalse (matrix.isIdentity());
        assertEquals(3, matrix.getNumRow());
        assertEquals(4, matrix.getNumCol());
        Matrix expected = Matrices.create(3, 4, new double[] {
            0, 0,-1, 0,
            0, 0,-1, 0,
            0, 0, 0, 1
        });
        assertMatrixEquals(expected, matrix, "(N,E,U) → (D,D)");
    }

    /**
     * Tests that {@link Matrices#createTransform(AxisDirection[], AxisDirection[])}
     * throw an exception if a destination axis is not in the source.
     *
     * {@code Matrices.createTransform(AxisDirection[], AxisDirection[])} needs to be tested with special care,
     * because this method will be the most frequently invoked one when building CRS.
     */
    @Test
    public void testCreateTransformWithAxisNotInSource() {
        var e = assertThrows(IllegalArgumentException.class,
                () -> Matrices.createTransform(
                        new AxisDirection[] {NORTH, EAST, UP},
                        new AxisDirection[] {DOWN, GEOCENTRIC_X}));
        assertMessageContainsDirection(e, GEOCENTRIC_X);
    }

    /**
     * Tests that {@link Matrices#createTransform(AxisDirection[], AxisDirection[])}
     * throw an exception if the arguments contain colinear axis directions.
     *
     * {@code Matrices.createTransform(AxisDirection[], AxisDirection[])} needs to be tested with special care,
     * because this method will be the most frequently invoked one when building CRS.
     */
    @Test
    public void testCreateTransformWithColinearAxes() {
        var e = assertThrows(IllegalArgumentException.class,
                () -> Matrices.createTransform(
                        new AxisDirection[] {NORTH, EAST, UP, WEST},
                        new AxisDirection[] {NORTH, EAST, UP}));
        assertMessageContainsDirection(e, EAST);
        assertMessageContainsDirection(e, WEST);
    }

    /**
     * Asserts that the message of the given exception contains the given axis direction.
     */
    private static void assertMessageContainsDirection(final Throwable e, final AxisDirection direction) {
        final String message = e.getMessage();
        final String label = Types.getCodeTitle(direction).toString();
        if (!message.contains(label)) {
            fail("Direction \"" + label + "\" not found in error message: " + message);
        }
    }

    /**
     * Asserts that the given matrix uses extended precision. This is mandatory for all matrices
     * returned by {@link Matrices#createTransform(AxisDirection[], AxisDirection[])} because
     * {@code CoordinateSystems.swapAndScaleAxes(…)} will modify those matrices in-place with
     * the assumption that they accept extended precision.
     *
     * @param  matrix  the matrix to test.
     */
    private static void assertExtendedPrecision(final Matrix matrix) {
        assertTrue(matrix instanceof GeneralMatrix);
    }

    /**
     * Tests {@link Matrices#createTransform(Envelope, Envelope)}.
     * This method tests the example given in {@code Matrices.createTransform(…)} javadoc.
     */
    @Test
    public void testCreateTransformFromEnvelopes() {
        final Envelope srcEnvelope = new Envelope2D(null, -20, -40, 100, 200);
        final Envelope dstEnvelope = new Envelope2D(null, -10, -25, 300, 500);
        MatrixSIS matrix = Matrices.createTransform(srcEnvelope, dstEnvelope);
        assertTrue (matrix.isAffine());
        assertFalse(matrix.isIdentity());
        assertEquals(3, matrix.getNumRow());
        assertEquals(3, matrix.getNumCol());
        assertEquals(Matrices.create(3, 3, new double[] {
            3.0,  0,    50,
            0,    2.5,  75,
            0,    0,     1
        }), matrix);
        /*
         * Test dropping a dimension.
         */
        final GeneralEnvelope expanded = new GeneralEnvelope(3);
        expanded.subEnvelope(0, 2).setEnvelope(srcEnvelope);
        expanded.setRange(2, 1000, 2000);
        matrix = Matrices.createTransform(expanded, dstEnvelope);
        assertEquals(3, matrix.getNumRow());
        assertEquals(4, matrix.getNumCol());
        assertEquals(Matrices.create(3, 4, new double[] {
            3.0,  0,   0,  50,
            0,    2.5, 0,  75,
            0,    0,   0,   1
        }), matrix);
        /*
         * Test adding a dimension with coordinate values set to zero.
         */
        expanded.subEnvelope(0, 2).setEnvelope(dstEnvelope);
        matrix = Matrices.createTransform(srcEnvelope, expanded);
        assertEquals(4, matrix.getNumRow());
        assertEquals(3, matrix.getNumCol());
        assertEquals(Matrices.create(4, 3, new double[] {
            3.0,  0,    50,
            0,    2.5,  75,
            0,    0,     0,
            0,    0,     1
        }), matrix);
    }

    /**
     * Tests {@link Matrices#createTransform(Envelope, AxisDirection[], Envelope, AxisDirection[])}.
     * This method tests the example given in {@code Matrices.createTransform(…)} javadoc.
     */
    @Test
    public void testCreateTransformFromEnvelopesAndAxes() {
        final Envelope srcEnvelope = new Envelope2D(null, -40, +20, 200, 100); // swapped (y,-x)
        final Envelope dstEnvelope = new Envelope2D(null, -10, -25, 300, 500);
        MatrixSIS matrix = Matrices.createTransform(
                srcEnvelope, new AxisDirection[] {NORTH, WEST},
                dstEnvelope, new AxisDirection[] {EAST, NORTH});
        assertTrue (matrix.isAffine());
        assertFalse(matrix.isIdentity());
        assertEquals(3, matrix.getNumRow());
        assertEquals(3, matrix.getNumCol());
        Matrix expected = Matrices.create(3, 3, new double[] {
            0,   -3.0, 350,
            2.5,  0,    75,
            0,    0,     1
        });
        assertMatrixEquals(expected, matrix, "(N,E) → (E,N)");
        /*
         * Test dropping a dimension.
         */
        final GeneralEnvelope expanded = new GeneralEnvelope(3);
        expanded.subEnvelope(0, 2).setEnvelope(srcEnvelope);
        expanded.setRange(2, 1000, 2000);
        matrix = Matrices.createTransform(
                expanded,    new AxisDirection[] {NORTH, WEST, UP},
                dstEnvelope, new AxisDirection[] {EAST, NORTH});
        assertEquals(3, matrix.getNumRow());
        assertEquals(4, matrix.getNumCol());
        expected = Matrices.create(3, 4, new double[] {
            0,   -3.0, 0, 350,
            2.5,  0,   0,  75,
            0,    0,   0,   1
        });
        assertMatrixEquals(expected, matrix, "(N,E,U) → (E,N)");
    }

    /**
     * Tests {@link Matrices#createDimensionSelect(int, int[])}.
     * This method tests the example given in {@code Matrices.createDimensionSelect(…)} javadoc.
     */
    @Test
    public void testCreateDimensionSelect() {
        final MatrixSIS matrix = Matrices.createDimensionSelect(4, new int[] {1, 0, 3});
        assertEquals(Matrices.create(4, 5, new double[] {
            0, 1, 0, 0, 0,
            1, 0, 0, 0, 0,
            0, 0, 0, 1, 0,
            0, 0, 0, 0, 1
        }), matrix);
    }

    /**
     * Tests {@link Matrices#createPassThrough(int, Matrix, int)} with dimensions
     * added both before and after the sub-matrix.
     */
    @Test
    public void testCreatePassThrough() {
        MatrixSIS matrix = Matrices.create(3, 4, new double[] {
            2, 0, 3, 8,
            0, 4, 7, 5,
            0, 0, 0, 1
        });
        matrix = Matrices.createPassThrough(2, matrix, 1);
        assertEquals(Matrices.create(6, 7, new double[] {
            1, 0, 0, 0, 0, 0, 0,        // Dimension added
            0, 1, 0, 0, 0, 0, 0,        // Dimension added
            0, 0, 2, 0, 3, 0, 8,        // Sub-matrix, row 0
            0, 0, 0, 4, 7, 0, 5,        // Sub-matrix, row 1
            0, 0, 0, 0, 0, 1, 0,        // Dimension added
            0, 0, 0, 0, 0, 0, 1         // Last sub-matrix row
        }), matrix);
    }

    /**
     * Tests {@link Matrices#createAffine(Matrix, DirectPosition)}.
     */
    @Test
    public void testCreateAffine() {
        MatrixSIS derivative = Matrices.create(2, 3, new double[] {
            2, 3, 8,
            0, 7, 5
        });
        DirectPosition translation = new DirectPosition2D(-3, 9);
        assertEquals(Matrices.create(3, 4, new double[] {
            2, 3, 8, -3,
            0, 7, 5,  9,
            0, 0, 0,  1
        }), Matrices.createAffine(derivative, translation));
    }

    /**
     * Tests {@link Matrices#resizeAffine(Matrix, int, int)}.
     */
    @Test
    public void testResizeAffine() {
        // Add dimensions
        MatrixSIS matrix = Matrices.create(3, 4, new double[] {
            2, 0, 3, 8,
            0, 4, 7, 5,
            0, 0, 0, 1
        });
        assertEquals(Matrices.create(5, 6, new double[] {
            2, 0, 3, 0, 0, 8,
            0, 4, 7, 0, 0, 5,
            0, 0, 1, 0, 0, 0,
            0, 0, 0, 1, 0, 0,
            0, 0, 0, 0, 0, 1
        }), Matrices.resizeAffine(matrix, 5, 6));

        // Remove dimensions
        matrix = Matrices.create(4, 5, new double[] {
            1, 2, 7, 8, 9,
            3, 4, 6, 7, 8,
            9, 8, 7, 6, 5,
            4, 3, 2, 1, -1
        });
        // Matrix type is different in the following test.
        Matrix expected = Matrices.create(3, 3, new double[] {
            1, 2, 9,
            3, 4, 8,
            4, 3, -1
        });
        assertMatrixEquals(expected, Matrices.resizeAffine(matrix, 3, 3), "To square matrix");
    }

    /**
     * Tests {@link MatrixSIS#removeRows(int, int)}
     */
    @Test
    public void testRemoveRows() {
        MatrixSIS matrix = Matrices.create(4, 5, new double[] {
            1, 2, 7, 8, 9,
            3, 4, 6, 7, 8,
            9, 8, 7, 6, 5,
            4, 3, 2, 1, 0
        });
        matrix = matrix.removeRows(3, 4);
        assertEquals(Matrices.create(3, 5, new double[] {
            1, 2, 7, 8, 9,
            3, 4, 6, 7, 8,
            9, 8, 7, 6, 5
        }), matrix);
    }

    /**
     * Tests {@link MatrixSIS#removeColumns(int, int)}
     */
    @Test
    public void testRemoveColumns() {
        MatrixSIS matrix = Matrices.create(4, 5, new double[] {
            1, 2, 7, 8, 9,
            3, 4, 6, 7, 8,
            9, 8, 7, 6, 5,
            4, 3, 2, 1, 0
        });
        matrix = matrix.removeColumns(2, 4);
        assertEquals(Matrices.create(4, 3, new double[] {
            1, 2, 9,
            3, 4, 8,
            9, 8, 5,
            4, 3, 0
        }), matrix);
    }

    /**
     * Tests {@link Matrices#forceNonZeroScales(Matrix, double)}.
     */
    public void testForceNonZeroScales() {
        MatrixSIS matrix = Matrices.create(4, 4, new double[] {
            2, 0, 0, 8,
            0, 0, 4, 7,
            0, 0, 0, 6,
            0, 0, 0, 1
        });
        MatrixSIS expected = matrix.clone();
        expected.setElement(2, 1, 3);
        assertTrue(Matrices.forceNonZeroScales(matrix, 3));
        assertEquals(expected, matrix);
    }

    /**
     * Tests {@link Matrices#equals(Matrix, Matrix, double, boolean)}.
     */
    @Test
    public void testEquals() {
        Matrix2 m1 = new Matrix2(-1.001, 0, 0, 1);
        Matrix2 m2 = new Matrix2(-1,     0, 0, 1);
        assertTrue(Matrices.equals(m1, m2, 0.002, true));
        /*
         * An infinite value with a relative tolerance threshold causes an infinite threshold,
         * which is undesirable. Verify that the comparison code handle is robust to infinities.
         */
        m1 = new Matrix2(Double.POSITIVE_INFINITY, 0, 0, 1);
        assertFalse(Matrices.equals(m1, m2, 0.002, true));
    }

    /**
     * Tests {@link Matrices#copy(Matrix)}
     */
    @Test
    public void testCopy() {
        final Matrix matrix = new Matrix3(10, 20, 30, 40, 50, 60, 70, 80, 90);
        final Matrix copy = Matrices.copy(matrix);
        assertNotSame(matrix, copy);
        assertEquals (matrix, copy);
    }

    /**
     * Tests {@link Matrices#toString(Matrix)}
     */
    @Test
    public void testToString() {
        assertMultilinesEquals(
                "┌            ┐\n" +
                "│ 1  0  0  0 │\n" +
                "│ 0  1  0  0 │\n" +
                "│ 0  0  1  0 │\n" +
                "│ 0  0  0  1 │\n" +
                "└            ┘\n", new Matrix4().toString());

        assertMultilinesEquals(
                "┌               ┐\n" +
                "│   1    0    0 │\n" +
                "│ NaN  NaN  NaN │\n" +
                "│   0    0    1 │\n" +
                "└               ┘\n", new Matrix3(1, 0, 0, NaN, NaN, NaN, 0, 0, 1).toString());
        /*
         * Mix of values with different precision, ±0, ±1, NaN and infinities.
         * In addition, the first column contains numbers having the maximal number of digits allowed
         * by the IEEE 754 `double` representation (we put an additional trailing '1' for making sure
         * that we exceed the `double` accuracy). Our string representation shall put spaces, not 0,
         * for those numbers in order to not give a false sense of accuracy.
         */
        final MatrixSIS matrix = Matrices.create(4, 5, new double[] {
            39.5193682106975151,  -68.5200,  -1.0,  1,  98,
           -66.0358637477182201,       NaN,  43.0,  0,  Double.NEGATIVE_INFINITY,
             2.0741018968776337,   83.7260,  -0.0,  1,  -3,
            91.8796187759200601,  -18.2674,  24.5,  0,  36.5
        });
        assertMultilinesEquals(
                "┌                                               ┐\n" +
                "│  39.519368210697515   -68.5200  -1    1  98.0 │\n" +
                "│ -66.03586374771822         NaN  43.0  0    -∞ │\n" +
                "│   2.0741018968776337   83.7260   0    1  -3.0 │\n" +     // Sign of zero is lost.
                "│  91.87961877592006    -18.2674  24.5  0  36.5 │\n" +
                "└                                               ┘\n", matrix.toString());
    }
}
