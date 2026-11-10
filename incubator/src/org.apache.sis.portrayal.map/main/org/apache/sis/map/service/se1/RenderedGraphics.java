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

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;


/**
 * The same image painted at multiple places, each one with its own transform.
 * This is used for graphics repeated along a line.
 *
 * @author Johann Sorel (Geomatys)
 */
final class RenderedGraphics implements RenderedVisual {

    /**
     * Image to paint, not null.
     */
    private final BufferedImage image;

    /**
     * Composite used when painting the image.
     */
    private final AlphaComposite composite;

    /**
     * Transforms from image space to display space, one per painted image.
     */
    final List<AffineTransform> placements = new ArrayList<>();

    /**
     * @param image image to paint, not null
     * @param composite composite used when painting the image, not null
     */
    RenderedGraphics(BufferedImage image, AlphaComposite composite) {
        this.image = image;
        this.composite = composite;
    }

    @Override
    public boolean paint(Graphics2D g2d) {
        if (placements.isEmpty()) return false;
        synchronized (g2d) {
            final Composite oldComposite = g2d.getComposite();
            try {
                g2d.setComposite(composite);
                for (AffineTransform placement : placements) {
                    g2d.drawImage(image, placement, null);
                }
            } finally {
                g2d.setComposite(oldComposite);
            }
        }
        return true;
    }

    /**
     * Tests intersection of the mask with the bounds of each placed image.
     */
    @Override
    public boolean intersects(Shape mask) {
        final Rectangle2D imageBounds = new Rectangle2D.Double(0, 0, image.getWidth(), image.getHeight());
        for (AffineTransform placement : placements) {
            if (RenderedVisual.intersects(placement.createTransformedShape(imageBounds), mask)) {
                return true;
            }
        }
        return false;
    }
}
