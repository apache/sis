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
package org.apache.sis.internal.feature.j2d;

import java.util.Iterator;
import java.util.Collections;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.geom.PathIterator;
import java.awt.geom.AffineTransform;
import org.apache.sis.internal.referencing.j2d.IntervalRectangle;


/**
 * A polylines or polygons as a Java2D {@link Shape}. This class has some similarities
 * with {@link java.awt.geom.Path2D} with the following differences:
 *
 * <ul>
 *   <li>No synchronization.</li>
 *   <li>Line segments only (no Bézier curves).</li>
 *   <li>No multi-polylines (e.g. no "move to" operation in the middle).</li>
 *   <li>Naive {@code intersect(…)} and {@code contains(…)} methods.</li>
 * </ul>
 *
 * The {@code intersect(…)} and {@code contains(…)} methods may be improved in a future version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
class Polyline extends FlatShape {
    /**
     * The coordinate values as (x,y) tuples.
     */
    private final double[] coordinates;

    /**
     * Creates a new polylines with the given coordinates.
     * The given arguments are stored by reference; they are not cloned.
     * The array shall not be empty.
     *
     * @param  bounds       the polyline bounds (not cloned).
     * @param  coordinates  the coordinate values as (x,y) tuples (not cloned).
     */
    Polyline(final IntervalRectangle bounds, final double[] coordinates) {
        super(bounds);
        assert coordinates.length != 0;         // Required by our PathIterator.
        this.coordinates = coordinates;
    }

    /**
     * Delegates operations to {@link Rectangle2D} bounds. This naive implementation
     * is not compliant with Java2D contract. We may need to revisit in a future version.
     */
    @Override public final boolean contains  (Rectangle2D r)                          {return bounds.contains  (r);}
    @Override public final boolean intersects(Rectangle2D r)                          {return bounds.intersects(r);}
    @Override public final boolean contains  (double x, double y)                     {return bounds.contains  (x,y);}
    @Override public final boolean contains  (double x, double y, double w, double h) {return bounds.contains  (x,y,w,h);}
    @Override public final boolean intersects(double x, double y, double w, double h) {return bounds.intersects(x,y,w,h);}

    /**
     * Returns an iterator over coordinates in this polyline.
     */
    @Override
    public final PathIterator getPathIterator(final AffineTransform at) {
        return new Iter(at, this, Collections.emptyIterator());
    }

    /**
     * Iterator over polyline(s) or polygon(s) coordinates. This implementation requires that all {@link Polyline}
     * instances have non-empty coordinates array, otherwise {@link ArrayIndexOutOfBoundsException} will occur.
     */
    static final class Iter implements PathIterator {
        /**
         * The transform to apply on each coordinate tuple.
         */
        private final AffineTransform at;

        /**
         * Next polylines on which to iterate, or an empty iterator if none.
         */
        private final Iterator<Polyline> polylines;

        /**
         * Coordinates to return in calls to {@link #currentSegment(double[])}.
         */
        private double[] coordinates;

        /**
         * Current position in {@link #coordinates} array.
         */
        private int position;

        /**
         * {@code true} if {@link #currentSegment(double[])} shall return {@link #SEG_CLOSE}.
         */
        private boolean closing;

        /**
         * Whether current coordinates make a polygon. If {@code true}, then iteration shall
         * emit a closing {@link #SEG_CLOSE} type before to move to next polyline or polygon.
         */
        private boolean isPolygon;

        /**
         * Whether iteration is finished.
         */
        private boolean isDone;

        /**
         * Creates an empty iterator.
         */
        Iter() {
            at        = null;
            polylines = null;
            isDone    = true;
        }

        /**
         * Creates a new iterator.
         *
         * @param  at     the transform to apply on each coordinate tuple.
         * @param  first  the first polyline or polygon.
         * @param  next   all other polylines or polygons.
         */
        Iter(final AffineTransform at, final Polyline first, final Iterator<Polyline> next) {
            this.at     = (at != null) ? at : new AffineTransform();
            polylines   = next;
            coordinates = first.coordinates;
            isPolygon   = (first instanceof Polygon);
        }

        /**
         * Arbitrary winding rule, since enclosing class do not yet compute shape interior.
         */
        @Override
        public int getWindingRule() {
            return WIND_NON_ZERO;
        }

        /**
         * Returns {@code true} if there is no more points to iterate.
         */
        @Override
        public boolean isDone() {
            return isDone;
        }

        /**
         * Moves to the next point.
         */
        @Override
        public void next() {
            if ((position += 2) >= coordinates.length) {
                if (isPolygon) {
                    closing = !closing;
                    if (closing) return;
                }
                if (polylines.hasNext()) {
                    final Polyline next = polylines.next();
                    isPolygon   = (next instanceof Polygon);
                    coordinates = next.coordinates;
                    position    = 0;
                } else {
                    isDone = true;
                }
            }
        }

        /**
         * Returns coordinates of current line segment.
         */
        @Override
        public int currentSegment(final float[] coords) {
            if (closing) return SEG_CLOSE;
            at.transform(coordinates, position, coords, 0, 1);
            return (position == 0) ? SEG_MOVETO : SEG_LINETO;
        }

        /**
         * Returns coordinates of current line segment.
         */
        @Override
        public int currentSegment(final double[] coords) {
            if (closing) return SEG_CLOSE;
            at.transform(coordinates, position, coords, 0, 1);
            return (position == 0) ? SEG_MOVETO : SEG_LINETO;
        }
    }
}
