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

import java.util.List;

// Test dependencies
import org.junit.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests for {@link Font}.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class FontTest extends StyleTestCase {
    /**
     * Creates a new test case.
     */
    public FontTest() {
    }

    /**
     * Test of {@code Family} property.
     */
    @Test
    public void testFamily() {
        final var cdt = factory.createFont();

        // Check default
        assertTrue(cdt.family().isEmpty());

        // Check get/set
        var value = literal("A random family");
        cdt.family().add(value);
        assertEquals(List.of(value), cdt.family());
    }
    /**
     * Test of {@code Style} property.
     */
    @Test
    public void testStyle() {
        final var cdt = factory.createFont();

        // Check default
        assertLiteralEquals("normal", cdt.getStyle());

        // Check get/set
        var value = literal("A random style");
        cdt.setStyle(value);
        assertEquals(value, cdt.getStyle());
    }

    /**
     * Test of {@code Weight} property.
     */
    @Test
    public void testWeight() {
        final var cdt = factory.createFont();

        // Check default
        assertLiteralEquals("normal", cdt.getWeight());

        // Check get/set
        var value = literal("A random weight");
        cdt.setWeight(value);
        assertEquals(value, cdt.getWeight());
    }

    /**
     * Test of {@code Size} property.
     */
    @Test
    public void testSize() {
        final var cdt = factory.createFont();

        // Check default
        assertLiteralEquals(10.0, cdt.getSize());

        // Check get/set
        var value = literal(12);
        cdt.setSize(value);
        assertEquals(value, cdt.getSize());
    }
}
