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
import org.apache.sis.measure.Units;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.apache.sis.geometries.GeometryTest;


/**
 * Tests {@link LinearRing}.
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class LinearRingTest extends GeometryTest {
    /**
     * The ring of a square of 10 × 10, therefore of length 40.
     * Its first position is repeated at the end, a ring being closed.
     */
    private static final double[] SQUARE = {0,0, 10,0, 10,10, 0,10, 0,0};

    protected LinearRingTest() {
    }

    /**
     * Creates a ring through the given positions in the given coordinate reference system.
     * The caller is responsible for repeating the first position at the end.
     *
     * @param  crs          the coordinate reference system of the ring to create, not null.
     * @param  coordinates  the coordinates of the positions, in the axis order of the given system.
     * @return a new ring through the given positions, never null.
     */
    protected abstract LinearRing createLinearRing(CoordinateReferenceSystem crs, double... coordinates);

    @Test
    @Disabled("Not implemented yet.")
    public void testGetGeometryType() {
    }

    /**
     * A ring is closed by definition, whatever the implementation of the operation on the curves it
     * specializes.
     */
    @Test
    public void testIsClosed() {
        assertTrue(createLinearRing(CRS_2D, SQUARE).isClosed(), "A linear ring is closed by definition.");
    }

    /**
     * A ring is simple by definition, whatever the implementation of the operation on the curves it
     * specializes.
     */
    @Test
    public void testIsSimple() {
        assertTrue(createLinearRing(CRS_2D, SQUARE).isSimple(), "A linear ring is simple by definition.");
    }

    /**
     * A ring is both closed and simple, and is therefore a ring.
     */
    @Test
    public void testIsRing() {
        assertTrue(createLinearRing(CRS_2D, SQUARE).isRing(), "A linear ring is a ring by definition.");
    }

    /**
     * The length of a ring is the perimeter of the area it bounds, and is measured in the units of
     * the coordinate system axes.
     */
    @Test
    public void testGetLength() {
        final Quantity<?> length = createLinearRing(CRS_2D, SQUARE).getLength();
        assertEquals(40, length.getValue().doubleValue(), TOLERANCE, "The four edges of a square of 10 × 10.");
        assertEquals(Units.DEGREE, length.getUnit(), "The length shall use the unit of the axes.");
    }

    /**
     * A ring closes on itself, so it has no end position and therefore no boundary.
     */
    @Test
    public void testGetBoundary() {
        assertTrue(createLinearRing(CRS_2D, SQUARE).getBoundary().isEmpty(),
                   "A closed curve has no boundary.");
    }

    @Test
    public void testIsValid() {
        assertTrue(createLinearRing(CRS_2D, SQUARE).isValid(), "The ring of a square is valid.");
    }

}
