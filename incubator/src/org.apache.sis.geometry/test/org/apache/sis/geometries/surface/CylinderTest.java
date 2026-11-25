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
package org.apache.sis.geometries.surface;

import java.util.ArrayList;
import java.util.List;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.maths.Vector;
import static org.apache.sis.geometries.surface.CapsuleTest.INTEGRATION_TOLERANCE;
import static org.apache.sis.geometries.surface.SphereTest.assertPosition;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.apache.sis.geometries.GeometryTest;


/**
 * Tests the centroid of {@link Cylinder}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class CylinderTest extends GeometryTest {

    private static final CoordinateReferenceSystem CRS = Geometries.getUndefinedCRS(3);

    private static Cylinder cylinder(final double r1, final double r2, final boolean closed, final double... axis) {
        final GeometryFactory f = GeometryFactory.DEFAULT;
        final Cylinder cylinder = f.createCylinder(f.createDataPoints(CRS, axis));
        cylinder.setRadiusBottom(r1);
        cylinder.setRadiusTop(r2);
        cylinder.setClosed(closed);
        return cylinder;
    }

    /**
     * A cylinder of equal radii has its centroid in the middle of its axis.
     */
    @Test
    public void testEqualRadii() {
        assertPosition(1, 2, 4, cylinder(1, 1, true,  1,2,3, 1,2,5).getCentroid().getPosition());
        assertPosition(1, 2, 4, cylinder(1, 1, false, 1,2,3, 1,2,5).getCentroid().getPosition());
    }

    /**
     * The lateral surface of an open cone has its centroid at one third of the height from the base.
     */
    @Test
    public void testOpenCone() {
        assertPosition(0, 0, 1, cylinder(2, 0, false, 0,0,0, 0,0,3).getCentroid().getPosition());
    }

    /**
     * Frustums, open and closed, on an axis which is not a coordinate axis.
     * The expected values are integrated on a fine subdivision of the profile.
     */
    @Test
    public void testFrustum() {
        final double r1 = 2, r2 = 0.5;
        for (final boolean closed : new boolean[] {false, true}) {
            final Cylinder cylinder = cylinder(r1, r2, closed, 0,0,0, 2,1,3);
            final double h = cylinder.getHeight();
            final List<double[]> profile = new ArrayList<>();
            final int n = 10000;
            if (closed) {
                for (int i = 0; i < n; i++) profile.add(new double[] {0, r1 * i / n});
            }
            for (int i = 0; i <= n; i++) {
                profile.add(new double[] {h * i / n, r1 + (r2 - r1) * i / n});
            }
            if (closed) {
                for (int i = n - 1; i >= 0; i--) profile.add(new double[] {h, r2 * i / n});
            }
            final double[] expected = CapsuleTest.revolve(profile);
            assertEquals(expected[0], cylinder.getArea().getValue().doubleValue(), expected[0] * INTEGRATION_TOLERANCE, "area");
            assertEquals(expected[2], cylinder.getEnclosedVolume().getValue().doubleValue(), expected[2] * INTEGRATION_TOLERANCE, "volume");
            final double ratio = expected[1] / h;
            final Vector<?> centroid = cylinder.getCentroid().getPosition();
            assertEquals(2 * ratio, centroid.get(0), h * INTEGRATION_TOLERANCE, "x");
            assertEquals(    ratio, centroid.get(1), h * INTEGRATION_TOLERANCE, "y");
            assertEquals(3 * ratio, centroid.get(2), h * INTEGRATION_TOLERANCE, "z");
        }
    }

    /**
     * The enclosed volume is π<var>r</var>²<var>h</var> for a right cylinder and a third of it for a cone,
     * whether the cylinder is closed or not.
     */
    @Test
    public void testGetEnclosedVolume() {
        assertEquals(2 * Math.PI, cylinder(1, 1, true,  0,0,0, 0,0,2).getEnclosedVolume().getValue().doubleValue(), TOLERANCE);
        assertEquals(2 * Math.PI, cylinder(1, 1, false, 0,0,0, 0,0,2).getEnclosedVolume().getValue().doubleValue(), TOLERANCE);
        assertEquals(4 * Math.PI, cylinder(2, 0, false, 0,0,0, 0,0,3).getEnclosedVolume().getValue().doubleValue(), TOLERANCE);
        final GeometryFactory f = GeometryFactory.DEFAULT;
        final Cylinder flat = f.createCylinder(f.createDataPoints(Geometries.getUndefinedCRS(2), 0,0, 0,2));
        assertThrows(UnsupportedOperationException.class, flat::getEnclosedVolume);
    }

    /**
     * A cylinder without area has its centroid in the middle of its axis.
     */
    @Test
    public void testNoArea() {
        assertPosition(0, 0, 1, cylinder(0, 0, true, 0,0,0, 0,0,2).getCentroid().getPosition());
    }
}
