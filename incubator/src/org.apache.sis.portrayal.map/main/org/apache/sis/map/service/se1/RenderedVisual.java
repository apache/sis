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

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Area;
import java.util.List;


/**
 * A visual element ready to be painted in display space.
 *
 * @author Johann Sorel (Geomatys)
 */
interface RenderedVisual {

    /**
     * Paints this visual with given Graphics2D.
     * The Graphics2D state (composite, paint, stroke, …) is restored after painting.
     * Implementations synchronize on the Graphics2D because presentations may be rendered in parallel.
     *
     * @param g2d not null
     * @return true if something was painted.
     */
    boolean paint(Graphics2D g2d);

    /**
     * Tests intersection of this visual with the searched mask.
     *
     * @param mask not null, in display space.
     * @return true if this visual intersects the mask.
     */
    boolean intersects(Shape mask);

    /**
     * Paints all given visuals in order.
     *
     * @param visuals  the visuals to paint.
     * @param g2d      where to paint.
     */
    static void paint(List<? extends RenderedVisual> visuals, Graphics2D g2d) {
        for (RenderedVisual visual : visuals) {
            visual.paint(g2d);
        }
    }

    /**
     * Returns whether at least one of the given visuals intersects the mask.
     *
     * @param visuals  the visuals to test.
     * @param mask     the mask in display space.
     * @return whether a visual intersects the mask.
     */
    static boolean intersects(List<? extends RenderedVisual> visuals, Shape mask) {
        for (RenderedVisual visual : visuals) {
            if (visual.intersects(mask)) return true;
        }
        return false;
    }

    /**
     * Returns whether the area of the given shape intersects the area of the mask.
     *
     * @param shape  the shape to test.
     * @param mask   the mask in display space.
     * @return whether the two areas intersect.
     */
    static boolean intersects(Shape shape, Shape mask) {
        if (!shape.getBounds2D().intersects(mask.getBounds2D())) {
            return false;
        }
        final Area area = new Area(mask);
        area.intersect(new Area(shape));
        return !area.isEmpty();
    }
}
