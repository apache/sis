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
package org.apache.sis.internal.processing.isoline;

import java.util.Arrays;
import java.awt.geom.Path2D;
import org.apache.sis.internal.feature.j2d.PathBuilder;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Debug;


/**
 * Coordinates of a polyline under construction. Coordinates can be appended in only one direction.
 * If the polyline may growth on both directions (which happens if the polyline crosses the bottom
 * side and the right side of a cell), then the two directions are handled by two distinct instances
 * connected by their {@link #opposite} field.
 *
 * <p>When a polyline has been completed, its content is copied to {@link Tracer.Level#path}
 * and the {@code PolylineBuffer} object is recycled for a new polyline.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.1
 * @module
 */
final class PolylineBuffer {
    /**
     * Number of coordinates in a tuple.
     */
    static final int DIMENSION = 2;

    /**
     * Coordinates as (x,y) tuples. This array is expanded as needed.
     */
    double[] coordinates;

    /**
     * Number of valid elements in the {@link #coordinates} array.
     * This is twice the number of points.
     */
    int size;

    /**
     * If the polyline has points added to its two extremities, the other extremity. Otherwise {@code null}.
     * The first point of {@code opposite} polyline is connected to the first point of this polyline.
     * Consequently when those two polylines are joined in a single polyline, the coordinates of either
     * {@code this} or {@code opposite} must be iterated in reverse order.
     */
    PolylineBuffer opposite;

    /**
     * Creates an initially empty polyline.
     */
    PolylineBuffer() {
        coordinates = ArraysExt.EMPTY_DOUBLE;
    }

    /**
     * Creates a new polyline wrapping the given coordinates. Used for wrapping {@link Fragments}
     * instances in objects expected by {@link Tracer#writeTo(Joiner, Polyline[], boolean)}.
     * Those {@code Polyline} instances are temporary.
     */
    PolylineBuffer(final double[] data) {
        coordinates = data;
        size = data.length;
    }

    /**
     * Discards all coordinates in this polyline. This method does not clear
     * the {@link #opposite} polyline; it is caller's responsibility to do so.
     */
    final void clear() {
        opposite = null;
        size = 0;
    }

    /**
     * Returns whether this polyline is empty. This method is used only for {@code assert isEmpty()}
     * statement because of the check for {@code opposite == null}: an empty polyline should not have
     * a non-null {@link #opposite} value.
     */
    final boolean isEmpty() {
        return size == 0 & (opposite == null);
    }

    /**
     * Declares that the specified polyline will add points in the direction opposite to this polyline.
     * This happens when the polyline crosses the bottom side and the right side of a cell (assuming an
     * iteration from left to right and top to bottom).
     *
     * <p>This method is typically invoked in the following pattern (but this is not mandatory).
     * An important aspect is that {@code this} and {@code other} should be on perpendicular axes:</p>
     *
     * {@preformat java
     *     interpolateOnBottomSide(polylinesOnTop[x].attach(polylineOnLeft));
     * }
     *
     * @return {@code this} for method calls chaining.
     */
    final PolylineBuffer attach(final PolylineBuffer other) {
        assert (opposite == null) & (other.opposite == null);
        other.opposite = this;
        opposite = other;
        return this;
    }

    /**
     * Transfers all coordinates from given polylines to this polylines, in same order.
     * This is used when polyline on the left side continues on bottom side,
     * or conversely when polyline on the top side continues on right side.
     * This polyline shall be empty before this method is invoked.
     * The given source will become empty after this method returned.
     *
     * @param  source  the source from which to take data.
     * @return {@code this} for method calls chaining.
     */
    final PolylineBuffer transferFrom(final PolylineBuffer source) {
        assert isEmpty();
        final double[] swap = coordinates;
        coordinates = source.coordinates;
        size        = source.size;
        opposite    = source.opposite;
        if (opposite != null) {
            opposite.opposite = this;
        }
        source.clear();
        source.coordinates = swap;
        return this;
    }

    /**
     * Transfers all coordinates from this polyline to the polyline going in opposite direction.
     * This is used when this polyline reached the right image border, in which case its data
     * will be lost if we do not copy them somewhere.
     *
     * @return {@code true} if coordinates have been transferred,
     *         or {@code false} if there is no opposite direction.
     */
    final boolean transferToOpposite() {
        if (opposite == null) {
            return false;
        }
        final int sum = size + opposite.size;
        double[] data = opposite.coordinates;
        if (sum > data.length) {
            data = new double[sum];
        }
        System.arraycopy(opposite.coordinates, 0, data, size, opposite.size);
        for (int i=0, t=size; (t -= DIMENSION) >= 0;) {
            data[t  ] = coordinates[i++];
            data[t+1] = coordinates[i++];
        }
        opposite.size = sum;
        opposite.coordinates = data;
        opposite.opposite = null;
        clear();
        return true;
    }

    /**
     * Appends given coordinates to this polyline.
     *
     * @param  x  first coordinate of the (x,y) tuple to add.
     * @param  y  second coordinate of the (x,y) tuple to add.
     */
    final void append(final double x, final double y) {
        if (size >= coordinates.length) {
            coordinates = Arrays.copyOf(coordinates, Math.max(Math.multiplyExact(size, 2), 32));
        }
        coordinates[size++] = x;
        coordinates[size++] = y;
    }

    /**
     * Returns a string representation of this {@code Polyline} for debugging purposes.
     */
    @Override
    public String toString() {
        return PathBuilder.toString(coordinates, size);
    }

    /**
     * Appends the pixel coordinates of this polyline to the given path, for debugging purposes only.
     * The {@link #gridToCRS} transform is <em>not</em> applied by this method.
     * For avoiding confusing behavior, that transform should be null.
     *
     * @param  appendTo  where to append the coordinates.
     *
     * @see Tracer.Level#toRawPath(Path2D)
     */
    @Debug
    final void toRawPath(final Path2D appendTo) {
        int i = 0;
        if (i < size) {
            appendTo.moveTo(coordinates[i++], coordinates[i++]);
            while (i < size) {
                appendTo.lineTo(coordinates[i++], coordinates[i++]);
            }
        }
    }
}
