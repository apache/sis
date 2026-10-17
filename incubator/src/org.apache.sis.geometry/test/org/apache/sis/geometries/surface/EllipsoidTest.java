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

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.GeometryType;
import org.apache.sis.geometries.curve.EllipticArc;
import org.apache.sis.maths.Vector;
import static org.apache.sis.geometries.surface.SphereTest.assertEnvelope;
import static org.apache.sis.geometries.surface.SphereTest.assertPosition;
import static org.apache.sis.geometries.surface.SphereTest.line;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.apache.sis.geometries.GeometryTest;


/**
 * Tests the parameterization of {@link Ellipsoid}: u is the longitude and v the latitude, in radians.
 *
 * @author Johann Sorel (Geomatys)
 */
public class EllipsoidTest extends GeometryTest {

    private static final CoordinateReferenceSystem CRS = Geometries.getUndefinedCRS(3);

    /**
     * Creates the ellipsoid of semi-axes 3, 2 and 1 centered on the origin.
     */
    private static Ellipsoid ellipsoid() {
        return GeometryFactory.DEFAULT.createEllipsoid(GeometryFactory.DEFAULT.createDataPoints(CRS, 0, 0, 0), 3, 2, 1);
    }

    /**
     * Returns the value of the ellipsoid equation at the given position, which is 1 on the ellipsoid.
     */
    private static double equation(final double[] p) {
        return p[0]*p[0]/9 + p[1]*p[1]/4 + p[2]*p[2];
    }

    @Test
    public void testGetSurface() {
        final Ellipsoid ellipsoid = ellipsoid();
        assertPosition(3, 0, 0, ellipsoid.getSurface(0, 0));
        assertPosition(0, 2, 0, ellipsoid.getSurface(Math.PI / 2, 0));
        assertPosition(0, 0, 1, ellipsoid.getSurface(0.7, Math.PI / 2));
        final Vector<?> p = ellipsoid.getSurface(0.7, 0.3);
        assertEquals(1, equation(p.toArrayDouble()), TOLERANCE);
    }

    /**
     * The horizontal curves are the ellipses of constant latitude.
     */
    @Test
    public void testGetHorizontalCurve() {
        final Curve curve = ellipsoid().getHorizontalCurve(Math.PI / 6);
        assertInstanceOf(EllipticArc.class, curve);
        assertEquals(GeometryType.ELLIPTICARC, ellipsoid().getHorizontalCurveType());
        final double c = Math.cos(Math.PI / 6);
        for (final double[] p : line(curve)) {
            assertEquals(0.5, p[2], TOLERANCE, "Point not at the latitude of the curve.");
            assertEquals(1, equation(p), 1E-9, "Point not on the ellipsoid.");
        }
        assertEnvelope(curve.getEnvelope(), -3*c, -2*c, 0.5, 3*c, 2*c, 0.5);
    }

    @Test
    public void testGetHorizontalCurveAtPole() {
        for (final double[] p : line(ellipsoid().getHorizontalCurve(-Math.PI / 2))) {
            assertArrayEquals(new double[] {0, 0, -1}, p, TOLERANCE);
        }
    }

    /**
     * The vertical curves are the half ellipses of constant longitude, from the south pole to the north pole.
     */
    @Test
    public void testGetVerticalCurve() {
        final Curve curve = ellipsoid().getVerticalCurve(Math.PI / 4);
        assertInstanceOf(EllipticArc.class, curve);
        assertPosition(0, 0, -1, curve.getStartPoint().getPosition());
        assertPosition(0, 0,  1, curve.getEndPoint().getPosition());
        final double s = Math.sqrt(0.5);
        for (final double[] p : line(curve)) {
            assertEquals(1, equation(p), 1E-9, "Point not on the ellipsoid.");
            assertEquals(2 * p[0] / 3, p[1], 1E-9, "Point not at the longitude of the curve.");
        }
        assertEnvelope(curve.getEnvelope(), 0, 0, -1, 3*s, 2*s, 1);
    }

    /**
     * At longitude 90°, the first axis is perpendicular to the plane of the curve,
     * so the axes of the ellipse are the second and the third coordinate system axes.
     */
    @Test
    public void testGetVerticalCurvePerpendicularToFirstAxis() {
        final Curve curve = ellipsoid().getVerticalCurve(Math.PI / 2);
        for (final double[] p : line(curve)) {
            assertEquals(0, p[0], TOLERANCE);
            assertEquals(1, equation(p), 1E-9, "Point not on the ellipsoid.");
        }
        assertEnvelope(curve.getEnvelope(), 0, 0, -1, 0, 2, 1);
    }
}
