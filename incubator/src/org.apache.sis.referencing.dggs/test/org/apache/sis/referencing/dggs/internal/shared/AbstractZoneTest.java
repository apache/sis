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
package org.apache.sis.referencing.dggs.internal.shared;

import java.util.List;
import javax.measure.Quantity;
import javax.measure.quantity.Area;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.dggs.Zone;
import org.apache.sis.referencing.dggs.s2.S2Dggrs;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link AbstractZone} through S2 zones.
 *
 * @author Johann Sorel (Geomatys)
 */
public class AbstractZoneTest {

    private final DiscreteGlobalGridReferenceSystem dggrs = new S2Dggrs();

    /**
     * Returns the first two S2 faces.
     */
    private List<Zone> faces() throws TransformException {
        return dggrs.getGridSystem().getHierarchy().getGrids().get(0).getZones().limit(2).toList();
    }

    /**
     * Returns the first child of the given zone.
     */
    private static Zone firstChild(Zone zone) {
        return zone.getChildren().iterator().next();
    }

    /**
     * Tests the surface of the celestial body: Earth surface is about 5.1×10¹⁴ m².
     */
    @Test
    public void testCelestialBodySurface() {
        final Quantity<Area> surface = dggrs.getGridSystem().getCelestialBodySurface();
        assertEquals(Units.SQUARE_METRE, surface.getUnit());
        assertEquals(5.1E14, surface.getValue().doubleValue(), 0.005 * 5.1E14);
    }

    /**
     * Tests the measures of a zone. The six S2 faces have the same area, a sixth of the celestial body surface.
     */
    @Test
    public void testMeasures() throws Exception {
        final double surface = dggrs.getGridSystem().getCelestialBodySurface().getValue().doubleValue();
        final Zone face = faces().get(0);
        final Quantity<Area> area = face.getArea().orElseThrow();
        assertEquals(Units.SQUARE_METRE, area.getUnit());
        assertEquals(surface / 6, area.getValue().doubleValue(), surface * 1E-9);
        assertTrue(face.getVolume().isEmpty());
        assertTrue(face.getDuration().isEmpty());
    }

    /**
     * Tests {@link AbstractZone#isAncestorOf(Object, int)} and {@link AbstractZone#isDescendantOf(Object, int)}
     * with a maximum depth equal to or greater than the actual depth.
     */
    @Test
    public void testAncestorAndDescendant() throws Exception {
        final Zone face = faces().get(0);
        final Zone child = firstChild(face);
        final Zone grandChild = firstChild(child);

        assertTrue (face.isAncestorOf(child, 1));
        assertTrue (face.isAncestorOf(child, 5));
        assertFalse(face.isAncestorOf(grandChild, 1));
        assertTrue (face.isAncestorOf(grandChild, 2));
        assertTrue (face.isAncestorOf(grandChild, 5));
        assertFalse(child.isAncestorOf(face, 5));
        assertTrue (grandChild.isDescendantOf(face, 5));
    }

    /**
     * Tests {@link AbstractZone#overlaps(Object)} in both directions, at different depths.
     */
    @Test
    public void testOverlaps() throws Exception {
        final List<Zone> faces = faces();
        final Zone face = faces.get(0);
        final Zone child = firstChild(face);
        final Zone grandChild = firstChild(child);
        final Zone otherChild = firstChild(faces.get(1));

        assertTrue (face.overlaps(child));
        assertTrue (child.overlaps(face));
        assertTrue (face.overlaps(grandChild));
        assertTrue (grandChild.overlaps(face));
        assertFalse(face.overlaps(face));
        assertFalse(face.overlaps(otherChild));
        assertFalse(otherChild.overlaps(face));
    }
}
