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
package org.apache.sis.internal.referencing;

import java.util.Set;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.apache.sis.internal.jdk9.JDK9;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link WKTKeywords} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   0.6
 * @module
 */
public final strictfp class WKTKeywordsTest extends TestCase {
    /**
     * Ensures that all constants are equal to the name of the field that declare it.
     * The intent is to avoid misleading constant names when reading code.
     *
     * <p>This test is not strictly necessary. We are just checking an arbitrary convention here, not a requirement.
     * If a developer change the constant values without changing the constant names (for example in order to use the
     * abridged WKT 2 keyword names instead that their long name), this is okay — just ignore this test.</p>
     *
     * @throws ReflectiveOperationException if a field is not public, or other error related to reflexion.
     */
    @Test
    public void verifyConstantValues() throws ReflectiveOperationException {
        for (final Field field : WKTKeywords.class.getDeclaredFields()) {
            final String name = field.getName();
            final int modifiers = field.getModifiers();
            if (name.equals("TYPES")) {
                assertFalse(name, Modifier.isPublic(modifiers));
                continue;
            }
            assertTrue(name, Modifier.isPublic(modifiers));
            assertTrue(name, Modifier.isStatic(modifiers));
            assertTrue(name, Modifier.isFinal (modifiers));
            assertEquals("As a policy of WKTKeywords, constants value should be equal to field name.", name, field.get(null));
        }
    }

    /**
     * Verifies that {@link SingleCRS}, {@link CoordinateReferenceSystem} and {@link Datum} base types
     * contain all WKT keywords associated to subtypes.
     */
    @Test
    public void verifyTypeHierarchy() {
        verifyTypeHierarchy(SingleCRS.class, GeocentricCRS.class, GeographicCRS.class, ProjectedCRS.class,
                            VerticalCRS.class, TemporalCRS.class, EngineeringCRS.class);
        verifyTypeHierarchy(CoordinateReferenceSystem.class, SingleCRS.class, CompoundCRS.class,
                            GeocentricCRS.class, GeographicCRS.class, ProjectedCRS.class,
                            VerticalCRS.class, TemporalCRS.class, EngineeringCRS.class);
        verifyTypeHierarchy(Datum.class, GeodeticDatum.class, VerticalDatum.class, TemporalDatum.class,
                            EngineeringDatum.class);
    }

    /**
     * Verify that the specified {@code base} type contain all WKT keywords associated to specified subtypes.
     */
    @SafeVarargs
    private static <T> void verifyTypeHierarchy(final Class<T> base, final Class<? extends T>... subtypes) {
        final Set<String> all = JDK9.setOf(WKTKeywords.forType(base));
        assertNotNull(base.getName(), all);
        for (final Class<? extends T> subtype : subtypes) {
            final Set<String> specialized = JDK9.setOf(WKTKeywords.forType(subtype));
            final String name = subtype.getName();
            assertNotNull(name, specialized);
            assertTrue(name, all.size() > specialized.size());
            assertTrue(name, all.containsAll(specialized));
        }
    }
}
