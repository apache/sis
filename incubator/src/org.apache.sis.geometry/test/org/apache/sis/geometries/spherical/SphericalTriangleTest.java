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

import org.apache.sis.geometries.Sphere;
import org.apache.sis.geometries.math.ReadOnly;
import org.apache.sis.geometries.math.Vector3D;

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
    private final ReadOnly.Vector<?> a = new Vector3D.Double(1, 0, 0);
    private final ReadOnly.Vector<?> b = new Vector3D.Double(0, 1, 0);
    private final ReadOnly.Vector<?> c = new Vector3D.Double(0, 0, 1);

    /**
     * Constructor and corner accessors test.
     */
    @Test
    public void constructorTest() {
        final SphericalTriangle triangle = new SphericalTriangle(new Sphere(3), a, b, c);
        assertSame(a, triangle.getA());
        assertSame(b, triangle.getB());
        assertSame(c, triangle.getC());
    }

    /**
     * Centroid test.
     */
    @Test
    public void getCentroidTest() {
        final SphericalTriangle triangle = new SphericalTriangle(new Sphere(3), a, b, c);
        final ReadOnly.Vector<?> centroid = triangle.getCentroidVector();
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
        final SphericalTriangle triangle = new SphericalTriangle(new Sphere(3), a, b, c);
        assertEquals(Math.PI / 2, triangle.getSphericalExcess(), TOLERANCE);
    }

    /**
     * Area test.
     * The octant triangle covers 1/8th of the sphere surface (4*PI*r^2),
     * so its area is PI/2 * r^2.
     */
    @Test
    public void getAreaTest() {
        final SphericalTriangle triangle = new SphericalTriangle(new Sphere(3), a, b, c);
        assertEquals(Math.PI / 2, triangle.getArea(), TOLERANCE);

        final SphericalTriangle biggerTriangle = new SphericalTriangle(new Sphere(3, 2), a, b, c);
        assertEquals(Math.PI / 2 * 4, biggerTriangle.getArea(), TOLERANCE);
    }

    /**
     * Contains test.
     */
    @Test
    public void containsTest() {
        final SphericalTriangle triangle = new SphericalTriangle(new Sphere(3), a, b, c);
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
        final SphericalTriangle triangle = new SphericalTriangle(new Sphere(3), a, b, c);
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

    private static double[] middle(ReadOnly.Vector<?> p, ReadOnly.Vector<?> q) {
        return p.copy().add(q).normalize().toArrayDouble();
    }

}
