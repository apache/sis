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

import org.opengis.referencing.cs.AxisDirection;
import org.apache.sis.internal.referencing.AxisDirections;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static java.lang.StrictMath.*;
import static org.junit.Assert.*;
import static org.apache.sis.referencing.cs.CoordinateSystems.*;


/**
 * Tests the {@link CoordinateSystems} class.
 *
 * @author  Martin Desruisseaux (IRD)
 * @since   0.4 (derived from geotk-2.2)
 * @version 0.4
 * @module
 */
@DependsOn(DirectionAlongMeridianTest.class)
public final strictfp class CoordinateSystemsTest extends TestCase {
    /**
     * Tolerance threshold for strict floating point comparisons.
     */
    static final double STRICT = 0;

    /**
     * Tests {@link CoordinateSystems#parseAxisDirection(String)}.
     */
    @Test
    public void testParseAxisDirection() {
        assertEquals("NORTH",            AxisDirection.NORTH,            parseAxisDirection("NORTH"));
        assertEquals("north",            AxisDirection.NORTH,            parseAxisDirection("north"));
        assertEquals("  north ",         AxisDirection.NORTH,            parseAxisDirection("  north "));
        assertEquals("east",             AxisDirection.EAST,             parseAxisDirection("east"));
        assertEquals("NORTH_EAST",       AxisDirection.NORTH_EAST,       parseAxisDirection("NORTH_EAST"));
        assertEquals("north-east",       AxisDirection.NORTH_EAST,       parseAxisDirection("north-east"));
        assertEquals("north east",       AxisDirection.NORTH_EAST,       parseAxisDirection("north east"));
        assertEquals("south-south-east", AxisDirection.SOUTH_SOUTH_EAST, parseAxisDirection("south-south-east"));
        assertEquals("South along 180°", parseAxisDirection("South along 180 deg").name());
        assertEquals("South along 180°", parseAxisDirection("South along 180°").name());
        assertEquals("South along 180°", parseAxisDirection(" SOUTH  along  180 ° ").name());
        assertEquals("South along 90°E", parseAxisDirection("south along 90 deg east").name());
        assertEquals("South along 90°E", parseAxisDirection("south along 90°e").name());
        assertEquals("North along 45°E", parseAxisDirection("north along 45 deg e").name());
        assertEquals("North along 45°W", parseAxisDirection("north along 45 deg west").name());
    }

    /**
     * Tests the {@link CoordinateSystems#getCompassAngle(AxisDirection, AxisDirection)} method.
     */
    @Test
    public void testGetCompassAngle() {
        final AxisDirection[] compass = new AxisDirection[] {
            AxisDirection.NORTH,
            AxisDirection.NORTH_NORTH_EAST,
            AxisDirection.NORTH_EAST,
            AxisDirection.EAST_NORTH_EAST,
            AxisDirection.EAST,
            AxisDirection.EAST_SOUTH_EAST,
            AxisDirection.SOUTH_EAST,
            AxisDirection.SOUTH_SOUTH_EAST,
            AxisDirection.SOUTH,
            AxisDirection.SOUTH_SOUTH_WEST,
            AxisDirection.SOUTH_WEST,
            AxisDirection.WEST_SOUTH_WEST,
            AxisDirection.WEST,
            AxisDirection.WEST_NORTH_WEST,
            AxisDirection.NORTH_WEST,
            AxisDirection.NORTH_NORTH_WEST
        };
        assertEquals(compass.length, COMPASS_DIRECTION_COUNT);
        final int base = AxisDirection.NORTH.ordinal();
        final int h = compass.length / 2;
        for (int i=0; i<compass.length; i++) {
            final AxisDirection direction = compass[i];
            final AxisDirection opposite  = AxisDirections.opposite(direction);
            final String        message   = direction.name();
            int io = i+h, in = i;
            if (i >= h) io -= COMPASS_DIRECTION_COUNT;
            if (i >  h) in -= COMPASS_DIRECTION_COUNT;
            assertEquals(message, base + i,  direction.ordinal());
            assertEquals(message, base + io, opposite.ordinal());
            assertEquals(message, 0,     getCompassAngle(direction, direction));
            assertEquals(message, h, abs(getCompassAngle(direction, opposite)));
            assertEquals(message, in,    getCompassAngle(direction, AxisDirection.NORTH));
        }
    }

    /**
     * Tests {@link CoordinateSystems#angle(AxisDirection, AxisDirection)}.
     */
    @Test
    @DependsOnMethod("testGetCompassAngle")
    public void testAngle() {
        assertEquals(    0, angle(AxisDirection.EAST,             AxisDirection.EAST),       STRICT);
        assertEquals(  +90, angle(AxisDirection.EAST,             AxisDirection.NORTH),      STRICT);
        assertEquals(  -90, angle(AxisDirection.NORTH,            AxisDirection.EAST),       STRICT);
        assertEquals(  +90, angle(AxisDirection.WEST,             AxisDirection.SOUTH),      STRICT);
        assertEquals(  -90, angle(AxisDirection.SOUTH,            AxisDirection.WEST),       STRICT);
        assertEquals( -180, angle(AxisDirection.NORTH,            AxisDirection.SOUTH),      STRICT);
        assertEquals(  180, angle(AxisDirection.SOUTH,            AxisDirection.NORTH),      STRICT);
        assertEquals(   45, angle(AxisDirection.NORTH_EAST,       AxisDirection.NORTH),      STRICT);
        assertEquals( 22.5, angle(AxisDirection.NORTH_NORTH_EAST, AxisDirection.NORTH),      STRICT);
        assertEquals(-22.5, angle(AxisDirection.NORTH_NORTH_WEST, AxisDirection.NORTH),      STRICT);
        assertEquals(   45, angle(AxisDirection.SOUTH,            AxisDirection.SOUTH_EAST), STRICT);
    }

    /**
     * Tests {@link CoordinateSystems#angle(AxisDirection, AxisDirection)} using directions parsed from text.
     */
    @Test
    @DependsOnMethod({"testParseAxisDirection", "testAngle"})
    public void testAngleAlongMeridians() {
        compareAngle( 90.0, "West",                    "South");
        compareAngle(-90.0, "South",                   "West");
        compareAngle( 45.0, "South",                   "South-East");
        compareAngle(-22.5, "North-North-West",        "North");
        compareAngle(-22.5, "North_North_West",        "North");
        compareAngle(-22.5, "North North West",        "North");
        compareAngle( 90.0, "North along 90 deg East", "North along 0 deg");
        compareAngle( 90.0, "South along 180 deg",     "South along 90 deg West");
        compareAngle(   90, "North along 90°E",        "North along 0°");
        compareAngle(  135, "North along 90°E",        "North along 45°W");
        compareAngle( -135, "North along 45°W",        "North along 90°E");
    }

    /**
     * Compare the angle between the specified directions.
     */
    private static void compareAngle(final double expected, final String source, final String target) {
        final AxisDirection dir1 = parseAxisDirection(source);
        final AxisDirection dir2 = parseAxisDirection(target);
        assertNotNull(source, dir1);
        assertNotNull(target, dir2);
        assertEquals(expected, angle(dir1, dir2), STRICT);
    }
}
