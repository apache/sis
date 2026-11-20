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

import java.awt.image.DataBufferDouble;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.util.GenericName;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.BufferedGridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.crs.DefaultEngineeringCRS;
import org.apache.sis.referencing.cs.AbstractCS;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridHierarchy;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.dggs.Zone;
import org.apache.sis.referencing.dggs.s2.S2Dggrs;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.MemoryFeatureSet;
import org.apache.sis.storage.MemoryGridCoverageResource;
import org.apache.sis.storage.dggs.internal.shared.ArrayDiscreteGlobalGridCoverage;
import org.apache.sis.storage.rs.CodeIterator;
import org.apache.sis.storage.rs.CodedCoverage;
import org.apache.sis.storage.rs.WritableCodeIterator;
import org.apache.sis.util.iso.Names;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link DiscreteGlobalGridCoverageProcessor}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class DiscreteGlobalGridCoverageProcessorTest {

    private static final GenericName NAME = Names.createLocalName(null, null, "test");

    private final DiscreteGlobalGridReferenceSystem dggrs = new S2Dggrs();
    private final DiscreteGlobalGridHierarchy hierarchy = dggrs.getGridSystem().getHierarchy();

    /**
     * Tests that resampling a categorized feature property keeps the most frequent category of each zone.
     */
    @Test
    public void testResampleFeatureSetCategory() throws Exception {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("parcels");
        ftb.addAttribute(Point.class).setName("geom").setCRS(CommonCRS.WGS84.normalizedGeographic()).addRole(AttributeRole.DEFAULT_GEOMETRY);
        ftb.addAttribute(Integer.class).setName("landuse").setValidValues(1, 2, 3);
        final FeatureType type = ftb.build();

        final GeometryFactory gf = new GeometryFactory();
        final double[][] coordinates = {{19, 19}, {20, 20}, {21, 21}};
        final int[] categories = {1, 2, 2};
        final List<Feature> features = new ArrayList<>();
        for (int i = 0; i < categories.length; i++) {
            final Feature feature = type.newInstance();
            feature.setPropertyValue("geom", gf.createPoint(new CoordinateXY(coordinates[i][0], coordinates[i][1])));
            feature.setPropertyValue("landuse", categories[i]);
            features.add(feature);
        }

        final Zone zone = hierarchy.getGrids().get(1).getZone(new DirectPosition2D(CommonCRS.WGS84.normalizedGeographic(), 20, 20));
        final DiscreteGlobalGridGeometry geometry = DiscreteGlobalGridGeometry.unstructured(dggrs, List.of(zone.getIdentifier()), null);

        final CodedCoverage coverage = new DiscreteGlobalGridCoverageProcessor().resample(
                new MemoryFeatureSet(null, type, features), geometry,
                List.of(new SampleDimension.Builder().setName("landuse").build()));

        final CodeIterator iterator = coverage.createIterator();
        iterator.moveTo(new int[] {0});
        assertEquals(2.0, iterator.getSampleDouble(0));
    }

    /**
     * Tests resampling a coded coverage on zones which are partially outside the coverage.
     * The zones outside the coverage shall be left to NaN.
     */
    @Test
    public void testResampleCodedCoverageWithMissingZones() throws Exception {
        final List<Zone> faces = hierarchy.getGrids().get(0).getZones().limit(2).toList();
        final List<Object> covered = faces.get(0).getChildren().stream().map(Zone::getIdentifier).toList();
        final ArrayDiscreteGlobalGridCoverage source = new ArrayDiscreteGlobalGridCoverage(NAME,
                DiscreteGlobalGridGeometry.unstructured(dggrs, covered, null),
                new SampleDimension.Builder().setName("value").build());
        try (WritableCodeIterator iterator = source.createWritableIterator()) {
            while (iterator.next()) {
                iterator.setSample(0, 10.0 * iterator.getPosition()[0]);
            }
        }
        final Object outside = faces.get(1).getChildren().iterator().next().getIdentifier();
        final DiscreteGlobalGridGeometry query = DiscreteGlobalGridGeometry.unstructured(dggrs, List.of(covered.get(2), outside), null);

        final CodedCoverage result = new DiscreteGlobalGridCoverageProcessor().resample(source, query);
        final CodeIterator iterator = result.createIterator();
        iterator.moveTo(new int[] {0});
        assertEquals(20.0, iterator.getSampleDouble(0));
        iterator.moveTo(new int[] {1});
        assertEquals(Double.NaN, iterator.getSampleDouble(0));
    }

    /**
     * Tests resampling a grid coverage whose second dimension is the zone identifier.
     * The first dimension has two slices, only the first one shall be read.
     * The source has two bands, only the second one is requested.
     *
     * <p>The identifiers of S2 faces 0 to 3 are (2<var>k</var>+1)·2⁶⁰,
     * which is the conversion from grid index <var>k</var> to cell identifier.</p>
     */
    @Test
    public void testResampleZonalCoverage() throws Exception {
        final CoordinateSystem gridCS = CommonCRS.Engineering.GRID.crs().getCoordinateSystem();
        final CoordinateReferenceSystem crs = new DefaultEngineeringCRS(Map.of(DefaultEngineeringCRS.NAME_KEY, "slice, cell"),
                CommonCRS.Engineering.GRID.datum(), null,
                new AbstractCS(Map.of(AbstractCS.NAME_KEY, "slice, cell"), gridCS.getAxis(1), gridCS.getAxis(0)));
        final Matrix3 gridToCRS = new Matrix3(
                1, 0,      0,
                0, 0x1p61, 0x1p60,
                0, 0,      1);
        final GridGeometry domain = new GridGeometry(new GridExtent(null, new long[2], new long[] {2, 4}, false),
                PixelInCell.CELL_CENTER, MathTransforms.linear(gridToCRS), crs);

        final double[][] banks = new double[2][8];
        for (int cell = 0; cell < 4; cell++) {
            for (int slice = 0; slice < 2; slice++) {
                banks[0][slice + 2*cell] =       10*cell + slice;
                banks[1][slice + 2*cell] = 100 + 10*cell + slice;
            }
        }
        final BufferedGridCoverage coverage = new BufferedGridCoverage(domain,
                List.of(new SampleDimension.Builder().setName("a").build(), new SampleDimension.Builder().setName("b").build()),
                new DataBufferDouble(banks, 8));
        final GridCoverageResource source = new MemoryGridCoverageResource(null, null, coverage, null) {
            @Override
            public Optional<GenericName> getIdentifier() {
                return Optional.of(NAME);
            }
        };

        final List<Object> faces = hierarchy.getGrids().get(0).getZones()
                .filter((zone) -> zone.getLongIdentifier() > 0).map(Zone::getIdentifier).toList();
        assertEquals(4, faces.size());
        final CodedCoverage result = new DiscreteGlobalGridCoverageProcessor().resampleZonalCoverage(source,
                DiscreteGlobalGridGeometry.unstructured(dggrs, faces, null), 1);

        assertEquals(1, result.getSampleDimensions().size());
        final CodeIterator iterator = result.createIterator();
        int count = 0;
        while (iterator.next()) {
            final long id = hierarchy.getZone(faces.get(iterator.getPosition()[0])).getLongIdentifier();
            final int face = (int) (((id >>> 60) - 1) / 2);
            assertEquals(100 + 10*face, iterator.getSampleDouble(0), "Face " + face);
            count++;
        }
        assertEquals(4, count);
    }
}
