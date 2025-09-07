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
package org.apache.sis.referencing.internal;

import java.time.Year;
import java.time.YearMonth;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link Epoch}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
public final class EpochTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public EpochTest() {
    }

    /**
     * Tests with {@link Year}.
     */
    @Test
    public void testYear() {
        var epoch = new Epoch(Year.of(2010), false);
        assertEquals(2010, epoch.value);
        assertEquals(0, epoch.precision);
        assertEquals("Epoch[2010]", epoch.toString());
    }

    /**
     * Tests with {@link YearMonth}.
     */
    @Test
    public void testYearMonth() {
        var epoch = new Epoch(YearMonth.of(2016, 1), false);
        assertEquals(2016, epoch.value);
        assertEquals(2, epoch.precision);
        assertEquals("Epoch[2016.00]", epoch.toString());

        epoch = new Epoch(YearMonth.of(2016, 7), false);
        assertEquals(2016.49726775956, epoch.value, 1E-11);
        assertEquals(2, epoch.precision);
        assertEquals("Epoch[2016.50]", epoch.toString());
    }

    /**
     * Tests with {@link LocalDate}.
     */
    @Test
    public void testLocalDate() {
        var epoch = new Epoch(LocalDate.of(2016, 7, 20), false);
        assertEquals(2016.54918032787, epoch.value, 1E-11);
        assertEquals(3, epoch.precision);
        assertEquals("Epoch[2016.549]", epoch.toString());
    }

    /**
     * Tests with {@link LocalDateTime}.
     */
    @Test
    public void testLocalDateTime() {
        var epoch = new Epoch(LocalDateTime.of(2014, 2, 15, 10, 40), false);
        assertEquals(2014.12450532725, epoch.value, 1E-11);
        assertEquals(8, epoch.precision);
        assertEquals("Epoch[2014.12450533]", epoch.toString());
    }

    /**
     * Tests {@link Epoch#fromYear(String)}.
     */
    @Test
    public void testFromYear() {
        assertEquals(     Year.of(2010),                Epoch.fromYear("2010"));
        assertEquals(YearMonth.of(2010, Month.JANUARY), Epoch.fromYear("2010.0"));
        assertEquals(YearMonth.of(2010, Month.APRIL),   Epoch.fromYear("2010.3"));
    }
}
