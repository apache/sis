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
package org.apache.sis.storage.rs.internal.shared;

import java.util.List;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.GenericName;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridHierarchy;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.dggs.Zone;
import org.apache.sis.referencing.dggs.s2.S2Dggrs;
import org.apache.sis.storage.NoSuchDataException;
import org.apache.sis.storage.dggs.DiscreteGlobalGridGeometry;
import org.apache.sis.storage.rs.CodeIterator;
import org.apache.sis.storage.rs.CodedCoverage;
import org.apache.sis.storage.rs.WritableCodeIterator;
import org.apache.sis.util.iso.Names;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link CodedResources} through {@link MemoryCodedResource}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class CodedResourcesTest {

    private static final GenericName NAME = Names.createLocalName(null, null, "memory");

    private final DiscreteGlobalGridReferenceSystem dggrs = new S2Dggrs();
    private final DiscreteGlobalGridHierarchy hierarchy = dggrs.getGridSystem().getHierarchy();

    private Zone zoneAt(int level, double lon, double lat) throws TransformException {
        return hierarchy.getGrids().get(level).getZone(new DirectPosition2D(CommonCRS.WGS84.normalizedGeographic(), lon, lat));
    }

    /**
     * Creates a resource on given zones, the cell value is the zone index.
     */
    private MemoryCodedResource createResource(List<Object> zones) throws Exception {
        final ArrayCodedCoverage coverage = new ArrayCodedCoverage(NAME,
                DiscreteGlobalGridGeometry.unstructured(dggrs, zones, null),
                new SampleDimension.Builder().setName("index").build());
        try (WritableCodeIterator iterator = coverage.createWritableIterator()) {
            while (iterator.next()) {
                iterator.setSample(0, (double) iterator.getPosition()[0]);
            }
        }
        return new MemoryCodedResource(NAME, coverage);
    }

    /**
     * Tests reading with the exact resource geometry returns the coverage.
     */
    @Test
    public void testReadExact() throws Exception {
        final List<Object> zones = hierarchy.getGrids().get(1).getZones().map(Zone::getIdentifier).toList();
        final MemoryCodedResource resource = createResource(zones);
        final CodedCoverage coverage = resource.read(resource.getGridGeometry());
        assertSame(resource.getGridGeometry(), coverage.getGeometry());
    }

    /**
     * Tests reading with a geometry which does not match the resource geometry,
     * the result must be aligned on the resource zones.
     */
    @Test
    public void testReadMisaligned() throws Exception {
        final List<Object> zones = hierarchy.getGrids().get(1).getZones().map(Zone::getIdentifier).toList();
        final MemoryCodedResource resource = createResource(zones);

        //query a level 0 zone, the 4 level 1 children are expected
        final Zone face = zoneAt(0, 5, 5);
        final CodedCoverage coverage = resource.read(DiscreteGlobalGridGeometry.unstructured(dggrs, List.of(face.getIdentifier()), null));
        final List<Object> kept = ((DiscreteGlobalGridGeometry) coverage.getGeometry()).getZoneIds();
        assertEquals(4, kept.size());

        final CodeIterator iterator = coverage.createIterator();
        int count = 0;
        while (iterator.next()) {
            final Object zid = kept.get(iterator.getPosition()[0]);
            assertEquals(face.getLongIdentifier(), hierarchy.getZone(zid).getFirstParent(0).getLongIdentifier());
            assertEquals(zones.indexOf(zid), iterator.getSampleDouble(0), 0.0);
            count++;
        }
        assertEquals(4, count);
    }

    /**
     * Tests reading with a geometry which does not intersect the resource.
     */
    @Test
    public void testReadDisjoint() throws Exception {
        final Zone face = zoneAt(0, 5, 5);
        final List<Object> zones = face.getChildrenAtRelativeDepth(1).map(Zone::getIdentifier).toList();
        final MemoryCodedResource resource = createResource(zones);

        final Zone other = zoneAt(1, 179, -60);
        assertNotEquals(face.getLongIdentifier(), other.getFirstParent(0).getLongIdentifier());
        assertThrows(NoSuchDataException.class,
                () -> resource.read(DiscreteGlobalGridGeometry.unstructured(dggrs, List.of(other.getIdentifier()), null)));
    }
}
