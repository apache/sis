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
package org.apache.sis.geometries.internal.shared;

import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;

// Test dependencies
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.apache.sis.geometries.GeometryTest;


/**
 * Tests {@link DefaultReversedCurve}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class DefaultReversedCurveTest extends GeometryTest {
    @Test
    public void testGetTopologicDimension() {
        final LineString base = GeometryFactory.DEFAULT.createLineString(
                GeometryFactory.DEFAULT.createDataPoints(CRS_2D, 0,0, 10,0));
        final Curve reversed = GeometryFactory.DEFAULT.createReversed(base);
        assertEquals(1, reversed.getTopologicDimension());
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetDataPoints() {
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
    public void testGetControlPoints() {
    }

    /**
     * A reversed curve keeps the construction parameterization of its base curve.
     */
    @Test
    public void testGetKnots() {
        final LineString base = GeometryFactory.DEFAULT.createLineString(
                GeometryFactory.DEFAULT.createDataPoints(CRS_2D, 0,0, 10,0, 10,10));
        assertArrayEquals(base.getKnots(), GeometryFactory.DEFAULT.createReversed(base).getKnots());
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetInterpolation() {
    }

    @Test
    public void testGetStartConstrParam() {
        final LineString base = GeometryFactory.DEFAULT.createLineString(
                GeometryFactory.DEFAULT.createDataPoints(CRS_2D, 0,0, 10,0, 10,10));
        assertEquals(2, GeometryFactory.DEFAULT.createReversed(base).getStartConstrParam());
    }

    @Test
    public void testGetEndConstrParam() {
        final LineString base = GeometryFactory.DEFAULT.createLineString(
                GeometryFactory.DEFAULT.createDataPoints(CRS_2D, 0,0, 10,0, 10,10));
        assertEquals(0, GeometryFactory.DEFAULT.createReversed(base).getEndConstrParam());
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

    /**
     * Distances along the reversed curve are measured from the end of the base curve.
     */
    @Test
    public void testSubCurve_Length() {
        final LineString base = GeometryFactory.DEFAULT.createLineString(
                GeometryFactory.DEFAULT.createDataPoints(CRS_2D, 0,0, 10,0, 10,10));
        final Curve reversed = GeometryFactory.DEFAULT.createReversed(base);
        assertPositionEquals(10, 10, reversed.subCurve(Quantities.create(  0, Units.PERCENT)));
        assertPositionEquals(10,  5, reversed.subCurve(Quantities.create( 25, Units.PERCENT)));
        assertPositionEquals( 0,  0, reversed.subCurve(Quantities.create(100, Units.PERCENT)));
        assertPositionEquals(10,  5, reversed.subCurve(Quantities.create(  5, Units.DEGREE)));
        assertPositionEquals( 5,  0, reversed.subCurve(Quantities.create( 15, Units.DEGREE)));
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

    @Test
    @Disabled("Not implemented yet.")
    public void testGetEnvelope() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetCentroid() {
    }

    @Test
    public void testGetRepresentativePoint() {
        final LineString base = GeometryFactory.DEFAULT.createLineString(
                GeometryFactory.DEFAULT.createDataPoints(CRS_2D, 0,0, 10,0));
        final Curve reversed = GeometryFactory.DEFAULT.createReversed(base);
        assertPositionEquals(5, 0, reversed.getRepresentativePoint());
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
