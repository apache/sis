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

import java.util.AbstractSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.curve.LinearRing;
import org.apache.sis.geometries.operation.ShewchukPredicates;
import org.apache.sis.geometries.solid.Polyhedron;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.geometries.topology.Complex;
import org.apache.sis.geometries.topology.DirectedEdge;
import org.apache.sis.geometries.topology.DirectedFace;
import org.apache.sis.geometries.topology.DirectedNode;
import org.apache.sis.geometries.topology.DirectedSolid;
import org.apache.sis.geometries.topology.DirectedTopo;
import org.apache.sis.geometries.topology.Edge;
import org.apache.sis.geometries.topology.Face;
import org.apache.sis.geometries.topology.Node;
import org.apache.sis.geometries.topology.Primitive;
import org.apache.sis.geometries.topology.Solid;


/**
 * A maximal topological complex storing its primitives in arrays of indices.
 * Instances are created by {@link ComplexBuilder} and modified by {@link ComplexEditor}.
 *
 * <p>The storage is the following:</p>
 * <ul>
 *   <li>A node is a point of {@link NodePoints}: its coordinates and the other attributes of the geometries
 *       from which it has been built, such as normals or colors. The geometric realizations keep them.</li>
 *   <li>An edge is its start node and its end node.</li>
 *   <li>A face is a list of rings, the exterior ring first. A ring is a cycle of signed edge references,
 *       oriented so that the face is on the left of every edge.</li>
 *   <li>A solid is a list of shells, the exterior shell first. A shell is a set of signed face references,
 *       oriented so that the solid is below every face.</li>
 * </ul>
 *
 * <p>A signed reference is the index <var>i</var> of a primitive for its positive orientation, and
 * {@code ~i} (which is −<var>i</var> − 1) for its negative orientation. Each primitive also stores its
 * co-boundary, which is kept up to date by the edits. In 3 dimensions, the faces around an edge are sorted
 * in clockwise order seen from the end of the edge. In 2 dimensions, the edges around a node are sorted
 * in clockwise order. Both sorts use the {@code orient3d} and {@code orient2d} predicates of
 * {@link ShewchukPredicates}.</p>
 *
 * <p>A node inside a face or a solid, or an edge inside a solid, is isolated in it:
 * that face or solid is its {@linkplain Primitive#getContainer() container}.</p>
 *
 * <p>The primitives returned by this complex are lightweight views created on demand. Two views are
 * equal if they are of the same type, orientation and index in the same complex. A removed primitive
 * keeps its index, which is never reused, so that the indices of the other primitives do not change.
 * The views of a removed primitive shall not be used anymore.</p>
 *
 * <p>This class is not thread-safe while it is edited.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class IndexedComplex implements Complex {

    /**
     * Shared empty array, never modified.
     */
    private static final int[] EMPTY = new int[0];

    /**
     * Tolerance relative to the largest extent of the complex, for the geometric verifications.
     */
    private static final double RELATIVE_TOLERANCE = 1E-10;

    /**
     * The reference system of the node coordinates, or {@code null} for an empty complex.
     */
    final CoordinateReferenceSystem crs;

    /**
     * Number of coordinates of each node: 2, 3, or 0 for an empty complex.
     */
    final int dimension;

    /**
     * Largest distance between two positions considered the same in the geometric verifications.
     * It is relative to the extent of the complex when it was built.
     */
    final double tolerance;

    /**
     * The positions and other attributes of the nodes, one point per node index.
     */
    final NodePoints points;

    /**
     * Coordinates of the nodes in three dimensions, with z = 0 for 2-dimensional coordinates.
     * This is {@link NodePoints#xyz}, updated when the points grow.
     */
    double[] xyz;

    /**
     * Number of nodes, edges, faces and solids, including the removed ones.
     */
    int nodeCount, edgeCount, faceCount, solidCount;

    /**
     * The start node then the end node of each edge.
     */
    int[] edgeNodes;

    /**
     * Rings of each face, the exterior ring first. Each ring is the signed references to its edges, in traversal order.
     */
    int[][][] faceRings;

    /**
     * Shells of each solid, the exterior shell first. Each shell is the signed references to its faces.
     */
    int[][][] solidShells;

    /**
     * Co-boundary of each node, edge and face, as signed references to the primitives of the next higher dimension.
     */
    int[][] nodeCo, edgeCo, faceCo;

    /**
     * Container of each node: −1 if none, {@code 2f} for face <var>f</var>, or {@code 2s + 1} for solid <var>s</var>.
     */
    int[] nodeContainers;

    /**
     * Solid containing each edge, or −1 if none.
     */
    int[] edgeContainers;

    /**
     * Primitives isolated in each face and each solid. Those of faces are nodes.
     * Those of solids are node <var>n</var> as {@code n}, and edge <var>e</var> as {@code ~e}.
     */
    int[][] faceIsolated, solidIsolated;

    /**
     * The removed nodes, edges, faces and solids.
     */
    final BitSet removedNodes, removedEdges, removedFaces, removedSolids;

    /**
     * The geometries from which this complex has been built, or {@code null} after an edit.
     */
    private List<Geometry> sources;

    /**
     * Creates a complex from the given arrays, and verifies it. The faces and solids are given as offsets
     * in arrays of references: the rings of face <var>f</var> are from {@code faceRingOffsets[f]} inclusive
     * to {@code faceRingOffsets[f+1]} exclusive, and the references of ring <var>r</var> are at the positions
     * from {@code ringEdgeOffsets[r]} inclusive to {@code ringEdgeOffsets[r+1]} exclusive in {@code faceEdgeRefs}.
     * Likewise for solids.
     *
     * @throws IllegalArgumentException if the interiors of the primitives are not disjoint.
     */
    IndexedComplex(final CoordinateReferenceSystem crs, final int dimension, final NodePoints points,
                   final int[] edgeNodes, final int[] faceRingOffsets, final int[] ringEdgeOffsets, final int[] faceEdgeRefs,
                   final int[] solidShellOffsets, final int[] shellFaceOffsets, final int[] solidFaceRefs,
                   final List<Geometry> sources)
    {
        this.crs       = crs;
        this.dimension = dimension;
        this.points    = points;
        this.edgeNodes = edgeNodes;
        this.sources   = List.copyOf(sources);
        nodeCount  = points.size();
        edgeCount  = edgeNodes.length / 2;
        faceCount  = faceRingOffsets.length - 1;
        solidCount = solidShellOffsets.length - 1;
        removedNodes  = new BitSet();
        removedEdges  = new BitSet();
        removedFaces  = new BitSet();
        removedSolids = new BitSet();
        xyz = points.xyz;
        faceRings   = split(faceCount,  faceRingOffsets,   ringEdgeOffsets,  faceEdgeRefs);
        solidShells = split(solidCount, solidShellOffsets, shellFaceOffsets, solidFaceRefs);
        /*
         * The co-boundary of a node has the edges ending at the node (positive) and those starting at it
         * (negative). The other co-boundaries are the transpose of the boundaries.
         */
        int[] lower = new int[edgeCount * 2];
        int[] upper = new int[edgeCount * 2];
        for (int e = 0; e < edgeCount; e++) {
            lower[2*e]   = edgeNodes[2*e];   upper[2*e]   = ~e;
            lower[2*e+1] = edgeNodes[2*e+1]; upper[2*e+1] = e;
        }
        nodeCo = group(nodeCount, lower, upper, lower.length);
        edgeCo = transpose(edgeCount, faceRings);
        faceCo = transpose(faceCount, solidShells);
        if (dimension == 3) {
            for (int e = 0; e < edgeCount; e++) sortAroundEdge(e);
        } else if (dimension == 2) {
            for (int n = 0; n < nodeCount; n++) sortAroundNode(n);
        }
        double extent = 0;
        for (int k = 0; k < 3; k++) {
            double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
            for (int i = k; i < nodeCount * 3; i += 3) {
                min = Math.min(min, xyz[i]);
                max = Math.max(max, xyz[i]);
            }
            if (max > min) {
                extent = Math.max(extent, max - min);
            }
        }
        tolerance = extent * RELATIVE_TOLERANCE;
        /*
         * Verify the complex and find the isolated primitives, then the inverse of their containers.
         */
        final var validator = new ComplexValidator(this);
        validator.validate();
        nodeContainers = validator.nodeContainers;
        edgeContainers = validator.edgeContainers;
        lower = new int[nodeCount + edgeCount];
        upper = new int[nodeCount + edgeCount];
        int size = 0;
        for (int n = 0; n < nodeCount; n++) {
            final int c = nodeContainers[n];
            if (c >= 0 && (c & 1) == 0) {
                lower[size] = c >> 1;
                upper[size++] = n;
            }
        }
        faceIsolated = group(faceCount, lower, upper, size);
        size = 0;
        for (int n = 0; n < nodeCount; n++) {
            final int c = nodeContainers[n];
            if (c >= 0 && (c & 1) != 0) {
                lower[size] = c >> 1;
                upper[size++] = n;
            }
        }
        for (int e = 0; e < edgeCount; e++) {
            final int c = edgeContainers[e];
            if (c >= 0) {
                lower[size] = c;
                upper[size++] = ~e;
            }
        }
        solidIsolated = group(solidCount, lower, upper, size);
    }

    /**
     * Returns the index of the primitive designated by the given signed reference.
     */
    static int index(final int ref) {
        return (ref < 0) ? ~ref : ref;
    }

    /**
     * Returns the container code of a node isolated in the given face.
     */
    static int faceContainer(final int face) {
        return face << 1;
    }

    /**
     * Returns the container code of a node isolated in the given solid.
     */
    static int solidContainer(final int solid) {
        return (solid << 1) | 1;
    }

    /**
     * Splits packed references into one array per part of each primitive, such as the rings of each face.
     */
    private static int[][][] split(final int count, final int[] partOffsets, final int[] refOffsets, final int[] refs) {
        final int[][][] parts = new int[count][][];
        for (int p = 0; p < count; p++) {
            final int first = partOffsets[p], last = partOffsets[p + 1];
            parts[p] = new int[last - first][];
            for (int r = first; r < last; r++) {
                parts[p][r - first] = Arrays.copyOfRange(refs, refOffsets[r], refOffsets[r + 1]);
            }
        }
        return parts;
    }

    /**
     * Returns the co-boundaries of the primitives used by the parts of higher primitives,
     * such as the edges used by the rings of faces.
     */
    private static int[][] transpose(final int count, final int[][][] higher) {
        int size = 0;
        for (final int[][] parts : higher) {
            for (final int[] part : parts) size += part.length;
        }
        final int[] lower = new int[size];
        final int[] upper = new int[size];
        size = 0;
        for (int p = 0; p < higher.length; p++) {
            for (final int[] part : higher[p]) {
                for (final int ref : part) {
                    lower[size] = index(ref);
                    upper[size++] = (ref < 0) ? ~p : p;
                }
            }
        }
        return group(count, lower, upper, size);
    }

    /**
     * Groups the {@code upper} values by their {@code lower} index, keeping their order.
     */
    private static int[][] group(final int count, final int[] lower, final int[] upper, final int size) {
        final int[] counts = new int[count];
        for (int k = 0; k < size; k++) {
            counts[lower[k]]++;
        }
        final int[][] groups = new int[count][];
        for (int i = 0; i < count; i++) {
            groups[i] = (counts[i] == 0) ? EMPTY : new int[counts[i]];
        }
        Arrays.fill(counts, 0);
        for (int k = 0; k < size; k++) {
            final int i = lower[k];
            groups[i][counts[i]++] = upper[k];
        }
        return groups;
    }

    /**
     * Returns a copy of the given array with the given value appended.
     */
    static int[] append(final int[] array, final int value) {
        final int[] result = Arrays.copyOf(array, array.length + 1);
        result[array.length] = value;
        return result;
    }

    /**
     * Returns a copy of the given array without the first occurrence of the given value,
     * or the given array if it does not contain the value.
     */
    static int[] remove(final int[] array, final int value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == value) {
                if (array.length == 1) return EMPTY;
                final int[] result = new int[array.length - 1];
                System.arraycopy(array, 0, result, 0, i);
                System.arraycopy(array, i + 1, result, i, array.length - i - 1);
                return result;
            }
        }
        return array;
    }

    // ──────────────────────────────────────────────────────────────────────────────────────────
    //  Ordering of co-boundaries
    // ──────────────────────────────────────────────────────────────────────────────────────────

    /**
     * Sorts the faces around the given edge in clockwise order seen from the end of the edge, in 3 dimensions.
     * The first face stays first, since a circular sequence has no other start.
     */
    final void sortAroundEdge(final int e) {
        final int[] co = edgeCo[e];
        if (dimension != 3 || co.length < 3) {
            return;                     // a circular sequence of two elements has a single order.
        }
        final double[] a = position(edgeNodes[2*e]);
        final double[] b = position(edgeNodes[2*e + 1]);
        final double[][] fins = new double[co.length][];
        for (int k = 0; k < co.length; k++) {
            fins[k] = finPoint(co[k], a, b);
        }
        sortClockwise(co, fins, new AroundEdge(a, b));
    }

    /**
     * Returns a node of the given face which is, in the plane of the face, on the same side of the edge
     * from {@code a} to {@code b} as the face. The half-plane bounded by the edge line and passing through
     * that node is the half-plane of the face near the edge. The node farthest from the edge is chosen,
     * which excludes the nodes on the edge line, and those on the other side when the face is not convex.
     *
     * @param  faceRef  signed reference to the face, positive if the face goes from {@code a} to {@code b}.
     */
    private double[] finPoint(final int faceRef, final double[] a, final double[] b) {
        final int face = index(faceRef);
        /*
         * A face going from a to b is on the left of the edge seen from above the face,
         * so the direction from the edge into the face is normal × (b − a).
         */
        final double[] n = faceNormal(face);
        final double dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
        double wx = n[1]*dz - n[2]*dy;
        double wy = n[2]*dx - n[0]*dz;
        double wz = n[0]*dy - n[1]*dx;
        if (faceRef < 0) {
            wx = -wx; wy = -wy; wz = -wz;
        }
        int found = -1;
        double farthest = 0;
        for (final int ref : faceRings[face][0]) {
            final int p = startNode(ref) * 3;
            final double offset = wx * (xyz[p] - a[0]) + wy * (xyz[p+1] - a[1]) + wz * (xyz[p+2] - a[2]);
            if (offset > farthest) {
                farthest = offset;
                found = p;
            }
        }
        if (found >= 0) {
            return Arrays.copyOfRange(xyz, found, found + 3);
        }
        return new double[] {a[0] + wx, a[1] + wy, a[2] + wz};       // degenerate face.
    }

    /**
     * Sorts the edges around the given node in clockwise order, in 2 dimensions.
     * The first edge stays first, since a circular sequence has no other start.
     */
    final void sortAroundNode(final int n) {
        final int[] co = nodeCo[n];
        if (dimension != 2 || co.length < 3) {
            return;                     // a circular sequence of two elements has a single order.
        }
        final double[][] ends = new double[co.length][];
        for (int k = 0; k < co.length; k++) {
            // A positive edge ends at this node, so its other end is its start.
            final int ref = co[k];
            ends[k] = position(edgeNodes[2 * index(ref) + (ref < 0 ? 1 : 0)]);
        }
        sortClockwise(co, ends, new AroundNode(position(n)));
    }

    /**
     * Returns the coordinates of the given node in three dimensions.
     */
    final double[] position(final int node) {
        return Arrays.copyOfRange(xyz, node * 3, node * 3 + 3);
    }

    /**
     * Sorts references in clockwise order of their directions, keeping the first one first.
     * Each direction is classified relative to the first one: same direction, in the first half turn
     * clockwise, opposite direction, or in the second half turn. Two directions in the same half turn
     * are less than half a turn apart, so their order is given by the turn from one to the other.
     * The sort is an insertion sort, since there are usually few directions.
     *
     * @param refs      the references to sort.
     * @param points    a point giving the direction of each reference, sorted together with them.
     * @param rotation  the orientation of the directions.
     */
    private static void sortClockwise(final int[] refs, final double[][] points, final Rotation rotation) {
        final int n = points.length;
        final int[] half = new int[n];
        for (int i = 1; i < n; i++) {
            final double turn = rotation.turn(points[0], points[i]);
            half[i] = (turn < 0) ? 1 : (turn > 0) ? 3 : rotation.opposite(points[0], points[i]) ? 2 : 0;
        }
        for (int i = 2; i < n; i++) {
            final int h = half[i];
            final double[] p = points[i];
            final int ref = refs[i];
            int j = i;
            while (j > 1 && (h < half[j-1] || (h == half[j-1] && rotation.turn(p, points[j-1]) < 0))) {
                half[j]   = half[j-1];
                points[j] = points[j-1];
                refs[j]   = refs[j-1];
                j--;
            }
            half[j]   = h;
            points[j] = p;
            refs[j]   = ref;
        }
    }

    /**
     * Orientation of directions around a center, each direction given by a point.
     */
    private interface Rotation {
        /**
         * Returns a negative value if the direction of {@code q} is clockwise from the direction of {@code p}
         * by less than half a turn, a positive value if counterclockwise by less than half a turn,
         * or zero if both directions are aligned.
         */
        double turn(double[] p, double[] q);

        /**
         * Returns whether two aligned directions are opposite.
         */
        boolean opposite(double[] p, double[] q);
    }

    /**
     * Directions around the edge from {@code a} to {@code b}, seen from {@code b}.
     */
    private static final class AroundEdge implements Rotation {
        private final double[] a, b;

        AroundEdge(final double[] a, final double[] b) {
            this.a = a;
            this.b = b;
        }

        /**
         * The turn from p to q is counterclockwise when q is on the side of the plane (a, b, p)
         * toward which the normal (b − a) × (p − a) points, where {@code orient3d} is negative.
         */
        @Override
        public double turn(final double[] p, final double[] q) {
            return -ShewchukPredicates.orient3d(a[0], a[1], a[2], b[0], b[1], b[2],
                                                p[0], p[1], p[2], q[0], q[1], q[2]);
        }

        /**
         * Returns whether the components of p − a and q − a perpendicular to the edge have opposite directions.
         */
        @Override
        public boolean opposite(final double[] p, final double[] q) {
            final double dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
            final double ux = p[0] - a[0], uy = p[1] - a[1], uz = p[2] - a[2];
            final double vx = q[0] - a[0], vy = q[1] - a[1], vz = q[2] - a[2];
            final double dd = dx*dx + dy*dy + dz*dz;
            return (ux*vx + uy*vy + uz*vz) * dd - (ux*dx + uy*dy + uz*dz) * (vx*dx + vy*dy + vz*dz) < 0;
        }
    }

    /**
     * Directions around the point {@code c} in two dimensions.
     */
    private static final class AroundNode implements Rotation {
        private final double[] c;

        AroundNode(final double[] c) {
            this.c = c;
        }

        /**
         * The turn from p to q is counterclockwise when c, p, q are in counterclockwise order.
         */
        @Override
        public double turn(final double[] p, final double[] q) {
            return ShewchukPredicates.orient2d(c[0], c[1], p[0], p[1], q[0], q[1]);
        }

        @Override
        public boolean opposite(final double[] p, final double[] q) {
            return (p[0] - c[0]) * (q[0] - c[0]) + (p[1] - c[1]) * (q[1] - c[1]) < 0;
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────────────────
    //  Low-level modifications, without verification. Used by ComplexEditor.
    // ──────────────────────────────────────────────────────────────────────────────────────────

    /**
     * Declares that this complex no longer matches the geometries from which it has been built.
     */
    private void edited() {
        sources = null;
    }

    /**
     * Adds a node without edge and without container.
     *
     * @param  position  the {@link #dimension} coordinates of the node.
     * @return index of the new node.
     */
    final int newNode(final double[] position) {
        if (nodeCount == nodeCo.length) {
            final int capacity = Math.max(8, nodeCount * 2);
            nodeCo         = Arrays.copyOf(nodeCo, capacity);
            nodeContainers = Arrays.copyOf(nodeContainers, capacity);
        }
        final int n = points.add(position);         // same as nodeCount.
        xyz = points.xyz;
        nodeCount++;
        nodeCo[n] = EMPTY;
        nodeContainers[n] = -1;
        edited();
        return n;
    }

    /**
     * Changes the coordinates of a node.
     */
    final void moveNode(final int n, final double[] position) {
        points.setCoordinates(n, position);
        edited();
    }

    /**
     * Adds an edge between the given nodes, without face and without container.
     *
     * @return index of the new edge.
     */
    final int newEdge(final int start, final int end) {
        if (edgeCount == edgeCo.length) {
            final int capacity = Math.max(8, edgeCount * 2);
            edgeNodes      = Arrays.copyOf(edgeNodes, capacity * 2);
            edgeCo         = Arrays.copyOf(edgeCo, capacity);
            edgeContainers = Arrays.copyOf(edgeContainers, capacity);
        }
        final int e = edgeCount++;
        edgeNodes[2*e]     = start;
        edgeNodes[2*e + 1] = end;
        edgeCo[e] = EMPTY;
        edgeContainers[e] = -1;
        attachEdge(e);
        edited();
        return e;
    }

    /**
     * Changes the nodes of an edge, updating the co-boundaries of the nodes.
     */
    final void setEdgeNodes(final int e, final int start, final int end) {
        detachEdge(e);
        edgeNodes[2*e]     = start;
        edgeNodes[2*e + 1] = end;
        attachEdge(e);
        edited();
    }

    private void attachEdge(final int e) {
        final int start = edgeNodes[2*e], end = edgeNodes[2*e + 1];
        nodeCo[start] = append(nodeCo[start], ~e);
        nodeCo[end]   = append(nodeCo[end], e);
        sortAroundNode(start);
        sortAroundNode(end);
    }

    private void detachEdge(final int e) {
        final int start = edgeNodes[2*e], end = edgeNodes[2*e + 1];
        nodeCo[start] = remove(nodeCo[start], ~e);
        nodeCo[end]   = remove(nodeCo[end], e);
    }

    /**
     * Adds a face of the given rings of signed edge references, without solid.
     *
     * @return index of the new face.
     */
    final int newFace(final int[][] rings) {
        if (faceCount == faceCo.length) {
            final int capacity = Math.max(8, faceCount * 2);
            faceRings    = Arrays.copyOf(faceRings, capacity);
            faceCo       = Arrays.copyOf(faceCo, capacity);
            faceIsolated = Arrays.copyOf(faceIsolated, capacity);
        }
        final int f = faceCount++;
        faceRings[f]    = rings;
        faceCo[f]       = EMPTY;
        faceIsolated[f] = EMPTY;
        appendFace(f);
        sortAroundFace(f);
        edited();
        return f;
    }

    /**
     * Changes the rings of the given faces, updating the co-boundaries of the edges.
     * All rings are changed before the faces around the edges are sorted, since the sort uses the rings.
     *
     * @param faces  indices of the faces to change.
     * @param rings  the new rings of each face.
     */
    final void setFaceRings(final int[] faces, final int[][][] rings) {
        for (final int f : faces) {
            detachFace(f);
        }
        for (int i = 0; i < faces.length; i++) {
            faceRings[faces[i]] = rings[i];
        }
        for (final int f : faces) {
            appendFace(f);
        }
        for (final int f : faces) {
            sortAroundFace(f);
        }
        edited();
    }

    /**
     * Adds the given face to the co-boundaries of its edges, without sorting them.
     */
    private void appendFace(final int f) {
        for (final int[] ring : faceRings[f]) {
            for (final int ref : ring) {
                final int e = index(ref);
                edgeCo[e] = append(edgeCo[e], (ref < 0) ? ~f : f);
            }
        }
    }

    /**
     * Sorts the faces around each edge of the given face.
     */
    private void sortAroundFace(final int f) {
        for (final int[] ring : faceRings[f]) {
            for (final int ref : ring) {
                sortAroundEdge(index(ref));
            }
        }
    }

    private void detachFace(final int f) {
        for (final int[] ring : faceRings[f]) {
            for (final int ref : ring) {
                final int e = index(ref);
                edgeCo[e] = remove(edgeCo[e], (ref < 0) ? ~f : f);
            }
        }
    }

    /**
     * Adds a solid of the given shells of signed face references.
     *
     * @return index of the new solid.
     */
    final int newSolid(final int[][] shells) {
        if (solidCount == solidShells.length) {
            final int capacity = Math.max(8, solidCount * 2);
            solidShells   = Arrays.copyOf(solidShells, capacity);
            solidIsolated = Arrays.copyOf(solidIsolated, capacity);
        }
        final int s = solidCount++;
        solidShells[s]   = shells;
        solidIsolated[s] = EMPTY;
        attachSolid(s);
        edited();
        return s;
    }

    /**
     * Changes the shells of a solid, updating the co-boundaries of the faces.
     */
    final void setShells(final int s, final int[][] shells) {
        detachSolid(s);
        solidShells[s] = shells;
        attachSolid(s);
        edited();
    }

    private void attachSolid(final int s) {
        for (final int[] shell : solidShells[s]) {
            for (final int ref : shell) {
                final int f = index(ref);
                faceCo[f] = append(faceCo[f], (ref < 0) ? ~s : s);
            }
        }
    }

    private void detachSolid(final int s) {
        for (final int[] shell : solidShells[s]) {
            for (final int ref : shell) {
                final int f = index(ref);
                faceCo[f] = remove(faceCo[f], (ref < 0) ? ~s : s);
            }
        }
    }

    /**
     * Sets the face or solid in which a node is isolated, as a container code or −1 for none.
     */
    final void setNodeContainer(final int n, final int code) {
        final int old = nodeContainers[n];
        if (old != code) {
            if (old >= 0) {
                if ((old & 1) == 0) faceIsolated[old >> 1]  = remove(faceIsolated[old >> 1], n);
                else                solidIsolated[old >> 1] = remove(solidIsolated[old >> 1], n);
            }
            nodeContainers[n] = code;
            if (code >= 0) {
                if ((code & 1) == 0) faceIsolated[code >> 1]  = append(faceIsolated[code >> 1], n);
                else                 solidIsolated[code >> 1] = append(solidIsolated[code >> 1], n);
            }
        }
    }

    /**
     * Sets the solid in which an edge is isolated, or −1 for none.
     */
    final void setEdgeContainer(final int e, final int solid) {
        final int old = edgeContainers[e];
        if (old != solid) {
            if (old >= 0) solidIsolated[old] = remove(solidIsolated[old], ~e);
            edgeContainers[e] = solid;
            if (solid >= 0) solidIsolated[solid] = append(solidIsolated[solid], ~e);
        }
    }

    /**
     * Removes a node, which shall not be the end of any edge.
     */
    final void removeNode(final int n) {
        setNodeContainer(n, -1);
        removedNodes.set(n);
        edited();
    }

    /**
     * Removes an edge, which shall not be on the boundary of any face.
     */
    final void removeEdge(final int e) {
        detachEdge(e);
        setEdgeContainer(e, -1);
        removedEdges.set(e);
        edited();
    }

    /**
     * Removes a face, which shall not be on the boundary of any solid.
     * The nodes isolated in the face are no longer isolated.
     */
    final void removeFace(final int f) {
        detachFace(f);
        for (final int n : faceIsolated[f]) {
            nodeContainers[n] = -1;
        }
        faceIsolated[f] = EMPTY;
        faceRings[f] = new int[0][];
        removedFaces.set(f);
        edited();
    }

    /**
     * Removes a solid. The nodes and edges isolated in the solid are no longer isolated.
     */
    final void removeSolid(final int s) {
        detachSolid(s);
        for (final int v : solidIsolated[s]) {
            if (v >= 0) nodeContainers[v] = -1;
            else        edgeContainers[~v] = -1;
        }
        solidIsolated[s] = EMPTY;
        solidShells[s] = new int[0][];
        removedSolids.set(s);
        edited();
    }

    // ──────────────────────────────────────────────────────────────────────────────────────────
    //  Queries on the storage
    // ──────────────────────────────────────────────────────────────────────────────────────────

    /**
     * Returns the normal of the given face computed with Newell's method on its exterior ring.
     * The face is seen counterclockwise from the side toward which the normal points.
     * The normal is not normalized: its length is twice the area of the exterior ring.
     */
    final double[] faceNormal(final int face) {
        final double[] normal = new double[3];
        for (final int ref : faceRings[face][0]) {
            final int a = startNode(ref) * 3;
            final int b = endNode(ref) * 3;
            normal[0] += (xyz[a+1] - xyz[b+1]) * (xyz[a+2] + xyz[b+2]);
            normal[1] += (xyz[a+2] - xyz[b+2]) * (xyz[a]   + xyz[b]);
            normal[2] += (xyz[a]   - xyz[b])   * (xyz[a+1] + xyz[b+1]);
        }
        return normal;
    }

    /**
     * Returns the node where the given oriented edge starts.
     */
    final int startNode(final int edgeRef) {
        return edgeNodes[2 * index(edgeRef) + (edgeRef < 0 ? 1 : 0)];
    }

    /**
     * Returns the node where the given oriented edge ends.
     */
    final int endNode(final int edgeRef) {
        return edgeNodes[2 * index(edgeRef) + (edgeRef < 0 ? 0 : 1)];
    }

    /**
     * Returns the nodes of the given ring of signed edge references in traversal order, without repeating the first one.
     */
    final int[] nodes(final int[] ring) {
        final int[] nodes = new int[ring.length];
        for (int i = 0; i < ring.length; i++) {
            nodes[i] = startNode(ring[i]);
        }
        return nodes;
    }

    /**
     * Returns the indices of the faces of all shells of the given solid.
     */
    final int[] faces(final int solid) {
        int count = 0;
        for (final int[] shell : solidShells[solid]) count += shell.length;
        final int[] faces = new int[count];
        count = 0;
        for (final int[] shell : solidShells[solid]) {
            for (final int ref : shell) faces[count++] = index(ref);
        }
        return faces;
    }

    /**
     * Returns the number of primitives of the given dimension, including the removed ones.
     */
    private int count(final int dimension) {
        switch (dimension) {
            case 0:  return nodeCount;
            case 1:  return edgeCount;
            case 2:  return faceCount;
            default: return solidCount;
        }
    }

    /**
     * Returns the removed primitives of the given dimension.
     */
    final BitSet removed(final int dimension) {
        switch (dimension) {
            case 0:  return removedNodes;
            case 1:  return removedEdges;
            case 2:  return removedFaces;
            default: return removedSolids;
        }
    }

    /**
     * Returns the number of primitives of the given dimension which have not been removed.
     */
    private int liveCount(final int dimension) {
        return count(dimension) - removed(dimension).cardinality();
    }

    // ──────────────────────────────────────────────────────────────────────────────────────────
    //  Public API
    // ──────────────────────────────────────────────────────────────────────────────────────────

    /**
     * Returns the coordinate reference system of the node coordinates.
     *
     * @return the coordinate reference system, or {@code null} if this complex is empty.
     */
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return crs;
    }

    /**
     * Returns the number of node indices, including those of removed nodes.
     *
     * @return number of node indices.
     */
    public int getNodeCount() {
        return nodeCount;
    }

    /**
     * Returns the number of edge indices, including those of removed edges.
     *
     * @return number of edge indices.
     */
    public int getEdgeCount() {
        return edgeCount;
    }

    /**
     * Returns the number of face indices, including those of removed faces.
     *
     * @return number of face indices.
     */
    public int getFaceCount() {
        return faceCount;
    }

    /**
     * Returns the number of solid indices, including those of removed solids.
     *
     * @return number of solid indices.
     */
    public int getSolidCount() {
        return solidCount;
    }

    /**
     * Returns the node at the given index.
     *
     * @param  index  index of the node, from 0 inclusive to {@link #getNodeCount()} exclusive.
     * @return the node at the given index, or {@code null} if it has been removed.
     */
    public Node getNode(final int index) {
        return removedNodes.get(Objects.checkIndex(index, nodeCount)) ? null : new IndexedNode(this, index);
    }

    /**
     * Returns the edge at the given index.
     *
     * @param  index  index of the edge, from 0 inclusive to {@link #getEdgeCount()} exclusive.
     * @return the edge at the given index, or {@code null} if it has been removed.
     */
    public Edge getEdge(final int index) {
        return removedEdges.get(Objects.checkIndex(index, edgeCount)) ? null : new IndexedEdge(this, index);
    }

    /**
     * Returns the face at the given index.
     *
     * @param  index  index of the face, from 0 inclusive to {@link #getFaceCount()} exclusive.
     * @return the face at the given index, or {@code null} if it has been removed.
     */
    public Face getFace(final int index) {
        return removedFaces.get(Objects.checkIndex(index, faceCount)) ? null : new IndexedFace(this, index);
    }

    /**
     * Returns the solid at the given index.
     *
     * @param  index  index of the solid, from 0 inclusive to {@link #getSolidCount()} exclusive.
     * @return the solid at the given index, or {@code null} if it has been removed.
     */
    public Solid getSolid(final int index) {
        return removedSolids.get(Objects.checkIndex(index, solidCount)) ? null : new IndexedSolid(this, index);
    }

    /**
     * Returns the coordinates of the node at the given index.
     *
     * @param  node  index of the node, from 0 inclusive to {@link #getNodeCount()} exclusive.
     * @return a copy of the coordinates of the node.
     */
    public double[] getCoordinates(final int node) {
        Objects.checkIndex(node, nodeCount);
        return Arrays.copyOfRange(points.coordinates, node * dimension, (node + 1) * dimension);
    }

    /**
     * Returns the positions and other attributes of the nodes, as one point per node index.
     * The points of removed nodes are included. The attributes other than positions can be modified
     * through the returned object. The positions shall be modified with {@link ComplexEditor}.
     *
     * @return the points of all nodes.
     */
    public NodePoints getNodePoints() {
        return points;
    }

    /**
     * Returns the oriented node designated by the given signed reference.
     */
    final DirectedNode directedNode(final int ref) {
        final Node node = new IndexedNode(this, index(ref));
        return (ref < 0) ? node.getNegate() : node;
    }

    /**
     * Returns the oriented edge designated by the given signed reference.
     */
    final DirectedEdge directedEdge(final int ref) {
        final Edge edge = new IndexedEdge(this, index(ref));
        return (ref < 0) ? edge.getNegate() : edge;
    }

    /**
     * Returns the oriented face designated by the given signed reference.
     */
    final DirectedFace directedFace(final int ref) {
        final Face face = new IndexedFace(this, index(ref));
        return (ref < 0) ? face.getNegate() : face;
    }

    /**
     * Returns the oriented solid designated by the given signed reference.
     */
    final DirectedSolid directedSolid(final int ref) {
        final Solid solid = new IndexedSolid(this, index(ref));
        return (ref < 0) ? solid.getNegate() : solid;
    }

    /**
     * Returns the edges starting or ending at the given node, for the given orientation of that node.
     */
    final List<DirectedEdge> nodeCoBoundary(final int node, final boolean negate) {
        final int[] co = nodeCo[node];
        final DirectedEdge[] edges = new DirectedEdge[co.length];
        for (int i = 0; i < co.length; i++) {
            edges[i] = directedEdge(negate ? ~co[i] : co[i]);
        }
        return List.of(edges);
    }

    /**
     * Returns the faces around the given edge, for the given orientation of that edge.
     * The opposite edge is seen from its other end, which reverses the clockwise order.
     */
    final List<DirectedFace> edgeCoBoundary(final int edge, final boolean negate) {
        final int[] co = edgeCo[edge];
        final DirectedFace[] faces = new DirectedFace[co.length];
        for (int i = 0; i < co.length; i++) {
            if (negate) {
                faces[co.length - 1 - i] = directedFace(~co[i]);
            } else {
                faces[i] = directedFace(co[i]);
            }
        }
        return List.of(faces);
    }

    /**
     * Returns the solids on each side of the given face, for the given orientation of that face.
     */
    final List<DirectedSolid> faceCoBoundary(final int face, final boolean negate) {
        final int[] co = faceCo[face];
        final DirectedSolid[] solids = new DirectedSolid[co.length];
        for (int i = 0; i < co.length; i++) {
            solids[i] = directedSolid(negate ? ~co[i] : co[i]);
        }
        return List.of(solids);
    }

    /**
     * Returns the oriented edges of a ring of the given face. The opposite of a ring is traversed in reverse order.
     */
    final List<DirectedEdge> ring(final int face, final int ring, final boolean negate) {
        final int[] refs = faceRings[face][ring];
        final DirectedEdge[] edges = new DirectedEdge[refs.length];
        for (int i = 0; i < refs.length; i++) {
            if (negate) {
                edges[refs.length - 1 - i] = directedEdge(~refs[i]);
            } else {
                edges[i] = directedEdge(refs[i]);
            }
        }
        return List.of(edges);
    }

    /**
     * Returns the oriented edges of all rings of the given face, the exterior ring first.
     */
    final List<DirectedEdge> faceBoundary(final int face, final boolean negate) {
        final List<DirectedEdge> edges = new ArrayList<>();
        for (int r = 0; r < faceRings[face].length; r++) {
            edges.addAll(ring(face, r, negate));
        }
        return Collections.unmodifiableList(edges);
    }

    /**
     * Returns the rings of the given face other than the exterior one.
     */
    final List<List<DirectedEdge>> interiorRings(final int face) {
        final List<List<DirectedEdge>> rings = new ArrayList<>();
        for (int r = 1; r < faceRings[face].length; r++) {
            rings.add(ring(face, r, false));
        }
        return Collections.unmodifiableList(rings);
    }

    /**
     * Returns the oriented faces of a shell of the given solid.
     */
    final List<DirectedFace> shell(final int solid, final int shell, final boolean negate) {
        final int[] refs = solidShells[solid][shell];
        final DirectedFace[] faces = new DirectedFace[refs.length];
        for (int i = 0; i < refs.length; i++) {
            faces[i] = directedFace(negate ? ~refs[i] : refs[i]);
        }
        return List.of(faces);
    }

    /**
     * Returns the oriented faces of all shells of the given solid, the exterior shell first.
     */
    final List<DirectedFace> solidBoundary(final int solid, final boolean negate) {
        final List<DirectedFace> faces = new ArrayList<>();
        for (int s = 0; s < solidShells[solid].length; s++) {
            faces.addAll(shell(solid, s, negate));
        }
        return Collections.unmodifiableList(faces);
    }

    /**
     * Returns the shells of the given solid other than the exterior one.
     */
    final List<List<DirectedFace>> interiorShells(final int solid) {
        final List<List<DirectedFace>> shells = new ArrayList<>();
        for (int s = 1; s < solidShells[solid].length; s++) {
            shells.add(shell(solid, s, false));
        }
        return Collections.unmodifiableList(shells);
    }

    /**
     * Returns the face or solid in which the given primitive is isolated, or {@code null} if none.
     *
     * @param  dimension  dimension of the primitive: 0 for a node, 1 for an edge.
     * @param  index      index of the primitive.
     */
    final Primitive container(final int dimension, final int index) {
        switch (dimension) {
            case 0: {
                final int c = nodeContainers[index];
                if (c < 0) return null;
                return ((c & 1) == 0) ? new IndexedFace(this, c >> 1) : new IndexedSolid(this, c >> 1);
            }
            case 1: {
                final int c = edgeContainers[index];
                return (c < 0) ? null : new IndexedSolid(this, c);
            }
            default: return null;
        }
    }

    /**
     * Returns the primitives isolated in the given face or solid.
     *
     * @param  dimension  dimension of the primitive: 2 for a face, 3 for a solid.
     * @param  index      index of the primitive.
     */
    final Set<Primitive> isolated(final int dimension, final int index) {
        final int[] values;
        switch (dimension) {
            case 2:  values = faceIsolated[index];  break;
            case 3:  values = solidIsolated[index]; break;
            default: return Set.of();
        }
        final Set<Primitive> isolated = new LinkedHashSet<>();
        for (final int v : values) {
            isolated.add((v >= 0) ? new IndexedNode(this, v) : new IndexedEdge(this, ~v));
        }
        return Collections.unmodifiableSet(isolated);
    }

    /**
     * Returns a copy of the points of the given nodes with all their attributes,
     * followed by the point of the first node if {@code close} is true.
     */
    private DataPoints pointsOf(final int[] nodes, final boolean close) {
        return points.subset(close ? IndexedComplex.append(nodes, nodes[0]) : nodes);
    }

    /**
     * Returns the geometric realization of the given node, with all its attributes.
     */
    final Point point(final int node) {
        return GeometryFactory.DEFAULT.createPoint(pointsOf(new int[] {node}, false));
    }

    /**
     * Returns the geometric realization of an edge, as a segment between the given nodes with all their attributes.
     */
    final LineString lineString(final int start, final int end) {
        return GeometryFactory.DEFAULT.createLineString(pointsOf(new int[] {start, end}, false));
    }

    /**
     * Returns the geometric realization of the given face, with reversed rings if {@code reverse} is true.
     * A face of three nodes without hole is realized by a triangle.
     */
    final Polygon polygon(final int face, final boolean reverse) {
        final GeometryFactory f = GeometryFactory.DEFAULT;
        LinearRing exterior = null;
        int exteriorSize = 0;
        final List<LinearRing> interiors = new ArrayList<>();
        for (final int[] refs : faceRings[face]) {
            final int[] nodes = nodes(refs);
            if (reverse) {
                for (int i = 0, j = nodes.length - 1; i < j; i++, j--) {
                    final int t = nodes[i]; nodes[i] = nodes[j]; nodes[j] = t;
                }
            }
            final LinearRing ring = f.createLinearRing(pointsOf(nodes, true));
            if (exterior == null) {
                exterior = ring;
                exteriorSize = nodes.length;
            } else {
                interiors.add(ring);
            }
        }
        if (interiors.isEmpty() && exteriorSize == 3) {
            return f.createTriangle(exterior);
        }
        return f.createPolygon(exterior, interiors);
    }

    /**
     * Returns the geometric realization of the given solid, with each face oriented so that the solid is below it.
     */
    final Polyhedron polyhedron(final int solid) {
        final GeometryFactory f = GeometryFactory.DEFAULT;
        GeometryCollection<Polygon> exterior = null;
        final List<GeometryCollection<Polygon>> interiors = new ArrayList<>();
        for (final int[] refs : solidShells[solid]) {
            final Polygon[] polygons = new Polygon[refs.length];
            for (int i = 0; i < refs.length; i++) {
                polygons[i] = polygon(index(refs[i]), refs[i] < 0);
            }
            final GeometryCollection<Polygon> shell = f.createMultiPolygon(crs, polygons);
            if (exterior == null) {
                exterior = shell;
            } else {
                interiors.add(shell);
            }
        }
        return f.createPolyhedron(exterior, interiors);
    }

    /**
     * Returns {@code true} if this complex has no primitive.
     */
    @Override
    public boolean isEmpty() {
        return liveCount(0) == 0;
    }

    /**
     * Returns the largest dimension of the primitives of this complex, or −1 if it is empty.
     */
    @Override
    public int getDimension() {
        for (int d = 3; d >= 0; d--) {
            if (liveCount(d) != 0) return d;
        }
        return -1;
    }

    /**
     * Returns the sum of the primitives which are on the boundary of no other primitive.
     */
    private DefaultExpression topCells() {
        final List<DirectedTopo> cells = new ArrayList<>();
        for (int i = 0; i < solidCount; i++) {
            if (!removedSolids.get(i)) cells.add(new IndexedSolid(this, i));
        }
        for (int i = 0; i < faceCount; i++) {
            if (!removedFaces.get(i) && faceCo[i].length == 0) cells.add(new IndexedFace(this, i));
        }
        for (int i = 0; i < edgeCount; i++) {
            if (!removedEdges.get(i) && edgeCo[i].length == 0) cells.add(new IndexedEdge(this, i));
        }
        for (int i = 0; i < nodeCount; i++) {
            if (!removedNodes.get(i) && nodeCo[i].length == 0) cells.add(new IndexedNode(this, i));
        }
        return DefaultExpression.sum(cells);
    }

    /**
     * Returns the algebraic boundary of this complex: the boundary of the sum of its primitives which are
     * on the boundary of no other primitive. Faces shared by two solids, or edges shared by two faces of
     * consistent orientations, cancel out. Each term is listed once, with the sign of its coefficient.
     */
    @Override
    public List<DirectedTopo> getBoundary() {
        return topCells().getBoundary().toDirectedList();
    }

    /**
     * Returns an empty list: a maximal complex is on the boundary of nothing.
     */
    @Override
    public List<DirectedTopo> getCoBoundary() {
        return List.of();
    }

    /**
     * Returns the primitives of this complex which are neither on its boundary nor on the boundary of it.
     */
    @Override
    public Set<Primitive> getInterior() {
        final Set<Primitive> boundary = closure(getBoundary());
        final Set<Primitive> interior = new LinkedHashSet<>();
        for (final Primitive element : getElements()) {
            if (!boundary.contains(element)) {
                interior.add(element);
            }
        }
        return interior;
    }

    /**
     * Returns an empty set: every primitive of a maximal complex is in its interior or on its boundary.
     */
    @Override
    public Set<Primitive> getExterior() {
        return Set.of();
    }

    /**
     * Returns all primitives of this complex.
     */
    @Override
    public Set<Primitive> getClosure() {
        return getElements();
    }

    /**
     * Returns the primitives of the given oriented primitives and of their boundaries, recursively.
     */
    static Set<Primitive> closure(final Collection<? extends DirectedTopo> start) {
        final Set<Primitive> closure = new LinkedHashSet<>();
        final Deque<DirectedTopo> pending = new ArrayDeque<>(start);
        DirectedTopo next;
        while ((next = pending.poll()) != null) {
            if (closure.add(next.getTopo())) {
                pending.addAll(next.getBoundary());
            }
        }
        return closure;
    }

    /**
     * Returns this complex, which is maximal.
     */
    @Override
    public Complex getMaximalComplex() {
        return this;
    }

    /**
     * Returns all primitives of this complex which have not been removed: nodes, then edges, then faces, then solids.
     */
    @Override
    public Set<Primitive> getElements() {
        return new Elements();
    }

    /**
     * Returns the primitive of the given dimension and index.
     */
    private Primitive element(final int dimension, final int i) {
        switch (dimension) {
            case 0:  return new IndexedNode(this, i);
            case 1:  return new IndexedEdge(this, i);
            case 2:  return new IndexedFace(this, i);
            default: return new IndexedSolid(this, i);
        }
    }

    /**
     * View of all primitives of this complex which have not been removed.
     */
    private final class Elements extends AbstractSet<Primitive> {
        @Override
        public int size() {
            return liveCount(0) + liveCount(1) + liveCount(2) + liveCount(3);
        }

        @Override
        public boolean contains(final Object obj) {
            return (obj instanceof IndexedTopo t) && t.complex == IndexedComplex.this
                    && !t.isNegative() && !removed(t.getDimension()).get(t.index);
        }

        @Override
        public Iterator<Primitive> iterator() {
            return new Iterator<>() {
                private int kind, index = -1;
                private Primitive next = advance();

                /** Returns the next primitive which has not been removed, or {@code null} if none. */
                private Primitive advance() {
                    while (kind <= 3) {
                        index = removed(kind).nextClearBit(index + 1);
                        if (index < count(kind)) {
                            return element(kind, index);
                        }
                        kind++;
                        index = -1;
                    }
                    return null;
                }

                @Override
                public boolean hasNext() {
                    return next != null;
                }

                @Override
                public Primitive next() {
                    final Primitive current = next;
                    if (current == null) {
                        throw new NoSuchElementException();
                    }
                    next = advance();
                    return current;
                }
            };
        }
    }

    /**
     * Returns {@code true} if every primitive is linked to every other one. Edges link their nodes,
     * faces link their rings, solids link their shells, and isolated primitives are linked to their container.
     */
    @Override
    public boolean isConnected() {
        final int[] parent = new int[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            parent[i] = i;
        }
        for (int e = 0; e < edgeCount; e++) {
            if (!removedEdges.get(e)) {
                union(parent, edgeNodes[2*e], edgeNodes[2*e + 1]);
                if (edgeContainers[e] >= 0) {
                    union(parent, edgeNodes[2*e], solidNode(edgeContainers[e]));
                }
            }
        }
        for (int f = 0; f < faceCount; f++) {
            if (!removedFaces.get(f)) {
                for (final int[] ring : faceRings[f]) {
                    union(parent, faceNode(f), startNode(ring[0]));
                }
            }
        }
        for (int s = 0; s < solidCount; s++) {
            if (!removedSolids.get(s)) {
                for (final int[] shell : solidShells[s]) {
                    union(parent, solidNode(s), faceNode(index(shell[0])));
                }
            }
        }
        for (int n = 0; n < nodeCount; n++) {
            final int c = nodeContainers[n];
            if (c >= 0) {
                union(parent, n, ((c & 1) == 0) ? faceNode(c >> 1) : solidNode(c >> 1));
            }
        }
        int roots = 0;
        for (int i = 0; i < nodeCount; i++) {
            if (!removedNodes.get(i) && find(parent, i) == i && ++roots > 1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a node of the given face.
     */
    private int faceNode(final int face) {
        return startNode(faceRings[face][0][0]);
    }

    /**
     * Returns a node of the given solid.
     */
    private int solidNode(final int solid) {
        return faceNode(index(solidShells[solid][0][0]));
    }

    private static int find(final int[] parent, int i) {
        while (parent[i] != i) {
            parent[i] = parent[parent[i]];
            i = parent[i];
        }
        return i;
    }

    private static void union(final int[] parent, final int a, final int b) {
        parent[find(parent, a)] = find(parent, b);
    }

    /**
     * Returns this complex alone: it has no sub-complex other than itself.
     */
    @Override
    public Set<Complex> getSubComplexes() {
        return Set.of(this);
    }

    /**
     * Returns this complex alone: a maximal complex is contained in no other complex.
     */
    @Override
    public Set<Complex> getSuperComplexes() {
        return Set.of(this);
    }

    /**
     * Returns the geometric realization of this complex. Before any edit, this is the geometries from which
     * this complex has been built: the single geometry given to the builder, or a collection of them.
     * After an edit, this is a collection of the realizations of the primitives which are on the boundary
     * of no other primitive. The result is {@code null} if there is no geometry.
     */
    @Override
    public Geometry getGeometry() {
        List<Geometry> parts = sources;
        if (parts == null) {
            parts = new ArrayList<>();
            for (int i = 0; i < solidCount; i++) {
                if (!removedSolids.get(i)) parts.add(polyhedron(i));
            }
            for (int i = 0; i < faceCount; i++) {
                if (!removedFaces.get(i) && faceCo[i].length == 0) parts.add(polygon(i, false));
            }
            for (int i = 0; i < edgeCount; i++) {
                if (!removedEdges.get(i) && edgeCo[i].length == 0) parts.add(lineString(edgeNodes[2*i], edgeNodes[2*i + 1]));
            }
            for (int i = 0; i < nodeCount; i++) {
                if (!removedNodes.get(i) && nodeCo[i].length == 0) parts.add(point(i));
            }
        }
        switch (parts.size()) {
            case 0:  return null;
            case 1:  return parts.get(0);
            default: return GeometryFactory.DEFAULT.createGeometryCollection(crs, parts.toArray(Geometry[]::new));
        }
    }

    @Override
    public String toString() {
        return "IndexedComplex[nodes=" + liveCount(0) + ", edges=" + liveCount(1)
                + ", faces=" + liveCount(2) + ", solids=" + liveCount(3) + ']';
    }
}
