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
import org.apache.sis.geometries.Sphere;
import org.apache.sis.geometries.math.Array;
import org.apache.sis.geometries.math.DataType;
import org.apache.sis.geometries.math.NDArrays;
import org.apache.sis.geometries.math.Vector3D;

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
    private final Vector3D.Double a = new Vector3D.Double(1, 0, 1).normalize();
    private final Vector3D.Double b = new Vector3D.Double(0, 1, 1).normalize();
    private final Vector3D.Double c = new Vector3D.Double(-1, 0, 1).normalize();
    private final Vector3D.Double d = new Vector3D.Double(0, -1, 1).normalize();

    private final Array points = NDArrays.of(List.of(a, b, c, d, a), 3, DataType.DOUBLE);
    private final SphericalConvexPolygon polygon = new SphericalConvexPolygon(new Sphere(3), points);

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
        assertTrue(polygon.contains(new Vector3D.Double(0, 0, 1)));
    }

    /**
     * The south pole is diametrically opposed to the cap and must not be contained.
     */
    @Test
    public void containsOppositeSideTest() {
        assertFalse(polygon.contains(new Vector3D.Double(0, 0, -1)));
    }

    /**
     * Points on the equator are outside the cap, even though each of them lies
     * on the "inside" side of some, but not all, edges.
     */
    @Test
    public void containsOutsideEquatorTest() {
        assertFalse(polygon.contains(new Vector3D.Double(1, 0, 0)));
        assertFalse(polygon.contains(new Vector3D.Double(0, 1, 0)));
        assertFalse(polygon.contains(new Vector3D.Double(-1, 0, 0)));
        assertFalse(polygon.contains(new Vector3D.Double(0, -1, 0)));
    }

}
