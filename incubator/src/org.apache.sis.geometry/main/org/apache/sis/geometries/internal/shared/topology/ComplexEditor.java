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
package org.apache.sis.geometries.internal.shared.topology;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import org.apache.sis.geometries.Point;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Utilities;
import static org.apache.sis.geometries.internal.shared.topology.IndexedComplex.index;
import static org.apache.sis.geometries.internal.shared.topology.IndexedComplex.faceContainer;
import static org.apache.sis.geometries.internal.shared.topology.IndexedComplex.solidContainer;


/**
 * Edits an {@link IndexedComplex} while keeping the interiors of its primitives disjoint.
 * The operations are modeled after the topology editing functions of ISO/IEC 13249-3 (SQL/MM Spatial),
 * generalized to three dimensions:
 *
 * <table class="sis">
 *   <caption>Editing operations</caption>
 *   <tr><th>Method</th>                                 <th>SQL/MM equivalent</th></tr>
 *   <tr><td>{@link #addIsolatedNode(double...)}, {@link #addIsolatedNode(Point)}</td> <td>{@code ST_AddIsoNode}</td></tr>
 *   <tr><td>{@link #moveIsolatedNode(int, double...)}</td> <td>{@code ST_MoveIsoNode}</td></tr>
 *   <tr><td>{@link #removeIsolatedNode(int)}</td>       <td>{@code ST_RemIsoNode}</td></tr>
 *   <tr><td>{@link #addEdge(int, int)}</td>             <td>{@code ST_AddIsoEdge}, for any nodes</td></tr>
 *   <tr><td>{@link #removeEdge(int)}</td>               <td>{@code ST_RemIsoEdge}</td></tr>
 *   <tr><td>{@link #splitEdge(int, double...)}</td>     <td>{@code ST_ModEdgeSplit}</td></tr>
 *   <tr><td>{@link #healEdges(int, int)}</td>           <td>{@code ST_ModEdgeHeal}</td></tr>
 *   <tr><td>{@link #splitFace(int, int...)}</td>        <td>{@code ST_AddEdgeModFace}, for a path of edges</td></tr>
 *   <tr><td>{@link #mergeFaces(int)}</td>               <td>{@code ST_RemEdgeModFace}</td></tr>
 *   <tr><td>{@link #addFace(int[]...)}, {@link #removeFace(int)}</td>   <td>none, faces are implicit in SQL/MM</td></tr>
 *   <tr><td>{@link #addSolid(int[]...)}, {@link #removeSolid(int)}</td> <td>none, SQL/MM is planar</td></tr>
 * </table>
 *
 * <p>Primitives are designated by their index in the complex, and oriented primitives by signed references:
 * {@code i} for the positive orientation of primitive <var>i</var>, {@code ~i} for its negative orientation.
 * Each operation verifies its arguments and the geometry before modifying the complex, so that an operation
 * which throws an exception leaves the complex unchanged. The geometric verifications use the tolerance of
 * the complex, and compare the new primitives with all the primitives of the complex, without spatial index.</p>
 *
 * <p>Removed primitives keep their index, which is not reused. There is no verification that faces are planar:
 * splitting and merging faces only changes the topology, and the realization of a face is a polygon through
 * its nodes.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ComplexEditor {

    /**
     * The complex to edit.
     */
    private final IndexedComplex complex;

    /**
     * The geometric predicates on the complex.
     */
    private final ComplexGeometry geometry;

    /**
     * Creates an editor of the given complex.
     *
     * @param  complex  the complex to edit, which shall not be empty.
     * @throws IllegalArgumentException if the complex is empty, since it has no coordinate reference system.
     */
    public ComplexEditor(final IndexedComplex complex) {
        ArgumentChecks.ensureNonNull("complex", complex);
        if (complex.dimension == 0) {
            throw new IllegalArgumentException("An empty complex has no coordinate reference system.");
        }
        this.complex = complex;
        geometry = new ComplexGeometry(complex);
    }

    /**
     * Returns the complex edited by this editor.
     *
     * @return the edited complex.
     */
    public IndexedComplex getComplex() {
        return complex;
    }

    // ──────────────────────────────────────────────────────────────────────────────────────────
    //  Nodes
    // ──────────────────────────────────────────────────────────────────────────────────────────

    /**
     * Adds a node without edge. The node is isolated in the face or solid which contains it, if any.
     *
     * @param  coordinates  the coordinates of the node, in the reference system of the complex.
     * @return index of the new node.
     * @throws IllegalArgumentException if the position is on a node or inside an edge.
     */
    public int addIsolatedNode(final double... coordinates) {
        final double[] p = toXYZ(coordinates);
        checkPosition(p, -1);
        final int container = containerOf(p);
        final int node = complex.newNode(Arrays.copyOf(p, complex.dimension));
        complex.setNodeContainer(node, container);
        return node;
    }

    /**
     * Adds a node without edge at the position of the given point, with the other attributes of the point.
     * The node is isolated in the face or solid which contains it, if any.
     *
     * @param  point  the point to add, in the reference system of the complex.
     * @return index of the new node.
     * @throws IllegalArgumentException if the point has another reference system, is empty,
     *         or if its position is on a node or inside an edge.
     */
    public int addIsolatedNode(final Point point) {
        ArgumentChecks.ensureNonNull("point", point);
        if (point.isEmpty()) {
            throw failure("The point is empty.");
        }
        if (!Utilities.equalsIgnoreMetadata(complex.crs, point.getCoordinateReferenceSystem())) {
            throw failure("The point is not in the reference system of the complex.");
        }
        final int node = addIsolatedNode(point.getPosition().toArrayDouble());
        complex.points.merge(node, point.getDataPoints(), 0);
        return node;
    }

    /**
     * Moves a node without edge. The face or solid in which the node is isolated is updated.
     * The other attributes of the node are unchanged.
     *
     * @param  node         index of the node to move.
     * @param  coordinates  the new coordinates of the node, in the reference system of the complex.
     * @throws IllegalArgumentException if the node is the end of an edge, or if the new position
     *         is on another node or inside an edge.
     */
    public void moveIsolatedNode(final int node, final double... coordinates) {
        checkNode(node);
        if (complex.nodeCo[node].length != 0) {
            throw failure("The node " + geometry.describeNode(node) + " is the end of an edge.");
        }
        final double[] p = toXYZ(coordinates);
        checkPosition(p, node);
        final int container = containerOf(p);
        complex.moveNode(node, Arrays.copyOf(p, complex.dimension));
        complex.setNodeContainer(node, container);
    }

    /**
     * Removes a node without edge.
     *
     * @param  node  index of the node to remove.
     * @throws IllegalArgumentException if the node is the end of an edge.
     */
    public void removeIsolatedNode(final int node) {
        checkNode(node);
        if (complex.nodeCo[node].length != 0) {
            throw failure("The node " + geometry.describeNode(node) + " is the end of an edge.");
        }
        complex.removeNode(node);
    }

    // ──────────────────────────────────────────────────────────────────────────────────────────
    //  Edges
    // ──────────────────────────────────────────────────────────────────────────────────────────

    /**
     * Adds an edge between two nodes, on the boundary of no face. In three dimensions,
     * the edge is isolated in the solid which contains it, if any. An edge inside a face
     * shall be added by {@link #splitFace(int, int...)} instead.
     *
     * @param  start  index of the node where the edge starts.
     * @param  end    index of the node where the edge ends.
     * @return index of the new edge.
     * @throws IllegalArgumentException if there is already an edge between the nodes, or if the segment
     *         between them passes through a node, crosses an edge, or meets the interior of a face.
     */
    public int addEdge(final int start, final int end) {
        checkNode(start);
        checkNode(end);
        if (start == end) {
            throw failure("An edge needs two distinct nodes.");
        }
        checkNoEdge(start, end);
        checkSegment(start, end, -1);
        final double[] a = geometry.point(start), b = geometry.point(end);
        final double[] middle = {(a[0] + b[0]) / 2, (a[1] + b[1]) / 2, (a[2] + b[2]) / 2};
        int container = -1;
        for (int s = 0; s < complex.solidCount; s++) {
            if (!complex.removedSolids.get(s) && geometry.insideSolid(complex.solidShells[s], middle)) {
                container = s;
                break;
            }
        }
        final int edge = complex.newEdge(start, end);
        complex.setEdgeContainer(edge, container);
        return edge;
    }

    /**
     * Removes an edge on the boundary of no face. The nodes at the ends of the edge are kept.
     *
     * @param  edge  index of the edge to remove.
     * @throws IllegalArgumentException if the edge is on the boundary of a face.
     */
    public void removeEdge(final int edge) {
        checkEdge(edge);
        if (complex.edgeCo[edge].length != 0) {
            throw failure("The " + geometry.describeEdge(edge) + " is on the boundary of a face.");
        }
        complex.removeEdge(edge);
    }

    /**
     * Splits an edge by a new node. The edge keeps its start and ends at the new node, and a new edge goes
     * from the new node to the old end of the edge. Each face around the edge is bounded by both edges.
     * The attributes of the new node other than its position are interpolated between the ends of the edge,
     * as specified by {@link NodePoints#getAttributeInterpolation(String)}.
     *
     * @param  edge         index of the edge to split.
     * @param  coordinates  the coordinates of the new node, which shall be inside the edge within the tolerance.
     * @return index of the new node. The index of the new edge is {@link IndexedComplex#getEdgeCount()} − 1.
     * @throws IllegalArgumentException if the position is not inside the edge.
     */
    public int splitEdge(final int edge, final double... coordinates) {
        checkEdge(edge);
        final double[] p = toXYZ(coordinates);
        final int a = complex.edgeNodes[2*edge], b = complex.edgeNodes[2*edge + 1];
        if (!geometry.insideSegment(p, geometry.point(a), geometry.point(b))) {
            throw failure("The position is not inside the " + geometry.describeEdge(edge) + '.');
        }
        final int[] faces = distinct(complex.edgeCo[edge]);
        final int container = complex.edgeContainers[edge];
        final double[] pa = geometry.point(a), pb = geometry.point(b);
        double along = 0, length2 = 0;
        for (int k = 0; k < 3; k++) {
            along   += (p[k]  - pa[k]) * (pb[k] - pa[k]);
            length2 += (pb[k] - pa[k]) * (pb[k] - pa[k]);
        }
        final int node  = complex.newNode(Arrays.copyOf(p, complex.dimension));
        complex.points.interpolate(node, a, b, along / length2);
        final int added = complex.newEdge(node, b);
        complex.setEdgeNodes(edge, a, node);
        complex.setEdgeContainer(added, container);
        if (container >= 0) {
            complex.setNodeContainer(node, solidContainer(container));
        }
        /*
         * A face going along the edge now goes along both edges, in the same direction.
         */
        final int[][][] rings = new int[faces.length][][];
        for (int i = 0; i < faces.length; i++) {
            final int[][] old = complex.faceRings[faces[i]];
            rings[i] = new int[old.length][];
            for (int r = 0; r < old.length; r++) {
                final int[] ring = new int[old[r].length * 2];
                int n = 0;
                for (final int ref : old[r]) {
                    if (ref == edge) {
                        ring[n++] = edge;
                        ring[n++] = added;
                    } else if (ref == ~edge) {
                        ring[n++] = ~added;
                        ring[n++] = ~edge;
                    } else {
                        ring[n++] = ref;
                    }
                }
                rings[i][r] = Arrays.copyOf(ring, n);
            }
        }
        complex.setFaceRings(faces, rings);
        return node;
    }

    /**
     * Merges two edges sharing a node at which no other edge ends, and removes that node.
     * The first edge is kept and extended to the other end of the second edge, which is removed.
     * The shared node shall be on the segment between the two other ends, so the geometry does not change.
     *
     * @param  first   index of the edge to keep.
     * @param  second  index of the edge to remove.
     * @throws IllegalArgumentException if the edges do not share a node, if other edges end at that node,
     *         if the shared node is not between the other ends, or if the edges are not on the same faces.
     */
    public void healEdges(final int first, final int second) {
        checkEdge(first);
        checkEdge(second);
        if (first == second) {
            throw failure("The edges shall be distinct.");
        }
        final int a0 = complex.edgeNodes[2*first],  a1 = complex.edgeNodes[2*first  + 1];
        final int b0 = complex.edgeNodes[2*second], b1 = complex.edgeNodes[2*second + 1];
        final int shared;
        if (a1 == b0 || a1 == b1) {
            shared = a1;
        } else if (a0 == b0 || a0 == b1) {
            shared = a0;
        } else {
            throw failure("The edges do not share a node.");
        }
        final int near = (a0 == shared) ? a1 : a0;
        final int far  = (b0 == shared) ? b1 : b0;
        if (near == far) {
            throw failure("The edges are between the same nodes.");
        }
        if (complex.nodeCo[shared].length != 2) {
            throw failure("Other edges end at the node " + geometry.describeNode(shared) + '.');
        }
        if (complex.edgeContainers[first] != complex.edgeContainers[second]) {
            throw failure("The edges are isolated in different solids.");
        }
        if (!Arrays.equals(distinct(complex.edgeCo[first]), distinct(complex.edgeCo[second]))) {
            throw failure("The edges are not on the boundary of the same faces.");
        }
        if (!geometry.insideSegment(geometry.point(shared), geometry.point(near), geometry.point(far))) {
            throw failure("The node " + geometry.describeNode(shared) + " is not between the other ends of the edges.");
        }
        final int[] faces = distinct(complex.edgeCo[second]);
        final int[][][] rings = new int[faces.length][][];
        for (int i = 0; i < faces.length; i++) {
            final int[][] old = complex.faceRings[faces[i]];
            rings[i] = new int[old.length][];
            for (int r = 0; r < old.length; r++) {
                rings[i][r] = IndexedComplex.remove(IndexedComplex.remove(old[r], second), ~second);
            }
        }
        complex.setEdgeNodes(first, (a0 == shared) ? far : a0, (a1 == shared) ? far : a1);
        complex.setFaceRings(faces, rings);
        complex.removeEdge(second);
        complex.removeNode(shared);
    }

    // ──────────────────────────────────────────────────────────────────────────────────────────
    //  Faces
    // ──────────────────────────────────────────────────────────────────────────────────────────

    /**
     * Splits a face by a path of new edges, and returns the new face. The path is one of the following:
     *
     * <ul>
     *   <li>An open path from a node to another node of the same ring of the face, passing through
     *       nodes isolated in the face. The face is split in two along the path. If the ring is a hole,
     *       the new face is the part between the hole and the path.</li>
     *   <li>A closed path through nodes isolated in the face, the last node being the first one.
     *       The new face is the inside of the path, which becomes a hole of the face.</li>
     * </ul>
     *
     * The nodes of the path are no longer isolated. The holes and the isolated nodes of the face which are
     * inside the new face are moved to the new face. Each solid using the face also uses the new face,
     * with the same orientation.
     *
     * @param  face  index of the face to split.
     * @param  path  indices of the nodes of the path, at least two for an open path and four for a closed path.
     * @return index of the new face. The new edges are the last ones of the complex, in the order of the path.
     * @throws IllegalArgumentException if the path is invalid, goes outside the face, crosses itself,
     *         or crosses any other primitive.
     */
    public int splitFace(final int face, final int... path) {
        checkFace(face);
        final int k = path.length - 1;
        if (k < 1) {
            throw failure("A path needs at least two nodes.");
        }
        for (final int node : path) {
            checkNode(node);
        }
        final boolean closed = (path[0] == path[k]);
        if (closed && k < 3) {
            throw failure("A closed path needs at least three distinct nodes.");
        }
        for (int i = 0; i < k; i++) {
            for (int j = i + 1; j <= k; j++) {
                if (path[i] == path[j] && !(closed && i == 0 && j == k)) {
                    throw failure("The path passes twice through the node " + geometry.describeNode(path[i]) + '.');
                }
            }
        }
        final int code = faceContainer(face);
        for (int i = closed ? 0 : 1; i < k; i++) {
            if (complex.nodeContainers[path[i]] != code || complex.nodeCo[path[i]].length != 0) {
                throw failure("The node " + geometry.describeNode(path[i]) + " is not a node without edge inside the face.");
            }
        }
        /*
         * An open path starts and ends on the same ring of the face.
         */
        final int[][] rings = complex.faceRings[face];
        int ringIndex = -1, i0 = -1, j0 = -1;
        if (!closed) {
            for (int r = 0; r < rings.length; r++) {
                final int[] nodes = complex.nodes(rings[r]);
                int i = -1, j = -1;
                for (int p = 0; p < nodes.length; p++) {
                    if (nodes[p] == path[0] || nodes[p] == path[k]) {
                        if ((nodes[p] == path[0] ? i : j) >= 0) {
                            throw failure("The ring passes twice through the node " + geometry.describeNode(nodes[p]) + '.');
                        }
                        if (nodes[p] == path[0]) i = p; else j = p;
                    }
                }
                if (i >= 0 || j >= 0) {
                    if (i < 0 || j < 0 || ringIndex >= 0) {
                        throw failure("The ends of the path shall be on the same ring of the face.");
                    }
                    ringIndex = r;
                    i0 = i;
                    j0 = j;
                }
            }
            if (ringIndex < 0) {
                throw failure("The ends of the path are not on the boundary of the face.");
            }
            if (k == 1) {
                checkNoEdge(path[0], path[1]);
            }
        }
        /*
         * Each segment of the path is inside the face, crosses no other primitive, and no other segment.
         */
        final ComplexGeometry.Plane plane = geometry.plane(face);
        for (int t = 1; t <= k; t++) {
            checkSegment(path[t - 1], path[t], face);
            final double[] a = geometry.point(path[t - 1]), b = geometry.point(path[t]);
            final double[] middle = {(a[0] + b[0]) / 2, (a[1] + b[1]) / 2, (a[2] + b[2]) / 2};
            if (!geometry.insideFace(plane, middle, true)) {
                throw failure("The path goes outside the face.");
            }
        }
        for (int t = 1; t <= k; t++) {
            for (int u = t + 2; u <= k; u++) {
                if (closed && t == 1 && u == k) continue;       // adjacent through the first node.
                if (geometry.cross(geometry.point(path[t - 1]), geometry.point(path[t]),
                                   geometry.point(path[u - 1]), geometry.point(path[u])))
                {
                    throw failure("The path crosses itself.");
                }
            }
        }
        /*
         * Create the edges of the path, then the rings of both faces.
         */
        final int[] chord = new int[k];
        final int[] reversed = new int[k];
        for (int t = 0; t < k; t++) {
            chord[t] = complex.newEdge(path[t], path[t + 1]);
        }
        for (int t = 0; t < k; t++) {
            reversed[t] = ~chord[k - 1 - t];
        }
        for (int t = closed ? 0 : 1; t < k; t++) {
            complex.setNodeContainer(path[t], -1);
        }
        final List<int[]> kept  = new ArrayList<>();        // rings of the face, exterior first.
        final List<int[]> holes = new ArrayList<>();        // holes to move to the new face if inside it.
        final int[] exterior;                               // exterior ring of the new face.
        if (closed) {
            final boolean ccw = geometry.counterclockwise(plane, Arrays.copyOf(path, k));
            exterior = ccw ? chord : reversed;
            kept.add(rings[0]);
            kept.add(ccw ? reversed : chord);
            for (int r = 1; r < rings.length; r++) {
                holes.add(rings[r]);
            }
        } else {
            final int[] ring  = rings[ringIndex];
            final int[] part1 = concat(ring, i0, j0, reversed);     // along the ring to the end of the path, then back.
            final int[] part2 = concat(ring, j0, i0, chord);        // along the ring to the start of the path, then along it.
            if (ringIndex == 0) {
                kept.add(part1);
                exterior = part2;
                for (int r = 1; r < rings.length; r++) {
                    holes.add(rings[r]);
                }
            } else {
                // One part goes around the region between the hole and the path, the other part is the new hole.
                final boolean ccw = geometry.counterclockwise(plane, complex.nodes(part1));
                exterior = ccw ? part1 : part2;
                kept.add(rings[0]);
                for (int r = 1; r < rings.length; r++) {
                    if (r == ringIndex) {
                        kept.add(ccw ? part2 : part1);
                    } else {
                        holes.add(rings[r]);
                    }
                }
            }
        }
        final List<int[]> moved = new ArrayList<>();
        moved.add(exterior);
        final int[][] exteriorNodes = {complex.nodes(exterior)};
        for (final int[] hole : holes) {
            if (geometry.insideRings(plane, exteriorNodes, geometry.point(complex.startNode(hole[0])))) {
                moved.add(hole);
            } else {
                kept.add(hole);
            }
        }
        complex.setFaceRings(new int[] {face}, new int[][][] {kept.toArray(int[][]::new)});
        final int created = complex.newFace(moved.toArray(int[][]::new));
        /*
         * The solids using the face also use the new face, in the same shell and with the same orientation.
         */
        for (final int ref : complex.faceCo[face].clone()) {
            final int solid = index(ref);
            final int use = (ref < 0) ? ~face : face;
            final int[][] shells = complex.solidShells[solid].clone();
            for (int h = 0; h < shells.length; h++) {
                if (contains(shells[h], use)) {
                    shells[h] = IndexedComplex.append(shells[h], (ref < 0) ? ~created : created);
                }
            }
            complex.setShells(solid, shells);
        }
        /*
         * The isolated nodes inside the new face are moved to it.
         */
        final int[][] createdNodes = new int[moved.size()][];
        for (int r = 0; r < createdNodes.length; r++) {
            createdNodes[r] = complex.nodes(moved.get(r));
        }
        for (final int node : complex.faceIsolated[face].clone()) {
            if (geometry.insideRings(plane, createdNodes, geometry.point(node))) {
                complex.setNodeContainer(node, faceContainer(created));
            }
        }
        return created;
    }

    /**
     * Merges the two faces on each side of an edge, and removes the edge. The first face around the edge
     * is kept and extended to the second face, which is removed. The holes and isolated nodes of the second
     * face are moved to the first face. If an end of the edge is not the end of any other edge, it becomes
     * a node isolated in the face.
     *
     * @param  edge  index of the edge between the faces to merge.
     * @throws IllegalArgumentException if the edge does not separate exactly two faces of consistent
     *         orientations, if the faces are not on the boundaries of the same solids, or if the faces
     *         share another edge.
     */
    public void mergeFaces(final int edge) {
        checkEdge(edge);
        final int[] co = complex.edgeCo[edge];
        if (co.length != 2) {
            throw failure("The " + geometry.describeEdge(edge) + " does not separate exactly two faces.");
        }
        final int f = index(co[0]), g = index(co[1]);
        if (f == g) {
            throw failure("The " + geometry.describeEdge(edge) + " has the same face on both sides.");
        }
        if ((co[0] < 0) == (co[1] < 0)) {
            throw failure("The faces on each side of the " + geometry.describeEdge(edge) + " have opposite orientations.");
        }
        final int[] solidsOfF = complex.faceCo[f].clone();
        final int[] solidsOfG = complex.faceCo[g].clone();
        Arrays.sort(solidsOfF);
        Arrays.sort(solidsOfG);
        if (!Arrays.equals(solidsOfF, solidsOfG)) {
            throw failure("The faces are not on the boundary of the same solids.");
        }
        final int[][] rf = complex.faceRings[f];
        final int[][] rg = complex.faceRings[g];
        final int[] pf = locate(rf, edge);
        final int[] pg = locate(rg, edge);
        final int[] merged = splice(rf[pf[0]], pf[1], rg[pg[0]], pg[1]);
        final BitSet used = new BitSet();
        for (final int ref : merged) {
            if (used.get(index(ref))) {
                throw failure("The faces share other edges than the " + geometry.describeEdge(edge) + '.');
            }
            used.set(index(ref));
        }
        /*
         * The merged ring is the exterior ring if both rings were exterior rings.
         * Otherwise one face is in a hole of the other face, and the merged ring is that hole made smaller.
         */
        final List<int[]> rings = new ArrayList<>();
        if (pf[0] == 0 && pg[0] == 0) {
            rings.add(merged);
        } else if (pf[0] == 0) {
            rings.add(rg[0]);
            rings.add(merged);
        } else if (pg[0] == 0) {
            rings.add(rf[0]);
            rings.add(merged);
        } else {
            throw failure("The " + geometry.describeEdge(edge) + " is on holes of both faces.");
        }
        for (int r = 1; r < rf.length; r++) {
            if (r != pf[0]) rings.add(rf[r]);
        }
        for (int r = 1; r < rg.length; r++) {
            if (r != pg[0]) rings.add(rg[r]);
        }
        /*
         * Remove the second face from the solids, move its isolated nodes, then replace it by the first face.
         */
        for (final int ref : complex.faceCo[g].clone()) {
            final int solid = index(ref);
            final int[][] shells = complex.solidShells[solid].clone();
            for (int h = 0; h < shells.length; h++) {
                shells[h] = IndexedComplex.remove(shells[h], (ref < 0) ? ~g : g);
            }
            complex.setShells(solid, shells);
        }
        for (final int node : complex.faceIsolated[g].clone()) {
            complex.setNodeContainer(node, faceContainer(f));
        }
        complex.removeFace(g);
        complex.setFaceRings(new int[] {f}, new int[][][] {rings.toArray(int[][]::new)});
        final int a = complex.edgeNodes[2*edge], b = complex.edgeNodes[2*edge + 1];
        complex.removeEdge(edge);
        for (final int node : new int[] {a, b}) {
            if (complex.nodeCo[node].length == 0) {
                complex.setNodeContainer(node, faceContainer(f));
            }
        }
    }

    /**
     * Adds a face bounded by existing edges. The holes are reversed if needed for being wound
     * opposite to the exterior ring. The nodes inside the face become isolated in it.
     *
     * @param  rings  the rings of the face, the exterior ring first, as signed references to edges in traversal order.
     * @return index of the new face.
     * @throws IllegalArgumentException if a ring is not closed, if an edge is used twice, if a face has the same edges,
     *         if an edge crosses the face or is inside it, or if the face is inside a solid.
     */
    public int addFace(final int[]... rings) {
        if (rings.length == 0) {
            throw failure("A face needs an exterior ring.");
        }
        final int[][] refs  = new int[rings.length][];
        final int[][] nodes = new int[rings.length][];
        final BitSet used = new BitSet();
        for (int r = 0; r < rings.length; r++) {
            refs[r] = rings[r].clone();
            final int m = refs[r].length;
            if (m < 3) {
                throw failure("A ring needs at least three edges.");
            }
            for (final int ref : refs[r]) {
                final int e = index(ref);
                checkEdge(e);
                if (used.get(e)) {
                    throw failure("The " + geometry.describeEdge(e) + " is used twice.");
                }
                used.set(e);
            }
            for (int i = 0; i < m; i++) {
                if (complex.endNode(refs[r][i]) != complex.startNode(refs[r][(i + 1) % m])) {
                    throw failure("A ring is not closed after the " + geometry.describeEdge(index(refs[r][i])) + '.');
                }
            }
            nodes[r] = complex.nodes(refs[r]);
        }
        final ComplexGeometry.Plane plane = geometry.plane(nodes);
        if (plane == null) {
            throw failure("The face has no area.");
        }
        for (int r = 1; r < refs.length; r++) {
            if (geometry.counterclockwise(plane, nodes[r])) {
                final int[] ring = refs[r];
                final int m = ring.length;
                final int[] reversed = new int[m];
                for (int i = 0; i < m; i++) {
                    reversed[i] = ~ring[m - 1 - i];
                }
                refs[r]  = reversed;
                nodes[r] = complex.nodes(reversed);         // also updates the plane, which shares this array.
            }
        }
        for (final int ref : complex.edgeCo[index(refs[0][0])]) {
            if (sameEdges(index(ref), used)) {
                throw failure("A face with the same edges already exists.");
            }
        }
        /*
         * No other edge in the face or across it, and no part of the face inside a solid.
         */
        for (int e = 0; e < complex.edgeCount; e++) {
            if (!complex.removedEdges.get(e) && !used.get(e) &&
                    geometry.meetsFace(plane, geometry.point(complex.edgeNodes[2*e]), geometry.point(complex.edgeNodes[2*e + 1])))
            {
                throw failure("The " + geometry.describeEdge(e) + " crosses the face or is inside it.");
            }
        }
        final BitSet corners = new BitSet();
        for (final int[] ring : nodes) {
            for (final int node : ring) corners.set(node);
        }
        final List<Integer> inside = new ArrayList<>();
        for (int n = 0; n < complex.nodeCount; n++) {
            if (!complex.removedNodes.get(n) && !corners.get(n) && geometry.insideFace(plane, geometry.point(n), true)) {
                if (complex.nodeContainers[n] >= 0) {
                    throw failure("The node " + geometry.describeNode(n) + " is already inside a face or a solid.");
                }
                inside.add(n);
            }
        }
        final double[] p = geometry.interiorPoint(plane);
        for (int s = 0; s < complex.solidCount; s++) {
            if (!complex.removedSolids.get(s) && geometry.insideSolid(complex.solidShells[s], p)) {
                throw failure("The face is inside the solid " + s + '.');
            }
        }
        final int face = complex.newFace(refs);
        for (final int node : inside) {
            complex.setNodeContainer(node, faceContainer(face));
        }
        return face;
    }

    /**
     * Removes a face on the boundary of no solid. The edges of the face are kept,
     * and the nodes isolated in the face are no longer isolated.
     *
     * @param  face  index of the face to remove.
     * @throws IllegalArgumentException if the face is on the boundary of a solid.
     */
    public void removeFace(final int face) {
        checkFace(face);
        if (complex.faceCo[face].length != 0) {
            throw failure("The " + geometry.describeFace(face) + " is on the boundary of a solid.");
        }
        complex.removeFace(face);
    }

    // ──────────────────────────────────────────────────────────────────────────────────────────
    //  Solids
    // ──────────────────────────────────────────────────────────────────────────────────────────

    /**
     * Adds a solid bounded by existing faces. Each shell is reversed if needed, so that the exterior shell
     * encloses a positive volume and the interior shells a negative one, which puts the solid below all faces.
     * The nodes and edges inside the solid become isolated in it.
     *
     * @param  shells  the shells of the solid, the exterior shell first, as signed references to faces.
     * @return index of the new solid.
     * @throws IllegalArgumentException if a face is used twice, if a shell is not closed, if a face would have
     *         two solids on the same side, if another face is inside the solid, or if the solid is inside another one.
     */
    public int addSolid(final int[]... shells) {
        if (complex.dimension != 3) {
            throw failure("A solid requires 3-dimensional coordinates.");
        }
        if (shells.length == 0) {
            throw failure("A solid needs an exterior shell.");
        }
        final int[][] refs = new int[shells.length][];
        final BitSet used = new BitSet();
        for (int h = 0; h < shells.length; h++) {
            refs[h] = shells[h].clone();
            if (refs[h].length == 0) {
                throw failure("A shell needs faces.");
            }
            for (final int ref : refs[h]) {
                final int f = index(ref);
                checkFace(f);
                if (used.get(f)) {
                    throw failure("The " + geometry.describeFace(f) + " is used twice.");
                }
                used.set(f);
            }
            final double volume = geometry.signedVolume(refs[h]);
            if ((h == 0) ? volume < 0 : volume > 0) {
                for (int i = 0; i < refs[h].length; i++) {
                    refs[h][i] = ~refs[h][i];
                }
            }
        }
        final int[] uses = new int[complex.edgeCount];
        final int future = complex.solidCount;
        for (final int[] shell : refs) {
            final String problem = ComplexValidator.closed(complex, shell, uses);
            if (problem != null) {
                throw failure("A shell is " + problem + '.');
            }
            for (final int ref : shell) {
                final String side = ComplexValidator.sameSide(IndexedComplex.append(complex.faceCo[index(ref)], (ref < 0) ? ~future : future));
                if (side != null) {
                    throw failure("The solids " + side + " the " + geometry.describeFace(index(ref)) + '.');
                }
            }
        }
        /*
         * No other face inside the new solid, and no face of the new solid inside another solid.
         */
        final double[] box = geometry.box(refs);
        final int[] triangles = geometry.triangles(refs);
        for (int f = 0; f < complex.faceCount; f++) {
            if (!complex.removedFaces.get(f) && !used.get(f)
                    && geometry.insideSolid(box, triangles, geometry.interiorPoint(geometry.plane(f))))
            {
                throw failure("The " + geometry.describeFace(f) + " is inside the solid.");
            }
        }
        for (int s = 0; s < complex.solidCount; s++) {
            if (complex.removedSolids.get(s)) continue;
            final BitSet boundary = new BitSet();
            for (final int f : complex.faces(s)) boundary.set(f);
            final int[][] other = complex.solidShells[s];
            final double[] otherBox = geometry.box(other);
            final int[] otherTriangles = geometry.triangles(other);
            for (int f = used.nextSetBit(0); f >= 0; f = used.nextSetBit(f + 1)) {
                if (!boundary.get(f) && geometry.insideSolid(otherBox, otherTriangles, geometry.interiorPoint(geometry.plane(f)))) {
                    throw failure("The solid is inside the solid " + s + '.');
                }
            }
        }
        /*
         * The nodes and edges inside the new solid. A node isolated in a face is on that face, which is not inside.
         */
        final BitSet corners = new BitSet();
        for (int f = used.nextSetBit(0); f >= 0; f = used.nextSetBit(f + 1)) {
            for (final int[] ring : complex.faceRings[f]) {
                for (final int node : complex.nodes(ring)) corners.set(node);
            }
        }
        final List<Integer> insideNodes = new ArrayList<>();
        for (int n = 0; n < complex.nodeCount; n++) {
            final int c = complex.nodeContainers[n];
            if (complex.removedNodes.get(n) || corners.get(n) || (c >= 0 && (c & 1) == 0)) continue;
            if (geometry.insideSolid(box, triangles, geometry.point(n))) {
                if (c >= 0) {
                    throw failure("The node " + geometry.describeNode(n) + " is already inside a solid.");
                }
                insideNodes.add(n);
            }
        }
        final List<Integer> insideEdges = new ArrayList<>();
        for (int e = 0; e < complex.edgeCount; e++) {
            if (complex.removedEdges.get(e) || complex.edgeCo[e].length != 0) continue;
            final double[] a = geometry.point(complex.edgeNodes[2*e]);
            final double[] b = geometry.point(complex.edgeNodes[2*e + 1]);
            if (geometry.insideSolid(box, triangles, new double[] {(a[0] + b[0]) / 2, (a[1] + b[1]) / 2, (a[2] + b[2]) / 2})) {
                if (complex.edgeContainers[e] >= 0) {
                    throw failure("The " + geometry.describeEdge(e) + " is already inside a solid.");
                }
                insideEdges.add(e);
            }
        }
        final int solid = complex.newSolid(refs);
        for (final int node : insideNodes) {
            complex.setNodeContainer(node, solidContainer(solid));
        }
        for (final int edge : insideEdges) {
            complex.setEdgeContainer(edge, solid);
        }
        return solid;
    }

    /**
     * Removes a solid. The faces of the solid are kept,
     * and the nodes and edges isolated in the solid are no longer isolated.
     *
     * @param  solid  index of the solid to remove.
     */
    public void removeSolid(final int solid) {
        checkSolid(solid);
        complex.removeSolid(solid);
    }

    // ──────────────────────────────────────────────────────────────────────────────────────────
    //  Verifications and helper methods
    // ──────────────────────────────────────────────────────────────────────────────────────────

    /**
     * Returns an exception for an invalid edit.
     */
    private static IllegalArgumentException failure(final String message) {
        return new IllegalArgumentException(message);
    }

    private void checkNode(final int node) {
        if (node < 0 || node >= complex.nodeCount || complex.removedNodes.get(node)) {
            throw failure("No node at index " + node + '.');
        }
    }

    private void checkEdge(final int edge) {
        if (edge < 0 || edge >= complex.edgeCount || complex.removedEdges.get(edge)) {
            throw failure("No edge at index " + edge + '.');
        }
    }

    private void checkFace(final int face) {
        if (face < 0 || face >= complex.faceCount || complex.removedFaces.get(face)) {
            throw failure("No face at index " + face + '.');
        }
    }

    private void checkSolid(final int solid) {
        if (solid < 0 || solid >= complex.solidCount || complex.removedSolids.get(solid)) {
            throw failure("No solid at index " + solid + '.');
        }
    }

    /**
     * Verifies the given coordinates and returns them in three dimensions, with −0 replaced by +0.
     */
    private double[] toXYZ(final double[] coordinates) {
        if (coordinates.length != complex.dimension) {
            throw failure("Expected " + complex.dimension + " coordinates, got " + coordinates.length + '.');
        }
        final double[] p = new double[3];
        for (int k = 0; k < coordinates.length; k++) {
            if (!Double.isFinite(coordinates[k])) {
                throw failure("Coordinates shall be finite.");
            }
            p[k] = coordinates[k] + 0.0;
        }
        return p;
    }

    /**
     * Verifies that the given position is neither on a node nor inside an edge.
     *
     * @param ignored  a node to ignore, or −1 if none.
     */
    private void checkPosition(final double[] p, final int ignored) {
        for (int n = 0; n < complex.nodeCount; n++) {
            if (n != ignored && !complex.removedNodes.get(n) && geometry.coincide(p, geometry.point(n))) {
                throw failure("The position is on the node " + geometry.describeNode(n) + '.');
            }
        }
        for (int e = 0; e < complex.edgeCount; e++) {
            if (!complex.removedEdges.get(e) && geometry.insideSegment(p,
                    geometry.point(complex.edgeNodes[2*e]), geometry.point(complex.edgeNodes[2*e + 1])))
            {
                throw failure("The position is inside the " + geometry.describeEdge(e) + '.');
            }
        }
    }

    /**
     * Returns the container code of a position which is neither on a node nor inside an edge:
     * the face or solid containing the position, or −1 if none.
     */
    private int containerOf(final double[] p) {
        for (int f = 0; f < complex.faceCount; f++) {
            if (!complex.removedFaces.get(f) && geometry.insideFace(geometry.plane(f), p, true)) {
                return faceContainer(f);
            }
        }
        for (int s = 0; s < complex.solidCount; s++) {
            if (!complex.removedSolids.get(s) && geometry.insideSolid(complex.solidShells[s], p)) {
                return solidContainer(s);
            }
        }
        return -1;
    }

    /**
     * Verifies that there is no edge between the given nodes.
     */
    private void checkNoEdge(final int a, final int b) {
        for (final int ref : complex.nodeCo[a]) {
            final int e = index(ref);
            final int other = (ref < 0) ? complex.edgeNodes[2*e + 1] : complex.edgeNodes[2*e];
            if (other == b) {
                throw failure("There is already an edge between the nodes.");
            }
        }
    }

    /**
     * Verifies that the segment between two nodes passes through no other node, crosses no edge,
     * and does not meet the interior of any face other than the given one.
     *
     * @param ignored  a face to ignore, or −1 if none.
     */
    private void checkSegment(final int a, final int b, final int ignored) {
        final double[] pa = geometry.point(a), pb = geometry.point(b);
        for (int n = 0; n < complex.nodeCount; n++) {
            if (n != a && n != b && !complex.removedNodes.get(n) && geometry.insideSegment(geometry.point(n), pa, pb)) {
                throw failure("The node " + geometry.describeNode(n) + " is on the segment.");
            }
        }
        for (int e = 0; e < complex.edgeCount; e++) {
            if (complex.removedEdges.get(e)) continue;
            final int e0 = complex.edgeNodes[2*e], e1 = complex.edgeNodes[2*e + 1];
            if (e0 != a && e0 != b && e1 != a && e1 != b && geometry.cross(pa, pb, geometry.point(e0), geometry.point(e1))) {
                throw failure("The segment crosses the " + geometry.describeEdge(e) + '.');
            }
        }
        for (int f = 0; f < complex.faceCount; f++) {
            if (f != ignored && !complex.removedFaces.get(f) && geometry.meetsFace(geometry.plane(f), pa, pb)) {
                throw failure("The segment crosses the " + geometry.describeFace(f) + " or is inside it.");
            }
        }
    }

    /**
     * Returns the indices of the primitives designated by the given signed references, sorted and without duplicates.
     */
    private static int[] distinct(final int[] refs) {
        final int[] indices = new int[refs.length];
        for (int i = 0; i < refs.length; i++) {
            indices[i] = index(refs[i]);
        }
        Arrays.sort(indices);
        int n = 0;
        for (int i = 0; i < indices.length; i++) {
            if (n == 0 || indices[n - 1] != indices[i]) {
                indices[n++] = indices[i];
            }
        }
        return Arrays.copyOf(indices, n);
    }

    /**
     * Returns whether the given array contains the given value.
     */
    private static boolean contains(final int[] array, final int value) {
        for (final int v : array) {
            if (v == value) return true;
        }
        return false;
    }

    /**
     * Returns whether the given face has exactly the given edges.
     */
    private boolean sameEdges(final int face, final BitSet edges) {
        int count = 0;
        for (final int[] ring : complex.faceRings[face]) {
            for (final int ref : ring) {
                if (!edges.get(index(ref))) return false;
                count++;
            }
        }
        return count == edges.cardinality();
    }

    /**
     * Returns the ring index and the position in that ring of the given edge in the given rings.
     */
    private int[] locate(final int[][] rings, final int edge) {
        int[] found = null;
        for (int r = 0; r < rings.length; r++) {
            for (int p = 0; p < rings[r].length; p++) {
                if (index(rings[r][p]) == edge) {
                    if (found != null) {
                        throw failure("A face goes twice along the " + geometry.describeEdge(edge) + '.');
                    }
                    found = new int[] {r, p};
                }
            }
        }
        return found;
    }

    /**
     * Returns the elements of the given ring from position {@code from} inclusive to position {@code to}
     * exclusive, in cyclic order, followed by the given tail.
     */
    private static int[] concat(final int[] ring, final int from, final int to, final int[] tail) {
        final int count = Math.floorMod(to - from, ring.length);
        final int[] result = new int[count + tail.length];
        for (int i = 0; i < count; i++) {
            result[i] = ring[(from + i) % ring.length];
        }
        System.arraycopy(tail, 0, result, count, tail.length);
        return result;
    }

    /**
     * Joins two rings traversing an edge in opposite directions, without that edge. Each ring is taken
     * from the element after the edge, in cyclic order, so the first ring goes from one end of the edge
     * to the other, and the second ring goes back.
     */
    private static int[] splice(final int[] first, final int p1, final int[] second, final int p2) {
        final int[] a = concat(first,  (p1 + 1) % first.length,  p1, new int[0]);
        return concat(second, (p2 + 1) % second.length, p2, a);
    }
}
