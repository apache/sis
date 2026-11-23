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
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.dggs.Zone;
import org.apache.sis.referencing.dggs.s2.S2Dggrs;
import org.apache.sis.storage.dggs.DiscreteGlobalGridGeometry;
import org.apache.sis.storage.rs.CodeIterator;
import org.apache.sis.storage.rs.CodedCoverage;
import org.apache.sis.storage.rs.CodedGeometry;
import org.apache.sis.storage.rs.WritableCodeIterator;
import org.apache.sis.util.iso.Names;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link CompoundCodedCoverage}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class CompoundCodedCoverageTest {

    private static final GenericName NAME = Names.createLocalName(null, null, "test");

    private static final SampleDimension BAND = new SampleDimension.Builder().setName("value").build();

    /**
     * A coverage whose value is the grid position, iterated from the last position to the first one.
     */
    private static final class ReverseCoverage extends AbstractCodedCoverage {
        ReverseCoverage(final CodedGeometry geometry) throws FactoryException {
            super(NAME, geometry, List.of(BAND));
        }

        @Override
        public CodeIterator createIterator() {
            final int size = Math.toIntExact(extent.getSize(0));
            return new CodeIterator() {
                private int position = size;
                @Override public int[]   getPosition()             {return new int[] {position};}
                @Override public void    moveTo(int[] zid)         {position = zid[0];}
                @Override public boolean next()                    {return --position >= 0;}
                @Override public int     getNumBands()             {return 1;}
                @Override public double  getSampleDouble(int band) {return position;}
                @Override public void    rewind()                  {position = size;}
            };
        }

        @Override
        public WritableCodeIterator createWritableIterator() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Tests iterating over coverages which do not iterate in the same order.
     * Each cell shall combine the values of all coverages at the same position.
     */
    @Test
    public void testIterationOrder() throws Exception {
        final DiscreteGlobalGridReferenceSystem dggrs = new S2Dggrs();
        final List<Object> zones = dggrs.getGridSystem().getHierarchy().getGrids().get(1).getZones().map(Zone::getIdentifier).toList();
        final DiscreteGlobalGridGeometry geometry = DiscreteGlobalGridGeometry.unstructured(dggrs, zones, null);

        final ArrayCodedCoverage forward = new ArrayCodedCoverage(NAME, geometry, BAND);
        try (WritableCodeIterator iterator = forward.createWritableIterator()) {
            while (iterator.next()) {
                iterator.setSample(0, 10.0 * iterator.getPosition()[0]);
            }
        }
        final CompoundCodedCoverage compound = new CompoundCodedCoverage(NAME,
                new CodedCoverage[] {forward, new ReverseCoverage(geometry)});

        final CodeIterator iterator = compound.createIterator();
        int count = 0;
        while (iterator.next()) {
            final int p = iterator.getPosition()[0];
            assertEquals(10.0 * p, iterator.getSampleDouble(0));
            assertEquals(p,        iterator.getSampleDouble(1));
            count++;
        }
        assertEquals(zones.size(), count);
    }

    /**
     * Tests that the sample dimensions are those of the stacked coverages, in order.
     */
    @Test
    public void testStackedSampleDimensions() throws Exception {
        final DiscreteGlobalGridReferenceSystem dggrs = new S2Dggrs();
        final List<Object> zones = dggrs.getGridSystem().getHierarchy().getGrids().get(1).getZones().map(Zone::getIdentifier).toList();
        final DiscreteGlobalGridGeometry geometry = DiscreteGlobalGridGeometry.unstructured(dggrs, zones, null);
        final CodedCoverage[] coverages = {
            new ArrayCodedCoverage(NAME, geometry, new SampleDimension.Builder().setName("a").build(),
                                                   new SampleDimension.Builder().setName("b").build()),
            new ArrayCodedCoverage(NAME, geometry, new SampleDimension.Builder().setName("c").build())
        };
        final CompoundCodedCoverage compound = new CompoundCodedCoverage(NAME, coverages);
        assertEquals(List.of("a", "b", "c"), compound.getSampleDimensions().stream().map((s) -> s.getName().toString()).toList());
    }
}
