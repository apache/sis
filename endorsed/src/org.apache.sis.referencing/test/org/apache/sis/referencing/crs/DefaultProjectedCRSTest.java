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

import java.io.InputStream;
import jakarta.xml.bind.JAXBException;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.operation.Conversion;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.apache.sis.referencing.privy.GeodeticObjectBuilder;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.system.Loggers;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.measure.Units;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.opengis.test.Validators;
import org.apache.sis.xml.test.TestCase;
import org.apache.sis.referencing.cs.HardCodedCS;
import static org.apache.sis.test.TestUtilities.getScope;
import static org.apache.sis.test.Assertions.assertMessageContains;
import static org.apache.sis.referencing.Assertions.assertWktEquals;
import static org.apache.sis.referencing.Assertions.assertEpsgNameAndIdentifierEqual;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import static org.opengis.test.Assertions.assertAxisDirectionsEqual;


/**
 * Tests the {@link DefaultProjectedCRS} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class DefaultProjectedCRSTest extends TestCase.WithLogs {
    /**
     * Creates a new test case.
     */
    public DefaultProjectedCRSTest() {
        super(Loggers.COORDINATE_OPERATION);
    }

    /**
     * Opens the stream to the XML file in this package containing a projected CRS definition.
     *
     * @return stream opened on the XML document to use for testing purpose.
     */
    private static InputStream openTestFile() {
        // Call to `getResourceAsStream(…)` is caller sensitive: it must be in the same module.
        return DefaultProjectedCRSTest.class.getResourceAsStream("ProjectedCRS.xml");
    }

    /**
     * Creates a two-dimensional projected CRS and verifies its parameters.
     *
     * @throws FactoryException if the CRS creation failed.
     */
    @Test
    public void testConstructor2D() throws FactoryException {
        final ProjectedCRS crs = create(HardCodedCRS.NTF);
        verifyParameters(crs.getConversionFromBase().getParameterValues());
        assertEquals(2, crs.getCoordinateSystem().getDimension(), "dimension");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Creates a three-dimensional projected CRS and verifies its parameters.
     *
     * @throws FactoryException if the CRS creation failed.
     */
    @Test
    public void testConstructor3D() throws FactoryException {
        final ProjectedCRS crs = create(HardCodedCRS.WGS84_3D, HardCodedCS.PROJECTED_3D);
        verifyParameters(crs.getConversionFromBase().getParameterValues());
        assertEquals(3, crs.getCoordinateSystem().getDimension(), "dimension");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Verifies that the constructor does not accept inconsistent number of dimensions.
     *
     * @throws FactoryException if the CRS creation failed.
     */
    @Test
    public void testInvalidDimensions() throws FactoryException {
        var e = assertThrows(InvalidGeodeticParameterException.class, () -> create(HardCodedCRS.WGS84_3D),
                             "Should not accept a three-dimensional base CRS with two-dimensional CS.");
        assertMessageContains(e, "derivedCS");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Creates a two-dimensional "NTF (Paris) / Lambert zone II" CRS.
     * The prime meridian is always in grads, but the axes can be in degrees or in grads depending on whether
     * {@code baseCRS} is {@link HardCodedCRS#NTF_NORMALIZED_AXES} or {@link HardCodedCRS#NTF} respectively.
     *
     * @see HardCodedCRS#NTF
     */
    private static ProjectedCRS create(final GeographicCRS baseCRS) throws FactoryException {
        return create(baseCRS, HardCodedCS.PROJECTED);
    }

    /**
     * Creates a two- or three-dimensional "NTF (Paris) / Lambert zone II" CRS.
     * The prime meridian is always in grads, but the axes can be in degrees or in grads depending on whether
     * {@code baseCRS} is {@link HardCodedCRS#NTF_NORMALIZED_AXES} or {@link HardCodedCRS#NTF} respectively.
     */
    private static ProjectedCRS create(final GeographicCRS baseCRS, final CartesianCS derivedCS)
            throws FactoryException
    {
        return new GeodeticObjectBuilder()
                .setConversionMethod("Lambert Conic Conformal (1SP)")
                .setConversionName("Lambert zone II")
                .setParameter("Latitude of natural origin",             52, Units.GRAD)
                .setParameter("Scale factor at natural origin", 0.99987742, Units.UNITY)
                .setParameter("False easting",                      600000, Units.METRE)
                .setParameter("False northing",                    2200000, Units.METRE)
                .setCodeSpace(Citations.EPSG, Constants.EPSG)
                .addName("NTF (Paris) / Lambert zone II")
                .addIdentifier("27572")
                .createProjectedCRS(baseCRS, derivedCS);
    }

    /**
     * Verifies the parameters of a {@code ProjectedCRS} created by the {@link #create(GeographicCRS)} method
     * or something equivalent.
     */
    private static void verifyParameters(final ParameterValueGroup pg) {
        assertEquals(52,          pg.parameter("Latitude of natural origin")    .doubleValue(Units.GRAD));
        assertEquals( 0,          pg.parameter("Longitude of natural origin")   .doubleValue(Units.GRAD));
        assertEquals( 0.99987742, pg.parameter("Scale factor at natural origin").doubleValue());
        assertEquals( 600000,     pg.parameter("False easting")                 .doubleValue(Units.METRE));
        assertEquals(2200000,     pg.parameter("False northing")                .doubleValue(Units.METRE));
    }

    /**
     * Tests WKT 1 formatting.
     *
     * @throws FactoryException if the CRS creation failed.
     */
    @Test
    public void testWKT1() throws FactoryException {
        final ProjectedCRS crs = create(HardCodedCRS.NTF);
        assertWktEquals(Convention.WKT1,
                "PROJCS[“NTF (Paris) / Lambert zone II”,\n" +
                "  GEOGCS[“NTF (Paris)”,\n" +
                "    DATUM[“Nouvelle Triangulation Francaise”,\n" +
                "      SPHEROID[“NTF”, 6378249.2, 293.4660212936269]],\n" +
                "      PRIMEM[“Paris”, 2.5969213],\n" +
                "    UNIT[“grad”, 0.015707963267948967],\n" +
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

        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests WKT 1 formatting using {@link Convention#WKT1_COMMON_UNITS}.
     *
     * @throws FactoryException if the CRS creation failed.
     */
    @Test
    public void testWKT1_WithCommonUnits() throws FactoryException {
        final ProjectedCRS crs = create(HardCodedCRS.NTF);
        assertWktEquals(Convention.WKT1_COMMON_UNITS,
                "PROJCS[“NTF (Paris) / Lambert zone II”,\n" +
                "  GEOGCS[“NTF (Paris)”,\n" +
                "    DATUM[“Nouvelle Triangulation Francaise”,\n" +
                "      SPHEROID[“NTF”, 6378249.2, 293.4660212936269]],\n" +
                "      PRIMEM[“Paris”, 2.33722917],\n" +                    // Note the conversion from 2.5969213 grads.
                "    UNIT[“grad”, 0.015707963267948967],\n" +
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

        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests WKT 1 formatting with a somewhat convolved case where the units of the prime meridian is not
     * the same as the unit of axes. Since the axis units is what we write in the {@code UNIT[…]} element,
     * the WKT formatter need to convert the unit of prime meridian and all parameter angular values.
     *
     * @throws FactoryException if the CRS creation failed.
     */
    @Test
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

        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests WKT formatting in "internal" mode.
     * This mode is similar to WKT 2 but shall include the axes of the base CRS and more parameter identifiers.
     *
     * @throws FactoryException if the CRS creation failed.
     */
    @Test
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
                "      Unit[“grad”, 0.015707963267948967, Id[“EPSG”, 9105]]],\n" +
                "  Conversion[“Lambert zone II”,\n" +
                "    Method[“Lambert Conic Conformal (1SP)”, Id[“EPSG”, 9801], Id[“GeoTIFF”, 9]],\n" +
                "    Parameter[“Latitude of natural origin”, 52.0, Id[“EPSG”, 8801], Id[“GeoTIFF”, 3081]],\n" +
                "    Parameter[“Longitude of natural origin”, 0.0, Id[“EPSG”, 8802], Id[“GeoTIFF”, 3080]],\n" +
                "    Parameter[“Scale factor at natural origin”, 0.99987742, Id[“EPSG”, 8805], Id[“GeoTIFF”, 3092]],\n" +
                "    Parameter[“False easting”, 600000.0, Id[“EPSG”, 8806], Id[“GeoTIFF”, 3082]],\n" +
                "    Parameter[“False northing”, 2200000.0, Id[“EPSG”, 8807], Id[“GeoTIFF”, 3083]]],\n" +
                "  CS[Cartesian, 2],\n" +
                "    Axis[“Easting (E)”, east],\n" +
                "    Axis[“Northing (N)”, north],\n" +
                "    Unit[“metre”, 1, Id[“EPSG”, 9001]],\n" +
                "  Id[“EPSG”, 27572]]",
                crs);

        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests WKT 2 formatting in simplified mode.
     *
     * @throws FactoryException if the CRS creation failed.
     */
    @Test
    public void testWKT2_Simplified() throws FactoryException {
        ProjectedCRS crs = create(HardCodedCRS.NTF);
        assertWktEquals(Convention.WKT2_SIMPLIFIED,
                "ProjectedCRS[“NTF (Paris) / Lambert zone II”,\n" +
                "  BaseGeodCRS[“NTF (Paris)”,\n" +
                "    Datum[“Nouvelle Triangulation Francaise”,\n" +
                "      Ellipsoid[“NTF”, 6378249.2, 293.4660212936269]],\n" +
                "      PrimeMeridian[“Paris”, 2.5969213],\n" +
                "    Unit[“grad”, 0.015707963267948967]],\n" +
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
                "      PrimeMeridian[“Paris”, 2.5969213, Unit[“grad”, 0.015707963267948967]],\n" +
                "    Unit[“degree”, 0.017453292519943295]],\n" +
                "  Conversion[“Lambert zone II”,\n" +
                "    Method[“Lambert Conic Conformal (1SP)”],\n" +
                "    Parameter[“Latitude of natural origin”, 52.0, Unit[“grad”, 0.015707963267948967]],\n" +
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

        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests WKT 2 formatting. Contrarily to the WKT 1 formatting, in this case it does not matter
     * if we mix the units of measurement because the unit is declared for each parameter and axis.
     *
     * @throws FactoryException if the CRS creation failed.
     */
    @Test
    public void testWKT2_WithMixedUnits() throws FactoryException {
        final ProjectedCRS crs = create(HardCodedCRS.NTF_NORMALIZED_AXES);
        assertWktEquals(Convention.WKT2,
                "PROJCRS[“NTF (Paris) / Lambert zone II”,\n" +
                "  BASEGEODCRS[“NTF (Paris)”,\n" +
                "    DATUM[“Nouvelle Triangulation Francaise”,\n" +
                "      ELLIPSOID[“NTF”, 6378249.2, 293.4660212936269, LENGTHUNIT[“metre”, 1]]],\n" +
                "      PRIMEM[“Paris”, 2.5969213, ANGLEUNIT[“grad”, 0.015707963267948967]]],\n" +
                "  CONVERSION[“Lambert zone II”,\n" +
                "    METHOD[“Lambert Conic Conformal (1SP)”, ID[“EPSG”, 9801]],\n" +
                "    PARAMETER[“Latitude of natural origin”, 52.0, ANGLEUNIT[“grad”, 0.015707963267948967], ID[“EPSG”, 8801]],\n" +
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
                "      PrimeMeridian[“Paris”, 2.5969213, Unit[“grad”, 0.015707963267948967]],\n" +
                "    Unit[“degree”, 0.017453292519943295]],\n" +
                "  Conversion[“Lambert zone II”,\n" +
                "    Method[“Lambert Conic Conformal (1SP)”],\n" +
                "    Parameter[“Latitude of natural origin”, 52.0, Unit[“grad”, 0.015707963267948967]],\n" +
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

        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests WKT 1 formatting of a pseudo-projection with explicit {@code "semi-major"} and {@code "semi-minor"}
     * parameter values. This was a way to define the Google pseudo-projection using standard projection method
     * name before EPSG introduced the <q>Popular Visualisation Pseudo Mercator</q> projection method.
     * The approach tested in this method is now deprecated at least for the Google projection (while it may
     * still be useful for other projections), but we still test it for compatibility reasons.
     *
     * @throws FactoryException if the CRS creation failed.
     */
    @Test
    public void testWKT1_WithExplicitAxisLength() throws FactoryException {
        final ProjectedCRS crs = new GeodeticObjectBuilder()
                .setConversionMethod("Mercator (variant A)")
                .setConversionName("Popular Visualisation Pseudo-Mercator")
                .setParameter("semi-major", 6378137, Units.METRE)
                .setParameter("semi-minor", 6378137, Units.METRE)
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
    public void testWKT2_ForEquirectangular() throws FactoryException {
        final ProjectedCRS crs = new GeodeticObjectBuilder()
                .setConversionMethod("Equirectangular")
                .setConversionName("Equidistant Cylindrical (Spherical)")
                .setParameter("False easting",  1000, Units.METRE)
                .setParameter("False northing", 2000, Units.METRE)
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

        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests (un)marshalling of a projected coordinate reference system.
     *
     * @throws FactoryException if the CRS creation failed.
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    public void testXML() throws FactoryException, JAXBException {
        final DefaultProjectedCRS crs = unmarshalFile(DefaultProjectedCRS.class, openTestFile());
        Validators.validate(crs);
        assertEpsgNameAndIdentifierEqual("NTF (Paris) / Lambert zone II", 27572, crs);
        assertEpsgNameAndIdentifierEqual("NTF (Paris)", 4807, crs.getBaseCRS());
        assertEquals("Large and medium scale topographic mapping and engineering survey.", getScope(crs));
        assertAxisDirectionsEqual(crs.getBaseCRS().getCoordinateSystem(), AxisDirection.NORTH, AxisDirection.EAST);
        assertAxisDirectionsEqual(crs.getCoordinateSystem(), AxisDirection.EAST, AxisDirection.NORTH);

        final Conversion conversion = crs.getConversionFromBase();
        assertEpsgNameAndIdentifierEqual("Lambert zone II", 18082, conversion);
        assertEpsgNameAndIdentifierEqual("Lambert Conic Conformal (1SP)", 9801, conversion.getMethod());
        assertNotNull(conversion.getMathTransform());
        verifyParameters(conversion.getParameterValues());
        /*
         * Test marshalling and compare with the original file. The comparison ignores the <gml:name> nodes because the
         * marshalled CRS contains many operation method and parameter aliases which were not in the original XML file.
         */
        assertMarshalEqualsFile(openTestFile(), crs, null, STRICT, new String[] {"gml:name"},
                new String[] {"xmlns:*", "xsi:schemaLocation", "gml:id"});
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@link DefaultProjectedCRS#equals(Object, ComparisonMode)}.
     * In particular, we want to test the ability to ignore axis order of the base CRS in "ignore metadata" mode.
     *
     * @throws FactoryException if the CRS creation failed.
     */
    @Test
    public void testEquals() throws FactoryException {
        final ProjectedCRS standard   = create(HardCodedCRS.WGS84_LATITUDE_FIRST);
        final ProjectedCRS normalized = create(HardCodedCRS.WGS84);
        final var c = assertInstanceOf(LenientComparable.class, standard);
        assertFalse(c.equals(normalized, ComparisonMode.STRICT));
        assertFalse(c.equals(normalized, ComparisonMode.BY_CONTRACT));
        assertTrue (c.equals(normalized, ComparisonMode.IGNORE_METADATA));
        assertTrue (c.equals(normalized, ComparisonMode.APPROXIMATE));
        assertTrue (c.equals(normalized, ComparisonMode.ALLOW_VARIANT));
        loggings.assertNoUnexpectedLog();
    }
}
