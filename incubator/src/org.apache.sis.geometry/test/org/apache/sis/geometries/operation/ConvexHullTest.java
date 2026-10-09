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

import org.apache.sis.geometries.Geometry;

// Test dependencies
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;


/**
 * Tests the {@code convexHull} operations of {@link GeometryProcessor}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class ConvexHullTest extends AbstractD9IMTest {
    /**
     * The inputs and expected result of a single test of {@code convexHull(Geometry)}.
     *
     * @param input    the geometry on which the operation is invoked.
     * @param expected the expected result, or {@code null} if an exception is expected.
     * @param error    the type of the expected exception, or {@code null} if the operation should succeed.
     */
    private record TestCase(Geometry input,
                            Geometry expected,
                            Class<? extends Exception> error)
    {
    }

    /**
     * All test cases of {@code convexHull(Geometry)}.
     */
    private static final TestCase[] ENTRIES = {
        // The convex hull of the empty set is empty.
        new TestCase(EMPTY_1, EMPTY_1, null),
        // points
        new TestCase(POINT_A, POINT_A, null)
    };

    /**
     * Tests {@code convexHull(Geometry)} on all declared test cases.
     */
    @Test
    public void testConvexHull() {
        for (final TestCase entry : ENTRIES) {
            try {
                final Geometry result = new GeometryProcessor().convexHull(entry.input());
                assertNull(entry.error(), "An exception was expected.");
                assertEquals(entry.expected(), result);
            } catch (Exception ex) {
                if (entry.error() == null || !entry.error().isInstance(ex)) {
                    throw new AssertionError("Unexpected exception for " + entry, ex);
                }
            }
        }
    }

    /**
     * Tests {@code convexHull(Geometry)} on the geometries which are not already convex.
     * The hull of a convex surface is that surface, and the hull of a curve crossing itself
     * is the smallest convex surface enclosing all of its positions.
     */
    @Test
    public void testConvexHullOfShapes() {
        final GeometryProcessor processor = new GeometryProcessor();
        assertEquals(100, areaOf(processor.convexHull(SQUARE)), TOLERANCE, "A square is already convex.");
        assertEquals(100, areaOf(processor.convexHull(SQUARE_WITH_HOLE)), TOLERANCE, "The hull of a square with a hole fills that hole.");
        /*
         * The four positions of that curve are the four corners of a square of 10 × 10,
         * whatever the order in which the curve visits them.
         */
        assertEquals(100, areaOf(processor.convexHull(LINE_SELF_CROSSING)), TOLERANCE, "The hull of a curve visiting the four corners of a square is that square.");
        /*
         * The hull of the two squares [0…10]² and [20…30]² is the hexagon
         * (0 0), (10 0), (30 20), (30 30), (20 30), (0 10), of area 500.
         */
        assertEquals(500, areaOf(processor.convexHull(MULTI_POLYGON)), TOLERANCE, "The hull of two squares placed along a diagonal is a hexagon.");
    }

}
