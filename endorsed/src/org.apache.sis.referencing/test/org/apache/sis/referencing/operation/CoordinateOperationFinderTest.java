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

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.text.ParseException;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.crs.DerivedCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.Transformation;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.ConcatenatedOperation;
import org.opengis.referencing.operation.OperationNotFoundException;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.internal.PositionalAccuracyConstant;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.cs.DefaultCartesianCS;
import org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis;
import org.apache.sis.referencing.datum.DefaultEngineeringDatum;
import org.apache.sis.referencing.crs.DefaultEngineeringCRS;
import org.apache.sis.referencing.crs.DefaultCompoundCRS;
import org.apache.sis.referencing.crs.DefaultDerivedCRS;
import org.apache.sis.io.wkt.WKTFormat;
import org.apache.sis.measure.Units;
import static org.apache.sis.util.privy.Constants.SECONDS_PER_DAY;
import static org.apache.sis.referencing.privy.Formulas.LINEAR_TOLERANCE;
import static org.apache.sis.referencing.privy.Formulas.ANGULAR_TOLERANCE;
import static org.apache.sis.referencing.internal.PositionalAccuracyConstant.DATUM_SHIFT_APPLIED;

// Test dependencies
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.apache.sis.referencing.operation.transform.MathTransformTestCase;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.referencing.cs.HardCodedCS;
import org.apache.sis.referencing.crs.HardCodedCRS;
import static org.apache.sis.test.Assertions.assertMessageContains;
import static org.apache.sis.test.Assertions.assertSetEquals;
import static org.apache.sis.test.TestCase.STRICT;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.test.Assertions;


/**
 * Tests {@link CoordinateOperationFinder}.
 * Contrarily to {@link CoordinateOperationRegistryTest}, tests in this class are run without EPSG geodetic dataset.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@Execution(ExecutionMode.SAME_THREAD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class CoordinateOperationFinderTest extends MathTransformTestCase {
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
    public CoordinateOperationFinderTest() throws ParseException {
        factory = new DefaultCoordinateOperationFactory();
        parser  = new WKTFormat();
        /*
         * The first keyword in WKT below should be "GeodeticCRS" in WKT 2, but we use the WKT 1 keyword ("GEOGCS")
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
     * Resets all fields that may be modified by test methods in this class.
     * This is needed because we reuse the same instance for all methods,
     * in order to reuse the factory and parser created in the constructor.
     */
    @Override
    @BeforeEach
    public void reset() {
        super.reset();
        isInverseTransformSupported = true;
    }

    /**
     * Returns the instance on which to execute the tests.
     *
     * @throws FactoryException if an error occurred while initializing the finder to test.
     */
    private CoordinateOperationFinder finder() throws FactoryException {
        return new CoordinateOperationFinder(null, factory, null);
    }

    /**
     * Returns the CRS for the given Well Known Text.
     */
    private CoordinateReferenceSystem parse(final String wkt) throws ParseException {
        return (CoordinateReferenceSystem) parser.parseObject(wkt);
    }

    /**
     * Verifies that the current transform is a linear transform with a matrix equals to the given one.
     */
    private void assertMatrixEquals(final Matrix expected) {
        Assertions.assertMatrixEquals(expected,
                assertInstanceOf(LinearTransform.class, transform).getMatrix(),
                STRICT, "transform.matrix");
    }

    /**
     * Makes sure that {@code createOperation(sourceCRS, targetCRS)} returns an identity transform
     * when {@code sourceCRS} and {@code targetCRS} are identical.
     *
     * @throws FactoryException if the operation cannot be created.
     */
    @Test
    public void testIdentityTransform() throws FactoryException {
        testIdentityTransform(CommonCRS.WGS84.geographic());
        testIdentityTransform(CommonCRS.WGS84.geographic3D());
        testIdentityTransform(CommonCRS.WGS84.geocentric());
        testIdentityTransform(CommonCRS.WGS84.spherical());
        testIdentityTransform(CommonCRS.WGS84.universal(0, 0));
        testIdentityTransform(CommonCRS.Vertical.DEPTH.crs());
        testIdentityTransform(CommonCRS.Temporal.JULIAN.crs());
    }

    /**
     * Implementation of {@link #testIdentityTransform()} using the given CRS.
     */
    private void testIdentityTransform(final CoordinateReferenceSystem crs) throws FactoryException {
        final CoordinateOperation operation = finder().createOperation(crs, crs);
        assertSame(crs, operation.getSourceCRS());
        assertSame(crs, operation.getTargetCRS());
        assertTrue(operation.getMathTransform().isIdentity());
        assertTrue(operation.getCoordinateOperationAccuracy().isEmpty());
        assertInstanceOf(Conversion.class, operation);
    }

    /**
     * Tests a transformation with a two-dimensional geographic source CRS.
     * This method verifies with both a two-dimensional and a three-dimensional target CRS.
     *
     * @throws ParseException if a CRS used in this test cannot be parsed.
     * @throws FactoryException if the operation cannot be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    public void testGeocentricTranslationInGeographic2D() throws ParseException, FactoryException, TransformException {
        /*
         * NAD27 (EPSG:4267) defined in WKT instead of relying on the CommonCRS.NAD27 constant in order to fix
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
     * @throws ParseException if a CRS used in this test cannot be parsed.
     * @throws FactoryException if the operation cannot be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
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
     * @param  sourceCRS  the NAD27 geographic CRS.
     * @param  targetCRS  either the two-dimensional or the three-dimensional geographic CRS using WGS84 datum.
     */
    private void testGeocentricTranslationInGeographicDomain(final String method,
            final GeographicCRS sourceCRS, final GeographicCRS targetCRS)
            throws ParseException, FactoryException, TransformException
    {
        final CoordinateOperation operation = finder().createOperation(sourceCRS, targetCRS);
        assertSame(sourceCRS, operation.getSourceCRS());
        assertSame(targetCRS, operation.getTargetCRS());
        assertFalse(operation.getMathTransform().isIdentity());
        assertEquals("Datum shift", operation.getName().getCode());
        assertSetEquals(Set.of(DATUM_SHIFT_APPLIED), operation.getCoordinateOperationAccuracy());
        assertInstanceOf(Transformation.class, operation);
        assertEquals(method, ((SingleOperation) operation).getMethod().getName().getCode());

        transform  = operation.getMathTransform();
        tolerance  = ANGULAR_TOLERANCE;
        zTolerance = 0.01;
        λDimension = new int[] {1};
        zDimension = new int[] {2};
        double[] source = {
            39.00,  -85.00,  -10000.00,   // The intent of those large height values is to cause a shift in (φ,λ)
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
        verifyTransform(source, target);
        validate();
    }

    /**
     * Tests a transformation using the <q>Geocentric translations (geog2D domain)</q> method
     * together with a longitude rotation and unit conversion. The CRS and sample point are taken from
     * the GR3DF97A – <cite>Grille de paramètres de transformation de coordonnées</cite> document.
     *
     * @throws ParseException if a CRS used in this test cannot be parsed.
     * @throws FactoryException if the operation cannot be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    public void testLongitudeRotation() throws ParseException, FactoryException, TransformException {
        final CoordinateReferenceSystem sourceCRS = parse(
                "GeodeticCRS[“NTF (Paris)”, $NTF,\n" +
                "  PrimeMeridian[“Paris”, 2.5969213],\n" +          // in grads, not degrees.
                "  CS[ellipsoidal, 2],\n" +
                "    Axis[“Latitude (φ)”, NORTH],\n" +
                "    Axis[“Longitude (λ)”, EAST],\n" +
                "    Unit[“grad”, 0.015707963267949],\n" +
                "  Id[“EPSG”, “4807”]]");

        final GeographicCRS       targetCRS = CommonCRS.WGS84.geographic();
        final CoordinateOperation operation = finder().createOperation(sourceCRS, targetCRS);

        assertSame(sourceCRS, operation.getSourceCRS());
        assertSame(targetCRS, operation.getTargetCRS());
        assertFalse(operation.getMathTransform().isIdentity());
        assertEquals("Datum shift", operation.getName().getCode());
        assertSetEquals(Set.of(DATUM_SHIFT_APPLIED), operation.getCoordinateOperationAccuracy());
        assertInstanceOf(Transformation.class, operation);
        assertEquals("Geocentric translations (geog2D domain)",
                ((SingleOperation) operation).getMethod().getName().getCode());
        /*
         * Same test point as the one used in FranceGeocentricInterpolationTest:
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
     * Tests a transformation using the <q>Geocentric translations (geocentric domain)</q> method,
     * together with a longitude rotation and unit conversion. The CRS and sample point are derived from
     * the GR3DF97A – <cite>Grille de paramètres de transformation de coordonnées</cite> document.
     *
     * @throws ParseException if a CRS used in this test cannot be parsed.
     * @throws FactoryException if the operation cannot be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    public void testGeocentricTranslationInGeocentricDomain() throws ParseException, FactoryException, TransformException {
        final CoordinateReferenceSystem sourceCRS = parse(
                "GeodeticCRS[“NTF (Paris)”, $NTF,\n" +
                "  PrimeMeridian[“Paris”, 2.33722917],\n" +         // in degrees.
                "  CS[Cartesian, 3],\n" +
                "    Axis[“(X)”, geocentricX],\n" +
                "    Axis[“(Y)”, geocentricY],\n" +
                "    Axis[“(Z)”, geocentricZ],\n" +
                "    Unit[“kilometre”, 1000]]");

        final GeodeticCRS         targetCRS = CommonCRS.WGS84.geocentric();
        final CoordinateOperation operation = finder().createOperation(sourceCRS, targetCRS);

        assertSame(sourceCRS, operation.getSourceCRS());
        assertSame(targetCRS, operation.getTargetCRS());
        assertFalse(operation.getMathTransform().isIdentity());
        assertEquals("Datum shift", operation.getName().getCode());
        assertSetEquals(Set.of(DATUM_SHIFT_APPLIED), operation.getCoordinateOperationAccuracy());
        assertInstanceOf(Transformation.class,  operation);
        assertEquals("Geocentric translations (geocentric domain)",
                ((SingleOperation) operation).getMethod().getName().getCode());
        /*
         * Same test point as the one used in FranceGeocentricInterpolationTest:
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
     * Tests conversion from geographic to geocentric coordinate reference system and conversely.
     * Both two-dimensional and three-dimensional cases are tested.
     *
     * @throws FactoryException if the operation cannot be created.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-376">SIS-376</a>
     */
    @Test
    public void testGeocentricConversions() throws FactoryException {
        testGeocentricConversion(HardCodedCRS.WGS84_3D,   HardCodedCRS.GEOCENTRIC);
        testGeocentricConversion(HardCodedCRS.WGS84,      HardCodedCRS.GEOCENTRIC);
        testGeocentricConversion(HardCodedCRS.GEOCENTRIC, HardCodedCRS.WGS84_3D);
        testGeocentricConversion(HardCodedCRS.GEOCENTRIC, HardCodedCRS.WGS84);
    }

    /**
     * Tests a single case of Geographic ↔︎ Geocentric conversions.
     */
    private void testGeocentricConversion(final CoordinateReferenceSystem sourceCRS,
                                          final CoordinateReferenceSystem targetCRS)
            throws FactoryException
    {
        final CoordinateOperation operation = finder().createOperation(sourceCRS, targetCRS);
        assertSame(sourceCRS, operation.getSourceCRS());
        assertSame(targetCRS, operation.getTargetCRS());
        assertEquals("Geocentric conversion", operation.getName().getCode());
        assertInstanceOf(Conversion.class, operation);
    }

    /**
     * Tests conversion from a geographic to a projected CRS without datum of axis changes.
     *
     * @throws ParseException if a CRS used in this test cannot be parsed.
     * @throws FactoryException if the operation cannot be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
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

        final CoordinateOperation operation = finder().createOperation(sourceCRS, targetCRS);
        assertSame(sourceCRS, operation.getSourceCRS());
        assertSame(targetCRS, operation.getTargetCRS());
        assertEquals("TM", operation.getName().getCode());
        assertInstanceOf(Conversion.class, operation);

        final ParameterValueGroup param = ((SingleOperation) operation).getParameterValues();
        assertEquals(6370997, param.parameter("semi_major"        ).doubleValue());
        assertEquals(6370997, param.parameter("semi_minor"        ).doubleValue());
        assertEquals(     50, param.parameter("latitude_of_origin").doubleValue());
        assertEquals(    170, param.parameter("central_meridian"  ).doubleValue());
        assertEquals(   0.95, param.parameter("scale_factor"      ).doubleValue());
        assertEquals(      0, param.parameter("false_easting"     ).doubleValue());
        assertEquals(      0, param.parameter("false_northing"    ).doubleValue());

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
     * Tests a datum shift applied as a position vector transformation in geocentric domain.
     * This test does not use the EPSG geodetic dataset.
     *
     * @throws ParseException if a CRS used in this test cannot be parsed.
     * @throws FactoryException if the operation cannot be created.
     * @throws TransformException if an error occurred while converting the test points.
     *
     * @see DefaultCoordinateOperationFactoryTest#testPositionVectorTransformation()
     * @see <a href="https://issues.apache.org/jira/browse/SIS-364">SIS-364</a>
     */
    @Test
    public void testPositionVectorTransformation() throws ParseException, FactoryException, TransformException {
        final CoordinateReferenceSystem sourceCRS = CommonCRS.WGS84.geographic();
        final CoordinateReferenceSystem targetCRS = parse(AGD66());
        final CoordinateOperation operation = finder().createOperation(sourceCRS, targetCRS);
        transform  = operation.getMathTransform();
        tolerance  = LINEAR_TOLERANCE;
        λDimension = new int[] {0};
        verifyTransform(expectedAGD66(true), expectedAGD66(false));
        validate();
    }

    /**
     * Returns test coordinates for a transformation between {@link #AGD66()} and WGS84.
     * We use this method for ensuring that {@link #testPositionVectorTransformation()}
     * and {@link DefaultCoordinateOperationFactoryTest#testPositionVectorTransformation()}
     * use the same data, as specified in {@link #AGD66()} contract.
     *
     * @param  WGS84  {@code true} for the WGS84 input, or {@code false} for the AGD66 output.
     */
    static double[] expectedAGD66(final boolean WGS84) {
        return WGS84 ? new double[] {-37.84, 114.0} : new double[] {763850.64, 5807560.94};
    }

    /**
     * Returns the WKT for a CRS using the Australian Geodetic Datum 1966. This method returns a WKT 1 string
     * with a {@code TOWGS84} element that should help Apache SIS to produce the same result regardless if an
     * EPSG geodetic dataset is used or not.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-364">SIS-364</a>
     */
    static String AGD66() {
        return "PROJCS[“AGD66 / AMG zone 49”, "
                + "GEOGCS[“AGD66”, "
                +   "DATUM[“Australian_Geodetic_Datum_1966”, "
                +     "SPHEROID[“Australian National Spheroid”,6378160, 298.25, AUTHORITY[“EPSG”,“7003”]],"
                +     "TOWGS84[-117.808,-51.536,137.784,0.303,0.446,0.234,-0.29], AUTHORITY[“EPSG”,“6202”]],"
                +     "PRIMEM[“Greenwich”, 0, AUTHORITY[“EPSG”,“8901”]],"
                +     "UNIT[“degree”, 0.0174532925199433, AUTHORITY[“EPSG”,“9122”]],"
                +   "AUTHORITY[“EPSG”,“4202”]],"
                +   "PROJECTION[“Transverse_Mercator”],"
                +   "PARAMETER[“latitude_of_origin”, 0],"
                +   "PARAMETER[“central_meridian”, 111],"
                +   "PARAMETER[“scale_factor”, 0.9996],"
                +   "PARAMETER[“false_easting”, 500000],"
                +   "PARAMETER[“false_northing”, 10000000],"
                +   "UNIT[“metre”,1,AUTHORITY[“EPSG”,“9001”]],"
                +   "AXIS[“Easting”,EAST],"
                +   "AXIS[“Northing”,NORTH],"
                + "AUTHORITY[“EPSG”,“20249”]]";
    }

    /**
     * Tests a transformation between two CRS for which no direct bursa-wolf parameters are defined.
     * However, a transformation should still be possible indirectly, through WGS 84.
     *
     * @throws ParseException if a CRS used in this test cannot be parsed.
     * @throws FactoryException if the operation cannot be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    public void testIndirectDatumShift() throws ParseException, FactoryException, TransformException {
        final CoordinateReferenceSystem sourceCRS = parse(
                "PROJCS[“RGF93 / Lambert-93”, "
                + "GEOGCS[“RGF93”, "
                +   "DATUM[“Reseau Geodesique Francais 1993”, "
                +     "SPHEROID[“GRS 1980”, 6378137, 298.257222101], "
                +     "TOWGS84[0,0,0,0,0,0,0]], "
                +     "PRIMEM[“Greenwich”,0], "
                +     "UNIT[“degree”, 0.0174532925199433]], "
                +   "PROJECTION[“Lambert_Conformal_Conic_2SP”], "
                +   "PARAMETER[“standard_parallel_1”, 49], "
                +   "PARAMETER[“standard_parallel_2”, 44], "
                +   "PARAMETER[“latitude_of_origin”, 46.5], "
                +   "PARAMETER[“central_meridian”, 3], "
                +   "PARAMETER[“false_easting”, 700000], "
                +   "PARAMETER[“false_northing”, 6600000], "
                +   "UNIT[“metre”,1], "
                +   "AUTHORITY[“EPSG”,“2154”]]");

        final CoordinateReferenceSystem targetCRS = parse(
                "PROJCS[“Amersfoort / RD New”, "
                + "GEOGCS[“Amersfoort”, "
                +   "DATUM[“Amersfoort”, "
                +     "SPHEROID[“Bessel 1841”, 6377397.155, 299.1528128], "
                +     "TOWGS84[565.417, 50.3319, 465.552, -0.398957, 0.343988, -1.8774, 4.0725]], "
                +     "PRIMEM[“Greenwich”,0], "
                +     "UNIT[“degree”,0.0174532925199433]], "
                +   "PROJECTION[“Oblique_Stereographic”], "
                +   "PARAMETER[“latitude_of_origin”, 52.15616055555555], "
                +   "PARAMETER[“central_meridian”, 5.38763888888889], "
                +   "PARAMETER[“scale_factor”, 0.9999079], "
                +   "PARAMETER[“false_easting”, 155000], "
                +   "PARAMETER[“false_northing”, 463000], "
                +   "UNIT[“metre”,1], "
                +   "AUTHORITY[“EPSG”,“28992”]]");
        /*
         * Transform a point as a way to verify that a datum shift is applied.
         * If no datum shift is applied, the point will be at 191 metres from
         * expected value.
         */
        final CoordinateOperation operation = finder().createOperation(sourceCRS, targetCRS);
        tolerance = LINEAR_TOLERANCE;
        transform = operation.getMathTransform();
        verifyTransform(new double[] {926713.702, 7348947.026},
                        new double[] {220798.684,  577583.801});        // With datum shift through WGS84.
                        //            220762.487,  577396.040           // Without datum shift.
        validate();
        /*
         * The accuracy should tell that the datum shift is indirect (through WGS 84).
         * However, the value may differ depending on whether EPSG database has been
         * used or not, because it depends on whether the datum have been completed
         * with domain of validity.
         */
        final double accuracy = CRS.getLinearAccuracy(operation);
        if (accuracy != PositionalAccuracyConstant.UNKNOWN_ACCURACY) {
            assertEquals(PositionalAccuracyConstant.INDIRECT_SHIFT_ACCURACY, accuracy);
        }
    }

    /**
     * Tests that an exception is thrown on attempt to grab a transformation between incompatible vertical CRS.
     *
     * @throws FactoryException if an exception other than the expected one occurred.
     */
    @Test
    public void testIncompatibleVerticalCRS() throws FactoryException {
        final VerticalCRS sourceCRS = CommonCRS.Vertical.NAVD88.crs();
        final VerticalCRS targetCRS = CommonCRS.Vertical.MEAN_SEA_LEVEL.crs();
        var e = assertThrows(OperationNotFoundException.class, () -> finder().createOperation(sourceCRS, targetCRS));
        assertMessageContains(e, "North American Vertical Datum", "Mean Sea Level");
    }

    /**
     * Tests a conversion of the temporal axis. We convert 1899-12-31 from a CRS having its epoch at 1970-1-1
     * to another CRS having its epoch at 1858-11-17, so the new value shall be approximately 41 years
     * after the new epoch. This conversion also implies a change of units from seconds to days.
     *
     * @throws FactoryException if the operation cannot be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    public void testTemporalConversion() throws FactoryException, TransformException {
        final TemporalCRS sourceCRS = CommonCRS.Temporal.UNIX.crs();
        final TemporalCRS targetCRS = CommonCRS.Temporal.MODIFIED_JULIAN.crs();
        final CoordinateOperation operation = finder().createOperation(sourceCRS, targetCRS);
        assertSame(sourceCRS, operation.getSourceCRS());
        assertSame(targetCRS, operation.getTargetCRS());
        assertEquals("Axis changes", operation.getName().getCode());
        assertInstanceOf(Conversion.class, operation);

        transform = operation.getMathTransform();
        tolerance = 2E-12;
        verifyTransform(new double[] {
            // December 31, 1899 at 12:00 UTC in seconds.
            CommonCRS.Temporal.DUBLIN_JULIAN.datum().getOrigin().getTime() / 1000
        }, new double[] {
            15019.5
        });
        validate();
    }




    //  ╔══════════════════════════════════════════════════════════╗
    //  ║                                                          ║
    //  ║        Tests that change the number of dimensions        ║
    //  ║                                                          ║
    //  ╚══════════════════════════════════════════════════════════╝

    /**
     * Tests the conversion from a four-dimensional geographic CRS to a two-dimensional geographic CRS.
     * The vertical and temporal dimensions are simply dropped.
     *
     * @throws FactoryException if the operation cannot be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    public void testGeographic4D_to_2D() throws FactoryException, TransformException {
        // NOTE: make sure that the 'sourceCRS' below is not equal to any other 'sourceCRS' created in this class.
        final CompoundCRS   sourceCRS = compound("Test4D", CommonCRS.WGS84.geographic3D(), CommonCRS.Temporal.UNIX.crs());
        final GeographicCRS targetCRS = CommonCRS.WGS84.geographic();
        final CoordinateOperation operation = finder().createOperation(sourceCRS, targetCRS);
        assertSame(sourceCRS, operation.getSourceCRS());
        assertSame(targetCRS, operation.getTargetCRS());

        transform = operation.getMathTransform();
        assertEquals(4, transform.getSourceDimensions());
        assertEquals(2, transform.getTargetDimensions());
        assertMatrixEquals(Matrices.create(3, 5, new double[] {
            1, 0, 0, 0, 0,
            0, 1, 0, 0, 0,
            0, 0, 0, 0, 1
        }));

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
     * @throws FactoryException if the operation cannot be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    public void testGeographic3D_to_2D() throws FactoryException, TransformException {
        final GeographicCRS sourceCRS = CommonCRS.WGS84.geographic3D();
        final GeographicCRS targetCRS = CommonCRS.WGS84.geographic();
        final CoordinateOperation operation = finder().createOperation(sourceCRS, targetCRS);
        assertSame(sourceCRS, operation.getSourceCRS());
        assertSame(targetCRS, operation.getTargetCRS());
        assertEquals("Axis changes", operation.getName().getCode());
        assertInstanceOf(Conversion.class, operation);

        final ParameterValueGroup parameters = ((SingleOperation) operation).getParameterValues();
        assertEquals("Geographic3D to 2D conversion", parameters.getDescriptor().getName().getCode());
        assertTrue(parameters.values().isEmpty());

        transform = operation.getMathTransform();
        assertEquals(3, transform.getSourceDimensions());
        assertEquals(2, transform.getTargetDimensions());
        assertMatrixEquals(Matrices.create(3, 4, new double[] {
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 0, 1
        }));

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
     * Coordinate values of the vertical dimension should be set to zero.
     *
     * @throws FactoryException if the operation cannot be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    public void testGeographic2D_to_3D() throws FactoryException, TransformException {
        final GeographicCRS sourceCRS = CommonCRS.WGS84.geographic();
        final GeographicCRS targetCRS = CommonCRS.WGS84.geographic3D();
        final CoordinateOperation operation = finder().createOperation(sourceCRS, targetCRS);
        assertSame(sourceCRS, operation.getSourceCRS());
        assertSame(targetCRS, operation.getTargetCRS());
        assertEquals("Axis changes", operation.getName().getCode());
        assertInstanceOf(Conversion.class, operation);

        final ParameterValueGroup parameters = ((SingleOperation) operation).getParameterValues();
        assertEquals("Geographic2D to 3D conversion", parameters.getDescriptor().getName().getCode());
        assertEquals(0, parameters.parameter("height").doubleValue());

        transform = operation.getMathTransform();
        assertEquals(2, transform.getSourceDimensions());
        assertEquals(3, transform.getTargetDimensions());
        assertMatrixEquals(Matrices.create(4, 3, new double[] {
            1, 0, 0,
            0, 1, 0,
            0, 0, 0,
            0, 0, 1
        }));

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
     * @throws FactoryException if the operation cannot be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    public void testGeographic3D_to_EllipsoidalHeight() throws FactoryException, TransformException {
        final CoordinateReferenceSystem sourceCRS = CommonCRS.WGS84.geographic3D();
        final CoordinateReferenceSystem targetCRS = HardCodedCRS.ELLIPSOIDAL_HEIGHT_cm;
        final CoordinateOperation operation = finder().createOperation(sourceCRS, targetCRS);
        assertSame(sourceCRS, operation.getSourceCRS());
        assertSame(targetCRS, operation.getTargetCRS());
        assertEquals("Axis changes", operation.getName().getCode());
        assertInstanceOf(Conversion.class, operation);

        transform = operation.getMathTransform();
        assertEquals(3, transform.getSourceDimensions());
        assertEquals(1, transform.getTargetDimensions());
        assertMatrixEquals(Matrices.create(2, 4, new double[] {
            0, 0, 100, 0,
            0, 0,   0, 1
        }));

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
     * Tests extracting the vertical part of a spatiotemporal CRS.
     *
     * @throws FactoryException if the operation cannot be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    public void testGeographic4D_to_EllipsoidalHeight() throws FactoryException, TransformException {
        // NOTE: make sure that the 'sourceCRS' below is not equal to any other 'sourceCRS' created in this class.
        final CompoundCRS sourceCRS = compound("Test4D", CommonCRS.WGS84.geographic3D(), CommonCRS.Temporal.JULIAN.crs());
        final VerticalCRS targetCRS = CommonCRS.Vertical.ELLIPSOIDAL.crs();
        final CoordinateOperation operation = finder().createOperation(sourceCRS, targetCRS);
        assertSame(sourceCRS, operation.getSourceCRS());
        assertSame(targetCRS, operation.getTargetCRS());
        assertEquals("Axis changes", operation.getName().getCode());
        assertInstanceOf(Conversion.class, operation);

        transform = operation.getMathTransform();
        assertEquals(4, transform.getSourceDimensions());
        assertEquals(1, transform.getTargetDimensions());
        assertMatrixEquals(Matrices.create(2, 5, new double[] {
            0, 0, 1, 0, 0,
            0, 0, 0, 0, 1
        }));

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
        return new DefaultCompoundCRS(properties(name), components);
    }

    /**
     * Returns property map with a value assigned to the "name" property.
     * This is a convenience method for construction of geodetic objects.
     */
    private static Map<String,String> properties(final String name) {
        return Map.of(CoordinateReferenceSystem.NAME_KEY, name);
    }

    /**
     * Tests conversion from four-dimensional compound CRS to two-dimensional projected CRS.
     *
     * @throws ParseException if a CRS used in this test cannot be parsed.
     * @throws FactoryException if the operation cannot be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
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
        sourceCRS = compound("Mercator 3D", sourceCRS, CommonCRS.Vertical.MEAN_SEA_LEVEL.crs());
        sourceCRS = compound("Mercator 4D", sourceCRS, CommonCRS.Temporal.MODIFIED_JULIAN.crs());

        final CoordinateOperation operation = finder().createOperation(sourceCRS, targetCRS);
        assertSame(sourceCRS, operation.getSourceCRS());
        assertSame(targetCRS, operation.getTargetCRS());

        transform = operation.getMathTransform();
        assertFalse(transform.isIdentity());
        assertInstanceOf(LinearTransform.class, transform,
                "The somewhat complex MathTransform chain should have been simplified to a single affine transform.");
        assertInstanceOf(Conversion.class, operation,
                "The operation should be a simple axis change, not a complex chain of ConcatenatedOperations.");

        assertEquals(4, transform.getSourceDimensions());
        assertEquals(2, transform.getTargetDimensions());
        assertMatrixEquals(Matrices.create(3, 5, new double[] {
            1, 0, 0, 0, 0,
            0, 1, 0, 0, 0,
            0, 0, 0, 0, 1
        }));

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
     * @throws FactoryException if the operation cannot be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    public void testGeographic3D_to_4D() throws FactoryException, TransformException {
        // NOTE: make sure that the 'sourceCRS' below is not equal to any other 'sourceCRS' created in this class.
        final CompoundCRS sourceCRS = compound("Test3D", CommonCRS.WGS84.geographic(),   CommonCRS.Temporal.UNIX.crs());
        final CompoundCRS targetCRS = compound("Test4D", CommonCRS.WGS84.geographic3D(), CommonCRS.Temporal.MODIFIED_JULIAN.crs());
        final CoordinateOperation operation = finder().createOperation(sourceCRS, targetCRS);
        assertSame(sourceCRS, operation.getSourceCRS());
        assertSame(targetCRS, operation.getTargetCRS());
        assertInstanceOf(ConcatenatedOperation.class, operation);
        assertEquals("CompoundCRS[“Test3D”] ⟶ CompoundCRS[“Test4D”]", operation.getName().getCode());

        transform = operation.getMathTransform();
        final var linear = assertInstanceOf(LinearTransform.class, transform);
        assertEquals(3, linear.getSourceDimensions());
        assertEquals(4, linear.getTargetDimensions());
        Assertions.assertMatrixEquals(Matrices.create(5, 4, new double[] {
                    1, 0, 0, 0,
                    0, 1, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 1./SECONDS_PER_DAY, 40587,
                    0, 0, 0, 1
                }), linear.getMatrix(), 1E-12, "transform.matrix");

        tolerance = 2E-12;
        verifyTransform(new double[] {
            -5, -8, CommonCRS.Temporal.DUBLIN_JULIAN.datum().getOrigin().getTime() / 1000
        }, new double[] {
            -5, -8, 0, 15019.5              // Same value as in testTemporalConversion().
        });
        validate();
    }

    /**
     * Tests conversion from spatiotemporal CRS to a derived CRS.
     *
     * @throws FactoryException if the operation cannot be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    public void testSpatioTemporalToDerived() throws FactoryException, TransformException {
        final Map<String,Object> properties = new HashMap<>();
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

        final CoordinateOperation operation = finder().createOperation(sourceCRS, targetCRS);
        assertSame(sourceCRS, operation.getSourceCRS());
        assertSame(targetCRS, operation.getTargetCRS());

        transform = operation.getMathTransform();
        assertEquals(3, transform.getSourceDimensions());
        assertEquals(2, transform.getTargetDimensions());
        assertMatrixEquals(Matrices.create(3, 4, new double[] {
            12,  0,  0, 480,
            0, -12,  0, 790,
            0,   0,  0,   1
        }));
        validate();
    }

    /**
     * Tests conversion between two engineering CRS.
     *
     * @throws FactoryException if the operation cannot be created.
     */
    @Test
    public void testEngineeringCRS() throws FactoryException {
        final DefaultEngineeringCRS sourceCRS = createEngineering("Screen display", AxisDirection.DISPLAY_DOWN);
        final DefaultEngineeringCRS targetCRS = createEngineering("Another device", AxisDirection.DISPLAY_DOWN);
        final CoordinateOperationFinder finder = finder();
        var e = assertThrows(OperationNotFoundException.class, () -> finder.createOperation(sourceCRS, targetCRS),
                "Should not create operation between CRS of different datum.");
        assertMessageContains(e, "A test CRS");

        final DefaultEngineeringCRS screenCRS = createEngineering("Screen display", AxisDirection.DISPLAY_UP);
        final CoordinateOperation operation = finder.createOperation(sourceCRS, screenCRS);
        assertSame(sourceCRS, operation.getSourceCRS());
        assertSame(screenCRS, operation.getTargetCRS());

        transform = operation.getMathTransform();
        assertEquals(2, transform.getSourceDimensions());
        assertEquals(2, transform.getTargetDimensions());
        assertMatrixEquals(Matrices.create(3, 3, new double[] {
            1,  0,  0,
            0, -1,  0,
            0,  0,  1
        }));
        validate();
    }

    /**
     * Constructs an axis the given abbreviation and axis direction.
     */
    private static DefaultEngineeringCRS createEngineering(final String datumName, final AxisDirection yDirection) {
        return new DefaultEngineeringCRS(properties("A test CRS"),
                new DefaultEngineeringDatum(properties(datumName)), null,
                new DefaultCartesianCS(properties("A test CS"),
                        new DefaultCoordinateSystemAxis(properties("x"), "x", AxisDirection.DISPLAY_RIGHT, Units.METRE),
                        new DefaultCoordinateSystemAxis(properties("y"), "y", yDirection, Units.METRE)));
    }
}
