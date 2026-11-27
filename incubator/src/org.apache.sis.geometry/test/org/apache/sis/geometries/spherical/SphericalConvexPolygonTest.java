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

import java.util.List;
import org.apache.sis.geometries.internal.shared.DefaultSphere;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.Vector3D;
import org.apache.sis.maths.Vector;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests for {@link SphericalConvexPolygon#contains}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class SphericalConvexPolygonTest {

    /**
     * Corners of a spherical square cap centered on the north pole, given in
     * CCW order viewed from outside the sphere.
     */
    private final Vector3D.Double a = Vector.of(1.0, 0.0, 1.0).normalize();
    private final Vector3D.Double b = Vector.of(0.0, 1.0, 1.0).normalize();
    private final Vector3D.Double c = Vector.of(-1.0, 0.0, 1.0).normalize();
    private final Vector3D.Double d = Vector.of(0.0, -1.0, 1.0).normalize();

    private final Array points = NDArrays.of(List.of(a, b, c, d, a), 3, DataType.DOUBLE);
    private final SphericalConvexPolygon polygon = new SphericalConvexPolygon(new DefaultSphere(3), points);

    /**
     * Corner points lie on the polygon boundary and must be considered contained.
     */
    @Test
    public void containsCornerTest() {
        assertTrue(polygon.contains(a));
        assertTrue(polygon.contains(b));
        assertTrue(polygon.contains(c));
        assertTrue(polygon.contains(d));
    }

    /**
     * The north pole is at the center of the cap and must be contained.
     */
    @Test
    public void containsInteriorTest() {
        assertTrue(polygon.contains(Vector.of(0.0, 0.0, 1.0)));
    }

    /**
     * The south pole is diametrically opposed to the cap and must not be contained.
     */
    @Test
    public void containsOppositeSideTest() {
        assertFalse(polygon.contains(Vector.of(0.0, 0.0, -1.0)));
    }

    /**
     * Points on the equator are outside the cap, even though each of them lies
     * on the "inside" side of some, but not all, edges.
     */
    @Test
    public void containsOutsideEquatorTest() {
        assertFalse(polygon.contains(Vector.of(1.0, 0.0, 0.0)));
        assertFalse(polygon.contains(Vector.of(0.0, 1.0, 0.0)));
        assertFalse(polygon.contains(Vector.of(-1.0, 0.0, 0.0)));
        assertFalse(polygon.contains(Vector.of(0.0, -1.0, 0.0)));
    }


    /**
     * The margin is positive inside, zero on a corner and negative outside.
     */
    @Test
    public void marginTest() {
        assertTrue(polygon.margin(Vector.of(0.0, 0.0, 1.0)) > 0);
        assertEquals(0, polygon.margin(a), 1e-15);
        assertTrue(polygon.margin(Vector.of(0.0, 0.0, -1.0)) < 0);
        assertTrue(polygon.margin(Vector.of(1.0, 1.0, 0.0).normalize()) < 0);
        // a point nearer to the center is further inside
        assertTrue(polygon.margin(Vector.of(0.0, 0.0, 1.0)) > polygon.margin(Vector.of(0.2, 0.0, 1.0).normalize()));
    }

    /**
     * Area of the square cap : four right triangles at the pole with legs of 45°,
     * each having an excess E with tan(E/2) = tan²(PI/8).
     */
    @Test
    public void getAreaTest() {
        final double t = Math.tan(Math.PI / 8);
        assertEquals(4 * 2 * Math.atan(t * t), polygon.getArea(), 1e-14);
        final SphericalConvexPolygon bigger = new SphericalConvexPolygon(new DefaultSphere(3, 3), points);
        assertEquals(9 * polygon.getArea(), bigger.getArea(), 1e-13);
    }

    /**
     * Densified boundary : corners at every second point, edge middles in between, closed ring.
     */
    @Test
    public void getBoundaryTest() {
        final Array boundary = polygon.getBoundary(2);
        assertEquals(9, boundary.getLength());
        final Vector3D.Double[] corners = {a, b, c, d, a};
        for (int i = 0; i < 9; i++) {
            final double[] expected = (i % 2 == 0)
                    ? corners[i / 2].toArrayDouble()
                    : corners[i / 2].add(corners[i / 2 + 1]).normalize().toArrayDouble();
            assertArrayEquals(expected, boundary.get(i).toArrayDouble(), 1e-15);
        }
    }
}
