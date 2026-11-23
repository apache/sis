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
import org.apache.sis.measure.NumberRange;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.dggs.Zone;
import org.apache.sis.referencing.dggs.s2.S2Dggrs;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.dggs.DiscreteGlobalGridGeometry;
import org.apache.sis.storage.rs.CodeIterator;
import org.apache.sis.storage.rs.CodedCoverage;
import org.apache.sis.storage.rs.WritableCodeIterator;
import org.apache.sis.util.iso.Names;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link TiledDiscreteGlobalGridCoverageResource}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class TiledDiscreteGlobalGridCoverageResourceTest {

    private static final List<SampleDimension> BANDS = List.of(
            new SampleDimension.Builder().setName("a").build(),
            new SampleDimension.Builder().setName("b").build());

    private final DiscreteGlobalGridReferenceSystem dggrs = new S2Dggrs();

    /**
     * Tiles on the S2 faces, with the face children as data zones.
     * At position <var>p</var> of a tile, a = 10·<var>p</var> and b = 20·<var>p</var>.
     */
    private final class FaceTiles extends TiledDiscreteGlobalGridCoverageResource {
        @Override
        public NumberRange<Integer> getTileAvailableDepths() {
            return NumberRange.create(0, true, 0, true);
        }

        @Override
        public int getTileRelativeDepth() {
            return 1;
        }

        @Override
        public CodedCoverage getZoneTile(Object identifierOrZone) throws DataStoreException {
            final Object id = (identifierOrZone instanceof Zone z) ? z.getIdentifier() : identifierOrZone;
            final ArrayDiscreteGlobalGridCoverage tile = new ArrayDiscreteGlobalGridCoverage(Names.createLocalName(null, null, "tile"),
                    DiscreteGlobalGridGeometry.subZone(dggrs, id, 1), BANDS.toArray(SampleDimension[]::new));
            try (WritableCodeIterator iterator = tile.createWritableIterator()) {
                while (iterator.next()) {
                    iterator.setSample(0, 10.0 * iterator.getPosition()[0]);
                    iterator.setSample(1, 20.0 * iterator.getPosition()[0]);
                }
            }
            return tile;
        }

        @Override
        public DiscreteGlobalGridGeometry getGridGeometry() {
            return DiscreteGlobalGridGeometry.unstructured(dggrs, null, null);
        }

        @Override
        public List<SampleDimension> getSampleDimensions() {
            return BANDS;
        }
    }

    /**
     * Tests reading one band of the zones of one tile.
     */
    @Test
    public void testReadRange() throws Exception {
        final Zone face = dggrs.getGridSystem().getHierarchy().getGrids().get(0).getZones().findFirst().orElseThrow();
        final CodedCoverage coverage = new FaceTiles().read(DiscreteGlobalGridGeometry.subZone(dggrs, face.getIdentifier(), 1), 1);
        assertEquals(1, coverage.getSampleDimensions().size());
        assertEquals("b", coverage.getSampleDimensions().get(0).getName().toString());
        final CodeIterator iterator = coverage.createIterator();
        int count = 0;
        while (iterator.next()) {
            assertEquals(20.0 * iterator.getPosition()[0], iterator.getSampleDouble(0));
            count++;
        }
        assertEquals(4, count);
    }
}
