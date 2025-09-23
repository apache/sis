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
package org.apache.sis.referencing.internal.shared;

import java.util.Set;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link WKTKeywords} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class WKTKeywordsTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public WKTKeywordsTest() {
    }

    /**
     * Ensures that all constants are equal to the name of the field that declare it.
     * The intent is to avoid misleading constant names when reading code.
     *
     * <p>This test is not strictly necessary. We are just checking an arbitrary convention here, not a requirement.
     * If a developer change the constant values without changing the constant names (for example in order to use the
     * abridged WKT 2 keyword names instead that their long name), this is okay — just ignore this test.</p>
     *
     * @throws ReflectiveOperationException if a field is not public, or other error related to reflection.
     */
    @Test
    public void verifyConstantValues() throws ReflectiveOperationException {
        for (final Field field : WKTKeywords.class.getDeclaredFields()) {
            final String name = field.getName();
            final int modifiers = field.getModifiers();
            if (name.equals("TYPES")) {
                assertFalse(Modifier.isPublic(modifiers), name);
                continue;
            }
            assertTrue(Modifier.isPublic(modifiers), name);
            assertTrue(Modifier.isStatic(modifiers), name);
            assertTrue(Modifier.isFinal (modifiers), name);
            assertEquals(name, field.get(null), "As a policy of WKTKeywords, constants value should be equal to field name.");
        }
    }

    /**
     * Verifies that {@link SingleCRS}, {@link CoordinateReferenceSystem} and {@link Datum} base types
     * contain all WKT keywords associated to subtypes.
     */
    @Test
    public void verifyTypeHierarchy() {
        verifyTypeHierarchy(SingleCRS.class, GeodeticCRS.class, GeographicCRS.class, ProjectedCRS.class,
                            VerticalCRS.class, TemporalCRS.class, EngineeringCRS.class);
        verifyTypeHierarchy(CoordinateReferenceSystem.class, SingleCRS.class, CompoundCRS.class,
                            GeodeticCRS.class, GeographicCRS.class, ProjectedCRS.class,
                            VerticalCRS.class, TemporalCRS.class, EngineeringCRS.class);
        verifyTypeHierarchy(Datum.class, GeodeticDatum.class, VerticalDatum.class, TemporalDatum.class,
                            EngineeringDatum.class);
    }

    /**
     * Verify that the specified {@code base} type contain all WKT keywords associated to specified subtypes.
     */
    @SafeVarargs
    private static <T> void verifyTypeHierarchy(final Class<T> base, final Class<? extends T>... subtypes) {
        final Set<String> all = Set.of(WKTKeywords.forType(base));
        assertNotNull(all, base.getName());
        for (final Class<? extends T> subtype : subtypes) {
            final Set<String> specialized = Set.of(WKTKeywords.forType(subtype));
            final String name = subtype.getName();
            assertNotNull(specialized, name);
            assertTrue(all.size() > specialized.size(), name);
            assertTrue(all.containsAll(specialized), name);
        }
    }
}
