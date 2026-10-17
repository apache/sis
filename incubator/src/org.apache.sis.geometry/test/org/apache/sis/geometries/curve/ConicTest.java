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
import org.apache.sis.geometries.GeometryFactory;

// Test dependencies
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.apache.sis.geometries.GeometryTest;


/**
 * Tests {@link Conic}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class ConicTest extends GeometryTest {
    @Test
    @Disabled("Not implemented yet.")
    public void testGetInterpolation() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetDataPoints() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetControlPoints() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testIsCycle() {
    }

    @Test
    public void testGetTopologicDimension() {
        final Conic conic = GeometryFactory.DEFAULT.createConic(
                GeometryFactory.DEFAULT.createDataPoints(CRS_2D, 0,0, 10,0, 10,10, 0,10, 0,0),
                null, true);
        assertEquals(1, conic.getTopologicDimension());
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetLength() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetStartPoint() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetEndPoint() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testIsClosed() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testIsRing() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetBoundary() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetKnots() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetStartConstrParam() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetEndConstrParam() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetStartParam() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetEndParam() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetNumDerivativesInterior() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetNumDerivativesStart() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetNumDerivativesEnd() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetReverse() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testAsLine() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testConstrParam() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetLength_DirectPosition_DirectPosition() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetLength_double_double() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testParam() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testParamForPoint() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testTangent_Length() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testTangent_double() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testSubCurve_double_double() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testSubCurve_Length_Length() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetOrientationSign() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetProxy() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetPrimitive() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetBoundaryType() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetDimension() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetSegments() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetCoordinateReferenceSystem() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testSetCoordinateReferenceSystem() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetMetadata() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetAttributesType() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetDimension_DirectPosition() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testIs3D() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetSpatialDimension() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetGeometryType() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetGeometryType2() {
    }

    /**
     * Creates a two-dimensional conic and returns its envelope.
     */
    private static Envelope envelope(final boolean cycle, final double... coordinates) {
        return GeometryFactory.DEFAULT.createConic(
                GeometryFactory.DEFAULT.createDataPoints(CRS_2D, coordinates), null, cycle).getEnvelope();
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

    private static void assertEnvelope(final Envelope envelope, final double... bounds) {
        final int dimension = bounds.length / 2;
        assertEquals(dimension, envelope.getDimension());
        for (int i = 0; i < dimension; i++) {
            assertEquals(bounds[i],             envelope.getMinimum(i), TOLERANCE, "minimum");
            assertEquals(bounds[i + dimension], envelope.getMaximum(i), TOLERANCE, "maximum");
        }
    }

    @Test
    public void testGetEnvelopeEllipse() {
        // Arc of the ellipse crossing its extremes along x at t = 0 and along y at t = π/2.
        final Envelope envelope = envelope(false, ellipse(-0.3, 0.4, 1.2, 2.0, 2.6));
        assertEquals(CRS_2D, envelope.getCoordinateReferenceSystem());
        assertEnvelope(envelope, 2 * Math.cos(2.6), Math.sin(-0.3), 2, 1);
        // Same arc in the other direction.
        assertEnvelope(envelope(false, ellipse(2.6, 2.0, 1.2, 0.4, -0.3)), 2 * Math.cos(2.6), Math.sin(-0.3), 2, 1);
        // Arc of the other side of the ellipse, from the same end points: it crosses the extremes at π and 3π/2.
        assertEnvelope(envelope(false, ellipse(2.6, 3.4, 4.2, 5.0, 2*Math.PI - 0.3)), -2, -1, 2 * Math.cos(0.3), Math.sin(2.6));
        // Full ellipse made of two arcs, the first point closing the second arc.
        assertEnvelope(envelope(true, ellipse(0.1, 0.9, 1.7, 2.5, 3.3, 4.1, 4.9, 5.7)), -2, -1, 2, 1);
    }

    @Test
    public void testGetEnvelopeHyperbola() {
        // Right branch of x² − y² = 1, crossing its vertex at (1, 0).
        final double[] u = {-1, -0.4, 0.2, 0.7, 1.2};
        final double[] coordinates = new double[10];
        for (int i = 0; i < u.length; i++) {
            coordinates[2*i]     = Math.cosh(u[i]);
            coordinates[2*i + 1] = Math.sinh(u[i]);
        }
        assertEnvelope(envelope(false, coordinates), 1, Math.sinh(-1), Math.cosh(1.2), Math.sinh(1.2));
        /*
         * Arc going from the right branch to the left branch: it passes through the point at infinity
         * in the direction of the asymptote y = x.
         */
        assertThrows(IllegalStateException.class, () -> envelope(false,
                Math.cosh(0.5), Math.sinh(0.5), Math.cosh(1.5), Math.sinh(1.5),
               -Math.cosh(1.5), -Math.sinh(1.5), -Math.cosh(0.5), -Math.sinh(0.5), -1, 0));
    }

    @Test
    public void testGetEnvelopeParabola() {
        // Parabola y = x², crossing its vertex at (0, 0).
        assertEnvelope(envelope(false, -1,1, -0.2,0.04, 0.5,0.25, 1,1, 2,4), -1, 0, 2, 4);
    }

    @Test
    public void testGetEnvelopeCollinear() {
        assertEnvelope(envelope(false, 0,0, 1,1, 2,2, 3,3, 4,4), 0, 0, 4, 4);
    }

    @Test
    public void testGetEnvelope3D() {
        // The ellipse of `testGetEnvelopeEllipse()`, translated in the horizontal plane at height 5.
        final double[] t = {-0.3, 0.4, 1.2, 2.0, 2.6};
        final double[] coordinates = new double[15];
        for (int i = 0; i < t.length; i++) {
            coordinates[3*i]     = 10 + 2 * Math.cos(t[i]);
            coordinates[3*i + 1] = 20 + Math.sin(t[i]);
            coordinates[3*i + 2] = 5;
        }
        final Conic conic = GeometryFactory.DEFAULT.createConic(
                GeometryFactory.DEFAULT.createDataPoints(CRS_3D, coordinates), null, false);
        assertEnvelope(conic.getEnvelope(), 10 + 2 * Math.cos(2.6), 20 + Math.sin(-0.3), 5, 12, 21, 5);
        // The same points with one of them out of the plane.
        coordinates[14] = 6;
        final Conic skew = GeometryFactory.DEFAULT.createConic(
                GeometryFactory.DEFAULT.createDataPoints(CRS_3D, coordinates), null, false);
        assertThrows(IllegalStateException.class, () -> skew.getEnvelope());
    }

    @Test
    public void testGetEnvelopeInvalid() {
        // Six points are not a whole number of arcs.
        assertThrows(IllegalStateException.class, () -> envelope(false, ellipse(0, 0.5, 1, 1.5, 2, 2.5)));
        // Points not in their order along the ellipse.
        assertThrows(IllegalStateException.class, () -> envelope(false, ellipse(0, 1.2, 0.4, 2.0, 2.6)));
        // Three collinear points: the conic degenerates into a pair of straight lines.
        assertThrows(IllegalStateException.class, () -> envelope(false, 0,0, 1,0, 2,0, 1,1, 0,2));
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetCentroid() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetRepresentativePoint() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetClosure() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetMaximalComplex() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testIsEmpty() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testIsSimple() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testIsValid() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testUserProperties() {
    }
}
