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

    private final Sphere sphere = new Sphere(3);

    /**
     * Triangle covering the octant of the sphere where x,y,z are all positive.
     * Corners are given in CCW order viewed from outside the sphere.
     */
    private final ReadOnly.Vector<?> a = new Vector3D.Double(1, 0, 0);
    private final ReadOnly.Vector<?> b = new Vector3D.Double(0, 1, 0);
    private final ReadOnly.Vector<?> c = new Vector3D.Double(0, 0, 1);
    private final SphericalTriangle triangle = new SphericalTriangle(sphere, a, b, c);

    /**
     * Constructor and corner accessors test.
     */
    @Test
    public void constructorTest() {
        assertSame(a, triangle.getA());
        assertSame(b, triangle.getB());
        assertSame(c, triangle.getC());
    }

    /**
     * Centroid test.
     */
    @Test
    public void getCentroidTest() {
        final ReadOnly.Vector<?> centroid = triangle.getCentroid();
        final double v = 1.0 / Math.sqrt(3.0);
        assertArrayEquals(new double[]{v, v, v}, centroid.toArrayDouble(), TOLERANCE);
    }

    /**
     * Contains test, using the single argument overload which relies on the
     * class default epsilon of -1e9. Since this epsilon is far below the
     * range of possible dot product values for unit vectors, this method
     * currently returns true regardless of the tested point.
     */
    @Test
    public void containsTest() {
        assertTrue(triangle.contains(triangle.getA()));
        assertTrue(triangle.contains(triangle.getB()));
        assertTrue(triangle.contains(triangle.getC()));
        assertTrue(triangle.contains(triangle.getCentroid()));
        assertFalse(triangle.contains(new Vector3D.Double(-1, 0, 0)));
        assertFalse(triangle.contains(new Vector3D.Double(0, -1, 0)));
        assertFalse(triangle.contains(new Vector3D.Double(0, 0, -1)));
    }

    /**
     * Quad subdivision test.
     */
    @Test
    public void quadSubdivideTest() {
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
