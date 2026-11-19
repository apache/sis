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
import java.awt.Shape;
import java.awt.geom.Area;


/**
 * The halo of a shape: the shape extended outward by a size, with rounded corners.
 * The halo is a single area, so that a translucent halo has a uniform opacity.
 * It is drawn beneath the shape, for example around the glyphs of a text.
 *
 * @param size  how far the halo extends outward from the shape contour, in pixels.
 *
 * @author Johann Sorel (Geomatys)
 */
public record ShapeHalo(double size) {
    /**
     * Returns whether this halo is invisible because its size is not positive or not finite.
     *
     * @return whether there is nothing to draw.
     */
    public boolean isEmpty() {
        return !(size > 0) || Double.isInfinite(size);
    }

    /**
     * Returns the halo of the given shape.
     *
     * @param  base  the shape around which to draw the halo.
     * @return the shape extended outward by the size of this halo.
     */
    public Shape apply(final Shape base) {
        final Area area = new Area(base);
        area.add(new Area(new BasicStroke((float) (2 * size), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                .createStrokedShape(base)));
        return area;
    }
}
