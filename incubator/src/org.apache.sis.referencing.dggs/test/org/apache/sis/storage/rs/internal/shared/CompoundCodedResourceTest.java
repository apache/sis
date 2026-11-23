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
import org.apache.sis.storage.AbstractResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.dggs.DiscreteGlobalGridGeometry;
import org.apache.sis.storage.rs.CodeIterator;
import org.apache.sis.storage.rs.CodedCoverage;
import org.apache.sis.storage.rs.CodedGeometry;
import org.apache.sis.storage.rs.CodedResource;
import org.apache.sis.storage.rs.WritableCodeIterator;
import org.apache.sis.util.iso.Names;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link CompoundCodedResource}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class CompoundCodedResourceTest {

    private final DiscreteGlobalGridReferenceSystem dggrs = new S2Dggrs();

    /**
     * Creates a resource on the children of the first S2 face.
     * The value of band <var>b</var> at position <var>p</var> is {@code factors[b]} × <var>p</var>.
     */
    private CodedResource createResource(String name, double[] factors, SampleDimension... bands) throws Exception {
        final List<Object> zones = dggrs.getGridSystem().getHierarchy().getGrids().get(0).getZones().findFirst().orElseThrow()
                .getChildren().stream().map(Zone::getIdentifier).toList();
        final GenericName id = Names.createLocalName(null, null, name);
        final ArrayCodedCoverage coverage = new ArrayCodedCoverage(id, DiscreteGlobalGridGeometry.unstructured(dggrs, zones, null), bands);
        try (WritableCodeIterator iterator = coverage.createWritableIterator()) {
            while (iterator.next()) {
                for (int b = 0; b < factors.length; b++) {
                    iterator.setSample(b, factors[b] * iterator.getPosition()[0]);
                }
            }
        }
        return new MemoryCodedResource(id, coverage);
    }

    /**
     * Creates the compound of resource "a" with bands a0 (in °C) and a1, and resource "b" with band b0.
     */
    private CompoundCodedResource createCompound() throws Exception {
        final CodedResource a = createResource("a", new double[] {10, 20},
                new SampleDimension.Builder().setName("a0").addQuantitative("temperature", -50.0, 50.0, Units.CELSIUS).build(),
                new SampleDimension.Builder().setName("a1").build());
        final CodedResource b = createResource("b", new double[] {100},
                new SampleDimension.Builder().setName("b0").build());
        return new CompoundCodedResource(Names.createLocalName(null, null, "compound"), List.of(a, b));
    }

    /**
     * A resource which records the bands requested at the last read.
     */
    private static final class RecordingResource extends AbstractResource implements CodedResource {
        private final CodedResource source;
        int[] lastRange;

        RecordingResource(final CodedResource source) {
            super(null);
            this.source = source;
        }

        @Override public Optional<GenericName>  getIdentifier()       throws DataStoreException {return source.getIdentifier();}
        @Override public List<SampleDimension>  getSampleDimensions() throws DataStoreException {return source.getSampleDimensions();}
        @Override public CodedGeometry          getGridGeometry()     throws DataStoreException {return source.getGridGeometry();}

        @Override
        public CodedCoverage read(final CodedGeometry geometry, final int... range) throws DataStoreException {
            lastRange = range;
            return source.read(geometry, range);
        }
    }

    /**
     * Tests that each resource is asked only for the bands it provides, and that the other resources are not read.
     */
    @Test
    public void testReadPassesRange() throws Exception {
        final RecordingResource a = new RecordingResource(createResource("a", new double[] {10, 20},
                new SampleDimension.Builder().setName("a0").build(),
                new SampleDimension.Builder().setName("a1").build()));
        final RecordingResource b = new RecordingResource(createResource("b", new double[] {100},
                new SampleDimension.Builder().setName("b0").build()));
        final CompoundCodedResource compound = new CompoundCodedResource(Names.createLocalName(null, null, "compound"), List.of(a, b));

        final CodedCoverage coverage = compound.read(a.getGridGeometry(), 1, 0);
        assertArrayEquals(new int[] {1, 0}, a.lastRange);
        assertNull(b.lastRange, "Resource b provides no requested band.");

        final CodeIterator iterator = coverage.createIterator();
        while (iterator.next()) {
            final int p = iterator.getPosition()[0];
            assertEquals(20.0 * p, iterator.getSampleDouble(0), "Band a1");
            assertEquals(10.0 * p, iterator.getSampleDouble(1), "Band a0");
        }
    }

    /**
     * Tests that the sample dimensions of the compound keep the units of the source sample dimensions.
     */
    @Test
    public void testSampleDimensionUnits() throws Exception {
        final List<SampleDimension> bands = createCompound().getSampleDimensions();
        assertEquals(3, bands.size());
        assertEquals(Optional.of(Units.CELSIUS), bands.get(0).getUnits());
    }

    /**
     * Tests that the bands of the read coverages are the bands of the resource, with their prefixed names.
     */
    @Test
    public void testReadSampleDimensions() throws Exception {
        final CompoundCodedResource compound = createCompound();
        final List<SampleDimension> bands = compound.getSampleDimensions();
        final CodedResource a = createResource("a", new double[] {10, 20}, new SampleDimension.Builder().setName("a0").build(),
                                                                          new SampleDimension.Builder().setName("a1").build());
        assertEquals(bands, compound.read(a.getGridGeometry()).getSampleDimensions());

        final List<SampleDimension> selected = compound.read(a.getGridGeometry(), 2, 0).getSampleDimensions();
        assertEquals(List.of("b:b0", "a:a0"), selected.stream().map((s) -> s.getName().toString()).toList());
        assertEquals(List.of(bands.get(2), bands.get(0)), selected);
    }

    /**
     * Tests reading a subset of the bands, in an order different than the resource order.
     */
    @Test
    public void testReadRange() throws Exception {
        final CompoundCodedResource compound = createCompound();
        final CodedResource a = createResource("a", new double[] {10, 20}, new SampleDimension.Builder().setName("a0").build(),
                                                                          new SampleDimension.Builder().setName("a1").build());
        final CodedCoverage coverage = compound.read(a.getGridGeometry(), 2, 0);
        assertEquals(2, coverage.getSampleDimensions().size());
        final CodeIterator iterator = coverage.createIterator();
        assertEquals(2, iterator.getNumBands());
        int count = 0;
        while (iterator.next()) {
            final int p = iterator.getPosition()[0];
            assertEquals(100.0 * p, iterator.getSampleDouble(0), "Band b0");
            assertEquals( 10.0 * p, iterator.getSampleDouble(1), "Band a0");
            count++;
        }
        assertEquals(4, count);
    }
}
