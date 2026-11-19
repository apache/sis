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
package org.apache.sis.map.service.common;

import java.awt.Font;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link TextShape}, {@link ShapeHalo} and {@link FontFamily}.
 * Since glyph shapes depend on the installed fonts, assertions are done on bounds with tolerances.
 * The texts use the letter "H", which has no descender.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class TextShapeTest {
    /**
     * The font of the texts: sans-serif of 20 pixels.
     */
    private static final Font FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 1).deriveFont(20f);

    /**
     * Creates a new test case.
     */
    public TextShapeTest() {
    }

    /**
     * Tests the alignment of a straight text relative to its anchor.
     */
    @Test
    public void testOutline() {
        final TextShape text = new TextShape("HH", FONT);
        Rectangle2D b = text.outline(0, 0.5).getBounds2D();
        assertTrue(b.getMinX() >= 0 && b.getMinX() < 5, "Text expected to start at the anchor: " + b);
        assertTrue(b.getMinY() < 0 && b.getMaxY() > 0, "Text expected vertically centered: " + b);
        b = text.outline(1, 0.5).getBounds2D();
        assertTrue(b.getMaxX() <= 0 && b.getMaxX() > -5, "Text expected to end at the anchor: " + b);
        b = text.outline(0.5, 0).getBounds2D();
        assertTrue(b.getMaxY() <= 0, "Text expected above the anchor: " + b);
        b = text.outline(0.5, 1).getBounds2D();
        assertTrue(b.getMinY() >= 0, "Text expected below the anchor: " + b);
        assertEquals(b.getWidth(), text.getAdvance(), 5, "Advance");
    }

    /**
     * Tests a text of two lines.
     */
    @Test
    public void testLines() {
        final TextShape single = new TextShape("HH", FONT);
        final TextShape lines  = new TextShape("HH\nHH", FONT);
        assertTrue(lines.getHeight() > 2 * single.getHeight() - 1, "Height of two lines.");
        final Rectangle2D b1 = single.outline(0.5, 0.5).getBounds2D();
        final Rectangle2D b2 = lines .outline(0.5, 0.5).getBounds2D();
        assertTrue(b2.getHeight() > 1.5 * b1.getHeight(), "Second line expected below the first one.");
        assertEquals(b1.getWidth(), b2.getWidth(), 1E-6, "Lines of the same width.");
        assertEquals(new TextShape("HH HH", FONT).getAdvance(), lines.getAdvance(), 1E-6,
                     "Line breaks along a line are spaces.");
    }

    /**
     * Tests a text written along a horizontal line, and the boxes of its glyphs.
     */
    @Test
    public void testAlongLine() {
        final TextShape text = new TextShape("HH", FONT);
        final LineWalker walker = LineWalker.of(new Line2D.Double(0, 50, 100, 50)).get(0);
        final Rectangle2D along = text.alongLine(walker, 10).getBounds2D();
        final Rectangle2D straight = text.outline(0, 0.5).getBounds2D();
        assertEquals(straight.getMinX() + 10, along.getMinX(), 1, "Start");
        assertEquals(straight.getMaxX() + 10, along.getMaxX(), 1, "End");
        assertEquals(straight.getCenterY() + 50, along.getCenterY(), 1, "Text expected centered on the line.");

        final List<LabelBox> boxes = text.boxesAlongLine(walker, 10, 0);
        assertEquals(2, boxes.size(), "One box per glyph.");
        final LabelBox probe = new LabelBox(new Rectangle2D.Double(0, 0, 1, 1), along.getCenterX(), along.getCenterY(), 0);
        assertTrue(boxes.stream().anyMatch(probe::intersects));
        final LabelBox far = new LabelBox(new Rectangle2D.Double(0, 0, 1, 1), 90, 50, 0);
        assertFalse(boxes.stream().anyMatch(far::intersects));
    }

    /**
     * Tests the halo of a shape.
     */
    @Test
    public void testShapeHalo() {
        final Shape halo = new ShapeHalo(2).apply(new Rectangle2D.Double(0, 0, 10, 10));
        final Rectangle2D bounds = halo.getBounds2D();
        assertEquals(-2, bounds.getMinX(), 1E-6);
        assertEquals(-2, bounds.getMinY(), 1E-6);
        assertEquals(12, bounds.getMaxX(), 1E-6);
        assertEquals(12, bounds.getMaxY(), 1E-6);
        assertTrue (new ShapeHalo(0).isEmpty());
        assertTrue (new ShapeHalo(Double.NaN).isEmpty());
        assertTrue (new ShapeHalo(Double.POSITIVE_INFINITY).isEmpty());
        assertFalse(new ShapeHalo(0.5).isEmpty());
    }

    /**
     * Tests the resolution of font family names.
     */
    @Test
    public void testFontFamily() {
        assertEquals(Font.SANS_SERIF, new FontFamily(" Sans-Serif ").javaName());
        assertEquals(Font.MONOSPACED, new FontFamily("monospace").javaName());
        assertNull(new FontFamily(null).javaName());
        assertNull(new FontFamily("No such font family").javaName());
    }
}
