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

import javax.measure.Quantity;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Test dependencies
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;


/**
 * Tests the {@code withinDistance} operations of {@link GeometryProcessor}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class WithinDistanceTest extends AbstractD9IMTest {
    /**
     * An arbitrary radius, smaller than the distance between the two points of the test cases.
     */
    private static final Quantity<?> RADIUS = Quantities.create(1, Units.DEGREE);

    /**
     * The inputs and expected result of a single test of {@code withinDistance(Geometry, Geometry, Length)}.
     *
     * @param input    the geometry on which the operation is invoked.
     * @param other    the other operand.
     * @param distance the maximal distance.
     * @param expected the expected result, or {@code null} if an exception is expected.
     * @param error    the type of the expected exception, or {@code null} if the operation should succeed.
     */
    private record TestCase(Geometry input,
                            Geometry other,
                            Quantity<?> distance,
                            Boolean expected,
                            Class<? extends Exception> error)
    {
    }

    /**
     * All test cases of {@code withinDistance(Geometry, Geometry, Quantity)}.
     */
    private static final TestCase[] ENTRIES = {
        new TestCase(EMPTY_1, POINT_A,     RADIUS, false, null),
        new TestCase(POINT_A, EMPTY_1,     RADIUS, false, null),
        new TestCase(POINT_A, POINT_A_BIS, RADIUS, true,  null),
        new TestCase(POINT_A, POINT_B,     RADIUS, false, null)
    };

    /**
     * Tests {@code withinDistance(Geometry, Geometry, Length)} on all declared test cases.
     */
    @Test
    public void testWithinDistance() {
        for (final TestCase entry : ENTRIES) {
            try {
                final boolean result = new GeometryProcessor().withinDistance(entry.input(), entry.other(), entry.distance());
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
     * Tests {@code withinDistance(Geometry, Geometry, Quantity)} between geometries which are
     * not positions. The answer is {@code true} as soon as the shortest distance between the
     * two geometries does not exceed the given one, the limit case included.
     */
    @Test
    public void testWithinDistanceBetweenShapes() {
        final GeometryProcessor processor = new GeometryProcessor();
        assertTrue (processor.withinDistance(SQUARE, LINE_FAR, degrees(11)), "The distance is 10.");
        assertTrue (processor.withinDistance(SQUARE, LINE_FAR, degrees(10)), "The distance is exactly the limit.");
        assertFalse(processor.withinDistance(SQUARE, LINE_FAR, degrees(9)), "The distance is 10.");
        /*
         * The two squares are 10√2 apart, which is between 14 and 15.
         */
        assertTrue (processor.withinDistance(SQUARE, SQUARE_DISJOINT, degrees(15)), "The distance is 10√2.");
        assertFalse(processor.withinDistance(SQUARE, SQUARE_DISJOINT, degrees(14)), "The distance is 10√2.");
        /*
         * Geometries which meet are within any distance, including a distance of zero.
         */
        assertTrue(processor.withinDistance(SQUARE, SQUARE_TOUCHING, degrees(0)), "Two squares sharing an edge are at a distance of zero.");
        assertTrue(processor.withinDistance(SQUARE, POINT_CENTER, degrees(0)), "A position inside a square is at a distance of zero from it.");
    }

    /**
     * Returns the given amount of degrees, the unit of the axes of the reference system
     * used by the geometries of these tests.
     */
    private static Quantity<?> degrees(final double value) {
        return Quantities.create(value, Units.DEGREE);
    }

}
