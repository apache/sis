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

import javax.measure.Unit;
import org.opengis.geometry.Envelope;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.Point;
import org.apache.sis.measure.Units;

// Test dependencies
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.apache.sis.geometries.GeometryTest;


/**
 * Tests {@link ArcByCenterPoint}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class ArcByCenterPointTest extends GeometryTest {
    @Test
    @Disabled("Not implemented yet.")
    public void testGetGeometryType() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetCenter() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetRadius() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetRadiusUnit() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetStartAngle() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetEndAngle() {
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
        final Point center = GeometryFactory.DEFAULT.createPoint(CRS_2D, 10, 20);
        final ArcByCenterPoint arc = GeometryFactory.DEFAULT.createArcByCenterPoint(center, 5.0, null, 0.0, 90.0);
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
     * Creates an arc around (10, 20) and returns its envelope.
     */
    private static Envelope envelope(final double radius, final Unit<?> unit, final double startAngle, final double endAngle) {
        final Point center = GeometryFactory.DEFAULT.createPoint(CRS_2D, 10, 20);
        return GeometryFactory.DEFAULT.createArcByCenterPoint(center, radius, unit, startAngle, endAngle).getEnvelope();
    }

    private static void assertEnvelope(final Envelope envelope, final double xmin, final double ymin,
                                       final double xmax, final double ymax)
    {
        assertEquals(CRS_2D, envelope.getCoordinateReferenceSystem());
        assertEquals(xmin, envelope.getMinimum(0), TOLERANCE, "xmin");
        assertEquals(ymin, envelope.getMinimum(1), TOLERANCE, "ymin");
        assertEquals(xmax, envelope.getMaximum(0), TOLERANCE, "xmax");
        assertEquals(ymax, envelope.getMaximum(1), TOLERANCE, "ymax");
    }

    @Test
    public void testGetEnvelope() {
        final double d = 5 * Math.sqrt(0.5);
        // Quarter arc between two extremes: the envelope is the box of its end points.
        assertEnvelope(envelope(5, null,   0,  90), 10,     20,     15,     25);
        // Arc crossing the extreme in the direction of the second axis.
        assertEnvelope(envelope(5, null,  45, 135), 10 - d, 20 + d, 10 + d, 25);
        // Arc crossing the extreme in the direction of the first axis, at angle 0.
        assertEnvelope(envelope(5, null, -45,  45), 10 + d, 20 - d, 15,     20 + d);
        // Same arc, with angles beyond 360°.
        assertEnvelope(envelope(5, null, 315, 405), 10 + d, 20 - d, 15,     20 + d);
        // Clockwise sweep over the same quarter as the first case.
        assertEnvelope(envelope(5, null,  90,   0), 10,     20,     15,     25);
        // Clockwise sweep from 90° to -90° through 0°, not through 180°.
        assertEnvelope(envelope(5, null,  90, -90), 10,     15,     15,     25);
        // Sweep of more than half a turn, crossing three extremes.
        assertEnvelope(envelope(5, null,  45, 315), 5,      15,     10 + d, 25);
        // Full circle.
        assertEnvelope(envelope(5, null,   0, 360), 5,      15,     15,     25);
        // Degenerated arc: a single position.
        assertEnvelope(envelope(5, null,  90,  90), 10,     25,     10,     25);
    }

    @Test
    public void testGetEnvelopeWithRadiusUnit() {
        // 300 arc-minutes are 5 degrees, the unit of the axes of `CRS_2D`.
        assertEnvelope(envelope(300, Units.ARC_MINUTE, 0, 360), 5, 15, 15, 25);
        // A radius in metres cannot be converted to degrees without a geodesic computation.
        assertThrows(UnsupportedOperationException.class, () -> envelope(5, Units.METRE, 0, 90));
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
