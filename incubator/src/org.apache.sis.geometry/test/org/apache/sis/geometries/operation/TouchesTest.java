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
import static org.apache.sis.geometries.operation.TestData.EMPTY_1;
import static org.apache.sis.geometries.operation.TestData.EMPTY_2;
import static org.apache.sis.geometries.operation.TestData.NON_EMPTY;
import static org.apache.sis.geometries.operation.TestData.POINT_A;
import static org.apache.sis.geometries.operation.TestData.POINT_A_BIS;
import static org.apache.sis.geometries.operation.TestData.POINT_B;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;


/**
 * Tests the {@code touches} operations of {@link GeometryProcessor}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class TouchesTest {
    /**
     * The inputs and expected result of a single test of {@code touches(Geometry, Geometry)}.
     *
     * @param input    the geometry on which the operation is invoked.
     * @param other    the other operand.
     * @param expected the expected result, or {@code null} if an exception is expected.
     * @param error    the type of the expected exception, or {@code null} if the operation should succeed.
     */
    private record TestCase(Geometry input,
                         Geometry other,
                         Boolean expected,
                         Class<? extends Exception> error)
    {
    }

    /**
     * All test cases of {@code touches(Geometry, Geometry)}.
     */
    private static final TestCase[] ENTRIES = {
        /*
         * Touching requires a shared position which is in the boundary of at least one of the
         * two geometries. The empty set has no boundary, therefore it touches nothing.
         */
        new TestCase(EMPTY_1,   NON_EMPTY, false, null),
        new TestCase(NON_EMPTY, EMPTY_1,   false, null),
        new TestCase(EMPTY_1,   EMPTY_1,   false, null),
        new TestCase(EMPTY_1,   EMPTY_2,   false, null),
        /*
         * Two points cannot touch: the boundary of a point is empty, therefore the only
         * position they may share is interior to both of them.
         */
        new TestCase(POINT_A, POINT_A_BIS, false, null),
        new TestCase(POINT_A, POINT_B,     false, null)
    };

    /**
     * Tests {@code touches(Geometry, Geometry)} on all declared test cases.
     */
    @Test
    public void testTouches() {
        for (final TestCase entry : ENTRIES) {
            try {
                final boolean result = new GeometryProcessor().touches(entry.input(), entry.other());
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
