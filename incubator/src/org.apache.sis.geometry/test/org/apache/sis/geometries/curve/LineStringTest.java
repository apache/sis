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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.measure.Quantity;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.AttributeInterpolation;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.Point;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.ReadOnly;
import org.apache.sis.maths.SampleSystem;
import org.apache.sis.maths.Vector;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.apache.sis.geometries.GeometryTest;


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
    public void testGetTopologicDimension() {
        assertEquals(1, createLineString(CRS_2D, SEGMENT).getTopologicDimension());
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
        final GeometryCollection<?> ends = assertInstanceOf(GeometryCollection.class, boundary,
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

    /**
     * The point at a distance along a curve of two segments of length 10, the distance being given
     * either in the unit of the axes or in percent of the length.
     */
    @Test
    public void testSubCurve_Length() {
        final LineString curve = createLineString(CRS_2D, 0,0, 10,0, 10,10);
        assertPositionEquals( 0,  0, curve.subCurve(Quantities.create(  0, Units.PERCENT)));
        assertPositionEquals(10,  5, curve.subCurve(Quantities.create( 75, Units.PERCENT)));
        assertPositionEquals(10, 10, curve.subCurve(Quantities.create(100, Units.PERCENT)));
        assertPositionEquals( 5,  0, curve.subCurve(Quantities.create(  5, Units.DEGREE)));
        assertPositionEquals(10,  0, curve.subCurve(Quantities.create( 10, Units.DEGREE)));
        assertPositionEquals(10,  8, curve.subCurve(Quantities.create( 18, Units.DEGREE)));
    }

    /**
     * The attributes other than the position are kept, each computed as its attribute interpolation
     * declares. The point at 80% lies at 60% of the second segment, between the values 10 and 30.
     */
    @Test
    public void testSubCurve_LengthWithAttributes() {
        final Map<String,Array> attributes = new LinkedHashMap<>();
        attributes.put(DataPointsType.ATT_POSITION, NDArrays.of(CRS_2D, new double[] {0,0, 10,0, 10,10}));
        attributes.put("nearest", NDArrays.of(1, new double[] {0, 10, 30}));
        attributes.put("linear",  NDArrays.of(1, new double[] {0, 10, 30}));
        attributes.put("leading", NDArrays.of(1, new double[] {0, 10, 30}));
        final LineString curve = GeometryFactory.DEFAULT.createLineString(withInterpolations(
                GeometryFactory.DEFAULT.createDataPoints(attributes),
                Map.of("linear",  AttributeInterpolation.LINEAR,
                       "leading", AttributeInterpolation.LEADING)));

        Point point = curve.subCurve(Quantities.create(80, Units.PERCENT));
        assertPositionEquals(10, 6, point);
        assertEquals(Set.of(DataPointsType.ATT_POSITION, "nearest", "linear", "leading"),
                     Set.copyOf(point.getDataPointsType().getAttributeNames()));
        assertEquals(30, point.getAttribute("nearest").get(0), TOLERANCE, "Value of the nearest point.");
        assertEquals(22, point.getAttribute("linear" ).get(0), TOLERANCE, "Interpolated along the segment.");
        assertEquals(10, point.getAttribute("leading").get(0), TOLERANCE, "Value of the segment start.");

        point = curve.subCurve(Quantities.create(100, Units.PERCENT));
        assertEquals(30, point.getAttribute("leading").get(0), TOLERANCE, "The end point has its own value.");
    }

    /**
     * Returns a view of the given sequence declaring the given attribute interpolations.
     * Attributes absent from the map keep the interpolation declared by the sequence.
     */
    private static DataPoints withInterpolations(final DataPoints points, final Map<String,AttributeInterpolation> methods) {
        final DataPointsType base = points.getType();
        final DataPointsType type = new DataPointsType() {
            @Override public List<String> getAttributeNames()            {return base.getAttributeNames();}
            @Override public SampleSystem getAttributeSystem(String name) {return base.getAttributeSystem(name);}
            @Override public DataType     getAttributeType(String name)   {return base.getAttributeType(name);}
            @Override public AttributeInterpolation getAttributeInterpolation(String name) {
                return methods.getOrDefault(name, base.getAttributeInterpolation(name));
            }
        };
        return new DataPoints() {
            @Override public CoordinateReferenceSystem getCoordinateReferenceSystem() {return points.getCoordinateReferenceSystem();}
            @Override public void setCoordinateReferenceSystem(CoordinateReferenceSystem crs) {points.setCoordinateReferenceSystem(crs);}
            @Override public DataPointsType getType() {return type;}
            @Override public int size() {return points.size();}
            @Override public Point getPoint(int index) {return points.getPoint(index);}
            @Override public Vector<?> getPosition(int index) {return points.getPosition(index);}
            @Override public void setPosition(int index, ReadOnly.Vector<?> value) {points.setPosition(index, value);}
            @Override public Vector<?> getAttribute(int index, String name) {return points.getAttribute(index, name);}
            @Override public void setAttribute(int index, String name, ReadOnly.Vector<?> value) {points.setAttribute(index, name, value);}
        };
    }

    /**
     * A distance outside the curve, or which cannot be expressed in the unit of the axes, is rejected.
     */
    @Test
    public void testSubCurve_LengthOutside() {
        final LineString curve = createLineString(CRS_2D, SEGMENT);
        assertThrows(IllegalArgumentException.class, () -> curve.subCurve(Quantities.create(101, Units.PERCENT)));
        assertThrows(IllegalArgumentException.class, () -> curve.subCurve(Quantities.create( -1, Units.DEGREE)));
        assertThrows(IllegalArgumentException.class, () -> curve.subCurve(Quantities.create( 11, Units.DEGREE)));
        assertThrows(IllegalArgumentException.class, () -> curve.subCurve(Quantities.create(  5, Units.METRE)));
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
     * The point at middle distance along a curve made of a single segment is that segment's midpoint.
     */
    @Test
    public void testGetRepresentativePoint() {
        assertPositionEquals(5, 0, createLineString(CRS_2D, SEGMENT).getRepresentativePoint());
        assertPositionEquals(5, 5, createLineString(CRS_2D, DIAGONAL).getRepresentativePoint());
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
