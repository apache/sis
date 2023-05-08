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
package org.apache.sis.metadata.iso.quality;

import java.util.List;
import java.time.Instant;
import java.time.temporal.Temporal;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.Assertions.assertSerializedEquals;


/**
 * Tests {@link DefaultEvaluationMethod}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
 */
public final class DefaultEvaluationMethodTest extends TestCase {
    /**
     * Tests the {@link DefaultEvaluationMethod#getDates()} list,
     * which is backed by a custom implementation.
     */
    @Test
    public void testDates() {
        final Instant now   = Instant.now();
        final Instant later = Instant.ofEpochSecond(now.getEpochSecond()+ 60);
        final List<Temporal> dates = (List<Temporal>) new DefaultEvaluationMethod().getDates();
        /*
         * dates = []
         */
        assertTrue("isEmpty()", dates.isEmpty());
        assertCanNotGet(dates, 2);
        assertCanNotGet(dates, 1);
        assertCanNotGet(dates, 0);
        /*
         * dates = [now]
         */
        assertCanNotAdd(dates, 2, now);
        assertCanNotAdd(dates, 1, now);
        dates.add(0, now);
        assertEquals("size()", 1, dates.size());
        assertCanNotGet(dates, 2);
        assertCanNotGet(dates, 1);
        assertEquals(now, dates.get(0));
        /*
         * dates = [now, later]
         */
        assertCanNotAdd(dates, 2, later);
        dates.add(1, later);
        assertEquals("size()", 2, dates.size());
        assertCanNotGet(dates, 2);
        assertEquals(later, dates.get(1));
        assertEquals(now,   dates.get(0));
        /*
         * dates = [later]
         */
        assertEquals(now, dates.remove(0));
        assertEquals("size()", 1, dates.size());
        assertCanNotGet(dates, 2);
        assertCanNotGet(dates, 1);
        assertEquals(later, dates.get(0));
        /*
         * dates = [now, later]
         */
        dates.add(0, now);
        assertEquals("size()", 2, dates.size());
        assertCanNotGet(dates, 2);
        assertEquals(later, dates.get(1));
        assertEquals(now,   dates.get(0));

        assertSerializedEquals(dates);
    }

    /**
     * Asserts that we cannot get a date at the given index in the given list.
     */
    private static void assertCanNotGet(final List<Temporal> dates, final int index) {
        try {
            dates.get(index);
            fail("Should not be allowed to get an element at index " + index);
        } catch (IndexOutOfBoundsException e) {
            // This is the expected exception.
        }
    }

    /**
     * Asserts that we cannot get add a date at the given index in the given list.
     */
    private static void assertCanNotAdd(final List<Temporal> dates, final int index, final Temporal date) {
        try {
            dates.add(index, date);
            fail("Should not be allowed to add an element at index " + index);
        } catch (IndexOutOfBoundsException e) {
            // This is the expected exception.
        }
    }
}
