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
package org.apache.sis.storage.rs;

import java.util.ArrayList;
import java.util.List;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.coverage.grid.DisjointExtentException;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.coverage.grid.GridRoundingMode;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridHierarchy;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.dggs.Zone;
import org.apache.sis.referencing.dggs.s2.S2Dggrs;
import org.apache.sis.storage.dggs.DiscreteGlobalGridGeometry;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link CodedDerivation}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class CodedDerivationTest {

    private final DiscreteGlobalGridReferenceSystem dggrs = new S2Dggrs();
    private final DiscreteGlobalGridHierarchy hierarchy = dggrs.getGridSystem().getHierarchy();
    private final VerticalCRS verticalCrs = CommonCRS.Vertical.MEAN_SEA_LEVEL.crs();

    /**
     * Vertical grid of 3 cells from 0 to 300 metres.
     */
    private GridGeometry verticalGrid() {
        final GeneralEnvelope env = new GeneralEnvelope(verticalCrs);
        env.setRange(0, 0, 300);
        return new GridGeometry(new GridExtent(null, 0, 2, true), env, GridOrientation.HOMOTHETY);
    }

    private List<Object> zoneIds(int level) throws TransformException {
        return hierarchy.getGrids().get(level).getZones().map(Zone::getIdentifier).toList();
    }

    private Zone zoneAt(int level, double lon, double lat) throws TransformException {
        return hierarchy.getGrids().get(level).getZone(new DirectPosition2D(CommonCRS.WGS84.normalizedGeographic(), lon, lat));
    }

    /**
     * Tests that no derivation returns the base geometry.
     */
    @Test
    public void testNoChange() throws Exception {
        final CodedGeometry base = DiscreteGlobalGridGeometry.unstructured(dggrs, zoneIds(1), null);
        assertSame(base, base.derive().build());
    }

    /**
     * Tests that rounding can not be set after subgrid.
     */
    @Test
    public void testRoundingAfterSubgrid() throws Exception {
        final CodedGeometry base = new CodedGeometry(verticalGrid());
        final CodedDerivation derivation = base.derive().subgrid(new CodedGeometry(verticalGrid()));
        assertThrows(IllegalStateException.class, () -> derivation.rounding(GridRoundingMode.ENCLOSING));
    }

    /**
     * Tests subgrid with an envelope covering both the DGGRS and the vertical components.
     */
    @Test
    public void testSubgridEnvelope() throws Exception {
        final List<Object> zones = zoneIds(1);
        final CodedGeometry base = CodedGeometry.compound(
                DiscreteGlobalGridGeometry.unstructured(dggrs, zones, null),
                new CodedGeometry(verticalGrid()));

        final GeneralEnvelope aoi = new GeneralEnvelope(CRS.compound(CommonCRS.WGS84.normalizedGeographic(), verticalCrs));
        aoi.setRange(0, 1, 10);
        aoi.setRange(1, 1, 10);
        aoi.setRange(2, 120, 180);

        final CodedGeometry result = base.derive().rounding(GridRoundingMode.ENCLOSING).subgrid(aoi).build();

        final List<Object> kept = ((DiscreteGlobalGridGeometry) result.slice(dggrs).get()).getZoneIds();
        assertTrue(kept.contains(zoneAt(1, 5, 5).getIdentifier()));
        assertFalse(kept.contains(zoneAt(1, -100, -40).getIdentifier()));
        assertTrue(zones.containsAll(kept));

        final GridExtent vertical = result.slice(verticalCrs).get().getExtent();
        assertEquals(1, vertical.getLow(0));
        assertEquals(1, vertical.getHigh(0));
    }

    /**
     * Tests subgrid with an envelope which does not intersect the vertical component.
     */
    @Test
    public void testSubgridEnvelopeDisjoint() throws Exception {
        final CodedGeometry base = CodedGeometry.compound(
                DiscreteGlobalGridGeometry.unstructured(dggrs, zoneIds(1), null),
                new CodedGeometry(verticalGrid()));

        final GeneralEnvelope aoi = new GeneralEnvelope(verticalCrs);
        aoi.setRange(0, 1000, 2000);
        assertThrows(DisjointExtentException.class, () -> base.derive().subgrid(aoi));
    }

    /**
     * Tests subgrid with an area of interest using the same DGGRS at a finer level.
     */
    @Test
    public void testSubgridFinerZones() throws Exception {
        final CodedGeometry base = DiscreteGlobalGridGeometry.unstructured(dggrs, zoneIds(1), null);
        final Zone parent = zoneAt(1, 5, 5);
        final List<Object> children = parent.getChildrenAtRelativeDepth(2).map(Zone::getIdentifier).toList();
        final CodedGeometry aoi = DiscreteGlobalGridGeometry.unstructured(dggrs, children, null);

        final DiscreteGlobalGridGeometry result = (DiscreteGlobalGridGeometry) base.derive().subgrid(aoi).build();
        assertEquals(List.of(parent.getIdentifier()), new ArrayList<>(result.getZoneIds()));
    }

    /**
     * Tests subgrid with an area of interest using the same DGGRS at a coarser level.
     */
    @Test
    public void testSubgridCoarserZones() throws Exception {
        final CodedGeometry base = DiscreteGlobalGridGeometry.unstructured(dggrs, zoneIds(2), null);
        final Zone face = zoneAt(0, 5, 5);
        final CodedGeometry aoi = DiscreteGlobalGridGeometry.unstructured(dggrs, List.of(face.getIdentifier()), null);

        final DiscreteGlobalGridGeometry result = (DiscreteGlobalGridGeometry) base.derive().subgrid(aoi).build();
        final List<Object> kept = result.getZoneIds();
        assertEquals(16, kept.size());
        for (Object zid : kept) {
            assertEquals(face.getLongIdentifier(), hierarchy.getZone(zid).getFirstParent(0).getLongIdentifier());
        }
    }

    /**
     * Tests that a base without zones accepts the area of interest zones.
     */
    @Test
    public void testSubgridUndefinedZones() throws Exception {
        final CodedGeometry base = DiscreteGlobalGridGeometry.unstructured(dggrs, (List<Object>) null, null);
        final CodedGeometry aoi = DiscreteGlobalGridGeometry.unstructured(dggrs, zoneIds(2), null);
        assertSame(aoi, base.derive().subgrid(aoi).build());
    }

    /**
     * Tests that a base without zones computes zones from a regular grid area of interest.
     */
    @Test
    public void testSubgridUndefinedZonesFromGrid() throws Exception {
        final CodedGeometry base = DiscreteGlobalGridGeometry.unstructured(dggrs, (List<Object>) null, null);
        final GeneralEnvelope env = new GeneralEnvelope(CommonCRS.WGS84.normalizedGeographic());
        env.setRange(0, 0, 10);
        env.setRange(1, 0, 10);
        final CodedGeometry aoi = new CodedGeometry(new GridGeometry(new GridExtent(10, 10), env, GridOrientation.HOMOTHETY));

        final DiscreteGlobalGridGeometry result = (DiscreteGlobalGridGeometry) base.derive().subgrid(aoi).build();
        final List<Object> zones = result.getZoneIds();
        assertFalse(zones.isEmpty());
        assertTrue(zones.contains(zoneAt(result.getRefinementLevel(), 5, 5).getIdentifier()));
    }

    /**
     * Tests that a component without extent accepts the area of interest cells.
     */
    @Test
    public void testSubgridUndefinedGrid() throws Exception {
        final CodedGeometry base = new CodedGeometry(new GridGeometry(null, null, null, verticalCrs));
        final CodedGeometry aoi = new CodedGeometry(verticalGrid());
        final CodedGeometry result = base.derive().subgrid(aoi).build();
        assertEquals(verticalCrs, result.getReferenceSystem());
        assertEquals(aoi.getExtent(), result.getExtent());
        assertEquals(aoi.getGridToRS(), result.getGridToRS());
    }

    /**
     * Tests that a base without reference system accepts any area of interest.
     */
    @Test
    public void testSubgridUndefinedBase() throws Exception {
        final CodedGeometry base = new CodedGeometry(null, null, null, null);
        final CodedGeometry aoi = new CodedGeometry(verticalGrid());
        assertSame(aoi, base.derive().subgrid(aoi).build());
    }

    /**
     * Tests subgrid with an area of interest covering only the vertical component.
     * The DGGRS component must be unchanged.
     */
    @Test
    public void testSubgridOtherComponent() throws Exception {
        final DiscreteGlobalGridGeometry dggs = DiscreteGlobalGridGeometry.unstructured(dggrs, zoneIds(1), null);
        final CodedGeometry base = CodedGeometry.compound(dggs, new CodedGeometry(verticalGrid()));
        final GridGeometry aoi = verticalGrid().derive().subgrid(new GridExtent(null, 2, 2, true)).build();

        final CodedGeometry result = base.derive().subgrid(new CodedGeometry(aoi)).build();
        assertSame(dggs, result.slice(dggrs).get());

        final GridExtent vertical = result.slice(verticalCrs).get().getExtent();
        assertEquals(2, vertical.getLow(0));
        assertEquals(2, vertical.getHigh(0));
    }
}
