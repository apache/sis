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
package org.apache.sis.referencing.crs;

import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import javax.measure.unit.Unit;
import javax.xml.bind.JAXBException;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.operation.Projection;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.cs.HardCodedCS;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.apache.sis.internal.referencing.GeodeticObjectBuilder;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.LenientComparable;

// Test dependencies
import org.opengis.test.Validators;
import org.apache.sis.test.LoggingWatcher;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.XMLTestCase;
import org.junit.After;
import org.junit.Test;
import org.junit.Rule;

import static org.apache.sis.test.ReferencingAssert.*;


/**
 * Tests the {@link DefaultProjectedCRS} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 */
@DependsOn({
    DefaultGeographicCRSTest.class,
    org.apache.sis.referencing.operation.DefaultConversionTest.class
})
public final strictfp class DefaultProjectedCRSTest extends XMLTestCase {
    /**
     * A JUnit rule for listening to log events emitted during execution of {@link #testWKT1_WithExplicitAxisLength()}.
     * This rule is used by the test methods for verifying that the logged messages contain the expected information.
     * The expected message is something like "Parameter semi_minor could have been omitted but got a value that does
     * not match the WGS84 ellipsoid".
     *
     * <p>This field is public because JUnit requires us to do so, but should be considered as an implementation details
     * (it should have been a private field).</p>
     */
    @Rule
    public final LoggingWatcher loggings = new LoggingWatcher(Loggers.COORDINATE_OPERATION);

    /**
     * Verifies that no unexpected warning has been emitted in any test defined in this class.
     */
    @After
    public void assertNoUnexpectedLog() {
        loggings.assertNoUnexpectedLog();
    }

    /**
     * An XML file in this package containing a projected CRS definition.
     */
    private static final String XML_FILE = "ProjectedCRS.xml";

    /**
     * Creates a projected CRS and verifies its parameters.
     * Verifies also that the constructor does not accept invalid base CRS.
     *
     * @throws FactoryException if the CRS creation failed.
     */
    @Test
    public void testConstructor() throws FactoryException {
        final ProjectedCRS crs = create(HardCodedCRS.NTF);
        verifyParameters(crs.getConversionFromBase().getParameterValues());
        try {
            create(HardCodedCRS.WGS84_3D);
            fail("Should not accept a three-dimensional base geodetic CRS.");
        } catch (InvalidGeodeticParameterException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("Lambert Conic Conformal (1SP)"));
        }
    }

    /**
     * Creates the "NTF (Paris) / Lambert zone II" CRS. The prime meridian is always in grades,
     * but the axes can be in degrees or in grades depending if the {@code baseCRS} argument is
     * {@link HardCodedCRS.NTF_NORMALIZED_AXES} or {@link HardCodedCRS.NTF} respectively.
     *
     * @see HardCodedCRS#NTF
     */
    private static ProjectedCRS create(final GeographicCRS baseCRS) throws FactoryException {
        return new GeodeticObjectBuilder()
                .setConversionMethod("Lambert Conic Conformal (1SP)")
                .setConversionName("Lambert zone II")
                .setParameter("Latitude of natural origin",             52, NonSI.GRADE)
                .setParameter("Scale factor at natural origin", 0.99987742, Unit.ONE)
                .setParameter("False easting",                      600000, SI.METRE)
                .setParameter("False northing",                    2200000, SI.METRE)
                .setCodeSpace(Citations.EPSG, Constants.EPSG)
                .addName("NTF (Paris) / Lambert zone II")
                .addIdentifier("27572")
                .createProjectedCRS(baseCRS, HardCodedCS.PROJECTED);
    }

    /**
     * Verifies the parameters of a {@code ProjectedCRS} created by the {@link #create(GeographicCRS)} method
     * or something equivalent.
     */
    private static void verifyParameters(final ParameterValueGroup pg) {
        assertEquals("Latitude of natural origin",    52,          pg.parameter("Latitude of natural origin")    .doubleValue(NonSI.GRADE), STRICT);
        assertEquals("Longitude of natural origin",    0,          pg.parameter("Longitude of natural origin")   .doubleValue(NonSI.GRADE), STRICT);
        assertEquals("Scale factor at natural origin", 0.99987742, pg.parameter("Scale factor at natural origin").doubleValue(),            STRICT);
        assertEquals("False easting",             600000,          pg.parameter("False easting")                 .doubleValue(SI.METRE),    STRICT);
        assertEquals("False northing",           2200000,          pg.parameter("False northing")                .doubleValue(SI.METRE),    STRICT);
    }

    /**
     * Tests WKT 1 formatting.
     *
     * @throws FactoryException if the CRS creation failed.
     */
    @Test
    @DependsOnMethod("testConstructor")
    public void testWKT1() throws FactoryException {
        final ProjectedCRS crs = create(HardCodedCRS.NTF);
        assertWktEquals(Convention.WKT1,
                "PROJCS[“NTF (Paris) / Lambert zone II”,\n" +
                "  GEOGCS[“NTF (Paris)”,\n" +
                "    DATUM[“Nouvelle Triangulation Francaise”,\n" +
                "      SPHEROID[“NTF”, 6378249.2, 293.4660212936269]],\n" +
                "      PRIMEM[“Paris”, 2.5969213],\n" +
                "    UNIT[“grade”, 0.015707963267948967],\n" +
                "    AXIS[“Longitude”, EAST],\n" +
                "    AXIS[“Latitude”, NORTH]],\n" +
                "  PROJECTION[“Lambert_Conformal_Conic_1SP”, AUTHORITY[“EPSG”, “9801”]],\n" +
                "  PARAMETER[“latitude_of_origin”, 52.0],\n" +          // In grads
                "  PARAMETER[“central_meridian”, 0.0],\n" +
                "  PARAMETER[“scale_factor”, 0.99987742],\n" +
                "  PARAMETER[“false_easting”, 600000.0],\n" +
                "  PARAMETER[“false_northing”, 2200000.0],\n" +
                "  UNIT[“metre”, 1],\n" +
                "  AXIS[“Easting”, EAST],\n" +
                "  AXIS[“Northing”, NORTH],\n" +
                "  AUTHORITY[“EPSG”, “27572”]]",
                crs);
    }

    /**
     * Tests WKT 1 formatting using {@link Convention#WKT1_COMMON_UNITS}.
     *
     * @throws FactoryException if the CRS creation failed.
     */
    @Test
    @DependsOnMethod("testWKT1")
    public void testWKT1_WithCommonUnits() throws FactoryException {
        final ProjectedCRS crs = create(HardCodedCRS.NTF);
        assertWktEquals(Convention.WKT1_COMMON_UNITS,
                "PROJCS[“NTF (Paris) / Lambert zone II”,\n" +
                "  GEOGCS[“NTF (Paris)”,\n" +
                "    DATUM[“Nouvelle Triangulation Francaise”,\n" +
                "      SPHEROID[“NTF”, 6378249.2, 293.4660212936269]],\n" +
                "      PRIMEM[“Paris”, 2.33722917],\n" +                    // Note the conversion from 2.5969213 grads.
                "    UNIT[“grade”, 0.015707963267948967],\n" +
                "    AXIS[“Longitude”, EAST],\n" +
                "    AXIS[“Latitude”, NORTH]],\n" +
                "  PROJECTION[“Lambert_Conformal_Conic_1SP”, AUTHORITY[“EPSG”, “9801”]],\n" +
                "  PARAMETER[“latitude_of_origin”, 46.8],\n" +              // Note the conversion from 52 grads.
                "  PARAMETER[“central_meridian”, 0.0],\n" +
                "  PARAMETER[“scale_factor”, 0.99987742],\n" +
                "  PARAMETER[“false_easting”, 600000.0],\n" +
                "  PARAMETER[“false_northing”, 2200000.0],\n" +
                "  UNIT[“meter”, 1],\n" +
                "  AXIS[“Easting”, EAST],\n" +
                "  AXIS[“Northing”, NORTH],\n" +
                "  AUTHORITY[“EPSG”, “27572”]]",
                crs);
    }

    /**
     * Tests WKT 1 formatting with a somewhat convolved case where the units of the prime meridian is not
     * the same than the unit of axes. Since the axis units is what we write in the {@code UNIT[…]} element,
     * the WKT formatter need to convert the unit of prime meridian and all parameter angular values.
     *
     * @throws FactoryException if the CRS creation failed.
     */
    @Test
    @DependsOnMethod("testWKT1_WithCommonUnits")
    public void testWKT1_WithMixedUnits() throws FactoryException {
        final ProjectedCRS crs = create(HardCodedCRS.NTF_NORMALIZED_AXES);
        Validators.validate(crs);   // Opportunist check.
        assertWktEquals(Convention.WKT1,
                "PROJCS[“NTF (Paris) / Lambert zone II”,\n" +
                "  GEOGCS[“NTF (Paris)”,\n" +
                "    DATUM[“Nouvelle Triangulation Francaise”,\n" +
                "      SPHEROID[“NTF”, 6378249.2, 293.4660212936269]],\n" +
                "      PRIMEM[“Paris”, 2.33722917],\n" +                    // Note the conversion from 2.5969213 grads.
                "    UNIT[“degree”, 0.017453292519943295],\n" +
                "    AXIS[“Longitude”, EAST],\n" +
                "    AXIS[“Latitude”, NORTH]],\n" +
                "  PROJECTION[“Lambert_Conformal_Conic_1SP”, AUTHORITY[“EPSG”, “9801”]],\n" +
                "  PARAMETER[“latitude_of_origin”, 46.8],\n" +              // Note the conversion from 52 grads.
                "  PARAMETER[“central_meridian”, 0.0],\n" +
                "  PARAMETER[“scale_factor”, 0.99987742],\n" +
                "  PARAMETER[“false_easting”, 600000.0],\n" +
                "  PARAMETER[“false_northing”, 2200000.0],\n" +
                "  UNIT[“metre”, 1],\n" +
                "  AXIS[“Easting”, EAST],\n" +
                "  AXIS[“Northing”, NORTH],\n" +
                "  AUTHORITY[“EPSG”, “27572”]]",
                crs);
    }

    /**
     * Tests WKT formatting in "internal" mode.
     * This mode is similar to WKT 2 but shall include the axes of the base CRS.
     *
     * @throws FactoryException if the CRS creation failed.
     */
    @Test
    @DependsOnMethod("testWKT1")
    public void testInternal() throws FactoryException {
        ProjectedCRS crs = create(HardCodedCRS.NTF);
        assertWktEquals(Convention.INTERNAL,
                "ProjectedCRS[“NTF (Paris) / Lambert zone II”,\n" +
                "  BaseGeodCRS[“NTF (Paris)”,\n" +
                "    Datum[“Nouvelle Triangulation Française”,\n" +
                "      Ellipsoid[“NTF”, 6378249.2, 293.4660212936269],\n" +
                "      Scope[“Topographic mapping.”],\n" +
                "      Id[“EPSG”, 6807]],\n" +
                "      PrimeMeridian[“Paris”, 2.5969213, Id[“EPSG”, 8903]],\n" +
                "    CS[ellipsoidal, 2],\n" +
                "      Axis[“Longitude (λ)”, east],\n" +
                "      Axis[“Latitude (φ)”, north],\n" +
                "      Unit[“grade”, 0.015707963267948967, Id[“EPSG”, 9105]]],\n" +
                "  Conversion[“Lambert zone II”,\n" +
                "    Method[“Lambert Conic Conformal (1SP)”, Id[“EPSG”, 9801], Id[“GeoTIFF”, 9]],\n" +
                "    Parameter[“Latitude of natural origin”, 52.0, Id[“EPSG”, 8801]],\n" +
                "    Parameter[“Longitude of natural origin”, 0.0, Id[“EPSG”, 8802]],\n" +
                "    Parameter[“Scale factor at natural origin”, 0.99987742, Id[“EPSG”, 8805]],\n" +
                "    Parameter[“False easting”, 600000.0, Id[“EPSG”, 8806]],\n" +
                "    Parameter[“False northing”, 2200000.0, Id[“EPSG”, 8807]]],\n" +
                "  CS[Cartesian, 2],\n" +
                "    Axis[“Easting (E)”, east],\n" +
                "    Axis[“Northing (N)”, north],\n" +
                "    Unit[“metre”, 1, Id[“EPSG”, 9001]],\n" +
                "  Id[“EPSG”, 27572]]",
                crs);
    }

    /**
     * Tests WKT 2 formatting in simplified mode.
     *
     * @throws FactoryException if the CRS creation failed.
     */
    @Test
    @DependsOnMethod("testWKT1")
    public void testWKT2_Simplified() throws FactoryException {
        ProjectedCRS crs = create(HardCodedCRS.NTF);
        assertWktEquals(Convention.WKT2_SIMPLIFIED,
                "ProjectedCRS[“NTF (Paris) / Lambert zone II”,\n" +
                "  BaseGeodCRS[“NTF (Paris)”,\n" +
                "    Datum[“Nouvelle Triangulation Francaise”,\n" +
                "      Ellipsoid[“NTF”, 6378249.2, 293.4660212936269]],\n" +
                "      PrimeMeridian[“Paris”, 2.5969213],\n" +
                "    Unit[“grade”, 0.015707963267948967]],\n" +
                "  Conversion[“Lambert zone II”,\n" +
                "    Method[“Lambert Conic Conformal (1SP)”],\n" +
                "    Parameter[“Latitude of natural origin”, 52.0],\n" +
                "    Parameter[“Longitude of natural origin”, 0.0],\n" +
                "    Parameter[“Scale factor at natural origin”, 0.99987742],\n" +
                "    Parameter[“False easting”, 600000.0],\n" +
                "    Parameter[“False northing”, 2200000.0]],\n" +
                "  CS[Cartesian, 2],\n" +
                "    Axis[“Easting (E)”, east],\n" +
                "    Axis[“Northing (N)”, north],\n" +
                "    Unit[“metre”, 1],\n" +
                "  Id[“EPSG”, 27572, URI[“urn:ogc:def:crs:EPSG::27572”]]]",
                crs);
        /*
         * Try again, but with mixed units. It should force the formatter to add explicit
         * unit declaration in PrimeMeridian[…] and some Parameter[…] elements.
         */
        crs = create(HardCodedCRS.NTF_NORMALIZED_AXES);
        assertWktEquals(Convention.WKT2_SIMPLIFIED,
                "ProjectedCRS[“NTF (Paris) / Lambert zone II”,\n" +
                "  BaseGeodCRS[“NTF (Paris)”,\n" +
                "    Datum[“Nouvelle Triangulation Francaise”,\n" +
                "      Ellipsoid[“NTF”, 6378249.2, 293.4660212936269]],\n" +
                "      PrimeMeridian[“Paris”, 2.5969213, Unit[“grade”, 0.015707963267948967]],\n" +
                "    Unit[“degree”, 0.017453292519943295]],\n" +
                "  Conversion[“Lambert zone II”,\n" +
                "    Method[“Lambert Conic Conformal (1SP)”],\n" +
                "    Parameter[“Latitude of natural origin”, 52.0, Unit[“grade”, 0.015707963267948967]],\n" +
                "    Parameter[“Longitude of natural origin”, 0.0],\n" +
                "    Parameter[“Scale factor at natural origin”, 0.99987742],\n" +
                "    Parameter[“False easting”, 600000.0],\n" +
                "    Parameter[“False northing”, 2200000.0]],\n" +
                "  CS[Cartesian, 2],\n" +
                "    Axis[“Easting (E)”, east],\n" +
                "    Axis[“Northing (N)”, north],\n" +
                "    Unit[“metre”, 1],\n" +
                "  Id[“EPSG”, 27572, URI[“urn:ogc:def:crs:EPSG::27572”]]]",
                crs);
    }

    /**
     * Tests WKT 2 formatting. Contrarily to the WKT 1 formatting, in this case it does not matter
     * if we mix the units of measurement because the unit is declared for each parameter and axis.
     *
     * @throws FactoryException if the CRS creation failed.
     */
    @Test
    @DependsOnMethod("testWKT1")
    public void testWKT2_WithMixedUnits() throws FactoryException {
        final ProjectedCRS crs = create(HardCodedCRS.NTF_NORMALIZED_AXES);
        assertWktEquals(Convention.WKT2,
                "PROJCRS[“NTF (Paris) / Lambert zone II”,\n" +
                "  BASEGEODCRS[“NTF (Paris)”,\n" +
                "    DATUM[“Nouvelle Triangulation Francaise”,\n" +
                "      ELLIPSOID[“NTF”, 6378249.2, 293.4660212936269, LENGTHUNIT[“metre”, 1]]],\n" +
                "      PRIMEM[“Paris”, 2.5969213, ANGLEUNIT[“grade”, 0.015707963267948967]]],\n" +
                "  CONVERSION[“Lambert zone II”,\n" +
                "    METHOD[“Lambert Conic Conformal (1SP)”, ID[“EPSG”, 9801]],\n" +
                "    PARAMETER[“Latitude of natural origin”, 52.0, ANGLEUNIT[“grade”, 0.015707963267948967], ID[“EPSG”, 8801]],\n" +
                "    PARAMETER[“Longitude of natural origin”, 0.0, ANGLEUNIT[“degree”, 0.017453292519943295], ID[“EPSG”, 8802]],\n" +
                "    PARAMETER[“Scale factor at natural origin”, 0.99987742, SCALEUNIT[“unity”, 1], ID[“EPSG”, 8805]],\n" +
                "    PARAMETER[“False easting”, 600000.0, LENGTHUNIT[“metre”, 1], ID[“EPSG”, 8806]],\n" +
                "    PARAMETER[“False northing”, 2200000.0, LENGTHUNIT[“metre”, 1], ID[“EPSG”, 8807]]],\n" +
                "  CS[Cartesian, 2],\n" +
                "    AXIS[“Easting (E)”, east, ORDER[1]],\n" +
                "    AXIS[“Northing (N)”, north, ORDER[2]],\n" +
                "    LENGTHUNIT[“metre”, 1],\n" +
                "  ID[“EPSG”, 27572, URI[“urn:ogc:def:crs:EPSG::27572”]]]",
                crs);

        assertWktEquals(Convention.WKT2_SIMPLIFIED,
                "ProjectedCRS[“NTF (Paris) / Lambert zone II”,\n" +
                "  BaseGeodCRS[“NTF (Paris)”,\n" +
                "    Datum[“Nouvelle Triangulation Francaise”,\n" +
                "      Ellipsoid[“NTF”, 6378249.2, 293.4660212936269]],\n" +
                "      PrimeMeridian[“Paris”, 2.5969213, Unit[“grade”, 0.015707963267948967]],\n" +
                "    Unit[“degree”, 0.017453292519943295]],\n" +
                "  Conversion[“Lambert zone II”,\n" +
                "    Method[“Lambert Conic Conformal (1SP)”],\n" +
                "    Parameter[“Latitude of natural origin”, 52.0, Unit[“grade”, 0.015707963267948967]],\n" +
                "    Parameter[“Longitude of natural origin”, 0.0],\n" +
                "    Parameter[“Scale factor at natural origin”, 0.99987742],\n" +
                "    Parameter[“False easting”, 600000.0],\n" +
                "    Parameter[“False northing”, 2200000.0]],\n" +
                "  CS[Cartesian, 2],\n" +
                "    Axis[“Easting (E)”, east],\n" +
                "    Axis[“Northing (N)”, north],\n" +
                "    Unit[“metre”, 1],\n" +
                "  Id[“EPSG”, 27572, URI[“urn:ogc:def:crs:EPSG::27572”]]]",
                crs);
    }

    /**
     * Tests WKT 1 formatting of a pseudo-projection with explicit {@code "semi-major"} and {@code "semi-minor"}
     * parameter values. This was a way to define the Google pseudo-projection using standard projection method
     * name before EPSG introduced the <cite>"Popular Visualisation Pseudo Mercator"</cite> projection method.
     * The approach tested in this method is now deprecated at least for the Google projection (while it may
     * still be useful for other projections), but we still test it for compatibility reasons.
     *
     * @throws FactoryException if the CRS creation failed.
     */
    @Test
    @DependsOnMethod("testWKT1")
    public void testWKT1_WithExplicitAxisLength() throws FactoryException {
        final ProjectedCRS crs = new GeodeticObjectBuilder()
                .setConversionMethod("Mercator (variant A)")
                .setConversionName("Popular Visualisation Pseudo-Mercator")
                .setParameter("semi-major", 6378137, SI.METRE)
                .setParameter("semi-minor", 6378137, SI.METRE)
                .addName("WGS 84 / Pseudo-Mercator")
                .createProjectedCRS(HardCodedCRS.WGS84, HardCodedCS.PROJECTED);

        assertWktEquals(Convention.WKT1,
                "PROJCS[“WGS 84 / Pseudo-Mercator”,\n" +
                "  GEOGCS[“WGS 84”,\n" +
                "    DATUM[“World Geodetic System 1984”,\n" +
                "      SPHEROID[“WGS84”, 6378137.0, 298.257223563]],\n" +
                "      PRIMEM[“Greenwich”, 0.0],\n" +
                "    UNIT[“degree”, 0.017453292519943295],\n" +
                "    AXIS[“Longitude”, EAST],\n" +
                "    AXIS[“Latitude”, NORTH]],\n" +
                "  PROJECTION[“Mercator_1SP”, AUTHORITY[“EPSG”, “9804”]],\n" +
                "  PARAMETER[“semi_minor”, 6378137.0],\n" +     // Non-standard: appears because its value is different than the ellipsoid value.
                "  PARAMETER[“latitude_of_origin”, 0.0],\n" +
                "  PARAMETER[“central_meridian”, 0.0],\n" +
                "  PARAMETER[“scale_factor”, 1.0],\n" +
                "  PARAMETER[“false_easting”, 0.0],\n" +
                "  PARAMETER[“false_northing”, 0.0],\n" +
                "  UNIT[“metre”, 1],\n" +
                "  AXIS[“Easting”, EAST],\n" +
                "  AXIS[“Northing”, NORTH]]",
                crs);

        loggings.assertNextLogContains("semi_minor", "WGS84");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests formatting of “Equidistant Cylindrical (Spherical)” projected CRS. This one is a special case
     * because it is simplified to an affine transform. The referencing module should be able to find the
     * original projection parameters.
     *
     * @throws FactoryException if the CRS creation failed.
     */
    @Test
    @DependsOnMethod("testWKT2_Simplified")
    public void testWKT2_ForEquirectangular() throws FactoryException {
        final ProjectedCRS crs = new GeodeticObjectBuilder()
                .setConversionMethod("Equirectangular")
                .setConversionName("Equidistant Cylindrical (Spherical)")
                .setParameter("False easting",  1000, SI.METRE)
                .setParameter("False northing", 2000, SI.METRE)
                .addName("Equidistant Cylindrical (Spherical)")
                .createProjectedCRS(HardCodedCRS.WGS84, HardCodedCS.PROJECTED);

        assertWktEquals(Convention.WKT2_SIMPLIFIED,
                "ProjectedCRS[“Equidistant Cylindrical (Spherical)”,\n" +
                "  BaseGeodCRS[“WGS 84”,\n" +
                "    Datum[“World Geodetic System 1984”,\n" +
                "      Ellipsoid[“WGS84”, 6378137.0, 298.257223563]],\n" +
                "    Unit[“degree”, 0.017453292519943295]],\n" +
                "  Conversion[“Equidistant Cylindrical (Spherical)”,\n" +
                "    Method[“Equidistant Cylindrical (Spherical)”],\n" +
                "    Parameter[“Latitude of 1st standard parallel”, 0.0],\n" +
                "    Parameter[“Longitude of natural origin”, 0.0],\n" +
                "    Parameter[“False easting”, 1000.0],\n" +
                "    Parameter[“False northing”, 2000.0]],\n" +
                "  CS[Cartesian, 2],\n" +
                "    Axis[“Easting (E)”, east],\n" +
                "    Axis[“Northing (N)”, north],\n" +
                "    Unit[“metre”, 1]]",
                crs);
    }

    /**
     * Tests (un)marshalling of a projected coordinate reference system.
     *
     * @throws FactoryException if the CRS creation failed.
     * @throws JAXBException If an error occurred during (un)marshalling.
     */
    @Test
    public void testXML() throws FactoryException, JAXBException {
        final DefaultProjectedCRS crs = unmarshalFile(DefaultProjectedCRS.class, XML_FILE);
        Validators.validate(crs);
        assertEpsgNameAndIdentifierEqual("NTF (Paris) / Lambert zone II", 27572, crs);
        assertEpsgNameAndIdentifierEqual("NTF (Paris)", 4807, crs.getBaseCRS());
        assertEquals("scope", "Large and medium scale topographic mapping and engineering survey.", crs.getScope().toString());
        assertAxisDirectionsEqual("baseCRS", crs.getBaseCRS().getCoordinateSystem(), AxisDirection.NORTH, AxisDirection.EAST);
        assertAxisDirectionsEqual("coordinateSystem", crs.getCoordinateSystem(), AxisDirection.EAST, AxisDirection.NORTH);

        final Projection conversion = crs.getConversionFromBase();
        assertEpsgNameAndIdentifierEqual("Lambert zone II", 18082, conversion);
        assertEpsgNameAndIdentifierEqual("Lambert Conic Conformal (1SP)", 9801, conversion.getMethod());
        assertNotNull("conversion.mathTransform", conversion.getMathTransform());
        verifyParameters(conversion.getParameterValues());
        /*
         * Test marshalling and compare with the original file. The comparison ignores the <gml:name> nodes because the
         * marshalled CRS contains many operation method and parameter aliases which were not in the original XML file.
         */
        assertMarshalEqualsFile(XML_FILE, crs, STRICT, new String[] {"gml:name"},
                new String[] {"xmlns:*", "xsi:schemaLocation", "gml:id"});
    }

    /**
     * Tests {@link DefaultProjectedCRS#equals(Object, ComparisonMode)}.
     * In particular, we want to test the ability to ignore axis order of the base CRS in "ignore metadata" mode.
     *
     * @throws FactoryException if the CRS creation failed.
     *
     * @since 0.7
     */
    @Test
    public void testEquals() throws FactoryException {
        final ProjectedCRS standard   = create(CommonCRS.WGS84.geographic());
        final ProjectedCRS normalized = create(CommonCRS.WGS84.normalizedGeographic());
        assertFalse("STRICT",          ((LenientComparable) standard).equals(normalized, ComparisonMode.STRICT));
        assertFalse("BY_CONTRACT",     ((LenientComparable) standard).equals(normalized, ComparisonMode.BY_CONTRACT));
        assertTrue ("IGNORE_METADATA", ((LenientComparable) standard).equals(normalized, ComparisonMode.IGNORE_METADATA));
        assertTrue ("APPROXIMATIVE",   ((LenientComparable) standard).equals(normalized, ComparisonMode.APPROXIMATIVE));
        assertTrue ("ALLOW_VARIANT",   ((LenientComparable) standard).equals(normalized, ComparisonMode.ALLOW_VARIANT));
    }
}
