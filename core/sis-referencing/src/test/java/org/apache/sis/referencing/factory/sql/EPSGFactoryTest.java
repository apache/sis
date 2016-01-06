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
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.Projection;
import org.opengis.referencing.operation.Transformation;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.util.FactoryException;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.crs.DefaultGeographicCRS;
import org.apache.sis.referencing.datum.BursaWolfParameters;
import org.apache.sis.referencing.datum.DefaultGeodeticDatum;
import org.apache.sis.referencing.operation.AbstractCoordinateOperation;
import org.apache.sis.referencing.factory.IdentifiedObjectFinder;
import org.apache.sis.referencing.factory.UnavailableFactoryException;

// Test imports
import org.junit.Rule;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.LoggingWatcher;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;

import static org.junit.Assume.assumeNotNull;
import static org.apache.sis.test.ReferencingAssert.*;


/**
 * Tests {@link EPSGFactory}.
 * More tests are provided by the {@code GIGS2000} series
 * in the {@code org.apache.sis.referencing.factory} package.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Vadim Semenov
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn({
    org.apache.sis.referencing.factory.GeodeticObjectFactoryTest.class,
    org.apache.sis.referencing.factory.AuthorityFactoryProxyTest.class,
    org.apache.sis.referencing.factory.IdentifiedObjectFinderTest.class
})
public final strictfp class EPSGFactoryTest extends TestCase {
    /**
     * The factory instance to use for the tests, or {@code null} if not available.
     */
    private static EPSGFactory factory;

    /**
     * Creates the factory to use for all tests in this class.
     *
     * @throws FactoryException if an error occurred while creating the factory.
     */
    @BeforeClass
    public static void createFactory() throws FactoryException {
        try {
            factory = new EPSGFactory();
        } catch (UnavailableFactoryException e) {
            Logging.getLogger(Loggers.CRS_FACTORY).warning(e.toString());
            // Leave INSTANCE to null. This will have the effect of skipping tests.
        }
    }

    /**
     * Force releases of JDBC connections after the tests in this class.
     *
     * @throws FactoryException if an error occurred while closing the connections.
     */
    @AfterClass
    public static void close() throws FactoryException {
        if (factory != null) {
            factory.close();
            factory = null;
        }
    }

    /**
     * Words that we expect to find in each log messages to be emitted.
     */
    private String[][] expectedLogWords;

    /**
     * A JUnit {@linkplain Rule rule} for listening to log events. This field is public
     * because JUnit requires us to do so, but should be considered as an implementation
     * details (it should have been a private field).
     */
    @Rule
    public final LoggingWatcher listener = new LoggingWatcher(Logging.getLogger(Loggers.CRS_FACTORY)) {
        /**
         * Ensures that the logging message contains some expected words.
         */
        @Override
        protected void verifyMessage(final String message) {
            if (expectedLogWords != null) {
                for (final String word : expectedLogWords[expectedLogWords.length - (maximumLogCount + 1)]) {
                    assertTrue(message, message.contains(word));
                }
            }
        }
    };

    /**
     * Tests {@link EPSGDataAccess#tableMatches(String, String)}.
     */
    @Test
    public void testTableMatches() {
        assertTrue(EPSGDataAccess.tableMatches("Coordinate_Operation",          "epsg_coordoperation"));
        assertTrue(EPSGDataAccess.tableMatches("[Coordinate Reference System]", "epsg_coordinatereferencesystem"));
    }

    /**
     * Tests the "WGS 84" geographic CRS (EPSG::4326).
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testWGS84() throws FactoryException {
        assumeNotNull(factory);
        final GeographicCRS crs = factory.createGeographicCRS("EPSG:4326");
        assertEpsgNameAndIdentifierEqual("WGS 84", 4326, crs);
        assertEpsgNameAndIdentifierEqual("World Geodetic System 1984", 6326, crs.getDatum());
        assertAxisDirectionsEqual("EPSG::6422", crs.getCoordinateSystem(), AxisDirection.NORTH, AxisDirection.EAST);

        final BursaWolfParameters[] bwp = ((DefaultGeodeticDatum) crs.getDatum()).getBursaWolfParameters();
        assertEquals("Expected no Bursa-Wolf parameters.", 0, bwp.length);

        assertSame("CRS shall be cached", crs, factory.createCoordinateReferenceSystem("4326"));
    }

    /**
     * Tests the "Datum 73" geographic CRS (EPSG::4274), which has a datum different than the WGS84 one.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    @DependsOnMethod("testWGS84")
    public void testGeographic2D() throws FactoryException {
        assumeNotNull(factory);
        final GeographicCRS crs = factory.createGeographicCRS("4274");
        assertEpsgNameAndIdentifierEqual("Datum 73", 4274, crs);
        assertEpsgNameAndIdentifierEqual("Datum 73", 6274, crs.getDatum());
        assertAxisDirectionsEqual("EPSG::6422", crs.getCoordinateSystem(), AxisDirection.NORTH, AxisDirection.EAST);

        final BursaWolfParameters[] bwp = ((DefaultGeodeticDatum) crs.getDatum()).getBursaWolfParameters();
        assertTrue("Expected a transformation to WGS84.", bwp.length >= 1);

        assertSame("CRS shall be cached", crs, factory.createCoordinateReferenceSystem("4274"));
    }

    /**
     * Tests the "Lao 1997" geographic CRS (EPSG::4993) with an ellipsoidal height.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    @DependsOnMethod("testGeographic2D")
    public void testGeographic3D() throws FactoryException {
        assumeNotNull(factory);
        final GeographicCRS crs = factory.createGeographicCRS("EPSG::4993");
        assertEpsgNameAndIdentifierEqual("Lao 1997", 4993, crs);
        assertEpsgNameAndIdentifierEqual("Lao National Datum 1997", 6678, crs.getDatum());
        assertAxisDirectionsEqual("EPSG::6423", crs.getCoordinateSystem(),
                AxisDirection.NORTH, AxisDirection.EAST, AxisDirection.UP);

        assertSame("CRS shall be cached", crs, factory.createCoordinateReferenceSystem("4993"));
    }

    /**
     * Tests the "ITRF93" geocentric CRS (EPSG::4915).
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testGeocentric() throws FactoryException {
        assumeNotNull(factory);
        final GeocentricCRS crs = factory.createGeocentricCRS("epsg:4915");
        assertEpsgNameAndIdentifierEqual("ITRF93", 4915, crs);
        assertEpsgNameAndIdentifierEqual("International Terrestrial Reference Frame 1993", 6652, crs.getDatum());
        assertAxisDirectionsEqual("EPSG::6500", crs.getCoordinateSystem(),
                AxisDirection.GEOCENTRIC_X, AxisDirection.GEOCENTRIC_Y, AxisDirection.GEOCENTRIC_Z);

        assertSame("CRS shall be cached", crs, factory.createCoordinateReferenceSystem("4915"));
    }

    /**
     * Tests the "NAD27(76) / UTM zone 15N" projected CRS (EPSG::2027).
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    @DependsOnMethod("testGeographic2D")
    public void testProjected() throws FactoryException {
        assumeNotNull(factory);
        final ProjectedCRS crs = factory.createProjectedCRS("2027");
        assertEpsgNameAndIdentifierEqual("NAD27(76) / UTM zone 15N", 2027, crs);
        assertEpsgNameAndIdentifierEqual("NAD27(76)", 4608, crs.getBaseCRS());
        assertEpsgNameAndIdentifierEqual("North American Datum 1927 (1976)", 6608, crs.getDatum());
        assertEpsgNameAndIdentifierEqual("Transverse Mercator", 9807, crs.getConversionFromBase().getMethod());
        assertEpsgNameAndIdentifierEqual("UTM zone 15N", 16015, crs.getConversionFromBase());
        assertAxisDirectionsEqual("EPSG::4400", crs.getCoordinateSystem(), AxisDirection.EAST, AxisDirection.NORTH);
        verifyTransverseMercatorParmeters(crs.getConversionFromBase().getParameterValues(), -93);

        assertSame("CRS shall be cached", crs, factory.createCoordinateReferenceSystem("2027"));
    }

    /**
     * Verifies the parameter values of the given Universal Transverse Mercator projection.
     *
     * @param parameters The parameter value to verify.
     * @param cm The expected central meridian value.
     */
    private static void verifyTransverseMercatorParmeters(final ParameterValueGroup parameters, final double cm) {
        assertEquals("Transverse Mercator",       parameters.getDescriptor().getName().getCode());
        assertEquals("central_meridian",      cm, parameters.parameter("central_meridian"  ).doubleValue(), STRICT);
        assertEquals("latitude_of_origin",     0, parameters.parameter("latitude_of_origin").doubleValue(), STRICT);
        assertEquals("scale_factor",      0.9996, parameters.parameter("scale_factor"      ).doubleValue(), STRICT);
        assertEquals("false_easting",     500000, parameters.parameter("false_easting"     ).doubleValue(), STRICT);
        assertEquals("false_northing",         0, parameters.parameter("false_northing"    ).doubleValue(), STRICT);
    }

    /**
     * Tests the "Beijing 1954 / 3-degree Gauss-Kruger CM 135E" projected CRS (EPSG::2442).
     * This projected CRS has (North, East) axis orientations instead of (East, North).
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    @DependsOnMethod("testProjected")
    public void testProjectedNorthEast() throws FactoryException {
        assumeNotNull(factory);
        final ProjectedCRS crs = factory.createProjectedCRS(" EPSG : 2442 ");
        assertEpsgNameAndIdentifierEqual("Beijing 1954 / 3-degree Gauss-Kruger CM 135E", 2442, crs);
        assertAliasTipEquals            ("Beijing 1954 / 3GK 135E", crs);
        assertEpsgNameAndIdentifierEqual("Beijing 1954", 4214, crs.getBaseCRS());
        assertEpsgNameAndIdentifierEqual("Beijing 1954", 6214, crs.getDatum());
        assertEpsgNameAndIdentifierEqual("Transverse Mercator", 9807, crs.getConversionFromBase().getMethod());
        assertEpsgNameAndIdentifierEqual("Gauss-Kruger CM 135E", 16323, crs.getConversionFromBase());
        assertAxisDirectionsEqual("EPSG::4530", crs.getCoordinateSystem(), AxisDirection.NORTH, AxisDirection.EAST);

        final ParameterValueGroup parameters = crs.getConversionFromBase().getParameterValues();
        assertEquals("Transverse Mercator",       parameters.getDescriptor().getName().getCode());
        assertEquals("central_meridian",     135, parameters.parameter("central_meridian"  ).doubleValue(), STRICT);
        assertEquals("latitude_of_origin",     0, parameters.parameter("latitude_of_origin").doubleValue(), STRICT);
        assertEquals("scale_factor",           1, parameters.parameter("scale_factor"      ).doubleValue(), STRICT);
        assertEquals("false_easting",     500000, parameters.parameter("false_easting"     ).doubleValue(), STRICT);
        assertEquals("false_northing",         0, parameters.parameter("false_northing"    ).doubleValue(), STRICT);

        assertSame("CRS shall be cached", crs, factory.createCoordinateReferenceSystem("2442"));
    }

    /**
     * Tests the "WGS 72 / UTM zone 10N" projection and ensures
     * that it is not confused with "WGS 72BE / UTM zone 10N".
     * In the EPSG database, those two projected CRS uses the same conversion.
     * However in Apache SIS the conversions must differ because the datum are not the same.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    @DependsOnMethod("testProjected")
    public void testProjectedWithSharedConversion() throws FactoryException {
        assumeNotNull(factory);
        final ProjectedCRS crs = factory.createProjectedCRS("32210");
        assertEpsgNameAndIdentifierEqual("WGS 72 / UTM zone 10N", 32210, crs);
        assertEpsgNameAndIdentifierEqual("WGS 72", 4322, crs.getBaseCRS());
        assertEpsgNameAndIdentifierEqual("World Geodetic System 1972", 6322, crs.getDatum());
        assertEpsgNameAndIdentifierEqual("Transverse Mercator", 9807, crs.getConversionFromBase().getMethod());
        assertEpsgNameAndIdentifierEqual("UTM zone 10N", 16010, crs.getConversionFromBase());
        assertAxisDirectionsEqual("EPSG::4400", crs.getCoordinateSystem(), AxisDirection.EAST, AxisDirection.NORTH);
        verifyTransverseMercatorParmeters(crs.getConversionFromBase().getParameterValues(), -123);

        final ProjectedCRS variant = factory.createProjectedCRS("32410");
        assertEpsgNameAndIdentifierEqual("WGS 72BE / UTM zone 10N", 32410, variant);
        assertEpsgNameAndIdentifierEqual("WGS 72BE", 4324, variant.getBaseCRS());
        assertEpsgNameAndIdentifierEqual("WGS 72 Transit Broadcast Ephemeris", 6324, variant.getDatum());
        assertEpsgNameAndIdentifierEqual("Transverse Mercator", 9807, variant.getConversionFromBase().getMethod());
        assertEpsgNameAndIdentifierEqual("UTM zone 10N", 16010, variant.getConversionFromBase());
        verifyTransverseMercatorParmeters(crs.getConversionFromBase().getParameterValues(), -123);
        assertSame("Operation method", crs.getConversionFromBase().getMethod(),
                                   variant.getConversionFromBase().getMethod());
        assertSame("Coordinate system", crs.getCoordinateSystem(),
                                    variant.getCoordinateSystem());

        assertNotDeepEquals(crs.getConversionFromBase(), variant.getConversionFromBase());
        assertNotDeepEquals(crs, variant);
    }

    /**
     * Tests a projected CRS fetched by its name instead than its code.
     * Tests also {@link EPSGDataAccess#createObject(String)}.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     *
     * @see #testCreateByName()
     */
    @Test
    @DependsOnMethod("testCreateByName")
    public void testProjectedByName() throws FactoryException {
        assumeNotNull(factory);
        final ProjectedCRS crs = factory.createProjectedCRS("NTF (Paris) / Lambert zone I");
        assertEpsgNameAndIdentifierEqual("NTF (Paris) / Lambert zone I", 27571, crs);
        assertEpsgNameAndIdentifierEqual("NTF (Paris)", 4807, crs.getBaseCRS());
        assertEpsgNameAndIdentifierEqual("Lambert Conic Conformal (1SP)", 9801, crs.getConversionFromBase().getMethod());
        assertEpsgNameAndIdentifierEqual("Lambert zone I", 18081, crs.getConversionFromBase());
        assertAxisDirectionsEqual("EPSG::4499", crs.getCoordinateSystem(), AxisDirection.EAST, AxisDirection.NORTH);
        assertSame(crs, factory.createProjectedCRS("27571"));
        /*
         * Gets the CRS using 'createObject'. It will require more SQL
         * statement internally in order to determines the object type.
         */
        assertSame(crs, factory.createObject("27571"));
        assertSame(crs, factory.createObject("NTF (Paris) / Lambert zone I"));
    }

    /**
     * Tests a projected CRS located on the North pole.
     * Axis directions are "South along 90°E" and "South along 180°E".
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    @Ignore("“Lambert Azimuthal Equal Area (Spherical)” projection is not yet implemented.")
    public void testProjectedOnPole() throws FactoryException {
        assumeNotNull(factory);
        final ProjectedCRS crs = factory.createProjectedCRS("3408");
        assertEpsgNameAndIdentifierEqual("NSIDC EASE-Grid North", 3408, crs);
        assertEpsgNameAndIdentifierEqual("Unspecified datum based upon the International 1924 Authalic Sphere", 4053, crs.getBaseCRS());
        assertEpsgNameAndIdentifierEqual("Lambert Azimuthal Equal Area (Spherical)", 1027, crs.getConversionFromBase().getMethod());
        assertEpsgNameAndIdentifierEqual("US NSIDC Equal Area north projection", 3897, crs.getConversionFromBase());

        // TODO: test axis directions.

        assertSame("CRS shall be cached", crs, factory.createCoordinateReferenceSystem("3408"));
    }

    /**
     * Tests the Google projected CRS.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testGoogleProjection() throws FactoryException {
        assumeNotNull(factory);
        final ProjectedCRS crs = factory.createProjectedCRS("3857");
        assertEpsgNameAndIdentifierEqual("WGS 84 / Pseudo-Mercator", 3857, crs);
        assertEpsgNameAndIdentifierEqual("WGS 84", 4326, crs.getBaseCRS());
        assertEpsgNameAndIdentifierEqual("Popular Visualisation Pseudo Mercator", 1024, crs.getConversionFromBase().getMethod());
        assertEpsgNameAndIdentifierEqual("Popular Visualisation Pseudo-Mercator", 3856, crs.getConversionFromBase());
        assertAxisDirectionsEqual("EPSG::4499", crs.getCoordinateSystem(), AxisDirection.EAST, AxisDirection.NORTH);

        assertSame("CRS shall be cached", crs, factory.createCoordinateReferenceSystem("3857"));
    }

    /**
     * Tests the "Barcelona Grid B1" engineering CRS (EPSG::5801).
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testEngineering() throws FactoryException {
        assumeNotNull(factory);
        final EngineeringCRS crs = factory.createEngineeringCRS("EPSG:5801");
        assertEpsgNameAndIdentifierEqual("Barcelona Grid B1", 5801, crs);
        assertEpsgNameAndIdentifierEqual("Barcelona", 9301, crs.getDatum());
        assertAxisDirectionsEqual("EPSG::4500", crs.getCoordinateSystem(), AxisDirection.NORTH, AxisDirection.EAST);
        assertSame("CRS shall be cached", crs, factory.createCoordinateReferenceSystem("5801"));
    }

    /**
     * Tests the "Black Sea height" vertical CRS (EPSG:5735).
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testVertical() throws FactoryException {
        assumeNotNull(factory);
        final VerticalCRS crs = factory.createVerticalCRS("EPSG:5735");
        assertEpsgNameAndIdentifierEqual("Black Sea height", 5735, crs);
        assertEpsgNameAndIdentifierEqual("Black Sea", 5134, crs.getDatum());
        assertSame("CRS shall be cached", crs, factory.createCoordinateReferenceSystem("5735"));
        assertAxisDirectionsEqual("EPSG::6499", crs.getCoordinateSystem(), AxisDirection.UP);
    }

    /**
     * Tests the "NTF (Paris) + NGF IGN69 height" compound CRS (EPSG:7400).
     * This method tests also the domain of validity.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    @DependsOnMethod({"testGeographic2D", "testVertical"})
    public void testCompound() throws FactoryException {
        assumeNotNull(factory);
        final CompoundCRS crs = factory.createCompoundCRS("EPSG:7400");
        assertEpsgNameAndIdentifierEqual("NTF (Paris) + NGF IGN69 height", 7400, crs);

        final List<CoordinateReferenceSystem> components = crs.getComponents();
        assertEquals("components.size()", 2, components.size());
        assertEpsgNameAndIdentifierEqual("NTF (Paris)",      4807, components.get(0));
        assertEpsgNameAndIdentifierEqual("NGF IGN69 height", 5720, components.get(1));

        assertAxisDirectionsEqual("(no EPSG code)", crs.getCoordinateSystem(),
                AxisDirection.NORTH, AxisDirection.EAST, AxisDirection.UP);

        final GeographicBoundingBox bbox = CRS.getGeographicBoundingBox(crs);
        assertNotNull("No bounding box. Maybe an older EPSG database is used?", bbox);
        assertEquals("southBoundLatitude", 42.25, bbox.getSouthBoundLatitude(), STRICT);
        assertEquals("northBoundLatitude", 51.10, bbox.getNorthBoundLatitude(), STRICT);
        assertEquals("westBoundLongitude", -5.20, bbox.getWestBoundLongitude(), STRICT);
        assertEquals("eastBoundLongitude",  8.23, bbox.getEastBoundLongitude(), STRICT);

        assertSame("CRS shall be cached", crs, factory.createCoordinateReferenceSystem("7400"));
    }

    /**
     * Tests a legacy geographic CRS (no longer supported by EPSG).
     * This test verifies that the expected warnings are logged.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    @DependsOnMethod("testGeographic2D")
    public void testDeprecatedGeographic() throws FactoryException {
        assumeNotNull(factory);

        listener.maximumLogCount = 2;
        expectedLogWords = new String[][] {
            {"EPSG:6405"},                      // Coordinate System 6405 is no longer supported by EPSG
            {"EPSG:63266405", "4326"}           // EPSG no longer support codes in the 60000000 series.
        };

        final GeographicCRS crs = factory.createGeographicCRS("63266405");
        assertEpsgNameAndIdentifierEqual("WGS 84 (deg)", 63266405, crs);
        assertAxisDirectionsEqual(null, crs.getCoordinateSystem(), AxisDirection.NORTH, AxisDirection.EAST);

        assertSame("CRS shall be cached", crs, factory.createCoordinateReferenceSystem("63266405"));
    }

    /**
     * Tests a deprecated projected CRS.
     * This test verifies that the expected warnings are logged.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    @DependsOnMethod("testDeprecatedGeographic")
    public void testDeprecatedProjected() throws FactoryException {
        assumeNotNull(factory);

        listener.maximumLogCount = 3;
        expectedLogWords = new String[][] {
            {"EPSG:9823",  "1029"},              // Operation method 9823 has been replaced by 1029
            {"EPSG:19968", "4086"},              // Coordinate Operation 19968 has been replaced by 4086
            {"EPSG:3786",  "4088"}               // Coordinate Reference System 3786 has been replaced by 4088
        };

        final ProjectedCRS crs = factory.createProjectedCRS("3786");
        assertEpsgNameAndIdentifierEqual("World Equidistant Cylindrical (Sphere)", 3786, crs);
        assertEpsgNameAndIdentifierEqual("Equidistant Cylindrical (Spherical)", 9823, crs.getConversionFromBase().getMethod());
        assertAxisDirectionsEqual("EPSG::4499", crs.getCoordinateSystem(), AxisDirection.EAST, AxisDirection.NORTH);
        assertEquals("All warnings should have been logged at this point.", 0, listener.maximumLogCount);

        final ProjectedCRS replacement = factory.createProjectedCRS("4088");
        assertEpsgNameAndIdentifierEqual("World Equidistant Cylindrical (Sphere)", 4088, replacement);
        assertEpsgNameAndIdentifierEqual("Equidistant Cylindrical (Spherical)", 1029, replacement.getConversionFromBase().getMethod());

        assertSame("Base CRS", crs.getBaseCRS(), replacement.getBaseCRS());
        assertSame("Coordinate system", crs.getCoordinateSystem(), replacement.getCoordinateSystem());

        assertSame("CRS shall be cached", crs, factory.createCoordinateReferenceSystem("3786"));
        assertSame("CRS shall be cached", replacement, factory.createCoordinateReferenceSystem("4088"));
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
        assumeNotNull(factory);
        assertSame   (factory.createUnit("9002"), factory.createUnit("foot"));
        assertNotSame(factory.createUnit("9001"), factory.createUnit("foot"));
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
        try {
            factory.createGeographicCRS("WGS83");
            fail("Should not find a geographic CRS named “WGS83” (the actual name is “WGS 84”).");
        } catch (NoSuchAuthorityCodeException e) {
            // This is the expected exception.
            assertEquals("WGS83", e.getAuthorityCode());
        }
    }

    /**
     * Tests the {@link EPSGDataAccess#getDescriptionText(String)} method.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testDescriptionText() throws FactoryException {
        assumeNotNull(factory);

        assertEquals("World Geodetic System 1984", factory.getDescriptionText( "6326").toString(Locale.US));
        assertEquals("Mean Sea Level",             factory.getDescriptionText( "5100").toString(Locale.US));
        assertEquals("NTF (Paris) / Nord France",  factory.getDescriptionText("27591").toString(Locale.US));
        assertEquals("NTF (Paris) / France II",    factory.getDescriptionText("27582").toString(Locale.US));
        assertEquals("Ellipsoidal height",         factory.getDescriptionText(   "84").toString(Locale.US));
    }

    /**
     * Tests the "UTM zone 10N" conversion (EPSG::16010).
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    @DependsOnMethod("testProjectedWithSharedConversion")
    public void testConversion() throws FactoryException {
        assumeNotNull(factory);
        /*
         * Fetch directly the "UTM zone 10N" operation. Because this operation was not obtained in
         * the context of a projected CRS, the source and target CRS shall be unspecified (i.e. null).
         */
        final CoordinateOperation operation = factory.createCoordinateOperation("16010");
        assertEpsgNameAndIdentifierEqual("UTM zone 10N", 16010, operation);
        assertInstanceOf("EPSG::16010", Conversion.class, operation);
        assertNull("sourceCRS", operation.getSourceCRS());
        assertNull("targetCRS", operation.getTargetCRS());
        assertNull("transform", operation.getMathTransform());
        /*
         * Fetch the "WGS 72 / UTM zone 10N" projected CRS.
         * The operation associated to this CRS should now define the source and target CRS.
         */
        final ProjectedCRS crs = factory.createProjectedCRS("32210");
        final CoordinateOperation projection = crs.getConversionFromBase();
        assertEpsgNameAndIdentifierEqual("WGS 72 / UTM zone 10N", 32210, crs);
        assertEpsgNameAndIdentifierEqual("UTM zone 10N", 16010, projection);
        assertNotSame("The defining conversion and the actual conversion should differ since the "
                + "actual conversion should have semi-axis length values.", projection, operation);
        assertInstanceOf("EPSG::16010", Projection.class, projection);
        assertNotNull("sourceCRS", projection.getSourceCRS());
        assertNotNull("targetCRS", projection.getTargetCRS());
        assertNotNull("transform", projection.getMathTransform());
        /*
         * Compare the conversion obtained directly with the conversion obtained
         * indirectly through a projected CRS. Both should use the same method.
         */
        final OperationMethod copMethod = ((Conversion) operation) .getMethod();
        final OperationMethod crsMethod = ((Conversion) projection).getMethod();
        assertEpsgNameAndIdentifierEqual("Transverse Mercator", 9807, copMethod);
        assertEpsgNameAndIdentifierEqual("Transverse Mercator", 9807, crsMethod);
        try {
            assertSame("Conversion method", copMethod, crsMethod);
            assertSame("Conversion method", copMethod, factory.createOperationMethod("9807"));
        } catch (AssertionError error) {
            out.println("The following contains more information about a JUnit test failure.");
            out.println("See the JUnit report for the stack trace. Below is a cache dump.");
            out.println("See the operation method EPSG::9807 and compare with:");
            out.print  ("  - Method obtained directly:   "); out.println(System.identityHashCode(copMethod));
            out.print  ("  - Method obtained indirectly: "); out.println(System.identityHashCode(crsMethod));
            out.println("Content of EPSGFactory cache:");
            factory.printCacheContent(out);
            throw error;
        }
    }

    /**
     * Tests longitude rotation (EPSG::1764). This is a very simple case for checking
     * that this part is okay before to try more complex transformations.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testSimpleTransformation() throws FactoryException {
        assumeNotNull(factory);
        final CoordinateOperation operation = factory.createCoordinateOperation("1764");
        assertEpsgNameAndIdentifierEqual("NTF (Paris) to NTF (2)", 1764, operation);
        assertInstanceOf("EPSG::1764", Transformation.class, operation);
        assertSame("Operation shall be cached", operation, factory.createCoordinateOperation("1764"));
    }

    /**
     * Tests "BD72 to WGS 84 (1)" (EPSG::1609) transformation. This one has an unusual unit for the
     * "Scale difference" parameter (EPSG::8611). The value is 0.999999 and the unit is "unity" (EPSG::9201)
     * instead of the usual "parts per million" (EPSG:9202).
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    @DependsOnMethod("testSimpleTransformation")
    public void testTransformation() throws FactoryException {
        assumeNotNull(factory);
        final CoordinateOperation operation = factory.createCoordinateOperation("1609");
        assertEpsgNameAndIdentifierEqual("BD72 to WGS 84 (1)", 1609, operation);
        assertEquals(1.0, ((AbstractCoordinateOperation) operation).getLinearAccuracy(), STRICT);
        assertSame("Operation shall be cached", operation, factory.createCoordinateOperation("1609"));
    }

    /**
     * Tests {@link EPSGDataAccess#createFromCoordinateReferenceSystemCodes(String, String)}.
     * This method verifies the presence of 3 {@link Transformation} instances between the same source and target CRS.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    @DependsOnMethod("testTransformation")
    public void testCreateFromCoordinateReferenceSystemCodes() throws FactoryException {
        assumeNotNull(factory);
        /*
         * ED50 (4230)  to  WGS 84 (4326)  using
         * Geocentric translations (9603).
         * Accuracy = 2.5
         */
        final CoordinateOperation      operation1 = factory.createCoordinateOperation("1087");
        final CoordinateReferenceSystem sourceCRS = operation1.getSourceCRS();
        final CoordinateReferenceSystem targetCRS = operation1.getTargetCRS();
        final MathTransform             transform = operation1.getMathTransform();
        assertInstanceOf("EPSG::1087", Transformation.class, operation1);
        assertEpsgNameAndIdentifierEqual("ED50 to WGS 84 (37)", 1087, operation1);
        assertEpsgNameAndIdentifierEqual("ED50",                4230, sourceCRS);
        assertEpsgNameAndIdentifierEqual("WGS 84",              4326, targetCRS);
        assertFalse("transform.isIdentity()", operation1.getMathTransform().isIdentity());
        assertEquals(2.5, AbstractCoordinateOperation.castOrCopy(operation1).getLinearAccuracy(), STRICT);
        /*
         * ED50 (4230)  to  WGS 84 (4326)  using
         * Position Vector 7-param. transformation (9606).
         * Accuracy = 1.5
         */
        final CoordinateOperation operation2 = factory.createCoordinateOperation("1631");
        assertEpsgNameAndIdentifierEqual("ED50 to WGS 84 (27)", 1631, operation2);
        assertInstanceOf("EPSG::1631", Transformation.class, operation2);
        assertSame ("sourceCRS", sourceCRS, operation2.getSourceCRS());
        assertSame ("targetCRS", targetCRS, operation2.getTargetCRS());
        assertFalse("transform.isIdentity()", operation2.getMathTransform().isIdentity());
        assertFalse("Should be a more accurate transformation.", transform.equals(operation2.getMathTransform()));
        assertEquals(1.5, AbstractCoordinateOperation.castOrCopy(operation2).getLinearAccuracy(), STRICT);
        /*
         * ED50 (4230)  to  WGS 84 (4326)  using
         * Coordinate Frame rotation (9607).
         * Accuracy = 1.0
         */
        final CoordinateOperation operation3 = factory.createCoordinateOperation("1989");
        assertInstanceOf("EPSG::1989", Transformation.class, operation3);
        assertEpsgNameAndIdentifierEqual("ED50 to WGS 84 (34)", 1989, operation3);
        assertSame ("sourceCRS", sourceCRS, operation3.getSourceCRS());
        assertSame ("targetCRS", targetCRS, operation3.getTargetCRS());
        assertFalse("transform.isIdentity()", operation3.getMathTransform().isIdentity());
        assertFalse("Should be a more accurate transformation.", transform.equals(operation3.getMathTransform()));
        assertEquals(1.0, AbstractCoordinateOperation.castOrCopy(operation3).getLinearAccuracy(), STRICT);
        /*
         * Creates from CRS codes. There is 40 such operations in EPSG version 6.7.
         * The preferred one (according the "supersession" table) is EPSG:1612.
         *
         * Note: PostgreSQL because its "ORDER BY" clause put null values last, while Access and HSQL put them first.
         * The PostgreSQL behavior is better for what we want (operations with unknown accuracy last). Unfortunately,
         * I do not know yet how to instructs Access to put null values last using standard SQL
         * ("IIF" is not standard, and Access does not seem to understand "CASE ... THEN" clauses).
         */
        final Set<CoordinateOperation> all = factory.createFromCoordinateReferenceSystemCodes("4230", "4326");
        assertTrue("Number of coordinate operations.", all.size() >= 3);
        assertTrue("contains(“EPSG::1087”)", all.contains(operation1));
        assertTrue("contains(“EPSG::1631”)", all.contains(operation2));
        assertTrue("contains(“EPSG::1989”)", all.contains(operation3));

        int count = 0;
        listener.maximumLogCount = all.size();              // Ignore log message for unsupported operation methods.
        for (final CoordinateOperation tr : all) {
            assertSame("sourceCRS", sourceCRS, tr.getSourceCRS());
            assertSame("targetCRS", targetCRS, tr.getTargetCRS());
            if (count == 0) {   // Preferred transformation (see above comment).
                assertEpsgNameAndIdentifierEqual("ED50 to WGS 84 (23)", 1612, tr);
            }
            count++;
        }
        assertEquals(count, all.size());        // Size may have been modified after above loop.
    }

    /**
     * Tests {@link EPSGFactory#newIdentifiedObjectFinder()} method with a geographic CRS.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    @DependsOnMethod("testWGS84")
    public void testFindGeographic() throws FactoryException {
        assumeNotNull(factory);
        final IdentifiedObjectFinder finder = factory.newIdentifiedObjectFinder();
        final DefaultGeographicCRS crs = (DefaultGeographicCRS) CRS.fromWKT(
                "GEOGCS[“WGS 84”,\n" +
                "  DATUM[“WGS 84”,\n" +     // Use the alias instead than primary name for forcing a deeper search.
                "    SPHEROID[“WGS 1984”, 6378137.0, 298.257223563]],\n" +  // Different name for forcing a deeper search.
                "  PRIMEM[“Greenwich”, 0.0],\n" +
                "  UNIT[“degree”, 0.017453292519943295],\n" +
                "  AXIS[“Geodetic latitude”, NORTH],\n" +
                "  AXIS[“Geodetic longitude”, EAST]]");
        /*
         * First, search for a CRS with axis order that does not match the ones in the EPSG database.
         * IdentifiedObjectFinder should not accept EPSG::4326 as a match for the given CRS.
         */
        assertTrue("Full scan should be enabled by default.", finder.isFullScanAllowed());
        assertTrue("Should not find WGS84 because the axis order is not the same.",
                   finder.find(crs.forConvention(AxesConvention.NORMALIZED)).isEmpty());
        /*
         * Ensure that the cache is empty.
         */
        finder.setFullScanAllowed(false);
        assertTrue("Should not find without a full scan, because the WKT contains no identifier " +
                   "and the CRS name is ambiguous (more than one EPSG object have this name).",
                   finder.find(crs).isEmpty());
        /*
         * Scan the database for searching the CRS.
         */
        finder.setFullScanAllowed(true);
        final IdentifiedObject found = finder.findSingleton(crs);
        assertNotNull("With full scan allowed, the CRS should be found.", found);
        assertEpsgNameAndIdentifierEqual("WGS 84", 4326, found);
        /*
         * Should find the CRS without the need of a full scan, because of the cache.
         */
        finder.setFullScanAllowed(false);
        assertSame("The CRS should still in the cache.", found, finder.findSingleton(crs));
    }
}
