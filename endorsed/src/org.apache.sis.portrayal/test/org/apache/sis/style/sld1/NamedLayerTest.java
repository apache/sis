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

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests for {@link NamedLayer}.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class NamedLayerTest extends SLDTestCase {
    /**
     * Creates a new test case.
     */
    public NamedLayerTest() {
    }

    /**
     * Test of {@code Name} property.
     */
    @Test
    public void testName() {
        final var cdt = new NamedLayer();

        // Check defaults
        assertEmpty(cdt.getName());

        // Check get/set
        String value = "A random name";
        cdt.setName(value);
        assertOptionalEquals(value, cdt.getName());
    }

    /**
     * Test of {@code Description} property.
     */
    @Test
    public void testDescription() {
        final var cdt = new NamedLayer();

        // Check defaults
        assertEmpty(cdt.getDescription());

        // Check get/set
        var desc = anyDescription();
        cdt.setDescription(desc);
        assertOptionalEquals(desc, cdt.getDescription());
    }

    /**
     * Test of {@code LayerFeatureConstraints} property.
     */
    @Test
    public void testConstraints() {
        final var cdt = new NamedLayer();

        // Check defaults
        assertEmpty(cdt.getConstraints());

        // Check get/set
        var value = new LayerFeatureConstraints();
        value.constraints().add(new FeatureTypeConstraint());
        cdt.setConstraints(value);
        assertOptionalEquals(value, cdt.getConstraints());
    }

    /**
     * Test of {@code NamedStyle} and {@code UserStyle} properties.
     */
    @Test
    public void testStyles() {
        final var cdt = new NamedLayer();

        // Check defaults
        assertTrue(cdt.styles().isEmpty());

        // Check get/set
        var named = new NamedStyle();
        var user  = anyUserStyle("user_style");
        cdt.styles().add(named);
        cdt.styles().add(user);
        assertEquals(2, cdt.styles().size());
        assertSame(named, cdt.styles().get(0));
        assertSame(user,  cdt.styles().get(1));
    }

    /**
     * Tests {@code equals}, {@code hashCode} and deep {@code clone}.
     */
    @Test
    public void testClone() {
        final var cdt = new NamedLayer();
        cdt.setName("named_layer");
        cdt.setDescription(anyDescription());
        final var constraints = new LayerFeatureConstraints();
        constraints.constraints().add(new FeatureTypeConstraint());
        cdt.setConstraints(constraints);
        final var style = new NamedStyle();
        style.setName("named_style");
        cdt.styles().add(style);

        final var clone = cdt.clone();
        assertCloneEquals(cdt, clone);
        assertNotSame(cdt.getDescription().orElseThrow(), clone.getDescription().orElseThrow());
        assertNotSame(constraints, clone.getConstraints().orElseThrow());
        assertNotSame(style, clone.styles().get(0));
        assertEquals (style, clone.styles().get(0));

        clone.styles().clear();
        assertNotEquals(cdt, clone);
        assertEquals(1, cdt.styles().size());
    }
}
