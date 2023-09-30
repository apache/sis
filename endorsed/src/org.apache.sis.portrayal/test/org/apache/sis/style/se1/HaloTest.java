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

// Specific to the main branch:
import org.apache.sis.feature.AbstractFeature;


/**
 * Tests for {@link Halo}.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class HaloTest extends StyleTestCase {
    /**
     * Creates a new test case.
     */
    public HaloTest() {
    }

    /**
     * Test of {@code Fill} property.
     */
    @Test
    public void testFill() {
        final var cdt = factory.createHalo();

        // Check default
        Fill<AbstractFeature> value = cdt.getFill();
        assertEquals(factory.white, value.getColor());
        assertLiteralEquals(1.0, value.getOpacity());

        // Check get/set
        value.setColor(anyColor());
        value.setOpacity(literal(0.8));
        cdt.setFill(value);
        assertEquals(value, cdt.getFill());
    }

    /**
     * Test of {@code Radius} property.
     */
    @Test
    public void testRadius() {
        final var cdt = factory.createHalo();

        // Check default
        assertLiteralEquals(1.0, cdt.getRadius());

        // Check get/set
        cdt.setRadius(literal(40));
        assertLiteralEquals(40, cdt.getRadius());
    }
}
