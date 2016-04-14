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
package org.apache.sis.feature;

import java.util.Collections;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.apache.sis.util.iso.Names;
import org.apache.sis.internal.feature.FeatureTypeBuilder;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link BoundsOperation}.
 *
 * @author  Johann Sorel (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn({
    AbstractOperationTest.class,
    DenseFeatureTest.class
})
public final strictfp class BoundsOperationTest extends TestCase {
    /**
     * Creates a feature type with a bounds operation.
     * The feature contains the following properties:
     *
     * <ul>
     *   <li>{@code name} as a  {@link String}</li>
     *   <li>{@code classes} as a {@link Polygon}</li>
     *   <li>{@code climbing wall} as a {@link Point}</li>
     *   <li>{@code gymnasium} as a {@link Polygon}</li>
     *   <li>{@code bounds} as the feature envelope attribute.</li>
     * </ul>
     *
     * @return The feature for a city.
     */
    private static DefaultFeatureType school() {
        //Create type with an aggregation
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("school");
        ftb.addProperty("name", String.class);
        ftb.addProperty("classes", Polygon.class, CommonCRS.WGS84.geographic());
        ftb.addProperty("climbing wall", Point.class, CommonCRS.WGS84.geographic());
        ftb.addProperty("gymnasium", Polygon.class, CommonCRS.WGS84.normalizedGeographic());
        ftb.addProperty(FeatureOperations.bounds(
                Collections.singletonMap(AbstractOperation.NAME_KEY, Names.parseGenericName(null, ":", "bounds")),
                CommonCRS.WGS84.geographic()));
        return ftb.build();
    }

    /**
     * Implementation of the test methods.
     */
    private static void run(final AbstractFeature feature) {
        GeneralEnvelope bounds;

        //no geometry set
        assertNull(feature.getPropertyValue("bounds"));

        //set one geometry
        Polygon classes = new Polygon();
        classes.startPath(10, 20);
        classes.lineTo(10, 30);
        classes.lineTo(15, 30);
        classes.lineTo(15, 20);
        feature.setPropertyValue("classes", classes);
        bounds = new GeneralEnvelope(CommonCRS.WGS84.geographic());
        bounds.setRange(0, 10, 15);
        bounds.setRange(1, 20, 30);
        assertEquals(bounds,feature.getPropertyValue("bounds"));

        //set second geometry
        Point wall = new Point(18, 40);
        feature.setPropertyValue("climbing wall", wall);
        bounds = new GeneralEnvelope(CommonCRS.WGS84.geographic());
        bounds.setRange(0, 10, 18);
        bounds.setRange(1, 20, 40);
        assertEquals(bounds,feature.getPropertyValue("bounds"));

        //set third geometry, this geometry has crs axis reversed
        Polygon gymnasium = new Polygon();
        gymnasium.startPath(-5, -30);
        gymnasium.lineTo(-6, -30);
        gymnasium.lineTo(-6, -31);
        gymnasium.lineTo(-5, -31);
        feature.setPropertyValue("gymnasium", gymnasium);
        bounds = new GeneralEnvelope(CommonCRS.WGS84.geographic());
        bounds.setRange(0, -31, 18);
        bounds.setRange(1, -6, 40);
        assertEquals(bounds,feature.getPropertyValue("bounds"));

    }


    /**
     * Tests a dense type with operations.
     */
    @Test
    public void testDenseFeature() {
        run(new DenseFeature(school()));
    }

    /**
     * Tests a sparse feature type with operations.
     */
    @Test
    public void testSparseFeature() {
        run(new SparseFeature(school()));
    }
}
