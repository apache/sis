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
package org.apache.sis.temporal;

import java.time.ZoneId;
import java.time.ZonedDateTime;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link TimeMethods} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class TimeMethodsTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public TimeMethodsTest() {
    }

    /**
     * Tests {@link TimeMethods#withZone(Object, ZoneId)}.
     */
    @Test
    public void testWithZone() {
        final ZoneId source = ZoneId.of("America/New_York");
        final var time  = ZonedDateTime.of(2025, 4, 3, 8, 50, 0, 0, source);
        final var local = time.toLocalDateTime();
        assertTrue(TimeMethods.withZone(local, source, false).isEmpty());
        assertEquals(time, TimeMethods.withZone(local, source, true).orElseThrow());

        final ZoneId target = ZoneId.of("CET");
        final var expected = ZonedDateTime.of(2025, 4, 3, 8+6, 50, 0, 0, target);
        assertEquals(expected, TimeMethods.withZone(time, target, true).orElseThrow());
        assertEquals(expected, TimeMethods.withZone(time, target, false).orElseThrow());
    }
}
