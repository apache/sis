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
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.Orientable;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.curve.LinearRing;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.operation.polyhedron.SolidFixtures;
import org.apache.sis.geometries.solid.Polyhedron;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.geometries.topology.DirectedFace;
import org.apache.sis.geometries.topology.DirectedSolid;
import org.apache.sis.geometries.topology.Edge;
import org.apache.sis.geometries.topology.Face;
import org.apache.sis.geometries.topology.Node;
import org.apache.sis.geometries.topology.Primitive;
import org.apache.sis.geometries.topology.Solid;
import org.apache.sis.maths.NDArrays;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link ComplexBuilder} and the {@link IndexedComplex} it builds.
 *
 * @author Johann Sorel (Geomatys)
 */
public class ComplexBuilderTest {

    private static final double EPS = 1E-9;

    private static final CoordinateReferenceSystem CRS2D = Geometries.getUndefinedCRS(2);

    private static final CoordinateReferenceSystem CRS3D = SolidFixtures.CRS;

    /**
     * Builds the complex of the given geometries.
     */
    static IndexedComplex build(final Geometry... geometries) {
        final ComplexBuilder builder = new ComplexBuilder();
        for (final Geometry geometry : geometries) {
            builder.add(geometry);
        }
        return builder.build();
    }

    /**
     * A box gives 8 nodes, 12 edges, 6 faces and 1 solid. Each edge is used in both directions by its two faces,
     * the boundary of the boundary of the solid is zero, and the faces enclose the volume of the box.
     */
    @Test
    public void testBox() {
        final Polyhedron box = SolidFixtures.box(0, 0, 0, 1, 2, 3);
        final IndexedComplex complex = build(box);
        assertEquals(8,  complex.getNodeCount());
        assertEquals(12, complex.getEdgeCount());
        assertEquals(6,  complex.getFaceCount());
        assertEquals(1,  complex.getSolidCount());
        assertEquals(3,  complex.getDimension());
        assertEquals(8 + 12 + 6 + 1, complex.getElements().size());
        assertTrue(complex.isConnected());
        assertTrue(complex.isMaximal());
        assertSame(box, complex.getGeometry());
        for (int i = 0; i < complex.getNodeCount(); i++) {
            assertEquals(3, complex.getNode(i).getCoBoundary().size(), "Edges around a corner.");
        }
        for (int i = 0; i < complex.getEdgeCount(); i++) {
            assertOppositeFaces(complex.getEdge(i));
        }
        final Solid solid = complex.getSolid(0);
        for (int i = 0; i < complex.getFaceCount(); i++) {
            assertEquals(List.of(solid), complex.getFace(i).getCoBoundary(), "The solid is below each face.");
        }
        assertClosed(solid);
        assertEquals(6, volume(solid), EPS);
        assertEquals(6, complex.getBoundary().size(), "The boundary of the complex is the boundary of the solid.");
    }

    /**
     * A cube with a void gives two shells, connected through the solid.
     */
    @Test
    public void testHollowCube() {
        final IndexedComplex complex = build(SolidFixtures.hollowCube(4, 1));
        assertEquals(16, complex.getNodeCount());
        assertEquals(24, complex.getEdgeCount());
        assertEquals(12, complex.getFaceCount());
        assertTrue(complex.isConnected());
        final Solid solid = complex.getSolid(0);
        assertEquals(6, solid.getExteriorShell().size());
        assertEquals(1, solid.getInteriorShells().size());
        assertClosed(solid);
        assertEquals(64 - 8, volume(solid), EPS);
    }

    /**
     * Shells wound the wrong way are reversed, so that the solid is below all its faces:
     * an exterior shell oriented inward, and a void oriented like the exterior shell.
     */
    @Test
    public void testOrientation() {
        final GeometryFactory f = GeometryFactory.DEFAULT;
        final Polyhedron inverted = f.createPolyhedron(SolidFixtures.boxShell(0, 0, 0, 1, 1, 1, false), List.of());
        Solid solid = build(inverted).getSolid(0);
        assertClosed(solid);
        assertEquals(1, volume(solid), EPS);

        final Polyhedron hollow = f.createPolyhedron(SolidFixtures.boxShell(0, 0, 0, 4, 4, 4, true),
                                             List.of(SolidFixtures.boxShell(1, 1, 1, 3, 3, 3, true)));
        solid = build(hollow).getSolid(0);
        assertClosed(solid);
        assertEquals(64 - 8, volume(solid), EPS);
    }

    /**
     * Two cubes sharing a face share that face, used positively by one solid and negatively by the other.
     * The shared face cancels out in the boundary of the complex.
     */
    @Test
    public void testSharingFace() {
        final Polyhedron[] pair = SolidFixtures.sharingFace();
        final IndexedComplex complex = build(pair[0], pair[1]);
        assertEquals(12, complex.getNodeCount());
        assertEquals(20, complex.getEdgeCount());
        assertEquals(11, complex.getFaceCount());
        assertEquals(2,  complex.getSolidCount());
        assertTrue(complex.isConnected());
        int shared = 0;
        for (int i = 0; i < complex.getFaceCount(); i++) {
            final List<DirectedSolid> solids = complex.getFace(i).getCoBoundary();
            if (solids.size() == 2) {
                shared++;
                assertNotEquals(solids.get(0).getOrientationSign(), solids.get(1).getOrientationSign());
            }
        }
        assertEquals(1, shared, "Faces between the two solids.");
        assertEquals(10, complex.getBoundary().size());
        assertEquals(1, volume(complex.getSolid(0)), EPS);
        assertEquals(1, volume(complex.getSolid(1)), EPS);
    }

    /**
     * Cubes sharing an edge or a vertex are valid complexes. The shared edge has four faces around it.
     */
    @Test
    public void testSharingEdgeOrVertex() {
        Polyhedron[] pair = SolidFixtures.sharingEdge();
        IndexedComplex complex = build(pair[0], pair[1]);
        assertEquals(14, complex.getNodeCount());
        assertEquals(23, complex.getEdgeCount());
        assertEquals(12, complex.getFaceCount());
        int around = 0;
        for (int i = 0; i < complex.getEdgeCount(); i++) {
            around = Math.max(around, complex.getEdge(i).getCoBoundary().size());
        }
        assertEquals(4, around);

        pair = SolidFixtures.sharingVertex();
        complex = build(pair[0], pair[1]);
        assertEquals(15, complex.getNodeCount());
        assertEquals(24, complex.getEdgeCount());
        assertTrue(complex.isConnected());
    }

    /**
     * The prism with a hole has faces with an interior ring, and a closed boundary.
     */
    @Test
    public void testPrismWithHole() {
        final IndexedComplex complex = build(SolidFixtures.prismWithHole());
        final Solid solid = complex.getSolid(0);
        assertClosed(solid);
        assertEquals(24, volume(solid), EPS);
        boolean hole = false;
        for (int i = 0; i < complex.getFaceCount(); i++) {
            hole |= !complex.getFace(i).getInteriorRings().isEmpty();
        }
        assertTrue(hole, "The lower and upper faces have a hole.");
    }

    /**
     * Three triangles around an edge are listed in clockwise order seen from the end of the edge,
     * whatever their order in the mesh. The opposite edge lists the opposite faces in reverse order.
     */
    @Test
    public void testRadialOrder() {
        final double cos = Math.cos(Math.toRadians(200)), sin = Math.sin(Math.toRadians(200));
        final MeshPrimitive mesh = MeshPrimitive.create(MeshPrimitive.Type.TRIANGLES);
        mesh.setPositions(NDArrays.of(CRS3D, new double[] {
            0, 0, 0,                // start of the edge
            0, 0, 1,                // end of the edge
            1, 0, 0.5,              // fin toward 0°
            0, 1, 0.5,              // fin toward 90°
            cos, sin, 0.5           // fin toward 200°
        }));
        mesh.setIndex(NDArrays.ofUnsigned(1, new int[] {0,1,2, 0,1,3, 0,1,4}));
        final IndexedComplex complex = build(mesh);
        assertEquals(3, complex.getFaceCount());
        final Edge axis = complex.getEdge(0);
        assertArrayEquals(new double[] {0, 0, 1}, complex.getCoordinates(1));
        assertEquals(List.of(complex.getFace(0), complex.getFace(2), complex.getFace(1)),
                     axis.getCoBoundary(), "Clockwise seen from above: 0°, then 200°, then 90°.");
        assertEquals(List.of(complex.getFace(1).getNegate(), complex.getFace(2).getNegate(), complex.getFace(0).getNegate()),
                     axis.getNegate().getCoBoundary());
    }

    /**
     * Four triangles around an edge, two by two in the same plane on opposite sides of the edge.
     */
    @Test
    public void testRadialOrderOpposite() {
        final MeshPrimitive mesh = MeshPrimitive.create(MeshPrimitive.Type.TRIANGLES);
        mesh.setPositions(NDArrays.of(CRS3D, new double[] {
            0, 0, 0,   0, 0, 1,     // the edge
             1, 0, 0.5,             // fin toward 0°
            -1, 0, 0.5,             // fin toward 180°
             0, 1, 0.5,             // fin toward 90°
             0,-1, 0.5              // fin toward 270°
        }));
        mesh.setIndex(NDArrays.ofUnsigned(1, new int[] {0,1,2, 0,1,3, 0,1,4, 0,1,5}));
        final IndexedComplex complex = build(mesh);
        assertEquals(List.of(complex.getFace(0), complex.getFace(3), complex.getFace(1), complex.getFace(2)),
                     complex.getEdge(0).getCoBoundary(), "Clockwise seen from above: 0°, 270°, 180°, 90°.");
    }

    /**
     * A face which is not convex, with nodes on both sides of the line of an edge, is ordered around
     * that edge by the side where the face is near the edge.
     */
    @Test
    public void testRadialOrderNonConvex() {
        final double cos = Math.cos(Math.toRadians(200)), sin = Math.sin(Math.toRadians(200));
        // In the plane y = 0, on the side x > 0 of the edge, and going around above the edge to x < 0.
        final Polygon hook  = polygon(CRS3D, 0,0,0, 0,0,1, 1,0,1, 1,0,2, -1,0,2, -1,0,3, 2,0,3, 2,0,0, 0,0,0);
        final Polygon right = polygon(CRS3D, 0,0,0, 0,0,1, 0,1,0.5, 0,0,0);
        final Polygon other = polygon(CRS3D, 0,0,0, 0,0,1, cos,sin,0.5, 0,0,0);
        final IndexedComplex complex = build(GeometryFactory.DEFAULT.createMultiPolygon(CRS3D, hook, right, other));
        assertEquals(List.of(complex.getFace(0), complex.getFace(2), complex.getFace(1)),
                     complex.getEdge(0).getCoBoundary(), "Clockwise seen from above: 0°, 200°, 90°.");
    }

    /**
     * Points and lines in two dimensions. A point inside a polygon is isolated in the face,
     * and the ends of lines are shared with the corners of the polygon.
     */
    @Test
    public void testPointsAndLines2D() {
        final GeometryFactory f = GeometryFactory.DEFAULT;
        final Polygon    square = polygon(CRS2D, 0,0, 4,0, 4,4, 0,4, 0,0);
        final Point      inner  = f.createPoint(CRS2D, 1, 1);
        final Point      corner = f.createPoint(CRS2D, 5, 5);
        final LineString path   = line(CRS2D, 4,0, 6,1, 7,0);
        final LineString link   = line(CRS2D, 4,4, 5,5);
        final LineString tail   = line(CRS2D, 0,0, -1,-1);
        final IndexedComplex complex = build(square, inner, corner, path, link, tail);
        assertEquals(9, complex.getNodeCount());
        assertEquals(8, complex.getEdgeCount());
        assertEquals(1, complex.getFaceCount());
        assertTrue(complex.isConnected(), "The isolated node is linked to its face.");

        final Face face = complex.getFace(0);
        final Node isolated = complex.getNode(4);
        assertArrayEquals(new double[] {1, 1}, complex.getCoordinates(4));
        assertEquals(face, isolated.getContainer());
        assertEquals(Set.of(isolated), face.getIsolated());
        assertTrue(isolated.getCoBoundary().isEmpty());
        assertNull(complex.getNode(5).getContainer(), "Outside the square.");
        assertEquals(1, complex.getNode(5).getCoBoundary().size(), "The end of the link.");
        assertEquals(List.of(complex.getEdge(0).getNegate(), complex.getEdge(7).getNegate(), complex.getEdge(3)),
                     complex.getNode(0).getCoBoundary(), "Clockwise around the origin: 0°, 225°, 90°.");

        assertFalse(build(square, f.createPoint(CRS2D, 9, 9)).isConnected());
        assertThrows(IllegalArgumentException.class, () -> build(square, f.createPoint(CRS2D, 2, 0)), "A point inside an edge.");
        assertThrows(IllegalArgumentException.class, () -> build(square, line(CRS2D, -1,2, 5,2)), "A line crossing the face.");
        assertThrows(IllegalArgumentException.class, () -> build(square, line(CRS2D, 1,2, 3,2)), "A line inside the face.");
    }

    /**
     * Points and lines inside a solid are isolated in the solid, and a point on a face is isolated in the face.
     */
    @Test
    public void testIsolatedInSolid() {
        final GeometryFactory f = GeometryFactory.DEFAULT;
        final Polyhedron cube    = SolidFixtures.box(0, 0, 0, 4, 4, 4);
        final Point      center  = f.createPoint(CRS3D, 2, 2, 2);
        final LineString inside  = line(CRS3D, 1,1,1, 3,3,1);
        final Point      onFace  = f.createPoint(CRS3D, 2, 2, 4);
        final LineString antenna = line(CRS3D, 2,2,4, 2,2,6);
        final IndexedComplex complex = build(cube, center, inside, onFace, antenna);
        assertEquals(13, complex.getNodeCount());
        assertEquals(14, complex.getEdgeCount());
        assertTrue(complex.isConnected());

        final Solid solid = complex.getSolid(0);
        assertEquals(solid, complex.getNode(8).getContainer());
        assertEquals(solid, complex.getNode(9).getContainer());
        assertEquals(solid, complex.getEdge(12).getContainer());
        assertEquals(Set.of(complex.getNode(8), complex.getNode(9), complex.getNode(10), complex.getEdge(12)),
                     solid.getIsolated());

        final Primitive top = complex.getNode(11).getContainer();
        assertInstanceOf(Face.class, top, "The point on the top face.");
        assertEquals(Set.of(complex.getNode(11)), top.getIsolated());
        assertNull(complex.getEdge(13).getContainer(), "The antenna is outside the solid.");
        assertClosed(solid);

        assertThrows(IllegalArgumentException.class, () -> build(cube, line(CRS3D, 2,2,-1, 2,2,5)), "A line through the solid.");
        assertThrows(IllegalArgumentException.class, () -> build(cube, f.createPoint(CRS3D, 2, 0, 0)), "A point inside an edge.");
        assertThrows(IllegalArgumentException.class, () -> build(cube, polygon(CRS3D, 1,1,1, 3,1,1, 3,3,1, 1,1,1)),
                     "A face inside the solid.");
    }

    /**
     * Meshes of points and lines give nodes and edges. A loop has no boundary.
     */
    @Test
    public void testMeshPointsAndLines() {
        final MeshPrimitive loop = MeshPrimitive.create(MeshPrimitive.Type.LINE_LOOP);
        loop.setPositions(NDArrays.of(CRS2D, new double[] {0,0, 1,0, 1,1, 0,1}));
        final MeshPrimitive points = MeshPrimitive.create(MeshPrimitive.Type.POINTS);
        points.setPositions(NDArrays.of(CRS2D, new double[] {0,0, 5,5}));
        final IndexedComplex complex = build(loop, points);
        assertEquals(5, complex.getNodeCount());
        assertEquals(4, complex.getEdgeCount());
        assertEquals(0, complex.getFaceCount());
        assertEquals(1, complex.getDimension());
        assertFalse(complex.isConnected());
        assertTrue(complex.getBoundary().isEmpty(), "A cycle and a point have no boundary.");
    }

    /**
     * Two squares sharing an edge in two dimensions share that edge with opposite orientations.
     */
    @Test
    public void testPolygons2D() {
        final Polygon a = polygon(CRS2D, 0,0, 1,0, 1,1, 0,1, 0,0);
        final Polygon b = polygon(CRS2D, 1,0, 2,0, 2,1, 1,1, 1,0);
        final IndexedComplex complex = build(GeometryFactory.DEFAULT.createMultiPolygon(CRS2D, a, b));
        assertEquals(6, complex.getNodeCount());
        assertEquals(7, complex.getEdgeCount());
        assertEquals(2, complex.getFaceCount());
        assertEquals(0, complex.getSolidCount());
        assertEquals(2, complex.getDimension());
        assertTrue(complex.isConnected());
        assertEquals(6, complex.getBoundary().size(), "The shared edge cancels out.");
        int shared = 0;
        for (int i = 0; i < complex.getEdgeCount(); i++) {
            final List<DirectedFace> faces = complex.getEdge(i).getCoBoundary();
            if (faces.size() == 2) {
                shared++;
                assertNotEquals(faces.get(0).getOrientationSign(), faces.get(1).getOrientationSign());
            }
        }
        assertEquals(1, shared);
        assertEquals(5, ((Polygon) complex.getFace(0).getGeometry()).getExteriorRing().getDataPoints().size());
    }

    /**
     * The views are equal when they designate the same oriented primitive, and the realizations
     * of the negative orientations are reversed.
     */
    @Test
    public void testViews() {
        final IndexedComplex complex = build(SolidFixtures.box(0, 0, 0, 1, 1, 1));
        final Edge edge = complex.getEdge(0);
        assertEquals(edge, complex.getEdge(0));
        assertEquals(edge, edge.getNegate().getNegate());
        assertNotEquals(edge, edge.getNegate());
        assertEquals(edge, edge.getNegate().getTopo());
        assertEquals(List.of(edge, edge.getNegate()), edge.getProxy());
        assertEquals(edge.getNegate(), edge.asDirectedTopo(Orientable.Sign.NEGATIVE));
        assertEquals(Set.of(edge), edge.getNegate().getInterior());
        assertEquals(3, edge.getClosure().size(), "The edge and its two nodes.");
        assertEquals(complex.getElements().size() - 3, edge.getExterior().size());
        assertSame(complex, edge.getMaximalComplex());

        final double[] c = positions((LineString) edge.getGeometry());
        final double[] r = positions((LineString) edge.getNegate().getGeometry());
        assertArrayEquals(new double[] {c[3], c[4], c[5], c[0], c[1], c[2]}, r);
        assertEquals(edge.getBoundary().get(1).getTopo(), edge.getNegate().getBoundary().get(0).getTopo(),
                     "The opposite edge starts where the edge ends.");

        final Face face = complex.getFace(0);
        assertEquals(face.getExteriorRing().size(), face.getNegate().getBoundary().size());
        assertEquals(face.getExteriorRing().get(0).getNegate(), face.getNegate().getBoundary().get(3));
    }

    /**
     * Geometries whose interiors intersect are rejected.
     */
    @Test
    public void testNotDisjoint() {
        final Polyhedron[] overlapping = SolidFixtures.overlapping();
        assertThrows(IllegalArgumentException.class, () -> build(overlapping[0], overlapping[1]), "Crossing faces.");

        final Polyhedron[] offset = SolidFixtures.offsetAlongX();
        assertThrows(IllegalArgumentException.class, () -> build(offset[0], offset[1]), "Overlapping coplanar faces.");

        assertThrows(IllegalArgumentException.class, () -> build(SolidFixtures.box(0, 0, 0, 4, 4, 4),
                                                                SolidFixtures.box(1, 1, 1, 2, 2, 2)), "Nested solids.");

        final Polyhedron box = SolidFixtures.box(0, 0, 0, 1, 1, 1);
        assertThrows(IllegalArgumentException.class, () -> build(box, box), "The same solid twice.");

        final Polygon big   = polygon(CRS2D, 0,0, 2,0, 2,2, 0,2, 0,0);
        final Polygon lower = polygon(CRS2D, 2,0, 3,0, 3,1, 2,1, 2,0);
        final Polygon upper = polygon(CRS2D, 2,1, 3,1, 3,2, 2,2, 2,1);
        assertThrows(IllegalArgumentException.class, () -> build(big, lower, upper), "A node inside an edge.");

        final MeshPrimitive mesh = MeshPrimitive.create(MeshPrimitive.Type.TRIANGLES);
        mesh.setPositions(NDArrays.of(CRS2D, new double[] {0,0, 2,0, 0,2, 1,1.5, 1,-1, 3,1}));
        mesh.setIndex(NDArrays.ofUnsigned(1, new int[] {0,1,2, 3,4,5}));
        assertThrows(IllegalArgumentException.class, () -> build(mesh), "Crossing triangles.");

        final Polygon inside = polygon(CRS2D, 0.5,0.5, 1,0.5, 1,1, 0.5,0.5);
        assertThrows(IllegalArgumentException.class, () -> build(big, inside), "A face inside another.");
    }

    /**
     * Unsupported geometries and mixed reference systems are rejected.
     */
    @Test
    public void testInvalidInput() {
        final GeometryFactory f = GeometryFactory.DEFAULT;
        assertThrows(IllegalArgumentException.class, () -> new ComplexBuilder().add(
                f.createCircularString(f.createDataPoints(CRS2D, 0,0, 1,1, 2,0))), "Not a line string.");
        assertThrows(IllegalArgumentException.class, () -> new ComplexBuilder().add(line(CRS2D, 1,1, 1,1)), "Degenerate line.");
        final ComplexBuilder mixed = new ComplexBuilder().add(SolidFixtures.box(0, 0, 0, 1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> mixed.add(polygon(CRS2D, 0,0, 1,0, 1,1, 0,0)));
    }

    /**
     * Verifies that the two faces around the given edge use it in opposite directions.
     */
    private static void assertOppositeFaces(final Edge edge) {
        final List<DirectedFace> faces = edge.getCoBoundary();
        assertEquals(2, faces.size(), "Faces around an edge.");
        assertNotEquals(faces.get(0).getOrientationSign(), faces.get(1).getOrientationSign());
    }

    /**
     * Verifies that the boundary of the solid is closed and consistently oriented,
     * which is that the boundary of its boundary is zero.
     */
    static void assertClosed(final Solid solid) {
        assertFalse(solid.getExpression().getBoundary().isZero());
        assertTrue(solid.getExpression().getBoundary().getBoundary().isZero(), "Boundary of the boundary.");
    }

    /**
     * Returns the volume enclosed by the realizations of the oriented faces of the given solid.
     */
    static double volume(final Solid solid) {
        double volume = 0;
        for (final DirectedFace face : solid.getBoundary()) {
            final Polygon polygon = (Polygon) face.getGeometry();
            volume += fan(polygon.getExteriorRing());
            for (final LinearRing hole : polygon.getInteriorRings()) {
                volume += fan(hole);
            }
        }
        return volume / 6;
    }

    /**
     * Returns six times the signed volume of the cone joining the origin to the given closed ring.
     */
    private static double fan(final LinearRing ring) {
        final double[] c = ring.getDataPoints().getAttributeArray(DataPointsType.ATT_POSITION).toArrayDouble();
        double sum = 0;
        for (int i = 3; i + 3 < c.length; i += 3) {
            sum += c[0] * (c[i+1] * c[i+5] - c[i+2] * c[i+4])
                 + c[1] * (c[i+2] * c[i+3] - c[i]   * c[i+5])
                 + c[2] * (c[i]   * c[i+4] - c[i+1] * c[i+3]);
        }
        return sum;
    }

    static double[] positions(final LineString line) {
        return line.getDataPoints().getAttributeArray(DataPointsType.ATT_POSITION).toArrayDouble();
    }

    static Polygon polygon(final CoordinateReferenceSystem crs, final double... coordinates) {
        final GeometryFactory f = GeometryFactory.DEFAULT;
        return f.createPolygon(f.createLinearRing(f.createDataPoints(NDArrays.of(crs, coordinates))), List.of());
    }

    static LineString line(final CoordinateReferenceSystem crs, final double... coordinates) {
        final GeometryFactory f = GeometryFactory.DEFAULT;
        return f.createLineString(f.createDataPoints(NDArrays.of(crs, coordinates)));
    }
}
