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
package org.apache.sis.geometries.spherical;

import org.apache.sis.geometries.internal.shared.DefaultSphere;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.Vector;
import org.apache.sis.maths.Vector3D;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests for {@link SphericalTriangle}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class SphericalTriangleTest {

    private static final double TOLERANCE = 1e-12;

    /**
     * Triangle covering the octant of the sphere where x,y,z are all positive.
     * Corners are given in CCW order viewed from outside the sphere.
     */
    private final Vector<?> a = new Vector3D.Double(1, 0, 0);
    private final Vector<?> b = new Vector3D.Double(0, 1, 0);
    private final Vector<?> c = new Vector3D.Double(0, 0, 1);

    /**
     * Constructor and corner accessors test.
     */
    @Test
    public void constructorTest() {
        final SphericalTriangle triangle = new SphericalTriangle(new DefaultSphere(3), a, b, c);
        assertSame(a, triangle.getA());
        assertSame(b, triangle.getB());
        assertSame(c, triangle.getC());
    }

    /**
     * Centroid test.
     */
    @Test
    public void getCentroidTest() {
        final SphericalTriangle triangle = new SphericalTriangle(new DefaultSphere(3), a, b, c);
        final Vector<?> centroid = triangle.getCentroidVector();
        final double v = 1.0 / Math.sqrt(3.0);
        assertArrayEquals(new double[]{v, v, v}, centroid.toArrayDouble(), TOLERANCE);
    }

    /**
     * Spherical excess test.
     * The octant triangle (1,0,0),(0,1,0),(0,0,1) has three right angles,
     * so its spherical excess is 3*(PI/2) - PI = PI/2.
     */
    @Test
    public void getSphericalExcessTest() {
        final SphericalTriangle triangle = new SphericalTriangle(new DefaultSphere(3), a, b, c);
        assertEquals(Math.PI / 2, triangle.getSphericalExcess(), TOLERANCE);
    }

    /**
     * Spherical excess of small triangles, where summing the three angles and subtracting PI loses
     * most digits. A right triangle at the pole with two legs of angle s has an excess E with
     * tan(E/2) = tan²(s/2). The excess is positive whatever the winding of the corners.
     */
    @Test
    public void getSphericalExcessSmallTest() {
        for (final double s : new double[] {1e-2, 1e-4, 1e-6}) {
            final Vector<?> pole = new Vector3D.Double(0, 0, 1);
            final Vector<?> east = new Vector3D.Double(Math.sin(s), 0, Math.cos(s));
            final Vector<?> north = new Vector3D.Double(0, Math.sin(s), Math.cos(s));
            final double t = Math.tan(s / 2);
            final double expected = 2 * Math.atan(t * t);
            assertEquals(expected, new SphericalTriangle(new DefaultSphere(3), pole, east, north).getSphericalExcess(), expected * 1e-9);
            assertEquals(expected, new SphericalTriangle(new DefaultSphere(3), pole, north, east).getSphericalExcess(), expected * 1e-9);
        }
    }

    /**
     * Area test.
     * The octant triangle covers 1/8th of the sphere surface (4*PI*r^2),
     * so its area is PI/2 * r^2.
     */
    @Test
    public void getAreaTest() {
        final SphericalTriangle triangle = new SphericalTriangle(new DefaultSphere(3), a, b, c);
        assertEquals(Math.PI / 2, triangle.getArea(), TOLERANCE);

        final SphericalTriangle biggerTriangle = new SphericalTriangle(new DefaultSphere(3, 2), a, b, c);
        assertEquals(Math.PI / 2 * 4, biggerTriangle.getArea(), TOLERANCE);
    }

    /**
     * Contains test.
     */
    @Test
    public void containsTest() {
        final SphericalTriangle triangle = new SphericalTriangle(new DefaultSphere(3), a, b, c);
        assertTrue(triangle.contains(triangle.getA()));
        assertTrue(triangle.contains(triangle.getB()));
        assertTrue(triangle.contains(triangle.getC()));
        assertTrue(triangle.contains(triangle.getCentroidVector()));
        assertFalse(triangle.contains(new Vector3D.Double(-1, 0, 0)));
        assertFalse(triangle.contains(new Vector3D.Double(0, -1, 0)));
        assertFalse(triangle.contains(new Vector3D.Double(0, 0, -1)));
    }

    /**
     * Quad subdivision test.
     */
    @Test
    public void quadSubdivideTest() {
        final SphericalTriangle triangle = new SphericalTriangle(new DefaultSphere(3), a, b, c);
        final SphericalTriangle[] children = triangle.quadSubdivide();
        assertEquals(4, children.length);

        final double[] ab = middle(a, b);
        final double[] bc = middle(b, c);
        final double[] ca = middle(c, a);

        // child 0 : corner A
        assertSame(a, children[0].getA());
        assertArrayEquals(ab, children[0].getB().toArrayDouble(), TOLERANCE);
        assertArrayEquals(ca, children[0].getC().toArrayDouble(), TOLERANCE);

        // child 1 : corner B
        assertArrayEquals(ab, children[1].getA().toArrayDouble(), TOLERANCE);
        assertSame(b, children[1].getB());
        assertArrayEquals(bc, children[1].getC().toArrayDouble(), TOLERANCE);

        // child 2 : corner C
        assertArrayEquals(ca, children[2].getA().toArrayDouble(), TOLERANCE);
        assertArrayEquals(bc, children[2].getB().toArrayDouble(), TOLERANCE);
        assertSame(c, children[2].getC());

        // child 3 : center, opposite direction
        assertArrayEquals(ab, children[3].getA().toArrayDouble(), TOLERANCE);
        assertArrayEquals(bc, children[3].getB().toArrayDouble(), TOLERANCE);
        assertArrayEquals(ca, children[3].getC().toArrayDouble(), TOLERANCE);
    }

    private static double[] middle(Vector<?> p, Vector<?> q) {
        return p.add(q).normalize().toArrayDouble();
    }

    /**
     * A subdivision in one is the triangle itself.
     */
    @Test
    public void subdivideByOneTest() {
        final SphericalTriangle triangle = new SphericalTriangle(new DefaultSphere(3), a, b, c);
        final SphericalTriangle[] children = triangle.subdivide(1);
        assertEquals(1, children.length);
        assertSame(triangle, children[0]);
    }

    /**
     * Children of a subdivision must exactly tile their parent, so their areas must add up
     * to the parent area, they must all keep the CCW order and their centers must all fall
     * inside the parent.
     */
    @Test
    public void subdivideTilesTheParentTest() {
        final SphericalTriangle triangle = new SphericalTriangle(new DefaultSphere(3), a, b, c);
        for (final int n : new int[] {2, 3, 4, 5}) {
            final SphericalTriangle[] children = triangle.subdivide(n);
            assertEquals(n*n, children.length, "Subdivision by " + n);

            double sum = 0;
            for (SphericalTriangle child : children) {
                sum += child.getArea();
                /*
                 * The spherical excess is computed from the distances between the corners,
                 * so it stays positive whatever the order. The triple product is what tells
                 * the order apart, and it must stay positive for a CCW triangle.
                 */
                assertTrue(child.getA().dot(child.getB().cross(child.getC())) > 0,
                        "Child of a subdivision by " + n + " must keep the CCW order");
                assertTrue(triangle.contains(child.getCentroidVector()),
                        "Child center must fall inside the parent");
            }
            assertEquals(triangle.getArea(), sum, 1e-9, "Children of a subdivision by " + n + " must tile it");
        }
    }

    /**
     * A subdivision must reuse the parent corners as they are, and place the new vertices
     * on the parent edges.
     */
    @Test
    public void subdivideKeepsParentCornersTest() {
        final SphericalTriangle triangle = new SphericalTriangle(new DefaultSphere(3), a, b, c);
        final SphericalTriangle[] children = triangle.subdivide(3);
        assertEquals(9, children.length);
        /*
         * The 6 triangles pointing the same way as the parent come first, by rows starting
         * at corner A, so corner A is in the first one, corner B in the first of the last
         * row and corner C in the last of that row.
         */
        assertSame(a, children[0].getA());
        assertSame(b, children[3].getB());
        assertSame(c, children[5].getC());
    }

    /**
     * Subdividing must reject a ratio below one.
     */
    @Test
    public void subdivideInvalidTest() {
        final SphericalTriangle triangle = new SphericalTriangle(new DefaultSphere(3), a, b, c);
        assertThrows(IllegalArgumentException.class, () -> triangle.subdivide(0));
        assertThrows(IllegalArgumentException.class, () -> triangle.subdivide(-1));
    }

    /**
     * The margin is positive inside, zero on a corner and negative outside.
     */
    @Test
    public void marginTest() {
        final SphericalTriangle triangle = new SphericalTriangle(new DefaultSphere(3), a, b, c);
        assertTrue(triangle.margin(triangle.getCentroidVector()) > 0);
        assertEquals(0, triangle.margin(a), 1e-15);
        assertTrue(triangle.margin(new Vector3D.Double(-1, -1, -1).normalize()) < 0);
    }

    /**
     * Densified boundary : the corners, then the points at thirds of each edge, closed ring.
     */
    @Test
    public void getBoundaryTest() {
        final SphericalTriangle triangle = new SphericalTriangle(new DefaultSphere(3), a, b, c);
        final Array boundary = triangle.getBoundary(3);
        assertEquals(10, boundary.getLength());
        assertArrayEquals(a.toArrayDouble(), boundary.get(0).toArrayDouble(), TOLERANCE);
        final double third = Math.PI / 6;
        assertArrayEquals(new double[] {Math.cos(third), Math.sin(third), 0}, boundary.get(1).toArrayDouble(), TOLERANCE);
        assertArrayEquals(b.toArrayDouble(), boundary.get(3).toArrayDouble(), TOLERANCE);
        assertArrayEquals(c.toArrayDouble(), boundary.get(6).toArrayDouble(), TOLERANCE);
        assertArrayEquals(a.toArrayDouble(), boundary.get(9).toArrayDouble(), TOLERANCE);
    }

    /**
     * The signed excess is negative for clockwise corners.
     */
    @Test
    public void signedExcessTest() {
        assertEquals(Math.PI / 2, SphericalTriangle.signedExcess(a, b, c), TOLERANCE);
        assertEquals(-Math.PI / 2, SphericalTriangle.signedExcess(a, c, b), TOLERANCE);
    }
}
