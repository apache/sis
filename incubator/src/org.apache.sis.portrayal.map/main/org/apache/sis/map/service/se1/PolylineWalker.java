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
package org.apache.sis.map.service.se1;

import java.util.Arrays;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.maths.Vector;


/**
 * A polyline in display space which can be queried for positions at given distances along it.
 * Consecutive duplicated points are removed.
 *
 * @author Johann Sorel (Geomatys)
 */
final class PolylineWalker {

    /**
     * Coordinates of the polyline points.
     */
    private final double[] xs, ys;

    /**
     * Cumulative distance from the first point to each point.
     */
    private final double[] distances;

    /**
     * Creates a walker for the given line in display space.
     */
    static PolylineWalker of(LineString line) {
        final int n = line.getNumPoints();
        final double[] x = new double[n];
        final double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            final Vector<?> p = line.getPointN(i).getPosition();
            x[i] = p.get(0);
            y[i] = p.get(1);
        }
        return new PolylineWalker(x, y, n);
    }

    /**
     * Creates a walker for the given coordinates, removing consecutive duplicated points.
     */
    private PolylineWalker(final double[] x, final double[] y, final int n) {
        int count = 0;
        for (int i = 0; i < n; i++) {
            if (!Double.isFinite(x[i]) || !Double.isFinite(y[i])) continue;
            if (count > 0 && x[i] == x[count - 1] && y[i] == y[count - 1]) continue;
            x[count] = x[i];
            y[count] = y[i];
            count++;
        }
        xs = Arrays.copyOf(x, count);
        ys = Arrays.copyOf(y, count);
        distances = new double[count];
        for (int i = 1; i < count; i++) {
            distances[i] = distances[i - 1] + Math.hypot(xs[i] - xs[i - 1], ys[i] - ys[i - 1]);
        }
    }

    /**
     * Returns the total length of this polyline, 0 if it has less than two points.
     */
    double length() {
        return (distances.length == 0) ? 0 : distances[distances.length - 1];
    }

    /**
     * Returns the same polyline traversed in the opposite direction.
     */
    PolylineWalker reverse() {
        final int n = xs.length;
        final double[] x = new double[n];
        final double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = xs[n - 1 - i];
            y[i] = ys[n - 1 - i];
        }
        return new PolylineWalker(x, y, n);
    }

    /**
     * Computes the position and direction at the given distance along this polyline.
     * Distances outside the polyline range are clamped.
     *
     * @param  s       distance from the start of the polyline.
     * @param  target  where to store (x, y, angle in radians), length of at least 3.
     */
    void position(double s, final double[] target) {
        final int n = xs.length;
        if (n < 2) {
            target[0] = (n == 0) ? 0 : xs[0];
            target[1] = (n == 0) ? 0 : ys[0];
            target[2] = 0;
            return;
        }
        s = Math.max(0, Math.min(length(), s));
        int i = Arrays.binarySearch(distances, s);
        if (i < 0) i = ~i;                  // Index of the first point after s.
        i = Math.max(1, Math.min(n - 1, i));
        final double segment = distances[i] - distances[i - 1];
        final double ratio = (segment > 0) ? (s - distances[i - 1]) / segment : 0;
        final double dx = xs[i] - xs[i - 1];
        final double dy = ys[i] - ys[i - 1];
        target[0] = xs[i - 1] + dx * ratio;
        target[1] = ys[i - 1] + dy * ratio;
        target[2] = Math.atan2(dy, dx);
    }
}
