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
 * Tests the {@code intersects} operations of {@link GeometryProcessor}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class IntersectsTest {
    /**
     * The inputs and expected result of a single test of {@code intersects(Geometry, Geometry)}.
     *
     * @param input    the geometry on which the operation is invoked.
     * @param other    the other operand.
     * @param expected the expected result, or {@code null} if an exception is expected.
     * @param error    the type of the expected exception, or {@code null} if the operation should succeed.
     */
    private record Entry(Geometry input,
                         Geometry other,
                         Boolean expected,
                         Class<? extends Exception> error)
    {
    }

    /**
     * All test cases of {@code intersects(Geometry, Geometry)}.
     */
    private static final Entry[] ENTRIES = {
    };

    /**
     * Tests {@code intersects(Geometry, Geometry)} on all declared test cases.
     */
    @Test
    public void testIntersects() {
        for (final Entry entry : ENTRIES) {
            try {
                final boolean result = new GeometryProcessor().intersects(entry.input(), entry.other());
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
