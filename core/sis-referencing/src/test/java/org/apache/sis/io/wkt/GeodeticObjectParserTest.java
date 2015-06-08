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
package org.apache.sis.io.wkt;

import java.util.Date;
import java.util.Iterator;
import java.text.ParsePosition;
import java.text.ParseException;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import javax.measure.unit.Unit;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.ReferencingAssert.*;


/**
 * Tests {@link GeodeticObjectParser}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@DependsOn({
    MathTransformParserTest.class,
    org.apache.sis.referencing.crs.DefaultGeocentricCRSTest.class,
    org.apache.sis.referencing.crs.DefaultGeographicCRSTest.class,
    org.apache.sis.referencing.crs.DefaultProjectedCRSTest.class,
    org.apache.sis.referencing.crs.DefaultVerticalCRSTest.class,
    org.apache.sis.referencing.crs.DefaultTemporalCRSTest.class,
    org.apache.sis.referencing.crs.DefaultCompoundCRSTest.class,
    org.apache.sis.internal.referencing.AxisDirectionsTest.class
})
public final strictfp class GeodeticObjectParserTest extends TestCase {
    /**
     * The parser to use for the test.
     */
    private GeodeticObjectParser parser;

    /**
     * Parses the given text.
     *
     * @throws ParseException if an error occurred during the parsing.
     */
    private <T> T parse(final Class<T> type, final String text) throws ParseException {
        if (parser == null) {
            parser = new GeodeticObjectParser();
        }
        final ParsePosition position = new ParsePosition(0);
        final Object obj = parser.parseObject(text, position);
        assertEquals("errorIndex", -1, position.getErrorIndex());
        assertEquals("index", text.length(), position.getIndex());
        assertInstanceOf("GeodeticObjectParser.parseObject", type, obj);
        return type.cast(obj);
    }

    /**
     * Asserts that the name and (optionally) the EPSG identifier of the given object
     * are equal to the given strings.
     *
     * @param name The expected name.
     * @param epsg The expected EPSG identifier, or {@code 0} if the object shall have no identifier.
     */
    static void assertNameAndIdentifierEqual(final String name, final int epsg, final IdentifiedObject object) {
        final String message = object.getClass().getSimpleName();
        assertEquals(message, name, object.getName().getCode());
        assertEpsgIdentifierEquals(epsg, object.getIdentifiers());
    }

    /**
     * Asserts that the given axis is a longitude axis. This method expects the name to be
     * <cite>"Geodetic longitude"</cite> even if the WKT string contained a different name,
     * because {@link GeodeticObjectParser} should have done the replacement.
     */
    private static void assertLongitudeAxisEquals(final CoordinateSystemAxis axis) {
        assertAxisEquals("Geodetic longitude", "λ", AxisDirection.EAST, -180, +180, NonSI.DEGREE_ANGLE, RangeMeaning.WRAPAROUND, axis);
    }

    /**
     * Asserts that the given axis is a latitude axis. This method expects the name to be
     * <cite>"Geodetic latitude"</cite> even if the WKT string contained a different name,
     * because {@link GeodeticObjectParser} should have done the replacement.
     */
    private static void assertLatitudeAxisEquals(final CoordinateSystemAxis axis) {
        assertAxisEquals("Geodetic latitude", "φ", AxisDirection.NORTH, -90, +90, NonSI.DEGREE_ANGLE, RangeMeaning.EXACT, axis);
    }

    /**
     * Asserts the given axis is an axis of the given name without minimum and maximum values.
     */
    private static void assertUnboundedAxisEquals(final String name, final String abbreviation,
            final AxisDirection direction, final Unit<?> unit, final CoordinateSystemAxis axis)
    {
        assertAxisEquals(name, abbreviation, direction, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, unit, null, axis);
    }

    /**
     * Tests the parsing of a geocentric CRS from a WKT 1 string. The parser
     * shall replace the OGC 01-009 axis directions (OTHER, EAST, NORTH) by
     * ISO 19111 axis directions (Geocentric X, Geocentric Y, Geocentric Z).
     *
     * @throws ParseException if the parsing failed.
     */
    @Test
    public void testGeocentricCRS() throws ParseException {
        final GeocentricCRS crs = parse(GeocentricCRS.class,
                "GEOCCS[“Geocentric”,\n" +
                "  DATUM[“World Geodetic System 1984”,\n" +
                "    SPHEROID[“WGS84”, 6378137.0, 298.257223563, AUTHORITY[“EPSG”, “7030”]],\n" +
                "    AUTHORITY[“EPSG”, “6326”]],\n" +
                "    PRIMEM[“Greenwich”, 0.0, AUTHORITY[“EPSG”, “8901”]],\n" +
                "  UNIT[“metre”, 1.0],\n" +
                "  AXIS[“X”, OTHER],\n" +
                "  AXIS[“Y”, EAST],\n" +
                "  AXIS[“Z”, NORTH]]");

        assertNameAndIdentifierEqual("Geocentric", 0, crs);

        final GeodeticDatum datum = crs.getDatum();
        assertNameAndIdentifierEqual("World Geodetic System 1984", 6326, datum);
        assertNameAndIdentifierEqual("Greenwich", 8901, datum.getPrimeMeridian());

        final Ellipsoid ellipsoid = datum.getEllipsoid();
        assertNameAndIdentifierEqual("WGS84", 7030, ellipsoid);
        assertEquals("semiMajor", 6378137, ellipsoid.getSemiMajorAxis(), STRICT);
        assertEquals("inverseFlattening", 298.257223563, ellipsoid.getInverseFlattening(), STRICT);

        // Verify that the OGC 01-009 axes have been relaced by ISO 19111 axes.
        final CartesianCS cs = (CartesianCS) crs.getCoordinateSystem();
        assertEquals("dimension", 3, cs.getDimension());
        assertUnboundedAxisEquals("Geocentric X", "X", AxisDirection.GEOCENTRIC_X, SI.METRE, cs.getAxis(0));
        assertUnboundedAxisEquals("Geocentric Y", "Y", AxisDirection.GEOCENTRIC_Y, SI.METRE, cs.getAxis(1));
        assertUnboundedAxisEquals("Geocentric Z", "Z", AxisDirection.GEOCENTRIC_Z, SI.METRE, cs.getAxis(2));
    }

    /**
     * Tests the parsing of a geographic CRS from a WKT 1 string using (longitude, latitude) axis order.
     *
     * @throws ParseException if the parsing failed.
     */
    @Test
    public void testGeographicCRS() throws ParseException {
        testGeographicCRS(0,
               "  GEOGCS[“WGS84”,\n" +
               "    DATUM[“World Geodetic System 1984”,\n" +
               "      SPHEROID[“WGS84”, 6378137.0, 298.257223563]],\n" +
               "      PRIMEM[“Greenwich”, 0.0],\n" +
               "    UNIT[“degree”, 0.017453292519943295],\n" +
               "    AXIS[“Longitude”, EAST],\n" +
               "    AXIS[“Latitude”, NORTH]]");
    }

    /**
     * Tests the parsing of a geographic CRS from a WKT 1 string using (latitude, longitude) axis order.
     *
     * @throws ParseException if the parsing failed.
     */
    @Test
    @DependsOnMethod("testGeographicCRS")
    public void testAxisSwapping() throws ParseException {
        testGeographicCRS(1,
               "  GEOGCS[“WGS84”,\n" +
               "    DATUM[“World Geodetic System 1984”,\n" +
               "      SPHEROID[“WGS84”, 6378137.0, 298.257223563]],\n" +
               "      PRIMEM[“Greenwich”, 0.0],\n" +
               "    UNIT[“degree”, 0.017453292519943295],\n" +
               "    AXIS[“Latitude”, NORTH],\n" +
               "    AXIS[“Longitude”, EAST]]");
    }

    /**
     * Implementation of {@link #testGeographicCRS()} and {@link #testAxisSwapping()}.
     *
     * @param swap 1 if axes are expected to be swapped, or 0 otherwise.
     */
    private void testGeographicCRS(final int swap, final String wkt) throws ParseException {
        final GeographicCRS crs = parse(GeographicCRS.class, wkt);
        assertNameAndIdentifierEqual("WGS84", 0, crs);

        final GeodeticDatum datum = crs.getDatum();
        assertNameAndIdentifierEqual("World Geodetic System 1984", 0, datum);
        assertNameAndIdentifierEqual("Greenwich", 0, datum.getPrimeMeridian());

        final Ellipsoid ellipsoid = datum.getEllipsoid();
        assertNameAndIdentifierEqual("WGS84", 0, ellipsoid);
        assertEquals("semiMajor", 6378137, ellipsoid.getSemiMajorAxis(), STRICT);
        assertEquals("inverseFlattening", 298.257223563, ellipsoid.getInverseFlattening(), STRICT);

        final EllipsoidalCS cs = crs.getCoordinateSystem();
        assertEquals("dimension", 2, cs.getDimension());
        assertLongitudeAxisEquals(cs.getAxis(0 ^ swap));
        assertLatitudeAxisEquals (cs.getAxis(1 ^ swap));
    }

    /**
     * Tests the parsing of a projected CRS from a WKT 1 string.
     *
     * @throws ParseException if the parsing failed.
     */
    @Test
    @DependsOnMethod("testGeographicCRS")
    public void testProjectedCRS() throws ParseException {
        final ProjectedCRS crs = parse(ProjectedCRS.class,
                "PROJCS[“Mercator test”,\n" +
               "  GEOGCS[“WGS84”,\n" +
               "    DATUM[“World Geodetic System 1984”,\n" +
               "      SPHEROID[“WGS84”, 6378137.0, 298.257223563]],\n" +
               "      PRIMEM[“Greenwich”, 0.0],\n" +
               "    UNIT[“degree”, 0.017453292519943295],\n" +
               "    AXIS[“Longitude”, EAST],\n" +
               "    AXIS[“Latitude”, NORTH]],\n" +
               "  PROJECTION[“Mercator_1SP”],\n" +
               "  PARAMETER[“central_meridian”, -20.0],\n" +
               "  PARAMETER[“scale_factor”, 1.0],\n" +
               "  PARAMETER[“false_easting”, 500000.0],\n" +
               "  PARAMETER[“false_northing”, 0.0],\n" +
               "  UNIT[“metre”, 1.0],\n" +
               "  AXIS[“Easting”, EAST],\n" +
               "  AXIS[“Northing”, NORTH]]");

        assertNameAndIdentifierEqual("Mercator test", 0, crs);

        final GeodeticDatum datum = crs.getDatum();
        assertNameAndIdentifierEqual("World Geodetic System 1984", 0, datum);
        assertNameAndIdentifierEqual("Greenwich", 0, datum.getPrimeMeridian());

        final Ellipsoid ellipsoid = datum.getEllipsoid();
        assertNameAndIdentifierEqual("WGS84", 0, ellipsoid);
        assertEquals("semiMajor", 6378137, ellipsoid.getSemiMajorAxis(), STRICT);
        assertEquals("inverseFlattening", 298.257223563, ellipsoid.getInverseFlattening(), STRICT);

        final CartesianCS cs = crs.getCoordinateSystem();
        assertEquals("dimension", 2, cs.getDimension());
        assertUnboundedAxisEquals("Easting",  "E", AxisDirection.EAST,  SI.METRE, cs.getAxis(0));
        assertUnboundedAxisEquals("Northing", "N", AxisDirection.NORTH, SI.METRE, cs.getAxis(1));

        assertEquals("Mercator (variant A)", crs.getConversionFromBase().getMethod().getName().getCode());
        final ParameterValueGroup param = crs.getConversionFromBase().getParameterValues();
//      assertEquals("semi_major",   6378137.0, param.parameter("semi_major"      ).doubleValue(), STRICT);
//      assertEquals("semi_minor",   6356752.3, param.parameter("semi_minor"      ).doubleValue(), 0.1);
        assertEquals("central_meridian", -20.0, param.parameter("central_meridian").doubleValue(), STRICT);
        assertEquals("scale_factor",       1.0, param.parameter("scale_factor"    ).doubleValue(), STRICT);
        assertEquals("false_easting", 500000.0, param.parameter("false_easting"   ).doubleValue(), STRICT);
        assertEquals("false_northing",     0.0, param.parameter("false_northing"  ).doubleValue(), STRICT);
    }

    /**
     * Tests the parsing of a compound CRS from a WKT 1 string, except the time dimension which is WKT 2.
     *
     * @throws ParseException if the parsing failed.
     */
    @Test
    @DependsOnMethod("testGeographicCRS")
    public void testCompoundCRS() throws ParseException {
        final CompoundCRS crs = parse(CompoundCRS.class,
                "COMPD_CS[“WGS 84 + height + time”,\n" +
                "  GEOGCS[“WGS 84”,\n" +
                "    DATUM[“World Geodetic System 1984”,\n" +
                "      SPHEROID[“WGS84”, 6378137.0, 298.257223563]],\n" +
                "    PRIMEM[“Greenwich”, 0.0],\n" +
                "    UNIT[“degree”, 0.017453292519943295],\n" +
                "    AXIS[“Longitude”, EAST],\n" +
                "    AXIS[“Latitude”, NORTH]],\n" +
                "  VERT_CS[“Gravity-related height”,\n" +
                "    VERT_DATUM[“Mean Sea Level”, 2005],\n" +
                "    UNIT[“metre”, 1],\n" +
                "    AXIS[“Gravity-related height”, UP]],\n" +
                "  TimeCRS[“Time”,\n" +     // WKT 2
                "    TimeDatum[“Modified Julian”, TimeOrigin[1858-11-17T00:00:00.0Z]],\n" +
                "    Unit[“day”, 86400],\n" +
                "    Axis[“Time”, FUTURE]]]");

        // CompoundCRS parent
        assertNameAndIdentifierEqual("WGS 84 + height + time", 0, crs);
        final Iterator<CoordinateReferenceSystem> components = crs.getComponents().iterator();

        // GeographicCRS child
        final GeographicCRS geoCRS    = (GeographicCRS) components.next();
        final GeodeticDatum geoDatum  = geoCRS.getDatum();
        final Ellipsoid     ellipsoid = geoDatum.getEllipsoid();
        assertNameAndIdentifierEqual("WGS 84", 0, geoCRS);
        assertNameAndIdentifierEqual("World Geodetic System 1984", 0, geoDatum);
        assertNameAndIdentifierEqual("WGS84", 0, ellipsoid);
        assertNameAndIdentifierEqual("Greenwich", 0, geoDatum.getPrimeMeridian());
        assertEquals("semiMajor", 6378137, ellipsoid.getSemiMajorAxis(), STRICT);
        assertEquals("inverseFlattening", 298.257223563, ellipsoid.getInverseFlattening(), STRICT);

        // VerticalCRS child
        final VerticalCRS vertCRS = (VerticalCRS) components.next();
        assertNameAndIdentifierEqual("Gravity-related height", 0, vertCRS);
        assertNameAndIdentifierEqual("Mean Sea Level", 0, vertCRS.getDatum());

        // TemporalCRS child
        final TemporalCRS   timeCRS   = (TemporalCRS) components.next();
        final TemporalDatum timeDatum = timeCRS.getDatum();
        assertNameAndIdentifierEqual("Time", 0, timeCRS);
        assertNameAndIdentifierEqual("Modified Julian", 0, timeDatum);
        assertEquals("epoch", new Date(-40587 * (24*60*60*1000L)), timeDatum.getOrigin());

        // No more CRS.
        assertFalse(components.hasNext());

        // Axes: we verify only the CompoundCRS ones, which should include all others.
        final CoordinateSystem cs = crs.getCoordinateSystem();
        assertEquals("dimension", 4, cs.getDimension());
        assertLongitudeAxisEquals(cs.getAxis(0));
        assertLatitudeAxisEquals (cs.getAxis(1));
        assertUnboundedAxisEquals("Gravity-related height", "H", AxisDirection.UP, SI.METRE, cs.getAxis(2));
        assertUnboundedAxisEquals("Time", "t", AxisDirection.FUTURE, NonSI.DAY, cs.getAxis(3));
    }
}
