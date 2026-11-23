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
import java.util.stream.Stream;
import javax.measure.IncommensurableException;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.dggs.Zone;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.apache.sis.referencing.dggs.AbstractDggrsTest;


/**
 * Tests {@link SmcDggrs} on the global lattice.
 *
 * @author Johann Sorel (Geomatys)
 */
public class SmcTest extends AbstractDggrsTest {

    private static final SmcDggrs SMC = new SmcDggrs();

    public SmcTest() {
        super(SMC);
    }

    private static SmcDgg grid(final int level) {
        return SMC.dggs.dggh.get(level);
    }

    /**
     * Levels 7 and 9 have the cell sizes of the SMC6-25 grid.
     */
    @Test
    public void testIncrements() {
        assertEquals(0.3515625,   grid(7).longitudeIncrement);
        assertEquals(0.234375,    grid(7).latitudeIncrement);
        assertEquals(0.087890625, grid(9).longitudeIncrement);
        assertEquals(0.05859375,  grid(9).latitudeIncrement);
        assertEquals(1024, grid(7).columnCount);
        assertEquals(768,  grid(7).rowCount);
        assertEquals(27,   SMC.dggs.dggh.size());
    }

    /**
     * At level 7, the polar cells are surrounded by 8 cells.
     */
    @Test
    public void testPolarRing() {
        final SmcDgg grid = grid(7);
        assertTrue(grid.isPolarRow(0));
        assertTrue(grid.isPolarRow(grid.rowCount - 1));
        assertFalse(grid.isPolarRow(1));
        assertEquals(8, grid.columnCount / grid.mergeFactor(grid.rowCount - 2));
        assertEquals(8, grid.columnCount / grid.mergeFactor(1));

        final SmcZone north = grid.zoneAt(0, 90);
        assertTrue(north.isNorthPolar());
        assertEquals(8, north.getNeighbors().size());
        final SmcZone south = grid.zoneAt(0, -90);
        assertTrue(south.isPolar());
        assertFalse(south.isNorthPolar());
        assertEquals(8, south.getNeighbors().size());
    }

    /**
     * At level 7, rows between the 3rd (82.8°) and 4th (86.4°) size-changing parallels hold 128 cells,
     * and no row is merged up to 60°.
     */
    @Test
    public void testMergeFactors() {
        final SmcDgg grid = grid(7);
        assertEquals(128, grid.columnCount / grid.zoneAt(0,  84).mergeFactor());
        assertEquals(128, grid.columnCount / grid.zoneAt(0, -84).mergeFactor());
        assertEquals(64,  grid.columnCount / grid.zoneAt(0,  87).mergeFactor());

        final SmcZone below60 = grid.zoneAt(0, 59.9);
        assertEquals(60, below60.north());
        assertEquals(1, below60.mergeFactor());
        assertEquals(2, grid.zoneAt(0, 60.1).mergeFactor());
        for (long row = 0; row < grid.rowCount; row++) {
            if (Math.max(Math.abs(grid.latitude(row)), Math.abs(grid.latitude(row + 1))) <= 60) {
                assertEquals(1, grid.mergeFactor(row));
            }
        }
    }

    /**
     * Every non-polar row keeps at least 2 cells and a whole number of cells.
     */
    @Test
    public void testRowCellCounts() {
        for (int level = 0; level <= 10; level++) {
            final SmcDgg grid = grid(level);
            for (long row = 0; row < grid.rowCount; row++) {
                final long merge = grid.mergeFactor(row);
                assertEquals(0, grid.columnCount % merge);
                if (!grid.isPolarRow(row)) {
                    assertTrue(grid.columnCount / merge >= 2);
                }
            }
        }
    }

    /**
     * Children exactly cover their parent, including merged and polar cells.
     */
    @Test
    public void testChildrenCoverParent() {
        final List<SmcZone> zones = new ArrayList<>();
        for (int level = 0; level <= 2; level++) {
            zones.addAll(allZones(level));
        }
        zones.addAll(rowZones(7, grid(7).rowCount - 1));
        zones.addAll(rowZones(7, grid(7).rowCount - 2));
        zones.add(grid(6).zoneAt(10, 60.1));
        zones.add(grid(5).zoneAt(10, 75.2));     // Parallel 75.52° crosses the upper child row only.
        zones.add(grid(6).zoneAt(10, -82.9));
        for (SmcZone zone : zones) {
            final List<SmcZone> children = zone.getChildren();
            assertTrue(children.size() >= 2, zone::toString);
            double area = 0;
            for (SmcZone child : children) {
                assertEquals(zone, child.getFirstParent());
                assertTrue(child.south() >= zone.south() && child.north() <= zone.north(), child::toString);
                assertTrue(child.west()  >= zone.west()  && child.east()  <= zone.east(),  child::toString);
                area += child.getArea().orElseThrow().getValue().doubleValue();
            }
            final double expected = zone.getArea().orElseThrow().getValue().doubleValue();
            assertEquals(expected, area, 1e-12 * expected, zone::toString);
        }
    }

    /**
     * Neighborhood is symmetric, across the seam, merge boundaries and polar rings.
     */
    @Test
    public void testNeighborSymmetry() {
        final List<SmcZone> zones = new ArrayList<>(allZones(2));
        for (long row : new long[] {0, 1, 2, 383, 384, 640, 641, 765, 766, 767}) {
            zones.addAll(rowZones(7, row));
        }
        for (SmcZone zone : zones) {
            for (SmcZone neighbor : zone.getNeighbors()) {
                assertTrue(neighbor.getNeighbors().contains(zone), () -> zone + " is not a neighbor of " + neighbor);
            }
        }
        final SmcDgg grid = grid(2);
        assertTrue(grid.zone(10, 0).getNeighbors().contains(grid.zone(10, grid.columnCount - 1)));
    }

    /**
     * The zone count computed per merge band equals the number of enumerated zones.
     */
    @Test
    public void testZoneCount() throws TransformException {
        assertEquals(34, grid(0).getZoneCount());
        for (int level = 0; level <= 4; level++) {
            try (Stream<Zone> zones = grid(level).getZones()) {
                assertEquals(grid(level).getZoneCount(), zones.count());
            }
        }
    }

    /**
     * Tests the long and text identifiers.
     */
    @Test
    public void testIdentifiers() throws Exception {
        final DiscreteGlobalGridReferenceSystem.Coder coder = SMC.createCoder();
        coder.setPrecisionLevel(7);
        assertEquals("7-0-384", coder.encode(new DirectPosition2D(CommonCRS.WGS84.normalizedGeographic(), 0.1, 0.1)));
        assertEquals("7-0-767", coder.encode(new DirectPosition2D(CommonCRS.WGS84.normalizedGeographic(), 45, 90)));

        final SmcDggh hierarchy = SMC.dggs.dggh;
        final SmcDgg finest = grid(SMC.dggs.dggh.size() - 1);
        final List<SmcZone> zones = new ArrayList<>(allZones(1));
        zones.add(finest.zoneAt(179.9, 89.9));
        zones.add(finest.zoneAt(-0.1, -89.99));
        zones.add(finest.zoneAt(0, 90));
        for (SmcZone zone : zones) {
            final long id = zone.getLongIdentifier();
            assertTrue(id >= 0);
            assertEquals(zone, hierarchy.decode(id));
            assertEquals(zone, hierarchy.parse(zone.getTextIdentifier()));
        }
        assertThrows(IllegalArgumentException.class, () -> hierarchy.parse("7-8-767"));
        assertThrows(IllegalArgumentException.class, () -> hierarchy.parse("7-0"));
        assertThrows(IllegalArgumentException.class, () -> hierarchy.parse("a-b-c"));
        assertThrows(IllegalArgumentException.class, () -> hierarchy.parse("27-0-0"));
        assertThrows(IllegalArgumentException.class, () -> hierarchy.decode(-1));
    }

    /**
     * Polar envelopes reach the pole and cells ending on the anti-meridian end at 180°.
     */
    @Test
    public void testEnvelopes() {
        final SmcZone polar = grid(3).zoneAt(0, 90);
        assertEquals(SmcZone.POLAR_CAP, polar.getShapeType());
        Envelope envelope = polar.getEnvelope();
        assertEquals(90,   envelope.getMaximum(1));
        assertEquals(-180, envelope.getMinimum(0));
        assertEquals(180,  envelope.getMaximum(0));

        envelope = grid(0).zoneAt(170, 10).getEnvelope();
        assertEquals(135, envelope.getMinimum(0));
        assertEquals(180, envelope.getMaximum(0));
        assertEquals(0,   envelope.getMinimum(1));
        assertEquals(30,  envelope.getMaximum(1));
    }

    /**
     * Enabled for SMC, which passes the spatial search test disabled for other reference systems.
     */
    @Test
    @Override
    public void testSearchEnvelope() throws TransformException, IncommensurableException {
        super.testSearchEnvelope();
    }

    /**
     * Enabled for SMC, which passes the cell geometry test disabled for other reference systems.
     */
    @Test
    @Override
    public void testPickingInCellGeometry() throws TransformException, IncommensurableException {
        super.testPickingInCellGeometry();
    }

    /**
     * The precision of level 7 is its cell height, about 25 km.
     */
    @Test
    public void testPrecision() {
        assertEquals(26000, grid(7).getPrecision().getValue().doubleValue(), 500);
    }

    private static List<SmcZone> allZones(final int level) {
        final List<SmcZone> zones = new ArrayList<>();
        for (long row = 0; row < grid(level).rowCount; row++) {
            zones.addAll(rowZones(level, row));
        }
        return zones;
    }

    private static List<SmcZone> rowZones(final int level, final long row) {
        final List<SmcZone> zones = new ArrayList<>();
        grid(level).addZones(zones, row, 0, grid(level).columnCount);
        return zones;
    }
}
