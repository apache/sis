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
package org.apache.sis.map.service.se1;

import java.awt.image.BufferedImage;


/**
 * Relief shading (or “hill shading”) of an elevation model in display space.
 * The shading is computed with the slopes given by the Horn algorithm (3×3 neighborhood)
 * and a light coming from the upper-left corner of the display (azimuth 315°) at an altitude of 45°,
 * which is the usual cartographic convention.
 *
 * <p>The result is an overlay image to paint over the colors to shade: flat areas are fully transparent,
 * slopes facing away from the light are darkened by black pixels with an alpha proportional to the shadow,
 * and slopes facing the light are lightened by white pixels with an alpha proportional to the extra light.
 * Painting this overlay with the {@code SRC_OVER} rule multiplies the brightness of darkened colors
 * by the ratio between the light received by the slope and the light received by a flat area.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
final class ReliefShading {

    /**
     * Components of the unit vector toward the light, in display space (x right, y down, z up).
     * The light comes from azimuth 315° (north-west, upper-left of the display) at an altitude of 45°.
     */
    private static final double LIGHT_X = -0.5, LIGHT_Y = -0.5, LIGHT_Z = Math.sqrt(0.5);

    /**
     * Maximal alpha of the white pixels lightening the slopes facing the light.
     * Lower than 1 for avoiding to wash out the colors.
     */
    private static final double HIGHLIGHT = 0.5;

    private ReliefShading() {
    }

    /**
     * Computes the shading overlay of the given elevations.
     *
     * @param  elevations  elevations in metres, already multiplied by the relief factor,
     *                     in row-major order. NaN values are not shaded.
     * @param  width       number of display pixels in a row.
     * @param  height      number of display pixels in a column.
     * @param  cellSize    ground size of a display pixel, in metres.
     * @return the overlay to paint over the colors to shade.
     */
    static BufferedImage overlay(final double[] elevations, final int width, final int height, final double cellSize) {
        final int[] argb = new int[width * height];
        for (int y = 0; y < height; y++) {
            final int row0 = Math.max(y - 1, 0) * width;
            final int row1 = y * width;
            final int row2 = Math.min(y + 1, height - 1) * width;
            final int spanY = (row2 - row0) / width;
            for (int x = 0; x < width; x++) {
                final double e = elevations[row1 + x];
                if (Double.isNaN(e)) continue;
                final int x0 = Math.max(x - 1, 0);
                final int x2 = Math.min(x + 1, width - 1);
                final double a = at(elevations, row0 + x0, e), b = at(elevations, row0 + x, e), c = at(elevations, row0 + x2, e);
                final double d = at(elevations, row1 + x0, e),                                   f = at(elevations, row1 + x2, e);
                final double g = at(elevations, row2 + x0, e), h = at(elevations, row2 + x, e), i = at(elevations, row2 + x2, e);
                /*
                 * Horn algorithm: weighted differences over the 3×3 neighborhood. The spans are 2 pixels,
                 * except on the image borders where the neighborhood is truncated.
                 */
                final int spanX = x2 - x0;
                final double p = (spanX == 0) ? 0 : ((c + 2*f + i) - (a + 2*d + g)) / (4 * spanX * cellSize);   // ∂z/∂x
                final double q = (spanY == 0) ? 0 : ((g + 2*h + i) - (a + 2*b + c)) / (4 * spanY * cellSize);   // ∂z/∂y
                /*
                 * The surface normal is (−p, −q, 1) before normalization.
                 * The shade is the cosine of the angle between the normal and the light direction.
                 */
                final double shade = Math.max(0, (LIGHT_Z - p*LIGHT_X - q*LIGHT_Y) / Math.sqrt(p*p + q*q + 1));
                if (shade < LIGHT_Z) {
                    argb[row1 + x] = alpha(1 - shade / LIGHT_Z);                                    // Black.
                } else {
                    argb[row1 + x] = alpha(HIGHLIGHT * (shade - LIGHT_Z) / (1 - LIGHT_Z)) | 0xFFFFFF;  // White.
                }
            }
        }
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, argb, 0, width);
        return image;
    }

    /**
     * Returns the elevation at the given index, or the given fallback if the elevation is missing.
     */
    private static double at(final double[] elevations, final int index, final double fallback) {
        final double value = elevations[index];
        return Double.isNaN(value) ? fallback : value;
    }

    /**
     * Returns the given alpha value in [0 … 1] as the alpha bits of an ARGB value.
     */
    private static int alpha(final double alpha) {
        return (int) Math.round(Math.max(0, Math.min(1, alpha)) * 255) << 24;
    }
}
