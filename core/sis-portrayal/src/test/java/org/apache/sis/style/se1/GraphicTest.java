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

import org.junit.Test;
import static org.junit.Assert.*;


/**
 * Tests for {@link Graphic}.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.5
 * @since   1.5
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
        Graphic cdt = new Graphic();

        // Check defaults
        assertTrue(cdt.graphicalSymbols().isEmpty());

        // Check get/set
        cdt.graphicalSymbols().add(new Mark());
        assertEquals(1, cdt.graphicalSymbols().size());
    }

    /**
     * Test of {@code Opacity} property.
     */
    @Test
    public void testOpacity() {
        Graphic cdt = new Graphic();

        // Check default
        assertLiteralEquals(1.0, cdt.getOpacity());

        // Check get/set
        cdt.setOpacity(FF.literal(0.4));
        assertLiteralEquals(0.4, cdt.getOpacity());
    }

    /**
     * Test of {@code Size} property.
     */
    @Test
    public void testSize() {
        Graphic cdt = new Graphic();

        // Check default
        assertLiteralEquals(6.0, cdt.getSize());

        // Check get/set
        cdt.setSize(FF.literal(13));
        assertLiteralEquals(13, cdt.getSize());
    }

    /**
     * Test of {@code Rotation} property.
     */
    @Test
    public void testRotation() {
        Graphic cdt = new Graphic();

        // Check default
        assertLiteralEquals(0.0, cdt.getRotation());

        // Check get/set
        cdt.setRotation(FF.literal(90));
        assertLiteralEquals(90, cdt.getRotation());
    }

    /**
     * Test of {@code AnchorPoint} property.
     */
    @Test
    public void testAnchorPoint() {
        Graphic cdt = new Graphic();

        // Check default
        assertEquals(new AnchorPoint(), cdt.getAnchorPoint());

        // Check get/set
        var value = new AnchorPoint(-7, 3);
        cdt.setAnchorPoint(value);
        assertEquals(value, cdt.getAnchorPoint());
    }

    /**
     * Test of {@code Displacement} property.
     */
    @Test
    public void testDisplacement() {
        Graphic cdt = new Graphic();

        // Check default
        assertEquals(new Displacement(), cdt.getDisplacement());

        // Check get/set
        var value = new Displacement(12, -5);
        cdt.setDisplacement(value);
        assertEquals(value, cdt.getDisplacement());
    }
}
