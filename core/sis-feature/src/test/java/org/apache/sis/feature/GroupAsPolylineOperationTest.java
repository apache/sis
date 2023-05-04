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

import java.util.Map;
import java.util.Arrays;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.setup.GeometryLibrary;

// Test dependencies
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;

// Branch-dependent imports
import org.opengis.feature.Attribute;
import org.opengis.feature.Feature;
import org.opengis.feature.Operation;
import org.opengis.feature.Property;


/**
 * Tests {@link GroupAsPolylineOperation}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   0.4
 */
public final class GroupAsPolylineOperationTest extends TestCase {
    /**
     * Tests a feature with a sequence of points.
     */
    @Test
    public void testPoints() {
        final FeatureTypeBuilder builder = new FeatureTypeBuilder().setName("test");
        builder.addAttribute(Point.class).setMaximumOccurs(10).setName("points");
        final Feature feature = builder.build().newInstance();
        feature.setPropertyValue("points", Arrays.asList(
            new Point(-6, 4),
            new Point(12, 7),
            new Point( 8, 6)));

        final Operation group = FeatureOperations.groupAsPolyline(Map.of("name", "polyline"),
                                GeometryLibrary.ESRI, feature.getType().getProperty("points"));

        final Property result = group.apply(feature, null);
        final Object   value  = ((Attribute<?>) result).getValue();
        final Polyline poly   = (Polyline) value;
        assertEquals(-6, poly.getPoint(0).getX(), STRICT);
        assertEquals( 7, poly.getPoint(1).getY(), STRICT);
        assertEquals( 8, poly.getPoint(2).getX(), STRICT);
    }
}
