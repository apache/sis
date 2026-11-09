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
package org.apache.sis.style.sld1;

import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.util.iso.Names;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.filter.Filter;


/**
 * Tests for {@link FeatureTypeConstraint} and {@link Extent}.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class FeatureTypeConstraintTest extends SLDTestCase {
    /**
     * Creates a new test case.
     */
    public FeatureTypeConstraintTest() {
    }

    /**
     * Creates an arbitrary filter on a feature property.
     */
    private static Filter<Feature> anyFilter() {
        final var ff = DefaultFilterFactory.forFeatures();
        return ff.greaterOrEqual(ff.property("num_lanes"), ff.literal(4));
    }

    /**
     * Test of {@code FeatureTypeName} property.
     */
    @Test
    public void testFeatureTypeName() {
        final var cdt = new FeatureTypeConstraint();

        // Check defaults
        assertEmpty(cdt.getFeatureTypeName());

        // Check get/set
        var value = Names.createLocalName(null, null, "the_feature_type_name");
        cdt.setFeatureTypeName(value);
        assertOptionalEquals(value, cdt.getFeatureTypeName());
    }

    /**
     * Test of {@code Filter} property.
     */
    @Test
    public void testFilter() {
        final var cdt = new FeatureTypeConstraint();

        // Check defaults
        assertEmpty(cdt.getFilter());

        // Check get/set
        var value = anyFilter();
        cdt.setFilter(value);
        assertOptionalEquals(value, cdt.getFilter());
    }

    /**
     * Test of {@code Extent} property.
     */
    @Test
    public void testExtents() {
        final var cdt = new FeatureTypeConstraint();

        // Check defaults
        assertTrue(cdt.extents().isEmpty());

        // Check get/set
        var value = new Extent();
        cdt.extents().add(value);
        assertEquals(1, cdt.extents().size());
        assertSame(value, cdt.extents().get(0));
    }

    /**
     * Test of {@code Name} and {@code Value} properties of {@link Extent}.
     */
    @Test
    public void testExtent() {
        final var cdt = new Extent();

        // Check defaults
        assertEmpty(cdt.getName());
        assertEmpty(cdt.getValue());

        // Check get/set
        cdt.setName("extent_name");
        cdt.setValue("extent_value");
        assertOptionalEquals("extent_name",  cdt.getName());
        assertOptionalEquals("extent_value", cdt.getValue());

        final var clone = cdt.clone();
        assertCloneEquals(cdt, clone);
        clone.setValue("other");
        assertNotEquals(cdt, clone);
    }

    /**
     * Tests {@code equals}, {@code hashCode} and deep {@code clone}.
     * The filter shall be shared, not cloned.
     */
    @Test
    public void testClone() {
        final var cdt = new FeatureTypeConstraint();
        cdt.setFeatureTypeName(Names.createLocalName(null, null, "the_feature_type_name"));
        cdt.setFilter(anyFilter());
        final var extent = new Extent();
        extent.setName("extent_name");
        extent.setValue("extent_value");
        cdt.extents().add(extent);

        final var clone = cdt.clone();
        assertCloneEquals(cdt, clone);
        assertSame(cdt.getFilter().orElseThrow(), clone.getFilter().orElseThrow());
        assertNotSame(cdt.extents(), clone.extents());
        assertNotSame(extent, clone.extents().get(0));
        assertEquals (extent, clone.extents().get(0));

        final var copy = new FeatureTypeConstraint(cdt);
        assertEquals(cdt, copy);
        assertSame(extent, copy.extents().get(0));
    }
}
