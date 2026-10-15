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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.apache.sis.geometries.GeometryTest;


/**
 * Tests {@link CircularString}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class CircularStringTest extends GeometryTest {
    @Test
    @Disabled("Not implemented yet.")
    public void testGetGeometryType() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetNumArcs() {
    }

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
    public void testGetCoordinateReferenceSystem() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testSetCoordinateReferenceSystem() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetAttributesType() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testIsEmpty() {
    }

    @Test
    public void testGetTopologicDimension() {
        final CircularString arc = GeometryFactory.DEFAULT.createCircularString(
                GeometryFactory.DEFAULT.createDataPoints(CRS_2D, 0,0, 5,5, 10,0, 5,-5, 0,0));
        assertEquals(1, arc.getTopologicDimension());
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
    public void testIsCycle() {
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
    public void testGetMetadata() {
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
    public void testGetGeometryType2() {
    }

    /**
     * Creates a two-dimensional circular string and returns its envelope.
     */
    private static Envelope envelope(final double... coordinates) {
        return GeometryFactory.DEFAULT.createCircularString(
                GeometryFactory.DEFAULT.createDataPoints(CRS_2D, coordinates)).getEnvelope();
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
    public void testGetEnvelope() {
        // Half circle centred on (5, 0).
        assertEnvelope(envelope(0,0, 5,5, 10,0), 0, 0, 10, 5);
        assertEquals(CRS_2D, envelope(0,0, 5,5, 10,0).getCoordinateReferenceSystem());
        // Minor arc: the box of the points, the middle point being the top of the arc.
        assertEnvelope(envelope(0,0, 5,2, 10,0), 0, 0, 10, 2);
        // Major arc of radius 5.5625 centred on (5, 2.4375): it goes beyond the end points on both sides.
        assertEnvelope(envelope(0,0, 5,8, 10,0), -0.5625, 0, 10.5625, 8);
        // Arc of the circle of radius 5 centred on (0, 0), from 0° to about 233° counter-clockwise.
        // The middle point is not where the arc is the furthest from the chord.
        assertEnvelope(envelope(5,0, 0,5, -3,-4), -5, -4, 5, 5);
        // Same arc in the other direction.
        assertEnvelope(envelope(-3,-4, 0,5, 5,0), -5, -4, 5, 5);
        // Two arcs making a full circle.
        assertEnvelope(envelope(0,0, 5,5, 10,0, 5,-5, 0,0), 0, -5, 10, 5);
        // Collinear points: a straight segment.
        assertEnvelope(envelope(0,0, 5,0, 10,0), 0, 0, 10, 0);
        // Identical start and end points: a full circle whose diameter goes from them to the middle point.
        assertEnvelope(envelope(0,0, 10,0, 0,0), 0, -5, 10, 5);
    }

    @Test
    public void testGetEnvelope3D() {
        // Half circle in the plane of the first and third axes.
        final CircularString arc = GeometryFactory.DEFAULT.createCircularString(
                GeometryFactory.DEFAULT.createDataPoints(CRS_3D, 0,0,0, 5,0,5, 10,0,0));
        assertEnvelope(arc.getEnvelope(), 0, 0, 0, 10, 0, 5);
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
