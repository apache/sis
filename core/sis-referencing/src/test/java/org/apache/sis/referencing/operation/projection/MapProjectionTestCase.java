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

import javax.measure.unit.NonSI;
import org.opengis.util.FactoryException;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.test.referencing.ParameterizedTransformTest;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.internal.referencing.provider.MapProjection;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.referencing.operation.transform.MathTransformTestCase;
import org.apache.sis.test.mock.MathTransformFactoryMock;
import org.apache.sis.test.mock.GeodeticDatumMock;

import static java.lang.StrictMath.*;
import static org.junit.Assert.*;


/**
 * Base class of map projection tests.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
strictfp class MapProjectionTestCase extends MathTransformTestCase {
    /**
     * Tolerance level for comparing formulas on the unitary sphere or ellipsoid.
     */
    static final double NORMALIZED_TOLERANCE = 1E-12;

    /**
     * Creates a new test case.
     */
    MapProjectionTestCase() {
    }

    /**
     * Returns the parameters to use for instantiating the projection to test.
     *
     * @param  provider The provider of the projection to test.
     * @param  ellipse {@code false} for a sphere, or {@code true} for WGS84 ellipsoid.
     * @return The parameters to use for instantiating the projection.
     */
    static Parameters parameters(final DefaultOperationMethod provider, final boolean ellipse) {
        final Parameters parameters = Parameters.castOrWrap(provider.getParameters().createValue());
        final Ellipsoid ellipsoid = (ellipse ? GeodeticDatumMock.WGS84 : GeodeticDatumMock.SPHERE).getEllipsoid();
        parameters.parameter(Constants.SEMI_MAJOR).setValue(ellipsoid.getSemiMajorAxis());
        parameters.parameter(Constants.SEMI_MINOR).setValue(ellipsoid.getSemiMinorAxis());
        return parameters;
    }

    /**
     * Instantiates the object to use for running GeoAPI test.
     *
     * @param  provider The provider of the projection to test.
     * @return The GeoAPI test class using the given provider.
     */
    static ParameterizedTransformTest createGeoApiTest(final MapProjection provider) {
        return new ParameterizedTransformTest(new MathTransformFactoryMock(provider));
    }

    /**
     * Initializes a complete projection (including conversion from degrees to radians) for the given provider.
     * This method uses arbitrary central meridian, scale factor, false easting and false northing for increasing
     * the chances to detect a mismatch.
     */
    final void initialize(final DefaultOperationMethod provider, final boolean ellipse,
            final boolean hasLatitudeOfOrigin,
            final boolean hasStandardParallel,
            final boolean hasScaleFactor)
            throws FactoryException
    {
        final Parameters parameters = parameters(provider, ellipse);
        parameters.parameter(Constants.CENTRAL_MERIDIAN).setValue(0.5, NonSI.DEGREE_ANGLE);
        parameters.parameter(Constants.FALSE_EASTING)   .setValue(200);
        parameters.parameter(Constants.FALSE_NORTHING)  .setValue(100);
        if (hasLatitudeOfOrigin) {
            parameters.parameter("latitude_of_origin").setValue(40);
        }
        if (hasStandardParallel) {
            parameters.parameter(Constants.STANDARD_PARALLEL_1).setValue(20);
        }
        if (hasScaleFactor) {
            parameters.parameter(Constants.SCALE_FACTOR).setValue(0.997);
        }
        transform = new MathTransformFactoryMock(provider).createParameterizedTransform(parameters);
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
    final double transform(final double φ) throws ProjectionException {
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
     * Inverse projects the given northing value. The easting is fixed to zero.
     * This method is useful for testing the behavior close to poles in a simple case.
     *
     * @param  y The northing.
     * @return The latitude.
     * @throws ProjectionException if the projection failed.
     */
    final double inverseTransform(final double y) throws ProjectionException {
        final double[] coordinate = new double[2];
        coordinate[1] = y;
        ((NormalizedProjection) transform).inverseTransform(coordinate, 0, coordinate, 0);
        final double φ = coordinate[1];
        if (!Double.isNaN(φ)) {
            /*
             * Opportunistically verify that the longitude is still zero. However the longitude value is meaningless
             * at poles. We can not always use coordinate[0] for testing if we are at a pole because its calculation
             * is not finished (the denormalization matrix has not yet been applied).  In the particular case of SIS
             * implementation, we observe sometime a ±180° rotation, which we ignore below. Such empirical hack is
             * not rigorous, but it is not the purpose of this test to check the longitude value - we are doing only
             * an opportunist check, other test methods will test longitude more accurately.
             */
            double λ = coordinate[0];
            λ -= rint(λ / PI) * PI;
            assertEquals(0, λ, tolerance);
        }
        return φ;
    }
}
