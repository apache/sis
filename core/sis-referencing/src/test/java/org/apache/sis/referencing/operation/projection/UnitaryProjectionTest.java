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

import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static java.lang.Double.*;
import static java.lang.StrictMath.*;
import static org.junit.Assert.*;


/**
 * Tests the {@link UnitaryProjection} class. This class uses {@link Mercator}
 * for testing purpose, because it is the simplest non-trivial projection.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public final strictfp class UnitaryProjectionTest extends TestCase {
    /**
     * Tolerance level for comparing floating point numbers.
     */
    private static final double TOLERANCE = 1E-12;

    /**
     * Natural logarithm of the pseudo-infinity as returned by Mercator formulas in the spherical
     * case, truncated to nearest integer. This is not a real infinity because there is no exact
     * representation of π/2 in base 2, so tan(π/2) is not positive infinity.
     */
    static final int LN_INFINITY = 37;

    /**
     * The projection to test.
     */
    private UnitaryProjection projection;

    /**
     * Computes {@link UnitaryProjection#t(double, double)} for the given latitude.
     *
     * @param  φ The latitude in radians.
     * @return Function 9-13 or 15-9, or the negative of function 7-7 from Snyder.
     */
    private double t(final double φ) {
        return projection.t(φ, sin(φ));
    }

    /**
     * Computes {@link UnitaryProjection#φ(double)}.
     *
     * @param  t The value returned by {@link #t(double)}.
     * @return The latitude in radians.
     * @throws ProjectionException if the iteration does not converge.
     */
    private double φ(final double t) throws ProjectionException {
        return projection.φ(t);
    }

    /**
     * Tests the value documented in the javadoc. Those value may be freely changed;
     * those tests exist only to increase the chances that the documented values are right.
     */
    @Test
    public void testDocumentation() {
        double minutes = toDegrees(UnitaryProjection.ANGLE_TOLERANCE) * 60;
        assertEquals("Documentation said 0.2″ precision.", 0.2, minutes*60, 0.1);
        assertEquals("Documentation said 6 km precision.", 6, minutes*1852, 0.5);

        minutes = toDegrees(UnitaryProjection.ITERATION_TOLERANCE) * 60;
        assertEquals("Documentation said 1 mm precision.", 0.001, minutes*1852, 0.0005);
    }

    /**
     * Tests a few formulas used by the Mercator projection in the spherical case.
     * This is a little bit more a Java test than an Apache SIS test (or to be more
     * accurate, a test of our understanding of the {@code java.lang.Math} library).
     *
     * {@preformat text
     *   Forward:  y = log(tan(π/4 + φ/2))
     *   Inverse:  φ = π/2 - 2*atan(exp(-y))
     * }
     */
    @Test
    public void testMath() {
        assertEquals("Forward 0°N",      0, log(tan(PI/4)),                   TOLERANCE);
        assertEquals("Inverse 0 m",      0, PI/2 - 2*atan(exp(0)),            TOLERANCE);
        assertEquals("Forward 90°S",     NEGATIVE_INFINITY, log(tan(0)),      TOLERANCE);
        assertEquals("Forward (90+ε)°S", NaN,  log(tan(-nextUp(0))),          TOLERANCE);
        assertEquals("Inverse -∞",       PI/2, atan(exp(-NEGATIVE_INFINITY)), TOLERANCE);
        assertEquals("Inverse -∞ appr.", PI/2, atan(exp(LN_INFINITY + 1)),    TOLERANCE);
        /*
         * tan(PI/2) do not produces positive infinity as we would expect, because there is no
         * exact representation of PI in base 2.  Experiments show that we get some high value
         * instead (1.633E+16 on my machine, having a logarithm of 37.332).
         */
        assertTrue  ("Forward 90°N",     1E+16 < tan(PI/2));
        assertTrue  ("Forward 90°N",     LN_INFINITY < log(tan(PI/2)));
        assertEquals("Forward (90+ε)°N", NaN, log(tan(nextUp(PI/2))),      TOLERANCE);
        assertEquals("Inverse +∞",       0, atan(exp(NEGATIVE_INFINITY)),  TOLERANCE);
        assertEquals("Inverse +∞ appr.", 0, atan(exp(-(LN_INFINITY + 1))), TOLERANCE);
    }

    /**
     * Tests the {@link UnitaryProjection#t(double, double)} function.
     * This is also a test of the forward Mercator projection in the ellipsoidal case.
     *
     * {@preformat text
     *   Forward:  y = -log(tsfn(φ))
     *   Inverse:  φ = cphi2(exp(-y))
     * }
     */
    @Test
    @org.junit.Ignore("Pending port of the Mercator projection.")
    public void test_t() {
        projection = null; // TODO: set spherical case here.
        doTest_t();
        projection = null; // TODO: set ellipsoidal case here.
        doTest_t();
    }

    /**
     * Implementation of {@link #test_t()}.
     * The {@link #projection} field must have been set before this method is called.
     */
    private void doTest_t() {
        assertEquals("Function contract",  NaN, t(NaN),               TOLERANCE);
        assertEquals("Function contract",  NaN, t(POSITIVE_INFINITY), TOLERANCE);
        assertEquals("Function contract",  NaN, t(NEGATIVE_INFINITY), TOLERANCE);
        assertEquals("Function contract",    1, t(0),                 TOLERANCE);
        assertEquals("Function contract",    0, t(+PI/2),             TOLERANCE);
        assertTrue  ("Function contract",       t(-PI/2)              > 1E+16);
        assertTrue  ("Out of bounds",           t(+PI/2 + 0.1)        < 0);
        assertTrue  ("Out of bounds",           t(-PI/2 - 0.1)        < 0);
        assertEquals("Out of bounds",       -1, t(PI),                TOLERANCE);
        assertTrue  ("Out of bounds",           t(PI*3/2)             < -1E+16);
        assertEquals("Function periodicity", 1, t(2*PI),              TOLERANCE);
        assertEquals("Function periodicity", 0, t(PI*5/2),            TOLERANCE);
        assertEquals("Forward 0°N",  0,                 -log(t(0)),     TOLERANCE);
        assertEquals("Forward 90°N", POSITIVE_INFINITY, -log(t(+PI/2)), TOLERANCE);
        assertTrue  ("Forward 90°S", -LN_INFINITY >     -log(t(-PI/2)));
    }

    /**
     * Tests the {@link UnitaryProjection#φ(double)} function. We expect it to be the converse of the
     * {@link UnitaryProjection#t(double, double)} function. In theory only the range [-90° … +90°]
     * needs to be tested. However the function still consistent in the range [-90° … +270°]
     * so we test that range for tracking this fact.
     *
     * @throws ProjectionException Should never happen.
     */
    @Test
    @DependsOnMethod("test_t")
    @org.junit.Ignore("Pending port of the Mercator projection.")
    public void test_φ() throws ProjectionException {
        projection = null; // TODO: set spherical case here.
        doTest_φ(TOLERANCE);
        projection = null; // TODO: set ellipsoidal case here.
        doTest_φ(UnitaryProjection.ITERATION_TOLERANCE);
    }

    /**
     * Implementation of {@link #test_φ()}.
     * The {@link #projection} field must have been set before this method is called.
     */
    private void doTest_φ(final double tolerance) throws ProjectionException {
        assertEquals("Function contract",  NaN,  φ(NaN),               tolerance);
        assertEquals("Function contract",  PI/2, φ(0),                 tolerance);
        assertEquals("Function contract",  PI/2, φ(MIN_VALUE),         tolerance);
        assertEquals("Function contract",  0,    φ(1),                 tolerance);
        assertEquals("Function contract", -PI/2, φ(MAX_VALUE),         tolerance);
        assertEquals("Function contract", -PI/2, φ(POSITIVE_INFINITY), tolerance);
        assertEquals("Out of bounds",   PI+PI/2, φ(NEGATIVE_INFINITY), tolerance);
        assertEquals("Out of bounds",   PI+PI/2, φ(-MAX_VALUE),        tolerance);
        assertEquals("Out of bounds",   PI,      φ(-1),                tolerance);
        assertEquals("Almost f. contract", PI/2, φ(-MIN_VALUE),        tolerance);
        /*
         * Using t(φ) as a reference.
         */
        for (int i=-90; i<=270; i+=5) {
            final double φ   = toRadians(i);
            final double t    = t(φ);
            final double back = toDegrees(φ(t));
            if (i <= 90) {
                assertTrue("tsfn in valid range should be positive.", t >= 0);
            } else {
                assertTrue("tsfn in invalid range should be negative.", t < 0);
            }
            assertEquals("Inverse function doesn't match.", i, back, tolerance);
        }
    }
}
