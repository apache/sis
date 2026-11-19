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
 * Tests for {@link LayerFeatureConstraints} and {@link LayerCoverageConstraints}.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class LayerConstraintsTest extends SLDTestCase {
    /**
     * Creates a new test case.
     */
    public LayerConstraintsTest() {
    }

    /**
     * Test of {@code FeatureTypeConstraint} property.
     */
    @Test
    public void testFeatureConstraints() {
        final var cdt = new LayerFeatureConstraints();

        // Check defaults
        assertTrue(cdt.constraints().isEmpty());

        // Check get/set
        var value = new FeatureTypeConstraint();
        cdt.constraints().add(value);
        assertEquals(1, cdt.constraints().size());
        assertSame(value, cdt.constraints().get(0));

        // Deep clone
        final LayerConstraints<FeatureTypeConstraint> clone = cdt.clone();
        assertCloneEquals(cdt, clone);
        assertNotSame(cdt.constraints(), clone.constraints());
        assertNotSame(value, clone.constraints().get(0));
        assertEquals (value, clone.constraints().get(0));
    }

    /**
     * Test of {@code CoverageConstraint} property.
     */
    @Test
    public void testCoverageConstraints() {
        final var cdt = new LayerCoverageConstraints();

        // Check defaults
        assertTrue(cdt.constraints().isEmpty());

        // Check get/set
        var value = new CoverageConstraint();
        value.setCoverageName("the_coverage_name");
        cdt.constraints().add(value);
        assertEquals(1, cdt.constraints().size());
        assertSame(value, cdt.constraints().get(0));

        // Deep clone
        final LayerConstraints<CoverageConstraint> clone = cdt.clone();
        assertCloneEquals(cdt, clone);
        assertNotSame(cdt.constraints(), clone.constraints());
        assertNotSame(value, clone.constraints().get(0));
        assertEquals (value, clone.constraints().get(0));
    }

    /**
     * Verifies that feature and coverage constraints are not equal even when both are empty.
     */
    @Test
    public void testNotEquals() {
        assertNotEquals(new LayerFeatureConstraints(), new LayerCoverageConstraints());
        assertEquals(new LayerFeatureConstraints(), new LayerFeatureConstraints());
    }
}
