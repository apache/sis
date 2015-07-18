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
package org.apache.sis.referencing.operation.projection;

import java.util.Random;
import org.opengis.test.referencing.TransformTestCase;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestUtilities;
import org.junit.Test;

import static java.lang.Double.*;
import static java.lang.StrictMath.*;
import static org.junit.Assert.*;


/**
 * Tests the {@link ConformalProjection} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@DependsOn(NormalizedProjectionTest.class)
public final strictfp class ConformalProjectionTest extends TransformTestCase {
    /**
     * Computes {@link ConformalProjection#expOfNorthing(double, double)} for the given latitude.
     *
     * @param  φ The latitude in radians.
     * @return {@code Math.exp} of the Mercator projection of the given latitude.
     */
    private double expOfNorthing(final double φ) {
        return NormalizedProjectionTest.expOfNorthing((ConformalProjection) transform, φ);
    }

    /**
     * Computes {@link ConformalProjection#φ(double)}.
     *
     * @param  expOfSouthing The reciprocal of the value returned by {@link #expOfNorthing(double)}.
     * @return The latitude in radians.
     * @throws ProjectionException if the iteration does not converge.
     */
    private double φ(final double expOfSouthing) throws ProjectionException {
        return ((ConformalProjection) transform).φ(expOfSouthing);
    }

    /**
     * Tests the {@link ConformalProjection#φ(double)} function. We expect it to be
     * the converse of the {@link NormalizedProjection#expOfNorthing(double, double)} function.
     * In theory only the [-90° … +90°] range needs to be tested. However the function is still
     * consistent in the [-90° … +270°] range so we test that range for tracking this fact.
     *
     * @throws ProjectionException Should never happen.
     */
    @Test
    @DependsOnMethod("testExpOfNorthing")
    public void test_φ() throws ProjectionException {
        transform = new NoOp(false);   // Spherical case
        tolerance = NormalizedProjectionTest.TOLERANCE;
        doTest_φ();
        transform = new NoOp(true);    // Ellipsoidal case
        tolerance = NormalizedProjection.ITERATION_TOLERANCE;
        doTest_φ();
    }

    /**
     * Implementation of {@link #test_φ()}.
     * The {@link #projection} field must have been set before this method is called.
     */
    private void doTest_φ() throws ProjectionException {
        assertEquals("φ(NaN) = NaN",    NaN,   φ(NaN),               tolerance);
        assertEquals("φ( ∞)  = -90°", -PI/2,   φ(POSITIVE_INFINITY), tolerance);
        assertEquals("φ( ∞)  = -90°", -PI/2,   φ(MAX_VALUE),         tolerance);
        assertEquals("φ( 1)  =   0°",    0,    φ(1),                 tolerance);
        assertEquals("φ( ε)  →  90°",  PI/2,   φ(MIN_VALUE),         tolerance);
        assertEquals("φ( 0)  =  90°",  PI/2,   φ(0),                 tolerance);
        assertEquals("φ(-ε)  →  90°",  PI/2,   φ(-MIN_VALUE),        tolerance);
        assertEquals("φ(-1)  = 180°",  PI,     φ(-1),                tolerance);
        assertEquals("φ(-∞)  = 270°",  PI*1.5, φ(-MAX_VALUE),        tolerance);
        assertEquals("φ(-∞)  = 270°",  PI*1.5, φ(NEGATIVE_INFINITY), tolerance);
        /*
         * Using t(φ) as a reference.
         */
        for (int i=-90; i<=270; i+=5) {
            final double φ   = toRadians(i);
            final double t    = 1 / expOfNorthing(φ);
            final double back = toDegrees(φ(t));
            if (i <= 90) {
                assertTrue("φ(t) in valid range should be positive.", t >= 0);
            } else {
                assertTrue("φ(t) in invalid range should be negative.", t < 0);
            }
            assertEquals("Inverse function does not match.", i, back, tolerance);
        }
    }

    /**
     * Performs a comparison between φ values computed by various methods.
     * The comparisons are:
     *
     * <ol>
     *   <li>φ values computed by an iterative method.</li>
     *   <li>φ values computed by the series expansion given by EPSG guide.</li>
     *   <li>φ values computed by modified form of series expansion, using trigonometric identities.</li>
     *   <li>φ values computed by the actual {@link ConformalProjection} implementation.</li>
     * </ol>
     *
     * {@link ConformalProjection#φ(double)} which uses a mix of 1 and 3 in the above list.
     * See {@link MercatorMethodComparison} for a discussion.
     *
     * @throws ProjectionException if an error occurred during computation of φ.
     *
     * @see MercatorMethodComparison
     */
    @Test
    public void compareWithSeriesExpansion() throws ProjectionException {
        final ConformalProjection projection = new NoOp(true);
        final MercatorMethodComparison comparator = new MercatorMethodComparison(projection.excentricitySquared);
        final Random random = TestUtilities.createRandomNumberGenerator();
        final int numSamples = 2000;
        for (int i=0; i<numSamples; i++) {
            final double φ = random.nextDouble() * PI - PI/2;
            final double t = 1 / comparator.expOfNorthing(φ);
            final double byIterativeMethod = comparator.byIterativeMethod(t);
            final double bySeriesExpansion = comparator.bySeriesExpansion(t);
            final double byTrigoIdentities = comparator.usingTrigonometricIdentities(t);
            final double byImplementation  = projection.φ(t);
            assertEquals("Iterative method",  φ, byIterativeMethod, 1E-11);
            assertEquals("Series expansion",  φ, bySeriesExpansion, 1E-11);
            assertEquals("Trigo. identities", φ, byTrigoIdentities, 1E-11);
            assertEquals("Implementation",    φ, byImplementation,  1E-11);
            /*
             * Verify that the formulas modified with trigonometric identities give the same results
             * than the original formulas. The main purpose of this test is to detect mistake during
             * the application of identities.
             */
            assertEquals(byTrigoIdentities, bySeriesExpansion, 1E-15);  // Tolerance threshold close to 1 ULP of 2π.
            assertEquals(projection.φ(t),   byTrigoIdentities, 1E-15);
        }
    }
}
