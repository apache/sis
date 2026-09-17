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
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.Point;
import org.apache.sis.maths.SampleSystem;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CommonCRS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class DistanceTest {

    private static final SampleSystem CRS2D = SampleSystem.of(CommonCRS.WGS84.geographic());

    /**
     * Test point to point distance.
     */
    @Test
    public void PointPoint() {

        { //different CRS
            final Point point1 = GeometryFactory.createPoint(CommonCRS.WGS84.geographic());
            final Point point2 = GeometryFactory.createPoint(CommonCRS.WGS84.normalizedGeographic());
            try {
                new GeometryProcessor().distance(point1, point2);
                fail("evaluation should fail");
            } catch (OperationException ex) {
                //ok
            }
        }

        { //at same position
            final Point point1 = GeometryFactory.createPoint(CRS2D, 10.0, 5.0);
            final Point point2 = GeometryFactory.createPoint(CRS2D, 10.0, 5.0);
            final Quantity<?> distance = new GeometryProcessor().distance(point1, point2);
            assertEquals(Units.METRE, distance.getUnit());
            assertEquals(0.0, distance.getValue().doubleValue(), 0.0);
        }

        { //at 1.0 of distance
            final Point point1 = GeometryFactory.createPoint(CRS2D, 10, 5);
            final Point point2 = GeometryFactory.createPoint(CRS2D, 10, 6);
            final Quantity<?> distance = new GeometryProcessor().distance(point1, point2);
            assertEquals(Units.METRE, distance.getUnit());
            assertEquals(1.0, distance.getValue().doubleValue(), 0.0);
        }
    }

    /**
     * The inputs and expected result of a single test of {@code distance(Geometry, Geometry)}.
     *
     * @param input    the geometry on which the operation is invoked.
     * @param other    the other operand.
     * @param expected the expected result, or {@code null} if an exception is expected.
     * @param error    the type of the expected exception, or {@code null} if the operation should succeed.
     */
    private record Entry(Geometry input,
                         Geometry other,
                         Quantity<?> expected,
                         Class<? extends Exception> error)
    {
    }

    /**
     * All test cases of {@code distance(Geometry, Geometry)}.
     */
    private static final Entry[] ENTRIES = {
    };

    /**
     * Tests {@code distance(Geometry, Geometry)} on all declared test cases.
     */
    @Test
    public void testDistance() {
        for (final Entry entry : ENTRIES) {
            try {
                final Quantity<?> result = new GeometryProcessor().distance(entry.input(), entry.other());
                assertNull(entry.error(), "An exception was expected.");
                assertEquals(entry.expected(), result);
            } catch (Exception ex) {
                if (entry.error() == null || !entry.error().isInstance(ex)) {
                    throw new AssertionError("Unexpected exception for " + entry, ex);
                }
            }
        }
    }
}
