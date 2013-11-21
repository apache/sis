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

import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.opengis.referencing.cs.AxisDirection.*;


/**
 * Tests the {@link Directions} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4 (derived from geotk-3.00)
 * @version 0.4
 * @module
 */
public final strictfp class DirectionsTest extends TestCase {
    /**
     * Tests the standards directions.
     */
    @Test
    public void testCompass() {
        assertSame(NORTH, Directions.find("NORTH"));
        assertSame(SOUTH, Directions.find("South"));
        assertSame(EAST,  Directions.find("east"));
        assertSame(WEST,  Directions.find(" west "));
        assertSame(WEST,  Directions.find("W"));
        assertSame(NORTH, Directions.find(" N "));
        assertSame(SOUTH, Directions.find("s"));
    }

    /**
     * Tests mixin of the 4 compass directions.
     */
    @Test
    @DependsOnMethod("testCompass")
    public void testMixin() {
        assertSame(NORTH_EAST,       Directions.find("North-East"));
        assertSame(SOUTH_SOUTH_WEST, Directions.find("South South West"));
        assertSame(NORTH_EAST,       Directions.find("NE"));
        assertSame(SOUTH_SOUTH_WEST, Directions.find("SSW"));
    }

    /**
     * Tests the geocentric X direction.
     */
    @Test
    public void testGeocentricX() {
        assertSame(GEOCENTRIC_X, Directions.find("Geocentre > equator/PM"));
        assertSame(GEOCENTRIC_X, Directions.find("Geocentre>equator / PM"));
        assertSame(GEOCENTRIC_X, Directions.find("Geocentre > equator/0°E"));
    }

    /**
     * Tests the geocentric Y direction.
     */
    @Test
    public void testGeocentricY() {
        assertSame(GEOCENTRIC_Y, Directions.find("Geocentre > equator/90°E"));
        assertSame(GEOCENTRIC_Y, Directions.find("Geocentre > equator/90dE"));
        assertSame(GEOCENTRIC_Y, Directions.find("Geocentre>equator / 90dE"));
        assertSame(GEOCENTRIC_Y, Directions.find("GEOCENTRE > EQUATOR/90dE"));
    }

    /**
     * Tests the geocentric Z direction.
     */
    @Test
    public void testGeocentricZ() {
        assertSame(GEOCENTRIC_Z, Directions.find("Geocentre > north pole"));
        assertSame(GEOCENTRIC_Z, Directions.find("Geocentre>north pole "));
    }
}
