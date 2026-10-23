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
package org.apache.sis.geometries.internal.shared;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.curve.LinearRing;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.maths.Array;
import org.apache.sis.referencing.CRS;


/**
 * Helper methods for computations in the (x,y) plane on coordinates stored in primitive arrays,
 * two values per point.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Planar {

    private Planar() {
    }

    /**
     * Returns the (x,y) coordinates of the rings of a polygon, without consecutive duplicated points
     * and without closing point. The exterior ring is counterclockwise and the holes are clockwise,
     * so the polygon interior is always on the left. Degenerated holes are omitted, and the list is
     * empty if the exterior ring is degenerated.
     *
     * @param  polygon  the polygon.
     * @return exterior ring followed by the interior rings.
     */
    public static List<double[]> rings(final Polygon polygon) {
        final List<double[]> rings = new ArrayList<>();
        for (int i = -1, n = polygon.getNumInteriorRing(); i < n; i++) {
            final double[] ring = ring((i < 0) ? polygon.getExteriorRing() : polygon.getInteriorRingN(i), i < 0);
            if (ring.length >= 6) rings.add(ring);
            else if (i < 0) break;
        }
        return rings;
    }

    private static double[] ring(final LinearRing ring, final boolean counterclockwise) {
        final Array positions = ring.getDataPoints().getAttributeArray(DataPointsType.ATT_POSITION);
        final int dim = positions.getDimension();
        final double[] coords = positions.toArrayDouble();
        final double[] xy = new double[coords.length / dim * 2];
        for (int i = 0; i < xy.length / 2; i++) {
            xy[i*2]   = coords[i*dim];
            xy[i*2+1] = coords[i*dim+1];
        }
        final int n = removeDuplicates(xy, xy.length);
        double area = 0;
        for (int i = 0, j = n - 2; i < n; j = i, i += 2) {
            area += xy[j] * xy[i+1] - xy[i] * xy[j+1];
        }
        if ((area > 0) != counterclockwise) {
            for (int i = 0, j = n - 2; i < j; i += 2, j -= 2) {
                final double x = xy[i], y = xy[i+1];
                xy[i] = xy[j]; xy[i+1] = xy[j+1];
                xy[j] = x;     xy[j+1] = y;
            }
        }
        return Arrays.copyOf(xy, n);
    }

    /**
     * Removes consecutive duplicated points in place, including last points equal to the first one.
     *
     * @param  xy      the coordinates, two values per point.
     * @param  length  number of valid values in the array.
     * @return number of valid values after the removal.
     */
    public static int removeDuplicates(final double[] xy, final int length) {
        int n = 0;
        for (int i = 0; i < length; i += 2) {
            if (n == 0 || xy[i] != xy[n-2] || xy[i+1] != xy[n-1]) {
                xy[n++] = xy[i];
                xy[n++] = xy[i+1];
            }
        }
        while (n >= 4 && xy[0] == xy[n-2] && xy[1] == xy[n-1]) {
            n -= 2;
        }
        return n;
    }

    /**
     * Returns whether a point is inside rings, using the even-odd rule.
     * A point on a ring gets an arbitrary answer.
     *
     * @param  rings  the rings, without closing point.
     * @param  x      point x.
     * @param  y      point y.
     * @return whether the point is inside.
     */
    public static boolean inside(final List<double[]> rings, final double x, final double y) {
        boolean inside = false;
        for (double[] ring : rings) {
            for (int i = 0, j = ring.length - 2; i < ring.length; j = i, i += 2) {
                final double xi = ring[i], yi = ring[i+1], xj = ring[j], yj = ring[j+1];
                if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) {
                    inside = !inside;
                }
            }
        }
        return inside;
    }

    /**
     * Keeps the triangles inside, or outside, a boundary made of edges of the triangulation.
     * The triangle on the left of a boundary edge is inside and the one on its right is outside,
     * and the sides are propagated to the neighbors through the other edges. This is exact even for
     * very thin triangles along a boundary whose points are slightly off the lines of the rings.
     * The triangles not connected to the boundary are classified by their centroid relative to the rings.
     *
     * @param  xy         the points coordinates.
     * @param  triangles  counterclockwise triangles, three point indices per triangle.
     * @param  boundary   directed boundary edges, two point indices per edge, with the interior on their left.
     * @param  rings      the rings, see {@link #inside(List, double, double)}.
     * @param  inside     {@code true} for keeping triangles inside, {@code false} for outside.
     * @return the kept triangles.
     */
    public static int[] keep(final double[] xy, final int[] triangles, final int[] boundary,
                             final List<double[]> rings, final boolean inside)
    {
        final Map<Long,Integer> byEdge = new HashMap<>();
        for (int i = 0; i < triangles.length; i++) {
            byEdge.put(directed(triangles[i], triangles[i - i%3 + (i+1)%3]), i / 3);
        }
        final Set<Long> walls = new HashSet<>();
        final byte[] side = new byte[triangles.length / 3];
        final ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int i = 0; i < boundary.length; i += 2) {
            final int u = boundary[i], v = boundary[i+1];
            walls.add(edgeKey(u, v));
            label(byEdge.get(directed(u, v)), (byte)  1, side, queue);
            label(byEdge.get(directed(v, u)), (byte) -1, side, queue);
        }
        for (Integer t; (t = queue.poll()) != null;) {
            for (int k = 0; k < 3; k++) {
                final int a = triangles[t*3+k], b = triangles[t*3+(k+1)%3];
                if (!walls.contains(edgeKey(a, b))) {
                    label(byEdge.get(directed(b, a)), side[t], side, queue);
                }
            }
        }
        int n = 0;
        final int[] kept = new int[triangles.length];
        for (int t = 0; t < side.length; t++) {
            final int a = triangles[t*3]*2, b = triangles[t*3+1]*2, c = triangles[t*3+2]*2;
            final boolean in = (side[t] != 0) ? side[t] > 0
                    : inside(rings, (xy[a] + xy[b] + xy[c]) / 3, (xy[a+1] + xy[b+1] + xy[c+1]) / 3);
            if (in == inside) {
                System.arraycopy(triangles, t*3, kept, n, 3);
                n += 3;
            }
        }
        return Arrays.copyOf(kept, n);
    }

    /**
     * Sets the side of a triangle if not already known, and adds it to the triangles to propagate.
     */
    private static void label(final Integer triangle, final byte value, final byte[] side, final ArrayDeque<Integer> queue) {
        if (triangle != null && side[triangle] == 0) {
            side[triangle] = value;
            queue.add(triangle);
        }
    }

    /**
     * Returns a key for an edge which depends on its direction.
     */
    private static long directed(final int a, final int b) {
        return (((long) a) << 32) | (b & 0xFFFFFFFFL);
    }

    /**
     * Returns the number of segments needed for no segment being longer than the given distance.
     *
     * @param  length         length to divide.
     * @param  densification  maximum segment length.
     * @return number of segments, at least one.
     */
    public static int steps(final double length, final double densification) {
        return Math.max(1, (int) Math.ceil(length / densification));
    }

    /**
     * Returns a key for an edge which does not depend on its direction.
     *
     * @param  a  index of an edge end.
     * @param  b  index of the other edge end.
     * @return the edge key.
     */
    public static long edgeKey(final int a, final int b) {
        return (((long) Math.min(a, b)) << 32) | (Math.max(a, b) & 0xFFFFFFFFL);
    }

    /**
     * Returns the horizontal component of a CRS, or an undefined 2D CRS if none.
     *
     * @param  crs  the CRS, may be null.
     * @return the 2D CRS.
     */
    public static CoordinateReferenceSystem horizontal(CoordinateReferenceSystem crs) {
        if (crs != null && crs.getCoordinateSystem().getDimension() != 2) {
            crs = CRS.getComponentAt(crs, 0, 2);
        }
        return (crs != null) ? crs : Geometries.getUndefinedCRS(2);
    }

    /**
     * Exact (x,y) location used as key in hash maps.
     *
     * @param x  point x.
     * @param y  point y.
     */
    public record XY(double x, double y) {
        /**
         * Creates a key, replacing -0 by +0 which are not equal for records.
         *
         * @param  x  point x.
         * @param  y  point y.
         * @return the key.
         */
        public static XY of(final double x, final double y) {
            return new XY(x + 0.0, y + 0.0);
        }
    }

    /**
     * Growable array of doubles.
     */
    public static final class DoubleList {
        /** The values, valid up to {@link #size}. */
        public double[] values = new double[64];
        /** Number of valid values. */
        public int size;

        /**
         * Appends values.
         *
         * @param  v  the values to append.
         */
        public void add(final double... v) {
            if (size + v.length > values.length) values = Arrays.copyOf(values, Math.max(size * 2, size + v.length));
            System.arraycopy(v, 0, values, size, v.length);
            size += v.length;
        }

        /**
         * Returns a copy of the valid values.
         *
         * @return the values.
         */
        public double[] toArray() {
            return Arrays.copyOf(values, size);
        }
    }

    /**
     * Growable array of integers.
     */
    public static final class IntList {
        /** The values, valid up to {@link #size}. */
        public int[] values = new int[16];
        /** Number of valid values. */
        public int size;

        /**
         * Appends values.
         *
         * @param  v  the values to append.
         */
        public void add(final int... v) {
            if (size + v.length > values.length) values = Arrays.copyOf(values, Math.max(size * 2, size + v.length));
            System.arraycopy(v, 0, values, size, v.length);
            size += v.length;
        }

        /**
         * Returns a copy of the valid values.
         *
         * @return the values.
         */
        public int[] toArray() {
            return Arrays.copyOf(values, size);
        }
    }
}
