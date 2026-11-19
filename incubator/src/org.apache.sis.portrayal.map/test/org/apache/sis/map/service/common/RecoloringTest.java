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

import java.awt.Color;
import java.awt.image.BufferedImage;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests the image recoloring: {@link ImageTint}, {@link ColorSubstitution} and {@link ImageHalo}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class RecoloringTest {
    /**
     * Creates a new test case.
     */
    public RecoloringTest() {
    }

    /**
     * Creates an image of one row with the given non-premultiplied ARGB pixels.
     */
    private static BufferedImage row(final int... pixels) {
        final BufferedImage image = new BufferedImage(pixels.length, 1, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, pixels.length, 1, pixels, 0, pixels.length);
        return image;
    }

    /**
     * Tests the tint: white becomes the tint, black becomes the black tint, and the alpha test.
     */
    @Test
    public void testImageTint() {
        assertTrue (new ImageTint(Color.WHITE, Color.BLACK, 0).isIdentity());
        assertFalse(new ImageTint(Color.RED,   Color.BLACK, 0).isIdentity());
        assertFalse(new ImageTint(Color.WHITE, Color.BLACK, 0.5f).isIdentity());

        final ImageTint tint = new ImageTint(Color.RED, Color.BLUE, 0);
        final BufferedImage source = row(0xFFFFFFFF, 0x80000000, 0xFF808080);
        final BufferedImage result = tint.apply(source);
        assertEquals(0xFFFF0000, result.getRGB(0, 0), "White becomes the tint.");
        assertEquals(0x800000FF, result.getRGB(1, 0), "Black becomes the black tint, alpha unchanged.");
        assertEquals(0xFF80007F, result.getRGB(2, 0), "Gray is interpolated.");
        assertEquals(0xFFFFFFFF, source.getRGB(0, 0), "The source shall be unchanged.");

        final ImageTint threshold = new ImageTint(Color.WHITE, Color.BLACK, 0.6f);
        assertEquals(0,          threshold.recolor(0x80000000), "Alpha below the threshold.");
        assertEquals(0xA0000000, threshold.recolor(0xA0000000), "Alpha above the threshold.");
    }

    /**
     * Tests the color substitution: replaced and kept colors, alpha multiplication, transparent pixels.
     */
    @Test
    public void testColorSubstitution() {
        final ColorSubstitution substitution = new ColorSubstitution((color) -> {
            if (color.equals(Color.RED))   return Color.GREEN;
            if (color.equals(Color.WHITE)) return new Color(0, 0, 255, 128);
            return null;
        });
        assertEquals(0xFF00FF00, substitution.recolor(0xFFFF0000), "Replaced color.");
        assertEquals(0x8000FF00, substitution.recolor(0x80FF0000), "Alpha of the pixel kept.");
        assertEquals(0x800000FF, substitution.recolor(0xFFFFFFFF), "Alpha multiplied by the replacement alpha.");
        assertEquals(0xFF123456, substitution.recolor(0xFF123456), "No replacement.");
        assertEquals(0x00FF0000, substitution.recolor(0x00FF0000), "Transparent pixel unchanged.");

        final BufferedImage result = substitution.apply(row(0xFFFF0000, 0xFF123456));
        assertEquals(0xFF00FF00, result.getRGB(0, 0));
        assertEquals(0xFF123456, result.getRGB(1, 0));
    }

    /**
     * Tests the outline of the visible pixels of an image.
     */
    @Test
    public void testImageHalo() {
        final BufferedImage source = new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(1, 1, 0xFF000000);
        final ImageHalo halo = new ImageHalo(1, Color.WHITE);
        assertEquals(1, halo.margin());
        final BufferedImage result = halo.apply(source);
        assertEquals(5, result.getWidth());
        assertEquals(5, result.getHeight());
        assertEquals(0xFFFFFFFF, result.getRGB(2, 2), "Under the visible pixel.");
        assertEquals(0xFFFFFFFF, result.getRGB(3, 2), "At the size from the visible pixel.");
        assertEquals(0,          result.getRGB(3, 3) >>> 24, "Diagonal, farther than the size.");
        assertEquals(0,          result.getRGB(4, 2) >>> 24, "Farther than the size.");
    }
}
