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
package org.apache.sis.metadata.sql.privy;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link SQLBuilder} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class SQLBuilderTest extends TestCase {
    /**
     * The builder to use for the tests.
     */
    private final SQLBuilder builder;

    /**
     * Creates a new test case.
     *
     * @throws SQLException should never happen for this test.
     */
    public SQLBuilderTest() throws SQLException {
        builder = new SQLBuilder(null, false);
    }

    /**
     * Asserts that the builder content is equal to the expected value, then clears the buffer.
     *
     * @param expected the expected content.
     */
    private void compareAndClear(final String expected) {
        assertEquals(expected, builder.toString());
        assertSame(builder, builder.clear());
    }

    /**
     * Tests the formatting of values of different types.
     */
    @Test
    public void testAppendValue() {
        assertSame(builder, builder.appendValue(46));
        compareAndClear("46");

        assertSame(builder, builder.appendValue("46"));
        compareAndClear("'46'");

        assertSame(builder, builder.appendValue(Year.of(2024)));
        compareAndClear("'2024'");

        assertSame(builder, builder.appendValue(LocalDate.of(2024, 10, 2)));
        compareAndClear("'2024-10-02'");

        assertSame(builder, builder.appendValue(LocalTime.of(9, 5)));
        compareAndClear("'09:05'");

        assertSame(builder, builder.appendValue(LocalTime.of(18, 32, 7)));
        compareAndClear("'18:32:07'");

        assertSame(builder, builder.appendValue(LocalDateTime.of(2024, 10, 2, 18, 32, 7)));
        compareAndClear("'2024-10-02 18:32:07'");

        assertSame(builder, builder.appendValue(OffsetDateTime.of(2024, 10, 2, 18, 32, 7, 0, ZoneOffset.ofHours(4))));
        compareAndClear("'2024-10-02 18:32:07+04:00'");

        assertSame(builder, builder.appendValue(ZonedDateTime.of(2024, 10, 2, 18, 32, 7, 0, ZoneId.of("CET"))));
        compareAndClear("'2024-10-02 18:32:07+02:00'");
    }
}
