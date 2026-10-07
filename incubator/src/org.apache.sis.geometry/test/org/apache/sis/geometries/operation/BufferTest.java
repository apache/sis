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
import org.apache.sis.geometries.Surface;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;
import static org.apache.sis.geometries.operation.TestData.EMPTY_1;
import static org.apache.sis.geometries.operation.TestData.POINT_A;

// Test dependencies
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;


/**
 * Tests the {@code buffer} operations of {@link GeometryProcessor}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class BufferTest {
    /**
     * The inputs and expected result of a single test of {@code buffer(Geometry, Quantity)}.
     *
     * @param input    the geometry on which the operation is invoked.
     * @param radius   the buffer radius.
     * @param expected the expected result, or {@code null} if an exception is expected.
     * @param error    the type of the expected exception, or {@code null} if the operation should succeed.
     */
    private record TestCase(Geometry input,
                               Quantity<?> radius,
                               Geometry expected,
                               Class<? extends Exception> error)
    {
    }

    /**
     * All test cases of {@code buffer(Geometry, Quantity)}.
     */
    private static final TestCase[] RADIUS_ENTRIES = {
        /*
         * There is no position to grow a buffer around, therefore the buffer of an empty geometry
         * is the empty geometry. This holds for a negative radius, which shrinks a geometry, and
         * for a dimensionless radius, which is interpreted in the units of the coordinate system.
         */
        new TestCase(EMPTY_1, Quantities.create( 10, Units.METRE), EMPTY_1, null),
        new TestCase(EMPTY_1, Quantities.create(  0, Units.METRE), EMPTY_1, null),
        new TestCase(EMPTY_1, Quantities.create(-10, Units.METRE), EMPTY_1, null),
        new TestCase(EMPTY_1, Quantities.create( 10, Units.UNITY), EMPTY_1, null)
    };

    /**
     * Tests {@code buffer(Geometry, Length)} on all declared test cases.
     */
    @Test
    public void testBufferByRadius() {
        for (final TestCase entry : RADIUS_ENTRIES) {
            try {
                final Geometry result = new GeometryProcessor().buffer(entry.input(), entry.radius());
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
     * Tests that growing a buffer around a point gives a surface approximating a disk.
     * The operation is delegated to JTS, which approximates the disk by a polygon inscribed
     * in it. The area of that polygon is therefore slightly less than the area of the disk,
     * by an amount which depends on the number of segments used by JTS for each quadrant.
     * This test verifies only that the result is close to the disk, not the exact number
     * of segments.
     */
    @Test
    public void testBufferOfPoint() {
        final double radius = 10;
        final Geometry result = new GeometryProcessor().buffer(POINT_A, Quantities.create(radius, Units.METRE));
        assertFalse(result.isEmpty(), "The buffer of a point with a positive radius is not empty.");

        final Surface surface = assertInstanceOf(Surface.class, result, "The buffer of a point is a surface.");
        final double area = surface.getArea().getValue().doubleValue();
        final double disk = Math.PI * radius * radius;
        assertTrue(area <= disk, () -> "Inscribed polygon area " + area + " cannot exceed disk area " + disk);
        assertTrue(area >= 0.95 * disk, () -> "Area " + area + " is too far from the disk area " + disk);
    }

    /**
     * Tests that a buffer of radius zero around a point gives an empty geometry:
     * a point has no surface to preserve.
     */
    @Test
    public void testBufferOfPointWithZeroRadius() {
        final Geometry result = new GeometryProcessor().buffer(POINT_A, Quantities.create(0, Units.METRE));
        assertTrue(result.isEmpty(), "The buffer of a point with a radius of zero is empty.");
    }
}
