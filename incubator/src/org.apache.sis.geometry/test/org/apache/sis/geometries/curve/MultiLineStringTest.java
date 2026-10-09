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
import org.apache.sis.measure.Units;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.apache.sis.geometries.GeometryTest;


/**
 * Tests {@link MultiLineString}.
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class MultiLineStringTest extends GeometryTest {
    /**
     * A horizontal segment of length 10 at the origin, and a parallel one 20 above it.
     * Both have the same length, so the centroid of the set is half way between theirs.
     */
    private static final double[] SEGMENT_1 = {0,0, 10,0};
    private static final double[] SEGMENT_2 = {0,20, 10,20};

    protected MultiLineStringTest() {
    }

    /**
     * Creates a set of curves in the given coordinate reference system.
     *
     * @param  crs    the coordinate reference system of the set to create, not null.
     * @param  lines  the coordinates of each curve, in the axis order of the given system.
     * @return a new set holding the given curves, never null.
     */
    protected abstract MultiLineString createMultiLineString(CoordinateReferenceSystem crs, double[]... lines);

    @Test
    @Disabled("Not implemented yet.")
    public void testGetGeometryType() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetElementType() {
    }

    /**
     * The length of a set of curves is the sum of the lengths of its elements, measured in the
     * units of the coordinate system axes.
     */
    @Test
    public void testGetLength() {
        final Quantity<?> length = createMultiLineString(CRS_2D, SEGMENT_1, SEGMENT_2).getLength();
        assertEquals(20, length.getValue().doubleValue(), TOLERANCE, "Two segments of 10 each.");
        assertEquals(Units.DEGREE, length.getUnit(), "The length shall use the unit of the axes.");
    }

    /**
     * The centroid of a set of curves is weighted by length, so two segments of the same length
     * give the middle of their two centroids.
     */
    @Test
    public void testGetCentroid() {
        final double[] centroid = createMultiLineString(CRS_2D, SEGMENT_1, SEGMENT_2).getCentroid()
                                                                                     .getPosition().toArrayDouble();
        assertEquals( 5, centroid[0], TOLERANCE);
        assertEquals(10, centroid[1], TOLERANCE);
    }

    /**
     * The boundary of a set of open curves is the set of their end positions, so two disjoint
     * segments are bounded by four positions.
     */
    @Test
    public void testGetBoundary() {
        final Geometry boundary = createMultiLineString(CRS_2D, SEGMENT_1, SEGMENT_2).getBoundary();
        assertFalse(boundary.isEmpty(), "Two open curves have end positions.");
        assertEquals(0, boundary.getTopologicDimension(), "The boundary of a curve is made of positions.");
    }

    /**
     * Two curves which neither cross themselves nor each other are both simple and valid.
     */
    @Test
    public void testIsSimpleAndValid() {
        final MultiLineString lines = createMultiLineString(CRS_2D, SEGMENT_1, SEGMENT_2);
        assertTrue(lines.isSimple(), "Two disjoint segments are simple.");
        assertTrue(lines.isValid(),  "Two disjoint segments are valid.");
    }

}
