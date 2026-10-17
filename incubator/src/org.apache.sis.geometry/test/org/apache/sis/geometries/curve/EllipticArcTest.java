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
package org.apache.sis.geometries.curve;

import org.opengis.geometry.Envelope;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.GeometryFactory;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.apache.sis.geometries.GeometryTest;


/**
 * Tests {@link EllipticArc}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class EllipticArcTest extends GeometryTest {
    @Test
    @Disabled("Not implemented yet.")
    public void testGetInterpolation() {
    }

    /**
     * Returns the coordinates of the points of the ellipse x²/4 + y² = 1 at the given parameters.
     */
    private static double[] ellipse(final double... t) {
        final double[] coordinates = new double[t.length * 2];
        for (int i = 0; i < t.length; i++) {
            coordinates[2*i]     = 2 * Math.cos(t[i]);
            coordinates[2*i + 1] = Math.sin(t[i]);
        }
        return coordinates;
    }

    private static EllipticArc arc(final boolean cycle, final double... coordinates) {
        return GeometryFactory.DEFAULT.createEllipticArc(GeometryFactory.DEFAULT.createDataPoints(CRS_2D, coordinates), null, cycle);
    }

    private static void assertEnvelope(final Envelope e, final double xmin, final double ymin, final double xmax, final double ymax) {
        assertEquals(xmin, e.getMinimum(0), TOLERANCE, "xmin");
        assertEquals(ymin, e.getMinimum(1), TOLERANCE, "ymin");
        assertEquals(xmax, e.getMaximum(0), TOLERANCE, "xmax");
        assertEquals(ymax, e.getMaximum(1), TOLERANCE, "ymax");
    }

    /**
     * An arc of four points crossing the extremes of the ellipse along x at t = 0 and along y at t = π/2.
     */
    @Test
    public void testGetEnvelope() {
        assertEnvelope(arc(false, ellipse(-0.3, 0.6, 1.5, 2.6)).getEnvelope(), 2 * Math.cos(2.6), Math.sin(-0.3), 2, 1);
        // Complete ellipse made of two arcs, the first data point closing the second arc.
        assertEnvelope(arc(true, ellipse(0.1, 1.1, 2.1, 3.1, 4.1, 5.1)).getEnvelope(), -2, -1, 2, 1);
    }

    @Test
    public void testAsLine() {
        final DataPoints points = arc(false, ellipse(-0.3, 0.6, 1.5, 2.6)).asLine(null, null).getDataPoints();
        assertTrue(points.size() > 10);
        double previous = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < points.size(); i++) {
            final double[] p = points.getPosition(i).toArrayDouble();
            assertEquals(1, p[0]*p[0]/4 + p[1]*p[1], 1E-9, "Point not on the ellipse.");
            final double angle = Math.atan2(p[1], p[0] / 2);
            assertTrue(angle > previous, "Points not in their order along the ellipse.");
            previous = angle;
        }
    }

    @Test
    public void testInvalid() {
        // Five points are not a whole number of arcs of four points.
        assertThrows(IllegalStateException.class, () -> arc(false, ellipse(0, 0.5, 1, 1.5, 2)).getEnvelope());
        // Four points of the hyperbola x² − y² = 1, which is not an ellipse.
        final double[] u = {-1, -0.3, 0.4, 1.1};
        final double[] h = new double[8];
        for (int i = 0; i < u.length; i++) {
            h[2*i] = Math.cosh(u[i]);
            h[2*i + 1] = Math.sinh(u[i]);
        }
        assertThrows(IllegalStateException.class, () -> arc(false, h).getEnvelope());
        // Two pairs of points having the same y coordinate: the pair of lines y = 0 and y = sin(π/3) goes through them too.
        assertThrows(IllegalStateException.class, () -> arc(false, ellipse(0, Math.PI / 3, 2 * Math.PI / 3, Math.PI)).getEnvelope());
    }
}
