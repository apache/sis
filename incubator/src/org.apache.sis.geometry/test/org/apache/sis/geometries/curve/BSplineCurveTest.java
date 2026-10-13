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

import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;

// Test dependencies
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.apache.sis.geometries.GeometryTest;


/**
 * Tests {@link BSplineCurve}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class BSplineCurveTest extends GeometryTest {
    @Test
    @Disabled("Not implemented yet.")
    public void testGetInterpolation() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetCurveForm() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetKnots() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetDegree() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetKnotSpec() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testIsRational() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetNumArc() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetSegment() {
    }

    @Test
    public void testGetTopologicDimension() {
        final BSplineCurve curve = GeometryFactory.DEFAULT.createBSplineCurve(
                GeometryFactory.DEFAULT.createDataPoints(CRS_2D, 0,0, 10,0), null, null, 3,
                null, null, false);
        assertEquals(1, curve.getTopologicDimension());
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetDataPoints() {
    }

    /**
     * A b-spline of degree 1 is the polyline of its control points.
     */
    @Test
    public void testGetLength() {
        assertEquals(20, createPolyline().getLength().getValue().doubleValue(), 1E-9);
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

    /**
     * Creates a cubic b-spline clamped at both ends without interior knot, which is the Bézier curve
     * of its 4 control points. The control polygon is symmetric around x = 5, so is the curve.
     */
    private static BSplineCurve createArch() {
        return GeometryFactory.DEFAULT.createBSplineCurve(
                GeometryFactory.DEFAULT.createDataPoints(CRS_2D, 0,0, 0,10, 10,10, 10,0), null, null, 3,
                null, null, false);
    }

    /**
     * Creates a b-spline of degree 1 given by its distinct knot values,
     * which is the polyline (0 0) (10 0) (10 10) of its control points.
     */
    private static BSplineCurve createPolyline() {
        return GeometryFactory.DEFAULT.createBSplineCurve(
                GeometryFactory.DEFAULT.createDataPoints(CRS_2D, 0,0, 10,0, 10,10), null, new double[] {0, 1, 2}, 1,
                null, null, false);
    }

    @Test
    public void testSubCurve_Length() {
        final BSplineCurve arch = createArch();
        assertPositionEquals( 0, 0,   arch.subCurve(Quantities.create(  0, Units.PERCENT)));
        assertPositionEquals( 5, 7.5, arch.subCurve(Quantities.create( 50, Units.PERCENT)));
        assertPositionEquals(10, 0,   arch.subCurve(Quantities.create(100, Units.PERCENT)));

        final BSplineCurve polyline = createPolyline();
        assertPositionEquals( 5, 0, polyline.subCurve(Quantities.create(25, Units.PERCENT)));
        assertPositionEquals(10, 5, polyline.subCurve(Quantities.create(75, Units.PERCENT)));
        assertPositionEquals(10, 8, polyline.subCurve(Quantities.create(18, Units.DEGREE)));
    }

    /**
     * Rational b-splines are not supported: NURBS curves shall be used instead.
     */
    @Test
    public void testSubCurve_LengthRational() {
        final BSplineCurve curve = GeometryFactory.DEFAULT.createBSplineCurve(
                GeometryFactory.DEFAULT.createDataPoints(CRS_2D, 0,0, 0,10, 10,10, 10,0), null, null, 3,
                null, null, true);
        assertThrows(UnsupportedOperationException.class, () -> curve.subCurve(Quantities.create(50, Units.PERCENT)));
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
        assertPositionEquals(5, 7.5, createArch().getRepresentativePoint());
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
