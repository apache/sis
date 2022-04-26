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
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.PathIterator;
import java.awt.geom.AffineTransform;
import org.apache.sis.internal.util.Numerics;


/**
 * A polylines or polygons as a Java2D {@link Shape}. This class has some similarities
 * with {@link java.awt.geom.Path2D} with the following differences:
 *
 * <ul>
 *   <li>No synchronization.</li>
 *   <li>Line segments only (no Bézier curves).</li>
 *   <li>No multi-polylines (e.g. no "move to" operation in the middle).</li>
 *   <li>Coordinates "compressed" (with a simple translation) as {@code float}.</li>
 * </ul>
 *
 * <h2>Precision and pseudo-compression</h2>
 * Coordinates are stored with {@code float} precision for reducing memory usage with large polylines.
 * This is okay if coordinates are approximate anyway, for example if they are values interpolated by
 * the {@code Isolines} class. For attenuating the precision lost, coordinate values are converted by
 * applications of the two following steps:
 *
 * <ol class="verbose">
 *   <li>First, translate coordinates toward zero. For example latitude or longitude values in the
 *       [50 … 60]° range have a precision of about 4E-6° (about 0.4 meter). But translating those
 *       coordinates to the [-5 … 5]° range increases their precision to 0.05 meter. The precision
 *       gain is more important when the original coordinates are projected coordinates with high
 *       "false easting" / "false northing" parameters.</li>
 *   <li>Next, if minimum or maximum coordinate values are outside the range allowed by {@code float}
 *       exponent values, multiply coordinates by a power of 2 (<strong>Not yet implemented)</strong>.
 *       Note that precision gain happens only when values are made closer to zero by a translation.
 *       Making coordinates closer to zero by a multiplication has no effect on the precision.
 *       This step is required only for avoiding overflow or underflow.</li>
 * </ol>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
class Polyline extends FlatShape {
    /**
     * The "compressed" coordinate values as (x,y) tuples. To get the desired coordinates,
     * those values must be converted by the {@link #inflate} transform.
     */
    private final float[] coordinates;

    /**
     * The transform from {@link #coordinates} values to the values given by {@link Iter}.
     * This transform is usually only a translation.
     */
    private final AffineTransform inflate;

    /**
     * Creates a new polylines with the given coordinates.
     * The {@code coordinates} array shall not be empty.
     *
     * @param  coordinates  the coordinate values as (x,y) tuples.
     * @param  size         number of valid value in {@code coordinates} array.
     */
    Polyline(final double[] coordinates, final int size) {
        super(coordinates, size);
        this.coordinates = new float[size];
        final double tx = round(bounds.getCenterX(), bounds.xmin, bounds.xmax);
        final double ty = round(bounds.getCenterY(), bounds.ymin, bounds.ymax);
        inflate = AffineTransform.getTranslateInstance(tx, ty);
        AffineTransform.getTranslateInstance(-tx, -ty).transform(coordinates, 0, this.coordinates, 0, size / 2);
    }

    /**
     * Rounds the translation to an arbitrary number of bits (currently 20).
     * The intent is to avoid that zero values become something like 1E-9.
     * The number of bits that we kept should be less that the number of bits
     * in the significand (mantissa) of {@code float} type.
     */
    private static double round(final double center, final double min, final double max) {
        final int e = Math.getExponent(Math.max(Math.abs(min), Math.abs(max))) - (Numerics.SIGNIFICAND_SIZE_OF_FLOAT - 3);
        return Math.scalb(Math.round(Math.scalb(center, -e)), e);
    }

    /**
     * Tests if the given coordinates are inside the boundary of this shape.
     */
    @Override
    public boolean contains(final double x, final double y) {
        return bounds.contains(x, y) && Path2D.contains(iterator(), x, y);
    }

    /**
     * Tests if the interior of this shape intersects the interior of the given rectangle.
     * May conservatively return {@code true} if an intersection is probable but accurate
     * answer would be too costly to compute.
     */
    @Override
    public boolean intersects(final double x, final double y, final double w, final double h) {
        return bounds.intersects(x, y, w, h) && Path2D.intersects(iterator(), x, y, w, h);
    }

    /**
     * Tests if the interior of this shape intersects the interior of the given rectangle.
     * May conservatively return {@code true} if an intersection is probable but accurate
     * answer would be too costly to compute.
     */
    @Override
    public boolean intersects(final Rectangle2D r) {
        return bounds.intersects(r) && Path2D.intersects(iterator(), r);
    }

    /**
     * Tests if the interior of this shape entirely contains the interior of the given rectangle.
     * May conservatively return {@code false} if an accurate answer would be too costly to compute.
     */
    @Override
    public boolean contains(final double x, final double y, final double w, final double h) {
        return bounds.contains(x, y, w, h) && Path2D.contains(iterator(), x, y, w, h);
    }

    /**
     * Tests if the interior of this shape entirely contains the interior of the given rectangle.
     * May conservatively return {@code false} if an accurate answer would be too costly to compute.
     */
    @Override
    public boolean contains(final Rectangle2D r) {
        return bounds.contains(r) && Path2D.contains(iterator(), r);
    }

    /**
     * Returns an iterator over coordinates without user transform.
     */
    private PathIterator iterator() {
        return getPathIterator(null);
    }

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
         * The user-specified transform, or {@code null} if none.
         */
        private final AffineTransform toUserSpace;

        /**
         * The transform to apply on each coordinate tuple. This is the concatenation of user-specified
         * transform with {@link Polyline#inflate}. Shall not be null, unless the iterator is empty.
         */
        private AffineTransform inflate;

        /**
         * Next polylines on which to iterate, or an empty iterator if none.
         */
        private final Iterator<Polyline> polylines;

        /**
         * Coordinates to return (after conversion by {@link #inflate}) in calls to {@link #currentSegment(double[])}.
         */
        private float[] coordinates;

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
            toUserSpace = null;
            polylines   = null;
            isDone      = true;
        }

        /**
         * Creates a new iterator.
         *
         * @param  at     the transform to apply on each coordinate tuple.
         * @param  first  the first polyline or polygon.
         * @param  next   all other polylines or polygons.
         */
        Iter(final AffineTransform at, final Polyline first, final Iterator<Polyline> next) {
            if (at != null) {
                inflate = new AffineTransform();
            }
            toUserSpace = at;
            polylines = next;
            setSource(first);
        }

        /**
         * Initializes the {@link #coordinates}, {@link #isPolygon} and {@link #inflate} fields
         * for iteration over coordinate values given by the specified polyline.
         */
        private void setSource(final Polyline polyline) {
            isPolygon = (polyline instanceof Polygon);
            coordinates = polyline.coordinates;
            if (toUserSpace != null) {
                inflate.setTransform(toUserSpace);
                inflate.concatenate(polyline.inflate);
            } else {
                inflate = polyline.inflate;
            }
        }

        /**
         * Arbitrary winding rule, since enclosing class do not yet compute shape interior.
         */
        @Override
        public int getWindingRule() {
            return WIND_NON_ZERO;
        }

        /**
         * Returns {@code true} if there are no more points to iterate.
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
                    setSource(polylines.next());
                    position = 0;
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
            inflate.transform(coordinates, position, coords, 0, 1);
            return (position == 0) ? SEG_MOVETO : SEG_LINETO;
        }

        /**
         * Returns coordinates of current line segment.
         */
        @Override
        public int currentSegment(final double[] coords) {
            if (closing) return SEG_CLOSE;
            inflate.transform(coordinates, position, coords, 0, 1);
            return (position == 0) ? SEG_MOVETO : SEG_LINETO;
        }
    }
}
