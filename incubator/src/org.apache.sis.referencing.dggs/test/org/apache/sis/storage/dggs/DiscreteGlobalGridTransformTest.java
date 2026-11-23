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
package org.apache.sis.storage.dggs;

import java.util.List;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.dggs.Zone;
import org.apache.sis.referencing.dggs.s2.S2Dggrs;
import org.apache.sis.referencing.dggs.smc.SmcDggrs;
import org.apache.sis.referencing.rs.Code;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link DiscreteGlobalGridTransform}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class DiscreteGlobalGridTransformTest {

    private final DiscreteGlobalGridReferenceSystem dggrs = new S2Dggrs();

    /**
     * Creates a transform on the children of the first two S2 faces, 4 zones per face.
     */
    private DiscreteGlobalGridTransform createSubZones() throws Exception {
        final List<Zone> faces = dggrs.getGridSystem().getHierarchy().getGrids().get(0).getZones().limit(2).toList();
        final Object[] bases = {faces.get(0).getIdentifier(), faces.get(1).getIdentifier()};
        return DiscreteGlobalGridTransform.subZones(dggrs, bases, 1);
    }

    /**
     * Tests that {@code toGrid} returns the position in the whole grid, including for the zones of the second tile.
     */
    @Test
    public void testSubZonesToGrid() throws Exception {
        final DiscreteGlobalGridTransform transform = createSubZones();
        final List<Object> zoneIds = transform.getZoneIds();
        assertEquals(8, zoneIds.size());
        for (int i = 0; i < 8; i++) {
            assertEquals(i, transform.toGrid(new Code(dggrs, zoneIds.get(i)))[0], "Grid position");
        }
    }

    /**
     * Tests that transforms on the same zone identifiers are equal only if they use the same reference system.
     * The two SMC systems use the same identifiers, but the second one has a rotated pole.
     */
    @Test
    public void testEqualsComparesReferenceSystem() throws Exception {
        final DiscreteGlobalGridReferenceSystem smc = new SmcDggrs();
        final DiscreteGlobalGridReferenceSystem rotated = new SmcDggrs("SMC rotated", null, 0, -90, 45, 30, 8, 6, 10, 135);
        assertNotEquals(smc, rotated);
        final Object[] ids = smc.getGridSystem().getHierarchy().getGrids().get(0).getZones().limit(2).map(Zone::getIdentifier).toArray();

        assertEquals   (DiscreteGlobalGridTransform.unstructured(smc, ids), DiscreteGlobalGridTransform.unstructured(smc, ids));
        assertNotEquals(DiscreteGlobalGridTransform.unstructured(smc, ids), DiscreteGlobalGridTransform.unstructured(rotated, ids));

        assertEquals   (DiscreteGlobalGridTransform.subZone(smc, ids[0], 1), DiscreteGlobalGridTransform.subZone(smc, ids[0], 1));
        assertEquals   (DiscreteGlobalGridTransform.subZone(smc, ids[0], 1).hashCode(), DiscreteGlobalGridTransform.subZone(smc, ids[0], 1).hashCode());
        assertNotEquals(DiscreteGlobalGridTransform.subZone(smc, ids[0], 1), DiscreteGlobalGridTransform.subZone(rotated, ids[0], 1));

        assertEquals   (DiscreteGlobalGridTransform.subZones(smc, ids, 1), DiscreteGlobalGridTransform.subZones(smc, ids, 1));
        assertNotEquals(DiscreteGlobalGridTransform.subZones(smc, ids, 1), DiscreteGlobalGridTransform.subZones(rotated, ids, 1));
    }

    /**
     * Tests {@code toAddress} when the zone identifier is not the first ordinate.
     */
    @Test
    public void testSubZonesToAddressAtOffset() throws Exception {
        final DiscreteGlobalGridTransform transform = createSubZones();
        final Object[] location = new Object[2];
        transform.toAddress(new int[] {99, 5}, location, 1);
        assertNull(location[0]);
        assertEquals(transform.getZoneIds().get(5), location[1]);
    }
}
