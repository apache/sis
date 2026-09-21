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
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CommonCRS;
import static org.apache.sis.geometries.operation.TestData.EMPTY_1;
import static org.apache.sis.geometries.operation.TestData.EMPTY_2;
import static org.apache.sis.geometries.operation.TestData.NON_EMPTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class DistanceTest {

    private static final SampleSystem CRS2D = SampleSystem.of(CommonCRS.WGS84.geographic());

    /**
     * Two points at the same position, and a third one at a distance of 1 along the second axis.
     */
    private static final Point POINT_10_5     = GeometryFactory.createPoint(CRS2D, 10.0, 5.0);
    private static final Point POINT_10_5_BIS = GeometryFactory.createPoint(CRS2D, 10.0, 5.0);
    private static final Point POINT_10_6     = GeometryFactory.createPoint(CRS2D, 10.0, 6.0);

    /**
     * Two points using coordinate reference systems which differ by their axis order.
     */
    private static final Point POINT_GEOGRAPHIC = GeometryFactory.createPoint(CommonCRS.WGS84.geographic());
    private static final Point POINT_NORMALIZED = GeometryFactory.createPoint(CommonCRS.WGS84.normalizedGeographic());

    /**
     * The distance from an empty geometry to any geometry, including itself, is infinite.
     * The quantity is dimensionless, the empty set having no measurement unit to report.
     */
    private static final Quantity<?> INFINITY = Quantities.create(Double.POSITIVE_INFINITY, Units.UNITY);

    /**
     * The inputs and expected result of a single test of {@code distance(Geometry, Geometry)}.
     *
     * @param input    the geometry on which the operation is invoked.
     * @param other    the other operand.
     * @param expected the expected result, or {@code null} if an exception is expected.
     * @param error    the type of the expected exception, or {@code null} if the operation should succeed.
     */
    private record TestCase(Geometry input,
                         Geometry other,
                         Quantity<?> expected,
                         Class<? extends Exception> error)
    {
    }

    /**
     * All test cases of {@code distance(Geometry, Geometry)}.
     */
    private static final TestCase[] ENTRIES = {
        /*
         * Point to point. The distance is zero when the two points are at the same position, and is
         * otherwise the Pythagorean distance computed in the units of the coordinate system axes.
         * The unit reported is metre in both cases; see the limitation documented on
         * `GeometryProcessor.distance(Geometry, Geometry)`.
         */
        new TestCase(POINT_10_5, POINT_10_5_BIS, Quantities.create(0.0, Units.DEGREE), null),
        new TestCase(POINT_10_5, POINT_10_6,     Quantities.create(1.0, Units.DEGREE), null),
        new TestCase(POINT_10_6, POINT_10_5,     Quantities.create(1.0, Units.DEGREE), null),
        /*
         * The operation computes in the coordinate reference system of the first geometry,
         * and does not transform the second one.
         */
        new TestCase(POINT_GEOGRAPHIC, POINT_NORMALIZED, null, OperationException.class),
        /*
         * The distance from an empty geometry to any geometry, including itself, is infinite.
         */
        new TestCase(EMPTY_1,   NON_EMPTY, INFINITY, null),
        new TestCase(NON_EMPTY, EMPTY_1,   INFINITY, null),
        new TestCase(EMPTY_1,   EMPTY_1,   INFINITY, null),
        new TestCase(EMPTY_1,   EMPTY_2,   INFINITY, null)
    };

    /**
     * Tests {@code distance(Geometry, Geometry)} on all declared test cases.
     */
    @Test
    public void testDistance() {
        for (final TestCase entry : ENTRIES) {
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
