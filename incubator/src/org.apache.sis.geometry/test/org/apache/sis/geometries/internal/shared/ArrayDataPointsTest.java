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

import java.util.LinkedHashMap;
import java.util.Map;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.apache.sis.geometries.DataPointsTest;


/**
 * Tests {@link ArrayDataPoints}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class ArrayDataPointsTest extends DataPointsTest {
    /**
     * Tests the copies of points made from the arrays, which do not use the default methods.
     */
    @Test
    public void testSubsetOfArrays() {
        final DataPoints points = createArraySample();
        assertInstanceOf(ArrayDataPoints.class, points);
        assertSubsets(points);
        assertInstanceOf(ArrayDataPoints.class, points.subset(new int[] {1}));
        assertInstanceOf(ArrayDataPoints.class, points.subset(0, 1));
    }

    private static final CoordinateReferenceSystem CRS2D = CommonCRS.WGS84.normalizedGeographic();

    /**
     * Tests that attributes of empty data points can be set before the positions.
     */
    @Test
    public void testAttributeBeforePositions() {
        final ArrayDataPoints points = new ArrayDataPoints(NDArrays.of(CRS2D, new double[0]));
        points.setAttribute("TEST", NDArrays.of(1, new double[] {1, 2, 3}));
        points.setAttribute(DataPointsType.ATT_POSITION, NDArrays.of(CRS2D, new double[] {0,0, 1,1, 2,2}));
        assertEquals(3, points.size());
        assertArrayEquals(new double[] {1, 2, 3}, points.getAttribute("TEST").toArrayDouble());
    }

    /**
     * Tests that positions of a new length replace the positions of empty data points
     * and that the empty attributes are removed.
     */
    @Test
    public void testPositionsOfEmpty() {
        final Map<String,Array> attributes = new LinkedHashMap<>();
        attributes.put(DataPointsType.ATT_POSITION, NDArrays.of(CRS2D, new double[0]));
        attributes.put("TEST", NDArrays.of(1, new double[0]));
        final ArrayDataPoints points = new ArrayDataPoints(attributes);
        points.setAttribute(DataPointsType.ATT_POSITION, NDArrays.of(CRS2D, new double[] {0,0, 1,1}));
        assertEquals(2, points.size());
        assertNull(points.getAttribute("TEST"));
    }

    /**
     * Tests that attributes whose length does not match the positions are rejected.
     */
    @Test
    public void testLengthMismatch() {
        final ArrayDataPoints points = new ArrayDataPoints(NDArrays.of(CRS2D, new double[] {0,0, 1,1}));
        assertThrows(IllegalArgumentException.class, () -> points.setAttribute("TEST", NDArrays.of(1, new double[] {1, 2, 3})));

        //positions alone can be replaced by positions of another length
        points.setAttribute(DataPointsType.ATT_POSITION, NDArrays.of(CRS2D, new double[] {0,0, 1,1, 2,2}));
        assertEquals(3, points.size());

        //but not when another attribute has the old length
        points.setAttribute("TEST", NDArrays.of(1, new double[] {1, 2, 3}));
        assertThrows(IllegalArgumentException.class,
                () -> points.setAttribute(DataPointsType.ATT_POSITION, NDArrays.of(CRS2D, new double[] {0,0, 1,1})));

        final ArrayDataPoints empty = new ArrayDataPoints(NDArrays.of(CRS2D, new double[0]));
        empty.setAttribute("TEST", NDArrays.of(1, new double[] {1, 2, 3}));
        assertThrows(IllegalArgumentException.class,
                () -> empty.setAttribute(DataPointsType.ATT_POSITION, NDArrays.of(CRS2D, new double[] {0,0, 1,1})));
    }
}
