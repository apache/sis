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

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.GenericName;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.BufferedGridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.image.DataType;
import org.apache.sis.image.internal.shared.RasterFactory;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.dggs.Zone;
import org.apache.sis.referencing.dggs.s2.S2Dggrs;
import org.apache.sis.referencing.rs.ReferenceSystems;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.MemoryGridCoverageResource;
import org.apache.sis.storage.dggs.DiscreteGlobalGridGeometry;
import org.apache.sis.storage.rs.CodeIterator;
import org.apache.sis.storage.rs.CodedCoverage;
import org.apache.sis.storage.rs.CodedCoverages;
import org.apache.sis.storage.rs.CodedGeometry;
import org.apache.sis.storage.rs.CodedResource;
import org.apache.sis.util.iso.Names;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link ReducedCodedResource}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class ReducedCodedResourceTest {

    private static final GenericName NAME = Names.createLocalName(null, null, "reduced");

    private final DiscreteGlobalGridReferenceSystem dggrs = new S2Dggrs();

    /**
     * Hides the temporal component, the vertical component is preserved.
     */
    @Test
    public void testHideTemporal() throws Exception {
        final CodedResource source = createSource();
        final ReducedCodedResource reduced = new ReducedCodedResource(NAME, source, Map.of(2, 1));

        final CodedGeometry geometry = reduced.getGridGeometry();
        final List<ReferenceSystem> components = ReferenceSystems.getSingleComponents(geometry.getReferenceSystem(), true);
        assertEquals(2, components.size());
        assertEquals(dggrs, components.get(0));
        assertInstanceOf(VerticalCRS.class, components.get(1));

        final List<Object> zoneIds = rootZoneIds();
        final CodedGeometry query = CodedGeometry.compound(
                DiscreteGlobalGridGeometry.unstructured(dggrs, zoneIds, null),
                geometry.slice(components.get(1)).get());

        final CodedCoverage coverage = reduced.read(query);
        assertEquals(2, coverage.getGeometry().getExtent().getDimension());
        assertEquals(components, ReferenceSystems.getSingleComponents(coverage.getGeometry().getReferenceSystem(), true));

        // values at time index 1 are 4, 5, 6 for vertical index 0, 1, 2
        final CodeIterator iterator = coverage.createIterator();
        int count = 0;
        while (iterator.next()) {
            final int[] position = iterator.getPosition();
            assertEquals(2, position.length);
            assertEquals(4.0 + position[1], iterator.getSampleDouble(0), 0.0);
            count++;
        }
        assertEquals(zoneIds.size() * 3, count);
    }

    /**
     * Hides both vertical and temporal components, only the DGGRS component is preserved.
     */
    @Test
    public void testHideVerticalAndTemporal() throws Exception {
        final CodedResource source = createSource();
        final ReducedCodedResource reduced = new ReducedCodedResource(NAME, source, Map.of(1, 0, 2, 2));

        final CodedGeometry geometry = reduced.getGridGeometry();
        assertEquals(dggrs, geometry.getReferenceSystem());

        final List<Object> zoneIds = rootZoneIds();
        final CodedCoverage coverage = reduced.read(DiscreteGlobalGridGeometry.unstructured(dggrs, zoneIds, null));
        assertEquals(1, coverage.getGeometry().getExtent().getDimension());

        // value at vertical index 0 and time index 2 is 7
        final CodeIterator iterator = coverage.createIterator();
        int count = 0;
        while (iterator.next()) {
            assertEquals(1, iterator.getPosition().length);
            assertEquals(7.0, iterator.getSampleDouble(0), 0.0);
            count++;
        }
        assertEquals(zoneIds.size(), count);
    }

    /**
     * Tests invalid constructor arguments.
     */
    @Test
    public void testInvalidArguments() throws Exception {
        final CodedResource source = createSource();
        // component does not exist
        assertThrows(IllegalArgumentException.class, () -> new ReducedCodedResource(NAME, source, Map.of(3, 0)));
        // DGGRS component can not be hidden
        assertThrows(IllegalArgumentException.class, () -> new ReducedCodedResource(NAME, source, Map.of(0, 0)));
        // index outside vertical extent
        assertThrows(IllegalArgumentException.class, () -> new ReducedCodedResource(NAME, source, Map.of(1, 3)));
    }

    private List<Object> rootZoneIds() throws TransformException {
        return dggrs.getGridSystem().getHierarchy().getGrids().get(1).getZones().map(Zone::getIdentifier).toList();
    }

    /**
     * Creates a resource with DGGRS, vertical and temporal components.
     * Cell values are {@code 1 + vertical index + 3 * temporal index}.
     */
    private CodedResource createSource() throws Exception {
        return CodedCoverages.viewAsDggrs(Names.createLocalName(null, null, "coverage4d"), create4D(), dggrs);
    }

    private static GridCoverageResource create4D() throws Exception {
        final GeneralEnvelope dataEnv = new GeneralEnvelope(CRS.compound(
                CommonCRS.WGS84.normalizedGeographic(),
                CommonCRS.Vertical.ELLIPSOIDAL.crs(),
                CommonCRS.Temporal.JAVA.crs()
        ));
        dataEnv.setRange(0, -180, 180);
        dataEnv.setRange(1, -90, 90);
        dataEnv.setRange(2, 100, 300);
        dataEnv.setRange(3,
                Instant.parse("2000-06-10T10:00:00Z").toEpochMilli(),
                Instant.parse("2000-06-10T12:00:00Z").toEpochMilli());
        final GridGeometry dataGrid = new GridGeometry(
                new GridExtent(null, new long[4], new long[] { 2, 2, 3, 3 }, false),
                dataEnv,
                GridOrientation.DISPLAY
        );

        final ByteBuffer dataBuffer = ByteBuffer.wrap(new byte[] {
                1, 1, 1, 1,     // v0 t0
                2, 2, 2, 2,     // v1 t0
                3, 3, 3, 3,     // v2 t0
                4, 4, 4, 4,     // v0 t1
                5, 5, 5, 5,     // v1 t1
                6, 6, 6, 6,     // v2 t1
                7, 7, 7, 7,     // v0 t2
                8, 8, 8, 8,     // v1 t2
                9, 9, 9, 9      // v2 t2
        });

        final BufferedGridCoverage coverage = new BufferedGridCoverage(
                dataGrid,
                (List) List.of(new SampleDimension.Builder().setName("test").build()),
                RasterFactory.wrap(DataType.BYTE, dataBuffer)
        );
        return new MemoryGridCoverageResource(null, null, coverage, null) {
            @Override
            public Optional<GenericName> getIdentifier() {
                return Optional.of(Names.createLocalName(null, null, "test"));
            }
        };
    }
}
