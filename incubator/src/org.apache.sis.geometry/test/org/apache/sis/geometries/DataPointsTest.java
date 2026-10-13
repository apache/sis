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
import org.apache.sis.maths.Array;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.Vector;

// Test dependencies
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link DataPoints}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class DataPointsTest {
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
    public void testGetDimension() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetAttributesType() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetEnvelope() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testIsEmpty() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testSize() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetPoint() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetPosition() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testSetPosition() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetAttribute() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testSetAttribute() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetAttributeArray() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetAttributeRange() {
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
                Map.of("linear",  AttributeInterpolation.LINEAR,
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
        assertPoint(points.interpolatePoint(0, 1, 0),     0, 0, 10, 10, 10);
        assertPoint(points.interpolatePoint(0, 1, 0.25), 2.5, 0, 10, 15, 10);
        assertPoint(points.interpolatePoint(0, 1, 0.5),    5, 0, 10, 20, 10);
        assertPoint(points.interpolatePoint(0, 1, 0.75), 7.5, 0, 30, 25, 10);
        assertPoint(points.interpolatePoint(0, 1, 1),     10, 0, 30, 30, 30);
    }

    /**
     * Interpolation in the triangle (0 0) (10 0) (0 10), holding the values 10, 20 and 30, at the
     * barycentric coordinates (0.2, 0.3, 0.5). The leading value applies to the whole triangle.
     */
    @Test
    public void testInterpolateTriangle() {
        final DataPoints points = createPoints(new double[] {0,0, 10,0, 0,10}, new double[] {10, 20, 30});
        assertPoint(points.interpolatePoint(new int[] {0, 1, 2}, new double[] {0.2, 0.3, 0.5}), 3, 5, 30, 23, 10);
        assertPoint(points.interpolatePoint(new int[] {0, 1, 2}, new double[] {0, 1, 0}),      10, 0, 20, 20, 10);
        assertPoint(points.interpolatePoint(new int[] {2, 0, 1}, new double[] {0.5, 0.2, 0.3}), 3, 5, 30, 23, 30);
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
        assertPoint(points.interpolatePoint(new int[] {0, 1, 2, 3}, weights), 2.5, 7.5, 40, 31.25, 10);
    }

    /**
     * Interpolation with a single point gives the values of that point.
     */
    @Test
    public void testInterpolateSinglePoint() {
        final DataPoints points = createPoints(new double[] {0,0, 10,0}, new double[] {10, 20});
        assertPoint(points.interpolatePoint(new int[] {1}, new double[] {1}), 10, 0, 20, 20, 20);
    }

    /**
     * At least one point, with one weight each, is required.
     */
    @Test
    public void testInterpolateInvalid() {
        final DataPoints points = createPoints(new double[] {0,0, 10,0, 0,10, 10,10}, new double[] {10, 20, 30, 40});
        assertThrows(IllegalArgumentException.class, () -> points.interpolatePoint(new int[0], new double[0]));
        assertThrows(IllegalArgumentException.class, () -> points.interpolatePoint(new int[] {0, 1, 2}, new double[] {0.5, 0.5}));
        assertThrows(IllegalArgumentException.class, () -> points.interpolateAttribute("linear", new int[0], new double[0]));
        assertThrows(IllegalArgumentException.class, () -> points.interpolateAttribute("linear", new int[] {0, 1}, new double[] {1}));
    }
}
