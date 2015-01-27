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
package org.apache.sis.util.logging;

import java.util.concurrent.TimeUnit;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.util.logging.PerformanceLevel.*;


/**
 * Tests the {@link PerformanceLevel} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final strictfp class PerformanceLevelTest extends TestCase {
    /**
     * Ensures that the duration are conform to the documentation.
     */
    @Test
    public void testGetMinDuration() {
        assertEquals(0,   PERFORMANCE.getMinDuration(TimeUnit.NANOSECONDS));
        assertEquals(100, SLOW       .getMinDuration(TimeUnit.MILLISECONDS));
        assertEquals(1,   SLOWER     .getMinDuration(TimeUnit.SECONDS));
        assertEquals(5,   SLOWEST    .getMinDuration(TimeUnit.SECONDS));
    }

    /**
     * Tests modifying the configuration.
     */
    @Test
    public void testSetMinDuration() {
        try {
            SLOW.setMinDuration(2, TimeUnit.SECONDS);
            assertEquals(0, PERFORMANCE.getMinDuration(TimeUnit.SECONDS));
            assertEquals(2, SLOW       .getMinDuration(TimeUnit.SECONDS));
            assertEquals(2, SLOWER     .getMinDuration(TimeUnit.SECONDS));
            assertEquals(5, SLOWEST    .getMinDuration(TimeUnit.SECONDS));

            SLOWEST.setMinDuration(1, TimeUnit.SECONDS);
            assertEquals(0, PERFORMANCE.getMinDuration(TimeUnit.SECONDS));
            assertEquals(1, SLOW       .getMinDuration(TimeUnit.SECONDS));
            assertEquals(1, SLOWER     .getMinDuration(TimeUnit.SECONDS));
            assertEquals(1, SLOWEST    .getMinDuration(TimeUnit.SECONDS));

            PERFORMANCE.setMinDuration(6, TimeUnit.SECONDS);
            assertEquals(0, PERFORMANCE.getMinDuration(TimeUnit.SECONDS));
            assertEquals(6, SLOW       .getMinDuration(TimeUnit.SECONDS));
            assertEquals(6, SLOWER     .getMinDuration(TimeUnit.SECONDS));
            assertEquals(6, SLOWEST    .getMinDuration(TimeUnit.SECONDS));
        } finally {
            SLOW   .setMinDuration(100, TimeUnit.MILLISECONDS);
            SLOWER .setMinDuration(1,   TimeUnit.SECONDS);
            SLOWEST.setMinDuration(5,   TimeUnit.SECONDS);
        }
    }

    /**
     * Tests the {@link PerformanceLevel#forDuration(long, TimeUnit)} method.
     */
    @Test
    public void testForDuration() {
        assertSame(SLOW,        forDuration(500, TimeUnit.MILLISECONDS));
        assertSame(SLOWER,      forDuration(2,   TimeUnit.SECONDS));
        assertSame(SLOWEST,     forDuration(6,   TimeUnit.SECONDS));
        assertSame(PERFORMANCE, forDuration(50,  TimeUnit.MILLISECONDS));
    }
}
