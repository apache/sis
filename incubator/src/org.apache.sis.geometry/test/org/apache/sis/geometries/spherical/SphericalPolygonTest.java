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

import java.util.ArrayList;
import java.util.List;
import org.apache.sis.geometries.internal.shared.DefaultSphere;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.Vector;
import org.apache.sis.maths.Vector3D;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests for {@link SphericalPolygon}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class SphericalPolygonTest {

    private static final double TOLERANCE = 1e-12;

    /**
     * Unit vector of a position given in degrees.
     */
    private static Vector3D.Double point(double lon, double lat) {
        return Vector3D.fromLatLon(Math.toRadians(lat), Math.toRadians(lon));
    }

    /**
     * Vertices of a ring given as (lon, lat) pairs in degrees, not closed.
     */
    private static Vector3D.Double[] vertices(double... lonLat) {
        final Vector3D.Double[] vertices = new Vector3D.Double[lonLat.length / 2];
        for (int i = 0; i < vertices.length; i++) {
            vertices[i] = point(lonLat[2 * i], lonLat[2 * i + 1]);
        }
        return vertices;
    }

    /**
     * Closed ring array of the given vertices.
     */
    private static Array ring(Vector<?>... vertices) {
        final List<Vector<?>> points = new ArrayList<>(List.of(vertices));
        points.add(vertices[0]);
        return NDArrays.of(points, 3, DataType.DOUBLE);
    }

    /**
     * Square of 10° with a notch from its top side: concave, counter-clockwise seen from outside.
     */
    private static final Vector3D.Double[] NOTCHED = vertices(0, 0, 10, 0, 10, 10, 6, 10, 6, 4, 4, 4, 4, 10, 0, 10);

    /**
     * Containment in a concave ring : inside both arms and the base, outside the notch.
     */
    @Test
    public void ringContainsConcaveTest() {
        assertTrue (SphericalPolygon.ringContains(NOTCHED, point(2, 5)));
        assertTrue (SphericalPolygon.ringContains(NOTCHED, point(8, 5)));
        assertTrue (SphericalPolygon.ringContains(NOTCHED, point(5, 2)));
        assertFalse(SphericalPolygon.ringContains(NOTCHED, point(5, 7)), "notch");
        assertFalse(SphericalPolygon.ringContains(NOTCHED, point(20, 5)));
        assertFalse(SphericalPolygon.ringContains(NOTCHED, point(5, -5)));
    }

    /**
     * Containment in a polygon with a hole.
     */
    @Test
    public void containsWithHoleTest() {
        final SphericalPolygon polygon = new SphericalPolygon(new DefaultSphere(3),
                ring(vertices(0, 0, 10, 0, 10, 10, 0, 10)),
                ring(vertices(4, 4, 6, 4, 6, 6, 4, 6)));
        assertTrue (polygon.contains(point(2, 2)));
        assertFalse(polygon.contains(point(5, 5)), "in the hole");
        assertFalse(polygon.contains(point(15, 5)));
        assertEquals(1, polygon.getInteriors().length);
    }

    /**
     * Orientation of a ring, and the same ring reversed.
     */
    @Test
    public void orientationTest() {
        assertEquals(1, SphericalPolygon.orientation(NOTCHED));
        final Vector3D.Double[] reversed = new Vector3D.Double[NOTCHED.length];
        for (int i = 0; i < reversed.length; i++) {
            reversed[i] = NOTCHED[NOTCHED.length - 1 - i];
        }
        assertEquals(-1, SphericalPolygon.orientation(reversed));
    }

    /**
     * Edge normals are the cross products of consecutive vertices, the last edge closing the ring.
     */
    @Test
    public void edgeNormalsTest() {
        final Vector<?>[] ring = {new Vector3D.Double(1, 0, 0), new Vector3D.Double(0, 1, 0), new Vector3D.Double(0, 0, 1)};
        final Vector<?>[] normals = SphericalPolygon.edgeNormals(ring);
        assertArrayEquals(new double[] {0, 0, 1}, normals[0].toArrayDouble(), TOLERANCE);
        assertArrayEquals(new double[] {1, 0, 0}, normals[1].toArrayDouble(), TOLERANCE);
        assertArrayEquals(new double[] {0, 1, 0}, normals[2].toArrayDouble(), TOLERANCE);
    }

    /**
     * Area of the octant, of a polygon with a hole, and on a sphere of radius 2.
     */
    @Test
    public void getAreaTest() {
        final Vector3D.Double a = new Vector3D.Double(1, 0, 0);
        final Vector3D.Double b = new Vector3D.Double(0, 1, 0);
        final Vector3D.Double c = new Vector3D.Double(0, 0, 1);
        assertEquals(Math.PI / 2, new SphericalPolygon(new DefaultSphere(3), ring(a, b, c)).getArea(), TOLERANCE);
        assertEquals(Math.PI / 2, new SphericalPolygon(new DefaultSphere(3), ring(c, b, a)).getArea(), TOLERANCE, "winding");
        assertEquals(2 * Math.PI, new SphericalPolygon(new DefaultSphere(3, 2), ring(a, b, c)).getArea(), TOLERANCE);

        final Vector3D.Double[] hole = vertices(30, 30, 60, 30, 45, 50);
        final double holeArea = new SphericalTriangle(new DefaultSphere(3), hole[0], hole[1], hole[2]).getSphericalExcess();
        final SphericalPolygon withHole = new SphericalPolygon(new DefaultSphere(3), ring(a, b, c), ring(hole));
        assertEquals(Math.PI / 2 - holeArea, withHole.getArea(), TOLERANCE);
    }

    /**
     * The signed excess of a ring is positive counter-clockwise and sums the fan triangles.
     */
    @Test
    public void excessTest() {
        final Vector3D.Double[] square = vertices(0, 0, 10, 0, 10, 10, 0, 10);
        final double expected = SphericalTriangle.signedExcess(square[0], square[1], square[2])
                              + SphericalTriangle.signedExcess(square[0], square[2], square[3]);
        assertEquals(expected, SphericalPolygon.excess(square), 0);
        assertTrue(expected > 0);
        assertEquals(-expected, SphericalPolygon.excess(new Vector3D.Double[] {square[3], square[2], square[1], square[0]}), TOLERANCE);
    }
}
