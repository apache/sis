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

import java.util.Date;
import java.util.List;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link AbstractElement}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final strictfp class AbstractElementTest extends TestCase {
    /**
     * Tests the {@link AbstractElement#getDates()} list, which is backed by a custom implementation.
     */
    @Test
    public void testDates() {
        final Date now   = new Date();
        final Date later = new Date(now.getTime() + 60000);
        final List<Date> dates = (List<Date>) new AbstractElement().getDates();
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
     * Asserts that we can not get a date at the given index in the given list.
     */
    private static void assertCanNotGet(final List<Date> dates, final int index) {
        try {
            dates.get(index);
            fail("Should not be allowed to get an element at index " + index);
        } catch (IndexOutOfBoundsException e) {
            // This is the expected exception.
        }
    }

    /**
     * Asserts that we can not get add a date at the given index in the given list.
     */
    private static void assertCanNotAdd(final List<Date> dates, final int index, final Date date) {
        try {
            dates.add(index, date);
            fail("Should not be allowed to add an element at index " + index);
        } catch (IndexOutOfBoundsException e) {
            // This is the expected exception.
        }
    }
}
