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
package org.apache.sis.geometries.operation.extrude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleBinaryOperator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.curve.LinearRing;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.mesh.MultiMeshPrimitive;
import org.apache.sis.geometries.operation.extrude.TerrainExtruder.HeightReference;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.maths.Maths;
import org.apache.sis.maths.NDArrays;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link TerrainExtruder}.
 *
 * <p>The terrain is a 10×10 square made of two triangles, sloped along x with z = x.
 * Its diagonal goes from (0,0) to (10,10).</p>
 * <pre>
 *  10 ■-----------■
 *     |         ⟋ |
 *     |       ⟋   |
 *     |     ⟋     |
 *     |   ⟋       |
 *     | ⟋         |
 *   0 ■-----------■
 *     0           10
 * </pre>
 *
 * @author Johann Sorel (Geomatys)
 */
public class TerrainExtruderTest {

    private static final CoordinateReferenceSystem CRS3D = Geometries.getUndefinedCRS(3);

    /**
     * Footprint crossing the terrain diagonal at (3,3) and (5,5).
     */
    private static final double[] FOOTPRINT = {2,3, 5,3, 5,6, 2,6};

    /**
     * Absolute top joined to the terrain by walls.
     */
    @Test
    public void testAbsoluteWithWalls() {
        final Mesh mesh = extrude(20, 20, polygon(FOOTPRINT));
        for (double[] p : new double[][] {{2,3},{5,3},{5,6},{2,6},{3,3},{5,5}}) {
            assertTrue(mesh.hasVertex(p[0], p[1], 20), "Missing top vertex at " + p[0] + " " + p[1]);
        }
        mesh.assertElevations((x, y) -> inside(FOOTPRINT, x, y) ? 20 : x);
        assertTrue(mesh.countVertical() > 0, "Walls expected.");
        mesh.assertWatertight();
    }

    /**
     * Rotated footprints on a grid terrain, whose edges cross many terrain edges at interpolated points
     * slightly off the footprint lines. The walls shall have no hole.
     */
    @Test
    public void testWallsOnGrid() {
        final int n = 10;
        final double[] xyz = new double[(n + 1) * (n + 1) * 3];
        for (int i = 0, k = 0; i <= n; i++) {
            for (int j = 0; j <= n; j++) {
                xyz[k++] = i; xyz[k++] = j; xyz[k++] = i;
            }
        }
        final int[] index = new int[n * n * 6];
        for (int i = 0, k = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                final int a = i * (n + 1) + j, b = a + 1, d = a + n + 1, e = d + 1;
                index[k++] = a; index[k++] = d; index[k++] = e;
                index[k++] = a; index[k++] = e; index[k++] = b;
            }
        }
        final MeshPrimitive.Triangles grid = new MeshPrimitive.Triangles();
        grid.setPositions(NDArrays.of(CRS3D, xyz));
        grid.setIndex(NDArrays.ofUnsigned(1, index));
        final TerrainExtruder extruder = new TerrainExtruder(grid);
        extruder.appendFlatConstraint(polygon(new double[] {1.3,2.1, 6.7,1.2, 7.9,5.3, 2.4,6.6}), 15, 15, HeightReference.ABSOLUTE, true);
        extruder.appendFlatConstraint(polygon(new double[] {6.1,6.2, 8.8,7.1, 8.1,9.3, 5.7,8.4}), 15, 18, HeightReference.ABSOLUTE, true);
        new Mesh(extruder.getResult()).assertWatertight();
    }

    /**
     * Terrain, roof and walls in separated primitives, without shared vertices.
     */
    @Test
    public void testSeparateParts() {
        final TerrainExtruder extruder = new TerrainExtruder(terrain());
        extruder.setSeparateParts(true);
        extruder.appendFlatConstraint(polygon(FOOTPRINT), 20, 23, HeightReference.ABSOLUTE, true);
        final MultiMeshPrimitive<MeshPrimitive.Triangles> result = extruder.getResult();
        assertEquals(3, result.getNumGeometries());
        final Mesh ground = new Mesh(result.getGeometryN(0));
        final Mesh roof   = new Mesh(result.getGeometryN(1));
        final Mesh walls  = new Mesh(result.getGeometryN(2));
        assertEquals(0, ground.countVertical(), "No wall expected in the terrain.");
        assertEquals(0, roof.countVertical(), "No wall expected in the roof.");
        assertEquals(walls.index.length / 3, walls.countVertical(), "Only walls expected.");
        ground.assertElevations((x, y) -> x);
        for (int i = 2; i < roof.positions.length; i += 3) {
            assertTrue(roof.positions[i] >= 20, "Roof vertex below the eaves.");
        }
    }

    /**
     * Absolute top joined to the terrain by moving the terrain vertices on the rings.
     */
    @Test
    public void testAbsoluteFlattened() {
        final TerrainExtruder extruder = new TerrainExtruder(terrain());
        extruder.appendFlatConstraint(polygon(FOOTPRINT), 20, 20, HeightReference.ABSOLUTE, false);
        final Mesh mesh = new Mesh(extruder.getResult());
        assertEquals(0, mesh.countVertical(), "No wall expected.");
        final Set<List<Double>> xy = new HashSet<>();
        for (int i = 0; i < mesh.positions.length; i += 3) {
            assertTrue(xy.add(List.of(mesh.positions[i], mesh.positions[i+1])), "Duplicated vertex location.");
            if (onRing(FOOTPRINT, mesh.positions[i], mesh.positions[i+1])) {
                assertEquals(20, mesh.positions[i+2], 1E-9);
            }
        }
        mesh.assertWatertight();
    }

    /**
     * Top relative to the ground, along the slope: flat cross-section, climbing with the terrain.
     */
    @Test
    public void testRelative() {
        final TerrainExtruder extruder = new TerrainExtruder(terrain());
        extruder.setDensification(0.5);
        extruder.appendFlatConstraint(polygon(new double[] {1,4, 9,4, 9,6, 1,6}), 2, 2, HeightReference.RELATIVE_TO_GROUND, true);
        final Mesh mesh = new Mesh(extruder.getResult());
        //top vertices on the long sides, by x
        final Map<Double,Double> south = new HashMap<>(), north = new HashMap<>();
        for (int i = 0; i < mesh.positions.length; i += 3) {
            final double x = mesh.positions[i], y = mesh.positions[i+1], z = mesh.positions[i+2];
            if (x < 3 || x > 7 || z <= x + 1) continue;             // Skip ground vertices and road ends.
            if (y == 4) south.put(x, z);
            if (y == 6) north.put(x, z);
            assertEquals(x + 2, z, 1E-6, "Top shall be 2 above the ground at the centerline.");
        }
        assertFalse(south.isEmpty());
        south.forEach((x, z) -> {
            if (north.containsKey(x)) assertEquals(z, north.get(x), 1E-6, "Cross-section shall be flat.");
        });
        mesh.assertWatertight();
    }

    /**
     * Top relative to the ground at the centroid (3.5,4.5) where the ground is at 3.5:
     * horizontal at 5.5, with walls down to the ground on all sides.
     */
    @Test
    public void testPointAtCentroid() {
        final TerrainExtruder extruder = new TerrainExtruder(terrain());
        extruder.appendFlatConstraint(polygon(FOOTPRINT), 2, 2, HeightReference.POINT_AT_CENTROID, true);
        final Mesh mesh = new Mesh(extruder.getResult());
        for (double[] p : new double[][] {{2,3},{5,3},{5,6},{2,6},{3,3},{5,5}}) {
            assertTrue(mesh.hasVertex(p[0], p[1], 5.5), "Missing top vertex at " + p[0] + " " + p[1]);
            assertTrue(mesh.hasVertex(p[0], p[1], p[0]), "Missing ground vertex at " + p[0] + " " + p[1]);
        }
        mesh.assertElevations((x, y) -> inside(FOOTPRINT, x, y) ? 5.5 : x);
        mesh.assertWatertight();
    }

    /**
     * Two overlapping footprints, the last appended one wins.
     * Not watertight: where the second rings cross a wall of the first footprint, the new walls on each
     * side have different bottoms (top of the first footprint or ground), leaving a vertical T-junction.
     */
    @Test
    public void testOverlap() {
        final double[] first  = {2,2, 6,2, 6,6, 2,6};
        final double[] second = {4,4, 8,4, 8,8, 4,8};
        final TerrainExtruder extruder = new TerrainExtruder(terrain());
        extruder.appendFlatConstraint(polygon(first), 15, 15, HeightReference.ABSOLUTE, true);
        extruder.appendFlatConstraint(polygon(second), 25, 25, HeightReference.ABSOLUTE, true);
        final Mesh mesh = new Mesh(extruder.getResult());
        mesh.assertElevations((x, y) -> inside(second, x, y) ? 25 : inside(first, x, y) ? 15 : x);
        for (int t = 0; t < mesh.index.length / 3; t++) {
            final double[] c = mesh.centroid(t);
            assertFalse(mesh.isVertical(t) && inside(second, c[0], c[1]) && !onRing(second, c[0], c[1]),
                        "Wall of the first footprint inside the second one.");
        }
    }

    /**
     * Footprint with a courtyard, the terrain stays in the courtyard.
     */
    @Test
    public void testHole() {
        final double[] exterior = {2,2, 8,2, 8,8, 2,8};
        final double[] hole     = {4,4, 4,6, 6,6, 6,4};
        final Mesh mesh = extrude(20, 20, polygon(exterior, hole));
        mesh.assertElevations((x, y) -> inside(exterior, x, y) && !inside(hole, x, y) ? 20 : x);
        int holeWalls = 0;
        for (int t = 0; t < mesh.index.length / 3; t++) {
            final double[] c = mesh.centroid(t);
            if (mesh.isVertical(t) && onRing(hole, c[0], c[1])) holeWalls++;
        }
        assertTrue(holeWalls > 0, "Walls expected around the courtyard.");
        mesh.assertWatertight();
    }

    /**
     * Hipped roof on a rectangle: eaves at 20, ridge from (3,5) to (7,5) at 23.
     * Every roof face is planar, its elevation grows with the distance to the footprint edges.
     */
    @Test
    public void testRoof() {
        final double[] footprint = {2,4, 8,4, 8,6, 2,6};
        final Mesh mesh = extrude(20, 23, polygon(footprint));
        assertTrue(mesh.hasVertex(3, 5, 23), "Missing ridge start.");
        assertTrue(mesh.hasVertex(7, 5, 23), "Missing ridge end.");
        for (double[] p : new double[][] {{2,4},{8,4},{8,6},{2,6},{4,4},{6,6}}) {
            assertTrue(mesh.hasVertex(p[0], p[1], 20), "Missing eave vertex at " + p[0] + " " + p[1]);
        }
        //distance to the nearest edge of the rectangle
        mesh.assertElevations((x, y) -> inside(footprint, x, y)
                ? 20 + 3 * Math.min(Math.min(x - 2, 8 - x), Math.min(y - 4, 6 - y)) : x);
        mesh.assertWatertight();
    }

    /**
     * Extrudes a single polygon with walls in the terrain.
     */
    private static Mesh extrude(final double height, final double roofHeight, final Polygon polygon) {
        final TerrainExtruder extruder = new TerrainExtruder(terrain());
        extruder.appendFlatConstraint(polygon, height, roofHeight, HeightReference.ABSOLUTE, true);
        return new Mesh(extruder.getResult());
    }

    /**
     * Creates the terrain, z = x.
     */
    private static MeshPrimitive.Triangles terrain() {
        final MeshPrimitive.Triangles terrain = new MeshPrimitive.Triangles();
        terrain.setPositions(NDArrays.of(CRS3D, new double[] {0,0,0, 10,0,10, 10,10,10, 0,10,0}));
        terrain.setIndex(NDArrays.ofUnsigned(1, new int[] {0,1,2, 0,2,3}));
        return terrain;
    }

    /**
     * Creates a polygon from unclosed (x,y) rings, the exterior one first.
     */
    private static Polygon polygon(final double[] exterior, final double[]... holes) {
        final List<LinearRing> interiors = new ArrayList<>();
        for (double[] hole : holes) {
            interiors.add(ring(hole));
        }
        return GeometryFactory.DEFAULT.createPolygon(ring(exterior), interiors);
    }

    private static LinearRing ring(final double[] xy) {
        final double[] xyz = new double[xy.length / 2 * 3 + 3];
        for (int i = 0; i < xyz.length / 3; i++) {
            xyz[i*3]   = xy[i*2 % xy.length];
            xyz[i*3+1] = xy[(i*2 + 1) % xy.length];
        }
        return GeometryFactory.DEFAULT.createLinearRing(GeometryFactory.DEFAULT.createDataPoints(CRS3D, xyz));
    }

    /**
     * Even-odd point in ring test.
     */
    private static boolean inside(final double[] ring, final double x, final double y) {
        boolean inside = false;
        for (int i = 0, j = ring.length - 2; i < ring.length; j = i, i += 2) {
            if ((ring[i+1] > y) != (ring[j+1] > y) && x < (ring[j] - ring[i]) * (y - ring[i+1]) / (ring[j+1] - ring[i+1]) + ring[i]) {
                inside = !inside;
            }
        }
        return inside;
    }

    /**
     * Whether the point is on a segment of the ring, with a small tolerance.
     */
    private static boolean onRing(final double[] ring, final double x, final double y) {
        for (int i = 0, j = ring.length - 2; i < ring.length; j = i, i += 2) {
            if (Maths.distanceSquare(ring[j], ring[j+1], ring[i], ring[i+1], x, y) < 1E-12) {
                return true;
            }
        }
        return false;
    }

    /**
     * Result mesh with helper methods.
     */
    private static final class Mesh {
        final double[] positions;
        final int[] index;

        Mesh(final MultiMeshPrimitive<MeshPrimitive.Triangles> result) {
            this(single(result));
        }

        Mesh(final MeshPrimitive.Triangles mesh) {
            positions = mesh.getPositions().toArrayDouble();
            index = mesh.getIndex().toArrayInt();
        }

        private static MeshPrimitive.Triangles single(final MultiMeshPrimitive<MeshPrimitive.Triangles> result) {
            assertEquals(1, result.getNumGeometries(), "Parts are not separated by default.");
            return result.getGeometryN(0);
        }

        double[] vertex(final int triangle, final int k) {
            final int v = index[triangle*3 + k];
            return new double[] {positions[v*3], positions[v*3+1], positions[v*3+2]};
        }

        double[] centroid(final int triangle) {
            final double[] c = new double[3];
            for (int k = 0; k < 3; k++) {
                final double[] p = vertex(triangle, k);
                for (int i = 0; i < 3; i++) c[i] += p[i] / 3;
            }
            return c;
        }

        boolean isVertical(final int triangle) {
            final double[] a = vertex(triangle, 0), b = vertex(triangle, 1), c = vertex(triangle, 2);
            return (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]) == 0;
        }

        int countVertical() {
            int count = 0;
            for (int t = 0; t < index.length / 3; t++) {
                if (isVertical(t)) count++;
            }
            return count;
        }

        boolean hasVertex(final double x, final double y, final double z) {
            for (int i = 0; i < positions.length; i += 3) {
                if (Math.abs(positions[i] - x) < 1E-9 && Math.abs(positions[i+1] - y) < 1E-9
                        && Math.abs(positions[i+2] - z) < 1E-9) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Verifies that the centroid of each non-vertical triangle is at the expected elevation.
         * Faces are planar, so this also verifies that no face crosses an elevation change.
         */
        void assertElevations(final DoubleBinaryOperator expected) {
            for (int t = 0; t < index.length / 3; t++) {
                if (!isVertical(t)) {
                    final double[] c = centroid(t);
                    assertEquals(expected.applyAsDouble(c[0], c[1]), c[2], 1E-9, "Wrong elevation at " + c[0] + " " + c[1]);
                }
            }
        }

        /**
         * Verifies that each directed edge is used once and its reverse once, except on the terrain border.
         */
        void assertWatertight() {
            final Map<List<Integer>, Integer> edges = new HashMap<>();
            for (int i = 0; i < index.length; i += 3) {
                for (int k = 0; k < 3; k++) {
                    edges.merge(List.of(index[i+k], index[i+(k+1)%3]), 1, Integer::sum);
                }
            }
            edges.forEach((edge, count) -> {
                final int a = edge.get(0), b = edge.get(1);
                assertEquals(1, count, "Edge used more than once in the same direction.");
                if (!onBorder(a, b)) {
                    assertTrue(edges.containsKey(List.of(b, a)), "Edge without neighbor at "
                            + positions[a*3] + " " + positions[a*3+1] + " " + positions[a*3+2] + " -> "
                            + positions[b*3] + " " + positions[b*3+1] + " " + positions[b*3+2]);
                }
            });
        }

        private boolean onBorder(final int a, final int b) {
            for (int axis = 0; axis < 2; axis++) {
                final double va = positions[a*3+axis], vb = positions[b*3+axis];
                if (va == vb && (va == 0 || va == 10)) return true;
            }
            return false;
        }
    }
}
