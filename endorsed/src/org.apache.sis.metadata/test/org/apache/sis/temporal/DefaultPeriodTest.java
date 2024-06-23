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
import java.time.Year;
import org.opengis.temporal.TemporalPrimitive;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.opengis.filter.TemporalOperatorName;


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
     * Opportunistically tests {@link TemporalObjects#createPeriod(Temporal, Temporal)} too.
     */
    @Test
    public void testBounds() {
        var beginning = LocalDate.of(2010, 5, 1);
        var ending    = LocalDate.of(2015, 8, 6);
        var period    = TemporalObjects.createPeriod(beginning, ending);
        assertEquals(beginning, period.getBeginning().getPosition());
        assertEquals(ending,    period.getEnding().getPosition());
    }

    /**
     * Test of equals and hash code methods.
     */
    @Test
    public void testEquals() {
        var p1 = TemporalObjects.createPeriod(LocalDate.of(2000, 1, 1), LocalDate.of(2010, 1, 1));
        var p2 = TemporalObjects.createPeriod(LocalDate.of(1988, 1, 1), LocalDate.of(2010, 1, 1));
        var p3 = TemporalObjects.createPeriod(LocalDate.of(1988, 1, 1), LocalDate.of(2010, 1, 1));

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
        var p1 = TemporalObjects.createPeriod(LocalDate.of(2000, 1, 1), LocalDate.of(2010, 1, 1));
        assertEquals("2000-01-01/2010-01-01", p1.toString());
    }

    /**
     * Tests {@link DefaultPeriod#length()}.
     */
    @Test
    public void testLength() {
        var beginning = LocalDate.of(2010, 5, 1);
        var ending    = LocalDate.of(2015, 8, 6);
        var period    = TemporalObjects.createPeriod(beginning, ending);
        assertEquals(Period.of(5, 3, 5), period.length());
    }

    /**
     * Tests {@link DefaultPeriod#findRelativePosition(TemporalPrimitive)}.
     */
    @Test
    public void testFindRelativePosition() {
        var p04 = TemporalObjects.createPeriod(Year.of(2000), Year.of(2004));
        var p56 = TemporalObjects.createPeriod(Year.of(2005), Year.of(2006));
        var p13 = TemporalObjects.createPeriod(Year.of(2001), Year.of(2003));
        var p14 = TemporalObjects.createPeriod(Year.of(2001), Year.of(2004));
        assertRelativePositionEquals(TemporalOperatorName.EQUALS,    p04, p04);
        assertRelativePositionEquals(TemporalOperatorName.BEFORE,    p04, p56);
        assertRelativePositionEquals(TemporalOperatorName.CONTAINS,  p04, p13);
        assertRelativePositionEquals(TemporalOperatorName.ENDED_BY,  p04, p14);
        assertRelativePositionEquals(TemporalOperatorName.EQUALS,    p56, p56);
        assertRelativePositionEquals(TemporalOperatorName.AFTER,     p56, p13);
        assertRelativePositionEquals(TemporalOperatorName.AFTER,     p56, p14);
        assertRelativePositionEquals(TemporalOperatorName.EQUALS,    p13, p13);
        assertRelativePositionEquals(TemporalOperatorName.BEGINS,    p13, p14);
        assertRelativePositionEquals(TemporalOperatorName.EQUALS,    p14, p14);
    }

    /**
     * Finds the relative position of {@code p1} relative to {@code p2} and compare against the expected value.
     * Then reverses argument order and test again.
     *
     * @param expected  the expected result.
     * @param self      period for which to find the relative position.
     * @param other     the period against which {@code self} is compared.
     */
    private static void assertRelativePositionEquals(TemporalOperatorName expected,
            org.opengis.temporal.Period self, org.opengis.temporal.Period other)
    {
        assertEquals(expected, self.findRelativePosition(other));
        assertEquals(expected.reversed().orElseThrow(), other.findRelativePosition(self));
    }
}
