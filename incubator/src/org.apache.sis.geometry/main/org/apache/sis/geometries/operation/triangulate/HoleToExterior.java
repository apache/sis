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
import java.util.stream.IntStream;
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
 * in <cite>Triangulation by Ear Clipping</cite>. If there is none, which may happen with an invalid polygon,
 * the cut starts from the nearest end of the segment of the ring nearest to the hole. The two sides of a cut
 * are made of the same positions, so the resulting ring touches itself along the cut, and no position is added.</p>
 *
 * <p>The positions are compared in the coordinate plane where the polygon has its largest area,
 * so that a vertical polygon is not degenerate. This is the first stage of {@link EarClipping}.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class HoleToExterior {

    /**
     * The polygon whose holes are joined to the exterior ring.
     */
    private final Polygon polygon;

    /**
     * Positions of the polygon, the exterior ring first, with their coordinates in the order used for comparing them.
     */
    private final Vector<?>[] working;

    /**
     * Whether the exterior ring is clockwise. The joined ring has the same orientation.
     */
    private final boolean clockwise;

    /**
     * Prepares the joining of the holes of the given polygon.
     */
    private HoleToExterior(final Polygon polygon) {
        this.polygon = polygon;
        final DataPoints exterior  = polygon.getExteriorRing().getDataPoints();
        final DataPoints positions = polygon.getDataPoints();
        final int[] axes = EarClipping.workingAxes(exterior.getAttributeArray(DataPointsType.ATT_POSITION));
        working = new Vector<?>[positions.size()];
        Arrays.setAll(working, (i) -> positions.getPosition(i).swizzle(axes));
        clockwise = Maths.isClockWise(Arrays.asList(working).subList(0, exterior.size()));
    }

    /**
     * Returns the exterior ring of the given polygon with its holes joined to it, as the indices of its positions
     * in {@code polygon.getDataPoints()}, where the positions of the exterior ring are followed by those of the holes.
     * The positions at both ends of the cuts are repeated, but no position is added. The ring is closed: its last
     * index is the one of the last position of the exterior ring, which is at the same location as the first one.
     *
     * @param  polygon  the polygon whose holes are to be joined to the exterior ring.
     * @return indices of the positions of the joined ring, or an empty array if the exterior ring is empty.
     */
    public static int[] ring(final Polygon polygon) {
        if (polygon.getExteriorRing().getDataPoints().isEmpty()) {
            return new int[0];
        }
        return new HoleToExterior(polygon).join();
    }

    /**
     * Returns a polygon without hole covering the same area as the given polygon. The exterior ring of the
     * returned polygon is the {@linkplain #ring(Polygon) joined ring}, made of the positions of the given
     * polygon with all their attributes.
     *
     * @param  polygon  the polygon whose holes are to be joined to the exterior ring.
     * @return the given polygon if it has no hole, or a new polygon without hole otherwise.
     */
    public static Polygon apply(final Polygon polygon) {
        if (polygon.getExteriorRing().getDataPoints().isEmpty()
                || polygon.getInteriorRings().stream().allMatch((hole) -> hole.getDataPoints().isEmpty())) {
            return polygon;
        }
        final GeometryFactory factory = GeometryFactory.DEFAULT;
        return factory.createPolygon(factory.createLinearRing(polygon.getDataPoints().subset(ring(polygon))), null);
    }

    /**
     * Joins the holes of the polygon to its exterior ring.
     *
     * @return indices of the positions of the joined ring.
     */
    private int[] join() {
        //the positions of the polygon are those of the exterior ring followed by those of the holes.
        int start = polygon.getExteriorRing().getDataPoints().size();
        final List<Integer> border = range(0, start);
        final List<List<Integer>> holes = new ArrayList<>();
        for (final LinearRing hole : polygon.getInteriorRings()) {
            final int size = hole.getDataPoints().size();
            if (size > 1) {
                holes.add(range(start, size));
            }
            start += size;
        }

        //the holes shall turn in the opposite direction of the exterior ring, so that the joined ring has a single
        //orientation. Each hole is reordered to start and end at its vertex of maximum x, where its cut starts.
        for (final List<Integer> loop : holes) {
            if (Maths.isClockWise(positions(loop)) == clockwise) {
                Collections.reverse(loop);
            }
            loop.remove(loop.size() - 1);
            Collections.rotate(loop, -loop.indexOf(Collections.max(loop, Comparator.comparingDouble(this::x))));
            loop.add(loop.get(0));
        }

        //join the holes by decreasing maximum x: the region on the right of a hole is then
        //bounded by the exterior ring and by the holes already joined to it.
        holes.sort(Comparator.comparingDouble((List<Integer> loop) -> x(loop.get(0))).reversed());
        for (final List<Integer> loop : holes) {
            //the cut goes from a vertex of the ring around the hole, and back to that vertex.
            final int vertex = cutStart(border, loop.get(0));
            loop.add(border.get(vertex));
            border.addAll(vertex + 1, loop);
        }
        return border.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Returns the position in the ring of the vertex where the cut toward the given hole vertex starts.
     * It is a vertex visible from the hole vertex if there is one or, which may happen with an invalid
     * polygon, the nearest end of the segment of the ring nearest to the hole vertex.
     *
     * @param  border  the ring, the last position being the same vertex as the first one.
     * @param  hole    the hole vertex of maximum x.
     * @return position in {@code border} of the vertex where the cut starts.
     */
    private int cutStart(final List<Integer> border, final int hole) {
        final int visible = visibleVertex(border, hole);
        if (visible >= 0) {
            return visible;
        }
        final Distance.SegmentPoint nearest = Distance.nearestSegment(positions(border), working[hole]);
        return occurrence(border, nearest.segment() + (nearest.ratio() > 0.5 ? 1 : 0), hole);
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
     * @param  border  the ring, the last position being the same vertex as the first one.
     * @param  hole    the hole vertex of maximum x.
     * @return position in {@code border} of a visible vertex, or -1.
     */
    private int visibleVertex(final List<Integer> border, final int hole) {
        final double mx = x(hole), my = y(hole);
        double ix = Double.POSITIVE_INFINITY;
        int hit = -1;               // position of the vertex reached by the ray, if any.
        int edge = -1;              // position of the start of the segment reached by the ray, if any.
        for (int i = 1; i < border.size(); i++) {
            final int a = border.get(i-1), b = border.get(i);
            if (y(b) == my && x(b) >= mx && x(b) < ix) {
                ix = x(b); hit = i; edge = -1;
            } else if ((y(a) < my && y(b) > my) || (y(a) > my && y(b) < my)) {
                final double x = x(a) + (my - y(a)) * (x(b) - x(a)) / (y(b) - y(a));
                if (x >= mx && x < ix) {
                    ix = x; hit = -1; edge = i-1;
                }
            }
        }
        if (hit >= 0) {
            return occurrence(border, hit, hole);
        }
        if (edge < 0) {
            return -1;
        }
        final int end = (x(border.get(edge + 1)) > x(border.get(edge))) ? edge + 1 : edge;
        final Vector<?> m = working[hole], p = working[border.get(end)];
        final Vector<?> reached = m.copy();
        reached.set(0, ix);
        final Comparator<Integer> closestToRay = Comparator.comparingDouble((Integer k) -> angle(hole, border.get(k)))
                .thenComparingDouble((k) -> Distance.distanceSquare(m, working[border.get(k)]));
        final int visible = IntStream.range(0, border.size() - 1).boxed().filter((k) -> {
            final Vector<?> v = working[border.get(k)];
            return !v.equals(p) && isReflex(border, k) && Maths.inTriangle(m, reached, p, v);
        }).min(closestToRay).orElse(end);
        return occurrence(border, visible, hole);
    }

    /**
     * Returns the position in the ring of the vertex at the given position, or of another occurrence of it
     * such as those added by previous cuts, from which the segment toward the hole vertex enters the polygon.
     */
    private int occurrence(final List<Integer> border, int position, final int hole) {
        if (position == border.size() - 1) {
            position = 0;           // the last position is the same vertex as the first one.
        }
        if (!inCone(border, position, hole)) {
            final Vector<?> vertex = working[border.get(position)];
            for (int k = 0; k < border.size() - 1; k++) {
                if (working[border.get(k)].equals(vertex) && inCone(border, k, hole)) {
                    return k;
                }
            }
        }
        return position;
    }

    /**
     * Returns whether the segment from the vertex at the given position of the ring toward the given vertex
     * starts inside the polygon.
     */
    private boolean inCone(final List<Integer> border, final int position, final int point) {
        return Maths.inCone(working[previous(border, position)], working[border.get(position)],
                            working[border.get(position + 1)], working[point], clockwise);
    }

    /**
     * Returns whether the ring turns toward its outside at the given position,
     * which is a convex vertex for the ring traversed in the opposite direction.
     */
    private boolean isReflex(final List<Integer> border, final int position) {
        return Maths.isConvex(working[previous(border, position)], working[border.get(position)],
                              working[border.get(position + 1)], !clockwise);
    }

    /**
     * Returns the vertex before the given position of the ring, the last position being the same vertex as the first one.
     */
    private static int previous(final List<Integer> border, final int position) {
        return border.get((position == 0) ? border.size() - 2 : position - 1);
    }

    /**
     * Returns the angle between the increasing x direction and the segment between the two given vertices.
     */
    private double angle(final int from, final int to) {
        return Math.abs(Math.atan2(y(to) - y(from), x(to) - x(from)));
    }

    /**
     * Returns the first working coordinate of the given vertex.
     */
    private double x(final int vertex) {
        return working[vertex].get(0);
    }

    /**
     * Returns the second working coordinate of the given vertex.
     */
    private double y(final int vertex) {
        return working[vertex].get(1);
    }

    /**
     * Returns the working positions of the given vertices.
     */
    private List<Vector<?>> positions(final List<Integer> vertices) {
        return vertices.stream().<Vector<?>>map((vertex) -> working[vertex]).toList();
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
}
