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
package org.apache.sis.referencing.operation.transform;

import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static java.lang.StrictMath.toRadians;


/**
 * Tests {@link EllipsoidalToCartesianTransform}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn({
    CoordinateDomainTest.class,
    ContextualParametersTest.class
})
public final strictfp class EllipsoidalToCartesianTransformTest extends MathTransformTestCase {
    /**
     * The coordinate to transform, or {@code null} if not yet created.
     * Stored as a field for allowing test chaining.
     */
    private double[] coordinate;

    /**
     * Tests conversion of a single point from geographic to geocentric coordinates.
     * This test uses the example given in EPSG guidance note #7.
     * The point in WGS84 is 53°48'33.820"N, 02°07'46.380"E, 73.00 metres.
     *
     * @throws TransformException should never happen.
     */
    @Test
    public void testGeographicToGeocentric() throws TransformException {
        coordinate = new double[] {
             2 + ( 7 + 46.38/60)/60,    // Longitude
            53 + (48 + 33.82/60)/60,    // Latitude
            73.0                        // Height
        };
        transform = EllipsoidalToCartesianTransform.createGeodeticConversion(CommonCRS.WGS84.ellipsoid(), true);
        validate();
        final double delta = toRadians(100.0 / 60) / 1852;  // Approximatively 100 metres.
        derivativeDeltas = new double[] {delta, delta};
        tolerance = 0.0005;
        verifyTransform(coordinate, new double[] {
            3771793.968,
             140253.342,
            5124304.349
        });
    }
}
