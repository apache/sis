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

import java.util.List;
import java.util.Set;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.operation.polyhedron.SolidFixtures;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.geometries.topology.DirectedFace;
import org.apache.sis.geometries.topology.Edge;
import org.apache.sis.geometries.topology.Face;
import org.apache.sis.geometries.topology.Node;
import org.apache.sis.geometries.topology.Solid;
import static org.apache.sis.geometries.internal.shared.topology.ComplexBuilderTest.assertClosed;
import static org.apache.sis.geometries.internal.shared.topology.ComplexBuilderTest.build;
import static org.apache.sis.geometries.internal.shared.topology.ComplexBuilderTest.line;
import static org.apache.sis.geometries.internal.shared.topology.ComplexBuilderTest.polygon;
import static org.apache.sis.geometries.internal.shared.topology.ComplexBuilderTest.positions;
import static org.apache.sis.geometries.internal.shared.topology.ComplexBuilderTest.volume;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link ComplexEditor}. Most tests start from the square from (0,0) to (4,4), whose nodes 0 to 3
 * are its corners in counterclockwise order from the origin, and whose edge <var>i</var> goes from node
 * <var>i</var> to the next node.
 *
 * @author Johann Sorel (Geomatys)
 */
public class ComplexEditorTest {

    private static final double EPS = 1E-9;

    private static final CoordinateReferenceSystem CRS2D = Geometries.getUndefinedCRS(2);

    private static final CoordinateReferenceSystem CRS3D = SolidFixtures.CRS;

    /**
     * Returns the complex of the square from (0,0) to (4,4).
     */
    private static IndexedComplex square() {
        return build(polygon(CRS2D, 0,0, 4,0, 4,4, 0,4, 0,0));
    }

    /**
     * Returns the coordinates of the given node.
     */
    private static double[] coordinates(final Node node) {
        return ((Point) node.getGeometry()).getPosition().toArrayDouble();
    }

    /**
     * Isolated nodes are added in the face which contains them, moved, and removed.
     */
    @Test
    public void testIsolatedNodes() {
        final IndexedComplex complex = square();
        final ComplexEditor editor = new ComplexEditor(complex);
        final int inside  = editor.addIsolatedNode(1, 1);
        final int outside = editor.addIsolatedNode(6, 6);
        assertEquals(complex.getFace(0), complex.getNode(inside).getContainer());
        assertNull(complex.getNode(outside).getContainer());
        assertThrows(IllegalArgumentException.class, () -> editor.addIsolatedNode(2, 0), "Inside an edge.");
        assertThrows(IllegalArgumentException.class, () -> editor.addIsolatedNode(4, 4), "On a node.");
        assertThrows(IllegalArgumentException.class, () -> editor.addIsolatedNode(1, 2, 3), "Wrong dimension.");

        editor.moveIsolatedNode(inside, 7, 7);
        assertNull(complex.getNode(inside).getContainer());
        assertTrue(complex.getFace(0).getIsolated().isEmpty());

        editor.removeIsolatedNode(outside);
        assertNull(complex.getNode(outside));
        assertEquals(5 + 4 + 1, complex.getElements().size());
        assertThrows(IllegalArgumentException.class, () -> editor.removeIsolatedNode(0), "The end of edges.");
    }

    /**
     * Edges are added between nodes, unless they cross an edge or are inside a face, and removed.
     */
    @Test
    public void testEdges() {
        final IndexedComplex complex = square();
        final ComplexEditor editor = new ComplexEditor(complex);
        final int a = editor.addIsolatedNode(6, 0);
        final int b = editor.addIsolatedNode(6, 4);
        final int edge = editor.addEdge(a, b);
        assertEquals(5, complex.getEdgeCount());
        assertEquals(1, complex.getNode(a).getCoBoundary().size());
        assertThrows(IllegalArgumentException.class, () -> editor.addEdge(a, b), "Already an edge.");

        final int c = editor.addIsolatedNode(5, 2);
        final int d = editor.addIsolatedNode(7, 2);
        assertThrows(IllegalArgumentException.class, () -> editor.addEdge(c, d), "Crossing an edge.");
        final int g = editor.addIsolatedNode(1, 1);
        final int h = editor.addIsolatedNode(3, 1);
        assertThrows(IllegalArgumentException.class, () -> editor.addEdge(g, h), "Inside the face.");
        assertThrows(IllegalArgumentException.class, () -> editor.addEdge(0, 2), "Diagonal of the face.");

        editor.removeEdge(edge);
        assertNull(complex.getEdge(edge));
        assertNotNull(complex.getNode(a));
        assertTrue(complex.getNode(a).getCoBoundary().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> editor.removeEdge(0), "On the boundary of a face.");
    }

    /**
     * Splitting an edge of a face inserts a node in the ring of the face, and healing restores the edge.
     */
    @Test
    public void testSplitAndHealEdge() {
        final IndexedComplex complex = square();
        final ComplexEditor editor = new ComplexEditor(complex);
        final int node = editor.splitEdge(0, 2, 0);
        final int added = complex.getEdgeCount() - 1;
        final Face face = complex.getFace(0);
        assertEquals(5, face.getBoundary().size());
        assertTrue(face.getExpression().getBoundary().getBoundary().isZero());
        assertEquals(6, ((Polygon) face.getGeometry()).getExteriorRing().getDataPoints().size());
        assertEquals(Set.of(complex.getEdge(0), complex.getEdge(added).getNegate()),
                     Set.copyOf(complex.getNode(node).getCoBoundary()), "Ending at the node, then starting from it.");
        assertEquals(List.of(face), complex.getEdge(added).getCoBoundary());
        assertThrows(IllegalArgumentException.class, () -> editor.splitEdge(1, 5, 1), "Not on the edge.");
        assertThrows(IllegalArgumentException.class, () -> editor.healEdges(0, 1), "Other edges at the shared node.");

        editor.healEdges(0, added);
        assertNull(complex.getNode(node));
        assertNull(complex.getEdge(added));
        assertEquals(4, face.getBoundary().size());
        assertArrayEquals(new double[] {4, 0}, coordinates(complex.getEdge(0).getBoundary().get(1).getTopo()));
    }

    /**
     * Splitting an edge of a solid inserts a node in both faces around the edge, and keeps the solid closed.
     */
    @Test
    public void testSplitEdgeOfSolid() {
        final IndexedComplex complex = build(SolidFixtures.box(0, 0, 0, 1, 1, 1));
        final ComplexEditor editor = new ComplexEditor(complex);
        final double[] c = positions((LineString) complex.getEdge(0).getGeometry());
        editor.splitEdge(0, (c[0] + c[3]) / 2, (c[1] + c[4]) / 2, (c[2] + c[5]) / 2);
        final int added = complex.getEdgeCount() - 1;
        for (final DirectedFace face : complex.getEdge(added).getCoBoundary()) {
            assertEquals(5, face.getBoundary().size());
        }
        final Solid solid = complex.getSolid(0);
        assertClosed(solid);
        assertEquals(1, volume(solid), EPS);

        editor.healEdges(0, added);
        assertClosed(solid);
        assertEquals(8 + 12 + 6 + 1, complex.getElements().size());
    }

    /**
     * Splitting a face by a diagonal gives two faces sharing the diagonal, and merging restores the face.
     */
    @Test
    public void testSplitAndMergeFaces() {
        final IndexedComplex complex = square();
        final ComplexEditor editor = new ComplexEditor(complex);
        final int created = editor.splitFace(0, 0, 2);
        final int diagonal = complex.getEdgeCount() - 1;
        assertEquals(3, complex.getFace(0).getBoundary().size());
        assertEquals(3, complex.getFace(created).getBoundary().size());
        final List<DirectedFace> faces = complex.getEdge(diagonal).getCoBoundary();
        assertEquals(2, faces.size());
        assertNotEquals(faces.get(0).getOrientationSign(), faces.get(1).getOrientationSign());
        assertEquals(4, complex.getBoundary().size(), "The diagonal cancels out.");
        assertThrows(IllegalArgumentException.class, () -> editor.splitFace(0, 0, 1), "Already an edge.");
        assertThrows(IllegalArgumentException.class, () -> editor.splitFace(0, 0, 3), "Not on the face.");

        editor.mergeFaces(diagonal);
        assertNull(complex.getFace(created));
        assertNull(complex.getEdge(diagonal));
        assertEquals(4, complex.getFace(0).getBoundary().size());
        assertTrue(complex.getFace(0).getExpression().getBoundary().getBoundary().isZero());
        assertThrows(IllegalArgumentException.class, () -> editor.mergeFaces(0), "A single face around the edge.");
    }

    /**
     * Splitting a face by a path through isolated nodes, which are moved to the face containing them.
     */
    @Test
    public void testSplitFaceByPath() {
        final IndexedComplex complex = square();
        final ComplexEditor editor = new ComplexEditor(complex);
        final int middle = editor.addIsolatedNode(2, 1);
        final int upper  = editor.addIsolatedNode(1, 3);
        final int lower  = editor.addIsolatedNode(3, 1);
        final int created = editor.splitFace(0, 0, middle, 2);
        assertEquals(4, complex.getFace(0).getBoundary().size());
        assertEquals(4, complex.getFace(created).getBoundary().size());
        assertNull(complex.getNode(middle).getContainer());
        assertEquals(2, complex.getNode(middle).getCoBoundary().size());
        assertEquals(complex.getFace(created), complex.getNode(upper).getContainer());
        assertEquals(complex.getFace(0), complex.getNode(lower).getContainer());
        assertThrows(IllegalArgumentException.class, () -> editor.splitFace(0, 1, lower, 3), "Not on the same face.");
    }

    /**
     * Splitting a face by a closed path gives a face inside a hole of the face.
     */
    @Test
    public void testSplitFaceByClosedPath() {
        final IndexedComplex complex = square();
        final ComplexEditor editor = new ComplexEditor(complex);
        final int a = editor.addIsolatedNode(1, 1);
        final int b = editor.addIsolatedNode(3, 1);
        final int c = editor.addIsolatedNode(3, 3);
        final int d = editor.addIsolatedNode(1, 3);
        final int center = editor.addIsolatedNode(2, 2);
        final int created = editor.splitFace(0, a, b, c, d, a);
        assertEquals(1, complex.getFace(0).getInteriorRings().size());
        assertEquals(4, complex.getFace(created).getExteriorRing().size());
        assertEquals(complex.getFace(created), complex.getNode(center).getContainer());
        assertEquals(4, complex.getBoundary().size(), "The hole and the inner face cancel out.");
        assertTrue(complex.isConnected());
        final int edge = complex.getEdgeCount() - 1;
        assertThrows(IllegalArgumentException.class, () -> editor.mergeFaces(edge), "The faces share other edges.");
    }

    /**
     * Splitting a face of a solid adds the new face to the shell of the solid.
     */
    @Test
    public void testSplitFaceOfSolid() {
        final IndexedComplex complex = build(SolidFixtures.box(0, 0, 0, 1, 1, 1));
        final ComplexEditor editor = new ComplexEditor(complex);
        final int[] corners = complex.nodes(complex.faceRings[5][0]);
        final int created = editor.splitFace(5, corners[0], corners[2]);
        final Solid solid = complex.getSolid(0);
        assertEquals(7, solid.getBoundary().size());
        assertEquals(List.of(solid), complex.getFace(created).getCoBoundary());
        assertClosed(solid);
        assertEquals(1, volume(solid), EPS);

        editor.mergeFaces(complex.getEdgeCount() - 1);
        assertEquals(6, solid.getBoundary().size());
        assertClosed(solid);
        assertEquals(1, volume(solid), EPS);
    }

    /**
     * Faces are added on existing edges, with the nodes inside them isolated, and removed.
     */
    @Test
    public void testAddAndRemoveFace() {
        final IndexedComplex complex = build(line(CRS2D, 0,0, 4,0), line(CRS2D, 4,0, 4,4), line(CRS2D, 4,4, 0,4),
                line(CRS2D, 0,4, 0,0), GeometryFactory.DEFAULT.createPoint(CRS2D, 1, 1));
        assertEquals(0, complex.getFaceCount());
        final ComplexEditor editor = new ComplexEditor(complex);
        final int face = editor.addFace(new int[] {0, 1, 2, 3});
        assertEquals(complex.getFace(face), complex.getNode(4).getContainer());
        assertTrue(complex.getFace(face).getExpression().getBoundary().getBoundary().isZero());
        assertThrows(IllegalArgumentException.class, () -> editor.addFace(new int[] {0, 1, 2, 3}), "Same edges.");
        assertThrows(IllegalArgumentException.class, () -> editor.addFace(new int[] {0, 1, 3}), "Not closed.");

        editor.removeFace(face);
        assertNull(complex.getFace(face));
        assertNull(complex.getNode(4).getContainer());
        final Edge edge = complex.getEdge(0);
        assertNotNull(edge);
        assertTrue(edge.getCoBoundary().isEmpty());
    }

    /**
     * Solids are added on existing faces, with the nodes inside them isolated, and removed.
     */
    @Test
    public void testAddAndRemoveSolid() {
        final IndexedComplex complex = build(SolidFixtures.boxShell(0, 0, 0, 2, 2, 2, true),
                                             GeometryFactory.DEFAULT.createPoint(CRS3D, 1, 1, 1));
        assertEquals(0, complex.getSolidCount());
        final ComplexEditor editor = new ComplexEditor(complex);
        int solid = editor.addSolid(new int[] {0, 1, 2, 3, 4, 5});
        assertEquals(complex.getSolid(solid), complex.getNode(8).getContainer());
        assertClosed(complex.getSolid(solid));
        assertEquals(8, volume(complex.getSolid(solid)), EPS);

        editor.removeSolid(solid);
        assertNull(complex.getNode(8).getContainer());
        assertThrows(IllegalArgumentException.class, () -> editor.addSolid(new int[] {0, 1, 2, 3, 4}), "Open shell.");

        solid = editor.addSolid(new int[] {~0, ~1, ~2, ~3, ~4, ~5});
        assertEquals(8, volume(complex.getSolid(solid)), EPS, "The shell shall be reversed.");
        assertThrows(IllegalArgumentException.class, () -> editor.addSolid(new int[] {0, 1, 2, 3, 4, 5}), "Same side.");
    }

    /**
     * The geometry of the complex after an edit is made of the realizations of its primitives.
     */
    @Test
    public void testGeometryAfterEdit() {
        final IndexedComplex complex = square();
        new ComplexEditor(complex).addIsolatedNode(6, 6);
        assertEquals(2, ((org.apache.sis.geometries.GeometryCollection<?>) complex.getGeometry()).getNumGeometries());
        assertEquals(5, ((Polygon) ((org.apache.sis.geometries.GeometryCollection<?>) complex.getGeometry()).getGeometryN(0))
                .getExteriorRing().getDataPoints().getAttributeArray(DataPointsType.ATT_POSITION).getLength());
    }
}
