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

import org.apache.sis.style.Style;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests for {@link NamedStyle}.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class NamedStyleTest extends SLDTestCase {
    /**
     * Creates a new test case.
     */
    public NamedStyleTest() {
    }

    /**
     * Verifies that a named style is a {@link Style}.
     */
    @Test
    public void testIsStyle() {
        assertInstanceOf(Style.class, new NamedStyle());
    }

    /**
     * Test of {@code Name} property.
     */
    @Test
    public void testName() {
        final var cdt = new NamedStyle();

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
        final var cdt = new NamedStyle();

        // Check defaults
        assertEmpty(cdt.getDescription());

        // Check get/set
        var desc = anyDescription();
        cdt.setDescription(desc);
        assertOptionalEquals(desc, cdt.getDescription());
    }

    /**
     * Tests {@code equals}, {@code hashCode} and deep {@code clone}.
     */
    @Test
    public void testClone() {
        final var cdt = new NamedStyle();
        cdt.setName("named_style");
        cdt.setDescription(anyDescription());

        final var clone = cdt.clone();
        assertCloneEquals(cdt, clone);
        assertNotSame(cdt.getDescription().orElseThrow(), clone.getDescription().orElseThrow());

        clone.setName("other");
        assertNotEquals(cdt, clone);
    }
}
