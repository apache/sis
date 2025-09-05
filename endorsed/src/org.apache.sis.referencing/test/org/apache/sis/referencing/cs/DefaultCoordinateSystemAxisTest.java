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
package org.apache.sis.referencing.cs;

import java.util.Map;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.RangeMeaning;
import org.apache.sis.measure.Units;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.util.ComparisonMode;
import static org.apache.sis.referencing.IdentifiedObjects.getProperties;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.opengis.test.Validators;
import org.apache.sis.test.TestCase;
import static org.apache.sis.referencing.cs.HardCodedAxes.*;
import static org.apache.sis.test.Assertions.assertSerializedEquals;
import static org.apache.sis.referencing.Assertions.assertWktEquals;


/**
 * Tests the {@link DefaultCoordinateSystemAxis} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
public final class DefaultCoordinateSystemAxisTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultCoordinateSystemAxisTest() {
    }

    /**
     * Validates the {@link HardCodedAxes} constants.
     */
    @Test
    public void validate() {
        Validators.validate(GEODETIC_LONGITUDE);
        Validators.validate(GEODETIC_LATITUDE);
        Validators.validate(LONGITUDE_gon);
        Validators.validate(LATITUDE_gon);
        Validators.validate(ELLIPSOIDAL_HEIGHT);
        Validators.validate(GRAVITY_RELATED_HEIGHT);
        Validators.validate(ALTITUDE);
        Validators.validate(DEPTH);
        Validators.validate(GEOCENTRIC_RADIUS);
        Validators.validate(SPHERICAL_LONGITUDE);
        Validators.validate(SPHERICAL_LATITUDE);
        Validators.validate(X);
        Validators.validate(Y);
        Validators.validate(Z);
        Validators.validate(GEOCENTRIC_X);
        Validators.validate(GEOCENTRIC_Y);
        Validators.validate(GEOCENTRIC_Z);
        Validators.validate(EASTING);
        Validators.validate(WESTING);
        Validators.validate(NORTHING);
        Validators.validate(SOUTHING);
        Validators.validate(TIME);
        Validators.validate(COLUMN);
        Validators.validate(ROW);
        Validators.validate(DISPLAY_X);
        Validators.validate(DISPLAY_Y);
    }

    /**
     * Tests WKT formatting of predefined constants.
     *
     * Note that this method cannot test the replacement of Greek letters by Latin letters in abbreviations,
     * because those replacements depend on the {@code CoordinateSystem} context, which is not provided by
     * this test method.
     */
    @Test
    public void testWKT() {
        assertWktEquals(Convention.WKT2, "AXIS[“x”, east, LENGTHUNIT[“metre”, 1]]",  X);
        assertWktEquals(Convention.WKT2, "AXIS[“y”, north, LENGTHUNIT[“metre”, 1]]", Y);
        assertWktEquals(Convention.WKT2, "AXIS[“z”, up, LENGTHUNIT[“metre”, 1]]",    Z);
        assertWktEquals(Convention.WKT2, "AXIS[“Longitude (λ)”, east, ANGLEUNIT[“grad”, 0.015707963267948967]]",             LONGITUDE_gon);
        assertWktEquals(Convention.WKT2, "AXIS[“Latitude (φ)”, north, ANGLEUNIT[“grad”, 0.015707963267948967]]",             LATITUDE_gon);
        assertWktEquals(Convention.WKT2, "AXIS[“Altitude (h)”, up, LENGTHUNIT[“metre”, 1]]",                                 ALTITUDE);
        assertWktEquals(Convention.WKT2, "AXIS[“Time (t)”, future, TIMEUNIT[“day”, 86400]]",                                 TIME);
        assertWktEquals(Convention.WKT2, "AXIS[“Longitude (λ)”, east, ANGLEUNIT[“degree”, 0.017453292519943295]]",           GEODETIC_LONGITUDE);
        assertWktEquals(Convention.WKT2, "AXIS[“Spherical longitude (θ)”, east, ANGLEUNIT[“degree”, 0.017453292519943295]]", SPHERICAL_LONGITUDE);
        assertWktEquals(Convention.WKT2, "AXIS[“Latitude (φ)”, north, ANGLEUNIT[“degree”, 0.017453292519943295]]",           GEODETIC_LATITUDE);
        assertWktEquals(Convention.WKT2, "AXIS[“Spherical latitude (Ω)”, north, ANGLEUNIT[“degree”, 0.017453292519943295]]", SPHERICAL_LATITUDE);

        assertWktEquals(Convention.WKT1,     "AXIS[“x”, EAST]",  X);
        assertWktEquals(Convention.WKT1,     "AXIS[“y”, NORTH]", Y);
        assertWktEquals(Convention.WKT1,     "AXIS[“z”, UP]",    Z);
        assertWktEquals(Convention.INTERNAL, "Axis[“Geodetic longitude (λ)”, east, Unit[“degree”, 0.017453292519943295, Id[“EPSG”, 9102]]]",  GEODETIC_LONGITUDE);
        assertWktEquals(Convention.INTERNAL, "Axis[“Spherical longitude (θ)”, east, Unit[“degree”, 0.017453292519943295, Id[“EPSG”, 9102]]]", SPHERICAL_LONGITUDE);
        assertWktEquals(Convention.INTERNAL, "Axis[“Geodetic latitude (φ)”, north, Unit[“degree”, 0.017453292519943295, Id[“EPSG”, 9102]]]",  GEODETIC_LATITUDE);
        assertWktEquals(Convention.INTERNAL, "Axis[“Spherical latitude (Ω)”, north, Unit[“degree”, 0.017453292519943295, Id[“EPSG”, 9102]]]", SPHERICAL_LATITUDE);
    }

    /**
     * Tests the WKT of axis of the kind "South along 90°W".
     */
    @Test
    public void testMeridianWKT() {
        assertWktEquals(
                Convention.WKT2,
                "AXIS[“South along 90°W (x)”, south, MERIDIAN[-90.0, ANGLEUNIT[“degree”, 0.017453292519943295]], LENGTHUNIT[“metre”, 1]]",
                new DefaultCoordinateSystemAxis(
                        Map.of(DefaultCoordinateSystemAxis.NAME_KEY, "South along 90°W"),
                        "x",
                        new DirectionAlongMeridian(AxisDirection.SOUTH, -90).getDirection(),
                        Units.METRE));
    }

    /**
     * Tests the {@link DefaultCoordinateSystemAxis#isHeuristicMatchForName(String)} method.
     */
    @Test
    public void testIsHeuristicMatchForName() {
        assertTrue (LONGITUDE_gon.isHeuristicMatchForName(GEODETIC_LONGITUDE.getName().getCode()));
        assertFalse(LONGITUDE_gon.isHeuristicMatchForName(GEODETIC_LATITUDE .getName().getCode()));
        assertFalse(LONGITUDE_gon.isHeuristicMatchForName(ALTITUDE          .getName().getCode()));
        assertTrue (LATITUDE_gon .isHeuristicMatchForName(GEODETIC_LATITUDE .getName().getCode()));
        assertFalse(X            .isHeuristicMatchForName(LONGITUDE_gon     .getName().getCode()));
        assertFalse(X            .isHeuristicMatchForName(EASTING           .getName().getCode()));
        assertFalse(X            .isHeuristicMatchForName(NORTHING          .getName().getCode()));
    }

    /**
     * Tests the comparison of some axis, ignoring metadata.
     */
    @Test
    public void testEqualsIgnoreMetadata() {
        /*
         * Defines, only for the purpose of this test, axis constants identical to
         * (GEODETIC_LONGITUDE, GEODETIC_LATITUDE) except for the name.
         */
        final DefaultCoordinateSystemAxis LONGITUDE = new DefaultCoordinateSystemAxis(getProperties(LONGITUDE_gon),
                "λ", AxisDirection.EAST, Units.DEGREE);
        final DefaultCoordinateSystemAxis LATITUDE = new DefaultCoordinateSystemAxis(getProperties(LATITUDE_gon),
                "φ", AxisDirection.NORTH, Units.DEGREE);
        /*
         * Verifies the properties inferred by the constructor.
         */
        assertEquals(-180, LONGITUDE.getMinimumValue());
        assertEquals(+180, LONGITUDE.getMaximumValue());
        assertEquals(RangeMeaning.WRAPAROUND, LONGITUDE.getRangeMeaning());
        assertEquals(-90, LATITUDE.getMinimumValue());
        assertEquals(+90, LATITUDE.getMaximumValue());
        assertEquals(RangeMeaning.EXACT, LATITUDE.getRangeMeaning());
        /*
         * Those axes shall be considered different.
         */
        assertFalse(X        .equals(GEOCENTRIC_X,        ComparisonMode.IGNORE_METADATA));
        assertFalse(LONGITUDE.equals(GEODETIC_LONGITUDE,  ComparisonMode.STRICT));
        assertFalse(LONGITUDE.equals(SPHERICAL_LONGITUDE, ComparisonMode.STRICT));
        assertFalse(LONGITUDE.equals(SPHERICAL_LONGITUDE, ComparisonMode.IGNORE_METADATA));
        /*
         * Tests aliases in the special "longitude" and "latitude" cases.
         */
        assertTrue (LONGITUDE.equals(GEODETIC_LONGITUDE,  ComparisonMode.IGNORE_METADATA));
        assertTrue (LATITUDE .equals(GEODETIC_LATITUDE,   ComparisonMode.IGNORE_METADATA));
        assertFalse(LATITUDE .equals(LONGITUDE,           ComparisonMode.IGNORE_METADATA));
        /*
         * Ensures that difference in "wraparound" ranges causes the axes to be considered different.
         */
        assertFalse(GEODETIC_LONGITUDE.equals(SHIFTED_LONGITUDE, ComparisonMode.IGNORE_METADATA));
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialization() {
        assertSerializedEquals(X);
        assertSerializedEquals(GEOCENTRIC_X);
        assertSerializedEquals(GEODETIC_LONGITUDE);
    }
}
