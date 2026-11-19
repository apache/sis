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
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;


/**
 * Replacement of the colors of an image by the colors given by a function.
 * The function receives the opaque color of a pixel and returns its replacement,
 * or {@code null} for keeping the color unchanged.
 *
 * <ul>
 *   <li>The alpha of each pixel is multiplied by the alpha of its replacement color.</li>
 *   <li>Fully transparent pixels are unchanged.</li>
 *   <li>The replacement of each color is computed once.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ColorSubstitution {
    /**
     * The function giving the replacement of an opaque color, or {@code null} for no replacement.
     */
    private final Function<Color, Color> replacement;

    /**
     * The opaque ARGB replacement of each RGB color already seen.
     */
    private final Map<Integer, Integer> cache;

    /**
     * Creates a color substitution.
     *
     * @param  replacement  the function giving the replacement of an opaque color, or {@code null} for no replacement.
     */
    public ColorSubstitution(final Function<Color, Color> replacement) {
        this.replacement = Objects.requireNonNull(replacement, "replacement");
        cache = new ConcurrentHashMap<>();
    }

    /**
     * Returns the new color of a pixel.
     *
     * @param  argb  the color of the pixel, as a non-premultiplied ARGB value.
     * @return the replacement color with the alpha of the pixel multiplied by the alpha of the replacement.
     */
    public int recolor(final int argb) {
        final int alpha = argb >>> 24;
        if (alpha == 0) {
            return argb;
        }
        final int substitute = cache.computeIfAbsent(argb & 0xFFFFFF, (rgb) -> {
            final Color color = replacement.apply(new Color(rgb));
            return (color != null) ? color.getRGB() : 0xFF000000 | rgb;
        });
        final int a = alpha * (substitute >>> 24) / 255;
        return (a << 24) | (substitute & 0xFFFFFF);
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
}
