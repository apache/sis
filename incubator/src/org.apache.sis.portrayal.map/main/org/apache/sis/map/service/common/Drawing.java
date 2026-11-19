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

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Shape;


/**
 * Something to draw on the scene, in display coordinates.
 * Drawings are computed once, then painted or tested for intersection with an area such as a mouse selection.
 *
 * <ul>
 *   <li>The opacity of a drawing multiplies the alpha of the {@link AlphaComposite} of the graphics,
 *       so that a drawing painted inside a translucent drawing is translucent too.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 */
public interface Drawing {
    /**
     * Draws on the given graphics, which is in display coordinates.
     * The state of the graphics (transform, composite, paint, stroke, clip) is restored after painting.
     *
     * @param  graphics  where to draw.
     */
    void paint(Graphics2D graphics);

    /**
     * Returns whether this drawing intersects the given area, in display coordinates.
     *
     * @param  mask  the area to test.
     * @return whether a painted part of this drawing is inside the area.
     */
    boolean intersects(Shape mask);

    /**
     * Multiplies the alpha of the composite of the given graphics by the given opacity.
     * If the composite is not an {@link AlphaComposite}, it is replaced by a source-over composite of the given opacity.
     *
     * @param  graphics  the graphics whose composite to change.
     * @param  opacity   the opacity between 0 and 1. Values of 1 or more leave the composite unchanged.
     */
    static void multiplyOpacity(final Graphics2D graphics, final float opacity) {
        if (opacity < 1) {
            final float alpha = Math.max(0, opacity);
            if (graphics.getComposite() instanceof AlphaComposite current) {
                graphics.setComposite(current.derive(current.getAlpha() * alpha));
            } else {
                graphics.setComposite(AlphaComposite.SrcOver.derive(alpha));
            }
        }
    }
}
