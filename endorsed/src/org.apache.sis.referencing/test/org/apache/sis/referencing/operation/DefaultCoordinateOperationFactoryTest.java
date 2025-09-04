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
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.ConcatenatedOperation;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.CoordinateOperationAuthorityFactory;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.privy.Formulas;
import org.apache.sis.referencing.internal.PositionalAccuracyConstant;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.io.wkt.WKTFormat;

// Test dependencies
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.operation.transform.MathTransformTestCase;
import static org.apache.sis.test.Assertions.assertMessageContains;
import static org.apache.sis.referencing.Assertions.assertEpsgNameAndIdentifierEqual;


/**
 * Tests {@link DefaultCoordinateOperationFactory}, with or without EPSG geodetic dataset.
 *
 * <h2>Relationship with other tests</h2>
 * <ul>
 *   <li>{@link CoordinateOperationRegistryTest} requires an EPSG geodetic dataset (otherwise tests are skipped).</li>
 *   <li>{@link CoordinateOperationFinderTest} do not use any EPSG geodetic dataset.</li>
 *   <li>{@code DefaultCoordinateOperationFactoryTest} is a mix of both.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@Execution(ExecutionMode.SAME_THREAD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class DefaultCoordinateOperationFactoryTest extends MathTransformTestCase {
    /**
     * The transformation factory to use for testing.
     */
    private final DefaultCoordinateOperationFactory factory;

    /**
     * The parser to use for WKT strings used in this test.
     */
    private final WKTFormat parser;

    /**
     * Creates a new {@link DefaultCoordinateOperationFactory} to use for testing purpose.
     * The same factory will be used for all tests in this class.
     *
     * @throws ParseException if an error occurred while preparing the WKT parser.
     */
    public DefaultCoordinateOperationFactoryTest() throws ParseException {
        factory = new DefaultCoordinateOperationFactory();
        parser  = new WKTFormat();
        parser.addFragment("NTF",
                "ProjectedCRS[“NTF (Paris) / Lambert zone II”,\n" +
                "  BaseGeodCRS[“NTF (Paris)”,\n" +
                "    Datum[“Nouvelle Triangulation Française (Paris)”,\n" +
                "      Ellipsoid[“Clarke 1880 (IGN)”, 6378249.2, 293.4660212936269]],\n" +
                "      PrimeMeridian[“Paris”, 2.5969213],\n" +
                "    Unit[“grad”, 0.015707963267948967]]\n," +
                "  Conversion[“Lambert zone II”,\n" +
                "    Method[“Lambert Conic Conformal (1SP)”],\n" +
                "    Parameter[“Latitude of natural origin”, 52.0],\n" +
                "    Parameter[“Scale factor at natural origin”, 0.99987742],\n" +
                "    Parameter[“False easting”, 600000.0],\n" +
                "    Parameter[“False northing”, 2200000.0]],\n" +
                "  CS[Cartesian, 2]\n," +
                "    Axis[“Easting (X)”, east],\n" +
                "    Axis[“Northing (Y)”, north],\n" +
                "    Unit[“metre”, 1],\n" +
                "  Id[“EPSG”, 27572]]");

        parser.addFragment("Mercator",
                "ProjectedCRS[“WGS 84 / World Mercator”,\n" +
                "  BaseGeodCRS[“WGS 84”,\n" +
                "    Datum[“World Geodetic System 1984”,\n" +
                "      Ellipsoid[“WGS 84”, 6378137.0, 298.257223563]],\n" +
                "    Unit[“degree”, 0.017453292519943295]],\n" +
                "  Conversion[“World Mercator”,\n" +
                "    Method[“Mercator (variant A)”]],\n" +
                "  CS[Cartesian, 2]\n," +
                "    Axis[“Easting (X)”, east],\n" +
                "    Axis[“Northing (Y)”, north],\n" +
                "    Unit[“metre”, 1],\n" +
                "  Id[“EPSG”, 3395]]");
    }

    /**
     * Resets all fields that may be modified by test methods in this class.
     * This is needed because we reuse the same instance for all test methods,
     * in order to reuse the factory and parser created in the constructor.
     */
    @Override
    @BeforeEach
    public void reset() {
        super.reset();
        isInverseTransformSupported = true;
    }

    /**
     * Returns the CRS for the given Well Known Text.
     */
    private CoordinateReferenceSystem parse(final String wkt) throws ParseException {
        return assertInstanceOf(CoordinateReferenceSystem.class, parser.parseObject(wkt));
    }

    /**
     * Returns {@code true} if {@link #factory} is expected to use the EPSG factory.
     */
    private static boolean isUsingEpsgFactory() throws FactoryException {
        return DefaultCoordinateOperationFactory.USE_EPSG_FACTORY &&
               CRS.getAuthorityFactory(Constants.EPSG) instanceof CoordinateOperationAuthorityFactory;
    }

    /**
     * Tests a transformation between 2D projected CRS which implies a change of prime meridian.
     *
     * @throws ParseException if a CRS used in this test cannot be parsed.
     * @throws FactoryException if the operation cannot be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    public void testProjectionAndLongitudeRotation() throws ParseException, FactoryException, TransformException {
        final CoordinateReferenceSystem sourceCRS = parse("$NTF");
        final CoordinateReferenceSystem targetCRS = parse("$Mercator");
        final CoordinateOperation operation = factory.createOperation(sourceCRS, targetCRS, null);
        assertSame      (sourceCRS, operation.getSourceCRS());
        assertSame      (targetCRS, operation.getTargetCRS());
        assertInstanceOf(ConcatenatedOperation.class, operation);
        /*
         * The accuracy of the coordinate operation depends on whether a path has been found with the help
         * of the EPSG database (in which case the reported accuracy is 2 metres) or if we had to find an
         * operation by ourselves (in which case we conservatively report an accuracy of 3000 metres, but
         * in practice observe an error between 80 and 515 metres for this test depending on the operation
         * method used). By comparison, the translation declared in EPSG database is about 370 metres in
         * geocentric coordinates.
         */
        final boolean isUsingEpsgFactory = verifyParametersNTF(((ConcatenatedOperation) operation).getOperations(), 1);
        assertEquals(isUsingEpsgFactory ? 2 : PositionalAccuracyConstant.UNKNOWN_ACCURACY,
                     CRS.getLinearAccuracy(operation));

        tolerance = isUsingEpsgFactory ? Formulas.LINEAR_TOLERANCE : 600;
        transform = operation.getMathTransform();
        /*
         * Test using the location of Paris (48.856578°N, 2.351828°E) first,
         * then using a coordinate different than the prime meridian.
         */
        verifyTransform(new double[] {
            601124.99, 2428693.45,
            600000.00, 2420000.00
        }, new double[] {
            261804.30, 6218365.73,
            260098.74, 6205194.95
        });
        validate();
    }

    /**
     * Tests a transformation from a 4D projection to a 2D projection which imply a change of
     * prime meridian. This is the same test as {@link #testProjectionAndLongitudeRotation()},
     * with extra dimension which should be just dropped.
     *
     * <p>This tests requires the EPSG database, because it requires the coordinate operation
     * path which is defined there.</p>
     *
     * @throws ParseException if a CRS used in this test cannot be parsed.
     * @throws FactoryException if the operation cannot be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    public void testCompoundAndLongitudeRotation() throws ParseException, FactoryException, TransformException {
        final CoordinateReferenceSystem sourceCRS = parse(
                "CompoundCRS[“NTF 4D”," +
                "  $NTF,\n" +
                "  VerticalCRS[“Geoidal height”,\n" +
                "    VerticalDatum[“Geoid”],\n" +
                "    CS[vertical, 1],\n" +
                "      Axis[“Geoidal height (H)”, up],\n" +
                "      Unit[“metre”, 1]],\n" +
                "  TimeCRS[“Modified Julian”,\n" +
                "    TimeDatum[“Modified Julian”, TimeOrigin[1858-11-17T00:00:00.0Z]],\n" +
                "    CS[temporal, 1],\n" +
                "      Axis[“Time (t)”, future],\n" +
                "      TimeUnit[“day”, 86400]]]");

        final CoordinateReferenceSystem targetCRS = parse("$Mercator");
        final CoordinateOperation operation = factory.createOperation(sourceCRS, targetCRS, null);
        assertSame      (sourceCRS, operation.getSourceCRS());
        assertSame      (targetCRS, operation.getTargetCRS());
        assertInstanceOf(ConcatenatedOperation.class, operation);
        /*
         * The accuracy of the coordinate operation depends on whether a path has been found with the help
         * of the EPSG database. See testProjectionAndLongitudeRotation() for more information.
         */
        final boolean isUsingEpsgFactory = verifyParametersNTF(((ConcatenatedOperation) operation).getOperations(), 2);
        assertEquals(isUsingEpsgFactory ? 2 : PositionalAccuracyConstant.UNKNOWN_ACCURACY,
                     CRS.getLinearAccuracy(operation));

        tolerance = isUsingEpsgFactory ? Formulas.LINEAR_TOLERANCE : 600;
        transform = operation.getMathTransform();
        isInverseTransformSupported = false;
        /*
         * Same coordinates as testProjectionAndLongitudeRotation(),
         * but with random elevation and time which should be dropped.
         */
        verifyTransform(new double[] {
            601124.99, 2428693.45, 400, 1000,
            600000.00, 2420000.00, 400, 1000
        }, new double[] {
            261804.30, 6218365.73,
            260098.74, 6205194.95
        });
        validate();
    }

    /**
     * Verifies the datum shift parameters in the <q>NTF to WGS 84 (1)</q> transformation.
     * Those parameters depends on whether an EPSG database have been used or not.
     *
     * @param  steps            the list returned by {@link DefaultConcatenatedOperation#getOperations()}.
     * @param  datumShiftIndex  index of the datum shift operations in the {@code steps} list.
     * @return the {@link #isUsingEpsgFactory()} value, returned for convenience.
     */
    private static boolean verifyParametersNTF(final List<? extends CoordinateOperation> steps, final int datumShiftIndex)
            throws FactoryException
    {
        if (isUsingEpsgFactory()) {
            final SingleOperation step1 = (SingleOperation) steps.get(datumShiftIndex);
            final SingleOperation step2 = (SingleOperation) steps.get(datumShiftIndex + 1);
            assertEpsgNameAndIdentifierEqual("NTF (Paris) to NTF (1)", 1763, step1);
            assertEpsgNameAndIdentifierEqual("NTF to WGS 84 (1)",      1193, step2);
            final ParameterValueGroup p1 = step1.getParameterValues();
            final ParameterValueGroup p2 = step2.getParameterValues();
            assertEquals(2.5969213, p1.parameter("Longitude offset")  .doubleValue());
            assertEquals(-168,      p2.parameter("X-axis translation").doubleValue());
            assertEquals( -60,      p2.parameter("Y-axis translation").doubleValue());
            assertEquals( 320,      p2.parameter("Z-axis translation").doubleValue());
            return true;
        } else {
            assertSame(CoordinateOperationFinder.UNSPECIFIED_DATUM_CHANGE, steps.get(datumShiftIndex).getName());
            return false;
        }
    }

    /**
     * Tests the conversion from Mercator projection to the Google projection. The referencing module
     * should detects that the conversion is something more complex that an identity transform.
     *
     * @throws ParseException if a CRS used in this test cannot be parsed.
     * @throws FactoryException if the operation cannot be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    public void testMercatorToGoogle() throws ParseException, FactoryException, TransformException {
        final CoordinateReferenceSystem sourceCRS = parse("$Mercator");
        final CoordinateReferenceSystem targetCRS = parse(
                "ProjectedCRS[“WGS 84 / Pseudo-Mercator”,\n" +
                "  BaseGeodCRS[“WGS 84”,\n" +
                "    Datum[“World Geodetic System 1984”,\n" +
                "      Ellipsoid[“WGS 84”, 6378137.0, 298.257223563]],\n" +
                "    Unit[“degree”, 0.017453292519943295]],\n" +
                "  Conversion[“Popular Visualisation Pseudo-Mercator”,\n" +
                "    Method[“Popular Visualisation Pseudo Mercator”]],\n" +
                "  CS[Cartesian, 2],\n" +
                "    Axis[“Easting (X)”, east],\n" +
                "    Axis[“Northing (Y)”, north],\n" +
                "    Unit[“metre”, 1],\n" +
                "  Id[“EPSG”, 3857]]");

        final CoordinateOperation operation = factory.createOperation(sourceCRS, targetCRS, null);
        assertSame      (sourceCRS, operation.getSourceCRS());
        assertSame      (targetCRS, operation.getTargetCRS());
        assertInstanceOf(ConcatenatedOperation.class, operation);

        transform = operation.getMathTransform();
        tolerance = 1;

        assertFalse(transform.isIdentity(), "Mercator to Google should not be an identity transform.");
        final DirectPosition2D sourcePt = new DirectPosition2D(334000, 4840000);        // Approximately 40°N 3°W
        final DirectPosition2D targetPt = new DirectPosition2D();
        assertSame(targetPt, transform.transform(sourcePt, targetPt));
        assertEquals(sourcePt.getX(), targetPt.getX(), "Easting should be unchanged");
        assertEquals(27476, targetPt.getY() - sourcePt.getY(), tolerance, "Expected 27 km shift");
    }

    /**
     * Tests a datum shift applied as a position vector transformation in geocentric domain.  This method performs
     * the same test as {@link CoordinateOperationFinderTest#testPositionVectorTransformation()} except that the
     * EPSG geodetic dataset may be used. The result however should be the same because of the {@code TOWGS84}
     * parameter in the WKT used for the test.
     *
     * @throws ParseException if a CRS used in this test cannot be parsed.
     * @throws FactoryException if the operation cannot be created.
     * @throws TransformException if an error occurred while converting the test points.
     *
     * @see CoordinateOperationFinderTest#testPositionVectorTransformation()
     * @see <a href="https://issues.apache.org/jira/browse/SIS-364">SIS-364</a>
     */
    @Test
    public void testPositionVectorTransformation() throws ParseException, FactoryException, TransformException {
        final CoordinateReferenceSystem sourceCRS = HardCodedCRS.WGS84_LATITUDE_FIRST;
        final CoordinateReferenceSystem targetCRS = parse(CoordinateOperationFinderTest.AGD66());
        final CoordinateOperation operation = factory.createOperation(sourceCRS, targetCRS, null);
        transform  = operation.getMathTransform();
        tolerance  = Formulas.LINEAR_TOLERANCE;
        λDimension = new int[] {0};
        verifyTransform(CoordinateOperationFinderTest.expectedAGD66(true),
                        CoordinateOperationFinderTest.expectedAGD66(false));
        validate();
    }

    /**
     * Verifies that requesting an unknown method throws {@link NoSuchIdentifierException}.
     *
     * @throws FactoryException if an unexpected error occurred.
     */
    @Test
    public void testUnknownMethod() throws FactoryException {
        var e = assertThrows(NoSuchIdentifierException.class, () -> factory.getOperationMethod("I do not exist"));
        assertMessageContains(e, "I do not exist");
    }
}
