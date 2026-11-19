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
package org.apache.sis.map.service.common;

import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.maths.Vector;
import org.apache.sis.util.ArgumentChecks;


/**
 * A polyline in display coordinates, walked for finding positions at given distances along it.
 * This is used for placing graphics along a stroke and labels along a line.
 *
 * <ul>
 *   <li>Points with non-finite coordinates and consecutive duplicated points are removed.</li>
 *   <li>Distances are in display units (pixels), from the first point of the polyline.</li>
 *   <li>Directions are in radians, clockwise on screen, as display coordinates increase downward.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class LineWalker {
    /**
     * Maximal distance between a curve and its flattened lines, in pixels.
     */
    private static final double FLATNESS = 0.25;

    /**
     * Coordinates of the points.
     */
    private final double[] xs, ys;

    /**
     * Distance along the polyline from the first point to each point.
     */
    private final double[] distances;

    /**
     * Creates a walker along the given line in display coordinates.
     *
     * @param  line  the line in display coordinates.
     */
    public LineWalker(final LineString line) {
        this(new Points(line));
    }

    /**
     * Creates a walker along the given points.
     *
     * @param  points  the valid points of the polyline.
     */
    private LineWalker(final Points points) {
        final int n = points.size;
        xs = Arrays.copyOf(points.x, n);
        ys = Arrays.copyOf(points.y, n);
        distances = new double[n];
        for (int i = 1; i < n; i++) {
            distances[i] = distances[i - 1] + Math.hypot(xs[i] - xs[i - 1], ys[i] - ys[i - 1]);
        }
    }

    /**
     * Creates a walker along each sub-path of the given shape. Curves are flattened,
     * and closed sub-paths end with their first point.
     *
     * @param  shape  the lines in display coordinates.
     * @return a walker for each non-empty sub-path.
     */
    public static List<LineWalker> of(final Shape shape) {
        final List<LineWalker> walkers = new ArrayList<>();
        final Points points = new Points(8);
        final double[] point = new double[6];
        for (final PathIterator it = shape.getPathIterator(null, FLATNESS); !it.isDone(); it.next()) {
            switch (it.currentSegment(point)) {
                case PathIterator.SEG_MOVETO: {
                    points.addTo(walkers);
                    points.add(point[0], point[1]);
                    break;
                }
                case PathIterator.SEG_CLOSE: {
                    points.close();
                    break;
                }
                default: {
                    points.add(point[0], point[1]);
                    break;
                }
            }
        }
        points.addTo(walkers);
        return walkers;
    }

    /**
     * The valid points of a polyline being built.
     * Points with non-finite coordinates and consecutive duplicated points are ignored.
     */
    private static final class Points {
        /** Coordinates of the points. */
        double[] x, y;

        /** Number of points. */
        int size;

        /** Creates an empty list of points with the given initial capacity. */
        Points(final int capacity) {
            x = new double[capacity];
            y = new double[capacity];
        }

        /** Creates the list of the points of the given line. */
        Points(final LineString line) {
            this(line.getNumPoints());
            for (int i = 0, n = line.getNumPoints(); i < n; i++) {
                final Vector<?> position = line.getPointN(i).getPosition();
                add(position.get(0), position.get(1));
            }
        }

        /** Adds a point if it is finite and different than the previous point. */
        void add(final double px, final double py) {
            if (!Double.isFinite(px) || !Double.isFinite(py)) return;
            if (size > 0 && px == x[size - 1] && py == y[size - 1]) return;
            if (size == x.length) {
                x = Arrays.copyOf(x, Math.max(8, size * 2));
                y = Arrays.copyOf(y, Math.max(8, size * 2));
            }
            x[size] = px;
            y[size] = py;
            size++;
        }

        /** Adds the first point at the end, for closing the polyline. */
        void close() {
            if (size != 0) add(x[0], y[0]);
        }

        /** Adds a walker along the points in the given list if there is at least one point, then clears the points. */
        void addTo(final List<LineWalker> walkers) {
            if (size != 0) {
                walkers.add(new LineWalker(this));
                size = 0;
            }
        }
    }

    /**
     * Returns whether this polyline has no point.
     *
     * @return whether this polyline is empty.
     */
    public boolean isEmpty() {
        return xs.length == 0;
    }

    /**
     * Returns the length of this polyline, 0 if it has less than two points.
     *
     * @return the length in pixels.
     */
    public double length() {
        return (distances.length != 0) ? distances[distances.length - 1] : 0;
    }

    /**
     * Returns the position at the given distance along this polyline, with the direction of the line at that position.
     * Distances outside the polyline are clamped. A polyline of a single point has the direction 0.
     *
     * @param  distance  distance from the first point, in pixels.
     * @return the position and direction.
     * @throws IllegalStateException if this polyline is empty.
     */
    public GraphicPlacement position(final double distance) {
        final int n = xs.length;
        if (n < 2) {
            if (n == 0) {
                throw new IllegalStateException("Empty line.");
            }
            return new GraphicPlacement(xs[0], ys[0], 0);
        }
        final double s = Math.max(0, Math.min(length(), distance));
        final int found = Arrays.binarySearch(distances, s);
        final int i = Math.max(1, Math.min(n - 1, (found >= 0) ? found : ~found));
        final double segment = distances[i] - distances[i - 1];
        final double ratio = (segment > 0) ? (s - distances[i - 1]) / segment : 0;
        final double dx = xs[i] - xs[i - 1];
        final double dy = ys[i] - ys[i - 1];
        return new GraphicPlacement(xs[i - 1] + dx * ratio, ys[i - 1] + dy * ratio, Math.atan2(dy, dx));
    }

    /**
     * Returns the positions at regular distances along this polyline, with the direction of the line.
     * If the start is negative, the positions before the start of the polyline are skipped.
     *
     * @param  start  distance of the first position from the start of the polyline, in pixels.
     * @param  step   distance between two consecutive positions, in pixels.
     * @return the positions on this polyline, empty if this polyline has no length.
     * @throws IllegalArgumentException if the step is not strictly positive.
     */
    public List<GraphicPlacement> walk(final double start, final double step) {
        ArgumentChecks.ensureStrictlyPositive("step", step);
        final List<GraphicPlacement> positions = new ArrayList<>();
        final double length = length();
        if (length > 0 && Double.isFinite(start)) {
            double s = start;
            if (s < 0) {
                s += Math.ceil(-s / step) * step;
            }
            for (; s <= length; s += step) {
                positions.add(position(s));
            }
        }
        return positions;
    }

    /**
     * Returns the same polyline walked in the opposite direction.
     *
     * @return the reversed polyline.
     */
    public LineWalker reverse() {
        final Points points = new Points(xs.length);
        for (int i = xs.length; --i >= 0;) {
            points.add(xs[i], ys[i]);
        }
        return new LineWalker(points);
    }

    /**
     * Returns whether a text written along the chord between two positions would be upside down.
     * This is the case if the chord goes to the left on screen, or straight downward.
     *
     * @param  from  distance of the first position, in pixels.
     * @param  to    distance of the second position, in pixels.
     * @return whether the chord shall be reversed for keeping a text upright.
     */
    public boolean isUpsideDown(final double from, final double to) {
        final GraphicPlacement start = position(from);
        final GraphicPlacement end   = position(to);
        return readsUpsideDown(end.x() - start.x(), end.y() - start.y());
    }

    /**
     * Returns whether a text written along the given vector would be upside down.
     */
    private boolean readsUpsideDown(final double dx, final double dy) {
        return dx < 0 || (dx == 0 && dy > 0);
    }

    /**
     * Returns the direction of the chord between two positions, kept upright.
     * The angle is between −90° inclusive and 90° exclusive, so that a text rotated by this angle
     * reads from left to right, or from bottom to top on vertical lines.
     *
     * @param  from  distance of the first position, in pixels.
     * @param  to    distance of the second position, in pixels.
     * @return the direction in radians, clockwise on screen.
     */
    public double uprightDirection(final double from, final double to) {
        final GraphicPlacement start = position(from);
        final GraphicPlacement end   = position(to);
        final double dx = end.x() - start.x();
        final double dy = end.y() - start.y();
        if (dx == 0 && dy == 0) {
            return 0;
        }
        final double angle = Math.atan2(dy, dx);
        return readsUpsideDown(dx, dy) ? angle - Math.copySign(Math.PI, angle) : angle;
    }
}
