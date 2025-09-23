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
package org.apache.sis.geometry.wrapper.j2d;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.PathIterator;
import java.awt.geom.AffineTransform;
import org.apache.sis.geometry.wrapper.AbstractGeometry;
import org.apache.sis.referencing.internal.shared.IntervalRectangle;


/**
 * A shape made of straight lines. This shape does not contain any Bézier curve.
 * Consequently, the flatness factor of path iterator can be ignored.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class FlatShape extends AbstractGeometry implements Shape {
    /**
     * Cached values of shape bounds.
     *
     * @see #getBounds2D()
     */
    final IntervalRectangle bounds;

    /**
     * Creates a shape with the given bounds.
     * The given argument is stored by reference; it is not cloned.
     *
     * @param  bounds  the shape bounds (not cloned).
     */
    FlatShape(final IntervalRectangle bounds) {
        this.bounds = bounds;
    }

    /**
     * Creates a shape with bounds initialized to minimum and maximum coordinates of given array.
     * The {@code coordinates} array shall not be empty.
     *
     * @param  coordinates  the coordinate values as (x,y) tuples.
     * @param  size         number of valid value in {@code coordinates} array.
     */
    FlatShape(final double[] coordinates, final int size) {
        double xmax, ymax;
        double xmin = xmax = coordinates[0];
        double ymin = ymax = coordinates[1];
        for (int i=2; i<size;) {
            final double x = coordinates[i++];
            final double y = coordinates[i++];
            if (x < xmin) xmin = x;
            if (x > xmax) xmax = x;
            if (y < ymin) ymin = y;
            if (y > ymax) ymax = y;
        }
        bounds = new IntervalRectangle(xmin, ymin, xmax, ymax);
    }

    /**
     * Returns an integer rectangle that completely encloses the shape.
     * There is no guarantee that the rectangle is the smallest bounding box that encloses the shape.
     */
    @Override
    public final Rectangle getBounds() {
        return bounds.getBounds();
    }

    /**
     * Returns a rectangle that completely encloses the shape.
     * There is no guarantee that the rectangle is the smallest bounding box that encloses the shape.
     */
    @Override
    public final Rectangle2D getBounds2D() {
        return bounds.getBounds2D();
    }

    /**
     * Tests if the specified point is inside the boundary of the shape.
     * This method delegates to {@link #contains(double, double)}.
     */
    @Override
    public final boolean contains(final Point2D p) {
        return contains(p.getX(), p.getY());
    }

    /**
     * Returns an iterator for the shape outline geometry. The flatness factor is ignored on the assumption
     * that this shape does not contain any Bézier curve, as stipulated in {@code FlatShape} class contract.
     *
     * @param  at        an optional transform to apply on coordinate values.
     * @param  flatness  ignored.
     * @return an iterator for the shape outline geometry.
     */
    @Override
    public final PathIterator getPathIterator(final AffineTransform at, final double flatness) {
        return getPathIterator(at);
    }

    /**
     * Returns a potentially smaller shape containing all polylines that intersect the given area of interest.
     * This method performs only a quick check based on bounds intersections. It does not test individual points.
     * The returned shape may still have many points outside the given bounds.
     *
     * @param  areaOfInterest  the area of interest. Edges are considered exclusive.
     * @return a potentially smaller shape, or {@code null} if this shape is fully outside the AOI.
     */
    public FlatShape fastClip(final Rectangle2D areaOfInterest) {
        return bounds.intersects(areaOfInterest) ? this : null;
    }
}
