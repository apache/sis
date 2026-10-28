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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.sis.geometries.internal.shared.Planar;
import org.apache.sis.geometries.internal.shared.Planar.DoubleList;
import org.apache.sis.geometries.internal.shared.Planar.IntList;
import org.apache.sis.geometries.operation.OperationException;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.util.ArgumentChecks;


/**
 * Computes the straight skeleton of a polygon, which may have holes.
 *
 * <p>The straight skeleton is traced by the vertices of the polygon while all its edges move inward,
 * parallel to themselves, at the same speed. The time at which a skeleton node is reached is its distance
 * to the lines of the edges which created it. Seen as heights, the skeleton gives a hipped roof where every
 * face is planar and slopes up from one edge of the polygon with the same pitch.</p>
 *
 * <p>The computation simulates the moving wavefront. At each step all possible events are searched:
 * an edge collapsing to a point (edge event) or a reflex vertex reaching an opposite edge (split event),
 * and the earliest one is processed. This costs O(n²) per event, so O(n³) for the whole polygon, which is
 * fine for footprints of buildings but not for polygons with thousands of vertices.
 * Parallel opposite edges meeting at the same time, frequent in buildings, are handled by
 * joining the collapsed parts of the wavefront with ridge arcs.</p>
 *
 * <p>Only the first two coordinates are used.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class StraightSkeleton {

    /**
     * Unit direction and line offset of each input edge. The inward normal is (-dy, dx)
     * and the edge line at time t is the points q with normal·q = offset + t.
     */
    private double[] dx, dy, offset;

    /**
     * Skeleton nodes, three values per node: x, y, time.
     */
    private final DoubleList nodes = new DoubleList();

    /**
     * Skeleton arcs, two node indices per arc, and their keys for avoiding duplicates.
     */
    private final IntList arcs = new IntList();
    private final Set<Long> arcKeys = new HashSet<>();

    /**
     * Vertices of the wavefront which may still be alive.
     */
    private final List<Vertex> vertices = new ArrayList<>();

    /**
     * Distance tolerance relative to the polygon size, current time of the simulation and largest node time.
     */
    private double epsilon, now, maxTime;

    /**
     * Computes the straight skeleton of the given polygon.
     *
     * @param polygon the polygon, not null
     * @throws OperationException if the computation did not converge
     */
    public StraightSkeleton(final Polygon polygon) throws OperationException {
        ArgumentChecks.ensureNonNull("polygon", polygon);
        final List<double[]> rings = new ArrayList<>();
        for (double[] ring : Planar.rings(polygon)) {
            ring = removeCollinear(ring);
            if (ring.length >= 6) rings.add(ring);
            else if (rings.isEmpty()) return;           // Degenerated exterior ring.
        }
        if (!rings.isEmpty()) {
            initialize(rings);
            run();
        }
    }

    /**
     * Returns the skeleton nodes, three values per node: x, y and the time at which the wavefront
     * reached the node. The vertices of the polygon come first, with time 0.
     *
     * @return the nodes, a copy
     */
    public double[] getNodes() {
        return nodes.toArray();
    }

    /**
     * Returns the skeleton arcs, as pairs of indices in the {@linkplain #getNodes() nodes}.
     *
     * @return the arcs, a copy
     */
    public int[] getArcs() {
        return arcs.toArray();
    }

    /**
     * Returns the largest time of the nodes, which is the half-width of the widest part of the polygon.
     *
     * @return the largest node time
     */
    public double getMaximumTime() {
        return maxTime;
    }

    /**
     * Creates the input edges, the polygon nodes and the initial wavefront.
     * The rings are oriented with the polygon interior on their left.
     */
    private void initialize(final List<double[]> rings) {
        final double[] exterior = rings.get(0);
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < exterior.length; i += 2) {
            minX = Math.min(minX, exterior[i]);   maxX = Math.max(maxX, exterior[i]);
            minY = Math.min(minY, exterior[i+1]); maxY = Math.max(maxY, exterior[i+1]);
        }
        epsilon = 1e-9 * Math.max(Math.hypot(maxX - minX, maxY - minY), Double.MIN_NORMAL);
        final int nbEdge = rings.stream().mapToInt((ring) -> ring.length / 2).sum();
        dx = new double[nbEdge];
        dy = new double[nbEdge];
        offset = new double[nbEdge];
        int first = 0;
        for (double[] ring : rings) {
            final int n = ring.length / 2;
            for (int i = 0; i < n; i++) {
                final double x0 = ring[i*2], y0 = ring[i*2+1];
                final double ux = ring[(i+1) % n * 2] - x0, uy = ring[(i+1) % n * 2 + 1] - y0, len = Math.hypot(ux, uy);
                dx[first + i] = ux / len;
                dy[first + i] = uy / len;
                offset[first + i] = dx[first + i] * y0 - dy[first + i] * x0;
            }
            final Vertex[] loop = new Vertex[n];
            for (int i = 0; i < n; i++) {
                loop[i] = create(ring[i*2], ring[i*2+1], first + (i + n - 1) % n, first + i, addNode(ring[i*2], ring[i*2+1]));
            }
            for (int i = 0; i < n; i++) {
                link(loop[i], loop[(i+1) % n]);
            }
            first += n;
        }
    }

    /**
     * Searches and processes the events until the wavefront vanished.
     */
    private void run() throws OperationException {
        final int limit = 20 * vertices.size() * vertices.size() + 100;
        for (int iteration = 0; ; iteration++) {
            if (iteration > limit) {
                throw new OperationException("Straight skeleton computation did not converge.");
            }
            vertices.removeIf((v) -> !v.alive);
            if (vertices.isEmpty()) {
                return;
            }
            //search the earliest event, edge events first on equal times
            double best = Double.POSITIVE_INFINITY;
            Vertex bestA = null, bestB = null;
            boolean bestSplit = false;
            for (Vertex a : vertices) {
                final double t = edgeEvent(a, a.next);
                if (t < best - epsilon) {
                    best = t; bestA = a; bestB = a.next;
                }
            }
            for (Vertex v : vertices) {
                if (!v.reflex || v.antiparallel) continue;
                for (Vertex p : vertices) {
                    if (p == v || p.next == v) continue;
                    final double t = splitEvent(v, p, p.next);
                    if (t < best - epsilon) {
                        best = t; bestA = v; bestB = p; bestSplit = true;
                    }
                }
            }
            if (bestA == null) {
                //no more event: remaining loops are degenerated, join them
                for (Vertex v : vertices) {
                    if (v.alive) collapse(v);
                }
                return;
            }
            now = Math.max(now, best);
            if (bestSplit) {
                processSplit(bestA, bestB);
            } else {
                resolve(merge(bestA, bestB, (bestA.x(now) + bestB.x(now)) / 2, (bestA.y(now) + bestB.y(now)) / 2));
            }
        }
    }

    /**
     * Returns the time when vertices a and b, consecutive on the wavefront, meet. Infinity if never.
     */
    private double edgeEvent(final Vertex a, final Vertex b) {
        if (a == b || a.antiparallel || b.antiparallel) return Double.POSITIVE_INFINITY;
        final int e = a.out;
        final double gap = dx[e] * (b.x(now) - a.x(now)) + dy[e] * (b.y(now) - a.y(now));
        if (gap <= epsilon) return now;
        final double approach = dx[e] * (a.vx - b.vx) + dy[e] * (a.vy - b.vy);
        return (approach > 0) ? now + gap / approach : Double.POSITIVE_INFINITY;
    }

    /**
     * Returns the time when the reflex vertex v reaches the wavefront edge from p to q. Infinity if never.
     */
    private double splitEvent(final Vertex v, final Vertex p, final Vertex q) {
        final int e = p.out;
        if (e == v.in || e == v.out) return Double.POSITIVE_INFINITY;
        final double vx = v.x(now), vy = v.y(now), nx = -dy[e], ny = dx[e];
        //distance of v in front of the moving edge line, and speed at which they approach
        final double ahead = nx * vx + ny * vy - (offset[e] + now);
        final double closing = 1 - (nx * v.vx + ny * v.vy);
        if (ahead < -epsilon || closing <= 0) return Double.POSITIVE_INFINITY;
        final double tau = Math.max(0, ahead) / closing, t = now + tau;
        //the hit point must be on the edge at that time
        final double sh = dx[e] * (vx + v.vx * tau) + dy[e] * (vy + v.vy * tau);
        final double sp = dx[e] * p.x(t) + dy[e] * p.y(t);
        final double sq = dx[e] * q.x(t) + dy[e] * q.y(t);
        return (sh >= sp - epsilon && sh <= sq + epsilon) ? t : Double.POSITIVE_INFINITY;
    }

    /**
     * The reflex vertex v reaches the wavefront edge from p to p.next: the wavefront is split
     * in two loops, or two loops are merged if the edge is in another loop.
     */
    private void processSplit(final Vertex v, final Vertex p) {
        final double x = v.x(now), y = v.y(now);
        final int node = addNode(x, y);
        addArc(v.node, node);
        v.alive = false;
        final Vertex w1 = create(x, y, v.in, p.out, node);
        final Vertex w2 = create(x, y, p.out, v.out, node);
        final Vertex q = p.next;
        link(v.prev, w1);
        link(w1, q);
        link(p, w2);
        link(w2, v.next);
        resolve(w1);
        resolve(w2);
    }

    /**
     * Replaces the consecutive vertices from first to last by a single vertex at the given location,
     * joined to them by arcs. If those vertices are the whole loop, the loop is removed.
     *
     * @return the new vertex, or null if the loop has been removed
     */
    private Vertex merge(final Vertex first, final Vertex last, final double x, final double y) {
        final int node = addNode(x, y);
        for (Vertex v = first; ; v = v.next) {
            addArc(v.node, node);
            v.alive = false;
            if (v == last) break;
        }
        if (last.next == first) {
            return null;
        }
        final Vertex m = create(x, y, first.in, last.out, node);
        link(first.prev, m);
        link(m, last.next);
        return m;
    }

    /**
     * Cleans the loop containing the given vertex after an event: loops without area are joined by arcs
     * and removed, and degenerated vertices between two opposite parallel edges are moved along
     * the collapsed part of the wavefront up to the nearest of their neighbors, and merged with it.
     */
    private void resolve(Vertex start) {
        while (start != null && start.alive) {
            final List<Vertex> loop = loop(start);
            if (isFlat(loop)) {
                collapse(start);
                return;
            }
            final Vertex w = loop.stream().filter((v) -> v.antiparallel).findFirst().orElse(null);
            if (w == null) {
                return;
            }
            final Vertex p = w.prev, n = w.next;
            final double px = p.x(now), py = p.y(now), nx = n.x(now), ny = n.y(now);
            final double distP = dx[w.out] * (px - w.x) + dy[w.out] * (py - w.y);
            final double distN = dx[w.out] * (nx - w.x) + dy[w.out] * (ny - w.y);
            if (Math.abs(distP - distN) <= epsilon || p.prev == n) {
                start = merge(p, n, (px + nx) / 2, (py + ny) / 2);
            } else if (distN < distP) {
                start = merge(w, n, nx, ny);
            } else {
                start = merge(p, w, px, py);
            }
        }
    }

    /**
     * Removes the loop containing the given vertex, joining its vertices by arcs at their current location.
     */
    private void collapse(final Vertex start) {
        final List<Vertex> loop = loop(start);
        final int[] current = new int[loop.size()];
        for (int i = 0; i < current.length; i++) {
            final Vertex v = loop.get(i);
            current[i] = addNode(v.x(now), v.y(now));
            addArc(v.node, current[i]);
            v.alive = false;
        }
        for (int i = 0; i < current.length; i++) {
            addArc(current[i], current[(i + 1) % current.length]);
        }
    }

    private static List<Vertex> loop(final Vertex start) {
        final List<Vertex> loop = new ArrayList<>();
        Vertex v = start;
        do {
            loop.add(v);
            v = v.next;
        } while (v != start);
        return loop;
    }

    /**
     * Returns whether the area of the loop is negligible compared to its perimeter.
     */
    private boolean isFlat(final List<Vertex> loop) {
        double area = 0, perimeter = 0;
        for (int i = 0, n = loop.size(); i < n; i++) {
            final Vertex a = loop.get(i), b = loop.get((i + 1) % n);
            final double ax = a.x(now), ay = a.y(now), bx = b.x(now), by = b.y(now);
            area += ax * by - bx * ay;
            perimeter += Math.hypot(bx - ax, by - ay);
        }
        return Math.abs(area) / 2 <= epsilon * perimeter;
    }

    private Vertex create(final double x, final double y, final int in, final int out, final int node) {
        final Vertex w = new Vertex();
        w.x = x;
        w.y = y;
        w.t = now;
        w.in = in;
        w.out = out;
        w.node = node;
        //velocity v such as v·n_in = 1 and v·n_out = 1
        final double nix = -dy[in], niy = dx[in], nox = -dy[out], noy = dx[out];
        final double dot = nix * nox + niy * noy;
        w.antiparallel = (1 + dot) < 1e-12;
        if (!w.antiparallel) {
            w.vx = (nix + nox) / (1 + dot);
            w.vy = (niy + noy) / (1 + dot);
        }
        //turning right while the interior is on the left
        w.reflex = (dx[in] * dy[out] - dy[in] * dx[out]) < 0;
        vertices.add(w);
        return w;
    }

    private static void link(final Vertex a, final Vertex b) {
        a.next = b;
        b.prev = a;
    }

    /**
     * Returns the node at the given location and current time, creating it if none is closer than the tolerance.
     */
    private int addNode(final double x, final double y) {
        for (int i = 0; i < nodes.size; i += 3) {
            if (Math.abs(nodes.values[i] - x) <= epsilon && Math.abs(nodes.values[i+1] - y) <= epsilon) {
                return i / 3;
            }
        }
        nodes.add(x, y, now);
        maxTime = Math.max(maxTime, now);
        return nodes.size / 3 - 1;
    }

    private void addArc(final int a, final int b) {
        if (a != b && arcKeys.add(Planar.edgeKey(a, b))) {
            arcs.add(a, b);
        }
    }

    /**
     * Removes the vertices aligned with their neighbors.
     */
    private static double[] removeCollinear(final double[] ring) {
        final DoubleList result = new DoubleList();
        final int n = ring.length / 2;
        for (int i = 0; i < n; i++) {
            final int p = (i + n - 1) % n * 2, c = i * 2, q = (i + 1) % n * 2;
            if ((ring[c] - ring[p]) * (ring[q+1] - ring[c+1]) - (ring[c+1] - ring[p+1]) * (ring[q] - ring[c]) != 0) {
                result.add(ring[c], ring[c+1]);
            }
        }
        return result.toArray();
    }

    /**
     * A vertex of the moving wavefront.
     */
    private static final class Vertex {
        /** Position at the creation time. */
        double x, y, t;
        /** Velocity. */
        double vx, vy;
        /** Incoming and outgoing input edges. */
        int in, out;
        /** Skeleton node where the vertex started. */
        int node;
        /** Neighbors on the wavefront. */
        Vertex prev, next;
        /** Whether the vertex turns right, it may split an opposite edge. */
        boolean reflex;
        /** Whether the incoming and outgoing edges are opposite, the vertex is on a collapsed part. */
        boolean antiparallel;
        boolean alive = true;

        double x(final double time) {
            return x + vx * (time - t);
        }

        double y(final double time) {
            return y + vy * (time - t);
        }
    }
}
