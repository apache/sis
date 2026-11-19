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
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;


/**
 * A gradient whose colors vary with the angle around a center, from 0 to 100 percent of a full turn.
 * The turn starts in the direction of the start angle and goes counter-clockwise on screen,
 * which is the convention of the gradient angle.
 *
 * @see "OGC CartoSym Part 2 (draft) section 10.2"
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ConicGradientPaint implements Paint {
    /**
     * Center of the gradient, in user space.
     */
    private final Point2D center;

    /**
     * Direction of the start of the gradient, in radians counter-clockwise on screen from the right.
     */
    private final double start;

    /**
     * Positions of the colors along the turn, strictly increasing between 0 and 1.
     */
    private final float[] fractions;

    /**
     * Colors at the positions given by {@link #fractions}.
     */
    private final Color[] colors;

    /**
     * Creates a conic gradient.
     *
     * @param  center     center of the gradient, in user space.
     * @param  start      direction of the start, in radians counter-clockwise on screen from the right.
     * @param  fractions  positions of the colors along the turn, strictly increasing between 0 and 1.
     * @param  colors     colors at the given positions.
     */
    public ConicGradientPaint(final Point2D center, final double start, final float[] fractions, final Color[] colors) {
        this.center    = center;
        this.start     = start;
        this.fractions = fractions.clone();
        this.colors    = colors.clone();
    }

    @Override
    public PaintContext createContext(final ColorModel cm, final Rectangle deviceBounds, final Rectangle2D userBounds,
                                      final AffineTransform xform, final RenderingHints hints)
    {
        AffineTransform deviceToUser;
        try {
            deviceToUser = xform.createInverse();
        } catch (NoninvertibleTransformException e) {
            deviceToUser = new AffineTransform();
        }
        return new Context(deviceToUser);
    }

    @Override
    public int getTransparency() {
        for (final Color color : colors) {
            if (color.getAlpha() != 255) {
                return Transparency.TRANSLUCENT;
            }
        }
        return Transparency.OPAQUE;
    }

    /**
     * Returns the color at the given position, as non-premultiplied ARGB.
     *
     * @param  x  horizontal coordinate in user space.
     * @param  y  vertical coordinate in user space, increasing downward.
     * @return the color at the given position.
     */
    private int argb(final double x, final double y) {
        double turn = (Math.atan2(center.getY() - y, x - center.getX()) - start) / (2 * Math.PI);
        turn -= Math.floor(turn);
        if (!(turn > fractions[0])) {
            return colors[0].getRGB();
        }
        for (int i = 1; i < fractions.length; i++) {
            if (turn <= fractions[i]) {
                final double t = (turn - fractions[i-1]) / (fractions[i] - fractions[i-1]);
                return interpolate(colors[i-1].getRGB(), colors[i].getRGB(), t);
            }
        }
        return colors[colors.length - 1].getRGB();
    }

    /**
     * Interpolates linearly each ARGB component between the given colors.
     */
    private int interpolate(final int c0, final int c1, final double t) {
        int result = 0;
        for (int shift = 0; shift < Integer.SIZE; shift += Byte.SIZE) {
            final int a = (c0 >>> shift) & 0xFF;
            final int b = (c1 >>> shift) & 0xFF;
            result |= ((int) Math.round(a + (b - a) * t) & 0xFF) << shift;
        }
        return result;
    }

    /**
     * Computes the pixels of this gradient in device space.
     */
    private final class Context implements PaintContext {
        /**
         * Conversion from device space to the user space of the gradient.
         */
        private final AffineTransform deviceToUser;

        /**
         * Creates a context for this gradient.
         *
         * @param  deviceToUser  conversion from device space to the user space of the gradient.
         */
        Context(final AffineTransform deviceToUser) {
            this.deviceToUser = deviceToUser;
        }

        @Override
        public void dispose() {
        }

        @Override
        public ColorModel getColorModel() {
            return ColorModel.getRGBdefault();
        }

        /**
         * Returns the colors of the given device pixels, evaluated at the pixel centers.
         */
        @Override
        public Raster getRaster(final int x, final int y, final int w, final int h) {
            final WritableRaster raster = getColorModel().createCompatibleWritableRaster(w, h);
            final int[] row = new int[w];
            final Point2D.Double point = new Point2D.Double();
            for (int j = 0; j < h; j++) {
                for (int i = 0; i < w; i++) {
                    point.setLocation(x + i + 0.5, y + j + 0.5);
                    deviceToUser.transform(point, point);
                    row[i] = argb(point.x, point.y);
                }
                raster.setDataElements(0, j, w, 1, row);
            }
            return raster;
        }
    }
}
