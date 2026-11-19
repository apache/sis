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
 * Tests for {@link UserLayer}.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class UserLayerTest extends SLDTestCase {
    /**
     * Creates a new test case.
     */
    public UserLayerTest() {
    }

    /**
     * Test of {@code Name} property.
     */
    @Test
    public void testName() {
        final var cdt = new UserLayer();

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
        final var cdt = new UserLayer();

        // Check defaults
        assertEmpty(cdt.getDescription());

        // Check get/set
        var desc = anyDescription();
        cdt.setDescription(desc);
        assertOptionalEquals(desc, cdt.getDescription());
    }

    /**
     * Test of {@code RemoteOWS} and {@code InlineFeature} properties.
     */
    @Test
    public void testSource() {
        final var cdt = new UserLayer();

        // Check defaults
        assertEmpty(cdt.getSource());

        // Check get/set
        var remote = new RemoteOWS();
        remote.setService("WFS");
        cdt.setSource(remote);
        assertOptionalEquals(remote, cdt.getSource());

        var inline = new InlineFeature();
        cdt.setSource(inline);
        assertOptionalEquals(inline, cdt.getSource());

        cdt.setSource(null);
        assertEmpty(cdt.getSource());
    }

    /**
     * Test of {@code LayerFeatureConstraints} and {@code LayerCoverageConstraints} properties.
     */
    @Test
    public void testConstraints() {
        final var cdt = new UserLayer();

        // Check defaults
        assertEmpty(cdt.getConstraints());

        // Check get/set
        var features = new LayerFeatureConstraints();
        features.constraints().add(new FeatureTypeConstraint());
        cdt.setConstraints(features);
        assertOptionalEquals(features, cdt.getConstraints());

        var coverages = new LayerCoverageConstraints();
        coverages.constraints().add(new CoverageConstraint());
        cdt.setConstraints(coverages);
        assertOptionalEquals(coverages, cdt.getConstraints());
    }

    /**
     * Test of {@code UserStyle} property.
     */
    @Test
    public void testStyles() {
        final var cdt = new UserLayer();

        // Check defaults
        assertTrue(cdt.styles().isEmpty());

        // Check get/set
        var value = anyUserStyle("user_style");
        cdt.styles().add(value);
        assertEquals(1, cdt.styles().size());
        assertSame(value, cdt.styles().get(0));
    }

    /**
     * Tests {@code equals}, {@code hashCode} and deep {@code clone}.
     */
    @Test
    public void testClone() {
        final var cdt = new UserLayer();
        cdt.setName("user_layer");
        final var remote = new RemoteOWS();
        remote.setService("WCS");
        remote.setOnlineResource(anyOnlineResource());
        cdt.setSource(remote);
        final var coverages = new LayerCoverageConstraints();
        final var constraint = new CoverageConstraint();
        constraint.setCoverageName("the_coverage_name");
        coverages.constraints().add(constraint);
        cdt.setConstraints(coverages);
        final var style = anyUserStyle("user_style");
        cdt.styles().add(style);

        final var clone = cdt.clone();
        assertCloneEquals(cdt, clone);
        assertNotSame(remote, clone.getSource().orElseThrow());
        assertNotSame(coverages, clone.getConstraints().orElseThrow());
        assertNotSame(style, clone.styles().get(0));
        assertEquals (style, clone.styles().get(0));
        assertSame(remote.getOnlineResource().orElseThrow(),
                   ((RemoteOWS) clone.getSource().orElseThrow()).getOnlineResource().orElseThrow());

        final var copy = new UserLayer(cdt);
        assertEquals(cdt, copy);
        assertSame(remote, copy.getSource().orElseThrow());
    }
}
