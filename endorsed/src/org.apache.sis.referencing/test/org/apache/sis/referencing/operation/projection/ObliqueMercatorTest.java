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


/**
 * Tests the {@link ObliqueMercator} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
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
}
