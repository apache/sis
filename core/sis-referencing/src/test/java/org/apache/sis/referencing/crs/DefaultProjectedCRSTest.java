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
import org.opengis.test.Validators;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.cs.HardCodedCS;
import org.apache.sis.referencing.GeodeticObjectBuilder;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.test.LoggingWatcher;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.XMLTestCase;
import org.junit.Test;
import org.junit.Rule;

import static org.apache.sis.test.MetadataAssert.*;


/**
 * Tests the {@link DefaultProjectedCRS} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@DependsOn({
    DefaultGeographicCRSTest.class,
    org.apache.sis.referencing.operation.DefaultConversionTest.class
})
public final strictfp class DefaultProjectedCRSTest extends XMLTestCase {
    /**
     * A JUnit rule for listening to log events emitted during execution of {@link #testWKT1_WithExplicitAxisLength()}.
     * This rule verifies that the message logged contains the expected information. The expected message is something
     * like "Parameter semi_minor could have been omitted but got a value that does not match the WGS84 ellipsoid".
     *
     * <p>This field is public because JUnit requires us to do so, but should be considered as an implementation details
     * (it should have been a private field).</p>
     */
    @Rule
    public final LoggingWatcher listener = new LoggingWatcher(Logging.getLogger(Loggers.COORDINATE_OPERATION)) {
        @Override protected void verifyMessage(final String message) {
            assertTrue(message, message.contains("semi_minor"));
            assertTrue(message, message.contains("WGS84"));
        }
    };

    /**
     * An XML file in this package containing a projected CRS definition.
     */
    private static final String XML_FILE = "NTF.xml";

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
                "ProjectedCRS[“NTF (Paris) / Lambert zone II”,\n" +
                "  BaseGeodCRS[“NTF (Paris)”,\n" +
                "    Datum[“Nouvelle Triangulation Francaise”,\n" +
                "      Ellipsoid[“NTF”, 6378249.2, 293.4660212936269, LengthUnit[“metre”, 1]]],\n" +
                "      PrimeMeridian[“Paris”, 2.5969213, AngleUnit[“grade”, 0.015707963267948967]]],\n" +
                "  Conversion[“Lambert zone II”,\n" +
                "    Method[“Lambert Conic Conformal (1SP)”, Id[“EPSG”, 9801]],\n" +
                "    Parameter[“Latitude of natural origin”, 52.0, AngleUnit[“grade”, 0.015707963267948967], Id[“EPSG”, 8801]],\n" +
                "    Parameter[“Longitude of natural origin”, 0.0, AngleUnit[“degree”, 0.017453292519943295], Id[“EPSG”, 8802]],\n" +
                "    Parameter[“Scale factor at natural origin”, 0.99987742, ScaleUnit[“unity”, 1], Id[“EPSG”, 8805]],\n" +
                "    Parameter[“False easting”, 600000.0, LengthUnit[“metre”, 1], Id[“EPSG”, 8806]],\n" +
                "    Parameter[“False northing”, 2200000.0, LengthUnit[“metre”, 1], Id[“EPSG”, 8807]]],\n" +
                "  CS[Cartesian, 2],\n" +
                "    Axis[“Easting (E)”, east, Order[1]],\n" +
                "    Axis[“Northing (N)”, north, Order[2]],\n" +
                "    LengthUnit[“metre”, 1],\n" +
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
        listener.maximumLogCount = 1;
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

        assertEquals("A warning should have been logged.", 0, listener.maximumLogCount);
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
    @org.junit.Ignore("Still missing some JAXB annotations.")
    public void testXML() throws FactoryException, JAXBException {
        final DefaultProjectedCRS crs = unmarshalFile(DefaultProjectedCRS.class, XML_FILE);
        Validators.validate(crs);
        assertEquals("scope", "Large and medium scale topographic mapping and engineering survey.", crs.getScope().toString());
        /*
         * Marshal and compare with the original file.
         */
        assertMarshalEqualsFile(XML_FILE, crs, "xmlns:*", "xsi:schemaLocation");
    }
}
