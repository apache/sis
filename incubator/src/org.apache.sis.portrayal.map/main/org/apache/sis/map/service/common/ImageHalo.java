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
 * The outline of an image: the visible pixels of the image extended outward by a size, in a single color.
 * The alpha value of each outline pixel is the highest alpha value of the image pixels within the size.
 * The outline image is larger than the image by a {@linkplain #margin() margin} on each side.
 *
 * @param size   how far the outline extends outward from the visible pixels, in pixels.
 * @param color  the color of the outline.
 *
 * @see "OGC CartoSym Part 2 (draft) section 11"
 *
 * @author Johann Sorel (Geomatys)
 */
public record ImageHalo(double size, Color color) {
    /**
     * Returns the number of pixels added on each side of the image.
     *
     * @return the margin of the outline image.
     */
    public int margin() {
        return (int) Math.ceil(size);
    }

    /**
     * Returns the outline of the given image.
     *
     * @param  source  the image around which to draw the outline.
     * @return the outline, larger than the image by the margin on each side.
     */
    public BufferedImage apply(final BufferedImage source) {
        final int margin = margin();
        final int width  = source.getWidth();
        final int height = source.getHeight();
        final int stride = width + 2 * margin;
        final int[] pixels = source.getRGB(0, 0, width, height, null, 0, width);
        final int[] halo = new int[stride * (height + 2 * margin)];
        final double limit = size * size;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int alpha = pixels[y * width + x] >>> 24;
                if (alpha != 0) {
                    for (int dy = -margin; dy <= margin; dy++) {
                        for (int dx = -margin; dx <= margin; dx++) {
                            if (dx * dx + dy * dy <= limit) {
                                final int i = (y + margin + dy) * stride + (x + margin + dx);
                                halo[i] = Math.max(halo[i], alpha);
                            }
                        }
                    }
                }
            }
        }
        final int rgb = color.getRGB() & 0xFFFFFF;
        for (int i = 0; i < halo.length; i++) {
            halo[i] = (halo[i] << 24) | rgb;
        }
        final BufferedImage result = new BufferedImage(stride, height + 2 * margin, BufferedImage.TYPE_INT_ARGB);
        result.setRGB(0, 0, stride, height + 2 * margin, halo, 0, stride);
        return result;
    }
}
