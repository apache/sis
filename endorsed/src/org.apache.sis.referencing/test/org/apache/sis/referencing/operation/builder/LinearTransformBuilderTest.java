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
package org.apache.sis.referencing.operation.builder;

import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.awt.geom.AffineTransform;
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.geometry.DirectPosition1D;
import org.apache.sis.geometry.DirectPosition2D;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.referencing.operation.HardCodedConversions;
import static org.apache.sis.test.Assertions.assertMapEquals;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import static org.opengis.test.Assertions.assertMatrixEquals;


/**
 * Tests {@link LinearTransformBuilder}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class LinearTransformBuilderTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public LinearTransformBuilderTest() {
    }

    /**
     * Tests a very simple case where an exact answer is expected.
     *
     * @throws FactoryException if the transform cannot be created.
     */
    @Test
    public void testMinimalist1D() throws FactoryException {
        final var builder = new LinearTransformBuilder();
        final Map<DirectPosition1D,DirectPosition1D> pos = new HashMap<>(4);
        assertNull(pos.put(new DirectPosition1D(1), new DirectPosition1D(1)));
        assertNull(pos.put(new DirectPosition1D(2), new DirectPosition1D(3)));
        builder.setControlPoints(pos);

        assertArrayEquals(new double[] {1}, builder.getControlPoint(new int[] {1}));
        assertArrayEquals(new double[] {3}, builder.getControlPoint(new int[] {2}));
        assertNull(                         builder.getControlPoint(new int[] {3}));

        final Matrix m = builder.create(null).getMatrix();
        assertEquals( 2, m.getElement(0, 0), "m₀₀");
        assertEquals(-1, m.getElement(0, 1), "m₀₁");
        assertArrayEquals(new double[] {1}, builder.correlation());
    }

    /**
     * Tests a very simple case where an exact answer is expected.
     * Tolerance threshold is set to zero because the math transform has been built from exactly 3 points,
     * in which case we expect an exact solution without rounding errors at the scale of the {@code double}
     * type. This is possible because SIS implementation uses double-double arithmetic.
     *
     * @throws FactoryException if the transform cannot be created.
     */
    @Test
    public void testMinimalist2D() throws FactoryException {
        final Map<DirectPosition2D,DirectPosition2D> pos = new HashMap<>(8);
        assertNull(pos.put(new DirectPosition2D(1, 1), new DirectPosition2D(3, 2)));
        assertNull(pos.put(new DirectPosition2D(1, 2), new DirectPosition2D(3, 5)));
        assertNull(pos.put(new DirectPosition2D(2, 2), new DirectPosition2D(5, 5)));
        final var builder = new LinearTransformBuilder();
        builder.setControlPoints(pos);

        assertArrayEquals(new double[] {3, 2}, builder.getControlPoint(new int[] {1, 1}));
        assertArrayEquals(new double[] {3, 5}, builder.getControlPoint(new int[] {1, 2}));
        assertArrayEquals(new double[] {5, 5}, builder.getControlPoint(new int[] {2, 2}));
        assertNull(                            builder.getControlPoint(new int[] {2, 1}));

        final Matrix m = builder.create(null).getMatrix();

        // First row (x)
        assertEquals(2, m.getElement(0, 0), "m₀₀");
        assertEquals(0, m.getElement(0, 1), "m₀₁");
        assertEquals(1, m.getElement(0, 2), "m₀₂");

        // Second row (y)
        assertEquals( 0, m.getElement(1, 0), "m₁₀");
        assertEquals( 3, m.getElement(1, 1), "m₁₁");
        assertEquals(-1, m.getElement(1, 2), "m₁₂");

        assertArrayEquals(new double[] {1, 1}, builder.correlation());
    }

    /**
     * Tests a two-dimensional case where sources coordinates are explicitly given.
     *
     * @throws FactoryException if the transform cannot be created.
     */
    @Test
    public void testExplicitSource2D() throws FactoryException {
        testSetAllPoints(new LinearTransformBuilder());
        testSetEachPoint(new LinearTransformBuilder());
    }

    /**
     * Same test as {@link #testExplicitSource2D()}, but using the
     * {@link LinearTransformBuilder#LinearTransformBuilder(int...)} constructor.
     *
     * @throws FactoryException if the transform cannot be created.
     */
    @Test
    public void testImplicitSource2D() throws FactoryException {
        testSetAllPoints(new LinearTransformBuilder(2, 3));
        testSetEachPoint(new LinearTransformBuilder(2, 3));
    }

    /**
     * Execution of {@link #testExplicitSource2D()} and {@link #testImplicitSource2D()}
     * where all control points are specified by a map.
     */
    private void testSetAllPoints(final LinearTransformBuilder builder) throws FactoryException {
        final Map<DirectPosition2D,DirectPosition2D> pos = new HashMap<>(8);
        assertNull(pos.put(new DirectPosition2D(0, 0), new DirectPosition2D(3, 9)));
        assertNull(pos.put(new DirectPosition2D(0, 1), new DirectPosition2D(4, 7)));
        assertNull(pos.put(new DirectPosition2D(0, 2), new DirectPosition2D(6, 6)));
        assertNull(pos.put(new DirectPosition2D(1, 0), new DirectPosition2D(4, 8)));
        assertNull(pos.put(new DirectPosition2D(1, 1), new DirectPosition2D(5, 4)));
        assertNull(pos.put(new DirectPosition2D(1, 2), new DirectPosition2D(8, 2)));
        builder.setControlPoints(pos);
        verify(builder);
    }

    /**
     * Execution of {@link #testExplicitSource2D()} and {@link #testImplicitSource2D()}
     * where all control points are specified one-by-one.
     */
    private void testSetEachPoint(final LinearTransformBuilder builder) throws FactoryException {
        builder.setControlPoint(new int[] {0, 0}, new double[] {3, 9});
        builder.setControlPoint(new int[] {0, 1}, new double[] {4, 7});
        builder.setControlPoint(new int[] {0, 2}, new double[] {6, 6});
        builder.setControlPoint(new int[] {1, 0}, new double[] {4, 8});
        builder.setControlPoint(new int[] {1, 1}, new double[] {5, 4});
        builder.setControlPoint(new int[] {1, 2}, new double[] {8, 2});
        verify(builder);
    }

    /**
     * Verifies the transform created by {@link #testExplicitSource2D()} and {@link #testImplicitSource2D()}.
     */
    private void verify(final LinearTransformBuilder builder) throws FactoryException {
        final Matrix m = builder.create(null).getMatrix();

        assertArrayEquals(new double[] {3, 9}, builder.getControlPoint(new int[] {0, 0}));
        assertArrayEquals(new double[] {4, 7}, builder.getControlPoint(new int[] {0, 1}));
        assertArrayEquals(new double[] {6, 6}, builder.getControlPoint(new int[] {0, 2}));
        assertArrayEquals(new double[] {4, 8}, builder.getControlPoint(new int[] {1, 0}));
        assertArrayEquals(new double[] {5, 4}, builder.getControlPoint(new int[] {1, 1}));
        assertArrayEquals(new double[] {8, 2}, builder.getControlPoint(new int[] {1, 2}));
        /*
         * Expect strict results (tolerance of 0) because Apache SIS uses double-double arithmetic.
         */
        assertEquals( 16 / 12d, m.getElement(0, 0), "m₀₀");        // First row (x)
        assertEquals( 21 / 12d, m.getElement(0, 1), "m₀₁");
        assertEquals( 31 / 12d, m.getElement(0, 2), "m₀₂");
        assertEquals(-32 / 12d, m.getElement(1, 0), "m₁₀");        // Second row (y)
        assertEquals(-27 / 12d, m.getElement(1, 1), "m₁₁");
        assertEquals(115 / 12d, m.getElement(1, 2), "m₁₂");

        assertArrayEquals(new double[] {0.9656, 0.9536}, builder.correlation(), 0.0001);
    }

    /**
     * Tests with a random number of points with an exact solution expected.
     *
     * @throws FactoryException if the transform cannot be created.
     */
    @Test
    public void testExact1D() throws FactoryException {
        final Random rd = TestUtilities.createRandomNumberGenerator(-6080923837183751016L);
        for (int i=0; i<10; i++) {
            test1D(rd, rd.nextInt(900) + 100, false, 1E-14, 1E-12);
        }
    }

    /**
     * Tests with a random number of points with an exact solution expected.
     *
     * <p><b>Note:</b> this test can pass with a random seed most of the time. But we fix the seed anyway
     * because there is always a small probability that truly random points are all colinear, in which case
     * the test would fail. Even if the probability is low, we do not take the risk of random build failures.</p>
     *
     * @throws FactoryException if the transform cannot be created.
     */
    @Test
    public void testExact2D() throws FactoryException {
        final Random rd = TestUtilities.createRandomNumberGenerator(41632405806929L);
        for (int i=0; i<10; i++) {
            test2D(rd, rd.nextInt(900) + 100, false, 1E-14, 1E-12);
        }
    }

    /**
     * Tests with a random number of points and a random errors in target points.
     *
     * @throws FactoryException if the transform cannot be created.
     */
    @Test
    public void testNonExact1D() throws FactoryException {
        final Random rd = TestUtilities.createRandomNumberGenerator(8819436190826166876L);
        for (int i=0; i<4; i++) {
            test1D(rd, rd.nextInt(900) + 100, true, 0.02, 0.5);
        }
    }

    /**
     * Tests with a random number of points and a random errors in target points.
     *
     * <p><b>Note:</b> this test can pass with a random seed most of the time. But we fix the seed anyway
     * because there is always a small probability that truly random points are all colinear, or that lot
     * of errors are in the same directions (thus introducing a larger bias than expected), in which case
     * the test would fail. Even if the probability is low, we do not take the risk of such random build
     * failures.</p>
     *
     * @throws FactoryException if the transform cannot be created.
     */
    @Test
    public void testNonExact2D() throws FactoryException {
        final Random rd = TestUtilities.createRandomNumberGenerator(270575025643864L);
        for (int i=0; i<4; i++) {
            test2D(rd, rd.nextInt(900) + 100, true, 0.02, 0.5);
        }
    }

    /**
     * Implementation of {@link #testExact1D()} and {@link #testNonExact1D()}.
     *
     * @param  rd              the random number generator to use.
     * @param  numPts          the number of points to generate.
     * @param  addErrors       {@code true} for adding a random error in the target points.
     * @param  scaleTolerance  tolerance threshold for floating point comparisons.
     */
    private static void test1D(final Random rd, final int numPts, final boolean addErrors,
            final double scaleTolerance, final double translationTolerance) throws FactoryException
    {
        final double scale  = rd.nextDouble() * 30 - 12;
        final double offset = rd.nextDouble() * 10 - 4;
        final Map<DirectPosition1D,DirectPosition1D> pos = new HashMap<>(numPts);
        for (int i=0; i<numPts; i++) {
            final DirectPosition1D src = new DirectPosition1D(rd.nextDouble() * 100 - 50);
            final DirectPosition1D tgt = new DirectPosition1D(src.coordinate * scale + offset);
            if (addErrors) {
                tgt.coordinate += rd.nextDouble() * 10 - 5;
            }
            assertNull(pos.put(src, tgt));
        }
        /*
         * Create the fitted transform to test.
         */
        final var builder = new LinearTransformBuilder();
        builder.setControlPoints(pos);
        final Matrix m = builder.create(null).getMatrix();
        assertEquals(scale,  m.getElement(0, 0), scaleTolerance, "m₀₀");
        assertEquals(offset, m.getElement(0, 1), translationTolerance, "m₀₁");
        assertEquals(1, StrictMath.abs(builder.correlation()[0]), scaleTolerance);
    }

    /**
     * Implementation of {@link #testExact2D()} and {@link #testNonExact2D()}.
     *
     * @param  rd              the random number generator to use.
     * @param  numPts          the number of points to generate.
     * @param  addErrors       {@code true} for adding a random error in the target points.
     * @param  scaleTolerance  tolerance threshold for floating point comparisons.
     */
    private static void test2D(final Random rd, final int numPts, final boolean addErrors,
            final double scaleTolerance, final double translationTolerance) throws FactoryException
    {
        /*
         * Create an AffineTransform to use as the reference implementation.
         */
        final AffineTransform ref = AffineTransform.getRotateInstance(
                rd.nextDouble() * (2 * StrictMath.PI),  // Rotation angle
                rd.nextDouble() * 30 - 12,              // Center X
                rd.nextDouble() * 10 - 8);              // Center Y
        final Map<DirectPosition2D,DirectPosition2D> pos = new HashMap<>(numPts);
        for (int i=0; i<numPts; i++) {
            final var src = new DirectPosition2D(rd.nextDouble() * 100 - 50, rd.nextDouble() * 200 - 75);
            final var tgt = new DirectPosition2D();
            assertSame(tgt, ref.transform(src, tgt));
            if (addErrors) {
                tgt.x += rd.nextDouble() * 10 - 5;
                tgt.y += rd.nextDouble() * 10 - 5;
            }
            assertNull(pos.put(src, tgt));
        }
        /*
         * Create the fitted transform to test.
         */
        final var builder = new LinearTransformBuilder();
        builder.setControlPoints(pos);
        final Matrix m = builder.create(null).getMatrix();
        /*
         * Compare the coefficients with the reference implementation.
         */
        assertEquals(ref.getScaleX(),     m.getElement(0, 0), scaleTolerance,       "m₀₀");
        assertEquals(ref.getShearX(),     m.getElement(0, 1), scaleTolerance,       "m₀₁");
        assertEquals(ref.getTranslateX(), m.getElement(0, 2), translationTolerance, "m₀₂");
        assertEquals(ref.getShearY(),     m.getElement(1, 0), scaleTolerance,       "m₁₀");
        assertEquals(ref.getScaleY(),     m.getElement(1, 1), scaleTolerance,       "m₁₁");
        assertEquals(ref.getTranslateY(), m.getElement(1, 2), translationTolerance, "m₁₂");
        assertArrayEquals(new double[] {1, 1}, builder.correlation(), scaleTolerance);
    }

    /**
     * Tests {@link LinearTransformBuilder#getControlPoints()} with gridded source points.
     */
    @Test
    public void testGetControlPoints() {
        testGetControlPoints(new LinearTransformBuilder(3, 4));
    }

    /**
     * Tests {@link LinearTransformBuilder#getControlPoints()} with non-gridded source points.
     */
    @Test
    public void testGetUngriddedControlPoints() {
        testGetControlPoints(new LinearTransformBuilder());
    }

    /**
     * Tests {@link LinearTransformBuilder#getControlPoints()} with the given builder.
     * If the builder is backed by a grid, then the grid size shall be at least 3×4.
     */
    private static void testGetControlPoints(final LinearTransformBuilder builder) {
        final DirectPosition2D s12, s23, s00;
        final DirectPosition2D t12, t23, t00;
        s12 = new DirectPosition2D(1, 2);   t12 = new DirectPosition2D(3, 2);
        s23 = new DirectPosition2D(2, 3);   t23 = new DirectPosition2D(4, 1);
        s00 = new DirectPosition2D(0, 0);   t00 = new DirectPosition2D(7, 3);

        final Map<DirectPosition2D,DirectPosition2D> expected = new HashMap<>();
        final Map<DirectPosition,DirectPosition> actual = builder.getControlPoints();
        assertEquals(0, actual.size());
        assertTrue(actual.isEmpty());
        assertFalse(actual.containsKey  (s12));
        assertFalse(actual.containsKey  (s23));
        assertFalse(actual.containsKey  (s00));
        assertFalse(actual.containsValue(t12));
        assertFalse(actual.containsValue(t23));
        assertFalse(actual.containsValue(t00));
        assertMapEquals(expected, actual);

        builder.setControlPoint(new int[] {1, 2}, t12.getCoordinates());
        assertNull(expected.put(s12, t12));
        assertEquals(1, actual.size());
        assertFalse(actual.isEmpty());
        assertTrue (actual.containsKey  (s12));
        assertFalse(actual.containsKey  (s23));
        assertFalse(actual.containsKey  (s00));
        assertTrue (actual.containsValue(t12));
        assertFalse(actual.containsValue(t23));
        assertFalse(actual.containsValue(t00));
        assertMapEquals(expected, actual);

        builder.setControlPoint(new int[] {2, 3}, t23.getCoordinates());
        assertNull(expected.put(s23, t23));
        assertEquals(2, actual.size());
        assertFalse(actual.isEmpty());
        assertTrue (actual.containsKey  (s12));
        assertTrue (actual.containsKey  (s23));
        assertFalse(actual.containsKey  (s00));
        assertTrue (actual.containsValue(t12));
        assertTrue (actual.containsValue(t23));
        assertFalse(actual.containsValue(t00));
        assertMapEquals(expected, actual);

        builder.setControlPoint(new int[] {0, 0}, t00.getCoordinates());
        assertNull(expected.put(s00, t00));
        assertEquals(3, actual.size());
        assertFalse(actual.isEmpty());
        assertTrue (actual.containsKey  (s12));
        assertTrue (actual.containsKey  (s23));
        assertTrue (actual.containsKey  (s00));
        assertTrue (actual.containsValue(t12));
        assertTrue (actual.containsValue(t23));
        assertTrue (actual.containsValue(t00));
        assertMapEquals(expected, actual);
    }

    /**
     * Tests {@link LinearTransformBuilder#setControlPoints(MathTransform)}.
     * This test uses the Mercator projection as a source of non-linear transform
     * and verify that we can get a linear approximation from it for a small region.
     *
     * @throws TransformException if an error occurred during map projection.
     * @throws FactoryException if the transform cannot be created.
     */
    @Test
    public void testSetPointsFromTransform() throws TransformException, FactoryException {
        final var builder = new LinearTransformBuilder(3, 5);
        builder.setControlPoints(HardCodedConversions.mercator().getConversionFromBase().getMathTransform());
        assertPointEquals(builder, 0, 0,      0,      0);
        assertPointEquals(builder, 1, 0, 111319,      0);
        assertPointEquals(builder, 2, 0, 222639,      0);
        assertPointEquals(builder, 2, 1, 222639, 110580);
        assertPointEquals(builder, 2, 2, 222639, 221194);
        assertPointEquals(builder, 1, 3, 111319, 331877);
        assertPointEquals(builder, 1, 4, 111319, 442662);
        final Matrix actual = builder.create(null).getMatrix();
        final Matrix expected = new Matrix3(
                111319, 0,   0,
                0, 110662, -62,
                0, 0, 1);
        assertMatrixEquals(expected, actual, 0.5, "linear");
    }

    /**
     * Asserts that the builder contains the expected target coordinates for a given source grid coordinates.
     */
    private static void assertPointEquals(final LinearTransformBuilder builder,
            final int gridX, final int gridY, final int expectedX, final int expectedY)
    {
        assertArrayEquals(new double[] {expectedX, expectedY}, builder.getControlPoint(new int[] {gridX,gridY}), 0.5);
    }

    /**
     * Tests the effect of {@link LinearTransformBuilder#addLinearizers(Map, int...)}.
     *
     * @throws FactoryException if the transform cannot be created.
     */
    @Test
    public void testLinearizers() throws FactoryException {
        final int width  = 3;
        final int height = 4;
        final var builder = new LinearTransformBuilder(width, height);
        for (int y=0; y<height; y++) {
            final int[]    source = new int[2];
            final double[] target = new double[2];
            for (int x=0; x<width; x++) {
                source[0] = x;
                source[1] = y;
                target[0] = StrictMath.cbrt(3 + x*2);
                target[1] = StrictMath.sqrt(1 + y);
                builder.setControlPoint(source, target);
            }
        }
        final var tr = new NonLinearTransform();
        builder.addLinearizers(Map.of("x² y³", tr));
        builder.addLinearizers(Map.of("x³ y²", tr), 1, 0);
        builder.addLinearizers(Map.of("identity", MathTransforms.identity(2)));
        final Matrix actual = builder.create(null).getMatrix();
        assertEquals("x³ y²", builder.linearizer().get().getKey());
        assertNotSame(tr, builder.linearizer().get());    // Not same because axes should have been swapped.
        assertArrayEquals(new double[] {1, 1}, builder.correlation(), 1E-15);
        final Matrix expected = new Matrix3(
                2, 0, 3,
                0, 1, 1,
                0, 0, 1);
        assertMatrixEquals(expected, actual, 1E-15, "linear");
    }
}
