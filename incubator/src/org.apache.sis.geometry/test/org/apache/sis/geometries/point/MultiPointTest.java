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
package org.apache.sis.geometries.point;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.Point;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.apache.sis.geometries.GeometryTest;


/**
 * Tests {@link MultiPoint}.
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class MultiPointTest extends GeometryTest {
    /**
     * Two distinct positions, the middle of which is (3.5 3.5).
     */
    private static final double[] TWO_POSITIONS = {2,2, 5,5};

    /**
     * The same position twice, which makes the set not simple.
     */
    private static final double[] REPEATED_POSITION = {5,5, 5,5};

    protected MultiPointTest() {
    }

    /**
     * Creates a set of positions in the given coordinate reference system.
     *
     * @param  crs          the coordinate reference system of the set to create, not null.
     * @param  coordinates  the coordinates of the positions, in the axis order of the given system.
     * @return a new set holding the given positions, never null.
     */
    protected abstract MultiPoint<?> createMultiPoint(CoordinateReferenceSystem crs, double... coordinates);

    @Test
    @Disabled("Not implemented yet.")
    public void testGetGeometryType() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetElementType() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testAsDataPoints() {
    }

    /**
     * All the positions of the set have the same weight, so the centroid of two positions is their
     * middle.
     */
    @Test
    public void testGetCentroid() {
        final double[] centroid = createMultiPoint(CRS_2D, TWO_POSITIONS).getCentroid()
                                                                         .getPosition().toArrayDouble();
        assertEquals(3.5, centroid[0], TOLERANCE);
        assertEquals(3.5, centroid[1], TOLERANCE);
    }

    /**
     * Contrarily to the centroid, the returned position shall be one of the positions of the set,
     * but which one is left to the implementation.
     */
    @Test
    public void testGetRepresentativePoint() {
        final Point representative = createMultiPoint(CRS_2D, TWO_POSITIONS).getRepresentativePoint();
        final double[] position = representative.getPosition().toArrayDouble();
        final boolean isFirst  = Math.abs(position[0] - 2) < TOLERANCE && Math.abs(position[1] - 2) < TOLERANCE;
        final boolean isSecond = Math.abs(position[0] - 5) < TOLERANCE && Math.abs(position[1] - 5) < TOLERANCE;
        assertTrue(isFirst || isSecond, "The representative position shall be one of the positions of the set.");
    }

    /**
     * A position has no boundary, so a finite set of positions has none either.
     */
    @Test
    public void testGetBoundary() {
        assertTrue(createMultiPoint(CRS_2D, TWO_POSITIONS).getBoundary().isEmpty(),
                   "The boundary of a finite set of positions is empty.");
    }

    /**
     * A repeated position is an anomalous position.
     */
    @Test
    public void testIsSimple() {
        assertTrue (createMultiPoint(CRS_2D, TWO_POSITIONS).isSimple(),     "Two distinct positions are simple.");
        assertFalse(createMultiPoint(CRS_2D, REPEATED_POSITION).isSimple(), "A repeated position is not simple.");
    }

    /**
     * Contrarily to simplicity, a repeated position stays valid.
     */
    @Test
    public void testIsValid() {
        assertTrue(createMultiPoint(CRS_2D, TWO_POSITIONS).isValid(),     "Two distinct positions are valid.");
        assertTrue(createMultiPoint(CRS_2D, REPEATED_POSITION).isValid(), "A repeated position stays valid.");
    }

}
