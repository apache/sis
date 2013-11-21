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
package org.apache.sis.internal.referencing;

import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.opengis.referencing.cs.AxisDirection.*;


/**
 * Tests the {@link AxisDirections} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4 (derived from geotk-3.00)
 * @version 0.4
 * @module
 */
public final strictfp class AxisDirectionsTest extends TestCase {
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
}
