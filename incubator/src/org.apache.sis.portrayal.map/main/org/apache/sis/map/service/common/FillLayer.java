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
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Objects;


/**
 * One of the layers painted for filling a shape: color, pattern, gradient, hatch, dot pattern or stipple.
 * The caller gives each layer its own copy of the graphics, so a layer may change the state of the graphics.
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract sealed class FillLayer permits FillLayer.Paint, FillLayer.DotPattern, FillLayer.Hatch, FillLayer.Stipple {
    /**
     * Paints this layer inside the given shape.
     *
     * @param  graphics  where to paint. Its state may be modified.
     * @param  shape     the area to fill, in the user space of the graphics.
     */
    protected abstract void fill(Graphics2D graphics, Shape shape);

    /**
     * A fill layer painting a color, a texture or a gradient over the whole shape.
     */
    public static final class Paint extends FillLayer {
        /**
         * The color, texture or gradient.
         */
        private final java.awt.Paint paint;

        /**
         * Opacity multiplied with the paint, between 0 and 1.
         */
        private final float opacity;

        /**
         * Creates a layer filling with the given paint.
         *
         * @param  paint    the color, texture or gradient.
         * @param  opacity  opacity multiplied with the paint, between 0 and 1.
         */
        public Paint(final java.awt.Paint paint, final float opacity) {
            this.paint   = Objects.requireNonNull(paint, "paint");
            this.opacity = Math.max(0, Math.min(1, opacity));
        }

        /**
         * Creates a layer repeating the given image as tiles of the image size.
         * The tiles are aligned on the origin of the display, so that the patterns of adjacent shapes join.
         *
         * @param  tile     the image to repeat.
         * @param  opacity  opacity multiplied with the image, between 0 and 1.
         */
        public Paint(final BufferedImage tile, final float opacity) {
            this(new TexturePaint(tile, new Rectangle2D.Double(0, 0, tile.getWidth(), tile.getHeight())), opacity);
        }

        @Override
        public void fill(final Graphics2D graphics, final Shape shape) {
            Drawing.multiplyOpacity(graphics, opacity);
            graphics.setPaint(paint);
            graphics.fill(shape);
        }
    }

    /**
    * A fill layer made of round dots on a regular grid, clipped to the shape.
    * The dot centers are at the middle of the grid cells, and the grid is aligned on the origin
    * of the user space so that the dots of adjacent shapes are aligned.
    *
    * <ul>
    *   <li>The standard does not define the size of the dots. The diameter is computed by the caller.</li>
    *   <li>Distances smaller than {@value #MINIMUM_DISTANCE} pixel are replaced by that minimum.</li>
    * </ul>
    *
    * @see "OGC 18-067r4 section 13.2"
    */
   public static final class DotPattern extends FillLayer {
       /**
        * Minimal distance between two dots, in pixels, for keeping the number of dots reasonable.
        */
       private static final double MINIMUM_DISTANCE = 1;

       /**
        * Color of the dots, opaque.
        */
       private final Color color;

       /**
        * Opacity of the dots, between 0 and 1.
        */
       private final float opacity;

       /**
        * Horizontal and vertical distances between the centers of two dots, in pixels.
        */
       private final double horizontal, vertical;

       /**
        * Diameter of the dots, in pixels.
        */
       private final double size;

       /**
        * Creates a dot pattern.
        *
        * @param  color       color of the dots.
        * @param  opacity     opacity of the dots, between 0 and 1.
        * @param  horizontal  horizontal distance between the centers of two dots, in pixels.
        * @param  vertical    vertical distance between the centers of two dots, in pixels.
        * @param  size        diameter of the dots, in pixels.
        */
       public DotPattern(final Color color, final float opacity, final double horizontal, final double vertical, final double size) {
           this.color      = color;
           this.opacity    = opacity;
           this.horizontal = Math.max(horizontal, MINIMUM_DISTANCE);
           this.vertical   = Math.max(vertical,   MINIMUM_DISTANCE);
           this.size       = size;
       }

       @Override
       public void fill(final Graphics2D graphics, final Shape shape) {
           if (!(size > 0)) {
               return;
           }
           graphics.clip(shape);
           final Rectangle2D region = graphics.getClip().getBounds2D();
           Drawing.multiplyOpacity(graphics, opacity);
           graphics.setPaint(color);
           final double radius = size / 2;
           final long firstColumn = (long) Math.floor((region.getMinX() - radius) / horizontal - 0.5);
           final long lastColumn  = (long) Math.ceil ((region.getMaxX() + radius) / horizontal - 0.5);
           final long firstRow    = (long) Math.floor((region.getMinY() - radius) / vertical   - 0.5);
           final long lastRow     = (long) Math.ceil ((region.getMaxY() + radius) / vertical   - 0.5);
           final Ellipse2D.Double dot = new Ellipse2D.Double();
           for (long row = firstRow; row <= lastRow; row++) {
               final double y = (row + 0.5) * vertical;
               for (long column = firstColumn; column <= lastColumn; column++) {
                   dot.setFrame((column + 0.5) * horizontal - radius, y - radius, size, size);
                   graphics.fill(dot);
               }
           }
       }
   }

   /**
    * A fill layer made of parallel lines clipped to the shape.
    * The lines are aligned on the origin of the user space, so that the hatches of adjacent shapes join.
    *
    * <ul>
    *   <li>The angle turns clockwise on screen from the horizontal, as stated by the Hatch class table.
    *       Note that the figure of the default hatch shows lines turned counter-clockwise.</li>
    *   <li>Distances smaller than {@value #MINIMUM_DISTANCE} pixel are replaced by that minimum.</li>
    * </ul>
    *
    * @see "OGC 18-067r4 section 13.1"
    */
   public static final class Hatch extends FillLayer {
       /**
        * Minimal distance between two lines, in pixels, for keeping the number of lines reasonable.
        */
       private static final double MINIMUM_DISTANCE = 1;

       /**
        * Color of the lines, opaque.
        */
       private final Color color;

       /**
        * Opacity of the lines, between 0 and 1.
        */
       private final float opacity;

       /**
        * Thickness of the lines, in pixels.
        */
       private final double width;

       /**
        * Orientation of the lines, in radians clockwise on screen from the horizontal.
        */
       private final double angle;

       /**
        * Distance between two consecutive lines, in pixels.
        */
       private final double distance;

       /**
        * Creates a hatch.
        *
        * @param  color     color of the lines.
        * @param  opacity   opacity of the lines, between 0 and 1.
        * @param  width     thickness of the lines, in pixels.
        * @param  angle     orientation of the lines, in radians clockwise on screen from the horizontal.
        * @param  distance  distance between two consecutive lines, in pixels.
        */
       public Hatch(final Color color, final float opacity, final double width, final double angle, final double distance) {
           this.color    = color;
           this.opacity  = opacity;
           this.width    = width;
           this.angle    = angle;
           this.distance = Math.max(distance, MINIMUM_DISTANCE);
       }

       @Override
       public void fill(final Graphics2D graphics, final Shape shape) {
           if (!(width > 0) || !Double.isFinite(angle)) {
               return;
           }
           graphics.clip(shape);
           final Rectangle2D visible = graphics.getClip().getBounds2D();
           final Rectangle2D region = AffineTransform.getRotateInstance(-angle).createTransformedShape(visible).getBounds2D();
           graphics.rotate(angle);
           Drawing.multiplyOpacity(graphics, opacity);
           graphics.setStroke(new BasicStroke((float) width));
           graphics.setPaint(color);
           final double xmin = region.getMinX() - width;
           final double xmax = region.getMaxX() + width;
           final long last = (long) Math.ceil((region.getMaxY() + width) / distance);
           final Line2D.Double line = new Line2D.Double();
           for (long i = (long) Math.floor((region.getMinY() - width) / distance); i <= last; i++) {
               final double y = i * distance;
               line.setLine(xmin, y, xmax, y);
               graphics.draw(line);
           }
       }
   }

    /**
     * A fill layer made of randomly placed dots, clipped to the shape.
     * The user space is divided in square cells of {@value #CELL} pixels, and each cell is a dot
     * with a probability equal to the ratio, so that the dots cover that ratio of the area.
     *
     * <ul>
     *   <li>The standard does not define the size of the dots. They are squares of {@value #CELL} pixels.</li>
     *   <li>The random choice of each cell depends only on the cell position and on the bounds of the shape,
     *       so that painting the same shape again gives the same dots.</li>
     * </ul>
     *
     * @see "OGC 18-067r4 section 13.3"
     */
    public static final class Stipple extends FillLayer {
        /**
         * Size of the cells and of the dots, in pixels.
         */
        private static final int CELL = 2;

        /**
         * Color of the dots, opaque.
         */
        private final Color color;

        /**
         * Opacity of the dots, between 0 and 1.
         */
        private final float opacity;

        /**
         * Ratio of the area covered by dots, between 0 and 1.
         */
        private final double ratio;

        /**
         * Creates a stipple.
         *
         * @param  color    color of the dots.
         * @param  opacity  opacity of the dots, between 0 and 1.
         * @param  ratio    ratio of the area covered by dots, between 0 and 1.
         */
        public Stipple(final Color color, final float opacity, final double ratio) {
            this.color   = color;
            this.opacity = opacity;
            this.ratio   = ratio;
        }

        @Override
        public void fill(final Graphics2D graphics, final Shape shape) {
            if (!(ratio > 0)) {
                return;
            }
            final long seed = seed(shape.getBounds2D());
            graphics.clip(shape);
            final Rectangle visible = graphics.getClipBounds();
            if (visible == null || visible.isEmpty()) {
                return;
            }
            final int firstColumn = Math.floorDiv(visible.x, CELL);
            final int firstRow    = Math.floorDiv(visible.y, CELL);
            final int columns = Math.floorDiv(visible.x + visible.width  - 1, CELL) - firstColumn + 1;
            final int rows    = Math.floorDiv(visible.y + visible.height - 1, CELL) - firstRow    + 1;
            final BufferedImage image = new BufferedImage(columns * CELL, rows * CELL, BufferedImage.TYPE_INT_ARGB);
            final int[] dot = new int[CELL * CELL];
            Arrays.fill(dot, color.getRGB());
            for (int row = 0; row < rows; row++) {
                for (int column = 0; column < columns; column++) {
                    if (isDot(firstColumn + column, firstRow + row, seed)) {
                        image.setRGB(column * CELL, row * CELL, CELL, CELL, dot, 0, CELL);
                    }
                }
            }
            Drawing.multiplyOpacity(graphics, opacity);
            graphics.drawImage(image, firstColumn * CELL, firstRow * CELL, null);
        }

        /**
         * Returns the seed of the random choices for a shape of the given bounds, rounded to pixels.
         */
        private long seed(final Rectangle2D bounds) {
            long seed = Math.round(bounds.getMinX());
            seed = seed * 31 + Math.round(bounds.getMinY());
            seed = seed * 31 + Math.round(bounds.getWidth());
            seed = seed * 31 + Math.round(bounds.getHeight());
            return seed;
        }

        /**
         * Returns whether the given cell is a dot. The choice is a hash of the cell position
         * and of the seed (SplitMix64 finalizer), compared with the ratio.
         */
        private boolean isDot(final long column, final long row, final long seed) {
            long h = seed ^ (column * 0x9E3779B97F4A7C15L) ^ (row * 0xC2B2AE3D27D4EB4FL);
            h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
            h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
            h ^= (h >>> 31);
            return (h >>> 11) * 0x1.0p-53 < ratio;
        }
    }
}
