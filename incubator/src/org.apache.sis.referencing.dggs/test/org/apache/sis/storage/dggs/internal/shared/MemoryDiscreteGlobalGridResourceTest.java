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
import org.apache.sis.coverage.SampleDimension;
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
 * Tests {@link MemoryDiscreteGlobalGridResource}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class MemoryDiscreteGlobalGridResourceTest {

    /**
     * Tests reading one band of a coverage with bands "a" and "b".
     */
    @Test
    public void testReadRange() throws Exception {
        final DiscreteGlobalGridReferenceSystem dggrs = new S2Dggrs();
        final List<Object> zones = dggrs.getGridSystem().getHierarchy().getGrids().get(1).getZones().map(Zone::getIdentifier).toList();
        final ArrayDiscreteGlobalGridCoverage coverage = new ArrayDiscreteGlobalGridCoverage(Names.createLocalName(null, null, "test"),
                DiscreteGlobalGridGeometry.unstructured(dggrs, zones, null),
                new SampleDimension.Builder().setName("a").build(),
                new SampleDimension.Builder().setName("b").build());
        try (WritableCodeIterator iterator = coverage.createWritableIterator()) {
            while (iterator.next()) {
                iterator.setSample(0, 10.0 * iterator.getPosition()[0]);
                iterator.setSample(1, 20.0 * iterator.getPosition()[0]);
            }
        }
        final MemoryDiscreteGlobalGridResource resource = new MemoryDiscreteGlobalGridResource(coverage);
        final CodedCoverage result = resource.read(resource.getGridGeometry(), 1);
        assertEquals(1, result.getSampleDimensions().size());
        assertEquals("b", result.getSampleDimensions().get(0).getName().toString());
        final CodeIterator iterator = result.createIterator();
        while (iterator.next()) {
            assertEquals(20.0 * iterator.getPosition()[0], iterator.getSampleDouble(0));
        }
    }
}
