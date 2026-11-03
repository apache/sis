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
package org.apache.sis.geometries.operation;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.GeometryType;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.curve.LinearRing;
import org.apache.sis.geometries.internal.shared.ArrayDataPoints;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;


/**
 * Tests the {@code offsetCurve} operation of {@link GeometryProcessor}.
 * The offset algorithm itself is tested by {@code DefaultOffsetLineStringTest}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class OffsetCurveTest {

    private static final double TOLERANCE = 1E-9;

    private static final CoordinateReferenceSystem CRS_2D = CommonCRS.WGS84.normalizedGeographic();

    /**
     * Create a line string from coordinates ordered as x0, y0, ... xn, yn.
     */
    private static LineString line(double ... coords) {
        return GeometryFactory.DEFAULT.createLineString(new ArrayDataPoints(NDArrays.of(CRS_2D, coords)));
    }

    private static Geometry offset(Geometry geom, double distance) {
        return new GeometryProcessor().offsetCurve(geom, distance);
    }

    private static double[] positions(Geometry geom) {
        return geom.getDataPoints().getAttributeArray(DataPointsType.ATT_POSITION).toArrayDouble();
    }

    /**
     * Tests that a line string gives a line string, a negative distance being the right side.
     */
    @Test
    public void testLineString() {
        final Geometry result = offset(line(2, 1, 2, 4), -3);
        assertInstanceOf(LineString.class, result);
        assertEquals(CRS_2D, result.getCoordinateReferenceSystem());
        assertArrayEquals(new double[]{5, 1,  5, 4}, positions(result), TOLERANCE);
    }

    /**
     * Tests that a zero distance returns the geometry unchanged.
     */
    @Test
    public void testZeroDistance() {
        final LineString line = line(2, 1, 2, 4);
        assertSame(line, offset(line, 0));
    }

    /**
     * Tests that a linear ring stays a linear ring.
     */
    @Test
    public void testLinearRing() {
        final LinearRing ring = GeometryFactory.DEFAULT.createLinearRing(new ArrayDataPoints(NDArrays.of(CRS_2D,
                -2.0, -2.0, -2.0, 3.0, 5.0, 3.0, 5.0, -2.0, -2.0, -2.0)));
        final Geometry result = offset(ring, -1.5);
        assertInstanceOf(LinearRing.class, result);
        assertArrayEquals(new double[]{-0.5, -0.5,  -0.5, 1.5,  3.5, 1.5,  3.5, -0.5,  -0.5, -0.5},
                positions(result), TOLERANCE);
    }

    /**
     * Tests that each line string of a collection is offset.
     */
    @Test
    public void testCollection() {
        final GeometryCollection<LineString> lines = GeometryFactory.DEFAULT.createMultiLineString(CRS_2D,
                line(2, 1, 2, 4),
                line(2, 1, 4, 1));
        final Geometry result = offset(lines, -3);
        final GeometryCollection<?> col = assertInstanceOf(GeometryCollection.class, result);
        assertEquals(GeometryType.LINESTRING, col.getElementType());
        assertEquals(CRS_2D, col.getCoordinateReferenceSystem());
        assertEquals(2, col.getNumGeometries());
        assertInstanceOf(LineString.class, col.getGeometryN(0));
        assertArrayEquals(new double[]{5, 1,  5, 4},  positions(col.getGeometryN(0)), TOLERANCE);
        assertArrayEquals(new double[]{2, -2, 4, -2}, positions(col.getGeometryN(1)), TOLERANCE);
    }

    /**
     * Tests that geometries other than line strings are rejected.
     */
    @Test
    public void testUnsupported() {
        assertThrows(UnsupportedOperationException.class,
                () -> offset(GeometryFactory.DEFAULT.createPoint(CRS_2D, 1.0, 2.0), 1));
        final GeometryCollection<?> mixed = GeometryFactory.DEFAULT.createGeometryCollection(CRS_2D,
                GeometryFactory.DEFAULT.createPoint(CRS_2D, 1.0, 2.0));
        assertThrows(OperationException.class, () -> offset(mixed, 1));
    }
}
