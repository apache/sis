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
import org.junit.Test;
import static org.junit.Assert.*;

// Branch-dependent imports
import org.apache.sis.feature.AbstractFeature;


/**
 * Tests for {@link PolygonSymbolizer}.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.5
 * @since   1.5
 */
public final class PolygonSymbolizerTest extends StyleTestCase {
    /**
     * Creates a new test case.
     */
    public PolygonSymbolizerTest() {
    }

    /**
     * Test of {@code Stroke} property.
     */
    @Test
    public void testStroke() {
        final var cdt = factory.createPolygonSymbolizer();

        // Check default
        var value = cdt.getStroke().orElseThrow();
        assertLiteralEquals("bevel",  value.getLineJoin());
        assertLiteralEquals("square", value.getLineCap());

        // Check get/set
        value = factory.createStroke();
        cdt.setStroke(value);
        assertOptionalEquals(value, cdt.getStroke());
    }

    /**
     * Test of {@code Fill} property.
     */
    @Test
    public void testFill() {
        final var cdt = factory.createPolygonSymbolizer();

        // Check default
        Fill<AbstractFeature> value = cdt.getFill().orElseThrow();
        assertLiteralEquals(Color.GRAY, value.getColor());

        // Check get/set
        value = factory.createFill();
        value.setColorAndOpacity(ANY_COLOR);
        cdt.setFill(value);
        assertOptionalEquals(value, cdt.getFill());
    }

    /**
     * Test of {@code Displacement} property.
     */
    @Test
    public void testDisplacement() {
        final var cdt = factory.createPolygonSymbolizer();

        // Check default
        Displacement<AbstractFeature> value = cdt.getDisplacement();
        assertLiteralEquals(0.0, value.getDisplacementX());
        assertLiteralEquals(0.0, value.getDisplacementY());

        // Check get/set
        value = factory.createDisplacement(4, 1);
        cdt.setDisplacement(value);
        assertEquals(value, cdt.getDisplacement());
    }

    /**
     * Test of {@code PerpendicularOffset} property.
     */
    @Test
    public void testPerpendicularOffset() {
        final var cdt = factory.createPolygonSymbolizer();

        // Check default
        assertLiteralEquals(0.0, cdt.getPerpendicularOffset());

        // Check get/set
        cdt.setPerpendicularOffset(literal(10));
        assertLiteralEquals(10, cdt.getPerpendicularOffset());
    }
}
