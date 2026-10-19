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
package org.apache.sis.maths;

import org.opengis.coordinate.MismatchedDimensionException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link NDRange}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class NDRangeTest {

    private static final CoordinateReferenceSystem CRS = CommonCRS.WGS84.geographic3D();

    private static Vector<?> vector(double... values) {
        return Vectors.create(values.length, DataType.DOUBLE).set(values);
    }

    private static NDRange range() {
        return new NDRange(CRS, vector(1, 2, 3), vector(4, 6, 3));
    }

    @Test
    public void testEnvelope() {
        final NDRange range = range();
        assertEquals(CRS, range.getCoordinateReferenceSystem());
        assertEquals(3, range.getDimension());
        assertEquals(1, range.getMinimum(0));
        assertEquals(6, range.getMaximum(1));
        assertEquals(2.5, range.getMedian(0));
        assertEquals(4, range.getSpan(1));
        assertEquals(0, range.getSpan(2));
        assertThrows(IndexOutOfBoundsException.class, () -> range.getMinimum(3));
    }

    @Test
    public void testCorners() {
        final NDRange range = range();
        final DirectPosition lower = range.getLowerCorner();
        final DirectPosition upper = range.getUpperCorner();
        assertArrayEquals(new double[] {1, 2, 3}, lower.getCoordinates());
        assertArrayEquals(new double[] {4, 6, 3}, upper.getCoordinates());
        assertEquals(CRS, lower.getCoordinateReferenceSystem());
        assertArrayEquals(new double[] {1, 2, 3}, range.getLower().toArrayDouble());
        assertArrayEquals(new double[] {4, 6, 3}, range.getUpper().toArrayDouble());
    }

    /**
     * The range keeps its own copy of the bounds, and gives copies of them.
     */
    @Test
    public void testCopies() {
        final Vector<?> lower = vector(1, 2, 3);
        final NDRange range = new NDRange(lower, vector(4, 6, 3));
        lower.set(0, -100);
        range.getLower().set(0, -100);
        range.getLowerCorner().setCoordinate(0, -100);
        assertEquals(1, range.getMinimum(0));
    }

    /**
     * The bounds keep their data type.
     */
    @Test
    public void testDataType() {
        final Vector<?> lower = Vectors.createInt(2).set(new double[] {1, 2});
        final Vector<?> upper = Vectors.createInt(2).set(new double[] {5, 7});
        final NDRange range = new NDRange(lower, upper);
        assertEquals(DataType.INT, range.getLower().getDataType());
        assertEquals(5, range.getMaximum(0));
    }

    @Test
    public void testFromEnvelope() {
        final GeneralEnvelope envelope = new GeneralEnvelope(CRS);
        envelope.setEnvelope(1, 2, 3, 4, 6, 3);
        final NDRange range = new NDRange(envelope);
        assertEquals(range(), range);
        assertEquals(range().hashCode(), range.hashCode());
        assertEquals(CRS, range.getCoordinateReferenceSystem());
    }

    @Test
    public void testInvalid() {
        assertThrows(MismatchedDimensionException.class, () -> new NDRange(vector(1, 2, 3), Vectors.createDouble(2)));
        assertThrows(IllegalArgumentException.class, () -> new NDRange(vector(5, 2, 3), vector(4, 6, 3)));
        assertThrows(MismatchedDimensionException.class, () -> new NDRange(CommonCRS.WGS84.geographic(),
                vector(1, 2, 3), vector(4, 6, 3)));
    }

    /**
     * NaN bounds make an empty range.
     */
    @Test
    public void testEmpty() {
        final NDRange range = new NDRange(vector(Double.NaN, 2, 3), vector(Double.NaN, 6, 3));
        assertTrue(Double.isNaN(range.getSpan(0)));
    }

    @Test
    public void testAdd() {
        final NDRange range = range();
        range.add(vector(0, 3, 10));
        assertArrayEquals(new double[] {0, 2, 3}, range.getLower().toArrayDouble());
        assertArrayEquals(new double[] {4, 6, 10}, range.getUpper().toArrayDouble());
        // A position inside the range does not change it.
        range.add(vector(2, 3, 5));
        assertEquals(new NDRange(CRS, vector(0, 2, 3), vector(4, 6, 10)), range);
        // A NaN coordinate is ignored.
        range.add(vector(Double.NaN, 8, 5));
        assertArrayEquals(new double[] {4, 8, 10}, range.getUpper().toArrayDouble());
        // The same with a direct position.
        range.add(Vectors.asDirectPostion(vector(-1, 2, 3)));
        assertEquals(-1, range.getMinimum(0));
        assertThrows(MismatchedDimensionException.class, () -> range.add(Vectors.createDouble(2)));
    }

    /**
     * A NaN bound makes the range empty in its dimension, and adding a position does not change that,
     * as in {@link GeneralEnvelope}.
     */
    @Test
    public void testAddToEmpty() {
        final NDRange range = new NDRange(vector(Double.NaN, 2, 3), vector(Double.NaN, 6, 3));
        range.add(vector(1, 1, 3));
        assertTrue(Double.isNaN(range.getMinimum(0)));
        assertEquals(1, range.getMinimum(1));
    }

    /**
     * Bounds of integer or float types are widened, so that the added position is contained.
     */
    @Test
    public void testAddRounding() {
        final NDRange integers = new NDRange(Vectors.createInt(1).set(new double[] {1}), Vectors.createInt(1).set(new double[] {5}));
        integers.add(Vectors.createDouble(1).set(new double[] {7.5}));
        integers.add(Vectors.createDouble(1).set(new double[] {0.5}));
        assertEquals(0, integers.getMinimum(0));
        assertEquals(8, integers.getMaximum(0));
        assertTrue(integers.contains(Vectors.createDouble(1).set(new double[] {7.5})));

        final NDRange floats = new NDRange(Vectors.createFloat(1).set(new double[] {1}), Vectors.createFloat(1).set(new double[] {2}));
        floats.add(Vectors.createDouble(1).set(new double[] {0.1}));
        floats.add(Vectors.createDouble(1).set(new double[] {2.3}));
        assertTrue(floats.getMinimum(0) <= 0.1);
        assertTrue(floats.getMaximum(0) >= 2.3);
        assertTrue(floats.contains(Vectors.createDouble(1).set(new double[] {0.1})));
    }

    @Test
    public void testContains() {
        final NDRange range = range();
        assertTrue (range.contains(vector(2, 3, 3)));
        assertTrue (range.contains(vector(1, 6, 3)), "The border is contained.");
        assertFalse(range.contains(vector(0, 3, 3)));
        assertFalse(range.contains(vector(2, 3, 4)));
        assertFalse(range.contains(vector(Double.NaN, 3, 3)));
        assertTrue (range.contains(Vectors.asDirectPostion(vector(4, 2, 3))));
        assertThrows(MismatchedDimensionException.class, () -> range.contains(Vectors.createDouble(2)));
    }

    /**
     * Ranges intersect if they overlap, touch, or are separated by no more than the tolerance in every dimension.
     */
    @Test
    public void testIntersects() {
        final NDRange range = range();
        final NDRange overlapping = new NDRange(vector(3, 5, 3), vector(7, 8, 3));
        assertTrue(range.intersects(overlapping, 0));
        assertTrue(overlapping.intersects(range, 0));
        assertTrue(range.intersects(range(), 0), "A range intersects itself.");
        assertTrue(range.intersects(new NDRange(vector(2, 3, 3), vector(3, 4, 3)), 0), "A contained range intersects.");
        assertTrue(range.intersects(new NDRange(vector(4, 6, 3), vector(5, 7, 3)), 0), "Touching ranges intersect.");

        // Separated by 0.5 along the first axis only.
        final NDRange near = new NDRange(vector(4.5, 2, 3), vector(5, 6, 3));
        assertFalse(range.intersects(near, 0));
        assertFalse(range.intersects(near, 0.25));
        assertTrue (range.intersects(near, 0.5));
        assertTrue (near.intersects(range, 0.5));

        // Overlapping along two axes is not enough.
        assertFalse(range.intersects(new NDRange(vector(1, 2, 4), vector(4, 6, 5)), 0));

        // Any envelope implementation is accepted.
        final GeneralEnvelope envelope = new GeneralEnvelope(CRS);
        envelope.setEnvelope(0, 0, 0, 1, 2, 3);
        assertTrue (range.intersects(envelope, 0));
        envelope.setEnvelope(0, 0, 0, 0.9, 2, 3);
        assertFalse(range.intersects(envelope, 0));

        // A NaN bound makes the range empty.
        final NDRange empty = new NDRange(vector(Double.NaN, 2, 3), vector(Double.NaN, 6, 3));
        assertFalse(range.intersects(empty, 1));
        assertFalse(empty.intersects(range, 1));

        assertThrows(MismatchedDimensionException.class, () -> range.intersects(new NDRange(2), 0));
        assertThrows(IllegalArgumentException.class, () -> range.intersects(overlapping, -1));
        assertThrows(IllegalArgumentException.class, () -> range.intersects(overlapping, Double.NaN));
    }

    /**
     * The constructors without bounds create a range of the given data type, {@code DOUBLE} by default,
     * with all bounds at zero.
     */
    @Test
    public void testConstructors() {
        NDRange range = new NDRange(2);
        assertEquals(2, range.getDimension());
        assertEquals(DataType.DOUBLE, range.getLower().getDataType());
        assertArrayEquals(new double[] {0, 0}, range.getUpper().toArrayDouble());

        range = new NDRange(4, DataType.INT);
        assertEquals(4, range.getDimension());
        assertEquals(DataType.INT, range.getUpper().getDataType());

        range = new NDRange(CRS);
        assertEquals(CRS, range.getCoordinateReferenceSystem());
        assertEquals(3, range.getDimension());
        assertEquals(DataType.DOUBLE, range.getLower().getDataType());

        range = new NDRange(CRS, DataType.FLOAT);
        assertEquals(CRS, range.getCoordinateReferenceSystem());
        assertEquals(DataType.FLOAT, range.getLower().getDataType());

        range = new NDRange(SampleSystem.of(CRS));
        assertEquals(CRS, range.getCoordinateReferenceSystem());
        assertEquals(DataType.DOUBLE, range.getLower().getDataType());

        range = new NDRange(SampleSystem.of(CRS), DataType.SHORT);
        assertEquals(DataType.SHORT, range.getLower().getDataType());
        assertEquals(new NDRange(SampleSystem.of(CRS), DataType.SHORT), range);

        assertThrows(IllegalArgumentException.class, () -> new NDRange(0));
    }

    /**
     * Setting a range keeps the data type of the bounds, rounding outward the values it cannot represent.
     */
    @Test
    public void testSetRange() {
        final NDRange integers = new NDRange(3, DataType.INT);
        integers.setRange(0, 1, 5);
        integers.setRange(1, 1.5, 4.5);
        integers.setRange(2, 3L, 9L);
        assertEquals(DataType.INT, integers.getLower().getDataType());
        assertArrayEquals(new double[] {1, 1, 3}, integers.getLower().toArrayDouble());
        assertArrayEquals(new double[] {5, 5, 9}, integers.getUpper().toArrayDouble());

        final NDRange floats = new NDRange(2, DataType.FLOAT);
        floats.setRange(0, 0.1, 2.3);
        floats.setRange(1, 0.5f, 1.5f);
        assertEquals(DataType.FLOAT, floats.getLower().getDataType());
        assertTrue(floats.getMinimum(0) <= 0.1 && floats.getMaximum(0) >= 2.3);
        assertEquals(0.5, floats.getMinimum(1));
        assertEquals(1.5, floats.getMaximum(1));

        final NDRange doubles = new NDRange(1);
        doubles.setRange(0, 0.1, 2.3);
        assertEquals(0.1, doubles.getMinimum(0));
        assertEquals(2.3, doubles.getMaximum(0));

        assertThrows(IllegalArgumentException.class, () -> doubles.setRange(0, 3, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> doubles.setRange(1, 0, 1));
    }

    @Test
    public void testToString() {
        assertEquals("NDRange[(1.0, 2.0, 3.0) … (4.0, 6.0, 3.0)]", range().toString());
    }
}
