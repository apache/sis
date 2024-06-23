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

import java.time.Period;
import java.time.Duration;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link GeneralDuration} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class GeneralDurationTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public GeneralDurationTest() {
    }

    /**
     * Tests {@link GeneralDuration#parse(CharSequence)}.
     */
    @Test
    public void testParse() {
        assertEquals(Period.of(2, 3, 4),    GeneralDuration.parse("P2Y3M4D"));
        assertEquals(Duration.ofHours(100), GeneralDuration.parse("PT100H"));
        assertEquals(Duration.ofHours(200), GeneralDuration.parse("pt200H"));
        var r = assertInstanceOf(GeneralDuration.class, GeneralDuration.parse("P2Y3M4DT10H"));
        assertEquals(Period.of(2, 3, 4),    r.period);
        assertEquals(Duration.ofHours(10),  r.time);
    }
}
