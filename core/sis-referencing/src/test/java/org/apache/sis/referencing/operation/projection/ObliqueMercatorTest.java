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
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.referencing.provider.ObliqueMercatorCenter;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.test.DependsOn;
import org.junit.*;

import static java.lang.StrictMath.*;


/**
 * Tests the {@link ObliqueMercator} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
@DependsOn(MercatorTest.class)
public final strictfp class ObliqueMercatorTest extends MapProjectionTestCase {
    /**
     * Returns a new instance of {@link ObliqueMercator}.
     *
     * @param  cx       the longitude of projection center.
     * @param  cy       the latitude of projection center.
     * @param  azimuth  the azimuth.
     * @return newly created projection.
     */
    private static ObliqueMercator create(final double cx, final double cy, final double azimuth) {
        final ObliqueMercatorCenter method = new ObliqueMercatorCenter();
        final ParameterValueGroup values = method.getParameters().createValue();
        final Ellipsoid ellipsoid = CommonCRS.WGS84.ellipsoid();
        values.parameter("semi_major")         .setValue(ellipsoid.getSemiMajorAxis());
        values.parameter("semi_minor")         .setValue(ellipsoid.getSemiMinorAxis());
        values.parameter("azimuth")            .setValue(azimuth);
        values.parameter("rectified_grid_angle").setValue(azimuth);
        values.parameter("longitude_of_center").setValue(cx);
        values.parameter("latitude_of_center") .setValue(cy);
        return new ObliqueMercator(method, Parameters.castOrWrap(values));
    }

    // TODO: supprimer rectified_grid_angle. Nécessite de modifier Parameters.getValue(),
    // ce qui peut rendre inutile Molodensky.optional(…).

    /**
     * Creates a projection and derivates a few points.
     *
     * @throws TransformException if an error occurred while converting a point.
     */
    @Test
    public void testEllipsoidalDerivative() throws TransformException {
        tolerance = 1E-9;
        transform = create(0, 0, 0);
        validate();

        final double delta = toRadians(100.0 / 60) / 1852;      // Approximatively 100 metres.
        derivativeDeltas = new double[] {delta, delta};
        verifyDerivative(toRadians( 0), toRadians( 0));
        verifyDerivative(toRadians(15), toRadians(30));
        verifyDerivative(toRadians(15), toRadians(40));
        verifyDerivative(toRadians(10), toRadians(60));
    }

    /**
     * Tests with an azimuth of 90°.
     *
     * @throws TransformException if an error occurred while converting a point.
     */
    @Test
    public void testAzimuth90() throws TransformException {
        tolerance = 1E-9;
        transform = create(10, 20, 90);
        validate();

        final double delta = toRadians(100.0 / 60) / 1852;      // Approximatively 100 metres.
        derivativeDeltas = new double[] {delta, delta};
        verifyInverse(0.15, 0.2);
    }

    /**
     * Tests the <cite>"Hotine Oblique Mercator (variant B)"</cite> (EPSG:9815) projection method.
     * This test is defined in GeoAPI conformance test suite.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     *
     * @see org.opengis.test.referencing.ParameterizedTransformTest#testHotineObliqueMercator()
     */
    @Test
    public void runGeoapiTest() throws FactoryException, TransformException {
        createGeoApiTest(new ObliqueMercatorCenter()).testHotineObliqueMercator();
    }
}
