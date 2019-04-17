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
package org.apache.sis.internal.referencing.provider;

import java.util.Random;
import java.awt.geom.Point2D;
import java.awt.geom.AffineTransform;
import javax.measure.quantity.Dimensionless;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.measure.Units;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.DependsOnMethod;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link DatumShiftGridFile}. This class creates a grid using values computed by an affine transform,
 * and compare values computed by the grid using the affine transform as a reference.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public strictfp class DatumShiftGridFileTest extends TestCase {
    /**
     * Size of the grid created for testing purpose.
     */
    private static final int WIDTH = 30, HEIGHT = 45;

    /**
     * Tolerance threshold used in this test. We use a relatively high threshold because translations are stored
     * in {@code float[]} arrays, so the tolerance needs to be slightly higher than {@code float} precision.
     */
    private static final float TOLERANCE = 2E-5f;

    /**
     * Number of random points to test in each method.
     */
    private static final int NUM_TESTS = 20;

    /**
     * The transform to use as a reference.
     */
    private AffineTransform reference;

    /**
     * The grid wrapping computed from affine transform.
     */
    DatumShiftGridFile<Dimensionless,Dimensionless> grid;

    /**
     * Creates a new grid using an affine transform as a reference.
     * An arbitrary non-uniform scale is applied on axes.
     *
     * @param  rotation  rotation angle in degrees to apply on affine transform.
     */
    void init(final double rotation) throws NoninvertibleTransformException {
        reference = AffineTransform.getRotateInstance(StrictMath.toRadians(rotation), WIDTH/2, HEIGHT/2);
        reference.scale(2, 5);
        final DatumShiftGridFile.Float<Dimensionless,Dimensionless> grid = new DatumShiftGridFile.Float<>(
                2, Units.UNITY, Units.UNITY, true, 0, 0, 1, 1, WIDTH, HEIGHT, null);
        assertEquals(2, grid.offsets.length);
        final Point2D.Float point = new Point2D.Float();
        int i = 0;
        for (int y=0; y<HEIGHT; y++) {
            for (int x=0; x<WIDTH; x++) {
                point.x = x;
                point.y = y;
                assertSame(point, reference.transform(point, point));
                grid.offsets[0][i] = point.x - x;
                grid.offsets[1][i] = point.y - y;
                i++;
            }
        }
        assertEquals(grid.offsets[0].length, i);
        assertEquals(grid.offsets[1].length, i);
        this.grid = grid;
    }

    /**
     * Tests {@link DatumShiftGridFile#interpolateInCell(double, double, double[])}.
     * This test does not perform interpolations and does not compute derivatives.
     *
     * @throws TransformException if an error occurred while transforming a coordinates.
     */
    @Test
    public void testInterpolateAtIntegers() throws TransformException {
        final Random random = TestUtilities.createRandomNumberGenerator();
        final Point2D.Float point = new Point2D.Float();
        final double[] vector = new double[2];
        init(15);
        for (int i=0; i<NUM_TESTS; i++) {
            /*
             * Compute the reference point.
             */
            final int x = random.nextInt(WIDTH);
            final int y = random.nextInt(HEIGHT);
            point.x = x;
            point.y = y;
            assertSame(point, reference.transform(point, point));
            /*
             * Compute the actual point and compare.
             */
            grid.interpolateInCell(x, y, vector);
            assertEquals("x", point.x, (float) (vector[0] + x), TOLERANCE);
            assertEquals("y", point.y, (float) (vector[1] + y), TOLERANCE);
        }
    }

    /**
     * Tests {@link DatumShiftGridFile#interpolateAt(double...)}.
     * This tests include interpolations.
     *
     * @throws TransformException if an error occurred while transforming a coordinates.
     */
    @Test
    @DependsOnMethod("testInterpolateAtIntegers")
    public void testInterpolateAtReals() throws TransformException {
        final Random random = TestUtilities.createRandomNumberGenerator();
        final Point2D.Float point = new Point2D.Float();
        final double[] vector = new double[2];
        init(0);                                    // No rotation for having same interpolations.
        for (int i=0; i<NUM_TESTS; i++) {
            /*
             * Compute the reference point.
             */
            final float x = random.nextFloat() * (WIDTH  - 1);
            final float y = random.nextFloat() * (HEIGHT - 1);
            point.x = x;
            point.y = y;
            assertSame(point, reference.transform(point, point));
            /*
             * Compute the actual point and compare.
             */
            grid.interpolateInCell(x, y, vector);
            assertEquals("x", point.x, (float) (vector[0] + x), TOLERANCE);
            assertEquals("y", point.y, (float) (vector[1] + y), TOLERANCE);
        }
    }

    /**
     * Tests {@link DatumShiftGridFile#interpolateAt(double...)} with opportunistic derivative calculations.
     * Since the grid is computed from an affine transform, the derivative should be constant everywhere.
     *
     * @throws TransformException if an error occurred while transforming a coordinates.
     */
    @Test
    @DependsOnMethod("testInterpolateAtIntegers")
    public void testInterpolateAndDerivative() throws TransformException {
        final Random random = TestUtilities.createRandomNumberGenerator();
        final Point2D.Float point = new Point2D.Float();
        final double[] vector = new double[6];
        init(30);
        for (int i=0; i<NUM_TESTS; i++) {
            /*
             * Compute the reference point. We need to avoid points outside the grid because derivates at those
             * locations are partially fixed to identity, which is different than affine transform coefficients.
             */
            final int x = random.nextInt(WIDTH  - 1);
            final int y = random.nextInt(HEIGHT - 1);
            point.x = x;
            point.y = y;
            assertSame(point, reference.transform(point, point));
            /*
             * Compute the actual point, compare, then check derivative.
             */
            grid.interpolateInCell(x, y, vector);
            assertEquals("x", point.x, (float) (vector[0] + x),   TOLERANCE);
            assertEquals("y", point.y, (float) (vector[1] + y),   TOLERANCE);
            assertEquals("m00", reference.getScaleX(), vector[2], TOLERANCE);
            assertEquals("m01", reference.getShearX(), vector[3], TOLERANCE);
            assertEquals("m10", reference.getShearY(), vector[4], TOLERANCE);
            assertEquals("m11", reference.getScaleY(), vector[5], TOLERANCE);
            assertSameDerivative(x, y, vector);
        }
    }

    /**
     * Tests {@link DatumShiftGridFile#interpolateAt(double...)} with some values outside the grid.
     * Derivatives outside the grid have different coefficients than derivatives inside the grid.
     * This test verifies that methods computing derivatives are self-consistent.
     *
     * @throws TransformException if an error occurred while transforming a coordinates.
     */
    @Test
    @DependsOnMethod("testInterpolateAndDerivative")
    public void testExtrapolation() throws TransformException {
        final Random random = TestUtilities.createRandomNumberGenerator();
        final Point2D.Float point = new Point2D.Float();
        final double[] vector = new double[6];
        init(50);
        for (int i=0; i<NUM_TESTS; i++) {
            final int x = random.nextInt(WIDTH  * 2) - WIDTH  / 4;
            final int y = random.nextInt(HEIGHT * 2) - HEIGHT / 4;
            point.x = x;
            point.y = y;
            grid.interpolateInCell(x, y, vector);
            if (x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT) {
                assertSame(point, reference.transform(point, point));
                assertEquals("x", point.x, (float) (vector[0] + x), TOLERANCE);
                assertEquals("y", point.y, (float) (vector[1] + y), TOLERANCE);
            }
            assertSameDerivative(x, y, vector);
        }
    }

    /**
     * Verifies that the matrix returned by {@link DatumShiftGridFile#derivativeInCell(double, double)}
     * contains coefficients identical to the ones in the given vector.
     */
    private void assertSameDerivative(final int x, final int y, final double[] vector) {
        Matrix m = grid.derivativeInCell(x, y);
        assertEquals("numRow", 2, m.getNumRow());
        assertEquals("numCol", 2, m.getNumCol());
        assertEquals("m00", m.getElement(0,0), vector[2], STRICT);
        assertEquals("m01", m.getElement(0,1), vector[3], STRICT);
        assertEquals("m10", m.getElement(1,0), vector[4], STRICT);
        assertEquals("m11", m.getElement(1,1), vector[5], STRICT);
    }
}
