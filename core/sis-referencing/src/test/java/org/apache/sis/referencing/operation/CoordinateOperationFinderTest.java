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

import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Collections;
import java.text.ParseException;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.GeocentricCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.crs.DerivedCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.Projection;
import org.opengis.referencing.operation.Transformation;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.ConcatenatedOperation;
import org.opengis.referencing.operation.OperationNotFoundException;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.crs.DefaultCompoundCRS;
import org.apache.sis.referencing.crs.DefaultDerivedCRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.io.wkt.WKTFormat;

import static org.apache.sis.internal.referencing.Formulas.LINEAR_TOLERANCE;
import static org.apache.sis.internal.referencing.Formulas.ANGULAR_TOLERANCE;
import static org.apache.sis.internal.referencing.PositionalAccuracyConstant.DATUM_SHIFT_APPLIED;

// Test dependencies
import org.apache.sis.referencing.operation.transform.MathTransformTestCase;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.cs.HardCodedCS;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.Assert;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link CoordinateOperationFinder}.
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
public final strictfp class CoordinateOperationFinderTest extends MathTransformTestCase {
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
    private CoordinateOperationFinder inference;

    /**
     * Creates a new test case.
     *
     * @throws FactoryException if an error occurred while initializing the finder to test.
     */
    public CoordinateOperationFinderTest() throws FactoryException {
        inference = new CoordinateOperationFinder(null, factory, null);
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
        /*
         * The fist keyword in WKT below should be "GeodeticCRS" in WKT 2, but we use the WKT 1 keyword ("GEOGCS")
         * for allowing inclusion in ProjectedCRS.  SIS is okay with mixed WKT versions, but this is of course not
         * something to recommend in production.
         */
        parser.addFragment("Sphere",
                "GEOGCS[“Sphere”,\n" +
                "  Datum[“Sphere”, Ellipsoid[“Sphere”, 6370997, 0]],\n" +
                "  CS[ellipsoidal, 2],\n" +
                "    Axis[“Longitude (λ)”, EAST],\n" +          // Use of non-ASCII letters is departure from WKT 2.
                "    Axis[“Latitude (φ)”, NORTH],\n" +
                "    Unit[“degree”, 0.017453292519943295]]");
        /*
         * Nouvelle Triangulation Française (Datum of EPSG:4807 CRS).
         * Use non-Greenwich prime meridian grad units (0.9 grad = 1°).
         * We use the WKT 1 format because TOWGS84[…] is not a legal WKT 2 element.
         */
        parser.addFragment("NTF",
                "DATUM[“Nouvelle Triangulation Française”,\n" +
                "  SPHEROID[“Clarke 1880 (IGN)”, 6378249.2, 293.466021293627],\n" +
                "  TOWGS84[-168, -60, 320]]");
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
     * Makes sure that {@code createOperation(sourceCRS, targetCRS)} returns an identity transform
     * when {@code sourceCRS} and {@code targetCRS} are identical.
     *
     * @throws FactoryException if the operation can not be created.
     */
    @Test
    public void testIdentityTransform() throws FactoryException {
        testIdentityTransform(CommonCRS.WGS84.geographic());
        testIdentityTransform(CommonCRS.WGS84.geographic3D());
        testIdentityTransform(CommonCRS.WGS84.geocentric());
        testIdentityTransform(CommonCRS.WGS84.spherical());
        testIdentityTransform(CommonCRS.WGS84.UTM(0, 0));
        testIdentityTransform(CommonCRS.Vertical.DEPTH.crs());
        testIdentityTransform(CommonCRS.Temporal.JULIAN.crs());
    }

    /**
     * Implementation of {@link #testIdentityTransform()} using the given CRS.
     */
    private void testIdentityTransform(final CoordinateReferenceSystem crs) throws FactoryException {
        final CoordinateOperation operation = inference.createOperation(crs, crs);
        assertSame      ("sourceCRS",  crs, operation.getSourceCRS());
        assertSame      ("targetCRS",  crs, operation.getTargetCRS());
        assertTrue      ("isIdentity", operation.getMathTransform().isIdentity());
        assertTrue      ("accuracy",   operation.getCoordinateOperationAccuracy().isEmpty());
        assertInstanceOf("operation",  Conversion.class, operation);
        inference = new CoordinateOperationFinder(null, factory, null);        // Reset for next call.
    }

    /**
     * Tests a transformation with a two-dimensional geographic source CRS.
     * This method verifies with both a two-dimensional and a three-dimensional target CRS.
     *
     * @throws ParseException if a CRS used in this test can not be parsed.
     * @throws FactoryException if the operation can not be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    @DependsOnMethod("testIdentityTransform")
    public void testGeocentricTranslationInGeographic2D() throws ParseException, FactoryException, TransformException {
        /*
         * NAD27 (EPSG:4267) defined in WKT instead than relying on the CommonCRS.NAD27 constant in order to fix
         * the TOWGS84[…] parameter to values that we control. Note that TOWGS84[…] is not a legal WKT 2 element.
         * We could mix WKT 1 and WKT 2 elements (SIS allows that), but we nevertheless use WKT 1 for the whole
         * string as a matter of principle.
         */
        final GeographicCRS sourceCRS = (GeographicCRS) parse(
                "GEOGCS[“NAD27”,\n" +
                "  DATUM[“North American Datum 1927”,\n" +
                "    SPHEROID[“Clarke 1866”, 6378206.4, 294.9786982138982],\n" +
                "    TOWGS84[-8, 160, 176]]," +                                     // EPSG:1173
                "    PRIMEM[“Greenwich”, 0.0]," +
                "  UNIT[“degree”, 0.017453292519943295],\n" +
                "  AXIS[“Latitude (φ)”, NORTH],\n" +
                "  AXIS[“Longitude (λ)”, EAST],\n" +
                "  AUTHORITY[“EPSG”, “4267”]]");

        testGeocentricTranslationInGeographicDomain("Geocentric translations (geog2D domain)", sourceCRS, CommonCRS.WGS84.geographic());
        testGeocentricTranslationInGeographicDomain("Geocentric translations (geog3D domain)", sourceCRS, CommonCRS.WGS84.geographic3D());
    }

    /**
     * Tests a transformation with a three-dimensional geographic source CRS.
     * This method verifies with both a three-dimensional and a two-dimensional target CRS.
     *
     * @throws ParseException if a CRS used in this test can not be parsed.
     * @throws FactoryException if the operation can not be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    @DependsOnMethod("testGeocentricTranslationInGeographic2D")
    public void testGeocentricTranslationInGeographic3D() throws ParseException, FactoryException, TransformException {
        final GeographicCRS sourceCRS = (GeographicCRS) parse(
                "GeodeticCRS[“NAD27”,\n" +
                "  Datum[“North American Datum 1927”,\n" +
                "    Ellipsoid[“Clarke 1866”, 6378206.4, 294.9786982138982],\n" +
                "    ToWGS84[-8, 160, 176]]," +                                     // See comment in above test.
                "  CS[ellipsoidal, 3],\n" +
                "    Axis[“Latitude (φ)”, NORTH, Unit[“degree”, 0.017453292519943295]],\n" +
                "    Axis[“Longitude (λ)”, EAST, Unit[“degree”, 0.017453292519943295]],\n" +
                "    Axis[“Height (h)”, UP, Unit[“m”, 1]]]");

        testGeocentricTranslationInGeographicDomain("Geocentric translations (geog3D domain)",
                sourceCRS, CommonCRS.WGS84.geographic3D());

        isInverseTransformSupported = false;                // Because lost of height values changes (φ,λ) results.
        testGeocentricTranslationInGeographicDomain("Geocentric translations (geog3D domain)",
                sourceCRS, CommonCRS.WGS84.geographic());
    }

    /**
     * Implementation of {@link #testGeocentricTranslationInGeographic2D()}
     * and {@link #testGeocentricTranslationInGeographic3D()}.
     *
     * @param sourceCRS The NAD27 geographic CRS.
     * @param targetCRS Either the two-dimensional or the three-dimensional geographic CRS using WGS84 datum.
     */
    private void testGeocentricTranslationInGeographicDomain(final String method,
            final GeographicCRS sourceCRS, final GeographicCRS targetCRS)
            throws ParseException, FactoryException, TransformException
    {
        final CoordinateOperation operation = inference.createOperation(sourceCRS, targetCRS);
        assertSame      ("sourceCRS",  sourceCRS,            operation.getSourceCRS());
        assertSame      ("targetCRS",  targetCRS,            operation.getTargetCRS());
        assertFalse     ("isIdentity",                       operation.getMathTransform().isIdentity());
        assertEquals    ("name",       "Datum shift",        operation.getName().getCode());
        assertSetEquals (Arrays.asList(DATUM_SHIFT_APPLIED), operation.getCoordinateOperationAccuracy());
        assertInstanceOf("operation",  Transformation.class, operation);
        assertEquals    ("method", method, ((SingleOperation) operation).getMethod().getName().getCode());

        transform  = operation.getMathTransform();
        tolerance  = ANGULAR_TOLERANCE;
        zTolerance = 0.01;
        λDimension = new int[] {1};
        zDimension = new int[] {2};
        double[] source = {
            39.00,  -85.00,  -10000.00,   // The intend of those large height values is to cause a shift in (φ,λ)
            38.26,  -80.58,  +10000.00    // large enough for being detected if we fail to use h in calculations.
        };
        double[] target;
        if (sourceCRS.getCoordinateSystem().getDimension() == 2) {
            source = TestUtilities.dropLastDimensions(source, 3, 2);
            target = new double[] {
                39.00004480, -84.99993102, -38.28,  // This is NOT the most accurate NAD27 to WGS84 transformation.
                38.26005019, -80.57979096, -37.62   // We use non-optimal TOWGS84[…] for the purpose of this test.
            };
        } else {
            target = new double[] {
                39.00004487, -84.99993091, -10038.28,
                38.26005011, -80.57979129,   9962.38
            };
        }
        if (targetCRS.getCoordinateSystem().getDimension() == 2) {
            target = TestUtilities.dropLastDimensions(target, 3, 2);
        }
        tolerance = zTolerance; // Because GeoAPI 3.0 does not distinguish z axis from other axis (fixed in GeoAPI 3.1).
        verifyTransform(source, target);
        validate();
    }

    /**
     * Tests a transformation using the <cite>"Geocentric translations (geog2D domain)"</cite> method
     * together with a longitude rotation and unit conversion. The CRS and sample point are taken from
     * the GR3DF97A – <cite>Grille de paramètres de transformation de coordonnées</cite> document.
     *
     * @throws ParseException if a CRS used in this test can not be parsed.
     * @throws FactoryException if the operation can not be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    @DependsOnMethod("testGeocentricTranslationInGeographic2D")
    public void testLongitudeRotation() throws ParseException, FactoryException, TransformException {
        final CoordinateReferenceSystem sourceCRS = parse(
                "GeodeticCRS[“NTF (Paris)”, $NTF,\n" +
                "  PrimeMeridian[“Paris”, 2.5969213],\n" +          // in grads, not degrees.
                "  CS[ellipsoidal, 2],\n" +
                "    Axis[“Latitude (φ)”, NORTH],\n" +
                "    Axis[“Longitude (λ)”, EAST],\n" +
                "    Unit[“grade”, 0.015707963267949],\n" +
                "  Id[“EPSG”, “4807”]]");

        final GeographicCRS       targetCRS = CommonCRS.WGS84.geographic();
        final CoordinateOperation operation = inference.createOperation(sourceCRS, targetCRS);

        assertSame      ("sourceCRS",  sourceCRS,            operation.getSourceCRS());
        assertSame      ("targetCRS",  targetCRS,            operation.getTargetCRS());
        assertFalse     ("isIdentity",                       operation.getMathTransform().isIdentity());
        assertEquals    ("name",       "Datum shift",        operation.getName().getCode());
        assertSetEquals (Arrays.asList(DATUM_SHIFT_APPLIED), operation.getCoordinateOperationAccuracy());
        assertInstanceOf("operation",  Transformation.class, operation);
        assertEquals("method", "Geocentric translations (geog2D domain)",
                ((SingleOperation) operation).getMethod().getName().getCode());
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
     * Tests a transformation using the <cite>"Geocentric translations (geocentric domain)"</cite> method,
     * together with a longitude rotation and unit conversion. The CRS and sample point are derived from
     * the GR3DF97A – <cite>Grille de paramètres de transformation de coordonnées</cite> document.
     *
     * @throws ParseException if a CRS used in this test can not be parsed.
     * @throws FactoryException if the operation can not be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    @DependsOnMethod("testLongitudeRotation")
    public void testGeocentricTranslationInGeocentricDomain() throws ParseException, FactoryException, TransformException {
        final CoordinateReferenceSystem sourceCRS = parse(
                "GeodeticCRS[“NTF (Paris)”, $NTF,\n" +
                "  PrimeMeridian[“Paris”, 2.33722917],\n" +         // in degrees.
                "  CS[Cartesian, 3],\n" +
                "    Axis[“(X)”, geocentricX],\n" +
                "    Axis[“(Y)”, geocentricY],\n" +
                "    Axis[“(Z)”, geocentricZ],\n" +
                "    Unit[“km”, 1000]]");

        final GeocentricCRS       targetCRS = CommonCRS.WGS84.geocentric();
        final CoordinateOperation operation = inference.createOperation(sourceCRS, targetCRS);

        assertSame      ("sourceCRS",  sourceCRS,            operation.getSourceCRS());
        assertSame      ("targetCRS",  targetCRS,            operation.getTargetCRS());
        assertFalse     ("isIdentity",                       operation.getMathTransform().isIdentity());
        assertEquals    ("name",       "Datum shift",        operation.getName().getCode());
        assertSetEquals (Arrays.asList(DATUM_SHIFT_APPLIED), operation.getCoordinateOperationAccuracy());
        assertInstanceOf("operation", Transformation.class,  operation);
        assertEquals    ("method", "Geocentric translations (geocentric domain)",
                ((SingleOperation) operation).getMethod().getName().getCode());
        /*
         * Same test point than the one used in FranceGeocentricInterpolationTest:
         *
         * ┌────────────────────────────────────────────┬──────────────────────────────────────────────────────────┐
         * │         Geographic coordinates (°)         │                  Geocentric coordinates (m)              │
         * ├────────────────────────────────────────────┼──────────────────────────────────────────────────────────┤
         * │    NTF: 48°50′40.2441″N  2°25′32.4187″E    │    X = 4201905.725   Y = 177998.072   Z = 4778904.260    │
         * │    RGF: 48°50′39.9967″N  2°25′29.8273″E    │      ΔX = -168         ΔY = -60          ΔZ = 320        │
         * └────────────────────────────────────────────┴──────────────────────────────────────────────────────────┘
         *
         * The source coordinate below is different than in the above table because the prime meridian is set to the
         * Paris meridian, so there is a longitude rotation to take in account for X and Y axes.
         */
        transform = operation.getMathTransform();
        tolerance = LINEAR_TOLERANCE;
        verifyTransform(new double[] {4205.669137,     6.491944,   4778.904260},    // Paris prime meridian
                        new double[] {4201737.725,   177938.072,   4779224.260});   // Greenwich prime meridian
        validate();
    }

    /**
     * Tests conversion from a geographic to a projected CRS without datum of axis changes.
     *
     * @throws ParseException if a CRS used in this test can not be parsed.
     * @throws FactoryException if the operation can not be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    @DependsOnMethod("testIdentityTransform")
    public void testGeographicToProjected() throws ParseException, FactoryException, TransformException {
        final CoordinateReferenceSystem sourceCRS = parse("$Sphere");
        final CoordinateReferenceSystem targetCRS = parse(
                "ProjectedCRS[“TM”,\n" +
                "  $Sphere,\n" +
                "  Conversion[“TM”,\n" +
                "    Method[“Transverse Mercator”],\n" +
                "    Parameter[“Longitude of natural origin”, 170],\n" +
                "    Parameter[“Latitude of natural origin”, 50],\n" +
                "    Parameter[“Scale factor at natural origin”, 0.95]],\n" +
                "  CS[Cartesian, 2],\n" +
                "    Axis[“x”, EAST],\n" +
                "    Axis[“y”, NORTH],\n" +
                "    Unit[“US survey foot”, 0.304800609601219]]");

        final CoordinateOperation operation = inference.createOperation(sourceCRS, targetCRS);
        assertSame      ("sourceCRS", sourceCRS,        operation.getSourceCRS());
        assertSame      ("targetCRS", targetCRS,        operation.getTargetCRS());
        assertEquals    ("name",      "TM",             operation.getName().getCode());
        assertInstanceOf("operation", Projection.class, operation);

        final ParameterValueGroup param = ((SingleOperation) operation).getParameterValues();
        assertEquals("semi_major",     6370997, param.parameter("semi_major"        ).doubleValue(), STRICT);
        assertEquals("semi_minor",     6370997, param.parameter("semi_minor"        ).doubleValue(), STRICT);
        assertEquals("latitude_of_origin",  50, param.parameter("latitude_of_origin").doubleValue(), STRICT);
        assertEquals("central_meridian",   170, param.parameter("central_meridian"  ).doubleValue(), STRICT);
        assertEquals("scale_factor",      0.95, param.parameter("scale_factor"      ).doubleValue(), STRICT);
        assertEquals("false_easting",        0, param.parameter("false_easting"     ).doubleValue(), STRICT);
        assertEquals("false_northing",       0, param.parameter("false_northing"    ).doubleValue(), STRICT);

        transform = operation.getMathTransform();
        tolerance = ANGULAR_TOLERANCE;
        verifyTransform(new double[] {170, 50}, new double[] {0, 0});
        validate();

        transform  = transform.inverse();
        tolerance  = LINEAR_TOLERANCE;
        λDimension = new int[] {0};
        verifyTransform(new double[] {0, 0}, new double[] {170, 50});
        validate();
    }

    /**
     * Tests that an exception is thrown on attempt to grab a transformation between incompatible vertical CRS.
     *
     * @throws FactoryException if an exception other than the expected one occurred.
     */
    @Test
    @DependsOnMethod("testIdentityTransform")
    public void testIncompatibleVerticalCRS() throws FactoryException {
        final VerticalCRS sourceCRS = CommonCRS.Vertical.NAVD88.crs();
        final VerticalCRS targetCRS = CommonCRS.Vertical.MEAN_SEA_LEVEL.crs();
        try {
            inference.createOperation(sourceCRS, targetCRS);
            fail("The operation should have failed.");
        } catch (OperationNotFoundException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("North American Vertical Datum"));
            assertTrue(message, message.contains("Mean Sea Level"));
        }
    }

    /**
     * Tests a conversion of the temporal axis. We convert 1899-12-31 from a CRS having its epoch at 1970-1-1
     * to an other CRS having its epoch at 1858-11-17, so the new value shall be approximatively 41 years
     * after the new epoch. This conversion also implies a change of units from seconds to days.
     *
     * @throws FactoryException if the operation can not be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    @DependsOnMethod("testIdentityTransform")
    public void testTemporalConversion() throws FactoryException, TransformException {
        final TemporalCRS sourceCRS = CommonCRS.Temporal.UNIX.crs();
        final TemporalCRS targetCRS = CommonCRS.Temporal.MODIFIED_JULIAN.crs();
        final CoordinateOperation operation = inference.createOperation(sourceCRS, targetCRS);
        assertSame      ("sourceCRS", sourceCRS,        operation.getSourceCRS());
        assertSame      ("targetCRS", targetCRS,        operation.getTargetCRS());
        assertEquals    ("name",      "Axis changes",   operation.getName().getCode());
        assertInstanceOf("operation", Conversion.class, operation);

        transform = operation.getMathTransform();
        tolerance = 1E-12;
        verifyTransform(new double[] {
            // December 31, 1899 at 12:00 UTC in seconds.
            CommonCRS.Temporal.DUBLIN_JULIAN.datum().getOrigin().getTime() / 1000
        }, new double[] {
            15019.5
        });
        validate();
    }




    //////////////////////////////////////////////////////////////////////////////////
    ////////////                                                          ////////////
    ////////////        Tests that change the number of dimensions        ////////////
    ////////////                                                          ////////////
    //////////////////////////////////////////////////////////////////////////////////

    /**
     * Tests the conversion from a four-dimensional geographic CRS to a two-dimensional geographic CRS.
     * The vertical and temporal dimensions are simply dropped.
     *
     * @throws FactoryException if the operation can not be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    @DependsOnMethod("testGeographic3D_to_2D")
    public void testGeographic4D_to_2D() throws FactoryException, TransformException {
        // NOTE: make sure that the 'sourceCRS' below is not equal to any other 'sourceCRS' created in this class.
        final CompoundCRS   sourceCRS = compound("Test4D", CommonCRS.WGS84.geographic3D(), CommonCRS.Temporal.UNIX.crs());
        final GeographicCRS targetCRS = CommonCRS.WGS84.geographic();
        final CoordinateOperation operation = inference.createOperation(sourceCRS, targetCRS);
        assertSame      ("sourceCRS", sourceCRS,        operation.getSourceCRS());
        assertSame      ("targetCRS", targetCRS,        operation.getTargetCRS());

        transform = operation.getMathTransform();
        assertInstanceOf("transform", LinearTransform.class, transform);
        assertEquals("sourceDimensions", 4, transform.getSourceDimensions());
        assertEquals("targetDimensions", 2, transform.getTargetDimensions());
        Assert.assertMatrixEquals("transform.matrix", Matrices.create(3, 5, new double[] {
            1, 0, 0, 0, 0,
            0, 1, 0, 0, 0,
            0, 0, 0, 0, 1
        }), ((LinearTransform) transform).getMatrix(), STRICT);

        isInverseTransformSupported = false;
        verifyTransform(new double[] {
            30, 10,  20, 1000,
            20, 30, -10, 3000
        }, new double[] {
            30, 10,
            20, 30
        });
        validate();
    }

    /**
     * Tests the conversion from a three-dimensional geographic CRS to a two-dimensional geographic CRS.
     * The vertical dimension is simply dropped.
     *
     * @throws FactoryException if the operation can not be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    @DependsOnMethod("testIdentityTransform")
    public void testGeographic3D_to_2D() throws FactoryException, TransformException {
        final GeographicCRS sourceCRS = CommonCRS.WGS84.geographic3D();
        final GeographicCRS targetCRS = CommonCRS.WGS84.geographic();
        final CoordinateOperation operation = inference.createOperation(sourceCRS, targetCRS);
        assertSame      ("sourceCRS", sourceCRS,        operation.getSourceCRS());
        assertSame      ("targetCRS", targetCRS,        operation.getTargetCRS());
        assertEquals    ("name",      "Axis changes",   operation.getName().getCode());
        assertInstanceOf("operation", Conversion.class, operation);

        final ParameterValueGroup parameters = ((SingleOperation) operation).getParameterValues();
        assertEquals("parameters.descriptor", "Geographic3D to 2D conversion", parameters.getDescriptor().getName().getCode());
        assertTrue  ("parameters.isEmpty", parameters.values().isEmpty());

        transform = operation.getMathTransform();
        assertInstanceOf("transform", LinearTransform.class, transform);
        assertEquals("sourceDimensions", 3, transform.getSourceDimensions());
        assertEquals("targetDimensions", 2, transform.getTargetDimensions());
        Assert.assertMatrixEquals("transform.matrix", Matrices.create(3, 4, new double[] {
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 0, 1
        }), ((LinearTransform) transform).getMatrix(), STRICT);

        isInverseTransformSupported = false;
        verifyTransform(new double[] {
            30, 10,  20,
            20, 30, -10
        }, new double[] {
            30, 10,
            20, 30
        });
        validate();
    }

    /**
     * Tests the conversion from a two-dimensional geographic CRS to a three-dimensional geographic CRS.
     * Ordinate values of the vertical dimension should be set to zero.
     *
     * @throws FactoryException if the operation can not be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    @DependsOnMethod("testGeographic3D_to_2D")
    public void testGeographic2D_to_3D() throws FactoryException, TransformException {
        final GeographicCRS sourceCRS = CommonCRS.WGS84.geographic();
        final GeographicCRS targetCRS = CommonCRS.WGS84.geographic3D();
        final CoordinateOperation operation = inference.createOperation(sourceCRS, targetCRS);
        assertSame      ("sourceCRS", sourceCRS,        operation.getSourceCRS());
        assertSame      ("targetCRS", targetCRS,        operation.getTargetCRS());
        assertEquals    ("name",      "Axis changes",   operation.getName().getCode());
        assertInstanceOf("operation", Conversion.class, operation);

        final ParameterValueGroup parameters = ((SingleOperation) operation).getParameterValues();
        assertEquals("parameters.descriptor", "Geographic2D to 3D conversion", parameters.getDescriptor().getName().getCode());
        assertEquals("parameters.height", 0, parameters.parameter("height").doubleValue(), STRICT);

        transform = operation.getMathTransform();
        assertInstanceOf("transform", LinearTransform.class, transform);
        assertEquals("sourceDimensions", 2, transform.getSourceDimensions());
        assertEquals("targetDimensions", 3, transform.getTargetDimensions());
        Assert.assertMatrixEquals("transform.matrix", Matrices.create(4, 3, new double[] {
            1, 0, 0,
            0, 1, 0,
            0, 0, 0,
            0, 0, 1
        }), ((LinearTransform) transform).getMatrix(), STRICT);

        verifyTransform(new double[] {
            30, 10,
            20, 30
        }, new double[] {
            30, 10, 0,
            20, 30, 0
        });
        validate();
    }

    /**
     * Tests transformation from a tree-dimensional geographic CRS to an ellipsoidal CRS.
     * Such vertical CRS are illegal according ISO 19111, but they are the easiest test
     * that we can perform for geographic → vertical transformation.
     *
     * @throws FactoryException if the operation can not be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    @DependsOnMethod("testIdentityTransform")
    public void testGeographic3D_to_EllipsoidalHeight() throws FactoryException, TransformException {
        final CoordinateReferenceSystem sourceCRS = CommonCRS.WGS84.geographic3D();
        final CoordinateReferenceSystem targetCRS = HardCodedCRS.ELLIPSOIDAL_HEIGHT_cm;
        final CoordinateOperation operation = inference.createOperation(sourceCRS, targetCRS);
        assertSame      ("sourceCRS", sourceCRS,        operation.getSourceCRS());
        assertSame      ("targetCRS", targetCRS,        operation.getTargetCRS());
        assertEquals    ("name",      "Axis changes",   operation.getName().getCode());
        assertInstanceOf("operation", Conversion.class, operation);

        transform = operation.getMathTransform();
        assertInstanceOf("transform", LinearTransform.class, transform);
        assertEquals("sourceDimensions", 3, transform.getSourceDimensions());
        assertEquals("targetDimensions", 1, transform.getTargetDimensions());
        Assert.assertMatrixEquals("transform.matrix", Matrices.create(2, 4, new double[] {
            0, 0, 100, 0,
            0, 0,   0, 1
        }), ((LinearTransform) transform).getMatrix(), STRICT);

        isInverseTransformSupported = false;
        verifyTransform(new double[] {
             0,  0,  0,
             5,  8, 20,
            -5, -8, 24
        }, new double[] {
                     0,
                  2000,
                  2400,
        });
        validate();
    }

    /**
     * Tests extracting the vertical part of a spatio-temporal CRS.
     *
     * @throws FactoryException if the operation can not be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    @DependsOnMethod("testGeographic3D_to_EllipsoidalHeight")
    public void testGeographic4D_to_EllipsoidalHeight() throws FactoryException, TransformException {
        // NOTE: make sure that the 'sourceCRS' below is not equal to any other 'sourceCRS' created in this class.
        final CompoundCRS sourceCRS = compound("Test4D", CommonCRS.WGS84.geographic3D(), CommonCRS.Temporal.JULIAN.crs());
        final VerticalCRS targetCRS = CommonCRS.Vertical.ELLIPSOIDAL.crs();
        final CoordinateOperation operation = inference.createOperation(sourceCRS, targetCRS);
        assertSame      ("sourceCRS", sourceCRS,        operation.getSourceCRS());
        assertSame      ("targetCRS", targetCRS,        operation.getTargetCRS());
        assertEquals    ("name",      "Axis changes",   operation.getName().getCode());
        assertInstanceOf("operation", Conversion.class, operation);

        transform = operation.getMathTransform();
        assertInstanceOf("transform", LinearTransform.class, transform);
        assertEquals("sourceDimensions", 4, transform.getSourceDimensions());
        assertEquals("targetDimensions", 1, transform.getTargetDimensions());
        Assert.assertMatrixEquals("transform.matrix", Matrices.create(2, 5, new double[] {
            0, 0, 1, 0, 0,
            0, 0, 0, 0, 1
        }), ((LinearTransform) transform).getMatrix(), STRICT);

        isInverseTransformSupported = false;
        verifyTransform(new double[] {
             0,  0,  0,  0,
             5,  8, 20, 10,
            -5, -8, 24, 30
        }, new double[] {
                     0,
                    20,
                    24,
        });
        validate();
    }

    /**
     * Convenience method for creating a compound CRS.
     */
    private static CompoundCRS compound(final String name, final CoordinateReferenceSystem... components) {
        return new DefaultCompoundCRS(Collections.singletonMap(CompoundCRS.NAME_KEY, name), components);
    }

    /**
     * Tests conversion from four-dimensional compound CRS to two-dimensional projected CRS.
     *
     * @throws ParseException if a CRS used in this test can not be parsed.
     * @throws FactoryException if the operation can not be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    @DependsOnMethod("testTemporalConversion")
    public void testProjected4D_to_2D() throws ParseException, FactoryException, TransformException {
        final CoordinateReferenceSystem targetCRS = parse(
                "ProjectedCRS[“WGS 84 / World Mercator”,\n" +
                "  BaseGeodCRS[“WGS 84”,\n" +
                "    Datum[“World Geodetic System 1984”,\n" +
                "      Ellipsoid[“WGS 84”, 6378137.0, 298.257223563]]],\n" +
                "  Conversion[“WGS 84 / World Mercator”,\n" +
                "    Method[“Mercator (1SP)”]],\n" +
                "  CS[Cartesian, 2],\n" +
                "    Axis[“Easting”, EAST],\n" +
                "    Axis[“Northing”, NORTH],\n" +
                "    Unit[“m”, 1],\n" +
                "  Id[“EPSG”, “3395”]]");

        CoordinateReferenceSystem sourceCRS = targetCRS;
        sourceCRS = compound("Mercator 3D", sourceCRS, CommonCRS.Vertical.ELLIPSOIDAL.crs());
        sourceCRS = compound("Mercator 4D", sourceCRS, CommonCRS.Temporal.MODIFIED_JULIAN.crs());

        final CoordinateOperation operation = inference.createOperation(sourceCRS, targetCRS);
        assertSame("sourceCRS", sourceCRS, operation.getSourceCRS());
        assertSame("targetCRS", targetCRS, operation.getTargetCRS());

        transform = operation.getMathTransform();
        assertFalse("transform.isIdentity", transform.isIdentity());
        assertInstanceOf("The somewhat complex MathTransform chain should have been simplified " +
                         "to a single affine transform.", LinearTransform.class, transform);
        assertInstanceOf("The operation should be a simple axis change, not a complex" +
                         "chain of ConcatenatedOperations.", Conversion.class, operation);

        assertEquals("sourceDimensions", 4, transform.getSourceDimensions());
        assertEquals("targetDimensions", 2, transform.getTargetDimensions());
        Assert.assertMatrixEquals("transform.matrix", Matrices.create(3, 5, new double[] {
            1, 0, 0, 0, 0,
            0, 1, 0, 0, 0,
            0, 0, 0, 0, 1
        }), ((LinearTransform) transform).getMatrix(), STRICT);

        isInverseTransformSupported = false;
        verifyTransform(new double[] {
               0,     0,  0,    0,
            1000, -2000, 20, 4000
        }, new double[] {
               0,     0,
            1000, -2000
        });
        validate();
    }

    /**
     * Tests conversion from three-dimensional geographic CRS to four-dimensional compound CRS
     * where the last dimension is time.
     *
     * @throws FactoryException if the operation can not be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    @DependsOnMethod("testTemporalConversion")
    public void testGeographic3D_to_4D() throws FactoryException, TransformException {
        // NOTE: make sure that the 'sourceCRS' below is not equal to any other 'sourceCRS' created in this class.
        final CompoundCRS sourceCRS = compound("Test3D", CommonCRS.WGS84.geographic(),   CommonCRS.Temporal.UNIX.crs());
        final CompoundCRS targetCRS = compound("Test4D", CommonCRS.WGS84.geographic3D(), CommonCRS.Temporal.MODIFIED_JULIAN.crs());
        final CoordinateOperation operation = inference.createOperation(sourceCRS, targetCRS);
        assertSame      ("sourceCRS", sourceCRS, operation.getSourceCRS());
        assertSame      ("targetCRS", targetCRS, operation.getTargetCRS());
        assertInstanceOf("operation", ConcatenatedOperation.class, operation);
        assertEquals    ("name", "CompoundCRS[“Test3D”] → CompoundCRS[“Test4D”]", operation.getName().getCode());

        transform = operation.getMathTransform();
        assertInstanceOf("transform", LinearTransform.class, transform);
        assertEquals("sourceDimensions", 3, transform.getSourceDimensions());
        assertEquals("targetDimensions", 4, transform.getTargetDimensions());
        Assert.assertMatrixEquals("transform.matrix", Matrices.create(5, 4, new double[] {
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 0, 0,
            0, 0, 1./(24*60*60), 40587,
            0, 0, 0, 1
        }), ((LinearTransform) transform).getMatrix(), 1E-12);

        tolerance = 1E-12;
        verifyTransform(new double[] {
            -5, -8, CommonCRS.Temporal.DUBLIN_JULIAN.datum().getOrigin().getTime() / 1000
        }, new double[] {
            -5, -8, 0, 15019.5              // Same value than in testTemporalConversion().
        });
        validate();
    }

    /**
     * Tests conversion from spatio-temporal CRS to a derived CRS.
     *
     * @throws FactoryException if the operation can not be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    @DependsOnMethod("testProjected4D_to_2D")
    public void testSpatioTemporalToDerived() throws FactoryException, TransformException {
        final Map<String,Object> properties = new HashMap<String,Object>();
        properties.put(DerivedCRS.NAME_KEY, "Display");
        properties.put("conversion.name", "Display to WGS84");

        final GeographicCRS WGS84     = CommonCRS.WGS84.normalizedGeographic();
        final CompoundCRS   sourceCRS = compound("Test3D", WGS84, CommonCRS.Temporal.UNIX.crs());
        final DerivedCRS    targetCRS = DefaultDerivedCRS.create(properties,
                WGS84, null, factory.getOperationMethod("Affine"),
                MathTransforms.linear(Matrices.create(3, 3, new double[] {
                    12,  0, 480,
                    0, -12, 790,
                    0,   0,   1
                })), HardCodedCS.DISPLAY);

        final CoordinateOperation operation = inference.createOperation(sourceCRS, targetCRS);
        assertSame("sourceCRS", sourceCRS, operation.getSourceCRS());
        assertSame("targetCRS", targetCRS, operation.getTargetCRS());

        transform = operation.getMathTransform();
        assertInstanceOf("transform", LinearTransform.class, transform);
        assertEquals("sourceDimensions", 3, transform.getSourceDimensions());
        assertEquals("targetDimensions", 2, transform.getTargetDimensions());
        Assert.assertMatrixEquals("transform.matrix", Matrices.create(3, 4, new double[] {
            12,  0,  0, 480,
            0, -12,  0, 790,
            0,   0,  0,   1
        }), ((LinearTransform) transform).getMatrix(), STRICT);
        validate();
    }
}
