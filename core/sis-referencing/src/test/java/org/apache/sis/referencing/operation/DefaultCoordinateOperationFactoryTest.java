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
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.ConcatenatedOperation;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.CoordinateOperationAuthorityFactory;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.referencing.PositionalAccuracyConstant;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.io.wkt.WKTFormat;
import org.apache.sis.referencing.CRS;

// Test dependencies
import org.apache.sis.referencing.operation.transform.MathTransformTestCase;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

import static org.apache.sis.test.ReferencingAssert.*;


/**
 * Tests {@link DefaultCoordinateOperationFactory}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn({
    CoordinateOperationRegistryTest.class,
    CoordinateOperationFinderTest.class
})
public final strictfp class DefaultCoordinateOperationFactoryTest extends MathTransformTestCase {
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
                "ProjectedCRS[“NTF (Paris) / Lambert zone II”,\n" +
                "  BaseGeodCRS[“NTF (Paris)”,\n" +
                "    Datum[“Nouvelle Triangulation Française (Paris)”,\n" +
                "      Ellipsoid[“Clarke 1880 (IGN)”, 6378249.2, 293.4660212936269]],\n" +
                "      PrimeMeridian[“Paris”, 2.5969213],\n" +
                "    Unit[“grade”, 0.015707963267948967]]\n," +
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
     * Returns {@code true} if {@link #factory} is expected to use the EPSG factory.
     */
    private static boolean isUsingEpsgFactory() throws FactoryException {
        return DefaultCoordinateOperationFactory.USE_EPSG_FACTORY &&
               CRS.getAuthorityFactory(Constants.EPSG) instanceof CoordinateOperationAuthorityFactory;
    }

    /**
     * Tests a transformation between 2D projected CRS which implies a change of prime meridian.
     *
     * @throws ParseException if a CRS used in this test can not be parsed.
     * @throws FactoryException if the operation can not be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    public void testProjectionAndLongitudeRotation() throws ParseException, FactoryException, TransformException {
        final CoordinateReferenceSystem sourceCRS = parse("$NTF");
        final CoordinateReferenceSystem targetCRS = parse("$Mercator");
        final CoordinateOperation operation = factory.createOperation(sourceCRS, targetCRS);
        assertSame      ("sourceCRS", sourceCRS, operation.getSourceCRS());
        assertSame      ("targetCRS", targetCRS, operation.getTargetCRS());
        assertInstanceOf("operation", ConcatenatedOperation.class, operation);
        /*
         * The accuracy of the coordinate operation depends on whether a path as been found with the help
         * of the EPSG database (in which case the reported accuracy is 2 metres) or if we had to find an
         * operation by ourselves (in which case we conservatively report an accuracy of 3000 metres, bu
         * in practice observe an error of about 80 metres for this test).
         */
        final boolean isUsingEpsgFactory = verifyParametersNTF(((ConcatenatedOperation) operation).getOperations(), 1);
        assertEquals("linearAccuracy", isUsingEpsgFactory ? 2 : PositionalAccuracyConstant.UNKNOWN_ACCURACY,
                                       CRS.getLinearAccuracy(operation), STRICT);

        tolerance = isUsingEpsgFactory ? Formulas.LINEAR_TOLERANCE : 100;
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
     * prime meridian. This is the same test than {@link #testProjectionAndLongitudeRotation()},
     * with extra dimension which should be just dropped.
     *
     * <p>This tests requires the EPSG database, because it requires the coordinate operation
     * path which is defined there.</p>
     *
     * @throws ParseException if a CRS used in this test can not be parsed.
     * @throws FactoryException if the operation can not be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    @DependsOnMethod("testProjectionAndLongitudeRotation")
    public void testCompoundAndLongitudeRotation() throws ParseException, FactoryException, TransformException {
        final CoordinateReferenceSystem sourceCRS = parse(
                "CompoundCRS[“NTF 4D”," +
                "  $NTF,\n" +
                "  VerticalCRS[“Ellipsoidal height”,\n" +
                "    VerticalDatum[“Ellipsoid”],\n" +
                "    CS[vertical, 1],\n" +
                "      Axis[“Ellipsoidal height (h)”, up],\n" +
                "      Unit[“metre”, 1]],\n" +
                "  TimeCRS[“Modified Julian”,\n" +
                "    TimeDatum[“Modified Julian”, TimeOrigin[1858-11-17T00:00:00.0Z]],\n" +
                "    CS[temporal, 1],\n" +
                "      Axis[“Time (t)”, future],\n" +
                "      TimeUnit[“day”, 86400]]]");

        final CoordinateReferenceSystem targetCRS = parse("$Mercator");
        final CoordinateOperation operation = factory.createOperation(sourceCRS, targetCRS);
        assertSame      ("sourceCRS", sourceCRS, operation.getSourceCRS());
        assertSame      ("targetCRS", targetCRS, operation.getTargetCRS());
        assertInstanceOf("operation", ConcatenatedOperation.class, operation);
        /*
         * The accuracy of the coordinate operation depends on whether a path as been found with the help
         * of the EPSG database. See testProjectionAndLongitudeRotation() for more information.
         */
        final boolean isUsingEpsgFactory = verifyParametersNTF(((ConcatenatedOperation) operation).getOperations(), 2);
        assertEquals("linearAccuracy", isUsingEpsgFactory ? 2 : PositionalAccuracyConstant.UNKNOWN_ACCURACY,
                                       CRS.getLinearAccuracy(operation), STRICT);

        tolerance = isUsingEpsgFactory ? Formulas.LINEAR_TOLERANCE : 100;
        transform = operation.getMathTransform();
        isInverseTransformSupported = false;
        /*
         * Same coordinates than testProjectionAndLongitudeRotation(),
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
     * Verifies the datum shift parameters in the <cite>"NTF to WGS 84 (1)"</cite> transformation.
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
            assertEquals("Longitude offset", 2.5969213, p1.parameter("Longitude offset")  .doubleValue(), STRICT);
            assertEquals("X-axis translation",    -168, p2.parameter("X-axis translation").doubleValue(), STRICT);
            assertEquals("Y-axis translation",     -60, p2.parameter("Y-axis translation").doubleValue(), STRICT);
            assertEquals("Z-axis translation",     320, p2.parameter("Z-axis translation").doubleValue(), STRICT);
            return true;
        } else {
            assertSame(CoordinateOperationFinder.ELLIPSOID_CHANGE, steps.get(datumShiftIndex).getName());
            return false;
        }
    }

    /**
     * Tests the conversion from Mercator projection to the Google projection. The referencing module
     * should detects that the conversion is something more complex that an identity transform.
     *
     * @throws ParseException if a CRS used in this test can not be parsed.
     * @throws FactoryException if the operation can not be created.
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

        final CoordinateOperation operation = factory.createOperation(sourceCRS, targetCRS);
        assertSame      ("sourceCRS", sourceCRS, operation.getSourceCRS());
        assertSame      ("targetCRS", targetCRS, operation.getTargetCRS());
        assertInstanceOf("operation", ConcatenatedOperation.class, operation);

        transform = operation.getMathTransform();
        tolerance = 1;

        assertFalse("Mercator to Google should not be an identity transform.", transform.isIdentity());
        final DirectPosition2D sourcePt = new DirectPosition2D(334000, 4840000);        // Approximatively 40°N 3°W
        final DirectPosition2D targetPt = new DirectPosition2D();
        assertSame(targetPt, transform.transform(sourcePt, targetPt));
        assertEquals("Easting should be unchanged", sourcePt.getX(),  targetPt.getX(), STRICT);
        assertEquals("Expected 27 km shift", 27476, targetPt.getY() - sourcePt.getY(), tolerance);
    }
}
