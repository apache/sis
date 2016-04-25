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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
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
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.internal.metadata.AxisNames;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.datum.BursaWolfParameters;
import org.apache.sis.referencing.datum.DefaultGeodeticDatum;
import org.apache.sis.referencing.factory.GeodeticObjectFactory;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.ReferencingAssert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;

// Branch-specific imports
import org.opengis.util.Factory;


/**
 * Tests {@link GeodeticObjectParser}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.6
 * @version 0.7
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
    org.apache.sis.referencing.crs.DefaultEngineeringCRSTest.class,
    org.apache.sis.referencing.crs.DefaultImageCRSTest.class,
    org.apache.sis.referencing.cs.DirectionAlongMeridianTest.class
})
public final strictfp class GeodeticObjectParserTest extends TestCase {
    /**
     * The parser to use for the test.
     */
    private GeodeticObjectParser parser;

    /**
     * Instantiates the parser to test.
     */
    private void newParser(final Convention convention) {
        parser = new GeodeticObjectParser(Symbols.getDefault(), Collections.<String,Element>emptyMap(),
                null, null, null, convention, Transliterator.DEFAULT, null, new HashMap<Class<?>,Factory>());
        assertEquals(GeodeticObjectFactory.class.getCanonicalName(), parser.getPublicFacade());
    }

    /**
     * Parses the given text. It is caller's responsibility to verify if some warnings have been emitted.
     *
     * @param  type The expected object type.
     * @param  text The WKT string to parse.
     * @return The parsed object.
     * @throws ParseException if an error occurred during the parsing.
     */
    private <T> T parseIgnoreWarnings(final Class<T> type, final String text) throws ParseException {
        if (parser == null) {
            newParser(Convention.DEFAULT);
        }
        final ParsePosition position = new ParsePosition(0);
        final Object obj = parser.parseObject(text, position);
        assertEquals("errorIndex", -1, position.getErrorIndex());
        assertEquals("index", text.length(), position.getIndex());
        assertInstanceOf("GeodeticObjectParser.parseObject", type, obj);
        return type.cast(obj);
    }

    /**
     * Parses the given text and ensure that no warnings have been emitted.
     *
     * @param  type The expected object type.
     * @param  text The WKT string to parse.
     * @return The parsed object.
     * @throws ParseException if an error occurred during the parsing.
     */
    private <T> T parse(final Class<T> type, final String text) throws ParseException {
        final T obj = parseIgnoreWarnings(type, text);
        assertNull("warnings", parser.getAndClearWarnings(obj));
        assertTrue("ignoredElements", parser.ignoredElements.isEmpty());
        return obj;
    }

    /**
     * Asserts that the name and (optionally) the EPSG identifier of the given object are equal to the given strings.
     * As a special case if the given EPSG code is 0, then this method verifies that the given object has no identifier.
     *
     * <p>This method is similar to {@link #assertEpsgNameAndIdentifierEqual(String, int, IdentifiedObject)} except
     * that the given name is not necessarily in the EPSG namespace and the EPSG code is allowed to be absent.</p>
     *
     * @param name The expected name.
     * @param epsg The expected EPSG identifier, or {@code 0} if the object shall have no identifier.
     */
    static void assertNameAndIdentifierEqual(final String name, final int epsg, final IdentifiedObject object) {
        final String message = object.getClass().getSimpleName();
        assertEquals(message, name, object.getName().getCode());
        if (epsg != 0) {
            assertEquals(message, String.valueOf(epsg), getSingleton(object.getIdentifiers()).getCode());
        } else {
            assertTrue(message, object.getIdentifiers().isEmpty());
        }
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
     * Tests the parsing of an axis.
     *
     * @throws ParseException if the parsing failed.
     */
    @Test
    public void testAxis() throws ParseException {
        CoordinateSystemAxis axis = parse(CoordinateSystemAxis.class, "AXIS[“(Y)”, geocentricY]");
        assertEquals("name", "Y", axis.getName().getCode());
        assertEquals("abbreviation", "Y", axis.getAbbreviation());
        assertEquals("direction", AxisDirection.GEOCENTRIC_Y, axis.getDirection());
        assertEquals("unit", SI.METRE, axis.getUnit());

        axis = parse(CoordinateSystemAxis.class, "AXIS[“latitude”,north,ORDER[1],ANGLEUNIT[“degree”,0.0174532925199433]]");
        assertEquals("name", "Latitude", axis.getName().getCode());
        assertEquals("abbreviation", "φ", axis.getAbbreviation());
        assertEquals("direction", AxisDirection.NORTH, axis.getDirection());
        assertEquals("unit", NonSI.DEGREE_ANGLE, axis.getUnit());

        axis = parse(CoordinateSystemAxis.class, "AXIS[“longitude”,EAST,order[2],UNIT[“degree”,0.0174532925199433]]");
        assertEquals("name", "Longitude", axis.getName().getCode());
        assertEquals("abbreviation", "λ", axis.getAbbreviation());
        assertEquals("direction", AxisDirection.EAST, axis.getDirection());
        assertEquals("unit", NonSI.DEGREE_ANGLE, axis.getUnit());

        axis = parse(CoordinateSystemAxis.class, "AXIS[“ellipsoidal height (h)”,up,ORDER[3],LengthUnit[“kilometre”,1000]]");
        assertEquals("name", "Ellipsoidal height", axis.getName().getCode());
        assertEquals("abbreviation", "h", axis.getAbbreviation());
        assertEquals("direction", AxisDirection.UP, axis.getDirection());
        assertEquals("unit", SI.KILOMETRE, axis.getUnit());

        axis = parse(CoordinateSystemAxis.class, "AXIS[“time (t)”,future,TimeUnit[“hour”,3600]]");
        assertEquals("name", "Time", axis.getName().getCode());
        assertEquals("abbreviation", "t", axis.getAbbreviation());
        assertEquals("direction", AxisDirection.FUTURE, axis.getDirection());
        assertEquals("unit", NonSI.HOUR, axis.getUnit());

        axis = parse(CoordinateSystemAxis.class, "AXIS[“easting (X)”,south,MERIDIAN[90,UNIT[“degree”,0.0174532925199433]]]");
        assertEquals("name", "Easting", axis.getName().getCode());
        assertEquals("abbreviation", "X", axis.getAbbreviation());
        assertEquals("direction", CoordinateSystems.directionAlongMeridian(AxisDirection.SOUTH, 90), axis.getDirection());
        assertEquals("unit", SI.METRE, axis.getUnit());
    }

    /**
     * Tests the parsing of a geodetic datum from a WKT 2 string.
     *
     * @throws ParseException if the parsing failed.
     */
    @Test
    public void testDatum() throws ParseException {
        final GeodeticDatum datum = parse(GeodeticDatum.class,
                "DATUM[“Tananarive 1925”,\n" +
                "  ELLIPSOID[“International 1924”, 6378.388, 297.0, LENGTHUNIT[“km”, 1000]],\n" +
                "  ANCHOR[“Tananarive observatory”]]");

        assertNameAndIdentifierEqual("Tananarive 1925", 0, datum);
        assertEquals("anchor", "Tananarive observatory", String.valueOf(datum.getAnchorPoint()));

        final Ellipsoid ellipsoid = datum.getEllipsoid();
        assertNameAndIdentifierEqual("International 1924", 0, ellipsoid);
        assertEquals("unit", SI.KILOMETRE, ellipsoid.getAxisUnit());
        assertEquals("semiMajor", 6378.388, ellipsoid.getSemiMajorAxis(), STRICT);
        assertEquals("inverseFlattening", 297, ellipsoid.getInverseFlattening(), STRICT);
        assertEquals("greenwichLongitude", 0, datum.getPrimeMeridian().getGreenwichLongitude(), STRICT);
    }

    /**
     * Tests the parsing of a geocentric CRS from a WKT 1 string. The parser
     * shall replace the OGC 01-009 axis directions (OTHER, EAST, NORTH) by
     * ISO 19111 axis directions (Geocentric X, Geocentric Y, Geocentric Z).
     *
     * @throws ParseException if the parsing failed.
     */
    @Test
    @DependsOnMethod({"testAxis", "testDatum"})
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
    @DependsOnMethod({"testAxis", "testDatum"})
    public void testGeographicCRS() throws ParseException {
        verifyGeographicCRS(0, parse(GeographicCRS.class,
               "GEOGCS[“WGS 84”,\n" +
               "  DATUM[“World Geodetic System 1984”,\n" +
               "    SPHEROID[“WGS84”, 6378137.0, 298.257223563]],\n" +
               "    PRIMEM[“Greenwich”, 0.0],\n" +
               "  UNIT[“degree”, 0.017453292519943295],\n" +
               "  AXIS[“Longitude”, EAST],\n" +
               "  AXIS[“Latitude”, NORTH]]"));
    }

    /**
     * Tests the parsing of a geographic CRS from a WKT 1 string using (latitude, longitude) axis order.
     *
     * @throws ParseException if the parsing failed.
     */
    @Test
    @DependsOnMethod("testGeographicCRS")
    public void testGeographicWithLatLonAxes() throws ParseException {
        verifyGeographicCRS(1, parse(GeographicCRS.class,
               "GEOGCS[“WGS 84”,\n" +
               "  DATUM[“World Geodetic System 1984”,\n" +
               "    SPHEROID[“WGS84”, 6378137.0, 298.257223563]],\n" +
               "    PRIMEM[“Greenwich”, 0.0],\n" +
               "  UNIT[“degree”, 0.017453292519943295],\n" +
               "  AXIS[“Latitude”, NORTH],\n" +
               "  AXIS[“Longitude”, EAST]]"));
    }

    /**
     * Tests the parsing of a geographic CRS from a WKT 1 string that does not declare explicitly the axes.
     *
     * @throws ParseException if the parsing failed.
     *
     * @see #testGeographicWithImplicitAxesInSeconds()
     */
    @Test
    @DependsOnMethod("testGeographicCRS")
    public void testGeographicWithImplicitAxes() throws ParseException {
        verifyGeographicCRS(0, parse(GeographicCRS.class,
               "GEOGCS[“WGS 84”,\n" +
               "  DATUM[“World Geodetic System 1984”,\n" +
               "    SPHEROID[“WGS84”, 6378137.0, 298.257223563]],\n" +
               "    PRIMEM[“Greenwich”, 0.0],\n" +
               "  UNIT[“degree”, 0.017453292519943295]]"));
    }

    /**
     * Tests parsing geographic CRS with implicit axes in seconds instead of degrees.
     *
     * @throws ParseException if the parsing failed.
     *
     * @see #testGeographicWithImplicitAxes()
     */
    @Test
    @DependsOnMethod("testGeographicWithImplicitAxes")
    public void testGeographicWithImplicitAxesInSeconds() throws ParseException {
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
     * Tests the parsing of a geographic CRS from a WKT string with axes in wrong order according the
     * {@code ORDER} elements. The {@code ORDER} elements are defined in WKT 2 (they did not existed in WKT 1),
     * but the rest of the string is WKT 1. The SIS parser should sort the axes in the order declared
     * in the {@code ORDER} elements.
     *
     * @throws ParseException if the parsing failed.
     */
    @Test
    @DependsOnMethod("testGeographicWithLatLonAxes")
    public void testGeographicWithUnorderedAxes() throws ParseException {
        verifyGeographicCRS(1, parse(GeographicCRS.class,
               "GEOGCS[“WGS 84”,\n" +
               "  DATUM[“World Geodetic System 1984”,\n" +
               "    SPHEROID[“WGS84”, 6378137.0, 298.257223563]],\n" +
               "    PRIMEM[“Greenwich”, 0.0],\n" +
               "  UNIT[“degree”, 0.017453292519943295],\n" +
               "  AXIS[“Longitude”, EAST, order[2]],\n" +
               "  AXIS[“Latitude”, NORTH, order[1]]]"));
    }

    /**
     * Tests parsing of a CRS using a prime meridian other than Greenwich. The result depends on whether
     * the parsing is standard compliant or if the parsing use {@link Convention#WKT1_COMMON_UNITS}.
     *
     * @throws ParseException if the parsing failed.
     */
    @Test
    @DependsOnMethod("testGeographicCRS")
    public void testGeographicWithParisMeridian() throws ParseException {
        String wkt = "GEOGCS[“NTF (Paris)”,\n" +
                     "  DATUM[“Nouvelle Triangulation Française (Paris)”,\n" +
                     "    SPHEROID[“Clarke 1880 (IGN)”, 6378249.2, 293.4660212936269]],\n" +
                     "    PRIMEM[“Paris”, 2.5969213, AUTHORITY[“EPSG”, “8903”]],\n" +
                     "  UNIT[“grade”, 0.015707963267948967],\n" +
                     "  AXIS[“Latitude”, NORTH],\n" +
                     "  AXIS[“Longitude”, EAST]]";

        GeographicCRS crs = parse(GeographicCRS.class, wkt);
        assertNameAndIdentifierEqual("NTF (Paris)", 0, crs);
        PrimeMeridian pm = verifyNTF(crs.getDatum(), false);
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
        newParser(Convention.WKT1_IGNORE_AXES);
        crs = parse(GeographicCRS.class, wkt);
        assertNameAndIdentifierEqual("NTF (Paris)", 0, crs);
        pm = verifyNTF(crs.getDatum(), false);
        assertEquals("angularUnit", NonSI.DEGREE_ANGLE, pm.getAngularUnit());
        assertEquals("greenwichLongitude", 2.33722917, pm.getGreenwichLongitude(), STRICT);
        cs = crs.getCoordinateSystem();
        assertEquals("dimension", 2, cs.getDimension());
        assertAxisEquals(AxisNames.GEODETIC_LONGITUDE, "λ", AxisDirection.EAST,  -200, +200, NonSI.GRADE, RangeMeaning.WRAPAROUND, cs.getAxis(0));
        assertAxisEquals(AxisNames.GEODETIC_LATITUDE,  "φ", AxisDirection.NORTH, -100, +100, NonSI.GRADE, RangeMeaning.EXACT,      cs.getAxis(1));
    }

    /**
     * Tests parsing of a CRS with a prime meridian having implicit unit in grads but axes having explicit unit
     * in degrees. The specification in §8.2.2 (ii) said:
     *
     *     "(snip) the prime meridian’s {@literal <irm longitude>} value shall be given in
     *     the same angular units as those for the horizontal axes of the geographic CRS."
     *
     * Consequently we expect the prime meridian to be in decimal degrees even if the WKT used in this test has
     * an {@code Unit[“grade”, 0.015707963267948967]} element, because this WK also declare the axis as being in
     * degrees. Since this can be confusing, we expect the parser to emit a warning.
     *
     * @throws ParseException if the parsing failed.
     */
    @Test
    @DependsOnMethod("testGeographicWithParisMeridian")
    public void testMismatchedAngularUnits() throws ParseException {
        String wkt = "GeodeticCRS[“NTF (Paris)”,\n" +
                     "  Datum[“Nouvelle Triangulation Française (Paris)”,\n" +
                     "    Ellipsoid[“Clarke 1880 (IGN)”, 6378249.2, 293.4660212936269]],\n" +
                     "    PrimeMeridian[“Paris”, 2.33722917],\n" +              // In units of the longitude axis.
                     "  CS[ellipsoidal, 2],\n" +
                     "    Axis[“Latitude (φ)”, NORTH, Unit[“degree”, 0.017453292519943295]],\n" +
                     "    Axis[“Longitude (λ)”, EAST, Unit[“degree”, 0.017453292519943295]],\n" +
                     "    Unit[“grade”, 0.015707963267948967]\n," +             // Inconsistent with axis units.
                     "  Id[“EPSG”, 4807]]";

        GeographicCRS crs = parseIgnoreWarnings(GeographicCRS.class, wkt);
        final Warnings warnings = parser.getAndClearWarnings(crs);
        assertTrue("ignoredElements", parser.ignoredElements.isEmpty());
        assertNotNull("warnings", warnings);
        assertEquals("warnings.numMessages", 1, warnings.getNumMessages());

        assertNameAndIdentifierEqual("NTF (Paris)", 4807, crs);
        PrimeMeridian pm = crs.getDatum().getPrimeMeridian();
        assertEquals("angularUnit", NonSI.DEGREE_ANGLE, pm.getAngularUnit());
        assertEquals("greenwichLongitude", 2.33722917, pm.getGreenwichLongitude(), STRICT);
        EllipsoidalCS cs = crs.getCoordinateSystem();
        assertEquals("dimension", 2, cs.getDimension());
        assertAxisEquals(AxisNames.GEODETIC_LATITUDE,  "φ", AxisDirection.NORTH,  -90,  +90, NonSI.DEGREE_ANGLE, RangeMeaning.EXACT,      cs.getAxis(0));
        assertAxisEquals(AxisNames.GEODETIC_LONGITUDE, "λ", AxisDirection.EAST,  -180, +180, NonSI.DEGREE_ANGLE, RangeMeaning.WRAPAROUND, cs.getAxis(1));
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
     * Verifies that the axes of the coordinate system are those of a projected CRS,
     * with (East, North) axis directions.
     */
    private static void verifyProjectedCS(final CartesianCS cs, final Unit<Length> unit) {
        assertEquals("dimension", 2, cs.getDimension());
        assertUnboundedAxisEquals("Easting",  "E", AxisDirection.EAST,  unit, cs.getAxis(0));
        assertUnboundedAxisEquals("Northing", "N", AxisDirection.NORTH, unit, cs.getAxis(1));
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
     * Tests the parsing of a projected CRS from a WKT 1 string with authority and Bursa-Wolf parameters.
     *
     * @throws ParseException if the parsing failed.
     */
    @Test
    @DependsOnMethod("testProjectedCRS")
    public void testProjectedWithID() throws ParseException {
        final ProjectedCRS crs = parse(ProjectedCRS.class,
               "PROJCS[“OSGB 1936 / British National Grid”,\n" +
               "  GEOGCS[“OSGB 1936”,\n" +
               "    DATUM[“OSGB_1936”,\n" +
               "      SPHEROID[“Airy 1830”, 6377563.396, 299.3249646, AUTHORITY[“EPSG”, “7001”]],\n" +
               "      TOWGS84[375.0, -111.0, 431.0, 0.0, 0.0, 0.0, 0.0],\n" +
               "      AUTHORITY[“EPSG”, “6277”]],\n" +
               "      PRIMEM[“Greenwich”,0.0, AUTHORITY[“EPSG”, “8901”]],\n" +
               "    UNIT[“DMSH”,0.0174532925199433],\n" +
               "    AXIS[“Lat”,NORTH],AXIS[“Long”,EAST], AUTHORITY[“EPSG”, “4277”]],\n" +
               "  PROJECTION[“Transverse_Mercator”],\n" +
               "  PARAMETER[“latitude_of_origin”, 49.0],\n" +
               "  PARAMETER[“central_meridian”, -2.0],\n" +
               "  PARAMETER[“scale_factor”, 0.999601272],\n" +
               "  PARAMETER[“false_easting”, 400000.0],\n" +
               "  PARAMETER[“false_northing”, -100000.0],\n" +
               "  UNIT[“metre”, 1.0, AUTHORITY[“EPSG”, “9001”]],\n" +
               "  AXIS[“E”,EAST],\n" +
               "  AXIS[“N”,NORTH],\n" +
               "  AUTHORITY[“EPSG”, “27700”]]");

        assertNameAndIdentifierEqual("OSGB 1936 / British National Grid", 27700, crs);
        assertNameAndIdentifierEqual("OSGB 1936", 4277, crs.getBaseCRS());
        assertNameAndIdentifierEqual("OSGB_1936", 6277, crs.getDatum());
        verifyProjectedCS(crs.getCoordinateSystem(), SI.METRE);

        final ParameterValueGroup param = crs.getConversionFromBase().getParameterValues();
        assertEquals("Transverse Mercator", crs.getConversionFromBase().getMethod().getName().getCode());
        assertEquals("semi_major",   6377563.396, param.parameter("semi_major"        ).doubleValue(), 1E-4);
        assertEquals("semi_minor",   6356256.909, param.parameter("semi_minor"        ).doubleValue(), 1E-3);
        assertEquals("latitude_of_origin",  49.0, param.parameter("latitude_of_origin").doubleValue(), 1E-8);
        assertEquals("central_meridian",    -2.0, param.parameter("central_meridian"  ).doubleValue(), 1E-8);
        assertEquals("scale_factor",      0.9996, param.parameter("scale_factor"      ).doubleValue(), 1E-5);
        assertEquals("false_easting",   400000.0, param.parameter("false_easting"     ).doubleValue(), 1E-4);
        assertEquals("false_northing", -100000.0, param.parameter("false_northing"    ).doubleValue(), 1E-4);

        final BursaWolfParameters[] bwp = ((DefaultGeodeticDatum) crs.getDatum()).getBursaWolfParameters();
        assertEquals("BursaWolfParameters", 1, bwp.length);
        assertArrayEquals("BursaWolfParameters", new double[] {375, -111, 431}, bwp[0].getValues(), STRICT);
    }

    /**
     * Tests the parsing of a projected CRS with feet units.
     *
     * @throws ParseException if the parsing failed.
     */
    @Test
    @DependsOnMethod("testProjectedCRS")
    public void testProjectedWithFeetUnits() throws ParseException {
        final ProjectedCRS crs = parse(ProjectedCRS.class,
               "PROJCS[“TransverseMercator”,\n" +
               "  GEOGCS[“Sphere”,\n" +
               "    DATUM[“Sphere”,\n" +
               "      SPHEROID[“Sphere”, 6370997.0, 0.0],\n" +
               "      TOWGS84[0, 0, 0, 0, 0, 0, 0]],\n" +
               "      PRIMEM[“Greenwich”, 0.0],\n" +
               "    UNIT[“degree”, 0.017453292519943295],\n" +
               "    AXIS[“Longitude”, EAST],\n" +
               "    AXIS[“Latitude”, NORTH]],\n" +
               "  PROJECTION[“Transverse_Mercator”,\n" +
               "    AUTHORITY[“OGC”, “Transverse_Mercator”]],\n" +
               "  PARAMETER[“central_meridian”, 170.0],\n" +
               "  PARAMETER[“latitude_of_origin”, 50.0],\n" +
               "  PARAMETER[“scale_factor”, 0.95],\n" +
               "  PARAMETER[“false_easting”, 0.0],\n" +
               "  PARAMETER[“false_northing”, 0.0],\n" +
               "  UNIT[“feet”, 0.304800609601219],\n" +
               "  AXIS[“E”, EAST],\n" +
               "  AXIS[“N”, NORTH]]");

        assertNameAndIdentifierEqual("TransverseMercator", 0, crs);
        assertNameAndIdentifierEqual("Sphere", 0, crs.getBaseCRS());
        assertNameAndIdentifierEqual("Sphere", 0, crs.getDatum());
        verifyProjectedCS(crs.getCoordinateSystem(), NonSI.FOOT_SURVEY_US);

        final ParameterValueGroup param = crs.getConversionFromBase().getParameterValues();
        assertEquals("Transverse Mercator", crs.getConversionFromBase().getMethod().getName().getCode());
        assertEquals("semi_major",     6370997.0, param.parameter("semi_major"        ).doubleValue(), 1E-5);
        assertEquals("semi_minor",     6370997.0, param.parameter("semi_minor"        ).doubleValue(), 1E-5);
        assertEquals("latitude_of_origin",  50.0, param.parameter("latitude_of_origin").doubleValue(), 1E-8);
        assertEquals("central_meridian",   170.0, param.parameter("central_meridian"  ).doubleValue(), 1E-8);
        assertEquals("scale_factor",        0.95, param.parameter("scale_factor"      ).doubleValue(), 1E-8);
        assertEquals("false_easting",        0.0, param.parameter("false_easting"     ).doubleValue(), 1E-8);
        assertEquals("false_northing",       0.0, param.parameter("false_northing"    ).doubleValue(), 1E-8);
    }

    /**
     * Tests the parsing of a projected CRS using angular values in grades instead than degrees
     * and in lengths in kilometres instead than metres.
     *
     * @throws ParseException if the parsing failed.
     */
    @Test
    @DependsOnMethod({"testGeographicWithParisMeridian", "testProjectedCRS"})
    public void testProjectedWithGradUnits() throws ParseException {
        String wkt = "PROJCS[“NTF (Paris) / Lambert zone II”," +
                     "  GEOGCS[“NTF (Paris)”," +
                     "    DATUM[“Nouvelle Triangulation Française (Paris)”," +
                     "      SPHEROID[“Clarke 1880 (IGN)”, 6378249.2, 293.4660212936269]," +
                     "      TOWGS84[-168,-60,320,0,0,0,0]]," +
                     "    PRIMEM[“Paris”, 2.5969213, AUTHORITY[“EPSG”, “8903”]]," +  // In grades.
                     "    UNIT[“grad”, 0.01570796326794897]]," +
                     "  PROJECTION[“Lambert Conformal Conic (1SP)”]," +  // Intentional swapping of "Conformal" and "Conic".
                     "  PARAMETER[“latitude_of_origin”, 52.0]," +        // In grades.
                     "  PARAMETER[“scale_factor”, 0.99987742]," +
                     "  PARAMETER[“false_easting”, 600.0]," +
                     "  PARAMETER[“false_northing”, 2200.0]," +
                     "  UNIT[“km”,1000]]";

        validateParisFranceII(parse(ProjectedCRS.class, wkt), 0, true);
        /*
         * Parse again using Convention.WKT1_COMMON_UNITS and ignoring AXIS[…] elements.
         * See the comment in 'testGeographicWithParisMeridian' method for a discussion.
         * The new aspect tested by this method is that the unit should be ignored
         * for the parameters in addition to the prime meridian.
         */
        wkt = wkt.replace("2.5969213", "2.33722917");       // Convert unit in prime meridian.
        wkt = wkt.replace("52.0",      "46.8");             // Convert unit in “latitude_of_origin” parameter.
        wkt = wkt.replace("600.0",     "600000");           // Convert unit in “false_easting” parameter.
        wkt = wkt.replace("2200.0",    "2200000");          // Convert unit in “false_northing” parameter.
        newParser(Convention.WKT1_IGNORE_AXES);
        final ProjectedCRS crs = parse(ProjectedCRS.class, wkt);
        assertNameAndIdentifierEqual("NTF (Paris) / Lambert zone II", 0, crs);
        verifyProjectedCS(crs.getCoordinateSystem(), SI.KILOMETRE);
        final PrimeMeridian pm = verifyNTF(crs.getDatum(), true);
        assertEquals("angularUnit", NonSI.DEGREE_ANGLE, pm.getAngularUnit());
        assertEquals("greenwichLongitude", 2.33722917, pm.getGreenwichLongitude(), STRICT);
        final ParameterValue<?> param = verifyNTF(crs.getConversionFromBase().getParameterValues());
        assertEquals("angularUnit", NonSI.DEGREE_ANGLE, param.getUnit());
        assertEquals("latitude_of_origin",  46.8, param.doubleValue(), STRICT);
    }

    /**
     * Tests the same CRS than {@link #testProjectedWithGradUnits()}, but from a WKT 2 definition
     * (except for inclusion of accented characters).
     *
     * @throws ParseException if the parsing failed.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-309">SIS-309</a>
     * @see <a href="https://issues.apache.org/jira/browse/SIS-310">SIS-310</a>
     */
    @Test
    @DependsOnMethod("testProjectedWithGradUnits")
    public void testProjectedFromWKT2() throws ParseException {
        String wkt = "ProjectedCRS[“NTF (Paris) / Lambert zone II”,\n" +
                     "  BaseGeodCRS[“NTF (Paris)”,\n" +
                     "    Datum[“Nouvelle Triangulation Française (Paris)”,\n" +
                     "      Ellipsoid[“Clarke 1880 (IGN)”, 6378249.2, 293.4660212936269]],\n" +
                     "      PrimeMeridian[“Paris”, 2.5969213, Unit[“grade”, 0.015707963267948967], Id[“EPSG”, 8903]],\n" +
                     "    AngleUnit[“degree”, 0.017453292519943295]],\n" +
                     "  Conversion[“Lambert zone II”,\n" +
                     "    Method[“Lambert Conic Conformal (1SP)”],\n" +
                     "    Parameter[“Latitude of natural origin”, 52.0, AngleUnit[“grade”, 0.015707963267948967]],\n" +
                     "    Parameter[“Longitude of natural origin”, 0.0],\n" +
                     "    Parameter[“Scale factor at natural origin”, 0.99987742],\n" +
                     "    Parameter[“False easting”, 600.0],\n" +
                     "    Parameter[“False northing”, 2200.0]],\n" +
                     "  CS[Cartesian, 2],\n" +
                     "    Axis[“Easting (E)”, east],\n" +
                     "    Axis[“Northing (N)”, north],\n" +
                     "    LengthUnit[“km”, 1000],\n" +
                     "  Scope[“Large and medium scale topographic mapping and engineering survey.”],\n" +
                     "  Id[“EPSG”, 27572, URI[“urn:ogc:def:crs:EPSG::27572”]]]";

        final ProjectedCRS crs = parse(ProjectedCRS.class, wkt);
        validateParisFranceII(crs, 27572, false);
        assertNull("Identifier shall not have a version.", getSingleton(crs.getIdentifiers()).getVersion());
    }

    /**
     * Verifies the parameters of a “NTF (Paris) / Lambert zone II” projection.
     */
    private static void validateParisFranceII(final ProjectedCRS crs, final int identifier, final boolean hasToWGS84) {
        assertNameAndIdentifierEqual("NTF (Paris) / Lambert zone II", identifier, crs);
        verifyProjectedCS(crs.getCoordinateSystem(), SI.KILOMETRE);
        final PrimeMeridian pm = verifyNTF(crs.getDatum(), hasToWGS84);
        assertEquals("angularUnit", NonSI.GRADE, pm.getAngularUnit());
        assertEquals("greenwichLongitude", 2.5969213, pm.getGreenwichLongitude(), STRICT);
        final ParameterValue<?> param = verifyNTF(crs.getConversionFromBase().getParameterValues());
        assertEquals("angularUnit", NonSI.GRADE, param.getUnit());
        assertEquals("latitude_of_origin",  52.0, param.doubleValue(), STRICT);
    }

    /**
     * Verifies the properties of a datum which is expected to be “Nouvelle Triangulation Française (Paris)”.
     * This is used by the methods in this class which test a CRS using less frequently used units and prime
     * meridian.
     *
     * @param  datum The datum to verify.
     * @param  hasToWGS84 Whether the datum is expected to have a {@code TOWGS84[…]} element.
     * @return The prime meridian, to be verified by the caller because the unit of measurement depends on the test.
     */
    private static PrimeMeridian verifyNTF(final GeodeticDatum datum, final boolean hasToWGS84) {
        assertNameAndIdentifierEqual("Nouvelle Triangulation Française (Paris)", 0, datum);

        final BursaWolfParameters[] bwp = ((DefaultGeodeticDatum) datum).getBursaWolfParameters();
        assertEquals("BursaWolfParameters", hasToWGS84 ? 1 : 0, bwp.length);
        if (hasToWGS84) {
            assertArrayEquals("BursaWolfParameters", new double[] {-168, -60, 320}, bwp[0].getValues(), STRICT);
        }

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
    public void testProjectedWithMissingName() throws ParseException {
        final ProjectedCRS crs = parse(ProjectedCRS.class,
                "PROJCS[“FRANCE/NTF/Lambert III”," +
                "GEOGCS[“”," + // Missing name (the purpose of this test).
                "DATUM[“NTF=GR3DF97A”,TOWGS84[-168, -60, 320] ," + // Intentionally misplaced coma.
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
        assertArrayEquals("BursaWolfParameters", new double[] {-168, -60, 320},
                ((DefaultGeodeticDatum) datum).getBursaWolfParameters()[0].getValues(), STRICT);

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

    /**
     * Parses a test CRS north or south oriented.
     * If the CRS is fully south-oriented with 0.0 northing, then it should be the EPSG:22285 one.
     */
    private ProjectedCRS parseTransverseMercator(final boolean methodSouth,
            final boolean axisSouth, final double northing) throws ParseException
    {
        final String method = methodSouth ? "Transverse Mercator (South Orientated)" : "Transverse Mercator";
        final String axis = axisSouth ? "“Southing”, SOUTH" : "“Northing”, NORTH";
        return parse(ProjectedCRS.class,
                "PROJCS[“South African Coordinate System zone 25”, " +
                  "GEOGCS[“Cape”, " +
                    "DATUM[“Cape”, " +
                      "SPHEROID[“Clarke 1880 (Arc)”, 6378249.145, 293.4663077, AUTHORITY[“EPSG”,“7013”]], " +
                      "TOWGS84[-136.0, -108.0, -292.0], " +
                      "AUTHORITY[“EPSG”,“6222”]], " +
                    "PRIMEM[“Greenwich”, 0.0, AUTHORITY[“EPSG”,“8901”]], " +
                    "UNIT[“degree”, 0.017453292519943295], " +
                    "AXIS[“Geodetic latitude”, NORTH], " +
                    "AXIS[“Geodetic longitude”, EAST], " +
                    "AUTHORITY[“EPSG”,“4222”]], " +
                  "PROJECTION[“" + method + "”], " +
                  "PARAMETER[“central_meridian”, 25.0], " +
                  "PARAMETER[“latitude_of_origin”, 0.0], " +
                  "PARAMETER[“scale_factor”, 1.0], " +
                  "PARAMETER[“false_easting”, 0.0], " +
                  "PARAMETER[“false_northing”, " + northing + "], " +
                  "UNIT[“m”, 1.0], " +
                  "AXIS[“Westing”, WEST], " +
                  "AXIS[" + axis + "]]");
    }

    /**
     * Returns the conversion from {@code north} to {@code south}.
     */
    private static Matrix conversion(final ProjectedCRS north, final ProjectedCRS south)
            throws NoninvertibleTransformException
    {
        final MathTransform transform = MathTransforms.concatenate(
                north.getConversionFromBase().getMathTransform().inverse(),
                south.getConversionFromBase().getMathTransform());
        assertInstanceOf("North to South", LinearTransform.class, transform);
        return ((LinearTransform) transform).getMatrix();
    }

    /**
     * Tests the {@link MathTransform} between North-Orientated and South-Orientated cases.
     *
     * @throws ParseException if the parsing failed.
     * @throws NoninvertibleTransformException if computation of the conversion from North-Orientated
     *         to South-Orientated failed.
     */
    @Test
    @DependsOnMethod("testProjectedCRS")
    public void testMathTransform() throws ParseException, NoninvertibleTransformException {
        /*
         * Test "Transverse Mercator" (not south-oriented) with an axis oriented toward south.
         * The 'south' transform is actually the usual Transverse Mercator projection, despite
         * having axis oriented toward South.  Consequently the "False Northing" parameter has
         * the same meaning for those two CRS. Since we assigned the same False Northing value,
         * those two CRS have their "False origin" at the same location. This is why conversion
         * from 'south' to 'north' introduce no translation, only a reversal of y axis.
         */
        ProjectedCRS north = parseTransverseMercator(false, false, 1000);
        assertEquals(AxisDirection.WEST,  north.getCoordinateSystem().getAxis(0).getDirection());
        assertEquals(AxisDirection.NORTH, north.getCoordinateSystem().getAxis(1).getDirection());

        ProjectedCRS south = parseTransverseMercator(false, true, 1000);
        assertEquals(AxisDirection.WEST,  south.getCoordinateSystem().getAxis(0).getDirection());
        assertEquals(AxisDirection.SOUTH, south.getCoordinateSystem().getAxis(1).getDirection());

        Matrix matrix = conversion(north, south);
        assertEquals("West direction should be unchanged. ",      +1, matrix.getElement(0,0), STRICT);
        assertEquals("North-South direction should be reverted.", -1, matrix.getElement(1,1), STRICT);
        assertEquals("No easting expected.",                       0, matrix.getElement(0,2), STRICT);
        assertEquals("No northing expected.",                      0, matrix.getElement(1,2), STRICT);
        assertDiagonalEquals(new double[] {+1, -1, 1}, true, matrix, STRICT);
        /*
         * Test "Transverse Mercator South Orientated". In this projection, the "False Northing" parameter
         * is actually a "False Southing". It may sound surprising, but "South Orientated" projections are
         * defined that way.  For converting from our CRS having a False Northing of 1000 to a CRS without
         * False Northing or Southing, we must subtract 1000 from the axis which is oriented toward North.
         * This means adding 1000 if the axis is rather oriented toward South. Then we add another 1000 m
         * (the value specified in the line just below) toward South.
         */
        south = parseTransverseMercator(true, true, 1000);  // "False Southing" of 1000 metres.
        assertEquals(AxisDirection.WEST,  south.getCoordinateSystem().getAxis(0).getDirection());
        assertEquals(AxisDirection.SOUTH, south.getCoordinateSystem().getAxis(1).getDirection());
        matrix = conversion(north, south);
        assertEquals("West direction should be unchanged. ",      +1, matrix.getElement(0,0), STRICT);
        assertEquals("North-South direction should be reverted.", -1, matrix.getElement(1,1), STRICT);
        assertEquals("No easting expected.",                       0, matrix.getElement(0,2), STRICT);
        assertEquals("Northing expected.",                      2000, matrix.getElement(1,2), STRICT);
    }

    /**
     * Tests the parsing of an engineering CRS from a WKT 2 string.
     *
     * @throws ParseException if the parsing failed.
     */
    @Test
    public void testEngineeringCRS() throws ParseException {
        final EngineeringCRS crs = parse(EngineeringCRS.class,
                "EngineeringCRS[“A building-centred CRS”,\n" +
                "  EngineeringDatum[“Building reference point”],\n" +
                "  CS[Cartesian, 3],\n" +
                "    Axis[“x”, east],\n" +
                "    Axis[“y”, north],\n" +
                "    Axis[“z”, up],\n" +
                "    Unit[“metre”, 1]]");

        assertNameAndIdentifierEqual("A building-centred CRS", 0, crs);
        assertNameAndIdentifierEqual("Building reference point", 0, crs.getDatum());
        final CoordinateSystem cs = crs.getCoordinateSystem();
        assertEquals("dimension", 3, cs.getDimension());

        // Axis names are arbitrary and could change in future SIS versions.
        assertUnboundedAxisEquals("Easting",  "x", AxisDirection.EAST,  SI.METRE, cs.getAxis(0));
        assertUnboundedAxisEquals("Northing", "y", AxisDirection.NORTH, SI.METRE, cs.getAxis(1));
        assertUnboundedAxisEquals("z",        "z", AxisDirection.UP,    SI.METRE, cs.getAxis(2));
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
     * Tests the parsing of a compound CRS from a WKT 1 string with ellipsoidal height and its conversion
     * to a three-dimensional geographic CRS.
     *
     * @throws ParseException if the parsing failed.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-317">SIS-317</a>
     *
     * @since 0.7
     */
    @Test
    @DependsOnMethod("testCompoundCRS")
    public void testCompoundWKT1() throws ParseException {
        final GeographicCRS crs = parse(GeographicCRS.class,
                "COMPD_CS[“WGS 84 (3D)”,\n" +
                "  GEOGCS[“WGS 84”,\n" +
                "    DATUM[“World Geodetic System 1984”,\n" +
                "      SPHEROID[“WGS84”, 6378137.0, 298.257223563]],\n" +
                "      PRIMEM[“Greenwich”, 0.0],\n" +
                "    UNIT[“degree”, 0.017453292519943295],\n" +
                "    AXIS[“Longitude”, EAST],\n" +
                "    AXIS[“Latitude”, NORTH]],\n" +
                "  VERT_CS[“Ellipsoidal height”,\n" +
                "    VERT_DATUM[“Ellipsoid”, 2002],\n" +
                "    UNIT[“metre”, 1],\n" +
                "    AXIS[“Ellipsoidal height”, UP]]]");

        final CoordinateSystem cs = crs.getCoordinateSystem();
        assertEquals("dimension", 3, cs.getDimension());
        assertLongitudeAxisEquals(cs.getAxis(0));
        assertLatitudeAxisEquals (cs.getAxis(1));
        assertUnboundedAxisEquals("Ellipsoidal height", "h", AxisDirection.UP, SI.METRE, cs.getAxis(2));
    }

    /**
     * Ensures that parsing a WKT with wrong units throws an exception.
     */
    @Test
    public void testIncompatibleUnits() {
        try {
            parse(GeographicCRS.class,
                    "GEOGCS[“NAD83”,\n" +
                    "  DATUM[“North American Datum 1983”,\n" +
                    "    SPHEROID[“GRS 1980”, 6378137.0, 298.257222]],\n" +
                    "  PRIMEM[“Greenwich”, 0],\n" +
                    "  UNIT[“km”, 1000]]");                                             // Wrong unit
            fail("Should not have parsed a CRS with wrong unit of measurement.");
        } catch (ParseException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("km"));
        }
    }

    /**
     * Tests the production of a warning messages when the WKT contains unknown elements.
     *
     * @throws ParseException if the parsing failed.
     */
    @Test
    @DependsOnMethod("testGeographicWithImplicitAxes")
    public void testWarnings() throws ParseException {
        final GeographicCRS crs = parseIgnoreWarnings(GeographicCRS.class,
               "GEOGCS[“WGS 84”,\n" +
               "  DATUM[“World Geodetic System 1984”,\n" +
               "    SPHEROID[“WGS84”, 6378137.0, 298.257223563, Ext1[“foo”], Ext2[“bla”]]],\n" +
               "    PRIMEM[“Greenwich”, 0.0, Intruder[“unknown”], UNIT[“degree”, 0.01745]],\n" +    // Truncated scale factor.
               "  UNIT[“degree”, 0.017453292519943295], Intruder[“foo”]]");

        verifyGeographicCRS(0, crs);
        final Warnings warnings = parser.getAndClearWarnings(crs);
        assertNotNull("warnings", warnings);

        assertTrue("warnings.getExceptions()",
                warnings.getExceptions().isEmpty());

        assertEquals("warnings.getRootElement()", "WGS 84",
                warnings.getRootElement());

        assertArrayEquals("warnings.getUnknownElements()",
                new String[] {"Intruder", "Ext1", "Ext2"},
                warnings.getUnknownElements().toArray());

        assertArrayEquals("warnings.getUnknownElementLocations(…)",
                new String[] {"PRIMEM", "GEOGCS"},
                warnings.getUnknownElementLocations("Intruder").toArray());

        assertArrayEquals("warnings.getUnknownElementLocations(…)",
                new String[] {"SPHEROID"},
                warnings.getUnknownElementLocations("Ext1").toArray());

        assertArrayEquals("warnings.getUnknownElementLocations(…)",
                new String[] {"SPHEROID"},
                warnings.getUnknownElementLocations("Ext2").toArray());

        assertMultilinesEquals("Parsing of “WGS 84” done, but some elements were ignored.\n" +
                               " • Unexpected scale factor 0.017 for unit of measurement “°”.\n" +
                               " • The text contains unknown elements:\n" +
                               "    ‣ “Intruder” in PRIMEM, GEOGCS.\n" +
                               "    ‣ “Ext1” in SPHEROID.\n" +
                               "    ‣ “Ext2” in SPHEROID.", warnings.toString(Locale.US));

        assertMultilinesEquals("La lecture de « WGS 84 » a été faite, mais en ignorant certains éléments.\n" +
                               " • Le facteur d’échelle 0,017 est inattendu pour l’unité de mesure « ° ».\n" +
                               " • Le texte contient des éléments inconnus :\n" +
                               "    ‣ « Intruder » dans PRIMEM, GEOGCS.\n" +
                               "    ‣ « Ext1 » dans SPHEROID.\n" +
                               "    ‣ « Ext2 » dans SPHEROID.", warnings.toString(Locale.FRANCE));
    }
}
