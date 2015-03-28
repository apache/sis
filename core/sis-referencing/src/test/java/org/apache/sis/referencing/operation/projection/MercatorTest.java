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

import org.apache.sis.parameter.Parameters;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.referencing.provider.Mercator2SP;
import org.apache.sis.referencing.operation.transform.MathTransformTestCase;
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
public final strictfp class MercatorTest extends MathTransformTestCase {
    /**
     * Returns a new instance of {@link Mercator} for an ellipsoid.
     */
    private void initialize() {
        final Mercator2SP method = new Mercator2SP();
        final Parameters parameters = Parameters.castOrWrap(method.getParameters().createValue());
        parameters.parameter(Constants.SEMI_MAJOR).setValue(6378137);
        parameters.parameter(Constants.SEMI_MINOR).setValue(6356752);
        transform = new Mercator(method, parameters);
        tolerance = 1E-12;
    }

    /**
     * Replaces the ellipsoidal formulas by the spherical ones.
     */
    private void switchToSpherical() {
        assertEquals(Mercator.class, transform.getClass());
        transform = new Mercator.Spherical((Mercator) transform);
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
     * Tests the projection at a few extreme points.
     *
     * @throws ProjectionException Should never happen.
     */
    @Test
    public void testExtremes() throws ProjectionException {
        initialize();
        doTestExtremes();
//      switchToSpherical();
//      doTestExtremes();
    }

    /**
     * Implementation of {@link #testExtremes()}, to be executed twice.
     */
    private void doTestExtremes() throws ProjectionException {
        validate();

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
}
