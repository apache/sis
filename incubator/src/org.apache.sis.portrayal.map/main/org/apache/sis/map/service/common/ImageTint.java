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


/**
 * Recoloring and alpha test of an image.
 * Each red, green and blue component <var>v</var> between 0 and 1 of each pixel becomes
 * {@code tint} × <var>v</var> + {@code blackTint} × (1 − <var>v</var>).
 * Pixels whose alpha value is below the threshold become fully transparent.
 *
 * @param tint            the color replacing white.
 * @param blackTint       the color replacing black.
 * @param alphaThreshold  the lowest alpha value, between 0 and 1, of the pixels which are kept.
 *
 * @see "OGC 18-067r4 section 9.2.1.7"
 *
 * @author Johann Sorel (Geomatys)
 */
public record ImageTint(Color tint, Color blackTint, float alphaThreshold) {
    /**
     * Returns whether this operation leaves images unchanged.
     *
     * @return whether the tint is white, the black tint is black and there is no alpha test.
     */
    public boolean isIdentity() {
        return (tint.getRGB() & 0xFFFFFF) == 0xFFFFFF && (blackTint.getRGB() & 0xFFFFFF) == 0 && !(alphaThreshold > 0);
    }

    /**
     * Returns the new color of a pixel, or a fully transparent pixel if its alpha is below the threshold.
     *
     * @param  argb  the color of the pixel, as a non-premultiplied ARGB value.
     * @return the recolored pixel.
     */
    public int recolor(final int argb) {
        final int alpha = argb >>> 24;
        if (alpha == 0 || alpha < Math.ceil(alphaThreshold * 255)) {
            return 0;
        }
        return (alpha << 24)
             | (blend(argb >>> 16, tint.getRed(),   blackTint.getRed())   << 16)
             | (blend(argb >>>  8, tint.getGreen(), blackTint.getGreen()) <<  8)
             |  blend(argb,        tint.getBlue(),  blackTint.getBlue());
    }

    /**
     * Returns a recolored copy of the given image.
     *
     * @param  source  the image to recolor, which is not modified.
     * @return the recolored image.
     */
    public BufferedImage apply(final BufferedImage source) {
        final int width  = source.getWidth();
        final int height = source.getHeight();
        final int[] pixels = source.getRGB(0, 0, width, height, null, 0, width);
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = recolor(pixels[i]);
        }
        final BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        result.setRGB(0, 0, width, height, pixels, 0, width);
        return result;
    }

    /**
     * Interpolates a color component between the black tint and the tint.
     *
     * @param  value  the component in the 8 lowest bits.
     * @param  white  the component of the tint, between 0 and 255.
     * @param  black  the component of the black tint, between 0 and 255.
     * @return the recolored component, between 0 and 255.
     */
    private int blend(final int value, final int white, final int black) {
        final int v = value & 0xFF;
        return (white * v + black * (255 - v) + 127) / 255;
    }
}
