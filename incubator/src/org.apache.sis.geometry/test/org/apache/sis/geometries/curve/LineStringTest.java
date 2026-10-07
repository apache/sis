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

import javax.measure.Quantity;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryTest;
import org.apache.sis.geometries.point.MultiPoint;
import org.apache.sis.measure.Units;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link LineString}.
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class LineStringTest extends GeometryTest {
    /**
     * A horizontal segment of length 10, from (0 0) to (10 0).
     */
    private static final double[] SEGMENT = {0,0, 10,0};

    /**
     * The diagonal of a square of 10 × 10, of length 10√2.
     */
    private static final double[] DIAGONAL = {0,0, 10,10};

    /**
     * A curve crossing itself at (5 5). It is valid, a curve being allowed to cross itself,
     * but it is not simple.
     */
    private static final double[] SELF_CROSSING = {0,0, 10,10, 10,0, 0,10};

    protected LineStringTest() {
    }

    /**
     * Creates a curve through the given positions in the given coordinate reference system.
     *
     * @param  crs          the coordinate reference system of the curve to create, not null.
     * @param  coordinates  the coordinates of the positions, in the axis order of the given system.
     * @return a new curve through the given positions, never null.
     */
    protected abstract LineString createLineString(CoordinateReferenceSystem crs, double... coordinates);

    @Test
    @Disabled("Not implemented yet.")
    public void testGetGeometryType() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetInterpolation() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testAsLine() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetNumPoints() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetPointN() {
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
    public void testIsLine() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetEnvelope() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetTopologicDimension() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetDataPoints() {
    }

    /**
     * The length is measured in the units of the coordinate system axes: stating it in metres would
     * claim a measurement on the reference surface, which is not what is computed.
     */
    @Test
    public void testGetLength() {
        final Quantity<?> length = createLineString(CRS_2D, SEGMENT).getLength();
        assertEquals(10, length.getValue().doubleValue(), TOLERANCE, "A horizontal segment of 10.");
        assertEquals(Units.DEGREE, length.getUnit(), "The length shall use the unit of the axes.");
        assertEquals(10 * Math.sqrt(2), createLineString(CRS_2D, DIAGONAL).getLength().getValue().doubleValue(),
                     TOLERANCE, "The diagonal of a square of 10 × 10.");
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

    /**
     * The boundary of an open curve is its two end positions.
     */
    @Test
    public void testGetBoundary() {
        final Geometry boundary = createLineString(CRS_2D, SEGMENT).getBoundary();
        final MultiPoint<?> ends = assertInstanceOf(MultiPoint.class, boundary,
                "The boundary of an open curve is its two end positions.");
        assertEquals(2, ends.getNumGeometries());
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
     * The centroid of a curve is weighted by length, and is therefore the middle of a segment.
     */
    @Test
    public void testGetCentroid() {
        assertPositionEquals(5, 0, createLineString(CRS_2D, SEGMENT).getCentroid());
        assertPositionEquals(5, 5, createLineString(CRS_2D, DIAGONAL).getCentroid());
    }

    /**
     * Contrarily to the centroid, the returned position shall lie on the curve, but which position
     * is returned is left to the implementation. The segment under test being horizontal, lying on
     * it means having a null second ordinate and a first ordinate within the range of the segment.
     */
    @Test
    public void testGetRepresentativePoint() {
        final double[] position = createLineString(CRS_2D, SEGMENT).getRepresentativePoint()
                                                                   .getPosition().toArrayDouble();
        assertEquals(0, position[1], TOLERANCE, "The position shall lie on the segment.");
        assertTrue(position[0] >= 0 && position[0] <= 10, "The position shall lie on the segment.");
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

    /**
     * A curve is simple when it does not cross itself.
     */
    @Test
    public void testIsSimple() {
        assertTrue (createLineString(CRS_2D, SEGMENT).isSimple(),       "A segment does not cross itself.");
        assertTrue (createLineString(CRS_2D, DIAGONAL).isSimple(),      "A segment does not cross itself.");
        assertFalse(createLineString(CRS_2D, SELF_CROSSING).isSimple(), "A curve crossing itself is not simple.");
    }

    /**
     * Validity and simplicity are independent on a curve: a curve is allowed to cross itself and
     * stays valid.
     */
    @Test
    public void testIsValid() {
        assertTrue(createLineString(CRS_2D, SEGMENT).isValid(),       "A segment is a valid curve.");
        assertTrue(createLineString(CRS_2D, SELF_CROSSING).isValid(), "A curve is allowed to cross itself.");
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testUserProperties() {
    }
}
