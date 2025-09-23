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
import static java.lang.Double.*;
import static java.lang.StrictMath.*;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.util.internal.shared.DoubleDouble;
import org.apache.sis.referencing.operation.transform.AbstractMathTransform1D;
import static org.apache.sis.referencing.operation.projection.NormalizedProjectionTest.TOLERANCE;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opengis.test.referencing.TransformTestCase;
import org.apache.sis.test.FailureDetailsReporter;
import org.apache.sis.test.TestUtilities;


/**
 * Tests the {@link ConformalProjection} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@ExtendWith(FailureDetailsReporter.class)
public final class ConformalProjectionTest extends TransformTestCase {
    /**
     * Natural logarithm of the pseudo-infinity as returned by Mercator formulas in the spherical
     * case, truncated to nearest integer. This is not a real infinity because there is no exact
     * representation of π/2 in base 2, so tan(π/2) is not positive infinity.
     */
    static final int LN_INFINITY = 37;

    /**
     * Creates a new test case.
     */
    public ConformalProjectionTest() {
    }

    /**
     * Tests a few formulas used by the Mercator projection in the spherical case.
     * This is a little bit more a Java test than an Apache SIS test (or to be more
     * accurate, a test of our understanding of the {@code java.lang.Math} library).
     *
     * <pre class="text">
     *   Forward:  y = log(tan(π/4 + φ/2))
     *   Inverse:  φ = π/2 - 2*atan(exp(-y))</pre>
     */
    @Test
    public void verifyMath() {
        assertEquals(0, log(tan(PI/4)),                   TOLERANCE, "Forward 0°N");
        assertEquals(0, PI/2 - 2*atan(exp(0)),            TOLERANCE, "Inverse 0 m");
        assertEquals(NEGATIVE_INFINITY, log(tan(0)),      TOLERANCE, "Forward 90°S");
        assertEquals(NaN,  log(tan(nextDown(0))),         TOLERANCE, "Forward (90+ε)°S");
        assertEquals(PI/2, atan(exp(-NEGATIVE_INFINITY)), TOLERANCE, "Inverse −∞");
        assertEquals(PI/2, atan(exp(LN_INFINITY + 1)),    TOLERANCE, "Inverse −∞ appr.");
        /*
         * tan(PI/2) do not produces positive infinity as we would expect, because there is no
         * exact representation of PI in base 2.  Experiments show that we get some high value
         * instead (1.633E+16 on my machine, having a logarithm of 37.332).
         */
        assertTrue  (1E+16 < tan(PI/2),                           "Forward 90°N");
        assertTrue  (LN_INFINITY < log(tan(PI/2)),                "Forward 90°N");
        assertEquals(NaN, log(tan(nextUp(PI/2))),      TOLERANCE, "Forward (90+ε)°N");
        assertEquals(0, atan(exp(NEGATIVE_INFINITY)),  TOLERANCE, "Inverse +∞");
        assertEquals(0, atan(exp(-(LN_INFINITY + 1))), TOLERANCE, "Inverse +∞ appr.");
        /*
         * Some checks performed in our projection implementations assume that
         * conversion of 90° to radians give exactly Math.PI/2.
         */
        final DoubleDouble dd = DoubleDouble.DEGREES_TO_RADIANS.multiply(90);
        assertEquals(PI/2, dd.value);
        assertEquals(PI/2, toRadians(90));
    }

    /**
     * Tests the {@link ConformalProjection#expΨ(double, double)} function.
     *
     * <pre class="text">
     *   Forward:  y = -log(t(φ))
     *   Inverse:  φ = φ(exp(-y))</pre>
     */
    @Test
    public void verifyNorthingKnownValues() {
        verifyNorthingKnownValues(false);                       // Spherical case
        verifyNorthingKnownValues(true);                        // Ellipsoidal case
    }

    /**
     * Tests {@link ConformalProjection#expΨ(double, double)} on some known values.
     *
     * @param  ellipsoidal  {@code true} for an ellipsoidal case, or {@code false} for a spherical case.
     */
    private void verifyNorthingKnownValues(final boolean ellipsoidal) {
        transform = new NoOp(ellipsoidal);
        tolerance = TOLERANCE;

        assertEquals(NaN, expΨ(NaN),               tolerance, "f(NaN) = NaN");
        assertEquals(NaN, expΨ(NEGATIVE_INFINITY), tolerance, "f( ±∞) = NaN");
        assertEquals(NaN, expΨ(POSITIVE_INFINITY), tolerance, "f( ±∞) = NaN");
        assertEquals(  1, expΨ(0),                 tolerance, "f(  0°) = 1");
        assertEquals(  0, expΨ(-PI/2),             tolerance, "f(-90°) = 0");
        assertTrue  (     expΨ(-PI/2 - 0.1)        < 0,       "f(< -90°) < 0");
        assertTrue  (     expΨ(nextDown(-PI/2))    < 0,       "f(< -90°) < 0");
        /*
         * Values around π/2 are a special case. Theoretically the result should be positive infinity.
         * But since we do not have an exact representatation of π/2, we instead get a high number.
         * Furthermore, the value does not become negative immediately after π/2; we have to skip an
         * other IEEE 754 double value. This is because the real π/2 value is actually between PI/2
         * and nextUp(PI/2):
         *
         *      PI/2          =   1.570796326794896558…
         *      π/2           =   1.570796326794896619…
         *      nextUp(PI/2)  =   1.570796326794896780…
         */
        assertTrue(expΨ(+PI/2) > exp(LN_INFINITY),  "f(+90°) → ∞");
        assertTrue(expΨ(+PI/2 + 0.1) < 0,           "f(> +90°) < 0");
        assertTrue(expΨ(nextUp(nextUp(+PI/2))) < 0, "f(> +90°) < 0");
        /*
         * Test function periodicity. This is not a strong requirement for the expΨ(…) function,
         * but we nevertheless try to ensure that the method behaves correctly with unexpected values.
         */
        assertEquals( 1, expΨ(+2*PI),   tolerance,           "f(+360°)");
        assertEquals( 0, expΨ(+PI*3/2), tolerance,           "f(+270°)");
        assertEquals(-1, expΨ(+PI),     tolerance,           "f(+180°)");
        assertEquals(-1, expΨ(-PI),     tolerance,           "f(-180°)");
        assertTrue  (    expΨ(-PI*3/2)  < exp(-LN_INFINITY), "f(-270°) → ∞");
        assertEquals( 1, expΨ(-2*PI),   tolerance,           "f(-360°)");
        assertEquals( 0, expΨ(-PI*5/2), tolerance,           "f(-450°)");
        /*
         * Use in a way close to (but not identical)
         * to the way the Mercator projection need it.
         */
        assertEquals(0,                 log(expΨ(0)),     tolerance, "Mercator(0°)");
        assertEquals(NEGATIVE_INFINITY, log(expΨ(-PI/2)), tolerance, "Mercator(90°S)");
        assertTrue  (LN_INFINITY <      log(expΨ(+PI/2)),            "Mercator(90°N)");
    }

    /**
     * Computes {@link ConformalProjection#expΨ(double, double)} for the given latitude.
     * The {@link #transform} field must have been set before this method is invoked.
     *
     * @param  φ  the latitude in radians.
     * @return {@code Math.exp} of the Mercator projection of the given latitude.
     *
     * @see #φ(double)
     */
    private double expΨ(final double φ) {
        final ConformalProjection projection = (ConformalProjection) transform;
        return projection.expΨ(φ, projection.eccentricity * sin(φ));
    }

    /**
     * Tests the {@link ConformalProjection#dy_dφ(double, double)} method.
     *
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void test_dy_dφ() throws TransformException {
        test_dy_dφ(false);                                      // Spherical case
        test_dy_dφ(true);                                       // Ellipsoidal case
    }

    /**
     * Tests the {@link ConformalProjection#dy_dφ(double, double)} method.
     *
     * @param  ellipsoidal  {@code true} for an ellipsoidal case, or {@code false} for a spherical case.
     */
    private void test_dy_dφ(final boolean ellipsoidal) throws TransformException {
        final NoOp projection = new NoOp(ellipsoidal);
        transform = new AbstractMathTransform1D() {
            @Override public double transform(final double φ) {
                return projection.expΨ(φ, projection.eccentricity * sin(φ));
            }
            @Override public double derivative(final double φ) {
                final double sinφ = sin(φ);
                return projection.dy_dφ(sinφ, cos(φ)) * transform(φ);
            }
        };
    }

    /**
     * Computes {@link ConformalProjection#φ(double)}.
     * The {@link #transform} field must have been set before this method is invoked.
     *
     * @param  rexpΨ  the reciprocal of the value returned by {@link #expΨ(double)}.
     * @return the latitude in radians.
     * @throws ProjectionException if the iteration does not converge.
     *
     * @see #expΨ(double)
     */
    private double φ(final double rexpΨ) throws ProjectionException {
        return ((ConformalProjection) transform).φ(rexpΨ);
    }

    /**
     * Tests the {@link ConformalProjection#φ(double)} function. We expect it to be
     * the converse of the {@link ConformalProjection#expΨ(double, double)} function.
     * In theory only the [-90° … +90°] range needs to be tested. However, the function is still
     * consistent in the [-90° … +270°] range so we test that range for tracking this fact.
     *
     * @throws ProjectionException if an error occurred while projecting a point.
     */
    @Test
    public void test_φ() throws ProjectionException {
        test_φ(false);                                          // Spherical case
        test_φ(true);                                           // Ellipsoidal case
    }

    /**
     * Tests {@link ConformalProjection#φ(double)} on known values and as the reverse of {@code expΨ(φ)}.
     */
    private void test_φ(final boolean ellipsoidal) throws ProjectionException {
        transform = new NoOp(ellipsoidal);
        tolerance = ellipsoidal ? NormalizedProjection.ITERATION_TOLERANCE : TOLERANCE;

        assertEquals(  NaN,   φ(NaN),               tolerance, "φ(NaN) = NaN");
        assertEquals(-PI/2,   φ(POSITIVE_INFINITY), tolerance, "φ( ∞)  = -90°");
        assertEquals(-PI/2,   φ(MAX_VALUE),         tolerance, "φ( ∞)  = -90°");
        assertEquals(   0,    φ(1),                 tolerance, "φ( 1)  =   0°");
        assertEquals( PI/2,   φ(MIN_VALUE),         tolerance, "φ( ε)  →  90°");
        assertEquals( PI/2,   φ(0),                 tolerance, "φ( 0)  =  90°");
        assertEquals( PI/2,   φ(-MIN_VALUE),        tolerance, "φ(-ε)  →  90°");
        assertEquals( PI,     φ(-1),                tolerance, "φ(-1)  = 180°");
        assertEquals( PI*1.5, φ(-MAX_VALUE),        tolerance, "φ(−∞)  = 270°");
        assertEquals( PI*1.5, φ(NEGATIVE_INFINITY), tolerance, "φ(−∞)  = 270°");
        /*
         * Using t(φ) as a reference.
         */
        for (int i=-90; i<=270; i+=5) {
            final double φ   = toRadians(i);
            final double t    = 1 / expΨ(φ);
            final double back = toDegrees(φ(t));
            if (i <= 90) {
                assertTrue(t >= 0, "φ(t) in valid range should be positive.");
            } else {
                assertTrue(t < 0, "φ(t) in invalid range should be negative.");
            }
            assertEquals(i, back, tolerance, "Inverse function does not match.");
        }
    }

    /**
     * Performs a comparison between φ values computed by various methods.
     * The comparisons are:
     *
     * <ol>
     *   <li>φ values computed by an iterative method.</li>
     *   <li>φ values computed by the series expansion given by EPSG guide.</li>
     *   <li>φ values computed by the actual {@link ConformalProjection} implementation,
     *       which uses a modified form of series expansion using trigonometric identities.</li>
     * </ol>
     *
     * @throws ProjectionException if an error occurred during computation of φ.
     *
     * @see MercatorMethodComparison
     */
    @Test
    public void compareReference() throws ProjectionException {
        final ConformalProjection projection = new NoOp(true);
        final MercatorMethodComparison comparator = new MercatorMethodComparison(projection);
        final Random random = TestUtilities.createRandomNumberGenerator();
        final int numSamples = 2000;
        for (int i=0; i<numSamples; i++) {
            final double φ = random.nextDouble() * PI - PI/2;
            final double t = 1 / comparator.expΨ(φ);
            final double byIterativeMethod = comparator.byIterativeMethod(t);
            final double bySeriesExpansion = comparator.bySeriesExpansion(t);
            final double byImplementation  = projection.φ(t);
            assertEquals(φ, byIterativeMethod, 1E-11, "Iterative method");
            assertEquals(φ, bySeriesExpansion, 1E-11, "Series expansion");
            assertEquals(φ, byImplementation,  1E-11, "Implementation");
            /*
             * Verify that the formulas modified with trigonometric identities give the same results
             * than the original formulas. The main purpose of this test is to detect mistake during
             * the application of identities.
             */
            assertEquals(byImplementation, bySeriesExpansion, 1E-15);       // Tolerance threshold close to 1 ULP of 2π.
        }
    }
}
