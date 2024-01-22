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
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests for {@link RasterSymbolizer}.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class RasterSymbolizerTest extends StyleTestCase {
    /**
     * Creates a new test case.
     */
    public RasterSymbolizerTest() {
    }

    /**
     * Test of {@code Opacity} property.
     */
    @Test
    public void testOpacity() {
        final var cdt = factory.createRasterSymbolizer();

        // Check default
        assertLiteralEquals(1.0, cdt.getOpacity());

        // Check get/set
        cdt.setOpacity(literal(0.7));
        assertLiteralEquals(0.7, cdt.getOpacity());
    }

    /**
     * Test of {@code ChannelSelection} property.
     */
    @Test
    public void testChannelSelection() {
        final var cdt = factory.createRasterSymbolizer();

        // Check default
        assertEmpty(cdt.getChannelSelection());

        // Check get/set
        var value = factory.createChannelSelection();
        cdt.setChannelSelection(value);
        assertOptionalEquals(value, cdt.getChannelSelection());
    }

    /**
     * Test of {@code OverlapBehavior} property.
     */
    @Test
    public void testOverlapBehavior() {
        final var cdt = factory.createRasterSymbolizer();

        // Check default
        assertNotNull(cdt.getOverlapBehavior());

        // Check get/set
        cdt.setOverlapBehavior(OverlapBehavior.EARLIEST_ON_TOP);
        assertEquals(OverlapBehavior.EARLIEST_ON_TOP, cdt.getOverlapBehavior());
    }

    /**
     * Test of {@code ColorMap} property.
     */
    @Test
    public void testColorMap() {
        final var cdt = factory.createRasterSymbolizer();

        // Check default
        assertEmpty(cdt.getColorMap());

        // Check get/set
        var value = factory.createColorMap();
        cdt.setColorMap(value);
        assertOptionalEquals(value, cdt.getColorMap());
    }

    /**
     * Test of {@code ContrastEnhancement} property.
     */
    @Test
    public void testGetContrastEnhancement() {
        final var cdt = factory.createRasterSymbolizer();

        // Check default
        assertEmpty(cdt.getContrastEnhancement());

        // Check get/set
        var value = factory.createContrastEnhancement();
        cdt.setContrastEnhancement(value);
        assertOptionalEquals(value, cdt.getContrastEnhancement());
    }

    /**
     * Test of {@code ShadedRelief} property.
     */
    @Test
    public void testGetShadedRelief() {
        final var cdt = factory.createRasterSymbolizer();

        // Check default
        assertEmpty(cdt.getShadedRelief());

        // Check get/set
        var value = factory.createShadedRelief();
        cdt.setShadedRelief(value);
        assertOptionalEquals(value, cdt.getShadedRelief());
    }

    /**
     * Test of {@code ImageOutline} property.
     */
    @Test
    public void testImageOutline() {
        final var cdt = factory.createRasterSymbolizer();

        // Check default
        assertEmpty(cdt.getImageOutline());

        // Check get/set
        var value = factory.createLineSymbolizer();
        cdt.setImageOutline(value);
        assertOptionalEquals(value, cdt.getImageOutline());
    }
}
