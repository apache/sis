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

import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.GeometryType;
import org.apache.sis.geometries.curve.Arc;
import org.apache.sis.geometries.curve.Circle;
import org.apache.sis.maths.Vector;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.apache.sis.geometries.GeometryTest;


/**
 * Tests the parameterization of {@link Sphere}: u is the longitude and v the latitude, in radians.
 *
 * @author Johann Sorel (Geomatys)
 */
public class SphereTest extends GeometryTest {

    private static final CoordinateReferenceSystem CRS = Geometries.getUndefinedCRS(3);

    /**
     * Creates the sphere of radius 2 centered on (1, 2, 3).
     */
    private static Sphere sphere() {
        return GeometryFactory.DEFAULT.createSphere(GeometryFactory.DEFAULT.createDataPoints(CRS, 1, 2, 3), 2);
    }

    static void assertPosition(final double x, final double y, final double z, final Vector<?> p) {
        assertEquals(x, p.get(0), TOLERANCE, "x");
        assertEquals(y, p.get(1), TOLERANCE, "y");
        assertEquals(z, p.get(2), TOLERANCE, "z");
    }

    static void assertEnvelope(final Envelope e, final double... bounds) {
        for (int i = 0; i < 3; i++) {
            assertEquals(bounds[i],     e.getMinimum(i), TOLERANCE, "minimum");
            assertEquals(bounds[i + 3], e.getMaximum(i), TOLERANCE, "maximum");
        }
    }

    /**
     * Returns the positions of the linear approximation of the given curve.
     */
    static double[][] line(final Curve curve) {
        final DataPoints points = curve.asLine(null, null).getDataPoints();
        final double[][] positions = new double[points.size()][];
        for (int i = 0; i < positions.length; i++) {
            positions[i] = points.getPosition(i).toArrayDouble();
        }
        return positions;
    }

    @Test
    public void testGetSurface() {
        final Sphere sphere = sphere();
        assertPosition(3, 2, 3, sphere.getSurface(0, 0));
        assertPosition(1, 4, 3, sphere.getSurface(Math.PI / 2, 0));
        assertPosition(-1, 2, 3, sphere.getSurface(Math.PI, 0));
        assertPosition(1, 2, 5, sphere.getSurface(0.7, Math.PI / 2));
        assertPosition(1, 2, 1, sphere.getSurface(0.7, -Math.PI / 2));
        final Vector<?> p = sphere.getSurface(0.7, 0.3);
        assertEquals(2, Math.sqrt(Math.pow(p.get(0) - 1, 2) + Math.pow(p.get(1) - 2, 2) + Math.pow(p.get(2) - 3, 2)), TOLERANCE);
    }

    /**
     * A sphere in two dimensions is a circle, which has no position for two parameters.
     */
    @Test
    public void testGetSurface2D() {
        final Sphere circle = GeometryFactory.DEFAULT.createSphere(
                GeometryFactory.DEFAULT.createDataPoints(Geometries.getUndefinedCRS(2), 0, 0), 1);
        assertThrows(IllegalStateException.class, () -> circle.getSurface(0, 0));
    }

    /**
     * The horizontal curves are the circles of constant latitude.
     */
    @Test
    public void testGetHorizontalCurve() {
        final Curve curve = sphere().getHorizontalCurve(Math.PI / 6);
        assertInstanceOf(Circle.class, curve);
        assertEquals(GeometryType.CIRCLE, sphere().getHorizontalCurveType());
        final double r = Math.sqrt(3);                       // 2⋅cos(30°)
        for (final double[] p : line(curve)) {
            assertEquals(4, p[2], TOLERANCE, "Point not at the latitude of the curve.");
            assertEquals(r, Math.hypot(p[0] - 1, p[1] - 2), TOLERANCE, "Point not on the circle.");
        }
        assertEnvelope(curve.getEnvelope(), 1 - r, 2 - r, 4, 1 + r, 2 + r, 4);
    }

    /**
     * At a pole, the circle of constant latitude is reduced to the pole.
     */
    @Test
    public void testGetHorizontalCurveAtPole() {
        for (final double[] p : line(sphere().getHorizontalCurve(Math.PI / 2))) {
            assertArrayEquals(new double[] {1, 2, 5}, p, TOLERANCE);
        }
    }

    /**
     * The vertical curves are the half great circles of constant longitude, from the south pole to the north pole.
     */
    @Test
    public void testGetVerticalCurve() {
        final Curve curve = sphere().getVerticalCurve(Math.PI / 2);
        assertInstanceOf(Arc.class, curve);
        assertEquals(GeometryType.ARC, sphere().getVerticalCurveType());
        assertPosition(1, 2, 1, curve.getStartPoint().getPosition());
        assertPosition(1, 2, 5, curve.getEndPoint().getPosition());
        for (final double[] p : line(curve)) {
            assertEquals(1, p[0], TOLERANCE, "Point not at the longitude of the curve.");
            assertTrue(p[1] >= 2 - TOLERANCE, "Point on the other half of the great circle.");
            assertEquals(2, Math.hypot(p[1] - 2, p[2] - 3), TOLERANCE, "Point not on the sphere.");
        }
        assertEnvelope(curve.getEnvelope(), 1, 2, 1, 1, 4, 5);
    }
}
