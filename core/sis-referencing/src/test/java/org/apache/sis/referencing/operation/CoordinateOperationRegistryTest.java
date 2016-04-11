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
package org.apache.sis.referencing.operation;

import java.util.List;
import java.text.ParseException;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.referencing.operation.ConcatenatedOperation;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.CoordinateOperationAuthorityFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.CRS;
import org.apache.sis.io.wkt.WKTFormat;

// Test dependencies
import org.apache.sis.referencing.operation.transform.MathTransformTestCase;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

import static org.apache.sis.internal.referencing.Formulas.ANGULAR_TOLERANCE;
import static org.apache.sis.test.ReferencingAssert.*;
import static org.junit.Assume.*;


/**
 * Tests {@link CoordinateOperationRegistry}.
 * This class tests the following operations:
 *
 * <ul>
 *   <li><cite>"NTF (Paris) to WGS 84 (1)"</cite> operation (EPSG:8094), which implies a longitude rotation
 *       followed by a geocentric translation in the geographic domain.</li>
 * </ul>
 *
 * The operations are tested with various axis order and dimension in source and target CRS.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn({
    DefaultConversionTest.class,
    DefaultTransformationTest.class,
    DefaultPassThroughOperationTest.class,
    DefaultConcatenatedOperationTest.class
})
public final strictfp class CoordinateOperationRegistryTest extends MathTransformTestCase {
    /**
     * Tolerance threshold for strict comparisons of floating point numbers.
     * This constant can be used like below, where {@code expected} and {@code actual} are {@code double} values:
     *
     * {@preformat java
     *     assertEquals(expected, actual, STRICT);
     * }
     */
    private static final double STRICT = 0;

    /**
     * The transformation factory to use for testing.
     */
    private static DefaultCoordinateOperationFactory factory;

    /**
     * The parser to use for WKT strings used in this test.
     */
    private static WKTFormat parser;

    /**
     * The instance on which to execute the tests.
     */
    private final CoordinateOperationRegistry registry;

    /**
     * Creates a new test case.
     *
     * @throws FactoryException if an error occurred while creating the factory to be tested.
     */
    public CoordinateOperationRegistryTest() throws FactoryException {
        final CRSAuthorityFactory crsFactory = CRS.getAuthorityFactory("EPSG");
        assumeTrue("EPSG factory required.", crsFactory instanceof CoordinateOperationAuthorityFactory);
        registry = new CoordinateOperationRegistry(factory, (CoordinateOperationAuthorityFactory) crsFactory, null);
    }

    /**
     * Creates a new {@link DefaultCoordinateOperationFactory} to use for testing purpose.
     * The same factory will be used for all tests in this class.
     *
     * @throws ParseException if an error occurred while preparing the WKT parser.
     */
    @BeforeClass
    public static void createFactory() throws ParseException {
        factory = new DefaultCoordinateOperationFactory();
        parser  = new WKTFormat(null, null);
        parser.addFragment("NTF",
                "Datum[“Nouvelle Triangulation Française (Paris)”,\n" +
                "  Ellipsoid[“Clarke 1880 (IGN)”, 6378249.2, 293.4660212936269]]");
    }

    /**
     * Disposes the factory created by {@link #createFactory()} after all tests have been executed.
     */
    @AfterClass
    public static void disposeFactory() {
        factory = null;
        parser  = null;
    }

    /**
     * Returns the CRS for the given Well Known Text.
     */
    private static CoordinateReferenceSystem parse(final String wkt) throws ParseException {
        return (CoordinateReferenceSystem) parser.parseObject(wkt);
    }

    /**
     * Tests <cite>"NTF (Paris) to WGS 84 (1)"</cite> operation with source and target CRS conform to EPSG definitions.
     *
     * @throws ParseException if a CRS used in this test can not be parsed.
     * @throws FactoryException if the operation can not be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    public void testLongitudeRotationBetweenConformCRS() throws ParseException, FactoryException, TransformException {
        final CoordinateReferenceSystem sourceCRS = parse(
                "GeodeticCRS[“NTF (Paris)”,\n" +
                "  $NTF,\n" +
                "    PrimeMeridian[“Paris”, 2.5969213],\n" +
                "  CS[ellipsoidal, 2],\n" +
                "    Axis[“Latitude (φ)”, NORTH],\n" +
                "    Axis[“Longitude (λ)”, EAST],\n" +
                "    Unit[“grade”, 0.015707963267948967]]");
                // Intentionally omit Id[“EPSG”, 4807] for testing capability to find it back.

        final CoordinateReferenceSystem targetCRS = CommonCRS.WGS84.geographic();
        final CoordinateOperation operation = registry.createOperation(sourceCRS, targetCRS);
        assertEpsgNameAndIdentifierEqual("NTF (Paris) to WGS 84 (1)", 8094, operation);
        assertEpsgNameAndIdentifierEqual("NTF (Paris)", 4807, operation.getSourceCRS());
        assertEpsgNameAndIdentifierEqual("WGS 84",      4326, operation.getTargetCRS());
        verifyNTF(operation);
        /*
         * Same test point than the one used in FranceGeocentricInterpolationTest:
         *
         * NTF: 48°50′40.2441″N  2°25′32.4187″E
         * RGF: 48°50′39.9967″N  2°25′29.8273″E     (close to WGS84)
         */
        transform  = operation.getMathTransform();
        tolerance  = ANGULAR_TOLERANCE;
        λDimension = new int[] {1};
        verifyTransform(new double[] {54.271680278,  0.098269657},      // in grads east of Paris
                        new double[] {48.844443528,  2.424952028});     // in degrees east of Greenwich
        validate();
    }

    /**
     * Tests <cite>"NTF (Paris) to WGS 84 (1)"</cite> operation with normalized source and target CRS.
     * {@link CoordinateOperationRegistry} should be able to find the operation despite the difference
     * in axis order an units.
     *
     * @throws ParseException if a CRS used in this test can not be parsed.
     * @throws FactoryException if the operation can not be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    @DependsOnMethod("testLongitudeRotationBetweenConformCRS")
    public void testLongitudeRotationBetweenNormalizedCRS() throws ParseException, FactoryException, TransformException {
        final CoordinateReferenceSystem sourceCRS = parse(
                "GeodeticCRS[“NTF (Paris)”,\n" +
                "  $NTF,\n" +
                "    PrimeMeridian[“Paris”, 2.33722917],\n" +
                "  CS[ellipsoidal, 2],\n" +
                "    Axis[“Longitude (λ)”, EAST],\n" +
                "    Axis[“Latitude (φ)”, NORTH],\n" +
                "    Unit[“degree”, 0.017453292519943295]]");

        final CoordinateReferenceSystem targetCRS = CommonCRS.WGS84.normalizedGeographic();
        final CoordinateOperation operation = registry.createOperation(sourceCRS, targetCRS);
        verifyNTF(operation);

        transform  = operation.getMathTransform();
        tolerance  = ANGULAR_TOLERANCE;
        λDimension = new int[] {1};
        verifyTransform(new double[] {0.088442691, 48.844512250},      // in degrees east of Paris
                        new double[] {2.424952028, 48.844443528});     // in degrees east of Greenwich
        validate();
    }

    /**
     * Verifies a coordinate operation which is expected to be <cite>"NTF (Paris) to WGS 84 (1)"</cite> (EPSG:8094).
     */
    static void verifyNTF(final CoordinateOperation operation) {
        assertEquals("name",           "NTF (Paris) to WGS 84 (1)",  operation.getName().getCode());
        assertEquals("sourceCRS.name", "NTF (Paris)",                operation.getSourceCRS().getName().getCode());
        assertEquals("targetCRS.name", "WGS 84",                     operation.getTargetCRS().getName().getCode());
        assertEquals("Should report only the coarsest accuracy.", 1, operation.getCoordinateOperationAccuracy().size());
        assertEquals("linearAccuracy",                            2, CRS.getLinearAccuracy(operation), STRICT);

        assertInstanceOf("Operation should have two steps.", ConcatenatedOperation.class, operation);
        final List<? extends CoordinateOperation> steps = ((ConcatenatedOperation) operation).getOperations();
        assertEquals("Operation should have two steps.", 2, steps.size());

        final SingleOperation step1 = (SingleOperation) steps.get(0);
        final SingleOperation step2 = (SingleOperation) steps.get(1);
        assertSame("SourceCRS shall be the targetCRS of previous step.",    step1.getTargetCRS(), step2.getSourceCRS());
        assertEquals("Step 1",   "NTF (Paris) to NTF (1)",                  step1.getName().getCode());
        assertEquals("Step 2",   "NTF to WGS 84 (1)",                       step2.getName().getCode());
        assertEquals("Method 1", "Longitude rotation",                      step1.getMethod().getName().getCode());
        assertEquals("Method 2", "Geocentric translations (geog2D domain)", step2.getMethod().getName().getCode());

        final ParameterValueGroup p1 = step1.getParameterValues();
        final ParameterValueGroup p2 = step2.getParameterValues();
        assertEquals("Longitude offset", 2.5969213, p1.parameter("Longitude offset")  .doubleValue(), STRICT);
        assertEquals("X-axis translation",    -168, p2.parameter("X-axis translation").doubleValue(), STRICT);
        assertEquals("Y-axis translation",     -60, p2.parameter("Y-axis translation").doubleValue(), STRICT);
        assertEquals("Z-axis translation",     320, p2.parameter("Z-axis translation").doubleValue(), STRICT);
    }
}
