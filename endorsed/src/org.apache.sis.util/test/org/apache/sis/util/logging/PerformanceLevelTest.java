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
import static java.util.logging.Level.FINE;
import static org.apache.sis.util.logging.PerformanceLevel.*;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link PerformanceLevel} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class PerformanceLevelTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public PerformanceLevelTest() {
    }

    /**
     * Ensures that the duration are conform to the documentation.
     */
    @Test
    public void testGetMinDuration() {
        assertEquals(1,  SLOWNESS.getMinDuration(TimeUnit.SECONDS));
        assertEquals(10, SLOWER  .getMinDuration(TimeUnit.SECONDS));
    }

    /**
     * Tests modifying the configuration.
     */
    @Test
    @ResourceLock("Logging")
    public void testSetMinDuration() {
        final long t1 = SLOWNESS.getMinDuration(TimeUnit.SECONDS);
        final long t2 = SLOWER  .getMinDuration(TimeUnit.SECONDS);
        try {
            SLOWNESS.setMinDuration(80, TimeUnit.SECONDS);
            assertEquals(80, SLOWNESS.getMinDuration(TimeUnit.SECONDS));
            assertEquals(80, SLOWER  .getMinDuration(TimeUnit.SECONDS));

            SLOWER.setMinDuration(4, TimeUnit.SECONDS);
            assertEquals(4, SLOWNESS.getMinDuration(TimeUnit.SECONDS));
            assertEquals(4, SLOWER  .getMinDuration(TimeUnit.SECONDS));

            SLOWNESS.setMinDuration(6, TimeUnit.SECONDS);
            assertEquals(6, SLOWNESS.getMinDuration(TimeUnit.SECONDS));
            assertEquals(6, SLOWER  .getMinDuration(TimeUnit.SECONDS));
        } finally {
            SLOWER.setMinDuration(t1, TimeUnit.SECONDS);
            SLOWER.setMinDuration(t2, TimeUnit.SECONDS);
        }
    }

    /**
     * Tests the {@link PerformanceLevel#forDuration(long, TimeUnit)} method.
     */
    @Test
    public void testForDuration() {
        assertSame(SLOWNESS, forDuration( 2, TimeUnit.SECONDS));
        assertSame(SLOWER,   forDuration(10, TimeUnit.SECONDS));
        assertSame(SLOWER,   forDuration(20, TimeUnit.SECONDS));
        assertSame(FINE,     forDuration(50, TimeUnit.MILLISECONDS));
    }
}
