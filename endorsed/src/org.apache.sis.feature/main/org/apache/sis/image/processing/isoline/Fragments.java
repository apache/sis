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
package org.apache.sis.image.processing.isoline;

import java.awt.Point;
import java.util.Map;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import org.apache.sis.util.internal.shared.Numerics;


/**
 * List of {@code PolylineBuffer} coordinates that have not yet been closed.
 * Each {@code double[]} in this list is a copy of a {@link PolylineBuffer} used by {@link Tracer.Level}.
 * Those copies are performed for saving data before they are overwritten by next iterated cell.
 *
 * <h2>List indices and ordering of points</h2>
 * For a given {@code Fragments} list, all {@code double[]} arrays at even indices shall have their points read
 * in reverse order and all {@code double[]} arrays at odd indices shall have their points read in forward order.
 * The list size must be even and the list may contain null elements when there is no data in the corresponding
 * iteration order. This convention makes easy to reverse the order of all points, simply by reversing the order
 * of {@code double[]} arrays: because even indices become odd and odd indices become even, points order are
 * implicitly reverted without the need to rewrite all {@code double[]} array contents.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see Tracer.Level#partialPaths
 */
@SuppressWarnings({"serial", "CloneableImplementsClone"})           // Not intended to be serialized.
final class Fragments extends ArrayList<double[]> {
    /**
     * The first points and last point in this list of polylines. By convention the coordinate having fraction
     * digits has all its bits inverted by the {@code ~} operator. May be {@code null} if a coordinate is NaN.
     * Do not modify {@link Point} field values, because those instances are keys in {@link Tracer.Level#partialPaths}.
     */
    private Point firstPoint, lastPoint;

    /**
     * Creates a list of polylines initialized to the given items.
     * The given polylines and their opposite directions are cleared by this method.
     *
     * @param  polylineOnLeft  first polyline with points in forward order. Shall not be null.
     * @param  polylineOnTop    next polyline with points in reverse order, or {@code null} if none.
     */
    Fragments(final PolylineBuffer polylineOnLeft, final PolylineBuffer polylineOnTop) {
        /*
         * Search for first and last point by inspecting `PolylineBuffer` instances in the order shown below.
         * The first 4 rows and the last 4 rows search for first point and last point respectively.
         * The empty rows in the middle are an intentional gap for creating a regular pattern that
         * we can exploit for 3 decisions that need to be done during the loop:
         *
         *     ✓ (index & 2) = 0    if using `polylineOnLeft` (otherwise `polylineOnTop`).
         *     ✓ (index % 3) = 0    if using `opposite` value of polyline (may be null).
         *     ✓ (index & 1) = 0    if fetching last point (otherwise fetch first point).
         *
         *  Index   PolylineBuffer        (order) !(i & 2)  !(i % 3)  !(i & 1)   Comment
         *  ────────────────────────────────────────────────────────────────────────────
         *   [0]    polylineOnLeft.opposite  (←)      ✓         ✓         ✓        (1)
         *   [1]    polylineOnLeft           (→)      ✓                            (2)
         *   [2]    polylineOnTop            (←)                          ✓        (1)
         *   [3]    polylineOnTop.opposite   (→)                ✓                  (2)
         *   [4]                                      ✓                   ✓
         *   |5]                                      ✓
         *   [6]    polylineOnTop.opposite   (→)                ✓         ✓        (3)
         *   [7]    polylineOnTop            (←)                                   (4)
         *   [8]    polylineOnLeft           (→)      ✓                   ✓        (3)
         *   [9]    polylineOnLeft.opposite  (←)      ✓         ✓                  (4)
         *
         * Comments:
         *   (1) Last  `PolylineBuffer` point is first `Fragments` point because of reverse iteration order.
         *   (2) First `PolylineBuffer` point is first `Fragments` point because of forward iteration order.
         *   (3) Last  `PolylineBuffer` point is last  `Fragments` point because of forward iteration order.
         *   (4) First `PolylineBuffer` point is last  `Fragments` point because of reverse iteration order.
         */
        int index = 0;
        do {
            PolylineBuffer polyline = ((index & 2) == 0) ? polylineOnLeft : polylineOnTop;  // See above table (column 4).
            if (index % 3 == 0 && polyline != null) polyline = polyline.opposite;           // See above table (column 5).
            if (polyline != null) {
                int n = polyline.size;
                if (n != 0) {
                    final double[] coordinates = polyline.coordinates;
                    final double x, y;
                    if (((index & 1) == 0)) {                          // See above table in comment (column 6).
                        y = coordinates[--n];
                        x = coordinates[--n];
                    } else {
                        x = coordinates[0];
                        y = coordinates[1];
                    }
                    final boolean isLastPoint = (index >= 6);          // See row [6] in above table.
                    if (Double.isFinite(x) && Double.isFinite(y)) {
                        final Point p = new Point((int) x, (int) y);
                        if (!Numerics.isInteger(x)) p.x = ~p.x;
                        if (!Numerics.isInteger(y)) p.y = ~p.y;
                        if (isLastPoint) {
                            lastPoint = p;
                            break;                                     // Done searching both points.
                        }
                        firstPoint = p;
                    } else if (isLastPoint) {
                        /*
                         * If the last point was NaN, check if it was also the case of first point.
                         * If yes, we will not be able to store this `Fragments` in `partialPaths`
                         * because we have no point that we can use as key (it would be pointless
                         * to search for another point further in the `coordinates` array because
                         * that point could never be matched with another `Fragments`). Leave this
                         * list empty for avoiding the copies done by `take(…)` calls. Instead,
                         * callers should write polylines in `Tracer.Level.path` immediately.
                         */
                        if (firstPoint == null) return;
                        break;
                    }
                    /*
                     * Done searching the first point (may still be null if that point is NaN).
                     * Row [6] in above table is the first row for the search of last point.
                     */
                    index = 6;
                    continue;
                }
            }
            if (++index == 4) {
                // Found no non-empty polylines during search for first point. No need to continue searching.
                return;
            }
        } while (index <= 9);
        /*
         * Copies coordinates only if at least one of `firstPoint` or `lastPoint` is a valid point.
         */
        take(polylineOnLeft.opposite);          // Point will be iterated in reverse order.
        take(polylineOnLeft);                   // Point will be iterated in forward order.
        if (polylineOnTop != null) {
            PolylineBuffer suffix = polylineOnTop.opposite;
            take(polylineOnTop);                // Inverse order. Set `polylineOnTop.opposite` to null.
            take(suffix);                       // Forward order.
        }
    }

    /**
     * Takes a copy of coordinate values of given polyline, then clears that polyline.
     */
    private void take(final PolylineBuffer polyline) {
        if (polyline != null && polyline.size != 0) {
            add(Arrays.copyOf(polyline.coordinates, polyline.size));
            polyline.clear();
        } else {
            add(null);                  // No data for iteration order at this position.
        }
    }

    /**
     * Returns {@code true} if the given point is equal to the start point or end point.
     * This is used in assertions for checking key validity in {@link Tracer.Level#partialPaths}.
     */
    final boolean isExtremity(final Point key) {
        return key.equals(firstPoint) || key.equals(lastPoint);
    }

    /**
     * Associates this polyline to its two extremities in the given map. If other polylines already exist
     * for one or both extremities, then this polyline will be merged with previously existing polylines.
     * This method returns {@code true} if caller should store the coordinates in {@link Tracer.Level#path}
     * immediately. It may be either because the polyline has been closed as a polygon, or because the two
     * extremities contains NaN values in which case the polylines are not anymore in {@code partialPaths}.
     *
     * @param  partialPaths  where to add or merge polylines.
     * @return {@code true} if this polyline became a closed polygon as a result of merge operation.
     */
    final boolean addOrMerge(final Map<Point,Fragments> partialPaths) {
        final Fragments before = partialPaths.remove(firstPoint);
        final Fragments after  = partialPaths.remove(lastPoint);
        if (before != null) partialPaths.remove(addAll(before, true));
        if (after  != null) partialPaths.remove(addAll(after, false));
        if (firstPoint != null && firstPoint.equals(lastPoint)) {       // First/last points may have changed.
            partialPaths.remove(firstPoint);
            partialPaths.remove(lastPoint);
            return true;
        } else {
            // Intentionally replace previous values.
            if (firstPoint != null) partialPaths.put(firstPoint, this);
            if (lastPoint  != null) partialPaths.put(lastPoint,  this);
            return firstPoint == null && lastPoint == null;
        }
    }

    /**
     * Prepends or appends the given polylines to this list of polylines.
     * Points order will be changed as needed in order to match extremities.
     * The {@code other} instance should be forgotten after this method call.
     *
     * @param  other    the other polyline to append or prepend to this polyline.
     * @param  prepend  {@code true} for prepend operation, {@code false} for append.
     * @return extremity of {@code other} which has not been assigned to {@code this}.
     */
    private Point addAll(final Fragments other, final boolean prepend) {
        assert ((size() | other.size()) & 1) == 0;      // Must have even number of elements in both lists.
        /*
         * In figures below, ● are the extremities to attach together.
         * `r` is a bitmask telling which polylines to reverse:
         * 1=this, 2=other, together with combinations 0=none and 3=other.
         */
        int r; if ( lastPoint != null &&  lastPoint.equals(other.firstPoint)) r = 0;    // ○──────● ●──────○
        else   if (firstPoint != null && firstPoint.equals(other.firstPoint)) r = 1;    // ●──────○ ●──────○
        else   if ( lastPoint != null &&  lastPoint.equals(other. lastPoint)) r = 2;    // ○──────● ○──────●
        else   if (firstPoint != null && firstPoint.equals(other. lastPoint)) r = 3;    // ●──────○ ○──────●
        else {
            // Should never happen because `other` has been obtained using a point of `this`.
            throw new AssertionError();
        }
        if (prepend) r ^= 3;                      // Swap order in above  ○──○ ○──○  figures.
        if ((r & 1) != 0)  this.reverse();
        if ((r & 2) != 0) other.reverse();
        if (prepend) {
            addAll(0, other);
            firstPoint = other.firstPoint;
            return other.lastPoint;
        } else {
            addAll(other);
            lastPoint = other.lastPoint;
            return other.firstPoint;
        }
    }

    /**
     * Reverse the order of all points. The last polyline will become the first polyline and vice-versa.
     * For each polyline, points will be iterated in opposite order. The trick on point order is done by
     * moving polylines at even indices to odd indices, and conversely (see class javadoc for convention
     * about even/odd indices).
     */
    private void reverse() {
        Collections.reverse(this);
        final Point swap = firstPoint;
        firstPoint = lastPoint;
        lastPoint = swap;
    }

    /**
     * Returns the content of this list as an array of {@link PolylineBuffer} instances.
     * {@code PolylineBuffer} instances at even index should be written with their points in reverse order.
     *
     * @return  elements of this array as polylines. May contain null elements.
     *
     * @see #writeTo(Joiner, PolylineBuffer[], boolean)
     */
    final PolylineBuffer[] toPolylines() {
        final PolylineBuffer[] polylines = new PolylineBuffer[size()];
        for (int i=0; i<polylines.length; i++) {
            final double[] coordinates = get(i);
            if (coordinates != null) {
                polylines[i] = new PolylineBuffer(coordinates);
            }
        }
        return polylines;
    }
}
