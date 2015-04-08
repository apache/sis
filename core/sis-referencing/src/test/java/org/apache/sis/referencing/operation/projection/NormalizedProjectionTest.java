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

import org.opengis.referencing.operation.TransformException;
import org.opengis.test.referencing.TransformTestCase;
import org.apache.sis.referencing.operation.transform.AbstractMathTransform1D;
import org.apache.sis.internal.util.DoubleDouble;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestUtilities;
import org.junit.Test;

import static java.lang.Double.*;
import static java.lang.StrictMath.*;
import static org.apache.sis.internal.metadata.ReferencingServices.NAUTICAL_MILE;
import static org.junit.Assert.*;


/**
 * Tests the {@link NormalizedProjection} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@DependsOn({
    // Following dependency is where the basic parameters (e.g. SEMI_MAJOR) are tested.
    // Those parameters are needed by NoOp pseudo-projection, which is used in this class.
    org.apache.sis.internal.referencing.provider.MapProjectionTest.class
})
public final strictfp class NormalizedProjectionTest extends TransformTestCase {
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
     * Computes {@link NormalizedProjection#expOfNorthing(double, double)} for the given latitude.
     *
     * @param  projection The projection on which to invoke {@code expOfNorthing(…)}.
     * @param  φ The latitude in radians.
     * @return {@code Math.exp} of the Mercator projection of the given latitude.
     */
    static double expOfNorthing(final NormalizedProjection projection, final double φ) {
        return projection.expOfNorthing(φ, projection.excentricity * sin(φ));
    }

    /**
     * Computes {@link NormalizedProjection#expOfNorthing(double, double)} for the given latitude.
     *
     * @param  φ The latitude in radians.
     * @return {@code Math.exp} of the Mercator projection of the given latitude.
     */
    private double expOfNorthing(final double φ) {
        return expOfNorthing((NormalizedProjection) transform, φ);
    }

    /**
     * Computes {@link NormalizedProjection#φ(double)}.
     *
     * @param  expOfSouthing The reciprocal of the value returned by {@link #expOfNorthing(double)}.
     * @return The latitude in radians.
     * @throws ProjectionException if the iteration does not converge.
     */
    private double φ(final double expOfSouthing) throws ProjectionException {
        return ((NormalizedProjection) transform).φ(expOfSouthing);
    }

    /**
     * Tests the value documented in the javadoc. Those value may be freely changed;
     * those tests exist only to increase the chances that the documented values are right.
     */
    @Test
    public void testDocumentation() {
        double minutes = toDegrees(NormalizedProjection.ANGULAR_TOLERANCE) * 60;
        assertEquals("Documentation said 1 cm precision.", 0.01, minutes*NAUTICAL_MILE, 0.005);

        minutes = toDegrees(NormalizedProjection.ITERATION_TOLERANCE) * 60;
        assertEquals("Documentation said 2.5 mm precision.", 0.0025, minutes*NAUTICAL_MILE, 0.0005);
    }

    /**
     * Tests the {@link NormalizedProjection#excentricity} value.
     */
    @Test
    public void testExcentricity() {
        NormalizedProjection projection;
        transform = projection = new NoOp(false);
        assertEquals("excentricity", 0.0, projection.excentricity, 0.0);

        transform = projection = new NoOp(true);
        assertEquals("excentricity", 0.08181919084262157, projection.excentricity, TOLERANCE);
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
        /*
         * Some checks performed in our projection implementations assume that
         * conversion of 90° to radians give exactly Math.PI/2.
         */
        final DoubleDouble dd = DoubleDouble.createDegreesToRadians();
        dd.multiply(90);
        assertEquals(PI/2, dd.value, 0.0);
        assertEquals(PI/2, toRadians(90), 0.0);
    }

    /**
     * Tests the {@link NormalizedProjection#expOfNorthing(double, double)} function.
     *
     * {@preformat text
     *   Forward:  y = -log(t(φ))
     *   Inverse:  φ = φ(exp(-y))
     * }
     */
    @Test
    public void testExpOfNorthing() {
        transform = new NoOp(false);   // Spherical case
        tolerance = TOLERANCE;
        doTestExpOfNorthing();
        transform = new NoOp(true);    // Ellipsoidal case
        doTestExpOfNorthing();
    }

    /**
     * Implementation of {@link #testExpOfNorthing()}.
     * The {@link #projection} field must have been set before this method is called.
     */
    private void doTestExpOfNorthing() {
        assertEquals("f(NaN) = NaN",       NaN, expOfNorthing(NaN),               tolerance);
        assertEquals("f( ±∞) = NaN",       NaN, expOfNorthing(NEGATIVE_INFINITY), tolerance);
        assertEquals("f( ±∞) = NaN",       NaN, expOfNorthing(POSITIVE_INFINITY), tolerance);
        assertEquals("f(  0°) = 1",          1, expOfNorthing(0),                 tolerance);
        assertEquals("f(-90°) = 0",          0, expOfNorthing(-PI/2),             tolerance);
        assertTrue  ("f(< -90°) < 0",           expOfNorthing(-PI/2 - 0.1)        < 0);
        assertTrue  ("f(< -90°) < 0",           expOfNorthing(nextDown(-PI/2))    < 0);
        /*
         * Values around π/2 are a special case. Theoretically the result should be positive infinity.
         * But since we do not have an exact representatation of π/2, we instead get a high number.
         * Furthermore the value does not become negative immediately after π/2; we have to skip an
         * other IEEE 754 double value. This is because the real π/2 value is actually between PI/2
         * and nextUp(PI/2):
         *
         *      PI/2          =   1.570796326794896558…
         *      π/2           =   1.570796326794896619…
         *      nextUp(PI/2)  =   1.570796326794896780…
         */
        assertTrue("f(+90°) → ∞",   expOfNorthing(+PI/2) > exp(LN_INFINITY));
        assertTrue("f(> +90°) < 0", expOfNorthing(+PI/2 + 0.1) < 0);
        assertTrue("f(> +90°) < 0", expOfNorthing(nextUp(nextUp(+PI/2))) < 0);
        /*
         * Test function periodicity. This is not a strong requirement for the expOfNorthing(…) function,
         * but we nevertheless try to ensure that the method behaves correctly with unexpected values.
         */
        assertEquals("f(+360°)",  1, expOfNorthing(+2*PI),   tolerance);
        assertEquals("f(+270°)",  0, expOfNorthing(+PI*3/2), tolerance);
        assertEquals("f(+180°)", -1, expOfNorthing(+PI),     tolerance);
        assertEquals("f(-180°)", -1, expOfNorthing(-PI),     tolerance);
        assertTrue  ("f(-270°) → ∞", expOfNorthing(-PI*3/2)  < exp(-LN_INFINITY));
        assertEquals("f(-360°)",  1, expOfNorthing(-2*PI),   tolerance);
        assertEquals("f(-450°)",  0, expOfNorthing(-PI*5/2), tolerance);
        /*
         * Use in a way close to (but not identical)
         * to the way the Mercator projection need it.
         */
        assertEquals("Mercator(0°)",   0,                 log(expOfNorthing(0)),     tolerance);
        assertEquals("Mercator(90°S)", NEGATIVE_INFINITY, log(expOfNorthing(-PI/2)), tolerance);
        assertTrue  ("Mercator(90°N)", LN_INFINITY <      log(expOfNorthing(+PI/2)));
    }

    /**
     * Tests the {@link NormalizedProjection#φ(double)} function. We expect it to be the converse of the
     * {@link NormalizedProjection#t(double, double)} function. In theory only the [-90° … +90°] range needs
     * to be tested. However the function still consistent in the [-90° … +270°] range so we test that range
     * for tracking this fact.
     *
     * @throws ProjectionException Should never happen.
     */
    @Test
    @DependsOnMethod("testExpOfNorthing")
    public void test_φ() throws ProjectionException {
        transform = new NoOp(false);   // Spherical case
        tolerance = TOLERANCE;
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
            assertEquals("Inverse function doesn't match.", i, back, tolerance);
        }
    }

    /**
     * Tests the {@link NormalizedProjection#dy_dφ(double, double)} method.
     *
     * @throws TransformException Should never happen.
     */
    @Test
    @DependsOnMethod("testExpOfNorthing")
    public void test_dy_dφ() throws TransformException {
        tolerance = 1E-7;
        doTest_dy_dφ(new NoOp(false));      // Spherical case
        doTest_dy_dφ(new NoOp(true));       // Ellipsoidal case
    }

    /**
     * Implementation of {@link #test_dy_dφ()}.
     * The {@link #projection} field must have been set before this method is called.
     */
    private void doTest_dy_dφ(final NoOp projection) throws TransformException {
        transform = new AbstractMathTransform1D() {
            @Override public double transform(final double φ) {
                return expOfNorthing(projection, φ);
            }
            @Override public double derivative(final double φ) {
                final double sinφ = sin(φ);
                return projection.dy_dφ(sinφ, cos(φ)) * expOfNorthing(projection, φ);
            }
        };
        verifyInDomain(-89 * (PI/180), 89 * (PI/180));  // Verify from 85°S to 85°N.
    }

    /**
     * Convenience method invoking {@link TransformTestCase#verifyInDomain} for an 1D transform.
     */
    private void verifyInDomain(final double min, final double max) throws TransformException {
        derivativeDeltas = new double[] {2E-8};
        isInverseTransformSupported = false;
        verifyInDomain(
                new double[] {min},     // Minimal value to test.
                new double[] {max},     // Maximal value to test.
                new int[]    {100},     // Number of points to test.
                TestUtilities.createRandomNumberGenerator());
    }
}
