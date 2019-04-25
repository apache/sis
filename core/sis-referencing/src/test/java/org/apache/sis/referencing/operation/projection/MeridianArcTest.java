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
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.transform.AbstractMathTransform1D;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.internal.referencing.Resources;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static java.lang.StrictMath.*;
import static org.junit.Assert.assertEquals;


/**
 * Tests the distances along meridian arc computed by {@link MeridianArcBased}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
@DependsOn(NormalizedProjectionTest.class)
public final strictfp class MeridianArcTest extends MapProjectionTestCase {
    /**
     * Threshold for comparison of floating point values.
     */
    private static final double STRICT = 0;

    /**
     * Creates the projection to be tested.
     *
     * @param  ellipsoidal   {@code false} for a sphere, or {@code true} for WGS84 ellipsoid.
     * @return a test instance of the projection.
     */
    private MeridianArcBased create(final boolean ellipsoidal) {
        final DefaultOperationMethod provider = new org.apache.sis.internal.referencing.provider.Sinusoidal();
        final Sinusoidal projection = new Sinusoidal(provider, parameters(provider, ellipsoidal));
        tolerance = NormalizedProjection.ANGULAR_TOLERANCE;     // = linear tolerance on a sphere of radius 1.
        return projection;
    }

    /**
     * Computes the distance on meridian using equation given in EPSG guidance notes, which is also from Snyder book.
     * The equation is given in Snyder 3-21. We use this equation as a reference for testing validity of other forms.
     * The equation is:
     *
     * {@preformat math
     *   M = a[(1 – e²/4 – 3e⁴/64  –  5e⁶/256  – …)⋅φ
     *          – (3e²/8 + 3e⁴/32  + 45e⁶/1024 + …)⋅sin2φ
     *                 + (15e⁴/256 + 45e⁶/1024 + …)⋅sin4φ
     *                            – (35e⁶/3072 + …)⋅sin6φ
     *                                         + …]
     * }
     *
     * @param  φ  latitude in radians.
     * @return distance on meridian from equator to the given latitude on an ellipsoid with semi-major axis of 1.
     */
    private static double reference(final MeridianArcBased projection, final double φ) {
        final double e2 = projection.eccentricitySquared;
        final double e4 = e2*e2;
        final double e6 = e2*e4;
        /*
         * Smallest terms of the series should be added first for better accuracy.
         * The final multiplication by (1 - e2) is already included in all terms.
         */
        return - (35./3072*e6)                        * sin(6*φ)
               + (45./1024*e6 + 15./256*e4)           * sin(4*φ)
               - (45./1024*e6 +  3./32 *e4 + 3./8*e2) * sin(2*φ)
               + (-5./256 *e6 -  3./64 *e4 - 1./4*e2 + 1)  *  φ;
    }

    /**
     * Series expansion with more terms. We use this formulas as a reference for testing accuracy of the formula
     * implemented by {@link MeridianArcBased#distance(double, double, double)} after making sure that this value
     * is in agreement with {@link #reference(MeridianArcBased, double)}.
     *
     * <p>References:</p>
     * <ul>
     *   <li>Kawase, Kazushige (2011). A General Formula for Calculating Meridian Arc Length and its Application to Coordinate
     *       Conversion in the Gauss-Krüger Projection. Bulletin of the Geospatial Information Authority of Japan, Vol.59.
     *       <a href="http://www.gsi.go.jp/common/000062452.pdf">(download)</a></li>
     *   <li><a href="https://en.wikipedia.org/wiki/Meridian_arc#Series_expansions">Meridian arc — series expansions</a>
     *       on Wikipedia.</li>
     * </ul>
     */
    private static double referenceMoreAccurate(final MeridianArcBased projection, final double φ) {
        final double e2  = projection.eccentricitySquared;
        final double e4  = e2*e2;
        final double e6  = e2*e4;
        final double e8  = e4*e4;
        final double e10 = e2*e8;
        final double C1  = 43659./ 65536*e10 + 11025./16384*e8 + 175./256*e6 + 45./64*e4 + 3./4*e2 + 1;
        final double C2  = 72765./ 65536*e10 +  2205./ 2048*e8 + 525./512*e6 + 15./16*e4 + 3./4*e2;
        final double C3  = 10395./ 16384*e10 +  2205./ 4096*e8 + 105./256*e6 + 15./64*e4;
        final double C4  = 31185./131072*e10 +   315./ 2048*e8 +  35./512*e6;
        final double C5  =  3465./ 65536*e10 +   315./16384*e8;
        final double C6  =   693./131072*e10;
        final double v   = -C6*sin(10*φ)/10 + C5*sin(8*φ)/8 - C4*sin(6*φ)/6 + C3*sin(4*φ)/4 - C2*sin(2*φ)/2 + C1*φ;
        return v * (1 - e2);
    }

    /**
     * Computes the latitude of a given distance along meridian arc using Newton's method. This method is described in
     * <a href="https://en.wikipedia.org/wiki/Meridian_arc#The_inverse_meridian_problem_for_the_ellipsoid">Wikipedia</a>.
     * This method depends on the forward method and can not be more accurate than that method. For testing purposes, we
     * use the {@linkplain #referenceMoreAccurate must accurate method implemented in this class} regardless performance.
     *
     * @param  m  the distance on ellipsoid of semi-major axis length of 1.
     * @return latitude for the given distance on meridian arc, in radians.
     */
    private static double inverse(final MeridianArcBased projection, final double m) throws TransformException {
        final double ITERATION_TOLERANCE = 1E-13;               // Should be less than `referenceMoreAccurate` accuracy.
        final double e2 = projection.eccentricitySquared;
        int i = NormalizedProjection.MAXIMUM_ITERATIONS;
        double φ = m;                                           // Starting point.
        do {
            /*
             * Meridian radius of curvature:   M(φ) = a(1 - ℯ²) / (1 - ℯ²sinφ)^(3/2).
             * We compute the inverse of M.
             */
            final double sinφ = sin(φ);
            double iM = 1 - e2*(sinφ*sinφ);
            iM *= sqrt(iM);
            iM /= (1 - e2);
            /*
             * From Wikipedia: Newton's method:  φᵢ₊₁ = φᵢ - [m(φᵢ) - m] / M(φᵢ)
             * In principle the numerator should be the derivative of m(φᵢ), but
             * the meridian radius of curvature can be used instead.
             */
            final double t = (m - referenceMoreAccurate(projection, φ)) * iM;
            φ += t;
            if (abs(t) <= ITERATION_TOLERANCE) {
                return φ;
            }
        } while (--i >= 0);
        throw new ProjectionException(Resources.format(Resources.Keys.NoConvergence));
    }

    /**
     * Compares {@link MeridianArcBased#distance(double, double, double)} with formulas taken as references.
     */
    @Test
    public void compareWithReference() {
        final MeridianArcBased projection = create(true);
        final Random random = TestUtilities.createRandomNumberGenerator();
        for (int i=0; i<100; i++) {
            final double φ = random.nextDouble() * PI - PI/2;
            final double reference = reference(projection, φ);
            final double accurate  = referenceMoreAccurate(projection, φ);
            final double actual    = projection.distance(φ, sin(φ), cos(φ));
            assertEquals("Accurate formula disagrees with reference.", reference, accurate, 2E-10);
            assertEquals("Implementation disagrees with our formula.", accurate,  actual,   1E-13);
        }
    }

    /**
     * Compares {@link MeridianArcBased#distance(double, double, double)} with spherical formula.
     * In the spherical case, {@code distance(φ)} should be equal to φ.
     */
    @Test
    public void compareWithSphere() {
        final MeridianArcBased projection = create(false);
        assertEquals("Expected spherical projection.", 0, projection.eccentricity, STRICT);
        final Random random = TestUtilities.createRandomNumberGenerator();
        for (int i=0; i<20; i++) {
            final double φ = random.nextDouble() * PI - PI/2;
            assertEquals("When excentricity=0, distance(φ, sinφ, cosφ) simplify to φ.",
                    φ, projection.distance(φ, sin(φ), cos(φ)), 1E-15);
        }
    }

    /**
     * Compares {@link MeridianArcBased#latitude(double)} with iterative approach.
     * On WGS 84 ellipsoid, the distance <var>M</var> is between ±π/2 × 0.9983243 approximately.
     *
     * @throws TransformException if the iterative method does not converge.
     */
    @Test
    @DependsOnMethod("compareWithReference")
    public void compareInverse() throws TransformException {
        final MeridianArcBased projection = create(true);
        final Random random = TestUtilities.createRandomNumberGenerator();
        for (int i=0; i<50; i++) {
            final double m = random.nextDouble() * (PI * 0.998) - (PI/2) * 0.998;
            final double reference = inverse(projection, m);
            final double actual    = projection.latitude(m);
            assertEquals("Implementation disagrees with reference.", reference, actual, 1E-10);
        }
    }

    /**
     * Tests all methods, including {@link MeridianArcBased#dM_dφ(double)}, on a sphere.
     *
     * @throws TransformException if an iteration does not converge.
     */
    @Test
    @DependsOnMethod("compareInverse")
    public void testOnSphere() throws TransformException {
        verifyInDomain(false, 20);
    }

    /**
     * Tests all methods, including {@link MeridianArcBased#dM_dφ(double)}, on an ellipsoid.
     *
     * @throws TransformException if an iteration does not converge.
     */
    @Test
    @DependsOnMethod("testOnSphere")
    public void testOnEllipsoid() throws TransformException {
        verifyInDomain(true, 100);
    }

    /**
     * Verifies transform, inverse transform and derivative in the [-90 … 90]° latitude range.
     */
    private void verifyInDomain(final boolean ellipsoidal, final int numCoordinates) throws TransformException {
        final MeridianArcBased projection = create(ellipsoidal);
        derivativeDeltas = new double[] {toRadians(0.01)};
        transform = new AbstractMathTransform1D() {
            @Override public double transform (final double φ) {
                return projection.distance(φ, sin(φ), cos(φ));
            }
            @Override public double derivative(final double φ) {
                final double sinφ = sin(φ);
                return projection.dM_dφ(sinφ*sinφ);
            }
            @Override public MathTransform1D inverse() {
                return new AbstractMathTransform1D() {
                    @Override public double transform (final double M) throws TransformException {
                        return projection.latitude(M);
                    }
                    @Override public double derivative(final double φ) throws TransformException {
                        throw new TransformException("Unsupported");
                    }
                };
            }
        };
        verifyInDomain(new double[] {toRadians(-89)},
                       new double[] {toRadians(+89)},
                       new int[] {numCoordinates},
                       TestUtilities.createRandomNumberGenerator());
    }
}
