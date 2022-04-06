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

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.awt.Shape;
import org.opengis.referencing.operation.TransformException;


/**
 * Builds a {@link Polyline}, {@link Polygon} or {@link MultiPolylines} from given coordinates.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class PathBuilder {
    /**
     * Number of coordinates in a tuple.
     */
    private static final int DIMENSION = 2;

    /**
     * The coordinates as (x,y) tuples. The number of valid coordinates is given by {@link #size}
     * and this array is expanded as needed. Shall not contains {@link Double#NaN} values.
     */
    private double[] coordinates;

    /**
     * Number of valid coordinates. This is twice the amount of points.
     */
    private int size;

    /**
     * The polylines built from the coordinates.
     */
    private final List<Polyline> polylines;

    /**
     * Creates a new builder.
     */
    public PathBuilder() {
        coordinates = new double[100];
        polylines = new ArrayList<>();
    }

    /**
     * Verifies that {@link #size} is even, positive and smaller than the given limit.
     * This method is used for assertions.
     */
    private boolean isValidSize(final int limit) {
        return size >= 0 && size <= limit && (size & 1) == 0;
    }

    /**
     * Adds all polylines defined in the other builder. The other builder shall have no polylines under
     * construction, i.e. {@link #append(double[], int, boolean)} shall not have been invoked since last
     * {@link #createPolyline(boolean)} invocation.
     *
     * @param  other  the other builder for which to add polylines, or {@code null} if none.
     */
    public final void append(final PathBuilder other) {
        if (other != null) {
            assert other.size == 0;
            polylines.addAll(other.polylines);
        }
    }

    /**
     * Appends the given coordinates to current polyline, omitting repetitive points.
     * Coordinates are added to the same polyline than the one updated by previous calls
     * to this method, unless {@link #createPolyline(boolean)} has been invoked before.
     * The {@link #filterChunk(double[], int, int)} method is invoked after the points have been added
     * for allowing subclasses to apply customized filtering in addition to the above-cited removal
     * of repetitive points.
     *
     * <h4>NaN coordinate values</h4>
     * If the given array contains {@link Double#NaN} values, then the coordinates before and after NaNs are stored
     * in two distinct polylines. This is an exception to above paragraph saying that this method does not create
     * new polyline. The {@link #filterChunk(double[], int, int)} method will be invoked for each of those polylines.
     *
     * @param  source   coordinates to copy.
     * @param  limit    index after the last coordinate to copy. Must be an even number.
     * @param  reverse  whether to copy (x,y) tuples in reverse order.
     * @throws TransformException if {@link #filterFull(double[], int)} wanted to apply a coordinate operation
     *         and that transform failed.
     */
    public final void append(final double[] source, final int limit, final boolean reverse) throws TransformException {
        assert limit >= 0 && (limit & 1) == 0 : limit;
        int offset = size;
        if (limit >= coordinates.length - offset) {
            coordinates = Arrays.copyOf(coordinates, Math.addExact(offset, Math.max(offset, limit)));
        }
        final double[] coordinates = this.coordinates;
        double px, py;              // Previous point.
        if (offset != 0) {
            px = coordinates[offset - 2];
            py = coordinates[offset - 1];
        } else {
            px = py = Double.NaN;
        }
        for (int i=0; i<limit;) {
            final double x, y;
            if (reverse) {
                y = source[limit - ++i];
                x = source[limit - ++i];
            } else {
                x = source[i++];
                y = source[i++];
            }
            if (x != px || y != py) {
                if (Double.isNaN(x) || Double.isNaN(y)) {
                    if (offset != 0) {
                        size = filterChunk(coordinates, size, offset);
                        assert isValidSize(offset) : size;
                        createPolyline(false);
                        offset = 0;
                    }
                } else {
                    coordinates[offset++] = x;
                    coordinates[offset++] = y;
                }
                px = x;
                py = y;
            }
        }
        size = filterChunk(coordinates, size, offset);
        assert isValidSize(offset) : size;
    }

    /**
     * Applies a custom filtering on the coordinates added by a call to {@link #append(double[], int, boolean)}.
     * The default implementation does nothing. Subclasses can override this method for changing or removing some
     * coordinate values.
     *
     * <p>This method is invoked at least once per {@link #append(double[], int, boolean)} call.
     * Consequently it is not necessarily invoked with the coordinates of a complete polyline or polygon,
     * because caller can build a polyline with multiple calls to {@code append(…)}.
     * If those {@code append(…)} calls correspond to some logical chunks (at users choice),
     * this {@code filterChunk(…)} method allows users to exploit this subdivision in their processing.</p>
     *
     * @param  coordinates  the coordinates to filter. Values can be modified in-place.
     * @param  lower        index of first coordinate to filter. Always even.
     * @param  upper        index after the last coordinate to filter. Always even.
     * @return number of valid coordinates after filtering.
     *         Should be {@code upper}, unless some coordinates have been removed.
     *         Must be an even number ≥ 0 and ≤ upper.
     */
    protected int filterChunk(double[] coordinates, int lower, int upper) {
        return upper;
    }

    /**
     * Applies a custom filtering on the coordinates of a polyline or polygon.
     * The default implementation does nothing. Subclasses can override this method for changing or removing some
     * coordinate values. For example a subclass could decimate points using Ramer–Douglas–Peucker algorithm.
     * Contrarily to {@link #filterChunk(double[], int, int)}, this method is invoked when the coordinates of
     * the full polyline or polygon are available. If polyline points need to be transformed before to build
     * the final geometry, this is the right place to do so.
     *
     * @param  coordinates  the coordinates to filter. Values can be modified in-place.
     * @param  upper        index after the last coordinate to filter. Always even.
     * @return number of valid coordinates after filtering.
     *         Should be {@code upper}, unless some coordinates have been removed.
     *         Must be an even number ≥ 0 and ≤ upper.
     * @throws TransformException if this method wanted to apply a coordinate operation
     *         and that transform failed.
     */
    protected int filterFull(double[] coordinates, int upper) throws TransformException {
        return upper;
    }

    /**
     * Creates a new polyline or polygon with the coordinates added by {@link #append(double[], int, boolean)}.
     * If the first point and last point have the same coordinates, then the polyline is automatically closed as
     * a polygon. After this method call, next calls to {@code append(…)} will add coordinates in a new polyline.
     *
     * @param  close  whether to force a polygon even if source and last points are different.
     * @throws TransformException if {@link #filterFull(double[], int)} wanted to apply a coordinate operation
     *         and that transform failed.
     */
    public final void createPolyline(boolean close) throws TransformException {
        size = filterFull(coordinates, size);
        assert isValidSize(coordinates.length) : size;
        /*
         * If the point would be alone, discard the lonely point because it would be invisible
         * (a "move to" operation without "line to"). If there is two points, they should not
         * be equal because `append(…)` filtered repetitive points.
         */
        if (size >= 2*DIMENSION) {
            if (coordinates[0] == coordinates[size - 2] &&
                coordinates[1] == coordinates[size - 1])
            {
                size -= DIMENSION;
                close = true;
            }
            polylines.add(close ? new Polygon(coordinates, size) : new Polyline(coordinates, size));
        }
        size = 0;
    }

    /**
     * Returns a shape containing all polylines or polygons added to this builder.
     * The {@link #createPolyline(boolean)} method should be invoked before this method
     * for making sure that there are no pending polylines.
     *
     * @return the polyline, polygon or collector of polylines.
     *         May be {@code null} if no polyline or polygon has been created.
     */
    public final Shape build() {
        switch (polylines.size()) {
            case 0:  return null;
            case 1:  return polylines.get(0);
            default: return new MultiPolylines(polylines.toArray(new Polyline[polylines.size()]));
        }
    }

    /**
     * Returns a string representation of the polyline under construction for debugging purposes.
     * Current implementation formats only the first and last points, and tells how many points are between.
     */
    @Override
    public String toString() {
        return toString(coordinates, size);
    }

    /**
     * Returns a string representation of the given coordinates for debugging purposes.
     * Current implementation formats only the first and last points, and tells how many
     * points are between.
     *
     * @param  coordinates  the coordinates for which to return a string representation.
     * @param  size         index after the last valid coordinate in {@code coordinates}.
     * @return a string representation for debugging purposes.
     */
    public static String toString(final double[] coordinates, final int size) {
        final StringBuilder b = new StringBuilder(30).append('[');
        if (size >= DIMENSION) {
            b.append((float) coordinates[0]).append(", ").append((float) coordinates[1]);
            final int n = size - DIMENSION;
            if (n >= DIMENSION) {
                b.append(", ");
                if (size >= DIMENSION*3) {
                    b.append(" … (").append(size / DIMENSION - 2).append(" pts) … ");
                }
                b.append((float) coordinates[n]).append(", ").append((float) coordinates[n+1]);
            }
        }
        return b.append(']').toString();
    }
}
