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

import java.util.Arrays;
import java.util.Iterator;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.geom.PathIterator;
import java.awt.geom.AffineTransform;
import org.apache.sis.referencing.internal.shared.IntervalRectangle;
import org.apache.sis.util.Classes;


/**
 * Collection of polylines or polygons as a Java2D {@link Shape}.
 * This class has some similarities with {@link java.awt.geom.Path2D}
 * with the following differences:
 *
 * <ul>
 *   <li>No synchronization.</li>
 *   <li>Line segments only (no Bézier curves).</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class MultiPolylines extends FlatShape {
    /**
     * The polylines or polygons in this collection.
     *
     * @todo Store in a RTree or QuadTree with {@link Polyline#bounds} as keys.
     *       Replace loops in {@code contains(…)} and {@code intersect(…)} methods by use of this tree.
     */
    private final Polyline[] polylines;

    /**
     * Creates a collection of polylines.
     * The given argument is stored by reference; it is not cloned.
     *
     * @param  polylines  the polylines. This array is not cloned.
     */
    public MultiPolylines(final Polyline[] polylines) {
        super(new IntervalRectangle());
        this.polylines = polylines;
        bounds.setRect(polylines[0].bounds);
        for (int i=1; i<polylines.length; i++) {
            bounds.add(polylines[i].bounds);
        }
    }

    /**
     * Tests if the given coordinates are inside the boundary of this shape.
     */
    @Override
    public boolean contains(final double x, final double y) {
        if (bounds.contains(x, y)) {
            for (final Polyline p : polylines) {
                if (p.contains(x, y)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Tests if the interior of this shape intersects the interior of the given rectangle.
     * May conservatively return {@code true} if an intersection is probable but accurate
     * answer would be too costly to compute.
     */
    @Override
    public boolean intersects(final double x, final double y, final double w, final double h) {
        if (bounds.intersects(x, y, w, h)) {
            for (final Polyline p : polylines) {
                if (p.intersects(x, y, w, h)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Tests if the interior of this shape intersects the interior of the given rectangle.
     * May conservatively return {@code true} if an intersection is probable but accurate
     * answer would be too costly to compute.
     */
    @Override
    public boolean intersects(final Rectangle2D r) {
        if (bounds.intersects(r)) {
            for (final Polyline p : polylines) {
                if (p.intersects(r)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Tests if the interior of this shape entirely contains the interior of the given rectangle.
     * May conservatively return {@code false} if an accurate answer would be too costly to compute.
     */
    @Override
    public boolean contains(final double x, final double y, final double w, final double h) {
        if (bounds.contains(x, y, w, h)) {
            for (final Polyline p : polylines) {
                if (p.contains(x, y, w, h)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Tests if the interior of this shape entirely contains the interior of the given rectangle.
     * May conservatively return {@code false} if an accurate answer would be too costly to compute.
     */
    @Override
    public boolean contains(final Rectangle2D r) {
        if (bounds.contains(r)) {
            for (final Polyline p : polylines) {
                if (p.contains(r)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns an iterator over coordinates in this multi-polylines.
     */
    @Override
    public PathIterator getPathIterator(final AffineTransform at) {
        final Iterator<Polyline> it = Arrays.asList(polylines).iterator();
        return it.hasNext() ? new Polyline.Iter(at, it.next(), it) : new Polyline.Iter();
    }

    /**
     * Returns a potentially smaller shape containing all polylines that intersect the given area of interest.
     * This method performs only a quick check based on bounds intersections.
     * The returned shape may still have many points outside the given bounds.
     */
    @Override
    public FlatShape fastClip(final Rectangle2D areaOfInterest) {
        if (bounds.intersects(areaOfInterest)) {
            final Polyline[] clipped = new Polyline[polylines.length];
            int count = 0;
            for (final Polyline p : polylines) {
                if (p.bounds.intersects(areaOfInterest)) {
                    clipped[count++] = p;
                }
            }
            if (count != 0) {
                if (count == polylines.length) {
                    return this;
                }
                return new MultiPolylines(Arrays.copyOf(clipped, count));
            }
        }
        return null;
    }

    /**
     * Returns a string representation for debugging purposes.
     *
     * @return a debug string representation.
     */
    @Override
    public String toString() {
        return Classes.getShortClassName(this) + '[' + (polylines.length / 2) + " polylines]";
    }
}
