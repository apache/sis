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
package org.apache.sis.geometries.simplify.greedyinsert;

import java.awt.geom.Point2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.LinearRing;
import org.apache.sis.geometries.PointSequence;
import org.apache.sis.geometries.Triangle;
import org.apache.sis.geometries.math.Maths;
import org.apache.sis.geometries.math.Tuple;
import org.apache.sis.geometries.math.TupleArray;
import org.apache.sis.geometries.math.TupleArrays;
import org.apache.sis.geometries.operation.OperationException;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.ArgumentChecks;


/**
 * Variant of progressive TIN creation until an error threshold is met.
 * This approach is called 'greedy insertion'.
 *
 * Multiple papers and algorithms variants exist.
 * http://mgarland.org/files/papers/scape.pdf
 * https://pdfs.semanticscholar.org/5f25/071f17d3a6bd730463984644767d206d0def.pdf
 *
 * @author Johann Sorel (Geomatys)
 */
public final class TINBuilder {

    private static final Logger LOGGER = Logger.getLogger("org.apache.sis.geometries");

    private final CoordinateReferenceSystem crs;
    private final ArrayDeque<WTriangle> workStackTriangles = new ArrayDeque<>();
    private final ArrayDeque<Edge> workStackEdges = new ArrayDeque<>();
    private final List<WTriangle> finished = new ArrayList<>();
    private final List<Tuple> vertices = new ArrayList<>();
    private final double delta;
    private final BiFunction<Tuple,Triangle,Double> errorCalculator;
    private boolean decimateInsert = true;

    /**
     *
     *  p0 +---+ p1
     *     | / |
     *  p2 +---+ p3
     *
     * @param p0 first dem corner
     * @param p1 second dem corner
     * @param p2 third dem corner
     * @param p3 fourth dem corner
     * @param delta minimum distance to dem to include point
     * @throws OperationException if an invalid geometry state occurs
     */
    public TINBuilder(Tuple p0, Tuple p1, Tuple p2, Tuple p3, double delta, BiFunction<Tuple,Triangle,Double> errorCalculator) throws OperationException {
        this.crs = p0.getCoordinateReferenceSystem();
        ArgumentChecks.ensureNonNull("crs", crs);
        if (  !crs.equals(p1.getCoordinateReferenceSystem())
           || !crs.equals(p2.getCoordinateReferenceSystem())
           || !crs.equals(p3.getCoordinateReferenceSystem())
            ) {
            throw new IllegalArgumentException("All corner points must have the same crs");
        }

        this.delta = delta;
        this.errorCalculator = errorCalculator;

        final Edge e01 = new Edge(p0, p1);
        final Edge e02 = new Edge(p0, p2);
        final Edge e12 = new Edge(p1, p2);
        final Edge e31 = new Edge(p3, p1);
        final Edge e32 = new Edge(p3, p2);

        final WTriangle t0 = new WTriangle(e01,e12,e02, errorCalculator);
        final WTriangle t1 = new WTriangle(e12,e31,e32, errorCalculator);

        e01.t1 = t0;
        e02.t0 = t0;
        e12.t1 = t0;
        e12.t0 = t1;
        e31.t0 = t1;
        e32.t1 = t1;

        vertices.add(p0);
        vertices.add(p1);
        vertices.add(p2);
        vertices.add(p3);

        finished.add(t0);
        finished.add(t1);

        assert t0.validate();
        assert t1.validate();
    }

    public List<Triangle> getTriangles(){
        final List<Triangle> triangles = new ArrayList<>(finished.size());
        for (int i = 0, n = finished.size(); i < n; i++) {
            final WTriangle t = finished.get(i);
            final TupleArray positions = TupleArrays.of(Arrays.asList(t.p0, t.p1, t.p2, t.p0), t.p0.getSampleSystem(), t.p0.getDataType());
            final PointSequence points = GeometryFactory.createSequence(positions);
            final LinearRing exterior = GeometryFactory.createLinearRing(points);
            triangles.add(GeometryFactory.createTriangle(exterior));
        }
        return triangles;
    }

    /**
     * Loop on all edges.
     * Caution : use with care, may break the building process.
     * @param consumer to be called on each edge.
     */
    public void forEachEdge(Consumer<Edge> consumer) {
        for (WTriangle triangle : finished) {
            consumer.accept(triangle.e0);
            consumer.accept(triangle.e1);
            consumer.accept(triangle.e2);
        }
    }

    /**
     * Insert new points in the TIN.
     *
     * @param pts points to insert
     * @param decimate true to apply decimation based on error calculator, false to force insertion.
     * @throws ProcessException
     */
    public void add(List<Tuple> pts, boolean decimate) throws OperationException {
        decimateInsert = decimate;

        //remove any duplicated points
        final Set<Point2D.Double> set = new HashSet<>(pts.size());
        final List<Tuple> uniques = new ArrayList<>();
        for (Tuple t : pts) {
            if (!CRS.equivalent(crs, t.getCoordinateReferenceSystem())) {
                throw new OperationException("Points must have the same crs avec corner points");
            }
            if (set.add(new Point2D.Double(t.get(0), t.get(1)))) {
                uniques.add(t);
            } else {
                LOGGER.log(Level.FINE, "Point {0} already defined in add list, point will be ignored", t);
            }
        }

        //assign points into each triangle
        assign(uniques, finished);

        workStackTriangles.addAll(finished);
        finished.clear();

        //solve triangles
        Edge edge;
        WTriangle triangle;
        while (!workStackEdges.isEmpty() || !workStackTriangles.isEmpty()) {
            //solve delaunay edges
            for (edge = workStackEdges.poll(); edge != null; edge = workStackEdges.poll()) {
                //delaunay simplification test
                if (!edge.isObsolete()) {
                    testDelaunay(edge);
                }
            }

            triangle = workStackTriangles.poll();
            if (triangle != null && !triangle.isObsolete()) {
                solve(triangle);
            }
        }

        finished.addAll(workStackTriangles);
        workStackTriangles.clear();
        workStackEdges.clear();
        //remove all points we didn't use
        for (WTriangle t : finished) {
            t.clearCandidates();
        }

    }

    private void assign(List<Tuple> pts, Collection<WTriangle> triangles) throws OperationException {
        ptLoop:
        for (int i = pts.size() - 1; i >= 0; i--) {
            final Tuple pt = pts.get(i);
            if (pt == null) continue;

            double ptx = pt.get(0);
            double pty = pt.get(1);

            for (WTriangle t : triangles) {

                if (   (t.p0.get(0) == ptx && t.p0.get(1) == pty)
                    || (t.p1.get(0) == ptx && t.p1.get(1) == pty)
                    || (t.p2.get(0) == ptx && t.p2.get(1) == pty)
                    ){
                    LOGGER.log(Level.FINE, "Point {0} already exist in TIN, point will be ignored", pt);
                    pts.set(i, null);
                    continue ptLoop;
                }

                if (t.contains(pt)) {
                    t.addCandidate(pt);
                    pts.set(i, null);
                    continue ptLoop;
                }
            }

            LOGGER.log(Level.INFO, "Point {0} not in any triangle", pt);
        }
    }

    private void solve(WTriangle triangle) throws OperationException {

        final Tuple maxPt = triangle.findMaxDistancePoint();
        if (maxPt == null) {
            finished.add(triangle);
            return;
        }

        if (decimateInsert) {
            final double error = errorCalculator.apply(maxPt, triangle);
            if (error < delta) {
                finished.add(triangle);
                return;
            }
        }

        //remove point from list
        triangle.removeCandidate(maxPt);
        vertices.add(maxPt);

        if (triangle.e0.isOnEdge(maxPt)) {
            splitOnEdge(triangle.e0, maxPt);
        } else if (triangle.e1.isOnEdge(maxPt)) {
            splitOnEdge(triangle.e1, maxPt);
        } else if (triangle.e2.isOnEdge(maxPt)) {
            splitOnEdge(triangle.e2, maxPt);
        } else {
            splitCenter(triangle, maxPt);
        }
    }

    private void splitCenter(WTriangle t, Tuple c) throws OperationException {

        /*
            p0
            +__
            |\ \__ a0
            |e0   \__
            |  \     \
         a2 |  c+-e1--+ p1
            |  /   __/
            |e2 __/
            |/_/   a1
            +
            p2
        */

        final Tuple p0 = t.p0;
        final Tuple p1 = t.p1;
        final Tuple p2 = t.p2;
        final Edge a0 = t.getEdge(p0, p1);
        final Edge a1 = t.getEdge(p1, p2);
        final Edge a2 = t.getEdge(p2, p0);
        //create the 3 new edges
        final Edge e0 = new Edge(p0, c);
        final Edge e1 = new Edge(p1, c);
        final Edge e2 = new Edge(p2, c);
        //create the 3 new triangles
        final WTriangle t0 = new WTriangle(a0, e1, e0, errorCalculator);
        final WTriangle t1 = new WTriangle(a1, e2, e1, errorCalculator);
        final WTriangle t2 = new WTriangle(a2, e0, e2, errorCalculator);
        //assign edge triangles
        a0.change(t, t0);
        a1.change(t, t1);
        a2.change(t, t2);
        e0.t0 = t0; e0.t1 = t2;
        e1.t0 = t1; e1.t1 = t0;
        e2.t0 = t2; e2.t1 = t1;

        //reassign points in new triangles
        t.reassign(t0, t1, t2);
        workStackTriangles.add(t0);
        workStackTriangles.add(t1);
        workStackTriangles.add(t2);
        assert t0.validate();
        assert t1.validate();
        assert t2.validate();

        //ensure we have delaunay triangles
        workStackEdges.addFirst(a0);
        workStackEdges.addFirst(a1);
        workStackEdges.addFirst(a2);
    }

    private void splitOnEdge(Edge edge, Tuple pt) throws OperationException {
        WTriangle t0 = edge.t0;
        WTriangle t1 = edge.t1;
        if (t0 == null) {
            t0 = t1;
            t1 = null;
            finished.remove(t0);
            workStackTriangles.remove(t0);
        } else {
            finished.remove(t0);
            finished.remove(t1);
            workStackTriangles.remove(t0);
            workStackTriangles.remove(t1);
        }

        /*
                +
               /|\
           a0 / | \ a1
             /  e2 \
            /   |   \
        p0 +----+----+ p1
            e0    e1

        */

        //split edge in two
        Edge e0 = new Edge(edge.p0, pt);
        Edge e1 = new Edge(edge.p1, pt);
        //inherit constraint if parent is.
        e0.setConstraint(edge.isConstraint());
        e1.setConstraint(edge.isConstraint());
        Edge e2 = new Edge(t0.oppositePoint(edge), pt);
        t0.obsolete();
        //find opposite edges
        Edge a0 = null;
        Edge a1 = null;
        if (t0.e0 != edge) {
            if (t0.e0.hasPoint(edge.p0)) a0 = t0.e0;
            if (t0.e0.hasPoint(edge.p1)) a1 = t0.e0;
        }
        if (t0.e1 != edge) {
            if (t0.e1.hasPoint(edge.p0)) a0 = t0.e1;
            if (t0.e1.hasPoint(edge.p1)) a1 = t0.e1;
        }
        if (t0.e2 != edge) {
            if (t0.e2.hasPoint(edge.p0)) a0 = t0.e2;
            if (t0.e2.hasPoint(edge.p1)) a1 = t0.e2;
        }
        final WTriangle t0_0 = new WTriangle(e0, a0, e2, errorCalculator);
        final WTriangle t0_1 = new WTriangle(e1, a1, e2, errorCalculator);
        //affect triangles to edges
        e0.t0 = t0_0; e0.t1 = t1;
        e1.t0 = t0_1; e1.t1 = t1;
        e2.t0 = t0_0; e2.t1 = t0_1;
        a0.change(t0, t0_0);
        a1.change(t0, t0_1);

        //reassign points
        workStackTriangles.add(t0_0);
        workStackTriangles.add(t0_1);
        t0.reassign(t0_0, t0_1);
        assert t0_0.validate();
        assert t0_1.validate();
        workStackEdges.addFirst(a0);
        workStackEdges.addFirst(a1);

        if (t1 != null) {

            e2 = new Edge(t1.oppositePoint(edge), pt);
            t1.obsolete();
            //find opposite edges
            a0 = null;
            a1 = null;
            if (t1.e0 != edge) {
                if (t1.e0.hasPoint(edge.p0)) a0 = t1.e0;
                if (t1.e0.hasPoint(edge.p1)) a1 = t1.e0;
            }
            if (t1.e1 != edge) {
                if (t1.e1.hasPoint(edge.p0)) a0 = t1.e1;
                if (t1.e1.hasPoint(edge.p1)) a1 = t1.e1;
            }
            if (t1.e2 != edge) {
                if (t1.e2.hasPoint(edge.p0)) a0 = t1.e2;
                if (t1.e2.hasPoint(edge.p1)) a1 = t1.e2;
            }

            final WTriangle t1_0 = new WTriangle(e0, a0, e2, errorCalculator);
            final WTriangle t1_1 = new WTriangle(e1, a1, e2, errorCalculator);
            //affect triangles to edges
            e0.change(t1, t1_0);
            e1.change(t1, t1_1);
            e2.t0 = t1_0; e2.t1 = t1_1;
            a0.change(t1, t1_0);
            a1.change(t1, t1_1);

            //reassign points
            workStackTriangles.add(t1_0);
            workStackTriangles.add(t1_1);
            t1.reassign(t1_0, t1_1);
            assert t1_0.validate();
            assert t1_1.validate();
            workStackEdges.addFirst(a0);
            workStackEdges.addFirst(a1);
        }
        //ensure edge wont be used anymore
        edge.makeObsolete();
    }

    /**
     * Ensure the 2 triangles on each side of the edge are delaunay triangles.
     * if it's not the case the edge will be swapped.
     *
     * @param e0
     * @param pt
     */
    private void testDelaunay(Edge e0) throws OperationException {
        if (e0.isConstraint()) {
            //edge is a constraint, it can not be removed
            return;
        }
        WTriangle t0 = e0.t0;
        WTriangle t1 = e0.t1;
        if (t0 == null || t1 == null) return;

        /*
                    p1
                    +
                   /|\
            t0    / | \    t1
                 /  |  \
             p0 +   e0  + p2
                 \  |  /
                  \ | /
                   \|/
                    +
                    p3
        */
        final Tuple p0 = t0.oppositePoint(e0);
        final Tuple p1 = e0.p0;
        final Tuple p2 = t1.oppositePoint(e0);
        final Tuple p3 = e0.p1;

        if (Maths.inCircle(t0.p0, t0.p1, t0.p2, p2)) {
            /* swap edge

                    p1
                    +
                   / \
            t2   a0   a1
                 /     \
             p0 +---e1--+ p2
                 \     /
            t3   a3   a2
                   \ /
                    +
                    p3
            */

            //create new triangles
            final Edge a0 = t0.getEdge(p0, p1);
            final Edge a1 = t1.getEdge(p1, p2);
            final Edge a2 = t1.getEdge(p2, p3);
            final Edge a3 = t0.getEdge(p3, p0);
            e0.makeObsolete();
            t0.obsolete();
            t1.obsolete();
            final Edge e1 = new Edge(p0, p2);
            final WTriangle t2 = new WTriangle(e1, a0, a1, errorCalculator);
            final WTriangle t3 = new WTriangle(e1, a2, a3, errorCalculator);
            e1.t0 = t2;
            e1.t1 = t3;
            a0.change(t0, t2);
            a1.change(t1, t2);
            a2.change(t1, t3);
            a3.change(t0, t3);
            //reassign points
            t0.reassign(t2, t3);
            t1.reassign(t2, t3);

            //remove old and add new triangles in the stack
            workStackTriangles.remove(t0);
            workStackTriangles.remove(t1);
            finished.remove(t0);
            finished.remove(t1);
            workStackTriangles.add(t2);
            workStackTriangles.add(t3);

            assert t2.validate();
            assert t3.validate();

            workStackEdges.addFirst(a0);
            workStackEdges.addFirst(a1);
            workStackEdges.addFirst(a2);
            workStackEdges.addFirst(a3);
        }
    }

}
