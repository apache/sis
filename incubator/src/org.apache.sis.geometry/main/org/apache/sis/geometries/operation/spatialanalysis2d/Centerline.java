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
package org.apache.sis.geometries.operation.spatialanalysis2d;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.internal.shared.Planar;
import org.apache.sis.geometries.internal.shared.Planar.DoubleList;
import org.apache.sis.geometries.internal.shared.Planar.IntList;
import org.apache.sis.geometries.internal.shared.Planar.XY;
import org.apache.sis.geometries.operation.OperationException;
import org.apache.sis.geometries.operation.triangulate.delaunay.Delaunay;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.util.ArgumentChecks;


/**
 * Computes the centerline of a polygon, the lines running along the middle of its elongated parts,
 * for example the axis of a road.
 *
 * <p>The centerline is computed with the chordal axis transform. The rings are densified so that no segment
 * is longer than the densification distance, then the polygon is triangulated with a constrained Delaunay
 * triangulation. The edges of the triangles which are not on a ring are the chords of the polygon.
 * A triangle with two chords contributes the segment joining their middles, and a triangle with three chords
 * contributes the segments joining their middles to its centroid. Triangles with a single chord are at the ends
 * of the polygon and contribute nothing.</p>
 *
 * <p>Branches ending inside the polygon which are shorter than the {@linkplain #getMinimumBranchLength()
 * minimum branch length} are removed, they are caused by the bumps of the rings and not by the general shape.
 * The remaining segments are merged in polylines, broken at the nodes where branches meet.</p>
 *
 * <p>Only the first two coordinates are used. The result is in the horizontal component of the polygon CRS.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Centerline {

    /**
     * Maximum length of a ring segment.
     */
    private final double densification;

    /**
     * Branches with a free end shorter than this length are removed.
     */
    private double minimumBranchLength;

    /**
     * Creates a centerline extractor.
     *
     * @param densification maximum length of a ring segment, also used as default minimum branch length.
     *        Smaller values give a smoother centerline but more triangles.
     */
    public Centerline(final double densification) {
        ArgumentChecks.ensureStrictlyPositive("densification", densification);
        this.densification = densification;
        this.minimumBranchLength = 2 * densification;
    }

    /**
     * Returns the length under which a branch with a free end is removed.
     *
     * @return the minimum branch length, twice the densification distance by default
     */
    public double getMinimumBranchLength() {
        return minimumBranchLength;
    }

    /**
     * Sets the length under which a branch with a free end is removed.
     *
     * @param length the minimum branch length, zero to keep all branches
     */
    public void setMinimumBranchLength(final double length) {
        ArgumentChecks.ensurePositive("length", length);
        this.minimumBranchLength = length;
    }

    /**
     * Computes the centerline of a polygon.
     *
     * @param polygon the polygon, not null
     * @return the polylines of the centerline, empty if the polygon has no elongated part
     * @throws OperationException if the triangulation of the polygon failed
     */
    public GeometryCollection<LineString> compute(final Polygon polygon) throws OperationException {
        ArgumentChecks.ensureNonNull("polygon", polygon);
        final CoordinateReferenceSystem crs = Planar.horizontal(polygon.getCoordinateReferenceSystem());
        final GeometryFactory factory = GeometryFactory.DEFAULT;
        final List<double[]> lines = computeXY(polygon);
        final LineString[] geometries = new LineString[lines.size()];
        for (int i = 0; i < geometries.length; i++) {
            geometries[i] = factory.createLineString(factory.createDataPoints(crs, lines.get(i)));
        }
        return factory.createMultiLineString(crs, geometries);
    }

    /**
     * Computes the centerline polylines as (x,y) coordinates, two values per point.
     */
    private List<double[]> computeXY(final Polygon polygon) throws OperationException {
        final List<double[]> rings = Planar.rings(polygon);

        //densified rings, with unique points since rings may touch
        final Map<XY, Integer> unique = new HashMap<>();
        final DoubleList points = new DoubleList();
        final IntList segments = new IntList();
        for (double[] ring : rings) {
            final IntList ids = new IntList();
            for (int i = 0; i < ring.length; i += 2) {
                final double x0 = ring[i], y0 = ring[i+1];
                final double x1 = ring[(i+2) % ring.length], y1 = ring[(i+3) % ring.length];
                final int steps = Planar.steps(Math.hypot(x1 - x0, y1 - y0), densification);
                for (int k = 0; k < steps; k++) {
                    final double r = (double) k / steps;
                    ids.add(unique.computeIfAbsent(XY.of(x0 + (x1 - x0) * r, y0 + (y1 - y0) * r), (p) -> {
                        points.add(p.x(), p.y());
                        return points.size / 2 - 1;
                    }));
                }
            }
            for (int j = 0; j < ids.size; j++) {
                segments.add(ids.values[j], ids.values[(j+1) % ids.size]);
            }
        }
        final double[] xy = points.toArray();
        final int[] triangles = Planar.keep(xy, Delaunay.triangulate(xy, segments.toArray()), rings, true);

        //chords are the edges shared by two triangles, edges on the rings have a single triangle inside
        final Map<Long,Integer> edgeCount = new HashMap<>();
        for (int i = 0; i < triangles.length; i += 3) {
            for (int k = 0; k < 3; k++) {
                edgeCount.merge(Planar.edgeKey(triangles[i+k], triangles[i+(k+1)%3]), 1, Integer::sum);
            }
        }

        //build the graph of the chordal axis
        final Graph graph = new Graph();
        final Map<Long,Integer> chordNodes = new HashMap<>();
        final int[] chords = new int[3];
        for (int i = 0; i < triangles.length; i += 3) {
            int nb = 0;
            for (int k = 0; k < 3; k++) {
                final int a = triangles[i+k], b = triangles[i+(k+1)%3];
                final long key = Planar.edgeKey(a, b);
                if (edgeCount.get(key) == 2) {
                    chords[nb++] = chordNodes.computeIfAbsent(key,
                            (e) -> graph.addNode((xy[a*2] + xy[b*2]) / 2, (xy[a*2+1] + xy[b*2+1]) / 2));
                }
            }
            if (nb == 2) {
                graph.addEdge(chords[0], chords[1]);
            } else if (nb == 3) {
                final int a = triangles[i]*2, b = triangles[i+1]*2, c = triangles[i+2]*2;
                final int center = graph.addNode((xy[a] + xy[b] + xy[c]) / 3, (xy[a+1] + xy[b+1] + xy[c+1]) / 3);
                for (int k = 0; k < 3; k++) {
                    graph.addEdge(center, chords[k]);
                }
            }
        }
        graph.prune(minimumBranchLength);
        return graph.polylines();
    }

    /**
     * Undirected graph of the centerline segments.
     */
    private static final class Graph {
        /** Node coordinates, two values per node. */
        private final DoubleList nodes = new DoubleList();
        /** Edge ids of each node. */
        private final List<IntList> incident = new ArrayList<>();
        /** Node ids of each edge, two values per edge. */
        private final IntList edges = new IntList();
        /** Removed edges. */
        private final BitSet removed = new BitSet();

        int addNode(final double x, final double y) {
            nodes.add(x, y);
            incident.add(new IntList());
            return incident.size() - 1;
        }

        void addEdge(final int a, final int b) {
            incident.get(a).add(edges.size / 2);
            incident.get(b).add(edges.size / 2);
            edges.add(a, b);
        }

        private int other(final int edge, final int node) {
            final int a = edges.values[edge*2];
            return (a == node) ? edges.values[edge*2+1] : a;
        }

        private int degree(final int node) {
            int degree = 0;
            final IntList list = incident.get(node);
            for (int i = 0; i < list.size; i++) {
                if (!removed.get(list.values[i])) degree++;
            }
            return degree;
        }

        /**
         * Returns an incident edge of the node which is not removed and not the excluded edge, or -1.
         */
        private int nextEdge(final int node, final int excluded) {
            final IntList list = incident.get(node);
            for (int i = 0; i < list.size; i++) {
                final int e = list.values[i];
                if (e != excluded && !removed.get(e)) return e;
            }
            return -1;
        }

        private double length(final int edge) {
            final int a = edges.values[edge*2]*2, b = edges.values[edge*2+1]*2;
            return Math.hypot(nodes.values[a] - nodes.values[b], nodes.values[a+1] - nodes.values[b+1]);
        }

        /**
         * Removes the branches with a free end which are shorter than the given length
         * and attached to a node where at least two other branches meet.
         */
        void prune(final double minLength) {
            boolean changed = true;
            while (changed) {
                changed = false;
                for (int node = 0, n = incident.size(); node < n; node++) {
                    if (degree(node) != 1) continue;
                    final BitSet path = new BitSet();
                    double length = 0;
                    int current = node;
                    for (int edge = nextEdge(node, -1); edge >= 0; edge = nextEdge(current, edge)) {
                        path.set(edge);
                        length += length(edge);
                        current = other(edge, current);
                        if (degree(current) != 2 || length >= minLength) break;
                    }
                    if (length < minLength && degree(current) >= 3) {
                        removed.or(path);
                        changed = true;
                    }
                }
            }
        }

        /**
         * Merges the remaining edges in polylines broken at the nodes where the degree is not 2.
         * Edges remaining after the walks from those nodes are closed loops.
         */
        List<double[]> polylines() {
            final List<double[]> lines = new ArrayList<>();
            final BitSet visited = (BitSet) removed.clone();
            for (int node = 0, n = incident.size(); node < n; node++) {
                if (degree(node) == 2) continue;
                final IntList list = incident.get(node);
                for (int i = 0; i < list.size; i++) {
                    if (!visited.get(list.values[i])) {
                        lines.add(walk(node, list.values[i], visited));
                    }
                }
            }
            for (int e = visited.nextClearBit(0); e < edges.size / 2; e = visited.nextClearBit(e + 1)) {
                lines.add(walk(edges.values[e*2], e, visited));
            }
            return lines;
        }

        private double[] walk(final int start, int edge, final BitSet visited) {
            final DoubleList line = new DoubleList();
            line.add(nodes.values[start*2], nodes.values[start*2+1]);
            int current = start;
            while (edge >= 0 && !visited.get(edge)) {
                visited.set(edge);
                current = other(edge, current);
                line.add(nodes.values[current*2], nodes.values[current*2+1]);
                if (current == start || degree(current) != 2) break;
                edge = nextEdge(current, edge);
            }
            return line.toArray();
        }
    }
}
