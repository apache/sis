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

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests for {@link Mark}.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class MarkTest extends StyleTestCase {
    /**
     * Creates a new test case.
     */
    public MarkTest() {
    }

    /**
     * Test of {@code WellKnownName} property.
     */
    @Test
    public void testWellKnownName() {
        final var cdt = factory.createMark();

        // Check default
        assertLiteralEquals("square", cdt.getWellKnownName());

        // Check get/set
        var value = literal("A random name");
        cdt.setWellKnownName(value);
        assertEquals(value, cdt.getWellKnownName());
    }

    /**
     * Test of {@code Fill} property.
     */
    @Test
    public void testFill() {
        final var cdt = factory.createMark();

        // Check default
        assertOptionalEquals(factory.createFill(), cdt.getFill());

        // Check get/set
        var value = factory.createFill();
        value.setColorAndOpacity(ANY_COLOR);
        cdt.setFill(value);
        assertOptionalEquals(value, cdt.getFill());
    }

    /**
     * Test of {@code getStroke} property.
     */
    @Test
    public void testStroke() {
        final var cdt = factory.createMark();

        // Check default
        assertOptionalEquals(factory.createStroke(), cdt.getStroke());

        // Check get/set
        var value = factory.createStroke();
        value.setOpacity(literal(0.75));
        cdt.setStroke(value);
        assertOptionalEquals(value, cdt.getStroke());
    }

    /**
     * Test of {@code MarkIndex} property.
     */
    @Test
    public void testMarkIndex() {
        final var cdt = factory.createMark();

        // Check default
        assertEmpty(cdt.getMarkIndex());

        // Check get/set
        var value = literal(3);
        cdt.setMarkIndex(value);
        assertOptionalEquals(value, cdt.getMarkIndex());
    }

    /**
     * Verifies that the copy constructor preserves all properties,
     * including the explicit absence of fill and stroke.
     */
    @Test
    public void testCopyConstructor() {
        final var cdt = factory.createMark();
        cdt.setWellKnownName(literal("circle"));
        cdt.setFill(null);
        cdt.setStroke(null);
        cdt.setMarkIndex(literal(3));
        cdt.setFormat("image/svg+xml");

        final var copy = new Mark<>(cdt);
        assertEquals(cdt, copy);
        assertEmpty(copy.getFill());
        assertEmpty(copy.getStroke());
        assertOptionalEquals(cdt.getMarkIndex().orElseThrow(), copy.getMarkIndex());
        assertOptionalEquals("image/svg+xml", copy.getFormat());
    }

    /**
     * Verifies that {@code equals} takes in account the properties inherited from {@link GraphicalSymbol}.
     */
    @Test
    public void testEqualsWithInheritedProperties() {
        final var m1 = factory.createMark();
        final var m2 = factory.createMark();
        assertEquals(m1, m2);
        m2.setFormat("image/svg+xml");
        assertNotEquals(m1, m2);
    }
}
