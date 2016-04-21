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

import java.util.Random;
import java.awt.geom.AffineTransform;
import java.util.Arrays;

import org.opengis.referencing.operation.Matrix;
import org.apache.sis.geometry.DirectPosition1D;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.TestCase;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;
import org.opengis.geometry.MismatchedDimensionException;


/**
 * Tests {@link LinearTransformBuilder}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public final strictfp class LinearTransformBuilderTest extends TestCase {
    /**
     * Tests a very simple case where an exact answer is expected.
     */
    @Test
    public void testMinimalist1D() {
        final LinearTransformBuilder builder = new LinearTransformBuilder();
        builder.setSourcePoints(
                new DirectPosition1D(1),
                new DirectPosition1D(2));
        builder.setTargetPoints(
                new DirectPosition1D(1),
                new DirectPosition1D(3));
        final Matrix m = builder.create().getMatrix();
        assertEquals("m₀₀",  2, m.getElement(0, 0), STRICT);
        assertEquals("m₀₁", -1, m.getElement(0, 1), STRICT);
        assertArrayEquals("correlation", new double[] {1}, builder.correlation(), STRICT);
    }

    /**
     * Tests a very simple case where an exact answer is expected.
     * Tolerance threshold is set to zero because the math transform has been built from exactly 3 points,
     * in which case we expect an exact solution without rounding errors at the scale of the {@code double}
     * type. This is possible because SIS implementation uses double-double arithmetic.
     */
    @Test
    public void testMinimalist2D() {
        final LinearTransformBuilder builder = new LinearTransformBuilder();
        builder.setSourcePoints(
                new DirectPosition2D(1, 1),
                new DirectPosition2D(1, 2),
                new DirectPosition2D(2, 2));
        builder.setTargetPoints(
                new DirectPosition2D(3, 2),
                new DirectPosition2D(3, 5),
                new DirectPosition2D(5, 5));
        final Matrix m = builder.create().getMatrix();

        // First row (x)
        assertEquals("m₀₀",  2, m.getElement(0, 0), STRICT);
        assertEquals("m₀₁",  0, m.getElement(0, 1), STRICT);
        assertEquals("m₀₂",  1, m.getElement(0, 2), STRICT);

        // Second row (y)
        assertEquals("m₁₀",  0, m.getElement(1, 0), STRICT);
        assertEquals("m₁₁",  3, m.getElement(1, 1), STRICT);
        assertEquals("m₁₂", -1, m.getElement(1, 2), STRICT);

        assertArrayEquals("correlation", new double[] {1, 1}, builder.correlation(), STRICT);
    }

    /**
     * Tests a very simple on regular grid, case where an exact answer is expected.
     * Tolerance threshold is set to zero because the math transform has been built from exactly 4 points,
     * in which case we expect an exact solution without rounding errors at the scale of the {@code double}
     * type. This is possible because SIS implementation uses double-double arithmetic.
     */
    @Test
    public void testMinimalistRegular2D() {
        final LinearTransformBuilder builder = new LinearTransformBuilder(2, 2);
        final DirectPosition2D[] sourcePoints = new DirectPosition2D[]{new DirectPosition2D(0, 0),
                                                                       new DirectPosition2D(1, 0),
                                                                       new DirectPosition2D(1, 1),
                                                                       new DirectPosition2D(0, 1)};

        final DirectPosition2D[] targetPoints = new DirectPosition2D[]{new DirectPosition2D(1, -1),
                                                                       new DirectPosition2D(3, -1),
                                                                       new DirectPosition2D(3, 2),
                                                                       new DirectPosition2D(1, 2)};

        builder.setPoints(sourcePoints, targetPoints);
        assertTrue(builder.isValid());
        final Matrix m = builder.create().getMatrix();

        // First row (x)
        assertEquals("m₀₀",  2, m.getElement(0, 0), STRICT);
        assertEquals("m₀₁",  0, m.getElement(0, 1), STRICT);
        assertEquals("m₀₂",  1, m.getElement(0, 2), STRICT);

        // Second row (y)
        assertEquals("m₁₀",  0, m.getElement(1, 0), STRICT);
        assertEquals("m₁₁",  3, m.getElement(1, 1), STRICT);
        assertEquals("m₁₂", -1, m.getElement(1, 2), STRICT);

        assertArrayEquals("correlation", new double[] {1, 1}, builder.correlation(), STRICT);
    }

    /**
     * Test different kind of insertions.
     */
    @Test
    public void testValidity() {

        //-- test insertion in many times
        final LinearTransformBuilder builder  = new LinearTransformBuilder();
        final DirectPosition2D[] sourcePoints = new DirectPosition2D[]{new DirectPosition2D(0, 0),
                                                                       new DirectPosition2D(1, 0),
                                                                       new DirectPosition2D(1, 1),
                                                                       new DirectPosition2D(0, 1)};

        final DirectPosition2D[] targetPoints = new DirectPosition2D[]{new DirectPosition2D(1, -1),
                                                                       new DirectPosition2D(3, -1),
                                                                       new DirectPosition2D(3, 2),
                                                                       new DirectPosition2D(1, 2)};

        builder.setPoints(Arrays.copyOfRange(sourcePoints, 0, 2), Arrays.copyOfRange(targetPoints, 0, 2));
        assertTrue(builder.isValid());
        builder.setPoints(Arrays.copyOfRange(sourcePoints, 2, 4), Arrays.copyOfRange(targetPoints, 2, 4));
        assertTrue(builder.isValid());

        final Matrix m = builder.create().getMatrix();

        // First row (x)
        assertEquals("m₀₀",  2, m.getElement(0, 0), STRICT);
        assertEquals("m₀₁",  0, m.getElement(0, 1), STRICT);
        assertEquals("m₀₂",  1, m.getElement(0, 2), STRICT);

        // Second row (y)
        assertEquals("m₁₀",  0, m.getElement(1, 0), STRICT);
        assertEquals("m₁₁",  3, m.getElement(1, 1), STRICT);
        assertEquals("m₁₂", -1, m.getElement(1, 2), STRICT);

        assertArrayEquals("correlation", new double[] {1, 1}, builder.correlation(), STRICT);

                                //--------------//


        //-- Test switch from regular grid to no regular
        //-- begin insertion with integer coordinates points and switch with floating coordinates points.
        final LinearTransformBuilder regBuilder  = new LinearTransformBuilder();
        final DirectPosition2D[] noRegSourcePoints = new DirectPosition2D[]{new DirectPosition2D(1+1E-12, 1),
                                                                       new DirectPosition2D(0, 1+1E-12)};

        //-- set 2 pts as regular grid.
        regBuilder.setPoints(Arrays.copyOfRange(sourcePoints, 0, 2), Arrays.copyOfRange(targetPoints, 0, 2));
        assertTrue(regBuilder.isValid());

        //-- set 2 no regular points and verify that switch between regular to none regular grid is controled
        regBuilder.setPoints(noRegSourcePoints, Arrays.copyOfRange(targetPoints, 2, 4));
        assertTrue(regBuilder.isValid());

                                //--------------//


        //-- test insert source points and targets points separately
        LinearTransformBuilder builderTest  = new LinearTransformBuilder();
        builderTest.setSourcePoints(sourcePoints);
        builderTest.setTargetPoints(targetPoints);
        assertTrue(builderTest.isValid());

                                //--------------//

        //-- test validity at origin
        builderTest  = new LinearTransformBuilder();
        assertTrue(builderTest.isValid());
    }

    /**
     * Test method {@link LinearTransformBuilder#setModelTiePoints(int, int, int, int, int, double[]) }.
     */
    @Test
    public void setTiePointTest() {

        final double[] points = new double[]{
        //-- [Empty][x source][y source][Empty][Empty][x dest][y dest]
        Double.NaN, 0, 0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1, -1,//-- pt 0
        Double.NaN, 1, 0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 3, -1,//-- pt 1
        Double.NaN, 1, 1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 3,  2,//-- pt 2
        Double.NaN, 0, 1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1,  2,//-- pt 3
        };

        //-- No regular
        LinearTransformBuilder builder  = new LinearTransformBuilder();

        builder.setModelTiePoints(1, 2, 5, 2, 7, points);
        assertTrue(builder.isValid());

        Matrix m = builder.create().getMatrix();

        // First row (x)
        assertEquals("m₀₀",  2, m.getElement(0, 0), STRICT);
        assertEquals("m₀₁",  0, m.getElement(0, 1), STRICT);
        assertEquals("m₀₂",  1, m.getElement(0, 2), STRICT);

        // Second row (y)
        assertEquals("m₁₀",  0, m.getElement(1, 0), STRICT);
        assertEquals("m₁₁",  3, m.getElement(1, 1), STRICT);
        assertEquals("m₁₂", -1, m.getElement(1, 2), STRICT);

        assertArrayEquals("correlation", new double[] {1, 1}, builder.correlation(), STRICT);

        //-- regular
        builder  = new LinearTransformBuilder(2, 2);

        builder.setModelTiePoints(1, 2, 5, 2, 7, points);
        assertTrue(builder.isValid());

        m = builder.create().getMatrix();

        // First row (x)
        assertEquals("m₀₀",  2, m.getElement(0, 0), STRICT);
        assertEquals("m₀₁",  0, m.getElement(0, 1), STRICT);
        assertEquals("m₀₂",  1, m.getElement(0, 2), STRICT);

        // Second row (y)
        assertEquals("m₁₀",  0, m.getElement(1, 0), STRICT);
        assertEquals("m₁₁",  3, m.getElement(1, 1), STRICT);
        assertEquals("m₁₂", -1, m.getElement(1, 2), STRICT);

        assertArrayEquals("correlation", new double[] {1, 1}, builder.correlation(), STRICT);
    }


    /**
     * Test comportements which should fail.
     */
    @Test
    public void testFail() {

        //-- build a no 2D grid
        try {
            final LinearTransformBuilder builder = new LinearTransformBuilder(2);
            Assert.fail("Test should thrown an exception it is normaly impossible to build a no 2D grid.");
        } catch (MismatchedDimensionException ex) {
            //-- do nothing test validate
        }
        try {
            final LinearTransformBuilder builder = new LinearTransformBuilder(2, 1);
            Assert.fail("Test should thrown an exception it is normaly impossible to build a grid only define on one axis.");
        } catch (IllegalArgumentException ex) {
            //-- do nothing test validate
        }
        try {
            final LinearTransformBuilder builder = new LinearTransformBuilder(1, 10);
            Assert.fail("Test should thrown an exception it is normaly impossible to build a grid only define on one axis.");
        } catch (MismatchedDimensionException ex) {
            //-- do nothing test validate
        }
        try {
            final LinearTransformBuilder builder = new LinearTransformBuilder(1, -5);
            Assert.fail("Test should thrown an exception it is normaly impossible to build a grid with negative size value.");
        } catch (IllegalArgumentException ex) {
            //-- do nothing test validate
        }

        final DirectPosition2D[] sourcePoints = new DirectPosition2D[]{new DirectPosition2D(0, 0),
                                                                       new DirectPosition2D(1, 0),
                                                                       new DirectPosition2D(1, 1),
                                                                       new DirectPosition2D(0, 1)};

        final DirectPosition2D[] targetPoints = new DirectPosition2D[]{new DirectPosition2D(1, -1),
                                                                       new DirectPosition2D(3, -1),
                                                                       new DirectPosition2D(3, 2),
                                                                       new DirectPosition2D(1, 2)};


        //-- try to build a regular grid with a missing point
        try {
            final LinearTransformBuilder builder = new LinearTransformBuilder(2, 2);
            //-- missing one point
            builder.setPoints(Arrays.copyOfRange(sourcePoints, 0, 3), Arrays.copyOfRange(targetPoints, 0, 3));
            final Matrix m = builder.create().getMatrix();
            Assert.fail("Test should thrown an exception it is normaly impossible to build a transformation with missing grid point.");
        } catch (IllegalStateException ex) {
            //-- test with missing points from expected defined grid.
        }

        try {
            final LinearTransformBuilder builder = new LinearTransformBuilder();

            builder.setSourcePoints(Arrays.copyOfRange(sourcePoints, 0, 3));
            builder.setTargetPoints(targetPoints);
            final Matrix m = builder.create().getMatrix();
            Assert.fail("Test should thrown an exception it is normaly impossible to build a transformation with missing grid point.");
        } catch (AssertionError ex) {
            //-- define grid with missing source points.
        }

        try {
            //-- test define builder as regular grid and only set targets points
            final LinearTransformBuilder builderTest = new LinearTransformBuilder(2, 2);
            assertTrue(builderTest.isValid());
            builderTest.setTargetPoints(targetPoints);
            builderTest.isValid();
        } catch (AssertionError ex) {
            //-- only setted target points + grid is not enought to build transform
        }


        try {
            //-- test define builder as regular grid and only set targets points
            final LinearTransformBuilder builderTest = new LinearTransformBuilder(2, 2);
            assertTrue(builderTest.isValid());
            builderTest.setSourcePoints(sourcePoints);
            builderTest.isValid();
        } catch (AssertionError ex) {
            //-- only setted source points + grid is not enought to build transform
        }
    }

    /**
     * Tests with a random number of points with an exact solution expected.
     */
    @Test
    @DependsOnMethod("testMinimalist1D")
    public void testExact1D() {
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
     */
    @Test
    @DependsOnMethod("testMinimalist2D")
    public void testExact2D() {
        final Random rd = TestUtilities.createRandomNumberGenerator(41632405806929L);
        for (int i=0; i<10; i++) {
            test2D(rd, rd.nextInt(900) + 100, false, 1E-14, 1E-12);
        }
    }

    /**
     * Tests with a random number of points and a random errors in target points.
     */
    @Test
    @DependsOnMethod("testExact1D")
    public void testNonExact1D() {
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
     */
    @Test
    @DependsOnMethod("testExact2D")
    public void testNonExact2D() {
        final Random rd = TestUtilities.createRandomNumberGenerator(270575025643864L);
        for (int i=0; i<4; i++) {
            test2D(rd, rd.nextInt(900) + 100, true, 0.02, 0.5);
        }
    }

    /**
     * Implementation of {@link #testExact1D()} and {@link #testNonExact1D()}.
     *
     * @param rd        The random number generator to use.
     * @param numPts    The number of points to generate.
     * @param addErrors {@code true} for adding a random error in the target points.
     * @param scaleTolerance Tolerance threshold for floating point comparisons.
     */
    private static void test1D(final Random rd, final int numPts, final boolean addErrors,
            final double scaleTolerance, final double translationTolerance)
    {
        final double scale  = rd.nextDouble() * 30 - 12;
        final double offset = rd.nextDouble() * 10 - 4;
        final DirectPosition1D[] sources = new DirectPosition1D[numPts];
        final DirectPosition1D[] targets = new DirectPosition1D[numPts];
        for (int i=0; i<numPts; i++) {
            final DirectPosition1D src = new DirectPosition1D(rd.nextDouble() * 100 - 50);
            final DirectPosition1D tgt = new DirectPosition1D(src.ordinate * scale + offset);
            if (addErrors) {
                tgt.ordinate += rd.nextDouble() * 10 - 5;
            }
            sources[i] = src;
            targets[i] = tgt;
        }
        /*
         * Create the fitted transform to test.
         */
        final LinearTransformBuilder builder = new LinearTransformBuilder();
        builder.setSourcePoints(sources);
        builder.setTargetPoints(targets);
        final Matrix m = builder.create().getMatrix();
        assertEquals("m₀₀", scale,  m.getElement(0, 0), scaleTolerance);
        assertEquals("m₀₁", offset, m.getElement(0, 1), translationTolerance);
        assertEquals("correlation", 1, StrictMath.abs(builder.correlation()[0]), scaleTolerance);
    }

    /**
     * Implementation of {@link #testExact2D()} and {@link #testNonExact2D()}.
     *
     * @param rd        The random number generator to use.
     * @param numPts    The number of points to generate.
     * @param addErrors {@code true} for adding a random error in the target points.
     * @param scaleTolerance Tolerance threshold for floating point comparisons.
     */
    private static void test2D(final Random rd, final int numPts, final boolean addErrors,
            final double scaleTolerance, final double translationTolerance)
    {
        /*
         * Create an AffineTransform to use as the reference implementation.
         */
        final AffineTransform ref = AffineTransform.getRotateInstance(
                rd.nextDouble() * (2 * StrictMath.PI),  // Rotation angle
                rd.nextDouble() * 30 - 12,              // Center X
                rd.nextDouble() * 10 - 8);              // Center Y
        final DirectPosition2D[] sources = new DirectPosition2D[numPts];
        final DirectPosition2D[] targets = new DirectPosition2D[numPts];
        for (int i=0; i<numPts; i++) {
            final DirectPosition2D src = new DirectPosition2D(rd.nextDouble() * 100 - 50, rd.nextDouble() * 200 - 75);
            final DirectPosition2D tgt = new DirectPosition2D();
            assertSame(tgt, ref.transform(src, tgt));
            if (addErrors) {
                tgt.x += rd.nextDouble() * 10 - 5;
                tgt.y += rd.nextDouble() * 10 - 5;
            }
            sources[i] = src;
            targets[i] = tgt;
        }
        /*
         * Create the fitted transform to test.
         */
        final LinearTransformBuilder builder = new LinearTransformBuilder();
        builder.setSourcePoints(sources);
        builder.setTargetPoints(targets);
        final Matrix m = builder.create().getMatrix();
        /*
         * Compare the coefficients with the reference implementation.
         */
        assertEquals("m₀₀", ref.getScaleX(),     m.getElement(0, 0), scaleTolerance);
        assertEquals("m₀₁", ref.getShearX(),     m.getElement(0, 1), scaleTolerance);
        assertEquals("m₀₂", ref.getTranslateX(), m.getElement(0, 2), translationTolerance);
        assertEquals("m₁₀", ref.getShearY(),     m.getElement(1, 0), scaleTolerance);
        assertEquals("m₁₁", ref.getScaleY(),     m.getElement(1, 1), scaleTolerance);
        assertEquals("m₁₂", ref.getTranslateY(), m.getElement(1, 2), translationTolerance);
        assertArrayEquals("correlation", new double[] {1, 1}, builder.correlation(), scaleTolerance);
    }
}
