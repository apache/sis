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
package org.apache.sis.referencing;

import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.time.Instant;
import java.text.SimpleDateFormat;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.datum.TemporalDatum;
import org.opengis.referencing.datum.VerticalDatum;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.internal.shared.AxisNames;
import org.apache.sis.referencing.internal.VerticalDatumTypes;
import org.apache.sis.util.internal.shared.Constants;
import static org.apache.sis.util.internal.shared.Constants.UTC;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.apache.sis.test.TestCase;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertEqualsIgnoreMetadata;
import static org.apache.sis.test.Assertions.assertMessageContains;
import static org.apache.sis.test.Assertions.assertSingleton;

// Specific to the main and geoapi-3.1 branches:
import org.apache.sis.temporal.TemporalDate;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.datum.RealizationMethod;
import org.opengis.test.Validators;
import static org.opengis.test.Assertions.assertAxisDirectionsEqual;


/**
 * Tests the {@link CommonCRS} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
@SuppressWarnings("exports")
public final class CommonCRSTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public CommonCRSTest() {
    }

    /**
     * Verifies that the same EPSG code is not used for two objects. Collisions are not allowed between
     * {@code geographic}, {@code geocentric}, {@code geo3D} and all UTM projections. Strictly speaking
     * all the above-cited codes may collide with {@code datum} and {@code ellipsoid}, but we nevertheless
     * avoid those collisions in order to simplify {@link EPSGFactoryFallback#createObject(String)} implementation.
     */
    @Test
    public void ensureNoCodeCollision() {
        final Map<Integer,Enum<?>> codes = new HashMap<>();
        final CommonCRS[] values = CommonCRS.values();
        for (final CommonCRS crs : values) {
            assertNoCodeCollision(codes, crs, crs.geographic);
            assertNoCodeCollision(codes, crs, crs.geocentric);
            assertNoCodeCollision(codes, crs, crs.geo3D);
            assertNoCodeCollision(codes, crs, crs.northUPS);
            assertNoCodeCollision(codes, crs, crs.southUPS);
            for (int zone = crs.firstZone; zone <= crs.lastZone; zone++) {
                if (crs.northUTM != 0) assertNoCodeCollision(codes, crs, crs.northUTM + zone);
                if (crs.southUTM != 0) assertNoCodeCollision(codes, crs, crs.southUTM + zone);
            }
        }
        final CommonCRS.Vertical[] vertical = CommonCRS.Vertical.values();
        for (final CommonCRS.Vertical crs : vertical) {
            if (crs.isEPSG) {
                assertNoCodeCollision(codes, crs, crs.crs);
            }
        }
        /*
         * Following restrictions are not strictly required, but their enforcement
         * simplifies the EPSGFactoryFallback.createObject(String) implementation.
         */
        assertNull(codes.put(Integer.valueOf(StandardDefinitions.GREENWICH), CommonCRS.WGS84));
        for (final CommonCRS crs : values) assertNull(codes.get(Integer.valueOf(crs.ellipsoid)), crs.name());
        for (final CommonCRS crs : values) assertNotSame(crs, codes.put(Integer.valueOf(crs.ellipsoid), crs), crs.name());
        for (final CommonCRS crs : values) assertNull(codes.get(Integer.valueOf(crs.datum)), crs.name());
        for (final CommonCRS.Vertical crs : vertical) {
            if (crs.isEPSG) {
                assertNull(codes.get(Integer.valueOf(crs.datum)), crs.name());
            }
        }
    }

    /**
     * Helper method for {@link #ensureNoCodeCollision()} only.
     */
    private static void assertNoCodeCollision(final Map<Integer,Enum<?>> codes, final Enum<?> crs, final int n) {
        if (n != 0) {
            final Enum<?> existing = codes.put(n, crs);
            if (existing != null) {
                fail(existing + " and " + crs + " both use the same EPSG:" + n + " code.");
            }
        }
    }

    /**
     * Tests the {@link CommonCRS#geographic()} method.
     */
    @Test
    public void testGeographic() {
        final GeographicCRS geographic = CommonCRS.WGS84.geographic();
        Validators.validate(geographic);
        GeodeticObjectVerifier.assertIsWGS84(geographic, true, true);
        assertSame(geographic, CommonCRS.WGS84.geographic(), "Cached value");
    }

    /**
     * Tests the {@link CommonCRS#normalizedGeographic()} method.
     */
    @Test
    public void testNormalizedGeographic() {
        final GeographicCRS geographic = CommonCRS.WGS84.geographic();
        final GeographicCRS normalized = CommonCRS.WGS84.normalizedGeographic();
        Validators.validate(normalized);
        assertSame(geographic.getDatum(), normalized.getDatum());
        /*
         * Compare axes. Note that axes in different order have different EPSG codes.
         */
        final CoordinateSystem φλ = geographic.getCoordinateSystem();
        final CoordinateSystem λφ = normalized.getCoordinateSystem();
        assertEqualsIgnoreMetadata(φλ.getAxis(1), λφ.getAxis(0));       // Longitude
        assertEqualsIgnoreMetadata(φλ.getAxis(0), λφ.getAxis(1));       // Latitude
        assertSame(normalized, CommonCRS.WGS84.normalizedGeographic(), "Cached value");
    }

    /**
     * Tests the {@link CommonCRS#geographic3D()} method.
     */
    @Test
    public void testGeographic3D() {
        final GeographicCRS crs = CommonCRS.WGS72.geographic3D();
        Validators.validate(crs);
        assertEquals ("WGS 72", crs.getName().getCode());
        assertSame   (CommonCRS.WGS72.geographic().getDatum(), crs.getDatum());
        assertNotSame(CommonCRS.WGS84.geographic().getDatum(), crs.getDatum());

        final EllipsoidalCS cs = crs.getCoordinateSystem();
        final String name = cs.getName().getCode();
        assertTrue(name.startsWith("Ellipsoidal 3D"), name);
        assertEquals(3, cs.getDimension(), "dimension");
        assertAxisDirectionsEqual(cs, AxisDirection.NORTH, AxisDirection.EAST, AxisDirection.UP);
        assertSame(crs, CommonCRS.WGS72.geographic3D(), "Cached value");
    }

    /**
     * Tests the {@link CommonCRS#geocentric()} method.
     */
    @Test
    public void testGeocentric() {
        final GeodeticCRS crs = CommonCRS.WGS72.geocentric();
        Validators.validate(crs);
        assertEquals ("WGS 72", crs.getName().getCode());
        assertSame   (CommonCRS.WGS72.geographic().getDatum(), crs.getDatum());
        assertNotSame(CommonCRS.WGS84.geographic().getDatum(), crs.getDatum());

        final CoordinateSystem cs = crs.getCoordinateSystem();
        final String name = cs.getName().getCode();
        assertTrue(name.startsWith("Cartesian 3D"), name);
        assertEquals(3, cs.getDimension(), "dimension");
        assertAxisDirectionsEqual(cs, AxisDirection.GEOCENTRIC_X, AxisDirection.GEOCENTRIC_Y, AxisDirection.GEOCENTRIC_Z);
        assertSame(crs, CommonCRS.WGS72.geocentric(), "Cached value");
    }

    /**
     * Tests the {@link CommonCRS#spherical()} method.
     */
    @Test
    public void testSpherical() {
        final GeodeticCRS crs = CommonCRS.ETRS89.spherical();
        Validators.validate(crs);
        assertEquals ("ETRS89", crs.getName().getCode());
        assertSame   (CommonCRS.ETRS89.geographic().getDatum(), crs.getDatum());
        assertNotSame(CommonCRS.WGS84 .datum(true), crs.getDatum());

        final CoordinateSystem cs = crs.getCoordinateSystem();
        final String name = cs.getName().getCode();
        assertTrue(name.startsWith("Spherical"), name);
        assertEquals(3, cs.getDimension(), "dimension");
        assertAxisDirectionsEqual(cs, AxisDirection.NORTH, AxisDirection.EAST, AxisDirection.UP);
        assertSame(crs, CommonCRS.ETRS89.spherical(), "Cached value");
    }

    /**
     * Verifies the vertical datum enumeration.
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testVertical() {
        for (final CommonCRS.Vertical e : CommonCRS.Vertical.values()) {
            final RealizationMethod method;
            final String axisName, datumName;
            switch (e) {
                case NAVD88:         axisName = AxisNames.GRAVITY_RELATED_HEIGHT; datumName = "North American Vertical Datum 1988"; method = RealizationMethod. GEOID; break;
                case BAROMETRIC:     axisName = "Barometric altitude";            datumName = "Constant pressure surface";          method = RealizationMethod.valueOf("BAROMETRIC"); break;
                case MEAN_SEA_LEVEL: axisName = AxisNames.GRAVITY_RELATED_HEIGHT; datumName = "Mean Sea Level";                     method = RealizationMethod. TIDAL; break;
                case DEPTH:          axisName = AxisNames.DEPTH;                  datumName = "Mean Sea Level";                     method = RealizationMethod. TIDAL; break;
                case ELLIPSOIDAL:    axisName = AxisNames.ELLIPSOIDAL_HEIGHT;     datumName = "Ellipsoid";                          method = VerticalDatumTypes.ellipsoidal(); break;
                case OTHER_SURFACE:  axisName = "Height";                         datumName = "Other surface";                      method = null; break;
                default: throw new AssertionError(e);
            }
            final String        name  = e.name();
            final VerticalDatum datum = e.datum();
            final VerticalCRS   crs   = e.crs();
            if (e.isEPSG) {
                /*
                 * BAROMETRIC and ELLIPSOIDAL uses an axis named "Height", which is not a valid
                 * axis name according ISO 19111. We skip the validation test for those enums.
                 */
                Validators.validate(crs);
            }
            assertSame(datum, e.datum(), name);                         // Datum before CRS creation.
            assertSame(crs.getDatum(), e.datum(), name);                // Datum after CRS creation.
            assertEquals(datumName, datum.getName().getCode(), name);
            assertEquals(axisName,  crs.getCoordinateSystem().getAxis(0).getName().getCode(), name);
            if (!e.isEPSG) {  // Because the information is not in EPSG database 9.x.
                assertEquals(method, datum.getRealizationMethod().orElse(null), name);
            }
        }
    }

    /**
     * Verifies the epoch values of temporal enumeration compared to the Julian epoch.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Julian_day">Wikipedia: Julian day</a>
     */
    @Test
    public void testTemporal() {
        final var julianEpoch = TemporalDate.toInstant(CommonCRS.Temporal.JULIAN.datum().getOrigin());
        final double SECONDS_PER_DAY = Constants.SECONDS_PER_DAY;
        final double julianEpochSecond = julianEpoch.getEpochSecond() / SECONDS_PER_DAY;
        assertTrue(julianEpochSecond < 0);
        /*
         * We need to use `java.text.DateFormat` rather than `Instant.parse(String)` because
         * they have different policy regarding the calendar for dates before October 15, 1582.
         * The `java.time` classes use the proleptic Gregorian calendar while `java.text` uses
         * the proleptic Julian calendar. The latter is what we need for this test.
         */
        final var dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CANADA);
        dateFormat.setTimeZone(TimeZone.getTimeZone(UTC));
        dateFormat.setLenient(false);

        for (final CommonCRS.Temporal e : CommonCRS.Temporal.values()) {
            final String epoch;
            final double days;
            switch (e) {
                case JAVA:             // Fall through
                case UNIX:             epoch = "1970-01-01 00:00:00"; days = 2440587.5; break;
                case TRUNCATED_JULIAN: epoch = "1968-05-24 00:00:00"; days = 2440000.5; break;
                case DUBLIN_JULIAN:    epoch = "1899-12-31 12:00:00"; days = 2415020.0; break;
                case MODIFIED_JULIAN:  epoch = "1858-11-17 00:00:00"; days = 2400000.5; break;
                case JULIAN:           epoch = "4713-01-01 12:00:00"; days = 0;         break;
                case TROPICAL_YEAR:    epoch = "2000-01-01 00:00:00"; days = 2451544.5; break;
                default: throw new AssertionError(e);
            }
            final String        name   = e.name();
            final TemporalDatum datum  = e.datum();
            final TemporalCRS   crs    = e.crs();
            final Date          origin = datum.getOrigin();
            Validators.validate(crs);
            assertSame(datum, e.datum(), name);             // Datum before CRS creation.
            assertSame(crs.getDatum(), e.datum(), name);    // Datum after CRS creation.
            assertEquals(epoch, dateFormat.format(origin), name);
            assertEquals(days, origin.getTime() / (1000*SECONDS_PER_DAY) - julianEpochSecond, name);
            switch (e) {
                case JAVA: {
                    assertNameContains(datum, "Unix/POSIX");
                    assertNameContains(crs,   "Java");
                    break;
                }
                case UNIX: {
                    assertNameContains(datum, "Unix/POSIX");
                    assertNameContains(crs,   "Unix/POSIX");
                    break;
                }
            }
        }
    }

    /**
     * Verifies that the name of given object contains the given word.
     */
    private static void assertNameContains(final IdentifiedObject object, final String word) {
        final String name = object.getName().getCode();
        assertTrue(name.contains(word), name);
    }

    /**
     * Tests {@link CommonCRS.Temporal#forIdentifier(String, boolean)}.
     */
    @Test
    public void testTemporalForIdentifier() {
        assertSame(CommonCRS.Temporal.TRUNCATED_JULIAN, CommonCRS.Temporal.forIdentifier("TruncatedJulianDate", false));
        assertSame(CommonCRS.Temporal.TRUNCATED_JULIAN, CommonCRS.Temporal.forIdentifier("TruncatedJulianDate", true));
        assertSame(CommonCRS.Temporal.MODIFIED_JULIAN,  CommonCRS.Temporal.forIdentifier("ModifiedJulianDate",  false));
        var exception = assertThrows(IllegalArgumentException.class,
                () -> CommonCRS.Temporal.forIdentifier("ModifiedJulianDate", true),
                "Unexpected because not in OGC namespace.");
        assertMessageContains(exception, "ModifiedJulianDate", "OGC");
        assertEquals("OGC:TruncatedJulianDate", assertSingleton(CommonCRS.Temporal.TRUNCATED_JULIAN.crs().getIdentifiers()).toString());
        assertEquals("SIS:ModifiedJulianDate",  assertSingleton(CommonCRS.Temporal. MODIFIED_JULIAN.crs().getIdentifiers()).toString());
    }

    /**
     * Tests the URN lookup on temporal CRS.
     *
     * @throws FactoryException if a call to {@link IdentifiedObjects#lookupURN lookupURN(…)} failed.
     */
    @Test
    public void testLookupURN() throws FactoryException {
        final TemporalCRS crs = CommonCRS.Temporal.TRUNCATED_JULIAN.crs();
        assertNull(IdentifiedObjects.lookupEPSG(crs));                  // Not an EPSG code.
        assertNull(IdentifiedObjects.lookupURN(crs, Citations.SIS));    // Not in SIS namespace.
        assertEquals("urn:ogc:def:crs:OGC::TruncatedJulianDate", IdentifiedObjects.lookupURN(crs, Citations.OGC));
    }

    /**
     * Tests {@link CommonCRS#universal(double, double)} with Universal Transverse Mercator (UTM) projections.
     */
    @Test
    public void testUTM() {
        final ProjectedCRS crs = CommonCRS.WGS72.universal(-45, -122);
        assertEquals("WGS 72 / UTM zone 10S", crs.getName().getCode());
        final ParameterValueGroup pg = crs.getConversionFromBase().getParameterValues();
        assertEquals(       0, pg.parameter(Constants.LATITUDE_OF_ORIGIN).doubleValue());
        assertEquals(    -123, pg.parameter(Constants.CENTRAL_MERIDIAN)  .doubleValue());
        assertEquals(  0.9996, pg.parameter(Constants.SCALE_FACTOR)      .doubleValue());
        assertEquals(  500000, pg.parameter(Constants.FALSE_EASTING)     .doubleValue());
        assertEquals(10000000, pg.parameter(Constants.FALSE_NORTHING)    .doubleValue());
        assertSame(crs, CommonCRS.WGS72.universal(-45, -122), "Expected a cached instance.");
        assertNotSame(crs, CommonCRS.WGS72.universal(+45, -122), "Expected a new instance.");
    }

    /**
     * Tests {@link CommonCRS#universal(double, double)} with Universal Polar Stereographic (UPS) projections.
     */
    @Test
    public void testUPS() {
        final ProjectedCRS crs = CommonCRS.WGS72.universal(-85, -122);
        assertEquals("WGS 72 / Universal Polar Stereographic South", crs.getName().getCode());
        final ParameterValueGroup pg = crs.getConversionFromBase().getParameterValues();
        assertEquals(    -90, pg.parameter(Constants.LATITUDE_OF_ORIGIN).doubleValue());
        assertEquals(      0, pg.parameter(Constants.CENTRAL_MERIDIAN)  .doubleValue());
        assertEquals(  0.994, pg.parameter(Constants.SCALE_FACTOR)      .doubleValue());
        assertEquals(2000000, pg.parameter(Constants.FALSE_EASTING)     .doubleValue());
        assertEquals(2000000, pg.parameter(Constants.FALSE_NORTHING)    .doubleValue());
        assertSame(crs, CommonCRS.WGS72.universal(-85, -122), "Expected a cached instance.");
        assertNotSame(crs, CommonCRS.WGS72.universal(+85, -122), "Expected a new instance.");
    }

    /**
     * Tests {@link CommonCRS#forDatum(CoordinateReferenceSystem)}.
     *
     * @sinc 0.8
     */
    @Test
    public void testForDatum() {
        assertSame(CommonCRS.WGS84, CommonCRS.forDatum(CommonCRS.WGS84.geographic()));
        assertSame(CommonCRS.WGS72, CommonCRS.forDatum(CommonCRS.WGS72.geographic()));
    }

    /**
     * Tests {@link CommonCRS.Temporal#forEpoch(Instant)}.
     */
    @Test
    public void testForEpoch() {
        assertSame(CommonCRS.Temporal.UNIX, CommonCRS.Temporal.forEpoch(Instant.ofEpochMilli(0)));        // As specified in Javadoc.
    }

    /**
     * Tests formatting in a {@link java.util.Formatter}.
     */
    @Test
    public void testFormat() {
        assertTrue(String.format("%s",  CommonCRS.WGS84.datum(true)).startsWith("World Geodetic System 1984"));
        assertTrue(String.format("%S",  CommonCRS.WGS84.datum(true)).startsWith("WORLD GEODETIC SYSTEM 1984"));
        assertTrue(String.format("%#s", CommonCRS.WGS84.datum(true)).endsWith(":6326"));
    }
}
