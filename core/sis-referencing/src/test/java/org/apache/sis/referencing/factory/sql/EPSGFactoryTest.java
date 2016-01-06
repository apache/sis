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
import java.util.Locale;
import org.opengis.metadata.Identifier;
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
import org.apache.sis.referencing.operation.AbstractCoordinateOperation;
import org.apache.sis.referencing.factory.UnavailableFactoryException;
import org.apache.sis.referencing.CRS;

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

import static org.apache.sis.test.Assert.*;
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
    public void testGeographic2D_4274() throws FactoryException {
        assumeNotNull(factory);
        final GeographicCRS crs = factory.createGeographicCRS("4274");
        assertEquals("identifier", "4274", getIdentifier(crs));
        assertSame("CRS shall be cached", crs, factory.createCoordinateReferenceSystem("4274"));
        assertAxisDirectionsEqual("axes", crs.getCoordinateSystem(), AxisDirection.NORTH, AxisDirection.EAST);
    }

    /**
     * Tests a geographic CRS.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testGeographic2D_4617() throws FactoryException {
        assumeNotNull(factory);
        final GeographicCRS crs = factory.createGeographicCRS("EPSG:4617");
        assertEquals("identifier", "4617", getIdentifier(crs));
        assertSame("CRS shall be cached", crs, factory.createCoordinateReferenceSystem("4617"));
        assertAxisDirectionsEqual("axes", crs.getCoordinateSystem(), AxisDirection.NORTH, AxisDirection.EAST);
    }

    /**
     * Tests a three-dimensional geographic CRS.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testGeographic3D_4993() throws FactoryException {
        assumeNotNull(factory);
        final GeographicCRS crs = factory.createGeographicCRS("EPSG:4993");
        assertEquals("identifier", "4993", getIdentifier(crs));
        assertSame("CRS shall be cached", crs, factory.createCoordinateReferenceSystem("4993"));
        assertAxisDirectionsEqual("axes", crs.getCoordinateSystem(),
                AxisDirection.NORTH, AxisDirection.EAST, AxisDirection.UP);
    }

    /**
     * Tests a geocentric CRS.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testGeocentric_4915() throws FactoryException {
        assumeNotNull(factory);
        final GeocentricCRS crs = factory.createGeocentricCRS("EPSG:4915");
        assertEquals("identifier", "4915", getIdentifier(crs));
        assertSame("CRS shall be cached", crs, factory.createCoordinateReferenceSystem("4915"));
        assertAxisDirectionsEqual("axes", crs.getCoordinateSystem(),
                AxisDirection.GEOCENTRIC_X, AxisDirection.GEOCENTRIC_Y, AxisDirection.GEOCENTRIC_Z);
    }

    /**
     * Tests a vertical CRS.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testVertical_5735() throws FactoryException {
        assumeNotNull(factory);
        final VerticalCRS crs = factory.createVerticalCRS("EPSG:5735");
        assertEquals("identifier", "5735", getIdentifier(crs));
        assertSame("CRS shall be cached", crs, factory.createCoordinateReferenceSystem("5735"));
        assertAxisDirectionsEqual("axes", crs.getCoordinateSystem(), AxisDirection.UP);
    }

    /**
     * Tests a projected CRS using Transverse Mercator projection.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testProjected_2027() throws FactoryException {
        assumeNotNull(factory);
        final ProjectedCRS crs = factory.createProjectedCRS("2027");
        assertEquals("identifier", "2027", getIdentifier(crs));
        assertSame("CRS shall be cached", crs, factory.createCoordinateReferenceSystem("2027"));
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
    public void testProjected_2442() throws FactoryException {
        assumeNotNull(factory);
        final ProjectedCRS crs = factory.createProjectedCRS(" EPSG : 2442 ");
        assertEquals("identifier", "2442", getIdentifier(crs));
        assertSame("CRS shall be cached", crs, factory.createCoordinateReferenceSystem("2442"));
        assertAxisDirectionsEqual("axes", crs.getCoordinateSystem(), AxisDirection.NORTH, AxisDirection.EAST);

        final ParameterValueGroup parameters = crs.getConversionFromBase().getParameterValues();
        assertEquals("central_meridian",     135, parameters.parameter("central_meridian"  ).doubleValue(), STRICT);
        assertEquals("latitude_of_origin",     0, parameters.parameter("latitude_of_origin").doubleValue(), STRICT);
        assertEquals("scale_factor",           1, parameters.parameter("scale_factor"      ).doubleValue(), STRICT);
        assertEquals("false_easting",     500000, parameters.parameter("false_easting"     ).doubleValue(), STRICT);
        assertEquals("false_northing",         0, parameters.parameter("false_northing"    ).doubleValue(), STRICT);
    }

    /**
     * Tests the "WGS 72 / UTM zone 10N" projection and ensures
     * that it is not confused with "WGS 72BE / UTM zone 10N".
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testProjected_32210() throws FactoryException {
        assumeNotNull(factory);
        final ProjectedCRS crs = factory.createProjectedCRS("32210");
        assertEquals("name", "WGS 72 / UTM zone 10N", crs.getName().getCode());
        assertEquals("Conversion name", "UTM zone 10N", crs.getConversionFromBase().getName().getCode());

        final ProjectedCRS variant = factory.createProjectedCRS("32410");
        assertEquals("name", "WGS 72BE / UTM zone 10N", variant.getName().getCode());
        assertEquals("Conversion name", "UTM zone 10N", variant.getConversionFromBase().getName().getCode());
        assertSame("Operation method", crs.getConversionFromBase().getMethod(),
                                   variant.getConversionFromBase().getMethod());

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
    public void testProjected_27571() throws FactoryException {
        assumeNotNull(factory);
        final ProjectedCRS crs = factory.createProjectedCRS("NTF (Paris) / Lambert zone I");
        assertAxisDirectionsEqual("axes", crs.getCoordinateSystem(), AxisDirection.EAST, AxisDirection.NORTH);
        assertEquals("27571", getIdentifier(crs));
        assertSame(crs, factory.createProjectedCRS("27571"));
        /*
         * Gets the CRS using 'createObject'. It will require more SQL
         * statement internally in order to determines the object type.
         */
        assertSame(crs, factory.createObject("27571"));
        assertSame(crs, factory.createObject("NTF (Paris) / Lambert zone I"));
    }

    /**
     * Tests a projected CRS using Lambert Azimuthal Equal Area (Spherical) projection.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    @Ignore("“Lambert Azimuthal Equal Area (Spherical)” projection is not yet implemented.")
    public void testProjected_3408() throws FactoryException {
        assumeNotNull(factory);
        final ProjectedCRS crs = factory.createProjectedCRS("3408");
        assertEquals("identifier", "3408", getIdentifier(crs));
        assertEquals("name", "NSIDC EASE-Grid North", crs.getName().getCode());
        assertEquals("method", "EPSG:1027", getOperationMethod(crs));
        assertAxisDirectionsEqual("axes", crs.getCoordinateSystem(), AxisDirection.EAST, AxisDirection.NORTH);
        assertSame("CRS shall be cached", crs, factory.createCoordinateReferenceSystem("3408"));
    }

    /**
     * Tests the Google projection.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testProjected_3857() throws FactoryException {
        assumeNotNull(factory);
        final ProjectedCRS crs = factory.createProjectedCRS("3857");
        assertEquals("identifier", "3857", getIdentifier(crs));
        assertEquals("name", "WGS 84 / Pseudo-Mercator", crs.getName().getCode());
        assertEquals("method", "EPSG:1024", getOperationMethod(crs));
        assertAxisDirectionsEqual("axes", crs.getCoordinateSystem(), AxisDirection.EAST, AxisDirection.NORTH);
        assertSame("CRS shall be cached", crs, factory.createCoordinateReferenceSystem("3857"));
    }

    /**
     * Tests an engineering CRS.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testEngineering_5801() throws FactoryException {
        assumeNotNull(factory);
        final EngineeringCRS crs = factory.createEngineeringCRS("EPSG:5801");
        assertEquals("identifier", "5801", getIdentifier(crs));
        assertAxisDirectionsEqual("axes", crs.getCoordinateSystem(), AxisDirection.NORTH, AxisDirection.EAST);
        assertSame("CRS shall be cached", crs, factory.createCoordinateReferenceSystem("5801"));
    }

    /**
     * Tests a compound CRS and its domain of validity.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testCompound_7400() throws FactoryException {
        assumeNotNull(factory);
        final CompoundCRS crs = factory.createCompoundCRS("EPSG:7400");
        assertEquals("identifier", "7400", getIdentifier(crs));
        assertAxisDirectionsEqual("axes", crs.getCoordinateSystem(),
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
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testDeprecatedGeographic_63266405() throws FactoryException {
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
        assertSame("CRS shall be cached", crs, factory.createCoordinateReferenceSystem("63266405"));
    }

    /**
     * Tests a deprecated projected CRS.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testDeprecatedProjected_3786() throws FactoryException {
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
        assertSame("CRS shall be cached", crs, factory.createCoordinateReferenceSystem("3786"));
    }

    /**
     * Tests the replacement of the deprecated EPSG::3786 projected CRS.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testProjected_4088() throws FactoryException {
        assumeNotNull(factory);
        final ProjectedCRS crs = factory.createProjectedCRS("4088");
        assertEquals("identifier", "4088", getIdentifier(crs));
        assertEquals("name", "World Equidistant Cylindrical (Sphere)", crs.getName().getCode());
        assertEquals("method", "EPSG:1029", getOperationMethod(crs));
        assertAxisDirectionsEqual("axes", crs.getCoordinateSystem(), AxisDirection.EAST, AxisDirection.NORTH);
        assertSame("CRS shall be cached", crs, factory.createCoordinateReferenceSystem("4088"));
    }

    /**
     * Tests the creation of CRS using name instead of primary key.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     *
     * @see #testProjected_27571()
     */
    @Test
    public final void testCreateByName() throws FactoryException {
        assumeNotNull(factory);
        assertSame   (factory.createUnit("9002"), factory.createUnit("foot"));
        assertNotSame(factory.createUnit("9001"), factory.createUnit("foot"));

        final CoordinateSystem cs = factory.createCoordinateSystem(
                "Ellipsoidal 2D CS. Axes: latitude, longitude. Orientations: north, east. UoM: degree");
        assertEquals("6422", getIdentifier(cs));
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
     * Tests the creation of a {@link Conversion} object.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    @DependsOnMethod("testProjected_32210")
    public void testCreateConversion() throws FactoryException {
        assumeNotNull(factory);
        /*
         * Fetch directly the "UTM zone 10N" operation. Because this operation was not obtained in
         * the context of a projected CRS, the source and target CRS shall be unspecified (i.e. null).
         */
        final CoordinateOperation operation = factory.createCoordinateOperation("16010");
        assertEquals("Conversion name", "UTM zone 10N", operation.getName().getCode());
        assertEquals("Defining conversion identifier", "16010", getIdentifier(operation));
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
        assertEquals("Projected CRS identifier", "32210", getIdentifier(crs));
        assertEquals("Conversion identifier",    "16010", getIdentifier(projection));
        assertInstanceOf("EPSG::16010", Projection.class, projection);
        assertNotNull("sourceCRS", projection.getSourceCRS());
        assertNotNull("targetCRS", projection.getTargetCRS());
        assertNotNull("transform", projection.getMathTransform());
        assertNotSame("The defining conversion and the actual conversion should differ since the "
                + "actual conversion should have semi-axis length values.", projection, operation);
        /*
         * Compare the conversion obtained directly with the conversion obtained
         * indirectly through a projected CRS. Both should use the same method.
         */
        final OperationMethod copMethod = ((Conversion) operation) .getMethod();
        final OperationMethod crsMethod = ((Conversion) projection).getMethod();
        assertEquals("Defining conversion method",      "9807", getIdentifier(copMethod));
        assertEquals("Projected CRS conversion method", "9807", getIdentifier(crsMethod));
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
     * Tests longitude rotation. This is a very simple case for checking
     * that this part is okay before to try more complex transformations.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    public void testTransformation_1764() throws FactoryException {
        assumeNotNull(factory);
        assertInstanceOf("EPSG::1764", Transformation.class, factory.createCoordinateOperation("1764"));
    }

    /**
     * Tests "BD72 to WGS 84 (1)" (EPSG:1609) creation. This one has an unusual unit for the
     * "Scale difference" parameter (EPSG:8611). The value is 0.999999 and the unit is "unity"
     * (EPSG:9201) instead of the usual "parts per million" (EPSG:9202).
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    @DependsOnMethod("testTransformation_1764")
    public void testTransformation_1609() throws FactoryException {
        assumeNotNull(factory);
        assertEquals(1.0, AbstractCoordinateOperation.castOrCopy(factory.createCoordinateOperation("1609")).getLinearAccuracy(), STRICT);
    }

    /**
     * Tests the creation of 3 {@link Transformation} objects between the same source and target CRS.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    @DependsOnMethod("testTransformation_1764")
    public void testTransformations() throws FactoryException {
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
        assertEquals("ED50 to WGS84", "1087", getIdentifier(operation1));
        assertEquals("ED50",          "4230", getIdentifier(sourceCRS));
        assertEquals(        "WGS84", "4326", getIdentifier(targetCRS));
        assertFalse("transform.isIdentity()", operation1.getMathTransform().isIdentity());
        assertEquals(2.5, AbstractCoordinateOperation.castOrCopy(operation1).getLinearAccuracy(), STRICT);
        /*
         * ED50 (4230)  to  WGS 84 (4326)  using
         * Position Vector 7-param. transformation (9606).
         * Accuracy = 1.5
         */
        final CoordinateOperation operation2 = factory.createCoordinateOperation("1631");
        assertEquals("ED50 to WGS84", "1631", getIdentifier(operation2));
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
        assertEquals("ED50 to WGS84", "1989", getIdentifier(operation3));
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
            if (count == 0) {
                assertEquals("Preferred transformation", "1612", getIdentifier(tr));         // see comment above.
            }
            count++;
        }
        assertEquals(count, all.size());        // Size may have been modified after above loop.
    }

    /**
     * Tests {@link EPSGDataAccess#createFromCoordinateReferenceSystemCodes(String, String)}.
     *
     * @throws FactoryException if an error occurred while querying the factory.
     */
    @Test
    @DependsOnMethod("testProjected_32210")
    public void testCreateFromCoordinateReferenceSystemCodes() throws FactoryException {
        assumeNotNull(factory);
        final ProjectedCRS crs = factory.createProjectedCRS("32210");
        assertEquals("baseCRS", "4322", getIdentifier(crs.getBaseCRS()));
        final Set<CoordinateOperation> all = factory.createFromCoordinateReferenceSystemCodes("4322", "32210");
        assertEquals("Number of operation from 4322 to 32210:", 1, all.size());
        assertTrue("Operation from 4322 to 32210 should be the map projection.",
                all.contains(crs.getConversionFromBase()));
    }
}
