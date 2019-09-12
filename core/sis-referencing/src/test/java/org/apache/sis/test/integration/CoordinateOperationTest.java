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
package org.apache.sis.test.integration;

import java.util.Random;
import org.opengis.util.FactoryException;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.operation.transform.MathTransformTestCase;
import org.apache.sis.internal.referencing.CoordinateOperations;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests mixing use of EPSG dataset, change of axes convention, application of math transforms,
 * orthodromic distances, <i>etc</i>.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.1
 * @since   0.8
 * @module
 */
@DependsOn({
    org.apache.sis.referencing.CRSTest.class,
    org.apache.sis.referencing.CommonCRSTest.class,
    org.apache.sis.referencing.datum.DefaultEllipsoidTest.class,
    org.apache.sis.referencing.operation.DefaultCoordinateOperationFactoryTest.class,
    org.apache.sis.referencing.operation.transform.EllipsoidToCentricTransformTest.class
})
public final strictfp class CoordinateOperationTest extends MathTransformTestCase {
    /**
     * The transformation factory to use for testing.
     */
    private final CoordinateOperationFactory opFactory;

    /**
     * Creates the test suite.
     */
    public CoordinateOperationTest() {
        opFactory = CoordinateOperations.factory();
    }

    /**
     * Tests a "geographic to geocentric" conversion.
     *
     * @throws FactoryException if an error occurred while creating a test CRS.
     * @throws TransformException if an error occurred while testing a coordinate conversion.
     */
    @Test
    public void testGeocentricTransform() throws FactoryException, TransformException {
        final Random random = new Random(661597560);
        /*
         * Gets the math transform from WGS84 to a geocentric transform.
         */
        final Ellipsoid                 ellipsoid = CommonCRS.WGS84.ellipsoid();
        final CoordinateReferenceSystem sourceCRS = AbstractCRS.castOrCopy(CommonCRS.WGS84.geographic3D()).forConvention(AxesConvention.RIGHT_HANDED);
        final CoordinateReferenceSystem targetCRS = CommonCRS.WGS84.geocentric();
        final CoordinateOperation       operation = opFactory.createOperation(sourceCRS, targetCRS);
        transform = operation.getMathTransform();
        final int dimension = transform.getSourceDimensions();
        assertEquals("Source dimension", 3, dimension);
        assertEquals("Target dimension", 3, transform.getTargetDimensions());
        assertSame("Inverse transform", transform, transform.inverse().inverse());
        validate();
        /*
         * Constructs an array of random points. The first 8 points
         * are initialized to know values. Other points are random.
         */
        final double distance[] = new double[4];
        final double[] array0 = new double[900];                // Must be divisible by 3.

        array0[0]=35.0; array0[1]=24.0; array0[2]=8000;         // 24°N 35°E 8km
        array0[3]=34.8; array0[4]=24.7; array0[5]=5000;         // … about 80 km away
        distance[0] = 80284.00;

        // array0[6,7,8,10,11] = 0
        array0[9]=180;                                          // Antipodes; distance should be 2*6378.137 km
        distance[1] = ellipsoid.getSemiMajorAxis() * 2;

                       array0[13]=-90;
        array0[15]=180; array0[16]=+90;                         // Antipodes; distance should be 2*6356.752 km
        distance[2] = ellipsoid.getSemiMinorAxis() * 2;

        array0[18]= 95; array0[19]=-38;
        array0[21]=-85; array0[22]=+38;                         // Antipodes
        distance[3] = 12740147.19;

        for (int i=24; i<array0.length; i++) {
            final int range;
            switch (i % 3) {
                case 0:  range =   360; break;                      // Longitude
                case 1:  range =   180; break;                      // Latitidue
                case 2:  range = 10000; break;                      // Altitude
                default: throw new AssertionError(i);
            }
            array0[i] = random.nextDouble() * range - (range/2);
        }
        /*
         * Transforms all points, and then inverse transform them. The resulting
         * array2 should be equal to array0 except for rounding errors. We tolerate
         * maximal error of 0.1 second in longitude or latitude and 1 cm in height.
         */
        final double[] array1 = new double[array0.length];
        final double[] array2 = new double[array0.length];
        transform          .transform(array0, 0, array1, 0, array0.length / dimension);
        transform.inverse().transform(array1, 0, array2, 0, array1.length / dimension);
        for (int i=0; i<array0.length;) {
            assertEquals("Longitude", array2[i], array0[i], 0.1/3600); i++;
            assertEquals("Latitude",  array2[i], array0[i], 0.1/3600); i++;
            assertEquals("Height",    array2[i], array0[i], 0.01); i++;
        }
        /*
         * Compares the distances between "special" points with expected distances.
         * We require a precision of 10 centimetres.
         */
        for (int i=0; i < array0.length / 6; i++) {
            final int base = i*6;
            final double cartesian = MathFunctions.magnitude(
                    array1[base+0] - array1[base+3],
                    array1[base+1] - array1[base+4],
                    array1[base+2] - array1[base+5]);
            if (i < distance.length) {
                assertEquals("Cartesian distance", distance[i], cartesian, 0.1);
            }
        }
    }
}
