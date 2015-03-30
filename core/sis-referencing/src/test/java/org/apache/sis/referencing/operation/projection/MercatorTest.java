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

import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.referencing.provider.Mercator1SP;
import org.apache.sis.internal.referencing.provider.Mercator2SP;
import org.apache.sis.internal.referencing.provider.PseudoMercator;
import org.apache.sis.internal.referencing.provider.MillerCylindrical;
import org.apache.sis.internal.referencing.provider.Mercator2SP;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static java.lang.Double.*;
import static java.lang.StrictMath.*;
import static org.opengis.test.Assert.*;
import static org.apache.sis.referencing.operation.projection.NormalizedProjectionTest.LN_INFINITY;


/**
 * Tests the {@link Mercator} projection.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Simon Reynard (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@DependsOn(NormalizedProjectionTest.class)
public final strictfp class MercatorTest extends MapProjectionTestCase {
    /**
     * Returns a new instance of {@link Mercator} for an ellipsoid.
     *
     * @param ellipse {@code false} for a sphere, or {@code true} for WGS84 ellipsoid.
     */
    private void initialize(final boolean ellipse) {
        final Mercator2SP method = new Mercator2SP();
        transform = new Mercator(method, parameters(method, ellipse));
        if (!ellipse) {
            transform = new Mercator.Spherical((Mercator) transform);
        }
        tolerance = 1E-12;
        validate();
    }

    /**
     * Projects the given latitude value. The longitude is fixed to zero.
     * This method is useful for testing the behavior close to poles in a simple case.
     *
     * @param  φ The latitude.
     * @return The northing.
     * @throws ProjectionException if the projection failed.
     */
    private double transform(final double φ) throws ProjectionException {
        final double[] coordinate = new double[2];
        coordinate[1] = φ;
        ((NormalizedProjection) transform).transform(coordinate, 0, coordinate, 0, false);
        final double y = coordinate[1];
        if (!Double.isNaN(y) && !Double.isInfinite(y)) {
            assertEquals(0, coordinate[0], tolerance);
        }
        return y;
    }

    /**
     * Inverse projects the given northing value. The longitude is fixed to zero.
     * This method is useful for testing the behavior close to poles in a simple case.
     *
     * @param  y The northing.
     * @return The latitude.
     * @throws ProjectionException if the projection failed.
     */
    private double inverseTransform(final double y) throws ProjectionException {
        final double[] coordinate = new double[2];
        coordinate[1] = y;
        ((NormalizedProjection) transform).inverseTransform(coordinate, 0, coordinate, 0);
        final double φ = coordinate[1];
        if (!Double.isNaN(φ)) {
            final double λ = coordinate[0];
            assertEquals(0, λ, tolerance);
        }
        return φ;
    }

    /**
     * Verifies the consistency of spherical formulas with the elliptical formulas.
     *
     * @throws ProjectionException Should never happen.
     */
    @Test
    @DependsOnMethod("testSpecialLatitudes")
    public void testSphericalCase() throws ProjectionException {
        initialize(false); // Spherical case
        testSpecialLatitudes();

        // Make sure that 'testSpecialLatitudes' did not overwrite the 'transform' field.
        assertEquals("transform.class", Mercator.Spherical.class, transform.getClass());
    }

    /**
     * Tests the projection at some special latitudes (0, ±π/2, NaN).
     *
     * @throws ProjectionException Should never happen.
     */
    @Test
    public void testSpecialLatitudes() throws ProjectionException {
        if (transform == null) {    // May have been initialized by 'testSphericalCase'.
            initialize(true);       // Elliptical case
        }
        assertEquals ("Not a number",     NaN,                    transform(NaN),           tolerance);
        assertEquals ("Out of range",     NaN,                    transform(+2),            tolerance);
        assertEquals ("Out of range",     NaN,                    transform(-2),            tolerance);
        assertEquals ("Forward 0°N",      0,                      transform(0),             tolerance);
        assertEquals ("Forward 90°N",     POSITIVE_INFINITY,      transform(+PI/2),         tolerance);
        assertEquals ("Forward 90°S",     NEGATIVE_INFINITY,      transform(-PI/2),         tolerance);
        assertEquals ("Forward (90+ε)°N", POSITIVE_INFINITY,      transform(+nextUp(PI/2)), tolerance);
        assertEquals ("Forward (90+ε)°S", NEGATIVE_INFINITY,      transform(-nextUp(PI/2)), tolerance);
        assertBetween("Forward (90-ε)°N", +MIN_VALUE, +MAX_VALUE, transform(-nextUp(-PI/2)));
        assertBetween("Forward (90-ε)°S", -MAX_VALUE, -MIN_VALUE, transform(+nextUp(-PI/2)));

        assertEquals ("Not a number",     NaN,   inverseTransform(NaN),                tolerance);
        assertEquals ("Inverse 0 m",      0,     inverseTransform(0),                  tolerance);
        assertEquals ("Inverse +∞",       +PI/2, inverseTransform(POSITIVE_INFINITY),  tolerance);
        assertEquals ("Inverse +∞ appr.", +PI/2, inverseTransform(LN_INFINITY + 1),    tolerance);
        assertEquals ("Inverse -∞",       -PI/2, inverseTransform(NEGATIVE_INFINITY),  tolerance);
        assertEquals ("Inverse -∞ appr.", -PI/2, inverseTransform(-(LN_INFINITY + 1)), tolerance);
    }

    /**
     * Tests the <cite>"Mercator (variant B)"</cite> case.
     * This test is defined in GeoAPI conformance test suite.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     *
     * @see org.opengis.test.referencing.ParameterizedTransformTest#testMercator2SP()
     */
    @Test
    @DependsOnMethod("testSpecialLatitudes")
    public void testMercator2SP() throws FactoryException, TransformException {
        createGeoApiTest(new Mercator2SP()).testMercator2SP();
    }
}
