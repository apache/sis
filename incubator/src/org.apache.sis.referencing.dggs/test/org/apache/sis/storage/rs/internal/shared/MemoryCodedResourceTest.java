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
import org.opengis.util.GenericName;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridHierarchy;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.dggs.Zone;
import org.apache.sis.referencing.dggs.s2.S2Dggrs;
import org.apache.sis.storage.dggs.DiscreteGlobalGridGeometry;
import org.apache.sis.storage.rs.CodeIterator;
import org.apache.sis.storage.rs.CodedCoverage;
import org.apache.sis.storage.rs.WritableCodeIterator;
import org.apache.sis.util.iso.Names;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link MemoryCodedResource}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class MemoryCodedResourceTest {

    private static final GenericName NAME = Names.createLocalName(null, null, "memory");

    private final DiscreteGlobalGridReferenceSystem dggrs = new S2Dggrs();
    private final DiscreteGlobalGridHierarchy hierarchy = dggrs.getGridSystem().getHierarchy();

    /**
     * Zones of the resource: the children of the first S2 face.
     */
    private List<Object> zones() throws Exception {
        return hierarchy.getGrids().get(0).getZones().findFirst().orElseThrow()
                .getChildren().stream().map(Zone::getIdentifier).toList();
    }

    /**
     * Creates a resource with bands "a" and "b". At position <var>p</var>, a = 10·<var>p</var> and b = 20·<var>p</var>.
     */
    private MemoryCodedResource createResource() throws Exception {
        final ArrayCodedCoverage coverage = new ArrayCodedCoverage(NAME,
                DiscreteGlobalGridGeometry.unstructured(dggrs, zones(), null),
                new SampleDimension.Builder().setName("a").build(),
                new SampleDimension.Builder().setName("b").build());
        try (WritableCodeIterator iterator = coverage.createWritableIterator()) {
            while (iterator.next()) {
                iterator.setSample(0, 10.0 * iterator.getPosition()[0]);
                iterator.setSample(1, 20.0 * iterator.getPosition()[0]);
            }
        }
        return new MemoryCodedResource(NAME, coverage);
    }

    /**
     * Tests reading one band on the whole resource geometry.
     */
    @Test
    public void testReadRange() throws Exception {
        final MemoryCodedResource resource = createResource();
        final CodedCoverage coverage = resource.read(resource.getGridGeometry(), 1);
        assertEquals(1, coverage.getSampleDimensions().size());
        assertEquals("b", coverage.getSampleDimensions().get(0).getName().toString());
        final CodeIterator iterator = coverage.createIterator();
        assertEquals(1, iterator.getNumBands());
        int count = 0;
        while (iterator.next()) {
            assertEquals(20.0 * iterator.getPosition()[0], iterator.getSampleDouble(0));
            count++;
        }
        assertEquals(4, count);
    }

    /**
     * Tests reading bands in reverse order on a subset of the resource zones.
     */
    @Test
    public void testReadRangeOnSubset() throws Exception {
        final MemoryCodedResource resource = createResource();
        final List<Object> zones = zones();
        final DiscreteGlobalGridGeometry query = DiscreteGlobalGridGeometry.unstructured(dggrs, List.of(zones.get(3), zones.get(1)), null);
        final CodedCoverage coverage = resource.read(query, 1, 0);
        assertEquals(2, coverage.getSampleDimensions().size());

        final List<Object> kept = ((DiscreteGlobalGridGeometry) coverage.getGeometry()).getZoneIds();
        final CodeIterator iterator = coverage.createIterator();
        int count = 0;
        while (iterator.next()) {
            final int source = zones.indexOf(kept.get(iterator.getPosition()[0]));
            assertEquals(20.0 * source, iterator.getSampleDouble(0), "Band b");
            assertEquals(10.0 * source, iterator.getSampleDouble(1), "Band a");
            count++;
        }
        assertEquals(2, count);
    }
}
