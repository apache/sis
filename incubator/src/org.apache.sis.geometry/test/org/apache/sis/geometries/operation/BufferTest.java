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

import javax.measure.quantity.Length;
import org.apache.sis.geometries.Geometry;

// Test dependencies
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;


/**
 * Tests the {@code buffer} operations of {@link GeometryProcessor}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class BufferTest {
    /**
     * The inputs and expected result of a single test of {@code buffer(Geometry, double)}.
     *
     * @param input    the geometry on which the operation is invoked.
     * @param distance the buffer distance.
     * @param expected the expected result, or {@code null} if an exception is expected.
     * @param error    the type of the expected exception, or {@code null} if the operation should succeed.
     */
    private record Entry(Geometry input,
                         double distance,
                         Geometry expected,
                         Class<? extends Exception> error)
    {
    }

    /**
     * All test cases of {@code buffer(Geometry, double)}.
     */
    private static final Entry[] ENTRIES = {
    };

    /**
     * Tests {@code buffer(Geometry, double)} on all declared test cases.
     */
    @Test
    public void testBuffer() {
        for (final Entry entry : ENTRIES) {
            try {
                final Geometry result = new GeometryProcessor().buffer(entry.input(), entry.distance());
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
     * The inputs and expected result of a single test of {@code buffer(Geometry, Length)}.
     *
     * @param input    the geometry on which the operation is invoked.
     * @param radius   the buffer radius.
     * @param expected the expected result, or {@code null} if an exception is expected.
     * @param error    the type of the expected exception, or {@code null} if the operation should succeed.
     */
    private record RadiusEntry(Geometry input,
                               Length radius,
                               Geometry expected,
                               Class<? extends Exception> error)
    {
    }

    /**
     * All test cases of {@code buffer(Geometry, Length)}.
     */
    private static final RadiusEntry[] RADIUS_ENTRIES = {
    };

    /**
     * Tests {@code buffer(Geometry, Length)} on all declared test cases.
     */
    @Test
    public void testBufferByRadius() {
        for (final RadiusEntry entry : RADIUS_ENTRIES) {
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
}
