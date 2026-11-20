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
package org.apache.sis.storage.dggs.internal.shared;

import java.util.List;
import org.opengis.util.GenericName;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.dggs.Zone;
import org.apache.sis.referencing.dggs.s2.S2Dggrs;
import org.apache.sis.storage.dggs.DiscreteGlobalGridGeometry;
import org.apache.sis.storage.rs.CodedCoverage;
import org.apache.sis.util.iso.Names;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link TiledDiscreteGlobalGridCoverage}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class TiledDiscreteGlobalGridCoverageTest {

    private static final GenericName NAME = Names.createLocalName(null, null, "tile");

    private static final SampleDimension BAND = new SampleDimension.Builder().setName("value").build();

    /**
     * Creates a tile on the children of the given zone at the given relative depth.
     */
    private static CodedCoverage tile(DiscreteGlobalGridReferenceSystem dggrs, Zone base, int relativeDepth) {
        return new ArrayDiscreteGlobalGridCoverage(NAME, DiscreteGlobalGridGeometry.subZone(dggrs, base.getIdentifier(), relativeDepth), BAND);
    }

    /**
     * Tests aggregating tiles which use two equal, but distinct, reference system instances.
     */
    @Test
    public void testEqualReferenceSystems() throws Exception {
        final DiscreteGlobalGridReferenceSystem dggrs1 = new S2Dggrs();
        final DiscreteGlobalGridReferenceSystem dggrs2 = new S2Dggrs();
        assertNotSame(dggrs1, dggrs2);
        assertEquals(dggrs1, dggrs2);
        final List<Zone> faces = dggrs1.getGridSystem().getHierarchy().getGrids().get(0).getZones().limit(2).toList();

        final TiledDiscreteGlobalGridCoverage coverage = new TiledDiscreteGlobalGridCoverage(new CodedCoverage[] {
            tile(dggrs1, faces.get(0), 1),
            tile(dggrs2, faces.get(1), 1)
        });
        assertEquals(8, ((DiscreteGlobalGridGeometry) coverage.getGeometry()).getZoneIds().size());
    }

    /**
     * Tests that tiles of different relative depths are rejected.
     */
    @Test
    public void testDifferentRelativeDepths() throws Exception {
        final DiscreteGlobalGridReferenceSystem dggrs = new S2Dggrs();
        final List<Zone> faces = dggrs.getGridSystem().getHierarchy().getGrids().get(0).getZones().limit(2).toList();
        assertThrows(IllegalArgumentException.class, () -> new TiledDiscreteGlobalGridCoverage(new CodedCoverage[] {
            tile(dggrs, faces.get(0), 1),
            tile(dggrs, faces.get(1), 2)
        }));
    }
}
