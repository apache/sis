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
package org.apache.sis.referencing.dggs.smc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.dggs.Zone;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests the validation of {@link SmcDggrs} lattice definitions and regional lattices.
 *
 * @author Johann Sorel (Geomatys)
 */
public class SmcDggrsTest {

    /**
     * Regional lattice over the North Atlantic, with an odd number of columns.
     * Rows beyond 60° are merged from level 1.
     */
    private static final SmcDggrs REGIONAL = new SmcDggrs("Regional", null, -20, 45, 4, 3, 7, 6);

    private static SmcDgg grid(final int level) {
        return REGIONAL.dggs.dggh.get(level);
    }

    /**
     * Tests the constraints listed in the {@link SmcDggrs} constructor javadoc.
     */
    @Test
    public void testValidation() {
        assertThrows(IllegalArgumentException.class, () -> new SmcDggrs("Test", null, 0,   0, 10, 10, 10, 9), "Pole reached without wrapping.");
        assertThrows(IllegalArgumentException.class, () -> new SmcDggrs("Test", null, 0, -90, 50, 30, 8, 6), "More than 360°.");
        assertThrows(IllegalArgumentException.class, () -> new SmcDggrs("Test", null, 0, -90, 360, 30, 1, 6), "Wrapping with a single column.");
        assertThrows(IllegalArgumentException.class, () -> new SmcDggrs("Test", null, 0, -90, 45, 180, 8, 1), "Both poles in a single row.");
        assertThrows(IllegalArgumentException.class, () -> new SmcDggrs("Test", null, 0, -10, 1e-9, 1e-9, Integer.MAX_VALUE, Integer.MAX_VALUE), "Identifier overflow.");
        assertThrows(IllegalArgumentException.class, () -> new SmcDggrs("Test", null, 0, -10, 0, 1, 10, 10), "Zero increment.");
        assertFalse(REGIONAL.wraps);
        assertTrue(new SmcDggrs().wraps);
    }

    /**
     * Two reference systems with the same name but different lattices are not equal.
     */
    @Test
    public void testEquals() {
        assertEquals(new SmcDggrs(), new SmcDggrs());
        assertNotEquals(new SmcDggrs(), new SmcDggrs(SmcDggrs.IDENTIFIER, null, 0, -90, 90, 30, 4, 6));
    }

    /**
     * Parent and children stay nested in a regional lattice with merged rows.
     */
    @Test
    public void testRegionalNesting() {
        assertEquals(1, grid(0).mergeFactor(5));
        assertEquals(2, grid(1).mergeFactor(11));
        for (int level = 0; level <= 3; level++) {
            for (SmcZone zone : allZones(level)) {
                double area = 0;
                for (SmcZone child : zone.getChildren()) {
                    assertEquals(zone, child.getFirstParent());
                    area += child.getAreaMetersSquare();
                }
                assertEquals(zone.getAreaMetersSquare(), area, 1e-12 * zone.getAreaMetersSquare(), zone::toString);
            }
        }
    }

    /**
     * Neighbors and positions outside a regional domain do not exist.
     */
    @Test
    public void testRegionalBounds() throws TransformException {
        assertEquals(Set.of(grid(0).zone(0, 1), grid(0).zone(1, 0)), grid(0).zone(0, 0).getNeighbors());
        for (int level = 0; level <= 3; level++) {
            for (SmcZone zone : allZones(level)) {
                for (SmcZone neighbor : zone.getNeighbors()) {
                    assertTrue(neighbor.getNeighbors().contains(zone), () -> zone + " is not a neighbor of " + neighbor);
                }
            }
        }
        assertThrows(TransformException.class, () -> grid(0).getZone(
                new DirectPosition2D(CommonCRS.WGS84.normalizedGeographic(), 100, 50)));
        final SmcZone zone = grid(0).getZone(new DirectPosition2D(CommonCRS.WGS84.normalizedGeographic(), -19, 46));
        assertEquals("0-0-0", zone.getTextIdentifier().toString());

        for (int level = 0; level <= 3; level++) {
            try (Stream<Zone> zones = grid(level).getZones()) {
                assertEquals(grid(level).getZoneCount(), zones.count());
            }
        }
    }

    private static List<SmcZone> allZones(final int level) {
        final List<SmcZone> zones = new ArrayList<>();
        for (long row = 0; row < grid(level).rowCount; row++) {
            grid(level).addZones(zones, row, 0, grid(level).columnCount);
        }
        return zones;
    }
}
