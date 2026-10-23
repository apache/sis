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
package org.apache.sis.maths;

import java.util.Arrays;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class MathsTest {

    private static final double EPSILON = 1e-6;

    @Test
    public void testProjectionRatio() {

        double x1,y1,x2,y2;

        /*
          + +

          S-E
        */
        x1 = 0.0;
        y1 = 0.0;
        x2 = 1.0;
        y2 = 0.0;
        assertEquals(-7.0, Maths.projectionRatio(x1, y1, x2, y2,-7.0, 0.0), EPSILON);
        assertEquals(-2.0, Maths.projectionRatio(x1, y1, x2, y2,-2.0, 0.0), EPSILON);
        assertEquals( 0.0, Maths.projectionRatio(x1, y1, x2, y2, 0.0, 0.0), EPSILON);
        assertEquals( 0.5, Maths.projectionRatio(x1, y1, x2, y2, 0.5, 0.0), EPSILON);
        assertEquals( 1.0, Maths.projectionRatio(x1, y1, x2, y2, 1.0, 0.0), EPSILON);
        assertEquals( 2.0, Maths.projectionRatio(x1, y1, x2, y2, 2.0, 0.0), EPSILON);
        assertEquals( 7.0, Maths.projectionRatio(x1, y1, x2, y2, 7.0, 0.0), EPSILON);
        /*
          E +
          |
          S +
        */
        x1 = 0.0;
        y1 = 0.0;
        x2 = 0.0;
        y2 = 1.0;
        assertEquals(-7.0, Maths.projectionRatio(x1, y1, x2, y2, 0.0,-7.0), EPSILON);
        assertEquals(-2.0, Maths.projectionRatio(x1, y1, x2, y2, 0.0,-2.0), EPSILON);
        assertEquals( 0.0, Maths.projectionRatio(x1, y1, x2, y2, 0.0, 0.0), EPSILON);
        assertEquals( 0.5, Maths.projectionRatio(x1, y1, x2, y2, 0.0, 0.5), EPSILON);
        assertEquals( 1.0, Maths.projectionRatio(x1, y1, x2, y2, 0.0, 1.0), EPSILON);
        assertEquals( 2.0, Maths.projectionRatio(x1, y1, x2, y2, 0.0, 2.0), EPSILON);
        assertEquals( 7.0, Maths.projectionRatio(x1, y1, x2, y2, 0.0, 7.0), EPSILON);
        /*
          + E
           /
          S +
        */
        x1 = 0.0;
        y1 = 0.0;
        x2 = 1.0;
        y2 = 1.0;
        assertEquals(-7.0, Maths.projectionRatio(x1, y1, x2, y2,-7.0,-7.0), EPSILON);
        assertEquals(-2.0, Maths.projectionRatio(x1, y1, x2, y2,-2.0,-2.0), EPSILON);
        assertEquals( 0.0, Maths.projectionRatio(x1, y1, x2, y2, 0.0, 0.0), EPSILON);
        assertEquals( 1.0, Maths.projectionRatio(x1, y1, x2, y2, 1.0, 1.0), EPSILON);
        assertEquals( 2.0, Maths.projectionRatio(x1, y1, x2, y2, 2.0, 2.0), EPSILON);
        assertEquals( 7.0, Maths.projectionRatio(x1, y1, x2, y2, 7.0, 7.0), EPSILON);
        /*
          + S
           /
          E +
        */
        x1 = 10.0;
        y1 = 15.0;
        x2 = 5.0;
        y2 = 10.0;
        assertEquals(-1.0, Maths.projectionRatio(x1, y1, x2, y2,15.0,20.0), EPSILON);
        assertEquals( 0.0, Maths.projectionRatio(x1, y1, x2, y2,10.0,15.0), EPSILON);
        assertEquals( 1.0, Maths.projectionRatio(x1, y1, x2, y2, 5.0,10.0), EPSILON);
        assertEquals( 2.0, Maths.projectionRatio(x1, y1, x2, y2, 0.0, 5.0), EPSILON);

    }

    @Test
    public void testDistanceSquare() {

        { /* Point exactly on the segment (same Y), to ensure line detection works,
             otherwise we obtain a distance of 1.4551915228366852E-11 */
            double distance = Maths.distanceSquare(
                976136.1298301395,5118355.120854909,
                976587.6962741626,5118355.120854909,
                976420.4186063276,5118355.120854909);
            assertEquals(0.0, distance, 0.0);
        }

        { /* Point exactly on the segment (same X), to ensure line detection works,
             otherwise we obtain a distance of 1.4551915228366852E-11 */
            double distance = Maths.distanceSquare(
                5118355.120854909,976136.1298301395,
                5118355.120854909,976587.6962741626,
                5118355.120854909,976420.4186063276);
            assertEquals(0.0, distance, 0.0);
        }

    }

    /**
     * Returns a vector of the given values.
     */
    private static Vector<?> vector(final double... values) {
        return Vectors.create(values, DataType.DOUBLE);
    }

    /**
     * Tests the robust {@link Maths#inTriangle(Vector, Vector, Vector, Vector)},
     * which includes the edges and corners of the triangle, whatever its orientation.
     */
    @Test
    public void testInTriangle() {
        final Vector<?> a = vector(0, 0), b = vector(4, 0), c = vector(0, 4);
        for (final Vector<?>[] triangle : new Vector<?>[][] {{a, b, c}, {a, c, b}}) {
            assertTrue (Maths.inTriangle(triangle[0], triangle[1], triangle[2], vector(1, 1)), "Inside");
            assertTrue (Maths.inTriangle(triangle[0], triangle[1], triangle[2], vector(2, 0)), "On an edge");
            assertTrue (Maths.inTriangle(triangle[0], triangle[1], triangle[2], vector(4, 0)), "On a corner");
            assertFalse(Maths.inTriangle(triangle[0], triangle[1], triangle[2], vector(3, 3)), "Outside");
            assertFalse(Maths.inTriangle(triangle[0], triangle[1], triangle[2], vector(-1, 1)), "Outside");
        }
        //a degenerate triangle contains the points of its line between its corners.
        final Vector<?> d = vector(2, 2), e = vector(4, 4);
        assertTrue (Maths.inTriangle(a, d, e, vector(1, 1)));
        assertFalse(Maths.inTriangle(a, d, e, vector(5, 5)));
        assertFalse(Maths.inTriangle(a, d, e, vector(1, 0)));
    }

    /**
     * Tests {@link Maths#isConvex(Vector, Vector, Vector, boolean)}.
     */
    @Test
    public void testIsConvex() {
        //the counterclockwise ring (0,0), (4,0), (4,4) turns left at (4,0).
        assertTrue (Maths.isConvex(vector(0,0), vector(4,0), vector(4,4), false));
        assertFalse(Maths.isConvex(vector(0,0), vector(4,0), vector(4,4), true));
        assertTrue (Maths.isConvex(vector(4,4), vector(4,0), vector(0,0), true), "Same ring traversed clockwise");
        //aligned vertices are not convex.
        assertFalse(Maths.isConvex(vector(0,0), vector(2,0), vector(4,0), false));
        assertFalse(Maths.isConvex(vector(0,0), vector(2,0), vector(4,0), true));
    }

    /**
     * Tests {@link Maths#inCone(Vector, Vector, Vector, Vector, boolean)}
     * at a convex vertex and at a reflex vertex, in counterclockwise and clockwise rings.
     */
    @Test
    public void testInCone() {
        //convex vertex (0,0) of the counterclockwise square (0,0), (4,0), (4,4), (0,4).
        final Vector<?> vertex = vector(0,0), previous = vector(0,4), next = vector(4,0);
        assertTrue (Maths.inCone(previous, vertex, next, vector( 2,  2), false));
        assertFalse(Maths.inCone(previous, vertex, next, vector(-1, -1), false));
        assertFalse(Maths.inCone(previous, vertex, next, vector( 2,  0), false), "Along an edge");
        assertTrue (Maths.inCone(next, vertex, previous, vector( 2,  2), true), "Same ring traversed clockwise");
        //reflex vertex (2,1) of the counterclockwise ring (0,0), (4,0), (4,4), (2,1), (0,4), with a notch above it.
        final Vector<?> reflex = vector(2,1), before = vector(4,4), after = vector(0,4);
        assertTrue (Maths.inCone(before, reflex, after, vector(2, 0), false));
        assertFalse(Maths.inCone(before, reflex, after, vector(2, 3), false), "In the notch");
        assertTrue (Maths.inCone(after, reflex, before, vector(2, 0), true), "Same ring traversed clockwise");
    }

    /**
     * Tests {@link Maths#calculatePolygonNormal(Array)}, whose length is twice the area of the polygon.
     */
    @Test
    public void testCalculatePolygonNormal() {
        //a counterclockwise square in the (x,y) plane, closed or not.
        assertArrayEquals(new double[] {0, 0, 8}, Maths.calculatePolygonNormal(
                NDArrays.of(3, new double[] {0,0,0, 2,0,0, 2,2,0, 0,2,0, 0,0,0})), EPSILON);
        assertArrayEquals(new double[] {0, 0, 8}, Maths.calculatePolygonNormal(
                NDArrays.of(3, new double[] {0,0,0, 2,0,0, 2,2,0, 0,2,0})), EPSILON);
        //a vertical square in the (x,z) plane, with collinear points.
        assertArrayEquals(new double[] {0, -8, 0}, Maths.calculatePolygonNormal(
                NDArrays.of(3, new double[] {0,1,0, 1,1,0, 2,1,0, 2,1,2, 0,1,2, 0,1,0})), EPSILON);
        assertThrows(IllegalArgumentException.class, () -> Maths.calculatePolygonNormal(NDArrays.of(2, new double[] {0,0, 1,0, 0,1})));
    }

    /**
     * Tests {@link Maths#projectionAxes(double[])} on the normals of the six faces of a cube.
     * The axis left out is the one of the normal, and the order of the two others depends on its sign.
     */
    @Test
    public void testProjectionAxes() {
        assertArrayEquals(new int[] {0, 1, 2}, Maths.projectionAxes(new double[] { 0,  0,  1}));
        assertArrayEquals(new int[] {1, 0, 2}, Maths.projectionAxes(new double[] { 0,  0, -1}));
        assertArrayEquals(new int[] {1, 2, 0}, Maths.projectionAxes(new double[] { 1,  0,  0}));
        assertArrayEquals(new int[] {2, 1, 0}, Maths.projectionAxes(new double[] {-1,  0,  0}));
        assertArrayEquals(new int[] {2, 0, 1}, Maths.projectionAxes(new double[] { 0,  1,  0}));
        assertArrayEquals(new int[] {0, 2, 1}, Maths.projectionAxes(new double[] { 0, -1,  0}));
        assertArrayEquals(new int[] {0, 1, 2}, Maths.projectionAxes(new double[] { 0,  0,  0}), "A zero normal gives the (x,y) plane.");
        assertArrayEquals(new int[] {0, 1, 2}, Maths.projectionAxes(new double[] { 1,  1,  1}), "Ties leave out z first.");
        assertThrows(IllegalArgumentException.class, () -> Maths.projectionAxes(new double[] {0, 1}));
    }

    /**
     * Tests {@link Maths#planeSide(double[], double[], double[], double[], double)} on a horizontal triangle,
     * with and without tolerance, and on a triangle in an oblique plane with coordinates which are not integers.
     */
    @Test
    public void testPlaneSide() {
        final double[] a = {0, 0, 0}, b = {1, 0, 0}, c = {0, 1, 0};
        assertEquals( 1, Maths.planeSide(a, b, c, new double[] {0.3, 0.3,  1}, 0), "Above, toward the normal.");
        assertEquals(-1, Maths.planeSide(a, b, c, new double[] {0.3, 0.3, -1}, 0), "Below.");
        assertEquals(-1, Maths.planeSide(a, c, b, new double[] {0.3, 0.3,  1}, 0), "Reversed triangle.");
        assertEquals( 0, Maths.planeSide(a, b, c, new double[] {5,   5,    0}, 0), "On the plane, outside of the triangle.");
        assertEquals( 1, Maths.planeSide(a, b, c, new double[] {0.3, 0.3, 1E-12}, 0));
        assertEquals( 0, Maths.planeSide(a, b, c, new double[] {0.3, 0.3, 1E-12}, 1E-9), "Within the tolerance.");
        assertEquals( 1, Maths.planeSide(a, b, c, new double[] {0.3, 0.3, 2E-9},  1E-9), "Beyond the tolerance.");
        assertEquals( 0, Maths.planeSide(a, b, new double[] {2, 0, 0}, new double[] {0, 0, 1}, 0), "Degenerate triangle.");
        /*
         * A triangle made of two orthonormal vectors u and v of an oblique plane, whose normal is u × v.
         */
        final double[] o = {0.1, 0.2, 0.3}, u = {1/3., 2/3., 2/3.}, v = {2/3., 1/3., -2/3.};
        final double[] n = Vectors.cross(u, v);
        final double[] ou = Vectors.add(o, u), ov = Vectors.add(o, v);
        final double[] inPlane = Vectors.add(o, Vectors.add(Vectors.scale(u, 0.4), Vectors.scale(v, 0.3)));
        assertEquals( 1, Maths.planeSide(o, ou, ov, Vectors.add(inPlane, Vectors.scale(n,  1E-3)), 0));
        assertEquals(-1, Maths.planeSide(o, ou, ov, Vectors.add(inPlane, Vectors.scale(n, -1E-3)), 0));
        assertEquals( 0, Maths.planeSide(o, ou, ov, Vectors.add(inPlane, Vectors.scale(n,  1E-3)), 1E-2));
        assertEquals( 0, Maths.planeSide(o, ou, ov, inPlane, 1E-12), "Rounding errors within the tolerance.");
    }

    /**
     * Tests that {@link Maths#projectionAxes(double[])} keeps the orientation: a triangle whose normal has the
     * direction given to the method is counterclockwise once projected, and the reversed triangle clockwise.
     */
    @Test
    public void testProjectionAxesOrientation() {
        final double[][] normals = {
            {0, 0, 1}, {0, 0, -1}, {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0},
            {1, 2, 3}, {-3, 1, -2}, {0.5, -4, 0.1}, {-0.2, 0.3, -0.25}
        };
        for (final double[] normal : normals) {
            //two vectors of the plane such as t1 × t2 has the direction of the normal.
            final double[] other = (Math.abs(normal[0]) < 0.9 * Vectors.length(normal)) ? new double[] {1, 0, 0} : new double[] {0, 1, 0};
            final double[] t1 = Vectors.cross(normal, other);
            final double[] t2 = Vectors.cross(normal, t1);
            final Vector<?> a = vector(0.3, -1.7, 2.1);
            final Vector<?> b = vector(a.get(0) + t1[0], a.get(1) + t1[1], a.get(2) + t1[2]);
            final Vector<?> c = vector(a.get(0) + t2[0], a.get(1) + t2[1], a.get(2) + t2[2]);
            final int[] axes = Maths.projectionAxes(normal);
            final String message = Arrays.toString(normal);
            assertTrue(Maths.lineSide(a.swizzle(axes), b.swizzle(axes), c.swizzle(axes)) > 0, message);
            assertTrue(Maths.lineSide(a.swizzle(axes), c.swizzle(axes), b.swizzle(axes)) < 0, message);
            assertEquals(Math.abs(normal[axes[2]]), Math.max(Math.abs(normal[0]), Math.max(Math.abs(normal[1]), Math.abs(normal[2]))),
                         message + ": the axis left out shall be the largest component of the normal.");
        }
    }

    @Test
    public void testNormalizedByte() {
        assertEquals(-1.0, Maths.normalizedByte(Byte.MIN_VALUE), 0.0);
        assertEquals(-1.0, Maths.normalizedByte((byte)-127), 0.0);
        assertEquals(0.0, Maths.normalizedByte((byte) 0), 0.0);
        assertEquals(1.0, Maths.normalizedByte(Byte.MAX_VALUE), 0.0);
    }

    @Test
    public void testNormalizedUByte() {
        assertEquals(0.0, Maths.normalizedUByte((byte) 0), 0.0);
        assertEquals(1.0, Maths.normalizedUByte((byte) 0xFF), 0.0);
    }

    @Test
    public void testNormalizedShort() {
        assertEquals(-1.0, Maths.normalizedShort(Short.MIN_VALUE), 0.0);
        assertEquals(-1.0, Maths.normalizedShort((short)-32767), 0.0);
        assertEquals(0.0, Maths.normalizedShort((short) 0), 0.0);
        assertEquals(1.0, Maths.normalizedShort(Short.MAX_VALUE), 0.0);
    }

    @Test
    public void testNormalizedUShort() {
        assertEquals(0.0, Maths.normalizedUShort((short) 0), 0.0);
        assertEquals(1.0, Maths.normalizedUShort((short) 0xFFFF), 0.0);
    }

    @Test
    public void testNormalizedInt() {
        assertEquals(-1.0, Maths.normalizedInt(Integer.MIN_VALUE), 0.0);
        assertEquals(-1.0, Maths.normalizedInt(Integer.MIN_VALUE+1), 0.0);
        assertEquals(0.0, Maths.normalizedInt(0), 0.0);
        assertEquals(1.0, Maths.normalizedInt(Integer.MAX_VALUE), 0.0);
    }

    @Test
    public void testNormalizedUInt() {
        assertEquals(0.0, Maths.normalizedUInt(0), 0.0);
        assertEquals(1.0, Maths.normalizedUInt(0xFFFFFFFF), 0.0);
    }

    @Test
    public void testNormalizedLong() {
        assertEquals(-1.0, Maths.normalizedLong(Long.MIN_VALUE), 0.0);
        assertEquals(-1.0, Maths.normalizedLong(Long.MIN_VALUE+1), 0.0);
        assertEquals(0.0, Maths.normalizedLong(0), 0.0);
        assertEquals(1.0, Maths.normalizedLong(Long.MAX_VALUE), 0.0);
    }

    @Test
    public void testNormalizedULong() {
        assertEquals(0.0, Maths.normalizedULong(0L), 0.0);
        assertEquals(1.0, Maths.normalizedULong(~0L), 0.0);
    }

    @Test
    public void testToNormalizedByte() {
        assertEquals(Byte.MIN_VALUE+1, Maths.toNormalizedByte(-1.0));
        assertEquals(0, Maths.toNormalizedByte(0.0));
        assertEquals(Byte.MAX_VALUE, Maths.toNormalizedByte(1.0));
    }

    @Test
    public void testToNormalizedUByte() {
        assertEquals((byte) 0, Maths.toNormalizedUByte(0.0));
        assertEquals((byte) 0xFF, Maths.toNormalizedUByte(1.0));
    }

    @Test
    public void testToNormalizedShort() {
        assertEquals(Short.MIN_VALUE+1, Maths.toNormalizedShort(-1.0));
        assertEquals((short) 0, Maths.toNormalizedByte(0.0));
        assertEquals(Short.MAX_VALUE, Maths.toNormalizedShort(1.0));
    }

    @Test
    public void testToNormalizedUShort() {
        assertEquals((short) 0, Maths.toNormalizedUShort(0.0));
        assertEquals((short) 0xFFFF, Maths.toNormalizedUShort(1.0));
    }

    @Test
    public void testToNormalizedInt() {
        assertEquals(Integer.MIN_VALUE+1, Maths.toNormalizedInt(-1.0));
        assertEquals(0, Maths.toNormalizedInt(0.0));
        assertEquals(Integer.MAX_VALUE, Maths.toNormalizedInt(1.0));
    }

    @Test
    public void testToNormalizedUInt() {
        assertEquals(0, Maths.toNormalizedUInt(0.0));
        assertEquals(0xFFFFFFFF, Maths.toNormalizedUInt(1.0));
    }

    @Test
    public void testToNormalizedLong() {
        assertEquals(Long.MIN_VALUE, Maths.toNormalizedLong(-1.0));
        assertEquals(0L, Maths.toNormalizedLong(0.0));
        assertEquals(Long.MAX_VALUE, Maths.toNormalizedLong(1.0));
    }

    @Test
    public void testToNormalizedULong() {
        assertEquals(0, Maths.toNormalizedULong(0.0));
        assertEquals(Long.MIN_VALUE, Maths.toNormalizedULong(0.5));
        assertEquals(0xFFFFFFFFFFFFFFFFL, Maths.toNormalizedULong(1.0));
    }

    /**
     * A known double precisions mathematical problem.
     */
    @Test
    public void testInCircle() {

        {
            final double ax = 8.64735;
            final double ay = 41.76673;
            final double bx = 8.64841;
            final double by = 41.7668;
            final double cx = 8.64642;
            final double cy = 41.76692;
            final double dx = 8.64685;
            final double dy = 41.7668;

            //double precision arithmetic fails on this
            assertTrue(Maths.inCircleFast(ax, ay, bx, by, dx, dy, cx, cy));
            assertFalse(Maths.inCircleFast(bx, by, dx, dy, ax, ay, cx, cy));
            assertFalse(Maths.inCircleFast(dx, dy, ax, ay, bx, by, cx, cy));
            assertTrue(Maths.inCircleFast(ax, ay, dx, dy, cx, cy, bx, by));
            assertTrue(Maths.inCircleFast(dx, dy, cx, cy, ax, ay, bx, by));
            assertFalse(Maths.inCircleFast(cx, cy, ax, ay, dx, dy, bx, by));

            //check the new method find the right answer
            assertTrue(Maths.inCircle(ax, ay, bx, by, dx, dy, cx, cy));
            assertTrue(Maths.inCircle(bx, by, dx, dy, ax, ay, cx, cy));
            assertTrue(Maths.inCircle(dx, dy, ax, ay, bx, by, cx, cy));
            assertTrue(Maths.inCircle(ax, ay, dx, dy, cx, cy, bx, by));
            assertTrue(Maths.inCircle(dx, dy, cx, cy, ax, ay, bx, by));
            assertTrue(Maths.inCircle(cx, cy, ax, ay, dx, dy, bx, by));
        }

        {
            final double ax = 8.68629;
            final double ay = 41.90324;
            final double bx = 8.6874;
            final double by = 41.90363;
            final double cx = 8.6875;
            final double cy = 41.90367;
            final double dx = 8.68772;
            final double dy = 41.90366;

            //double precision arithmetic fails on this
            assertTrue(Maths.inCircleFast(ax, ay, bx, by, dx, dy, cx, cy));
            assertTrue(Maths.inCircleFast(bx, by, dx, dy, ax, ay, cx, cy));
            assertTrue(Maths.inCircleFast(dx, dy, ax, ay, bx, by, cx, cy));
            assertTrue(Maths.inCircleFast(ax, ay, dx, dy, cx, cy, bx, by));
            assertFalse(Maths.inCircleFast(dx, dy, cx, cy, ax, ay, bx, by));
            assertTrue(Maths.inCircleFast(cx, cy, ax, ay, dx, dy, bx, by));

            //check the new method find the right answer
            assertTrue(Maths.inCircle(ax, ay, bx, by, dx, dy, cx, cy));
            assertTrue(Maths.inCircle(bx, by, dx, dy, ax, ay, cx, cy));
            assertTrue(Maths.inCircle(dx, dy, ax, ay, bx, by, cx, cy));
            assertTrue(Maths.inCircle(ax, ay, dx, dy, cx, cy, bx, by));
            assertTrue(Maths.inCircle(dx, dy, cx, cy, ax, ay, bx, by));
            assertTrue(Maths.inCircle(cx, cy, ax, ay, dx, dy, bx, by));
        }
    }

    /**
     * Same ill-conditioned (nearly collinear) coordinates as
     * {@code ShewchukPredicatesTest#testOrient2dRobustness()} : the naive
     * {@link Maths#lineSideFast} collapses to exactly 0 (falsely reporting the points as
     * collinear), while the robust {@link Maths#lineSide} must fall back and find the true,
     * small but non-zero, signed area.
     */
    @Test
    public void testLineSideRobust() {
        final double ax = -2552049145.4853754, ay = 954889314.1911564;
        final double bx = -8744220500.533537,  by = -8807976600.675346;
        final double cx = -3827380787.3278,    cy = -1055857983.0624254;

        assertEquals(0.0, Maths.lineSideFast(ax,ay, bx,by, cx,cy), 0.0);
        assertEquals(126.54970229522473, Maths.lineSide(ax,ay, bx,by, cx,cy), 0.1);
    }

    /**
     * A known mathematical problem.
     * TODO : find a more robust way to compute this operation.
     */
    @Test
    @Disabled
    public void testIsPointInTriangle_BaryAlgo() {
        //POLYGON ((-88.4521713256836 19.713144302368164, -77.766901 -10.770846, -77.766711 -10.770966, -88.4521713256836 19.713144302368164))
        //POINT (-77.766791 -10.770866)

        assertTrue(Maths.isPointInTriangle_BaryAlgo(
                    -88.4521713256836, 19.713144302368164,
                    -77.766901, -10.770846,
                    -77.766711, -10.770966,
                    -77.766791, -10.770866
            ));
    }

    @Test
    public void testIsPointInTriangle_SideAlgo() {

        assertTrue(Maths.isPointInTriangle_SideAlgo(
                    -88.4521713256836, 19.713144302368164,
                    -77.766901, -10.770846,
                    -77.766711, -10.770966,
                    -77.766791, -10.770866
            ));
    }
}
