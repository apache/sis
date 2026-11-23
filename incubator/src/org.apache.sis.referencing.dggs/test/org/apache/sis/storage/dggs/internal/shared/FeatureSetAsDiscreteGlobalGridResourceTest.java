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

import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.dggs.Zone;
import org.apache.sis.referencing.dggs.s2.S2Dggrs;
import org.apache.sis.storage.MemoryFeatureSet;
import org.apache.sis.storage.dggs.DiscreteGlobalGridCoverageProcessor;
import org.apache.sis.storage.dggs.DiscreteGlobalGridGeometry;
import org.apache.sis.storage.rs.CodeIterator;
import org.apache.sis.storage.rs.CodedCoverage;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link FeatureSetAsDiscreteGlobalGridResource}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class FeatureSetAsDiscreteGlobalGridResourceTest {

    /**
     * Tests reading one of the two numeric properties of points in a single zone.
     * The zone value is the mean of the property values.
     */
    @Test
    public void testReadRange() throws Exception {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("points");
        ftb.addAttribute(Point.class).setName("geom").setCRS(CommonCRS.WGS84.normalizedGeographic()).addRole(AttributeRole.DEFAULT_GEOMETRY);
        ftb.addAttribute(Double.class).setName("a");
        ftb.addAttribute(Double.class).setName("b");
        final FeatureType type = ftb.build();

        final GeometryFactory gf = new GeometryFactory();
        final List<Feature> features = new ArrayList<>();
        for (final double[] values : new double[][] {{19, 1, 10}, {21, 3, 30}}) {
            final Feature feature = type.newInstance();
            feature.setPropertyValue("geom", gf.createPoint(new CoordinateXY(values[0], values[0])));
            feature.setPropertyValue("a", values[1]);
            feature.setPropertyValue("b", values[2]);
            features.add(feature);
        }

        final DiscreteGlobalGridReferenceSystem dggrs = new S2Dggrs();
        final Zone zone = dggrs.getGridSystem().getHierarchy().getGrids().get(1)
                .getZone(new DirectPosition2D(CommonCRS.WGS84.normalizedGeographic(), 20, 20));
        final FeatureSetAsDiscreteGlobalGridResource resource = new FeatureSetAsDiscreteGlobalGridResource(
                dggrs, new MemoryFeatureSet(null, type, features), new DiscreteGlobalGridCoverageProcessor());

        final CodedCoverage coverage = resource.read(DiscreteGlobalGridGeometry.unstructured(dggrs, List.of(zone.getIdentifier()), null), 1);
        assertEquals(1, coverage.getSampleDimensions().size());
        assertEquals("b", coverage.getSampleDimensions().get(0).getName().toString());
        final CodeIterator iterator = coverage.createIterator();
        iterator.moveTo(new int[] {0});
        assertEquals(20.0, iterator.getSampleDouble(0));
    }
}
