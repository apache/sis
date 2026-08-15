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
package org.apache.sis.gui.coverage;

import java.util.function.Predicate;
import javafx.animation.FadeTransition;
import javafx.scene.shape.Rectangle;
import org.apache.sis.util.internal.shared.Numerics;


/**
 * Merges adjacent rectangles (only for rectangles with no stroke).
 * This is used for simplifying small tiles into a single larger tile.
 * The goal is to avoid to put too much pressure on JavaFX when there
 * is a lot of small tiles.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class RectangleMerger implements Predicate<FadeTransition> {
    /**
     * The user data that rectangles must have for being merged.
     * This is an identification of the list of children where the rectangles belong.
     */
    private final CoverageCanvas.StaticGraphics filter;

    /**
     * Bounds of the rectangle.
     */
    private double xmin, xmax, ymin, ymax;

    /**
     * Tolerance factor on each axis.
     */
    private double tolX, tolY;

    /**
     * Whether a coordinate has been modified.
     */
    private boolean modified;

    /**
     * Creates a new merger starting with the given rectangle.
     *
     * @param  r  the initial rectangle.
     */
    RectangleMerger(final CoverageCanvas.StaticGraphics filter, final Rectangle r) {
        this.filter = filter;
        xmin = r.getX();
        ymin = r.getY();
        xmax = xmin + (tolX = r.getWidth());
        ymax = ymin + (tolY = r.getHeight());
        tolX *= Numerics.COMPARISON_THRESHOLD;
        tolY *= Numerics.COMPARISON_THRESHOLD;
    }

    /**
     * Tries to merge the node of the given transition with the rectangle if possible.
     * If the two rectangle intersect, then merges if either the left and right bounds are approximately equal
     * (merge vertically), or if the top and bottom bounds are approximately equal (merge horizontally).
     *
     * @param  t  the transition to try to merge.
     * @return whether the rectangles have been merged.
     */
    @Override
    public boolean test(final FadeTransition t) {
        if (t.getNode() instanceof Rectangle r && r.getUserData() == filter) {
            final double x0 = r.getX();
            final double y0 = r.getY();
            final double x1 = x0 + r.getWidth();
            final double y1 = y0 + r.getHeight();
            if ((Math.abs(x0 - xmin) <= tolX && Math.abs(x1 - xmax) <= tolX && y0 - tolY <= ymax && y1 + tolY >= ymin) ||
                (Math.abs(y0 - ymin) <= tolY && Math.abs(y1 - ymax) <= tolY && x0 - tolX <= xmax && x1 + tolX >= xmin))
            {
                if (x0 < xmin) {xmin = x0; modified = true;}
                if (y0 < ymin) {ymin = y0; modified = true;}
                if (x1 > xmax) {xmax = x1; modified = true;}
                if (y1 > ymax) {ymax = y1; modified = true;}
                return true;
            }
        }
        return false;
    }

    /**
     * Stores the coordinates in the given rectangle.
     *
     * @param  r  the rectangle to modify.
     */
    void copyTo(final Rectangle r) {
        if (modified) {
            r.setX(xmin);
            r.setY(ymin);
            r.setWidth (xmax - xmin);
            r.setHeight(ymax - ymin);
        }
    }
}
