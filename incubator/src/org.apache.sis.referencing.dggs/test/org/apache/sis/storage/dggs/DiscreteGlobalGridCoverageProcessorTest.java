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

import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.dggs.Zone;
import org.apache.sis.referencing.dggs.s2.S2Dggrs;
import org.apache.sis.storage.MemoryFeatureSet;
import org.apache.sis.storage.rs.CodeIterator;
import org.apache.sis.storage.rs.CodedCoverage;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link DiscreteGlobalGridCoverageProcessor}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class DiscreteGlobalGridCoverageProcessorTest {

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

        final DiscreteGlobalGridReferenceSystem dggrs = new S2Dggrs();
        final Zone zone = dggrs.getGridSystem().getHierarchy().getGrids().get(1)
                .getZone(new DirectPosition2D(CommonCRS.WGS84.normalizedGeographic(), 20, 20));
        final DiscreteGlobalGridGeometry geometry = DiscreteGlobalGridGeometry.unstructured(dggrs, List.of(zone.getIdentifier()), null);

        final CodedCoverage coverage = new DiscreteGlobalGridCoverageProcessor().resample(
                new MemoryFeatureSet(null, type, features), geometry,
                List.of(new SampleDimension.Builder().setName("landuse").build()));

        final CodeIterator iterator = coverage.createIterator();
        iterator.moveTo(new int[] {0});
        assertEquals(2.0, iterator.getSampleDouble(0));
    }
}
