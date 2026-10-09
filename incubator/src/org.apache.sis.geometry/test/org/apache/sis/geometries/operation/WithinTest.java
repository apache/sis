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
 * Tests the {@code within} operations of {@link GeometryProcessor}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class WithinTest extends AbstractD9IMTest {
    /**
     * The inputs and expected result of a single test of {@code within(Geometry, Geometry)}.
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
     * All test cases of {@code within(Geometry, Geometry)}.
     */
    private static final TestCase[] ENTRIES = {
        // The empty set is a subset of every geometry. Converse of `contains(Geometry, Geometry)`.
        new TestCase(EMPTY_1,            NON_EMPTY,     true,  null),
        new TestCase(EMPTY_1,            EMPTY_1,       true,  null),
        new TestCase(EMPTY_1,            EMPTY_2,       true,  null),
        new TestCase(NON_EMPTY,          EMPTY_1,       false, null),
        /*
         * A point is a subset of another point only when the two are at the same position.
         */
        new TestCase(POINT_A,            POINT_A_BIS,   true,  null),
        new TestCase(POINT_A,            POINT_B,       false, null),
        new TestCase(POINT_B,            POINT_A,       false, null),
        /*
         * A position and a surface. A position on the boundary of a surface is not within it:
         * being within requires a position interior to both geometries, and the interior of a
         * position is the position itself.
         */
        new TestCase(POINT_CENTER,       SQUARE,        true,  null),
        new TestCase(POINT_ON_EDGE,      SQUARE,        false, null),
        new TestCase(POINT_OUTSIDE,      SQUARE,        false, null),
        /*
         * A position and a curve, the position being interior to the curve or one of its ends.
         */
        new TestCase(POINT_ON_EDGE,      LINE_BOTTOM,   true,  null),
        new TestCase(POINT_CORNER,       LINE_BOTTOM,   false, null),
        /*
         * A curve and a surface. A curve lying on the boundary of a surface is not within it.
         */
        new TestCase(LINE_INSIDE,        SQUARE,        true,  null),
        new TestCase(LINE_BOTTOM,        SQUARE,        false, null),
        /*
         * Two surfaces, and sets of geometries.
         */
        new TestCase(SQUARE_INNER,       SQUARE,        true,  null),
        new TestCase(SQUARE_OVERLAP,     SQUARE,        false, null),
        new TestCase(SQUARE,             SQUARE_BIS,    true,  null),
        new TestCase(MULTI_POINT_INSIDE, SQUARE,        true,  null),
        new TestCase(MULTI_POINT_SPREAD, SQUARE,        false, null),
        new TestCase(SQUARE,             MULTI_POLYGON, true,  null)
    };

    /**
     * Tests {@code within(Geometry, Geometry)} on all declared test cases.
     */
    @Test
    public void testWithin() {
        for (final TestCase entry : ENTRIES) {
            try {
                final boolean result = new GeometryProcessor().within(entry.input(), entry.other());
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
