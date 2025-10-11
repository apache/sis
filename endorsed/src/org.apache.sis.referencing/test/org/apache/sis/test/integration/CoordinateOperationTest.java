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
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.CoordinateOperationAuthorityFactory;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.geometry.DirectPosition2D;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.referencing.operation.transform.MathTransformTestCase;
import static org.apache.sis.test.Assertions.assertEqualsIgnoreMetadata;
import static org.apache.sis.test.TestCase.assumeConnectionToEPSG;

// Specific to the main and geoapi-3.1 branches:
import org.apache.sis.referencing.operation.CoordinateOperationContext;


/**
 * Tests mixing use of EPSG dataset, change of axes convention, application of math transforms,
 * orthodromic distances, <i>etc</i>.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Olivier Lhemann (OSDU)
 * @author  Michael Arneson (OSDU)
 */
public final class CoordinateOperationTest extends MathTransformTestCase {
    /**
     * For avoiding ambiguity.
     */
    private static final CoordinateOperationContext CONTEXT = null;

    /**
     * The transformation factory to use for testing.
     */
    private final DefaultCoordinateOperationFactory opFactory;

    /**
     * Creates the test suite.
     */
    public CoordinateOperationTest() {
        opFactory = DefaultCoordinateOperationFactory.provider();
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
        final CoordinateOperation       operation = opFactory.createOperation(sourceCRS, targetCRS, CONTEXT);
        transform = operation.getMathTransform();
        final int dimension = transform.getSourceDimensions();
        assertEquals(3, dimension);
        assertEquals(3, transform.getTargetDimensions());
        assertSame(transform, transform.inverse().inverse());
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

        array0[12]=  0; array0[13]=-90;
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
            assertEquals(array2[i], array0[i], 0.1/3600); i++;
            assertEquals(array2[i], array0[i], 0.1/3600); i++;
            assertEquals(array2[i], array0[i], 0.01); i++;
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
                assertEquals(distance[i], cartesian, 0.1);
            }
        }
    }

    /**
     * Tests manual concatenation of transforms for MGI Ferro.
     * The transformation tested here involve an unusual case where the coordinate operation (EPSG:3966)
     * explicitly declares a <var>Longitude rotation</var> parameter value slightly different than the
     * <var>Greenwich longitude</var> value defined by the prime meridian, even if the target meridian
     * is Greenwich in both cases. Apache SIS follows exactly the steps described by the coordinate operation,
     * i.e. it applies a rotation of 17°39′46.02″W, even if it disagree with the Greenwich longitude value
     * declared in the prime meridian (which is 17°40′00″ W). The following table summarizes the values
     * computed by SIS depending on which longitude rotation is applied:
     *
     * <table class="sis">
     *   <caption>Transformation results for different longitude rotations</caption>
     *   <tr><th>Axis</th><th>Source coordinates</th><th>17°39′46.02″W rotation</th><th>17°40′00″W rotation</th></tr>
     *   <tr><td><var>X</var></td> <td>16°E</td>    <td>-25394.59</td> <td>-25097.74</td></tr>
     *   <tr><td><var>Y</var></td> <td>46.72°N</td> <td>175688.20</td> <td>175686.95</td></tr>
     * </table>
     *
     * Current implementation assumes that the column for 17°39′46.02″W longitude rotation
     * (as declared by EPSG:3966) is the correct one.
     *
     * @throws FactoryException if an error occurred while creating a test CRS.
     * @throws TransformException if an error occurred while testing a coordinate conversion.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-489">SIS-489 on issue tracker</a>
     */
    @Test
    public void testMGIFerro() throws FactoryException, TransformException {
        final double latitude  = 46.72;
        final double longitude = 16.00;
        final double expectedX = -25394.59;
        final double expectedY = 175688.20;

        final CRSAuthorityFactory crsFactory = CRS.getAuthorityFactory("EPSG");
        assumeConnectionToEPSG(crsFactory instanceof CoordinateOperationAuthorityFactory);
        var asOpFactory = (CoordinateOperationAuthorityFactory) crsFactory;

        // MGI (Ferro) to WGS 84 (1)
        CoordinateOperation datumOperation = asOpFactory.createCoordinateOperation("3966");

        // MGI (Ferro) / Austria GK East Zone
        CoordinateReferenceSystem targetCRS = crsFactory.createCoordinateReferenceSystem("31253");

        // Normalize the axis for the target
        targetCRS = AbstractCRS.castOrCopy(targetCRS).forConvention(AxesConvention.DISPLAY_ORIENTED);
        assertSame(datumOperation.getSourceCRS(), ((ProjectedCRS) targetCRS).getBaseCRS());

        CoordinateOperation targetOperation = CRS.findOperation(datumOperation.getSourceCRS(), targetCRS, null);
        assertEqualsIgnoreMetadata(targetOperation, ((ProjectedCRS) targetCRS).getConversionFromBase());
        /*
         * We have two operations to concatenate. The first operation is itself
         * a concatenation of a datum shift followed by a longitude rotation.
         *
         *   step1: "WGS 84" → "MGI 1901" → "MGI (Ferro)"
         *   step2: a Transverse Mercator projection.
         */
        MathTransform step1 = datumOperation.getMathTransform().inverse();
        MathTransform step2 = targetOperation.getMathTransform();
        MathTransform completeTransform = MathTransforms.concatenate(step1, step2);
        /*
         * Transform to x,y in one step.
         */
        DirectPosition source = new DirectPosition2D(latitude, longitude);
        DirectPosition target = completeTransform.transform(source, null);
        final double[] coordinate = target.getCoordinate();
        assertEquals(expectedX, coordinate[0], 0.01);
        assertEquals(expectedY, coordinate[1], 0.01);
    }

    /**
     * Tests a Mercator projection that require wraparound of longitude values.
     *
     * @throws FactoryException if an error occurred while creating a test CRS.
     * @throws TransformException if an error occurred while testing a coordinate conversion.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-547">SIS-547 on issue tracker</a>
     */
    @Test
    public void testMercatorWraparound() throws FactoryException, TransformException {
        final CRSAuthorityFactory crsFactory = CRS.getAuthorityFactory("EPSG");
        assumeConnectionToEPSG(crsFactory instanceof CoordinateOperationAuthorityFactory);

        CoordinateReferenceSystem sourceCRS = crsFactory.createCoordinateReferenceSystem("3001");
        CoordinateReferenceSystem targetCRS = crsFactory.createCoordinateReferenceSystem("4211");

        CoordinateOperation operation = opFactory.createOperation(sourceCRS, targetCRS, CONTEXT);
        MathTransform mt = operation.getMathTransform();

        double[] expectedXyValues = new double[] {-2.0, -71.0};
        double[] sourceXyValues   = new double[] {23764105.84, 679490.646};
        double[] actualXyValues   = new double[2];

        mt.transform(sourceXyValues, 0, actualXyValues, 0, 1);
        assertEquals(expectedXyValues[0], actualXyValues[0], 6E-7);
        assertEquals(expectedXyValues[1], actualXyValues[1], 6E-7);
    }
}
