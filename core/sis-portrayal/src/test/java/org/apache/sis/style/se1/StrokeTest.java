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


/**
 * Tests for {@link Stroke}.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.5
 * @since   1.5
 */
public final class StrokeTest extends StyleTestCase {
    /**
     * Creates a new test case.
     */
    public StrokeTest() {
    }

    /**
     * Test of {@code GraphicFill} property.
     */
    @Test
    public void testGraphicFill() {
        Stroke cdt = new Stroke();

        // Check default
        assertEmpty(cdt.getGraphicFill());

        // Check get/set
        GraphicFill value = new GraphicFill();
        cdt.setGraphicFill(value);
        assertOptionalEquals(value, cdt.getGraphicFill());
    }

    /**
     * Test of {@code GraphicStroke} property.
     */
    @Test
    public void testGraphicStroke() {
        Stroke cdt = new Stroke();

        // Check default
        assertEmpty(cdt.getGraphicStroke());

        // Check get/set
        GraphicStroke value = new GraphicStroke();
        cdt.setGraphicStroke(value);
        assertOptionalEquals(value, cdt.getGraphicStroke());
    }

    /**
     * Test of {@code Color} property.
     */
    @Test
    public void testColor() {
        Stroke cdt = new Stroke();

        // Check default
        assertLiteralEquals(Color.BLACK, cdt.getColor());

        // Check get/set
        cdt.setColor(anyColor());
        assertLiteralEquals(ANY_COLOR, cdt.getColor());
    }

    /**
     * Test of {@code Opacity} property.
     */
    @Test
    public void testOpacity() {
        Stroke cdt = new Stroke();

        // Check default
        assertLiteralEquals(1.0, cdt.getOpacity());

        // Check get/set
        cdt.setOpacity(FF.literal(0.7));
        assertLiteralEquals(0.7, cdt.getOpacity());
    }

    /**
     * Test of the construct setting {@code Color} and {@code Opacity} together.
     */
    @Test
    public void testColorAndOpacity() {
        Stroke cdt = new Stroke(new Color(255, 255, 0, 128));
        assertLiteralEquals(Color.YELLOW, cdt.getColor());
        assertLiteralEquals(0.5, cdt.getOpacity());
    }

    /**
     * Test of {@code Width} property.
     */
    @Test
    public void testWidth() {
        Stroke cdt = new Stroke();

        // Check default
        assertLiteralEquals(1.0, cdt.getWidth());

        // Check get/set
        cdt.setWidth(FF.literal(14));
        assertLiteralEquals(14, cdt.getWidth());
    }

    /**
     * Test of {@code LineJoin} property.
     */
    @Test
    public void testLineJoin() {
        Stroke cdt = new Stroke();

        // Check default
        assertLiteralEquals("bevel", cdt.getLineJoin());

        // Check get/set
        var value = FF.literal("A random join");
        cdt.setLineJoin(value);
        assertEquals(value, cdt.getLineJoin());
    }

    /**
     * Test of {@code LineCap} property.
     */
    @Test
    public void testLineCap() {
        Stroke cdt = new Stroke();

        // Check default
        assertLiteralEquals("square", cdt.getLineCap());

        // Check get/set
        var value = FF.literal("A random cap");
        cdt.setLineCap(value);
        assertEquals(value, cdt.getLineCap());
    }

    /**
     * Test of {@code DashArray} property.
     */
    @Test
    public void testDashArray() {
        Stroke cdt = new Stroke();

        // Check default
        assertEmpty(cdt.getDashArray());

        // Check get/set
        final var value = new float[] {1,2,3};
        cdt.setDashArray(FF.literal(value));
        assertLiteralEquals(value, cdt.getDashArray().orElseThrow());
    }

    /**
     * Test of {@code DashOffset} property.
     */
    @Test
    public void testDashOffset() {
        Stroke cdt = new Stroke();

        // Check default
        assertLiteralEquals(0, cdt.getDashOffset());

        // Check get/set
        cdt.setDashOffset(FF.literal(21));
        assertLiteralEquals(21, cdt.getDashOffset());
    }
}
