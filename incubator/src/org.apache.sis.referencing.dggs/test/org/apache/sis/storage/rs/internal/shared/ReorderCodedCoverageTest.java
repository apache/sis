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
import java.util.Optional;
import org.opengis.util.GenericName;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.measure.Units;
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
 * Tests {@link ReorderCodedCoverage}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class ReorderCodedCoverageTest {

    private static final GenericName NAME = Names.createLocalName(null, null, "test");

    /**
     * Creates a coverage with bands "a", "b" and "c", where "c" is in metres.
     * At position <var>p</var>, a = <var>p</var>, b = 10·<var>p</var> and c = 100·<var>p</var>.
     */
    private static ArrayCodedCoverage createSource() throws Exception {
        final DiscreteGlobalGridReferenceSystem dggrs = new S2Dggrs();
        final List<Object> zones = dggrs.getGridSystem().getHierarchy().getGrids().get(1).getZones().map(Zone::getIdentifier).toList();
        final ArrayCodedCoverage source = new ArrayCodedCoverage(NAME, DiscreteGlobalGridGeometry.unstructured(dggrs, zones, null),
                new SampleDimension.Builder().setName("a").build(),
                new SampleDimension.Builder().setName("b").build(),
                new SampleDimension.Builder().setName("c").addQuantitative("height", 0.0, 10000.0, Units.METRE).build());
        try (WritableCodeIterator iterator = source.createWritableIterator()) {
            while (iterator.next()) {
                final int p = iterator.getPosition()[0];
                iterator.setSample(0, p);
                iterator.setSample(1, 10.0 * p);
                iterator.setSample(2, 100.0 * p);
            }
        }
        return source;
    }

    /**
     * Tests selecting two bands in reverse order.
     */
    @Test
    public void testReorder() throws Exception {
        final ReorderCodedCoverage coverage = new ReorderCodedCoverage(NAME, createSource(), 2, 0);
        assertEquals(List.of("c", "a"), coverage.getSampleDimensions().stream().map((s) -> s.getName().toString()).toList());
        final CodeIterator iterator = coverage.createIterator();
        assertEquals(2, iterator.getNumBands());
        int count = 0;
        while (iterator.next()) {
            final int p = iterator.getPosition()[0];
            assertEquals(100.0 * p, iterator.getSampleDouble(0), "Band c");
            assertEquals(p,         iterator.getSampleDouble(1), "Band a");
            count++;
        }
        assertEquals(24, count);
    }

    /**
     * Tests renaming the selected bands. The units of the bands shall be kept.
     */
    @Test
    public void testRename() throws Exception {
        final GenericName[] names = {Names.createLocalName(null, null, "x"), Names.createLocalName(null, null, "y")};
        final ReorderCodedCoverage coverage = new ReorderCodedCoverage(NAME, createSource(), new int[] {2, 0}, names);
        final List<SampleDimension> bands = coverage.getSampleDimensions();
        assertEquals(List.of("x", "y"), bands.stream().map((s) -> s.getName().toString()).toList());
        assertEquals(Optional.of(Units.METRE), bands.get(0).getUnits());

        final CodeIterator iterator = coverage.createIterator();
        iterator.moveTo(new int[] {3});
        assertArrayEquals(new double[] {300, 3}, iterator.getCell((double[]) null));
    }

    /**
     * Tests that the number of names shall be the number of selected bands.
     */
    @Test
    public void testRenameMismatch() throws Exception {
        final ArrayCodedCoverage source = createSource();
        final GenericName[] names = {Names.createLocalName(null, null, "x")};
        assertThrows(IllegalArgumentException.class, () -> new ReorderCodedCoverage(NAME, source, new int[] {2, 0}, names));
    }

    /**
     * Tests selecting the same band twice.
     */
    @Test
    public void testDuplicatedBand() throws Exception {
        final CodeIterator iterator = new ReorderCodedCoverage(NAME, createSource(), 1, 1).createIterator();
        iterator.moveTo(new int[] {3});
        assertArrayEquals(new double[] {30, 30}, iterator.getCell((double[]) null));
    }

    /**
     * Tests that writing in the view writes in the selected band of the source.
     */
    @Test
    public void testWrite() throws Exception {
        final ArrayCodedCoverage source = createSource();
        try (WritableCodeIterator iterator = new ReorderCodedCoverage(NAME, source, 2, 0).createWritableIterator()) {
            iterator.moveTo(new int[] {3});
            iterator.setSample(0, -1);
        }
        final CodeIterator iterator = source.createIterator();
        iterator.moveTo(new int[] {3});
        assertArrayEquals(new double[] {3, 30, -1}, iterator.getCell((double[]) null));
    }
}
