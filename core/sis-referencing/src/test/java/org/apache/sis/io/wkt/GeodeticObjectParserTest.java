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
import javax.measure.quantity.Length;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.internal.metadata.AxisNames;
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
     * Uses a new parser for the given convention.
     */
    private void setConvention(final Convention convention, final boolean isAxisIgnored) {
        final GeodeticObjectParser p = parser;
        parser = new GeodeticObjectParser(p.symbols, convention, isAxisIgnored, p.errorLocale, null);
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
        assertAxisEquals(AxisNames.GEODETIC_LONGITUDE, "λ", AxisDirection.EAST, -180, +180, NonSI.DEGREE_ANGLE, RangeMeaning.WRAPAROUND, axis);
    }

    /**
     * Asserts that the given axis is a latitude axis. This method expects the name to be
     * <cite>"Geodetic latitude"</cite> even if the WKT string contained a different name,
     * because {@link GeodeticObjectParser} should have done the replacement.
     */
    private static void assertLatitudeAxisEquals(final CoordinateSystemAxis axis) {
        assertAxisEquals(AxisNames.GEODETIC_LATITUDE, "φ", AxisDirection.NORTH, -90, +90, NonSI.DEGREE_ANGLE, RangeMeaning.EXACT, axis);
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
     * Verifies that the axes of the coordinate system are those of a projected CRS,
     * with (East, North) axis directions.
     */
    private static void verifyProjectedCS(final CartesianCS cs, final Unit<Length> unit) {
        assertEquals("dimension", 2, cs.getDimension());
        assertUnboundedAxisEquals("Easting",  "E", AxisDirection.EAST,  unit, cs.getAxis(0));
        assertUnboundedAxisEquals("Northing", "N", AxisDirection.NORTH, unit, cs.getAxis(1));
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
        assertUnboundedAxisEquals(AxisNames.GEOCENTRIC_X, "X", AxisDirection.GEOCENTRIC_X, SI.METRE, cs.getAxis(0));
        assertUnboundedAxisEquals(AxisNames.GEOCENTRIC_Y, "Y", AxisDirection.GEOCENTRIC_Y, SI.METRE, cs.getAxis(1));
        assertUnboundedAxisEquals(AxisNames.GEOCENTRIC_Z, "Z", AxisDirection.GEOCENTRIC_Z, SI.METRE, cs.getAxis(2));
    }

    /**
     * Tests the parsing of a geographic CRS from a WKT 1 string using (longitude, latitude) axis order.
     *
     * @throws ParseException if the parsing failed.
     */
    @Test
    public void testGeographicCRS() throws ParseException {
        verifyGeographicCRS(0, parse(GeographicCRS.class,
               "  GEOGCS[“WGS 84”,\n" +
               "    DATUM[“World Geodetic System 1984”,\n" +
               "      SPHEROID[“WGS84”, 6378137.0, 298.257223563]],\n" +
               "      PRIMEM[“Greenwich”, 0.0],\n" +
               "    UNIT[“degree”, 0.017453292519943295],\n" +
               "    AXIS[“Longitude”, EAST],\n" +
               "    AXIS[“Latitude”, NORTH]]"));
    }

    /**
     * Tests the parsing of a geographic CRS from a WKT 1 string using (latitude, longitude) axis order.
     *
     * @throws ParseException if the parsing failed.
     */
    @Test
    @DependsOnMethod("testGeographicCRS")
    public void testWithAxisSwapping() throws ParseException {
        verifyGeographicCRS(1, parse(GeographicCRS.class,
               "  GEOGCS[“WGS 84”,\n" +
               "    DATUM[“World Geodetic System 1984”,\n" +
               "      SPHEROID[“WGS84”, 6378137.0, 298.257223563]],\n" +
               "      PRIMEM[“Greenwich”, 0.0],\n" +
               "    UNIT[“degree”, 0.017453292519943295],\n" +
               "    AXIS[“Latitude”, NORTH],\n" +
               "    AXIS[“Longitude”, EAST]]"));
    }

    /**
     * Tests the parsing of a geographic CRS from a WKT 1 string that does not declare explicitly the axes.
     *
     * @throws ParseException if the parsing failed.
     *
     * @see #testWithImplicitAxesInSeconds()
     */
    @Test
    @DependsOnMethod("testGeographicCRS")
    public void testWithImplicitAxes() throws ParseException {
        verifyGeographicCRS(0, parse(GeographicCRS.class,
               "  GEOGCS[“WGS 84”,\n" +
               "    DATUM[“World Geodetic System 1984”,\n" +
               "      SPHEROID[“WGS84”, 6378137.0, 298.257223563]],\n" +
               "      PRIMEM[“Greenwich”, 0.0],\n" +
               "    UNIT[“degree”, 0.017453292519943295]]"));
    }

    /**
     * Implementation of {@link #testGeographicCRS()} and {@link #testWithAxisSwapping()}.
     * This test expects no {@code AUTHORITY} element on any component.
     *
     * @param swap 1 if axes are expected to be swapped, or 0 otherwise.
     */
    private void verifyGeographicCRS(final int swap, final GeographicCRS crs) throws ParseException {
        assertNameAndIdentifierEqual("WGS 84", 0, crs);

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
                "  GEOGCS[“WGS 84”,\n" +
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
        verifyProjectedCS(crs.getCoordinateSystem(), SI.METRE);
        verifyGeographicCRS(0, crs.getBaseCRS());

        final GeodeticDatum datum = crs.getDatum();
        assertNameAndIdentifierEqual("World Geodetic System 1984", 0, datum);
        assertNameAndIdentifierEqual("Greenwich", 0, datum.getPrimeMeridian());

        final Ellipsoid ellipsoid = datum.getEllipsoid();
        assertNameAndIdentifierEqual("WGS84", 0, ellipsoid);
        assertEquals("semiMajor", 6378137, ellipsoid.getSemiMajorAxis(), STRICT);
        assertEquals("inverseFlattening", 298.257223563, ellipsoid.getInverseFlattening(), STRICT);

        assertEquals("Mercator (variant A)", crs.getConversionFromBase().getMethod().getName().getCode());
        final ParameterValueGroup param = crs.getConversionFromBase().getParameterValues();
        assertEquals("semi_major",   6378137.0, param.parameter("semi_major"      ).doubleValue(SI   .METRE),        STRICT);
        assertEquals("semi_minor",   6356752.3, param.parameter("semi_minor"      ).doubleValue(SI   .METRE),        0.1);
        assertEquals("central_meridian", -20.0, param.parameter("central_meridian").doubleValue(NonSI.DEGREE_ANGLE), STRICT);
        assertEquals("scale_factor",       1.0, param.parameter("scale_factor"    ).doubleValue(Unit .ONE),          STRICT);
        assertEquals("false_easting", 500000.0, param.parameter("false_easting"   ).doubleValue(SI   .METRE),        STRICT);
        assertEquals("false_northing",     0.0, param.parameter("false_northing"  ).doubleValue(SI   .METRE),        STRICT);
    }

    /**
     * Tests parsing of a CRS using a prime meridian other than Greenwich. The result depends on whether
     * the parsing is standard compliant or if the parsing use {@link Convention#WKT1_COMMON_UNITS}.
     *
     * @throws ParseException if the parsing failed.
     */
    @Test
    @DependsOnMethod("testGeographicCRS")
    public void testWithNonGreenwichMeridian() throws ParseException {
        String wkt = "GEOGCS[“NTF (Paris)”,\n" +
                     "  DATUM[“Nouvelle Triangulation Française (Paris)”,\n" +
                     "    SPHEROID[“Clarke 1880 (IGN)”, 6378249.2, 293.4660212936269]],\n" +
                     "    PRIMEM[“Paris”, 2.5969213, AUTHORITY[“EPSG”, “8903”]],\n" +
                     "  UNIT[“grade”, 0.015707963267948967],\n" +
                     "  AXIS[“Latitude”, NORTH],\n" +
                     "  AXIS[“Longitude”, EAST]]";

        GeographicCRS crs = parse(GeographicCRS.class, wkt);
        assertNameAndIdentifierEqual("NTF (Paris)", 0, crs);
        PrimeMeridian pm = verifyNTF(crs.getDatum());
        assertEquals("angularUnit", NonSI.GRADE, pm.getAngularUnit());
        assertEquals("greenwichLongitude", 2.5969213, pm.getGreenwichLongitude(), STRICT);
        EllipsoidalCS cs = crs.getCoordinateSystem();
        assertEquals("dimension", 2, cs.getDimension());
        assertAxisEquals(AxisNames.GEODETIC_LATITUDE,  "φ", AxisDirection.NORTH, -100, +100, NonSI.GRADE, RangeMeaning.EXACT,      cs.getAxis(0));
        assertAxisEquals(AxisNames.GEODETIC_LONGITUDE, "λ", AxisDirection.EAST,  -200, +200, NonSI.GRADE, RangeMeaning.WRAPAROUND, cs.getAxis(1));
        /*
         * Parse again using Convention.WKT1_COMMON_UNITS and ignoring AXIS[…] elements (but not the UNIT[…] element,
         * which still apply to the axes). When using this convention, the parser does not apply the angular units to
         * the Greenwich longitude value in PRIMEM[…] elements. Instead, the longitude is unconditionally interpreted
         * as a value in degrees, which is why we convert "2.5969213" grade to "2.33722917" degrees in the WKT before
         * to run the test (but we do NOT change the UNIT[…] element since the purpose of this test is to verify that
         * those units are ignored).
         *
         * This is a violation of both OGC 01-009 and ISO 19162 standards, but this is what GDAL does.
         * So we allow this interpretation in Convention.WKT1_COMMON_UNITS for compatibility reasons.
         */
        wkt = wkt.replace("2.5969213", "2.33722917");   // Convert unit in prime meridian.
        setConvention(Convention.WKT1_COMMON_UNITS, true);
        crs = parse(GeographicCRS.class, wkt);
        assertNameAndIdentifierEqual("NTF (Paris)", 0, crs);
        pm = verifyNTF(crs.getDatum());
        assertEquals("angularUnit", NonSI.DEGREE_ANGLE, pm.getAngularUnit());
        assertEquals("greenwichLongitude", 2.33722917, pm.getGreenwichLongitude(), STRICT);
        cs = crs.getCoordinateSystem();
        assertEquals("dimension", 2, cs.getDimension());
        assertAxisEquals(AxisNames.GEODETIC_LONGITUDE, "λ", AxisDirection.EAST,  -200, +200, NonSI.GRADE, RangeMeaning.WRAPAROUND, cs.getAxis(0));
        assertAxisEquals(AxisNames.GEODETIC_LATITUDE,  "φ", AxisDirection.NORTH, -100, +100, NonSI.GRADE, RangeMeaning.EXACT,      cs.getAxis(1));
    }

    /**
     * Tests the parsing of a projected CRS using angular values in grades instead than degrees
     * and in lengths in kilometres instead than metres.
     *
     * @throws ParseException if the parsing failed.
     */
    @Test
    @DependsOnMethod({"testWithNonGreenwichMeridian", "testProjectedCRS"})
    public void testWithLessCommonUnits() throws ParseException {
        String wkt = "PROJCS[“NTF (Paris) / Lambert zone II”," +
                     "  GEOGCS[“NTF (Paris)”," +
                     "    DATUM[“Nouvelle Triangulation Française (Paris)”," +
                     "      SPHEROID[“Clarke 1880 (IGN)”, 6378249.2, 293.4660212936269]," +
                     "      TOWGS84[-168,-60,320,0,0,0,0]]," +
                     "    PRIMEM[“Paris”, 2.5969213, AUTHORITY[“EPSG”, “8903”]]," +  // In grads.
                     "    UNIT[“grad”, 0.01570796326794897]]," +
                     "  PROJECTION[“Lambert Conformal Conic (1SP)”]," +  // Intentional swapping of "Conformal" and "Conic".
                     "  PARAMETER[“latitude_of_origin”, 52.0]," +        // In grads.
                     "  PARAMETER[“scale_factor”, 0.99987742]," +
                     "  PARAMETER[“false_easting”, 600.0]," +
                     "  PARAMETER[“false_northing”, 2200.0]," +
                     "  UNIT[“metre”,1000]]";

        ProjectedCRS crs = parse(ProjectedCRS.class, wkt);
        assertNameAndIdentifierEqual("NTF (Paris) / Lambert zone II", 0, crs);
        verifyProjectedCS(crs.getCoordinateSystem(), SI.KILOMETRE);
        PrimeMeridian pm = verifyNTF(crs.getDatum());
        assertEquals("angularUnit", NonSI.GRADE, pm.getAngularUnit());
        assertEquals("greenwichLongitude", 2.5969213, pm.getGreenwichLongitude(), STRICT);
        ParameterValue<?> param = verifyNTF(crs.getConversionFromBase().getParameterValues());
        assertEquals("angularUnit", NonSI.GRADE, param.getUnit());
        assertEquals("latitude_of_origin",  52.0, param.doubleValue(), STRICT);
        /*
         * Parse again using Convention.WKT1_COMMON_UNITS and ignoring AXIS[…] elements.
         * See the comment in 'testWithNonGreenwichMeridian()' method for a discussion.
         * The new aspect tested by this method is that the unit should be ignored
         * for the parameters in addition to the prime meridian.
         */
        wkt = wkt.replace("2.5969213", "2.33722917");       // Convert unit in prime meridian.
        wkt = wkt.replace("52.0",      "46.8");             // Convert unit in “latitude_of_origin” parameter.
        wkt = wkt.replace("600.0",     "600000");           // Convert unit in “false_easting” parameter.
        wkt = wkt.replace("2200.0",    "2200000");          // Convert unit in “false_northing” parameter.
        setConvention(Convention.WKT1_COMMON_UNITS, true);
        crs = parse(ProjectedCRS.class, wkt);
        assertNameAndIdentifierEqual("NTF (Paris) / Lambert zone II", 0, crs);
        verifyProjectedCS(crs.getCoordinateSystem(), SI.KILOMETRE);
        pm = verifyNTF(crs.getDatum());
        assertEquals("angularUnit", NonSI.DEGREE_ANGLE, pm.getAngularUnit());
        assertEquals("greenwichLongitude", 2.33722917, pm.getGreenwichLongitude(), STRICT);
        param = verifyNTF(crs.getConversionFromBase().getParameterValues());
        assertEquals("angularUnit", NonSI.DEGREE_ANGLE, param.getUnit());
        assertEquals("latitude_of_origin",  46.8, param.doubleValue(), STRICT);
    }

    /**
     * Verifies the properties of a datum which is expected to be “Nouvelle Triangulation Française (Paris)”.
     * This is used by the methods in this class which test a CRS using less frequently used units and prime
     * meridian.
     *
     * @return The prime meridian, to be verified by the caller because the unit of measurement depends on the test.
     */
    private static PrimeMeridian verifyNTF(final GeodeticDatum datum) {
        assertNameAndIdentifierEqual("Nouvelle Triangulation Française (Paris)", 0, datum);

        final Ellipsoid ellipsoid = datum.getEllipsoid();
        assertNameAndIdentifierEqual("Clarke 1880 (IGN)", 0, ellipsoid);
        assertEquals("semiMajor", 6378249.2, ellipsoid.getSemiMajorAxis(), STRICT);
        assertEquals("inverseFlattening", 293.4660212936269, ellipsoid.getInverseFlattening(), STRICT);

        final PrimeMeridian pm = datum.getPrimeMeridian();
        assertNameAndIdentifierEqual("Paris", 8903, pm);
        return pm;
    }

    /**
     * Verifies the parameter values for a projected CRS which is expected to be “NTF (Paris) / Lambert zone II”.
     * This is used by the methods in this class which test a CRS using less frequently used units and prime meridian.
     *
     * @return The latitude of origin, to be verified by the caller because the unit of measurement depends on the test.
     */
    private static ParameterValue<?> verifyNTF(final ParameterValueGroup param) {
        assertEquals("Lambert Conic Conformal (1SP)", param.getDescriptor().getName().getCode());
        assertEquals("semi_major",     6378249.2, param.parameter("semi_major"      ).doubleValue(SI   .METRE),        STRICT);
        assertEquals("semi_minor",     6356515.0, param.parameter("semi_minor"      ).doubleValue(SI   .METRE),        1E-12);
        assertEquals("central_meridian",     0.0, param.parameter("central_meridian").doubleValue(NonSI.DEGREE_ANGLE), STRICT);
        assertEquals("scale_factor",  0.99987742, param.parameter("scale_factor"    ).doubleValue(Unit .ONE),          STRICT);
        assertEquals("false_easting",   600000.0, param.parameter("false_easting"   ).doubleValue(SI   .METRE),        STRICT);
        assertEquals("false_northing", 2200000.0, param.parameter("false_northing"  ).doubleValue(SI   .METRE),        STRICT);
        return param.parameter("latitude_of_origin");
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
        verifyGeographicCRS(0, (GeographicCRS) components.next());

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

    /**
     * Tests parsing geographic CRS with implicit axes in seconds instead of degrees.
     *
     * @throws ParseException if the parsing failed.
     *
     * @see #testWithImplicitAxes()
     */
    @Test
    @DependsOnMethod("testWithImplicitAxes")
    public void testWithImplicitAxesInSeconds() throws ParseException {
        final GeographicCRS crs = parse(GeographicCRS.class,
                "GEOGCS[“NAD83 / NFIS Seconds”," +
                "DATUM[“North_American_Datum_1983”,\n" +
                "SPHEROID[“GRS 1980”, 6378137, 298.257222101]],\n" +
                "PRIMEM[“Greenwich”, 0],\n" +
                "UNIT[“Decimal_Second”, 4.84813681109536e-06],\n" +
                "AUTHORITY[“EPSG”, “100001”]]");

        assertNameAndIdentifierEqual("NAD83 / NFIS Seconds", 100001, crs);

        final GeodeticDatum datum = crs.getDatum();
        assertNameAndIdentifierEqual("North_American_Datum_1983", 0, datum);
        assertNameAndIdentifierEqual("Greenwich", 0, datum.getPrimeMeridian());

        final Ellipsoid ellipsoid = datum.getEllipsoid();
        assertNameAndIdentifierEqual("GRS 1980", 0, ellipsoid);
        assertEquals("semiMajor", 6378137, ellipsoid.getSemiMajorAxis(), STRICT);
        assertEquals("inverseFlattening", 298.257222101, ellipsoid.getInverseFlattening(), STRICT);

        final EllipsoidalCS cs = crs.getCoordinateSystem();
        final double secondsIn90 = 90*60*60;
        CoordinateSystemAxis axis = cs.getAxis(0);
        assertEquals("name", AxisNames.GEODETIC_LONGITUDE, axis.getName().getCode());
        assertEquals("abbreviation", "λ",                  axis.getAbbreviation());
        assertEquals("direction",    AxisDirection.EAST,   axis.getDirection());
        assertEquals("minimumValue", -secondsIn90*2,       axis.getMinimumValue(), 1E-9);
        assertEquals("maximumValue", +secondsIn90*2,       axis.getMaximumValue(), 1E-9);

        axis = cs.getAxis(1);
        assertEquals("name", AxisNames.GEODETIC_LATITUDE,  axis.getName().getCode());
        assertEquals("abbreviation", "φ",                  axis.getAbbreviation());
        assertEquals("direction",    AxisDirection.NORTH,  axis.getDirection());
        assertEquals("minimumValue", -secondsIn90,         axis.getMinimumValue(), 1E-9);
        assertEquals("maximumValue", +secondsIn90,         axis.getMaximumValue(), 1E-9);
    }

    /**
     * Tests parsing a WKT with a missing Geographic CRS name.
     * This should be considered invalid, but happen in practice.
     *
     * <p>The WKT tested in this method contains also some other oddities compared to the usual WKT:</p>
     * <ul>
     *   <li>The prime meridian is declared in the {@code "central_meridian"} projection parameter instead
     *       than in the {@code PRIMEM[…]} element.</li>
     *   <li>Some elements are not in the usual order.</li>
     * </ul>
     *
     * @throws ParseException if the parsing failed.
     */
    @Test
    @DependsOnMethod("testProjectedCRS")
    public void testWithMissingName() throws ParseException {
        final ProjectedCRS crs = parse(ProjectedCRS.class,
                "PROJCS[“FRANCE/NTF/Lambert III”," +
                "GEOGCS[“”," + // Missing name (the purpose of this test).
                "DATUM[“NTF=GR3DF97A”,TOWGS84[-168, -60, 320, 0, 0, 0, 0] ," + // Intentionally misplaced coma.
                "SPHEROID[“Clarke 1880 (IGN)”,6378249.2,293.4660212936269]]," +
                "PRIMEM[“Greenwich”,0],UNIT[“Degrees”,0.0174532925199433]," +
                "AXIS[“Long”,East],AXIS[“Lat”,North]]," +
                "PROJECTION[“Lambert_Conformal_Conic_1SP”]," +
                "PARAMETER[“latitude_of_origin”,44.1]," +
                "PARAMETER[“central_meridian”,2.33722917]," +   // Paris prime meridian.
                "PARAMETER[“scale_factor”,0.999877499]," +
                "PARAMETER[“false_easting”,600000]," +
                "PARAMETER[“false_northing”,200000]," +
                "UNIT[“Meter”,1]," +
                "AXIS[“Easting”,East],AXIS[“Northing”,North]]");

        assertNameAndIdentifierEqual("FRANCE/NTF/Lambert III", 0, crs);
        verifyProjectedCS(crs.getCoordinateSystem(), SI.METRE);
        final GeographicCRS geoCRS = crs.getBaseCRS();
        assertNameAndIdentifierEqual("NTF=GR3DF97A", 0, geoCRS);    // Inherited the datum name.

        final GeodeticDatum datum = geoCRS.getDatum();
        assertNameAndIdentifierEqual("NTF=GR3DF97A", 0, datum);
        assertNameAndIdentifierEqual("Greenwich", 0, datum.getPrimeMeridian());

        final Ellipsoid ellipsoid = datum.getEllipsoid();
        assertNameAndIdentifierEqual("Clarke 1880 (IGN)", 0, ellipsoid);
        assertEquals("semiMajor", 6378249.2, ellipsoid.getSemiMajorAxis(), STRICT);
        assertEquals("inverseFlattening", 293.4660212936269, ellipsoid.getInverseFlattening(), STRICT);

        final EllipsoidalCS cs = geoCRS.getCoordinateSystem();
        assertEquals("dimension", 2, cs.getDimension());
        assertLongitudeAxisEquals(cs.getAxis(0));
        assertLatitudeAxisEquals (cs.getAxis(1));

        final ParameterValueGroup param = crs.getConversionFromBase().getParameterValues();
        assertEquals("Lambert Conic Conformal (1SP)", param.getDescriptor().getName().getCode());
        assertEquals("semi_major",        6378249.2, param.parameter("semi_major"        ).doubleValue(SI   .METRE),        STRICT);
        assertEquals("semi_minor",        6356515.0, param.parameter("semi_minor"        ).doubleValue(SI   .METRE),        1E-12);
        assertEquals("latitude_of_origin",     44.1, param.parameter("latitude_of_origin").doubleValue(NonSI.DEGREE_ANGLE), STRICT);
        assertEquals("central_meridian", 2.33722917, param.parameter("central_meridian"  ).doubleValue(NonSI.DEGREE_ANGLE), STRICT);
        assertEquals("scale_factor",    0.999877499, param.parameter("scale_factor"      ).doubleValue(Unit .ONE),          STRICT);
        assertEquals("false_easting",      600000.0, param.parameter("false_easting"     ).doubleValue(SI   .METRE),        STRICT);
        assertEquals("false_northing",     200000.0, param.parameter("false_northing"    ).doubleValue(SI   .METRE),        STRICT);
    }
}
