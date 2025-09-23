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
package org.apache.sis.storage.gpx;

import org.opengis.util.GenericName;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.apache.sis.feature.internal.shared.AttributeConvention;
import org.apache.sis.storage.IllegalNameException;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;

// Specific to the main branch:
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.feature.AbstractIdentifiedType;


/**
 * Tests the {@link Types} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class TypesTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public TypesTest() {
    }

    /**
     * Verifies that all designations and definitions can be read from the resources.
     *
     * @throws FactoryException      if an error occurred while initializing the {@link Types} class.
     * @throws IllegalNameException  if an error occurred while initializing the {@link Types} class.
     */
    @Test
    public void testResources() throws FactoryException, IllegalNameException {
        final Types types = Types.DEFAULT;
        testResources(types.route);
        testResources(types.track);
        testResources(types.trackSegment);
        testResources(types.wayPoint);
    }

    /**
     * Verifies that all designations and definitions can be read from the resources.
     */
    private static void testResources(final DefaultFeatureType type) {
        for (final AbstractIdentifiedType p : type.getProperties(false)) {
            final GenericName name = p.getName();
            if (!AttributeConvention.contains(name)) {
                final String label = name.toString();
                assertNonEmpty(label, p.getDesignation().orElse(null));
                assertNonEmpty(label, p.getDefinition());
            }
        }
    }

    /**
     * Verifies that the given text is non-null and non-empty.
     */
    private static void assertNonEmpty(final String name, final InternationalString i18n) {
        assertNotNull(i18n, name);
        assertNotEquals(i18n.length(), 0, name);
    }
}
