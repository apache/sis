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
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.dggs.Zone;
import org.apache.sis.referencing.dggs.s2.S2Dggrs;
import org.apache.sis.storage.dggs.DiscreteGlobalGridGeometry;
import org.apache.sis.storage.rs.CodeIterator;
import org.apache.sis.storage.rs.WritableCodeIterator;
import org.apache.sis.util.iso.Names;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link AddDimCodedCoverage}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class AddDimCodedCoverageTest {

    private static final GenericName NAME = Names.createLocalName(null, null, "added");

    /**
     * Creates a coverage on the S2 level 0 zones, with an added vertical dimension of one cell.
     * The cell value is the zone index multiplied by 10.
     */
    private static AddDimCodedCoverage createCoverage() throws Exception {
        final DiscreteGlobalGridReferenceSystem dggrs = new S2Dggrs();
        final List<Object> zones = dggrs.getGridSystem().getHierarchy().getGrids().get(0).getZones().map(Zone::getIdentifier).toList();
        final ArrayCodedCoverage source = new ArrayCodedCoverage(NAME,
                DiscreteGlobalGridGeometry.unstructured(dggrs, zones, null),
                new SampleDimension.Builder().setName("value").build());
        try (WritableCodeIterator iterator = source.createWritableIterator()) {
            while (iterator.next()) {
                iterator.setSample(0, 10.0 * iterator.getPosition()[0]);
            }
        }
        final GeneralEnvelope vertical = new GeneralEnvelope(CommonCRS.Vertical.ELLIPSOIDAL.crs());
        vertical.setRange(0, 0, 100);
        final GridGeometry slice = new GridGeometry(new GridExtent(null, 0, 0, true), vertical, GridOrientation.HOMOTHETY);
        return new AddDimCodedCoverage(source, slice);
    }

    /**
     * Tests that moving to a position, including the added dimension, reads the cell at that position.
     */
    @Test
    public void testMoveTo() throws Exception {
        final CodeIterator iterator = createCoverage().createIterator();
        iterator.moveTo(new int[] {2, 0});
        assertEquals(20.0, iterator.getSampleDouble(0));
        assertArrayEquals(new int[] {2, 0}, iterator.getPosition());
    }

    /**
     * Tests that a position outside the added dimension is rejected.
     */
    @Test
    public void testMoveToOutsideAddedDimension() throws Exception {
        final CodeIterator iterator = createCoverage().createIterator();
        assertThrows(IllegalArgumentException.class, () -> iterator.moveTo(new int[] {2, 1}));
    }
}
