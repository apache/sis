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

import org.opengis.metadata.Identifier;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.util.FactoryException;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.referencing.factory.UnavailableFactoryException;

// Test imports
import org.junit.Rule;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.LoggingWatcher;

import static org.opengis.test.Assert.*;
import static org.junit.Assume.assumeNotNull;


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
        if (factory == null) try {
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
            for (final String word : expectedLogWords[expectedLogWords.length - (maximumLogCount + 1)]) {
                assertTrue(message, message.contains(word));
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
     * Returns the first identifier for the specified object.
     *
     * @param object The object for which to get the identifier.
     * @return The first identifier of the given object.
     */
    private static String getIdentifier(final IdentifiedObject object) {
        return object.getIdentifiers().iterator().next().getCode();
    }

    /**
     * Returns the EPSG code of the operation method for the given projected CRS.
     */
    private static String getOperationMethod(final ProjectedCRS crs) {
        final Identifier id = crs.getConversionFromBase().getMethod().getIdentifiers().iterator().next();
        return id.getCodeSpace() + ':' + id.getCode();
    }

    /**
     * Tests a geographic CRS.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void test4274() throws FactoryException {
        assumeNotNull(factory);
        final GeographicCRS crs = factory.createGeographicCRS("4274");
        assertEquals("identifier", "4274", getIdentifier(crs));
        assertAxisDirectionsEqual("axes", crs.getCoordinateSystem(), AxisDirection.NORTH, AxisDirection.EAST);
    }

    /**
     * Tests a geographic CRS.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void test4617() throws FactoryException {
        assumeNotNull(factory);
        final GeographicCRS crs = factory.createGeographicCRS("EPSG:4617");
        assertEquals("identifier", "4617", getIdentifier(crs));
        assertAxisDirectionsEqual("axes", crs.getCoordinateSystem(), AxisDirection.NORTH, AxisDirection.EAST);
    }

    /**
     * Tests a vertical CRS.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void test5735() throws FactoryException {
        assumeNotNull(factory);
        final VerticalCRS crs = factory.createVerticalCRS("EPSG:5735");
        assertEquals("identifier", "5735", getIdentifier(crs));
        assertAxisDirectionsEqual("axes", crs.getCoordinateSystem(), AxisDirection.UP);
    }

    /**
     * Tests a geocentric CRS.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void test4915() throws FactoryException {
        assumeNotNull(factory);
        final GeocentricCRS crs = factory.createGeocentricCRS("EPSG:4915");
        assertEquals("identifier", "4915", getIdentifier(crs));
        assertAxisDirectionsEqual("axes", crs.getCoordinateSystem(),
                AxisDirection.GEOCENTRIC_X, AxisDirection.GEOCENTRIC_Y, AxisDirection.GEOCENTRIC_Z);
    }

    /**
     * Tests a three-dimensional geographic CRS.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void test4993() throws FactoryException {
        assumeNotNull(factory);
        final GeographicCRS crs = factory.createGeographicCRS("EPSG:4993");
        assertEquals("identifier", "4993", getIdentifier(crs));
        assertAxisDirectionsEqual("axes", crs.getCoordinateSystem(),
                AxisDirection.NORTH, AxisDirection.EAST, AxisDirection.UP);
    }

    /**
     * Tests a projected CRS using Transverse Mercator projection.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void test2027() throws FactoryException {
        assumeNotNull(factory);
        final ProjectedCRS crs = factory.createProjectedCRS("2027");
        assertEquals("identifier", "2027", getIdentifier(crs));
        assertAxisDirectionsEqual("axes", crs.getCoordinateSystem(), AxisDirection.EAST, AxisDirection.NORTH);

        final ParameterValueGroup parameters = crs.getConversionFromBase().getParameterValues();
        assertEquals("central_meridian",     -93, parameters.parameter("central_meridian"  ).doubleValue(), STRICT);
        assertEquals("latitude_of_origin",     0, parameters.parameter("latitude_of_origin").doubleValue(), STRICT);
        assertEquals("scale_factor",      0.9996, parameters.parameter("scale_factor"      ).doubleValue(), STRICT);
        assertEquals("false_easting",     500000, parameters.parameter("false_easting"     ).doubleValue(), STRICT);
        assertEquals("false_northing",         0, parameters.parameter("false_northing"    ).doubleValue(), STRICT);
    }

    /**
     * Tests a projected CRS using Transverse Mercator projection.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void test2442() throws FactoryException {
        assumeNotNull(factory);
        final ProjectedCRS crs = factory.createProjectedCRS(" EPSG : 2442 ");
        assertEquals("identifier", "2442", getIdentifier(crs));
        assertAxisDirectionsEqual("axes", crs.getCoordinateSystem(), AxisDirection.NORTH, AxisDirection.EAST);

        final ParameterValueGroup parameters = crs.getConversionFromBase().getParameterValues();
        assertEquals("central_meridian",     135, parameters.parameter("central_meridian"  ).doubleValue(), STRICT);
        assertEquals("latitude_of_origin",     0, parameters.parameter("latitude_of_origin").doubleValue(), STRICT);
        assertEquals("scale_factor",           1, parameters.parameter("scale_factor"      ).doubleValue(), STRICT);
        assertEquals("false_easting",     500000, parameters.parameter("false_easting"     ).doubleValue(), STRICT);
        assertEquals("false_northing",         0, parameters.parameter("false_northing"    ).doubleValue(), STRICT);
    }

    /**
     * Tests a projected CRS using Lambert Azimuthal Equal Area (Spherical) projection.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    @Ignore("“Lambert Azimuthal Equal Area (Spherical)” projection is not yet implemented.")
    public void test3408() throws FactoryException {
        assumeNotNull(factory);
        final ProjectedCRS crs = factory.createProjectedCRS("3408");
        assertEquals("identifier", "3408", getIdentifier(crs));
        assertEquals("name", "NSIDC EASE-Grid North", crs.getName().getCode());
        assertEquals("method", "EPSG:1027", getOperationMethod(crs));
        assertAxisDirectionsEqual("axes", crs.getCoordinateSystem(), AxisDirection.EAST, AxisDirection.NORTH);
    }

    /**
     * Tests the Google projection.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void test3857() throws FactoryException {
        assumeNotNull(factory);
        final ProjectedCRS crs = factory.createProjectedCRS("3857");
        assertEquals("identifier", "3857", getIdentifier(crs));
        assertEquals("name", "WGS 84 / Pseudo-Mercator", crs.getName().getCode());
        assertEquals("method", "EPSG:1024", getOperationMethod(crs));
        assertAxisDirectionsEqual("axes", crs.getCoordinateSystem(), AxisDirection.EAST, AxisDirection.NORTH);
    }

    /**
     * Tests an engineering CRS.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void test5801() throws FactoryException {
        assumeNotNull(factory);
        final EngineeringCRS crs = factory.createEngineeringCRS("EPSG:5801");
        assertEquals("identifier", "5801", getIdentifier(crs));
        assertAxisDirectionsEqual("axes", crs.getCoordinateSystem(), AxisDirection.NORTH, AxisDirection.EAST);
    }

    /**
     * Tests a compound CRS.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void test7400() throws FactoryException {
        assumeNotNull(factory);
        final CompoundCRS crs = factory.createCompoundCRS("EPSG:7400");
        assertEquals("identifier", "7400", getIdentifier(crs));
        assertAxisDirectionsEqual("axes", crs.getCoordinateSystem(),
                AxisDirection.NORTH, AxisDirection.EAST, AxisDirection.UP);
    }

    /**
     * Tests a legacy geographic CRS (no longer supported by EPSG).
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void test63266405() throws FactoryException {
        assumeNotNull(factory);

        listener.maximumLogCount = 2;
        expectedLogWords = new String[][] {
            {"EPSG:6405"},                      // Coordinate System 6405 is no longer supported by EPSG
            {"EPSG:63266405", "4326"}           // EPSG no longer support codes in the 60000000 series.
        };

        final GeographicCRS crs = factory.createGeographicCRS("63266405");
        assertEquals("identifier", "63266405", getIdentifier(crs));
        assertEquals("name", "WGS 84 (deg)", crs.getName().getCode());
        assertAxisDirectionsEqual("axes", crs.getCoordinateSystem(), AxisDirection.NORTH, AxisDirection.EAST);
    }

    /**
     * Tests a deprecated projected CRS.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void test3786() throws FactoryException {
        assumeNotNull(factory);

        listener.maximumLogCount = 3;
        expectedLogWords = new String[][] {
            {"EPSG:9823",  "1029"},              // Operation method 9823 has been replaced by 1029
            {"EPSG:19968", "4086"},              // Coordinate Operation 19968 has been replaced by 4086
            {"EPSG:3786",  "4088"}               // Coordinate Reference System 3786 has been replaced by 4088
        };

        final ProjectedCRS crs = factory.createProjectedCRS("3786");
        assertEquals("identifier", "3786", getIdentifier(crs));
        assertEquals("name", "World Equidistant Cylindrical (Sphere)", crs.getName().getCode());
        assertEquals("method", "EPSG:9823", getOperationMethod(crs));
        assertAxisDirectionsEqual("axes", crs.getCoordinateSystem(), AxisDirection.EAST, AxisDirection.NORTH);
    }

    /**
     * Tests the replacement of the deprecated EPSG::3786 projected CRS.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void test4088() throws FactoryException {
        assumeNotNull(factory);
        final ProjectedCRS crs = factory.createProjectedCRS("4088");
        assertEquals("identifier", "4088", getIdentifier(crs));
        assertEquals("name", "World Equidistant Cylindrical (Sphere)", crs.getName().getCode());
        assertEquals("method", "EPSG:1029", getOperationMethod(crs));
        assertAxisDirectionsEqual("axes", crs.getCoordinateSystem(), AxisDirection.EAST, AxisDirection.NORTH);
    }
}
