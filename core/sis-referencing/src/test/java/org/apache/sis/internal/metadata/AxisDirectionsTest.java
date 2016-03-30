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
package org.apache.sis.internal.metadata;

import java.lang.reflect.Field;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import javax.measure.unit.Unit;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.apache.sis.referencing.cs.HardCodedAxes;
import org.apache.sis.referencing.cs.HardCodedCS;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static java.lang.StrictMath.abs;
import static org.opengis.referencing.cs.AxisDirection.*;
import static org.apache.sis.internal.metadata.AxisDirections.AWAY_FROM;
import static org.apache.sis.internal.metadata.AxisDirections.CLOCKWISE;
import static org.apache.sis.internal.metadata.AxisDirections.COUNTER_CLOCKWISE;


/**
 * Tests the {@link AxisDirections} class.
 *
 * <p>The {@code AxisDirections} class is defined in the {@code sis-metadata} module, but tested in the
 * {@code sis-referencing} module because those tests use {@link HardCodedAxes} constants.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.6
 * @module
 */
public final strictfp class AxisDirectionsTest extends TestCase {
    /**
     * Tests {@link AxisDirections#absolute(AxisDirection)}.
     */
    @Test
    public void testAbsolute() {
        assertEquals(NORTH,             AxisDirections.absolute(NORTH));
        assertEquals(NORTH,             AxisDirections.absolute(SOUTH));
        assertEquals(EAST,              AxisDirections.absolute(EAST));
        assertEquals(EAST,              AxisDirections.absolute(WEST));
        assertEquals(NORTH_EAST,        AxisDirections.absolute(NORTH_EAST));
        assertEquals(NORTH_EAST,        AxisDirections.absolute(SOUTH_WEST));
        assertEquals(NORTH_NORTH_EAST,  AxisDirections.absolute(NORTH_NORTH_EAST));
        assertEquals(NORTH_NORTH_EAST,  AxisDirections.absolute(SOUTH_SOUTH_WEST));
        assertEquals(UP,                AxisDirections.absolute(UP));
        assertEquals(UP,                AxisDirections.absolute(DOWN));
        assertEquals(FUTURE,            AxisDirections.absolute(FUTURE));
        assertEquals(FUTURE,            AxisDirections.absolute(PAST));
        assertEquals(AWAY_FROM,         AxisDirections.absolute(AWAY_FROM));
        assertEquals(COUNTER_CLOCKWISE, AxisDirections.absolute(CLOCKWISE));
        assertEquals(COUNTER_CLOCKWISE, AxisDirections.absolute(COUNTER_CLOCKWISE));
    }

    /**
     * Tests {@link AxisDirections#opposite(AxisDirection)}.
     */
    @Test
    public void testOpposite() {
        assertEquals(SOUTH,             AxisDirections.opposite(NORTH));
        assertEquals(NORTH,             AxisDirections.opposite(SOUTH));
        assertEquals(WEST,              AxisDirections.opposite(EAST));
        assertEquals(EAST,              AxisDirections.opposite(WEST));
        assertEquals(SOUTH_WEST,        AxisDirections.opposite(NORTH_EAST));
        assertEquals(NORTH_EAST,        AxisDirections.opposite(SOUTH_WEST));
        assertEquals(SOUTH_SOUTH_WEST,  AxisDirections.opposite(NORTH_NORTH_EAST));
        assertEquals(NORTH_NORTH_EAST,  AxisDirections.opposite(SOUTH_SOUTH_WEST));
        assertEquals(DOWN,              AxisDirections.opposite(UP));
        assertEquals(UP,                AxisDirections.opposite(DOWN));
        assertEquals(PAST,              AxisDirections.opposite(FUTURE));
        assertEquals(FUTURE,            AxisDirections.opposite(PAST));
        assertEquals(COUNTER_CLOCKWISE, AxisDirections.opposite(CLOCKWISE));
        assertEquals(CLOCKWISE,         AxisDirections.opposite(COUNTER_CLOCKWISE));
        assertNull  (                   AxisDirections.opposite(AWAY_FROM));
    }

    /**
     * Tests {@link AxisDirections#isOpposite(AxisDirection)}.
     */
    @Test
    @DependsOnMethod("testOpposite")
    public void testIsOpposite() {
        assertFalse(AxisDirections.isOpposite(NORTH ));
        assertTrue (AxisDirections.isOpposite(SOUTH ));
        assertFalse(AxisDirections.isOpposite(EAST  ));
        assertTrue (AxisDirections.isOpposite(WEST  ));
        assertFalse(AxisDirections.isOpposite(UP    ));
        assertTrue (AxisDirections.isOpposite(DOWN  ));
        assertFalse(AxisDirections.isOpposite(FUTURE));
        assertTrue (AxisDirections.isOpposite(PAST  ));
        assertFalse(AxisDirections.isOpposite(OTHER ));
    }

    /**
     * Tests {@link AxisDirections#isIntercardinal(AxisDirection)}.
     */
    @Test
    public void testIsIntercardinal() {
        assertFalse(AxisDirections.isIntercardinal(NORTH));
        assertTrue (AxisDirections.isIntercardinal(NORTH_NORTH_EAST));
        assertTrue (AxisDirections.isIntercardinal(NORTH_EAST));
        assertTrue (AxisDirections.isIntercardinal(EAST_NORTH_EAST));
        assertFalse(AxisDirections.isIntercardinal(EAST));
        assertTrue (AxisDirections.isIntercardinal(EAST_SOUTH_EAST));
        assertTrue (AxisDirections.isIntercardinal(SOUTH_EAST));
        assertTrue (AxisDirections.isIntercardinal(SOUTH_SOUTH_EAST));
        assertFalse(AxisDirections.isIntercardinal(SOUTH));
        assertTrue (AxisDirections.isIntercardinal(SOUTH_SOUTH_WEST));
        assertTrue (AxisDirections.isIntercardinal(SOUTH_WEST));
        assertTrue (AxisDirections.isIntercardinal(WEST_SOUTH_WEST));
        assertFalse(AxisDirections.isIntercardinal(WEST));
        assertTrue (AxisDirections.isIntercardinal(WEST_NORTH_WEST));
        assertTrue (AxisDirections.isIntercardinal(NORTH_WEST));
        assertTrue (AxisDirections.isIntercardinal(NORTH_NORTH_WEST));
        assertFalse(AxisDirections.isIntercardinal(UP));
        assertFalse(AxisDirections.isIntercardinal(FUTURE));
        assertFalse(AxisDirections.isIntercardinal(OTHER));
    }

    /**
     * Tests {@link AxisDirections#isVertical(AxisDirection)}.
     */
    @Test
    public void testIsVertical() {
        for (final AxisDirection dir : AxisDirection.values()) {
            assertEquals(dir.name(), dir == UP || dir == DOWN, AxisDirections.isVertical(dir));
        }
    }

    /**
     * Tests {@link AxisDirections#isGeocentric(AxisDirection)}.
     */
    @Test
    public void testIsGeocentric() {
        assertTrue (AxisDirections.isGeocentric(GEOCENTRIC_X));
        assertTrue (AxisDirections.isGeocentric(GEOCENTRIC_Y));
        assertTrue (AxisDirections.isGeocentric(GEOCENTRIC_Z));
        assertFalse(AxisDirections.isGeocentric(NORTH));
        assertFalse(AxisDirections.isGeocentric(UP));
        assertFalse(AxisDirections.isGeocentric(DOWN));
        assertFalse(AxisDirections.isGeocentric(FUTURE));
    }

    /**
     * Tests {@link AxisDirections#isSpatialOrUserDefined(AxisDirection, boolean)} and
     * {@link AxisDirections#isGrid(AxisDirection)}.
     */
    @Test
    public void testIsSpatialOrGrid() {
        /*
         * Spatial directions.
         */
        verifyProperties(true, false, false,
                NORTH, EAST, SOUTH, WEST, NORTH_EAST, SOUTH_EAST, SOUTH_WEST, NORTH_WEST,
                NORTH_NORTH_EAST, EAST_NORTH_EAST, EAST_SOUTH_EAST, SOUTH_SOUTH_EAST,
                SOUTH_SOUTH_WEST, WEST_SOUTH_WEST, WEST_NORTH_WEST, NORTH_NORTH_WEST,
                UP, DOWN, GEOCENTRIC_X, GEOCENTRIC_Y, GEOCENTRIC_Z);
        /*
         * Grid directions.
         */
        verifyProperties(false, true, false,
            COLUMN_POSITIVE, COLUMN_NEGATIVE, ROW_POSITIVE, ROW_NEGATIVE);
        /*
         * Display directions.
         */
        verifyProperties(false, false, true,
            DISPLAY_RIGHT, DISPLAY_LEFT, DISPLAY_UP, DISPLAY_DOWN);
        /*
         * Temporal directions.
         */
        verifyProperties(false, false, false,
            FUTURE, PAST);
    }

    /**
     * Asserts that
     * {@link AxisDirections#isSpatialOrUserDefined(AxisDirection, boolean)},
     * {@link AxisDirections#isCartesianOrCustom(AxisDirection)} and
     * {@link AxisDirections#isGrid(AxisDirection)}
     * returns the expected value for all the given axis directions.
     */
    private static void verifyProperties(final boolean isSpatial, final boolean isGrid, final boolean isDisplay,
            final AxisDirection... directions)
    {
        for (final AxisDirection dir : directions) {
            final String name = dir.name();
            assertEquals(name, isGrid, AxisDirections.isGrid(dir));
            assertEquals(name, isSpatial, AxisDirections.isSpatialOrUserDefined(dir, false));
            assertEquals(name, isSpatial | isGrid | isDisplay, AxisDirections.isSpatialOrUserDefined(dir, true));
        }
    }

    /**
     * Tests the {@link AxisDirections#angleForCompass(AxisDirection, AxisDirection)} method.
     */
    @Test
    public void testAngleForCompass() {
        final AxisDirection[] compass = new AxisDirection[] {
            NORTH,
            NORTH_NORTH_EAST,
            NORTH_EAST,
            EAST_NORTH_EAST,
            EAST,
            EAST_SOUTH_EAST,
            SOUTH_EAST,
            SOUTH_SOUTH_EAST,
            SOUTH,
            SOUTH_SOUTH_WEST,
            SOUTH_WEST,
            WEST_SOUTH_WEST,
            WEST,
            WEST_NORTH_WEST,
            NORTH_WEST,
            NORTH_NORTH_WEST
        };
        assertEquals(compass.length, AxisDirections.COMPASS_COUNT);
        final int base = NORTH.ordinal();
        final int h = compass.length / 2;
        for (int i=0; i<compass.length; i++) {
            final AxisDirection direction = compass[i];
            final AxisDirection opposite  = AxisDirections.opposite(direction);
            final String        message   = direction.name();
            int io = i+h, in = i;
            if (i >= h) io -= AxisDirections.COMPASS_COUNT;
            if (i >  h) in -= AxisDirections.COMPASS_COUNT;
            assertEquals(message, base + i,  direction.ordinal());
            assertEquals(message, base + io, opposite.ordinal());
            assertEquals(message, 0,     AxisDirections.angleForCompass(direction, direction));
            assertEquals(message, h, abs(AxisDirections.angleForCompass(direction, opposite)));
            assertEquals(message, in,    AxisDirections.angleForCompass(direction, NORTH));
        }
    }

    /**
     * Tests the {@link AxisDirections#angleForGeocentric(AxisDirection, AxisDirection)} method.
     */
    @Test
    public void testAngleForGeocentric() {
        assertEquals( 0, AxisDirections.angleForGeocentric(GEOCENTRIC_X, GEOCENTRIC_X));
        assertEquals( 1, AxisDirections.angleForGeocentric(GEOCENTRIC_X, GEOCENTRIC_Y));
        assertEquals( 1, AxisDirections.angleForGeocentric(GEOCENTRIC_Y, GEOCENTRIC_Z));
        assertEquals( 1, AxisDirections.angleForGeocentric(GEOCENTRIC_Z, GEOCENTRIC_X));
        assertEquals(-1, AxisDirections.angleForGeocentric(GEOCENTRIC_X, GEOCENTRIC_Z));
        assertEquals(-1, AxisDirections.angleForGeocentric(GEOCENTRIC_Z, GEOCENTRIC_Y));
        assertEquals(-1, AxisDirections.angleForGeocentric(GEOCENTRIC_Y, GEOCENTRIC_X));
    }

    /**
     * Tests the {@link AxisDirections#angleForDisplay(AxisDirection, AxisDirection)} method.
     */
    @Test
    public void testAngleForDisplay() {
        assertEquals( 0, AxisDirections.angleForDisplay(DISPLAY_RIGHT, DISPLAY_RIGHT));
        assertEquals( 1, AxisDirections.angleForDisplay(DISPLAY_RIGHT, DISPLAY_UP));
        assertEquals(-2, AxisDirections.angleForDisplay(DISPLAY_RIGHT, DISPLAY_LEFT));
        assertEquals(-1, AxisDirections.angleForDisplay(DISPLAY_RIGHT, DISPLAY_DOWN));
        assertEquals( 0, AxisDirections.angleForDisplay(DISPLAY_UP,    DISPLAY_UP));
        assertEquals( 1, AxisDirections.angleForDisplay(DISPLAY_UP,    DISPLAY_LEFT));
        assertEquals(-2, AxisDirections.angleForDisplay(DISPLAY_UP,    DISPLAY_DOWN));
        assertEquals(-1, AxisDirections.angleForDisplay(DISPLAY_UP,    DISPLAY_RIGHT));
        assertEquals( 0, AxisDirections.angleForDisplay(DISPLAY_LEFT,  DISPLAY_LEFT));
        assertEquals( 1, AxisDirections.angleForDisplay(DISPLAY_LEFT,  DISPLAY_DOWN));
        assertEquals( 2, AxisDirections.angleForDisplay(DISPLAY_LEFT,  DISPLAY_RIGHT));
        assertEquals(-1, AxisDirections.angleForDisplay(DISPLAY_LEFT,  DISPLAY_UP));
        assertEquals( 0, AxisDirections.angleForDisplay(DISPLAY_DOWN,  DISPLAY_DOWN));
        assertEquals( 1, AxisDirections.angleForDisplay(DISPLAY_DOWN,  DISPLAY_RIGHT));
        assertEquals( 2, AxisDirections.angleForDisplay(DISPLAY_DOWN,  DISPLAY_UP));
        assertEquals(-1, AxisDirections.angleForDisplay(DISPLAY_DOWN,  DISPLAY_LEFT));
    }

    /**
     * Tests {@link AxisDirections#valueOf(String)} for the North, South, East and West directions.
     */
    @Test
    public void testValueOfCardinalDirection() {
        assertSame(NORTH, AxisDirections.valueOf("NORTH"));
        assertSame(SOUTH, AxisDirections.valueOf("South"));
        assertSame(EAST,  AxisDirections.valueOf("east"));
        assertSame(WEST,  AxisDirections.valueOf(" west "));
        assertSame(WEST,  AxisDirections.valueOf("W"));
        assertSame(NORTH, AxisDirections.valueOf(" N "));
        assertSame(SOUTH, AxisDirections.valueOf("s"));
    }

    /**
     * Tests {@link AxisDirections#valueOf(String)} for directions like North-East and South-South-West.
     */
    @Test
    @DependsOnMethod("testValueOfCardinalDirection")
    public void testValueOfInterCardinalDirection() {
        assertSame(NORTH_EAST,       AxisDirections.valueOf("North-East"));
        assertSame(SOUTH_SOUTH_WEST, AxisDirections.valueOf("South South West"));
        assertSame(NORTH_EAST,       AxisDirections.valueOf("NE"));
        assertSame(SOUTH_SOUTH_WEST, AxisDirections.valueOf("SSW"));
    }

    /**
     * Tests {@link AxisDirections#valueOf(String)} for the geocentric X direction.
     */
    @Test
    public void testValueOfGeocentricX() {
        assertSame(GEOCENTRIC_X, AxisDirections.valueOf("Geocentre > equator/PM"));
        assertSame(GEOCENTRIC_X, AxisDirections.valueOf("Geocentre>equator / PM"));
        assertSame(GEOCENTRIC_X, AxisDirections.valueOf("Geocentre > equator/0°E"));
    }

    /**
     * Tests {@link AxisDirections#valueOf(String)} for the geocentric Y direction.
     */
    @Test
    public void testValueOfGeocentricY() {
        assertSame(GEOCENTRIC_Y, AxisDirections.valueOf("Geocentre > equator/90°E"));
        assertSame(GEOCENTRIC_Y, AxisDirections.valueOf("Geocentre > equator/90dE"));
        assertSame(GEOCENTRIC_Y, AxisDirections.valueOf("Geocentre>equator / 90dE"));
        assertSame(GEOCENTRIC_Y, AxisDirections.valueOf("GEOCENTRE > EQUATOR/90dE"));
    }

    /**
     * Tests {@link AxisDirections#valueOf(String)} for the geocentric Z direction.
     */
    @Test
    public void testValueOfGeocentricZ() {
        assertSame(GEOCENTRIC_Z, AxisDirections.valueOf("Geocentre > north pole"));
        assertSame(GEOCENTRIC_Z, AxisDirections.valueOf("Geocentre>north pole "));
    }

    /**
     * Tests {@link AxisDirections#indexOfColinear(CoordinateSystem, AxisDirection)}.
     */
    @Test
    public void testIndexOfColinear() {
        assertEquals(1, AxisDirections.indexOfColinear(HardCodedCS.GEODETIC_3D, AxisDirection.NORTH));
        assertEquals(1, AxisDirections.indexOfColinear(HardCodedCS.GEODETIC_3D, AxisDirection.SOUTH));
    }

    /**
     * Tests {@link AxisDirections#indexOfColinear(CoordinateSystem, CoordinateSystem)}.
     *
     * @since 0.5
     */
    @Test
    public void testIndexOfColinearCS() {
        assertEquals(0, AxisDirections.indexOfColinear(
                HardCodedCS.GEODETIC_3D,
                HardCodedCS.GEODETIC_2D));

        assertEquals(2, AxisDirections.indexOfColinear(
                HardCodedCS.GEODETIC_3D,
                HardCodedCS.ELLIPSOIDAL_HEIGHT));

        assertEquals(2, AxisDirections.indexOfColinear(
                HardCodedCS.GEODETIC_3D,
                HardCodedCS.DEPTH));

        assertEquals(-1, AxisDirections.indexOfColinear(
                HardCodedCS.GEODETIC_3D,
                HardCodedCS.DAYS));
    }

    /**
     * Tests {@link AxisDirections#suggestAbbreviation(String, AxisDirection, Unit)}.
     *
     * @since 0.6
     */
    @Test
    public void testSuggestAbbreviation() {
        assertEquals("x",   AxisDirections.suggestAbbreviation("x",                      EAST,             SI.METRE));
        assertEquals("λ",   AxisDirections.suggestAbbreviation("Geodetic longitude",     EAST,          NonSI.DEGREE_ANGLE));
        assertEquals("φ",   AxisDirections.suggestAbbreviation("Geodetic latitude",      NORTH,         NonSI.DEGREE_ANGLE));
        assertEquals("θ",   AxisDirections.suggestAbbreviation("Spherical longitude",    EAST,          NonSI.DEGREE_ANGLE));
        assertEquals("φ′",  AxisDirections.suggestAbbreviation("Spherical latitude",     NORTH,         NonSI.DEGREE_ANGLE));
        assertEquals("h",   AxisDirections.suggestAbbreviation("Ellipsoidal height",     UP,               SI.METRE));
        assertEquals("H",   AxisDirections.suggestAbbreviation("Gravity-related height", UP,               SI.METRE));
        assertEquals("φ",   AxisDirections.suggestAbbreviation("Elevation",              UP,               SI.METRE));
        assertEquals("R",   AxisDirections.suggestAbbreviation("Geocentric radius",      UP,               SI.METRE));
        assertEquals("r",   AxisDirections.suggestAbbreviation("Distance",               AWAY_FROM,        SI.METRE));
        assertEquals("θ",   AxisDirections.suggestAbbreviation("Bearing",                CLOCKWISE,     NonSI.DEGREE_ANGLE));
        assertEquals("X",   AxisDirections.suggestAbbreviation("not needed",             GEOCENTRIC_X,     SI.METRE));
        assertEquals("Y",   AxisDirections.suggestAbbreviation("not needed",             GEOCENTRIC_Y,     SI.METRE));
        assertEquals("Z",   AxisDirections.suggestAbbreviation("not needed",             GEOCENTRIC_Z,     SI.METRE));
        assertEquals("D",   AxisDirections.suggestAbbreviation("not needed",             DOWN,             SI.METRE));
        assertEquals("t",   AxisDirections.suggestAbbreviation("not needed",             FUTURE,        NonSI.DAY));
        assertEquals("i",   AxisDirections.suggestAbbreviation("not needed",             COLUMN_NEGATIVE,  Unit.ONE));
        assertEquals("j",   AxisDirections.suggestAbbreviation("not needed",             ROW_NEGATIVE,     Unit.ONE));
        assertEquals("x",   AxisDirections.suggestAbbreviation("not needed",             DISPLAY_RIGHT,    Unit.ONE));
        assertEquals("y",   AxisDirections.suggestAbbreviation("not needed",             DISPLAY_DOWN,     Unit.ONE));
        assertEquals("N",   AxisDirections.suggestAbbreviation("not needed",             NORTH,            SI.METRE));
        assertEquals("S",   AxisDirections.suggestAbbreviation("not needed",             SOUTH,            SI.METRE));
        assertEquals("E",   AxisDirections.suggestAbbreviation("not needed",             EAST,             SI.METRE));
        assertEquals("W",   AxisDirections.suggestAbbreviation("not needed",             WEST,             SI.METRE));
        assertEquals("NE",  AxisDirections.suggestAbbreviation("not needed",             NORTH_EAST,       SI.METRE));
        assertEquals("NW",  AxisDirections.suggestAbbreviation("not needed",             NORTH_WEST,       SI.METRE));
        assertEquals("SE",  AxisDirections.suggestAbbreviation("not needed",             SOUTH_EAST,       SI.METRE));
        assertEquals("SW",  AxisDirections.suggestAbbreviation("not needed",             SOUTH_WEST,       SI.METRE));
        assertEquals("SSE", AxisDirections.suggestAbbreviation("not needed",             SOUTH_SOUTH_EAST, SI.METRE));
        assertEquals("NNW", AxisDirections.suggestAbbreviation("not needed",             NORTH_NORTH_WEST, SI.METRE));
        assertEquals("ENE", AxisDirections.suggestAbbreviation("not needed",             EAST_NORTH_EAST,  SI.METRE));
    }

    /**
     * Verifies that the abbreviations used in {@link HardCodedAxes} constants are consistent with the abbreviations
     * suggested by {@link AxisDirections#suggestAbbreviation(String, AxisDirection, Unit)}.  Note that a failure in
     * this verification does not necessarily means that the {@code suggestAbbreviation(…)}. It could also be the
     * hard-coded constant which need a revision, or we may decide that the different abbreviations are intended and
     * should not be compared.
     *
     * @throws IllegalAccessException should never happen since we inspect only for public fields.
     *
     * @since 0.6
     */
    @Test
    public void verifyAbbreviationConsistency() throws IllegalAccessException {
        for (final Field field : HardCodedAxes.class.getFields()) {
            if (CoordinateSystemAxis.class.isAssignableFrom(field.getType())) {
                final CoordinateSystemAxis axis = (CoordinateSystemAxis) field.get(null);
                assertEquals(field.getName(), axis.getAbbreviation(), AxisDirections.suggestAbbreviation(
                        axis.getName().getCode(), axis.getDirection(), axis.getUnit()));
            }
        }
    }

    /**
     * Tests {@link AxisDirections#appendTo(StringBuilder, CoordinateSystemAxis[])}.
     *
     * @since 0.6
     */
    @Test
    public void testAppendTo() {
        final StringBuilder buffer = new StringBuilder("Compound CS");
        final String name = AxisDirections.appendTo(buffer, new CoordinateSystemAxis[] {
            HardCodedAxes.EASTING, HardCodedAxes.NORTHING, HardCodedAxes.HEIGHT_cm, HardCodedAxes.TIME
        });
        assertEquals("Compound CS: East (m), North (m), Up (cm), Future (d).", name);
    }
}
