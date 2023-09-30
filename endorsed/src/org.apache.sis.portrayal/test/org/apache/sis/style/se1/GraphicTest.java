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
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests for {@link Graphic}.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class GraphicTest extends StyleTestCase {
    /**
     * Creates a new test case.
     */
    public GraphicTest() {
    }

    /**
     * Test of {@code graphicalSymbols} property.
     */
    @Test
    public void testGraphicalSymbols() {
        final var cdt = factory.createGraphic();

        // Check defaults
        assertTrue(cdt.graphicalSymbols().isEmpty());

        // Check get/set
        cdt.graphicalSymbols().add(factory.createMark());
        assertEquals(1, cdt.graphicalSymbols().size());
    }

    /**
     * Test of {@code Opacity} property.
     */
    @Test
    public void testOpacity() {
        final var cdt = factory.createGraphic();

        // Check default
        assertLiteralEquals(1.0, cdt.getOpacity());

        // Check get/set
        cdt.setOpacity(literal(0.4));
        assertLiteralEquals(0.4, cdt.getOpacity());
    }

    /**
     * Test of {@code Size} property.
     */
    @Test
    public void testSize() {
        final var cdt = factory.createGraphic();

        // Check default
        assertLiteralEquals(6.0, cdt.getSize());

        // Check get/set
        cdt.setSize(literal(13));
        assertLiteralEquals(13, cdt.getSize());
    }

    /**
     * Test of {@code Rotation} property.
     */
    @Test
    public void testRotation() {
        final var cdt = factory.createGraphic();

        // Check default
        assertLiteralEquals(0.0, cdt.getRotation());

        // Check get/set
        cdt.setRotation(literal(90));
        assertLiteralEquals(90, cdt.getRotation());
    }

    /**
     * Test of {@code AnchorPoint} property.
     */
    @Test
    public void testAnchorPoint() {
        final var cdt = factory.createGraphic();

        // Check default
        assertEquals(factory.createAnchorPoint(), cdt.getAnchorPoint());

        // Check get/set
        var value = factory.createAnchorPoint(-7, 3);
        cdt.setAnchorPoint(value);
        assertEquals(value, cdt.getAnchorPoint());
    }

    /**
     * Test of {@code Displacement} property.
     */
    @Test
    public void testDisplacement() {
        final var cdt = factory.createGraphic();

        // Check default
        assertEquals(factory.createDisplacement(), cdt.getDisplacement());

        // Check get/set
        var value = factory.createDisplacement(12, -5);
        cdt.setDisplacement(value);
        assertEquals(value, cdt.getDisplacement());
    }
}
