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
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

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
     * Tests {@link CoordinateSystems#angle(AxisDirection, AxisDirection)}.
     */
    @Test
    @DependsOnMethod("testParseAxisDirection")
    public void testAngle() {
        assertEquals( +90, angle(AxisDirection.EAST,             AxisDirection.NORTH), STRICT);
        assertEquals( -90, angle(AxisDirection.NORTH,            AxisDirection.EAST),  STRICT);
        assertEquals( +90, angle(AxisDirection.WEST,             AxisDirection.SOUTH), STRICT);
        assertEquals( -90, angle(AxisDirection.SOUTH,            AxisDirection.WEST),  STRICT);
        assertEquals(-180, angle(AxisDirection.NORTH,            AxisDirection.SOUTH), STRICT);
        assertEquals( 180, angle(AxisDirection.SOUTH,            AxisDirection.NORTH), STRICT);
        assertEquals(  45, angle(AxisDirection.NORTH_EAST,       AxisDirection.NORTH), STRICT);
        assertEquals(22.5, angle(AxisDirection.NORTH_NORTH_EAST, AxisDirection.NORTH), STRICT);
        assertEquals(  90, angle(parseAxisDirection("North along 90°E"), parseAxisDirection("North along 0°")),   STRICT);
        assertEquals( 135, angle(parseAxisDirection("North along 90°E"), parseAxisDirection("North along 45°W")), STRICT);
        assertEquals(-135, angle(parseAxisDirection("North along 45°W"), parseAxisDirection("North along 90°E")), STRICT);
    }
}
