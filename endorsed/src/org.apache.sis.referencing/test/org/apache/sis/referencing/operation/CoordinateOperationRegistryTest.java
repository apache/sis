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
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.referencing.operation.ConcatenatedOperation;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.CoordinateOperationAuthorityFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.internal.shared.Formulas;
import org.apache.sis.referencing.crs.DefaultGeographicCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.io.wkt.WKTFormat;

// Test dependencies
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.apache.sis.referencing.operation.transform.MathTransformTestCase;
import static org.apache.sis.referencing.Assertions.assertEpsgNameAndIdentifierEqual;
import static org.apache.sis.test.TestCase.assumeConnectionToEPSG;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.Identifier;


/**
 * Tests {@link CoordinateOperationRegistry} using the EPSG geodetic dataset.
 * If no EPSG geodetic dataset is available in the running environment, then tests are skipped.
 * For tests without the need of an EPSG database, see {@link CoordinateOperationFinderTest}.
 *
 * <p>This class tests the following operations:</p>
 * <ul>
 *   <li><q>NTF (Paris) to WGS 84 (1)</q> operation (EPSG:8094), which implies a longitude rotation
 *       followed by a geocentric translation in the geographic domain.</li>
 *   <li><q>Martinique 1938 to RGAF09 (1)</q> operation (EPSG:5491), which implies a datum shift
 *       that does not go through WGS84. Furthermore, since the EPSG database defines (λ,φ) axis order in
 *       addition to the usual (φ,λ) order for the target CRS, this tests allows us to verify we can find
 *       this operation despite different axis order.</li>
 * </ul>
 *
 * The operations are tested with various axis order and dimension in source and target CRS.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
@Execution(ExecutionMode.SAME_THREAD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class CoordinateOperationRegistryTest extends MathTransformTestCase {
    /**
     * The transformation factory to use for testing.
     */
    private final DefaultCoordinateOperationFactory factory;

    /**
     * The parser to use for WKT strings used in this test.
     */
    private final WKTFormat parser;

    /**
     * The EPSG authority factory for CRS objects. Can be used as an alternative to {@link #parser}.
     */
    private final CRSAuthorityFactory crsFactory;

    /**
     * The instance on which to execute the tests.
     */
    private final CoordinateOperationRegistry registry;

    /**
     * Creates a new {@link DefaultCoordinateOperationFactory} to use for testing purpose.
     * The same factory will be used for all tests in this class.
     *
     * @throws ParseException if an error occurred while preparing the WKT parser.
     * @throws FactoryException if an error occurred while creating the factory to be tested.
     */
    public CoordinateOperationRegistryTest() throws ParseException, FactoryException {
        crsFactory = CRS.getAuthorityFactory("EPSG");
        assumeConnectionToEPSG(crsFactory instanceof CoordinateOperationAuthorityFactory);
        factory = new DefaultCoordinateOperationFactory();
        parser  = new WKTFormat();
        parser.addFragment("NTF",
                "Datum[“Nouvelle Triangulation Française (Paris)”,\n" +
                "  Ellipsoid[“Clarke 1880 (IGN)”, 6378249.2, 293.4660212936269]]");
        registry = new CoordinateOperationRegistry((CoordinateOperationAuthorityFactory) crsFactory, factory, null);
    }

    /**
     * Resets all fields that may be modified by test methods in this class.
     * This is needed because we reuse the same instance for all methods,
     * in order to reuse the factory and parser created in the constructor.
     */
    @Override
    @BeforeEach
    public void reset() {
        super.reset();
    }

    /**
     * Returns the CRS for the given Well Known Text.
     */
    private CoordinateReferenceSystem parse(final String wkt) throws ParseException {
        return assertInstanceOf(CoordinateReferenceSystem.class, parser.parseObject(wkt));
    }

    /**
     * Gets exactly one coordinate operation from the registry to test.
     */
    private CoordinateOperation createOperation(final CoordinateReferenceSystem sourceCRS,
                                                final CoordinateReferenceSystem targetCRS) throws FactoryException
    {
        registry.stopAtFirst = true;
        final List<CoordinateOperation> operations = registry.createOperations(sourceCRS, targetCRS);
        assertEquals(1, operations.size(), "Invalid number of operations.");
        return operations.get(0);
    }

    /**
     * Tests <q>NTF (Paris) to WGS 84 (1)</q> operation with source and target CRS conform to EPSG definitions.
     *
     * @throws ParseException if a CRS used in this test cannot be parsed.
     * @throws FactoryException if the operation cannot be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    public void testLongitudeRotationBetweenConformCRS() throws ParseException, FactoryException, TransformException {
        final CoordinateReferenceSystem sourceCRS = parse(
                "GeographicCRS[“NTF (Paris)”,\n" +
                "  $NTF,\n" +
                "    PrimeMeridian[“Paris”, 2.5969213],\n" +
                "  CS[ellipsoidal, 2],\n" +
                "    Axis[“Latitude (φ)”, NORTH],\n" +
                "    Axis[“Longitude (λ)”, EAST],\n" +
                "    Unit[“grad”, 0.015707963267948967]]");
                // Intentionally omit Id[“EPSG”, 4807] for testing capability to find it back.

        final CoordinateReferenceSystem targetCRS = CommonCRS.WGS84.geographic();
        final CoordinateOperation operation = createOperation(sourceCRS, targetCRS);
        verifyNTF(operation, "geog2D domain", true);
        /*
         * Same test point as the one used in FranceGeocentricInterpolationTest:
         *
         * NTF: 48°50′40.2441″N  2°25′32.4187″E
         * RGF: 48°50′39.9967″N  2°25′29.8273″E     (close to WGS84)
         */
        transform  = operation.getMathTransform();
        tolerance  = Formulas.ANGULAR_TOLERANCE;
        λDimension = new int[] {1};
        verifyTransform(new double[] {54.271680278,  0.098269657},      // in grads east of Paris
                        new double[] {48.844443528,  2.424952028});     // in degrees east of Greenwich
        validate();
    }

    /**
     * Tests <q>NTF (Paris) to WGS 84 (1)</q> operation with normalized source and target CRS.
     * {@link CoordinateOperationRegistry} should be able to find the operation despite the difference
     * in axis order an units.
     *
     * @throws ParseException if a CRS used in this test cannot be parsed.
     * @throws FactoryException if the operation cannot be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    public void testLongitudeRotationBetweenNormalizedCRS() throws ParseException, FactoryException, TransformException {
        final CoordinateReferenceSystem sourceCRS = parse(
                "GeographicCRS[“NTF (Paris)”,\n" +
                "  $NTF,\n" +
                "    PrimeMeridian[“Paris”, 2.33722917],\n" +
                "  CS[ellipsoidal, 2],\n" +
                "    Axis[“Longitude (λ)”, EAST],\n" +
                "    Axis[“Latitude (φ)”, NORTH],\n" +
                "    Unit[“degree”, 0.017453292519943295]]");

        final CoordinateReferenceSystem targetCRS = CommonCRS.WGS84.normalizedGeographic();
        final CoordinateOperation operation = createOperation(sourceCRS, targetCRS);
        verifyNTF(operation, "geog2D domain", false);

        transform  = operation.getMathTransform();
        tolerance  = Formulas.ANGULAR_TOLERANCE;
        λDimension = new int[] {1};
        verifyTransform(new double[] {0.088442691, 48.844512250},      // in degrees east of Paris
                        new double[] {2.424952028, 48.844443528});     // in degrees east of Greenwich
        validate();
    }

    /**
     * Tests the inverse of <q>NTF (Paris) to WGS 84 (1)</q> operation, also with different axis order.
     *
     * @throws ParseException if a CRS used in this test cannot be parsed.
     * @throws FactoryException if the operation cannot be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    public void testInverse() throws ParseException, FactoryException, TransformException {
        final CoordinateReferenceSystem targetCRS = parse(
                "GeographicCRS[“NTF (Paris)”,\n" +
                "  $NTF,\n" +
                "    PrimeMeridian[“Paris”, 2.5969213],\n" +
                "  CS[ellipsoidal, 2],\n" +
                "    Axis[“Longitude (λ)”, EAST],\n" +
                "    Axis[“Latitude (φ)”, NORTH],\n" +
                "    Unit[“grad”, 0.015707963267948967]]");

        final CoordinateReferenceSystem sourceCRS = CommonCRS.WGS84.normalizedGeographic();
        final CoordinateOperation operation = createOperation(sourceCRS, targetCRS);

        transform  = operation.getMathTransform();
        tolerance  = Formulas.ANGULAR_TOLERANCE;
        λDimension = new int[] {1};
        verifyTransform(new double[] {2.424952028, 48.844443528},      // in degrees east of Greenwich
                        new double[] {0.098269657, 54.271680278});     // in grads east of Paris
        validate();
    }

    /**
     * Tests <q>NTF (Paris) to WGS 84 (1)</q> operation with three-dimensional source and target CRS.
     * {@link CoordinateOperationRegistry} should be able to find the operation despite the difference
     * in the number of dimensions.
     *
     * @throws ParseException if a CRS used in this test cannot be parsed.
     * @throws FactoryException if the operation cannot be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    public void testLongitudeRotationBetweenGeographic3D() throws ParseException, FactoryException, TransformException {
        final CoordinateReferenceSystem sourceCRS = parse(
                "GeographicCRS[“NTF (Paris)”,\n" +
                "  $NTF,\n" +
                "    PrimeMeridian[“Paris”, 2.5969213],\n" +
                "  CS[ellipsoidal, 3],\n" +
                "    Axis[“Latitude (φ)”, NORTH, Unit[“grad”, 0.015707963267948967]],\n" +
                "    Axis[“Longitude (λ)”, EAST, Unit[“grad”, 0.015707963267948967]],\n" +
                "    Axis[“Height (h)”, UP, Unit[“m”, 1]]]");

        final CoordinateReferenceSystem targetCRS = CommonCRS.WGS84.geographic3D();
        final CoordinateOperation operation = createOperation(sourceCRS, targetCRS);
        verifyNTF(operation, "geog3D domain", false);

        transform  = operation.getMathTransform();
        tolerance  = Formulas.ANGULAR_TOLERANCE;
        zTolerance = Formulas.LINEAR_TOLERANCE;
        zDimension = new int[] {2};
        λDimension = new int[] {1};
        verifyTransform(new double[] {54.271680278,  0.098269657, 20.00},      // in grads east of Paris
                        new double[] {48.844443528,  2.424952028, 63.15});     // in degrees east of Greenwich
        validate();
    }

    /**
     * Tests <q>NTF (Paris) to WGS 84 (1)</q> operation with three-dimensional source and target CRS
     * having different axis order and units than the ones declared in the EPSG dataset.
     *
     * @throws ParseException if a CRS used in this test cannot be parsed.
     * @throws FactoryException if the operation cannot be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    public void testLongitudeRotationBetweenNormalizedGeographic3D() throws ParseException, FactoryException, TransformException {
        final CoordinateReferenceSystem sourceCRS = parse(
                "GeographicCRS[“NTF (Paris)”,\n" +
                "  $NTF,\n" +
                "    PrimeMeridian[“Paris”, 2.33722917],\n" +
                "  CS[ellipsoidal, 3],\n" +
                "    Axis[“Longitude (λ)”, EAST, Unit[“degree”, 0.017453292519943295]],\n" +
                "    Axis[“Latitude (φ)”, NORTH, Unit[“degree”, 0.017453292519943295]],\n" +
                "    Axis[“Height (h)”, UP, Unit[“m”, 1]]]");

        final CoordinateReferenceSystem targetCRS =
                DefaultGeographicCRS.castOrCopy(CommonCRS.WGS84.geographic3D()).forConvention(AxesConvention.NORMALIZED);
        final CoordinateOperation operation = createOperation(sourceCRS, targetCRS);
        verifyNTF(operation, "geog3D domain", false);

        transform  = operation.getMathTransform();
        tolerance  = Formulas.ANGULAR_TOLERANCE;
        zTolerance = Formulas.LINEAR_TOLERANCE;
        zDimension = new int[] {2};
        λDimension = new int[] {1};
        verifyTransform(new double[] {0.088442691, 48.844512250, 20.00},      // in degrees east of Paris
                        new double[] {2.424952028, 48.844443528, 63.15});     // in degrees east of Greenwich
        validate();
    }

    /**
     * Verifies a coordinate operation which is expected to be <q>NTF (Paris) to WGS 84 (1)</q> (EPSG:8094).
     *
     * @param  domain  either {@code "geog2D domain"} or either {@code "geog3D domain"}.
     * @param  isEPSG  {@code true} if the coordinate operation is expected to contain EPSG identifiers.
     */
    private static void verifyNTF(final CoordinateOperation operation, final String domain, final boolean isEPSG) {
        assertInstanceOf(ConcatenatedOperation.class, operation, "Operation should have two steps.");
        final List<? extends CoordinateOperation> steps = ((ConcatenatedOperation) operation).getOperations();
        assertEquals(2, steps.size(), "Operation should have two steps.");
        final SingleOperation step1 = (SingleOperation) steps.get(0);
        final SingleOperation step2 = (SingleOperation) steps.get(1);
        if (isEPSG) {
            assertEpsgNameAndIdentifierEqual("NTF (Paris) to WGS 84 (1)", 8094, operation);
            assertEpsgNameAndIdentifierEqual("NTF (Paris)",               4807, operation.getSourceCRS());
            assertEpsgNameAndIdentifierEqual("WGS 84",                    4326, operation.getTargetCRS());
            assertEpsgNameAndIdentifierEqual("NTF (Paris) to NTF (1)",    1763, step1);
            assertEpsgNameAndIdentifierEqual("NTF to WGS 84 (1)",         1193, step2);
        } else {
            assertEpsgNameWithoutIdentifierEqual("NTF (Paris) to WGS 84 (1)", operation);
            assertEpsgNameWithoutIdentifierEqual("NTF (Paris)", operation.getSourceCRS());
            assertEquals("WGS 84", operation.getTargetCRS().getName().getCode());
            assertEpsgNameWithoutIdentifierEqual("NTF (Paris) to NTF (1)", step1);
            assertEpsgNameWithoutIdentifierEqual("NTF to WGS 84 (1)",      step2);
        }
        assertSame(step1.getTargetCRS(), step2.getSourceCRS(), "SourceCRS shall be the targetCRS of previous step.");
        assertEquals("Longitude rotation",                       step1.getMethod().getName().getCode());
        assertEquals("Geocentric translations (" + domain + ')', step2.getMethod().getName().getCode());

        final ParameterValueGroup p1 = step1.getParameterValues();
        final ParameterValueGroup p2 = step2.getParameterValues();
        assertEquals(2.5969213, p1.parameter("Longitude offset")  .doubleValue());
        assertEquals(     -168, p2.parameter("X-axis translation").doubleValue());
        assertEquals(      -60, p2.parameter("Y-axis translation").doubleValue());
        assertEquals(      320, p2.parameter("Z-axis translation").doubleValue());
        assertEquals(        2, CRS.getLinearAccuracy(operation));
    }

    /**
     * Asserts that the given object has the expected name but no identifier. This method is used when the given object
     * has been modified compared to the object declared in the EPSG dataset, for example with a change of axis order
     * or the addition of height. In such case the modified object is not allowed to have the EPSG identifier of the
     * original object.
     *
     * @param  name    the expected EPSG name.
     * @param  object  the object to verify.
     */
    private static void assertEpsgNameWithoutIdentifierEqual(final String name, final IdentifiedObject object) {
        assertNotNull(object, name);
        assertEquals(name, object.getName().getCode(), "name");
        for (final Identifier id : object.getIdentifiers()) {
            assertFalse("EPSG".equalsIgnoreCase(id.getCodeSpace()), "EPSG identifier not allowed for modified objects.");
        }
    }

    /**
     * Tests <q>Martinique 1938 to RGAF09 (1)</q> operation with a target CRS fixed to EPSG:7086
     * instead of EPSG:5489. Both are <q>RGAF09</q>, but the former use (longitude, latitude) axis
     * order instead of the usual (latitude, longitude) order. The source CRS stay fixed to EPSG:4625.
     *
     * @throws FactoryException if an error occurred while creating a CRS or operation.
     */
    @Test
    public void testFindDespiteDifferentAxisOrder() throws FactoryException {
        CoordinateReferenceSystem sourceCRS = crsFactory.createGeographicCRS("EPSG:4625");
        CoordinateReferenceSystem targetCRS = crsFactory.createGeographicCRS("EPSG:5489");
        CoordinateOperation operation = createOperation(sourceCRS, targetCRS);
        assertEpsgNameAndIdentifierEqual("Martinique 1938 to RGAF09 (1)", 5491, operation);
        /*
         * Above was only a verification using the source and target CRS expected by EPSG dataset.
         * Now the interesting test: use a target CRS with different axis order.
         */
        targetCRS = crsFactory.createGeographicCRS("EPSG:7086");
        operation = createOperation(sourceCRS, targetCRS);
        assertEpsgNameWithoutIdentifierEqual("Martinique 1938 to RGAF09 (1)", operation);
        final ParameterValueGroup p = ((SingleOperation) operation).getParameterValues();
        /*
         * Values below are copied from EPSG geodetic dataset 9.1. They may need
         * to be adjusted if a future version of EPSG dataset modify those values.
         */
        assertEquals(127.744,  p.parameter("X-axis translation").doubleValue());
        assertEquals(547.069,  p.parameter("Y-axis translation").doubleValue());
        assertEquals(118.359,  p.parameter("Z-axis translation").doubleValue());
        assertEquals( -3.1116, p.parameter("X-axis rotation")   .doubleValue());
        assertEquals(  4.9509, p.parameter("Y-axis rotation")   .doubleValue());
        assertEquals( -0.8837, p.parameter("Z-axis rotation")   .doubleValue());
        assertEquals( 14.1012, p.parameter("Scale difference")  .doubleValue());
        assertEquals(  0.1,    CRS.getLinearAccuracy(operation));
    }
}
