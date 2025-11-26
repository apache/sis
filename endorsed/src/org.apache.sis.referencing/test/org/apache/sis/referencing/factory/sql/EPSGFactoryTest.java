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
package org.apache.sis.referencing.factory.sql;

import java.util.Set;
import java.util.List;
import java.util.Locale;
import java.util.Iterator;
import java.util.Collections;
import javax.measure.Unit;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.VerticalDatum;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.Transformation;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.system.Loggers;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.crs.DefaultGeographicCRS;
import org.apache.sis.referencing.datum.DatumOrEnsemble;
import org.apache.sis.referencing.operation.AbstractCoordinateOperation;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.factory.IdentifiedObjectFinder;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.geometry.DirectPosition2D;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.apache.sis.test.TestCaseWithLogs;
import org.apache.sis.referencing.factory.TestFactorySource;
import static org.apache.sis.test.Assertions.assertNotDeepEquals;
import static org.apache.sis.referencing.Assertions.assertEpsgNameAndIdentifierEqual;
import static org.apache.sis.referencing.Assertions.assertAliasTipEquals;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import static org.opengis.test.Assertions.assertMatrixEquals;
import static org.opengis.test.Assertions.assertAxisDirectionsEqual;


/**
 * Tests {@link EPSGFactory}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Vadim Semenov
 */
@SuppressWarnings("exports")
@Execution(ExecutionMode.SAME_THREAD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class EPSGFactoryTest extends TestCaseWithLogs {
    /**
     * The source of the EPSG factory.
     */
    private final TestFactorySource dataEPSG;

    /**
     * Creates the factory to use for all tests in this class.
     *
     * @throws FactoryException if an error occurred while creating the factory.
     */
    public EPSGFactoryTest() throws FactoryException {
        super(Loggers.CRS_FACTORY);
        dataEPSG = new TestFactorySource();
    }

    /**
     * Forces release of JDBC connections after the tests in this class.
     *
     * @throws FactoryException if an error occurred while closing the connections.
     */
    @AfterAll
    public void close() throws FactoryException {
        dataEPSG.close();
    }

    /**
     * Tests the "WGS 84" geographic CRS (EPSG:4326).
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testWGS84() throws FactoryException {
        final EPSGFactory factory = dataEPSG.factory();
        final GeographicCRS crs = factory.createGeographicCRS("EPSG:4326");
        assertEpsgNameAndIdentifierEqual("WGS 84", 4326, crs);

        String expected = "World Geodetic System 1984";
        if (crs.getDatum() == null) expected += " ensemble";
        assertEpsgNameAndIdentifierEqual(expected, 6326, DatumOrEnsemble.of(crs));
        assertAxisDirectionsEqual(crs.getCoordinateSystem(), AxisDirection.NORTH, AxisDirection.EAST);

        assertSame(crs, factory.createCoordinateReferenceSystem("4326"), "CRS shall be cached.");
        assertSame(crs, factory.createGeographicCRS("EPSG::4326"), "Shall accept \"::\"");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests the "Datum 73" geographic CRS (EPSG:4274), which has a datum different than the WGS84 one.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testGeographic2D() throws FactoryException {
        final EPSGFactory factory = dataEPSG.factory();
        final GeographicCRS crs = factory.createGeographicCRS("4274");
        assertEpsgNameAndIdentifierEqual("Datum 73", 4274, crs);
        assertEpsgNameAndIdentifierEqual("Datum 73", 6274, crs.getDatum());
        assertAxisDirectionsEqual(crs.getCoordinateSystem(), AxisDirection.NORTH, AxisDirection.EAST);

        assertSame(crs, factory.createCoordinateReferenceSystem("4274"), "CRS shall be cached.");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests the "Lao 1997" geographic CRS (EPSG:4993) with an ellipsoidal height.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testGeographic3D() throws FactoryException {
        final EPSGFactory factory = dataEPSG.factory();
        final GeographicCRS crs = factory.createGeographicCRS("EPSG::4993");
        assertEpsgNameAndIdentifierEqual("Lao 1997", 4993, crs);
        assertEpsgNameAndIdentifierEqual("Lao National Datum 1997", 6678, crs.getDatum());
        assertAxisDirectionsEqual(crs.getCoordinateSystem(), AxisDirection.NORTH, AxisDirection.EAST, AxisDirection.UP);

        assertSame(crs, factory.createCoordinateReferenceSystem("4993"), "CRS shall be cached.");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests the "ITRF93" geocentric CRS (EPSG:4915).
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testGeocentric() throws FactoryException {
        final EPSGFactory factory = dataEPSG.factory();
        final GeodeticCRS crs = factory.createGeodeticCRS("epsg:4915");
        assertEpsgNameAndIdentifierEqual("ITRF93", 4915, crs);
        assertEpsgNameAndIdentifierEqual("International Terrestrial Reference Frame 1993", 6652, crs.getDatum());
        assertAxisDirectionsEqual(crs.getCoordinateSystem(), AxisDirection.GEOCENTRIC_X, AxisDirection.GEOCENTRIC_Y, AxisDirection.GEOCENTRIC_Z);

        assertSame(crs, factory.createCoordinateReferenceSystem("4915"), "CRS shall be cached.");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests the "NAD27(76) / UTM zone 15N" projected CRS (EPSG:2027).
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testProjected() throws FactoryException {
        final EPSGFactory factory = dataEPSG.factory();
        final ProjectedCRS crs = factory.createProjectedCRS("2027");
        assertEpsgNameAndIdentifierEqual("NAD27(76) / UTM zone 15N", 2027, crs);
        assertEpsgNameAndIdentifierEqual("NAD27(76)", 4608, crs.getBaseCRS());
        assertEpsgNameAndIdentifierEqual("North American Datum 1927 (1976)", 6608, crs.getDatum());
        assertEpsgNameAndIdentifierEqual("Transverse Mercator", 9807, crs.getConversionFromBase().getMethod());
        assertEpsgNameAndIdentifierEqual("UTM zone 15N", 16015, crs.getConversionFromBase());
        assertAxisDirectionsEqual(crs.getCoordinateSystem(), AxisDirection.EAST, AxisDirection.NORTH);
        verifyTransverseMercatorParmeters(crs.getConversionFromBase().getParameterValues(), -93);

        assertSame(crs, factory.createCoordinateReferenceSystem("2027"), "CRS shall be cached.");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Verifies the parameter values of the given Universal Transverse Mercator projection.
     *
     * @param  parameters  the parameter value to verify.
     * @param  cm  the expected central meridian value.
     */
    private static void verifyTransverseMercatorParmeters(final ParameterValueGroup parameters, final double cm) {
        assertEquals("Transverse Mercator", parameters.getDescriptor().getName().getCode());
        assertEquals(    cm, parameters.parameter("central_meridian"  ).doubleValue(), "central_meridian");
        assertEquals(     0, parameters.parameter("latitude_of_origin").doubleValue(), "latitude_of_origin");
        assertEquals(0.9996, parameters.parameter("scale_factor"      ).doubleValue(), "scale_factor");
        assertEquals(500000, parameters.parameter("false_easting"     ).doubleValue(), "false_easting");
        assertEquals(     0, parameters.parameter("false_northing"    ).doubleValue(), "false_northing");
    }

    /**
     * Tests the "Beijing 1954 / 3-degree Gauss-Kruger CM 135E" projected CRS (EPSG:2442).
     * This projected CRS has (North, East) axis orientations instead of (East, North).
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testProjectedNorthEast() throws FactoryException {
        final EPSGFactory factory = dataEPSG.factory();
        final ProjectedCRS crs = factory.createProjectedCRS(" EPSG : 2442 ");
        assertEpsgNameAndIdentifierEqual("Beijing 1954 / 3-degree Gauss-Kruger CM 135E", 2442, crs);
        assertAliasTipEquals            ("Beijing 1954 / 3GK 135E", crs);
        assertEpsgNameAndIdentifierEqual("Beijing 1954", 4214, crs.getBaseCRS());
        assertEpsgNameAndIdentifierEqual("Beijing 1954", 6214, crs.getDatum());
        assertEpsgNameAndIdentifierEqual("Transverse Mercator", 9807, crs.getConversionFromBase().getMethod());
        assertEpsgNameAndIdentifierEqual("Gauss-Kruger CM 135E", 16323, crs.getConversionFromBase());
        assertAxisDirectionsEqual(crs.getCoordinateSystem(), AxisDirection.NORTH, AxisDirection.EAST);

        final ParameterValueGroup parameters = crs.getConversionFromBase().getParameterValues();
        assertEquals("Transverse Mercator", parameters.getDescriptor().getName().getCode());
        assertEquals(   135, parameters.parameter("central_meridian"  ).doubleValue(), "central_meridian");
        assertEquals(     0, parameters.parameter("latitude_of_origin").doubleValue(), "latitude_of_origin");
        assertEquals(     1, parameters.parameter("scale_factor"      ).doubleValue(), "scale_factor");
        assertEquals(500000, parameters.parameter("false_easting"     ).doubleValue(), "false_easting");
        assertEquals(     0, parameters.parameter("false_northing"    ).doubleValue(), "false_northing");

        assertSame(crs, factory.createCoordinateReferenceSystem("2442"), "CRS shall be cached.");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests the "WGS 72 / UTM zone 10N" projection and ensures
     * that it is not confused with "WGS 72BE / UTM zone 10N".
     * In the EPSG database, those two projected CRS use the same conversion.
     * However, in Apache SIS the conversions must differ because the datum are not the same.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testProjectedWithSharedConversion() throws FactoryException {
        final EPSGFactory factory = dataEPSG.factory();
        final ProjectedCRS crs = factory.createProjectedCRS("32210");
        assertEpsgNameAndIdentifierEqual("WGS 72 / UTM zone 10N", 32210, crs);
        assertEpsgNameAndIdentifierEqual("WGS 72", 4322, crs.getBaseCRS());
        assertEpsgNameAndIdentifierEqual("World Geodetic System 1972", 6322, crs.getDatum());
        assertEpsgNameAndIdentifierEqual("Transverse Mercator", 9807, crs.getConversionFromBase().getMethod());
        assertEpsgNameAndIdentifierEqual("UTM zone 10N", 16010, crs.getConversionFromBase());
        assertAxisDirectionsEqual(crs.getCoordinateSystem(), AxisDirection.EAST, AxisDirection.NORTH);
        verifyTransverseMercatorParmeters(crs.getConversionFromBase().getParameterValues(), -123);

        final ProjectedCRS variant = factory.createProjectedCRS("32410");
        assertEpsgNameAndIdentifierEqual("WGS 72BE / UTM zone 10N", 32410, variant);
        assertEpsgNameAndIdentifierEqual("WGS 72BE", 4324, variant.getBaseCRS());
        assertEpsgNameAndIdentifierEqual("WGS 72 Transit Broadcast Ephemeris", 6324, variant.getDatum());
        assertEpsgNameAndIdentifierEqual("Transverse Mercator", 9807, variant.getConversionFromBase().getMethod());
        assertEpsgNameAndIdentifierEqual("UTM zone 10N", 16010, variant.getConversionFromBase());
        verifyTransverseMercatorParmeters(crs.getConversionFromBase().getParameterValues(), -123);

        assertEquals(crs.getConversionFromBase().getMethod(), variant.getConversionFromBase().getMethod());
        assertEquals(crs.getCoordinateSystem(), variant.getCoordinateSystem());

        assertNotDeepEquals(crs.getConversionFromBase(), variant.getConversionFromBase());
        assertNotDeepEquals(crs, variant);
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests a projected CRS fetched by its name instead of its code.
     * Tests also {@link EPSGDataAccess#createObject(String)}.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     *
     * @see #testCreateByName()
     */
    @Test
    public void testProjectedByName() throws FactoryException {
        final EPSGFactory factory = dataEPSG.factory();
        final ProjectedCRS crs = factory.createProjectedCRS("NTF (Paris) / Lambert zone I");
        assertEpsgNameAndIdentifierEqual("NTF (Paris) / Lambert zone I", 27571, crs);
        assertEpsgNameAndIdentifierEqual("NTF (Paris)", 4807, crs.getBaseCRS());
        assertEpsgNameAndIdentifierEqual("Lambert Conic Conformal (1SP)", 9801, crs.getConversionFromBase().getMethod());
        assertEpsgNameAndIdentifierEqual("Lambert zone I", 18081, crs.getConversionFromBase());
        assertAxisDirectionsEqual(crs.getCoordinateSystem(), AxisDirection.EAST, AxisDirection.NORTH);
        assertSame(crs, factory.createProjectedCRS("27571"));
        /*
         * Gets the CRS using `createObject`. It will require more SQL
         * statement internally in order to determines the object type.
         */
        assertSame(crs, factory.createObject("27571"));
        assertSame(crs, factory.createObject("NTF (Paris) / Lambert zone I"));
        assertSame(crs, factory.createProjectedCRS("ntf (paris) / lambert zone I"));
        assertSame(crs, factory.createObject("ntf (paris) / lambert zone I"));
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests a projected CRS located on the North pole.
     * Axis directions are "South along 90°E" and "South along 180°E".
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    @Disabled("“Lambert Azimuthal Equal Area (Spherical)” projection is not yet implemented.")
    public void testProjectedOnPole() throws FactoryException {
        final EPSGFactory factory = dataEPSG.factory();
        final ProjectedCRS crs = factory.createProjectedCRS("3408");
        assertEpsgNameAndIdentifierEqual("NSIDC EASE-Grid North", 3408, crs);
        assertEpsgNameAndIdentifierEqual("Unspecified datum based upon the International 1924 Authalic Sphere", 4053, crs.getBaseCRS());
        assertEpsgNameAndIdentifierEqual("Lambert Azimuthal Equal Area (Spherical)", 1027, crs.getConversionFromBase().getMethod());
        assertEpsgNameAndIdentifierEqual("US NSIDC Equal Area north projection", 3897, crs.getConversionFromBase());

        // TODO: test axis directions.

        assertSame(crs, factory.createCoordinateReferenceSystem("3408"), "CRS shall be cached.");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests the Google projected CRS.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testGoogleProjection() throws FactoryException {
        final EPSGFactory factory = dataEPSG.factory();
        final ProjectedCRS crs = factory.createProjectedCRS("3857");
        assertEpsgNameAndIdentifierEqual("WGS 84 / Pseudo-Mercator", 3857, crs);
        assertEpsgNameAndIdentifierEqual("WGS 84", 4326, crs.getBaseCRS());
        assertEpsgNameAndIdentifierEqual("Popular Visualisation Pseudo Mercator", 1024, crs.getConversionFromBase().getMethod());
        assertEpsgNameAndIdentifierEqual("Popular Visualisation Pseudo-Mercator", 3856, crs.getConversionFromBase());
        assertAxisDirectionsEqual(crs.getCoordinateSystem(), AxisDirection.EAST, AxisDirection.NORTH);

        assertSame(crs, factory.createCoordinateReferenceSystem("3857"), "CRS shall be cached.");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests the "Barcelona Grid B1" engineering CRS (EPSG:5801).
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testEngineering() throws FactoryException {
        final EPSGFactory factory = dataEPSG.factory();
        final EngineeringCRS crs = factory.createEngineeringCRS("EPSG:5801");
        assertEpsgNameAndIdentifierEqual("Barcelona Grid B1", 5801, crs);
        assertEpsgNameAndIdentifierEqual("Barcelona", 9301, crs.getDatum());
        assertAxisDirectionsEqual(crs.getCoordinateSystem(), AxisDirection.NORTH, AxisDirection.EAST);
        assertSame(crs, factory.createCoordinateReferenceSystem("5801"), "CRS shall be cached.");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests the "Black Sea height" vertical CRS (EPSG:5735).
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testVertical() throws FactoryException {
        final EPSGFactory factory = dataEPSG.factory();
        final VerticalCRS crs = factory.createVerticalCRS("EPSG:5735");
        assertEpsgNameAndIdentifierEqual("Black Sea height", 5735, crs);
        assertEpsgNameAndIdentifierEqual("Black Sea", 5134, crs.getDatum());
        assertSame(crs, factory.createCoordinateReferenceSystem("5735"), "CRS shall be cached.");
        assertAxisDirectionsEqual(crs.getCoordinateSystem(), AxisDirection.UP);
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests the "NTF (Paris) + NGF-IGN69 height" compound CRS (EPSG:7400).
     * This method tests also the domain of validity.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testCompound() throws FactoryException {
        final EPSGFactory factory = dataEPSG.factory();
        final CompoundCRS crs = factory.createCompoundCRS("EPSG:7400");
        assertEpsgNameAndIdentifierEqual("NTF (Paris) + NGF-IGN69 height", 7400, crs);

        final List<CoordinateReferenceSystem> components = crs.getComponents();
        assertEquals(2, components.size());
        assertEpsgNameAndIdentifierEqual("NTF (Paris)",      4807, components.get(0));
        assertEpsgNameAndIdentifierEqual("NGF-IGN69 height", 5720, components.get(1));

        assertAxisDirectionsEqual(crs.getCoordinateSystem(),
                AxisDirection.NORTH, AxisDirection.EAST, AxisDirection.UP);

        final GeographicBoundingBox bbox = CRS.getGeographicBoundingBox(crs);
        assertNotNull(bbox, "No bounding box. Maybe an older EPSG database is used?");
        assertEquals(42.33, bbox.getSouthBoundLatitude(), "southBoundLatitude");
        assertEquals(51.14, bbox.getNorthBoundLatitude(), "northBoundLatitude");
        assertEquals(-4.87, bbox.getWestBoundLongitude(), "westBoundLongitude");
        assertEquals( 8.23, bbox.getEastBoundLongitude(), "eastBoundLongitude");

        assertSame(crs, factory.createCoordinateReferenceSystem("7400"), "CRS shall be cached.");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests creation of deprecated coordinate systems.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testDeprecatedCoordinateSystems() throws FactoryException {
        final EPSGFactory factory = dataEPSG.factory();
        for (int deprecatedCode = 6401; deprecatedCode <= 6420; deprecatedCode++) {
            final int replacementCode = EPSGDataAccess.replaceDeprecatedCS(deprecatedCode);
            if (replacementCode == deprecatedCode) {
                assertTrue(deprecatedCode >= 6403 && deprecatedCode <= 6404);
                continue;   // Non-deprecated code.
            }
            final CoordinateSystem expected = factory.createEllipsoidalCS(Integer.toString(replacementCode));
            loggings.assertNoUnexpectedLog();
            final String code = Integer.toString(deprecatedCode);
            final CoordinateSystem deprecated;
            try {
                deprecated = factory.createEllipsoidalCS(code);
            } catch (FactoryException e) {
                final String m = e.getMessage();
                if (m.contains("9115") || m.contains("9116") || m.contains("9117") ||
                    m.contains("9118") || m.contains("9119") || m.contains("9120"))
                {
                    // Unit "9116" to "9120" are known to be unsupported.
                    continue;
                }
                throw e;
            }
            loggings.assertNextLogContains(code);
            final int dimension = expected.getDimension();
            assertEquals(dimension, deprecated.getDimension(), "dimension");
            for (int i=0; i<dimension; i++) {
                final CoordinateSystemAxis ref  = expected.getAxis(i);
                final CoordinateSystemAxis axis = deprecated.getAxis(i);
                assertEquals(ref.getName(),                 axis.getName());
                assertEquals(ref.getAlias(),                axis.getAlias());
                assertEquals(ref.getDirection(),            axis.getDirection());
                assertEquals(ref.getRangeMeaning(),         axis.getRangeMeaning());
                assertEquals(ref.getUnit().getSystemUnit(), axis.getUnit().getSystemUnit());
            }
        }
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests a legacy geographic CRS (no longer supported by EPSG).
     * This test verifies that the expected warnings are logged.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testDeprecatedGeographic() throws FactoryException {
        final EPSGFactory factory = dataEPSG.factory();
        final GeographicCRS crs = factory.createGeographicCRS("63266405");
        assertEpsgNameAndIdentifierEqual("WGS 84 (deg)", 63266405, crs);
        assertAxisDirectionsEqual(crs.getCoordinateSystem(), AxisDirection.NORTH, AxisDirection.EAST);
        assertSame(crs, factory.createCoordinateReferenceSystem("63266405"), "CRS shall be cached.");

        loggings.skipNextLogIfContains("EPSG:6405");                 // Coordinate System 6405 is no longer supported by EPSG
        loggings.assertNextLogContains("EPSG:63266405", "4326");     // EPSG no longer support codes in the 60000000 series.
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests a deprecated projected CRS.
     * This test verifies that the expected warnings are logged.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testDeprecatedProjected() throws FactoryException {
        final EPSGFactory factory = dataEPSG.factory();
        final ProjectedCRS crs = factory.createProjectedCRS("3786");
        assertEpsgNameAndIdentifierEqual("World Equidistant Cylindrical (Sphere)", 3786, crs);
        assertEpsgNameAndIdentifierEqual("Equidistant Cylindrical (Spherical)", 9823, crs.getConversionFromBase().getMethod());
        assertAxisDirectionsEqual(crs.getCoordinateSystem(), AxisDirection.EAST, AxisDirection.NORTH);

        loggings.assertNextLogContains("EPSG:9823",  "1029");    // Operation method 9823 has been replaced by 1029
        loggings.assertNextLogContains("EPSG:19968", "4086");    // Coordinate Operation 19968 has been replaced by 4086
        loggings.assertNextLogContains("EPSG:3786",  "4088");    // Coordinate Reference System 3786 has been replaced by 4088
        loggings.assertNoUnexpectedLog();

        final ProjectedCRS replacement = factory.createProjectedCRS("4088");
        assertEpsgNameAndIdentifierEqual("World Equidistant Cylindrical (Sphere)", 4088, replacement);
        assertEpsgNameAndIdentifierEqual("Equidistant Cylindrical (Spherical)", 1029, replacement.getConversionFromBase().getMethod());

        loggings.assertNextLogContains("EPSG:4088", "4087");     // Coordinate Reference System 4088 has been replaced by 4087

        assertSame(crs.getBaseCRS(), replacement.getBaseCRS());
        assertSame(crs.getCoordinateSystem(), replacement.getCoordinateSystem());

        assertSame(crs, factory.createCoordinateReferenceSystem("3786"), "CRS shall be cached.");
        assertSame(replacement, factory.createCoordinateReferenceSystem("4088"), "CRS shall be cached.");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests the creation of CRS using name instead of primary key.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     *
     * @see #testProjectedByName()
     */
    @Test
    public void testCreateByName() throws FactoryException {
        final EPSGFactory factory = dataEPSG.factory();
        assertSame   (factory.createUnit("9002"), factory.createUnit("foot"));
        assertNotSame(factory.createUnit("9001"), factory.createUnit("foot"));
        assertSame   (factory.createUnit("9202"), factory.createUnit("ppm"));       // Search in alias table.
        /*
         * Test a name with colons.
         */
        final CoordinateSystem cs = factory.createCoordinateSystem(
                "Ellipsoidal 2D CS. Axes: latitude, longitude. Orientations: north, east. UoM: degree");
        assertEpsgNameAndIdentifierEqual(
                "Ellipsoidal 2D CS. Axes: latitude, longitude. Orientations: north, east. UoM: degree", 6422, cs);
        /*
         * Tests with a unknown name. The exception should be NoSuchAuthorityCodeException
         * (some previous version wrongly threw a SQLException when using HSQL database).
         */
        var e = assertThrows(NoSuchAuthorityCodeException.class, () -> factory.createGeographicCRS("WGS83"),
                "Should not find a geographic CRS named “WGS83” (the actual name is “WGS 84”).");
        assertEquals("WGS83", e.getAuthorityCode());
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@link EPSGDataAccess#getAuthorityCodes(Class)} method.
     * Some parts of this test are very slow. The slow parts are disabled by default.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testAuthorityCodes() throws FactoryException {
        final EPSGFactory factory = dataEPSG.factory();
        /*
         * Most basic objects.
         * Note: the numbers in `size() >= x` checks were determined from the content of EPSG dataset version 7.9.
         */
        try {
            final Set<String> axes = factory.getAuthorityCodes(CoordinateSystemAxis.class);
            assertFalse(axes.isEmpty(),       "Axes not found.");
            assertTrue (axes.contains("115"), "Shall contain Geocentric X.");
        } catch (BackingStoreException e) {
            throw e.unwrapOrRethrow(FactoryException.class);
        }
        final Set<String> coordinateSystems = factory.getAuthorityCodes(CoordinateSystem.class);
        assertFalse(coordinateSystems.isEmpty(),        "Coordinate systems not found.");
        assertTrue (coordinateSystems.contains("6422"), "Shall contain ellipsoidal CS.");

        final Set<String> primeMeridians = factory.getAuthorityCodes(PrimeMeridian.class);
        assertFalse(primeMeridians.isEmpty(),        "Prime meridians not found.");
        assertTrue (primeMeridians.size() >= 14,     "size() consistency check.");
        assertTrue (primeMeridians.contains("8901"), "Shall contain Greenwich.");
        assertTrue (primeMeridians.contains("8903"), "Shall contain Paris.");

        final Set<String> ellipsoids = factory.getAuthorityCodes(Ellipsoid.class);
        assertFalse(ellipsoids.isEmpty(),        "Ellipsoids not found.");
        assertTrue (ellipsoids.size() >= 48,     "size() consistency check.");
        assertTrue (ellipsoids.contains("7030"), "Shall contain WGS84.");
        assertTrue (ellipsoids.contains("7019"), "Shall contain GRS 1980.");

        /*
         * DATUM - The number of datums is not too large (612 in EPSG 7.9), so execution time should be reasonable
         *         for most tests even if a method call causes scanning of the whole Datum table. We nevertheless
         *         limit such tests to the VerticalDatum (unless EXTENSIVE is true), which is a smaller set.
         */
        final Set<String> datum = factory.getAuthorityCodes(Datum.class);
        assertFalse(datum.isEmpty(),        "Datums not found.");
        assertTrue (datum.contains("6326"), "Shall contain WGS84.");
        assertTrue (datum.contains("5100"), "Shall contain MSL.");

        final Set<String> geodeticDatum = factory.getAuthorityCodes(GeodeticDatum.class);
        assertFalse(geodeticDatum.isEmpty(),          "Geodetic reference frames not found.");
        assertTrue (geodeticDatum.contains("6326"),   "Shall contain WGS84.");
        assertFalse(geodeticDatum.contains("5100"),   "Shall not contain vertical datum.");
        assertFalse(geodeticDatum.containsAll(datum), "Geodetic reference frame should be a subset of datum.");  // Iteration should stop at the first mismatch.

        final Set<String> verticalDatum = factory.getAuthorityCodes(VerticalDatum.class);
        assertFalse(verticalDatum.isEmpty(),          "Vertical datums not found.");
        assertTrue (verticalDatum.size() >= 124,      "size() consistency check.");
        assertFalse(verticalDatum.contains("6326"),   "Shall not contain WGS84.");
        assertTrue (verticalDatum.contains("5100"),   "Shall contain Mean Sea Level (MSL).");
        assertFalse(verticalDatum.containsAll(datum), "Vertical datum should be a subset of datum.");  // Iteration should stop at the first mismatch.
        assertTrue (datum.containsAll(verticalDatum), "Vertical datum should be a subset of datum.");  // Iteration should over a small set (vertical datum).

        assertTrue(geodeticDatum.size() >= 445,         "size() consistency check.");
        assertTrue(datum.size() > geodeticDatum.size(), "Geodetic reference frame should be a subset of datum.");
        assertTrue(datum.size() > verticalDatum.size(), "Vertical datum should be a subset of datum.");
        assertTrue(datum.containsAll(geodeticDatum),    "Geodetic reference frame should be a subset of datum.");

        /*
         * COORDINATE REFERENCE SYSTEMS - There is thousands of CRS, so we avoid all tests that may require
         *                                an iteration over the full table unless EXTENSIVE is true.
         */
        final Set<String> crs = factory.getAuthorityCodes(CoordinateReferenceSystem.class);
        assertFalse (crs.isEmpty(),        "CRSs not found.");
        assertTrue  (crs.contains("4326"), "Shall contain WGS84.");
        assertTrue  (crs.contains("3395"), "Shall contain World Mercator.");

        assertTrue  (crs.size() >= 4175);      // Cause a scanning of the full table.
        assertEquals(crs.size(), crs.size());

        final Set<String> geographicCRS = factory.getAuthorityCodes(GeographicCRS.class);
        assertFalse(geographicCRS.isEmpty(),        "GeographicCRSs not found.");
        assertTrue (geographicCRS.contains("4326"), "Shall contain WGS84.");
        assertFalse(geographicCRS.contains("4978"), "Shall not contain geocentric CRS.");
        assertFalse(geographicCRS.contains("3395"), "Shall not contain projected CRS.");

        assertTrue (geographicCRS.size() >= 468,       "size() consistency check.");
        assertTrue (geographicCRS.size() < crs.size(), "Geographic CRS should be a subset of CRS.");
        assertFalse(geographicCRS.containsAll(crs),    "Geographic CRS should be a subset of CRS.");
        assertTrue (crs.containsAll(geographicCRS),    "Geographic CRS should be a subset of CRS.");

        final Set<String> projectedCRS = factory.getAuthorityCodes(ProjectedCRS.class);
        assertFalse(projectedCRS.isEmpty(),        "ProjectedCRSs not found.");
        assertFalse(projectedCRS.contains("4326"), "Shall not contain geographic CRS.");
        assertTrue (projectedCRS.contains("3395"), "Shall contain World Mercator.");

        assertTrue (projectedCRS.size() >= 3441,      "size() consistency check.");
        assertTrue (projectedCRS.size() < crs.size(), "Projected CRS should be a subset of CRS.");
        assertFalse(projectedCRS.containsAll(crs),    "Projected CRS should be a subset of CRS.");
        assertTrue (crs.containsAll(projectedCRS),    "Projected CRS should be a subset of CRS.");
        assertTrue (Collections.disjoint(geographicCRS, projectedCRS), "Projected CRS cannot be Geographic CRS.");

        /*
         * COORDINATE OPERATIONS - There is thousands of operations, so we avoid all tests that may require
         *                         an iteration over the full table unless EXTENSIVE is true.
         */
        final Set<String> methods         = factory.getAuthorityCodes(OperationMethod    .class);
        final Set<String> parameters      = factory.getAuthorityCodes(ParameterDescriptor.class);
        final Set<String> operations      = factory.getAuthorityCodes(SingleOperation    .class);
        final Set<String> conversions     = factory.getAuthorityCodes(Conversion         .class);
        final Set<String> transformations = factory.getAuthorityCodes(Transformation     .class);

        assertFalse(methods        .isEmpty(), "Methods not found.");
        assertFalse(parameters     .isEmpty(), "Parameters not found.");
        assertFalse(operations     .isEmpty(), "Operations not found.");
        assertFalse(conversions    .isEmpty(), "Conversions not found.");
        assertFalse(transformations.isEmpty(), "Transformations not found.");

        assertTrue (methods        .contains("9804"),  "Shall contain “Mercator 1SP”");
        assertTrue (parameters     .contains("8805"),  "Shall contain “Scale factor”");
        assertTrue (operations     .contains("1133"),  "Shall contain “ED50 to WGS 84 (1)”");
        assertFalse(conversions    .contains("1133"),  "Shall not contain “ED50 to WGS 84 (1)”");
        assertTrue (transformations.contains("1133"),  "Shall contain “ED50 to WGS 84 (1)”");
        assertTrue (operations     .contains("16001"), "Shall contain “UTM zone 1N”");
        assertTrue (conversions    .contains("16001"), "Shall contain “UTM zone 1N”");
        assertFalse(transformations.contains("16001"), "Shall not contain “UTM zone 1N”");

        assertTrue (conversions    .size() < operations .size(), "Conversions shall be a subset of operations.");
        assertTrue (transformations.size() < operations .size(), "Transformations shall be a subset of operations.");
        assertTrue (operations .containsAll(conversions),        "Conversion shall be a subset of operations.");
        assertTrue (operations .containsAll(transformations),    "Transformations shall be a subset of operations.");
        assertTrue (Collections.disjoint(conversions, transformations), "Conversions cannot be transformations.");

        // We are cheating here since we are breaking generic type check.
        // However, in the particular case of our EPSG factory, it works.
        @SuppressWarnings({"unchecked","rawtypes"})
        final Set<?> units = factory.getAuthorityCodes((Class) Unit.class);
        assertFalse(units.isEmpty());
        assertTrue (units.size() >= 2);

        // Try a dummy type.
        @SuppressWarnings({"unchecked","rawtypes"})
        final Class<? extends IdentifiedObject> wrong = (Class) String.class;
        assertTrue(factory.getAuthorityCodes(wrong).isEmpty());
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests the {@link EPSGDataAccess#getDescriptionText(class, String)} method.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testDescriptionText() throws FactoryException {
        final EPSGFactory factory = dataEPSG.factory();
        assertDescriptionStarts("World Geodetic System 1984", factory, GeodeticDatum.class,      6326);
        assertDescriptionStarts("Mean Sea Level",             factory, VerticalDatum.class,      5100);
        assertDescriptionStarts("NTF (Paris) / Nord France",  factory, ProjectedCRS.class,      27591);
        assertDescriptionStarts("NTF (Paris) / France II",    factory, ProjectedCRS.class,      27582);
        assertDescriptionStarts("Ellipsoidal height",         factory, CoordinateSystemAxis.class, 84);
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Asserts that the description text for the given code starts with the expected value.
     * We do not require a full match because suffix such as "ensemble" may or may not be present
     * depending on the version of the <abbr>EPSG</abbr> database.
     */
    private static void assertDescriptionStarts(final String expected, final EPSGFactory factory,
            final Class<? extends IdentifiedObject> type, final int code) throws FactoryException
    {
        final var description = factory.getDescriptionText(type, Integer.toString(code));
        assertTrue(description.isPresent(), expected);
        assertTrue(description.get().toString(Locale.US).startsWith(expected), expected);
    }

    /**
     * Tests the "UTM zone 10N" conversion (EPSG:16010).
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testConversion() throws FactoryException {
        final EPSGFactory factory = dataEPSG.factory();
        /*
         * Fetch directly the "UTM zone 10N" operation. Because this operation was not obtained in
         * the context of a projected CRS, the source and target CRS shall be unspecified (i.e. null).
         */
        final CoordinateOperation operation = factory.createCoordinateOperation("16010");
        assertEpsgNameAndIdentifierEqual("UTM zone 10N", 16010, operation);
        assertInstanceOf(Conversion.class, operation);
        assertNull(operation.getSourceCRS());
        assertNull(operation.getTargetCRS());
        assertNull(operation.getMathTransform());
        /*
         * Fetch the "WGS 72 / UTM zone 10N" projected CRS.
         * The operation associated to this CRS should now define the source and target CRS.
         */
        final ProjectedCRS crs = factory.createProjectedCRS("32210");
        final CoordinateOperation projection = crs.getConversionFromBase();
        assertEpsgNameAndIdentifierEqual("WGS 72 / UTM zone 10N", 32210, crs);
        assertEpsgNameAndIdentifierEqual("UTM zone 10N", 16010, projection);
        assertNotSame(projection, operation,
                "The defining conversion and the actual conversion should differ " +
                "because the actual conversion should have semi-axis length values.");
        assertInstanceOf(Conversion.class, projection);
        assertNotNull(projection.getSourceCRS());
        assertNotNull(projection.getTargetCRS());
        assertNotNull(projection.getMathTransform());
        /*
         * Compare the conversion obtained directly with the conversion obtained
         * indirectly through a projected CRS. Both should use the same method.
         */
        final OperationMethod copMethod = ((SingleOperation) operation) .getMethod();
        final OperationMethod crsMethod = ((SingleOperation) projection).getMethod();
        assertEpsgNameAndIdentifierEqual("Transverse Mercator", 9807, copMethod);
        assertEpsgNameAndIdentifierEqual("Transverse Mercator", 9807, crsMethod);
        try {
            assertEquals(copMethod, crsMethod, "Conversion method");
            assertEquals(copMethod, factory.createOperationMethod("9807"));
        } catch (AssertionError error) {
            out.println("The following contains more information about a JUnit test failure.");
            out.println("See the JUnit report for the stack trace. Below is a cache dump.");
            out.println("See the operation method EPSG:9807 and compare with:");
            out.print  ("  - Method obtained directly:   "); out.println(System.identityHashCode(copMethod));
            out.print  ("  - Method obtained indirectly: "); out.println(System.identityHashCode(crsMethod));
            out.println("Content of EPSGFactory cache:");
            factory.printCacheContent(out);
            throw error;
        }
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests longitude rotation (EPSG:1764). This is a very simple case for checking
     * that this part is okay before to try more complex transformations.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testSimpleTransformation() throws FactoryException {
        final EPSGFactory factory = dataEPSG.factory();
        final CoordinateOperation operation = factory.createCoordinateOperation("1764");
        assertEpsgNameAndIdentifierEqual("NTF (Paris) to NTF (2)", 1764, operation);
        assertInstanceOf(Transformation.class, operation);
        assertSame(operation, factory.createCoordinateOperation("1764"), "Operation shall be cached");
        loggings.assertNoUnexpectedLog();
        final LinearTransform tr = assertInstanceOf(LinearTransform.class, operation.getMathTransform());
        final var matrix = new Matrix3();
        matrix.m00 = 0.9;
        matrix.m11 = 0.9;
        matrix.m12 = 2.3372083333333333;
        assertMatrixEquals(matrix, tr.getMatrix(), 1E-16, null);
    }

    /**
     * Tests "Jamaica 1875 / Jamaica (Old Grid) to JAD69 / Jamaica National Grid (1)" (EPSG:10087) transformation.
     * This is for testing that there is no attempt to magically convert units in this case.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     * @throws TransformException if the test of a coordinate transformation failed.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-619">619</a>
     */
    @Test
    public void testAffineTransformation() throws FactoryException, TransformException {
        final EPSGFactory factory = dataEPSG.factory();
        final CoordinateOperation operation = factory.createCoordinateOperation("10087");
        assertEpsgNameAndIdentifierEqual(
                "Jamaica 1875 / Jamaica (Old Grid) to JAD69 / Jamaica National Grid (1)", 10087, operation);
        assertEquals(1.5, ((AbstractCoordinateOperation) operation).getLinearAccuracy());
        loggings.assertNoUnexpectedLog();
        final LinearTransform tr = assertInstanceOf(LinearTransform.class, operation.getMathTransform());
        final var matrix = new Matrix3();
        matrix.m00 =  0.304794369;
        matrix.m11 =  0.304794369;
        matrix.m01 =  1.5417425E-5;
        matrix.m10 = -1.5417425E-5;
        matrix.m02 =  82357.457;
        matrix.m12 =  28091.324;
        assertMatrixEquals(matrix, tr.getMatrix(), 1E-16, null);
        final var point = new DirectPosition2D(553900, 482500);     // Example form EPSG guidance note.
        assertSame(point, tr.transform(point, point));
        assertEquals(251190.497, point.x, 0.001);
        assertEquals(175146.067, point.y, 0.001);
    }

    /**
     * Tests "BD72 to WGS 84 (1)" (EPSG:1609) transformation. This one has an unusual unit for the
     * "Scale difference" parameter (EPSG:8611). The value is 0.999999 and the unit is "unity" (EPSG:9201)
     * instead of the usual "parts per million" (EPSG:9202).
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testTransformation() throws FactoryException {
        final EPSGFactory factory = dataEPSG.factory();
        final CoordinateOperation operation = factory.createCoordinateOperation("1609");
        assertEpsgNameAndIdentifierEqual("BD72 to WGS 84 (1)", 1609, operation);
        assertEquals(1.0, ((AbstractCoordinateOperation) operation).getLinearAccuracy());
        assertSame(operation, factory.createCoordinateOperation("1609"), "Operation shall be cached");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@link EPSGDataAccess#createFromCoordinateReferenceSystemCodes(String, String)}.
     * This method verifies the presence of 3 {@link Transformation} instances between the same source and target CRS.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testCreateFromCoordinateReferenceSystemCodes() throws FactoryException {
        final EPSGFactory factory = dataEPSG.factory();
        /*
         * ED50 (4230)  to  WGS 84 (4326)  using
         * Geocentric translations (9603).
         * Accuracy = 2.5
         */
        final CoordinateOperation      operation1 = factory.createCoordinateOperation("1087");
        final CoordinateReferenceSystem sourceCRS = operation1.getSourceCRS();
        final CoordinateReferenceSystem targetCRS = operation1.getTargetCRS();
        final MathTransform             transform = operation1.getMathTransform();
        assertInstanceOf(Transformation.class, operation1);
        assertEpsgNameAndIdentifierEqual("ED50 to WGS 84 (37)", 1087, operation1);
        assertEpsgNameAndIdentifierEqual("ED50",                4230, sourceCRS);
        assertEpsgNameAndIdentifierEqual("WGS 84",              4326, targetCRS);
        assertFalse(operation1.getMathTransform().isIdentity());
        assertEquals(2.5, AbstractCoordinateOperation.castOrCopy(operation1).getLinearAccuracy());
        /*
         * ED50 (4230)  to  WGS 84 (4326)  using
         * Position Vector 7-param. transformation (9606).
         * Accuracy = 1.5
         */
        final CoordinateOperation operation2 = factory.createCoordinateOperation("1631");
        assertEpsgNameAndIdentifierEqual("ED50 to WGS 84 (27)", 1631, operation2);
        assertInstanceOf(Transformation.class, operation2);
        assertSame (sourceCRS, operation2.getSourceCRS());
        assertSame (targetCRS, operation2.getTargetCRS());
        assertFalse(operation2.getMathTransform().isIdentity());
        assertNotEquals(transform, operation2.getMathTransform(), "Should be a more accurate transformation.");
        assertEquals(1.5, AbstractCoordinateOperation.castOrCopy(operation2).getLinearAccuracy());
        /*
         * ED50 (4230)  to  WGS 84 (4326)  using
         * Coordinate Frame rotation (9607).
         * Accuracy = 1.0
         */
        final CoordinateOperation operation3 = factory.createCoordinateOperation("1989");
        assertInstanceOf(Transformation.class, operation3);
        assertEpsgNameAndIdentifierEqual("ED50 to WGS 84 (34)", 1989, operation3);
        assertSame (sourceCRS, operation3.getSourceCRS());
        assertSame (targetCRS, operation3.getTargetCRS());
        assertFalse(operation3.getMathTransform().isIdentity());
        assertNotEquals(transform, operation3.getMathTransform(), "Should be a more accurate transformation.");
        assertEquals(1.0, AbstractCoordinateOperation.castOrCopy(operation3).getLinearAccuracy());
        /*
         * Creates from CRS codes. There is 40 such operations in EPSG version 6.7.
         * The one with the largest domain of validity is EPSG:1133.
         */
        final Set<CoordinateOperation> all = factory.createFromCoordinateReferenceSystemCodes("4230", "4326");
        assertTrue(all.size() >= 3, "Number of coordinate operations.");
        assertTrue(all.contains(operation1), "contains(“EPSG::1087”)");
        assertTrue(all.contains(operation2), "contains(“EPSG::1631”)");
        assertTrue(all.contains(operation3), "contains(“EPSG::1989”)");

        int count = 0;
        boolean found1590 = false;  // In Norway, superseded by 1612.
        boolean found1612 = false;  // Replacement for 1590.
        for (final CoordinateOperation tr : all) {
            assertSame(sourceCRS, tr.getSourceCRS());
            assertSame(targetCRS, tr.getTargetCRS());
            if (count == 0) {   // Preferred transformation (see above comment).
                assertEpsgNameAndIdentifierEqual("ED50 to WGS 84 (1)", 1133, tr);
            }
            switch (Integer.parseInt(IdentifiedObjects.getIdentifier(tr, Citations.EPSG).getCode())) {
                case 1612: found1612 = true; assertFalse(found1590); break;     // Should be find first.
                case 1590: found1590 = true; assertTrue (found1612); break;     // Should be after 1612.
            }
            count++;
        }
        assertTrue(found1612);
        assertFalse(found1590);                 // TODO `assertTrue` if we support "Norway Offshore Interpolation".
        assertEquals(count, all.size());        // Size may have been modified after above loop.
        loggings.clear();                       // Too installation-dependent for testing them.
    }

    /**
     * Tests {@link EPSGFactory#newIdentifiedObjectFinder()} method with a geographic CRS.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testFindGeographic() throws FactoryException {
        final EPSGFactory factory = dataEPSG.factory();
        final IdentifiedObjectFinder finder = factory.newIdentifiedObjectFinder();
        final var crs = (DefaultGeographicCRS) CRS.fromWKT(
                "GEOGCS[“WGS 84”,\n" +
                "  DATUM[“WGS 84”,\n" +     // Use the alias instead of primary name for forcing a deeper search.
                "    SPHEROID[“WGS 1984”, 6378137.0, 298.257223563]],\n" +  // Different name for forcing a deeper search.
                "  PRIMEM[“Greenwich”, 0.0],\n" +
                "  UNIT[“degree”, 0.017453292519943295],\n" +
                "  AXIS[“Geodetic latitude”, NORTH],\n" +
                "  AXIS[“Geodetic longitude”, EAST]]");
        /*
         * First, search for a CRS with axis order that does not match the ones in the EPSG database.
         * IdentifiedObjectFinder should not accept EPSG:4326 as a match for the given CRS.
         */
        assertEquals(IdentifiedObjectFinder.Domain.VALID_DATASET, finder.getSearchDomain(),
                     "Full scan should be enabled by default.");
        assertTrue(finder.find(crs.forConvention(AxesConvention.NORMALIZED)).isEmpty(),
                   "Should not find WGS84 because the axis order is not the same.");
        /*
         * Performs a search based only on the name.
         */
        finder.setSearchDomain(IdentifiedObjectFinder.Domain.DECLARATION);
        final IdentifiedObject found = finder.findSingleton(crs);
        assertEpsgNameAndIdentifierEqual("WGS 84", 4326, found);
        /*
         * Scan the database for searching the CRS.
         */
        finder.setSearchDomain(IdentifiedObjectFinder.Domain.EXHAUSTIVE_VALID_DATASET);
        assertEpsgNameAndIdentifierEqual("WGS 84", 4326, finder.findSingleton(crs));
        assertSame(found, finder.findSingleton(crs), "The CRS should be in the cache.");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@link EPSGFactory#newIdentifiedObjectFinder()} method with a projected CRS.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testFindProjected() throws FactoryException {
        final EPSGFactory factory = dataEPSG.factory();
        final IdentifiedObjectFinder finder = factory.newIdentifiedObjectFinder();
        /*
         * The PROJCS below intentionally uses a name different from the one found in the
         * EPSG database, in order to force a full scan (otherwise the EPSG database would
         * find it by name, but we want to test the scan).
         */
        final CoordinateReferenceSystem crs = CRS.fromWKT(
                "PROJCS[“Beijing 1954 (modified)”,\n" +
                "   GEOGCS[“Beijing 1954 (modified)”,\n" +
                "     DATUM[“Beijing 1954”,\n" +                                                // Datum name matter.
                "       SPHEROID[“Krassowsky 1940”, 6378245.00000006, 298.299999999998]],\n" +  // Intentional rounding error.
                "     PRIMEM[“Greenwich”, 0.0],\n" +
                "     UNIT[“degree”, 0.017453292519943295],\n" +
                "     AXIS[“Geodetic longitude”, EAST],\n" +
                "     AXIS[“Geodetic latitude”, NORTH]],\n" +                   // Wrong axis order, but should not block.
                "   PROJECTION[“Transverse Mercator”],\n" +
                "   PARAMETER[“central_meridian”, 135.0000000000013],\n" +      // Intentional rounding error.
                "   PARAMETER[“latitude_of_origin”, 0.0],\n" +
                "   PARAMETER[“scale_factor”, 1.0],\n" +
                "   PARAMETER[“false_easting”, 500000.000000004],\n" +          // Intentional rounding error.
                "   PARAMETER[“false_northing”, 0.0],\n" +
                "   UNIT[“m”, 1.0],\n" +
                "   AXIS[“Northing”, NORTH],\n" +
                "   AXIS[“Easting”, EAST]]");

        finder.setSearchDomain(IdentifiedObjectFinder.Domain.DECLARATION);
        assertTrue(finder.find(crs).isEmpty(), "Should not find the CRS without a full scan.");

        finder.setSearchDomain(IdentifiedObjectFinder.Domain.VALID_DATASET);
        final Set<IdentifiedObject> find = finder.find(crs);
        assertFalse(find.isEmpty(), "With full scan allowed, the CRS should be found.");
        /*
         * Both EPSG:2442 and EPSG:21463 defines the same projection with the same parameters
         * and the same base GeographicCRS (EPSG:4214). The only difference I found was the
         * area of validity...
         *
         * Note that there is also a EPSG:21483 code, but that one is deprecated and should
         * not be selected in this test.
         */
        final Iterator<IdentifiedObject> it = find.iterator();
        assertEpsgNameAndIdentifierEqual("Beijing 1954 / Gauss-Kruger CM 135E", 21463, it.next());
        assertEpsgNameAndIdentifierEqual("Beijing 1954 / 3-degree Gauss-Kruger CM 135E", 2442, it.next());
        assertFalse(it.hasNext());
        loggings.assertNoUnexpectedLog();
    }
}
