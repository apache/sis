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

import java.awt.Color;

// Test dependencies
import org.junit.Test;

import static org.junit.Assert.*;

// Specific to the main branch:
import org.apache.sis.feature.AbstractFeature;


/**
 * Tests for {@link LineSymbolizer}.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.5
 * @since   1.5
 */
public final class LineSymbolizerTest extends StyleTestCase {
    /**
     * Creates a new test case.
     */
    public LineSymbolizerTest() {
    }

    /**
     * Test of {@code Stroke} property.
     */
    @Test
    public void testStroke() {
        final var cdt = factory.createLineSymbolizer();

        // Check default
        Stroke<AbstractFeature> value = cdt.getStroke();
        assertLiteralEquals(Color.BLACK, value.getColor());

        // Check get/set
        value = factory.createStroke();
        cdt.setStroke(value);
        assertEquals(value, cdt.getStroke());
    }

    /**
     * Test of {@code PerpendicularOffset} property.
     */
    @Test
    public void testPerpendicularOffset() {
        final var cdt = factory.createLineSymbolizer();

        // Check default
        assertLiteralEquals(0.0, cdt.getPerpendicularOffset());

        // Check get/set
        cdt.setPerpendicularOffset(literal(20));
        assertLiteralEquals(20, cdt.getPerpendicularOffset());
    }
}
