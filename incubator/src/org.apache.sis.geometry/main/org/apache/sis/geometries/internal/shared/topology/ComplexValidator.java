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

import java.util.Arrays;
import java.util.BitSet;
import static org.apache.sis.geometries.internal.shared.topology.IndexedComplex.index;
import static org.apache.sis.geometries.internal.shared.topology.IndexedComplex.faceContainer;
import static org.apache.sis.geometries.internal.shared.topology.IndexedComplex.solidContainer;


/**
 * Verifies that the primitives of a newly built {@link IndexedComplex} have mutually disjoint interiors,
 * as required by ISO 19107 for building a topological complex from geometries, and finds the primitives
 * isolated in a primitive of higher dimension.
 *
 * <p>The verifications are geometric, with the tolerance of the complex:</p>
 * <ul>
 *   <li>no node lies inside an edge,</li>
 *   <li>no edge crosses another edge, or the interior of a face,</li>
 *   <li>each shell of a solid is closed and consistently oriented,</li>
 *   <li>no two solids are on the same side of a face, and no face other than those of its boundary
 *       lies inside a solid, so no solid lies inside another one.</li>
 * </ul>
 *
 * <p>A node inside a face or a solid, and an edge inside a solid, are isolated in that primitive,
 * as allowed by ISO 19107. An edge inside a face is not allowed, since it would be a dangling edge
 * on the boundary of the face.</p>
 *
 * <p>Two faces whose interiors overlap always have an edge of one crossing the other, or an edge
 * of one inside the other, so faces are not compared directly.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
final class ComplexValidator {

    /**
     * Container code of each node, as stored by {@link IndexedComplex#nodeContainers}.
     */
    final int[] nodeContainers;

    /**
     * Solid in which each edge is isolated, or −1 if none.
     */
    final int[] edgeContainers;

    /**
     * The complex to verify.
     */
    private final IndexedComplex complex;

    /**
     * The geometric predicates.
     */
    private final ComplexGeometry geometry;

    /**
     * Plane of each face.
     */
    private final ComplexGeometry.Plane[] planes;

    /**
     * Prepares the verification of the given complex, which shall have no removed primitive.
     *
     * @throws IllegalArgumentException if a face has no area.
     */
    ComplexValidator(final IndexedComplex complex) {
        this.complex = complex;
        geometry = new ComplexGeometry(complex);
        nodeContainers = new int[complex.nodeCount];
        edgeContainers = new int[complex.edgeCount];
        Arrays.fill(nodeContainers, -1);
        Arrays.fill(edgeContainers, -1);
        planes = new ComplexGeometry.Plane[complex.faceCount];
        for (int f = 0; f < planes.length; f++) {
            planes[f] = geometry.plane(f);
        }
    }

    /**
     * Runs all verifications.
     *
     * @throws IllegalArgumentException if the interiors of two primitives intersect, or a shell is not closed.
     */
    void validate() {
        checkIntersections();
        checkShells();
        checkSolids();
    }

    /**
     * Compares the nodes, edges and faces whose bounding boxes overlap. The boxes are sorted on their
     * minimal x value, then swept so that only boxes overlapping along x are compared.
     */
    private void checkIntersections() {
        final double tolerance = geometry.tolerance;
        final int nodeCount = complex.nodeCount;
        final int edgeEnd   = nodeCount + complex.edgeCount;
        final int count     = edgeEnd + complex.faceCount;
        final double[] boxes = new double[count * 6];
        for (int i = 0; i < nodeCount; i++) {
            box(boxes, i, new int[] {i});
        }
        for (int e = 0; e < complex.edgeCount; e++) {
            box(boxes, nodeCount + e, new int[] {complex.edgeNodes[2*e], complex.edgeNodes[2*e + 1]});
        }
        for (int f = 0; f < planes.length; f++) {
            box(boxes, edgeEnd + f, planes[f].rings[0]);
        }
        final Integer[] order = new Integer[count];
        for (int i = 0; i < count; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (i, j) -> Double.compare(boxes[i * 6], boxes[j * 6]));
        final int[] active = new int[count];
        int activeCount = 0;
        for (final int i : order) {
            final double min = boxes[i * 6] - tolerance;
            int kept = 0;
            for (int a = 0; a < activeCount; a++) {
                final int j = active[a];
                if (boxes[j * 6 + 3] >= min) {
                    active[kept++] = j;
                    if (overlaps(boxes, i, j, tolerance)) {
                        compare(Math.min(i, j), Math.max(i, j), nodeCount, edgeEnd);
                    }
                }
            }
            activeCount = kept;
            active[activeCount++] = i;
        }
    }

    /**
     * Stores in {@code boxes} the bounding box of the given nodes, as the minimal then maximal coordinates.
     */
    private void box(final double[] boxes, final int item, final int[] nodes) {
        final double[] xyz = complex.xyz;
        final int b = item * 6;
        for (int k = 0; k < 3; k++) {
            boxes[b + k]     = Double.POSITIVE_INFINITY;
            boxes[b + 3 + k] = Double.NEGATIVE_INFINITY;
        }
        for (final int node : nodes) {
            for (int k = 0; k < 3; k++) {
                final double value = xyz[node * 3 + k];
                boxes[b + k]     = Math.min(boxes[b + k], value);
                boxes[b + 3 + k] = Math.max(boxes[b + 3 + k], value);
            }
        }
    }

    /**
     * Returns whether the boxes of the two given items overlap, within the tolerance.
     */
    private static boolean overlaps(final double[] boxes, final int i, final int j, final double tolerance) {
        for (int k = 0; k < 3; k++) {
            if (boxes[i * 6 + k] > boxes[j * 6 + 3 + k] + tolerance ||
                boxes[j * 6 + k] > boxes[i * 6 + 3 + k] + tolerance)
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Compares two items whose boxes overlap. Items are numbered nodes first, then edges, then faces,
     * so the first item is never of a higher dimension than the second one.
     */
    private void compare(final int i, final int j, final int nodeCount, final int edgeEnd) {
        if (j < nodeCount) {
            return;                                         // two nodes are distinct by construction.
        }
        if (i < nodeCount) {
            if (j < edgeEnd) {
                nodeInEdge(i, j - nodeCount);
            } else {
                nodeInFace(i, j - edgeEnd);
            }
        } else if (i < edgeEnd) {
            if (j < edgeEnd) {
                edgeCrossesEdge(i - nodeCount, j - nodeCount);
            } else {
                edgeCrossesFace(i - nodeCount, j - edgeEnd);
            }
        }
        // Two faces are covered by the comparisons of their nodes and edges.
    }

    /**
     * Verifies that the given node is not inside the given edge.
     */
    private void nodeInEdge(final int node, final int edge) {
        final int a = complex.edgeNodes[2*edge], b = complex.edgeNodes[2*edge + 1];
        if (node != a && node != b && geometry.insideSegment(geometry.point(node), geometry.point(a), geometry.point(b))) {
            fail("the node " + geometry.describeNode(node) + " is inside the " + geometry.describeEdge(edge));
        }
    }

    /**
     * Records the given node as isolated in the given face if it is inside that face. The node may be
     * a point, the end of a line, or the corner of a face in another plane, such as the apex of a pyramid
     * touching the face. A corner of a face in the same plane cannot be inside the face without an edge
     * of one face inside or across the other, which is reported by the comparisons of edges and faces.
     */
    private void nodeInFace(final int node, final int face) {
        for (final int[] ring : planes[face].rings) {
            for (final int corner : ring) {
                if (corner == node) return;
            }
        }
        if (geometry.insideFace(planes[face], geometry.point(node), true)) {
            final int previous = nodeContainers[node];
            if (previous >= 0 && previous != faceContainer(face)) {
                fail("the node " + geometry.describeNode(node) + " is inside the " + geometry.describeFace(previous >> 1)
                        + " and inside the " + geometry.describeFace(face));
            }
            nodeContainers[node] = faceContainer(face);
        }
    }

    /**
     * Verifies that the given edges do not cross each other. Edges sharing a node are not compared:
     * if they overlap, the other node of one of them is inside the other edge.
     */
    private void edgeCrossesEdge(final int e1, final int e2) {
        final int a0 = complex.edgeNodes[2*e1], a1 = complex.edgeNodes[2*e1 + 1];
        final int b0 = complex.edgeNodes[2*e2], b1 = complex.edgeNodes[2*e2 + 1];
        if (a0 != b0 && a0 != b1 && a1 != b0 && a1 != b1 &&
                geometry.cross(geometry.point(a0), geometry.point(a1), geometry.point(b0), geometry.point(b1)))
        {
            fail("the " + geometry.describeEdge(e1) + " crosses the " + geometry.describeEdge(e2));
        }
    }

    /**
     * Verifies that the given edge does not cross the interior of the given face, unless it is an edge of that face.
     */
    private void edgeCrossesFace(final int edge, final int face) {
        for (final int[] ring : complex.faceRings[face]) {
            for (final int ref : ring) {
                if (index(ref) == edge) return;
            }
        }
        final double[] a = geometry.point(complex.edgeNodes[2*edge]);
        final double[] b = geometry.point(complex.edgeNodes[2*edge + 1]);
        if (geometry.meetsFace(planes[face], a, b)) {
            fail("the " + geometry.describeEdge(edge) + " crosses the " + geometry.describeFace(face));
        }
    }

    /**
     * Verifies that each shell is closed and consistently oriented:
     * every edge is used as many times in each direction by the faces of the shell.
     */
    private void checkShells() {
        final int[] uses = new int[complex.edgeCount];
        for (int s = 0; s < complex.solidCount; s++) {
            for (final int[] shell : complex.solidShells[s]) {
                final String problem = ComplexValidator.closed(complex, shell, uses);
                if (problem != null) {
                    fail("a shell of the solid " + s + " is " + problem);
                }
            }
        }
    }

    /**
     * Verifies that the given shell is closed and consistently oriented: every edge is used as many times
     * in each direction by the faces of the shell.
     *
     * @param  uses  a work array of one element per edge, which shall contain only zeros.
     *               It contains only zeros again on return.
     * @return {@code null} if the shell is valid, or a description of the problem otherwise.
     */
    static String closed(final IndexedComplex complex, final int[] shell, final int[] uses) {
        for (final int faceRef : shell) {
            for (final int[] ring : complex.faceRings[index(faceRef)]) {
                for (final int edgeRef : ring) {
                    uses[index(edgeRef)] += ((edgeRef < 0) == (faceRef < 0)) ? 1 : -1;
                }
            }
        }
        String problem = null;
        for (final int faceRef : shell) {
            for (final int[] ring : complex.faceRings[index(faceRef)]) {
                for (final int edgeRef : ring) {
                    final int edge = index(edgeRef);
                    if (uses[edge] != 0 && problem == null) {
                        problem = "open or not consistently oriented at the "
                                + new ComplexGeometry(complex).describeEdge(edge);
                    }
                    uses[edge] = 0;
                }
            }
        }
        return problem;
    }

    /**
     * Verifies that no two solids are on the same side of a face, and that no face is inside a solid
     * other than those having that face on their boundary. Since faces do not cross, a face is entirely
     * inside or outside a solid, which is tested on one point of the face with the winding number of the solid.
     * Then records the nodes and edges inside a solid as isolated in that solid.
     */
    private void checkSolids() {
        for (int f = 0; f < complex.faceCount; f++) {
            final String problem = sameSide(complex.faceCo[f]);
            if (problem != null) {
                fail("the solids " + problem + " the " + geometry.describeFace(f));
            }
        }
        final int faceCount = complex.faceCount;
        final double[][] interiorPoints = new double[faceCount][];
        for (int s = 0; s < complex.solidCount; s++) {
            final int[][] shells = complex.solidShells[s];
            final BitSet boundary = new BitSet(faceCount);
            final BitSet corners  = new BitSet(complex.nodeCount);
            for (final int face : complex.faces(s)) {
                boundary.set(face);
                for (final int[] ring : planes[face].rings) {
                    for (final int node : ring) corners.set(node);
                }
            }
            final double[] box = geometry.box(shells);
            final int[] triangles = geometry.triangles(shells);
            /*
             * Faces of other solids, or faces without solid. A point of a face is enough,
             * since a face which does not cross the boundary of the solid is entirely inside or outside.
             */
            for (int f = 0; f < faceCount; f++) {
                if (boundary.get(f)) continue;
                if (interiorPoints[f] == null) {
                    interiorPoints[f] = geometry.interiorPoint(planes[f]);
                }
                if (geometry.insideSolid(box, triangles, interiorPoints[f])) {
                    fail("the " + geometry.describeFace(f) + " is inside the solid " + s);
                }
            }
            /*
             * Nodes which are not on the boundary of the solid. A node isolated in a face is on that face,
             * which is not inside the solid as verified above.
             */
            for (int n = 0; n < complex.nodeCount; n++) {
                final int previous = nodeContainers[n];
                if (corners.get(n) || (previous >= 0 && (previous & 1) == 0)) continue;
                if (geometry.insideSolid(box, triangles, geometry.point(n))) {
                    if (previous >= 0) {
                        fail("the node " + geometry.describeNode(n) + " is inside the solids " + (previous >> 1) + " and " + s);
                    }
                    nodeContainers[n] = solidContainer(s);
                }
            }
            /*
             * Edges which are not on the boundary of a face. An edge which does not cross the boundary
             * of the solid is entirely inside or outside, so its middle is tested.
             */
            for (int e = 0; e < complex.edgeCount; e++) {
                if (complex.edgeCo[e].length != 0) continue;
                final double[] a = geometry.point(complex.edgeNodes[2*e]);
                final double[] b = geometry.point(complex.edgeNodes[2*e + 1]);
                final double[] middle = {(a[0] + b[0]) / 2, (a[1] + b[1]) / 2, (a[2] + b[2]) / 2};
                if (geometry.insideSolid(box, triangles, middle)) {
                    if (edgeContainers[e] >= 0) {
                        fail("the " + geometry.describeEdge(e) + " is inside the solids " + edgeContainers[e] + " and " + s);
                    }
                    edgeContainers[e] = s;
                }
            }
        }
    }

    /**
     * Verifies that at most one solid is below a face, and at most one above.
     *
     * @param  co  the signed references to the solids having the face on their boundary.
     * @return {@code null} if valid, or the solids and the side otherwise.
     */
    static String sameSide(final int[] co) {
        int below = -1, above = -1;
        for (final int ref : co) {
            final int solid = index(ref);
            if (ref >= 0) {
                if (below >= 0) return below + " and " + solid + " are both below";
                below = solid;
            } else {
                if (above >= 0) return above + " and " + solid + " are both above";
                above = solid;
            }
        }
        return null;
    }

    /**
     * Reports primitives whose interiors are not disjoint.
     */
    private static void fail(final String message) {
        throw new IllegalArgumentException("Invalid topological complex: " + message + '.');
    }
}
