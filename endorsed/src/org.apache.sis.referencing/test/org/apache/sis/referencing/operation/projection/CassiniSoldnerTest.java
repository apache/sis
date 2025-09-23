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

import static java.lang.Double.NaN;
import static java.lang.StrictMath.*;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.provider.MapProjection;
import org.apache.sis.referencing.internal.shared.Formulas;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.parameter.Parameters;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests the {@link CassiniSoldner} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 */
public final class CassiniSoldnerTest extends MapProjectionTestCase {
    /**
     * Creates a new test case.
     */
    public CassiniSoldnerTest() {
    }

    /**
     * Returns the operation method for the projection tested in this class.
     *
     * @param  hyperbolic  {@code false} for standard case, or {@code true} for hyperbolic case.
     */
    private static MapProjection method(final boolean hyperbolic) {
        return hyperbolic ? new org.apache.sis.referencing.operation.provider.HyperbolicCassiniSoldner()
                          : new org.apache.sis.referencing.operation.provider.CassiniSoldner();
    }

    /**
     * Creates a new instance of {@link CassiniSoldner}. This instance expects
     * angles in radians and a φ₀ value of zero; this is not a complete transform.
     *
     * @param  ellipse  {@code false} for a sphere, or {@code true} for WGS84 ellipsoid.
     * @return newly created projection.
     */
    private static CassiniSoldner create(final boolean ellipse) {
        final MapProjection provider = method(false);
        final Parameters pg = Parameters.castOrWrap(provider.getParameters().createValue());
        if (ellipse) {
            pg.parameter("semi-major").setValue(WGS84_A);
            pg.parameter("semi-minor").setValue(WGS84_B);
            return new CassiniSoldner(provider, pg);
        } else {
            pg.parameter("semi-major").setValue(RADIUS);
            pg.parameter("semi-minor").setValue(RADIUS);
            return new CassiniSoldner.Spherical(new CassiniSoldner(provider, pg));
        }
    }

    /**
     * Tests some identities related to the {@link CassiniSoldner#distance(double, double, double)} method.
     *
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void testSimplePoint() throws TransformException {
        transform = create(false);
        /*
         * Fix φ=45°, which implies tan(φ)=1.
         * Test using the CassiniSoldner spherical equation.
         */
        final var point = new DirectPosition2D();
        final double domain = toRadians(5);
        final double step   = domain / 20;
        for (double λ = -domain; λ <= domain; λ += step) {
            final double yFromSimplified = PI/2 - atan(cos(λ));
            point.x = λ;
            point.y = PI/4;
            assertSame(point, transform.transform(point, point));
            assertEquals(yFromSimplified, point.y, 1E-9,
                    "Given excentricity=0 and φ=45°, the spherical equation should simplify "
                    + "to a very simple expression, which we are testing here.");
        }
    }

    /**
     * Tests the point given in EPSG example for the usual "Cassini-Soldner" projection.
     * This is the same test as {@link #runGeoapiTest()} but is repeated here for easier debugging.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void testClassic() throws FactoryException, TransformException {
        createCompleteProjection(method(false),
                31706587.88,                            // Semi-major axis (Clarke's links)
                31706587.88 * (20855233./20926348),     // Semi-minor axis (Clarke's links)
                -(61 + 20./60),                         // Longitude of natural origin
                10 + (26 + 30./60)/60,                  // Latitude of natural origin
                NaN,                                    // Standard parallel 1 (none)
                NaN,                                    // Standard parallel 2 (none)
                NaN,                                    // Scale factor (none)
                430000,                                 // False easting  (Clarke's links)
                325000);                                // False northing (Clarke's links)

        final double λ = -62;
        final double φ =  10;
        final var p = new DirectPosition2D(λ, φ);
        assertSame(p, transform.transform(p, p));
        assertEquals(66644.94, p.x, 0.005);
        assertEquals(82536.22, p.y, 0.005);

        assertSame(p, transform.inverse().transform(p, p));
        assertEquals(λ, p.x, Formulas.ANGULAR_TOLERANCE);
        assertEquals(φ, p.y, Formulas.ANGULAR_TOLERANCE);
    }

    /**
     * Tests the point given in EPSG example for the "Hyperbolic Cassini-Soldner" projection.
     * This is the same test as {@link #runGeoapiHyperbolicTest()} but is repeated here for
     * easier debugging.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void testHyperbolic() throws FactoryException, TransformException {
        createCompleteProjection(method(true),
                317063.667,                             // Semi-major axis (chains)
                317063.667 * (20854895./20926202),      // Semi-minor axis (chains)
                179 + 20./60,                           // Longitude of natural origin
                -(16 + 15./60),                         // Latitude of natural origin
                NaN,                                    // Standard parallel 1 (none)
                NaN,                                    // Standard parallel 2 (none)
                NaN,                                    // Scale factor (none)
                12513.318,                              // False easting  (chains)
                16628.885);                             // False northing (chains)

        final double λ =  179 + (59 + 39.6115/60)/60;   // 179°59′39.6115″E
        final double φ = -(16 + (50 + 29.2435/60)/60);  //  16°50′29.2435″S
        final var p = new DirectPosition2D(λ, φ);
        assertSame(p, transform.transform(p, p));
        assertEquals(16015.2890, p.x, 0.00005);
        assertEquals(13369.6601, p.y, 0.00005);

        assertSame(p, transform.inverse().transform(p, p));
        assertEquals(λ, p.x, Formulas.ANGULAR_TOLERANCE);
        assertEquals(φ, p.y, Formulas.ANGULAR_TOLERANCE);
    }

    /**
     * Creates a projection and tests the derivatives at a few points.
     *
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void testDerivative() throws TransformException {
        final double delta = toRadians(1. / 60) / 1852;         // Approximately 1 meter.
        derivativeDeltas = new double[] {delta, delta};

        // Tests spherical formulas
        tolerance = 1E-9;
        transform = create(false);
        validate();
        verifyDerivative(toRadians(+3), toRadians(-6));
        verifyDerivative(toRadians(-4), toRadians(40));

        // Tests ellipsoidal formulas
        transform = create(true);
        validate();
        verifyDerivative(toRadians(+3), toRadians(-6));
        verifyDerivative(toRadians(+3), toRadians(-10));
        verifyDerivative(toRadians(-4), toRadians(+10));
    }
}
