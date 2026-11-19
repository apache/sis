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

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;


/**
 * The rectangle occupied by a label on the display, which may be rotated.
 * Two boxes intersect if they overlap or touch.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class LabelBox {
    /**
     * Number of corners of a box.
     */
    private static final int CORNERS = 4;

    /**
     * The bounds of the label relative to its anchor, with <var>y</var> down.
     */
    private final Rectangle2D bounds;

    /**
     * Display coordinates of the anchor.
     */
    private final double x, y;

    /**
     * Rotation around the anchor in radians, clockwise on the screen.
     */
    private final double rotation;

    /**
     * Display coordinates of the corners, in order around the box.
     */
    private final double[] xs, ys;

    /**
     * The bounds of the corners in display coordinates.
     */
    private final Rectangle2D envelope;

    /**
     * Creates the box of a label.
     *
     * @param  bounds    the bounds of the label relative to its anchor, with <var>y</var> down.
     * @param  x         horizontal display coordinate of the anchor.
     * @param  y         vertical display coordinate of the anchor.
     * @param  rotation  rotation around the anchor in radians, clockwise on the screen.
     */
    public LabelBox(final Rectangle2D bounds, final double x, final double y, final double rotation) {
        this.bounds   = (Rectangle2D) bounds.clone();
        this.x        = x;
        this.y        = y;
        this.rotation = rotation;
        final double[] corners = {
            bounds.getMinX(), bounds.getMinY(),
            bounds.getMaxX(), bounds.getMinY(),
            bounds.getMaxX(), bounds.getMaxY(),
            bounds.getMinX(), bounds.getMaxY()
        };
        final AffineTransform transform = AffineTransform.getTranslateInstance(x, y);
        transform.rotate(rotation);
        transform.transform(corners, 0, corners, 0, CORNERS);
        xs = new double[CORNERS];
        ys = new double[CORNERS];
        envelope = new Rectangle2D.Double(corners[0], corners[1], 0, 0);
        for (int i = 0; i < CORNERS; i++) {
            xs[i] = corners[2 * i];
            ys[i] = corners[2 * i + 1];
            envelope.add(xs[i], ys[i]);
        }
    }

    /**
     * Returns this box with a margin added on each side.
     *
     * @param  padding  the margin in pixels.
     * @return the enlarged box, or this box if the padding is not positive.
     */
    public LabelBox expand(final double padding) {
        if (!(padding > 0)) {
            return this;
        }
        final Rectangle2D enlarged = new Rectangle2D.Double(
                bounds.getMinX() - padding, bounds.getMinY() - padding,
                bounds.getWidth() + 2 * padding, bounds.getHeight() + 2 * padding);
        return new LabelBox(enlarged, x, y, rotation);
    }

    /**
     * Returns whether this box overlaps or touches the given box.
     *
     * @param  other  the other box.
     * @return whether the two boxes intersect.
     */
    public boolean intersects(final LabelBox other) {
        return envelope.getMinX() <= other.envelope.getMaxX() && other.envelope.getMinX() <= envelope.getMaxX()
            && envelope.getMinY() <= other.envelope.getMaxY() && other.envelope.getMinY() <= envelope.getMaxY()
            && !separatedBy(other) && !other.separatedBy(this);
    }

    /**
     * Returns whether an axis perpendicular to an edge of this box separates the two boxes.
     * This is the separating axis test for convex polygons.
     */
    private boolean separatedBy(final LabelBox other) {
        for (int edge = 0; edge < 2; edge++) {
            final double nx = ys[edge + 1] - ys[edge];
            final double ny = xs[edge] - xs[edge + 1];
            double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
            double otherMin = Double.POSITIVE_INFINITY, otherMax = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < CORNERS; i++) {
                final double p = xs[i] * nx + ys[i] * ny;
                final double q = other.xs[i] * nx + other.ys[i] * ny;
                min = Math.min(min, p);
                max = Math.max(max, p);
                otherMin = Math.min(otherMin, q);
                otherMax = Math.max(otherMax, q);
            }
            if (max < otherMin || otherMax < min) {
                return true;
            }
        }
        return false;
    }
}
