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

import org.apache.sis.internal.feature.j2d.PathBuilder;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;


/**
 * Assembles arbitrary number of {@link PolylineBuffer}s in a single Java2D {@link Shape} for an isoline level.
 * This class extends {@link PathBuilder} with two additional features: remove spikes caused by ambiguities,
 * then apply a {@link MathTransform} on all coordinate values.
 *
 * <h2>Spikes</h2>
 * If the shape delimited by given polylines has a part with zero width or height ({@literal i.e.} a spike),
 * truncates the polylines for removing that spike. This situation happens when some pixel values are exactly
 * equal to isoline value, as in the picture below:
 *
 * <pre class="text">
 *     ●╌╌╌╲╌╌○╌╌╌╌╌╌○╌╌╌╌╌╌○╌╌╌╌╌╌○
 *     ╎    ╲ ╎      ╎      ╎      ╎
 *     ╎     ╲╎      ╎   →  ╎      ╎
 *     ●╌╌╌╌╌╌●──────●──────●⤸╌╌╌╌╌○
 *     ╎     ╱╎      ╎   ←  ╎      ╎
 *     ╎    ╱ ╎      ╎      ╎      ╎
 *     ●╌╌╌╱╌╌○╌╌╌╌╌╌○╌╌╌╌╌╌○╌╌╌╌╌╌○</pre>
 *
 * The spike may appear or not depending on the convention adopted for strictly equal values.
 * In above picture, the spike appears because the convention used in this implementation is:
 *
 * <ul>
 *   <li>○: {@literal pixel value < isoline value}.</li>
 *   <li>●: {@literal pixel value ≥ isoline value}.</li>
 * </ul>
 *
 * If the following convention was used instead, the spike would not appear in above figure
 * (but would appear in different situations):
 *
 * <ul>
 *   <li>○: {@literal pixel value ≤ isoline value}.</li>
 *   <li>●: {@literal pixel value > isoline value}.</li>
 * </ul>
 *
 * This class detects and removes those spikes for avoiding convention-dependent results.
 * We assume that spikes can appear only at the junction between two {@link PolylineBuffer} instances.
 * Rational: having a spike require that we move forward then backward on the same coordinates,
 * which is possible only with a non-null {@link PolylineBuffer#opposite} field.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.1
 * @module
 */
final class Joiner extends PathBuilder {
    /**
     * Final transform to apply on coordinates, or {@code null} if none.
     */
    private final MathTransform gridToCRS;

    /**
     * Creates an initially empty set of isoline shapes.
     */
    Joiner(final MathTransform gridToCRS) {
        this.gridToCRS = gridToCRS;
    }

    /**
     * Detects and removes spikes for avoiding convention-dependent results.
     * See {@link Joiner} class-javadoc for a description of the problem.
     *
     * <p>We perform the analysis in this method instead of in {@link #filterFull(double[], int)} on the
     * the assumption that spikes can appear only between two calls to {@code append(…)} (because having
     * a spike requires that we move forward then backward on the same coordinates, which happen only with
     * two distinct {@link PolylineBuffer} instances). It reduce the amount of coordinates to examine since
     * we can check only the extremities instead of looking for spikes anywhere in the array.</p>
     *
     * @param  coordinates  the coordinates to filter. Values can be modified in-place.
     * @param  lower        index of first coordinate to filter. Always even.
     * @param  upper        index after the last coordinate to filter. Always even.
     * @return number of valid coordinates after filtering.
     */
    @Override
    protected int filterChunk(final double[] coordinates, final int lower, final int upper) {
        int spike0 = lower;         // Will be index where (x,y) become different than (xo,yo).
        int spike1 = lower;         // Idem, but searching forward instead of backward.
        if (spike1 < upper) {
            final double xo = coordinates[spike1++];
            final double yo = coordinates[spike1++];
            int equalityMask = 3;                   // Bit 1 set if (x == xo), bit 2 set if (y == yo).
            while (spike1 < upper) {
                final int before = equalityMask;
                if (coordinates[spike1++] != xo) equalityMask &= ~1;
                if (coordinates[spike1++] != yo) equalityMask &= ~2;
                if (equalityMask == 0) {
                    equalityMask = before;                  // For keeping same search criterion.
                    spike1 -= PolylineBuffer.DIMENSION;      // Restore previous position before mismatch.
                    break;
                }
            }
            while (spike0 > 0) {
                if (coordinates[--spike0] != yo) equalityMask &= ~2;
                if (coordinates[--spike0] != xo) equalityMask &= ~1;
                if (equalityMask == 0) {
                    spike0 += PolylineBuffer.DIMENSION;
                    break;
                }
            }
        }
        /*
         * Here we have range of indices where the polygon has a width or height of zero.
         * Search for a common point, then truncate at that point. Indices are like below:
         *
         *     0       spike0    lower          spike1         upper
         *     ●────●────●────●────●────●────●────●────●────●────●
         *                    └╌╌╌╌remove╌╌╌╌┘
         * where:
         *  - `lower` and `spike0` are inclusive.
         *  - `upper` and `spike1` are exclusive.
         *  - the region to remove are sowewhere between `spike0` and `spike1`.
         */
        final int limit = spike1;
        int base;
        while ((base = spike0 + 2*PolylineBuffer.DIMENSION) < limit) {    // Spikes exist only with at least 3 points.
            final double xo = coordinates[spike0++];
            final double yo = coordinates[spike0++];
            spike1 = limit;
            do {
                if (coordinates[spike1 - 2] == xo && coordinates[spike1 - 1] == yo) {
                    /*
                     * Remove points between the common point (xo,yo). The common point is kept on the
                     * left side (`spike0` is already after that point) and removed on the right side.
                     */
                    System.arraycopy(coordinates, spike1, coordinates, spike0, upper - spike1);
                    return upper - (spike1 - spike0);
                }
            } while ((spike1 -= PolylineBuffer.DIMENSION) > base);
        }
        return upper;       // Nothing to remove.
    }

    /**
     * Applies user-specified coordinate transform on all points of the whole polyline.
     * This method is invoked after {@link #filterChunk(double[], int, int)}.
     */
    @Override
    protected int filterFull(final double[] coordinates, final int upper) throws TransformException {
        if (gridToCRS != null) {
            gridToCRS.transform(coordinates, 0, coordinates, 0, upper / PolylineBuffer.DIMENSION);
        }
        return upper;
    }
}
