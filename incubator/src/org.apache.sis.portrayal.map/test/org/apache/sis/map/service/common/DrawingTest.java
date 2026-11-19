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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Consumer;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests the drawings: {@link DrawingShape}, {@link DrawingImage} and {@link DrawingComposite}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class DrawingTest {
    /**
     * Creates a new test case.
     */
    public DrawingTest() {
    }

    /**
     * Paints with the given action on a transparent image of 20×20 pixels.
     */
    private static BufferedImage paint(final Consumer<Graphics2D> painter) {
        final BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = image.createGraphics();
        try {
            painter.accept(g);
        } finally {
            g.dispose();
        }
        return image;
    }

    /**
     * Tests a filled and stroked shape: painting, opacity, restoration of the graphics state and hit tests.
     */
    @Test
    public void testShape() {
        final DrawingShape drawing = new DrawingShape(new Rectangle(5, 5, 10, 10))
                .setFill(Color.RED, 1)
                .setStroke(new BasicStroke(2), Color.BLUE, 0.5f);
        final BufferedImage image = paint((g) -> {
            drawing.paint(g);
            assertEquals(new BasicStroke(), g.getStroke(), "The graphics state shall be restored.");
        });
        assertEquals(0xFFFF0000, image.getRGB(10, 10), "Opaque fill.");
        assertEquals(0x80, image.getRGB(15, 10) >>> 24 & 0xFF, 1, "Translucent stroke.");
        assertEquals(0, image.getRGB(1, 1), "Outside the shape.");

        assertTrue (drawing.intersects(new Rectangle(9, 9, 2, 2)), "Inside the fill.");
        assertTrue (drawing.intersects(new Rectangle(15, 0, 1, 20)), "On the stroke.");
        assertFalse(drawing.intersects(new Rectangle(0, 0, 3, 3)));

        final DrawingShape line = new DrawingShape(new Line2D.Double(0, 10, 20, 10)).setStroke(new BasicStroke(2), Color.BLUE, 1);
        assertTrue (line.intersects(new Ellipse2D.Double(8, 9, 4, 4)), "Non-rectangular mask on the stroke.");
        assertFalse(line.intersects(new Ellipse2D.Double(8, 14, 4, 4)));
        assertFalse(new DrawingShape().setFill(Color.RED, 1).intersects(new Rectangle(0, 0, 20, 20)), "No shape.");
    }

    /**
     * Tests a shape filled with layers: a white background, then horizontal hatches every 5 pixels.
     */
    @Test
    public void testFillLayers() {
        final DrawingShape drawing = new DrawingShape(new Rectangle(0, 0, 20, 20)).setFill(List.of(
                new FillLayer.Paint(Color.WHITE, 1),
                new FillLayer.Hatch(Color.BLACK, 1, 2, 0, 5)));
        final BufferedImage image = paint((g) -> {
            drawing.paint(g);
            assertNull(g.getClip(), "The clip of the hatch shall be restored.");
        });
        assertEquals(0xFF000000, image.getRGB(10, 10), "Hatch line at y = 10.");
        assertEquals(0xFFFFFFFF, image.getRGB(10, 12), "Background between two lines.");
        assertTrue(drawing.intersects(new Rectangle(12, 12, 1, 1)));
    }

    /**
     * Tests an image drawn at several places: painting, opacity and hit tests on the visible pixels.
     */
    @Test
    public void testImage() {
        final BufferedImage symbol = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        symbol.setRGB(1, 1, 0xFF00FF00);
        final DrawingImage drawing = new DrawingImage(symbol, 0.5f)
                .add(AffineTransform.getTranslateInstance(2, 2))
                .add(AffineTransform.getTranslateInstance(10, 10));
        assertFalse(drawing.isEmpty());
        final BufferedImage image = paint(drawing::paint);
        assertEquals(0x80, image.getRGB(3, 3)   >>> 24, 1, "Translucent pixel of the first placement.");
        assertEquals(0x80, image.getRGB(11, 11) >>> 24, 1, "Translucent pixel of the second placement.");

        assertTrue (drawing.intersects(new Rectangle(11, 11, 1, 1)), "Visible pixel.");
        assertFalse(drawing.intersects(new Rectangle(12, 12, 2, 2)), "Transparent pixels.");
        assertFalse(drawing.intersects(new Rectangle(0, 0, 2, 2)), "Outside the images.");

        final AffineTransform rotation = AffineTransform.getTranslateInstance(10, 10);
        rotation.quadrantRotate(1);
        final DrawingImage rotated = new DrawingImage(symbol, 1).add(rotation);
        assertTrue (rotated.intersects(new Rectangle(8, 11, 1, 1)), "Visible pixel after rotation by 90°.");
        assertFalse(rotated.intersects(new Rectangle(11, 11, 1, 1)));

        final DrawingImage fixed = new DrawingImage(symbol, 5, 5);
        assertTrue(fixed.intersects(new Rectangle(6, 6, 1, 1)), "Image at a fixed location.");
    }

    /**
     * Tests the composition of drawings.
     */
    @Test
    public void testComposite() {
        final Drawing a = new DrawingShape(new Rectangle(0, 0, 5, 5)).setFill(Color.RED, 1);
        final Drawing b = new DrawingShape(new Rectangle(10, 10, 5, 5)).setFill(Color.BLUE, 1);
        assertNull(DrawingComposite.of(null, null));
        assertSame(a, DrawingComposite.of(null, a));
        final Drawing both = DrawingComposite.of(a, b);
        assertTrue (both.intersects(new Rectangle(12, 12, 1, 1)));
        assertFalse(both.intersects(new Rectangle(7, 7, 1, 1)));
        assertFalse(new DrawingComposite(List.of()).intersects(new Rectangle(0, 0, 20, 20)));
        final BufferedImage image = paint(both::paint);
        assertEquals(0xFF0000FF, image.getRGB(12, 12));
    }

    /**
     * Tests the hit tests of a filled shape with rectangular and non-rectangular masks.
     */
    @Test
    public void testHitArea() {
        final DrawingShape area = new DrawingShape(new Ellipse2D.Double(0, 0, 10, 10)).setFill(Color.RED, 1);
        assertTrue (area.intersects(new Rectangle(4, 4, 2, 2)));
        assertFalse(area.intersects(new Rectangle(0, 0, 1, 1)), "Corner outside the circle.");
        assertFalse(area.intersects(new Ellipse2D.Double(9, 9, 2, 2)));
    }
}
