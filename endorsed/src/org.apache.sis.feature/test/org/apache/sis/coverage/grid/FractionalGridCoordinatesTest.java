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
package org.apache.sis.coverage.grid;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link FractionalGridCoordinates} implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class FractionalGridCoordinatesTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public FractionalGridCoordinatesTest() {
    }

    /**
     * Creates a test instance with (4 -1.1 7.6) coordinate values.
     */
    private static FractionalGridCoordinates instance() {
        return new FractionalGridCoordinates(new double[] {4, -1.1, 7.6});
    }

    /**
     * Tests {@link FractionalGridCoordinates#getCoordinateValue(int)}.
     */
    @Test
    public void testGetCoordinateValue() {
        final FractionalGridCoordinates gc = instance();
        assertEquals( 4, gc.getCoordinateValue(0));
        assertEquals(-1, gc.getCoordinateValue(1));
        assertEquals( 8, gc.getCoordinateValue(2));
    }

    /**
     * Tests {@link FractionalGridCoordinates#toExtent(GridExtent, long...)}
     * with default parameter values.
     */
    @Test
    @SuppressWarnings("removal")   // TODO: make GridExtentTest.assertExtentEquals private.
    public void testToExtent() {
        final GridExtent extent = instance().toExtent(null);
        GridExtentTest.assertExtentEquals(extent, 0,  4,  4);
        GridExtentTest.assertExtentEquals(extent, 1, -2, -1);
        GridExtentTest.assertExtentEquals(extent, 2,  7,  8);
    }

    /**
     * Tests {@link FractionalGridCoordinates#toExtent(GridExtent, long...)} with a size of 1.
     */
    @Test
    @SuppressWarnings("removal")
    public void testToExtentSize1() {
        final GridExtent extent = instance().toExtent(null, 1, 1, 1);
        GridExtentTest.assertExtentEquals(extent, 0,  4,  4);
        GridExtentTest.assertExtentEquals(extent, 1, -1, -1);
        GridExtentTest.assertExtentEquals(extent, 2,  8,  8);
    }

    /**
     * Tests {@link FractionalGridCoordinates#toExtent(GridExtent, long...)} with a size greater than 2.
     */
    @Test
    @SuppressWarnings("removal")
    public void testToExtentSizeN() {
        final GridExtent extent = instance().toExtent(null, 3, 5, 4);
        GridExtentTest.assertExtentEquals(extent, 0,  3,  5);
        GridExtentTest.assertExtentEquals(extent, 1, -3,  1);
        GridExtentTest.assertExtentEquals(extent, 2,  6,  9);
    }

    /**
     * Tests {@link FractionalGridCoordinates#toExtent(GridExtent, long...)} with a bounds constraint.
     */
    @Test
    @SuppressWarnings("removal")
    public void testToExtentBounded() {
        final GridExtent bounds = new GridExtent(null, new long[] {0, -1, 0}, new long[] {4, 2, 8}, true);
        final GridExtent extent = instance().toExtent(bounds, 3, 5, 4);
        GridExtentTest.assertExtentEquals(extent, 0,  2,  4);
        GridExtentTest.assertExtentEquals(extent, 1, -1,  2);
        GridExtentTest.assertExtentEquals(extent, 2,  5,  8);
    }
}
