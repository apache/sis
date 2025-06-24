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

import static java.lang.StrictMath.*;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.provider.ObliqueMercatorCenter;
import org.apache.sis.parameter.Parameters;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.apache.sis.referencing.datum.HardCodedDatum;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.util.FactoryException;
import org.opengis.test.ToleranceModifier;


/**
 * Tests the {@link ObliqueMercator} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @author  Emmanuel Giasson (Thales)
 */
public final class ObliqueMercatorTest extends MapProjectionTestCase {
    /**
     * Creates a new test case.
     */
    public ObliqueMercatorTest() {
    }

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
        final Ellipsoid ellipsoid = HardCodedDatum.WGS84.getEllipsoid();
        values.parameter("semi_major")         .setValue(ellipsoid.getSemiMajorAxis());
        values.parameter("semi_minor")         .setValue(ellipsoid.getSemiMinorAxis());
        values.parameter("azimuth")            .setValue(azimuth);
        values.parameter("longitude_of_center").setValue(cx);
        values.parameter("latitude_of_center") .setValue(cy);
        return new ObliqueMercator(method, Parameters.castOrWrap(values));
    }

    /**
     * Creates a projection and derivates a few points.
     *
     * @throws TransformException if an error occurred while converting a point.
     */
    @Test
    public void testEllipsoidalDerivative() throws TransformException {
        tolerance = 1E-9;
        transform = create(5, 10, 20);
        validate();

        final double delta = toRadians(100.0 / 60) / 1852;      // Approximately 100 metres.
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

        final double delta = toRadians(100.0 / 60) / 1852;      // Approximately 100 metres.
        derivativeDeltas = new double[] {delta, delta};
        verifyInverse(toRadians(15), toRadians(25));
    }

    /**
     * Tests with a latitude close to 90°.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while converting a point.
     *
     * <a href="https://issues.apache.org/jira/browse/SIS-532">SIS-532</a>
     */
    @Test
    public void testPole() throws TransformException, FactoryException {
        tolerance = 0.01;
        transform = create(179.8, 89.8, -174).createMapProjection(context(null, null));
        transform = transform.inverse();
        validate();
        /*
         * The projection of (180, 90) with SIS 1.1 is (+0.004715980030596256, 22338.795490272343).
         * Empirical cordinates shifted by 0.01 meter: (-0.005463426921067797, 22338.792057282844).
         * With those shifted coordinated, Apache SIS 1.1 was used to compute φ = NaN because the
         * U′ value in `ObliqueMercator.inverseTransform(…)` was slightly greater than 1.
         */
        isInverseTransformSupported = false;
        toleranceModifier = ToleranceModifier.GEOGRAPHIC;
        verifyTransform(new double[] {-0.005464, 22338.792057},
                        new double[] {300, 90});
    }

    /**
     * Tests the <q>Hotine Oblique Mercator (variant B)</q> (EPSG:9815) projection method.
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
