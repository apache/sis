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
package org.apache.sis.geometries.operation;

import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.Plane;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.curve.Ray;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.SampleSystem;
import org.apache.sis.maths.Vector;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.operation.Intersection.RayInterval;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class IntersectionTest extends AbstractD9IMTest {

    private static final SampleSystem CRS2D = SampleSystem.of(CommonCRS.WGS84.geographic());

    /**
     * Test primitives triangles against primitive points.
     * - 2 triangles.
     * - 4 points, one in triangle, one on vertex, one on edge, one outside.
     *
     * 2-4  5
     *  +---o
     *  |\  |
     *  | o | o
     *  |o \|
     *  +---+
     *  0  1-3
     */
    @Test
    public void testPrimitiveTrianglesPrimitivePoints() {

        final MeshPrimitive geom1 = new MeshPrimitive.Triangles();
        geom1.setPositions(NDArrays.of(CRS2D,
                0.0, 0.0,  1.0, 0.0,  0.0, 1.0,
                1.0, 0.0,  1.0, 1.0,  0.0, 1.0
                ));
        geom1.setAttribute("test", NDArrays.of(1,
                0.0, 1.0, 2.0,
                1.0, 8.0, 2.0
                ));

        final MeshPrimitive geom2 = new MeshPrimitive.Points();
        geom2.setPositions(NDArrays.of(CRS2D,
                0.2, 0.2,
                0.5, 0.5,
                1.0, 1.0,
                0.5, 2.0
                ));

        final Geometry intersection = new GeometryProcessor().intersection(geom1, geom2);
        assertTrue(intersection instanceof MeshPrimitive.Points);
        final MeshPrimitive result = (MeshPrimitive) intersection;
        final Array positions = result.getPositions();
        final Array test = result.getAttribute("test");

        assertArrayEquals(new double[]{
                0.2, 0.2,
                0.5, 0.5,
                1.0, 1.0
        }, positions.toArrayDouble(), 0.0);
        assertArrayEquals(new double[]{
                0.6,
                1.5,
                8.0,
        }, test.toArrayDouble(), 0.0001);
    }

    /**
     * Test primitives triangles against primitive lines.
     * - 2 triangles.
     * - 3 lines, one inside triangles, one crossing outward, one outside
     *
     *   2-4  5
     *    +---+
     *    |\ ++--+
     *    |+-+|
     *    |  \|   +--+
     *    +---+
     *    0  1-3
     */
    @Test
    public void testPrimitiveTrianglesPrimitiveLines() {

        final MeshPrimitive geom1 = new MeshPrimitive.Triangles();
        geom1.setPositions(NDArrays.of(CRS2D,
                0.0, 0.0,  1.0, 0.0,  0.0, 1.0,
                1.0, 0.0,  1.0, 1.0,  0.0, 1.0
                ));
        geom1.setAttribute("test", NDArrays.of(1,
                0.0, 1.0, 2.0,
                1.0, 8.0, 2.0
                ));

        final MeshPrimitive geom2 = new MeshPrimitive.Lines();
        geom2.setPositions(NDArrays.of(CRS2D,
                0.1, 0.5,  0.9, 0.5, //crossing 2 triangles
                0.8, 0.9,  1.2, 0.9, //crossout 1 triangle
                3.0, 0.2,  4.0, 0.2  //outside
                ));

        final Geometry intersection = new GeometryProcessor().intersection(geom1, geom2);
        assertTrue(intersection instanceof MeshPrimitive.Lines);
        final MeshPrimitive.Lines result = (MeshPrimitive.Lines) intersection;
        final Array positions = result.getPositions();
        final Array test = result.getAttribute("test");

        assertArrayEquals(new double[]{
                //first line, cut in two
                0.1, 0.5,  0.5, 0.5,
                0.5, 0.5,  0.9, 0.5,
                //second line, truncated
                0.8, 0.9,  1.0, 0.9
                //no third line
        }, positions.toArrayDouble(), 0.0001);
        assertArrayEquals(new double[]{
                1.1, 1.5,
                1.5, 3.9,
                6.1, 7.3
        }, test.toArrayDouble(), 0.0001);
    }

    /**
     * The inputs and expected result of a single test of {@code intersection(Geometry, Geometry)}.
     *
     * @param input    the geometry on which the operation is invoked.
     * @param other    the other operand.
     * @param expected the expected result, or {@code null} if an exception is expected.
     * @param error    the type of the expected exception, or {@code null} if the operation should succeed.
     */
    private record TestCase(Geometry input,
                            Geometry other,
                            Geometry expected,
                            Class<? extends Exception> error)
    {
    }

    /**
     * All test cases of {@code intersection(Geometry, Geometry)}.
     */
    private static final TestCase[] ENTRIES = {
        // ∅ ∩ A = ∅ and A ∩ ∅ = ∅: the result is the operand which is already empty.
        new TestCase(EMPTY_1,   NON_EMPTY,   EMPTY_1,      null),
        new TestCase(NON_EMPTY, EMPTY_1,     EMPTY_1,      null),
        new TestCase(EMPTY_1,   EMPTY_2,     EMPTY_1,      null),
        // points
        new TestCase(POINT_A,   POINT_A_BIS, POINT_A,      null),
        new TestCase(POINT_A,   POINT_B,     EMPTY_RESULT, null)
    };

    /**
     * Tests {@code intersection(Geometry, Geometry)} on all declared test cases.
     */
    @Test
    public void testIntersection() {
        for (final TestCase entry : ENTRIES) {
            try {
                final Geometry result = new GeometryProcessor().intersection(entry.input(), entry.other());
                assertNull(entry.error(), "An exception was expected.");
                assertEquals(entry.expected(), result);
            } catch (Exception ex) {
                if (entry.error() == null || !entry.error().isInstance(ex)) {
                    throw new AssertionError("Unexpected exception for " + entry, ex);
                }
            }
        }
    }

    /**
     * Tests {@code intersection(Geometry, Geometry)} on two surfaces.
     * What two surfaces have in common is the surface covered by both of them.
     */
    @Test
    public void testIntersectionOfSurfaces() {
        final GeometryProcessor processor = new GeometryProcessor();
        assertEquals(25, areaOf(processor.intersection(SQUARE, SQUARE_OVERLAP)), TOLERANCE, "Two squares of 10 × 10 offset by 5 along both axes.");
        assertEquals(4, areaOf(processor.intersection(SQUARE, SQUARE_INNER)), TOLERANCE, "A square contained in another one.");
        assertTrue(processor.intersection(SQUARE, SQUARE_DISJOINT).isEmpty(), "Two disjoint squares have nothing in common.");
        /*
         * Two surfaces sharing only an edge have a curve in common, not a surface:
         * the dimension of the result is lower than the dimension of the operands.
         */
        final Geometry edge = processor.intersection(SQUARE, SQUARE_TOUCHING);
        assertFalse(edge.isEmpty(), "Two surfaces sharing an edge have that edge in common.");
        assertEquals(1, edge.getTopologicDimension(), "What two surfaces sharing only an edge have in common is a curve, not a surface.");
    }

    private static Vector<?> v(final double... coordinates) {
        return Vector.of(coordinates);
    }

    private static void assertRayPoint(final double t, final RayInterval actual) {
        assertTrue(actual.isPoint(), "Expected a single point.");
        assertEquals(t, actual.start(), TOLERANCE);
    }

    private static void assertRayInterval(final double start, final double end, final RayInterval actual) {
        assertFalse(actual.isEmpty(), "Expected an interval.");
        assertEquals(start, actual.start(), TOLERANCE);
        assertEquals(end,   actual.end(),   TOLERANCE);
    }

    /**
     * Verifies the position and the {@value #ATT_TEST} attribute of the given point.
     */
    private static void assertAttributedPoint(double x, double y, double z, double attribute, Geometry actual) {
        final Point point = assertInstanceOf(Point.class, actual);
        assertArrayEquals(new double[] {x, y, z}, point.getPosition().toArrayDouble(), TOLERANCE);
        assertEquals(attribute, point.getAttribute(ATT_TEST).get(0), TOLERANCE, "attribute");
    }

    /**
     * Tests {@link Intersection#rayPlane(Vector, Vector, Vector, Vector, double)} in 3 and 2 dimensions.
     */
    @Test
    public void testRayPlane() {
        final Vector<?> origin = v(0, 0, 0);
        final Vector<?> plane  = v(0, 0, 5);
        final Vector<?> up     = v(0, 0, 2);      // Not a unit vector, on purpose.
        assertRayPoint(5,   Intersection.rayPlane(origin, v(0, 0, 1), plane, up, 0));
        assertRayPoint(2.5, Intersection.rayPlane(origin, v(0, 0, 2), plane, up, 0));
        assertRayPoint(5,   Intersection.rayPlane(origin, v(1, 0, 1), plane, up, 0));
        assertTrue(Intersection.rayPlane(origin, v(0, 0, -1), plane, up, 0).isEmpty(), "Plane behind the ray.");
        assertTrue(Intersection.rayPlane(origin, v(1, 0,  0), plane, up, 0).isEmpty(), "Parallel to the plane.");

        final Vector<?> inside = v(3, 2, 5);
        assertRayPoint(0, Intersection.rayPlane(inside, v(0, 0, 1), plane, up, 0));
        assertRayInterval(0, Double.POSITIVE_INFINITY, Intersection.rayPlane(inside, v(1, 1, 0), plane, up, 0));
        assertRayPoint(0, Intersection.rayPlane(v(3, 2, 5 + 1E-12), v(0, 0, 1), plane, up, 1E-10));

        assertRayPoint(3, Intersection.rayPlane(v(0, 0), v(1, 0), v(3, 7), v(-1, 0), 0));
        assertThrows(IllegalArgumentException.class, () -> Intersection.rayPlane(origin, v(0, 0, 0), plane, up, 0));
        assertThrows(IllegalArgumentException.class, () -> Intersection.rayPlane(origin, up, plane, v(0, 0, 0), 0));
    }

    /**
     * Tests {@link Intersection#rayTriangle(Vector, Vector, Vector, Vector, Vector, double)} with a ray
     * crossing a triangle inside, on an edge, on a corner, or not at all.
     */
    @Test
    public void testRayTriangle() {
        final Vector<?> a = v(0, 0, 5), b = v(4, 0, 5), c = v(0, 4, 5);
        final Vector<?> up = v(0, 0, 2);
        assertRayPoint(2.5, Intersection.rayTriangle(v(1, 1, 0), up, a, b, c, 0));
        assertRayPoint(2.5, Intersection.rayTriangle(v(2, 0, 0), up, a, b, c, 0));     // On an edge.
        assertRayPoint(2.5, Intersection.rayTriangle(v(0, 0, 0), up, a, b, c, 0));     // On a corner.
        assertRayPoint(2.5, Intersection.rayTriangle(v(2, 2, 0), up, a, b, c, 0));     // On the hypotenuse.
        assertRayPoint(0,   Intersection.rayTriangle(v(1, 1, 5), up, a, b, c, 0));     // Starting on the triangle.
        assertRayPoint(5,   Intersection.rayTriangle(v(-4, 1, 0), v(1, 0, 1), a, b, c, 0));
        assertTrue(Intersection.rayTriangle(v(3, 3, 0), up, a, b, c, 0).isEmpty(), "Outside the triangle.");
        assertTrue(Intersection.rayTriangle(v(1, 1, 0), v(0, 0, -1), a, b, c, 0).isEmpty(), "Triangle behind the ray.");
        assertTrue(Intersection.rayTriangle(v(1, 1, 6), up, a, b, c, 0).isEmpty(), "Ray starting after the triangle.");
        /*
         * Triangles which are not parallel to a coordinate plane.
         */
        assertRayPoint(1, Intersection.rayTriangle(v(0, 0, 0), v(1, 1, 1), v(3, 0, 0), v(0, 3, 0), v(0, 0, 3), 0));
        assertRayPoint(2, Intersection.rayTriangle(v(0, 0, 0), v(1, 0, 0.25), v(2, -1, 0), v(2, 1, -1), v(2, 0, 2), 0));
        assertTrue(Intersection.rayTriangle(v(0, 0, 0), v(1, 0, 1.5), v(2, -1, 0), v(2, 1, -1), v(2, 0, 2), 0).isEmpty());
    }

    /**
     * Tests {@link Intersection#rayTriangle(Vector, Vector, Vector, Vector, Vector, double)} with a ray
     * in the plane of the triangle, which touches it along a segment or not at all.
     */
    @Test
    public void testRayTriangleCoplanar() {
        final Vector<?> a = v(0, 0, 5), b = v(4, 0, 5), c = v(0, 4, 5);
        final Vector<?> east = v(1, 0, 0);
        assertRayInterval(1, 4, Intersection.rayTriangle(v(-1, 1, 5), east, a, b, c, 0));
        assertRayInterval(0, 2, Intersection.rayTriangle(v( 1, 1, 5), east, a, b, c, 0));
        assertTrue(Intersection.rayTriangle(v(5, 1, 5), east, a, b, c, 0).isEmpty(), "Triangle behind the ray.");
        assertTrue(Intersection.rayTriangle(v(0, 0, 0), east, a, b, c, 0).isEmpty(), "Parallel, not coplanar.");
        assertTrue(Intersection.rayTriangle(v(0, 0, 0), east, a, b, v(8, 0, 5), 0).isEmpty(), "Degenerate triangle.");
    }

    /**
     * Tests {@link Intersection#raySegment(Vector, Vector, Vector, Vector, double)} with segments
     * crossing the ray, along the ray, or not touching it.
     */
    @Test
    public void testRaySegment() {
        final Vector<?> origin = v(0, 0, 0);
        final Vector<?> east = v(1, 0, 0);
        assertRayPoint(2, Intersection.raySegment(origin, east, v(2, -1, 0), v(2, 1, 0), 1E-12));
        assertRayPoint(1, Intersection.raySegment(origin, v(2, 0, 0), v(2, -1, 0), v(2, 1, 0), 1E-12));
        assertRayPoint(2, Intersection.raySegment(origin, east, v(2, -1, 1E-13), v(2, 1, 1E-13), 1E-12));
        assertTrue(Intersection.raySegment(origin, east, v(2, -1, 1), v(2, 1, 1), 1E-12).isEmpty(), "Skew segment.");
        assertTrue(Intersection.raySegment(origin, east, v(-2, -1, 0), v(-2, 1, 0), 1E-12).isEmpty(), "Segment behind.");
        assertTrue(Intersection.raySegment(origin, east, v(2, 1, 0), v(2, 3, 0), 1E-12).isEmpty(), "Segment beside.");

        assertRayInterval(3, 5, Intersection.raySegment(origin, east, v(5, 0, 0), v(3, 0, 0), 1E-12));
        assertRayInterval(0, 2, Intersection.raySegment(origin, east, v(-1, 0, 0), v(2, 0, 0), 1E-12));
        assertRayPoint(0, Intersection.raySegment(origin, east, v(-1, 0, 0), v(0, 0, 0), 1E-12));
        assertTrue(Intersection.raySegment(origin, east, v(-3, 0, 0), v(-1, 0, 0), 1E-12).isEmpty(), "Collinear behind.");

        assertRayPoint(3, Intersection.raySegment(v(0, 0), v(1, 1), v(0, 6), v(6, 0), 1E-12));
    }

    /**
     * Tests the intersection of a ray with a triangle, the attributes of the triangle being interpolated.
     */
    @Test
    public void testIntersectionOfRayAndTriangle() {
        final GeometryProcessor processor = new GeometryProcessor();
        assertAttributedPoint(1, 2, 5, 22.5, processor.intersection(ray(1, 2, 0, 0, 0, 1), TRIANGLE_3D));
        assertAttributedPoint(1, 2, 5, 22.5, processor.intersection(TRIANGLE_3D, ray(1, 2, 0, 0, 0, 1)));
        assertTrue(processor.intersection(ray(3, 3, 0, 0, 0, 1), TRIANGLE_3D).isEmpty());

        final LineString inside = assertInstanceOf(LineString.class, processor.intersection(ray(-1, 1, 5, 1, 0, 0), TRIANGLE_3D));
        assertEquals(2, inside.getNumPoints());
        assertAttributedPoint(0, 1, 5, 15,   inside.getPointN(0));
        assertAttributedPoint(3, 1, 5, 22.5, inside.getPointN(1));
    }

    /**
     * Tests the intersection of a ray with hyperplanes and planes.
     */
    @Test
    public void testIntersectionOfRayAndPlane() {
        final GeometryProcessor processor = new GeometryProcessor();
        final Point point = assertInstanceOf(Point.class, processor.intersection(ray(1, 2, 0, 0, 1, 1), PLANE_Z5));
        assertArrayEquals(new double[] {1, 7, 5}, point.getPosition().toArrayDouble(), TOLERANCE);
        assertInstanceOf(Ray.class, processor.intersection(ray(1, 2, 5, 1, 0, 0), PLANE_Z5));
        assertTrue(processor.intersection(ray(1, 2, 0, 0, 0, -1), PLANE_Z5).isEmpty());

        final Plane plane = new Plane(CRS_3D, v(0, 0, 5), v(0, 0, 1));
        final Point onPlane = assertInstanceOf(Point.class, processor.intersection(ray(1, 2, 0, 0, 0, 1), plane));
        assertArrayEquals(new double[] {1, 2, 5}, onPlane.getPosition().toArrayDouble(), TOLERANCE);
    }

    /**
     * Tests the intersection of a ray with a closed line string, which the ray crosses inside two segments,
     * through two corners including the closing one, or along a segment. The attributes are interpolated.
     */
    @Test
    public void testIntersectionOfRayAndLineString() {
        final GeometryProcessor processor = new GeometryProcessor();
        final GeometryCollection<?> crossings = assertInstanceOf(GeometryCollection.class,
                processor.intersection(ray(-1, 2, 1, 1, 0, 0), RING_3D));
        assertEquals(2, crossings.getNumGeometries());
        assertAttributedPoint(0, 2, 1, 20, crossings.getGeometryN(0));
        assertAttributedPoint(4, 2, 1, 15, crossings.getGeometryN(1));

        final GeometryCollection<?> corners = assertInstanceOf(GeometryCollection.class,
                processor.intersection(ray(-1, -1, 1, 1, 1, 0), RING_3D));
        assertEquals(2, corners.getNumGeometries());
        assertAttributedPoint(0, 0, 1, 0,  corners.getGeometryN(0));
        assertAttributedPoint(4, 4, 1, 20, corners.getGeometryN(1));

        final LineString along = assertInstanceOf(LineString.class, processor.intersection(ray(-1, 0, 1, 1, 0, 0), RING_3D));
        assertAttributedPoint(0, 0, 1, 0,  along.getPointN(0));
        assertAttributedPoint(4, 0, 1, 10, along.getPointN(1));

        assertTrue(processor.intersection(ray(-1, 2, 2, 1, 0, 0), RING_3D).isEmpty());
    }
}
