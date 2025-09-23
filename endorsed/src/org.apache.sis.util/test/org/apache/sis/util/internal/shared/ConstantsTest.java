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
package org.apache.sis.util.internal.shared;

import java.util.concurrent.TimeUnit;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link ConstantsTest} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ConstantsTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public ConstantsTest() {
    }

    /**
     * Verifies the constant values related to time duration.
     */
    @Test
    public void verifyConstantValues() {
        assertEquals(TimeUnit.DAYS.toSeconds(1),       Constants.SECONDS_PER_DAY);
        assertEquals(TimeUnit.DAYS.toMillis(1),        Constants.MILLISECONDS_PER_DAY);
        assertEquals(TimeUnit.DAYS.toNanos(1),         Constants.NANOSECONDS_PER_DAY);
        assertEquals(TimeUnit.MILLISECONDS.toNanos(1), Constants.NANOS_PER_MILLISECOND);
        assertEquals(TimeUnit.SECONDS.toNanos(1),      Constants.NANOS_PER_SECOND);
        assertEquals(365.24219 * (24*60*60 * 1000),    Constants.MILLIS_PER_TROPICAL_YEAR, 0.00001 * (24*60*60 * 1000));
    }
}
