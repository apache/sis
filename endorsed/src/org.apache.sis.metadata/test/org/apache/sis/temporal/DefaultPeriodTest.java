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

import java.time.LocalDate;
import java.time.Period;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link DefaultPeriod} class.
 *
 * @author  Mehdi Sidhoum (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class DefaultPeriodTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultPeriodTest() {
    }

    /**
     * Tests {@link DefaultPeriod#getBeginning()} and {@link DefaultPeriod#getEnding()}.
     * Opportunistically tests {@link TemporalUtilities#createPeriod(Temporal, Temporal)} too.
     */
    @Test
    public void testBounds() {
        var beginning = LocalDate.of(2010, 5, 1);
        var ending    = LocalDate.of(2015, 8, 6);
        var period    = TemporalUtilities.createPeriod(beginning, ending);
        assertEquals(beginning, period.getBeginning().getPosition());
        assertEquals(ending,    period.getEnding().getPosition());
    }

    /**
     * Test of equals and hash code methods.
     */
    @Test
    public void testEquals() {
        var p1 = TemporalUtilities.createPeriod(LocalDate.of(2000, 1, 1), LocalDate.of(2010, 1, 1));
        var p2 = TemporalUtilities.createPeriod(LocalDate.of(1988, 1, 1), LocalDate.of(2010, 1, 1));
        var p3 = TemporalUtilities.createPeriod(LocalDate.of(1988, 1, 1), LocalDate.of(2010, 1, 1));

        assertNotEquals(p1, p2);
        assertNotEquals(p1.hashCode(), p2.hashCode());

        assertEquals(p3, p2);
        assertEquals(p3.hashCode(), p2.hashCode());
    }

    /**
     * Tests {@link DefaultPeriod#toString()}.
     */
    @Test
    public void testToString() {
        var p1 = TemporalUtilities.createPeriod(LocalDate.of(2000, 1, 1), LocalDate.of(2010, 1, 1));
        assertEquals("2000-01-01/2010-01-01", p1.toString());
    }

    /**
     * Tests {@link DefaultPeriod#length()}.
     */
    @Test
    public void testLength() {
        var beginning = LocalDate.of(2010, 5, 1);
        var ending    = LocalDate.of(2015, 8, 6);
        var period    = TemporalUtilities.createPeriod(beginning, ending);
        assertEquals(Period.of(5, 3, 5), period.length());
    }
}
