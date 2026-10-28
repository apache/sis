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
package org.apache.sis.geometries;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.NDRange;
import org.apache.sis.maths.Vector;
import org.apache.sis.maths.Vectors;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link DataPoints}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class DataPointsTest {
    /**
     * Creates the {@code DataPoints} used by most tests: the positions (0 0), (10 5) and (4 20) in
     * {@link GeometryTest#CRS_2D}, with a one-dimensional "temperature" attribute of values 10, 30 and 20.
     * The returned instance implements only the abstract methods, in order to test the default methods.
     */
    private static DataPoints createSample() {
        return GeometryTest.withInterpolations(createArraySample(), Map.of());
    }

    /**
     * Creates the points of {@link #createSample()}, stored in the arrays of the default implementation.
     */
    protected static DataPoints createArraySample() {
        final Map<String,Array> attributes = new LinkedHashMap<>();
        attributes.put(DataPointsType.ATT_POSITION, NDArrays.of(GeometryTest.CRS_2D, new double[] {0,0, 10,5, 4,20}));
        attributes.put("temperature", NDArrays.of(1, new double[] {10, 30, 20}));
        return GeometryFactory.DEFAULT.createDataPoints(attributes);
    }

    /**
     * Tests {@link DataPoints#subset(int[])} and {@link DataPoints#subset(int, int)}.
     */
    @Test
    public void testSubset() {
        assertSubsets(createSample());
    }

    /**
     * Verifies the copies of points made by the given {@code DataPoints},
     * which shall hold the points of {@link #createSample()}.
     */
    protected static void assertSubsets(final DataPoints points) {
        final DataPoints subset = points.subset(new int[] {2, 0, 2});
        assertEquals(3, subset.size());
        assertEquals(points.getCoordinateReferenceSystem(), subset.getCoordinateReferenceSystem());
        assertEquals(Set.copyOf(points.getType().getAttributeNames()), Set.copyOf(subset.getType().getAttributeNames()));
        assertPositionEquals(4, 20, subset.getPosition(0));
        assertPositionEquals(0,  0, subset.getPosition(1));
        assertPositionEquals(4, 20, subset.getPosition(2));
        assertEquals(20, subset.getAttribute(0, "temperature").get(0));
        assertEquals(10, subset.getAttribute(1, "temperature").get(0));

        final DataPoints range = points.subset(1, 2);
        assertEquals(2, range.size());
        assertPositionEquals(10,  5, range.getPosition(0));
        assertPositionEquals( 4, 20, range.getPosition(1));
        assertEquals(30, range.getAttribute(0, "temperature").get(0));
        assertEquals(0, points.subset(3, 0).size());

        //the copies are independent of the copied points.
        final Vector<?> value = range.getAttribute(0, "temperature");
        range.setAttribute(0, "temperature", Vectors.create(new double[] {99}, value.getDataType()));
        assertEquals(30, points.getAttribute(1, "temperature").get(0));

        assertThrows(IndexOutOfBoundsException.class, () -> points.subset(new int[] {3}));
        assertThrows(IndexOutOfBoundsException.class, () -> points.subset(new int[] {-1}));
        assertThrows(IndexOutOfBoundsException.class, () -> points.subset(2, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> points.subset(-1, 1));
    }

    /**
     * Creates a {@code DataPoints} holding no point, implementing only the abstract methods.
     */
    private static DataPoints createEmpty() {
        return GeometryTest.withInterpolations(GeometryFactory.DEFAULT.createDataPoints(
                NDArrays.of(GeometryTest.CRS_2D, new double[0])), Map.of());
    }

    @Test
    public void testGetCoordinateReferenceSystem() {
        assertEquals(GeometryTest.CRS_2D, createSample().getCoordinateReferenceSystem());
    }

    /**
     * Changing the reference system does not transform the coordinates,
     * and a system of another number of dimensions is rejected.
     */
    @Test
    public void testSetCoordinateReferenceSystem() {
        final DataPoints points = createSample();
        final CoordinateReferenceSystem crs = CommonCRS.NAD83.normalizedGeographic();
        points.setCoordinateReferenceSystem(crs);
        assertEquals(crs, points.getCoordinateReferenceSystem());
        assertEquals(crs, points.getType().getAttributeSystem(DataPointsType.ATT_POSITION).getCoordinateReferenceSystem());
        assertPositionEquals(10, 5, points.getPosition(1));
        assertThrows(IllegalArgumentException.class, () -> points.setCoordinateReferenceSystem(GeometryTest.CRS_3D));
    }

    @Test
    public void testGetDimension() {
        assertEquals(2, createSample().getDimension());
    }

    /**
     * The type describes the position and the other attributes.
     */
    @Test
    public void testGetAttributesType() {
        final DataPointsType type = createSample().getType();
        assertEquals(Set.of(DataPointsType.ATT_POSITION, "temperature"), Set.copyOf(type.getAttributeNames()));
        assertEquals(GeometryTest.CRS_2D, type.getAttributeSystem(DataPointsType.ATT_POSITION).getCoordinateReferenceSystem());
        assertEquals(2, type.getAttributeSystem(DataPointsType.ATT_POSITION).getSize());
        assertEquals(1, type.getAttributeSystem("temperature").getSize());
        assertEquals(DataType.DOUBLE, type.getAttributeType(DataPointsType.ATT_POSITION));
        assertEquals(DataType.DOUBLE, type.getAttributeType("temperature"));
    }

    /**
     * The envelope is the range of the positions, in the reference system of the points.
     */
    @Test
    public void testGetEnvelope() {
        final Envelope envelope = createSample().getEnvelope();
        assertEquals(GeometryTest.CRS_2D, envelope.getCoordinateReferenceSystem());
        assertEquals( 0, envelope.getMinimum(0));
        assertEquals( 0, envelope.getMinimum(1));
        assertEquals(10, envelope.getMaximum(0));
        assertEquals(20, envelope.getMaximum(1));
        assertNull(createEmpty().getEnvelope(), "An empty sequence has no envelope.");
    }

    @Test
    public void testIsEmpty() {
        assertFalse(createSample().isEmpty());
        assertTrue (createEmpty().isEmpty());
    }

    @Test
    public void testSize() {
        assertEquals(3, createSample().size());
        assertEquals(0, createEmpty().size());
    }

    /**
     * A point gives the position and the attributes of the point at the same index.
     */
    @Test
    public void testGetPoint() {
        final Point point = createSample().getPoint(1);
        assertEquals(GeometryTest.CRS_2D, point.getCoordinateReferenceSystem());
        GeometryTest.assertPositionEquals(10, 5, point);
        assertEquals(30, point.getAttribute("temperature").get(0));
    }

    /**
     * The returned position is the position of the point at the given index.
     */
    @Test
    public void testGetPosition() {
        final DataPoints points = createSample();
        final Vector<?> position = points.getPosition(2);
        assertPositionEquals(4, 20, position);
        assertPositionEquals(4, 20, points.getPosition(2));
    }

    /**
     * Setting a position moves only the point at the given index.
     */
    @Test
    public void testSetPosition() {
        final DataPoints points = createSample();
        final Vector<?> position = points.getPosition(1);
        points.setPosition(1, Vectors.create(new double[] {7, 8}, position.getDataType()));
        assertPositionEquals(0, 0, points.getPosition(0));
        assertPositionEquals(7, 8, points.getPosition(1));
        assertPositionEquals(4, 20, points.getPosition(2));
    }

    /**
     * The position is an attribute like the others.
     */
    @Test
    public void testGetAttribute() {
        final DataPoints points = createSample();
        assertEquals(20, points.getAttribute(2, "temperature").get(0));
        assertPositionEquals(10, 5, points.getAttribute(1, DataPointsType.ATT_POSITION));
        assertEquals(10, points.getAttribute(0, "temperature").get(0));
    }

    /**
     * Setting an attribute changes only the value at the given index.
     */
    @Test
    public void testSetAttribute() {
        final DataPoints points = createSample();
        final Vector<?> value = points.getAttribute(0, "temperature");
        points.setAttribute(0, "temperature", Vectors.create(new double[] {15}, value.getDataType()));
        assertEquals(15, points.getAttribute(0, "temperature").get(0));
        assertEquals(30, points.getAttribute(1, "temperature").get(0));
    }

    /**
     * The array holds the values of all points, and is a copy.
     */
    @Test
    public void testGetAttributeArray() {
        final DataPoints points = createSample();
        final Array array = points.getAttributeArray("temperature");
        assertEquals(3, array.getLength());
        assertArrayEquals(new double[] {10, 30, 20}, array.toArrayDouble());
        assertArrayEquals(new double[] {0,0, 10,5, 4,20}, points.getAttributeArray(DataPointsType.ATT_POSITION).toArrayDouble());

        array.set(0, Vectors.create(new double[] {-1}, array.getDataType()));
        assertEquals(10, points.getAttribute(0, "temperature").get(0), "The array shall be a copy.");
    }

    /**
     * The range of an attribute is in the system of that attribute: the reference system of the points
     * for the position, a one-dimensional range for the temperature.
     */
    @Test
    public void testGetAttributeRange() {
        final DataPoints points = createSample();
        final NDRange positions = points.getAttributeRange(DataPointsType.ATT_POSITION);
        assertEquals(GeometryTest.CRS_2D, positions.getCoordinateReferenceSystem());
        assertEquals( 0, positions.getMinimum(0));
        assertEquals( 0, positions.getMinimum(1));
        assertEquals(10, positions.getMaximum(0));
        assertEquals(20, positions.getMaximum(1));

        final NDRange temperature = points.getAttributeRange("temperature");
        assertEquals(1, temperature.getDimension());
        assertEquals(10, temperature.getMinimum(0));
        assertEquals(30, temperature.getMaximum(0));

        assertNull(createEmpty().getAttributeRange("temperature"), "An empty sequence has no range.");
    }

    /**
     * Asserts that the given vector is the expected 2-dimensional position.
     */
    private static void assertPositionEquals(final double x, final double y, final Vector<?> actual) {
        assertArrayEquals(new double[] {x, y}, actual.toArrayDouble());
    }

    /**
     * Creates a {@code DataPoints} of the given positions, holding the given values in three attributes
     * interpolated in the three possible ways.
     */
    private static DataPoints createPoints(final double[] positions, final double[] values) {
        final Map<String,Array> attributes = new LinkedHashMap<>();
        attributes.put(DataPointsType.ATT_POSITION, NDArrays.of(GeometryTest.CRS_2D, positions));
        attributes.put("nearest", NDArrays.of(1, values.clone()));
        attributes.put("linear",  NDArrays.of(1, values.clone()));
        attributes.put("leading", NDArrays.of(1, values.clone()));
        return GeometryTest.withInterpolations(GeometryFactory.DEFAULT.createDataPoints(attributes),
                Map.of("nearest", AttributeInterpolation.NEAREST,
                       "linear",  AttributeInterpolation.LINEAR,
                       "leading", AttributeInterpolation.LEADING));
    }

    /**
     * Asserts that the given point has the expected position and attribute values.
     */
    private static void assertPoint(final Point point, final double x, final double y,
            final double nearest, final double linear, final double leading)
    {
        GeometryTest.assertPositionEquals(x, y, point);
        assertEquals(Set.of(DataPointsType.ATT_POSITION, "nearest", "linear", "leading"),
                     Set.copyOf(point.getDataPointsType().getAttributeNames()));
        assertEquals(nearest, point.getAttribute("nearest").get(0), GeometryTest.TOLERANCE, "nearest");
        assertEquals(linear,  point.getAttribute("linear" ).get(0), GeometryTest.TOLERANCE, "linear");
        assertEquals(leading, point.getAttribute("leading").get(0), GeometryTest.TOLERANCE, "leading");
    }

    /**
     * Interpolation on the segment from (0 0) to (10 0), holding the values 10 and 30.
     * The leading value applies to the whole segment except its end point.
     */
    @Test
    public void testInterpolateSegment() {
        final DataPoints points = createPoints(new double[] {0,0, 10,0}, new double[] {10, 30});
        assertPoint(points.getInterpolatedPoint(0, 1, 0),     0, 0, 10, 10, 10);
        assertPoint(points.getInterpolatedPoint(0, 1, 0.25), 2.5, 0, 10, 15, 10);
        assertPoint(points.getInterpolatedPoint(0, 1, 0.5),    5, 0, 10, 20, 10);
        assertPoint(points.getInterpolatedPoint(0, 1, 0.75), 7.5, 0, 30, 25, 10);
        assertPoint(points.getInterpolatedPoint(0, 1, 1),     10, 0, 30, 30, 30);
    }

    /**
     * Interpolation in the triangle (0 0) (10 0) (0 10), holding the values 10, 20 and 30, at the
     * barycentric coordinates (0.2, 0.3, 0.5). The leading value applies to the whole triangle.
     */
    @Test
    public void testInterpolateTriangle() {
        final DataPoints points = createPoints(new double[] {0,0, 10,0, 0,10}, new double[] {10, 20, 30});
        assertPoint(points.getInterpolatedPoint(new int[] {0, 1, 2}, new double[] {0.2, 0.3, 0.5}), 3, 5, 30, 23, 10);
        assertPoint(points.getInterpolatedPoint(new int[] {0, 1, 2}, new double[] {0, 1, 0}),      10, 0, 20, 20, 10);
        assertPoint(points.getInterpolatedPoint(new int[] {2, 0, 1}, new double[] {0.5, 0.2, 0.3}), 3, 5, 30, 23, 30);
    }

    /**
     * A single attribute is interpolated without creating a point, the same way as in a point.
     */
    @Test
    public void testInterpolateAttribute() {
        final DataPoints segment = createPoints(new double[] {0,0, 10,0}, new double[] {10, 30});
        assertEquals(10, segment.interpolateAttribute("nearest", 0, 1, 0.25).get(0), GeometryTest.TOLERANCE);
        assertEquals(15, segment.interpolateAttribute("linear",  0, 1, 0.25).get(0), GeometryTest.TOLERANCE);
        assertEquals(30, segment.interpolateAttribute("leading", 0, 1, 1).get(0),    GeometryTest.TOLERANCE);
        final Vector<?> position = segment.interpolateAttribute(DataPointsType.ATT_POSITION, 0, 1, 0.25);
        assertEquals(2.5, position.get(0), GeometryTest.TOLERANCE);
        assertEquals(0,   position.get(1), GeometryTest.TOLERANCE);

        final DataPoints triangle = createPoints(new double[] {0,0, 10,0, 0,10}, new double[] {10, 20, 30});
        final int[]    indices = {0, 1, 2};
        final double[] weights = {0.2, 0.3, 0.5};
        assertEquals(30, triangle.interpolateAttribute("nearest", indices, weights).get(0), GeometryTest.TOLERANCE);
        assertEquals(23, triangle.interpolateAttribute("linear",  indices, weights).get(0), GeometryTest.TOLERANCE);
        assertEquals(10, triangle.interpolateAttribute("leading", indices, weights).get(0), GeometryTest.TOLERANCE);
    }

    /**
     * Interpolation in the square (0 0) (10 0) (10 10) (0 10), holding the values 10, 20, 30 and 40,
     * with the bilinear weights of the position (2.5 7.5). The leading value applies to the whole square.
     */
    @Test
    public void testInterpolateQuadrilateral() {
        final DataPoints points = createPoints(new double[] {0,0, 10,0, 10,10, 0,10}, new double[] {10, 20, 30, 40});
        final double s = 0.25, t = 0.75;
        final double[] weights = {(1-s)*(1-t), s*(1-t), s*t, (1-s)*t};
        assertPoint(points.getInterpolatedPoint(new int[] {0, 1, 2, 3}, weights), 2.5, 7.5, 40, 31.25, 10);
    }

    /**
     * Interpolation with a single point gives the values of that point.
     */
    @Test
    public void testInterpolateSinglePoint() {
        final DataPoints points = createPoints(new double[] {0,0, 10,0}, new double[] {10, 20});
        assertPoint(points.getInterpolatedPoint(new int[] {1}, new double[] {1}), 10, 0, 20, 20, 20);
    }

    /**
     * At least one point, with one weight each, is required.
     */
    @Test
    public void testInterpolateInvalid() {
        final DataPoints points = createPoints(new double[] {0,0, 10,0, 0,10, 10,10}, new double[] {10, 20, 30, 40});
        assertThrows(IllegalArgumentException.class, () -> points.getInterpolatedPoint(new int[0], new double[0]));
        assertThrows(IllegalArgumentException.class, () -> points.getInterpolatedPoint(new int[] {0, 1, 2}, new double[] {0.5, 0.5}));
        assertThrows(IllegalArgumentException.class, () -> points.interpolateAttribute("linear", new int[0], new double[0]));
        assertThrows(IllegalArgumentException.class, () -> points.interpolateAttribute("linear", new int[] {0, 1}, new double[] {1}));
        assertThrows(IllegalArgumentException.class, () -> points.viewInterpolatedPoint(new int[0], new double[0]));
        assertThrows(IllegalArgumentException.class, () -> points.viewInterpolatedPoint(new int[] {0, 1}, new double[] {1}));
    }

    /**
     * The normalized linear interpolation of two orthogonal unit vectors gives the unit vector of the
     * bisector, while their linear interpolation gives a vector of length √½ only.
     */
    @Test
    public void testInterpolateLinearNormalized() {
        final Map<String,Array> attributes = new LinkedHashMap<>();
        attributes.put(DataPointsType.ATT_POSITION, NDArrays.of(GeometryTest.CRS_2D, new double[] {0,0, 10,0}));
        attributes.put("linear",     NDArrays.of(2, new double[] {1,0, 0,1}));
        attributes.put("normalized", NDArrays.of(2, new double[] {1,0, 0,1}));
        final DataPoints points = GeometryTest.withInterpolations(GeometryFactory.DEFAULT.createDataPoints(attributes),
                Map.of("linear",     AttributeInterpolation.LINEAR,
                       "normalized", AttributeInterpolation.LINEAR_NORMALIZED));

        final Vector<?> linear = points.interpolateAttribute("linear", 0, 1, 0.5);
        assertEquals(0.5, linear.get(0), GeometryTest.TOLERANCE);
        assertEquals(0.5, linear.get(1), GeometryTest.TOLERANCE);

        final double h = Math.sqrt(0.5);
        final Vector<?> normalized = points.interpolateAttribute("normalized", 0, 1, 0.5);
        assertEquals(h, normalized.get(0), GeometryTest.TOLERANCE);
        assertEquals(h, normalized.get(1), GeometryTest.TOLERANCE);

        final Vector<?> fromPoint = points.getInterpolatedPoint(0, 1, 0.25).getAttribute("normalized");
        assertEquals(1, fromPoint.length(), GeometryTest.TOLERANCE, "The interpolated vector shall have a unit length.");
        assertEquals(3 / Math.sqrt(10), fromPoint.get(0), GeometryTest.TOLERANCE);
        assertEquals(1 / Math.sqrt(10), fromPoint.get(1), GeometryTest.TOLERANCE);
    }

    /**
     * Normals and tangents are interpolated as unit vectors without declaring it, while other
     * attributes are interpolated linearly by default.
     */
    @Test
    public void testDefaultInterpolation() {
        final Map<String,Array> attributes = new LinkedHashMap<>();
        attributes.put(DataPointsType.ATT_POSITION, NDArrays.of(GeometryTest.CRS_2D, new double[] {0,0, 10,0}));
        attributes.put(DataPointsType.ATT_NORMAL,   NDArrays.of(2, new double[] {1,0, 0,1}));
        attributes.put("other",                     NDArrays.of(2, new double[] {1,0, 0,1}));
        final DataPoints points = GeometryFactory.DEFAULT.createDataPoints(attributes);
        final DataPointsType type = points.getType();
        assertEquals(AttributeInterpolation.LINEAR,            type.getAttributeInterpolation(DataPointsType.ATT_POSITION));
        assertEquals(AttributeInterpolation.LINEAR_NORMALIZED, type.getAttributeInterpolation(DataPointsType.ATT_NORMAL));
        assertEquals(AttributeInterpolation.LINEAR_NORMALIZED, type.getAttributeInterpolation(DataPointsType.ATT_TANGENT));
        assertEquals(AttributeInterpolation.LINEAR,            type.getAttributeInterpolation("other"));

        final double h = Math.sqrt(0.5);
        final Vector<?> normal = points.interpolateAttribute(DataPointsType.ATT_NORMAL, 0, 1, 0.5);
        assertEquals(h, normal.get(0), GeometryTest.TOLERANCE);
        assertEquals(h, normal.get(1), GeometryTest.TOLERANCE);

        final Vector<?> other = points.interpolateAttribute("other", 0, 1, 0.5);
        assertEquals(0.5, other.get(0), GeometryTest.TOLERANCE);
        assertEquals(0.5, other.get(1), GeometryTest.TOLERANCE);
    }

    /**
     * A view gives the same values as an interpolated point, but follows the changes of the viewed
     * points and cannot be modified.
     */
    @Test
    public void testViewInterpolatedPoint() {
        final DataPoints points = createPoints(new double[] {0,0, 10,0, 0,10}, new double[] {10, 20, 30});
        final int[]    indices = {0, 1, 2};
        final double[] weights = {0.2, 0.3, 0.5};
        final Point copy = points.getInterpolatedPoint(indices, weights);
        final Point view = points.viewInterpolatedPoint(indices, weights);
        assertPoint(copy, 3, 5, 30, 23, 10);
        assertPoint(view, 3, 5, 30, 23, 10);
        assertEquals(null, view.getAttribute("unknown"), "An attribute absent from the viewed points has no value.");

        final Vector<?> moved = Vectors.create(new double[] {0, 20}, points.getPosition(2).getDataType());
        points.setPosition(2, moved);
        weights[0] = 1;                                 // Shall not change the view, which copied the weights.
        GeometryTest.assertPositionEquals(3, 10, view);
        GeometryTest.assertPositionEquals(3,  5, copy);

        assertThrows(UnsupportedOperationException.class, () -> view.setAttribute("linear", copy.getAttribute("linear")));
        assertThrows(UnsupportedOperationException.class, () -> view.setCoordinateReferenceSystem(GeometryTest.CRS_2D));
    }
}
