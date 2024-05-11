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
package org.apache.sis.feature.builder;

import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.util.SimpleInternationalString;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Unit tests for class {@link AssociationRoleBuilder}.
 *
 * @author  Michael Hausegger
 */
public final class AssociationRoleBuilderTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public AssociationRoleBuilderTest() {
    }

    /**
     * Tests the name, designation, definition, description and multiplicity associated to the role.
     */
    @Test
    public void testMetadata() {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder().setName("Highway");
        final NamedIdentifier target = new NamedIdentifier(null, "Bridge");
        final AssociationRoleBuilder builder = new AssociationRoleBuilder(ftb, null, target)
                .setDescription("Bridges on the highway")
                .setDefinition("A definition")
                .setDesignation("A designation")
                .setMaximumOccurs(2)
                .setMinimumOccurs(1);

        final var role = builder.build();
        assertEquals(1, role.getMinimumOccurs());
        assertEquals(2, role.getMaximumOccurs());
        assertEquals(new SimpleInternationalString("A designation"),          role.getDesignation().orElseThrow());
        assertEquals(new SimpleInternationalString("A definition"),           role.getDefinition());
        assertEquals(new SimpleInternationalString("Bridges on the highway"), role.getDescription().orElseThrow());
    }
}
