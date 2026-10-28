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
package org.apache.sis.geometries.operation.polyhedron;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.curve.LinearRing;
import org.apache.sis.geometries.solid.Polyhedron;
import org.apache.sis.geometries.solid.polyhedron.Icosahedron;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.Vector;
import org.apache.sis.measure.NumberRange;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;


/**
 * Solids for the tests of the operations on polyhedra, all in the three-dimensional undefined reference system
 * of {@link Geometries#getUndefinedCRS(int)}. The faces of the exterior shells are oriented outward, which is
 * counterclockwise seen from outside, and the faces of the interior shells are oriented toward the inside of
 * their void, as required by {@link org.apache.sis.geometries.Solid}. Each method creates new geometries.
 *
 * <p>Pairs of solids are returned as arrays of two polyhedra. They are made of unit cubes touching each other,
 * except the pairs which overlap.</p>
 *
 * <p>This class also provides the assertions on polyhedra. They never format a polyhedron in a message,
 * because polyhedra have no Well Known Text and their {@code toString()} throws an exception.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class SolidFixtures {

    /**
     * The reference system of all solids.
     */
    public static final CoordinateReferenceSystem CRS = Geometries.getUndefinedCRS(3);

    /**
     * Corners of each face of a box, oriented outward. The corner of index <var>i</var> is at the
     * minimum or maximum of each axis depending on the bits of <var>i</var>: 1 for x, 2 for y, 4 for z.
     */
    private static final int[][] BOX_FACES = {
        {0, 4, 6, 2},       // x minimum
        {1, 3, 7, 5},       // x maximum
        {0, 1, 5, 4},       // y minimum
        {2, 6, 7, 3},       // y maximum
        {0, 2, 3, 1},       // z minimum
        {4, 5, 7, 6}        // z maximum
    };

    private SolidFixtures() {
    }

    /**
     * Returns the box between the given corners.
     *
     * @return a box of volume (x1−x0)(y1−y0)(z1−z0).
     */
    public static Polyhedron box(double x0, double y0, double z0, double x1, double y1, double z1) {
        return GeometryFactory.DEFAULT.createPolyhedron(boxShell(x0, y0, z0, x1, y1, z1, true), List.of());
    }

    /**
     * Returns the cube from 0 to {@code size} on all axes, with a cubic void inside it at the given distance
     * from its faces. The faces of the void are oriented toward the inside of the void.
     *
     * @return a solid of volume size³ − (size − 2⋅wall)³.
     */
    public static Polyhedron hollowCube(double size, double wall) {
        final double w = size - wall;
        return GeometryFactory.DEFAULT.createPolyhedron(boxShell(0, 0, 0, size, size, size, true),
                List.of(boxShell(wall, wall, wall, w, w, w, false)));
    }

    /**
     * Returns the box from (0,0,0) to (1,2,3), rotated around an oblique axis and translated,
     * so that its coordinates are not integers and its faces are not parallel to the axes.
     *
     * @return a box of volume 6.
     */
    public static Polyhedron rotatedBox() {
        final double[] axis = {1/3., 2/3., 2/3.};        // unit vector.
        final double[] translation = {0.3, -1.7, 2.9};
        final double[][] corners = corners(0, 0, 0, 1, 2, 3);
        for (int i = 0; i < corners.length; i++) {
            final double[] p = rotate(corners[i], axis, 0.7);
            for (int k = 0; k < 3; k++) {
                p[k] += translation[k];
            }
            corners[i] = p;
        }
        return GeometryFactory.DEFAULT.createPolyhedron(shell(corners, true), List.of());
    }

    /**
     * Returns the regular icosahedron centered on the origin, with its vertices on the unit sphere.
     * This polyhedron computes its faces from shared vertices, and does not support {@code getDataPoints()}.
     *
     * @return an icosahedron of edge a = 4/√(10 + 2√5) and volume 5(3 + √5)a³/12.
     */
    public static Polyhedron icosahedron() {
        return new Icosahedron();
    }

    /**
     * Returns the extrusion from z = 0 to z = 2 of the square from 0 to 4 with a square hole from 1 to 3,
     * computed by the prism of that polygon.
     *
     * @return a solid of volume (16 − 4)⋅2 = 24, whose lower and upper faces have a hole.
     */
    public static Polyhedron prismWithHole() {
        final GeometryFactory f = GeometryFactory.DEFAULT;
        final CoordinateReferenceSystem crs2D = Geometries.getUndefinedCRS(2);
        final Polygon base = f.createPolygon(ring(crs2D, 0,0, 4,0, 4,4, 0,4, 0,0),
                                     List.of(ring(crs2D, 1,1, 1,3, 3,3, 3,1, 1,1)));
        return (Polyhedron) f.createPrism(base, NumberRange.create(0., true, 2., true), null).toExtrusion();
    }

    /**
     * Returns two unit cubes sharing a face, the plane x = 1.
     */
    public static Polyhedron[] sharingFace() {
        return new Polyhedron[] {box(0, 0, 0, 1, 1, 1), box(1, 0, 0, 2, 1, 1)};
    }

    /**
     * Returns two unit cubes sharing an edge, the segment from (1,1,0) to (1,1,1).
     */
    public static Polyhedron[] sharingEdge() {
        return new Polyhedron[] {box(0, 0, 0, 1, 1, 1), box(1, 1, 0, 2, 2, 1)};
    }

    /**
     * Returns two unit cubes sharing a vertex, the point (1,1,1).
     */
    public static Polyhedron[] sharingVertex() {
        return new Polyhedron[] {box(0, 0, 0, 1, 1, 1), box(1, 1, 1, 2, 2, 2)};
    }

    /**
     * Returns two cubes of size 2 overlapping in the unit cube from (1,1,1) to (2,2,2).
     * No face of a cube is in the plane of a face of the other cube.
     */
    public static Polyhedron[] overlapping() {
        return new Polyhedron[] {box(0, 0, 0, 2, 2, 2), box(1, 1, 1, 3, 3, 3)};
    }

    /**
     * Returns two cubes of size 2 offset along x, overlapping in the box from (1,0,0) to (2,2,2).
     * Four faces of each cube are in the planes of four faces of the other cube.
     */
    public static Polyhedron[] offsetAlongX() {
        return new Polyhedron[] {box(0, 0, 0, 2, 2, 2), box(1, 0, 0, 3, 2, 2)};
    }

    /**
     * Returns the six faces of the box between the given corners.
     *
     * @param outward  whether the faces are oriented toward the outside of the box (exterior shell)
     *                 or toward its inside (shell of a void).
     */
    public static GeometryCollection<Polygon> boxShell(double x0, double y0, double z0,
                                                       double x1, double y1, double z1, boolean outward)
    {
        return shell(corners(x0, y0, z0, x1, y1, z1), outward);
    }

    /**
     * Returns the eight corners of the box between the given corners, in the order of {@link #BOX_FACES}.
     */
    private static double[][] corners(double x0, double y0, double z0, double x1, double y1, double z1) {
        final double[][] corners = new double[8][];
        for (int i = 0; i < 8; i++) {
            corners[i] = new double[] {
                ((i & 1) == 0) ? x0 : x1,
                ((i & 2) == 0) ? y0 : y1,
                ((i & 4) == 0) ? z0 : z1
            };
        }
        return corners;
    }

    /**
     * Returns the six faces of a box of the given corners, in the order of {@link #BOX_FACES}.
     */
    private static GeometryCollection<Polygon> shell(final double[][] corners, final boolean outward) {
        final GeometryFactory f = GeometryFactory.DEFAULT;
        final Polygon[] faces = new Polygon[BOX_FACES.length];
        for (int i = 0; i < faces.length; i++) {
            final int[] face = BOX_FACES[i];
            final double[] coordinates = new double[(face.length + 1) * 3];
            for (int j = 0; j <= face.length; j++) {
                final int corner = face[outward ? j % face.length : (face.length - j) % face.length];
                System.arraycopy(corners[corner], 0, coordinates, j * 3, 3);
            }
            faces[i] = f.createPolygon(ring(CRS, coordinates), List.of());
        }
        return f.createMultiPolygon(CRS, faces);
    }

    /**
     * Returns the given point rotated by the given angle around an axis through the origin,
     * with the formula of Rodrigues.
     *
     * @param axis   unit vector of the axis.
     * @param angle  angle in radians, counterclockwise seen from the side toward which the axis points.
     */
    private static double[] rotate(final double[] p, final double[] axis, final double angle) {
        final double cos = Math.cos(angle), sin = Math.sin(angle);
        final double dot = axis[0]*p[0] + axis[1]*p[1] + axis[2]*p[2];
        final double[] cross = {
            axis[1]*p[2] - axis[2]*p[1],
            axis[2]*p[0] - axis[0]*p[2],
            axis[0]*p[1] - axis[1]*p[0]
        };
        final double[] rotated = new double[3];
        for (int k = 0; k < 3; k++) {
            rotated[k] = p[k]*cos + cross[k]*sin + axis[k]*dot*(1 - cos);
        }
        return rotated;
    }

    private static LinearRing ring(final CoordinateReferenceSystem crs, final double... coordinates) {
        return GeometryFactory.DEFAULT.createLinearRing(GeometryFactory.DEFAULT.createDataPoints(NDArrays.of(crs, coordinates)));
    }

    /**
     * Verifies the volume of the given polyhedron, with a tolerance relative to the expected value.
     *
     * @param expected  the expected volume.
     * @param solid     the polyhedron to verify.
     */
    public static void assertVolume(final double expected, final Polyhedron solid) {
        assertEquals(expected, solid.getVolume().getValue().doubleValue(), VOLUME_TOLERANCE * Math.max(1, Math.abs(expected)), "Volume.");
    }

    /**
     * Verifies that each shell of the given polyhedron is closed and oriented. Every edge of a face, holes included,
     * shall be shared by exactly one other face going along it in the opposite direction, which makes the shell
     * closed, without more than two faces on an edge, and with all its faces oriented the same way. The volume
     * enclosed by the shell shall be positive for the exterior shell, oriented outward, and negative for a void,
     * whose faces are oriented toward the inside of the void.
     *
     * <p>The edges are compared by the exact positions of their ends, so a vertex of a face lying inside an edge
     * of another face is an error. This verification does not use the classes under test, and its messages do
     * not format the polyhedron, which has no Well Known Text.</p>
     *
     * @param solid  the polyhedron to verify.
     */
    public static void assertClosedAndOriented(final Polyhedron solid) {
        assertClosedAndOriented(solid.getExteriorShell(), "Exterior shell", true);
        final List<GeometryCollection<Polygon>> voids = solid.getInteriorShells();
        for (int i = 0; i < voids.size(); i++) {
            assertClosedAndOriented(voids.get(i), "Interior shell " + i, false);
        }
    }

    /**
     * Verifies that the given shell is closed and oriented.
     *
     * @param shell     the shell to verify.
     * @param name      name of the shell in the error messages.
     * @param exterior  whether the shell shall enclose a positive volume (exterior shell) or a negative one (void).
     */
    private static void assertClosedAndOriented(final GeometryCollection<Polygon> shell, final String name, final boolean exterior) {
        assertNotEquals(0, shell.getNumGeometries(), () -> name + " has no face.");
        final Map<Edge,Integer> edges = new HashMap<>();
        for (int i = 0, n = shell.getNumGeometries(); i < n; i++) {
            final Polygon face = shell.getGeometryN(i);
            addEdges(face.getExteriorRing(), edges);
            for (final LinearRing hole : face.getInteriorRings()) {
                addEdges(hole, edges);
            }
        }
        for (final Map.Entry<Edge,Integer> entry : edges.entrySet()) {
            final Edge edge = entry.getKey();
            final int count = entry.getValue();
            assertEquals(1, count, () -> name + ": the edge " + edge + " is used " + count + " times in the same direction.");
            assertEquals(1, edges.getOrDefault(edge.reverse(), 0), () -> name + ": the edge " + edge + " is not used once in the opposite direction.");
        }
        final double volume = signedVolume(shell);
        if (exterior) {
            assertTrue(volume > 0, () -> name + " shall be oriented outward, but encloses the volume " + volume + '.');
        } else {
            assertTrue(volume < 0, () -> name + " shall be oriented toward the inside of the void, but encloses the volume " + volume + '.');
        }
    }

    /**
     * Adds the edges of the given ring, each one counted in its direction. Edges of zero length are ignored.
     */
    private static void addEdges(final LinearRing ring, final Map<Edge,Integer> edges) {
        final DataPoints points = ring.getDataPoints();
        final int n = points.size();
        for (int i = 0; i < n; i++) {
            final Vertex from = vertex(points, i);
            final Vertex to   = vertex(points, (i + 1) % n);
            if (!from.equals(to)) {
                edges.merge(new Edge(from, to), 1, Integer::sum);
            }
        }
    }

    /**
     * Returns the volume enclosed by the given shell, positive if its faces are oriented outward of that volume.
     * The rings of each face are split in fans of triangles, and the volumes of the tetrahedra joining a vertex
     * of the shell to those triangles are added. The holes, oriented opposite to the exterior ring of their face,
     * subtract their volume.
     *
     * @param shell  the shell of a polyhedron.
     * @return the volume enclosed by the shell, with the sign of its orientation.
     */
    public static double signedVolume(final GeometryCollection<Polygon> shell) {
        double[] origin = null;
        double volume = 0;
        for (int i = 0, n = shell.getNumGeometries(); i < n; i++) {
            final Polygon face = shell.getGeometryN(i);
            final List<LinearRing> rings = new ArrayList<>();
            rings.add(face.getExteriorRing());
            rings.addAll(face.getInteriorRings());
            for (final LinearRing ring : rings) {
                final DataPoints points = ring.getDataPoints();
                if (points.size() < 3) continue;
                final double[] a = points.getPosition(0).toArrayDouble();
                if (origin == null) {
                    origin = a;
                }
                for (int j = 1; j + 1 < points.size(); j++) {
                    final double[] b = points.getPosition(j).toArrayDouble();
                    final double[] c = points.getPosition(j + 1).toArrayDouble();
                    final double ax = a[0] - origin[0], ay = a[1] - origin[1], az = a[2] - origin[2];
                    final double bx = b[0] - origin[0], by = b[1] - origin[1], bz = b[2] - origin[2];
                    final double cx = c[0] - origin[0], cy = c[1] - origin[1], cz = c[2] - origin[2];
                    volume += ax * (by*cz - bz*cy) + ay * (bz*cx - bx*cz) + az * (bx*cy - by*cx);
                }
            }
        }
        return volume / 6;
    }

    /**
     * Relative tolerance on the volumes.
     */
    private static final double VOLUME_TOLERANCE = 1E-9;

    /**
     * A vertex compared by its exact position. Negative zeros are replaced by positive zeros,
     * because the components of a record are compared as by {@link Double#compare(double, double)}.
     */
    private record Vertex(double x, double y, double z) {
    }

    /**
     * Returns the vertex at the given index of the given positions.
     */
    private static Vertex vertex(final DataPoints points, final int index) {
        final Vector<?> p = points.getPosition(index);
        return new Vertex(p.get(0) + 0.0, p.get(1) + 0.0, p.get(2) + 0.0);
    }

    /**
     * An edge going from a vertex to another one.
     */
    private record Edge(Vertex from, Vertex to) {
        /** Returns the same edge in the opposite direction. */
        Edge reverse() {
            return new Edge(to, from);
        }
    }
}
