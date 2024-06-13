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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import org.opengis.temporal.TemporalPrimitive;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.TemporalOperatorName;
import org.opengis.temporal.IndeterminateValue;
import org.opengis.temporal.IndeterminatePositionException;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link DefaultInstant} class.
 *
 * @author  Mehdi Sidhoum (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class DefaultInstantTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultInstantTest() {
    }

    /**
     * Tests {@link DefaultInstant#getPosition()}.
     * Opportunistically tests {@link TemporalObjects#createInstant(Temporal)} too.
     */
    @Test
    public void testGetPosition() {
        var date    = LocalDate.of(2010, 5, 1);
        var instant = TemporalObjects.createInstant(date);
        assertEquals(date, instant.getPosition());
    }

    /**
     * Test of equals and hash code methods.
     */
    @Test
    public void testEquals() {
        var instant1 = new DefaultInstant(LocalDate.of(2000, 1, 1), null);
        var instant2 = new DefaultInstant(LocalDate.of(1988, 1, 1), null);
        var instant3 = new DefaultInstant(LocalDate.of(1988, 1, 1), null);

        assertNotEquals(instant1, instant2);
        assertNotEquals(instant1.hashCode(), instant2.hashCode());

        assertEquals(instant3, instant2);
        assertEquals(instant3.hashCode(), instant2.hashCode());
    }

    /**
     * Tests {@link DefaultInstant#toString()}.
     */
    @Test
    public void testToString() {
        var date = LocalDate.of(2010, 5, 1);
        assertEquals("2010-05-01", new DefaultInstant(date, null).toString());
        assertEquals("after 2010-05-01", new DefaultInstant(date, IndeterminateValue.AFTER).toString());
    }

    /**
     * Tests {@link DefaultInstant#findRelativePosition(TemporalPrimitive)} between instants.
     */
    @Test
    public void testRelativePositionBetweenInstants() {
        final var t1981 = new DefaultInstant(LocalDate.of(1981, 6, 5), null);
        final var t2000 = new DefaultInstant(LocalDate.of(2000, 1, 1), null);
        assertEquals(TemporalOperatorName.BEFORE, t1981.findRelativePosition(t2000));
        assertEquals(TemporalOperatorName.AFTER,  t2000.findRelativePosition(t1981));
        assertEquals(TemporalOperatorName.EQUALS, t2000.findRelativePosition(t2000));
    }

    /**
     * Tests {@link DefaultInstant#findRelativePosition(TemporalPrimitive)} between an instant and a period.
     */
    @Test
    public void testRelativePositionBetweenInstantAndPeriod() {
        final var before = new DefaultInstant(LocalDate.of(1981, 1, 1), null);
        final var begins = new DefaultInstant(LocalDate.of(1981, 6, 5), null);
        final var during = new DefaultInstant(LocalDate.of(1990, 1, 1), null);
        final var ends   = new DefaultInstant(LocalDate.of(2000, 1, 1), null);
        final var after  = new DefaultInstant(LocalDate.of(2000, 1, 2), null);
        final var period = new DefaultPeriod(begins, ends);
        assertEquals(TemporalOperatorName.BEFORE, before.findRelativePosition(period));
        assertEquals(TemporalOperatorName.BEGINS, begins.findRelativePosition(period));
        assertEquals(TemporalOperatorName.DURING, during.findRelativePosition(period));
        assertEquals(TemporalOperatorName.ENDS,   ends  .findRelativePosition(period));
        assertEquals(TemporalOperatorName.AFTER,  after .findRelativePosition(period));
    }

    /**
     * Tests {@link DefaultInstant#findRelativePosition(TemporalPrimitive)} with indeterminate instants.
     * The position tested are "before" and "after".
     */
    @Test
    public void testIndeterminatePosition() {
        final var before2000 = new DefaultInstant(LocalDate.of(2000, 1, 1), IndeterminateValue.BEFORE);
        final var  after2010 = new DefaultInstant(LocalDate.of(2010, 1, 1), IndeterminateValue.AFTER);
        final var before2020 = new DefaultInstant(LocalDate.of(2020, 1, 1), IndeterminateValue.BEFORE);

        assertEquals(TemporalOperatorName.BEFORE, before2000.findRelativePosition( after2010));
        assertEquals(TemporalOperatorName.AFTER,   after2010.findRelativePosition(before2000));
        assertIndeterminate(() ->  after2010.findRelativePosition(before2020));
        assertIndeterminate(() -> before2000.findRelativePosition(before2020));
        assertIndeterminate(() -> before2020.findRelativePosition(before2000));
    }

    /**
     * Asserts that the result of the given comparison is indeterminate.
     *
     * @param  c  the comparison to perform.
     */
    private static void assertIndeterminate(final Executable c) {
        assertNotNull(assertThrows(IndeterminatePositionException.class, c).getMessage());
    }

    /**
     * Tests {@link DefaultInstant#distance(TemporalPrimitive)} between two locale dates.
     */
    @Test
    public void testDistanceBetweenLocalDates() {
        final var t1981 = new DefaultInstant(LocalDate.of(1981, 6, 5), null);
        final var t2000 = new DefaultInstant(LocalDate.of(2000, 8, 8), null);
        final Period expected = Period.of(19, 2, 3);
        assertEquals(expected,    t1981.distance(t2000));
        assertEquals(expected,    t2000.distance(t1981));
        assertEquals(Period.ZERO, t2000.distance(t2000));
    }

    /**
     * Tests {@link DefaultInstant#distance(TemporalPrimitive)} between two dates with timezone.
     */
    @Test
    public void testDistanceBetweenZonedDates() {
        final var t2000 = new DefaultInstant(ZonedDateTime.of(2000, 6, 5, 12, 4, 0, 0, ZoneOffset.UTC), null);
        final var t2001 = new DefaultInstant(ZonedDateTime.of(2001, 6, 5, 14, 4, 0, 0, ZoneOffset.UTC), null);
        final Duration expected = Duration.ofDays(365).plusHours(2);
        assertEquals(expected,      t2000.distance(t2001));
        assertEquals(expected,      t2001.distance(t2000));
        assertEquals(Duration.ZERO, t2000.distance(t2000));
    }

    /**
     * Tests {@link DefaultInstant#distance(TemporalPrimitive)} between two dates with times.
     * The period cannot be expressed with standard {@link java.time} objects.
     */
    @Test
    public void testDistanceBetweenLocalDateTimes() {
        final var t1 = new DefaultInstant(LocalDateTime.of(2000, 6, 5, 12, 4, 0, 0), null);
        final var t3 = new DefaultInstant(LocalDateTime.of(2001, 6, 9, 14, 4, 0, 0), null);
        final var t2 = new DefaultInstant(LocalDateTime.of(2001, 6, 9, 10, 4, 0, 0), null);

        Object expected = "P1Y4DT2H";
        assertEquals(expected,    t1.distance(t3).toString());
        assertEquals(expected,    t3.distance(t1).toString());
        assertEquals(Period.ZERO, t1.distance(t1));

        expected = "P1Y3DT22H";
        assertEquals(expected,    t1.distance(t2).toString());
        assertEquals(expected,    t2.distance(t1).toString());
        assertEquals(Period.ZERO, t2.distance(t2));

        expected = Duration.ofHours(4);
        assertEquals(expected,    t2.distance(t3));
        assertEquals(expected,    t3.distance(t2));
        assertEquals(Period.ZERO, t3.distance(t3));
    }

    /**
     * Tests {@link DefaultInstant#distance(TemporalPrimitive)} between an instant and a period.
     */
    @Test
    public void testDistanceWithPeriod() {
        final var before = new DefaultInstant(LocalDate.of(1981, 1, 1), null);
        final var begins = new DefaultInstant(LocalDate.of(1981, 6, 5), null);
        final var during = new DefaultInstant(LocalDate.of(1990, 1, 1), null);
        final var ends   = new DefaultInstant(LocalDate.of(2000, 1, 1), null);
        final var after  = new DefaultInstant(LocalDate.of(2000, 1, 2), null);
        final var period = new DefaultPeriod(begins, ends);

        assertEquals(Period.of(0, 5, 4), before.distance(period));
        assertEquals(Period.ZERO,        begins.distance(period));
        assertEquals(Duration.ZERO,      during.distance(period));      // `Duration` considered an implementation details.
        assertEquals(Period.ZERO,        ends  .distance(period));
        assertEquals(Period.of(0, 0, 1), after .distance(period));
    }
}
