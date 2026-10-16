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
package org.apache.sis.geometries.operation.triangulate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.curve.LinearRing;
import org.apache.sis.geometries.operation.Distance;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.maths.Maths;
import org.apache.sis.maths.Vector;


/**
 * Joins the holes of a polygon to its exterior ring, which gives a polygon without hole covering the same area.
 *
 * <p>The holes are first oriented in the opposite direction of the exterior ring, then joined from the
 * rightmost one to the leftmost one. Each hole is joined by a cut going from a vertex of the ring to the
 * rightmost position of the hole, around the hole, and back to the same vertex of the ring. The vertex
 * of the ring is one which can be reached without crossing any edge, found as described by David Eberly
 * in <cite>Triangulation by Ear Clipping</cite>. The two sides of a cut are made of the same positions,
 * so the resulting ring touches itself along the cut, and no position is added.</p>
 *
 * <p>If no such vertex is found, which may happen with an invalid polygon, the cut starts at a new
 * position on the nearest segment of the ring, whose attributes are interpolated along that segment.</p>
 *
 * <p>The positions are compared in the coordinate plane where the polygon has its largest area,
 * so that a vertical polygon is not degenerate. This is the first stage of {@link EarClipping}.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class HoleToExterior {

    /**
     * Positions of the polygon, of the exterior ring followed by those of the holes.
     * The vertex of index <var>i</var> is the position <var>i</var> of this {@code DataPoints}
     * if <var>i</var> is smaller than its size, or a vertex added for a cut otherwise.
     */
    private final DataPoints source;

    /**
     * Positions of all the vertices, with their coordinates in the order used for comparing them.
     */
    private final List<Vector<?>> working = new ArrayList<>();

    /**
     * For each vertex added to start a cut, the indices of the positions of the polygon it is interpolated from.
     * The vertex at index <var>i</var> in this list has the index {@code source.size() + i}.
     */
    private final List<int[]> addedIndices = new ArrayList<>();

    /**
     * For each vertex added to start a cut, the weights of the positions of the polygon it is interpolated from.
     */
    private final List<double[]> addedWeights = new ArrayList<>();

    /**
     * Prepares the joining of the holes of the given polygon.
     */
    private HoleToExterior(final Polygon polygon) {
        source = polygon.getDataPoints();
        final int[] axes = EarClipping.workingAxes(
                polygon.getExteriorRing().getDataPoints().getAttributeArray(DataPointsType.ATT_POSITION));
        for (int i = 0, n = source.size(); i < n; i++) {
            working.add(source.getPosition(i).swizzle(axes));
        }
    }

    /**
     * Returns a polygon without hole covering the same area as the given polygon.
     * The exterior ring of the returned polygon is the exterior ring of the given polygon with the holes
     * joined to it. Its positions are those of the given polygon, with all their attributes, some of them
     * repeated at both ends of the cuts. Positions are added only for the cuts which could not start
     * from a vertex.
     *
     * @param  polygon  the polygon whose holes are to be joined to the exterior ring.
     * @return the given polygon if it has no hole, or a new polygon without hole otherwise.
     */
    public static Polygon apply(final Polygon polygon) {
        if (polygon.getExteriorRing().getDataPoints().isEmpty()) {
            return polygon;
        }
        for (final LinearRing hole : polygon.getInteriorRings()) {
            if (!hole.getDataPoints().isEmpty()) {
                return new HoleToExterior(polygon).join(polygon);
            }
        }
        return polygon;
    }

    /**
     * Joins the holes of the given polygon, which is the polygon given to the constructor.
     */
    private Polygon join(final Polygon polygon) {
        //the positions of the polygon are those of the exterior ring followed by those of the holes.
        int start = polygon.getExteriorRing().getDataPoints().size();
        final List<Integer> border = range(0, start);
        final List<List<Integer>> holes = new ArrayList<>();
        for (final LinearRing hole : polygon.getInteriorRings()) {
            final int size = hole.getDataPoints().size();
            if (size != 0) {
                holes.add(range(start, size));
            }
            start += size;
        }

        //the holes shall turn in the opposite direction of the exterior ring, so that the joined ring has a single orientation.
        final double orientation = Math.signum(area(border));
        for (final List<Integer> loop : holes) {
            if (Math.signum(area(loop)) == orientation) {
                Collections.reverse(loop);
            }
        }

        //sort the holes by decreasing maximum x value: the region on the right of a hole is then
        //bounded by the exterior ring and by the holes already joined to it.
        holes.sort(Comparator.comparingDouble((List<Integer> h) ->
                h.stream().mapToDouble(this::x).max().orElse(Double.NaN)).reversed());

        //attach holes to the main geometry
        for (final List<Integer> loop : holes) {
            //remove the duplicated end point of the hole, the cut comes back to the start point instead.
            loop.remove(loop.size()-1);
            if (loop.isEmpty()) {
                continue;
            }
            //the cut starts from the hole position of maximum x.
            int index = 0;
            for (int k = 1; k < loop.size(); k++) {
                if (x(loop.get(k)) > x(loop.get(index))) {
                    index = k;
                }
            }
            final int rightmost = loop.get(index);
            final List<Integer> cut = new ArrayList<>(loop.size() + 2);
            cut.addAll(loop.subList(index, loop.size()));
            cut.addAll(loop.subList(0, index + 1));

            final int visible = visibleVertex(border, rightmost, orientation);
            if (visible >= 0) {
                //the cut goes from a position of the ring to the hole and back, no position is added.
                cut.add(border.get(visible));
                border.addAll(visible + 1, cut);
            } else {
                //no visible position, which may happen with an invalid polygon. The cut starts on the nearest
                //segment of the ring, at a new position whose coordinates and attributes are interpolated.
                final Vector<?> m = working.get(rightmost);
                final double[] buffer1 = new double[2];
                final double[] buffer2 = new double[2];
                final double[] ratio = new double[2];
                final int[] offset = new int[2];
                Distance.nearest(positions(border), buffer1, List.of(m, m), buffer2, ratio, offset, 0.000000001);
                final int bridge = interpolate(border.get(offset[0]), border.get(offset[0] + 1), ratio[0]);
                cut.add(0, bridge);
                cut.add(bridge);
                border.addAll(offset[0] + 1, cut);
            }
        }

        //build the ring, with the attributes of the positions of the polygon or interpolated for the added ones.
        final DataPointsType type = source.getType();
        final DataPoints ring = GeometryFactory.DEFAULT.createDataPoints(type, border.size());
        for (int i = 0; i < border.size(); i++) {
            final int vertex = border.get(i);
            final int added = vertex - source.size();
            for (final String name : type.getAttributeNames()) {
                ring.setAttribute(i, name, (added < 0) ? source.getAttribute(vertex, name)
                        : source.interpolateAttribute(name, addedIndices.get(added), addedWeights.get(added)));
            }
        }
        final GeometryFactory factory = GeometryFactory.DEFAULT;
        return factory.createPolygon(factory.createLinearRing(ring), null);
    }

    /**
     * Returns the position in the ring of a vertex which can be joined to the given hole vertex by a segment
     * crossing no edge, or -1 if none is found. This is the search of mutually visible vertices described by
     * David Eberly in <cite>Triangulation by Ear Clipping</cite>: a ray is cast from the hole vertex toward
     * increasing x, which first reaches the ring at a vertex or inside a segment. In the later case, the end
     * of the segment of largest x is visible unless a reflex vertex of the ring is inside the triangle made
     * of the hole vertex, the point reached and that end. Then the reflex vertex closest in angle to the ray
     * is visible.
     *
     * @param  border       the ring, the last position being the same vertex as the first one.
     * @param  hole         the hole vertex of maximum x.
     * @param  orientation  1 if the ring is counterclockwise, -1 if it is clockwise.
     * @return position in {@code border} of a visible vertex, or -1.
     */
    private int visibleVertex(final List<Integer> border, final int hole, final double orientation) {
        final double mx = x(hole), my = y(hole);
        double ix = Double.POSITIVE_INFINITY;
        int hit = -1;               // position of the vertex reached by the ray, if any.
        int edge = -1;              // position of the start of the segment reached by the ray, if any.
        for (int i = 1; i < border.size(); i++) {
            final int a = border.get(i-1), b = border.get(i);
            final double ay = y(a), by = y(b);
            for (final int k : new int[] {i-1, i}) {
                final int v = border.get(k);
                if (y(v) == my && x(v) >= mx && x(v) < ix) {
                    ix = x(v); hit = k; edge = -1;
                }
            }
            if ((ay < my && by > my) || (ay > my && by < my)) {
                final double x = x(a) + (my - ay) * (x(b) - x(a)) / (by - ay);
                if (x >= mx && x < ix) {
                    ix = x; hit = -1; edge = i-1;
                }
            }
        }
        if (hit >= 0) {
            return occurrence(border, hit, hole, orientation);
        }
        if (edge < 0) {
            return -1;
        }
        int best = (x(border.get(edge + 1)) > x(border.get(edge))) ? edge + 1 : edge;
        final int end = border.get(best);
        final double[] m = {mx, my}, reached = {ix, my}, p = {x(end), y(end)};
        double bestAngle = angle(hole, end);
        double bestDistance = distance(hole, end);
        for (int k = 0; k < border.size() - 1; k++) {
            final int v = border.get(k);
            if ((x(v) == p[0] && y(v) == p[1]) || !isReflex(border, k, orientation)
                    || !Maths.inTriangle(m, reached, p, new double[] {x(v), y(v)})) {
                continue;
            }
            final double angle = angle(hole, v);
            final double distance = distance(hole, v);
            if (angle < bestAngle || (angle == bestAngle && distance < bestDistance)) {
                best = k;
                bestAngle = angle;
                bestDistance = distance;
            }
        }
        return occurrence(border, best, hole, orientation);
    }

    /**
     * Returns the position in the ring of the vertex at the given position, or of another occurrence of it
     * such as those added by previous cuts, from which the segment toward the hole vertex enters the polygon.
     */
    private int occurrence(final List<Integer> border, int position, final int hole, final double orientation) {
        if (position == border.size() - 1) {
            position = 0;           // the last position is the same vertex as the first one.
        }
        final int vertex = border.get(position);
        if (inCone(border, position, hole, orientation)) {
            return position;
        }
        for (int k = 0; k < border.size() - 1; k++) {
            final int v = border.get(k);
            if (x(v) == x(vertex) && y(v) == y(vertex) && inCone(border, k, hole, orientation)) {
                return k;
            }
        }
        return position;
    }

    /**
     * Returns whether the segment from the vertex at the given position of the ring toward the given point
     * starts inside the polygon, which is between the two segments of the ring at that vertex.
     * This is the {@code InCone} test of Joseph O'Rourke in <cite>Computational Geometry in C</cite>.
     */
    private boolean inCone(final List<Integer> border, final int position, final int point, final double orientation) {
        final int previous = border.get(position == 0 ? border.size() - 2 : position - 1);
        final int vertex   = border.get(position);
        final int next     = border.get(position + 1);
        if (orientation * cross(vertex, next, previous) >= 0) {
            return orientation * cross(vertex, point, previous) > 0
                && orientation * cross(point, vertex, next) > 0;
        }
        return !(orientation * cross(vertex, point, next) >= 0
              && orientation * cross(point, vertex, previous) >= 0);
    }

    /**
     * Returns whether the ring turns in the opposite direction of its orientation at the given position.
     */
    private boolean isReflex(final List<Integer> border, final int position, final double orientation) {
        final int previous = border.get(position == 0 ? border.size() - 2 : position - 1);
        return orientation * cross(previous, border.get(position), border.get(position + 1)) < 0;
    }

    /**
     * Returns the cross product of the vectors from <var>p</var> to <var>q</var> and from <var>p</var> to <var>r</var>,
     * positive if <var>r</var> is on the left of the line from <var>p</var> to <var>q</var>.
     */
    private double cross(final int p, final int q, final int r) {
        return (x(q) - x(p)) * (y(r) - y(p)) - (y(q) - y(p)) * (x(r) - x(p));
    }

    /**
     * Returns the angle between the increasing x direction and the segment between the two given vertices.
     */
    private double angle(final int from, final int to) {
        return Math.abs(Math.atan2(y(to) - y(from), x(to) - x(from)));
    }

    /**
     * Returns the squared distance between the two given vertices.
     */
    private double distance(final int from, final int to) {
        final double dx = x(to) - x(from), dy = y(to) - y(from);
        return dx*dx + dy*dy;
    }

    /**
     * Returns twice the signed area of the given ring, positive if it is counterclockwise.
     */
    private double area(final List<Integer> ring) {
        double area = 0;
        for (int i = 0, n = ring.size(); i < n; i++) {
            final int a = ring.get(i), b = ring.get((i + 1) % n);
            area += x(a) * y(b) - x(b) * y(a);
        }
        return area;
    }

    /**
     * Returns the first working coordinate of the given vertex.
     */
    private double x(final int vertex) {
        return working.get(vertex).get(0);
    }

    /**
     * Returns the second working coordinate of the given vertex.
     */
    private double y(final int vertex) {
        return working.get(vertex).get(1);
    }

    /**
     * Returns the indices from {@code start} inclusive to {@code start + count} exclusive, in a mutable list.
     */
    private static List<Integer> range(final int start, final int count) {
        final List<Integer> indices = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            indices.add(start + i);
        }
        return indices;
    }

    /**
     * Returns the working positions of the given vertices.
     */
    private List<Vector<?>> positions(final List<Integer> vertices) {
        final List<Vector<?>> positions = new ArrayList<>(vertices.size());
        for (final int vertex : vertices) {
            positions.add(working.get(vertex));
        }
        return positions;
    }

    /**
     * Adds a vertex on the segment joining the two given vertices, and returns its index.
     * Its working position is interpolated linearly, and its attributes will be interpolated
     * from the positions of the polygon the two vertices are made of.
     *
     * @param  start  index of the vertex at ratio 0.
     * @param  end    index of the vertex at ratio 1.
     * @param  ratio  position of the new vertex between the two vertices, from 0 to 1.
     * @return index of the new vertex.
     */
    private int interpolate(final int start, final int end, final double ratio) {
        final Vector<?> p0 = working.get(start);
        final Vector<?> p1 = working.get(end);
        final Vector<?> position = p0.copy();
        for (int d = 0; d < position.getDimension(); d++) {
            position.set(d, p0.get(d) + ratio * (p1.get(d) - p0.get(d)));
        }
        final int[] i0 = sourceIndices(start), i1 = sourceIndices(end);
        final double[] w0 = sourceWeights(start), w1 = sourceWeights(end);
        final int[] indices = Arrays.copyOf(i0, i0.length + i1.length);
        System.arraycopy(i1, 0, indices, i0.length, i1.length);
        final double[] weights = new double[indices.length];
        for (int i = 0; i < w0.length; i++) weights[i] = (1 - ratio) * w0[i];
        for (int i = 0; i < w1.length; i++) weights[i0.length + i] = ratio * w1[i];
        working.add(position);
        addedIndices.add(indices);
        addedWeights.add(weights);
        return working.size() - 1;
    }

    /**
     * Returns the indices of the positions of the polygon the given vertex is interpolated from.
     */
    private int[] sourceIndices(final int vertex) {
        final int added = vertex - source.size();
        return (added < 0) ? new int[] {vertex} : addedIndices.get(added);
    }

    /**
     * Returns the weights of the positions of the polygon the given vertex is interpolated from.
     */
    private double[] sourceWeights(final int vertex) {
        final int added = vertex - source.size();
        return (added < 0) ? new double[] {1} : addedWeights.get(added);
    }
}
