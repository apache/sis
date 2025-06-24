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
package org.apache.sis.geometries.triangulate.delaunay;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.geometries.MeshPrimitive;
import org.apache.sis.geometries.MeshPrimitive.Vertex;
import org.apache.sis.geometries.index.KdTree;
import org.apache.sis.geometries.math.Maths;
import org.apache.sis.geometries.math.Tuple;
import org.apache.sis.geometries.math.TupleArray;
import org.apache.sis.geometries.math.TupleArrays;
import org.apache.sis.geometries.math.Vector;
import org.apache.sis.geometries.math.Vectors;
import org.apache.sis.geometries.operation.OperationException;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.util.ArgumentChecks;
import org.opengis.geometry.Envelope;

/**
 * Delaunay triangulation algorithm.
 *
 * https://en.wikipedia.org/wiki/Delaunay_triangulation
 * https://en.wikipedia.org/wiki/Constrained_Delaunay_triangulation
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Delaunay {

    private static final Logger LOGGER = Logger.getLogger("org.apache.sis.geometries");

    private MeshPrimitive.Points points;

    private MeshPrimitive pointsPlusRoot;
    private int rootOffset;

    /**
     * Contains edges which must be tested with delaunay circle.
     */
    private final ArrayDeque<OrientedEdge> triangulationStackEdges = new ArrayDeque<>();

    private final AtomicInteger crossingConstraints = new AtomicInteger();
    /*
    In some incorrect dataset we may have two constraints crossing each other.
    This would result in an infinite loop, to avoid it we do not allow a constraint
    edge to be processed more then once.
    */
    private final ArrayDeque<OrientedEdge> constraintStackEdges = new ArrayDeque<>() {
        private final HashSet<Long> unique = new HashSet<>();
        @Override
        public OrientedEdge poll() {
            OrientedEdge e = super.poll();
            //do not remove the edge from the set until the end of the constraint operation
            return e;
        }

        @Override
        public void addFirst(OrientedEdge e) {
            //create a key not affected by edge orientation
            int idx0 = e.getStart().getIndex();
            int idx1 = e.getEnd().getIndex();
            if (idx0 > idx1) {
                int i = idx0;
                idx0 = idx1;
                idx1 = i;
            }
            final long key = idx0 + ((long) idx1) << 32l;
            if (unique.add(key)) {
                super.addFirst(e);
            } else {
                crossingConstraints.incrementAndGet();
            }
        }

        @Override
        public void clear() {
            super.clear();
            unique.clear();
        }
    };

    /*
    Index the vertex to find closest point faster.
    */
    private final KdTree<Integer> tree = new KdTree();
    private OrientedEdge[] indexToEdge;

    private final Set<OrientedTriangle> triangles = new HashSet<>() {
        @Override
        public boolean add(OrientedTriangle triangle) {
            if (Maths.lineSide(triangle.a.getPosition(), triangle.b.getPosition(), triangle.c.getPosition()) == 0) {
                //flat triangle
                LOGGER.log(Level.FINE, "Delaunay triangulation generated a flat triangle, this may be normal on edges but may still cause issues later on.");
            }
            final boolean v = super.add(triangle);
            if (v) {
                indexToEdge[triangle.a.getIndex()] = triangle.ab;
                indexToEdge[triangle.b.getIndex()] = triangle.bc;
                indexToEdge[triangle.c.getIndex()] = triangle.ca;
            }
            return v;
        }
    };

    /**
     * Prepare a delaunay triangulation.
     */
    public Delaunay() {
    }

    /**
     * Create delaunay triangulation.
     * This method should be called only once.
     * @param points not null
     * @throws IllegalArgumentException if an index is incorrect
     * @throws OperationException if an algorithm exception occurs
     */
    public void build(MeshPrimitive.Points points) throws IllegalArgumentException, OperationException {
        ArgumentChecks.ensureNonNull("points", points);
        this.points = points;
        delaunayTriangulation();
    }

    /**
     * Insert a new constraint.
     *
     * @param startIndex start edge vertex index
     * @param endIndex end edge vertex index
     * @param hard true to create edge which should never be cut or changed again.
     *        false for soft constraint which may be removed later.
     * @throws IllegalArgumentException if an index is incorrect
     * @throws OperationException if an algorithm exception occurs
     */
    public void pushConstraint(int startIndex, int endIndex, boolean hard) throws IllegalArgumentException, OperationException {
        insertEdge(startIndex, endIndex, hard);
    }

    /**
     * Insert new constraints.
     *
     * @param constraints lines index, not null.
     * @param hard true to create edge which should never be cut or changed again.
     *        false for soft constraint which may be removed later.
     * @throws IllegalArgumentException if an index is incorrect
     * @throws OperationException if an algorithm exception occurs
     */
    public void pushConstraint(TupleArray constraints, boolean hard) throws IllegalArgumentException, OperationException {
        ArgumentChecks.ensureNonNull("constraints", constraints);

        final int[] index = constraints.toArrayInt();
        for (int i = 0; i < index.length; i += 2) {
            try {
                insertEdge(index[i], index[i+1], hard);
            } catch (StackOverflowError ex) {
                //TODO : fix it, hard to solve, catch it for now
                LOGGER.log(Level.FINE, "stack overflow {0} {1}", new Object[]{index[i], index[i+1]});
            } catch (IllegalStateException ex) {
                //TODO : fix it, hard to solve, catch it for now
                LOGGER.log(Level.FINE, "flat triangle {0} {1}", new Object[]{index[i], index[i+1]});
            }

            //solve any constraint edge we had to break
            for (OrientedEdge edge = constraintStackEdges.poll(); edge != null; edge = constraintStackEdges.poll()) {
                insertEdge(edge.getStart().getIndex(), edge.getEnd().getIndex(), true);
            }
        }
    }

    /**
     * @return index of the triangles.
     * @throws IllegalStateException if used before build method is called
     */
    public TupleArray getTrianglesIndex() throws IllegalStateException {
        if (triangles.isEmpty()) throw new IllegalStateException("Triangles list is empty, build method must be called first");

        int[] index = new int[(triangles.size()-3) * 3];
        int i = 0;
        for (OrientedTriangle t : triangles) {
            final int i0 = t.a.getIndex();
            final int i1 = t.b.getIndex();
            final int i2 = t.c.getIndex();
            if (!(isRootVertex(i0) || isRootVertex(i1) || isRootVertex(i2))) {
                index[i++] = i0;
                index[i++] = i1;
                index[i++] = i2;
            }
        }

        index = Arrays.copyOf(index, i);
        return TupleArrays.ofUnsigned(1, index);
    }

    /**
     * @return triangles as a primitive.
     * @throws IllegalStateException if used before build method is called
     */
    public MeshPrimitive.Triangles getTriangles() throws IllegalStateException {
        final MeshPrimitive.Triangles primitive = new MeshPrimitive.Triangles();
        primitive.setPositions(points.getPositions());
        primitive.setIndex(getTrianglesIndex());
        return primitive;
    }

    /**
     * Create the delaunay triangulation without constraints.
     * @throws IllegalArgumentException if an index is incorrect
     * @throws OperationException if an algorithm exception occurs
     */
    private void delaunayTriangulation() throws OperationException, IllegalArgumentException {
        triangles.clear();

        // INITIALIZE //////////////////////////////////////////////////////////
        /*
        Build the root triangle, contains all points
        Any triangle using one of the root triangle points will not be returned.

        C
        ■
        | ⟍
        |    ⟍
        | ■----■
        | |    | ⟍
        | ■----■    ⟍
        ■-------------■
        A             B

        Add an extra margin to the bounding box to reduce mathematical errors.
        */
        final TupleArray positions = this.points.getPositions();
        final Envelope envelope = buffer(points.getEnvelope(), 10.0);
        final double envelopeMinX = envelope.getMinimum(0);
        final double envelopeMinY = envelope.getMinimum(1);
        final Vector<?> A = Vectors.create(positions.getSampleSystem(), positions.getDataType());
        A.set(0, envelopeMinX);
        A.set(1, envelopeMinY);
        final Vector<?> B = A.copy();
        B.set(0, envelopeMinX + envelope.getSpan(0) * 2);
        B.set(1, envelopeMinY);
        final Vector<?> C = A.copy();
        C.set(0, envelopeMinX);
        C.set(1, envelopeMinY + envelope.getSpan(1) * 2);
        rootOffset = positions.getLength();
        final TupleArray extraPositions = positions.resize(3);
        extraPositions.set(0, A);
        extraPositions.set(1, B);
        extraPositions.set(2, C);
        final TupleArray positionsPlusRoot = TupleArrays.concatenate(positions, extraPositions);

        pointsPlusRoot = new MeshPrimitive.Points();
        pointsPlusRoot.setPositions(positionsPlusRoot);

        //prepare index
        indexToEdge = new OrientedEdge[positionsPlusRoot.getLength()];

        //base triangle
        final Vertex v0 = new Vertex(pointsPlusRoot, rootOffset);
        final Vertex v1 = new Vertex(pointsPlusRoot, rootOffset+1);
        final Vertex v2 = new Vertex(pointsPlusRoot, rootOffset+2);
        final OrientedTriangle triangle = new OrientedTriangle(v0, v1, v2);
        triangles.add(triangle);

        tree.insert(v0.getPosition(),v0.getIndex());
        tree.insert(v1.getPosition(),v1.getIndex());
        tree.insert(v2.getPosition(),v2.getIndex());


        // TRIANGULATE /////////////////////////////////////////////////////////
        final TupleArray index = points.getIndex();
        if (index == null) {
            //use all points
            for (int idx = 0, n = positions.getLength(); idx < n; idx++) {
                insertPoint(idx);
            }
        } else {
            //user defined points
            for (int idx : index.toArrayInt()) {
                insertPoint(idx);
            }
        }
    }

    /**
     * Insert one point in the TIN.
     * @param index vertex index.
     * @throws IllegalArgumentException if index is incorrect
     * @throws OperationException if an algorithm exception occurs
     */
    private void insertPoint(int index) throws OperationException, IllegalArgumentException {
        if (index < 0 || index >= rootOffset) throw new IllegalArgumentException("Index do not exist : " + index);
        final Vertex vertex = new Vertex(pointsPlusRoot, index);
        final Tuple position = vertex.getPosition();

        //find nearest vertex and tst all triangles using this vertex
        final int nearestVertex = tree.nearest(position).getValue();
        solve(nearestVertex, vertex);

        tree.insertNoCopy(vertex.getPosition(),vertex.getIndex());
        //solve delaunay edges
        for (OrientedEdge edge = triangulationStackEdges.poll(); edge != null; edge = triangulationStackEdges.poll()) {
            //delaunay simplification test
            if (!edge.isObsolete()) {
                testDelaunay(edge);
            }
        }
    }

    private void solve(int nearestVertex, Vertex vertex) {
        OrientedEdge edge = indexToEdge[nearestVertex];

        for (;;) {
            OrientedTriangle triangle = edge.getTriangle();
            if (solve(triangle, vertex)) return;

            //search edge where point is on it's right side
            if (edge.getSide(vertex) < 0) {
                edge = edge.reverse();
                continue;
            }
            //NOTE : we must try the adjacent edge first, otherwise we may end in an infinite loop
            //try adjacent edge
            final OrientedEdge adjacent = triangle.adjacentEdge(edge);
            if (adjacent.getSide(vertex) < 0) {
                edge = adjacent.reverse();
                continue;
            }
            //must be the opposite edge
            OrientedEdge opposite = triangle.getEdge(edge.getEnd());
            if (opposite.getSide(vertex) < 0) {
                edge = opposite.reverse();
                continue;
            }

            throw new OperationException("Algorithm flow, no triangle found for a vertex");
        }
    }

    /**
     *
     * @throws OperationException if an algorithm exception occurs
     */
    private boolean solve(OrientedTriangle t, Vertex v) throws OperationException {
        if (t.ab.isOnEdge(v)) {
            splitOnEdge(t.ab, v);
            return true;
        } else if (t.bc.isOnEdge(v)) {
            splitOnEdge(t.bc, v);
            return true;
        } else if (t.ca.isOnEdge(v)) {
            splitOnEdge(t.ca, v);
            return true;
        } else if (Maths.isPointInTriangle_SideAlgo(t.a.getPosition(), t.b.getPosition(), t.c.getPosition(), v.getPosition())) {
            splitCenter(t, v);
            return true;
        }
        return false;
    }

    /**
     * Enforce a constraint segment
     * @param startIndex segment start vertex index.
     * @param endIndex segment end vertex index.
     * @param isContraint true is created or existing edge should be marked as constraint.
     * @return The created or existing constraint edge.
     *         The edge end if guarantee to be the vertex with given end index.
     *         The edge start may be the starting vertex or the closest point
     *         to the end vertex which is on the edge line.
     * @throws OperationException if an algorithm exception occurs
     * @throws IllegalArgumentException if edge indexes are incorrect
     */
    private OrientedEdge insertEdge(int startIndex, int endIndex, boolean isContraint) throws OperationException, IllegalArgumentException {
        if (startIndex < 0 || startIndex >= rootOffset) throw new IllegalArgumentException("Constraint segment index do not exist : " + startIndex);
        if (endIndex < 0 || endIndex >= rootOffset) throw new IllegalArgumentException("Constraint segment index do not exist : " + endIndex);
//        System.out.println("e " + startIndex +" " + endIndex);

        final Vertex E = new Vertex(pointsPlusRoot, endIndex);

        //find the starting triangle
        /*
                    A
                    ■
                   / \
                  /   \
                 /     \
              B ■⎻⎻⎻⎻⎻⎻⎻■ C
                 \     /
                  \   /
                   \ /
                    ■
                    D

        */
        final OrientedEdge A_B = searchTriangle(startIndex, endIndex);
        final OrientedEdge X_E = insertEdge(A_B, E, isContraint);
        return X_E;
    }

    /**
     * Enforce a constraint segment
     *
     * @param startIndex segment start vertex index.
     * @param Z segment end vertex index.
     * @return The created or existing constraint edge.
     *         The edge end if guarantee to be the vertex with given end index.
     *         The edge start may be the starting vertex or the closest point
     *         to the end vertex which is on the edge line.
     * @throws OperationException if an algorithm exception occurs
     */
    private OrientedEdge insertEdge(OrientedEdge A_B, Vertex Z, boolean isContraint) throws OperationException {
//        System.out.println("ee " + A_B.getTriangle().asTextPolygon() +" LINESTRING(" + A_B.getStart().getPosition().get(0) +" "+  A_B.getStart().getPosition().get(1) + ", " + Z.getPosition().get(0) +" "+ Z.getPosition().get(1) + ")");

        //find the starting triangle
        /*
                    A
                    ■
                  _/ \_
              🡷 _/     \_ 🡴
               /    🡲    \
            B ■⎻⎻⎻⎻⎻⎻⎻⎻⎻⎻⎻■ C
               \_   🡰   _/
               🡶 \_   _/ 🡵
                   \ /
                    ■
                    D
        */

        { //special case : if this is a perfect match
            if (A_B.getEnd().getIndex() == Z.getIndex()) {
                //mark it as a constraint
                if (isContraint) A_B.setConstraint(true);
                return A_B;
            }
        }

        { //special case : if edge is colinear to constraint segment
            final Tuple endPosition = Z.getPosition();
            final double leftSide = Maths.lineSide(A_B.getStart().getPosition(), A_B.getEnd().getPosition(), endPosition);
            if (leftSide == 0) {
                //on the edge, mark the edge as a constraint and start a new constraint from edge end
                if (isContraint) A_B.setConstraint(true);
                return insertEdge(A_B.getEnd().getIndex(), Z.getIndex(), isContraint);
            } else if (leftSide < 0) {
                throw new OperationException("constraint end must be on the left side at this point, algorithm flaw");
            }
        }

        //intersects the triangle
        //use the flipscan algorithm to process this case
        //see https://github.com/jhasse/poly2tri/blob/master/doc/FlipScan.png
        final Vertex A = A_B.getStart();
        final Vertex B = A_B.getEnd();
        final Vertex C = A_B.getTriangle().oppositePoint(A_B);
        final OrientedEdge B_C = A_B.getTriangle().getEdge(B);
        final OrientedEdge C_B = B_C.reverse();
        final OrientedTriangle triangle = C_B.getTriangle();
        final Vertex D = triangle.oppositePoint(C_B);

        if (D.getIndex() == Z.getIndex()) {
            /*
            Case if neigbhor triangle is the last one.
            Last flip operation
                    A
                    ■
                  _/|\_
              🡷 _/  |  \_ 🡴
               /    |    \
            B ■    🡱|🡳    ■ C
               \_   |   _/
               🡶 \_ | _/ 🡵
                   \|/
                    ■
                    D = Z
            */
            final OrientedEdge A_D = swapTriangles(B_C, false, true);
            if (isContraint) A_D.setConstraint(true);
            return A_D;
        }

        //check if opposite triangle point is in the valid angle
        if (inScanArea(A, B, C, D)) {
            //flip triangles and continue to next one
            final OrientedEdge A_D = swapTriangles(B_C, false, true);
            if (Maths.lineSide(A.getPosition(), D.getPosition(), Z.getPosition()) >= 0) {
                /* continue with ADC triangle
                            A                               A
                            ■                               ■
                          _/|\_                           _/|\_
                        _/  | \\_                       _/  |  \_
                       /    |  \ \                     /    |    \
                    B ■     |   \ ■ C       OR      B ■     |     ■ C
                       \_   |   _\                     \_   |   _/
                         \_ | _/  \                      \_ | _/
                           \|/     \                       \|/
                            ■       \                       ■ D
                            D        ■                      |
                                     Z                      ■ Z
                */
                return insertEdge(A_D, Z, isContraint);
            } else {
                /* continue with ABD triangle
                            A
                            ■
                          _/|\_
                        _// |  \_
                       / /  |    \
                    B ■ /   |     ■ C
                       /_   |   _/
                      /  \_ | _/
                     /     \|/
                    /       ■
                   ■        D
                   E
                */
                return insertEdge(A_B, Z, isContraint);
            }
        } else {
            /*
            Scan for next point in area

                            A
                            ■
                          _/ \_
                        _/     \_
                       /         \
                    B ■⎻⎻⎻⎻⎻⎻⎻⎻⎻⎻⎻■ C
                   __/    _______/
                  /______/
               D ■

            Look for the new opposing point inside the scan area, then take this point
            and form a new temporary edge to flip along.
            */
            final Vertex E = scanNextValid(A_B, Z);
            final OrientedEdge X_A = insertEdge(E.getIndex(), A.getIndex(), false);
            if (X_A.getStart().getIndex() == Z.getIndex()) {
                //exact match
                return X_A.reverse();
            }
            final double side = Maths.lineSide(X_A.getStart().getPosition(), A.getPosition(), Z.getPosition());
            if (side > 0) {
                return insertEdge(X_A.getTriangle().getEdge(A), Z, isContraint);
            } else {
                return insertEdge(X_A.reverse(), Z, isContraint);
            }
        }

    }

    /**
     * @throws OperationException if an algorithm exception occurs
     */
    private Vertex scanNextValid(OrientedEdge A_B, Vertex Z) throws OperationException {
        final OrientedTriangle ABC = A_B.getTriangle();
        final Vertex A = A_B.getStart();
        final Vertex B = A_B.getEnd();
        final Vertex C = ABC.oppositePoint(A_B);
        final OrientedEdge B_C = ABC.getEdge(B);

        OrientedEdge V_T = B_C.reverse();
        for (;;) {
            final OrientedTriangle triangle = V_T.getTriangle();
            final Vertex T = V_T.getEnd();
            final Vertex U = triangle.oppositePoint(V_T);

            if (U.getIndex() == Z.getIndex() || inScanArea(A, B, C, U)) {
                return U;
            } else {
                //find the edge with intersect A_Z
                if (Maths.lineSide(A.getPosition(), Z.getPosition(), U.getPosition()) >= 0) {
                    /*
                                A
                                ■
                               / 🡰
                        T ■⎻⎻⎻/⎻⎻⎻⎻⎻⎻⎻■ V
                           \_/      _/
                           🡶/\_   _/ 🡵
                           /   \ /
                          /     ■
                         ■      U
                         Z
                    */
                    V_T = triangle.getEdge(T).reverse();
                } else {
                    /*
                                  A
                                  ■
                                🡰  \
                        T ■⎻⎻⎻⎻⎻⎻⎻⎻⎻\⎻■ V
                           \_       _\
                           🡶 \_   _/ 🡵\
                               \ /     \
                                ■       \
                                U        ■
                                         Z
                    */
                    V_T = triangle.getEdge(U).reverse();
                }
            }
        }

    }

    /**
     * @param index vertex index
     * @return true if vertex index is a root vertex
     */
    private boolean isRootVertex(int index) {
        return index >= rootOffset;
    }

    private static Envelope buffer(Envelope env, double margin) {
        final GeneralEnvelope buffer = new GeneralEnvelope(env);
        final int dimension = buffer.getDimension();
        for (int i = 0; i < dimension; i++) {
            buffer.setRange(i, buffer.getMinimum(i) - margin,  buffer.getMaximum(i) + margin);
        }
        return buffer;
    }

    /**
     * Divide triangle in three using given vertex as center.
     *
     * <pre>{@code
     *       A
     *       ■__
     *       |\ \__
     *       | \   \__🡴
     *       | 🡴\🡶    \__
     *       |   \    🡲  \
     *     🡳 |  X ■ ⎻⎻⎻⎻⎻⎻■ C
     *       |   /    🡰__/
     *       | 🡵/🡷  __/
     *       | / __/ 🡵
     *       |/_/
     *       ■
     *       B
     * }</pre>
     *
     * @param ABC triangle, not null
     * @param X splitting center, not null
     * @throws OperationException if an algorithm exception occurs
     */
    private void splitCenter(OrientedTriangle ABC, Vertex X) throws OperationException {

        final Vertex A = ABC.a;
        final Vertex B = ABC.b;
        final Vertex C = ABC.c;
        final OrientedEdge A_B = ABC.getEdge(A);
        final OrientedEdge B_C = ABC.getEdge(B);
        final OrientedEdge C_A = ABC.getEdge(C);
        //create the three new triangles
        final OrientedEdge A_X = new OrientedEdge(A, X);
        final OrientedEdge C_X = new OrientedEdge(C, X);
        final OrientedEdge B_X = new OrientedEdge(B, X);
        final OrientedEdge X_A = A_X.reverse(true);
        final OrientedEdge X_B = B_X.reverse(true);
        final OrientedEdge X_C = C_X.reverse(true);
        final OrientedTriangle ABX = new OrientedTriangle(A_B, B_X, X_A);
        final OrientedTriangle BCX = new OrientedTriangle(B_C, C_X, X_B);
        final OrientedTriangle CAX = new OrientedTriangle(C_A, A_X, X_C);
        assert ABX.validate();
        assert BCX.validate();
        assert CAX.validate();

        triangles.remove(ABC);
        triangles.add(ABX);
        triangles.add(BCX);
        triangles.add(CAX);

        //ensure we have delaunay triangles
        triangulationStackEdges.addFirst(A_B);
        triangulationStackEdges.addFirst(B_C);
        triangulationStackEdges.addFirst(C_A);
    }

    /**
     *
     * Before
     * <pre>{@code
     *           C
     *           ■
     *          / \
     *         /   \
     *      🡷 /     \ 🡴
     *       /   🡲   \
     *    A ■⎻⎻⎻⎻⎻⎻⎻⎻⎻■ B
     *       \   🡰   /
     *      🡶 \     / 🡵
     *         \   /
     *          \ /
     *           ■
     *           D
     * }</pre>
     *
     * After
     * <pre>{@code
     *           C
     *           ■
     *          /|\
     *         / | \
     *      🡷 / 🡱|🡳 \ 🡴
     *       / 🡲 | 🡲 \
     *    A ■⎻⎻⎻⎻■X⎻⎻⎻■ B
     *       \ 🡰 |🡰 /
     *      🡶 \  |  / 🡵
     *         \ | /
     *          \|/
     *           ■
     *           D
     * }</pre>
     *
     * @param A_B edge to split not null
     * @param X splitting point on the edge not null
     * @throws OperationException if an algorithm exception occurs
     */
    private void splitOnEdge(OrientedEdge A_B, Vertex X) throws OperationException {
        final OrientedTriangle ABC = A_B.getTriangle();
        final Vertex A = A_B.getStart();
        final Vertex B = A_B.getEnd();
        final Vertex C = ABC.oppositePoint(A_B);
        final OrientedEdge B_C = ABC.getEdge(B);
        final OrientedEdge C_A = ABC.getEdge(C);
        /*
        */

        //split edge in two
        final OrientedEdge A_X = new OrientedEdge(A, X);
        final OrientedEdge X_B = new OrientedEdge(X, B);
        final OrientedEdge C_X = new OrientedEdge(C, X);
        final OrientedEdge X_C = C_X.reverse(true);
        //inherit constraint from parent edge
        A_X.setConstraint(A_B.isConstraint());
        X_B.setConstraint(A_B.isConstraint());
        //create the two new triangles
        final OrientedTriangle AXC = new OrientedTriangle(A_X, X_C, C_A);
        final OrientedTriangle XBC = new OrientedTriangle(X_B, B_C, C_X);
        assert AXC.validate();
        assert XBC.validate();

        ABC.obsolete();
        triangles.remove(ABC);
        triangles.add(AXC);
        triangles.add(XBC);

        triangulationStackEdges.addFirst(B_C);
        triangulationStackEdges.addFirst(C_A);

        final OrientedEdge B_A = A_B.reverse();
        if (B_A != null) {
            final OrientedTriangle BAD = B_A.getTriangle();
            final Vertex D = BAD.oppositePoint(B_A);
            final OrientedEdge A_D = BAD.getEdge(A);
            final OrientedEdge D_B = BAD.getEdge(D);
            /*
                  🡰 X 🡰
             A ■⎻⎻⎻⎻■⎻⎻⎻⎻■ B
                \   |   /
               🡶 \ 🡱|🡳 / 🡵
                  \ | /
                   \|/
                    ■
                    D
            */
            final OrientedEdge D_X = new OrientedEdge(D, X);
            final OrientedEdge X_D = D_X.reverse(true);
            final OrientedEdge X_A = A_X.reverse(true);
            final OrientedEdge B_X = X_B.reverse(true);

            final OrientedTriangle XAD = new OrientedTriangle(X_A, A_D, D_X);
            final OrientedTriangle BXD = new OrientedTriangle(B_X, X_D, D_B);
            assert XAD.validate();
            assert BXD.validate();

            BAD.obsolete();
            triangles.remove(BAD);
            triangles.add(XAD);
            triangles.add(BXD);

            triangulationStackEdges.addFirst(A_D);
            triangulationStackEdges.addFirst(D_B);
        }
        //ensure edge wont be used anymore
        A_B.makeObsolete();
    }

    /**
     * Ensure the 2 triangles on each side of the edge are delaunay triangles.
     * if it's not the case the edge will be swapped.
     *
     * @param B_D
     * @param pt
     * @throws OperationException if an algorithm exception occurs
     */
    private void testDelaunay(OrientedEdge B_D) throws OperationException {
        if (B_D.isConstraint()) {
            //edge is a constraint, it can not be removed
            return;
        }
        final OrientedEdge D_B = B_D.reverse();
        if (D_B == null) {
            //single triangle
            return;
        }

        final OrientedTriangle BDA = B_D.getTriangle();
        final OrientedTriangle DBC = D_B.getTriangle();
        final Vertex C = DBC.oppositePoint(D_B);

        final double BDAradius = BDA.getDelaunayCircleRadiusSquare();
        final double BDADistance = BDA.distanceToDelaunayCircleCenterSquare(C.getPosition().get(0), C.getPosition().get(1));
        if (BDADistance < BDAradius) {
            /*
            In the Delaunay algorithm we may encounter the case where two triangles
            can be flipped infinitely. This happens when the four points of two adjacent
            triangles are contained in the two circle formed by each triangle.
            Ensure this is not an infinite loop where triangles are both valid.
            */
            final Vertex A = BDA.oppositePoint(B_D);
            final Vertex D = B_D.getEnd();
            final OrientedTriangle BCA = new OrientedTriangle(B_D.getStart(), C, A);
            if (!BCA.isInDelaunayCircle(D.getPosition().get(0), D.getPosition().get(1))) {
                swapTriangles(B_D, true, false);
            }
        }
    }

    /**
     * Flip the edge connecting two triangles.<p>
     *
     * Before
     * <pre>{@code
     *               D
     *               ■
     *              /|\
     *           🡷 / | \ 🡴
     *            /  |  \
     *         A ■  🡱|🡳  ■ C
     *            \  |  /
     *           🡶 \ | / 🡵
     *              \|/
     *               ■
     *               B
     * }</pre>
     *
     * After
     * <pre>{@code
     *               D
     *               ■
     *              / \
     *           🡷 /   \ 🡴
     *            /  🡲  \
     *         A ■⎻⎻⎻⎻⎻⎻⎻■ C
     *            \  🡰  /
     *           🡶 \   / 🡵
     *              \ /
     *               ■
     *               B
     * }</pre>
     *
     * @param cascade true to add candidate edges to the working list.
     * @param constraintAddMode true to force flipping even if edge is a constraint.
     *       if the edge was a constraint it is added in the list for futur reconstruction
     * @return the created edge, starting at the opposite point of given edge
     * @throws OperationException if edge can be swapped because it has a constraint set.
     */
    private OrientedEdge swapTriangles(OrientedEdge B_D, boolean cascade, boolean constraintAddMode) throws OperationException {
        if (B_D.isConstraint()) {
            if (constraintAddMode) {
                constraintStackEdges.addFirst(B_D);
            } else {
                throw new OperationException("Can not swap a constraint edge.");
            }
        }

        final OrientedEdge D_B = B_D.reverse();
        final OrientedTriangle BDA = B_D.getTriangle();
        final OrientedTriangle DBC = D_B.getTriangle();
        final Vertex A = BDA.oppositePoint(B_D);
        final Vertex B = B_D.getStart();
        final Vertex C = DBC.oppositePoint(D_B);
        final Vertex D = B_D.getEnd();

        /* swap edge

                D
                ■
               / \
            🡷 /   \ 🡴
             /  🡲  \
          A ■⎻⎻⎻⎻⎻⎻⎻■ C
             \  🡰  /
            🡶 \   / 🡵
               \ /
                ■
                B
        */

        //create new triangles
        final OrientedEdge D_A = BDA.getEdge(D);
        final OrientedEdge C_D = DBC.getEdge(C);
        final OrientedEdge B_C = DBC.getEdge(B);
        final OrientedEdge A_B = BDA.getEdge(A);
        final OrientedEdge A_C = new OrientedEdge(A, C);
        final OrientedEdge C_A = A_C.reverse(true);
        final OrientedTriangle ACD = new OrientedTriangle(A_C, C_D, D_A);
        final OrientedTriangle ABC = new OrientedTriangle(A_B, B_C, C_A);

        B_D.makeObsolete();
        BDA.obsolete();
        DBC.obsolete();

        //remove old and add new triangles in the stack
        triangles.remove(BDA);
        triangles.remove(DBC);
        triangles.add(ACD);
        triangles.add(ABC);

        assert ACD.validate();
        assert ABC.validate();

        if (cascade) {
            triangulationStackEdges.addFirst(D_A);
            triangulationStackEdges.addFirst(C_D);
            triangulationStackEdges.addFirst(B_C);
            triangulationStackEdges.addFirst(A_B);
        }

        return A_C;
    }

    /**
     * Find one triangle edge of the triangle intersecting the given segment.
     * @param index searched vertex index
     * @return triangle edge using this index, the edge may be colinear to the segment,
     * or the triangle may intersection the segment,
     * but the adjacent edge will not be colinear to the segment.
     * @throws OperationException if an algorithm exception occurs
     */
    private OrientedEdge searchTriangle(int startIndex, int endIndex) throws OperationException {
        final Vertex end = new Vertex(pointsPlusRoot, endIndex);
        final Tuple endPosition = end.getPosition();

        OrientedEdge edge = indexToEdge[startIndex];

        for (;;) {
            if (edge.getEnd().getIndex() == endIndex) {
                //edge already exist
                return edge;
            }

            //check if line intersect the triangle
            final double leftSide = Maths.lineSide(edge.getStart().getPosition(), edge.getEnd().getPosition(), endPosition);
            if (leftSide > 0) {
                //on the left side, check the adjacent edge
                final OrientedEdge adjacent = edge.getTriangle().adjacentEdge(edge);
                //end to start, we are on an oriented triangle, adjacent edge goes in the opposite direction
                final double rightSide = Maths.lineSide(adjacent.getEnd().getPosition(), adjacent.getStart().getPosition(), endPosition);
                if (rightSide > 0) {
                    //on the left side of adjacent edge, pick the left triangle and continue
                    edge = adjacent.reverse();
                } else if (rightSide < 0) {
                    //in the triangle
                    return edge;
                } else {
                    //on the adjacent edge
                    edge = adjacent.reverse();
                }
            } else if (leftSide < 0) {
                //on the right side, pick the right triangle and continue
                edge = edge.reverse();
                edge = edge.getTriangle().getEdge(edge.getEnd());
            } else {
                //point is perfectly aligned with the edge
                //but may not be on the edge

                //verify it with the adjacent edge
                /*
                        C
                        ■
                       / \
                    🡷 /   \ 🡴
                     /     \
                 ■⎻⎻■⎻⎻⎻■⎻⎻⎻■
                 X1 A   X2  B
                */
                final OrientedEdge A_C = edge.getTriangle().adjacentEdge(edge).reverse();
                final double side = Maths.lineSide(A_C.getStart().getPosition(), A_C.getEnd().getPosition(), endPosition);
                if (side < 0) {
                    //on the right side, this is X2 on the edge
                    return edge;
                } else if (side > 0) {
                    //on the left side, this is X1 not on the edge, continue the search
                    edge = A_C;
                } else {
                    //this is a flat triangle, rare, but may happen with border edges when processing constraints edges.
                    if (A_C.getEnd().getIndex() == endIndex) {
                        return A_C;
                    }
                    throw new IllegalStateException("Flat triangle case not supported");
                }
            }
        }
    }

    /**
     * Test if D point is in the angle format by vectors ab and ac.<p>
     * A,B,C must be placed ordered in counter clockwise direction.
     *
     *  <pre>{@code
     *               A
     *               ■
     *              / \
     *           🡷 /   \ 🡴
     *            /     \
     *         B ■⎻⎻⎻⎻⎻⎻⎻■ C
     *               🡲
     *
     *               ■
     *               D
     * }</pre>
     *
     * @return true if d is in the angle format by vectors ab and ac.
     */
    private static boolean inScanArea(Vertex a, Vertex b, Vertex c, Vertex d) {
        return Maths.lineSide(a.getPosition(), b.getPosition(), d.getPosition()) >= 0
         && Maths.lineSide(a.getPosition(), c.getPosition(), d.getPosition()) < 0;
    }
}
