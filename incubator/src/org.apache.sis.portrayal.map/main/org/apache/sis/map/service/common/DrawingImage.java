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

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * The same image drawn at one or more places, each one with its own transform.
 * This is used for coverages drawn at a fixed location of the display,
 * and for symbols repeated at points or along lines.
 *
 * <ul>
 *   <li>The hit tests consider only the pixels which are not fully transparent, and nothing if the opacity is zero.</li>
 *   <li>Painting is synchronized on the graphics, because presentations may be rendered in parallel.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class DrawingImage implements Drawing {
    /**
     * The image to draw, with non-premultiplied alpha.
     */
    private final BufferedImage image;

    /**
     * Opacity multiplying the alpha of the image, between 0 and 1.
     */
    private final float opacity;

    /**
     * Transforms from image coordinates to display coordinates, one per drawn image.
     */
    private final List<AffineTransform> placements;

    /**
     * Creates a drawing of the given image, without placement.
     *
     * @param  image    the image to draw, with non-premultiplied alpha.
     * @param  opacity  opacity multiplying the alpha of the image, between 0 and 1.
     */
    public DrawingImage(final BufferedImage image, final float opacity) {
        this.image   = Objects.requireNonNull(image, "image");
        this.opacity = opacity;
        placements   = new ArrayList<>();
    }

    /**
     * Creates a drawing of the given image at a fixed location of the display, one pixel per display cell.
     *
     * @param  image  the image to draw, with non-premultiplied alpha.
     * @param  x      display coordinate of the left border of the image.
     * @param  y      display coordinate of the upper border of the image.
     */
    public DrawingImage(final BufferedImage image, final int x, final int y) {
        this(image, 1);
        add(AffineTransform.getTranslateInstance(x, y));
    }

    /**
     * Adds a place where to draw the image.
     *
     * @param  placement  transform from image coordinates to display coordinates.
     * @return this drawing.
     */
    public DrawingImage add(final AffineTransform placement) {
        placements.add(new AffineTransform(placement));
        return this;
    }

    /**
     * Returns whether this drawing has no placement.
     *
     * @return whether there is nothing to draw.
     */
    public boolean isEmpty() {
        return placements.isEmpty();
    }

    /**
     * Draws the image at each place.
     *
     * @param  graphics  where to draw.
     */
    @Override
    public void paint(final Graphics2D graphics) {
        if (placements.isEmpty()) {
            return;
        }
        synchronized (graphics) {
            final Graphics2D g = (Graphics2D) graphics.create();
            try {
                Drawing.multiplyOpacity(g, opacity);
                for (final AffineTransform placement : placements) {
                    g.drawImage(image, placement, null);
                }
            } finally {
                g.dispose();
            }
        }
    }

    /**
     * Returns whether a pixel which is not fully transparent intersects the given area.
     *
     * @param  mask  the area to test, in display coordinates.
     * @return whether a visible pixel is inside the area.
     */
    @Override
    public boolean intersects(final Shape mask) {
        if (!(opacity > 0)) {
            return false;
        }
        for (final AffineTransform placement : placements) {
            if (intersects(placement, mask)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether a pixel of the image drawn with the given placement intersects the given area.
     * Each pixel is approximated by the bounds of its transformed square.
     */
    private boolean intersects(final AffineTransform placement, final Shape mask) {
        final Rectangle bounds = new Rectangle(image.getWidth(), image.getHeight());
        final Rectangle2D maskBounds = mask.getBounds2D();
        if (!placement.createTransformedShape(bounds).intersects(maskBounds)) {
            return false;
        }
        final Rectangle area;
        try {
            area = placement.createInverse().createTransformedShape(maskBounds).getBounds().intersection(bounds);
        } catch (NoninvertibleTransformException e) {
            return false;
        }
        final Rectangle pixel = new Rectangle(1, 1);
        for (int py = area.y; py < area.y + area.height; py++) {
            for (int px = area.x; px < area.x + area.width; px++) {
                if ((image.getRGB(px, py) >>> 24) != 0) {
                    pixel.setLocation(px, py);
                    if (mask.intersects(placement.createTransformedShape(pixel).getBounds2D())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
