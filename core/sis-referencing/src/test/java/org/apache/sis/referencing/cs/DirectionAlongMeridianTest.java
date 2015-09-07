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
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.MetadataAssert.*;


/**
 * Tests the {@link DirectionAlongMeridian} class.
 *
 * @author  Martin Desruisseaux (IRD)
 * @since   0.4
 * @version 0.4
 * @module
 */
@DependsOn(org.apache.sis.internal.metadata.AxisDirectionsTest.class)
public final strictfp class DirectionAlongMeridianTest extends TestCase {
    /**
     * Tests the {@link DirectionAlongMeridian#parse(AxisDirection)} method.
     */
    @Test
    public void testParse() {
        DirectionAlongMeridian dir;
        String name;

        name = "South along 180°";
        dir  = DirectionAlongMeridian.parse(name);
        assertNotNull(dir);
        assertEquals(AxisDirection.SOUTH, dir.baseDirection);
        assertEquals(180, dir.meridian, 0);
        assertEquals(name, dir.toString());
        assertEquals(dir, DirectionAlongMeridian.parse("South along 180 deg"));

        name = "South along 90°E";
        dir  = DirectionAlongMeridian.parse(name);
        assertNotNull(dir);
        assertEquals(AxisDirection.SOUTH, dir.baseDirection);
        assertEquals(90, dir.meridian, 0);
        assertEquals(name, dir.toString());
        assertEquals(dir, DirectionAlongMeridian.parse("South along 90 deg East"));

        name = "South along 90°W";
        dir  = DirectionAlongMeridian.parse(name);
        assertNotNull(dir);
        assertEquals(AxisDirection.SOUTH, dir.baseDirection);
        assertEquals(-90, dir.meridian, 0);
        assertEquals(name, dir.toString());
        assertEquals(dir, DirectionAlongMeridian.parse("South along 90 deg West"));

        name = "North along 45°E";
        dir  = DirectionAlongMeridian.parse(name);
        assertNotNull(dir);
        assertEquals(AxisDirection.NORTH, dir.baseDirection);
        assertEquals(45, dir.meridian, 0);
        assertEquals(name, dir.toString());
        assertEquals(dir, DirectionAlongMeridian.parse("North along 45 deg East"));
    }

    /**
     * Tests the ordering, which also involve a test of angle measurement.
     */
    @Test
    public void testOrdering() {
        assertOrdered("North along  90 deg East",   "North along   0 deg");
        assertOrdered("North along  75 deg West",   "North along 165 deg West");
        assertOrdered("South along  90 deg West",   "South along   0 deg");
        assertOrdered("South along  90 deg East",   "South along 180 deg");
        assertOrdered("South along 180 deg",        "South along  90 deg West");
        assertOrdered("North along 130 deg West",   "North along 140 deg East");
    }

    /**
     * Tests if the following directions have an angle of 90° between each other.
     */
    private static void assertOrdered(final String dir1, final String dir2) {
        final DirectionAlongMeridian m1 = DirectionAlongMeridian.parse(dir1);
        final DirectionAlongMeridian m2 = DirectionAlongMeridian.parse(dir2);
        assertEquals(+90, m1.angle(m2), STRICT);
        assertEquals(-90, m2.angle(m1), STRICT);
        assertEquals( -1, m1.compareTo(m2));
        assertEquals( +1, m2.compareTo(m1));
        assertFalse (m1.equals(m2));
    }

    /**
     * Tests Well Known Text formatting.
     */
    @Test
    public void testWKT() {
        final DirectionAlongMeridian dm = DirectionAlongMeridian.parse("South along 90°W");
        assertWktEquals(Convention.WKT2,
                "MERIDIAN[-90.0, ANGLEUNIT[“degree”, 0.017453292519943295]]", dm);
        assertWktEquals(Convention.WKT2_SIMPLIFIED,
                "Meridian[-90.0, Unit[“degree”, 0.017453292519943295]]", dm);
    }
}
