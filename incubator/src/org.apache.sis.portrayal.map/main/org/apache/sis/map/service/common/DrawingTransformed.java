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
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.util.Objects;


/**
 * A drawing painted with a transform and an opacity, such as a graphic placed at an anchor.
 * The opacity multiplies the opacity of each part of the drawing,
 * so overlapping parts of a translucent drawing are seen through each other.
 *
 * <ul>
 *   <li>A drawing with an opacity of zero, or with a non-invertible transform, is not part of the hit tests.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class DrawingTransformed implements Drawing {
    /**
     * The drawing to transform.
     */
    private final Drawing content;

    /**
     * Transform from the coordinates of the content to the coordinates of the graphics.
     */
    private final AffineTransform transform;

    /**
     * Opacity multiplying the opacity of the content, between 0 and 1.
     */
    private final float opacity;

    /**
     * Creates a transformed drawing.
     *
     * @param  content    the drawing to transform.
     * @param  transform  transform from the coordinates of the content to the coordinates of the graphics.
     * @param  opacity    opacity multiplying the opacity of the content, between 0 and 1.
     */
    public DrawingTransformed(final Drawing content, final AffineTransform transform, final float opacity) {
        this.content   = Objects.requireNonNull(content, "content");
        this.transform = new AffineTransform(transform);
        this.opacity   = opacity;
    }

    @Override
    public void paint(final Graphics2D graphics) {
        if (opacity > 0) {
            final Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.transform(transform);
                Drawing.multiplyOpacity(g, opacity);
                content.paint(g);
            } finally {
                g.dispose();
            }
        }
    }

    @Override
    public boolean intersects(final Shape mask) {
        if (opacity > 0) try {
            return content.intersects(transform.createInverse().createTransformedShape(mask));
        } catch (NoninvertibleTransformException e) {
            // A drawing scaled to nothing has no painted part.
        }
        return false;
    }
}
