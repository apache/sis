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
package org.apache.sis.style.se1;

import java.util.Set;
import java.util.List;
import org.apache.sis.util.iso.Names;

// Test dependencies
import org.junit.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests for {@link FeatureTypeStyle}.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class FeatureTypeStyleTest extends StyleTestCase {
    /**
     * Creates a new test case.
     */
    public FeatureTypeStyleTest() {
    }

    /**
     * Test of {@code Name} property.
     */
    @Test
    public void testName() {
        final var cdt = new FeatureTypeStyle();

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
        final var cdt = new FeatureTypeStyle();

        // Check defaults
        assertEmpty(cdt.getDescription());

        // Check get/set
        var desc = anyDescription();
        cdt.setDescription(desc);
        assertOptionalEquals(desc, cdt.getDescription());
    }

    /**
     * Test of {@code FeatureInstanceID} property.
     */
    @Test
    public void testFeatureInstanceIDs() {
        final var cdt = new FeatureTypeStyle();

        // Check defaults
        assertEmpty(cdt.getFeatureInstanceIDs());

        // Check get/set
        final var rid = factory.filterFactory.resourceId("A random identifier");
        cdt.setFeatureInstanceIDs(rid);
        assertOptionalEquals(rid, cdt.getFeatureInstanceIDs());
    }

    /**
     * Test of {@code FeatureTypeName} property.
     */
    @Test
    public void testFeatureTypeNames() {
        final var cdt = new FeatureTypeStyle();

        // Check defaults
        assertEmpty(cdt.getFeatureTypeName());

        // Check get/set
        var name = Names.createLocalName(null, null, "A random name");
        cdt.setFeatureTypeName(name);
        assertOptionalEquals(name, cdt.getFeatureTypeName());
    }

    /**
     * Test of {@code SemanticTypeIdentifier} property.
     */
    @Test
    public void testSemanticTypeIdentifiers() {
        final var cdt = new FeatureTypeStyle();

        // Check defaults
        assertTrue(cdt.semanticTypeIdentifiers().isEmpty());

        // Check get/set
        cdt.semanticTypeIdentifiers().add(SemanticType.LINE);
        assertEquals(Set.of(SemanticType.LINE), cdt.semanticTypeIdentifiers());
    }

    /**
     * Test of {@code Rule} property.
     */
    @Test
    public void testRules() {
        final var cdt = new FeatureTypeStyle();

        // Check defaults
        assertTrue(cdt.rules().isEmpty());

        // Check get/set
        var rule = factory.createRule();
        cdt.rules().add(rule);
        assertEquals(List.of(rule), cdt.rules());
    }
}
