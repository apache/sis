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


/**
 * Tests for {@link LinePlacement}.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class LinePlacementTest extends StyleTestCase {
    /**
     * Creates a new test case.
     */
    public LinePlacementTest() {
    }

    /**
     * Test of {@code PerpendicularOffset} property.
     */
    @Test
    public void testPerpendicularOffset() {
        final var cdt = factory.createLinePlacement();

        // Check default
        assertLiteralEquals(0.0, cdt.getPerpendicularOffset());

        // Check get/set
        cdt.setPerpendicularOffset(literal(15));
        assertLiteralEquals(15, cdt.getPerpendicularOffset());
    }

    /**
     * Test of {@code InitialGap} property.
     */
    @Test
    public void testInitialGap() {
        final var cdt = factory.createLinePlacement();

        // Check default
        assertLiteralEquals(0.0, cdt.getInitialGap());

        // Check get/set
        cdt.setInitialGap(literal(6));
        assertLiteralEquals(6, cdt.getInitialGap());
    }

    /**
     * Test of {@code Gap} property.
     */
    @Test
    public void testGap() {
        final var cdt = factory.createLinePlacement();

        // Check default
        assertLiteralEquals(0.0, cdt.getGap());

        // Check get/set
        cdt.setGap(literal(9));
        assertLiteralEquals(9, cdt.getGap());
    }

    /**
     * Test of {@code IsRepeated} property.
     */
    @Test
    public void testIsRepeated() {
        final var cdt = factory.createLinePlacement();

        // Check default
        assertLiteralEquals(Boolean.FALSE, cdt.isRepeated());

        // Check get/set
        cdt.setRepeated(literal(true));
        assertLiteralEquals(Boolean.TRUE, cdt.isRepeated());
    }

    /**
     * Test of {@code IsAligned} property.
     */
    @Test
    public void testIsAligned() {
        final var cdt = factory.createLinePlacement();

        // Check default
        assertLiteralEquals(Boolean.TRUE, cdt.isAligned());

        // Check get/set
        cdt.setAligned(literal(false));
        assertLiteralEquals(Boolean.FALSE, cdt.isAligned());
    }

    /**
     * Test of {@code GeneralizeLine} property.
     */
    @Test
    public void testGeneralizeLine() {
        final var cdt = factory.createLinePlacement();

        // Check default
        assertLiteralEquals(Boolean.FALSE, cdt.getGeneralizeLine());

        // Check get/set
        cdt.setGeneralizeLine(literal(true));
        assertLiteralEquals(Boolean.TRUE, cdt.getGeneralizeLine());
    }
}
