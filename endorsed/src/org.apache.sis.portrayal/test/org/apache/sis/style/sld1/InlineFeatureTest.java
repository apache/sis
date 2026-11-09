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

import org.apache.sis.feature.builder.FeatureTypeBuilder;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;


/**
 * Tests for {@link InlineFeature}.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class InlineFeatureTest extends SLDTestCase {
    /**
     * Creates a new test case.
     */
    public InlineFeatureTest() {
    }

    /**
     * Creates a feature instance of an arbitrary type.
     */
    private static Feature anyFeature() {
        final var builder = new FeatureTypeBuilder().setName("test");
        builder.addAttribute(String.class).setName("name");
        final Feature feature = builder.build().newInstance();
        feature.setPropertyValue("name", "A random name");
        return feature;
    }

    /**
     * Test of {@code features} property.
     */
    @Test
    public void testFeatures() {
        final var cdt = new InlineFeature();

        // Check defaults
        assertTrue(cdt.features().isEmpty());

        // Check get/set
        var value = anyFeature();
        cdt.features().add(value);
        assertEquals(1, cdt.features().size());
        assertSame(value, cdt.features().get(0));
    }

    /**
     * Tests {@code equals}, {@code hashCode} and {@code clone}.
     * The list shall be copied, but not the features.
     */
    @Test
    public void testClone() {
        final var cdt = new InlineFeature();
        final var feature = anyFeature();
        cdt.features().add(feature);

        final var clone = cdt.clone();
        assertCloneEquals(cdt, clone);
        assertNotSame(cdt.features(), clone.features());
        assertSame(feature, clone.features().get(0));

        clone.features().clear();
        assertNotEquals(cdt, clone);
        assertEquals(1, cdt.features().size());
    }
}
