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

import java.util.List;
import org.apache.sis.geometries.operation.Intersection.SegmentSegment;
import org.apache.sis.geometries.operation.Intersection.SegmentTriangle;
import org.apache.sis.geometries.operation.Intersection.TriangleTriangle;
import org.apache.sis.maths.Maths;
import org.apache.sis.maths.Vectors;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests the intersections of segments and triangles of {@link Intersection}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class Intersection3DTest {

    private static final double EPS = 1E-12;

    /**
     * The axes of the (x,y) plane.
     */
    private static final int[] XY = {0, 1};

    /**
     * The triangle used by most tests, in the (x,y) plane, counterclockwise seen from above.
     */
    private static final double[] A = {0, 0, 0}, B = {4, 0, 0}, C = {0, 4, 0};

    private static double[] p(final double... coordinates) {
        return coordinates;
    }

    /**
     * Segments crossing, touching at an end, parallel, or apart.
     */
    @Test
    public void testSegmentSegment() {
        SegmentSegment r = Intersection.segmentSegment2D(p(0,0), p(2,2), p(0,2), p(2,0), XY);
        assertEquals(SegmentSegment.Type.POINT, r.type());
        assertEquals(0.5, r.startOnFirst(),  EPS);
        assertEquals(0.5, r.startOnSecond(), EPS);

        r = Intersection.segmentSegment2D(p(0,0), p(1,0), p(1,0), p(1,1), XY);
        assertEquals(SegmentSegment.Type.POINT, r.type(), "Touching at their ends.");
        assertEquals(1, r.startOnFirst(),  "The end of a segment shall be exact.");
        assertEquals(0, r.startOnSecond(), "The end of a segment shall be exact.");

        r = Intersection.segmentSegment2D(p(0,0), p(2,0), p(1,0), p(1,1), XY);
        assertEquals(SegmentSegment.Type.POINT, r.type(), "An end on the other segment.");
        assertEquals(0.5, r.startOnFirst(), EPS);
        assertEquals(0,   r.startOnSecond());

        assertSame(SegmentSegment.NONE, Intersection.segmentSegment2D(p(0,0), p(1,0), p(0,1), p(1,1), XY), "Parallel.");
        assertSame(SegmentSegment.NONE, Intersection.segmentSegment2D(p(0,0), p(1,0), p(2,-1), p(2,1), XY), "Lines crossing apart.");

        //segments in the vertical plane y = 5, in its (x,z) axes.
        r = Intersection.segmentSegment2D(p(0,5,0), p(2,5,2), p(0,5,2), p(2,5,0), new int[] {0, 2});
        assertEquals(SegmentSegment.Type.POINT, r.type());
        assertEquals(0.5, r.startOnFirst(), EPS);
    }

    /**
     * Segments on the same line, overlapping, touching at an end, or apart.
     */
    @Test
    public void testCollinearSegments() {
        SegmentSegment r = Intersection.segmentSegment2D(p(0,0), p(4,0), p(3,0), p(1,0), XY);
        assertEquals(SegmentSegment.Type.OVERLAP, r.type());
        assertEquals(0.25, r.startOnFirst(), EPS);
        assertEquals(0.75, r.endOnFirst(),   EPS);
        assertEquals(1, r.startOnSecond(), "The point at (1,0) is the end of the second segment.");
        assertEquals(0, r.endOnSecond(),   "The point at (3,0) is the start of the second segment.");

        r = Intersection.segmentSegment2D(p(4,0), p(0,0), p(1,0), p(3,0), XY);
        assertEquals(SegmentSegment.Type.OVERLAP, r.type(), "First segment in the opposite direction.");
        assertEquals(0.25, r.startOnFirst(), EPS);
        assertEquals(0.75, r.endOnFirst(),   EPS);
        assertEquals(1, r.startOnSecond());
        assertEquals(0, r.endOnSecond());

        r = Intersection.segmentSegment2D(p(0,0), p(1,1), p(1,1), p(2,2), XY);
        assertEquals(SegmentSegment.Type.POINT, r.type(), "Touching at their ends.");
        assertEquals(1, r.startOnFirst());
        assertEquals(0, r.startOnSecond());

        assertSame(SegmentSegment.NONE, Intersection.segmentSegment2D(p(0,0), p(1,0), p(2,0), p(3,0), XY));
    }

    /**
     * A segment crossing the plane of the triangle inside it, on an edge, on a corner, or outside.
     */
    @Test
    public void testSegmentTriangle() {
        assertContact(SegmentTriangle.Type.INTERIOR, -1, 0.5,  p(1,1,-1), p(1,1,1));
        assertContact(SegmentTriangle.Type.EDGE,      0, 0.5,  p(2,0,-1), p(2,0,1));
        assertContact(SegmentTriangle.Type.EDGE,      1, 0.5,  p(2,2,-1), p(2,2,1));
        assertContact(SegmentTriangle.Type.EDGE,      2, 0.5,  p(0,2,-1), p(0,2,1));
        assertContact(SegmentTriangle.Type.VERTEX,    1, 0.5,  p(4,0,-1), p(4,0,1));
        assertContact(SegmentTriangle.Type.VERTEX,    0, 0.25, p(0,0,-1), p(0,0,3));
        assertSame(SegmentTriangle.NONE, Intersection.segmentTriangle(p(5,5,-1), p(5,5,1), A, B, C, 0), "Outside.");
        assertSame(SegmentTriangle.NONE, Intersection.segmentTriangle(p(1,1, 1), p(1,1,2), A, B, C, 0), "Above.");
    }

    /**
     * A segment with an end on the plane of the triangle, or in that plane.
     */
    @Test
    public void testSegmentEndOnTriangle() {
        assertContact(SegmentTriangle.Type.INTERIOR, -1, 0, p(1,1,0), p(1,1,5));
        assertContact(SegmentTriangle.Type.EDGE,      0, 1, p(2,0,3), p(2,0,0));
        assertContact(SegmentTriangle.Type.VERTEX,    2, 0, p(0,4,0), p(1,1,1));
        assertSame(SegmentTriangle.NONE,     Intersection.segmentTriangle(p(5,5,0), p(5,5,1), A, B, C, 0), "End on the plane, outside.");
        assertSame(SegmentTriangle.COPLANAR, Intersection.segmentTriangle(p(1,1,0), p(3,0,0), A, B, C, 0));
        //an end near the plane is on it only within the tolerance.
        assertSame(SegmentTriangle.NONE, Intersection.segmentTriangle(p(1,1,1E-12), p(1,1,5), A, B, C, 0));
        final SegmentTriangle r = Intersection.segmentTriangle(p(1,1,1E-12), p(1,1,5), A, B, C, 1E-9);
        assertEquals(SegmentTriangle.Type.INTERIOR, r.type());
        assertEquals(0, r.ratio());
    }

    /**
     * A segment crossing a triangle in an oblique plane, with coordinates which are not integers.
     */
    @Test
    public void testSegmentObliqueTriangle() {
        final double[] o = {0.1, 0.2, 0.3}, u = {1/3., 2/3., 2/3.}, v = {2/3., 1/3., -2/3.};
        final double[] n = Vectors.cross(u, v);
        final double[] a = o, b = Vectors.add(o, Vectors.scale(u, 4)), c = Vectors.add(o, Vectors.scale(v, 4));
        final double[] inside  = Vectors.add(o, Vectors.add(u, v));
        final double[] outside = Vectors.add(o, Vectors.scale(Vectors.add(u, v), 3));
        SegmentTriangle r = Intersection.segmentTriangle(Vectors.add(inside, Vectors.scale(n, -1)), Vectors.add(inside, Vectors.scale(n, 3)), a, b, c, 0);
        assertEquals(SegmentTriangle.Type.INTERIOR, r.type());
        assertEquals(0.25, r.ratio(), EPS);
        r = Intersection.segmentTriangle(Vectors.add(outside, Vectors.scale(n, -1)), Vectors.add(outside, Vectors.scale(n, 1)), a, b, c, 0);
        assertEquals(SegmentTriangle.Type.NONE, r.type());
    }

    /**
     * Segments in the plane of the triangle: inside, crossing, through a corner, along an edge, or outside.
     */
    @Test
    public void testSegmentInTriangle() {
        List<SegmentTriangle> r = Intersection.segmentInTriangle2D(p(1,1,0), p(2,1,0), A, B, C, XY);
        assertPoints(r, SegmentTriangle.Type.INTERIOR, -1, 0, SegmentTriangle.Type.INTERIOR, -1, 1);

        r = Intersection.segmentInTriangle2D(p(-1,1,0), p(5,1,0), A, B, C, XY);
        assertPoints(r, SegmentTriangle.Type.EDGE, 2, 1/6., SegmentTriangle.Type.EDGE, 1, 2/3.);

        r = Intersection.segmentInTriangle2D(p(1,1,0), p(5,1,0), A, B, C, XY);
        assertPoints(r, SegmentTriangle.Type.INTERIOR, -1, 0, SegmentTriangle.Type.EDGE, 1, 0.5);

        r = Intersection.segmentInTriangle2D(p(-1,-1,0), p(2,2,0), A, B, C, XY);
        assertPoints(r, SegmentTriangle.Type.VERTEX, 0, 1/3., SegmentTriangle.Type.EDGE, 1, 1);

        r = Intersection.segmentInTriangle2D(p(-1,0,0), p(2,0,0), A, B, C, XY);
        assertPoints(r, SegmentTriangle.Type.VERTEX, 0, 1/3., SegmentTriangle.Type.EDGE, 0, 1);

        r = Intersection.segmentInTriangle2D(p(4,0,0), p(5,-1,0), A, B, C, XY);
        assertEquals(1, r.size(), "Touching at a corner only.");
        assertEquals(SegmentTriangle.Type.VERTEX, r.get(0).type());
        assertEquals(1, r.get(0).feature());
        assertEquals(0, r.get(0).ratio());

        assertTrue(Intersection.segmentInTriangle2D(p(5,5,0), p(6,6,0), A, B, C, XY).isEmpty());

        //a triangle in the vertical plane y = 2, projected on the plane of its normal.
        final double[] a = {0,2,0}, b = {4,2,0}, c = {0,2,4};
        final int[] axes = Maths.projectionAxes(Maths.calculateNormal(a, b, c));
        r = Intersection.segmentInTriangle2D(p(1,2,1), p(1,2,5), a, b, c, axes);
        assertPoints(r, SegmentTriangle.Type.INTERIOR, -1, 0, SegmentTriangle.Type.EDGE, 1, 0.5);
    }

    /**
     * Triangles crossing, touching at a point or along an edge, coplanar or apart.
     */
    @Test
    public void testTriangleTriangle() {
        //a vertical triangle crossing the interior of the horizontal one.
        TriangleTriangle r = Intersection.triangleTriangle(A, B, C, p(1,1,-1), p(1,1,1), p(2.5,1,0), 0);
        assertEquals(TriangleTriangle.Type.SEGMENT, r.type());
        assertEnds(r, p(1,1,0), p(2.5,1,0));

        //a vertical triangle crossing an edge of the horizontal one.
        r = Intersection.triangleTriangle(A, B, C, p(1,-1,-1), p(1,-1,1), p(1,2,0), 0);
        assertEquals(TriangleTriangle.Type.SEGMENT, r.type());
        assertEnds(r, p(1,0,0), p(1,2,0));

        //a vertical triangle standing on an edge of the horizontal one.
        r = Intersection.triangleTriangle(A, B, C, p(1,0,0), p(3,0,0), p(2,0,2), 0);
        assertEquals(TriangleTriangle.Type.SEGMENT, r.type());
        assertEnds(r, p(1,0,0), p(3,0,0));

        //a triangle touching the horizontal one by a corner.
        r = Intersection.triangleTriangle(A, B, C, p(1,1,0), p(2,1,1), p(1,2,1), 0);
        assertEquals(TriangleTriangle.Type.POINT, r.type());
        assertSame(r.start(), r.end());
        assertArrayEquals(p(1,1,0), r.start().position());

        assertSame(TriangleTriangle.COPLANAR, Intersection.triangleTriangle(A, B, C, p(1,1,0), p(5,1,0), p(1,5,0), 0));
        assertSame(TriangleTriangle.DISJOINT, Intersection.triangleTriangle(A, B, C, p(10,10,1), p(11,10,1), p(10,11,2), 0), "Apart.");
        assertSame(TriangleTriangle.DISJOINT, Intersection.triangleTriangle(A, B, C, p(5,5,-1), p(5,5,1), p(6,6,0), 0),
                   "Crossing the plane outside of the triangle.");
    }

    /**
     * Verifies the result of the intersection of a segment with the triangle {@link #A}, {@link #B}, {@link #C}.
     */
    private static void assertContact(SegmentTriangle.Type type, int feature, double ratio, double[] s0, double[] s1) {
        final SegmentTriangle r = Intersection.segmentTriangle(s0, s1, A, B, C, 0);
        assertEquals(type,    r.type());
        assertEquals(feature, r.feature());
        assertEquals(ratio,   r.ratio(), EPS);
    }

    /**
     * Verifies the two ends of the part of a segment inside a triangle.
     */
    private static void assertPoints(final List<SegmentTriangle> points,
            final SegmentTriangle.Type type1, final int feature1, final double ratio1,
            final SegmentTriangle.Type type2, final int feature2, final double ratio2)
    {
        assertEquals(2, points.size());
        assertEquals(type1,    points.get(0).type());
        assertEquals(feature1, points.get(0).feature());
        assertEquals(ratio1,   points.get(0).ratio(), EPS);
        assertEquals(type2,    points.get(1).type());
        assertEquals(feature2, points.get(1).feature());
        assertEquals(ratio2,   points.get(1).ratio(), EPS);
    }

    /**
     * Verifies the ends of the intersection of two triangles, in any order.
     */
    private static void assertEnds(final TriangleTriangle r, final double[] p1, final double[] p2) {
        final double[] start = r.start().position(), end = r.end().position();
        if (Math.abs(start[0] - p1[0]) + Math.abs(start[1] - p1[1]) + Math.abs(start[2] - p1[2]) < EPS) {
            assertArrayEquals(p2, end, EPS);
        } else {
            assertArrayEquals(p1, end,   EPS);
            assertArrayEquals(p2, start, EPS);
        }
    }
}
