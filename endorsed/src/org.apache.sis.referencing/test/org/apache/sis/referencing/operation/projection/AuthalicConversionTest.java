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
import static java.lang.StrictMath.*;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.operation.provider.LambertCylindricalEqualArea;
import org.apache.sis.util.internal.shared.Constants;
import static org.apache.sis.math.MathFunctions.atanh;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestUtilities;


/**
 * Tests {@link AuthalicConversion}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class AuthalicConversionTest extends MapProjectionTestCase {
    /**
     * Creates a new test case.
     */
    public AuthalicConversionTest() {
    }

    /**
     * Creates the projection to be tested.
     *
     * @param  ellipsoidal   {@code false} for a sphere, or {@code true} for WGS84 ellipsoid.
     * @return a test instance of the projection.
     */
    private AuthalicConversion create(final boolean ellipsoidal) {
        final var provider   = new LambertCylindricalEqualArea();
        final var projection = new CylindricalEqualArea(provider, parameters(provider, ellipsoidal));
        tolerance = NormalizedProjection.ANGULAR_TOLERANCE;     // = linear tolerance on a sphere of radius 1.
        return projection;
    }

    /**
     * Computes φ using equation given in EPSG guidance notes, which is also from Snyder book.
     * We use this equation as a reference for testing validity of other forms.
     *
     * @param  y  in the cylindrical case, this is northing on the normalized ellipsoid.
     * @return the latitude in radians.
     */
    private static double reference(final AuthalicConversion projection, final double y) {
        final double e    = projection.eccentricity;
        final double e2   = projection.eccentricitySquared;
        final double e4   = e2 * e2;
        final double e6   = e2 * e4;
        final double c2β  = 517./5040  * e6  +  31./180 * e4  +  1./3 * e2;
        final double c4β  = 251./3780  * e6  +  23./360 * e4;
        final double c6β  = 761./45360 * e6;
        final double qmp  = (1/(1 - e*e) + atanh(e)/e);
        final double sinβ = y / qmp;
        final double β    = asin(sinβ);
        return c6β * sin(6*β)
             + c4β * sin(4*β)
             + c2β * sin(2*β)
             + β;                           // Snyder 3-18
    }

    /**
     * Compares {@link AuthalicConversion#φ(double)} with formula taken as references.
     *
     * @throws ProjectionException if the function does not converge.
     */
    @Test
    public void compareWithReference() throws ProjectionException {
        final AuthalicConversion projection = create(true);
        final Random random = TestUtilities.createRandomNumberGenerator();
        for (int i=0; i<100; i++) {
            final double y = random.nextDouble() * 3 - 1.5;
            final double reference = reference(projection, y);
            final double actual    = projection.φ(y / projection.qmPolar);
            assertEquals(reference, actual, NormalizedProjection.ITERATION_TOLERANCE);
        }
    }

    /**
     * Searches a value for {@link AuthalicConversion#ECCENTRICITY_THRESHOLD}.
     * This method is not part of test suite. Steps to enable:
     *
     * <ol>
     *   <li>In {@link AuthalicConversion#φ(double)} method, for {@code useIterations} to {@code false}.</li>
     *   <li>Add a {@link Test} annotation on this method.
     * </ol>
     *
     * @throws ProjectionException if {@link AuthalicConversion#φ(double)} did not converge.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public void searchThreshold() throws ProjectionException {
        tolerance = NormalizedProjection.ANGULAR_TOLERANCE;
        final var provider = new LambertCylindricalEqualArea();
        final Parameters parameters = parameters(provider, true);
        for (double e = 0.05; e <= 0.2; e += 0.001) {
            final double a = parameters.parameter(Constants.SEMI_MAJOR).doubleValue();
            parameters.parameter(Constants.SEMI_MINOR).setValue(a * sqrt(1 - e*e));
            final CylindricalEqualArea projection = new CylindricalEqualArea(provider, parameters);
            for (double y = -1.25; y <= 1.25; y += 0.01) {
                final double reference = reference(projection, y);
                final double actual = projection.φ(y / projection.qmPolar);
                if (abs(actual - reference) > NormalizedProjection.ANGULAR_TOLERANCE) {
                    System.out.println("Error exceeds tolerance threshold at eccentricity " + e);
                    return;
                }
            }
        }
    }
}
