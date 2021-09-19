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
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;

// Branch-dependent imports
import org.opengis.feature.FeatureAssociationRole;


/**
 * Unit tests for class {@link AssociationRoleBuilder}.
 *
 * @author  Michael Hausegger
 * @version 0.8
 * @since   0.8
 * @module
 */
public final strictfp class AssociationRoleBuilderTest extends TestCase {
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

        final FeatureAssociationRole role = builder.build();
        assertEquals("minimumOccurs", 1, role.getMinimumOccurs());
        assertEquals("maximumOccurs", 2, role.getMaximumOccurs());
        assertEquals("designation", new SimpleInternationalString("A designation"),          role.getDesignation());
        assertEquals("definition",  new SimpleInternationalString("A definition"),           role.getDefinition());
        assertEquals("description", new SimpleInternationalString("Bridges on the highway"), role.getDescription());
    }
}
